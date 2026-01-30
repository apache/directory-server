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
package org.apache.directory.server.operations.bind;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.net.SocketClient;
import org.apache.directory.api.asn1.util.Asn1Buffer;
import org.apache.directory.api.ldap.codec.api.LdapDecoder;
import org.apache.directory.api.ldap.codec.api.LdapEncoder;
import org.apache.directory.api.ldap.codec.api.LdapMessageContainer;
import org.apache.directory.api.ldap.model.constants.SaslQoP;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.Message;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Network;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.NoVerificationTrustManager;
import org.apache.directory.ldap.client.api.SaslCramMd5Request;
import org.apache.directory.ldap.client.api.SaslDigestMd5Request;
import org.apache.directory.ldap.client.api.SaslGssApiRequest;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.annotations.LoadSchema;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.apache.directory.shared.kerberos.KerberosAttribute;
import org.apache.kerby.kerberos.kdc.impl.NettyKdcServerImpl;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jose.util.StandardCharset;


/**
 * An {@link AbstractServerTest} testing SASL authentication.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@ApplyLdifs(
    {
        // Entry # 1
        "dn: ou=users,dc=example,dc=com",
        "objectClass: organizationalUnit",
        "objectClass: top",
        "ou: users\n",

        // Entry # 2
        "dn: uid=hnelson,ou=users,dc=example,dc=com",
        "objectClass: inetOrgPerson",
        "objectClass: organizationalPerson",
        "objectClass: person",
        "objectClass: krb5principal",
        "objectClass: krb5kdcentry",
        "objectClass: top",
        "uid: hnelson",
        "userPassword: secret",
        "krb5PrincipalName: hnelson@EXAMPLE.COM",
        "krb5KeyVersionNumber: 0",
        "cn: Horatio Nelson",
        "sn: Nelson",

        // krbtgt
        "dn: uid=krbtgt,ou=users,dc=example,dc=com",
        "objectClass: inetOrgPerson",
        "objectClass: organizationalPerson",
        "objectClass: person",
        "objectClass: krb5principal",
        "objectClass: krb5kdcentry",
        "objectClass: top",
        "uid: krbtgt",
        "userPassword: secret",
        "krb5PrincipalName: krbtgt/EXAMPLE.COM@EXAMPLE.COM",
        "krb5KeyVersionNumber: 0",
        "cn: KDC Service",
        "sn: Service",

        // ldap per host
        "dn: uid=ldap,ou=users,dc=example,dc=com",
        "objectClass: inetOrgPerson",
        "objectClass: organizationalPerson",
        "objectClass: person",
        "objectClass: krb5principal",
        "objectClass: krb5kdcentry",
        "objectClass: top",
        "uid: ldap",
        "userPassword: randall",
        "krb5PrincipalName: ldap/localhost@EXAMPLE.COM",
        "krb5KeyVersionNumber: 0",
        "cn: LDAP Service",
        "sn: Service" })
@CreateDS(
    allowAnonAccess = false,
    name = "SaslBindIT-class",
    loadedSchemas =
        {
            @LoadSchema(name = "apachedns", enabled = true),
            @LoadSchema(name = "autofs", enabled = true),
            @LoadSchema(name = "corba", enabled = true),
            @LoadSchema(name = "dhcp", enabled = true),
            @LoadSchema(name = "mozilla", enabled = true),
            @LoadSchema(name = "nis", enabled = true),
            @LoadSchema(name = "posix", enabled = true),
            @LoadSchema(name = "rfc2307bis", enabled = true),
            @LoadSchema(name = "samba", enabled = true)
        },
    partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                contextEntry =
                @ContextEntry(
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
    },
    additionalInterceptors =
        { KeyDerivationInterceptor.class })
@CreateLdapServer(transports =
    {
        @CreateTransport(protocol = "LDAP"),
        @CreateTransport(protocol = "LDAPS")
    },
    saslHost = "localhost",
    saslPrincipal = "ldap/localhost@EXAMPLE.COM",
    saslMechanisms =
        {
            @SaslMechanism(name = SupportedSaslMechanisms.PLAIN, implClass = PlainMechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.CRAM_MD5, implClass = CramMd5MechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.DIGEST_MD5, implClass = DigestMd5MechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.GSSAPI, implClass = GssapiMechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.NTLM, implClass = NtlmMechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.GSS_SPNEGO, implClass = NtlmMechanismHandler.class)
    },
    maxSizeLimit = 100,
    extendedOpHandlers =
        {
            StartTlsHandler.class,
            StoredProcedureExtendedOperationHandler.class
    },
    ntlmProvider = BogusNtlmProvider.class,
    searchBaseDn = "dc=example,dc=com")
public class SaslBindIT extends AbstractLdapTestUnit
{

    private Dn userDn;

    @BeforeEach
    public void init() throws Exception
    {
        ldapServer.setConfidentialityRequired( false );
        KerberosTestUtils.fixServicePrincipalName( "ldap/" + Network.LOOPBACK_HOSTNAME + "@EXAMPLE.COM",
            new Dn( "uid=ldap,ou=users,dc=example,dc=com" ), getLdapServer() );
        userDn = new Dn( "uid=hnelson,ou=users,dc=example,dc=com" );
    }


    /**
     * Tests to make sure the server properly returns the supportedSASLMechanisms.
     */
    @Test
    public void testSupportedSASLMechanisms() throws Exception
    {
        // We have to tell the server that it should accept anonymous
        // auth, because we are reading the rootDSE
        getLdapServer().getDirectoryService().setAllowAnonymousAccess( true );

        // Point on rootDSE
        DirContext context = new InitialDirContext();

        Attributes attrs = context.getAttributes( Network.ldapLoopbackUrl( getLdapServer().getPort() ), new String[]
            { "supportedSASLMechanisms" } );

        //             Thread.sleep( 10 * 60 * 1000 );
        NamingEnumeration<? extends Attribute> answer = attrs.getAll();
        Attribute result = answer.next();
        assertEquals( 6, result.size() );
        assertTrue( result.contains( SupportedSaslMechanisms.GSSAPI ) );
        assertTrue( result.contains( SupportedSaslMechanisms.DIGEST_MD5 ) );
        assertTrue( result.contains( SupportedSaslMechanisms.CRAM_MD5 ) );
        assertTrue( result.contains( SupportedSaslMechanisms.NTLM ) );
        assertTrue( result.contains( SupportedSaslMechanisms.PLAIN ) );
        assertTrue( result.contains( SupportedSaslMechanisms.GSS_SPNEGO ) );
    }


    /**
     * Tests to make sure PLAIN-binds works
     */
    @Test
    public void testSaslBindPLAIN() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );

        BindResponse resp = connection.bindSaslPlain( "hnelson", "secret" );
        assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );

        Entry entry = connection.lookup( userDn );
        assertEquals( "hnelson", entry.get( "uid" ).getString() );

        connection.close();

        // Try to bind with a wrong user
        resp = connection.bindSaslPlain( "hnelsom", "secret" );
        assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, resp.getLdapResult().getResultCode() );

        // Try to bind with a wrong password
        resp = connection.bindSaslPlain( "hnelson", "secres" );
        assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, resp.getLdapResult().getResultCode() );
    }


    /**
     * Test a SASL bind with an empty mechanism
     */
    @Test
    @Disabled("Activate and fix when DIRAPI-36 (Provide a SaslBindRequest extending BindRequest that can be used in LdapConnection.bind(...) method) is solved")
    public void testSaslBindNoMech() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );
        BindRequest bindReq = new BindRequestImpl();
        bindReq.setCredentials( "secret" );
        bindReq.setDn( userDn );
        bindReq.setSaslMechanism( "" ); // invalid mechanism
        bindReq.setSimple( false );

        try
        {
            connection.bind( bindReq );
            fail();
        }
        catch ( LdapException le )
        {
            //expected
        }

        connection.close();
    }


    /**
     * Tests to make sure CRAM-MD5 binds below the RootDSE work.
     */
    @Test
    public void testSaslCramMd5Bind() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );
        connection.setTimeOut(0);

        SaslCramMd5Request request = new SaslCramMd5Request();
        request.setUsername( userDn.getRdn().getValue() );
        request.setCredentials( "secret" );

        BindResponse resp = connection.bind( request );
        assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );

        Entry entry = connection.lookup( userDn );
        assertEquals( "hnelson", entry.get( "uid" ).getString() );

        connection.close();
    }


    /**
     * Tests to make sure CRAM-MD5 binds below the RootDSE fail if the password is bad.
     */
    @Test
    public void testSaslCramMd5BindBadPassword() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );

        SaslCramMd5Request request = new SaslCramMd5Request();
        request.setUsername( userDn.getRdn().getValue() );
        request.setCredentials( "badsecret" );

        BindResponse resp = connection.bind( request );
        assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, resp.getLdapResult().getResultCode() );
        connection.close();
    }


    /**
     * Tests to make sure DIGEST-MD5 binds below the RootDSE work with
     * SASL Quality of Protection set to 'auth'.
     */
    @Test
    public void testSaslDigestMd5BindSaslQoPAuth() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME,
            getLdapServer().getPort() );

        SaslDigestMd5Request request = new SaslDigestMd5Request();
        request.setUsername( userDn.getRdn().getValue() );
        request.setCredentials( "secret" );
        request.setRealmName( ldapServer.getSaslRealms().get( 0 ) );
        request.setQualityOfProtection( SaslQoP.AUTH );
        BindResponse resp = connection.bind( request );
        assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );

        Entry entry = connection.lookup( userDn );
        assertEquals( "hnelson", entry.get( "uid" ).getString() );

        connection.close();
    }


    /**
     * Tests to make sure DIGEST-MD5 binds below the RootDSE work with
     * SASL Quality of Protection set to 'auth' over ldaps://.
     */
    @Test
    public void testSaslDigestMd5BindSaslQoPAuthOverLdaps() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME,
            getLdapServer().getPortSSL(), true );
        connection.getConfig().setTrustManagers( new NoVerificationTrustManager() );

        SaslDigestMd5Request request = new SaslDigestMd5Request();
        request.setUsername( userDn.getRdn().getValue() );
        request.setCredentials( "secret" );
        request.setRealmName( ldapServer.getSaslRealms().get( 0 ) );
        BindResponse resp = connection.bind( request );
        assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );

        Entry entry = connection.lookup( userDn );
        assertEquals( "hnelson", entry.get( "uid" ).getString() );

        connection.close();
    }


    /**
     * Tests to make sure DIGEST-MD5 binds below the RootDSE work with
     * SASL Quality of Protection set to 'auth-int'.
     */
    @Test
    public void testSaslDigestMd5BindSaslQoPAuthInt() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME,
            getLdapServer().getPort() );

        // Repeat SASL bind to test proper replacement of the SASL filter
        for ( int i = 0; i < 3; i++ )
        {
            SaslDigestMd5Request request = new SaslDigestMd5Request();
            request.setUsername( userDn.getRdn().getValue() );
            request.setCredentials( "secret" );
            request.setRealmName( ldapServer.getSaslRealms().get( 0 ) );
            request.setQualityOfProtection( SaslQoP.AUTH_INT );
            BindResponse resp = connection.bind( request );
            assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );

            Entry entry = connection.lookup( userDn );
            assertEquals( "hnelson", entry.get( "uid" ).getString() );

            testSaslFilter( connection );

            connection.close();
        }
    }


    /**
     * Tests to make sure DIGEST-MD5 binds below the RootDSE work with
     * SASL Quality of Protection set to 'auth-int' over ldaps://.
     */
    @Test
    public void testSaslDigestMd5BindSaslQoPAuthIntOverLdaps() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME,
            getLdapServer().getPortSSL(), true );
        connection.getConfig().setTrustManagers( new NoVerificationTrustManager() );

        // Repeat SASL bind to test proper replacement of the SASL filter
        for ( int i = 0; i < 3; i++ )
        {
            SaslDigestMd5Request request = new SaslDigestMd5Request();
            request.setUsername( userDn.getRdn().getValue() );
            request.setCredentials( "secret" );
            request.setRealmName( ldapServer.getSaslRealms().get( 0 ) );
            request.setQualityOfProtection( SaslQoP.AUTH_INT );
            BindResponse resp = connection.bind( request );
            assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );

            Entry entry = connection.lookup( userDn );
            assertEquals( "hnelson", entry.get( "uid" ).getString() );

            testSaslFilter( connection );

            connection.close();
        }
    }


    /**
     * Tests to make sure DIGEST-MD5 binds below the RootDSE work with
     * SASL Quality of Protection set to 'auth-int' over StartTLS
     */
    @Test
    public void testSaslDigestMd5BindSaslQoPAuthIntOverStartTLS() throws Exception
    {
        ldapServer.setConfidentialityRequired( true );
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME,
            getLdapServer().getPort() );
        connection.getConfig().setTrustManagers( new NoVerificationTrustManager() );

        // Repeat SASL bind to test proper replacement of the SASL filter
        for ( int i = 0; i < 3; i++ )
        {
            connection.startTls();

            SaslDigestMd5Request request = new SaslDigestMd5Request();
            request.setUsername( userDn.getRdn().getValue() );
            request.setCredentials( "secret" );
            request.setRealmName( ldapServer.getSaslRealms().get( 0 ) );
            request.setQualityOfProtection( SaslQoP.AUTH_INT );
            BindResponse resp = connection.bind( request );
            assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );

            Entry entry = connection.lookup( userDn );
            assertEquals( "hnelson", entry.get( "uid" ).getString() );

            testSaslFilter( connection );

            connection.close();
        }
    }


    /**
     * Tests to make sure DIGEST-MD5 binds below the RootDSE work with
     * SASL Quality of Protection set to 'auth-conf'.
     */
    @Test
    public void testSaslDigestMd5BindSaslQoPAuthConf() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME,
            getLdapServer().getPort() );

        // Repeat SASL bind to test proper replacement of the SASL filter
        for ( int i = 0; i < 3; i++ )
        {
            SaslDigestMd5Request request = new SaslDigestMd5Request();
            request.setUsername( userDn.getRdn().getValue() );
            request.setCredentials( "secret" );
            request.setRealmName( ldapServer.getSaslRealms().get( 0 ) );
            request.setQualityOfProtection( SaslQoP.AUTH_CONF );
            BindResponse resp = connection.bind( request );
            assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );

            testSaslFilter( connection );

            connection.close();
        }
    }


    /**
     * Tests to make sure DIGEST-MD5 binds below the RootDSE work with
     * SASL Quality of Protection set to 'auth-conf' over ldaps://.
     */
    @Test
    public void testSaslDigestMd5BindSaslQoPAuthConfOverLdaps() throws Exception
    {
        ldapServer.setConfidentialityRequired( true );
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME,
            getLdapServer().getPortSSL(), true );
        connection.getConfig().setTrustManagers( new NoVerificationTrustManager() );

        // Repeat SASL bind to test proper replacement of the SASL filter
        for ( int i = 0; i < 3; i++ )
        {
            SaslDigestMd5Request request = new SaslDigestMd5Request();
            request.setUsername( userDn.getRdn().getValue() );
            request.setCredentials( "secret" );
            request.setRealmName( ldapServer.getSaslRealms().get( 0 ) );
            request.setQualityOfProtection( SaslQoP.AUTH_CONF );
            BindResponse resp = connection.bind( request );
            assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );

            testSaslFilter( connection );

            connection.close();
        }
    }


    /**
     * Tests to make sure DIGEST-MD5 binds below the RootDSE work with
     * SASL Quality of Protection set to 'auth-conf' over StartTLS.
     */
    @Test
    public void testSaslDigestMd5BindSaslQoPAuthConfOverStartTLS() throws Exception
    {
        ldapServer.setConfidentialityRequired( true );
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME,
            getLdapServer().getPort() );
        connection.getConfig().setTrustManagers( new NoVerificationTrustManager() );

        // Repeat SASL bind to test proper replacement of the SASL filter
        for ( int i = 0; i < 3; i++ )
        {
            connection.startTls();

            SaslDigestMd5Request request = new SaslDigestMd5Request();
            request.setUsername( userDn.getRdn().getValue() );
            request.setCredentials( "secret" );
            request.setRealmName( ldapServer.getSaslRealms().get( 0 ) );
            request.setQualityOfProtection( SaslQoP.AUTH_CONF );
            BindResponse resp = connection.bind( request );
            assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );

            testSaslFilter( connection );

            connection.close();
        }
    }


    /**
     * Run various search and modify operations with small and large payloads
     * to test proper SASL message splitting and re-assembling.
     */
    private void testSaslFilter( LdapNetworkConnection connection )
        throws LdapException, LdapInvalidAttributeValueException
    {
        // lookup Root DSE
        assertTrue( connection.lookup( "", "+", "*" ).containsAttribute( "namingContexts" ) );

        // lookup cn=schema with all schemas enabled which is larger than the SASL max buffer size
        Entry lookup = connection.lookup( "cn=schema", "+", "*" );
        assertTrue( lookup.containsAttribute( "objectClasses", "attributeTypes" ) );

        // subtree search which returns 100 entries
        List<Entry> entries = new ArrayList<>();
        for ( Entry e : connection.search( "ou=schema", "(objectClass=*)", SearchScope.SUBTREE, "+", "*" ) )
        {
            entries.add( e );
        }
        assertEquals( 100, entries.size() );

        // do a small modification
        connection.modify( userDn,
            new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "description", "test" ) );

        // do a medium size modification
        String largeString = RandomStringUtils.randomAscii( 10000 );
        connection.modify( userDn,
            new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "description", largeString ) );
        assertEquals( largeString, connection.lookup( userDn ).get( "description" ).getString() );

        // do a large modification
        byte[] largeBytes = RandomStringUtils.random( 500_000 ).getBytes( StandardCharset.UTF_8 );
        connection.modify( userDn,
            new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, "userCertificate", largeBytes ) );
        assertEquals( largeBytes.length, connection.lookup( userDn ).get( "userCertificate" ).getBytes().length );
    }


    /**
     * Tests to make sure DIGEST-MD5 binds below the RootDSE fail if the realm is bad.
     */
    @Test
    public void testSaslDigestMd5BindBadRealm() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );

        SaslDigestMd5Request request = new SaslDigestMd5Request();
        request.setUsername( userDn.getRdn().getValue() );
        request.setCredentials( "secret" );
        request.setRealmName( "badrealm.com" );
        BindResponse resp = connection.bind( request );
        assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, resp.getLdapResult().getResultCode() );

        connection.close();
    }


    /**
     * Tests to make sure DIGEST-MD5 binds below the RootDSE fail if the password is bad.
     */
    @Test
    public void testSaslDigestMd5BindBadPassword() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );

        SaslDigestMd5Request request = new SaslDigestMd5Request();
        request.setUsername( userDn.getRdn().getValue() );
        request.setCredentials( "badsecret" );
        request.setRealmName( ldapServer.getSaslRealms().get( 0 ) );
        BindResponse resp = connection.bind( request );
        assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, resp.getLdapResult().getResultCode() );

        connection.close();
    }


    /**
     * Tests to make sure GSS-API binds below the RootDSE work.
     */
    @Test
    public void testSaslGssApiBind() throws Exception
    {
        SimpleKdcServer kerbyServer = new SimpleKdcServer();

        String basedir = System.getProperty( "basedir" );
        if (basedir == null) {
            basedir = new File( "." ).getCanonicalPath();
        }

        kerbyServer.setKdcRealm( "EXAMPLE.COM" );
        kerbyServer.setAllowUdp( true );
        kerbyServer.setWorkDir( new File( basedir + "/target" ) );

        kerbyServer.setInnerKdcImpl( new NettyKdcServerImpl( kerbyServer.getKdcSetting() ) );
        kerbyServer.init();

        // Create principals
        String hnelson = "hnelson@EXAMPLE.COM";
        String ldap = "ldap/" + Network.LOOPBACK_HOSTNAME + "@EXAMPLE.COM";
        kerbyServer.createPrincipal( hnelson, "secret");
        kerbyServer.createPrincipal( ldap, "randall" );
        kerbyServer.start();

        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );

        //kdcServer.getConfig().setPaEncTimestampRequired( false );

        SaslGssApiRequest request = new SaslGssApiRequest();
        request.setUsername( userDn.getRdn().getValue() );
        request.setCredentials( "secret" );
        request.setRealmName( ldapServer.getSaslRealms().get( 0 ).toUpperCase( Locale.ROOT ) );
        request.setKdcHost( Network.LOOPBACK_HOSTNAME );
        request.setKdcPort( kerbyServer.getKdcPort() );
        BindResponse resp = connection.bind( request );
        assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );

        Entry entry = connection.lookup( userDn );
        assertEquals( "hnelson", entry.get( "uid" ).getString() );

        connection.close();

        kerbyServer.stop();
    }


    @Test
    public void testSaslGssApiBindSaslQoPAuthConfOverStartTLS() throws Exception
    {
        SimpleKdcServer kerbyServer = new SimpleKdcServer();

        String basedir = System.getProperty( "basedir" );
        if ( basedir == null )
        {
            basedir = new File( "." ).getCanonicalPath();
        }

        kerbyServer.setKdcRealm( "EXAMPLE.COM" );
        kerbyServer.setAllowUdp( true );
        kerbyServer.setWorkDir( new File( basedir + "/target" ) );

        kerbyServer.setInnerKdcImpl( new NettyKdcServerImpl( kerbyServer.getKdcSetting() ) );
        kerbyServer.init();

        // Create principals
        String hnelson = "hnelson@EXAMPLE.COM";
        String ldap = "ldap/" + Network.LOOPBACK_HOSTNAME + "@EXAMPLE.COM";
        kerbyServer.createPrincipal( hnelson, "secret" );
        kerbyServer.createPrincipal( ldap, "randall" );
        kerbyServer.start();

        //kdcServer.getConfig().setPaEncTimestampRequired( false );

        ldapServer.setConfidentialityRequired( true );
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME,
            getLdapServer().getPort() );
        connection.getConfig().setTrustManagers( new NoVerificationTrustManager() );

        // Repeat SASL bind to test proper replacement of the SASL filter
        for ( int i = 0; i < 3; i++ )
        {
            connection.startTls();

            SaslGssApiRequest request = new SaslGssApiRequest();
            request.setUsername( userDn.getRdn().getValue() );
            request.setCredentials( "secret" );
            request.setRealmName( ldapServer.getSaslRealms().get( 0 ).toUpperCase( Locale.ROOT ) );
            request.setKdcHost( Network.LOOPBACK_HOSTNAME );
            request.setKdcPort( kerbyServer.getKdcPort() );
            request.setQualityOfProtection( SaslQoP.AUTH_CONF );
            BindResponse resp = connection.bind( request );
            assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );

            Entry entry = connection.lookup( userDn );
            assertEquals( "hnelson", entry.get( "uid" ).getString() );

            testSaslFilter( connection );

            connection.close();
        }

        kerbyServer.stop();
    }


    /**
     * Tests to make sure GSS-API binds below the RootDSE fail if the realm is bad.
     */
    @Test
    public void testSaslGssApiBindBadRealm() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );

        SaslGssApiRequest request = new SaslGssApiRequest();
        request.setUsername( userDn.getRdn().getValue() );
        request.setCredentials( "secret" );
        request.setRealmName( "badrealm.com" );
        request.setKdcHost( Network.LOOPBACK_HOSTNAME );
        request.setKdcPort( 6088 );
        try
        {
            connection.bind( request );
        }
        catch ( Exception e )
        {
            assertTrue( e instanceof LdapException );
        }
        finally
        {
            connection.close();
        }
    }


    /**
     * Tests to make sure GSS-API binds below the RootDSE fail if the password is bad.
     */
    @Test
    public void testSaslGssApiBindBadPassword() throws Exception
    {
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );

        SaslGssApiRequest request = new SaslGssApiRequest();
        request.setUsername( userDn.getRdn().getValue() );
        request.setCredentials( "badsecret" );
        request.setRealmName( ldapServer.getSaslRealms().get( 0 ).toUpperCase( Locale.ROOT ) );
        request.setKdcHost( Network.LOOPBACK_HOSTNAME );
        request.setKdcPort( 6088 );
        try
        {
            connection.bind( request );
        }
        catch ( Exception e )
        {
            assertTrue( e instanceof LdapException );
        }
        finally
        {
            connection.close();
        }
    }


    /**
     * Tests that the plumbing for NTLM bind works.
     */
    @Test
    public void testNtlmBind() throws Exception
    {
        BogusNtlmProvider provider = getNtlmProviderUsingReflection();

        NtlmSaslBindClient client = new NtlmSaslBindClient( SupportedSaslMechanisms.NTLM );
        BindResponse type2response = client.bindType1( Strings.getBytesUtf8( "type1_test" ) );
        assertEquals( 1, type2response.getMessageId() );
        assertEquals( ResultCodeEnum.SASL_BIND_IN_PROGRESS, type2response.getLdapResult().getResultCode() );
        assertTrue(
            Objects.deepEquals( Strings.getBytesUtf8( "type1_test" ), provider.getType1Response() ) );
        assertTrue(
            Objects.deepEquals( Strings.getBytesUtf8( "challenge" ), type2response.getServerSaslCreds() ) );

        BindResponse finalResponse = client.bindType3( Strings.getBytesUtf8( "type3_test" ) );
        assertEquals( 2, finalResponse.getMessageId() );
        assertEquals( ResultCodeEnum.SUCCESS, finalResponse.getLdapResult().getResultCode() );
        assertTrue(
            Objects.deepEquals( Strings.getBytesUtf8( "type3_test" ), provider.getType3Response() ) );
    }


    /**
     * Tests that the plumbing for NTLM bind works.
     */
    @Test
    public void testGssSpnegoBind() throws Exception
    {
        BogusNtlmProvider provider = new BogusNtlmProvider();

        // the provider configured in @CreateLdapServer only sets for the NTLM mechanism
        // but we use the same NtlmMechanismHandler class for GSS_SPNEGO too but this is a separate
        // instance, so we need to set the provider in the NtlmMechanismHandler instance of GSS_SPNEGO mechanism
        NtlmMechanismHandler ntlmHandler = ( NtlmMechanismHandler ) getLdapServer().getSaslMechanismHandlers().get(
            SupportedSaslMechanisms.GSS_SPNEGO );
        ntlmHandler.setNtlmProvider( provider );

        NtlmSaslBindClient client = new NtlmSaslBindClient( SupportedSaslMechanisms.GSS_SPNEGO );
        BindResponse type2response = client.bindType1( Strings.getBytesUtf8( "type1_test" ) );
        assertEquals( 1, type2response.getMessageId() );
        assertEquals( ResultCodeEnum.SASL_BIND_IN_PROGRESS, type2response.getLdapResult().getResultCode() );
        assertTrue( Objects.deepEquals( Strings.getBytesUtf8( "type1_test" ), provider.getType1Response() ) );
        assertTrue( Objects.deepEquals( Strings.getBytesUtf8( "challenge" ), type2response.getServerSaslCreds() ) );

        BindResponse finalResponse = client.bindType3( Strings.getBytesUtf8( "type3_test" ) );
        assertEquals( 2, finalResponse.getMessageId() );
        assertEquals( ResultCodeEnum.SUCCESS, finalResponse.getLdapResult().getResultCode() );
        assertTrue( Objects.deepEquals( Strings.getBytesUtf8( "type3_test" ), provider.getType3Response() ) );
    }


    /**
     * Test for DIRAPI-30 (Sporadic NullPointerException during SASL bind).
     * Tests multiple connect/bind/unbind/disconnect.
     */
    @Disabled("Activate when DIRAPI-30 is solved")
    @Test
    public void testSequentialBinds() throws Exception
    {
        LdapNetworkConnection connection;
        BindResponse resp;
        Entry entry;

        for ( int i = 0; i < 1000; i++ )
        {
            System.out.println( "try " + i );

            // Digest-MD5
            connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );
            SaslDigestMd5Request digetDigestMd5Request = new SaslDigestMd5Request();
            digetDigestMd5Request.setUsername( userDn.getRdn().getValue() );
            digetDigestMd5Request.setCredentials( "secret" );
            digetDigestMd5Request.setRealmName( ldapServer.getSaslRealms().get( 0 ) );
            resp = connection.bind( digetDigestMd5Request );
            assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );
            entry = connection.lookup( userDn );
            assertEquals( "hnelson", entry.get( "uid" ).getString() );
            connection.close();

            // Cram-MD5
            connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );
            SaslCramMd5Request cramMd5Request = new SaslCramMd5Request();
            cramMd5Request.setUsername( userDn.getRdn().getValue() );
            cramMd5Request.setCredentials( "secret" );
            resp = connection.bind( cramMd5Request );
            assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );
            entry = connection.lookup( userDn );
            assertEquals( "hnelson", entry.get( "uid" ).getString() );
            connection.close();

            // GSSAPI
            connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );
            SaslGssApiRequest gssApiRequest = new SaslGssApiRequest();
            gssApiRequest.setUsername( userDn.getRdn().getValue() );
            gssApiRequest.setCredentials( "secret" );
            gssApiRequest.setRealmName( ldapServer.getSaslRealms().get( 0 ) );
            gssApiRequest.setKdcHost( Network.LOOPBACK_HOSTNAME );
            gssApiRequest.setKdcPort( 6088 );
            resp = connection.bind( gssApiRequest );
            assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );
            entry = connection.lookup( userDn );
            assertEquals( "hnelson", entry.get( "uid" ).getString() );
            connection.close();
        }
    }

    /**
     * A NTLM client
     */
    class NtlmSaslBindClient extends SocketClient
    {
        private final Logger LOG = LoggerFactory.getLogger( NtlmSaslBindClient.class );

        private final String mechanism;


        NtlmSaslBindClient( String mechanism ) throws Exception
        {
            this.mechanism = mechanism;
            setDefaultPort( getLdapServer().getPort() );
            connect( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );
            setTcpNoDelay( false );

            LOG.debug( "isConnected() = {}", isConnected() );
            LOG.debug( "LocalPort     = {}", getLocalPort() );
            LOG.debug( "LocalAddress  = {}", getLocalAddress() );
            LOG.debug( "RemotePort    = {}", getRemotePort() );
            LOG.debug( "RemoteAddress = {}", getRemoteAddress() );
        }


        BindResponse bindType1( byte[] type1response ) throws Exception
        {
            if ( !isConnected() )
            {
                throw new IllegalStateException( "Client is not connected." );
            }

            // Setup the bind request
            BindRequestImpl request = new BindRequestImpl();
            request.setMessageId( 1 );
            request.setDn( new Dn( "uid=admin,ou=system" ) );
            request.setSimple( false );
            request.setCredentials( type1response );
            request.setSaslMechanism( mechanism );
            request.setVersion3( true );

            // Setup the ASN1 Encoder and Decoder
            LdapDecoder decoder = new LdapDecoder();

            // Send encoded request to server
            Asn1Buffer buffer = new Asn1Buffer();
            ByteBuffer bb = LdapEncoder.encodeMessage( buffer, getService().getLdapCodecService(), request );

            bb.flip();

            _output_.write( bb.array() );
            _output_.flush();

            while ( _input_.available() <= 0 )
            {
                Thread.sleep( 100 );
            }

            // Retrieve the response back from server to my last request.
            LdapMessageContainer<? extends Message> container = new LdapMessageContainer(
                ldapServer.getDirectoryService().getLdapCodecService() );
            
            return ( BindResponse ) decoder.decode( _input_, container );
        }


        BindResponse bindType3( byte[] type3response ) throws Exception
        {
            if ( !isConnected() )
            {
                throw new IllegalStateException( "Client is not connected." );
            }

            // Setup the bind request
            BindRequestImpl request = new BindRequestImpl();
            request.setMessageId( 2 );
            request.setDn( new Dn( "uid=admin,ou=system" ) );
            request.setSimple( false );
            request.setCredentials( type3response );
            request.setSaslMechanism( mechanism );
            request.setVersion3( true );

            // Setup the ASN1 Enoder and Decoder
            LdapDecoder decoder = new LdapDecoder();

            // Send encoded request to server
            Asn1Buffer buffer = new Asn1Buffer();
            ByteBuffer bb = LdapEncoder.encodeMessage( buffer, getService().getLdapCodecService(), request );
            bb.flip();

            _output_.write( bb.array() );
            _output_.flush();

            while ( _input_.available() <= 0 )
            {
                Thread.sleep( 100 );
            }

            // Retrieve the response back from server to my last request.
            LdapMessageContainer<? extends Message> container = new LdapMessageContainer(
                ldapServer.getDirectoryService().getLdapCodecService() );
            
            return ( BindResponse ) decoder.decode( _input_, container );
        }
    }


    private BogusNtlmProvider getNtlmProviderUsingReflection()
    {
        BogusNtlmProvider provider = null;
        try
        {
            NtlmMechanismHandler ntlmHandler = ( NtlmMechanismHandler ) getLdapServer().getSaslMechanismHandlers().get(
                SupportedSaslMechanisms.NTLM );

            // there is no getter for 'provider' field hence this hack
            Field field = ntlmHandler.getClass().getDeclaredField( "provider" );
            field.setAccessible( true );
            provider = ( BogusNtlmProvider ) field.get( ntlmHandler );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return provider;
    }


    ////////////////////////
    protected Entry getPrincipalAttributes( String dn, String sn, String cn, String uid, String userPassword,
        String principal ) throws LdapException
    {
        Entry entry = new DefaultEntry( dn );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "person", "inetOrgPerson", "krb5principal", "krb5kdcentry" );
        entry.add( SchemaConstants.CN_AT, cn );
        entry.add( SchemaConstants.SN_AT, sn );
        entry.add( SchemaConstants.UID_AT, uid );
        entry.add( SchemaConstants.USER_PASSWORD_AT, userPassword );
        entry.add( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT, principal );
        entry.add( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT, "0" );

        return entry;
    }

}
