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
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.commons.collections.map.LRUMap;
import org.apache.directory.server.core.interceptor.context.LookupServiceContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.server.core.trigger.TriggerService;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ArrayUtils;
import org.apache.directory.shared.ldap.util.Base64;
import org.apache.directory.shared.ldap.util.StringTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple {@link Authenticator} that authenticates clear text passwords
 * contained within the <code>userPassword</code> attribute in DIT. If the
 * password is stored with a one-way encryption applied (e.g. SHA), the password
 * is hashed the same way before comparison.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SimpleAuthenticator extends AbstractAuthenticator
{
    private static final Logger log = LoggerFactory.getLogger( SimpleAuthenticator.class );
    
    /** A speedup for logger in debug mode */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

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
    private LRUMap credentialCache;
    
    /** Declare a default for this cache. 100 entries seems to be enough */
    private static final int DEFAULT_CACHE_SIZE = 100;

    /**
     * Define the interceptors we should *not* go through when we will have to request the backend
     * about a userPassword.
     */
    private static final Collection USERLOOKUP_BYPASS;
    static
    {
        Set<String> c = new HashSet<String>();
        c.add( "normalizationService" );
        c.add( "authenticationService" );
        c.add( "referralService" );
        c.add( "authorizationService" );
        c.add( "defaultAuthorizationService" );
        c.add( "exceptionService" );
        c.add( "operationalAttributeService" );
        c.add( "schemaService" );
        c.add( "subentryService" );
        c.add( "collectiveAttributeService" );
        c.add( "eventService" );
        c.add( TriggerService.SERVICE_NAME );
        USERLOOKUP_BYPASS = Collections.unmodifiableCollection( c );
    }


    /**
     * Creates a new instance.
     * @
     */
    @SuppressWarnings( "unchecked" )
    public SimpleAuthenticator()
    {
        super( "simple" );
        
        credentialCache = new LRUMap( DEFAULT_CACHE_SIZE );
    }

    /**
     * Creates a new instance, with an initial cache size
     */
    @SuppressWarnings( "unchecked" )
    public SimpleAuthenticator( int cacheSize)
    {
        super( "simple" );

        credentialCache = new LRUMap( cacheSize > 0 ? cacheSize : DEFAULT_CACHE_SIZE );
    }

    
    /**
     * Looks up <tt>userPassword</tt> attribute of the entry whose name is the
     * value of {@link Context#SECURITY_PRINCIPAL} environment variable, and
     * authenticates a user with the plain-text password.
     */
    public LdapPrincipal authenticate( LdapDN principalDn, ServerContext ctx ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Authenticating {}", principalDn );
        }
        
        // ---- extract password from JNDI environment
        Object creds = ctx.getEnvironment().get( Context.SECURITY_CREDENTIALS );
        byte[] credentials = null;
        String principalNorm = principalDn.getNormName();

        if ( creds == null )
        {
            credentials = ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        else if ( creds instanceof String )
        {
            credentials = StringTools.getBytesUtf8( ( String ) creds );
        }
        else if ( creds instanceof byte[] )
        {
            credentials = (byte[])creds;
        }
        else
        {
            log.info( "Incorrect credentials stored in {}", Context.SECURITY_CREDENTIALS );
            throw new LdapAuthenticationException();
        }

        boolean credentialsMatch = false;
        LdapPrincipal principal = null;
        
        // Check to see if the password is stored in the cache
        synchronized( credentialCache )
        {
            principal = (LdapPrincipal)credentialCache.get( principalNorm );
        }
        
        if ( principal != null )
        {
            // Found ! Are the password equals ?
            credentialsMatch = Arrays.equals( credentials, principal.getUserPassword() );
        }
        else
        {
            // Not found :(...
            // Get the user password from the backend
            byte[] userPassword = lookupUserPassword( principalDn );
            
            // Deal with the special case where the user didn't enter a password
            // We will compare the empty array with the credentials. Sometime,
            // a user does not set a password. This is bad, but there is nothing
            // we can do against that, except education ...
            if ( userPassword == null )
            {
                userPassword = ArrayUtils.EMPTY_BYTE_ARRAY;
            }
    
            // Compare the passwords
            credentialsMatch = Arrays.equals( credentials, userPassword );
    
            if ( ! credentialsMatch )
            {
                // Check if password is stored as a message digest, i.e. one-way
                // encrypted
                String algorithm = getAlgorithmForHashedPassword( userPassword );
                
                if ( algorithm != null )
                {
                    try
                    {
                        // create a corresponding digested password from creds
                        String digestedCredits = createDigestedPassword( algorithm, credentials );
        
                        credentialsMatch = Arrays.equals( StringTools.getBytesUtf8( digestedCredits ), userPassword );
                    }
                    catch ( IllegalArgumentException e )
                    {
                        log.warn( "Exception during authentication", e.getMessage() );
                    }
                }
            }
            
            // Last, if we have found the credential, we have to store it in the cache
            if ( credentialsMatch )
            {
                principal = new LdapPrincipal( principalDn, AuthenticationLevel.SIMPLE, userPassword );

                // Now, update the local cache.
                synchronized( credentialCache )
                {
                    credentialCache.put( principalNorm, principal );
                }
            }
        }
        
        if ( credentialsMatch )
        {
            if ( IS_DEBUG )
            {
                log.debug( "{} Authenticated", principalDn );
            }
            
            return principal;
        }
        else
        {
            log.info( "Password not correct for user '{}'", principalDn );
            throw new LdapAuthenticationException();
        }
    }
    
    /**
     * Local function which request the password from the backend
     */
    private byte[] lookupUserPassword( LdapDN principalDn ) throws NamingException
    {
        // ---- lookup the principal entry's userPassword attribute
        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        Attributes userEntry;

        try
        {
            LookupServiceContext lookupContex  = new LookupServiceContext( new String[] { SchemaConstants.USER_PASSWORD_AT } );
            lookupContex.setDn( principalDn );
            
            userEntry = proxy.lookup( lookupContex, USERLOOKUP_BYPASS );

            if ( userEntry == null )
            {
                throw new LdapAuthenticationException( "Failed to lookup user for authentication: " + principalDn );
            }
        }
        catch ( Exception cause )
        {
            log.error( "Authentication error : " + cause.getMessage() );
            LdapAuthenticationException e = new LdapAuthenticationException();
            e.setRootCause( e );
            throw e;
        }

        Object userPassword;

        Attribute userPasswordAttr = userEntry.get( SchemaConstants.USER_PASSWORD_AT );

        // ---- assert that credentials match
        if ( userPasswordAttr == null )
        {
            userPassword = ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        else
        {
            userPassword = userPasswordAttr.get();

            if ( userPassword instanceof String )
            {
                userPassword = StringTools.getBytesUtf8( ( String ) userPassword );
            }
        }
        
        return ( byte[] ) userPassword;
    }

    /**
     * Get the algorithm of a password, which is stored in the form "{XYZ}...".
     * The method returns null, if the argument is not in this form. It returns
     * XYZ, if XYZ is an algorithm known to the MessageDigest class of
     * java.security.
     * 
     * @param password a byte[]
     * @return included message digest alorithm, if any
     */
    protected String getAlgorithmForHashedPassword( byte[] password ) throws IllegalArgumentException
    {
        String result = null;

        // Check if password arg is string or byte[]
        String sPassword = StringTools.utf8ToString( password );
        int rightParen = sPassword.indexOf( '}' );

        if ( ( sPassword != null ) && 
             ( sPassword.length() > 2 ) && 
             ( sPassword.charAt( 0 ) == '{' ) &&
             ( rightParen > -1 ) )
        {
            String algorithm = sPassword.substring( 1, rightParen );

            try
            {
                MessageDigest.getInstance( algorithm );
                result = algorithm;
            }
            catch ( NoSuchAlgorithmException e )
            {
                log.warn( "Unknown message digest algorithm in password: " + algorithm, e );
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
            MessageDigest digest = MessageDigest.getInstance( algorithm );
            
            // calculate hashed value of password
            byte[] fingerPrint = digest.digest( password );
            char[] encoded = Base64.encode( fingerPrint );

            // create return result of form "{alg}bbbbbbb"
            return '{' + algorithm + '}' + new String( encoded );
        }
        catch ( NoSuchAlgorithmException nsae )
        {
            log.error( "Cannot create a digested password for algorithm '{}'", algorithm );
            throw new IllegalArgumentException( nsae.getMessage() );
        }
    }

    /**
     * Remove the principal form the cache. This is used when the user changes
     * his password.
     */
    public void invalidateCache( LdapDN bindDn )
    {
        synchronized( credentialCache )
        {
            credentialCache.remove( bindDn.getNormName() );
        }
    }
}
