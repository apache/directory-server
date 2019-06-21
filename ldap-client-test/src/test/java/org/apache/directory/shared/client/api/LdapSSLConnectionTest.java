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


import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

import javax.net.ssl.X509TrustManager;

import org.apache.directory.api.ldap.codec.api.SchemaBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapTlsHandshakeException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapClientTrustStoreManager;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.apache.directory.ldap.client.api.exception.InvalidConnectionException;
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
     * Test for DIRAPI-342: Unbind breaks connection
     */
    @Test
    public void testSSLConnectAndBindAndUnbindLoop() throws Exception
    {
        try ( LdapNetworkConnection connection = new LdapNetworkConnection( sslConfig ) )
        {
            for ( int i = 0; i < 10; i++ )
            {
                connection.connect();
                assertTrue( connection.isConnected() );
                assertTrue( connection.isSecured() );
                assertFalse( connection.isAuthenticated() );

                connection.bind( "uid=admin,ou=system", "secret" );
                assertTrue( connection.isConnected() );
                assertTrue( connection.isSecured() );
                assertTrue( connection.isAuthenticated() );

                connection.unBind();
                assertFalse( connection.isSecured() );
                assertFalse( connection.isConnected() );
                assertFalse( connection.isAuthenticated() );
            }
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


    /**
     * Test for DIRAPI-342: Unbind breaks connection
     */
    @Test
    public void testStartTLSConnectAndBindAndUnbindLoop() throws Exception
    {
        try ( LdapNetworkConnection connection = new LdapNetworkConnection( tlsConfig ) )
        {
            for ( int i = 0; i < 10; i++ )
            {
                connection.startTls();
                assertTrue( connection.isConnected() );
                assertTrue( connection.isSecured() );
                assertFalse( connection.isAuthenticated() );

                connection.bind( "uid=admin,ou=system", "secret" );
                assertTrue( connection.isConnected() );
                assertTrue( connection.isSecured() );
                assertTrue( connection.isAuthenticated() );

                connection.unBind();
                assertFalse( connection.isConnected() );
                assertFalse( connection.isSecured() );
                assertFalse( connection.isAuthenticated() );
            }
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


    @Test(expected = InvalidConnectionException.class)
    public void testSslConnectionWrongHost() throws LdapException, IOException
    {
        sslConfig.setLdapHost( "notexisting" );
        try ( LdapNetworkConnection connection = new LdapNetworkConnection( sslConfig ) )
        {
            connection.setTimeOut( 10000L );
            connection.connect();
        }
        catch ( Exception e )
        {
            assertThat( e, is( instanceOf( InvalidConnectionException.class ) ) );
            assertThat( e.getMessage(), containsString( "ERR_04121_CANNOT_RESOLVE_HOSTNAME" ) );
            assertThat( e.getMessage(), containsString( "notexisting" ) );
            throw e;
        }
    }


    @Test(expected = InvalidConnectionException.class)
    public void testStartTlsConnectionWrongHost() throws LdapException, IOException
    {
        tlsConfig.setLdapHost( "notexisting" );
        try ( LdapNetworkConnection connection = new LdapNetworkConnection( tlsConfig ) )
        {
            connection.setTimeOut( 10000L );
            connection.startTls();
        }
        catch ( Exception e )
        {
            assertThat( e, is( instanceOf( InvalidConnectionException.class ) ) );
            assertThat( e.getMessage(), containsString( "ERR_04121_CANNOT_RESOLVE_HOSTNAME" ) );
            assertThat( e.getMessage(), containsString( "notexisting" ) );
            throw e;
        }
    }


    @Test(expected = InvalidConnectionException.class)
    public void testSslConnectionWrongPort() throws LdapException, IOException
    {
        sslConfig.setLdapPort( 123 );
        try ( LdapNetworkConnection connection = new LdapNetworkConnection( sslConfig ) )
        {
            connection.setTimeOut( 10000L );
            connection.connect();
        }
        catch ( Exception e )
        {
            assertThat( e, is( instanceOf( InvalidConnectionException.class ) ) );
            assertThat( e.getMessage(), containsString( "ERR_04110_CANNOT_CONNECT_TO_SERVER" ) );
            assertThat( e.getMessage(), containsString( "Connection refused" ) );
            throw e;
        }
    }


    @Test(expected = InvalidConnectionException.class)
    public void testStartTlsConnectionWrongPort() throws LdapException, IOException
    {
        tlsConfig.setLdapPort( 123 );
        try ( LdapNetworkConnection connection = new LdapNetworkConnection( tlsConfig ) )
        {
            connection.setTimeOut( 10000L );
            connection.startTls();
        }
        catch ( Exception e )
        {
            assertThat( e, is( instanceOf( InvalidConnectionException.class ) ) );
            assertThat( e.getMessage(), containsString( "ERR_04110_CANNOT_CONNECT_TO_SERVER" ) );
            assertThat( e.getMessage(), containsString( "Connection refused" ) );
            throw e;
        }
    }


    @Test(expected = LdapTlsHandshakeException.class)
    public void testSslConnectionNonSslPort() throws LdapException, IOException
    {
        sslConfig.setLdapPort( getLdapServer().getPort() );
        try ( LdapNetworkConnection connection = new LdapNetworkConnection( sslConfig ) )
        {
            connection.setTimeOut( 10000L );
            connection.connect();
        }
        catch ( Exception e )
        {
            assertThat( e, is( instanceOf( LdapTlsHandshakeException.class ) ) );
            assertThat( e.getMessage(), containsString( "ERR_04120_TLS_HANDSHAKE_ERROR" ) );
            assertThat( e.getMessage(), containsString( "plaintext connection" ) );
            throw e;
        }
    }


    @Test(expected = LdapException.class)
    public void testStartTlsConnectionSslPort() throws LdapException, IOException
    {
        tlsConfig.setLdapPort( getLdapServer().getPortSSL() );
        try ( LdapNetworkConnection connection = new LdapNetworkConnection( tlsConfig ) )
        {
            connection.setTimeOut( 10000L );
            connection.startTls();
        }
        catch ( Exception e )
        {
            assertThat( e, is( instanceOf( LdapException.class ) ) );
            throw e;
        }
    }


    @Test(expected = LdapException.class)
    public void testSslConnectionTimeout() throws LdapException, IOException
    {
        try
        {
            /*
             * Create a server socket that doesn't accept any connection,
             * should lead to client-side timeout.
             */
            try ( ServerSocket ss = new ServerSocket( 0, 1 ) )
            {
                int port = ss.getLocalPort();
                sslConfig.setLdapPort( port );
                try ( LdapNetworkConnection connection = new LdapNetworkConnection( sslConfig ) )
                {
                    connection.setTimeOut( 10000L );
                    connection.connect();
                }
            }
        }
        catch ( Exception e )
        {
            assertThat( e, is( instanceOf( LdapException.class ) ) );
            assertThat( e.getMessage(), containsString( "ERR_04170_TIMEOUT_OCCURED" ) );
            assertThat( e.getMessage(), containsString( "TimeOut occurred" ) );
            throw e;
        }
    }


    @Test(expected = LdapException.class)
    public void testStartTlsConnectionTimeout() throws LdapException, IOException
    {
        try
        {
            /*
             * Create a server socket that doesn't accept any connection,
             * should lead to client-side timeout.
             */
            try ( ServerSocket ss = new ServerSocket( 0, 1 ) )
            {
                int port = ss.getLocalPort();
                tlsConfig.setLdapPort( port );
                try ( LdapNetworkConnection connection = new LdapNetworkConnection( tlsConfig ) )
                {
                    connection.setTimeOut( 10000L );
                    connection.startTls();
                }
            }
        }
        catch ( Exception e )
        {
            assertThat( e, is( instanceOf( LdapException.class ) ) );
            assertThat( e.getMessage(), containsString( "ERR_04170_TIMEOUT_OCCURED" ) );
            assertThat( e.getMessage(), containsString( "TimeOut occurred" ) );
            throw e;
        }
    }

}
