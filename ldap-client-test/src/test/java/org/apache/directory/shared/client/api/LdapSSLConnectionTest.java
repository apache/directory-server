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
package org.apache.directory.shared.client.api;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.net.ssl.X509TrustManager;

import org.apache.directory.api.ldap.codec.api.SchemaBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapClientTrustStoreManager;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the LdapConnection class by enabling SSL and StartTLS one after the other
 * (using both in the same test class saves the time required to start/stop another server for StartTLS)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */

@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    {
        @CreateTransport(protocol = "LDAP"),
        @CreateTransport(protocol = "LDAPS")
},
    saslHost = "localhost",
    saslMechanisms =
        {
            @SaslMechanism(name = SupportedSaslMechanisms.PLAIN, implClass = PlainMechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.CRAM_MD5, implClass = CramMd5MechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.DIGEST_MD5, implClass = DigestMd5MechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.GSSAPI, implClass = GssapiMechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.NTLM, implClass = NtlmMechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.GSS_SPNEGO, implClass = NtlmMechanismHandler.class)
    },
    extendedOpHandlers =
        {
            StartTlsHandler.class
    })
public class LdapSSLConnectionTest extends AbstractLdapTestUnit
{
    private LdapConnectionConfig sslConfig;

    private LdapConnectionConfig tlsConfig;


    @Before
    public void setup() throws Exception
    {
        sslConfig = new LdapConnectionConfig();
        sslConfig.setLdapHost( Network.LOOPBACK_HOSTNAME );
        sslConfig.setUseSsl( true );
        sslConfig.setLdapPort( getLdapServer().getPortSSL() );
        sslConfig.setTrustManagers( new NoVerificationTrustManager() );
        sslConfig.setBinaryAttributeDetector( new SchemaBinaryAttributeDetector(
                ldapServer.getDirectoryService().getSchemaManager() ) );

        tlsConfig = new LdapConnectionConfig();
        tlsConfig.setLdapHost( Network.LOOPBACK_HOSTNAME );
        tlsConfig.setLdapPort( getLdapServer().getPort() );
        tlsConfig.setTrustManagers( new NoVerificationTrustManager() );
        tlsConfig.setBinaryAttributeDetector( new SchemaBinaryAttributeDetector(
            ldapServer.getDirectoryService().getSchemaManager() ) );
    }


    /**
     * Test a successful bind request
     *
     * @throws IOException
     */
    @Test
    public void testBindRequestSSLConfig() throws Exception
    {
        try ( LdapNetworkConnection connection = new LdapNetworkConnection( sslConfig ) )
        {
            connection.bind( "uid=admin,ou=system", "secret" );

            assertTrue( connection.getConfig().isUseSsl() );
            assertTrue( connection.isAuthenticated() );
            assertTrue( connection.isSecured() );
        }
    }


    /**
     * Test a successful bind request
     *
     * @throws IOException
     */
    @Test
    public void testBindRequestSSLAuto() throws Exception
    {
        sslConfig.setTrustManagers( new X509TrustManager[] { new NoVerificationTrustManager() } );

        try ( LdapNetworkConnection connection = 
            new LdapNetworkConnection( sslConfig ) )
        {
            connection.bind( "uid=admin,ou=system", "secret" );
            assertTrue( connection.getConfig().isUseSsl() );

            assertTrue( connection.isAuthenticated() );
            assertTrue( connection.isSecured() );
        }
    }


    /**
     * Test a successful bind request
     *
     * @throws IOException
     */
    @Test
    public void testBindRequestSSLWithTrustManager() throws Exception
    {
        try ( LdapNetworkConnection connection = 
            new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPortSSL(), 
                new LdapClientTrustStoreManager( ldapServer.getKeystoreFile(), new char[] {'s', 'e', 'c', 'r', 'e', 't' }, null, true ) ) )
        {
            connection.bind( "uid=admin,ou=system", "secret" );
            
            assertTrue( connection.getConfig().isUseSsl() );
            assertTrue( connection.isAuthenticated() );
            assertTrue( connection.isSecured() );
        }
    }


    @Test
    public void testGetSupportedControls() throws Exception
    {
        try ( LdapConnection connection = new LdapNetworkConnection( sslConfig ) )
        {    
            Dn dn = new Dn( "uid=admin,ou=system" );
            connection.bind( dn.getName(), "secret" );
    
            List<String> controlList = connection.getSupportedControls();
            assertNotNull( controlList );
            assertFalse( controlList.isEmpty() );
        }
    }


    /**
     * Test a successful bind request after setting up TLS
     *
     * @throws IOException
     */
    @Test
    public void testStartTLSBindRequest() throws Exception
    {
        try ( LdapNetworkConnection connection = new LdapNetworkConnection( tlsConfig ) )
        {
            tlsConfig.setUseTls( true );
            connection.connect();

            connection.bind( "uid=admin,ou=system", "secret" );
            assertTrue( connection.isAuthenticated() );

            // try multiple binds with startTLS DIRAPI-173
            connection.bind( "uid=admin,ou=system", "secret" );
            assertTrue( connection.isAuthenticated() );
            
            connection.bind( "uid=admin,ou=system", "secret" );
            assertTrue( connection.isAuthenticated() );
            assertTrue( connection.isSecured() );

            connection.unBind();
        }
    }


    /**
     * Test a request before setting up TLS
     *
     * @throws IOException
     */
    @Test
    public void testStartTLSAfterBind() throws Exception
    {
        tlsConfig.setTrustManagers( new X509TrustManager[] { new NoVerificationTrustManager() } );

        try ( LdapNetworkConnection connection = 
            new LdapNetworkConnection( tlsConfig ) )
        {
            connection.connect();

            connection.bind( "uid=admin,ou=system", "secret" );
            assertFalse( connection.isSecured() );

            Entry rootDse = connection.getRootDse( "*", "+" );
            
            assertNotNull( rootDse );

            // startTLS
            connection.startTls();
            
            // try multiple binds with startTLS DIRAPI-173
            assertTrue( connection.isSecured() );

            Entry admin = connection.lookup( "uid=admin,ou=system" );

            assertNotNull( admin );
            assertEquals( "uid=admin,ou=system", admin.getDn().getName() );

            connection.unBind();
        }
    }


    /**
     * Test the startTLS call
     *
     * @throws IOException
     */
    @Test
    public void testStartTLS() throws Exception
    {
        tlsConfig.setTrustManagers( new X509TrustManager[] { new LdapClientTrustStoreManager( ldapServer.getKeystoreFile(), new char[] {'s', 'e', 'c', 'r', 'e', 't' }, null, true ) } );

        try ( LdapNetworkConnection connection = 
            new LdapNetworkConnection( tlsConfig ) )
        {
            assertFalse( connection.isConnected() );
            
            // Send the startTLS extended operation
            connection.startTls();
            assertTrue( connection.isSecured() );

            connection.bind( "uid=admin,ou=system", "secret" );
            assertTrue( connection.isSecured() );

            Entry admin = connection.lookup( "uid=admin,ou=system" );

            assertNotNull( admin );
            assertEquals( "uid=admin,ou=system", admin.getDn().getName() );

            connection.unBind();
        }
    }




    /**
     * Test the startTLS call using a config
     *
     * @throws IOException
     */
    @Test
    public void testStartTLSWithConfig() throws Exception
    {
        try ( LdapNetworkConnection connection = 
            new LdapNetworkConnection( tlsConfig ) )
        {
            assertFalse( connection.isConnected() );
            
            // Connect
            connection.connect();
            assertFalse( connection.isSecured() );

            connection.bind( "uid=admin,ou=system", "secret" );
            Entry admin = connection.lookup( "uid=admin,ou=system" );
            
            assertNotNull( admin );
            assertEquals( "uid=admin,ou=system", admin.getDn().getName() );
            assertFalse( connection.isSecured() );

            // Send the startTLS extended operation
            connection.startTls();
            assertTrue( connection.isSecured() );

            admin = connection.lookup( "uid=admin,ou=system" );
            assertNotNull( admin );
            assertEquals( "uid=admin,ou=system", admin.getDn().getName() );

            connection.unBind();
        }
    }


    @Test
    public void testGetSupportedControlsWithStartTLS() throws Exception
    {
        try ( LdapNetworkConnection connection = new LdapNetworkConnection( tlsConfig ) )
        {
            tlsConfig.setUseTls( true );
            connection.connect();
    
            Dn dn = new Dn( "uid=admin,ou=system" );
            connection.bind( dn.getName(), "secret" );
    
            List<String> controlList = connection.getSupportedControls();
            assertNotNull( controlList );
            assertFalse( controlList.isEmpty() );
        }
    }


    @Test(expected = LdapException.class)
    public void testFailsStartTLSWhenSSLIsInUse() throws Exception
    {
        try ( LdapNetworkConnection connection = new LdapNetworkConnection( tlsConfig ) )
        {
            tlsConfig.setUseSsl( true );
            tlsConfig.setLdapPort( ldapServer.getPortSSL() );
            connection.connect();
            connection.startTls();
        }
    }


    @Test(expected = LdapAuthenticationException.class)
    public void testStallingSsl() throws Exception
    {
        LdapConnectionConfig sslConfig = new LdapConnectionConfig();
        sslConfig.setLdapHost( Network.LOOPBACK_HOSTNAME );
        sslConfig.setUseSsl( true );
        sslConfig.setLdapPort( getLdapServer().getPortSSL() );
        sslConfig.setTrustManagers( new X509TrustManager[] { new NoVerificationTrustManager() } );


        try ( LdapNetworkConnection connection = new LdapNetworkConnection( sslConfig ) )
        {
            // We should get an exception here, as we don't have a trustManager defined
            connection.bind();
            
            assertTrue( connection.getConfig().isUseSsl() );
            assertTrue( connection.isConnected() );
        }
    }
}
