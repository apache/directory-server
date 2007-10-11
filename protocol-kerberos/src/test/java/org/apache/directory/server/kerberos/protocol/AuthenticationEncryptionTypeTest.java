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

import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.io.encoder.EncryptedDataEncoder;
import org.apache.directory.server.kerberos.shared.messages.AuthenticationReply;
import org.apache.directory.server.kerberos.shared.messages.ErrorMessage;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedTimeStamp;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KdcOptions;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationData;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationDataModifier;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationDataType;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBodyModifier;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;


/**
 * Tests various facets of working with encryption types in the Authentication Service (AS).
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AuthenticationEncryptionTypeTest extends AbstractAuthenticationServiceTest
{
    private KdcConfiguration config;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private DummySession session;


    /**
     * Creates a new instance of {@link AuthenticationEncryptionTypeTest}.
     */
    public AuthenticationEncryptionTypeTest()
    {
        config = new KdcConfiguration( null, null, null );
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
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );

        EncryptionType[] encryptionTypes =
            { EncryptionType.DES_CBC_MD5 };

        modifier.setEType( encryptionTypes );
        modifier.setNonce( random.nextInt() );
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

        assertEquals( "Encryption type", EncryptionType.DES_CBC_MD5, reply.getEncPart().getEncryptionType() );
    }


    /**
     * Tests the configuration of AES-128 as the sole supported encryption type.
     * 
     * @throws Exception
     */
    public void testRequestAes128() throws Exception
    {
        EncryptionType[] configuredEncryptionTypes =
            { EncryptionType.AES128_CTS_HMAC_SHA1_96 };
        config.setEncryptionTypes( configuredEncryptionTypes );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );

        EncryptionType[] encryptionTypes =
            { EncryptionType.AES128_CTS_HMAC_SHA1_96 };

        modifier.setEType( encryptionTypes );
        modifier.setNonce( random.nextInt() );
        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        String principalName = "hnelson@EXAMPLE.COM";
        String passPhrase = "secret";
        Set<EncryptionType> preAuthEncryptionTypes = new HashSet<EncryptionType>();
        preAuthEncryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Map<EncryptionType, EncryptionKey> keyMap = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase,
            preAuthEncryptionTypes );
        EncryptionKey clientKey = keyMap.get( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        KerberosTime timeStamp = new KerberosTime();
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientKey, timeStamp );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );
        assertTrue( "PRE_AUTHENT flag", reply.getTicket().getFlags().get( TicketFlags.PRE_AUTHENT ) );
        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getEncPart().getEncryptionType() );
    }


    /**
     * Tests that the client-chosen nonce is correctly returned in the response.
     * 
     * @throws Exception
     */
    public void testNonce() throws Exception
    {
        EncryptionType[] configuredEncryptionTypes =
            { EncryptionType.AES128_CTS_HMAC_SHA1_96 };
        config.setEncryptionTypes( configuredEncryptionTypes );

        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );

        EncryptionType[] encryptionTypes =
            { EncryptionType.AES128_CTS_HMAC_SHA1_96 };

        modifier.setEType( encryptionTypes );
        int nonce = random.nextInt();
        modifier.setNonce( nonce );
        modifier.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();
        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        modifier.setTill( requestedEndTime );

        String principalName = "hnelson@EXAMPLE.COM";
        String passPhrase = "secret";
        Set<EncryptionType> preAuthEncryptionTypes = new HashSet<EncryptionType>();
        preAuthEncryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        Map<EncryptionType, EncryptionKey> keyMap = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase,
            preAuthEncryptionTypes );
        EncryptionKey clientKey = keyMap.get( EncryptionType.AES128_CTS_HMAC_SHA1_96 );

        KerberosTime timeStamp = new KerberosTime();
        PreAuthenticationData[] paData = getPreAuthEncryptedTimeStamp( clientKey, timeStamp );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );
        assertTrue( "PRE_AUTHENT flag", reply.getTicket().getFlags().get( TicketFlags.PRE_AUTHENT ) );
        assertEquals( "Encryption type", EncryptionType.AES128_CTS_HMAC_SHA1_96, reply.getEncPart().getEncryptionType() );

        assertEquals( "Nonce", nonce, reply.getNonce() );
    }


    /**
     * Tests when a request is made for an encryption type that is not enabled in
     * configuration that the request fails with the correct error message.
     * 
     * @throws Exception
     */
    public void testAes128Configuration() throws Exception
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        modifier.setRealm( "EXAMPLE.COM" );

        EncryptionType[] requestedEncryptionTypes =
            { EncryptionType.AES128_CTS_HMAC_SHA1_96 };

        modifier.setEType( requestedEncryptionTypes );
        modifier.setNonce( random.nextInt() );
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


    protected PreAuthenticationData[] getPreAuthEncryptedTimeStamp( EncryptionKey clientKey, KerberosTime timeStamp )
        throws Exception
    {
        PreAuthenticationData[] paData = new PreAuthenticationData[1];

        EncryptedTimeStamp encryptedTimeStamp = new EncryptedTimeStamp( timeStamp, 0 );

        EncryptedData encryptedData = lockBox.seal( clientKey, encryptedTimeStamp, KeyUsage.NUMBER1 );

        byte[] encodedEncryptedData = EncryptedDataEncoder.encode( encryptedData );

        PreAuthenticationDataModifier preAuth = new PreAuthenticationDataModifier();
        preAuth.setDataType( PreAuthenticationDataType.PA_ENC_TIMESTAMP );
        preAuth.setDataValue( encodedEncryptedData );

        paData[0] = preAuth.getPreAuthenticationData();

        return paData;
    }
}
