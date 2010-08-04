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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.message.BindResponse;
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
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.name.DN;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the LdapConnection class with SSL enabled
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateLdapServer (
    transports =
    {
        @CreateTransport( protocol = "LDAP" ),
        @CreateTransport( protocol = "LDAPS" )
    },
    saslHost="localhost",
    saslMechanisms =
    {
        @SaslMechanism( name=SupportedSaslMechanisms.PLAIN, implClass=PlainMechanismHandler.class ),
        @SaslMechanism( name=SupportedSaslMechanisms.CRAM_MD5, implClass=CramMd5MechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.DIGEST_MD5, implClass=DigestMd5MechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.GSSAPI, implClass=GssapiMechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.NTLM, implClass=NtlmMechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.GSS_SPNEGO, implClass=NtlmMechanismHandler.class)
    },
    extendedOpHandlers =
    {
        StoredProcedureExtendedOperationHandler.class
    })
public class LdapSSLConnectionTest extends AbstractLdapTestUnit
{
    private static LdapConnectionConfig config;


    @Before
    public void setup()
    {
        X509TrustManager X509 = new X509TrustManager()
        {
            public void checkClientTrusted( X509Certificate[] x509Certificates, String s ) throws CertificateException
            {
            }

            public void checkServerTrusted( X509Certificate[] x509Certificates, String s ) throws CertificateException
            {
            }

            public X509Certificate[] getAcceptedIssuers()
            {
                return new X509Certificate[0];
            }
        };

        config = new LdapConnectionConfig();
        config.setLdapHost( "localhost" );
        config.setUseSsl( true );
        config.setLdapPort( ldapServer.getPortSSL() );
        config.setTrustManagers( new TrustManager[]{ X509 } );
    }


    /**
     * Test a successful bind request
     *
     * @throws IOException
     */
    @Test
    public void testBindRequest()
    {
        LdapConnection connection = null;
        try
        {
            connection = new LdapNetworkConnection( config );
            BindResponse bindResponse = connection.bind( "uid=admin,ou=system", "secret" );

            assertNotNull( bindResponse );

            connection.unBind();
        }
        catch ( Exception le )
        {
            le.printStackTrace();
            fail();
        }
    }


    @Test
    public void testGetSupportedControls() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( config );

        DN dn = new DN( "uid=admin,ou=system" );
        connection.bind( dn.getName(), "secret" );

        List<String> controlList = connection.getSupportedControls();
        assertNotNull( controlList );
        assertFalse( controlList.isEmpty() );
    }
}
