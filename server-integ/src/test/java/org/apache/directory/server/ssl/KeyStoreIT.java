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
package org.apache.directory.server.ssl;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.security.TlsKeyGenerator;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test key store scenarios.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(allowAnonAccess = true, name = "KeyStoreIT-class")
public class KeyStoreIT extends AbstractLdapTestUnit
{

    private static final String KEYSTORE_PW = "changeit";
    private static final String GOOD_KEYSTORE = "target/test-classes/good-keystore.ks";
    private static final String BAD_KEYSTORE_WITH_ZERO_ENTRIES = "target/test-classes/bad-keystore-with-zero-entries.ks";
    private static final String BAD_KEYSTORE_WITH_TWO_ENTRIES = "target/test-classes/bad-keystore-with-two-entries.ks";
    private static final String NON_EXISTING_KEY_STORE_FILE = "target/test-classes/non-existing-keystore-file.ks";


    @BeforeAll
    public static void installKeyStoreWithCertificate() throws Exception
    {
        // Create the good key store
        File goodKeyStoreFile = new File( GOOD_KEYSTORE );
        if ( goodKeyStoreFile.exists() )
        {
            goodKeyStoreFile.delete();
        }
        Entry entry = new DefaultEntry();
        TlsKeyGenerator.addKeyPair( entry );
        KeyPair keyPair = TlsKeyGenerator.getKeyPair( entry );
        X509Certificate cert = TlsKeyGenerator.getCertificate( entry );
        KeyStore goodKeyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
        goodKeyStore.load( null, null );
        goodKeyStore.setCertificateEntry( "apacheds", cert );
        goodKeyStore.setKeyEntry( "apacheds", keyPair.getPrivate(), KEYSTORE_PW.toCharArray(), new Certificate[]
            { cert } );
        try ( FileOutputStream out = new FileOutputStream( goodKeyStoreFile ) )
        {
            goodKeyStore.store( out, KEYSTORE_PW.toCharArray() );
        }

        // Create the empty key store
        File emptyKeyStoreFile = new File( BAD_KEYSTORE_WITH_ZERO_ENTRIES );
        if ( emptyKeyStoreFile.exists() )
        {
            emptyKeyStoreFile.delete();
        }
        KeyStore emptyKeyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
        emptyKeyStore.load( null, null );
        try ( FileOutputStream out = new FileOutputStream( emptyKeyStoreFile ) )
        {
            emptyKeyStore.store( new FileOutputStream( emptyKeyStoreFile ), KEYSTORE_PW.toCharArray() );
        }

        // Create the bad key store with two entries
        File twoEntiesKeyStoreFile = new File( BAD_KEYSTORE_WITH_TWO_ENTRIES );
        if ( twoEntiesKeyStoreFile.exists() )
        {
            twoEntiesKeyStoreFile.delete();
        }
        KeyStore twoEntiesKeyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
        twoEntiesKeyStore.load( null, null );
        twoEntiesKeyStore.setCertificateEntry( "foo123", cert );
        twoEntiesKeyStore.setKeyEntry( "apacheds", keyPair.getPrivate(), KEYSTORE_PW.toCharArray(), new Certificate[]
            { cert } );
        try ( FileOutputStream out = new FileOutputStream( twoEntiesKeyStoreFile ) )
        {
            twoEntiesKeyStore.store( out, KEYSTORE_PW.toCharArray() );
        }
    }


    @CreateLdapServer(transports =
        {
            @CreateTransport(protocol = "LDAPS")
        })
    @Test
    public void testLdaps_DefaultAdminCert_NoVerificationTrustManager() throws Exception
    {
        LdapConnectionConfig config = ldapsConnectionConfig();
        
        config.setTrustManagers( new X509TrustManager[] { new NoVerificationTrustManager() } );
        
        try (
            LdapNetworkConnection conn = new LdapNetworkConnection( config ); )
        {
            conn.connect();
            assertTrue( conn.isConnected() );
            assertTrue( conn.isSecured() );
        }
    }


    @CreateLdapServer(transports =
        {
            @CreateTransport(protocol = "LDAP"),
        }, extendedOpHandlers = StartTlsHandler.class)
    @Test
    public void testStartTls_DefaultAdminCert_NoVerificationTrustManager() throws Exception
    {
        LdapConnectionConfig config = startTlsConnectionConfig();
        
        config.setTrustManagers( new X509TrustManager[] { new NoVerificationTrustManager() } );

        try (
            LdapNetworkConnection conn = new LdapNetworkConnection( config ); )
        {
            conn.startTls();
            assertTrue( conn.isConnected() );
            assertTrue( conn.isSecured() );
        }
    }


    @CreateLdapServer(transports =
        {
            @CreateTransport(protocol = "LDAPS")
        })
    @Test
    public void testLdaps_DefaultAdminCert_DefaultTrustManager() throws Exception
    {
        LdapConnectionConfig config = ldapsConnectionConfig();

        try (
            LdapNetworkConnection conn = new LdapNetworkConnection( config ); )
        {
            try
            {
                conn.connect();
                fail( "Expected exception" );
            }
            catch ( LdapException e )
            {
                //e.printStackTrace();
                assertTrue( e.getMessage().contains( "ERR_04120_TLS_HANDSHAKE_ERROR The TLS handshake failed" ) );
            }
            assertFalse( conn.isConnected() );
            assertFalse( conn.isSecured() );
        }
    }


    @CreateLdapServer(transports =
        {
            @CreateTransport(protocol = "LDAP"),
        }, extendedOpHandlers = StartTlsHandler.class)
    @Test
    public void testStartTls_DefaultAdminCert_DefaultTrustManager() throws Exception
    {
        LdapConnectionConfig config = startTlsConnectionConfig();
        config.setTrustManagers( defaultTrustManagers() );

        try (
            LdapNetworkConnection conn = new LdapNetworkConnection( config ); )
        {
            try
            {
                conn.startTls();
                fail( "Expected exception" );
            }
            catch ( LdapException e )
            {
                assertTrue( e.getMessage().contains( "ERR_04120_TLS_HANDSHAKE_ERROR The TLS handshake failed" ) );
            }
            assertFalse( conn.isConnected() );
            assertFalse( conn.isSecured() );
        }
    }


    @CreateLdapServer(transports =
        {
            @CreateTransport(protocol = "LDAPS")
        }, keyStore = GOOD_KEYSTORE, certificatePassword = KEYSTORE_PW)
    @Test
    public void testLdaps_GoodKeyStore_NoVerificationTrustManager() throws Exception
    {
        assertTrue( getLdapServer().isStarted() );

        LdapConnectionConfig config = ldapsConnectionConfig();
        
        config.setTrustManagers( new X509TrustManager[] { new NoVerificationTrustManager() } );

        try (
            LdapNetworkConnection conn = new LdapNetworkConnection( config ); )
        {
            conn.connect();
            assertTrue( conn.isConnected() );
            assertTrue( conn.isSecured() );
        }
    }


    @CreateLdapServer(transports =
        {
            @CreateTransport(protocol = "LDAP")
        }, extendedOpHandlers = StartTlsHandler.class, keyStore = GOOD_KEYSTORE, certificatePassword = KEYSTORE_PW)
    @Test
    public void testStartTls_GoodKeyStore_NoVerificationTrustManager() throws Exception
    {
        assertTrue( getLdapServer().isStarted() );

        LdapConnectionConfig config = startTlsConnectionConfig();
        
        config.setTrustManagers( new X509TrustManager[] { new NoVerificationTrustManager() } );

        try (
            LdapNetworkConnection conn = new LdapNetworkConnection( config ); )
        {
            conn.startTls();
            assertTrue( conn.isConnected() );
            assertTrue( conn.isSecured() );
        }
    }


    @CreateLdapServer(transports =
        {
            @CreateTransport(protocol = "LDAPS")
        }, keyStore = GOOD_KEYSTORE, certificatePassword = KEYSTORE_PW)
    @Test
    public void testLdaps_GoodKeyStore_DefaultTrustManager() throws Exception
    {
        assertTrue( getLdapServer().isStarted() );

        LdapConnectionConfig config = ldapsConnectionConfig();
        config.setTrustManagers( defaultTrustManagers() );

        try (
            LdapNetworkConnection conn = new LdapNetworkConnection( config ); )
        {
            try
            {
                conn.connect();
                fail( "Expected exception" );
            }
            catch ( LdapException e )
            {
                assertTrue( e.getMessage().contains( "ERR_04120_TLS_HANDSHAKE_ERROR The TLS handshake failed" ) );
            }
            assertFalse( conn.isConnected() );
            assertFalse( conn.isSecured() );
        }
    }


    @CreateLdapServer(transports =
        {
            @CreateTransport(protocol = "LDAP"),
        }, extendedOpHandlers = StartTlsHandler.class, keyStore = GOOD_KEYSTORE, certificatePassword = KEYSTORE_PW)
    @Test
    public void testStartTls_GoodKeyStore_DefaultTrustManager() throws Exception
    {
        assertTrue( getLdapServer().isStarted() );

        LdapConnectionConfig config = startTlsConnectionConfig();
        config.setTrustManagers( defaultTrustManagers() );

        try (
            LdapNetworkConnection conn = new LdapNetworkConnection( config ); )
        {
            try
            {
                conn.startTls();
                fail( "Expected exception" );
            }
            catch ( LdapException e )
            {
                assertTrue( e.getMessage().contains( "ERR_04120_TLS_HANDSHAKE_ERROR The TLS handshake failed" ) );
            }
            assertFalse( conn.isConnected() );
            assertFalse( conn.isSecured() );
        }
    }


    @CreateLdapServer(transports =
        {
            @CreateTransport(protocol = "LDAPS")
        }, keyStore = GOOD_KEYSTORE, certificatePassword = "wrong key store password")
    @Test
    public void shouldNotStartServerIfKeyStorePasswordIsWrong() throws Exception
    {
        assertFalse( getLdapServer().isStarted() );
    }


    @CreateLdapServer(transports =
        {
            @CreateTransport(protocol = "LDAP"),
        }, extendedOpHandlers = StartTlsHandler.class, keyStore = NON_EXISTING_KEY_STORE_FILE, certificatePassword = KEYSTORE_PW)
    @Test
    public void shouldNotStartServerIfKeyStoreFileDoesNotExist() throws Exception
    {
        assertFalse( getLdapServer().isStarted() );
    }


    @CreateLdapServer(transports =
        {
            @CreateTransport(protocol = "LDAPS")
        }, keyStore = BAD_KEYSTORE_WITH_ZERO_ENTRIES, certificatePassword = KEYSTORE_PW)
    @Test
    public void shouldNotStartServerIfKeyStoreFileIsEmpty() throws Exception
    {
        assertFalse( getLdapServer().isStarted() );
    }


    @CreateLdapServer(transports =
        {
            @CreateTransport(protocol = "LDAP"),
        }, extendedOpHandlers = StartTlsHandler.class, keyStore = BAD_KEYSTORE_WITH_TWO_ENTRIES, certificatePassword = KEYSTORE_PW)
    @Test
    public void shouldNotStartServerIfKeyStoreFileContainsMoreThanOneEntry() throws Exception
    {
        assertFalse( getLdapServer().isStarted() );
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


    private TrustManager[] defaultTrustManagers() throws NoSuchAlgorithmException, KeyStoreException
    {
        TrustManagerFactory factory = TrustManagerFactory.getInstance( TrustManagerFactory
            .getDefaultAlgorithm() );
        factory.init( ( KeyStore ) null );
        TrustManager[] defaultTrustManagers = factory.getTrustManagers();
        return defaultTrustManagers;
    }

}
