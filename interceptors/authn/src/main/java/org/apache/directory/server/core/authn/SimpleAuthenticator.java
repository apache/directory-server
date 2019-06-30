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


import java.net.SocketAddress;

import javax.naming.Context;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyConfiguration;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyException;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.mina.core.session.IoSession;


/**
 * A simple {@link Authenticator} that authenticates clear text passwords
 * contained within the <code>userPassword</code> attribute in DIT. If the
 * password is stored with a one-way encryption applied (e.g. SHA), the password
 * is hashed the same way before comparison.
 *
 * We use a cache to speedup authentication, where the Dn/password are stored.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SimpleAuthenticator extends AbstractAuthenticator
{
    /** A speedup for logger in debug mode */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /**
     * A cache to store passwords. It's a speedup, we will be able to avoid backend lookups.
     *
     * Note that the backend also use a cache mechanism, but for performance gain, it's good
     * to manage a cache here. The main problem is that when a user modify his password, we will
     * have to update it at three different places :
     * - in the backend,
     * - in the partition cache,
     * - in this cache.
     *
     * The update of the backend and partition cache is already correctly handled, so we will
     * just have to offer an access to refresh the local cache.
     *
     * We need to be sure that frequently used passwords be always in cache, and not discarded.
     * We will use a LRU cache for this purpose.
     */
    private final LRUMap credentialCache;

    /** Declare a default for this cache. 100 entries seems to be enough */
    private static final int DEFAULT_CACHE_SIZE = 100;


    /**
     * Creates a new instance.
     */
    public SimpleAuthenticator()
    {
        super( AuthenticationLevel.SIMPLE );
        credentialCache = new LRUMap( DEFAULT_CACHE_SIZE );
    }


    /**
     * Creates a new instance.
     * @see AbstractAuthenticator
     *
     * @param baseDn The base Dn
     */
    public SimpleAuthenticator( Dn baseDn )
    {
        super( AuthenticationLevel.SIMPLE, baseDn );
        credentialCache = new LRUMap( DEFAULT_CACHE_SIZE );
    }


    /**
     * Creates a new instance, with an initial cache size
     * @param cacheSize the size of the credential cache
     */
    public SimpleAuthenticator( int cacheSize )
    {
        super( AuthenticationLevel.SIMPLE, Dn.ROOT_DSE );

        credentialCache = new LRUMap( cacheSize > 0 ? cacheSize : DEFAULT_CACHE_SIZE );
    }


    /**
     * Creates a new instance, with an initial cache size
     *
     * @param cacheSize the size of the credential cache
     * @param baseDn The base Dn
     */
    public SimpleAuthenticator( int cacheSize, Dn baseDn )
    {
        super( AuthenticationLevel.SIMPLE, baseDn );

        credentialCache = new LRUMap( cacheSize > 0 ? cacheSize : DEFAULT_CACHE_SIZE );
    }


    /**
     * Get the password either from cache or from backend.
     * @param principalDN The Dn from which we want the password
     * @return A byte array which can be empty if the password was not found
     * @throws Exception If we have a problem during the lookup operation
     */
    private LdapPrincipal getStoredPassword( BindOperationContext bindContext ) throws LdapException
    {
        LdapPrincipal principal = null;

        // use cache only if pwdpolicy is not enabled
        if ( !getDirectoryService().isPwdPolicyEnabled() )
        {
            synchronized ( credentialCache )
            {
                principal = ( LdapPrincipal ) credentialCache.get( bindContext.getDn() );
            }
        }

        byte[][] storedPasswords;

        if ( principal == null )
        {
            // Not found in the cache
            // Get the user password from the backend
            storedPasswords = lookupUserPassword( bindContext );

            // Deal with the special case where the user didn't enter a password
            // We will compare the empty array with the credentials. Sometime,
            // a user does not set a password. This is bad, but there is nothing
            // we can do against that, except education ...
            if ( storedPasswords == null )
            {
                storedPasswords = new byte[][]
                    {};
            }

            // Create the new principal before storing it in the cache
            principal = new LdapPrincipal( getDirectoryService().getSchemaManager(), bindContext.getDn(),
                AuthenticationLevel.SIMPLE );
            principal.setUserPassword( storedPasswords );

            // Now, update the local cache ONLY if pwdpolicy is not enabled.
            if ( !getDirectoryService().isPwdPolicyEnabled() )
            {
                synchronized ( credentialCache )
                {
                    credentialCache.put( bindContext.getDn().getNormName(), principal );
                }
            }
        }

        return principal;
    }


    /**
     * <p>
     * Looks up <tt>userPassword</tt> attribute of the entry whose name is the
     * value of {@link Context#SECURITY_PRINCIPAL} environment variable, and
     * authenticates a user with the plain-text password.
     * </p>
     */
    @Override
    public LdapPrincipal authenticate( BindOperationContext bindContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Authenticating {}", bindContext.getDn() );
        }

        // ---- extract password from JNDI environment
        byte[] credentials = bindContext.getCredentials();

        LdapPrincipal principal = getStoredPassword( bindContext );

        IoSession session = bindContext.getIoSession();

        if ( session != null )
        {
            SocketAddress clientAddress = session.getRemoteAddress();
            principal.setClientAddress( clientAddress );
            SocketAddress serverAddress = session.getServiceAddress();
            principal.setServerAddress( serverAddress );
        }

        // Get the stored password, either from cache or from backend
        byte[][] storedPasswords = principal.getUserPasswords();

        PasswordPolicyException ppe = null;
        try
        {
            checkPwdPolicy( bindContext.getEntry() );
        }
        catch ( PasswordPolicyException e )
        {
            ppe = e;
        }

        // Now, compare the passwords.
        for ( byte[] storedPassword : storedPasswords )
        {
            if ( PasswordUtil.compareCredentials( credentials, storedPassword ) )
            {
                if ( ppe != null )
                {
                    LOG.debug( "{} Authentication failed: {}", bindContext.getDn(), ppe.getMessage() );
                    throw ppe;
                }

                if ( IS_DEBUG )
                {
                    LOG.debug( "{} Authenticated", bindContext.getDn() );
                }

                return principal;
            }
        }

        // Bad password ...
        String message = I18n.err( I18n.ERR_230, bindContext.getDn().getName() );
        LOG.info( message );
        throw new LdapAuthenticationException( message );
    }


    /**
     * Local function which request the password from the backend
     * @param bindContext the Bind operation context
     * @return the credentials from the backend
     * @throws Exception if there are problems accessing backend
     */
    private byte[][] lookupUserPassword( BindOperationContext bindContext ) throws LdapException
    {
        // ---- lookup the principal entry's userPassword attribute
        Entry userEntry;

        try
        {
            /*
             * NOTE: at this point the BindOperationContext does not has a
             * null session since the user has not yet authenticated so we
             * cannot use lookup() yet.  This is a very special
             * case where we cannot rely on the bindContext to perform a new
             * sub operation.
             * We request all the attributes
             */
            userEntry = bindContext.getPrincipal();
            
            if ( userEntry == null )
            {
                LookupOperationContext lookupContext = new LookupOperationContext( getDirectoryService().getAdminSession(),
                    bindContext.getDn(), SchemaConstants.ALL_USER_ATTRIBUTES, SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES );
    
                lookupContext.setPartition( bindContext.getPartition() );
                lookupContext.setTransaction( bindContext.getTransaction() );
    
                userEntry = getDirectoryService().getPartitionNexus().lookup( lookupContext );
            }

            if ( userEntry == null )
            {
                Dn dn = bindContext.getDn();
                String upDn = dn == null ? "" : dn.getName();

                throw new LdapAuthenticationException( I18n.err( I18n.ERR_231, upDn ) );
            }
        }
        catch ( Exception cause )
        {
            LOG.error( I18n.err( I18n.ERR_6, cause.getLocalizedMessage() ) );
            LdapAuthenticationException e = new LdapAuthenticationException( cause.getLocalizedMessage() );
            e.initCause( cause );
            throw e;
        }

        DirectoryService directoryService = getDirectoryService();
        String userPasswordAttribute = SchemaConstants.USER_PASSWORD_AT;

        if ( directoryService.isPwdPolicyEnabled() )
        {
            AuthenticationInterceptor authenticationInterceptor = ( AuthenticationInterceptor ) directoryService
                .getInterceptor(
                InterceptorEnum.AUTHENTICATION_INTERCEPTOR.getName() );
            PasswordPolicyConfiguration pPolicyConfig = authenticationInterceptor.getPwdPolicy( userEntry );
            userPasswordAttribute = pPolicyConfig.getPwdAttribute();

        }

        Attribute userPasswordAttr = userEntry.get( userPasswordAttribute );

        bindContext.setEntry( new ClonedServerEntry( userEntry ) );

        // ---- assert that credentials match
        if ( userPasswordAttr == null )
        {
            return new byte[][]
                {};
        }
        else
        {
            byte[][] userPasswords = new byte[userPasswordAttr.size()][];
            int pos = 0;

            for ( Value userPassword : userPasswordAttr )
            {
                userPasswords[pos] = userPassword.getBytes();
                pos++;
            }

            return userPasswords;
        }
    }


    /**
     * Remove the principal form the cache. This is used when the user changes
     * his password.
     */
    @Override
    public void invalidateCache( Dn bindDn )
    {
        synchronized ( credentialCache )
        {
            credentialCache.remove( bindDn.getNormName() );
        }
    }
}
