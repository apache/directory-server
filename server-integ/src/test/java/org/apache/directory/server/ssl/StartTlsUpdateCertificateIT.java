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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Hashtable;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.security.TlsKeyGenerator;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test case to verify proper operation of confidentiality requirements as 
 * specified in https://issues.apache.org/jira/browse/DIRSERVER-1189.  
 * 
 * Starts up the server binds via SUN JNDI provider to perform various 
 * operations on entries which should be rejected when a TLS secured 
 * connection is not established.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(allowAnonAccess = true, name = "StartTlsUpdateCertificateIT-class")
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP"),
            @CreateTransport(protocol = "LDAPS")
    },
    extendedOpHandlers =
        { StartTlsHandler.class })
public class StartTlsUpdateCertificateIT extends AbstractLdapTestUnit
{
    private static final Logger LOG = LoggerFactory.getLogger( StartTlsUpdateCertificateIT.class );
    private static final String[] CERT_IDS = new String[]
        { "userCertificate" };
    private File ksFile;

    boolean oldConfidentialityRequiredValue;


    /**
     * Sets up the key store and installs the self signed certificate for the 
     * server (created on first startup) which is to be used by the StartTLS 
     * JDNDI client that will connect.  The key store is created from scratch
     * programmatically and whipped on each run.  The certificate is acquired 
     * by pulling down the bytes for administrator's userCertificate from 
     * uid=admin,ou=system.  We use sysRoot direct context instead of one over
     * the wire since the server is configured to prevent connections without
     * TLS secured connections.
     */
    @Before
    public void installKeyStoreWithCertificate() throws Exception
    {
        if ( ksFile != null && ksFile.exists() )
        {
            ksFile.delete();
        }

        ksFile = File.createTempFile( "testStore", "ks" );
        CoreSession session = getLdapServer().getDirectoryService().getAdminSession();
        Entry entry = session.lookup( new Dn( "uid=admin,ou=system" ), CERT_IDS );
        byte[] userCertificate = entry.get( CERT_IDS[0] ).getBytes();
        assertNotNull( userCertificate );

        try ( ByteArrayInputStream in = new ByteArrayInputStream( userCertificate ) )
        {
            CertificateFactory factory = CertificateFactory.getInstance( "X.509" );
            Certificate cert = factory.generateCertificate( in );
            KeyStore ks = KeyStore.getInstance( KeyStore.getDefaultType() );
            ks.load( null, null );
            ks.setCertificateEntry( "apacheds", cert );
            ks.store( new FileOutputStream( ksFile ), "changeit".toCharArray() );
            LOG.debug( "Keystore file installed: {}", ksFile.getAbsolutePath() );
        }

        oldConfidentialityRequiredValue = getLdapServer().isConfidentialityRequired();
    }


    /**
     * Just deletes the generated key store file.
     */
    @After
    public void deleteKeyStore() throws Exception
    {
        if ( ksFile != null && ksFile.exists() )
        {
            ksFile.delete();
        }

        LOG.debug( "Keystore file deleted: {}", ksFile.getAbsolutePath() );
        getLdapServer().setConfidentialityRequired( oldConfidentialityRequiredValue );
    }


    /**
     * Test for DIRSERVER-1373.
     */
    @Test
    public void testUpdateCertificate() throws Exception
    {
        // create a secure connection
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + getLdapServer().getPort() );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        LdapContext ctx = new InitialLdapContext( env, null );
        StartTlsResponse tls = ( StartTlsResponse ) ctx.extendedOperation( new StartTlsRequest() );
        tls.setHostnameVerifier( new HostnameVerifier()
        {
            public boolean verify( String hostname, SSLSession session )
            {
                return true;
            }
        } );
        tls.negotiate( BogusSSLContextFactory.getInstance( false ).getSocketFactory() );

        // create a new certificate
        String newIssuerDN = "cn=new_issuer_dn";
        String newSubjectDN = "cn=new_subject_dn";
        Entry entry = getLdapServer().getDirectoryService().getAdminSession().lookup(
            new Dn( "uid=admin,ou=system" ) );
        TlsKeyGenerator.addKeyPair( entry, newIssuerDN, newSubjectDN, "RSA" );

        // now update the certificate (over the wire)
        ModificationItem[] mods = new ModificationItem[3];
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(
            TlsKeyGenerator.PRIVATE_KEY_AT, entry.get( TlsKeyGenerator.PRIVATE_KEY_AT ).getBytes() ) );
        mods[1] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(
            TlsKeyGenerator.PUBLIC_KEY_AT, entry.get( TlsKeyGenerator.PUBLIC_KEY_AT ).getBytes() ) );
        mods[2] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(
            TlsKeyGenerator.USER_CERTIFICATE_AT, entry.get( TlsKeyGenerator.USER_CERTIFICATE_AT ).getBytes() ) );
        ctx.modifyAttributes( "uid=admin,ou=system", mods );
        ctx.close();

        getLdapServer().reloadSslContext();

        // create a new secure connection
        ctx = new InitialLdapContext( env, null );
        tls = ( StartTlsResponse ) ctx.extendedOperation( new StartTlsRequest() );
        tls.setHostnameVerifier( new HostnameVerifier()
        {
            public boolean verify( String hostname, SSLSession session )
            {
                return true;
            }
        } );
        tls.negotiate( BogusSSLContextFactory.getInstance( false ).getSocketFactory() );

        // check the received certificate, it must contain the updated server certificate
        X509Certificate[] lastReceivedServerCertificates = BogusTrustManagerFactory.lastReceivedServerCertificates;
        assertNotNull( lastReceivedServerCertificates );
        assertEquals( 1, lastReceivedServerCertificates.length );
        String issuerDN = lastReceivedServerCertificates[0].getIssuerDN().getName();
        String subjectDN = lastReceivedServerCertificates[0].getSubjectDN().getName();
        assertEquals( "Expected the new certificate with the new issuer", Strings.toLowerCase( newIssuerDN ),
            Strings.toLowerCase( issuerDN ) );
        assertEquals( "Expected the new certificate with the new subject", Strings.toLowerCase( newSubjectDN ),
            Strings.toLowerCase( subjectDN ) );
    }
}
