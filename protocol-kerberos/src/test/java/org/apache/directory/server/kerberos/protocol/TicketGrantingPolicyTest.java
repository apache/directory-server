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

import java.io.IOException;
import java.net.InetAddress;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.util.Network;
import org.apache.directory.server.kerberos.KerberosConfig;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.protocol.AbstractAuthenticationServiceTest.KrbDummySession;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.components.EncTicketPart;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.HostAddresses;
import org.apache.directory.shared.kerberos.components.KdcReq;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.flags.TicketFlag;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests configuration of Ticket-Granting Service (TGS) policy.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TicketGrantingPolicyTest extends AbstractTicketGrantingServiceTest
{
    private KerberosConfig config;
    private KdcServer kdcServer;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private KrbDummySession session;


    /**
     * Creates a new instance of {@link TicketGrantingPolicyTest}.
     */
    @BeforeEach
    public void setUp() throws IOException
    {
        kdcServer = new KdcServer();
        config = kdcServer.getConfig();

        /*
         * Body checksum verification must be disabled because we are bypassing
         * the codecs, where the body bytes are set on the KdcReq message.
         */
        config.setBodyChecksumVerified( false );

        store = new MapPrincipalStoreImpl();
        handler = new KerberosProtocolHandler( kdcServer, store );
        session = new KrbDummySession();
        lockBox = new CipherTextHandler();
    }


    /**
     * Shutdown the Kerberos server
     */
    @AfterEach
    public void shutDown()
    {
        kdcServer.stop();
    }


    /**
     * Tests when forwardable tickets are disallowed that requests for
     * forwardable tickets fail with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testForwardableTicket() throws Exception
    {
        // Deny FORWARDABLE tickets in policy.
        config.setForwardableAllowed( false );

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
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when forwardable tickets are disallowed that requests for
     * forwarded tickets fail with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testForwardedTicket() throws Exception
    {
        // Deny FORWARDABLE tickets in policy.
        config.setForwardableAllowed( false );

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

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when empty addresses are disallowed and forwarded tickets are requested
     * that requests with no addresses fail with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testForwardedNoAddressesTicket() throws Exception
    {
        // Deny empty addresses tickets in policy.
        config.setEmptyAddressesAllowed( false );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.FORWARDABLE );

        HostAddress[] address =
            { new HostAddress( InetAddress.getByAddress( new byte[4] ) ) };
        HostAddresses addresses = new HostAddresses( address );
        encTicketPart.setClientAddresses( addresses );

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

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when proxiable tickets are disallowed that requests for
     * proxiable tickets fail with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testProxiableTicket() throws Exception
    {
        // Deny PROXIABLE tickets in policy.
        config.setProxiableAllowed( false );

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
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when proxiable tickets are disallowed that requests for
     * proxy tickets fail with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testProxyTicket() throws Exception
    {
        // Deny PROXIABLE tickets in policy.
        config.setProxiableAllowed( false );

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
            { new HostAddress( Network.LOOPBACK ) };
        HostAddresses addresses = new HostAddresses( address );
        kdcReqBody.setAddresses( addresses );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when empty addresses are disallowed and proxy tickets are requested
     * that requests with no addresses fail with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testProxyNoAddressesTicket() throws Exception
    {
        // Deny empty addresses tickets in policy.
        config.setEmptyAddressesAllowed( false );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPart = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPart.setFlag( TicketFlag.PROXIABLE );

        HostAddress[] address =
            { new HostAddress( InetAddress.getByAddress( new byte[4] ) ) };
        HostAddresses addresses = new HostAddresses( address );
        encTicketPart.setClientAddresses( addresses );

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

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when postdated tickets are disallowed that requests for
     * ALLOW-POSTDATE tickets fail with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testAllowPostdate() throws Exception
    {
        // Deny ALLOW_POSTDATE tickets in policy.
        config.setPostdatedAllowed( false );

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
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when postdated tickets are disallowed that requests for
     * postdated tickets fail with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testPostdated() throws Exception
    {
        // Deny POSTDATED tickets in policy.
        config.setPostdatedAllowed( false );

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

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when postdated tickets are disallowed that requests for
     * validation of invalid tickets fail with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testValidateInvalidTicket() throws Exception
    {
        // Deny VALIDATE tickets in policy.
        config.setPostdatedAllowed( false );

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
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when renewable tickets are disallowed that requests for
     * renewal of tickets fail with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testRenewTicket() throws Exception
    {
        // Deny RENEWABLE tickets in policy.
        config.setRenewableAllowed( false );

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
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when renewable tickets are disallowed that requests for
     * RENEWABLE-OK tickets fail with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testRenewableOk() throws Exception
    {
        // Deny RENEWABLE tickets in policy.
        config.setRenewableAllowed( false );

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
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }


    /**
     * Tests when renewable tickets are disallowed that requests for
     * renewable tickets fail with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testRenewableTicket() throws Exception
    {
        // Deny RENEWABLE tickets in policy.
        config.setRenewableAllowed( false );

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
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC policy rejects request", ErrorType.KDC_ERR_POLICY, error.getErrorCode() );
    }
}
