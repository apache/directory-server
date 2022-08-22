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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.cert.X509Certificate;
import java.util.Hashtable;

import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.util.Network;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.apache.directory.server.operations.bind.BogusNtlmProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test case to verify DIREVE-216.  Starts up the server binds via SUN JNDI provider
 * to perform add modify operations on entries.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(allowAnonAccess = true, name = "LdapsUpdateCertificateIT-class")
@CreateLdapServer(
    transports =
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
            StoredProcedureExtendedOperationHandler.class
    },
    ntlmProvider = BogusNtlmProvider.class)
public class LdapsUpdateCertificateIT extends AbstractLdapTestUnit
{
    /**
     * Create an entry for a person.
     */
    public DirContext getSecureConnection() throws Exception
    {
        Hashtable<String, Object> env = setDefaultJNDIEnv();
        env.put( "java.naming.provider.url", "ldap://" + Network.LOOPBACK_HOSTNAME + ":"
            + getLdapServer().getPortSSL() + "/ou=system" );
        env.put( "java.naming.ldap.factory.socket", AdsSSLSocketFactory.class.getName() );

        return new InitialDirContext( env );
    }


    /**
     * Test for DIRSERVER-1373.
     */
    @Test
    public void testUpdateCertificate() throws Exception
    {
        // create a secure connection
        Hashtable<String, Object> env = setDefaultJNDIEnv();
        env.put( "java.naming.provider.url", "ldaps://" + Network.LOOPBACK_HOSTNAME + ":"
            + getLdapServer().getPortSSL() );
        env.put( "java.naming.ldap.factory.socket", AdsSSLSocketFactory.class.getName() );

        new InitialDirContext( env );

        // create a new certificate
        String newIssuerDN = "new_issuer_dn";
        String newSubjectDN = "new_subject_dn";
        changeCertificate( ldapServer.getKeystoreFile(), "secret", newIssuerDN, newSubjectDN, 365, "SHA256WithECDSA" );

        // now update the certificate (over the wire)
        getLdapServer().reloadSslContext();

        // create a secure connection
        new InitialDirContext( env );

        // check the received certificate, it must contain the updated server certificate
        X509Certificate[] lastReceivedServerCertificates = BogusTrustManagerFactory.lastReceivedServerCertificates;
        assertNotNull( lastReceivedServerCertificates );
        assertEquals( 1, lastReceivedServerCertificates.length );
        String issuerDN = lastReceivedServerCertificates[0].getIssuerDN().getName();
        String subjectDN = lastReceivedServerCertificates[0].getSubjectDN().getName();
        // converting the values to lowercase is required cause the certificate is
        // having attribute names in capital letters e.c the above newIssuerDN will be present as CN=new_issuer_dn
        assertEquals( 
            Strings.toLowerCaseAscii( issuerDN ), Strings.toLowerCaseAscii( "CN=new_issuer_dn, OU=directory, O=apache, C=US" ),
            "Expected the new certificate with the new issuer" );
        assertEquals( 
            Strings.toLowerCaseAscii( subjectDN ), Strings.toLowerCaseAscii( "CN=new_subject_dn, OU=directory, O=apache, C=US" ),
            "Expected the new certificate with the new subject" );
    }
}
