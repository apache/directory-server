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
package org.apache.directory.server.core.authn;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.ListSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.LdapJndiProperties;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.message.MessageTypeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;


/**
 * An {@link Interceptor} that authenticates users.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 * @org.apache.xbean.XBean
 */
public class AuthenticationInterceptor extends BaseInterceptor
{
    private static final Logger LOG = LoggerFactory.getLogger( AuthenticationInterceptor.class );

    /**
     * Speedup for logs
     */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    private Set<Authenticator> authenticators;
    private final Map<String, Collection<Authenticator>> authenticatorsMapByType = new HashMap<String, Collection<Authenticator>>();

    /**
     * Creates an authentication service interceptor.
     */
    public AuthenticationInterceptor()
    {
    }

    /**
     * Registers and initializes all {@link Authenticator}s to this service.
     */
    public void init( DirectoryService directoryService ) throws NamingException
    {

        if ( authenticators == null )
        {
            setDefaultAuthenticators();
        }
        // Register all authenticators
        for ( Authenticator authenticator : authenticators )
        {
            register( authenticator, directoryService );
        }
    }

    private void setDefaultAuthenticators()
    {
        Set<Authenticator> set = new HashSet<Authenticator>();
        set.add( new AnonymousAuthenticator() );
        set.add( new SimpleAuthenticator() );
        set.add( new StrongAuthenticator() );

        setAuthenticators( set );
    }


    public Set<Authenticator> getAuthenticators()
    {
        return authenticators;
    }

    /**
     * @param authenticators authenticators to be used by this AuthenticationInterceptor
     * @org.apache.xbean.Property nestedType="org.apache.directory.server.core.authn.Authenticator"
     */
    public void setAuthenticators( Set<Authenticator> authenticators )
    {
        this.authenticators = authenticators;
    }

    /**
     * Deinitializes and deregisters all {@link Authenticator}s from this service.
     */
    public void destroy()
    {
        authenticatorsMapByType.clear();
        Set<Authenticator> copy = new HashSet<Authenticator>( authenticators );
        authenticators = null;
        for ( Authenticator authenticator : copy )
        {
            authenticator.destroy();
        }
    }

    /**
     * Initializes the specified {@link Authenticator} and registers it to
     * this service.
     *
     * @param authenticator Authenticator to initialize and register by type
     * @param directoryService configuration info to supply to the Authenticator during initialization
     * @throws javax.naming.NamingException if initialization fails.
     */
    private void register( Authenticator authenticator, DirectoryService directoryService ) throws NamingException
    {
        authenticator.init( directoryService );

        Collection<Authenticator> authenticatorList = getAuthenticators( authenticator.getAuthenticatorType() );

        if ( authenticatorList == null )
        {
            authenticatorList = new ArrayList<Authenticator>();
            authenticatorsMapByType.put( authenticator.getAuthenticatorType(), authenticatorList );
        }

        authenticatorList.add( authenticator );
    }


    /**
     * Returns the list of {@link Authenticator}s with the specified type.
     *
     * @param type type of Authenticator sought
     * @return A list of Authenticators of the requested type or <tt>null</tt> if no authenticator is found.
     */
    private Collection<Authenticator> getAuthenticators( String type )
    {
        Collection<Authenticator> result = authenticatorsMapByType.get( type );

        if ( ( result != null ) && ( result.size() > 0 ) )
        {
            return result;
        } else
        {
            return null;
        }
    }


    public void add( NextInterceptor next, AddOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Adding the entry " +
                    opContext.getEntry() +
                    " for DN = '" + opContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.ADD_REQUEST );
        next.add( opContext );
    }


    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Deleting name = '" + opContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.DEL_REQUEST );
        next.delete( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public LdapDN getMatchedName( NextInterceptor next, GetMatchedNameOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Matching name = '" + opContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated();
        return next.getMatchedName( opContext );
    }


    public Attributes getRootDSE( NextInterceptor next, GetRootDSEOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Getting root DSE" );
        }

        checkAuthenticated();
        return next.getRootDSE( opContext );
    }


    public LdapDN getSuffix( NextInterceptor next, GetSuffixOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Getting suffix for name = '" + opContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated();
        return next.getSuffix( opContext );
    }


    public boolean hasEntry( NextInterceptor next, EntryOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Testing if entry name = '" + opContext.getDn().getUpName() + "' exists" );
        }

        checkAuthenticated();
        return next.hasEntry( opContext );
    }


    public NamingEnumeration<SearchResult> list( NextInterceptor next, ListOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Listing base = '" + opContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated();
        return next.list( opContext );
    }


    public Iterator<String> listSuffixes( NextInterceptor next, ListSuffixOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Listing suffixes" );
        }

        checkAuthenticated();
        return next.listSuffixes( opContext );
    }


    public Attributes lookup( NextInterceptor next, LookupOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            List<String> attrIds = opContext.getAttrsId();

            if ( ( attrIds != null ) && ( attrIds.size() != 0 ) )
            {
                String attrs = StringTools.listToString( attrIds );
                LOG.debug( "Lookup name = '" + opContext.getDn().getUpName() + "', attributes = " + attrs );
            } else
            {
                LOG.debug( "Lookup name = '" + opContext.getDn().getUpName() + "', no attributes " );
            }
        }

        checkAuthenticated();
        return next.lookup( opContext );
    }

    private void invalidateAuthenticatorCaches( LdapDN principalDn )
    {
        for ( String authMech : authenticatorsMapByType.keySet() )
        {
            Collection<Authenticator> authenticators = getAuthenticators( authMech );

            // try each authenticator
            for ( Authenticator authenticator : authenticators )
            {
                authenticator.invalidateCache( principalDn );
            }
        }
    }


    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( opContext.toString() );
        }

        checkAuthenticated( MessageTypeEnum.MODIFY_REQUEST );
        next.modify( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public void rename( NextInterceptor next, RenameOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Modifying name = '" + opContext.getDn().getUpName() + "', new RDN = '" +
                    opContext.getNewRdn() + "', " +
                    "oldRDN = '" + opContext.getDelOldDn() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.MOD_DN_REQUEST );
        next.rename( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext )
            throws NamingException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Moving name = '" + opContext.getDn().getUpName() + "' to name = '" +
                    opContext.getParent() + "', new RDN = '" +
                    opContext.getNewRdn() + "', oldRDN = '" +
                    opContext.getDelOldDn() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.MOD_DN_REQUEST );
        next.moveAndRename( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public void move( NextInterceptor next, MoveOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Moving name = '" + opContext.getDn().getUpName() + " to name = '" +
                    opContext.getParent().getUpName() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.MOD_DN_REQUEST );
        next.move( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public NamingEnumeration<SearchResult> search( NextInterceptor next, SearchOperationContext opContext ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Search for base = '" + opContext.getDn().getUpName() + "'" );
        }

        checkAuthenticated( MessageTypeEnum.SEARCH_REQUEST );
        return next.search( opContext );
    }


    private void checkAuthenticated( MessageTypeEnum operation ) throws NamingException
    {
        try
        {
            checkAuthenticated();
        }
        catch ( IllegalStateException ise )
        {
            LOG.error( "Attempted operation {} by unauthenticated caller.", operation.name() );

            throw new IllegalStateException( "Attempted operation by unauthenticated caller." );
        }
    }

    private void checkAuthenticated() throws NamingException
    {
        ServerContext ctx = ( ServerContext ) InvocationStack.getInstance().peek().getCaller();

        if ( ctx.getPrincipal() != null )
        {
            if ( ctx.getEnvironment().containsKey( Context.SECURITY_CREDENTIALS ) )
            {
                ctx.removeFromEnvironment( Context.SECURITY_CREDENTIALS );
            }

            return;
        }

        throw new IllegalStateException( "Attempted operation by unauthenticated caller." );
    }


    public void bind( NextInterceptor next, BindOperationContext opContext )
            throws NamingException
    {
        // The DN is always normalized here
        LdapDN normBindDn = opContext.getDn();
        String bindUpDn = normBindDn.getUpName();

        if ( IS_DEBUG )
        {
            LOG.debug( "Bind operation. bindDn: " + bindUpDn );
        }

        // check if we are already authenticated and if so we return making
        // sure first that the credentials are not exposed within context
        ServerContext ctx = ( ServerContext ) InvocationStack.getInstance().peek().getCaller();

        if ( IS_DEBUG )
        {
            LOG.debug( "bind: principal: " + ctx.getPrincipal() );
        }

        if ( ctx.getPrincipal() != null )
        {
            if ( ctx.getEnvironment().containsKey( Context.SECURITY_CREDENTIALS ) )
            {
                ctx.removeFromEnvironment( Context.SECURITY_CREDENTIALS );
            }

            return;
        }

        // pick the first matching authenticator type
        Collection<Authenticator> authenticators = null;

        for ( String mechanism : opContext.getMechanisms() )
        {
            authenticators = getAuthenticators( mechanism );

            if ( authenticators != null )
            {
                break;
            }
        }

        if ( authenticators == null )
        {
            LOG.debug( "No authenticators found, delegating bind to the nexus." );

            // as a last resort try binding via the nexus
            next.bind( opContext );

            LOG.debug( "Nexus succeeded on bind operation." );

            // bind succeeded if we got this far 
            ctx.setPrincipal( new TrustedPrincipalWrapper( new LdapPrincipal( normBindDn, LdapJndiProperties
                    .getAuthenticationLevel( ctx.getEnvironment() ) ) ) );

            // remove creds so there is no security risk
            ctx.removeFromEnvironment( Context.SECURITY_CREDENTIALS );
            return;
        }

        // TODO : we should refactor that.
        // try each authenticator
        for ( Authenticator authenticator : authenticators )
        {
            try
            {
                // perform the authentication
                LdapPrincipal authorizationId = authenticator.authenticate( normBindDn, ctx );

                // authentication was successful
                ctx.setPrincipal( new TrustedPrincipalWrapper( authorizationId ) );

                // remove creds so there is no security risk
                ctx.removeFromEnvironment( Context.SECURITY_CREDENTIALS );

                return;
            }
            catch ( LdapAuthenticationException e )
            {
                // authentication failed, try the next authenticator
                if ( LOG.isInfoEnabled() )
                {
                    LOG.info( "Authenticator " + authenticator.getClass() + " failed to authenticate " + bindUpDn );
                }
            }
            catch ( Exception e )
            {
                // Log other exceptions than LdapAuthenticationException
                if ( LOG.isWarnEnabled() )
                {
                    LOG.warn( "Unexpected exception from " + authenticator.getClass() + " for principal " + bindUpDn, e );
                }
            }
        }

        if ( LOG.isInfoEnabled() )
        {
            LOG.info( "Cannot bind to the server " );
        }

        throw new LdapAuthenticationException();
    }

    /**
     * FIXME This doesn't secure anything actually.
     * <p/>
     * Created this wrapper to pass to ctx.setPrincipal() which is public for added
     * security.  This adds more security because an instance of this class is not
     * easily accessible whereas LdapPrincipals can be accessed easily from a context
     * althought they cannot be instantiated outside of the authn package.  Malicious
     * code may not be able to set the principal to what they would like but they
     * could switch existing principals using the now public ServerContext.setPrincipal()
     * method.  To avoid this we make sure that this metho takes a TrustedPrincipalWrapper
     * as opposed to the LdapPrincipal.  Only this service can create and call setPrincipal
     * with a TrustedPrincipalWrapper.
     */
    public final class TrustedPrincipalWrapper
    {
        /**
         * the wrapped ldap principal
         */
        private final LdapPrincipal principal;


        /**
         * Creates a TrustedPrincipalWrapper around an LdapPrincipal.
         *
         * @param principal the LdapPrincipal to wrap
         */
        private TrustedPrincipalWrapper( LdapPrincipal principal )
        {
            this.principal = principal;
        }


        /**
         * Gets the LdapPrincipal this TrustedPrincipalWrapper wraps.
         *
         * @return the wrapped LdapPrincipal
         */
        public LdapPrincipal getPrincipal()
        {
            return principal;
        }
    }
}
