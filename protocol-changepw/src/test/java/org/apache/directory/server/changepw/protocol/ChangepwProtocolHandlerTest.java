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
package org.apache.directory.server.changepw.protocol;


import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.server.changepw.ChangePasswordServer;
import org.apache.directory.server.changepw.io.ChangePasswordDataEncoder;
import org.apache.directory.server.changepw.messages.ChangePasswordError;
import org.apache.directory.server.changepw.messages.ChangePasswordRequest;
import org.apache.directory.server.changepw.value.ChangePasswordData;
import org.apache.directory.server.changepw.value.ChangePasswordDataModifier;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.messages.application.PrivateMessage;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.kerberos.shared.store.TicketFactory;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.options.ApOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.EncKrbPrivPart;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.apache.directory.shared.kerberos.messages.Authenticator;
import org.apache.directory.shared.kerberos.messages.KrbError;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.DummySession;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the ChangePasswordProtocolHandler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class ChangepwProtocolHandlerTest
{
    /**
     * The Change Password SUCCESS result code.
     */
    // Never used...
    //private static final byte[] SUCCESS = new byte[]
    //    { ( byte ) 0x00, ( byte ) 0x00 };

    private static ChangePasswordServer config;
    private static PrincipalStore store;
    private static ChangePasswordProtocolHandler handler;

    private static final CipherTextHandler cipherTextHandler = new CipherTextHandler();


    /**
     * Creates a new instance of ChangepwProtocolHandlerTest.
     */
    @BeforeClass
    public static void setup()
    {
        config = new ChangePasswordServer();
        store = new MapPrincipalStoreImpl();
        handler = new ChangePasswordProtocolHandler( config, store );
    }


    /**
     * Tests the protocol version number, which must be '1'.
     */
    @Test
    public void testProtocolVersionNumber()
    {
        ChPwdDummySession session = new ChPwdDummySession();
        ChangePasswordRequest message = new ChangePasswordRequest( ( short ) 2, null, null );

        handler.messageReceived( session, message );

        ChangePasswordError reply = ( ChangePasswordError ) session.getMessage();
        KrbError error = reply.getKrbError();
        assertEquals( "Protocol version unsupported", 6, error.getErrorCode() );
    }


    /**
     * Tests when a service ticket is missing that the request is rejected with
     * the correct error message.
     */
    @Test
    public void testMissingTicket()
    {
        ChPwdDummySession session = new ChPwdDummySession();
        ChangePasswordRequest message = new ChangePasswordRequest( ( short ) 1, null, null );

        handler.messageReceived( session, message );

        ChangePasswordError reply = ( ChangePasswordError ) session.getMessage();
        KrbError error = reply.getKrbError();
        assertEquals( "Request failed due to an error in authentication processing", 3, error.getErrorCode() );
    }


    /**
     * Tests when the INITIAL flag is missing that the request is rejected with
     * the correct error message.
     *
     * @throws Exception
     */
    @Test
    @Ignore( "test started to fail after changes done to kerberos-codec, should be verified after completing the codec work" )
    public void testInitialFlagRequired() throws Exception
    {
        ChPwdDummySession session = new ChPwdDummySession();
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "kadmin/changepw@EXAMPLE.COM" );
        String serverPassword = "secret";

        TicketFactory ticketFactory = new TicketFactory();
        EncryptionKey serverKey = ticketFactory.getServerKey( serverPrincipal, serverPassword );
        Ticket serviceTicket = ticketFactory.getTicket( clientPrincipal, serverPrincipal, serverKey );

        EncryptionKey subSessionKey = RandomKeyFactory.getRandomKey( EncryptionType.DES_CBC_MD5 );

        ApOptions apOptions = new ApOptions();

        Authenticator authenticator = new Authenticator();
        authenticator.setVersionNumber( 5 );
        authenticator.setCRealm( "EXAMPLE.COM" );
        authenticator.setCName( getPrincipalName( "hnelson" ) );
        authenticator.setCTime( new KerberosTime() );
        authenticator.setCusec( 0 );

        authenticator.setSubKey( subSessionKey );

        EncryptedData encryptedAuthenticator = cipherTextHandler.seal( serviceTicket.getEncTicketPart().getKey(), authenticator
                , KeyUsage.AP_REQ_AUTHNT_SESS_KEY );

        ApReq apReq = new ApReq();
        apReq.setOption( apOptions );
        apReq.setTicket( serviceTicket );
        apReq.setAuthenticator( encryptedAuthenticator );

        String newPassword = "secretsecret";

        PrivateMessage priv = getChangePasswordPrivateMessage( newPassword, subSessionKey );

        ChangePasswordRequest message = new ChangePasswordRequest( ( short ) 1, apReq, priv );

        handler.messageReceived( session, message );

        ChangePasswordError reply = ( ChangePasswordError ) session.getMessage();
        KrbError error = reply.getKrbError();
        assertEquals( "Initial flag required", 7, error.getErrorCode() );

        //ChangePasswordReply reply = ( ChangePasswordReply ) session.getMessage();

        //processChangePasswordReply( reply, serviceTicket.getSessionKey(), subSessionKey );
    }

    /**
     * TODO : Check if this method is important or not. It was called in
     * the testInitialFlagRequired() method above, but this call has been commented
     * private void processChangePasswordReply( ChangePasswordReply reply, EncryptionKey sessionKey,
     * EncryptionKey subSessionKey ) throws Exception
     * {
     * PrivateMessage privateMessage = reply.getPrivateMessage();
     * <p/>
     * EncryptedData encPrivPart = privateMessage.getEncryptedPart();
     * <p/>
     * EncKrbPrivPart privPart;
     * <p/>
     * try
     * {
     * privPart = ( EncKrbPrivPart ) cipherTextHandler.unseal( EncKrbPrivPart.class, subSessionKey, encPrivPart,
     * KeyUsage.NUMBER13 );
     * }
     * catch ( KerberosException ke )
     * {
     * return;
     * }
     * <p/>
     * // Verify result code.
     * byte[] resultCode = privPart.getUserData();
     * <p/>
     * assertTrue( "Password change returned SUCCESS (0x00 0x00).", Arrays.equals( SUCCESS, resultCode ) );
     * }
     */


    @Test
    public void testSetPassword() throws Exception
    {
        ChPwdDummySession session = new ChPwdDummySession();
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "kadmin/changepw@EXAMPLE.COM" );
        String serverPassword = "secret";

        TicketFactory ticketFactory = new TicketFactory();
        EncryptionKey serverKey = ticketFactory.getServerKey( serverPrincipal, serverPassword );
        Ticket serviceTicket = ticketFactory.getTicket( clientPrincipal, serverPrincipal, serverKey );

        EncryptionKey subSessionKey = RandomKeyFactory.getRandomKey( EncryptionType.DES_CBC_MD5 );

        ApOptions apOptions = new ApOptions();

        Authenticator authenticator = new Authenticator();
        authenticator.setVersionNumber( 5 );
        authenticator.setCRealm( "EXAMPLE.COM" );
        authenticator.setCName( getPrincipalName( "hnelson" ) );
        authenticator.setCTime( new KerberosTime() );
        authenticator.setCusec( 0 );

        EncryptedData encryptedAuthenticator = cipherTextHandler.seal( serverKey, authenticator,
                KeyUsage.AP_REQ_AUTHNT_SESS_KEY );

        ApReq apReq = new ApReq();
        apReq.setOption( apOptions );
        apReq.setTicket( serviceTicket );
        apReq.setAuthenticator( encryptedAuthenticator );

        String newPassword = "secretsecret";

        PrivateMessage priv = getSetPasswordPrivateMessage( newPassword, subSessionKey, getPrincipalName( "hnelson" ) );

        ChangePasswordRequest message = new ChangePasswordRequest( ( short ) 0xFF80, apReq, priv );

        handler.messageReceived( session, message );

        ChangePasswordError reply = ( ChangePasswordError ) session.getMessage();
        KrbError error = reply.getKrbError();
        assertEquals( "Protocol version unsupported", 6, error.getErrorCode() );
    }


    /*
     * Legacy kpasswd (Change Password) version.  User data is the password bytes.
     */
    private PrivateMessage getChangePasswordPrivateMessage( String newPassword, EncryptionKey subSessionKey )
            throws UnsupportedEncodingException, KerberosException, UnknownHostException
    {
        // Make private message part.
        EncKrbPrivPart encReqPrivPart = new EncKrbPrivPart();
        encReqPrivPart.setUserData( newPassword.getBytes( "UTF-8" ) );
        encReqPrivPart.setSenderAddress( new HostAddress( InetAddress.getLocalHost() ) );

        // Seal private message part.
        EncryptedData encryptedPrivPart = cipherTextHandler.seal( subSessionKey, encReqPrivPart, KeyUsage.KRB_PRIV_ENC_PART_CHOSEN_KEY );

        // Make private message with private message part.
        PrivateMessage privateMessage = new PrivateMessage();
        privateMessage.setProtocolVersionNumber( 5 );
        privateMessage.setMessageType( KerberosMessageType.ENC_PRIV_PART );
        privateMessage.setEncryptedPart( encryptedPrivPart );

        return privateMessage;
    }


    /*
     * Set/Change Password version.  User data is an encoding of the new password and the target principal.
     */
    private PrivateMessage getSetPasswordPrivateMessage( String newPassword, EncryptionKey subSessionKey,
            PrincipalName targetPrincipalName ) throws UnsupportedEncodingException, KerberosException,
            UnknownHostException, IOException
    {
        // Make private message part.
        EncKrbPrivPart encReqPrivPart = new EncKrbPrivPart();

        ChangePasswordDataModifier dataModifier = new ChangePasswordDataModifier();
        dataModifier.setNewPassword( newPassword.getBytes() );
        dataModifier.setTargetName( targetPrincipalName );
        dataModifier.setTargetRealm( "EXAMPLE.COM" );
        ChangePasswordData data = dataModifier.getChangePasswdData();

        ChangePasswordDataEncoder encoder = new ChangePasswordDataEncoder();
        byte[] dataBytes = encoder.encode( data );

        encReqPrivPart.setUserData( dataBytes );

        encReqPrivPart.setSenderAddress( new HostAddress( InetAddress.getLocalHost() ) );

        // Seal private message part.
        EncryptedData encryptedPrivPart = cipherTextHandler.seal( subSessionKey, encReqPrivPart, KeyUsage.KRB_PRIV_ENC_PART_CHOSEN_KEY );

        // Make private message with private message part.
        PrivateMessage privateMessage = new PrivateMessage();
        privateMessage.setProtocolVersionNumber( 5 );
        privateMessage.setMessageType( KerberosMessageType.ENC_PRIV_PART );
        privateMessage.setEncryptedPart( encryptedPrivPart );

        return privateMessage;
    }


    private PrincipalName getPrincipalName( String name )
    {
        PrincipalName principalName = new PrincipalName();
        principalName.addName( name );
        principalName.setNameType( PrincipalNameType.KRB_NT_PRINCIPAL );

        return principalName;
    }

    private static class ChPwdDummySession extends DummySession
    {
        Object message;

        
        private Object getMessage()
        {
            return message;
        }


        public SocketAddress getRemoteAddress()
        {
            return new InetSocketAddress( 10464 );
        }
        
        
        public int getScheduledWriteRequests()
        {
            return 0;
        }
        
        
        public WriteFuture write(Object message) 
        {
            this.message = message;
            return null;
        }
    }
}
