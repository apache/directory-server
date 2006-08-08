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
package org.apache.directory.server.ldap.support;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ReferralException;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.configuration.StartupConfiguration;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.directory.shared.ldap.codec.util.LdapResultEnum;
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
import org.apache.directory.shared.ldap.message.ScopeEnum;
import org.apache.directory.shared.ldap.message.SearchRequest;
import org.apache.directory.shared.ldap.message.SearchResponseDone;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ArrayUtils;
import org.apache.directory.shared.ldap.util.ExceptionUtils;

import org.apache.mina.common.IoSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A handler for processing search requests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SearchHandler implements LdapMessageHandler
{
    private static final Logger log = LoggerFactory.getLogger( SearchHandler.class );
    private static final String DEREFALIASES_KEY = "java.naming.ldap.derefAliases";
    private StartupConfiguration cfg;

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /**
     * Builds the JNDI search controls for a SearchRequest.
     *  
     * @param req the search request.
     * @param ids the ids to return
     * @return the SearchControls to use with the ApacheDS server side JNDI provider
     */
    private SearchControls getSearchControls( SearchRequest req, String[] ids, boolean isAdmin )
    {
        // prepare all the search controls
        SearchControls controls = new SearchControls();
        
        // take the minimum of system limit with request specified value
        if ( isAdmin )
        {
            controls.setCountLimit( req.getSizeLimit() );
            
            // The setTimeLimit needs a number of milliseconds
            // when the search control is expressed in seconds
            int timeLimit = req.getTimeLimit();
            
            // Just check that we are not exceeding the maximum for a long 
            if ( timeLimit > Integer.MAX_VALUE / 1000 )
            {
                timeLimit = 0;
            }
            
            // The maximum time we can wait is around 24 days ...
            // Is it enough ? ;)
            controls.setTimeLimit( timeLimit * 1000 );
        }
        else
        {
            controls.setCountLimit( Math.min( req.getSizeLimit(), cfg.getMaxSizeLimit() ) );
            controls.setTimeLimit( ( int ) Math.min( req.getTimeLimit(), cfg.getMaxTimeLimit() ) );
        }
        
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
        boolean isBaseIsRoot = req.getBase().isEmpty();
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
    public void messageReceived( IoSession session, Object request ) throws Exception
    {
    	if ( IS_DEBUG )
    	{
    		log.debug( "Message received : " + request.toString() );
    	}

    	ServerLdapContext ctx;
        SearchRequest req = ( SearchRequest ) request;
        NamingEnumeration list = null;
        String[] ids = null;
        Collection retAttrs = new HashSet();
        retAttrs.addAll( req.getAttributes() );

        // add the search request to the registry of outstanding requests for this session
        SessionRegistry.getSingleton().addOutstandingRequest( session, req );

        // check the attributes to see if a referral's ref attribute is included
        if ( retAttrs.size() > 0 && !retAttrs.contains( "ref" ) )
        {
            retAttrs.add( "ref" );
            ids = ( String[] ) retAttrs.toArray( ArrayUtils.EMPTY_STRING_ARRAY );
        }
        else if ( retAttrs.size() > 0 )
        {
            ids = ( String[] ) retAttrs.toArray( ArrayUtils.EMPTY_STRING_ARRAY );
        }

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
                if ( !( unknown instanceof ServerLdapContext ) )
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
                if ( !( unknown instanceof ServerLdapContext ) )
                {
                    ctx = ( ServerLdapContext ) unknown.lookup( "" );
                }
                else
                {
                    ctx = ( ServerLdapContext ) unknown;
                }
                Control[] controls = ( Control[] ) req.getControls().values().toArray( new Control[0] );
                ctx.setRequestControls( controls );
            }
            ctx.addToEnvironment( DEREFALIASES_KEY, req.getDerefAliases().getName() );
            if ( req.getControls().containsKey( ManageDsaITControl.CONTROL_OID ) )
            {
                ctx.addToEnvironment( Context.REFERRAL, "ignore" );
            }
            else
            {
                ctx.addToEnvironment( Context.REFERRAL, "throw-finding-base" );
            }

            // ===============================================================
            // Handle annonymous binds
            // ===============================================================

            boolean allowAnonymousBinds = cfg.isAllowAnonymousAccess();
            boolean isAnonymousUser = ( ( ServerLdapContext ) ctx ).getPrincipal().getName().trim().equals( "" );

            if ( isAnonymousUser && !allowAnonymousBinds && !isRootDSESearch )
            {
                LdapResult result = req.getResultResponse().getLdapResult();
                result.setResultCode( ResultCodeEnum.INSUFFICIENTACCESSRIGHTS );
                String msg = "Bind failure: Anonymous binds have been disabled!";
                result.setErrorMessage( msg );
                session.write( req.getResultResponse() );
                return;
            }


            // ===============================================================
            // Set search limits differently based on user's identity
            // ===============================================================

            SearchControls controls = null;
            if ( isAnonymousUser )
            {
                controls = getSearchControls( req, ids, false );
            }
            else if ( ( ( ServerLdapContext ) ctx ).getPrincipal().getName()
                .trim().equals( PartitionNexus.ADMIN_PRINCIPAL ) )
            {
                controls = getSearchControls( req, ids, true );
            }
            else
            {
                controls = getSearchControls( req, ids, false );
            }
            
            
            // ===============================================================
            // Handle psearch differently
            // ===============================================================

            PersistentSearchControl psearchControl = ( PersistentSearchControl ) req.getControls().get(
                PersistentSearchControl.CONTROL_OID );
            if ( psearchControl != null )
            {
                // there are no limits for psearch processing
                controls.setCountLimit( 0 );
                controls.setTimeLimit( 0 );

                if ( !psearchControl.isChangesOnly() )
                {
                    list = ( ( ServerLdapContext ) ctx ).search( req.getBase(), req.getFilter(),
                        controls );
                    if ( list instanceof AbandonListener )
                    {
                        req.addAbandonListener( ( AbandonListener ) list );
                    }
                    if ( list.hasMore() )
                    {
                        Iterator it = new SearchResponseIterator( req, ctx, list, controls.getSearchScope(), session );
                        while ( it.hasNext() )
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
                                else
                                    break;
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
            list = ( ( ServerLdapContext ) ctx ).search( req.getBase(), req.getFilter(), controls );
            if ( list instanceof AbandonListener )
            {
                req.addAbandonListener( ( AbandonListener ) list );
            }

            if ( list.hasMore() )
            {
                Iterator it = new SearchResponseIterator( req, ctx, list, controls.getSearchScope(), session );
                while ( it.hasNext() )
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
                while ( it.hasNext() )
                {
                    session.write( it.next() );
                }
                return;
            }
        }
        catch ( ReferralException e )
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
            while ( e.skipReferral() );
            session.write( req.getResultResponse() );
            SessionRegistry.getSingleton().removeOutstandingRequest( session, req.getMessageId() );
            return;
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

            String msg = "failed on search operation";
            if ( log.isDebugEnabled() )
            {
                msg += ":\n" + req + ":\n" + ExceptionUtils.getStackTrace( e );
            }

            ResultCodeEnum code = null;
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
                && ( ( code == ResultCodeEnum.NOSUCHOBJECT ) || ( code == ResultCodeEnum.ALIASPROBLEM )
                    || ( code == ResultCodeEnum.INVALIDDNSYNTAX ) || ( code == ResultCodeEnum.ALIASDEREFERENCINGPROBLEM ) ) )
            {
                result.setMatchedDn( (LdapDN)e.getResolvedName() );
            }

            Iterator it = Collections.singleton( req.getResultResponse() ).iterator();
            while ( it.hasNext() )
            {
                session.write( it.next() );
            }
            SessionRegistry.getSingleton().removeOutstandingRequest( session, req.getMessageId() );
        }
        finally
        {
            if ( list != null )
            {
                try
                {
                    list.close();
                }
                catch ( NamingException e )
                {
                    log.error( "failed on list.close()", e );
                }
            }
        }
    }


    public void init( StartupConfiguration cfg )
    {
        this.cfg = cfg;
    }
}
