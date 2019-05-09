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
package org.apache.directory.server.ldap.handlers.sasl.external;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Network;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.*;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.security.TlsKeyGenerator;
import org.apache.directory.server.ldap.handlers.sasl.external.certificate.CertificateMechanismHandler;
import org.apache.directory.server.ssl.ClientCertificateSslSocketFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Date;
import java.util.Hashtable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test the authentication using EXTERNAL SASL client certificate authentication.
 * Stores the client certificate on a testuser which is also used for ldap connection.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(allowAnonAccess = true, name = "ClientCertificateAuthenticationIT-class",
        partitions =
                {
                        @CreatePartition(
                                name = "example",
                                suffix = "dc=example,dc=com",
                                contextEntry = @ContextEntry(
                                        entryLdif =
                                                "dn: dc=example,dc=com\n" +
                                                        "dc: example\n" +
                                                        "objectClass: top\n" +
                                                        "objectClass: domain\n\n"),
                                indexes =
                                        {
                                                @CreateIndex(attribute = "objectClass"),
                                                @CreateIndex(attribute = "dc"),
                                                @CreateIndex(attribute = "ou")
                                        })
                })
@CreateLdapServer(
        transports =
                {
                        @CreateTransport(protocol = "LDAPS", clientAuth = true)
                },
        saslMechanisms =
                {
                        @SaslMechanism(name = SupportedSaslMechanisms.EXTERNAL, implClass = CertificateMechanismHandler.class)
                })
@ApplyLdifs(
        {
                // Entry # 1
                "dn: ou=users,dc=example,dc=com",
                "objectClass: organizationalUnit",
                "objectClass: top",
                "ou: users\n",

                // Entry # 2
                "dn: uid=testsubject,ou=users,dc=example,dc=com",
                "objectClass: inetOrgPerson",
                "objectClass: organizationalPerson",
                "objectClass: person",
                "objectClass: top",
                "uid: testsubject",
                "userPassword: not_set",
                "cn: Test Subject",
                "sn: Subject"
        }
)
public class ClientCertificateAuthenticationIT extends AbstractLdapTestUnit
{

    private Dn authenticationUserDn;

    /**
     * Setup the test, prepare certificate and testuser
     * @throws Exception on any error
     */
    @Before
    public void installKeyStoreWithCertificate() throws Exception
        {
            authenticationUserDn = new Dn("uid=testsubject,ou=users,dc=example,dc=com");

            String hostName = InetAddress.getLocalHost().getHostName();
            String issuerDn = TlsKeyGenerator.CERTIFICATE_PRINCIPAL_DN;
            String subjectDn = "CN=" + hostName;
            Date startDate = new Date();
            Date expiryDate = new Date( System.currentTimeMillis() + TlsKeyGenerator.YEAR_MILLIS );
            String keyAlgo = "RSA";
            int keySize = 1024;

            Entry entry = new DefaultEntry();
            TlsKeyGenerator.addKeyPair( entry, issuerDn, subjectDn, startDate, expiryDate, keyAlgo, keySize, null );

            // prepare socket factory to provide client certificate
            try ( ByteArrayInputStream in = new ByteArrayInputStream( TlsKeyGenerator.getCertificate( entry ).getEncoded() ) )
            {
                CertificateFactory factory = CertificateFactory.getInstance( "X.509" );
                Certificate cert = factory.generateCertificate( in );
                KeyStore ks = KeyStore.getInstance( KeyStore.getDefaultType() );
                ks.load( null, null );
                ks.setKeyEntry("apacheds", TlsKeyGenerator.getKeyPair( entry ).getPrivate(), ClientCertificateSslSocketFactory.ksPassword, new Certificate[] { cert } );
                ks.store( new FileOutputStream( ClientCertificateSslSocketFactory.ksFile ), ClientCertificateSslSocketFactory.ksPassword );
            }

            // set certificte to testuser
            Modification mod = new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
                TlsKeyGenerator.USER_CERTIFICATE_AT, entry.get( TlsKeyGenerator.USER_CERTIFICATE_AT ).getBytes() );
            getLdapServer().getDirectoryService().getAdminSession().modify(new Dn("uid=testsubject,ou=users,dc=example,dc=com"), mod );
        }

    /**
     * Cleanup test, remove keystore
     * @throws Exception on any error
     */
    @After
    public void teardown() throws Exception {
        if ( ClientCertificateSslSocketFactory.ksFile != null && ClientCertificateSslSocketFactory.ksFile.exists() )
        {
            ClientCertificateSslSocketFactory.ksFile.delete();
        }
    }

    /**
     * Do just a connect and a simple search to verify if authentication works.
     * The test checks the authentication user in the current ldap session.
     *
     * @throws Exception on any error
     */
    @Test
    public void testExternalClientCertificateAuthentication() throws Exception
    {
        // create a new secure connection
        Hashtable<Object, Object> env = new Hashtable<>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", Network.ldapLoopbackUrl( getLdapServer().getPortSSL() ) );
        env.put( "java.naming.security.protocol", "ssl");
        env.put( "java.naming.ldap.factory.socket", ClientCertificateSslSocketFactory.class.getName () );
        env.put( "java.naming.security.authentication", "EXTERNAL" );

        DirContext ctx = new InitialDirContext( env );
        try
        {
            String searchFilter = "(objectClass=*)";
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope( SearchControls.OBJECT_SCOPE );

            NamingEnumeration<SearchResult> results = ctx.search("dc=example,dc=com", searchFilter, searchControls );
            assertTrue( results.hasMore() );

            assertEquals(authenticationUserDn.getName(),
                    getLdapServer().getLdapSessionManager().getSessions()[0].getCoreSession().getAuthenticatedPrincipal().getDn().getName());
        }
        finally
        {
            ctx.close();
        }
    }

}