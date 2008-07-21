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


import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.ReferralHandlingMode;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.newldap.LdapServer;
import org.apache.directory.server.newldap.LdapSession;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.constants.JndiPropertyConstants;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.OperationAbandonedException;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.message.AbandonListener;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.PersistentSearchControl;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.Response;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.ResultResponse;
import org.apache.directory.shared.ldap.message.ScopeEnum;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.message.SearchResponseDone;
import org.apache.directory.shared.ldap.message.SearchResponseEntry;
import org.apache.directory.shared.ldap.message.SearchResponseEntryImpl;
import org.apache.directory.shared.ldap.message.SearchResponseReference;
import org.apache.directory.shared.ldap.message.SearchResponseReferenceImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.ArrayUtils;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ReferralException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;


/**
 * A handler for processing search requests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664302 $
 */
public class NewSearchHandler extends LdapRequestHandler<SearchRequest>
{
    private static final Logger LOG = LoggerFactory.getLogger( NewSearchHandler.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();
    
    
    private void handlePersistentSearch( LdapSession session, SearchRequest req, 
        PersistentSearchControl psearchControl ) throws Exception 
    {
        /*
         * We want the search to complete first before we start listening to 
         * events when the control does NOT specify changes ONLY mode.
         */
        if ( ! psearchControl.isChangesOnly() )
        {
            EntryFilteringCursor cursor = session.getCoreSession().search( req );
            
            if ( cursor instanceof AbandonListener )
            {
                req.addAbandonListener( ( AbandonListener ) cursor );
            }
            
            cursor.beforeFirst();
            
            if ( cursor.next() )
            {
                Iterator<Response> it = new SearchResponseIterator( req, cursor, session );
                
                while ( it.hasNext() )
                {
                    Response resp = it.next();
                    
                    if ( resp instanceof SearchResponseDone )
                    {
                        // ok if normal search beforehand failed somehow quickly abandon psearch
                        ResultCodeEnum rcode = ( ( SearchResponseDone ) resp ).getLdapResult().getResultCode();

                        if ( rcode != ResultCodeEnum.SUCCESS )
                        {
                            session.getIoSession().write( resp );
                            return;
                        }
                        // if search was fine then we returned all entries so now
                        // instead of returning the DONE response we break from the
                        // loop and user the notification listener to send back
                        // notifications to the client in never ending search
                        else
                        {
                            break;
                        }
                    }
                    else
                    {
                        session.getIoSession().write( resp );
                    }
                }
            }
        }

        // now we process entries for ever as they change
        PersistentSearchListener handler = new PersistentSearchListener( session, req );
        getLdapServer().getDirectoryService().addNamingListener( req.getBase(), req.getFilter().toString(), handler );
        return;
    }

    
    /**
     * Main message handing method for search requests.
     * 
     * @param session the associated session
     * @param req the received SearchRequest
     */
    public void handle( LdapSession session, SearchRequest req ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Message received:  {}", req.toString() );
        }

        EntryFilteringCursor cursor = null;
        String[] ids = null;
        Collection<String> retAttrs = new HashSet<String>();
        
        // add the search request to the registry of outstanding requests for this session
        session.registerOutstandingRequest( req );

        try
        {
            boolean isRootDSESearch = isRootDSESearch( req );

            // ===============================================================
            // Handle search in rootDSE differently.
            // TODO : is this necessary ?
            // ===============================================================
            if ( isRootDSESearch )
            {
            }
            else
            {
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

            /*
             * Iterate through all search results building and sending back responses
             * for each search result returned.
             */
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
                Response response;
            	ClonedServerEntry result = cursor.get();
            	EntryAttribute ref = result.getOriginalEntry().get( SchemaConstants.REF_AT );
            	boolean hasManageDsaItControl = req.getControls().containsKey( ManageDsaITControl.CONTROL_OID );

            	if ( ref != null && ! hasManageDsaItControl )
            	{
                    SearchResponseReference respRef;
                    respRef = new SearchResponseReferenceImpl( req.getMessageId() );
                    respRef.setReferral( new ReferralImpl() );
                    
                    for ( Value<?> val : ref )
                    {
                        String url = ( String ) val.get();
                        respRef.getReferral().addLdapUrl( url );
                    }
                    
                    response = respRef;
            	}
            	else 
            	{
                    SearchResponseEntry respEntry;
                    respEntry = new SearchResponseEntryImpl( req.getMessageId() );
                    respEntry.setAttributes( ServerEntryUtils.toAttributesImpl( result ) );
                    respEntry.setObjectName( result.getDn() );
                    
                    response = respEntry;
            	}
                
                session.getIoSession().write( response );
            }

            // At the end, we have to write a last message : a responseDone
            SearchResponseDone respDone = ( SearchResponseDone ) req.getResultResponse();
            respDone.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );
            session.getIoSession().write( respDone );
        }
        catch ( ReferralException e )
        {
            LdapResult result = req.getResultResponse().getLdapResult();
            ReferralImpl refs = new ReferralImpl();
            result.setReferral( refs );
            result.setResultCode( ResultCodeEnum.REFERRAL );
            result.setErrorMessage( "Encountered referral attempting to handle add request." );

            do
            {
                refs.addLdapUrl( ( String ) e.getReferralInfo() );
            }
            while ( e.skipReferral() );
            
            session.getIoSession().write( req.getResultResponse() );
            session.unregisterOutstandingRequest( req );
        }
        catch ( NamingException e )
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

            String msg = "failed on search operation: " + e.getMessage();
            
            if ( LOG.isDebugEnabled() )
            {
                msg += ":\n" + req + ":\n" + ExceptionUtils.getStackTrace( e );
            }

            ResultCodeEnum code;
            
            if ( e instanceof LdapException )
            {
                code = ( ( LdapException ) e ).getResultCode();
            }
            else
            {
                code = ResultCodeEnum.getBestEstimate( e, req.getType() );
            }

            LdapResult result = req.getResultResponse().getLdapResult();
            result.setResultCode( code );
            result.setErrorMessage( msg );

            if ( ( e.getResolvedName() != null )
                && ( ( code == ResultCodeEnum.NO_SUCH_OBJECT ) || ( code == ResultCodeEnum.ALIAS_PROBLEM )
                    || ( code == ResultCodeEnum.INVALID_DN_SYNTAX ) || ( code == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM ) ) )
            {
                result.setMatchedDn( (LdapDN)e.getResolvedName() );
            }

            session.getIoSession().write( req.getResultResponse() );
            session.unregisterOutstandingRequest( req );
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
     * Based on the request and the max limits for time configured in the 
     * server, this returns the minimum allowed time limit.
     *
     * @param session the session
     * @param req the search request
     * @return the minimum allowed time limit
     */
    private int getTimeLimit( LdapSession session, SearchRequest req )
    {
        if ( session.getCoreSession().isAnAdministrator() )
        {
            // The setTimeLimit needs a number of milliseconds
            // when the search control is expressed in seconds
            int timeLimit = req.getTimeLimit();

            // Just check that we are not exceeding the maximum for a long
            if ( timeLimit > Integer.MAX_VALUE / 1000 )
            {
                timeLimit = 0;
            }
            
            return timeLimit * 1000;
        }
        
        return Math.min( req.getTimeLimit(), getLdapServer().getMaxTimeLimit() );
    }
    

    /**
     * Based on the request and the max limits for size configured in the 
     * server, this returns the minimum allowed size limit.
     *
     * @param session the session
     * @param req the search request
     * @return the minimum allowed size limit
     */
    private int getSizeLimit( LdapSession session, SearchRequest req )
    {
        if ( session.getCoreSession().isAnAdministrator() )
        {
            return req.getSizeLimit();
        }        

        return Math.min( req.getSizeLimit(), getLdapServer().getMaxSizeLimit() );
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
        boolean isBaseScope = req.getScope() == ScopeEnum.BASE_OBJECT;
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