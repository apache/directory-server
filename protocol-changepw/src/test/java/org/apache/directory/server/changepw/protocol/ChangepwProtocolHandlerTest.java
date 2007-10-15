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


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.security.auth.kerberos.KerberosPrincipal;

import junit.framework.TestCase;
import org.apache.directory.server.changepw.ChangePasswordServer;
import org.apache.directory.server.changepw.io.ChangePasswordDataEncoder;
import org.apache.directory.server.changepw.messages.ChangePasswordError;
import org.apache.directory.server.changepw.messages.ChangePasswordRequest;
import org.apache.directory.server.changepw.value.ChangePasswordData;
import org.apache.directory.server.changepw.value.ChangePasswordDataModifier;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.ErrorMessage;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.application.PrivateMessage;
import org.apache.directory.server.kerberos.shared.messages.components.AuthenticatorModifier;
import org.apache.directory.server.kerberos.shared.messages.components.EncKrbPrivPart;
import org.apache.directory.server.kerberos.shared.messages.components.EncKrbPrivPartModifier;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.ApOptions;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalName;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalNameModifier;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalNameType;
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
 * Tests the ChangePasswordProtocolHandler.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChangepwProtocolHandlerTest extends TestCase
{
    /**
     * The Change Password SUCCESS result code.
     */
    // Never used...
    //private static final byte[] SUCCESS = new byte[]
    //    { ( byte ) 0x00, ( byte ) 0x00 };

    private ChangePasswordServer config;
    private PrincipalStore store;
    private ChangePasswordProtocolHandler handler;
    private DummySession session;

    private CipherTextHandler cipherTextHandler = new CipherTextHandler();


    /**
     * Creates a new instance of ChangepwProtocolHandlerTest.
     */
    public ChangepwProtocolHandlerTest()
    {
        config = new ChangePasswordServer();
        store = new MapPrincipalStoreImpl();
        handler = new ChangePasswordProtocolHandler( config, store );
        session = new DummySession();
    }


    /**
     * Tests the protocol version number, which must be '1'.
     */
    public void testProtocolVersionNumber()
    {
        ChangePasswordRequest message = new ChangePasswordRequest( ( short ) 2, null, null );

        handler.messageReceived( session, message );

        ChangePasswordError reply = ( ChangePasswordError ) session.getMessage();
        ErrorMessage error = reply.getErrorMessage();
        assertEquals( "Protocol version unsupported", 6, error.getErrorCode() );
    }


    /**
     * Tests when a service ticket is missing that the request is rejected with
     * the correct error message.
     */
    public void testMissingTicket()
    {
        ChangePasswordRequest message = new ChangePasswordRequest( ( short ) 1, null, null );

        handler.messageReceived( session, message );

        ChangePasswordError reply = ( ChangePasswordError ) session.getMessage();
        ErrorMessage error = reply.getErrorMessage();
        assertEquals( "Request failed due to an error in authentication processing", 3, error.getErrorCode() );
    }


    /**
     * Tests when the INITIAL flag is missing that the request is rejected with
     * the correct error message.
     *
     * @throws Exception
     */
    public void testInitialFlagRequired() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "kadmin/changepw@EXAMPLE.COM" );
        String serverPassword = "secret";

        TicketFactory ticketFactory = new TicketFactory();
        EncryptionKey serverKey = ticketFactory.getServerKey( serverPrincipal, serverPassword );
        Ticket serviceTicket = ticketFactory.getTicket( clientPrincipal, serverPrincipal, serverKey );

        EncryptionKey subSessionKey = RandomKeyFactory.getRandomKey( EncryptionType.DES_CBC_MD5 );

        ApOptions apOptions = new ApOptions();

        AuthenticatorModifier modifier = new AuthenticatorModifier();
        modifier.setVersionNumber( 5 );
        modifier.setClientRealm( "EXAMPLE.COM" );
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setClientTime( new KerberosTime() );
        modifier.setClientMicroSecond( 0 );

        modifier.setSubSessionKey( subSessionKey );

        EncryptedData encryptedAuthenticator = cipherTextHandler.seal( serviceTicket.getSessionKey(), modifier
                .getAuthenticator(), KeyUsage.NUMBER11 );

        ApplicationRequest apReq = new ApplicationRequest( apOptions, serviceTicket, encryptedAuthenticator );

        String newPassword = "secretsecret";

        PrivateMessage priv = getChangePasswordPrivateMessage( newPassword, subSessionKey );

        ChangePasswordRequest message = new ChangePasswordRequest( ( short ) 1, apReq, priv );

        handler.messageReceived( session, message );

        ChangePasswordError reply = ( ChangePasswordError ) session.getMessage();
        ErrorMessage error = reply.getErrorMessage();
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


    public void testSetPassword() throws Exception
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );

        KerberosPrincipal serverPrincipal = new KerberosPrincipal( "kadmin/changepw@EXAMPLE.COM" );
        String serverPassword = "secret";

        TicketFactory ticketFactory = new TicketFactory();
        EncryptionKey serverKey = ticketFactory.getServerKey( serverPrincipal, serverPassword );
        Ticket serviceTicket = ticketFactory.getTicket( clientPrincipal, serverPrincipal, serverKey );

        EncryptionKey subSessionKey = RandomKeyFactory.getRandomKey( EncryptionType.DES_CBC_MD5 );

        ApOptions apOptions = new ApOptions();

        AuthenticatorModifier modifier = new AuthenticatorModifier();
        modifier.setVersionNumber( 5 );
        modifier.setClientRealm( "EXAMPLE.COM" );
        modifier.setClientName( getPrincipalName( "hnelson" ) );
        modifier.setClientTime( new KerberosTime() );
        modifier.setClientMicroSecond( 0 );

        EncryptedData encryptedAuthenticator = cipherTextHandler.seal( serverKey, modifier.getAuthenticator(),
                KeyUsage.NUMBER11 );

        ApplicationRequest apReq = new ApplicationRequest( apOptions, serviceTicket, encryptedAuthenticator );

        String newPassword = "secretsecret";

        PrivateMessage priv = getSetPasswordPrivateMessage( newPassword, subSessionKey, getPrincipalName( "hnelson" ) );

        ChangePasswordRequest message = new ChangePasswordRequest( ( short ) 0xFF80, apReq, priv );

        handler.messageReceived( session, message );

        ChangePasswordError reply = ( ChangePasswordError ) session.getMessage();
        ErrorMessage error = reply.getErrorMessage();
        assertEquals( "Protocol version unsupported", 6, error.getErrorCode() );
    }


    /*
     * Legacy kpasswd (Change Password) version.  User data is the password bytes.
     */
    private PrivateMessage getChangePasswordPrivateMessage( String newPassword, EncryptionKey subSessionKey )
            throws UnsupportedEncodingException, KerberosException, UnknownHostException
    {
        // Make private message part.
        EncKrbPrivPartModifier privPartModifier = new EncKrbPrivPartModifier();
        privPartModifier.setUserData( newPassword.getBytes( "UTF-8" ) );
        privPartModifier.setSenderAddress( new HostAddress( InetAddress.getLocalHost() ) );
        EncKrbPrivPart encReqPrivPart = privPartModifier.getEncKrbPrivPart();

        // Seal private message part.
        EncryptedData encryptedPrivPart = cipherTextHandler.seal( subSessionKey, encReqPrivPart, KeyUsage.NUMBER13 );

        // Make private message with private message part.
        PrivateMessage privateMessage = new PrivateMessage();
        privateMessage.setProtocolVersionNumber( 5 );
        privateMessage.setMessageType( MessageType.ENC_PRIV_PART );
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
        EncKrbPrivPartModifier privPartModifier = new EncKrbPrivPartModifier();

        ChangePasswordDataModifier dataModifier = new ChangePasswordDataModifier();
        dataModifier.setNewPassword( newPassword.getBytes() );
        dataModifier.setTargetName( targetPrincipalName );
        dataModifier.setTargetRealm( "EXAMPLE.COM" );
        ChangePasswordData data = dataModifier.getChangePasswdData();

        ChangePasswordDataEncoder encoder = new ChangePasswordDataEncoder();
        byte[] dataBytes = encoder.encode( data );

        privPartModifier.setUserData( dataBytes );

        privPartModifier.setSenderAddress( new HostAddress( InetAddress.getLocalHost() ) );
        EncKrbPrivPart encReqPrivPart = privPartModifier.getEncKrbPrivPart();

        // Seal private message part.
        EncryptedData encryptedPrivPart = cipherTextHandler.seal( subSessionKey, encReqPrivPart, KeyUsage.NUMBER13 );

        // Make private message with private message part.
        PrivateMessage privateMessage = new PrivateMessage();
        privateMessage.setProtocolVersionNumber( 5 );
        privateMessage.setMessageType( MessageType.ENC_PRIV_PART );
        privateMessage.setEncryptedPart( encryptedPrivPart );

        return privateMessage;
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


        private Object getMessage()
        {
            return message;
        }


        protected void updateTrafficMask()
        {
            // Do nothing.
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
            return new InetSocketAddress( 10464 );
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
