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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.integ.LdapServerFactory;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.SimpleMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.ldap.client.api.LdapConnection;
import org.apache.directory.shared.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.shared.ldap.client.api.exception.LdapException;
import org.apache.directory.shared.ldap.client.api.messages.BindResponse;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the LdapConnection class with SSL enabled
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
@Factory ( LdapSSLConnectionTest.Factory.class )
public class LdapSSLConnectionTest
{
    /** The server instance */
    public static LdapServer ldapServer;

    private LdapConnectionConfig config;
    
    public static class Factory implements LdapServerFactory
    {
        public LdapServer newInstance() throws Exception
        {
            DirectoryService service = new DefaultDirectoryService();
            IntegrationUtils.doDelete( service.getWorkingDirectory() );
            service.getChangeLog().setEnabled( true );
            service.setShutdownHookEnabled( false );

            // change the working directory to something that is unique
            // on the system and somewhere either under target directory
            // or somewhere in a temp area of the machine.

            LdapServer ldapServer = new LdapServer();
            ldapServer.setDirectoryService( service );
            int port = AvailablePortFinder.getNextAvailable( 1024 );
            TcpTransport tcpTransport = new TcpTransport( port );
            int portSSL = port + 1;
            TcpTransport tcpTransportSsl = new TcpTransport( portSSL );
            tcpTransportSsl.enableSSL( true );
            ldapServer.setTransports( tcpTransport, tcpTransportSsl );
            ldapServer.setEnabled( true );
            ldapServer.setConfidentialityRequired( true );
            ldapServer.addExtendedOperationHandler( new StoredProcedureExtendedOperationHandler() );

            // Setup SASL Mechanisms
            
            Map<String, MechanismHandler> mechanismHandlerMap = new HashMap<String,MechanismHandler>();
            mechanismHandlerMap.put( SupportedSaslMechanisms.PLAIN, new SimpleMechanismHandler() );

            CramMd5MechanismHandler cramMd5MechanismHandler = new CramMd5MechanismHandler();
            mechanismHandlerMap.put( SupportedSaslMechanisms.CRAM_MD5, cramMd5MechanismHandler );

            DigestMd5MechanismHandler digestMd5MechanismHandler = new DigestMd5MechanismHandler();
            mechanismHandlerMap.put( SupportedSaslMechanisms.DIGEST_MD5, digestMd5MechanismHandler );

            GssapiMechanismHandler gssapiMechanismHandler = new GssapiMechanismHandler();
            mechanismHandlerMap.put( SupportedSaslMechanisms.GSSAPI, gssapiMechanismHandler );

            NtlmMechanismHandler ntlmMechanismHandler = new NtlmMechanismHandler();
            mechanismHandlerMap.put( SupportedSaslMechanisms.NTLM, ntlmMechanismHandler );
            mechanismHandlerMap.put( SupportedSaslMechanisms.GSS_SPNEGO, ntlmMechanismHandler );

            ldapServer.setSaslMechanismHandlers( mechanismHandlerMap );

            return ldapServer;
        }
    }

    
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
            connection = new LdapConnection( config );
            BindResponse bindResponse = connection.bind( "uid=admin,ou=system", "secret" );
            
            assertNotNull( bindResponse );
            
            connection.unBind();
        }
        catch ( Exception le )
        {
            le.printStackTrace();
            fail();
        }
        finally
        {
            try
            {
                if( connection != null )
                {
                    connection.close();
                }
            }
            catch( IOException ioe )
            {
                fail();
            }
        }
    }
    
    
    @Test
    public void testGetSupportedControls() throws Exception
    {
        LdapConnection connection = new LdapConnection( config );

        LdapDN dn = new LdapDN( "uid=admin,ou=system" );
        connection.bind( dn.getName(), "secret" );
        
        List<String> controlList = connection.getSupportedConrols();
        assertNotNull( controlList );
        assertFalse( controlList.isEmpty() );
    }
}
