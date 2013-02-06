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
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.KerberosConfig;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.KdcRep;
import org.apache.directory.shared.kerberos.components.KdcReq;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.apache.directory.shared.kerberos.components.PaData;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.messages.AsRep;
import org.apache.directory.shared.kerberos.messages.AsReq;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.apache.directory.shared.kerberos.messages.TgsRep;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests the Authentication Service (AS) via the {@link KerberosProtocolHandler}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthenticationServiceTest extends AbstractAuthenticationServiceTest
{
    private KerberosConfig config;
    private KdcServer kdcServer;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private KrbDummySession session;


    /**
     * Creates a new instance of {@link AuthenticationServiceTest}.
     */
    @Before
    public void setUp()
    {
        kdcServer = new KdcServer();
        config = kdcServer.getConfig();
        store = new MapPrincipalStoreImpl();
        handler = new KerberosProtocolHandler( kdcServer, store );
        session = new KrbDummySession();
        lockBox = new CipherTextHandler();
    }


    /**
     * Shutdown the Kerberos server
     */
    @After
    public void shutDown()
    {
        kdcServer.stop();
    }


    /**
     * Tests the default minimum request, which consists of as little as the
     * client name, realm, till time, nonce, and encryption types.
     * 
     * This is the request archetype.
     */
    @Test
    public void testRequestArchetype()
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KerberosTime till = new KerberosTime();
        kdcReqBody.setTill( till );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        KrbError error = ( KrbError ) session.getMessage();

        assertEquals( "Additional pre-authentication required", ErrorType.KDC_ERR_PREAUTH_REQUIRED, error.getErrorCode() );
    }


    /**
     * Tests the protocol version number, which must be '5'.
     */
    @Test
    public void testProtocolVersionNumber()
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcReq message = new AsReq();
        message.setProtocolVersionNumber( 4 );
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "Requested protocol version number not supported", ErrorType.KDC_ERR_BAD_PVNO, error.getErrorCode() );
    }


    /**
     * Tests that Kerberos reply messages sent to the KDC will be rejected with the
     * correct error message.
     */
    @Test
    public void testIncorrectMessageDirection()
    {
        KdcRep message = new AsRep();

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "Incorrect message direction", ErrorType.KRB_AP_ERR_BADDIRECTION, error.getErrorCode() );

        message = new TgsRep();

        handler.messageReceived( session, message );

        msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        error = ( KrbError ) msg;
        assertEquals( "Incorrect message direction", ErrorType.KRB_AP_ERR_BADDIRECTION, error.getErrorCode() );
    }


    /**
     * Tests that a non-existent client principal returns the correct error message.
     * 
     * "If the requested client principal named in the request is
     * unknown because it doesn't exist in the KDC's principal database,
     * then an error message with a KDC_ERR_C_PRINCIPAL_UNKNOWN is returned."
     */
    @Test
    public void testClientNotFound()
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "baduser" ) );
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "Client not found in Kerberos database", ErrorType.KDC_ERR_C_PRINCIPAL_UNKNOWN, error.getErrorCode() );
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
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );

        Set<EncryptionType> encryptionTypes = new HashSet<EncryptionType>();
        encryptionTypes.add( EncryptionType.RC4_HMAC );

        kdcReqBody.setEType( encryptionTypes );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC has no support for encryption type", ErrorType.KDC_ERR_ETYPE_NOSUPP, error.getErrorCode() );
    }


    /**
     * Tests that a non-existent server principal returns the correct error message.
     * 
     * @throws Exception
     */
    @Test
    public void testServerNotFound() throws Exception
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "badserver" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "Server not found in Kerberos database", ErrorType.KDC_ERR_S_PRINCIPAL_UNKNOWN, error.getErrorCode() );
    }


    /**
     * Tests that when a client principal is not configured with Kerberos keys that
     * the correct error message is returned.
     */
    @Test
    public void testClientNullKey()
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "tquist" ) );
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "The client or server has a null key", ErrorType.KDC_ERR_NULL_KEY, error.getErrorCode() );
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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "tquist" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( new PrincipalName( new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + -1 * KerberosTime.DAY );
        kdcReqBody.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now );
        kdcReqBody.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.POSTDATED );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "Requested start time is later than end time", ErrorType.KDC_ERR_NEVER_VALID, error.getErrorCode() );
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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now );
        kdcReqBody.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + 3 * KerberosTime.MINUTE );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "Requested start time is later than end time", ErrorType.KDC_ERR_NEVER_VALID, error.getErrorCode() );
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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + 10 * KerberosTime.MINUTE );
        kdcReqBody.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.POSTDATED );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedStartTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setFrom( requestedStartTime );

        KerberosTime requestedEndTime = new KerberosTime( now + 2 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

        assertTrue( "Requested start time", requestedStartTime.equals( reply.getStartTime() ) );
        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );
        assertTrue( "POSTDATED flag", reply.getFlags().isPostdated() );
        assertTrue( "INVALID flag", reply.getFlags().isInvalid() );

        assertTrue( "Requested start time", requestedStartTime.equals( reply.getTicket().getEncTicketPart().getStartTime() ) );
        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );
        assertTrue( "POSTDATED flag", reply.getTicket().getEncTicketPart().getFlags().isPostdated() );
        assertTrue( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().isInvalid() );

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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY / 2 );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );

        assertTrue( "PRE_AUTHENT flag", reply.getTicket().getEncTicketPart().getFlags().isPreAuth() );
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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.WEEK );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        String epoch = "19700101000000Z";
        KerberosTime requestedEndTime = KerberosTime.getTime( epoch );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

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
    @Test
    public void testInitialServiceTicket() throws Exception
    {
        KerberosPrincipal servicePrincipalName = new KerberosPrincipal( "ldap/ldap.example.com@EXAMPLE.COM" );

        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( new PrincipalName( servicePrincipalName ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

        assertTrue( "INITIAL flag", reply.getFlags().isInitial() );
        assertFalse( "INVALID flag", reply.getFlags().isInvalid() );

        assertTrue( "INITIAL flag", reply.getTicket().getEncTicketPart().getFlags().isInitial() );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().isInvalid() );

        assertEquals( "Service principal name", "ldap/ldap.example.com", reply.getSName().getNameString() );
        assertEquals( "Service principal name", "ldap/ldap.example.com", reply.getTicket().getSName().getNameString() );
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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE_OK );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.WEEK );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.FORWARDABLE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.ALLOW_POSTDATE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.PROXIABLE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosTime requestedRenewTillTime = new KerberosTime( now + KerberosTime.WEEK / 2 );
        kdcReqBody.setRtime( requestedRenewTillTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

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
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEWABLE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosTime requestedRenewTillTime = new KerberosTime( now + 2 * KerberosTime.WEEK );
        kdcReqBody.setRtime( requestedRenewTillTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", AsRep.class, msg.getClass() );
        AsRep reply = ( AsRep ) msg;

        assertTrue( "RENEWABLE flag", reply.getFlags().isRenewable() );
        assertFalse( "INVALID flag", reply.getFlags().isInvalid() );

        assertTrue( "RENEWABLE flag", reply.getTicket().getEncTicketPart().getFlags().isRenewable() );
        assertFalse( "INVALID flag", reply.getTicket().getEncTicketPart().getFlags().isInvalid() );

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
    @Test
    public void testBadOptionRenew() throws Exception
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.RENEW );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC cannot accommodate requested option", ErrorType.KDC_ERR_BADOPTION, error.getErrorCode() );
    }


    /**
     * Tests that the option VALIDATE, which is bad for an AS_REQ, is rejected
     * with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testBadOptionValidate() throws Exception
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.VALIDATE );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC cannot accommodate requested option", ErrorType.KDC_ERR_BADOPTION, error.getErrorCode() );
    }


    /**
     * Tests that the option PROXY, which is bad for an AS_REQ, is rejected
     * with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testBadOptionProxy() throws Exception
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.PROXY );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC cannot accommodate requested option", ErrorType.KDC_ERR_BADOPTION, error.getErrorCode() );
    }


    /**
     * Tests that the option FORWARDED, which is bad for an AS_REQ, is rejected
     * with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testBadOptionForwarded() throws Exception
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.FORWARDED );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC cannot accommodate requested option", ErrorType.KDC_ERR_BADOPTION, error.getErrorCode() );
    }


    /**
     * Tests that the option ENC_TKT_IN_SKEY, which is bad for an AS_REQ, is rejected
     * with the correct error message.
     *
     * @throws Exception
     */
    @Test
    public void testBadOptionEncTktInSkey() throws Exception
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.ENC_TKT_IN_SKEY );
        kdcReqBody.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        for ( PaData paData : paDatas )
        {
            message.addPaData( paData );
        }

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC cannot accommodate requested option", ErrorType.KDC_ERR_BADOPTION, error.getErrorCode() );
    }
}
