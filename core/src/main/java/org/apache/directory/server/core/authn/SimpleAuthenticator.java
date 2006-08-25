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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
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
    private static final Collection USERLOOKUP_BYPASS;

    private WeakHashMap credentialCache = new WeakHashMap( 1000 );
    
    static
    {
        Set c = new HashSet();
        c.add( "normalizationService" );
        c.add( "collectiveAttributeService" );
        c.add( "authenticationService" );
        c.add( "authorizationService" );
        c.add( "defaultAuthorizationService" );
        c.add( "schemaService" );
        c.add( "subentryService" );
        c.add( "operationalAttributeService" );
        c.add( "eventService" );
        USERLOOKUP_BYPASS = Collections.unmodifiableCollection( c );
    }


    /**
     * Creates a new instance.
     */
    public SimpleAuthenticator()
    {
        super( "simple" );
    }

    
    /**
     * Looks up <tt>userPassword</tt> attribute of the entry whose name is the
     * value of {@link Context#SECURITY_PRINCIPAL} environment variable, and
     * authenticates a user with the plain-text password.
     */
    public LdapPrincipal authenticate( LdapDN principalDn, ServerContext ctx ) throws NamingException
    {
        // ---- extract password from JNDI environment

        Object creds = ctx.getEnvironment().get( Context.SECURITY_CREDENTIALS );

        if ( creds == null )
        {
            creds = ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        else if ( creds instanceof String )
        {
            creds = StringTools.getBytesUtf8( ( String ) creds );
        }

        byte[] userPassword = null;
        if ( credentialCache.containsKey( principalDn.getNormName() ) )
        {
            userPassword = ( byte[] ) credentialCache.get( principalDn.getNormName() );
        }
        else
        {
            userPassword = lookupUserPassword( principalDn );
        }

        boolean credentialsMatch = false;

        // Check if password is stored as a message digest, i.e. one-way
        // encrypted
        if ( this.isPasswordOneWayEncrypted( userPassword ) )
        {
            try
            {
                // create a corresponding digested password from creds
                String algorithm = this.getAlgorithmForHashedPassword( userPassword );
                String digestedCredits = this.createDigestedPassword( algorithm, creds );

                credentialsMatch = ArrayUtils.isEquals( StringTools.getBytesUtf8( digestedCredits ), userPassword );
            }
            catch ( NoSuchAlgorithmException nsae )
            {
                log.warn( "Password stored with unknown algorithm.", nsae );
            }
            catch ( IllegalArgumentException e )
            {
                log.warn( "Exception during authentication", e );
            }
        }
        else
        {
            // password is not stored one-way encrypted
            credentialsMatch = ArrayUtils.isEquals( creds, userPassword );
        }

        if ( credentialsMatch )
        {
            LdapPrincipal principal = new LdapPrincipal( principalDn, AuthenticationLevel.SIMPLE );
            credentialCache.put( principalDn.getNormName(), userPassword );
            return principal;
        }
        else
        {
            throw new LdapAuthenticationException();
        }
    }
    
    
    protected byte[] lookupUserPassword( LdapDN principalDn ) throws NamingException
    {
        // ---- lookup the principal entry's userPassword attribute

        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        Attributes userEntry;

        try
        {
            userEntry = proxy.lookup( principalDn, new String[]
                { "userPassword" }, USERLOOKUP_BYPASS );

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

        Attribute userPasswordAttr = userEntry.get( "userPassword" );

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
     * Checks if the argument is one-way encryped. If it is a string or a
     * byte-array which looks like "{XYZ}...", and XYZ is a known lessage
     * digest, the method returns true. The method does not throw an exception
     * otherwise, e.g. if the algorithm XYZ is not known to the runtime.
     * 
     * @param password
     *            agument, either a string or a byte-array
     * @return true, if the value is a digested password with algorithm included
     */
    protected boolean isPasswordOneWayEncrypted( Object password )
    {
        boolean result = false;
        try
        {
            String algorithm = getAlgorithmForHashedPassword( password );
            result = ( algorithm != null );
        }
        catch ( IllegalArgumentException ignored )
        {
        }
        return result;
    }


    /**
     * Get the algorithm of a password, which is stored in the form "{XYZ}...".
     * The method returns null, if the argument is not in this form. It returns
     * XYZ, if XYZ is an algorithm known to the MessageDigest class of
     * java.security.
     * 
     * @param password,
     *            either a String or a byte[]
     * @return included message digest alorithm, if any
     */
    protected String getAlgorithmForHashedPassword( Object password ) throws IllegalArgumentException
    {
        String result = null;

        // Check if password arg is string or byte[]
        String sPassword = null;
        if ( password instanceof byte[] )
        {
            sPassword = new String( ( byte[] ) password );
        }
        else if ( password instanceof String )
        {
            sPassword = ( String ) password;
        }
        else
        {
            throw new IllegalArgumentException( "password is neither a String nor a byte-Array." );
        }

        if ( sPassword != null && sPassword.length() > 2 && sPassword.charAt( 0 ) == '{'
            && sPassword.indexOf( '}' ) > -1 )
        {
            int algPosEnd = sPassword.indexOf( '}' );
            String algorithm = sPassword.substring( 1, algPosEnd );
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
     *            password value, either a string or a byte[]
     * 
     * @return a digested password, which looks like
     *         {SHA}LhkDrSoM6qr0fW6hzlfOJQW61tc=
     * 
     * @throws IllegalArgumentException
     *             if password is neither a String nor a byte[], or algorithm is
     *             not known to java.security.MessageDigest class
     */
    protected String createDigestedPassword( String algorithm, Object password ) throws NoSuchAlgorithmException,
        IllegalArgumentException
    {
        // Check if password arg is string or byte[]
        byte[] data = null;
        if ( password instanceof byte[] )
        {
            data = ( byte[] ) password;
        }
        else if ( password instanceof String )
        {
            data = StringTools.getBytesUtf8( ( String ) password );
        }
        else
        {
            throw new IllegalArgumentException( "password is neither a String nor a byte-Array." );
        }

        // create message digest object
        MessageDigest digest = null;
        try
        {
            digest = MessageDigest.getInstance( algorithm );
        }
        catch ( NoSuchAlgorithmException nsae )
        {
            throw new IllegalArgumentException( nsae.getMessage() );
        }

        // calculate hashed value of password
        byte[] fingerPrint = digest.digest( data );
        char[] encoded = Base64.encode( fingerPrint );

        // create return result of form "{alg}bbbbbbb"
        StringBuffer result = new StringBuffer();
        result.append( '{' );
        result.append( algorithm );
        result.append( '}' );
        result.append( encoded );

        return result.toString();
    }


    public void invalidateCache( LdapDN bindDn )
    {
        credentialCache.remove( bindDn.getNormName() );
    }
}
