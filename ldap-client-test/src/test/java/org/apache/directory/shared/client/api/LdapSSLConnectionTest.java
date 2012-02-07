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

import java.io.IOException;
import java.util.List;

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
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.plain.PlainMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.shared.ldap.codec.api.DefaultBinaryAttributeDetector;
import org.apache.directory.shared.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;
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
    public void setup()
    {
        sslConfig = new LdapConnectionConfig();
        sslConfig.setLdapHost( "localhost" );
        sslConfig.setUseSsl( true );
        sslConfig.setLdapPort( getLdapServer().getPortSSL() );
        sslConfig.setTrustManagers( new NoVerificationTrustManager() );
        sslConfig.setBinaryAttributeDetector( new DefaultBinaryAttributeDetector(
                ldapServer.getDirectoryService().getSchemaManager() ) );

        tlsConfig = new LdapConnectionConfig();
        tlsConfig.setLdapHost( "localhost" );
        tlsConfig.setLdapPort( getLdapServer().getPort() );
        tlsConfig.setTrustManagers( new NoVerificationTrustManager() );
        tlsConfig.setBinaryAttributeDetector( new DefaultBinaryAttributeDetector(
            ldapServer.getDirectoryService().getSchemaManager() ) );
    }


    /**
     * Test a successful bind request
     *
     * @throws IOException
     */
    @Test
    public void testBindRequest() throws Exception
    {
        LdapConnection connection = null;
        try
        {
            connection = new LdapNetworkConnection( sslConfig );
            connection.bind( "uid=admin,ou=system", "secret" );

            assertTrue( connection.isAuthenticated() );
        }
        finally
        {
            if ( connection != null )
            {
                connection.close();
            }
        }
    }


    @Test
    public void testGetSupportedControls() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( sslConfig );

        Dn dn = new Dn( "uid=admin,ou=system" );
        connection.bind( dn.getName(), "secret" );

        List<String> controlList = connection.getSupportedControls();
        assertNotNull( controlList );
        assertFalse( controlList.isEmpty() );

        connection.close();
    }


    /**
     * Test a successful bind request after setting up TLS
     *
     * @throws IOException
     */
    @Test
    public void testStartTLSBindRequest() throws Exception
    {
        LdapNetworkConnection connection = null;
        try
        {
            connection = new LdapNetworkConnection( tlsConfig );
            connection.connect();
            connection.startTls();
            connection.bind( "uid=admin,ou=system", "secret" );

            assertTrue( connection.isAuthenticated() );

            connection.unBind();
        }
        finally
        {
            if ( connection != null )
            {
                connection.close();
            }
        }
    }


    @Test
    public void testGetSupportedControlsWithStartTLS() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( tlsConfig );
        connection.connect();
        connection.startTls();

        Dn dn = new Dn( "uid=admin,ou=system" );
        connection.bind( dn.getName(), "secret" );

        List<String> controlList = connection.getSupportedControls();
        assertNotNull( controlList );
        assertFalse( controlList.isEmpty() );

        connection.close();
    }


    @Test(expected = LdapException.class)
    public void testFailsStartTLSWhenSSLIsInUse() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( tlsConfig );
        tlsConfig.setUseSsl( true );
        tlsConfig.setLdapPort( ldapServer.getPortSSL() );
        connection.connect();
        connection.startTls();
    }


    @Test(expected = InvalidConnectionException.class)
    public void testStallingSsl() throws Exception
    {
        LdapConnectionConfig sslConfig = new LdapConnectionConfig();
        sslConfig.setLdapHost( "localhost" );
        sslConfig.setUseSsl( true );
        sslConfig.setLdapPort( getLdapServer().getPortSSL() );
        //sslConfig.setTrustManagers( new NoVerificationTrustManager() );

        LdapNetworkConnection connection = new LdapNetworkConnection( sslConfig );

        // We should get an exception here, as we don't have a trustManager defined
        connection.bind();
    }
}
