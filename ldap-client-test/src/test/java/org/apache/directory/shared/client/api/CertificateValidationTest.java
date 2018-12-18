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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapTlsHandshakeException;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.security.TlsKeyGenerator;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test connections with SSL/StartTLS with various certificates.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(allowAnonAccess = true, name = "KeyStoreIT-class")
public class CertificateValidationTest extends AbstractLdapTestUnit
{

    private static final String KEYSTORE_PW = "changeit";

    private static final String ROOT_CA_KEYSTORE_PATH = "target/test-classes/root-ca-keystore.ks";
    private static KeyStore ROOT_CA_KEYSTORE;

    private static final String VALID_KEYSTORE_PATH = "target/test-classes/valid-keystore.ks";

    private static final String EXPIRED_KEYSTORE_PATH = "target/test-classes/expired-keystore.ks";

    private static final String NOT_YET_VALID_KEYSTORE_PATH = "target/test-classes/not-yet-valid-keystore.ks";

    private static final String SMALL_KEYSIZE_KEYSTORE_PATH = "target/test-classes/small-keysize-keystore.ks";


    @BeforeClass
    public static void installKeyStoreWithCertificate() throws Exception
    {
        String hostName = InetAddress.getLocalHost().getHostName();
        String issuerDn = TlsKeyGenerator.CERTIFICATE_PRINCIPAL_DN;
        String subjectDn = "CN=" + hostName;
        Date startDate = new Date();
        Date expiryDate = new Date( System.currentTimeMillis() + TlsKeyGenerator.YEAR_MILLIS );
        String keyAlgo = "RSA";
        int keySize = 1024;

        // generate root CA, self-signed
        String rootCaSubjectDn = issuerDn;
        ROOT_CA_KEYSTORE = createKeyStore( rootCaSubjectDn, issuerDn, startDate, expiryDate, keyAlgo, keySize, null,
            ROOT_CA_KEYSTORE_PATH );
        PrivateKey rootCaPrivateKey = ( PrivateKey ) ROOT_CA_KEYSTORE.getKey( "apacheds", KEYSTORE_PW.toCharArray() );

        // generate a valid certificate, signed by root CA
        createKeyStore( subjectDn, issuerDn, startDate, expiryDate, keyAlgo, keySize, rootCaPrivateKey,
            VALID_KEYSTORE_PATH );

        // generate an expired certificate, signed by root CA
        Date expiredStartDate = new Date( System.currentTimeMillis() - TlsKeyGenerator.YEAR_MILLIS );
        Date expiredExpiryDate = new Date( System.currentTimeMillis() - TlsKeyGenerator.YEAR_MILLIS / 365 );
        createKeyStore( subjectDn, issuerDn, expiredStartDate, expiredExpiryDate, keyAlgo, keySize,
            rootCaPrivateKey, EXPIRED_KEYSTORE_PATH );

        // generate a not yet valid certificate, signed by root CA
        Date notYetValidStartDate = new Date( System.currentTimeMillis() + TlsKeyGenerator.YEAR_MILLIS / 365 );
        Date notYetValidExpiryDate = new Date( System.currentTimeMillis() + TlsKeyGenerator.YEAR_MILLIS );
        createKeyStore( subjectDn, issuerDn, notYetValidStartDate, notYetValidExpiryDate, keyAlgo, keySize,
            rootCaPrivateKey, NOT_YET_VALID_KEYSTORE_PATH );

        // generate a certificate with small key size, signed by root CA
        int smallKeySize = 512;
        createKeyStore( subjectDn, issuerDn, startDate, expiryDate, keyAlgo, smallKeySize,
            rootCaPrivateKey, SMALL_KEYSIZE_KEYSTORE_PATH );

        // TODO signature does not match if root private key is null
    }


    private static KeyStore createKeyStore( String subjectDn, String issuerDn, Date startDate, Date expiryDate,
        String keyAlgo, int keySize, PrivateKey optionalSigningKey, String keystorePath )
        throws Exception
    {
        File goodKeyStoreFile = new File( keystorePath );
        if ( goodKeyStoreFile.exists() )
        {
            goodKeyStoreFile.delete();
        }
        Entry entry = new DefaultEntry();
        TlsKeyGenerator.addKeyPair( entry, issuerDn, subjectDn, startDate, expiryDate, keyAlgo, keySize,
            optionalSigningKey );
        KeyPair keyPair = TlsKeyGenerator.getKeyPair( entry );
        X509Certificate cert = TlsKeyGenerator.getCertificate( entry );
        //System.out.println( cert );

        KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
        keyStore.load( null, null );
        keyStore.setCertificateEntry( "apacheds", cert );
        keyStore.setKeyEntry( "apacheds", keyPair.getPrivate(), KEYSTORE_PW.toCharArray(), new Certificate[]
            { cert } );
        keyStore.store( new FileOutputStream( goodKeyStoreFile ), KEYSTORE_PW.toCharArray() );
        return keyStore;
    }


    @CreateLdapServer(transports = @CreateTransport(protocol = "LDAPS"), keyStore = VALID_KEYSTORE_PATH, certificatePassword = KEYSTORE_PW)
    @Test
    public void testLdaps_Valid_NoVerificationTrustManager() throws Exception
    {
        LdapConnectionConfig config = ldapsConnectionConfig();
        config.setTrustManagers( noVerificationTrustManagers() );
        connectOk( config );
    }


    @CreateLdapServer(transports = @CreateTransport(protocol = "LDAP"), extendedOpHandlers = StartTlsHandler.class, keyStore = VALID_KEYSTORE_PATH, certificatePassword = KEYSTORE_PW)
    @Test
    public void testStartTls_Valid_NoVerificationTrustManager() throws Exception
    {
        LdapConnectionConfig config = startTlsConnectionConfig();
        config.setTrustManagers( noVerificationTrustManagers() );
        connectOk( config );
    }


    @CreateLdapServer(transports = @CreateTransport(protocol = "LDAPS"), keyStore = VALID_KEYSTORE_PATH, certificatePassword = KEYSTORE_PW)
    @Test
    public void testLdaps_Valid_JvmDefaultTrustManager() throws Exception
    {
        LdapConnectionConfig config = ldapsConnectionConfig();
        config.setTrustManagers( jvmDefaultTrustManagers() );
        connectAndExpectTlsHandshakeException( config, CertPathBuilderException.class,
            "Failed to build certification path", "unable to find valid certification path to requested target" );
    }


    @CreateLdapServer(transports = @CreateTransport(protocol = "LDAP"), extendedOpHandlers = StartTlsHandler.class, keyStore = VALID_KEYSTORE_PATH, certificatePassword = KEYSTORE_PW)
    @Test
    public void testStartTls_Valid_JvmDefaultTrustManager() throws Exception
    {
        LdapConnectionConfig config = startTlsConnectionConfig();
        config.setTrustManagers( jvmDefaultTrustManagers() );
        connectAndExpectTlsHandshakeException( config, CertPathBuilderException.class,
            "Failed to build certification path", "unable to find valid certification path to requested target" );
    }


    @CreateLdapServer(transports = @CreateTransport(protocol = "LDAPS"), keyStore = VALID_KEYSTORE_PATH, certificatePassword = KEYSTORE_PW)
    @Test
    public void testLdaps_Valid_RootCaTrustManager() throws Exception
    {
        LdapConnectionConfig config = ldapsConnectionConfig();
        config.setTrustManagers( getCustomTrustManager( ROOT_CA_KEYSTORE ) );
        connectOk( config );
    }


    @CreateLdapServer(transports = @CreateTransport(protocol = "LDAP"), extendedOpHandlers = StartTlsHandler.class, keyStore = VALID_KEYSTORE_PATH, certificatePassword = KEYSTORE_PW)
    @Test
    public void testStartTls_Valid_RootCaTrustManager() throws Exception
    {
        LdapConnectionConfig config = startTlsConnectionConfig();
        config.setTrustManagers( getCustomTrustManager( ROOT_CA_KEYSTORE ) );
        connectOk( config );
    }


    @CreateLdapServer(transports = @CreateTransport(protocol = "LDAPS"), keyStore = EXPIRED_KEYSTORE_PATH, certificatePassword = KEYSTORE_PW)
    @Test
    public void testLdaps_Expired_RootCaTrustManager() throws Exception
    {
        LdapConnectionConfig config = ldapsConnectionConfig();
        config.setTrustManagers( getCustomTrustManager( ROOT_CA_KEYSTORE ) );
        connectAndExpectTlsHandshakeException( config, CertificateExpiredException.class, "Certificate expired",
            "NotAfter" );
    }


    @CreateLdapServer(transports = @CreateTransport(protocol = "LDAP"), extendedOpHandlers = StartTlsHandler.class, keyStore = EXPIRED_KEYSTORE_PATH, certificatePassword = KEYSTORE_PW)
    @Test
    public void testStartTls_Expired_RootCaTrustManager() throws Exception
    {
        LdapConnectionConfig config = startTlsConnectionConfig();
        config.setTrustManagers( getCustomTrustManager( ROOT_CA_KEYSTORE ) );
        connectAndExpectTlsHandshakeException( config, CertificateExpiredException.class, "Certificate expired",
            "NotAfter" );
    }


    @CreateLdapServer(transports = @CreateTransport(protocol = "LDAPS"), keyStore = NOT_YET_VALID_KEYSTORE_PATH, certificatePassword = KEYSTORE_PW)
    @Test
    public void testLdaps_NotYetValid_RootCaTrustManager() throws Exception
    {
        LdapConnectionConfig config = ldapsConnectionConfig();
        config.setTrustManagers( getCustomTrustManager( ROOT_CA_KEYSTORE ) );
        connectAndExpectTlsHandshakeException( config, CertificateNotYetValidException.class,
            "Certificate not yet valid",
            "NotBefore" );
    }


    @CreateLdapServer(transports = @CreateTransport(protocol = "LDAP"), extendedOpHandlers = StartTlsHandler.class, keyStore = NOT_YET_VALID_KEYSTORE_PATH, certificatePassword = KEYSTORE_PW)
    @Test
    public void testStartTls_NotYetValid_RootCaTrustManager() throws Exception
    {
        LdapConnectionConfig config = startTlsConnectionConfig();
        config.setTrustManagers( getCustomTrustManager( ROOT_CA_KEYSTORE ) );
        connectAndExpectTlsHandshakeException( config, CertificateNotYetValidException.class,
            "Certificate not yet valid",
            "NotBefore" );
    }


    @CreateLdapServer(transports = @CreateTransport(protocol = "LDAPS"), keyStore = SMALL_KEYSIZE_KEYSTORE_PATH, certificatePassword = KEYSTORE_PW)
    @Test
    public void testLdaps_SmallKeySize_RootCaTrustManager() throws Exception
    {
        LdapConnectionConfig config = ldapsConnectionConfig();
        config.setTrustManagers( getCustomTrustManager( ROOT_CA_KEYSTORE ) );
        connectAndExpectTlsHandshakeException( config, CertPathValidatorException.class,
            "Failed to verify certification path",
            "Algorithm constraints check failed on keysize limits" );
    }


    @CreateLdapServer(transports = @CreateTransport(protocol = "LDAP"), extendedOpHandlers = StartTlsHandler.class, keyStore = SMALL_KEYSIZE_KEYSTORE_PATH, certificatePassword = KEYSTORE_PW)
    @Test
    public void testStartTls_SmallKeySize_RootCaTrustManager() throws Exception
    {
        LdapConnectionConfig config = startTlsConnectionConfig();
        config.setTrustManagers( getCustomTrustManager( ROOT_CA_KEYSTORE ) );
        connectAndExpectTlsHandshakeException( config, CertPathValidatorException.class,
            "Failed to verify certification path",
            "Algorithm constraints check failed on keysize limits" );
    }


    private void connectOk( LdapConnectionConfig config ) throws LdapException, IOException
    {
        assertTrue( getLdapServer().isStarted() );

        try ( LdapNetworkConnection conn = new LdapNetworkConnection( config ) )
        {
            if ( config.isUseTls() )
            {
                conn.startTls();
            }
            else if ( config.isUseSsl() )
            {
                conn.connect();
            }
            else
            {
                fail( "Either useTls or useSsl must be enabled" );
            }

            assertTrue( conn.isConnected() );
            assertTrue( conn.isSecured() );
        }
    }


    private void connectAndExpectTlsHandshakeException( LdapConnectionConfig config,
        Class<? extends Throwable> rootCauseExceptionClass,
        String reasonPhrase, String reasonMessage ) throws IOException
    {
        assertTrue( getLdapServer().isStarted() );

        try ( LdapNetworkConnection conn = new LdapNetworkConnection( config ) )
        {
            try
            {
                if ( config.isUseTls() )
                {
                    conn.startTls();
                }
                else if ( config.isUseSsl() )
                {
                    conn.connect();
                }
                else
                {
                    fail( "Either useTls or useSsl must be enabled" );
                }

                fail( "Expected exception" );
            }
            catch ( LdapException e )
            {
                //e.printStackTrace();
                assertThat( e, is( instanceOf( LdapTlsHandshakeException.class ) ) );
                LdapTlsHandshakeException lthse = ( LdapTlsHandshakeException ) e;
                assertThat( lthse.getRootCause(), instanceOf( rootCauseExceptionClass ) );
                assertThat( lthse.getReasonPhrase(), equalTo( reasonPhrase ) );
                assertThat( lthse.getMessage(), containsString( "ERR_04120_TLS_HANDSHAKE_ERROR" ) );
                assertThat( lthse.getMessage(), containsString( "The TLS handshake failed" ) );
                assertThat( lthse.getMessage(), containsString( "reason: " + reasonPhrase ) );
                assertThat( lthse.getMessage(), containsString( reasonMessage ) );
            }

            assertFalse( conn.isConnected() );
            assertFalse( conn.isSecured() );
        }
    }


    private LdapConnectionConfig startTlsConnectionConfig()
    {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setTimeout( 1000 );
        config.setLdapHost( "localhost" );
        config.setLdapPort( getLdapServer().getPort() );
        config.setUseTls( true );
        return config;
    }


    private LdapConnectionConfig ldapsConnectionConfig()
    {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setTimeout( 1000 );
        config.setLdapHost( "localhost" );
        config.setLdapPort( getLdapServer().getPortSSL() );
        config.setUseSsl( true );
        return config;
    }


    private X509TrustManager getCustomTrustManager( KeyStore trustStore )
        throws NoSuchAlgorithmException, KeyStoreException
    {
        TrustManagerFactory factory = TrustManagerFactory.getInstance( TrustManagerFactory
            .getDefaultAlgorithm() );
        factory.init( trustStore );
        TrustManager[] trustManagers = factory.getTrustManagers();
        TrustManager trustManager = trustManagers[0];
        return ( X509TrustManager ) trustManager;
    }


    private TrustManager[] jvmDefaultTrustManagers() throws NoSuchAlgorithmException, KeyStoreException
    {
        TrustManagerFactory factory = TrustManagerFactory.getInstance( TrustManagerFactory
            .getDefaultAlgorithm() );
        factory.init( ( KeyStore ) null );
        TrustManager[] defaultTrustManagers = factory.getTrustManagers();
        return defaultTrustManagers;
    }


    private TrustManager[] noVerificationTrustManagers()
    {
        return new X509TrustManager[]
            { new NoVerificationTrustManager() };
    }

}
