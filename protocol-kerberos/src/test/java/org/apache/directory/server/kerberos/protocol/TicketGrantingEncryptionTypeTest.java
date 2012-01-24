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
package org.apache.directory.server.kerberos.protocol;


import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.protocol.AbstractAuthenticationServiceTest.KrbDummySession;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncTicketPart;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.KdcReq;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.messages.TgsRep;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests various facets of working with encryption types in the Ticket-Granting Service (TGS).
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TicketGrantingEncryptionTypeTest extends AbstractTicketGrantingServiceTest
{
    private KdcServer config;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private KrbDummySession session;


    /**
     * Creates a new instance of {@link TicketGrantingEncryptionTypeTest}.
     */
    @Before
    public void setUp()
    {
        config = new KdcServer();

        /*
         * Body checksum verification must be disabled because we are bypassing
         * the codecs, where the body bytes are set on the KdcReq message.
         */
        config.setBodyChecksumVerified( false );

        store = new MapPrincipalStoreImpl();
        handler = new KerberosProtocolHandler( config, store );
        session = new KrbDummySession();
        lockBox = new CipherTextHandler();
    }


    /**
     * Shutdown the Kerberos server
     */
    @After
    public void shutDown()
    {
        config.stop();
    }


    /**
     * Tests a basic request using DES-CBC-MD5.
     *
     * @throws Exception
     */
    @Test
    public void testRequestDesCbcMd5() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( new PrincipalName( new KerberosPrincipal( "ldap/ldap.example.com@EXAMPLE.COM" ) ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );

        Set<EncryptionType> encryptionTypes = new HashSet<EncryptionType>();
        encryptionTypes.add( EncryptionType.DES_CBC_MD5 );

        kdcReqBody.setEType( encryptionTypes );

        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertEquals( "Encryption type", EncryptionType.DES_CBC_MD5, reply.getEncPart().getEType() );
    }


    /**
     * Tests the use of a TGT containing a DES-CBC-MD5 session key while the
     * requested encryption type is AES-128.
     *
     * @throws Exception
     */
    @Test
    public void testRequestAes128() throws Exception
    {
        EncryptionType[] configuredEncryptionTypes =
            { EncryptionType.AES128_CTS_HMAC_SHA1_96 };
        config.setEncryptionTypes( configuredEncryptionTypes );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );

        Set<EncryptionType> encryptionTypes = new HashSet<EncryptionType>();
        encryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        kdcReqBody.setEType( encryptionTypes );

        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertEquals( "Encryption type", EncryptionType.DES_CBC_MD5, reply.getEncPart().getEType() );
        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getTicket().getEncPart()
            .getEType() );
    }


    /**
     * Tests the use of a TGT containing an AES-128 session key while the
     * requested encryption type is also AES-128.
     *
     * @throws Exception
     */
    @Test
    public void testRequestAes128TgtAndRequested() throws Exception
    {
        EncryptionType[] configuredEncryptionTypes =
            { EncryptionType.AES128_CTS_HMAC_SHA1_96 };
        config.setEncryptionTypes( configuredEncryptionTypes );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        sessionKey = RandomKeyFactory.getRandomKey( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        encTicketPart.setKey( sessionKey );

        // Seal the ticket for the server.
        String principalName = "krbtgt/EXAMPLE.COM@EXAMPLE.COM";
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( principalName );
        String passPhrase = "randomKey";
        Set<EncryptionType> preAuthEncryptionTypes = new HashSet<EncryptionType>();
        preAuthEncryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Map<EncryptionType, EncryptionKey> keyMap = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase,
            preAuthEncryptionTypes );
        EncryptionKey serverKey = keyMap.get( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );

        Set<EncryptionType> encryptionTypes = new HashSet<EncryptionType>();
        encryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        kdcReqBody.setEType( encryptionTypes );

        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getEncPart().getEType() );
        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getTicket().getEncPart()
            .getEType() );
    }


    /**
     * Tests that the client-chosen nonce is correctly returned in the response.
     *
     * @throws Exception
     */
    @Test
    public void testNonce() throws Exception
    {
        EncryptionType[] configuredEncryptionTypes =
            { EncryptionType.AES128_CTS_HMAC_SHA1_96 };
        config.setEncryptionTypes( configuredEncryptionTypes );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        sessionKey = RandomKeyFactory.getRandomKey( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        encTicketPart.setKey( sessionKey );

        // Seal the ticket for the server.
        String principalName = "krbtgt/EXAMPLE.COM@EXAMPLE.COM";
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( principalName );
        String passPhrase = "randomKey";
        Set<EncryptionType> preAuthEncryptionTypes = new HashSet<EncryptionType>();
        preAuthEncryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Map<EncryptionType, EncryptionKey> keyMap = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase,
            preAuthEncryptionTypes );
        EncryptionKey serverKey = keyMap.get( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );

        Set<EncryptionType> encryptionTypes = new HashSet<EncryptionType>();
        encryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        kdcReqBody.setEType( encryptionTypes );

        int nonce = random.nextInt();
        kdcReqBody.setNonce( nonce );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getEncPart().getEType() );
        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getTicket().getEncPart()
            .getEType() );

        assertEquals( "Nonce", nonce, reply.getNonce() );
    }


    /**
     * Tests that the default reply key is the session key from the TGT.
     *
     * @throws Exception
     */
    @Test
    public void testDecryptWithSessionKey() throws Exception
    {
        EncryptionType[] configuredEncryptionTypes =
            { EncryptionType.AES128_CTS_HMAC_SHA1_96 };
        config.setEncryptionTypes( configuredEncryptionTypes );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        sessionKey = RandomKeyFactory.getRandomKey( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        encTicketPart.setKey( sessionKey );

        // Seal the ticket for the server.
        String principalName = "krbtgt/EXAMPLE.COM@EXAMPLE.COM";
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( principalName );
        String passPhrase = "randomKey";
        Set<EncryptionType> preAuthEncryptionTypes = new HashSet<EncryptionType>();
        preAuthEncryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Map<EncryptionType, EncryptionKey> keyMap = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase,
            preAuthEncryptionTypes );
        EncryptionKey serverKey = keyMap.get( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );

        Set<EncryptionType> encryptionTypes = new HashSet<EncryptionType>();
        encryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        kdcReqBody.setEType( encryptionTypes );

        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getEncPart().getEType() );
        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getTicket().getEncPart()
            .getEType() );
    }


    /**
     * Tests when a sub-session key is placed in the Authenticator that the
     * reply key is the sub-session key and not the TGT session key.
     *
     * @throws Exception
     */
    @Test
    public void testDecryptWithSubSessionKey() throws Exception
    {
        EncryptionType[] configuredEncryptionTypes =
            { EncryptionType.AES128_CTS_HMAC_SHA1_96 };
        config.setEncryptionTypes( configuredEncryptionTypes );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        sessionKey = RandomKeyFactory.getRandomKey( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        encTicketPart.setKey( sessionKey );

        // Seal the ticket for the server.
        String principalName = "krbtgt/EXAMPLE.COM@EXAMPLE.COM";
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( principalName );
        String passPhrase = "randomKey";
        Set<EncryptionType> preAuthEncryptionTypes = new HashSet<EncryptionType>();
        preAuthEncryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Map<EncryptionType, EncryptionKey> keyMap = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase,
            preAuthEncryptionTypes );
        EncryptionKey serverKey = keyMap.get( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );

        Set<EncryptionType> encryptionTypes = new HashSet<EncryptionType>();
        encryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        kdcReqBody.setEType( encryptionTypes );

        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        subSessionKey = RandomKeyFactory.getRandomKey( EncryptionType.DES_CBC_MD5 );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertEquals( "Encryption type", EncryptionType.DES_CBC_MD5, reply.getEncPart().getEType() );
        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getTicket().getEncPart()
            .getEType() );
    }
}
