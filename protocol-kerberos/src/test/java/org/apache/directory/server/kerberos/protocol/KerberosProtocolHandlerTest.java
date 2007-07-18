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


import java.net.InetSocketAddress;
import java.net.SocketAddress;

import javax.security.auth.kerberos.KerberosPrincipal;

import junit.framework.TestCase;

import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
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
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalName;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalNameModifier;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalNameType;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBodyModifier;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.kerberos.shared.store.TicketFactory;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.support.BaseIoSession;


/**
 * Tests the KerberosProtocolHandler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KerberosProtocolHandlerTest extends TestCase
{
    private KdcConfiguration config;
    private PrincipalStore store;
    private KerberosProtocolHandler handler;
    private DummySession session;


    public KerberosProtocolHandlerTest()
    {
        config = new KdcConfiguration();
        store = new MapPrincipalStoreImpl();
        handler = new KerberosProtocolHandler( config, store );
        session = new DummySession();
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


    public void testServerNotFound() throws Exception
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "badserver" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthenticationData( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "Server not found in Kerberos database", 7, error.getErrorCode() );
    }


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


    public void testServerNullKey() throws Exception
    {
        RequestBodyModifier modifier = new RequestBodyModifier();
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setServerName( getPrincipalName( "tquist" ) );
        modifier.setRealm( "EXAMPLE.COM" );
        modifier.setEType( config.getEncryptionTypes() );

        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        String passPhrase = "secret";
        PreAuthenticationData[] paData = getPreAuthenticationData( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        ErrorMessage error = ( ErrorMessage ) session.getMessage();
        assertEquals( "The client or server has a null key", 9, error.getErrorCode() );
    }


    public void testSpecificEndTime() throws Exception
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
        PreAuthenticationData[] paData = getPreAuthenticationData( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

        assertTrue( "Requested end time", requestedEndTime.equals( reply.getEndTime() ) );
    }


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
        PreAuthenticationData[] paData = getPreAuthenticationData( clientPrincipal, passPhrase );

        KdcRequest message = new KdcRequest( 5, MessageType.KRB_AS_REQ, paData, modifier.getRequestBody() );

        handler.messageReceived( session, message );

        AuthenticationReply reply = ( AuthenticationReply ) session.getMessage();

        KerberosTime expectedEndTime = new KerberosTime( now + KerberosTime.DAY );
        boolean isClose = Math.abs( reply.getEndTime().getTime() - expectedEndTime.getTime() ) < 5000;
        assertTrue( "Expected end time", isClose );
    }


    public void testRenewableOk()
    {
        // RENEWABLE_OK defaulted on.
        // if ( renew_till non-zero || renewable set )
        // {
        //     clear renewable_ok
        // }

        // renewable and renewable_ok should not be set at the same time.
    }


    private PreAuthenticationData[] getPreAuthenticationData( KerberosPrincipal clientPrincipal, String passPhrase )
        throws Exception
    {
        PreAuthenticationData[] paData = new PreAuthenticationData[1];

        CipherTextHandler lockBox = new CipherTextHandler();

        KerberosTime timeStamp = new KerberosTime();
        EncryptedTimeStamp encryptedTimeStamp = new EncryptedTimeStamp( timeStamp, 0 );

        TicketFactory ticketFactory = new TicketFactory();
        EncryptionKey clientKey = ticketFactory.getServerKey( clientPrincipal, passPhrase );

        EncryptedData encryptedData = lockBox.seal( clientKey, encryptedTimeStamp, KeyUsage.NUMBER1 );

        byte[] encodedEncryptedData = EncryptedDataEncoder.encode( encryptedData );

        PreAuthenticationDataModifier preAuth = new PreAuthenticationDataModifier();
        preAuth.setDataType( PreAuthenticationDataType.PA_ENC_TIMESTAMP );
        preAuth.setDataValue( encodedEncryptedData );

        paData[0] = preAuth.getPreAuthenticationData();

        return paData;
    }


    private PrincipalName getPrincipalName( String principalName )
    {
        PrincipalNameModifier principalNameModifier = new PrincipalNameModifier();
        principalNameModifier.addName( principalName );
        principalNameModifier.setType( PrincipalNameType.KRB_NT_PRINCIPAL.getOrdinal() );

        return principalNameModifier.getPrincipalName();
    }

    private static class DummySession extends BaseIoSession
    {
        Object message;


        @Override
        public WriteFuture write( Object message )
        {
            this.message = message;

            return super.write( message );
        }


        public Object getMessage()
        {
            return message;
        }


        protected void updateTrafficMask()
        {
        }


        public IoService getService()
        {
            return null;
        }


        public IoHandler getHandler()
        {
            return null;
        }


        public IoFilterChain getFilterChain()
        {
            return null;
        }


        public TransportType getTransportType()
        {
            return null;
        }


        public SocketAddress getRemoteAddress()
        {
            return new InetSocketAddress( 10088 );
        }


        public SocketAddress getLocalAddress()
        {
            return null;
        }


        public IoSessionConfig getConfig()
        {
            return null;
        }


        public int getScheduledWriteRequests()
        {
            return 0;
        }


        public SocketAddress getServiceAddress()
        {
            return null;
        }


        public IoServiceConfig getServiceConfig()
        {
            return null;
        }


        public int getScheduledWriteBytes()
        {
            return 0;
        }
    }
}
