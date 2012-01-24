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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.protocol.AbstractAuthenticationServiceTest.KrbDummySession;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncTicketPart;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.HostAddresses;
import org.apache.directory.shared.kerberos.components.KdcReq;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.apache.directory.shared.kerberos.crypto.checksum.ChecksumType;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.flags.TicketFlag;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.apache.directory.shared.kerberos.messages.TgsRep;
import org.apache.directory.shared.kerberos.messages.TgsReq;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


public class TicketGrantingServiceTest extends AbstractTicketGrantingServiceTest
{
    private KdcServer config;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private KrbDummySession session;


    /**
     * Creates a new instance of {@link TicketGrantingServiceTest}.
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
    @Test
    public void testRequestArchetype() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );
    }


    /**
     * Tests the protocol version number, which must be '5'.
     */
    @Test
    public void testProtocolVersionNumber()
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcReq message = new TgsReq();
        message.setProtocolVersionNumber( 4 );
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "Requested protocol version number not supported", ErrorType.KDC_ERR_BAD_PVNO,
            error.getErrorCode() );
    }


    /**
     * Tests that a non-existent server principal returns the correct error message.
     * 
     * @throws Exception 
     */
    @Test
    public void testServerNotFound() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "badservice" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "Server not found in Kerberos database", ErrorType.KDC_ERR_S_PRINCIPAL_UNKNOWN,
            error.getErrorCode() );
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
    @Test
    public void testNoTicketFound() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        // Get the session key from the service ticket.
        sessionKey = tgt.getEncTicketPart().getKey();

        // Generate a new sequence number.
        sequenceNumber = random.nextInt();
        now = new KerberosTime();

        KdcReq message = new TgsReq();
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC has no support for padata type", ErrorType.KDC_ERR_PADATA_TYPE_NOSUPP, error.getErrorCode() );
    }


    /**
     * Tests that an inappropriate checksum returns the correct error message.
     * 
     * @throws Exception 
     */
    @Test
    @Ignore
    public void testInappropriateChecksum() throws Exception
    {
        config.setBodyChecksumVerified( true );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "tquist@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "Inappropriate type of checksum in message", ErrorType.KRB_AP_ERR_INAPP_CKSUM,
            error.getErrorCode() );
    }


    /**
     * Tests that an inappropriate checksum returns the correct error message.
     * 
     * @throws Exception 
     */
    @Test
    public void testChecksumTypeNoSupport() throws Exception
    {
        config.setBodyChecksumVerified( true );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "tquist@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        try
        {
            getKdcRequest( tgt, kdcReqBody, ChecksumType.DES_MAC_K );
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
    @Test
    public void testIntegrityCheckedFailed() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "badpassword";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "Integrity check on decrypted field failed", ErrorType.KRB_AP_ERR_BAD_INTEGRITY,
            error.getErrorCode() );
    }


    /**
     * Tests when the ticket isn't for us that the correct error message is returned.
     * 
     * @throws Exception
     */
    @Test
    public void testNotUs() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@APACHE.ORG" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "The ticket isn't for us", ErrorType.KRB_AP_ERR_NOT_US, error.getErrorCode() );
    }


    /**
     * "The TGS exchange between a client and the Kerberos TGS is initiated by a
     * client when ... it seeks to renew an existing ticket."
     * 
     * @throws Exception 
     */
    @Test
    public void testRenewTicket() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEW );
        kdcReqBody.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

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
    @Test
    public void testValidateTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.INVALID );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.VALIDATE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

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
    @Test
    public void testProxyTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.PROXIABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.PROXY );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        HostAddress[] address =
            { new HostAddress( InetAddress.getByName( null ) ) };
        HostAddresses addresses = new HostAddresses( address );
        kdcReqBody.setAddresses( addresses );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertTrue( "PROXY flag", reply.getFlags().isProxy() );
        assertFalse( "INVALID flag", reply.getFlags().isInvalid() );

        assertTrue( "PROXY flag", reply.getTicket().getEncTicketPart().getFlags().isProxy() );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().isInvalid() );

        assertNotNull( reply.getTicket().getEncTicketPart().getClientAddresses() );
    }


    /**
     * "The TGS exchange between a client and the Kerberos TGS is initiated by a
     * client when ... it seeks to obtain a forwarded ticket."
     * 
     * @throws Exception 
     */
    @Test
    public void testForwardedTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.FORWARDABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.FORWARDED );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        HostAddress[] address =
            { new HostAddress( InetAddress.getByName( null ) ) };
        HostAddresses addresses = new HostAddresses( address );
        kdcReqBody.setAddresses( addresses );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertTrue( "FORWARDED flag", reply.getFlags().isForwarded() );
        assertFalse( "INVALID flag", reply.getFlags().isInvalid() );

        assertTrue( "FORWARDED flag", reply.getTicket().getEncTicketPart().getFlags().isForwarded() );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().isInvalid() );

        assertNotNull( reply.getTicket().getEncTicketPart().getClientAddresses() );
    }


    /**
     * As is the case for all application servers, expired tickets are not
     * accepted by the TGS, so once a renewable or TGT expires, the client
     * must use a separate exchange to obtain valid tickets.
     * 
     * @throws Exception 
     */
    @Test
    public void testExpiredTgt() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setEndTime( new KerberosTime( 0 ) );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "Ticket expired", ErrorType.KRB_AP_ERR_TKT_EXPIRED, error.getErrorCode() );
    }


    /**
     * As is the case for all application servers, expired tickets are not accepted
     * by the TGS, so once a renewable or TGT expires, the client must use a separate
     * exchange to obtain valid tickets.
     * 
     * @throws Exception 
     */
    @Test
    public void testExpiredRenewableTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.RENEWABLE );
        encTicketPart.setRenewTill( new KerberosTime( 0 ) );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "ldap/ldap.example.com@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEW );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "Ticket expired", ErrorType.KRB_AP_ERR_TKT_EXPIRED, error.getErrorCode() );
    }


    /**
     * Tests when a renewable ticket is presented for renewal, that if the RENEW
     * flag is NOT set, the ticket is renewed for the endtime of the presented
     * ticket, as though it were a TGT.
     *
     * @throws Exception
     */
    @Test
    public void testRenewableTicketNoRenew() throws Exception
    {
        long now = System.currentTimeMillis();

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.RENEWABLE );
        encTicketPart.setStartTime( new KerberosTime( now - KerberosTime.DAY / 2 ) );
        encTicketPart.setEndTime( new KerberosTime( now + KerberosTime.DAY / 2 ) );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "ldap/ldap.example.com@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY / 2 );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

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
    @Test
    public void testRenewableTicketRenewal() throws Exception
    {
        long now = System.currentTimeMillis();

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.RENEWABLE );
        encTicketPart.setStartTime( new KerberosTime( now - KerberosTime.DAY / 2 ) );
        encTicketPart.setEndTime( new KerberosTime( now + KerberosTime.DAY / 2 ) );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "ldap/ldap.example.com@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEW );
        kdcReqBody.setKdcOptions( kdcOptions );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY / 2 );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

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
    @Test
    public void testEncryptionTypeNoSupport() throws Exception
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );

        Set<EncryptionType> encryptionTypes = new HashSet<EncryptionType>();
        encryptionTypes.add( EncryptionType.DES3_CBC_MD5 );

        kdcReqBody.setEType( encryptionTypes );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = new TgsReq();
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) session.getMessage();
        assertEquals( "KDC has no support for encryption type", ErrorType.KDC_ERR_ETYPE_NOSUPP, error.getErrorCode() );
    }


    /**
     * Tests that when a server principal is not configured with Kerberos keys that
     * the correct error message is returned.
     * 
     * @throws Exception 
     */
    @Test
    public void testServerNullKey() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "tquist" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) session.getMessage();
        assertEquals( "The client or server has a null key", ErrorType.KDC_ERR_NULL_KEY, error.getErrorCode() );
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
    @Test
    public void testStartTimeAbsentNoPostdate() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

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
    @Test
    public void testStartTimeInThePastNoPostdate() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + -1 * KerberosTime.DAY );
        kdcReqBody.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

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
    @Test
    public void testStartTimeAcceptableClockSkewNoPostdate() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now );
        kdcReqBody.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

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
    @Test
    public void testStartTimeOrderNeverValid() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.MAY_POSTDATE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.POSTDATED );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) session.getMessage();
        assertEquals( "Requested start time is later than end time", ErrorType.KDC_ERR_NEVER_VALID,
            error.getErrorCode() );
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
    @Test
    public void testStartTimeMinimumNeverValid() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.MAY_POSTDATE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now );
        kdcReqBody.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + 4 * KerberosTime.MINUTE );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) session.getMessage();
        assertEquals( "Requested start time is later than end time", ErrorType.KDC_ERR_NEVER_VALID,
            error.getErrorCode() );
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
    @Test
    public void testStartTimeNoPostdated() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.MAY_POSTDATE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + 10 * KerberosTime.MINUTE );
        kdcReqBody.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) session.getMessage();
        assertEquals( "Ticket not eligible for postdating", ErrorType.KDC_ERR_CANNOT_POSTDATE, error.getErrorCode() );
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
    @Test
    public void testSpecificStartTime() throws Exception
    {
        long now = System.currentTimeMillis();

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.MAY_POSTDATE );
        // Service ticket end time will be limited by TGT end time.
        encTicketPart.setEndTime( new KerberosTime( now + 3 * KerberosTime.DAY ) );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.POSTDATED );
        kdcReqBody.setKdcOptions( kdcOptions );

        KerberosTime requestedStartTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + 2 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertTrue( "Requested start time", requestedStartTime.equals( reply.getStartTime() ) );
        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );
        assertTrue( "POSTDATED flag", reply.getFlags().isPostdated() );
        assertTrue( "INVALID flag", reply.getFlags().isInvalid() );

        assertTrue( "Requested start time",
            requestedStartTime.equals( reply.getTicket().getEncTicketPart().getStartTime() ) );
        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );
        assertTrue( "POSTDATED flag", reply.getTicket().getEncTicketPart().getFlags().isPostdated() );
        assertTrue( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().isInvalid() );
    }


    /**
     * Tests when pre-authentication used during initial authentication, that the flag
     * is carried forward to derivative tickets.
     *
     * @throws Exception
     */
    @Test
    public void testPreAuthenticationFlag() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.PRE_AUTHENT );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertTrue( "PRE_AUTHENT flag", reply.getTicket().getEncTicketPart().getFlags().isPreAuth() );
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
    @Test
    public void testSpecificEndTime() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY / 2 );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

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
    @Test
    public void testEndTimeExceedsMaximumAllowable() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.WEEK );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

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
    @Test
    public void testEpochEndTime() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        String epoch = "19700101000000Z";
        KerberosTime requestedEndTime = KerberosTime.getTime( epoch );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

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
    @Test
    public void testRenewableOk() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.RENEWABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE_OK );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.WEEK );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        KerberosTime expectedEndTime = new KerberosTime( now + KerberosTime.DAY );
        boolean isClose = Math.abs( reply.getEndTime().getTime() - expectedEndTime.getTime() ) < 5000;
        assertTrue( "Expected end time", isClose );

        assertTrue( "RENEWABLE flag", reply.getFlags().isRenewable() );
        assertFalse( "INVALID flag", reply.getFlags().isInvalid() );

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
    @Test
    public void testForwardableTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.FORWARDABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.FORWARDABLE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertTrue( "FORWARDABLE flag", reply.getFlags().isForwardable() );
        assertFalse( "INVALID flag", reply.getFlags().isInvalid() );

        assertTrue( "FORWARDABLE flag", reply.getTicket().getEncTicketPart().getFlags().isForwardable() );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().isInvalid() );
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
    @Test
    public void testAllowPostdate() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.MAY_POSTDATE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.ALLOW_POSTDATE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertTrue( "MAY_POSTDATE flag", reply.getFlags().isMayPosdate() );
        assertFalse( "INVALID flag", reply.getFlags().isInvalid() );

        assertTrue( "MAY_POSTDATE flag", reply.getTicket().getEncTicketPart().getFlags().isMayPosdate() );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().isInvalid() );
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
    @Test
    public void testProxiableTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.PROXIABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.PROXIABLE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertTrue( "PROXIABLE flag", reply.getFlags().isProxiable() );
        assertFalse( "INVALID flag", reply.getFlags().isInvalid() );

        assertTrue( "PROXIABLE flag", reply.getTicket().getEncTicketPart().getFlags().isProxiable() );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().isInvalid() );
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
    @Test
    public void testRenewableTicket() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.RENEWABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosTime requestedRenewTillTime = new KerberosTime( now + KerberosTime.WEEK / 2 );
        kdcReqBody.setRtime( requestedRenewTillTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertTrue( "RENEWABLE flag", reply.getFlags().isRenewable() );
        assertFalse( "INVALID flag", reply.getFlags().isInvalid() );

        assertTrue( "RENEWABLE flag", reply.getTicket().getEncTicketPart().getFlags().isRenewable() );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().isInvalid() );

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
    @Test
    public void testRenewableTicketExceedsMaximumAllowable() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.RENEWABLE );

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosTime requestedRenewTillTime = new KerberosTime( now + 2 * KerberosTime.WEEK );
        kdcReqBody.setRtime( requestedRenewTillTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, msg.getClass() );
        TgsRep reply = ( TgsRep ) msg;

        assertTrue( "RENEWABLE flag", reply.getFlags().isRenewable() );
        assertFalse( "INVALID flag", reply.getFlags().isInvalid() );

        assertTrue( "RENEWABLE flag", reply.getTicket().getEncTicketPart().getFlags().isRenewable() );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().isInvalid() );

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
    @Test
    public void testAuthenticatorSubKey() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPart, serverPrincipal, serverKey );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
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

        assertFalse( "INVALID flag", reply.getFlags().isInvalid() );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().isInvalid() );
    }


    /**
     * Tests that the option RESERVED, which is bad for a TGS_REQ, is rejected
     * with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testBadOptionReserved() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String serverPassword = "randomKey";

        Ticket tgt = getTgt( clientPrincipal, serverPrincipal, serverPassword );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RESERVED_0 );
        kdcReqBody.setKdcOptions( kdcOptions );

        long currentTime = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( currentTime + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) session.getMessage();
        assertEquals( "KDC cannot accommodate requested option", ErrorType.KDC_ERR_BADOPTION, error.getErrorCode() );
    }
}
