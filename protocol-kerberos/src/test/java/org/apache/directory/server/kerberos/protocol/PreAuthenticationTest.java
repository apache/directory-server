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
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.io.encoder.EncryptedDataEncoder;
import org.apache.directory.server.kerberos.shared.messages.ErrorMessage;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedTimeStamp;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KdcOptions;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PaData;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBodyModifier;
import org.apache.directory.server.kerberos.shared.messages.value.types.PaDataType;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;


/**
 * Tests pre-authentication processing in the Authentication Service (AS) via the
 * {@link KerberosProtocolHandler}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PreAuthenticationTest extends AbstractAuthenticationServiceTest
{
    private KdcServer config;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private DummySession session;


    /**
     * Creates a new instance of {@link PreAuthenticationTest}.
     */
    public PreAuthenticationTest()
    {
        config = new KdcServer();
        store = new MapPrincipalStoreImpl();
        handler = new KerberosProtocolHandler( config, store );
        session = new DummySession();
        lockBox = new CipherTextHandler();
    }


    /**
     * Tests when the KDC configuration requires pre-authentication by encrypted
     * timestamp that an AS_REQ without pre-authentication is rejected with the
     * correct error message.
     * 
     * "If pre-authentication is required, but was not present in the request, an
     * error message with the code KDC_ERR_PREAUTH_REQUIRED is returned, and a
     * METHOD-DATA object will be stored in the e-data field of the KRB-ERROR
     * message to specify which pre-authentication mechanisms are acceptable."
     */
    public void testPreAuthenticationRequired()
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "hnelson" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, null, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Additional pre-authentication required", 25, error.getErrorCode() );
    }


    /**
     * Tests when the KDC configuration requires pre-authentication by encrypted
     * timestamp that an AS_REQ with pre-authentication using an incorrect key is
     * rejected with the correct error message.
     * 
     * "If required to do so, the server pre-authenticates the request, and
     * if the pre-authentication check fails, an error message with the code
     * KDC_ERR_PREAUTH_FAILED is returned."
     * 
     * @throws Exception 
     */
    public void testPreAuthenticationIntegrityFailed() throws Exception
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

        String passPhrase = "badpassword";
        PaData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Integrity check on decrypted field failed", 31, error.getErrorCode() );
    }


    /**
     * "If required to do so, the server pre-authenticates the request, and
     * if the pre-authentication check fails, an error message with the code
     * KDC_ERR_PREAUTH_FAILED is returned."
     * 
     * @throws Exception 
     */
    public void testPreAuthenticationFailed() throws Exception
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

        KerberosTime timeStamp = new KerberosTime( 0 );
        String passPhrase = "secret";
        PaData[] paData = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase, timeStamp );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();

        assertEquals( "Pre-authentication information was invalid", 24, error.getErrorCode() );
    }


    /**
     * Tests when pre-authentication is included that is not supported by the KDC, that
     * the correct error message is returned.
     * 
     * @throws Exception 
     */
    public void testPreAuthenticationNoSupport() throws Exception
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
        PaData[] paData = getPreAuthPublicKey( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();

        assertEquals( "KDC has no support for padata type", 16, error.getErrorCode() );
    }


    /**
     * Returns pre-authentication payload of type PA_PK_AS_REQ.  Note that the actual
     * payload is an encrypted timestamp, but with only the type set to PA_PK_AS_REQ.
     * This is being used to test the error condition when an unsupported pre-authentication
     * type is received by the KDC.  The time for the timestamp is set to the current time.
     *
     * @param clientPrincipal
     * @param passPhrase
     * @return The array of pre-authentication data.
     * @throws Exception
     */
    private PaData[] getPreAuthPublicKey( KerberosPrincipal clientPrincipal, String passPhrase )
        throws Exception
    {
        KerberosTime timeStamp = new KerberosTime();

        return getPreAuthPublicKey( clientPrincipal, passPhrase, timeStamp );
    }


    /**
     * Returns pre-authentication payload of type PA_PK_AS_REQ.  Note that the actual
     * payload is an encrypted timestamp, but with the type set to PA_PK_AS_REQ.  This
     * is being used to test the error condition caused when an unsupported
     * pre-authentication type is received by the KDC.
     *
     * @param clientPrincipal
     * @param passPhrase
     * @param timeStamp
     * @return The array of pre-authentication data.
     * @throws Exception
     */
    private PaData[] getPreAuthPublicKey( KerberosPrincipal clientPrincipal, String passPhrase,
        KerberosTime timeStamp ) throws Exception
    {
        PaData[] paData = new PaData[1];

        EncryptedTimeStamp encryptedTimeStamp = new EncryptedTimeStamp( timeStamp, 0 );

        EncryptionKey clientKey = getEncryptionKey( clientPrincipal, passPhrase );

        EncryptedData encryptedData = lockBox.seal( clientKey, encryptedTimeStamp, KeyUsage.NUMBER1 );

        byte[] encodedEncryptedData = EncryptedDataEncoder.encode( encryptedData );

        PaData preAuth = new PaData();
        preAuth.setPaDataType( PaDataType.PA_PK_AS_REQ );
        preAuth.setPaDataValue( encodedEncryptedData );

        paData[0] = preAuth;

        return paData;
    }
}
