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

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.KerberosConfig;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.protocol.AbstractAuthenticationServiceTest.KrbDummySession;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.components.EncTicketPart;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.KdcReq;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Test case for RFC 4120 Section 3.7. "User-to-User Authentication Exchanges."  This
 * is option "ENC-TKT-IN-SKEY."
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncTktInSkeyTest extends AbstractTicketGrantingServiceTest
{
    private KerberosConfig config;
    private KdcServer kdcServer;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private KrbDummySession session;


    /**
     * Creates a new instance of {@link EncTktInSkeyTest}.
     */
    @BeforeEach
    public void setUp()
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
     * If the ENC-TKT-IN-SKEY option has been specified and an additional ticket
     * has been included in the request, it indicates that the client is using
     * user-to-user authentication to prove its identity to a server that does
     * not have access to a persistent key.  Section 3.7 describes the effect
     * of this option on the entire Kerberos protocol.  When generating the
     * KRB_TGS_REP message, this option in the KRB_TGS_REQ message tells the KDC
     * to decrypt the additional ticket using the key for the server to which the
     * additional ticket was issued and to verify that it is a TGT.  If the name
     * of the requested server is missing from the request, the name of the client
     * in the additional ticket will be used.  Otherwise, the name of the requested
     * server will be compared to the name of the client in the additional ticket.
     * If it is different, the request will be rejected.  If the request succeeds,
     * the session key from the additional ticket will be used to encrypt the new
     * ticket that is issued instead of using the key of the server for which the
     * new ticket will be used.
     * 
     * @throws Exception 
     */
    @Test
    public void testEncTktInSkey() throws Exception
    {
        // Get the mutable ticket part.
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        EncTicketPart encTicketPartModifier = getTicketArchetype( clientPrincipal );

        // Make changes to test.

        // Seal the ticket for the server.
        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        String passPhrase = "randomKey";
        EncryptionKey serverKey = getEncryptionKey( serverPrincipal, passPhrase );
        Ticket tgt = getTicket( encTicketPartModifier, serverPrincipal, serverKey );

        KdcReqBody modifier = new KdcReqBody();
        modifier.setSName( getPrincipalName( "ldap/ldap.example.com@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );
        modifier.setNonce( random.nextInt() );

        KdcOptions kdcOptions = new KdcOptions();
        kdcOptions.set( KdcOptions.ENC_TKT_IN_SKEY );
        modifier.setKdcOptions( kdcOptions );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + 1 * KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        KerberosTime requestedRenewTillTime = new KerberosTime( now + KerberosTime.WEEK / 2 );
        modifier.setRtime( requestedRenewTillTime );

        KdcReq message = getKdcRequest( tgt, modifier );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "KDC cannot accommodate requested option", ErrorType.KDC_ERR_BADOPTION, error.getErrorCode() );
    }
}
