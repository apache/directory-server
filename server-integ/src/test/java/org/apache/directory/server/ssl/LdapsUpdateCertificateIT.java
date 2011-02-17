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

import java.security.cert.X509Certificate;
import java.util.Hashtable;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;

import org.apache.directory.junit.tools.MultiThreadedMultiInvoker;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.security.TlsKeyGenerator;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.plain.PlainMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.operations.bind.BogusNtlmProvider;
import org.apache.directory.shared.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test case to verify DIREVE-216.  Starts up the server binds via SUN JNDI provider
 * to perform add modify operations on entries.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class ) 
@CreateDS( allowAnonAccess=true, name="LdapsUpdateCertificateIT-class")
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
        @SaslMechanism( name= SupportedSaslMechanisms.GSS_SPNEGO, implClass=NtlmMechanismHandler.class)
    },
    extendedOpHandlers = 
    {
        StoredProcedureExtendedOperationHandler.class
    },
    ntlmProvider=BogusNtlmProvider.class
    )
public class LdapsUpdateCertificateIT extends AbstractLdapTestUnit
{
    @Rule
    public MultiThreadedMultiInvoker i = new MultiThreadedMultiInvoker( MultiThreadedMultiInvoker.NOT_THREADSAFE );

    /**
     * Create an entry for a person.
     */
    public DirContext getSecureConnection() throws Exception
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + getLdapServer().getPortSSL() + "/ou=system" );
        env.put( "java.naming.ldap.factory.socket", SSLSocketFactory.class.getName() );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        return new InitialDirContext( env );
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
        env.put( "java.naming.provider.url", "ldaps://localhost:" + getLdapServer().getPortSSL() );
        env.put( "java.naming.ldap.factory.socket", SSLSocketFactory.class.getName() );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        InitialDirContext ctx = new InitialDirContext( env );

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
        
        // create a secure connection
        ctx = new InitialDirContext( env );

        // check the received certificate, it must contain the updated server certificate
        X509Certificate[] lastReceivedServerCertificates = BogusTrustManagerFactory.lastReceivedServerCertificates;
        assertNotNull( lastReceivedServerCertificates );
        assertEquals( 1, lastReceivedServerCertificates.length );
        String issuerDN = lastReceivedServerCertificates[0].getIssuerDN().getName();
        String subjectDN = lastReceivedServerCertificates[0].getSubjectDN().getName();
        // converting the values to lowercase is required cause the certificate is
        // having attribute names in capital letters e.c the above newIssuerDN will be present as CN=new_issuer_dn
        assertEquals( "Expected the new certificate with the new issuer", newIssuerDN.toLowerCase(), issuerDN.toLowerCase() );
        assertEquals( "Expected the new certificate with the new subject", newSubjectDN.toLowerCase(), subjectDN.toLowerCase() );
    }

}
