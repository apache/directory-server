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


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.naming.Context;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.admin.AdministrativePointInterceptor;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.collective.CollectiveAttributeInterceptor;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.event.EventInterceptor;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.operational.OperationalAttributeInterceptor;
import org.apache.directory.server.core.schema.SchemaInterceptor;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.core.trigger.TriggerInterceptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.util.Base64;
import org.apache.directory.shared.util.UnixCrypt;
import org.apache.directory.shared.util.StringConstants;
import org.apache.directory.shared.util.Strings;


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
     * Define the interceptors we should *not* go through when we will have to request the backend
     * about a userPassword.
     */
    private static final Collection<String> USERLOOKUP_BYPASS;

    static
    {
        Set<String> c = new HashSet<String>();
        c.add( NormalizationInterceptor.class.getName() );
        c.add( AuthenticationInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        c.add( AdministrativePointInterceptor.class.getName() );
        c.add( ExceptionInterceptor.class.getName() );
        c.add( OperationalAttributeInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
        c.add( CollectiveAttributeInterceptor.class.getName() );
        c.add( SubentryInterceptor.class.getName() );
        c.add( EventInterceptor.class.getName() );
        c.add( TriggerInterceptor.class.getName() );
        USERLOOKUP_BYPASS = Collections.unmodifiableCollection( c );
    }


    /**
     * Creates a new instance.
     * @see AbstractAuthenticator
     */
    public SimpleAuthenticator()
    {
        super( AuthenticationLevel.SIMPLE );
        credentialCache = new LRUMap( DEFAULT_CACHE_SIZE );
    }


    /**
     * Creates a new instance, with an initial cache size
     * @param cacheSize the size of the credential cache
     */
    public SimpleAuthenticator( int cacheSize )
    {
        super( AuthenticationLevel.SIMPLE );

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
        if( !getDirectoryService().isPwdPolicyEnabled() )
        {
            synchronized ( credentialCache )
            {
                principal = ( LdapPrincipal ) credentialCache.get( bindContext.getDn().getNormName() );
            }
        }

        byte[] storedPassword;

        if ( principal == null )
        {
            // Not found in the cache
            // Get the user password from the backend
            storedPassword = lookupUserPassword( bindContext );

            // Deal with the special case where the user didn't enter a password
            // We will compare the empty array with the credentials. Sometime,
            // a user does not set a password. This is bad, but there is nothing
            // we can do against that, except education ...
            if ( storedPassword == null )
            {
                storedPassword = ArrayUtils.EMPTY_BYTE_ARRAY;
            }

            // Create the new principal before storing it in the cache
            principal = new LdapPrincipal( bindContext.getDn(), AuthenticationLevel.SIMPLE, storedPassword );

            // Now, update the local cache ONLY if pwdpolicy is not enabled.
            if( !getDirectoryService().isPwdPolicyEnabled() )
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
    public LdapPrincipal authenticate( BindOperationContext bindContext ) throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Authenticating {}", bindContext.getDn() );
        }

        // ---- extract password from JNDI environment
        byte[] credentials = bindContext.getCredentials();

        LdapPrincipal principal = getStoredPassword( bindContext );

        // Get the stored password, either from cache or from backend
        byte[] storedPassword = principal.getUserPassword();

        // Now, compare the two passwords.
        if ( PasswordUtil.compareCredentials( credentials, storedPassword ) )
        {
            if ( IS_DEBUG )
            {
                LOG.debug( "{} Authenticated", bindContext.getDn() );
            }

            return principal;
        }
        else
        {
            // Bad password ...
            String message = I18n.err( I18n.ERR_230, bindContext.getDn().getName() );
            LOG.info( message );
            throw new LdapAuthenticationException( message );
        }
    }


    /**
     * Local function which request the password from the backend
     * @param bindContext the Bind operation context
     * @return the credentials from the backend
     * @throws Exception if there are problems accessing backend
     */
    private byte[] lookupUserPassword( BindOperationContext bindContext ) throws LdapException
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
             */
            LookupOperationContext lookupContext = new LookupOperationContext( getDirectoryService().getAdminSession(),
                bindContext.getDn() );
            lookupContext.setByPassed( USERLOOKUP_BYPASS );
            lookupContext.setAttrsId( new String[]{ "+" } );

            userEntry = getDirectoryService().getOperationManager().lookup( lookupContext );

            if ( userEntry == null )
            {
                Dn dn = bindContext.getDn();
                String upDn = ( dn == null ? "" : dn.getName() );

                throw new LdapAuthenticationException( I18n.err( I18n.ERR_231, upDn ) );
            }
        }
        catch ( Exception cause )
        {
            LOG.error( I18n.err( I18n.ERR_6, cause.getLocalizedMessage() ) );
            LdapAuthenticationException e = new LdapAuthenticationException( cause.getLocalizedMessage() );
            e.initCause( e );
            throw e;
        }

        checkPwdPolicy( userEntry );

        Value<?> userPassword;

        EntryAttribute userPasswordAttr = userEntry.get( SchemaConstants.USER_PASSWORD_AT );

        bindContext.setEntry( new ClonedServerEntry( userEntry ) );

        // ---- assert that credentials match
        if ( userPasswordAttr == null )
        {
            return StringConstants.EMPTY_BYTES;
        }
        else
        {
            userPassword = userPasswordAttr.get();

            return userPassword.getBytes();
        }
    }


    /**
     * Get the algorithm of a password, which is stored in the form "{XYZ}...".
     * The method returns null, if the argument is not in this form. It returns
     * XYZ, if XYZ is an algorithm known to the MessageDigest class of
     * java.security.
     *
     * @param password a byte[]
     * @return included message digest alorithm, if any
     * @throws IllegalArgumentException if the algorithm cannot be identified
     */
    protected String getAlgorithmForHashedPassword( byte[] password ) throws IllegalArgumentException
    {
        String result = null;

        // Check if password arg is string or byte[]
        String sPassword = Strings.utf8ToString(password);
        int rightParen = sPassword.indexOf( '}' );

        if ( ( sPassword.length() > 2 ) && ( sPassword.charAt( 0 ) == '{' ) && ( rightParen > -1 ) )
        {
            String algorithm = sPassword.substring( 1, rightParen );

            if ( LdapSecurityConstants.HASH_METHOD_CRYPT.getName().equalsIgnoreCase( algorithm ) )
            {
                return algorithm;
            }

            try
            {
                MessageDigest.getInstance( algorithm );
                result = algorithm;
            }
            catch ( NoSuchAlgorithmException e )
            {
                LOG.warn( "Unknown message digest algorithm in password: " + algorithm, e );
            }
        }

        return result;
    }


    /**
     * Creates a digested password. For a given hash algorithm and a password
     * value, the algorithm is applied to the password, and the result is Base64
     * encoded. The method returns a String which looks like "{XYZ}bbbbbbb",
     * whereas XYZ is the name of the algorithm, and bbbbbbb is the Base64
     * encoded value of XYZ applied to the password.
     *
     * @param algorithm
     *            an algorithm which is supported by
     *            java.security.MessageDigest, e.g. SHA
     * @param password
     *            password value, a byte[]
     *
     * @return a digested password, which looks like
     *         {SHA}LhkDrSoM6qr0fW6hzlfOJQW61tc=
     *
     * @throws IllegalArgumentException
     *             if password is neither a String nor a byte[], or algorithm is
     *             not known to java.security.MessageDigest class
     */
    protected String createDigestedPassword( String algorithm, byte[] password ) throws IllegalArgumentException
    {
        // create message digest object
        try
        {
            if ( LdapSecurityConstants.HASH_METHOD_CRYPT.getName().equalsIgnoreCase( algorithm ) )
            {
                String saltWithCrypted = UnixCrypt.crypt(Strings.utf8ToString(password), "");
                String crypted = saltWithCrypted.substring( 2 );
                return '{' + algorithm + '}' + Arrays.toString( Strings.getBytesUtf8(crypted) );
            }
            else
            {
                MessageDigest digest = MessageDigest.getInstance( algorithm );

                // calculate hashed value of password
                byte[] fingerPrint = digest.digest( password );
                char[] encoded = Base64.encode(fingerPrint);

                // create return result of form "{alg}bbbbbbb"
                return '{' + algorithm + '}' + new String( encoded );
            }
        }
        catch ( NoSuchAlgorithmException nsae )
        {
            LOG.error( I18n.err( I18n.ERR_7, algorithm ) );
            throw new IllegalArgumentException( nsae.getLocalizedMessage() );
        }
    }


    /**
     * Remove the principal form the cache. This is used when the user changes
     * his password.
     */
    public void invalidateCache( Dn bindDn )
    {
        synchronized ( credentialCache )
        {
            credentialCache.remove( bindDn.getNormName() );
        }
    }
}
