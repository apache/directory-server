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

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PaDataType;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.KdcReq;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.apache.directory.shared.kerberos.components.PaData;
import org.apache.directory.shared.kerberos.components.PaEncTsEnc;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.messages.AsReq;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests pre-authentication processing in the Authentication Service (AS) via the
 * {@link KerberosProtocolHandler}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PreAuthenticationTest extends AbstractAuthenticationServiceTest
{
    private KdcServer config;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private KrbDummySession session;


    /**
     * Creates a new instance of {@link PreAuthenticationTest}.
     */
    @Before
    public void setUp()
    {
        Set<EncryptionType> encryptionTypes = new HashSet<EncryptionType>();
        encryptionTypes.add( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        
        config = new KdcServer();
        
        config.setEncryptionTypes( encryptionTypes );
        
        store  = new MapPrincipalStoreImpl();
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
     * Tests when the KDC configuration requires pre-authentication by encrypted
     * timestamp that an AS_REQ without pre-authentication is rejected with the
     * correct error message.
     * 
     * "If pre-authentication is required, but was not present in the request, an
     * error message with the code KDC_ERR_PREAUTH_REQUIRED is returned, and a
     * METHOD-DATA object will be stored in the e-data field of the KRB-ERROR
     * message to specify which pre-authentication mechanisms are acceptable."
     */
    @Test
    public void testPreAuthenticationRequired()
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        KdcReq message = new AsReq();
        message.setKdcReqBody( kdcReqBody );

        handler.messageReceived( session, message );

        Object msg = session.getMessage();
        assertEquals( "session.getMessage() instanceOf", KrbError.class, msg.getClass() );
        KrbError error = ( KrbError ) msg;
        assertEquals( "Additional pre-authentication required", ErrorType.KDC_ERR_PREAUTH_REQUIRED, error.getErrorCode() );
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
    @Test
    public void testPreAuthenticationIntegrityFailed() throws Exception
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "badpassword";
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
        assertEquals( "Integrity check on decrypted field failed", ErrorType.KRB_AP_ERR_BAD_INTEGRITY, error.getErrorCode() );
    }


    /**
     * "If required to do so, the server pre-authenticates the request, and
     * if the pre-authentication check fails, an error message with the code
     * KDC_ERR_PREAUTH_FAILED is returned."
     * 
     * @throws Exception 
     */
    @Test
    public void testPreAuthenticationFailed() throws Exception
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        KerberosTime timeStamp = new KerberosTime( 0 );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthEncryptedTimeStamp( clientPrincipal, passPhrase, timeStamp );

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

        assertEquals( "Pre-authentication information was invalid", ErrorType.KDC_ERR_PREAUTH_FAILED, error.getErrorCode() );
    }


    /**
     * Tests when pre-authentication is included that is not supported by the KDC, that
     * the correct error message is returned.
     * 
     * @throws Exception 
     */
    @Test
    public void testPreAuthenticationNoSupport() throws Exception
    {
        KdcReqBody kdcReqBody = new KdcReqBody();
        kdcReqBody.setCName( getPrincipalName( "hnelson" ) );
        kdcReqBody.setSName( getPrincipalName( "krbtgt/EXAMPLE.COM@EXAMPLE.COM" ) );
        kdcReqBody.setRealm( "EXAMPLE.COM" );
        kdcReqBody.setEType( config.getEncryptionTypes() );

        kdcReqBody.setKdcOptions( new KdcOptions() );

        long now = System.currentTimeMillis();

        KerberosTime requestedEndTime = new KerberosTime( now + KerberosTime.DAY );
        kdcReqBody.setTill( requestedEndTime );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        String passPhrase = "secret";
        PaData[] paDatas = getPreAuthPublicKey( clientPrincipal, passPhrase );

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

        assertEquals( "KDC has no support for padata type", ErrorType.KDC_ERR_PADATA_TYPE_NOSUPP, error.getErrorCode() );
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

        PaEncTsEnc encryptedTimeStamp = new PaEncTsEnc( timeStamp, 0 );

        EncryptionKey clientKey = getEncryptionKey( clientPrincipal, passPhrase );

        EncryptedData encryptedData = lockBox.seal( clientKey, encryptedTimeStamp, KeyUsage.AS_REQ_PA_ENC_TIMESTAMP_WITH_CKEY );

        ByteBuffer buffer = ByteBuffer.allocate( encryptedData.computeLength() );
        byte[] encodedEncryptedData = encryptedData.encode( buffer ).array();

        PaData preAuth = new PaData();
        preAuth.setPaDataType( PaDataType.PA_PK_AS_REQ );
        preAuth.setPaDataValue( encodedEncryptedData );

        paData[0] = preAuth;

        return paData;
    }
}
