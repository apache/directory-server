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
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.impl.DefaultCoreSession;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
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
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.exception.LdapOperationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    private final Map<String, Collection<Authenticator>> authenticatorsMapByType = 
        new HashMap<String, Collection<Authenticator>>();

    private DirectoryService directoryService;
    
    
    /**
     * Creates an authentication service interceptor.
     */
    public AuthenticationInterceptor()
    {
    }

    
    /**
     * Registers and initializes all {@link Authenticator}s to this service.
     */
    public void init( DirectoryService directoryService ) throws Exception
    {
        this.directoryService = directoryService;
        
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
     * @throws javax.naming.Exception if initialization fails.
     */
    private void register( Authenticator authenticator, DirectoryService directoryService ) throws Exception
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
        } 
        else
        {
            return null;
        }
    }


    public void add( NextInterceptor next, AddOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        next.add( opContext );
    }


    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        next.delete( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public LdapDN getMatchedName( NextInterceptor next, GetMatchedNameOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        return next.getMatchedName( opContext );
    }


    public ClonedServerEntry getRootDSE( NextInterceptor next, GetRootDSEOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        return next.getRootDSE( opContext );
    }


    public LdapDN getSuffix( NextInterceptor next, GetSuffixOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        return next.getSuffix( opContext );
    }


    public boolean hasEntry( NextInterceptor next, EntryOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        return next.hasEntry( opContext );
    }


    public EntryFilteringCursor list( NextInterceptor next, ListOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        return next.list( opContext );
    }


    public Set<String> listSuffixes( NextInterceptor next, ListSuffixOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        return next.listSuffixes( opContext );
    }


    public ClonedServerEntry lookup( NextInterceptor next, LookupOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
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


    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        next.modify( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public void rename( NextInterceptor next, RenameOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        next.rename( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public boolean compare( NextInterceptor next, CompareOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        boolean result = next.compare( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
        return result;
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext )
            throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        next.moveAndRename( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public void move( NextInterceptor next, MoveOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        next.move( opContext );
        invalidateAuthenticatorCaches( opContext.getDn() );
    }


    public EntryFilteringCursor search( NextInterceptor next, SearchOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        checkAuthenticated( opContext );
        return next.search( opContext );
    }


    /**
     * Check if the current operation has a valid PrincipalDN or not.
     *
     * @param opContext the OperationContext for this operation
     * @param operation the operation type
     * @throws Exception
     */
    private void checkAuthenticated( OperationContext operation ) throws Exception
    {
        if ( operation.getSession().isAnonymous() && !directoryService.isAllowAnonymousAccess() 
            && !operation.getDn().isEmpty() )
        {
            LOG.error( "Attempted operation {} by unauthenticated caller.", operation.getName() );
            throw new LdapNoPermissionException( "Attempted operation by unauthenticated caller." );
        }
    }


    public void bind( NextInterceptor next, BindOperationContext opContext ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", opContext );
        }

        if ( opContext.getSession() != null && opContext.getSession().getEffectivePrincipal() != null )
        {
            // null out the credentials
            opContext.setCredentials( null );
        }
        
        // pick the first matching authenticator type
        AuthenticationLevel level = opContext.getAuthenticationLevel();
        
        if ( level == AuthenticationLevel.UNAUTHENT )
        {
            // This is a case where the Bind request contains a DN, but no password.
            // We don't check the DN, we just return a UnwillingToPerform error
            throw new LdapOperationNotSupportedException( "Cannot Bind for DN " + opContext.getDn().getUpName(), ResultCodeEnum.UNWILLING_TO_PERFORM );
        }

        Collection<Authenticator> authenticators = getAuthenticators( level.getName() );

        if ( authenticators == null )
        {
            LOG.debug( "No authenticators found, delegating bind to the nexus." );

            // as a last resort try binding via the nexus
            next.bind( opContext );

            LOG.debug( "Nexus succeeded on bind operation." );

            // bind succeeded if we got this far
            // TODO - authentication level not being set
            LdapPrincipal principal = new LdapPrincipal( opContext.getDn(), AuthenticationLevel.SIMPLE );
            CoreSession session = new DefaultCoreSession( principal, directoryService );
            opContext.setSession( session );

            // remove creds so there is no security risk
            opContext.setCredentials( null );
            return;
        }

        // TODO : we should refactor that.
        // try each authenticator
        for ( Authenticator authenticator : authenticators )
        {
            try
            {
                // perform the authentication
                LdapPrincipal principal = authenticator.authenticate( opContext );
                
                LdapPrincipal clonedPrincipal = (LdapPrincipal)(principal.clone());

                // remove creds so there is no security risk
                opContext.setCredentials( null );
                clonedPrincipal.setUserPassword( StringTools.EMPTY_BYTES );

                // authentication was successful
                CoreSession session = new DefaultCoreSession( clonedPrincipal, directoryService );
                opContext.setSession( session );

                return;
            }
            catch ( LdapAuthenticationException e )
            {
                // authentication failed, try the next authenticator
                if ( LOG.isInfoEnabled() )
                {
                    LOG.info( "Authenticator {} failed to authenticate: {}", authenticator, opContext );
                }
            }
            catch ( Exception e )
            {
                // Log other exceptions than LdapAuthenticationException
                if ( LOG.isWarnEnabled() )
                {
                    LOG.info( "Unexpected failure for Authenticator {} : {}", authenticator, opContext );
                }
            }
        }

        if ( LOG.isInfoEnabled() )
        {
            LOG.info( "Cannot bind to the server " );
        }

        LdapDN dn = opContext.getDn();
        String upDn = ( dn == null ? "" : dn.getUpName() );
        throw new LdapAuthenticationException( "Cannot authenticate user " + upDn );
    }
}
