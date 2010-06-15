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
import org.apache.directory.server.core.DefaultCoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link Interceptor} that authenticates users.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthenticationInterceptor extends BaseInterceptor
{
    private static final Logger LOG = LoggerFactory.getLogger( AuthenticationInterceptor.class );

    /**
     * Speedup for logs
     */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    private Set<Authenticator> authenticators;
    private final Map<AuthenticationLevel, Collection<Authenticator>> authenticatorsMapByType = new HashMap<AuthenticationLevel, Collection<Authenticator>>();

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
    public void init( DirectoryService directoryService ) throws LdapException
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
    private void register( Authenticator authenticator, DirectoryService directoryService ) throws LdapException
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
    private Collection<Authenticator> getAuthenticators( AuthenticationLevel type )
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


    public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", addContext );
        }

        checkAuthenticated( addContext );
        next.add( addContext );
    }


    public void delete( NextInterceptor next, DeleteOperationContext deleteContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", deleteContext );
        }

        checkAuthenticated( deleteContext );
        next.delete( deleteContext );
        invalidateAuthenticatorCaches( deleteContext.getDn() );
    }


    public Entry getRootDSE( NextInterceptor next, GetRootDSEOperationContext getRootDseContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", getRootDseContext );
        }

        checkAuthenticated( getRootDseContext );
        return next.getRootDSE( getRootDseContext );
    }


    public boolean hasEntry( NextInterceptor next, EntryOperationContext hasEntryContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", hasEntryContext );
        }

        checkAuthenticated( hasEntryContext );
        return next.hasEntry( hasEntryContext );
    }


    public EntryFilteringCursor list( NextInterceptor next, ListOperationContext listContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", listContext );
        }

        checkAuthenticated( listContext );
        return next.list( listContext );
    }


    public Entry lookup( NextInterceptor next, LookupOperationContext lookupContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", lookupContext );
        }

        checkAuthenticated( lookupContext );
        return next.lookup( lookupContext );
    }

    
    private void invalidateAuthenticatorCaches( DN principalDn )
    {
        for ( AuthenticationLevel authMech : authenticatorsMapByType.keySet() )
        {
            Collection<Authenticator> authenticators = getAuthenticators( authMech );

            // try each authenticator
            for ( Authenticator authenticator : authenticators )
            {
                authenticator.invalidateCache( principalDn );
            }
        }
    }


    public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", modifyContext );
        }

        checkAuthenticated( modifyContext );
        next.modify( modifyContext );
        invalidateAuthenticatorCaches( modifyContext.getDn() );
    }


    public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", renameContext );
        }

        checkAuthenticated( renameContext );
        next.rename( renameContext );
        invalidateAuthenticatorCaches( renameContext.getDn() );
    }

    
    /**
     * {@inheritDoc}
     */
    public boolean compare( NextInterceptor next, CompareOperationContext compareContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", compareContext );
        }

        checkAuthenticated( compareContext );
        boolean result = next.compare( compareContext );
        invalidateAuthenticatorCaches( compareContext.getDn() );
        
        return result;
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameContext )
            throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", moveAndRenameContext );
        }

        checkAuthenticated( moveAndRenameContext );
        next.moveAndRename( moveAndRenameContext );
        invalidateAuthenticatorCaches( moveAndRenameContext.getDn() );
    }


    /**
     * {@inheritDoc}
     */
    public void move( NextInterceptor next, MoveOperationContext moveContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", moveContext );
        }

        checkAuthenticated( moveContext );
        next.move( moveContext );
        invalidateAuthenticatorCaches( moveContext.getDn() );
    }


    public EntryFilteringCursor search( NextInterceptor next, SearchOperationContext searchContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", searchContext );
        }

        checkAuthenticated( searchContext );
        return next.search( searchContext );
    }


    /**
     * Check if the current operation has a valid PrincipalDN or not.
     *
     * @param operation the operation type
     * @throws Exception
     */
    private void checkAuthenticated( OperationContext operation ) throws LdapException
    {
        if ( operation.getSession().isAnonymous() && !directoryService.isAllowAnonymousAccess() 
            && !operation.getDn().isEmpty() )
        {
            String msg = I18n.err( I18n.ERR_5, operation.getName() );
            LOG.error( msg );
            throw new LdapNoPermissionException( msg );
        }
    }


    public void bind( NextInterceptor next, BindOperationContext bindContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Operation Context: {}", bindContext );
        }

        if ( ( bindContext.getSession() != null ) && ( bindContext.getSession().getEffectivePrincipal() != null ) )
        {
            // null out the credentials
            bindContext.setCredentials( null );
        }
        
        // pick the first matching authenticator type
        AuthenticationLevel level = bindContext.getAuthenticationLevel();
        
        if ( level == AuthenticationLevel.UNAUTHENT )
        {
            // This is a case where the Bind request contains a DN, but no password.
            // We don't check the DN, we just return a UnwillingToPerform error
            // Cf RFC 4513, chap. 5.1.2
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, "Cannot Bind for DN " + bindContext.getDn().getName() );
        }

        Collection<Authenticator> authenticators = getAuthenticators( level );

        if ( authenticators == null )
        {
            LOG.debug( "No authenticators found, delegating bind to the nexus." );

            // as a last resort try binding via the nexus
            next.bind( bindContext );

            LOG.debug( "Nexus succeeded on bind operation." );

            // bind succeeded if we got this far
            // TODO - authentication level not being set
            LdapPrincipal principal = new LdapPrincipal( bindContext.getDn(), AuthenticationLevel.SIMPLE );
            CoreSession session = new DefaultCoreSession( principal, directoryService );
            bindContext.setSession( session );

            // remove creds so there is no security risk
            bindContext.setCredentials( null );
            return;
        }

        // TODO : we should refactor that.
        // try each authenticator
        for ( Authenticator authenticator : authenticators )
        {
            try
            {
                // perform the authentication
                LdapPrincipal principal = authenticator.authenticate( bindContext );
                
                LdapPrincipal clonedPrincipal = (LdapPrincipal)(principal.clone());

                // remove creds so there is no security risk
                bindContext.setCredentials( null );
                clonedPrincipal.setUserPassword( StringTools.EMPTY_BYTES );

                // authentication was successful
                CoreSession session = new DefaultCoreSession( clonedPrincipal, directoryService );
                bindContext.setSession( session );

                return;
            }
            catch ( LdapAuthenticationException e )
            {
                // authentication failed, try the next authenticator
                if ( LOG.isInfoEnabled() )
                {
                    LOG.info( "Authenticator {} failed to authenticate: {}", authenticator, bindContext );
                }
            }
            catch ( Exception e )
            {
                // Log other exceptions than LdapAuthenticationException
                if ( LOG.isWarnEnabled() )
                {
                    LOG.info( "Unexpected failure for Authenticator {} : {}", authenticator, bindContext );
                }
            }
        }

        if ( LOG.isInfoEnabled() )
        {
            LOG.info( "Cannot bind to the server " );
        }

        DN dn = bindContext.getDn();
        String upDn = ( dn == null ? "" : dn.getName() );
        throw new LdapAuthenticationException( I18n.err( I18n.ERR_229, upDn ) );
    }
}
