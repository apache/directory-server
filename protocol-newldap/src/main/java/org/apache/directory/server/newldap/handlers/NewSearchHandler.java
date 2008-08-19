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
package org.apache.directory.server.newldap.handlers;


import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.core.entry.ServerStringValue;
import org.apache.directory.server.core.event.NotificationCriteria;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.newldap.LdapSession;
import org.apache.directory.shared.ldap.codec.util.LdapURL;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.OperationAbandonedException;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.message.AbandonListener;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.PersistentSearchControl;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.Response;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.message.SearchResponseDone;
import org.apache.directory.shared.ldap.message.SearchResponseEntry;
import org.apache.directory.shared.ldap.message.SearchResponseEntryImpl;
import org.apache.directory.shared.ldap.message.SearchResponseReference;
import org.apache.directory.shared.ldap.message.SearchResponseReferenceImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;


/**
 * A handler for processing search requests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664302 $
 */
public class NewSearchHandler extends ReferralAwareRequestHandler<SearchRequest>
{
    private static final Logger LOG = LoggerFactory.getLogger( NewSearchHandler.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();
    
    AttributeType objectClassAttributeType;
    
    private EqualityNode<String> getOcIsReferralAssertion( LdapSession session ) throws Exception
    {
        if ( objectClassAttributeType == null )
        {
            objectClassAttributeType = session.getCoreSession().getDirectoryService().getRegistries()
                .getAttributeTypeRegistry().lookup( SchemaConstants.OBJECT_CLASS_AT );
        }
        
        EqualityNode<String> ocIsReferral = new EqualityNode<String>( 
            SchemaConstants.OBJECT_CLASS_AT,
            new ServerStringValue( objectClassAttributeType, SchemaConstants.REFERRAL_OC ) );
        
        return ocIsReferral;
    }
    
    
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

        // now we process entries for ever as they change
        PersistentSearchListener handler = new PersistentSearchListener( session, req );
        
        // TODO what about notification criteria ?????????????????? TODO add it 
        NotificationCriteria criteria = new NotificationCriteria();
        criteria.setAliasDerefMode( req.getDerefAliases() );
        criteria.setBase( req.getBase() );
        getLdapServer().getDirectoryService().getEventService().addListener( handler );
        return;
    }
    
    
    /**
     * Deal with RootDE search. 
     * @param session
     * @param req
     * @throws Exception
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
            		// This is an error ! We should never find more
            		// than one rootDSE !
            		// TODO : handle this error
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
    private SearchResponseDone doSimpleSearch( LdapSession session, SearchRequest req ) throws Exception
    {
        /*
         * Iterate through all search results building and sending back responses
         * for each search result returned.
         */
        EntryFilteringCursor cursor = null;
        
        try
        {
            cursor = session.getCoreSession().search( req );
            
            // TODO - fix this (need to make Cursors abandonable)
            if ( cursor instanceof AbandonListener )
            {
                req.addAbandonListener( ( AbandonListener ) cursor );
            }
    
            // Position the cursor at the beginning
            cursor.beforeFirst();
            
            while ( cursor.next() )
            {
                ClonedServerEntry entry = cursor.get();
                session.getIoSession().write( generateResponse( session, req, entry ) );
            }
    
            LdapResult ldapResult = req.getResultResponse().getLdapResult();
            ldapResult.setResultCode( ResultCodeEnum.SUCCESS );
            
            // DO NOT WRITE THE RESPONSE - JUST RETURN IT
            
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

        if ( ref != null && ! hasManageDsaItControl )
        {
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
            SearchResponseEntry respEntry;
            respEntry = new SearchResponseEntryImpl( req.getMessageId() );
            respEntry.setAttributes( ServerEntryUtils.toAttributesImpl( entry ) );
            respEntry.setObjectName( entry.getDn() );
            
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
        req.setFilter( new OrNode( req.getFilter(), getOcIsReferralAssertion( session ) ) );
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
    public void handleIgnoringReferrals( LdapSession session, LdapDN reqTargetDn, 
        ClonedServerEntry entry, SearchRequest req )
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Message received:  {}", req.toString() );
        }

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

            handleException( session, req, e );
        }
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
}