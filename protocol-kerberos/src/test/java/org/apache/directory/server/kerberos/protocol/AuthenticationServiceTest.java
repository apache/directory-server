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


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.AuthenticationReply;
import org.apache.directory.server.kerberos.shared.messages.ErrorMessage;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.value.KdcOptions;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationData;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBodyModifier;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;


/**
 * Tests the Authentication Service (AS) via the {@link KerberosProtocolHandler}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AuthenticationServiceTest extends AbstractAuthenticationServiceTest
{
    private KdcConfiguration config;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private DummySession session;


    /**
     * Creates a new instance of {@link AuthenticationServiceTest}.
     */
    public AuthenticationServiceTest()
    {
        config = new KdcConfiguration( null, null, null );
        store = new MapPrincipalStoreImpl();
        handler = new KerberosProtocolHandler( config, store );
        session = new DummySession();
        lockBox = new CipherTextHandler();
    }


    /**
     * Tests the default minimum request, which consists of as little as the
     * client name, realm, till time, nonce, and encryption types.
     * 
     * This is the request archetype.
     */
    public void testRequestArchetype()
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KerberosTime till = new KerberosTime();
        modifier.setTill( till );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();

        assertEquals( "Additional pre-authentication required", 25, error.getErrorCode() );
    }


    /**
     * Tests the protocol version number, which must be '5'.
     */
    public void testProtocolVersionNumber()
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcRequest message = new KdcRequest( 4, MessageType.KRB_AS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Requested protocol version number not supported", 3, error.getErrorCode() );
    }


    /**
     * Tests that Kerberos reply messages sent to the KDC will be rejected with the
     * correct error message.
     */
    public void testIncorrectMessageDirection()
    {
        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REP, null, null );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Incorrect message direction", 47, error.getErrorCode() );

        message = new KdcRequest( 5, MessageType.KRB_TGS_REP, null, null );

        handler.messageReceived( session, message );

        error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Incorrect message direction", 47, error.getErrorCode() );
    }


    /**
     * Tests that a non-existent client principal returns the correct error message.
     * 
     * "If the requested client principal named in the request is
     * unknown because it doesn't exist in the KDC's principal database,
     * then an error message with a KDC_ERR_C_PRINCIPAL_UNKNOWN is returned."
     */
    public void testClientNotFound()
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "baduser" ) );
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Client not found in Kerberos database", 6, error.getErrorCode() );
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
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );

        EncryptionType[] encryptionTypes = new EncryptionType[]
            { EncryptionType.DES3_CBC_MD5 };

        modifier.setEType( encryptionTypes );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC has no support for encryption type", 14, error.getErrorCode() );
    }


    /**
     * Tests that a non-existent server principal returns the correct error message.
     * 
     * @throws Exception 
     */
    public void testServerNotFound() throws Exception
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "badserver" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Server not found in Kerberos database", 7, error.getErrorCode() );
    }


    /**
     * Tests that when a client principal is not configured with Kerberos keys that
     * the correct error message is returned.
     */
    public void testClientNullKey()
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "tquist" ) );
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "The client or server has a null key", 9, error.getErrorCode() );
    }


    /**
     * Tests that when a server principal is not configured with Kerberos keys that
     * the correct error message is returned.
     * 
     * @throws Exception 
     */
    public void testServerNullKey() throws Exception
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "tquist" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + -1 * KerberosTime.DAY );
        modifier.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now );
        modifier.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.POSTDATED );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now );
        modifier.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + 4 * KerberosTime.MINUTE );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + 10 * KerberosTime.MINUTE );
        modifier.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.POSTDATED );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + 2 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

        assertTrue( "Requested start time", requestedStartTime.equals( reply.getStartTime() ) );
        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );
        assertTrue( "POSTDATED flag", reply.getFlags().get( TicketFlags.POSTDATED ) );
        assertTrue( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "Requested start time", requestedStartTime.equals( reply.getTicket().getStartTime() ) );
        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );
        assertTrue( "POSTDATED flag", reply.getTicket().getFlags().get( TicketFlags.POSTDATED ) );
        assertTrue( "INVALID flag", reply.getTicket().getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "PRE_AUTHENT flag", reply.getTicket().getFlags().get( TicketFlags.PRE_AUTHENT ) );
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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY / 2 );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );

        assertTrue( "PRE_AUTHENT flag", reply.getTicket().getFlags().get( TicketFlags.PRE_AUTHENT ) );
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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.WEEK );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        modifier.setKdcOptions( new KdcOptions() );

        String epoch = "19700101000000Z";
        KerberosTime requestedEndTime = KerberosTime.getTime( epoch );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

        long now = System.currentTimeMillis();
        KerberosTime expectedEndTime = new KerberosTime( now + KerberosTime.DAY );
        boolean isClose = Math.abs( reply.getEndTime().getTime() - expectedEndTime.getTime() ) < 5000;
        assertTrue( "Expected end time", isClose );
    }


    /**
     * Tests that a service ticket can be requested without the use of a TGT.  The
     * returned service ticket will have the INITIAL flag set.
     * 
     * @throws Exception
     */
    public void testInitialServiceTicket() throws Exception
    {
        String servicePrincipalName = "ldap/ldap.example.com@EXAMPLE.COM";

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( servicePrincipalName ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

        assertTrue( "INITIAL flag", reply.getFlags().get( TicketFlags.INITIAL ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "INITIAL flag", reply.getTicket().getFlags().get( TicketFlags.INITIAL ) );
        assertFalse( "INVALID flag", reply.getTicket().getFlags().get( TicketFlags.INVALID ) );

        assertEquals( "Service principal name", reply.getServerPrincipal().getName(), servicePrincipalName );
        assertEquals( "Service principal name", reply.getTicket().getServerPrincipal().getName(), servicePrincipalName );
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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE_OK );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.WEEK );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.FORWARDABLE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

        assertTrue( "FORWARDABLE flag", reply.getFlags().get( TicketFlags.FORWARDABLE ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "FORWARDABLE flag", reply.getTicket().getFlags().get( TicketFlags.FORWARDABLE ) );
        assertFalse( "INVALID flag", reply.getTicket().getFlags().get( TicketFlags.INVALID ) );
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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.ALLOW_POSTDATE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

        assertTrue( "MAY_POSTDATE flag", reply.getFlags().get( TicketFlags.MAY_POSTDATE ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "MAY_POSTDATE flag", reply.getTicket().getFlags().get( TicketFlags.MAY_POSTDATE ) );
        assertFalse( "INVALID flag", reply.getTicket().getFlags().get( TicketFlags.INVALID ) );
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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.PROXIABLE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

        assertTrue( "PROXIABLE flag", reply.getFlags().get( TicketFlags.PROXIABLE ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "PROXIABLE flag", reply.getTicket().getFlags().get( TicketFlags.PROXIABLE ) );
        assertFalse( "INVALID flag", reply.getTicket().getFlags().get( TicketFlags.INVALID ) );
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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosTime requestedRenewTillTime = new KerberosTime( now + KerberosTime.WEEK / 2 );
        modifier.setRtime( requestedRenewTillTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

        assertTrue( "RENEWABLE flag", reply.getFlags().get( TicketFlags.RENEWABLE ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "RENEWABLE flag", reply.getTicket().getFlags().get( TicketFlags.RENEWABLE ) );
        assertFalse( "INVALID flag", reply.getTicket().getFlags().get( TicketFlags.INVALID ) );

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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosTime requestedRenewTillTime = new KerberosTime( now + 2 * KerberosTime.WEEK );
        modifier.setRtime( requestedRenewTillTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

        assertTrue( "RENEWABLE flag", reply.getFlags().get( TicketFlags.RENEWABLE ) );
        assertFalse( "INVALID flag", reply.getFlags().get( TicketFlags.INVALID ) );

        assertTrue( "RENEWABLE flag", reply.getTicket().getFlags().get( TicketFlags.RENEWABLE ) );
        assertFalse( "INVALID flag", reply.getTicket().getFlags().get( TicketFlags.INVALID ) );

        KerberosTime expectedRenewTillTime = new KerberosTime( now + KerberosTime.WEEK );
        boolean isClose = Math.abs( reply.getRenewTill().getTime() - expectedRenewTillTime.getTime() ) < 5000;
        assertTrue( "Expected renew-till time", isClose );
    }


    /**
     * Tests that the option RENEW, which is bad for an AS_REQ, is rejected
     * with the correct error message.
     *
     * @throws Exception
     */
    public void testBadOptionRenew() throws Exception
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEW );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC cannot accommodate requested option", 13, error.getErrorCode() );
    }


    /**
     * Tests that the option VALIDATE, which is bad for an AS_REQ, is rejected
     * with the correct error message.
     *
     * @throws Exception
     */
    public void testBadOptionValidate() throws Exception
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.VALIDATE );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC cannot accommodate requested option", 13, error.getErrorCode() );
    }


    /**
     * Tests that the option PROXY, which is bad for an AS_REQ, is rejected
     * with the correct error message.
     *
     * @throws Exception
     */
    public void testBadOptionProxy() throws Exception
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.PROXY );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC cannot accommodate requested option", 13, error.getErrorCode() );
    }


    /**
     * Tests that the option FORWARDED, which is bad for an AS_REQ, is rejected
     * with the correct error message.
     *
     * @throws Exception
     */
    public void testBadOptionForwarded() throws Exception
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.FORWARDED );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC cannot accommodate requested option", 13, error.getErrorCode() );
    }


    /**
     * Tests that the option ENC_TKT_IN_SKEY, which is bad for an AS_REQ, is rejected
     * with the correct error message.
     *
     * @throws Exception
     */
    public void testBadOptionEncTktInSkey() throws Exception
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.ENC_TKT_IN_SKEY );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "KDC cannot accommodate requested option", 13, error.getErrorCode() );
    }
}
