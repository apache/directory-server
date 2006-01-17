/*
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.protocol.support;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ReferralException;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.codec.util.LdapResultEnum;
import org.apache.ldap.common.exception.LdapException;
import org.apache.ldap.common.exception.OperationAbandonedException;
import org.apache.ldap.common.filter.PresenceNode;
import org.apache.ldap.common.message.AbandonListener;
import org.apache.ldap.common.message.LdapResult;
import org.apache.ldap.common.message.ManageDsaITControl;
import org.apache.ldap.common.message.PersistentSearchControl;
import org.apache.ldap.common.message.ReferralImpl;
import org.apache.ldap.common.message.Response;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.message.ScopeEnum;
import org.apache.ldap.common.message.SearchRequest;
import org.apache.ldap.common.message.SearchResponseDone;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.util.ArrayUtils;
import org.apache.ldap.common.util.ExceptionUtils;
import org.apache.ldap.server.configuration.Configuration;
import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.jndi.ServerLdapContext;
import org.apache.ldap.server.protocol.SessionRegistry;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A handler for processing search requests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SearchHandler implements MessageHandler
{
    private static final Logger log = LoggerFactory.getLogger( SearchHandler.class );
    private static final String DEREFALIASES_KEY = "java.naming.ldap.derefAliases";

    
    /**
     * Builds the JNDI search controls for a SearchRequest.
     *  
     * @param req the search request.
     * @param ids the ids to return
     * @return the SearchControls to use with the ApacheDS server side JNDI provider
     */
    private static SearchControls getSearchControls( SearchRequest req, String[] ids )
    {
        // prepare all the search controls
        SearchControls controls = new SearchControls();
        controls.setCountLimit( req.getSizeLimit() );
        controls.setTimeLimit( req.getTimeLimit() );
        controls.setSearchScope( req.getScope().getValue() );
        controls.setReturningObjFlag( req.getTypesOnly() );
        controls.setReturningAttributes( ids );
        controls.setDerefLinkFlag( true );
        return controls;
    }

    
    /**
     * Determines if a search request is on the RootDSE of the server.
     * 
     * @param req the request issued
     * @return true if the search is on the RootDSE false otherwise
     */
    private static boolean isRootDSESearch( SearchRequest req )
    {
        boolean isBaseIsRoot = req.getBase().trim().equals( "" );
        boolean isBaseScope = req.getScope() == ScopeEnum.BASEOBJECT;
        boolean isRootDSEFilter = false;
        if ( req.getFilter() instanceof PresenceNode )
        {
            isRootDSEFilter = ( ( PresenceNode ) req.getFilter() ).getAttribute().equalsIgnoreCase( "objectClass" );
        }
        return isBaseIsRoot && isBaseScope && isRootDSEFilter;
    }
    

    /**
     * Main message handing method for search requests.
     */
    public void messageReceived( IoSession session, Object request )
    {
        ServerLdapContext ctx;
        SearchRequest req = ( SearchRequest ) request;
        NamingEnumeration list = null;
        String[] ids = null;
        Collection retAttrs = new HashSet();
        retAttrs.addAll( req.getAttributes() );
        
        // add the search request to the registry of outstanding requests for this session
        SessionRegistry.getSingleton().addOutstandingRequest( session, req );

        // check the attributes to see if a referral's ref attribute is included
        if( retAttrs.size() > 0 && !retAttrs.contains( "ref" ) )
        {
            retAttrs.add( "ref" );
            ids = ( String[] ) retAttrs.toArray( ArrayUtils.EMPTY_STRING_ARRAY );
        }
        else if( retAttrs.size() > 0 )
        {
            ids = ( String[] ) retAttrs.toArray( ArrayUtils.EMPTY_STRING_ARRAY );
        }
        SearchControls controls = getSearchControls( req, ids );

        try
        {
            // ===============================================================
            // Find session context
            // ===============================================================

            boolean isRootDSESearch = isRootDSESearch( req );
            // bypass checks to disallow anonymous binds for search on RootDSE with base obj scope
            if ( isRootDSESearch )
            {
                LdapContext unknown = SessionRegistry.getSingleton().getLdapContextOnRootDSEAccess( session, null );
                if ( ! ( unknown instanceof ServerLdapContext ) )
                {
                    ctx = ( ServerLdapContext ) unknown.lookup( "" );
                }
                else
                {
                    ctx = ( ServerLdapContext ) unknown;
                }
            }
            // all those search operations are subject to anonymous bind checks when anonymous binda are disallowed
            else
            {
                LdapContext unknown = SessionRegistry.getSingleton().getLdapContext( session, null, true );
                if ( ! ( unknown instanceof ServerLdapContext ) )
                {
                    ctx = ( ServerLdapContext ) unknown.lookup( "" );
                }
                else
                {
                    ctx = ( ServerLdapContext ) unknown;
                }
            }
            ctx.addToEnvironment( DEREFALIASES_KEY, req.getDerefAliases().getName() );
            if ( req.getControls().containsKey( ManageDsaITControl.CONTROL_OID ) )
            {
                ctx.addToEnvironment( Context.REFERRAL, "ignore" );
            }
            else
            {
                ctx.addToEnvironment( Context.REFERRAL, "throw" );
            }

            // ===============================================================
            // Handle annonymous binds
            // ===============================================================

            StartupConfiguration cfg = ( StartupConfiguration ) Configuration.toConfiguration( ctx.getEnvironment() );
            boolean allowAnonymousBinds = cfg.isAllowAnonymousAccess();
            boolean isAnonymousUser = ( ( ServerLdapContext ) ctx ).getPrincipal().getName().trim().equals( "" );

            if ( isAnonymousUser && ! allowAnonymousBinds && ! isRootDSESearch )
            {
                LdapResult result = req.getResultResponse().getLdapResult();
                result.setResultCode( ResultCodeEnum.INSUFFICIENTACCESSRIGHTS );
                String msg = "Bind failure: Anonymous binds have been disabled!";
                result.setErrorMessage( msg );
                session.write( req.getResultResponse() );
                return;
            }

            // ===============================================================
            // Handle psearch differently
            // ===============================================================

            PersistentSearchControl psearchControl = ( PersistentSearchControl ) 
                req.getControls().get( PersistentSearchControl.CONTROL_OID );
            if ( psearchControl != null )
            {
                // there are no limits for psearch processing
                controls.setCountLimit( 0 );
                controls.setTimeLimit( 0 );
                
                if ( ! psearchControl.isChangesOnly() )
                {
                    list = ( ( ServerLdapContext ) ctx ).search( new LdapName( req.getBase() ), req.getFilter(), controls );
                    if ( list instanceof AbandonListener )
                    {
                        req.addAbandonListener( ( AbandonListener ) list );
                    }
                    if( list.hasMore() )
                    {
                        Iterator it = new SearchResponseIterator( req, list );
                        while( it.hasNext() )
                        {
                            Response resp = ( Response ) it.next();
                            if ( resp instanceof SearchResponseDone )
                            {
                                // ok if normal search beforehand failed somehow quickly abandon psearch
                                ResultCodeEnum rcode = ( ( SearchResponseDone ) resp ).getLdapResult().getResultCode();
                                if ( rcode.getValue() != LdapResultEnum.SUCCESS )
                                {
                                    session.write( resp );
                                    return;
                                }
                                // if search was fine then we returned all entries so now
                                // instead of returning the DONE response we break from the
                                // loop and user the notification listener to send back 
                                // notificationss to the client in never ending search
                                else break;
                            }
                            else
                            {
                                session.write( resp );
                            }
                        }
                    }
                }
                
                // now we process entries for ever as they change 
                PersistentSearchListener handler = new PersistentSearchListener( ctx, session, req );
                StringBuffer buf = new StringBuffer();
                req.getFilter().printToBuffer( buf );
                ctx.addNamingListener( req.getBase(), buf.toString(), controls, handler );
                return;
            }
            
            // ===============================================================
            // Handle regular search requests from here down
            // ===============================================================

            /*
             * Iterate through all search results building and sending back responses 
             * for each search result returned.  
             */
            list = ( ( ServerLdapContext ) ctx ).search( new LdapName( req.getBase() ), req.getFilter(), controls );
            if ( list instanceof AbandonListener )
            {
                req.addAbandonListener( ( AbandonListener ) list );
            }
            
            if( list.hasMore() )
            {
                Iterator it = new SearchResponseIterator( req, list );
                while( it.hasNext() )
                {
                    session.write( it.next() );
                }

                return;
            }
            else
            {
                list.close();
                req.getResultResponse().getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );
                Iterator it = Collections.singleton( req.getResultResponse() ).iterator();
                while( it.hasNext() )
                {
                    session.write( it.next() );
                }
                return;
            }
        }
        catch( ReferralException e )
        {
            LdapResult result = req.getResultResponse().getLdapResult();
            ReferralImpl refs = new ReferralImpl();
            result.setReferral( refs );
            result.setResultCode( ResultCodeEnum.REFERRAL );
            result.setErrorMessage( "Encountered referral attempting to handle add request." );
            /* coming up null causing a NPE */
            // result.setMatchedDn( e.getResolvedName().toString() );
            do
            {
                refs.addLdapUrl( ( String ) e.getReferralInfo() );
            }
            while( e.skipReferral() );
            session.write( req.getResultResponse() );
            return;
        }
        catch( NamingException e )
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
            
            String msg = "failed on search operation";
            if ( log.isDebugEnabled() )
            {
                msg += ":\n" + req + ":\n" + ExceptionUtils.getStackTrace( e );
            }

            ResultCodeEnum code = null;
            if( e instanceof LdapException )
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

            if ( ( e.getResolvedName() != null ) &&
                    ( ( code == ResultCodeEnum.NOSUCHOBJECT ) ||
                      ( code == ResultCodeEnum.ALIASPROBLEM ) ||
                      ( code == ResultCodeEnum.INVALIDDNSYNTAX ) ||
                      ( code == ResultCodeEnum.ALIASDEREFERENCINGPROBLEM ) ) )
            {
                result.setMatchedDn( e.getResolvedName().toString() );
            }

            Iterator it = Collections.singleton( req.getResultResponse() ).iterator();
            while( it.hasNext() )
            {
                session.write( it.next() );
            }
        }
        finally
        {
            SessionRegistry.getSingleton().removeOutstandingRequest( session, req.getMessageId() );
            if ( list != null )
            {
                try { list.close(); } catch( NamingException e ){ log.error("failed on list.close()", e ); } 
            }
        }
    }
}
