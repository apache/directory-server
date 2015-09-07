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
package org.apache.directory.server.kerberos.changepwd.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.ChangePasswordConfig;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswdErrorType;
import org.apache.directory.server.kerberos.changepwd.exceptions.ChangePasswordException;
import org.apache.directory.server.kerberos.changepwd.messages.AbstractPasswordMessage;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordReply;
import org.apache.directory.server.kerberos.changepwd.messages.ChangePasswordRequest;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.shared.kerberos.KerberosUtils;
import org.apache.directory.shared.kerberos.codec.KerberosDecoder;
import org.apache.directory.shared.kerberos.codec.changePwdData.ChangePasswdDataContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.EncKrbPrivPart;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.HostAddresses;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.messages.ApRep;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.apache.directory.shared.kerberos.messages.Authenticator;
import org.apache.directory.shared.kerberos.messages.ChangePasswdData;
import org.apache.directory.shared.kerberos.messages.EncApRepPart;
import org.apache.directory.shared.kerberos.messages.KrbPriv;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChangePasswordService
{
    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ChangePasswordService.class );

    private static final CipherTextHandler CIPHER_TEXT_HANDLER = new CipherTextHandler();


    private ChangePasswordService()
    {
    }


    public static void execute( IoSession session, ChangePasswordContext changepwContext ) throws Exception
    {
        if ( LOG.isDebugEnabled() )
        {
            monitorRequest( changepwContext );
        }
        
        configureChangePassword( changepwContext );
        getAuthHeader( changepwContext );
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
        String newPassword = Strings.utf8ToString( changepwContext.getPasswordData().getNewPasswd() );
        KerberosPrincipal byPrincipal = KerberosUtils.getKerberosPrincipal( 
            authenticator.getCName(),
            authenticator.getCRealm() );

        KerberosPrincipal targetPrincipal = null;

        PrincipalName targName = changepwContext.getPasswordData().getTargName();
        
        if ( targName != null )
        {
            targetPrincipal = new KerberosPrincipal( targName.getNameString(), PrincipalNameType.KRB_NT_PRINCIPAL.getValue() );
        }
        else
        {
            targetPrincipal = byPrincipal;
        }
        
        // usec and seq-number must be present per MS but aren't in legacy kpasswd
        // seq-number must have same value as authenticator
        // ignore r-address

        store.changePassword( byPrincipal, targetPrincipal, newPassword, changepwContext.getTicket().getEncTicketPart().getFlags().isInitial() );
        LOG.debug( "Successfully modified password for {} BY {}.", targetPrincipal, byPrincipal );
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
            LOG.error( I18n.err( I18n.ERR_152 ), e );
        }
    }
    
    
    private static void configureChangePassword( ChangePasswordContext changepwContext )
    {
        changepwContext.setCipherTextHandler( CIPHER_TEXT_HANDLER );
    }
    
    
    private static void getAuthHeader( ChangePasswordContext changepwContext ) throws KerberosException
    {
        ChangePasswordRequest request = ( ChangePasswordRequest ) changepwContext.getRequest();

        short pvno = request.getVersionNumber();
        
        if ( ( pvno != AbstractPasswordMessage.PVNO ) && ( pvno != AbstractPasswordMessage.OLD_PVNO ) )
        {
            throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_BAD_VERSION );
        }

        if ( request.getAuthHeader() == null || request.getAuthHeader().getTicket() == null )
        {
            throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_AUTHERROR );
        }

        ApReq authHeader = request.getAuthHeader();
        Ticket ticket = authHeader.getTicket();

        changepwContext.setAuthHeader( authHeader );
        changepwContext.setTicket( ticket );
    }
    
    
    private static void verifyServiceTicket( ChangePasswordContext changepwContext ) throws KerberosException
    {
        ChangePasswordConfig config = changepwContext.getConfig();
        Ticket ticket = changepwContext.getTicket();
        String primaryRealm = config.getPrimaryRealm();
        KerberosPrincipal changepwPrincipal = config.getServicePrincipal();
        KerberosPrincipal serverPrincipal = KerberosUtils.getKerberosPrincipal( ticket.getSName(), ticket.getRealm() ); 

        // for some reason kpassword is setting the pricnipaltype value as 1 for ticket.getSName()
        // hence changing to string based comparison for server and changepw principals
        // instead of serverPrincipal.equals( changepwPrincipal )
        if ( !ticket.getRealm().equals( primaryRealm ) || !serverPrincipal.getName().equals( changepwPrincipal.getName() ) )
        {
            throw new KerberosException( org.apache.directory.shared.kerberos.exceptions.ErrorType.KRB_AP_ERR_NOT_US );
        }
    }
    
    
    private static void getServerEntry( ChangePasswordContext changepwContext ) throws KerberosException
    {
        Ticket ticket = changepwContext.getTicket();
        KerberosPrincipal principal =  KerberosUtils.getKerberosPrincipal( ticket.getSName(), ticket.getRealm() );
        PrincipalStore store = changepwContext.getStore();

        changepwContext.setServerEntry( KerberosUtils.getEntry( principal, store, ErrorType.KDC_ERR_S_PRINCIPAL_UNKNOWN ) );
    }
    
    
    private static void verifyServiceTicketAuthHeader( ChangePasswordContext changepwContext ) throws KerberosException
    {
        ApReq authHeader = changepwContext.getAuthHeader();
        Ticket ticket = changepwContext.getTicket();

        EncryptionType encryptionType = ticket.getEncPart().getEType();
        EncryptionKey serverKey = changepwContext.getServerEntry().getKeyMap().get( encryptionType );

        long clockSkew = changepwContext.getConfig().getAllowableClockSkew();
        ReplayCache replayCache = changepwContext.getReplayCache();
        boolean emptyAddressesAllowed = changepwContext.getConfig().isEmptyAddressesAllowed();
        InetAddress clientAddress = changepwContext.getClientAddress();
        CipherTextHandler cipherTextHandler = changepwContext.getCipherTextHandler();

        Authenticator authenticator = KerberosUtils.verifyAuthHeader( authHeader, ticket, serverKey, clockSkew, replayCache,
            emptyAddressesAllowed, clientAddress, cipherTextHandler, KeyUsage.AP_REQ_AUTHNT_SESS_KEY, false );

        changepwContext.setAuthenticator( authenticator );
    }
    
    
    private static void extractPassword( ChangePasswordContext changepwContext ) throws Exception
    {
        ChangePasswordRequest request = ( ChangePasswordRequest ) changepwContext.getRequest();
        Authenticator authenticator = changepwContext.getAuthenticator();
        CipherTextHandler cipherTextHandler = changepwContext.getCipherTextHandler();

        // get the subsession key from the Authenticator
        EncryptionKey subSessionKey = authenticator.getSubKey();

        // decrypt the request's private message with the subsession key
        EncryptedData encReqPrivPart = request.getPrivateMessage().getEncPart();

        ChangePasswdData passwordData = null;
        
        try
        {
            byte[] decryptedData = cipherTextHandler.decrypt( subSessionKey, encReqPrivPart, KeyUsage.KRB_PRIV_ENC_PART_CHOSEN_KEY );
            EncKrbPrivPart privatePart = KerberosDecoder.decodeEncKrbPrivPart( decryptedData );

            if ( authenticator.getSeqNumber() != privatePart.getSeqNumber() )
            {
                throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_MALFORMED );    
            }
            
            if ( request.getVersionNumber() == AbstractPasswordMessage.OLD_PVNO )
            {
                passwordData = new ChangePasswdData();
                passwordData.setNewPasswd( privatePart.getUserData() );
            }
            else
            {
                Asn1Decoder passwordDecoder = new Asn1Decoder();
                ByteBuffer stream = ByteBuffer.wrap( privatePart.getUserData() );
                ChangePasswdDataContainer container = new ChangePasswdDataContainer( stream );
                passwordDecoder.decode( stream, container );
                passwordData = container.getChngPwdData();
            }
        }
        catch ( KerberosException ke )
        {
            throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_SOFTERROR, ke );
        }

        changepwContext.setChngPwdData( passwordData );
    }

    
    private static void monitorContext( ChangePasswordContext changepwContext ) throws KerberosException
    {
        try
        {
            PrincipalStore store = changepwContext.getStore();
            ApReq authHeader = changepwContext.getAuthHeader();
            Ticket ticket = changepwContext.getTicket();
            ReplayCache replayCache = changepwContext.getReplayCache();
            long clockSkew = changepwContext.getConfig().getAllowableClockSkew();

            Authenticator authenticator = changepwContext.getAuthenticator();
            KerberosPrincipal clientPrincipal = KerberosUtils.getKerberosPrincipal( 
                authenticator.getCName(), authenticator.getCRealm() );

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
            sb.append( "\n\t" + "ChangePasswdData        " + changepwContext.getPasswordData() );
            sb.append( "\n\t" + "clientAddress          " + clientAddress );
            sb.append( "\n\t" + "clientAddresses        " + clientAddresses );
            sb.append( "\n\t" + "caddr contains sender  " + caddrContainsSender );
            sb.append( "\n\t" + "Ticket principal       " + ticket.getSName() );

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
            LOG.error( I18n.err( I18n.ERR_154 ), e );
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
        EncKrbPrivPart privPart = new EncKrbPrivPart();
        // first two bytes are the result code, rest is the string 'Password Changed' followed by a null char
        byte[] resultCode =
            { ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x50, ( byte ) 0x61, ( byte ) 0x73, ( byte ) 0x73, ( byte ) 0x77,
                ( byte ) 0x6F, ( byte ) 0x72, ( byte ) 0x64, ( byte ) 0x20, ( byte ) 0x63, ( byte ) 0x68,
                ( byte ) 0x61, ( byte ) 0x6E, ( byte ) 0x67, ( byte ) 0x65, ( byte ) 0x64, ( byte ) 0x00 };
        privPart.setUserData( resultCode );

        privPart.setSenderAddress( new HostAddress( InetAddress.getLocalHost() ) );

        // get the subsession key from the Authenticator
        EncryptionKey subSessionKey = authenticator.getSubKey();

        EncryptedData encPrivPart;

        try
        {
            encPrivPart = cipherTextHandler.seal( subSessionKey, privPart, KeyUsage.KRB_PRIV_ENC_PART_CHOSEN_KEY );
        }
        catch ( KerberosException ke )
        {
            throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_SOFTERROR, ke );
        }

        KrbPriv privateMessage = new KrbPriv();
        privateMessage.setEncPart( encPrivPart );

        // Begin AP_REP generation
        EncApRepPart repPart = new EncApRepPart();
        repPart.setCTime( authenticator.getCtime() );
        repPart.setCusec( authenticator.getCusec() );
        
        if ( authenticator.getSeqNumber() != null )
        {
            repPart.setSeqNumber( authenticator.getSeqNumber() );
        }
        
        repPart.setSubkey( subSessionKey );

        EncryptedData encRepPart;

        try
        {
            encRepPart = cipherTextHandler.seal( ticket.getEncTicketPart().getKey(), repPart, KeyUsage.AP_REP_ENC_PART_SESS_KEY );
        }
        catch ( KerberosException ke )
        {
            throw new ChangePasswordException( ChangePasswdErrorType.KRB5_KPASSWD_SOFTERROR, ke );
        }

        ApRep appReply = new ApRep();
        appReply.setEncPart( encRepPart );

        // return status message value object, the version number 
        changepwContext.setReply( new ChangePasswordReply( AbstractPasswordMessage.OLD_PVNO, appReply, privateMessage ) );
    }

    
    private static void monitorReply( ChangePasswordContext changepwContext ) throws KerberosException
    {
        try
        {
            ChangePasswordReply reply = ( ChangePasswordReply ) changepwContext.getReply();
            ApRep appReply = reply.getApplicationReply();
            KrbPriv priv = reply.getPrivateMessage();

            StringBuilder sb = new StringBuilder();
            sb.append( "Responding with change password reply:" );
            sb.append( "\n\t" + "appReply               " + appReply );
            sb.append( "\n\t" + "priv                   " + priv );

            LOG.debug( sb.toString() );
        }
        catch ( Exception e )
        {
            // This is a monitor.  No exceptions should bubble up.
            LOG.error( I18n.err( I18n.ERR_155 ), e );
        }
    }
}
