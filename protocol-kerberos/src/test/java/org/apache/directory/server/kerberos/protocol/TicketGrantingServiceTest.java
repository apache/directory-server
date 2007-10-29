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


import java.net.InetAddress;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.shared.KerberosMessageType;
import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.ErrorMessage;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.TicketGrantReply;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPartModifier;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KdcOptions;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBody;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBodyModifier;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;


/**
 * Tests the Ticket-Granting Service (TGS) via the {@link KerberosProtocolHandler}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TicketGrantingServiceTest extends AbstractTicketGrantingServiceTest
{
    private KdcServer config;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private DummySession session;


    /**
     * Creates a new instance of {@link TicketGrantingServiceTest}.
     */
    public TicketGrantingServiceTest()
    {
        config = new KdcServer();

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
     * Tests the default minimum request, which consists of as little as the
     * client name, service name, realm, till time, nonce, and encryption types.
     * 
     * This is the request archetype.
     * 
     * "The TGS exchange between a client and the Kerberos TGS is initiated by a
     * client ... when it seeks to obtain authentication credentials for a given
     * server (which might be registered in a remote realm)."
     * 
     * "In the first case, the client must already have acquired a ticket for the
     * Ticket-Granting Service using the AS exchange (the TGT is usually obtained
     * when a client initially authenticates to the system, such as when a user
     * logs in)."
     * 
     * @throws Exception 
     */
    public void testRequestArchetype() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();

        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );
    }


    /**
     * Tests the protocol version number, which must be '5'.
     */
    public void testProtocolVersionNumber()
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcRequest message = new KdcRequest( 4, KerberosMessageType.TGS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Requested protocol version number not supported", 3, error.getErrorCode() );
    }


    /**
     * Tests that a non-existent server principal returns the correct error message.
     * 
     * @throws Exception 
     */
    public void testServerNotFound() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "badservice" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();

        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Server not found in Kerberos database", 7, error.getErrorCode() );
    }


    /**
     * Tests when no ticket is found in the auth header that the request is rejected
     * with the correct error message.
     * 
     * "If no ticket can be found in the padata field, the KDC_ERR_PADATA_TYPE_NOSUPP
     * error is returned."
     * 
     * @throws Exception 
     */
    public void testNoTicketFound() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();

        // Get the session key from the service ticket.
        sessionKey = tgt.getEncTicketPart().getSessionKey();

        // Generate a new sequence number.
        sequenceNumber = random.nextInt();
        now = new KerberosTime();

        KdcRequest message = new KdcRequest( 5, KerberosMessageType.TGS_REQ, null, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC has no support for padata type", 16, error.getErrorCode() );
    }


    /**
     * Tests that an inappropriate checksum returns the correct error message.
     * 
     * @throws Exception 
     */
    public void testInappropriateChecksum() throws Exception
    {
        config.setBodyChecksumVerified( true );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "tquist@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();

        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Inappropriate type of checksum in message", 50, error.getErrorCode() );
    }


    /**
     * Tests that an inappropriate checksum returns the correct error message.
     * 
     * @throws Exception 
     */
    public void testChecksumTypeNoSupport() throws Exception
    {
        config.setBodyChecksumVerified( true );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "tquist@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();

        try
        {
            getKdcRequest( tgt, requestBody, ChecksumType.DES_MAC_K );
        }
        catch ( KerberosException ke )
        {
            assertEquals( "KDC has no support for checksum type", 15, ke.getErrorCode() );
        }
    }


    /**
     * "If any of the decryptions indicate failed integrity checks, the
     * KRB_AP_ERR_BAD_INTEGRITY error is returned."
     * 
     * @throws Exception
     */
    public void testIntegrityCheckedFailed() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "badpassword";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();

        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Integrity check on decrypted field failed", 31, error.getErrorCode() );
    }


    /**
     * Tests when the ticket isn't for us that the correct error message is returned.
     * 
     * @throws Exception
     */
    public void testNotUs() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@APACHE.ORG" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();

        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "The ticket isn't for us", 35, error.getErrorCode() );
    }


    /**
     * "The TGS exchange between a client and the Kerberos TGS is initiated by a
     * client when ... it seeks to renew an existing ticket."
     * 
     * @throws Exception 
     */
    public void testRenewTicket() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEW );
        modifier.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();

        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        KerberosTime expectedRenewTillTime = tgt.getEncTicketPart().getRenewTill();
        boolean isClose = Math.abs( reply.getRenewTill().getTime() - expectedRenewTillTime.getTime() ) < 5000;
        assertTrue( "Expected renew till time", isClose );
    }


    /**
     * "The TGS exchange between a client and the Kerberos TGS is initiated by a
     * client when ... it seeks to validate an existing ticket."
     * 
     * @throws Exception 
     */
    public void testValidateTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.INVALID );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.VALIDATE );
        modifier.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();

        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        KerberosTime expectedRenewTillTime = tgt.getEncTicketPart().getRenewTill();
        boolean isClose = Math.abs( reply.getRenewTill().getTime() - expectedRenewTillTime.getTime() ) < 5000;
        assertTrue( "Expected renew till time", isClose );
    }


    /**
     * "The TGS exchange between a client and the Kerberos TGS is initiated by a
     * client when ... it seeks to obtain a proxy ticket."
     * 
     * @throws Exception 
     */
    public void testProxyTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.PROXIABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.PROXY );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        HostAddress[] address =
            { new HostAddress( InetAddress.getByName( null ) ) };
        HostAddresses addresses = new HostAddresses( address );
        modifier.setAddresses( addresses );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertTrue( "PROXY flag", reply.getFlags().get( TicketFlags.PROXY ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "PROXY flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.PROXY ) );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.INVALID ) );

        assertNotNull( reply.getTicket().getEncTicketPart().getClientAddresses() );
    }


    /**
     * "The TGS exchange between a client and the Kerberos TGS is initiated by a
     * client when ... it seeks to obtain a forwarded ticket."
     * 
     * @throws Exception 
     */
    public void testForwardedTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.FORWARDABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.FORWARDED );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        HostAddress[] address =
            { new HostAddress( InetAddress.getByName( null ) ) };
        HostAddresses addresses = new HostAddresses( address );
        modifier.setAddresses( addresses );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertTrue( "FORWARDED flag", reply.getFlags().get( TicketFlags.FORWARDED ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "FORWARDED flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.FORWARDED ) );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.INVALID ) );

        assertNotNull( reply.getTicket().getEncTicketPart().getClientAddresses() );
    }


    /**
     * As is the case for all application servers, expired tickets are not
     * accepted by the TGS, so once a renewable or TGT expires, the client
     * must use a separate exchange to obtain valid tickets.
     * 
     * @throws Exception 
     */
    public void testExpiredTgt() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setEndTime( new KerberosTime( 0 ) );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Ticket expired", 32, error.getErrorCode() );
    }


    /**
     * As is the case for all application servers, expired tickets are not accepted
     * by the TGS, so once a renewable or TGT expires, the client must use a separate
     * exchange to obtain valid tickets.
     * 
     * @throws Exception 
     */
    public void testExpiredRenewableTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.RENEWABLE );
        encTicketPartModifier.setRenewTill( new KerberosTime( 0 ) );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "ldap/ldap.example.com@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEW );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Ticket expired", 32, error.getErrorCode() );
    }


    /**
     * Tests when a renewable ticket is presented for renewal, that if the RENEW
     * flag is NOT set, the ticket is renewed for the endtime of the presented
     * ticket, as though it were a TGT.
     *
     * @throws Exception
     */
    public void testRenewableTicketNoRenew() throws Exception
    {
        long now = System.currentTimeMillis();

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.RENEWABLE );
        encTicketPartModifier.setStartTime( new KerberosTime( now - KerberosTime.DAY / 2 ) );
        encTicketPartModifier.setEndTime( new KerberosTime( now + KerberosTime.DAY / 2 ) );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "ldap/ldap.example.com@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY / 2 );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        KerberosTime expectedEndTime = tgt.getEncTicketPart().getEndTime();
        boolean isClose = Math.abs( reply.getEndTime().getTime() - expectedEndTime.getTime() ) < 5000;
        assertTrue( "Expected renew till time", isClose );
    }


    /**
     * Tests when a renewable ticket is presented for renewal, that if the RENEW
     * flag is set, the ticket is renewed for the lifetime of the presented ticket.
     *
     * @throws Exception
     */
    public void testRenewableTicketRenewal() throws Exception
    {
        long now = System.currentTimeMillis();

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.RENEWABLE );
        encTicketPartModifier.setStartTime( new KerberosTime( now - KerberosTime.DAY / 2 ) );
        encTicketPartModifier.setEndTime( new KerberosTime( now + KerberosTime.DAY / 2 ) );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "ldap/ldap.example.com@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEW );
        modifier.setKdcOptions( kdcOptions );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY / 2 );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        KerberosTime expectedEndTime = new KerberosTime( now + KerberosTime.DAY );
        boolean isClose = Math.abs( reply.getEndTime().getTime() - expectedEndTime.getTime() ) < 5000;
        assertTrue( "Expected renew till time", isClose );
    }


    /**
     * Test when an unsupported encryption type is requested, that the request is
     * rejected with the correct error message.
     * 
     * "If the server cannot accommodate any encryption type requested by the
     * client, an error message with code KDC_ERR_ETYPE_NOSUPP is returned."
     * 
     * @throws Exception 
     */
    public void testEncryptionTypeNoSupport() throws Exception
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );

        EncryptionType[] encryptionTypes = new EncryptionType[]
            { EncryptionType.DES3_CBC_MD5 };

        modifier.setEType( encryptionTypes );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KdcRequest message = new KdcRequest( 5, KerberosMessageType.TGS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC has no support for encryption type", 14, error.getErrorCode() );
    }


    /**
     * Tests that when a server principal is not configured with Kerberos keys that
     * the correct error message is returned.
     * 
     * @throws Exception 
     */
    public void testServerNullKey() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "tquist" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();

        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "The client or server has a null key", 9, error.getErrorCode() );
    }


    /**
     * Tests when the starttime is absent and the POSTDATED option has not been
     * specified, that the starttime of the ticket is set to the authentication
     * server's current time.
     * 
     * "If the requested starttime is absent, indicates a time in the past,
     * or is within the window of acceptable clock skew for the KDC and the
     * POSTDATE option has not been specified, then the starttime of the
     * ticket is set to the authentication server's current time."
     * 
     * @throws Exception 
     */
    public void testStartTimeAbsentNoPostdate() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        KerberosTime expectedStartTime = new KerberosTime( now );
        boolean isClose = reply.getStartTime() == null
            || Math.abs( reply.getStartTime().getTime() - expectedStartTime.getTime() ) < 5000;
        assertTrue( "Expected start time", isClose );
    }


    /**
     * Tests when the starttime indicates a time in the past and the POSTDATED option
     * has not been specified, that the starttime of the ticket is set to the
     * authentication server's current time.
     * 
     * "If the requested starttime is absent, indicates a time in the past,
     * or is within the window of acceptable clock skew for the KDC and the
     * POSTDATE option has not been specified, then the starttime of the
     * ticket is set to the authentication server's current time."
     * 
     * @throws Exception 
     */
    public void testStartTimeInThePastNoPostdate() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + -1 * KerberosTime.DAY );
        modifier.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        KerberosTime expectedStartTime = new KerberosTime( now );
        boolean isClose = reply.getStartTime() == null
            || Math.abs( reply.getStartTime().getTime() - expectedStartTime.getTime() ) < 5000;
        assertTrue( "Expected start time", isClose );
    }


    /**
     * Tests when the starttime is within the window of acceptable clock skew for
     * the KDC and the POSTDATED option has not been specified, that the starttime
     * of the ticket is set to the authentication server's current time.
     * 
     * "If the requested starttime is absent, indicates a time in the past,
     * or is within the window of acceptable clock skew for the KDC and the
     * POSTDATE option has not been specified, then the starttime of the
     * ticket is set to the authentication server's current time."
     * 
     * @throws Exception 
     */
    public void testStartTimeAcceptableClockSkewNoPostdate() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now );
        modifier.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        KerberosTime expectedStartTime = new KerberosTime( now );
        boolean isClose = reply.getStartTime() == null
            || Math.abs( reply.getStartTime().getTime() - expectedStartTime.getTime() ) < 5000;
        assertTrue( "Expected start time", isClose );
    }


    /**
     * Tests when a start time is after an end time that the request is rejected with the
     * correct error message.
     * 
     * "If the requested expiration time minus the starttime (as determined above)
     * is less than a site-determined minimum lifetime, an error message with code
     * KDC_ERR_NEVER_VALID is returned."
     *
     * @throws Exception
     */
    public void testStartTimeOrderNeverValid() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.MAY_POSTDATE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.POSTDATED );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Requested start time is later than end time", 11, error.getErrorCode() );
    }


    /**
     * Tests when the absolute value of the difference between the start time is
     * and the end time is less than a configured minimum, that the request is
     * rejected with the correct error message.
     * 
     * "If the requested expiration time minus the starttime (as determined above)
     * is less than a site-determined minimum lifetime, an error message with code
     * KDC_ERR_NEVER_VALID is returned."
     *
     * @throws Exception
     */
    public void testStartTimeMinimumNeverValid() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.MAY_POSTDATE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now );
        modifier.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + 4 * KerberosTime.MINUTE );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Requested start time is later than end time", 11, error.getErrorCode() );
    }


    /**
     * Tests when a valid starttime is specified but the POSTDATE flag is not set,
     * that the request is rejected with the correct error message.
     * 
     * "If it indicates a time in the future beyond the acceptable clock skew, but
     * the POSTDATED option has not been specified, then the error
     * KDC_ERR_CANNOT_POSTDATE is returned."
     * 
     * @throws Exception 
     */
    public void testStartTimeNoPostdated() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.MAY_POSTDATE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + 10 * KerberosTime.MINUTE );
        modifier.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Ticket not eligible for postdating", 10, error.getErrorCode() );
    }


    /**
     * Tests that a user-specified start time is honored when that start time does not
     * violate policy.
     * 
     * "Otherwise the requested starttime is checked against the policy of the local
     * realm (the administrator might decide to prohibit certain types or ranges of
     * postdated tickets), and if the ticket's starttime is acceptable, it is set as
     * requested, and the INVALID flag is set in the new ticket.  The postdated
     * ticket MUST be validated before use by presenting it to the KDC after the
     * starttime has been reached."
     * 
     * "If the new ticket is postdated (the starttime is in the future), its
     * INVALID flag will also be set."
     * 
     * "The flags field of the new ticket will have the following options set
     * if they have been requested and if the policy of the local realm
     * allows:  FORWARDABLE, MAY-POSTDATE, POSTDATED, PROXIABLE, RENEWABLE."
     * 
     * @throws Exception
     */
    public void testSpecificStartTime() throws Exception
    {
        long now = System.currentTimeMillis();

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.MAY_POSTDATE );
        // Service ticket end time will be limited by TGT end time.
        encTicketPartModifier.setEndTime( new KerberosTime( now + 3 * KerberosTime.DAY ) );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.POSTDATED );
        modifier.setKdcOptions( kdcOptions );

        KerberosTime requestedStartTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + 2 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertTrue( "Requested start time", requestedStartTime.equals( reply.getStartTime() ) );
        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );
        assertTrue( "POSTDATED flag", reply.getFlags().get( TicketFlags.POSTDATED ) );
        assertTrue( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "Requested start time", requestedStartTime.equals( reply.getTicket().getEncTicketPart().getStartTime() ) );
        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );
        assertTrue( "POSTDATED flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.POSTDATED ) );
        assertTrue( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.INVALID ) );
    }


    /**
     * Tests when pre-authentication used during initial authentication, that the flag
     * is carried forward to derivative tickets.
     *
     * @throws Exception
     */
    public void testPreAuthenticationFlag() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.PRE_AUTHENT );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertTrue( "PRE_AUTHENT flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.PRE_AUTHENT ) );
    }


    /**
     * Tests that a user-specified end time is honored when that end time does not
     * violate policy.
     * 
     * "The expiration time of the ticket will be set to the earlier of the
     * requested endtime and a time determined by local policy, possibly by
     * using realm- or principal-specific factors."
     *
     * @throws Exception
     */
    public void testSpecificEndTime() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY / 2 );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );
    }


    /**
     * Tests when an end time is requested that exceeds the maximum end time as 
     * configured in policy that the maximum allowable end time is returned instead
     * of the requested end time.
     * 
     * "The expiration time of the ticket will be set to the earlier of the
     * requested endtime and a time determined by local policy, possibly by
     * using realm- or principal-specific factors."
     *
     * @throws Exception
     */
    public void testEndTimeExceedsMaximumAllowable() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.WEEK );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        KerberosTime expectedEndTime = new KerberosTime( now + KerberosTime.DAY );
        boolean isClose = Math.abs( reply.getEndTime().getTime() - expectedEndTime.getTime() ) < 5000;
        assertTrue( "Expected end time", isClose );
    }


    /**
     * Tests that a requested zulu end time of the epoch ("19700101000000Z") results
     * in the maximum endtime permitted according to KDC policy.  The zulu epoch is
     * the same as '0' (zero) milliseconds in Java.
     * 
     * @throws Exception
     */
    public void testEpochEndTime() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        modifier.setKdcOptions( new KdcOptions() );

        String epoch = "19700101000000Z";
        KerberosTime requestedEndTime = KerberosTime.getTime( epoch );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();

        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        long now = System.currentTimeMillis();
        KerberosTime expectedEndTime = new KerberosTime( now + KerberosTime.DAY );
        boolean isClose = Math.abs( reply.getEndTime().getTime() - expectedEndTime.getTime() ) < 5000;
        assertTrue( "Expected end time", isClose );
    }


    /**
     * Tests whether a renewable ticket will be accepted in lieu of a non-renewable
     * ticket if the requested ticket expiration date cannot be satisfied by a
     * non-renewable ticket (due to configuration constraints).
     * 
     * "If the requested expiration time for the ticket exceeds what was determined
     * as above, and if the 'RENEWABLE-OK' option was requested, then the 'RENEWABLE'
     * flag is set in the new ticket, and the renew-till value is set as if the
     * 'RENEWABLE' option were requested (the field and option names are described
     * fully in Section 5.4.1).
     * 
     * @throws Exception 
     */
    public void testRenewableOk() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.RENEWABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE_OK );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.WEEK );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        KerberosTime expectedEndTime = new KerberosTime( now + KerberosTime.DAY );
        boolean isClose = Math.abs( reply.getEndTime().getTime() - expectedEndTime.getTime() ) < 5000;
        assertTrue( "Expected end time", isClose );

        assertTrue( "RENEWABLE flag", reply.getFlags().get( TicketFlags.RENEWABLE ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        KerberosTime expectedRenewTillTime = new KerberosTime( now + KerberosTime.WEEK );
        isClose = Math.abs( reply.getRenewTill().getTime() - expectedRenewTillTime.getTime() ) < 5000;
        assertTrue( "Expected renew-till time", isClose );
    }


    /**
     * Tests forwardable tickets.
     * 
     * "The flags field of the new ticket will have the following options set
     * if they have been requested and if the policy of the local realm
     * allows:  FORWARDABLE, MAY-POSTDATE, POSTDATED, PROXIABLE, RENEWABLE."
     * 
     * @throws Exception 
     */
    public void testForwardableTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.FORWARDABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.FORWARDABLE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertTrue( "FORWARDABLE flag", reply.getFlags().get( TicketFlags.FORWARDABLE ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "FORWARDABLE flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.FORWARDABLE ) );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.INVALID ) );
    }


    /**
     * Tests allow postdating of derivative tickets.
     * 
     * "The flags field of the new ticket will have the following options set
     * if they have been requested and if the policy of the local realm
     * allows:  FORWARDABLE, MAY-POSTDATE, POSTDATED, PROXIABLE, RENEWABLE."
     * 
     * @throws Exception 
     */
    public void testAllowPostdate() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.MAY_POSTDATE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.ALLOW_POSTDATE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertTrue( "MAY_POSTDATE flag", reply.getFlags().get( TicketFlags.MAY_POSTDATE ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "MAY_POSTDATE flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.MAY_POSTDATE ) );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.INVALID ) );
    }


    /**
     * Tests proxiable tickets.
     * 
     * "The flags field of the new ticket will have the following options set
     * if they have been requested and if the policy of the local realm
     * allows:  FORWARDABLE, MAY-POSTDATE, POSTDATED, PROXIABLE, RENEWABLE."
     * 
     * @throws Exception 
     */
    public void testProxiableTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.PROXIABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.PROXIABLE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertTrue( "PROXIABLE flag", reply.getFlags().get( TicketFlags.PROXIABLE ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "PROXIABLE flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.PROXIABLE ) );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.INVALID ) );
    }


    /**
     * Tests that a user-specified renew-till time is honored when that renew-till
     * time does not violate policy.
     * 
     * "If the RENEWABLE option has been requested or if the RENEWABLE-OK
     * option has been set and a renewable ticket is to be issued, then the
     * renew-till field MAY be set to the earliest of ... its requested value [or]
     * the starttime of the ticket plus the maximum renewable lifetime
     * set by the policy of the local realm."
     * 
     * @throws Exception 
     */
    public void testRenewableTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.RENEWABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosTime requestedRenewTillTime = new KerberosTime( now + KerberosTime.WEEK / 2 );
        modifier.setRtime( requestedRenewTillTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertTrue( "RENEWABLE flag", reply.getFlags().get( TicketFlags.RENEWABLE ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "RENEWABLE flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.RENEWABLE ) );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "Requested renew-till time", requestedRenewTillTime.equals( reply.getRenewTill() ) );
    }


    /**
     * Tests when a renew-till time is requested that exceeds the maximum renew-till
     * time as configured in policy that the maximum allowable renew-till time is
     * returned instead of the requested renew-till time.
     * 
     * "If the RENEWABLE option has been requested or if the RENEWABLE-OK
     * option has been set and a renewable ticket is to be issued, then the
     * renew-till field MAY be set to the earliest of ... its requested value [or]
     * the starttime of the ticket plus the maximum renewable lifetime
     * set by the policy of the local realm."
     * 
     * @throws Exception 
     */
    public void testRenewableTicketExceedsMaximumAllowable() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.RENEWABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosTime requestedRenewTillTime = new KerberosTime( now + 2 * KerberosTime.WEEK );
        modifier.setRtime( requestedRenewTillTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        TicketGrantReply reply = ( TicketGrantReply ) session.getMessage();

        assertTrue( "RENEWABLE flag", reply.getFlags().get( TicketFlags.RENEWABLE ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "RENEWABLE flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.RENEWABLE ) );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.INVALID ) );

        KerberosTime expectedRenewTillTime = new KerberosTime( now + KerberosTime.WEEK );
        boolean isClose = Math.abs( reply.getRenewTill().getTime() - expectedRenewTillTime.getTime() ) < 5000;
        assertTrue( "Expected renew-till time", isClose );
    }


    /**
     * "The ciphertext part of the response in the KRB_TGS_REP message is encrypted
     * in the sub-session key from the Authenticator, if present, or in the session
     * key from the TGT."
     *     
     * @throws Exception 
     */
    public void testAuthenticatorSubKey() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
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

        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().get( TicketFlags.INVALID ) );
    }


    /**
     * Tests that the option RESERVED, which is bad for a TGS_REQ, is rejected
     * with the correct error message.
     *
     * @throws Exception
     */
    public void testBadOptionReserved() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RESERVED );
        modifier.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();

        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC cannot accommodate requested option", 13, error.getErrorCode() );
    }
}
