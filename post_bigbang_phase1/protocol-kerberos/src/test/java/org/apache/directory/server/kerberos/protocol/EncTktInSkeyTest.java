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

import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.messages.ErrorMessage;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPartModifier;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KdcOptions;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBody;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBodyModifier;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;


/**
 * Test case for RFC 4120 Section 3.7. "User-to-User Authentication Exchanges."  This
 * is option "ENC-TKT-IN-SKEY."
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EncTktInSkeyTest extends AbstractTicketGrantingServiceTest
{
    private KdcServer config;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private DummySession session;


    /**
     * Creates a new instance of {@link EncTktInSkeyTest}.
     */
    public EncTktInSkeyTest()
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
    public void testEncTktInSkey() throws Exception
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
        kdcOptions.set( KdcOptions.ENC_TKT_IN_SKEY );
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
        assertEquals( "KDC cannot accommodate requested option", 13, error.getErrorCode() );
    }
}
