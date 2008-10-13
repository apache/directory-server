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
package org.apache.directory.server.changepw.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.naming.NamingException;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.changepw.ChangePasswordServer;
import org.apache.directory.server.changepw.exceptions.ChangePasswordException;
import org.apache.directory.server.changepw.exceptions.ErrorType;
import org.apache.directory.server.changepw.io.ChangePasswordDataDecoder;
import org.apache.directory.server.changepw.messages.ChangePasswordReply;
import org.apache.directory.server.changepw.messages.ChangePasswordReplyModifier;
import org.apache.directory.server.changepw.messages.ChangePasswordRequest;
import org.apache.directory.server.changepw.value.ChangePasswordData;
import org.apache.directory.server.changepw.value.ChangePasswordDataModifier;
import org.apache.directory.server.kerberos.shared.KerberosUtils;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
//import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.messages.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.application.ApplicationReply;
import org.apache.directory.server.kerberos.shared.messages.application.PrivateMessage;
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.EncApRepPart;
import org.apache.directory.server.kerberos.shared.messages.components.EncApRepPartModifier;
import org.apache.directory.server.kerberos.shared.messages.components.EncKrbPrivPart;
import org.apache.directory.server.kerberos.shared.messages.components.EncKrbPrivPartModifier;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.replay.InMemoryReplayCache;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangePasswordService
{
    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ChangePasswordService.class );

    private static final ReplayCache replayCache = new InMemoryReplayCache();
    
    private static final CipherTextHandler cipherTextHandler = new CipherTextHandler();

    
    public static void execute( IoSession session, ChangePasswordContext changepwContext ) throws KerberosException, IOException
    {
        if ( LOG.isDebugEnabled() )
        {
            monitorRequest( changepwContext );
        }
        
        configureChangePassword( changepwContext );
        getAuthHeader( session, changepwContext );
        verifyServiceTicket( changepwContext );
        getServerEntry( changepwContext );
        verifyServiceTicketAuthHeader( changepwContext );
        extractPassword( changepwContext );
        
        if ( LOG.isDebugEnabled() )
        {
            monitorContext( changepwContext );
        }
        
        processPasswordChange( changepwContext );
        buildReply( changepwContext );
        
        if ( LOG.isDebugEnabled() )
        {
            monitorReply( changepwContext );
        }
    }
    
    
    private static void processPasswordChange( ChangePasswordContext changepwContext ) throws KerberosException
    {
        PrincipalStore store = changepwContext.getStore();
        Authenticator authenticator = changepwContext.getAuthenticator();
        String newPassword = changepwContext.getPassword();
        KerberosPrincipal clientPrincipal = authenticator.getClientPrincipal();

        // usec and seq-number must be present per MS but aren't in legacy kpasswd
        // seq-number must have same value as authenticator
        // ignore r-address

        try
        {
            String principalName = store.changePassword( clientPrincipal, newPassword );
            LOG.debug( "Successfully modified principal {}.", principalName );
        }
        catch ( NamingException ne )
        {
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_SOFTERROR, ne.getExplanation().getBytes(), ne );
        }
        catch ( Exception e )
        {
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_HARDERROR, e );
        }
    }
    
    
    private static void monitorRequest( ChangePasswordContext changepwContext ) throws KerberosException
    {
        try
        {
            ChangePasswordRequest request = ( ChangePasswordRequest ) changepwContext.getRequest();
            short versionNumber = request.getVersionNumber();

            StringBuffer sb = new StringBuffer();
            sb.append( "Responding to change password request:" );
            sb.append( "\n\t" + "versionNumber    " + versionNumber );

            LOG.debug( sb.toString() );
        }
        catch ( Exception e )
        {
            // This is a monitor.  No exceptions should bubble up.
            LOG.error( "Error in request monitor", e );
        }
    }
    
    
    private static void configureChangePassword( ChangePasswordContext changepwContext )
    {
        changepwContext.setReplayCache( replayCache );
        changepwContext.setCipherTextHandler( cipherTextHandler );
    }
    
    
    private static void getAuthHeader( IoSession session, ChangePasswordContext changepwContext ) throws KerberosException
    {
        ChangePasswordRequest request = ( ChangePasswordRequest ) changepwContext.getRequest();

        if ( request.getVersionNumber() != 1 )
        {
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_BAD_VERSION );
        }

        if ( request.getAuthHeader() == null || request.getAuthHeader().getTicket() == null )
        {
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_AUTHERROR );
        }

        ApplicationRequest authHeader = request.getAuthHeader();
        Ticket ticket = authHeader.getTicket();

        changepwContext.setAuthHeader( authHeader );
        changepwContext.setTicket( ticket );
    }
    
    
    private static void verifyServiceTicket( ChangePasswordContext changepwContext ) throws KerberosException
    {
        ChangePasswordServer config = changepwContext.getConfig();
        Ticket ticket = changepwContext.getTicket();
        String primaryRealm = config.getPrimaryRealm();
        KerberosPrincipal changepwPrincipal = config.getServicePrincipal();
        KerberosPrincipal serverPrincipal = ticket.getServerPrincipal(); 

        if ( !ticket.getRealm().equals( primaryRealm ) || !serverPrincipal.equals( changepwPrincipal ) )
        {
            throw new KerberosException( org.apache.directory.server.kerberos.shared.exceptions.ErrorType.KRB_AP_ERR_NOT_US );
        }
    }
    
    
    private static void getServerEntry( ChangePasswordContext changepwContext ) throws KerberosException
    {
        KerberosPrincipal principal =  changepwContext.getTicket().getServerPrincipal();
        PrincipalStore store = changepwContext.getStore();

        changepwContext.setServerEntry( KerberosUtils.getEntry( principal, store, org.apache.directory.server.kerberos.shared.exceptions.ErrorType.KDC_ERR_S_PRINCIPAL_UNKNOWN ) );
    }
    
    
    private static void verifyServiceTicketAuthHeader( ChangePasswordContext changepwContext ) throws KerberosException
    {
        ApplicationRequest authHeader = changepwContext.getAuthHeader();
        Ticket ticket = changepwContext.getTicket();

        EncryptionType encryptionType = ticket.getEncPart().getEType();
        EncryptionKey serverKey = changepwContext.getServerEntry().getKeyMap().get( encryptionType );

        long clockSkew = changepwContext.getConfig().getAllowableClockSkew();
        ReplayCache replayCache = changepwContext.getReplayCache();
        boolean emptyAddressesAllowed = changepwContext.getConfig().isEmptyAddressesAllowed();
        InetAddress clientAddress = changepwContext.getClientAddress();
        CipherTextHandler cipherTextHandler = changepwContext.getCipherTextHandler();

        Authenticator authenticator = KerberosUtils.verifyAuthHeader( authHeader, ticket, serverKey, clockSkew, replayCache,
            emptyAddressesAllowed, clientAddress, cipherTextHandler, KeyUsage.NUMBER11, false );

        ChangePasswordRequest request = ( ChangePasswordRequest ) changepwContext.getRequest();

        if ( request.getVersionNumber() == 1 && !ticket.getEncTicketPart().getFlags().isInitial() )
        {
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_INITIAL_FLAG_NEEDED );
        }

        changepwContext.setAuthenticator( authenticator );
    }
    
    
    private static void extractPassword( ChangePasswordContext changepwContext ) throws KerberosException, IOException
    {
        ChangePasswordRequest request = ( ChangePasswordRequest ) changepwContext.getRequest();
        Authenticator authenticator = changepwContext.getAuthenticator();
        CipherTextHandler cipherTextHandler = changepwContext.getCipherTextHandler();

        // TODO - check ticket is for service authorized to change passwords
        // ticket.getServerPrincipal().getName().equals(config.getChangepwPrincipal().getName()));

        // TODO - check client principal in ticket is authorized to change password

        // get the subsession key from the Authenticator
        EncryptionKey subSessionKey = authenticator.getSubSessionKey();

        // decrypt the request's private message with the subsession key
        EncryptedData encReqPrivPart = request.getPrivateMessage().getEncryptedPart();

        EncKrbPrivPart privatePart;

        try
        {
            privatePart = ( EncKrbPrivPart ) cipherTextHandler.unseal( EncKrbPrivPart.class, subSessionKey,
                encReqPrivPart, KeyUsage.NUMBER13 );
        }
        catch ( KerberosException ke )
        {
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_SOFTERROR, ke );
        }

        ChangePasswordData passwordData = null;

        if ( request.getVersionNumber() == ( short ) 1 )
        {
            // Use protocol version 0x0001, the legacy Kerberos change password protocol
            ChangePasswordDataModifier modifier = new ChangePasswordDataModifier();
            modifier.setNewPassword( privatePart.getUserData() );
            passwordData = modifier.getChangePasswdData();
        }
        else
        {
            // Use protocol version 0xFF80, the backwards-compatible MS protocol
            ChangePasswordDataDecoder passwordDecoder = new ChangePasswordDataDecoder();
            passwordData = passwordDecoder.decodeChangePasswordData( privatePart.getUserData() );
        }

        try
        {
            changepwContext.setPassword( new String( passwordData.getPassword(), "UTF-8" ) );
        }
        catch ( UnsupportedEncodingException uee )
        {
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_SOFTERROR, uee );
        }
    }

    
    private static void monitorContext( ChangePasswordContext changepwContext ) throws KerberosException
    {
        try
        {
            PrincipalStore store = changepwContext.getStore();
            ApplicationRequest authHeader = changepwContext.getAuthHeader();
            Ticket ticket = changepwContext.getTicket();
            ReplayCache replayCache = changepwContext.getReplayCache();
            long clockSkew = changepwContext.getConfig().getAllowableClockSkew();

            Authenticator authenticator = changepwContext.getAuthenticator();
            KerberosPrincipal clientPrincipal = authenticator.getClientPrincipal();
            String desiredPassword = changepwContext.getPassword();

            InetAddress clientAddress = changepwContext.getClientAddress();
            HostAddresses clientAddresses = ticket.getEncTicketPart().getClientAddresses();

            boolean caddrContainsSender = false;

            if ( ticket.getEncTicketPart().getClientAddresses() != null )
            {
                caddrContainsSender = ticket.getEncTicketPart().getClientAddresses().contains( new HostAddress( clientAddress ) );
            }

            StringBuffer sb = new StringBuffer();
            sb.append( "Monitoring context:" );
            sb.append( "\n\t" + "store                  " + store );
            sb.append( "\n\t" + "authHeader             " + authHeader );
            sb.append( "\n\t" + "ticket                 " + ticket );
            sb.append( "\n\t" + "replayCache            " + replayCache );
            sb.append( "\n\t" + "clockSkew              " + clockSkew );
            sb.append( "\n\t" + "clientPrincipal        " + clientPrincipal );
            sb.append( "\n\t" + "desiredPassword        " + desiredPassword );
            sb.append( "\n\t" + "clientAddress          " + clientAddress );
            sb.append( "\n\t" + "clientAddresses        " + clientAddresses );
            sb.append( "\n\t" + "caddr contains sender  " + caddrContainsSender );
            sb.append( "\n\t" + "Ticket principal       " + ticket.getServerPrincipal() );

            PrincipalStoreEntry ticketPrincipal = changepwContext.getServerEntry();
            
            sb.append( "\n\t" + "cn                     " + ticketPrincipal.getCommonName() );
            sb.append( "\n\t" + "realm                  " + ticketPrincipal.getRealmName() );
            sb.append( "\n\t" + "Service principal      " + ticketPrincipal.getPrincipal() );
            sb.append( "\n\t" + "SAM type               " + ticketPrincipal.getSamType() );

            EncryptionType encryptionType = ticket.getEncPart().getEType();
            int keyVersion = ticketPrincipal.getKeyMap().get( encryptionType ).getKeyVersion();
            sb.append( "\n\t" + "Ticket key type        " + encryptionType );
            sb.append( "\n\t" + "Service key version    " + keyVersion );

            LOG.debug( sb.toString() );
        }
        catch ( Exception e )
        {
            // This is a monitor.  No exceptions should bubble up.
            LOG.error( "Error in context monitor", e );
        }
    }
    
    
    private static void buildReply( ChangePasswordContext changepwContext ) throws KerberosException, UnknownHostException
    {
        Authenticator authenticator = changepwContext.getAuthenticator();
        Ticket ticket = changepwContext.getTicket();
        CipherTextHandler cipherTextHandler = changepwContext.getCipherTextHandler();

        // begin building reply

        // create priv message
        // user-data component is short result code
        EncKrbPrivPartModifier modifier = new EncKrbPrivPartModifier();
        byte[] resultCode =
            { ( byte ) 0x00, ( byte ) 0x00 };
        modifier.setUserData( resultCode );

        modifier.setSenderAddress( new HostAddress( InetAddress.getLocalHost() ) );
        EncKrbPrivPart privPart = modifier.getEncKrbPrivPart();

        // get the subsession key from the Authenticator
        EncryptionKey subSessionKey = authenticator.getSubSessionKey();

        EncryptedData encPrivPart;

        try
        {
            encPrivPart = cipherTextHandler.seal( subSessionKey, privPart, KeyUsage.NUMBER13 );
        }
        catch ( KerberosException ke )
        {
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_SOFTERROR, ke );
        }

        PrivateMessage privateMessage = new PrivateMessage( encPrivPart );

        // Begin AP_REP generation
        EncApRepPartModifier encApModifier = new EncApRepPartModifier();
        encApModifier.setClientTime( authenticator.getClientTime() );
        encApModifier.setClientMicroSecond( authenticator.getClientMicroSecond() );
        encApModifier.setSequenceNumber( new Integer( authenticator.getSequenceNumber() ) );
        encApModifier.setSubSessionKey( authenticator.getSubSessionKey() );

        EncApRepPart repPart = encApModifier.getEncApRepPart();

        EncryptedData encRepPart;

        try
        {
            encRepPart = cipherTextHandler.seal( ticket.getEncTicketPart().getSessionKey(), repPart, KeyUsage.NUMBER12 );
        }
        catch ( KerberosException ke )
        {
            throw new ChangePasswordException( ErrorType.KRB5_KPASSWD_SOFTERROR, ke );
        }

        ApplicationReply appReply = new ApplicationReply( encRepPart );

        // return status message value object
        ChangePasswordReplyModifier replyModifier = new ChangePasswordReplyModifier();
        replyModifier.setApplicationReply( appReply );
        replyModifier.setPrivateMessage( privateMessage );

        changepwContext.setReply( replyModifier.getChangePasswordReply() );
    }

    
    private static void monitorReply( ChangePasswordContext changepwContext ) throws KerberosException
    {
        try
        {
            ChangePasswordReply reply = ( ChangePasswordReply ) changepwContext.getReply();
            ApplicationReply appReply = reply.getApplicationReply();
            PrivateMessage priv = reply.getPrivateMessage();

            StringBuilder sb = new StringBuilder();
            sb.append( "Responding with change password reply:" );
            sb.append( "\n\t" + "appReply               " + appReply );
            sb.append( "\n\t" + "priv                   " + priv );

            LOG.debug( sb.toString() );
        }
        catch ( Exception e )
        {
            // This is a monitor.  No exceptions should bubble up.
            LOG.error( "Error in reply monitor", e );
        }
    }
}
