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


import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.TicketGrantReply;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPartModifier;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KdcOptions;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBody;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBodyModifier;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;


/**
 * Tests various facets of working with encryption types in the Ticket-Granting Service (TGS).
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TicketGrantingEncryptionTypeTest extends AbstractTicketGrantingServiceTest
{
    private KdcServer config;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private DummySession session;


    /**
     * Creates a new instance of {@link TicketGrantingEncryptionTypeTest}.
     */
    public TicketGrantingEncryptionTypeTest()
    {
        config = new KdcServer( null, null, null );

        /*
         * Body checksum verification must be disabled because we are bypassing
         * the codecs, where the body bytes are set on the KdcRequest message.
         */
        config.setBodyChecksumVerified( false );

        store = new MapPrincipalStoreImpl();
        handler = new KerberosProtocolHandler( config, store );
        session = new DummySession();
        lockBox = new CipherTextHandler();
    }


    /**
     * Tests a basic request using DES-CBC-MD5.
     *
     * @throws Exception
     */
    public void testRequestDesCbcMd5() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );

        EncryptionType[] encryptionTypes =
                {EncryptionType.DES_CBC_MD5};

        modifier.setEType( encryptionTypes );

        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertEquals( "Encryption type", EncryptionType.DES_CBC_MD5, reply.getEncPart().getEncryptionType() );
    }


    /**
     * Tests the use of a TGT containing a DES-CBC-MD5 session key while the
     * requested encryption type is AES-128.
     *
     * @throws Exception
     */
    public void testRequestAes128() throws Exception
    {
        EncryptionType[] configuredEncryptionTypes =
                {EncryptionType.AES128_CTS_HMAC_SHA1_96};
        config.setEncryptionTypes( configuredEncryptionTypes );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );

        EncryptionType[] encryptionTypes =
                {EncryptionType.AES128_CTS_HMAC_SHA1_96};

        modifier.setEType( encryptionTypes );

        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertEquals( "Encryption type", EncryptionType.DES_CBC_MD5, reply.getEncPart().getEncryptionType() );
        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getTicket().getEncPart()
                .getEncryptionType() );
    }


    /**
     * Tests the use of a TGT containing an AES-128 session key while the
     * requested encryption type is also AES-128.
     *
     * @throws Exception
     */
    public void testRequestAes128TgtAndRequested() throws Exception
    {
        EncryptionType[] configuredEncryptionTypes =
                {EncryptionType.AES128_CTS_HMAC_SHA1_96};
        config.setEncryptionTypes( configuredEncryptionTypes );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        sessionKey = RandomKeyFactory.getRandomKey( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        encTicketPartModifier.setSessionKey( sessionKey );

        // Seal the ticket for the server.
        String principalName = "krbtgt/EXAMPLE.COM@EXAMPLE.COM";
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( principalName );
        String passPhrase = "randomKey";
        Set<EncryptionType> preAuthEncryptionTypes = new HashSet<EncryptionType>();
        preAuthEncryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Map<EncryptionType, EncryptionKey> keyMap = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase,
                preAuthEncryptionTypes );
        EncryptionKey serverKey = keyMap.get( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );

        EncryptionType[] encryptionTypes =
                {EncryptionType.AES128_CTS_HMAC_SHA1_96};

        modifier.setEType( encryptionTypes );

        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getEncPart().getEncryptionType() );
        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getTicket().getEncPart()
                .getEncryptionType() );
    }


    /**
     * Tests that the client-chosen nonce is correctly returned in the response.
     *
     * @throws Exception
     */
    public void testNonce() throws Exception
    {
        EncryptionType[] configuredEncryptionTypes =
                {EncryptionType.AES128_CTS_HMAC_SHA1_96};
        config.setEncryptionTypes( configuredEncryptionTypes );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        sessionKey = RandomKeyFactory.getRandomKey( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        encTicketPartModifier.setSessionKey( sessionKey );

        // Seal the ticket for the server.
        String principalName = "krbtgt/EXAMPLE.COM@EXAMPLE.COM";
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( principalName );
        String passPhrase = "randomKey";
        Set<EncryptionType> preAuthEncryptionTypes = new HashSet<EncryptionType>();
        preAuthEncryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Map<EncryptionType, EncryptionKey> keyMap = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase,
                preAuthEncryptionTypes );
        EncryptionKey serverKey = keyMap.get( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );

        EncryptionType[] encryptionTypes =
                {EncryptionType.AES128_CTS_HMAC_SHA1_96};

        modifier.setEType( encryptionTypes );

        int nonce = random.nextInt();
        modifier.setNonce( nonce );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getEncPart().getEncryptionType() );
        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getTicket().getEncPart()
                .getEncryptionType() );

        assertEquals( "Nonce", nonce, reply.getNonce() );
    }


    /**
     * Tests that the default reply key is the session key from the TGT.
     *
     * @throws Exception
     */
    public void testDecryptWithSessionKey() throws Exception
    {
        EncryptionType[] configuredEncryptionTypes =
                {EncryptionType.AES128_CTS_HMAC_SHA1_96};
        config.setEncryptionTypes( configuredEncryptionTypes );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        sessionKey = RandomKeyFactory.getRandomKey( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        encTicketPartModifier.setSessionKey( sessionKey );

        // Seal the ticket for the server.
        String principalName = "krbtgt/EXAMPLE.COM@EXAMPLE.COM";
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( principalName );
        String passPhrase = "randomKey";
        Set<EncryptionType> preAuthEncryptionTypes = new HashSet<EncryptionType>();
        preAuthEncryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Map<EncryptionType, EncryptionKey> keyMap = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase,
                preAuthEncryptionTypes );
        EncryptionKey serverKey = keyMap.get( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );

        EncryptionType[] encryptionTypes =
                {EncryptionType.AES128_CTS_HMAC_SHA1_96};

        modifier.setEType( encryptionTypes );

        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getEncPart().getEncryptionType() );
        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getTicket().getEncPart()
                .getEncryptionType() );
    }


    /**
     * Tests when a sub-session key is placed in the Authenticator that the
     * reply key is the sub-session key and not the TGT session key.
     *
     * @throws Exception
     */
    public void testDecryptWithSubSessionKey() throws Exception
    {
        EncryptionType[] configuredEncryptionTypes =
                {EncryptionType.AES128_CTS_HMAC_SHA1_96};
        config.setEncryptionTypes( configuredEncryptionTypes );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        sessionKey = RandomKeyFactory.getRandomKey( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        encTicketPartModifier.setSessionKey( sessionKey );

        // Seal the ticket for the server.
        String principalName = "krbtgt/EXAMPLE.COM@EXAMPLE.COM";
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( principalName );
        String passPhrase = "randomKey";
        Set<EncryptionType> preAuthEncryptionTypes = new HashSet<EncryptionType>();
        preAuthEncryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Map<EncryptionType, EncryptionKey> keyMap = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase,
                preAuthEncryptionTypes );
        EncryptionKey serverKey = keyMap.get( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );

        EncryptionType[] encryptionTypes =
                {EncryptionType.AES128_CTS_HMAC_SHA1_96};

        modifier.setEType( encryptionTypes );

        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        subSessionKey = RandomKeyFactory.getRandomKey( EncryptionType.DES_CBC_MD5 );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertEquals( "Encryption type", EncryptionType.DES_CBC_MD5, reply.getEncPart().getEncryptionType() );
        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getTicket().getEncPart()
                .getEncryptionType() );
    }
}
