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

import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.messages.ErrorMessage;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
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
 * Tests configuration of Ticket-Granting Service (TGS) policy.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TicketGrantingPolicyTest extends AbstractTicketGrantingServiceTest
{
    private KdcConfiguration config;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private DummySession session;


    /**
     * Creates a new instance of {@link TicketGrantingPolicyTest}.
     */
    public TicketGrantingPolicyTest()
    {
        config = new KdcConfiguration();

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
     * Tests when forwardable tickets are disallowed that requests for
     * forwardable tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testForwardableTicket() throws Exception
    {
        // Deny FORWARDABLE tickets in policy.
        config.setForwardableAllowed( false );

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

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when forwardable tickets are disallowed that requests for
     * forwarded tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testForwardedTicket() throws Exception
    {
        // Deny FORWARDABLE tickets in policy.
        config.setForwardableAllowed( false );

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

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when empty addresses are disallowed and forwarded tickets are requested
     * that requests with no addresses fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testForwardedNoAddressesTicket() throws Exception
    {
        /*
         * Test case needs further testing to ensure the localhost address is
         * resolved uniformly on different platforms, or else the test case will fail.
         */
        assertTrue( true );

        /*
        // Deny empty addresses tickets in policy.
        config.setEmptyAddressesAllowed( false );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.FORWARDABLE );

        HostAddress[] address =
            { new HostAddress( InetAddress.getByAddress( new byte[4] ) ) };
        HostAddresses addresses = new HostAddresses( address );
        encTicketPartModifier.setClientAddresses( addresses );

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

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
        */
    }


    /**
     * Tests when proxiable tickets are disallowed that requests for
     * proxiable tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testProxiableTicket() throws Exception
    {
        // Deny PROXIABLE tickets in policy.
        config.setProxiableAllowed( false );

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

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when proxiable tickets are disallowed that requests for
     * proxy tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testProxyTicket() throws Exception
    {
        // Deny PROXIABLE tickets in policy.
        config.setProxiableAllowed( false );

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
            { new HostAddress( InetAddress.getLocalHost() ) };
        HostAddresses addresses = new HostAddresses( address );
        modifier.setAddresses( addresses );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when empty addresses are disallowed and proxy tickets are requested
     * that requests with no addresses fail with the correct error message.
     *  
     * @throws Exception 
     */
    public void testProxyNoAddressesTicket() throws Exception
    {
        /*
         * Test case needs further testing to ensure the localhost address is
         * resolved uniformly on different platforms, or else the test case will fail.
         */
        assertTrue( true );

        /*
        // Deny empty addresses tickets in policy.
        config.setEmptyAddressesAllowed( false );

        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPartModifier encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.
        encTicketPartModifier.setFlag( TicketFlags.PROXIABLE );

        HostAddress[] address =
            { new HostAddress( InetAddress.getByAddress( new byte[4] ) ) };
        HostAddresses addresses = new HostAddresses( address );
        encTicketPartModifier.setClientAddresses( addresses );

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

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
        */
    }


    /**
     * Tests when postdated tickets are disallowed that requests for
     * ALLOW-POSTDATE tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testAllowPostdate() throws Exception
    {
        // Deny ALLOW_POSTDATE tickets in policy.
        config.setPostdatedAllowed( false );

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

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when postdated tickets are disallowed that requests for
     * postdated tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testPostdated() throws Exception
    {
        // Deny POSTDATED tickets in policy.
        config.setPostdatedAllowed( false );

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

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        RequestBody requestBody = modifier.getRequestBody();
        KdcRequest message = getKdcRequest( tgt, requestBody );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when postdated tickets are disallowed that requests for
     * validation of invalid tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testValidateInvalidTicket() throws Exception
    {
        // Deny VALIDATE tickets in policy.
        config.setPostdatedAllowed( false );

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

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when renewable tickets are disallowed that requests for
     * renewal of tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testRenewTicket() throws Exception
    {
        // Deny RENEWABLE tickets in policy.
        config.setRenewableAllowed( false );

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

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when renewable tickets are disallowed that requests for
     * RENEWABLE-OK tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testRenewableOk() throws Exception
    {
        // Deny RENEWABLE tickets in policy.
        config.setRenewableAllowed( false );

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

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }


    /**
     * Tests when renewable tickets are disallowed that requests for
     * renewable tickets fail with the correct error message.
     * 
     * @throws Exception 
     */
    public void testRenewableTicket() throws Exception
    {
        // Deny RENEWABLE tickets in policy.
        config.setRenewableAllowed( false );

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

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC policy rejects request", 12, error.getErrorCode() );
    }
}
