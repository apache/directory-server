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
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.KerberosConfig;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.protocol.AbstractAuthenticationServiceTest.KrbDummySession;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.components.EncTicketPart;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.KdcReq;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.apache.directory.shared.kerberos.messages.TgsRep;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.junit.After;
import org.junit.Test;

/**
 * Tests for configurable {@link ReplayCache}.
 */
public class TGSReplayCacheTest extends AbstractTicketGrantingServiceTest
{
    private KdcServer kdcServer;
    private KerberosProtocolHandler handler;

    /**
     * Shutdown the Kerberos server
     */
    @After
    public void shutDown()
    {
        kdcServer.stop();
    }

    /**
     * Tests the replay cache is used by default.
     */
    @Test
    public void testDefaultReplayCache() throws Exception
    {
        initKdcServer( new KerberosConfig() );

        KdcReq message = createTgsRequest();

        KrbDummySession session = new KrbDummySession();
        handler.messageReceived( session, message );
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, session.getMessage().getClass() );

        handler.messageReceived( session, message );
        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = (KrbError) msg;
        assertEquals( "Replay not detected", ErrorType.KRB_AP_ERR_REPEAT, error.getErrorCode() );
    }

    /**
     * Tests the replay cache is not used if the type is explicitly set to null.
     */
    @Test
    public void testNullReplayCacheType() throws Exception
    {
        KerberosConfig config = new KerberosConfig();
        config.setReplayCacheType( null );
        initKdcServer( config );

        KdcReq message = createTgsRequest();

        KrbDummySession session = new KrbDummySession();
        handler.messageReceived( session, message );
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, session.getMessage().getClass() );

        handler.messageReceived( session, message );
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, session.getMessage().getClass() );
    }

    /**
     * Tests that custom replay cache can be set.
     */
    @Test
    public void testDummyReplayCacheType() throws Exception
    {
        KerberosConfig config = new KerberosConfig();
        config.setReplayCacheType( DisabledReplayCache.class );
        initKdcServer( config );

        KdcReq message = createTgsRequest();

        KrbDummySession session = new KrbDummySession();
        handler.messageReceived( session, message );
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, session.getMessage().getClass() );

        handler.messageReceived( session, message );
        assertEquals( "session.getMessage() instanceOf", TgsRep.class, session.getMessage().getClass() );

        assertTrue( "Incorrect cache implementation", DisabledReplayCache.replayTested );
    }

    private KdcReq createTgsRequest() throws KerberosException, ParseException, Exception
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
        kdcReqBody.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( kdcServer.getConfig().getEncryptionTypes() );
        kdcReqBody.setNonce( random.nextInt() );
        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KdcReq message = getKdcRequest( tgt, kdcReqBody );
        return message;
    }

    /**
     * Creates a new instance of {@link KdcServer}.
     */
    private void initKdcServer( KerberosConfig config )
    {
        config.setBodyChecksumVerified( false );
        kdcServer = new KdcServer( config );
        handler = new KerberosProtocolHandler( kdcServer, new MapPrincipalStoreImpl() );
        lockBox = new CipherTextHandler();
    }

    public static class DisabledReplayCache implements ReplayCache
    {

        public static volatile boolean replayTested = false;

        @Override
        public boolean isReplay(KerberosPrincipal serverPrincipal, KerberosPrincipal clientPrincipal, KerberosTime clientTime,
                int clientMicroSeconds)
        {
            replayTested = true;
            return false;
        }

        @Override
        public void save(KerberosPrincipal serverPrincipal, KerberosPrincipal clientPrincipal, KerberosTime clientTime,
                int clientMicroSeconds)
        {
        }

        @Override
        public void clear()
        {
        }

    }

}
