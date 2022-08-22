/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.integ;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.util.JndiUtils;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.ldap.LdapServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPException;


public class ServerIntegrationUtils extends IntegrationUtils
{
    /** The class logger */
    private static final Logger LOG = LoggerFactory.getLogger( ServerIntegrationUtils.class );
    private static final String CTX_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";

    private static final int DEFAULT_PORT = 10389;
    private static final String DEFAULT_ADMIN = ServerDNConstants.ADMIN_SYSTEM_DN;
    private static final String DEFAULT_PASSWORD = "secret";
    
    
    private static Hashtable<String, Object> setDefaultJNDIEnv( String factoryName, LdapServer ldapServer )
    {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        
        env.put( DirectoryService.JNDI_KEY, ldapServer.getDirectoryService() );
        env.put( Context.PROVIDER_URL, Network.ldapLoopbackUrl( ldapServer.getPort() ) );
        env.put( Context.SECURITY_PRINCIPAL,  ServerDNConstants.ADMIN_SYSTEM_DN );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, factoryName );

        return env;
    }
    

    /**
     * Creates a JNDI LdapContext with a connection over the wire using the 
     * SUN LDAP provider.  The connection is made using the administrative 
     * user as the principalDN.  The context is to the rootDSE.
     *
     * @param ldapServer the LDAP server to get the connection to
     * @return an LdapContext as the administrative user to the RootDSE
     * @throws NamingException if there are problems creating the context
     */
    public static LdapContext getWiredContext( LdapServer ldapServer ) throws NamingException
    {
        return getWiredContext( ldapServer, null );
    }


    /**
     * Creates a JNDI LdapContext with a connection over the wire using the 
     * SUN LDAP provider.  The connection is made using the administrative 
     * user as the principalDN.  The context is to the rootDSE.
     *
     * @param ldapServer the LDAP server to get the connection to
     * @param principalDn The user to use for authentication
     * @param password The user's password
     * @return an LdapContext as the administrative user to the RootDSE
     * @throws NamingException if there are problems creating the context
     */
    public static LdapContext getWiredContext( LdapServer ldapServer, String principalDn, String password )
        throws NamingException
    {
        LOG.debug( "Creating a wired context to local LDAP server on port {}", ldapServer.getPort() );
        Hashtable<String, Object> env = setDefaultJNDIEnv( CTX_FACTORY, ldapServer );
        env.put( Context.SECURITY_PRINCIPAL, principalDn );
        env.put( Context.SECURITY_CREDENTIALS, password );

        return new InitialLdapContext( env, null );
    }


    /**
     * Creates a JNDI LdapContext with a connection over the wire using the 
     * SUN LDAP provider.  The connection is made using the administrative 
     * user as the principalDN.  The context is to the rootDSE.
     *
     * @param ldapServer the LDAP server to get the connection to
     * @param controls The controls to use
     * @return an LdapContext as the administrative user to the RootDSE
     * @throws NamingException if there are problems creating the context
     */
    public static LdapContext getWiredContext( LdapServer ldapServer, Control[] controls ) throws NamingException
    {
        LOG.debug( "Creating a wired context to local LDAP server on port {}", ldapServer.getPort() );
        Hashtable<String, Object> env = setDefaultJNDIEnv( CTX_FACTORY, ldapServer );
        javax.naming.ldap.Control[] jndiControls;
        
        try
        {
            jndiControls = JndiUtils.toJndiControls( ldapServer.getDirectoryService().getLdapCodecService(), controls );
        }
        catch ( org.apache.directory.api.asn1.EncoderException ee )
        {
            throw new NamingException( ee.getMessage() );
        }
        
        return new InitialLdapContext( env, jndiControls );
    }


    /**
     * Creates a JNDI LdapContext with a connection over the wire using the 
     * SUN LDAP provider.  The connection is made using the administrative 
     * user as the principalDN.  The context is to the rootDSE.
     *
     * @param ldapServer the LDAP server to get the connection to
     * @return an LdapContext as the administrative user to the RootDSE
     * @throws NamingException if there are problems creating the context
     */
    public static LdapContext getWiredContextThrowOnRefferal( LdapServer ldapServer ) throws NamingException
    {
        LOG.debug( "Creating a wired context to local LDAP server on port {}", ldapServer.getPort() );
        Hashtable<String, Object> env = setDefaultJNDIEnv( CTX_FACTORY, ldapServer );
        env.put( Context.REFERRAL, "throw" );
        
        return new InitialLdapContext( env, null );
    }


    /**
     * Creates a JNDI LdapContext with a connection over the wire using the 
     * SUN LDAP provider.  The connection is made using the administrative 
     * user as the principalDN.  The context is to the rootDSE.
     *
     * @param ldapServer the LDAP server to get the connection to
     * @return an LdapContext as the administrative user to the RootDSE
     * @throws NamingException if there are problems creating the context
     */
    public static LdapContext getWiredContextRefferalIgnore( LdapServer ldapServer ) throws NamingException
    {
        LOG.debug( "Creating a wired context to local LDAP server on port {}", ldapServer.getPort() );
        Hashtable<String, Object> env = setDefaultJNDIEnv( CTX_FACTORY, ldapServer );
        env.put( Context.REFERRAL, "ignore" );
        
        return new InitialLdapContext( env, null );
    }


    /**
     * Creates a JNDI LdapContext with a connection over the wire using the 
     * SUN LDAP provider.  The connection is made using the administrative 
     * user as the principalDN.  The context is to the rootDSE.
     *
     * @param ldapServer the LDAP server to get the connection to
     * @return an LdapContext as the administrative user to the RootDSE
     * @throws NamingException if there are problems creating the context
     */
    public static LdapContext getWiredContextFollowOnRefferal( LdapServer ldapServer ) throws NamingException
    {
        LOG.debug( "Creating a wired context to local LDAP server on port {}", ldapServer.getPort() );
        Hashtable<String, Object> env = setDefaultJNDIEnv( CTX_FACTORY, ldapServer );
        env.put( Context.REFERRAL, "follow" );
        
        return new InitialLdapContext( env, null );
    }


    public static LdapConnection getWiredConnection( LdapServer ldapServer ) throws LdapException
    {
        String testServer = System.getProperty( "ldap.test.server", null );

        if ( testServer == null )
        {
            return getWiredConnection( ldapServer, ServerDNConstants.ADMIN_SYSTEM_DN, "secret" );
        }

        LOG.debug( "ldap.test.server = {}", testServer );

        String admin = System.getProperty( testServer + ".admin", DEFAULT_ADMIN );
        LOG.debug( "{}.admin = {}", testServer, admin );

        String password = System.getProperty( testServer + ".password", DEFAULT_PASSWORD );
        LOG.debug( "{}.password = {}", testServer, password );

        String host = System.getProperty( testServer + ".host", Network.LOOPBACK_HOSTNAME );
        LOG.debug( "{}.host = {}", testServer, host );

        int port = Integer.parseInt( System.getProperty( testServer + ".port", Integer.toString( DEFAULT_PORT ) ) );
        LOG.debug( "{}.port = {}", testServer, port );

        LdapConnection conn = new LdapNetworkConnection( host, port );
        conn.bind( admin, password );

        return conn;
    }


    public static LDAPConnection getNsdkWiredConnection( LdapServer ldapServer ) throws LDAPException
    {
        String testServer = System.getProperty( "ldap.test.server", null );

        if ( testServer == null )
        {
            return getNsdkWiredConnection( ldapServer, ServerDNConstants.ADMIN_SYSTEM_DN, "secret" );
        }

        LOG.debug( "ldap.test.server = {}", testServer );

        String admin = System.getProperty( testServer + ".admin", DEFAULT_ADMIN );
        LOG.debug( "{}.admin = {}", testServer, admin );

        String password = System.getProperty( testServer + ".password", DEFAULT_PASSWORD );
        LOG.debug( "{}.password = {}", testServer, password );

        String host = System.getProperty( testServer + ".host", Network.LOOPBACK_HOSTNAME );
        LOG.debug( "{}.host = {}", testServer, host );

        int port = Integer.parseInt( System.getProperty( testServer + ".port", Integer.toString( DEFAULT_PORT ) ) );
        LOG.debug( "{}.port = {}", testServer, port );

        LDAPConnection conn = new LDAPConnection();
        conn.connect( 3, host, port, admin, password );

        return conn;
    }


    /**
     * Gets a LDAP connection instance on a server, authenticating a user.
     * 
     * @param ldapServer The server we want to connect to
     * @param principalDn The user's DN
     * @param password The user's password
     * @return A LdapConnection instance if we got one
     * @throws LdapException If the connection cannot be created
     */
    public static LdapConnection getWiredConnection( LdapServer ldapServer, String principalDn, String password )
        throws LdapException
    {
        LdapConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );
        connection.bind( principalDn, password );

        return connection;
    }


    /**
     * Gets a LDAP connection instance on a server. We won't bind on the server.
     * 
     * @param ldapServer The server we want to connect to
     * @return A LdapConnection instance if we got one
     * @throws LdapException If the connection cannot be created
     */
    public static LdapConnection getLdapConnection( LdapServer ldapServer )
    {
        return new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );
    }


    /**
     * Gets a LDAP connection instance on a server, authenticating a user.
     * 
     * @param ldapServer The server we want to connect to
     * @param principalDn The user's DN
     * @param password The user's password
     * @return A LdapConnection instance if we got one
     * @throws LDAPException If the connection cannot be created
     */
    public static LDAPConnection getNsdkWiredConnection( LdapServer ldapServer, String principalDn, String password )
        throws LDAPException
    {
        LDAPConnection connection = new LDAPConnection();
        connection.connect( 3, Network.LOOPBACK_HOSTNAME, ldapServer.getPort(), principalDn, password );

        return connection;
    }


    /**
     * Gets a LDAP connection instance on a server. We will bind as Admin
     * 
     * @param ldapServer The server we want to connect to
     * @return A LdapConnection instance if we got one
     * @throws LdapException If the connection cannot be created
     */
    public static LdapConnection getAdminConnection( LdapServer ldapServer ) throws LdapException
    {
        LdapConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );
        connection.bind( ServerDNConstants.ADMIN_SYSTEM_DN, "secret" );

        return connection;
    }
}
