/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.ldap.handlers;


import java.util.concurrent.TimeUnit;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.ReferralManager;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerStringValue;
import org.apache.directory.server.core.event.EventType;
import org.apache.directory.server.core.event.NotificationCriteria;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.OperationAbandonedException;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.Response;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.Referral;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.message.SearchResponseDone;
import org.apache.directory.shared.ldap.message.SearchResponseEntry;
import org.apache.directory.shared.ldap.message.SearchResponseEntryImpl;
import org.apache.directory.shared.ldap.message.SearchResponseReference;
import org.apache.directory.shared.ldap.message.SearchResponseReferenceImpl;
import org.apache.directory.shared.ldap.message.control.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.control.PersistentSearchControl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.directory.server.ldap.LdapService.NO_SIZE_LIMIT;
import static org.apache.directory.server.ldap.LdapService.NO_TIME_LIMIT;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;


/**
 * A handler for processing search requests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664302 $
 */
public class SearchHandler extends ReferralAwareRequestHandler<SearchRequest>
{
    private static final Logger LOG = LoggerFactory.getLogger( SearchHandler.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** cached to save redundant lookups into registries */ 
    private AttributeType objectClassAttributeType;
    
    
    /**
     * Constructs a new filter EqualityNode asserting that a candidate 
     * objectClass is a referral.
     *
     * @param session the {@link LdapSession} to construct the node for
     * @return the {@link EqualityNode} (objectClass=referral) non-normalized
     * @throws Exception in the highly unlikely event of schema related failures
     */
    private EqualityNode<String> newIsReferralEqualityNode( LdapSession session ) throws Exception
    {
        if ( objectClassAttributeType == null )
        {
            objectClassAttributeType = session.getCoreSession().getDirectoryService().getRegistries()
                .getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT );
        }
        
        EqualityNode<String> ocIsReferral = new EqualityNode<String>( SchemaConstants.OBJECT_CLASS_AT,
            new ServerStringValue( objectClassAttributeType, SchemaConstants.REFERRAL_OC ) );
        
        return ocIsReferral;
    }
    
    
    /**
     * Handles search requests containing the persistent search control but 
     * delegates to doSimpleSearch() if the changesOnly parameter of the 
     * control is set to false.
     *
     * @param session the LdapSession for which this search is conducted 
     * @param req the search request containing the persistent search control
     * @param psearchControl the persistent search control extracted
     * @throws Exception if failures are encountered while searching
     */
    private void handlePersistentSearch( LdapSession session, SearchRequest req, 
        PersistentSearchControl psearchControl ) throws Exception 
    {
        /*
         * We want the search to complete first before we start listening to 
         * events when the control does NOT specify changes ONLY mode.
         */
        if ( ! psearchControl.isChangesOnly() )
        {
            SearchResponseDone done = doSimpleSearch( session, req );
            
            // ok if normal search beforehand failed somehow quickly abandon psearch
            if ( done.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS )
            {
                session.getIoSession().write( done );
                return;
            }
        }

        if ( req.isAbandoned() )
        {
            return;
        }
        
        // now we process entries forever as they change
        PersistentSearchListener handler = new PersistentSearchListener( session, req );
        
        // compose notification criteria and add the listener to the event 
        // service using that notification criteria to determine which events 
        // are to be delivered to the persistent search issuing client
        NotificationCriteria criteria = new NotificationCriteria();
        criteria.setAliasDerefMode( req.getDerefAliases() );
        criteria.setBase( req.getBase() );
        criteria.setFilter( req.getFilter() );
        criteria.setScope( req.getScope() );
        criteria.setEventMask( EventType.getEventTypes( psearchControl.getChangeTypes() ) );
        getLdapServer().getDirectoryService().getEventService().addListener( handler, criteria );
        req.addAbandonListener( new SearchAbandonListener( ldapService, handler ) );
        return;
    }
    
    
    /**
     * Handles search requests on the RootDSE. 
     * 
     * @param session the LdapSession for which this search is conducted 
     * @param req the search request on the RootDSE
     * @throws Exception if failures are encountered while searching
     */
    private void handleRootDseSearch( LdapSession session, SearchRequest req ) throws Exception
    {
        EntryFilteringCursor cursor = null;
        
        try
        {
            cursor = session.getCoreSession().search( req );
            
            // Position the cursor at the beginning
            cursor.beforeFirst();
            boolean hasRootDSE = false;
            
            while ( cursor.next() )
            {
            	if ( hasRootDSE )
            	{
            		// This is an error ! We should never find more than one rootDSE !
            	    LOG.error( "Got back more than one entry for search on RootDSE which means " +
            	    		"Cursor is not functioning properly!" );
            	}
            	else
            	{
            		hasRootDSE = true;
	                ClonedServerEntry entry = cursor.get();
	                session.getIoSession().write( generateResponse( session, req, entry ) );
            	}
            }
    
            // write the SearchResultDone message
            session.getIoSession().write( req.getResultResponse() );
        }
        finally
        {
        	// Close the cursor now.
            if ( cursor != null )
            {
                try
                {
                    cursor.close();
                }
                catch ( NamingException e )
                {
                    LOG.error( "failed on list.close()", e );
                }
            }
        }
    }
    
    
    /**
     * Based on the server maximum time limits configured for search and the 
     * requested time limits this method determines if at all to replace the 
     * default ClosureMonitor of the result set Cursor with one that closes
     * the Cursor when either server mandated or request mandated time limits 
     * are reached.
     *
     * @param req the {@link SearchRequest} issued
     * @param session the {@link LdapSession} on which search was requested
     * @param cursor the {@link EntryFilteringCursor} over the search results
     */
    private void setTimeLimitsOnCursor( SearchRequest req, LdapSession session, final EntryFilteringCursor cursor )
    {
        // Don't bother setting time limits for administrators
        if ( session.getCoreSession().isAnAdministrator() && req.getTimeLimit() == NO_TIME_LIMIT )
        {
            return;
        }
        
        /*
         * Non administrator based searches are limited by time if the server 
         * has been configured with unlimited time and the request specifies 
         * unlimited search time
         */
        if ( ldapService.getMaxTimeLimit() == NO_TIME_LIMIT && req.getTimeLimit() == NO_TIME_LIMIT )
        {
            return;
        }
        
        /*
         * If the non-administrator user specifies unlimited time but the server 
         * is configured to limit the search time then we limit by the max time 
         * allowed by the configuration 
         */
        if ( req.getTimeLimit() == 0 )
        {
            cursor.setClosureMonitor( new SearchTimeLimitingMonitor( ldapService.getMaxTimeLimit(), TimeUnit.SECONDS ) );
            return;
        }
        
        /*
         * If the non-administrative user specifies a time limit equal to or 
         * less than the maximum limit configured in the server then we 
         * constrain search by the amount specified in the request
         */
        if ( ldapService.getMaxTimeLimit() >= req.getTimeLimit() )
        {
            cursor.setClosureMonitor( new SearchTimeLimitingMonitor( req.getTimeLimit(), TimeUnit.SECONDS ) );
            return;
        }

        /*
         * Here the non-administrative user's requested time limit is greater 
         * than what the server's configured maximum limit allows so we limit
         * the search to the configured limit
         */
        cursor.setClosureMonitor( new SearchTimeLimitingMonitor( ldapService.getMaxTimeLimit(), TimeUnit.SECONDS ) );
    }
    
    
    private int getSearchSizeLimits( SearchRequest req, LdapSession session )
    {
        LOG.debug( "req size limit = {}, configured size limit = {}", req.getSizeLimit(), 
            ldapService.getMaxSizeLimit() );
        
        // Don't bother setting size limits for administrators that don't ask for it
        if ( session.getCoreSession().isAnAdministrator() && req.getSizeLimit() == NO_SIZE_LIMIT )
        {
            return NO_SIZE_LIMIT;
        }
        
        // Don't bother setting size limits for administrators that don't ask for it
        if ( session.getCoreSession().isAnAdministrator() )
        {
            return req.getSizeLimit();
        }
        
        /*
         * Non administrator based searches are limited by size if the server 
         * has been configured with unlimited size and the request specifies 
         * unlimited search size
         */
        if ( ldapService.getMaxSizeLimit() == NO_SIZE_LIMIT && req.getSizeLimit() == NO_SIZE_LIMIT )
        {
            return NO_SIZE_LIMIT;
        }
        
        /*
         * If the non-administrator user specifies unlimited size but the server 
         * is configured to limit the search size then we limit by the max size
         * allowed by the configuration 
         */
        if ( req.getSizeLimit() == 0 )
        {
            return ldapService.getMaxSizeLimit();
        }
        
        if ( ldapService.getMaxSizeLimit() == NO_SIZE_LIMIT )
        {
            return req.getSizeLimit();
        }
        
        /*
         * If the non-administrative user specifies a size limit equal to or 
         * less than the maximum limit configured in the server then we 
         * constrain search by the amount specified in the request
         */
        if ( ldapService.getMaxSizeLimit() >= req.getSizeLimit() )
        {
            return req.getSizeLimit();
        }
        
        /*
         * Here the non-administrative user's requested size limit is greater 
         * than what the server's configured maximum limit allows so we limit
         * the search to the configured limit
         */
        return ldapService.getMaxSizeLimit();
    }
    
    
    /**
     * Conducts a simple search across the result set returning each entry 
     * back except for the search response done.  This is calculated but not
     * returned so the persistent search mechanism can leverage this method
     * along with standard search.
     *
     * @param session the LDAP session object for this request
     * @param req the search request 
     * @return the result done 
     * @throws Exception if there are failures while processing the request
     */
    private SearchResponseDone doSimpleSearch( LdapSession session, SearchRequest req ) 
        throws Exception
    {
        /*
         * Iterate through all search results building and sending back responses
         * for each search result returned.
         */
        EntryFilteringCursor cursor = null;
        
        try
        {
            LdapResult ldapResult = req.getResultResponse().getLdapResult();
            cursor = session.getCoreSession().search( req );
            req.addAbandonListener( new SearchAbandonListener( ldapService, cursor ) );
            setTimeLimitsOnCursor( req, session, cursor );
            final int sizeLimit = getSearchSizeLimits( req, session );
            LOG.debug( "using {} for size limit", sizeLimit );
            
            // Position the cursor at the beginning
            cursor.beforeFirst();

            if ( sizeLimit == NO_SIZE_LIMIT )
            {
                while ( cursor.next() )
                {
                    if ( session.getIoSession().isClosing() )
                    {
                        break;
                    }
                    ClonedServerEntry entry = cursor.get();
                    Response response = generateResponse( session, req, entry );
                    session.getIoSession().write( response );
                }
            }
            else
            {
                int count = 0;
                while ( cursor.next() )
                {
                    if ( session.getIoSession().isClosing() )
                    {
                        break;
                    }
                    if ( count < sizeLimit )
                    {
                        ClonedServerEntry entry = cursor.get();
                        session.getIoSession().write( generateResponse( session, req, entry ) );
                        count++;
                    }
                    else
                    {
                        // DO NOT WRITE THE RESPONSE - JUST RETURN IT
                        ldapResult.setResultCode( ResultCodeEnum.SIZE_LIMIT_EXCEEDED );
                        return ( SearchResponseDone ) req.getResultResponse();
                    }  
                }
            }
    
            // DO NOT WRITE THE RESPONSE - JUST RETURN IT
            ldapResult.setResultCode( ResultCodeEnum.SUCCESS );
            return ( SearchResponseDone ) req.getResultResponse();
        }
        finally
        {
            if ( cursor != null )
            {
                try
                {
                    cursor.close();
                }
                catch ( NamingException e )
                {
                    LOG.error( "failed on list.close()", e );
                }
            }
        }
    }
    

    /**
     * Generates a response for an entry retrieved from the server core based 
     * on the nature of the request with respect to referral handling.  This 
     * method will either generate a SearchResponseEntry or a 
     * SearchResponseReference depending on if the entry is a referral or if 
     * the ManageDSAITControl has been enabled.
     *
     * @param req the search request
     * @param entry the entry to be handled
     * @return the response for the entry
     * @throws Exception if there are problems in generating the response
     */
    private Response generateResponse( LdapSession session, SearchRequest req, ClonedServerEntry entry ) throws Exception
    {
        EntryAttribute ref = entry.getOriginalEntry().get( SchemaConstants.REF_AT );
        boolean hasManageDsaItControl = req.getControls().containsKey( ManageDsaITControl.CONTROL_OID );

        if ( ( ref != null ) && ! hasManageDsaItControl )
        {
            // The entry is a referral.
            SearchResponseReference respRef;
            respRef = new SearchResponseReferenceImpl( req.getMessageId() );
            respRef.setReferral( new ReferralImpl() );
            
            for ( Value<?> val : ref )
            {
                String url = ( String ) val.get();
                
                if ( ! url.startsWith( "ldap" ) )
                {
                    respRef.getReferral().addLdapUrl( url );
                }
                
                LdapURL ldapUrl = new LdapURL();
                ldapUrl.setForceScopeRendering( true );
                try
                {
                    ldapUrl.parse( url.toCharArray() );
                }
                catch ( LdapURLEncodingException e )
                {
                    LOG.error( "Bad URL ({}) for ref in {}.  Reference will be ignored.", url, entry );
                }

                switch( req.getScope() )
                {
                    case SUBTREE:
                        ldapUrl.setScope( SearchScope.SUBTREE.getJndiScope() );
                        break;
                        
                    case ONELEVEL: // one level here is object level on remote server
                        ldapUrl.setScope( SearchScope.OBJECT.getJndiScope() );
                        break;
                        
                    default:
                        throw new IllegalStateException( "Unexpected base scope." );
                }
                
                respRef.getReferral().addLdapUrl( ldapUrl.toString() );
            }
            
            return respRef;
        }
        else 
        {
            // The entry is not a referral, or the ManageDsaIt control is set
            SearchResponseEntry respEntry;
            respEntry = new SearchResponseEntryImpl( req.getMessageId() );
            respEntry.setEntry( entry );
            respEntry.setObjectName( entry.getDn() );
            
            // Filter the userPassword if the server mandate to do so
            if ( session.getCoreSession().getDirectoryService().isPasswordHidden() )
            {
                // Remove the userPassord attribute from the entry.
                respEntry.getEntry().removeAttributes( SchemaConstants.USER_PASSWORD_AT );
            }
            
            return respEntry;
        }
    }
    
    
    /**
     * Alters the filter expression based on the presence of the 
     * ManageDsaIT control.  If the control is not present, the search
     * filter will be altered to become a disjunction with two terms.
     * The first term is the original filter.  The second term is a
     * (objectClass=referral) assertion.  When OR'd together these will
     * make sure we get all referrals so we can process continuations 
     * properly without having the filter remove them from the result 
     * set.
     * 
     * NOTE: original filter is first since most entries are not referrals 
     * so it has a higher probability on average of accepting and shorting 
     * evaluation before having to waste cycles trying to evaluate if the 
     * entry is a referral.
     *
     * @param session the session to use to construct the filter (schema access)
     * @param req the request to get the original filter from
     * @throws Exception if there are schema access problems
     */
    public void modifyFilter( LdapSession session, SearchRequest req ) throws Exception
    {
        if ( req.hasControl( ManageDsaITControl.CONTROL_OID ) )
        {
            return;
        }
        
        /*
         * Do not add the OR'd (objectClass=referral) expression if the user 
         * searches for the subSchemaSubEntry as the SchemaIntercepter can't 
         * handle an OR'd filter.
         */
        if ( isSubSchemaSubEntrySearch( session, req ) )
        {
            return;
        }
        
        /*
         * Most of the time the search filter is just (objectClass=*) and if 
         * this is the case then there's no reason at all to OR this with an
         * (objectClass=referral).  If we detect this case then we leave it 
         * as is to represent the OR condition:
         * 
         *  (| (objectClass=referral)(objectClass=*)) == (objectClass=*)
         */
        if ( req.getFilter() instanceof PresenceNode )
        {
            PresenceNode presenceNode = ( PresenceNode ) req.getFilter();
            
            AttributeType at = session.getCoreSession().getDirectoryService()
                .getRegistries().getAttributeTypeRegistry().lookup( presenceNode.getAttribute() );
            if ( at.getOid().equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
            {
                return;
            }
        }

        // using varags to add two expressions to an OR node 
        req.setFilter( new OrNode( req.getFilter(), newIsReferralEqualityNode( session ) ) );
    }
    
    
    /**
     * Main message handing method for search requests.  This will be called 
     * even if the ManageDsaIT control is present because the super class does
     * not know that the search operation has more to do after finding the 
     * base.  The call to this means that finding the base can ignore 
     * referrals.
     * 
     * @param session the associated session
     * @param req the received SearchRequest
     */
    public void handleIgnoringReferrals( LdapSession session, SearchRequest req )
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Message received:  {}", req.toString() );
        }

        // A flag set if we have a persistent search
        boolean isPersistentSearch = false;
        
        // A flag set when we've got an exception while processing a
        // persistent search
        boolean persistentSearchException = false;
        
        // add the search request to the registry of outstanding requests for this session
        session.registerOutstandingRequest( req );

        try
        {
            // modify the filter to affect continuation support
            modifyFilter( session, req );
            
            // ===============================================================
            // Handle search in rootDSE differently.
            // ===============================================================
            if ( isRootDSESearch( req ) )
            {
                handleRootDseSearch( session, req );
                
                return;
            }

            // ===============================================================
            // Handle psearch differently
            // ===============================================================

            PersistentSearchControl psearchControl = ( PersistentSearchControl ) 
                req.getControls().get( PersistentSearchControl.CONTROL_OID );
            
            if ( psearchControl != null )
            {
                // Set the flag to avoid the request being removed
                // from the session
                isPersistentSearch = true;

                handlePersistentSearch( session, req, psearchControl );
                
                return;
            }

            // ===============================================================
            // Handle regular search requests from here down
            // ===============================================================

            SearchResponseDone done = doSimpleSearch( session, req );
            session.getIoSession().write( done );
        }
        catch ( Exception e )
        {
            /*
             * From RFC 2251 Section 4.11:
             *
             * In the event that a server receives an Abandon Request on a Search
             * operation in the midst of transmitting responses to the Search, that
             * server MUST cease transmitting entry responses to the abandoned
             * request immediately, and MUST NOT send the SearchResultDone. Of
             * course, the server MUST ensure that only properly encoded LDAPMessage
             * PDUs are transmitted.
             *
             * SO DON'T SEND BACK ANYTHING!!!!!
             */
            if ( e instanceof OperationAbandonedException )
            {
                return;
            }

            // If it was a persistent search and if we had an exception,
            // we set the flag to remove the request from the session
            if ( isPersistentSearch )
            {
                persistentSearchException = true;
            }
            
            handleException( session, req, e );
        }
        finally 
        {
            
            // remove the request from the session, except if
            // we didn't got an exception for a Persistent search 
            if ( !isPersistentSearch || persistentSearchException )
            {
                session.unregisterOutstandingRequest( req );
            }
        }
    }


    /**
     * Handles processing with referrals without ManageDsaIT control.
     */
    public void handleWithReferrals( LdapSession session, LdapDN reqTargetDn, SearchRequest req ) throws NamingException
    {
        LdapResult result = req.getResultResponse().getLdapResult();
        ClonedServerEntry entry = null;
        boolean isReferral = false;
        boolean isparentReferral = false;
        ReferralManager referralManager = session.getCoreSession().getDirectoryService().getReferralManager();
        
        reqTargetDn.normalize( session.getCoreSession().getDirectoryService().getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
        
        // Check if the entry itself is a referral
        referralManager.lockRead();
        
        isReferral = referralManager.isReferral( reqTargetDn );
        
        if ( !isReferral )
        {
            // Check if the entry has a parent which is a referral
            isparentReferral = referralManager.hasParentReferral( reqTargetDn );
        }
        
        referralManager.unlock();
        
        if ( !isReferral && !isparentReferral )
        {
            // This is not a referral and it does not have a parent which 
            // is a referral : standard case, just deal with the request
            LOG.debug( "Entry {} is NOT a referral.", reqTargetDn );
            handleIgnoringReferrals( session, req );
            return;
        }
        else
        {
            // -------------------------------------------------------------------
            // Lookup Entry
            // -------------------------------------------------------------------
            
            // try to lookup the entry but ignore exceptions when it does not   
            // exist since entry may not exist but may have an ancestor that is a 
            // referral - would rather attempt a lookup that fails then do check 
            // for existence than have to do another lookup to get entry info
            try
            {
                entry = session.getCoreSession().lookup( reqTargetDn );
                LOG.debug( "Entry for {} was found: ", reqTargetDn, entry );
            }
            catch ( NameNotFoundException e )
            {
                /* ignore */
                LOG.debug( "Entry for {} not found.", reqTargetDn );
            }
            catch ( Exception e )
            {
                /* serious and needs handling */
                handleException( session, req, e );
                return;
            }
            
            // -------------------------------------------------------------------
            // Handle Existing Entry
            // -------------------------------------------------------------------
            
            if ( entry != null )
            {
                try
                {
                    LOG.debug( "Entry is a referral: {}", entry );
                    
                    handleReferralEntryForSearch( session, ( SearchRequest ) req, entry );

                    return;
                }
                catch ( Exception e )
                {
                    handleException( session, req, e );
                }
            }
    
            // -------------------------------------------------------------------
            // Handle Non-existing Entry
            // -------------------------------------------------------------------
            
            // if the entry is null we still have to check for a referral ancestor
            // also the referrals need to be adjusted based on the ancestor's ref
            // values to yield the correct path to the entry in the target DSAs
            
            else
            {
                // The entry is null : it has a parent referral.
                ClonedServerEntry referralAncestor = null;
    
                try
                {
                    referralAncestor = getFarthestReferralAncestor( session, reqTargetDn );
                }
                catch ( Exception e )
                {
                    handleException( session, req, e );
                    return;
                }
    
                if ( referralAncestor == null )
                {
                    result.setErrorMessage( "Entry not found." );
                    result.setResultCode( ResultCodeEnum.NO_SUCH_OBJECT );
                    session.getIoSession().write( req.getResultResponse() );
                    return;
                }
                  
                // if we get here then we have a valid referral ancestor
                try
                {
                    Referral referral = getReferralOnAncestorForSearch( session, ( SearchRequest ) req, referralAncestor );
                    
                    result.setResultCode( ResultCodeEnum.REFERRAL );
                    result.setReferral( referral );
                    session.getIoSession().write( req.getResultResponse() );
                }
                catch ( Exception e )
                {
                    handleException( session, req, e );
                }
            }
        }
    }
    
    
    /**
     * Handles processing a referral response on a target entry which is a 
     * referral.  It will for any request that returns an LdapResult in it's 
     * response.
     *
     * @param session the session to use for processing
     * @param reqTargetDn the dn of the target entry of the request
     * @param req the request
     * @param entry the entry associated with the request
     */
    private void handleReferralEntryForSearch( LdapSession session, SearchRequest req, ClonedServerEntry entry )
        throws Exception
    {
        LdapResult result = req.getResultResponse().getLdapResult();
        ReferralImpl referral = new ReferralImpl();
        result.setReferral( referral );
        result.setResultCode( ResultCodeEnum.REFERRAL );
        result.setErrorMessage( "Encountered referral attempting to handle request." );
        result.setMatchedDn( req.getBase() );

        EntryAttribute refAttr = entry.getOriginalEntry().get( SchemaConstants.REF_AT );
        
        for ( Value<?> refval : refAttr )
        {
            String refstr = ( String ) refval.get();
            
            // need to add non-ldap URLs as-is
            if ( ! refstr.startsWith( "ldap" ) )
            {
                referral.addLdapUrl( refstr );
                continue;
            }
            
            // parse the ref value and normalize the DN  
            LdapURL ldapUrl = new LdapURL();
            try
            {
                ldapUrl.parse( refstr.toCharArray() );
            }
            catch ( LdapURLEncodingException e )
            {
                LOG.error( "Bad URL ({}) for ref in {}.  Reference will be ignored.", refstr, entry );
                continue;
            }
            
            ldapUrl.setForceScopeRendering( true );
            ldapUrl.setAttributes( req.getAttributes() );
            ldapUrl.setScope( req.getScope().getJndiScope() );
            referral.addLdapUrl( ldapUrl.toString() );
        }

        session.getIoSession().write( req.getResultResponse() );
    }
    
    
    /**
     * Determines if a search request is on the RootDSE of the server.
     * 
     * It is a RootDSE search if :
     * - the base DN is empty
     * - and the scope is BASE OBJECT
     * - and the filter is (ObjectClass = *)
     * 
     * (RFC 4511, 5.1, par. 1 & 2)
     *
     * @param req the request issued
     * @return true if the search is on the RootDSE false otherwise
     */
    private static boolean isRootDSESearch( SearchRequest req )
    {
        boolean isBaseIsRoot = req.getBase().isEmpty();
        boolean isBaseScope = req.getScope() == SearchScope.OBJECT;
        boolean isRootDSEFilter = false;
        
        if ( req.getFilter() instanceof PresenceNode )
        {
            String attribute = ( ( PresenceNode ) req.getFilter() ).getAttribute();
            isRootDSEFilter = attribute.equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT ) ||
                                attribute.equals( SchemaConstants.OBJECT_CLASS_AT_OID );
        }
        
        return isBaseIsRoot && isBaseScope && isRootDSEFilter;
    }
    
    
    /**
     * <p>
     * Determines if a search request is a subSchemaSubEntry search.
     * </p>
     * <p>
     * It is a schema search if:
     * - the base DN is the DN of the subSchemaSubEntry of the root DSE
     * - and the scope is BASE OBJECT
     * - and the filter is (objectClass=subschema)
     * (RFC 4512, 4.4,)
     * </p>
     * <p>
     * However in this method we only check the first condition to avoid
     * performance issues.
     * </p>
     * 
     * @param session the LDAP session
     * @param req the request issued
     * 
     * @return true if the search is on the subSchemaSubEntry, false otherwise
     * 
     * @throws Exception the exception
     */
    private static boolean isSubSchemaSubEntrySearch( LdapSession session, SearchRequest req ) throws Exception
    {
        LdapDN base = req.getBase();
        String baseNormForm = ( base.isNormalized() ? base.getNormName() : base.toNormName() );

        DirectoryService ds = session.getCoreSession().getDirectoryService();
        PartitionNexus nexus = ds.getPartitionNexus();
        Value<?> subschemaSubentry = nexus.getRootDSE( null ).get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ).get();
        LdapDN subschemaSubentryDn = new LdapDN( ( String ) ( subschemaSubentry.get() ) );
        subschemaSubentryDn.normalize( ds.getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
        String subschemaSubentryDnNorm = subschemaSubentryDn.getNormName();
        
        return subschemaSubentryDnNorm.equals( baseNormForm );
    }
}