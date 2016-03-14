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
package org.apache.directory.server.kerberos.kdc.ticketgrant;


import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.KerberosConfig;
import org.apache.directory.server.kerberos.kdc.KdcContext;
import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.KerberosUtils;
import org.apache.directory.shared.kerberos.codec.KerberosDecoder;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.LastReqType;
import org.apache.directory.shared.kerberos.codec.types.PaDataType;
import org.apache.directory.shared.kerberos.components.AuthorizationData;
import org.apache.directory.shared.kerberos.components.Checksum;
import org.apache.directory.shared.kerberos.components.EncKdcRepPart;
import org.apache.directory.shared.kerberos.components.EncTicketPart;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.HostAddresses;
import org.apache.directory.shared.kerberos.components.KdcReq;
import org.apache.directory.shared.kerberos.components.KdcReqBody;
import org.apache.directory.shared.kerberos.components.LastReq;
import org.apache.directory.shared.kerberos.components.LastReqEntry;
import org.apache.directory.shared.kerberos.components.PaData;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.crypto.checksum.ChecksumType;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.InvalidTicketException;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.flags.TicketFlag;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.apache.directory.shared.kerberos.messages.Authenticator;
import org.apache.directory.shared.kerberos.messages.EncTgsRepPart;
import org.apache.directory.shared.kerberos.messages.TgsRep;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class TicketGrantingService
{

    /** the log for this class */
    private static final Logger LOG_KRB = LoggerFactory.getLogger( Loggers.KERBEROS_LOG.getName() );

    private static final CipherTextHandler CIPHER_TEXT_HANDLER = new CipherTextHandler();

    private static final String SERVICE_NAME = "Ticket-Granting Service (TGS)";

    private static final ChecksumHandler CHRECKSUM_HANDLER = new ChecksumHandler();


    private TicketGrantingService()
    {
    }


    public static void execute( TicketGrantingContext tgsContext ) throws Exception
    {
        if ( LOG_KRB.isDebugEnabled() )
        {
            monitorRequest( tgsContext );
        }

        configureTicketGranting( tgsContext );
        selectEncryptionType( tgsContext );
        getAuthHeader( tgsContext );
        // commenting to allow cross-realm auth
        //verifyTgt( tgsContext );
        getTicketPrincipalEntry( tgsContext );
        verifyTgtAuthHeader( tgsContext );
        verifyBodyChecksum( tgsContext );
        getRequestPrincipalEntry( tgsContext );
        generateTicket( tgsContext );
        buildReply( tgsContext );
    }


    private static void configureTicketGranting( TicketGrantingContext tgsContext ) throws KerberosException
    {
        tgsContext.setCipherTextHandler( CIPHER_TEXT_HANDLER );

        if ( tgsContext.getRequest().getProtocolVersionNumber() != KerberosConstants.KERBEROS_V5 )
        {
            throw new KerberosException( ErrorType.KDC_ERR_BAD_PVNO );
        }
    }


    private static void monitorRequest( KdcContext kdcContext ) throws Exception
    {
        KdcReq request = kdcContext.getRequest();

        try
        {
            String clientAddress = kdcContext.getClientAddress().getHostAddress();

            StringBuffer sb = new StringBuffer();

            sb.append( "Received " + SERVICE_NAME + " request:" );
            sb.append( "\n\t" + "messageType:           " + request.getMessageType() );
            sb.append( "\n\t" + "protocolVersionNumber: " + request.getProtocolVersionNumber() );
            sb.append( "\n\t" + "clientAddress:         " + clientAddress );
            sb.append( "\n\t" + "nonce:                 " + request.getKdcReqBody().getNonce() );
            sb.append( "\n\t" + "kdcOptions:            " + request.getKdcReqBody().getKdcOptions() );
            sb.append( "\n\t" + "clientPrincipal:       " + request.getKdcReqBody().getCName() );
            sb.append( "\n\t" + "serverPrincipal:       " + request.getKdcReqBody().getSName() );
            sb.append( "\n\t" + "encryptionType:        "
                + KerberosUtils.getEncryptionTypesString( request.getKdcReqBody().getEType() ) );
            sb.append( "\n\t" + "realm:                 " + request.getKdcReqBody().getRealm() );
            sb.append( "\n\t" + "from time:             " + request.getKdcReqBody().getFrom() );
            sb.append( "\n\t" + "till time:             " + request.getKdcReqBody().getTill() );
            sb.append( "\n\t" + "renew-till time:       " + request.getKdcReqBody().getRTime() );
            sb.append( "\n\t" + "hostAddresses:         " + request.getKdcReqBody().getAddresses() );

            LOG_KRB.debug( sb.toString() );
        }
        catch ( Exception e )
        {
            // This is a monitor.  No exceptions should bubble up.
            LOG_KRB.error( I18n.err( I18n.ERR_153 ), e );
        }
    }


    private static void selectEncryptionType( TicketGrantingContext tgsContext ) throws Exception
    {
        KdcContext kdcContext = tgsContext;
        KerberosConfig config = kdcContext.getConfig();

        Set<EncryptionType> requestedTypes = kdcContext.getRequest().getKdcReqBody().getEType();

        EncryptionType bestType = KerberosUtils.getBestEncryptionType( requestedTypes, config.getEncryptionTypes() );

        LOG_KRB.debug( "Session will use encryption type {}.", bestType );

        if ( bestType == null )
        {
            throw new KerberosException( ErrorType.KDC_ERR_ETYPE_NOSUPP );
        }

        kdcContext.setEncryptionType( bestType );
    }


    private static void getAuthHeader( TicketGrantingContext tgsContext ) throws Exception
    {
        KdcReq request = tgsContext.getRequest();

        if ( ( request.getPaData() == null ) || ( request.getPaData().size() < 1 ) )
        {
            throw new KerberosException( ErrorType.KDC_ERR_PADATA_TYPE_NOSUPP );
        }

        byte[] undecodedAuthHeader = null;

        for ( PaData paData : request.getPaData() )
        {
            if ( paData.getPaDataType() == PaDataType.PA_TGS_REQ )
            {
                undecodedAuthHeader = paData.getPaDataValue();
            }
        }

        if ( undecodedAuthHeader == null )
        {
            throw new KerberosException( ErrorType.KDC_ERR_PADATA_TYPE_NOSUPP );
        }

        ApReq authHeader = KerberosDecoder.decodeApReq( undecodedAuthHeader );

        Ticket tgt = authHeader.getTicket();

        tgsContext.setAuthHeader( authHeader );
        tgsContext.setTgt( tgt );
    }


    public static void verifyTgt( TicketGrantingContext tgsContext ) throws KerberosException
    {
        KerberosConfig config = tgsContext.getConfig();
        Ticket tgt = tgsContext.getTgt();

        // Check primary realm.
        if ( !tgt.getRealm().equals( config.getPrimaryRealm() ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_NOT_US );
        }

        String tgtServerName = KerberosUtils.getKerberosPrincipal( tgt.getSName(), tgt.getRealm() ).getName();
        String requestServerName = KerberosUtils.getKerberosPrincipal(
            tgsContext.getRequest().getKdcReqBody().getSName(), tgsContext.getRequest().getKdcReqBody().getRealm() )
            .getName();

        /*
         * if (tgt.sname is not a TGT for local realm and is not req.sname)
         *     then error_out(KRB_AP_ERR_NOT_US);
         */
        if ( !tgtServerName.equals( config.getServicePrincipal().getName() )
            && !tgtServerName.equals( requestServerName ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_NOT_US );
        }
    }


    private static void getTicketPrincipalEntry( TicketGrantingContext tgsContext ) throws KerberosException
    {
        PrincipalName principal = tgsContext.getTgt().getSName();
        PrincipalStore store = tgsContext.getStore();

        KerberosPrincipal principalWithRealm = KerberosUtils.getKerberosPrincipal( principal, tgsContext.getTgt()
            .getRealm() );
        PrincipalStoreEntry entry = getEntry( principalWithRealm, store, ErrorType.KDC_ERR_S_PRINCIPAL_UNKNOWN );
        tgsContext.setTicketPrincipalEntry( entry );
    }


    private static void verifyTgtAuthHeader( TicketGrantingContext tgsContext ) throws KerberosException
    {
        ApReq authHeader = tgsContext.getAuthHeader();
        Ticket tgt = tgsContext.getTgt();

        KdcOptions kdcOptions = tgsContext.getRequest().getKdcReqBody().getKdcOptions();
        boolean isValidate = kdcOptions.get( KdcOptions.VALIDATE );

        EncryptionType encryptionType = tgt.getEncPart().getEType();
        EncryptionKey serverKey = tgsContext.getTicketPrincipalEntry().getKeyMap().get( encryptionType );

        long clockSkew = tgsContext.getConfig().getAllowableClockSkew();
        ReplayCache replayCache = tgsContext.getReplayCache();
        boolean emptyAddressesAllowed = tgsContext.getConfig().isEmptyAddressesAllowed();
        InetAddress clientAddress = tgsContext.getClientAddress();
        CipherTextHandler cipherTextHandler = tgsContext.getCipherTextHandler();

        Authenticator authenticator = KerberosUtils.verifyAuthHeader( authHeader, tgt, serverKey, clockSkew,
            replayCache,
            emptyAddressesAllowed, clientAddress, cipherTextHandler,
            KeyUsage.TGS_REQ_PA_TGS_REQ_PADATA_AP_REQ_TGS_SESS_KEY, isValidate );

        tgsContext.setAuthenticator( authenticator );
    }


    /**
     * RFC4120
     * <li>Section 3.3.2. Receipt of KRB_TGS_REQ Message -> 2nd paragraph
     * <li>Section 5.5.1. KRB_AP_REQ Definition -> Authenticator -> cksum
     */
    private static void verifyBodyChecksum( TicketGrantingContext tgsContext ) throws KerberosException
    {
        KerberosConfig config = tgsContext.getConfig();

        if ( config.isBodyChecksumVerified() )
        {
            KdcReqBody body = tgsContext.getRequest().getKdcReqBody();
            // FIXME how this byte[] is computed??
            // is it full ASN.1 encoded bytes OR just the bytes of all the values alone?
            // for now am using the ASN.1 encoded value
            ByteBuffer buf = ByteBuffer.allocate( body.computeLength() );
            try
            {
                body.encode( buf );
            }
            catch ( EncoderException e )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_INAPP_CKSUM );
            }

            byte[] bodyBytes = buf.array();
            Checksum authenticatorChecksum = tgsContext.getAuthenticator().getCksum();

            if ( authenticatorChecksum != null )
            {
                // we need the session key
                Ticket tgt = tgsContext.getTgt();
                EncTicketPart encTicketPart = tgt.getEncTicketPart();
                EncryptionKey sessionKey = encTicketPart.getKey();

                if ( authenticatorChecksum == null || authenticatorChecksum.getChecksumType() == null
                    || authenticatorChecksum.getChecksumValue() == null || bodyBytes == null )
                {
                    throw new KerberosException( ErrorType.KRB_AP_ERR_INAPP_CKSUM );
                }

                LOG_KRB.debug( "Verifying body checksum type '{}'.", authenticatorChecksum.getChecksumType() );

                CHRECKSUM_HANDLER.verifyChecksum( authenticatorChecksum, bodyBytes, sessionKey.getKeyValue(),
                    KeyUsage.TGS_REQ_PA_TGS_REQ_PADATA_AP_REQ_AUTHNT_CKSUM_TGS_SESS_KEY );
            }
        }
    }


    public static void getRequestPrincipalEntry( TicketGrantingContext tgsContext ) throws KerberosException
    {
        KerberosPrincipal principal = KerberosUtils.getKerberosPrincipal(
            tgsContext.getRequest().getKdcReqBody().getSName(), tgsContext.getRequest().getKdcReqBody().getRealm() );
        PrincipalStore store = tgsContext.getStore();

        PrincipalStoreEntry entry = getEntry( principal, store, ErrorType.KDC_ERR_S_PRINCIPAL_UNKNOWN );
        tgsContext.setRequestPrincipalEntry( entry );
    }


    private static void generateTicket( TicketGrantingContext tgsContext ) throws KerberosException,
        InvalidTicketException
    {
        KdcReq request = tgsContext.getRequest();
        Ticket tgt = tgsContext.getTgt();
        Authenticator authenticator = tgsContext.getAuthenticator();
        CipherTextHandler cipherTextHandler = tgsContext.getCipherTextHandler();
        KerberosPrincipal ticketPrincipal = KerberosUtils.getKerberosPrincipal(
            request.getKdcReqBody().getSName(), request.getKdcReqBody().getRealm() );

        EncryptionType encryptionType = tgsContext.getEncryptionType();
        EncryptionKey serverKey = tgsContext.getRequestPrincipalEntry().getKeyMap().get( encryptionType );

        KerberosConfig config = tgsContext.getConfig();

        tgsContext.getRequest().getKdcReqBody().getAdditionalTickets();

        EncTicketPart newTicketPart = new EncTicketPart();

        newTicketPart.setClientAddresses( tgt.getEncTicketPart().getClientAddresses() );

        processFlags( config, request, tgt, newTicketPart );

        EncryptionKey sessionKey = RandomKeyFactory.getRandomKey( tgsContext.getEncryptionType() );
        newTicketPart.setKey( sessionKey );

        newTicketPart.setCName( tgt.getEncTicketPart().getCName() );
        newTicketPart.setCRealm( tgt.getEncTicketPart().getCRealm() );

        if ( request.getKdcReqBody().getEncAuthorizationData() != null )
        {
            byte[] authorizationData = cipherTextHandler.decrypt( authenticator.getSubKey(), request.getKdcReqBody()
                .getEncAuthorizationData(), KeyUsage.TGS_REQ_KDC_REQ_BODY_AUTHZ_DATA_ENC_WITH_TGS_SESS_KEY );
            AuthorizationData authData = KerberosDecoder.decodeAuthorizationData( authorizationData );
            authData.addEntry( tgt.getEncTicketPart().getAuthorizationData().getCurrentAD() );
            newTicketPart.setAuthorizationData( authData );
        }

        processTransited( newTicketPart, tgt );

        processTimes( config, request, newTicketPart, tgt );

        if ( request.getKdcReqBody().getKdcOptions().get( KdcOptions.ENC_TKT_IN_SKEY ) )
        {
            Ticket[] additionalTkts = tgsContext.getRequest().getKdcReqBody().getAdditionalTickets();

            if ( additionalTkts == null || additionalTkts.length == 0 )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            Ticket additionalTgt = additionalTkts[0];
            // reject if it is not a TGT
            if ( !additionalTgt.getEncTicketPart().getFlags().isInitial() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            serverKey = additionalTgt.getEncTicketPart().getKey();
            /*
             * if (server not specified) then
             *         server = req.second_ticket.client;
             * endif
             * 
             * if ((req.second_ticket is not a TGT) or
             *     (req.second_ticket.client != server)) then
             *         error_out(KDC_ERR_POLICY);
             * endif
             * 
             * new_tkt.enc-part := encrypt OCTET STRING using etype_for_key(second-ticket.key), second-ticket.key;
             */
            //throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
        }

        EncryptedData encryptedData = cipherTextHandler.seal( serverKey, newTicketPart,
            KeyUsage.AS_OR_TGS_REP_TICKET_WITH_SRVKEY );

        Ticket newTicket = new Ticket( request.getKdcReqBody().getSName(), encryptedData );
        newTicket.setEncTicketPart( newTicketPart );
        newTicket.setRealm( request.getKdcReqBody().getRealm() );

        tgsContext.setNewTicket( newTicket );
    }


    private static void buildReply( TicketGrantingContext tgsContext ) throws KerberosException
    {
        KdcReq request = tgsContext.getRequest();
        Ticket tgt = tgsContext.getTgt();
        Ticket newTicket = tgsContext.getNewTicket();

        TgsRep reply = new TgsRep();

        reply.setCName( tgt.getEncTicketPart().getCName() );
        reply.setCRealm( tgt.getEncTicketPart().getCRealm() );
        reply.setTicket( newTicket );

        EncKdcRepPart encKdcRepPart = new EncKdcRepPart();

        encKdcRepPart.setKey( newTicket.getEncTicketPart().getKey() );
        encKdcRepPart.setNonce( request.getKdcReqBody().getNonce() );
        // TODO - resp.last-req := fetch_last_request_info(client); requires store
        // FIXME temporary fix, IMO we should create some new ATs to store this info in DIT
        LastReq lastReq = new LastReq();
        lastReq.addEntry( new LastReqEntry( LastReqType.TIME_OF_INITIAL_REQ, new KerberosTime() ) );
        encKdcRepPart.setLastReq( lastReq );

        encKdcRepPart.setFlags( newTicket.getEncTicketPart().getFlags() );
        encKdcRepPart.setClientAddresses( newTicket.getEncTicketPart().getClientAddresses() );
        encKdcRepPart.setAuthTime( newTicket.getEncTicketPart().getAuthTime() );
        encKdcRepPart.setStartTime( newTicket.getEncTicketPart().getStartTime() );
        encKdcRepPart.setEndTime( newTicket.getEncTicketPart().getEndTime() );
        encKdcRepPart.setSName( newTicket.getSName() );
        encKdcRepPart.setSRealm( newTicket.getRealm() );

        if ( newTicket.getEncTicketPart().getFlags().isRenewable() )
        {
            encKdcRepPart.setRenewTill( newTicket.getEncTicketPart().getRenewTill() );
        }

        if ( LOG_KRB.isDebugEnabled() )
        {
            monitorContext( tgsContext );
            monitorReply( reply, encKdcRepPart );
        }

        EncTgsRepPart encTgsRepPart = new EncTgsRepPart();
        encTgsRepPart.setEncKdcRepPart( encKdcRepPart );

        Authenticator authenticator = tgsContext.getAuthenticator();

        EncryptedData encryptedData;

        if ( authenticator.getSubKey() != null )
        {
            encryptedData = CIPHER_TEXT_HANDLER.seal( authenticator.getSubKey(), encTgsRepPart,
                KeyUsage.TGS_REP_ENC_PART_TGS_AUTHNT_SUB_KEY );
        }
        else
        {
            encryptedData = CIPHER_TEXT_HANDLER.seal( tgt.getEncTicketPart().getKey(), encTgsRepPart,
                KeyUsage.TGS_REP_ENC_PART_TGS_SESS_KEY );
        }

        reply.setEncPart( encryptedData );
        reply.setEncKdcRepPart( encKdcRepPart );

        tgsContext.setReply( reply );
    }


    private static void monitorContext( TicketGrantingContext tgsContext )
    {
        try
        {
            Ticket tgt = tgsContext.getTgt();
            long clockSkew = tgsContext.getConfig().getAllowableClockSkew();

            Checksum cksum = tgsContext.getAuthenticator().getCksum();

            ChecksumType checksumType = null;
            if ( cksum != null )
            {
                checksumType = cksum.getChecksumType();
            }

            InetAddress clientAddress = tgsContext.getClientAddress();
            HostAddresses clientAddresses = tgt.getEncTicketPart().getClientAddresses();

            boolean caddrContainsSender = false;
            if ( tgt.getEncTicketPart().getClientAddresses() != null )
            {
                caddrContainsSender = tgt.getEncTicketPart().getClientAddresses()
                    .contains( new HostAddress( clientAddress ) );
            }

            StringBuffer sb = new StringBuffer();

            sb.append( "Monitoring " + SERVICE_NAME + " context:" );

            sb.append( "\n\t" + "clockSkew              " + clockSkew );
            sb.append( "\n\t" + "checksumType           " + checksumType );
            sb.append( "\n\t" + "clientAddress          " + clientAddress );
            sb.append( "\n\t" + "clientAddresses        " + clientAddresses );
            sb.append( "\n\t" + "caddr contains sender  " + caddrContainsSender );

            PrincipalName requestServerPrincipal = tgsContext.getRequest().getKdcReqBody().getSName();
            PrincipalStoreEntry requestPrincipal = tgsContext.getRequestPrincipalEntry();

            sb.append( "\n\t" + "principal              " + requestServerPrincipal );
            sb.append( "\n\t" + "cn                     " + requestPrincipal.getCommonName() );
            sb.append( "\n\t" + "realm                  " + requestPrincipal.getRealmName() );
            sb.append( "\n\t" + "principal              " + requestPrincipal.getPrincipal() );
            sb.append( "\n\t" + "SAM type               " + requestPrincipal.getSamType() );

            PrincipalName ticketServerPrincipal = tgsContext.getTgt().getSName();
            PrincipalStoreEntry ticketPrincipal = tgsContext.getTicketPrincipalEntry();

            sb.append( "\n\t" + "principal              " + ticketServerPrincipal );
            sb.append( "\n\t" + "cn                     " + ticketPrincipal.getCommonName() );
            sb.append( "\n\t" + "realm                  " + ticketPrincipal.getRealmName() );
            sb.append( "\n\t" + "principal              " + ticketPrincipal.getPrincipal() );
            sb.append( "\n\t" + "SAM type               " + ticketPrincipal.getSamType() );

            EncryptionType encryptionType = tgsContext.getTgt().getEncPart().getEType();
            int keyVersion = ticketPrincipal.getKeyMap().get( encryptionType ).getKeyVersion();
            sb.append( "\n\t" + "Ticket key type        " + encryptionType );
            sb.append( "\n\t" + "Service key version    " + keyVersion );

            LOG_KRB.debug( sb.toString() );
        }
        catch ( Exception e )
        {
            // This is a monitor.  No exceptions should bubble up.
            LOG_KRB.error( I18n.err( I18n.ERR_154 ), e );
        }
    }


    private static void monitorReply( TgsRep success, EncKdcRepPart part )
    {
        try
        {
            StringBuffer sb = new StringBuffer();

            sb.append( "Responding with " + SERVICE_NAME + " reply:" );
            sb.append( "\n\t" + "messageType:           " + success.getMessageType() );
            sb.append( "\n\t" + "protocolVersionNumber: " + success.getProtocolVersionNumber() );
            sb.append( "\n\t" + "nonce:                 " + part.getNonce() );
            sb.append( "\n\t" + "clientPrincipal:       " + success.getCName() );
            sb.append( "\n\t" + "client realm:          " + success.getCRealm() );
            sb.append( "\n\t" + "serverPrincipal:       " + part.getSName() );
            sb.append( "\n\t" + "server realm:          " + part.getSRealm() );
            sb.append( "\n\t" + "auth time:             " + part.getAuthTime() );
            sb.append( "\n\t" + "start time:            " + part.getStartTime() );
            sb.append( "\n\t" + "end time:              " + part.getEndTime() );
            sb.append( "\n\t" + "renew-till time:       " + part.getRenewTill() );
            sb.append( "\n\t" + "hostAddresses:         " + part.getClientAddresses() );

            LOG_KRB.debug( sb.toString() );
        }
        catch ( Exception e )
        {
            // This is a monitor.  No exceptions should bubble up.
            LOG_KRB.error( I18n.err( I18n.ERR_155 ), e );
        }
    }


    private static void processFlags( KerberosConfig config, KdcReq request, Ticket tgt,
        EncTicketPart newTicketPart ) throws KerberosException
    {
        if ( tgt.getEncTicketPart().getFlags().isPreAuth() )
        {
            newTicketPart.setFlag( TicketFlag.PRE_AUTHENT );
        }

        if ( request.getKdcReqBody().getKdcOptions().get( KdcOptions.FORWARDABLE ) )
        {
            if ( !config.isForwardableAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().isForwardable() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            newTicketPart.setFlag( TicketFlag.FORWARDABLE );
        }

        if ( request.getKdcReqBody().getKdcOptions().get( KdcOptions.FORWARDED ) )
        {
            if ( !config.isForwardableAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().isForwardable() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            if ( request.getKdcReqBody().getAddresses() != null
                && request.getKdcReqBody().getAddresses().getAddresses() != null
                && request.getKdcReqBody().getAddresses().getAddresses().length > 0 )
            {
                newTicketPart.setClientAddresses( request.getKdcReqBody().getAddresses() );
            }
            else
            {
                if ( !config.isEmptyAddressesAllowed() )
                {
                    throw new KerberosException( ErrorType.KDC_ERR_POLICY );
                }
            }

            newTicketPart.setFlag( TicketFlag.FORWARDED );
        }

        if ( tgt.getEncTicketPart().getFlags().isForwarded() )
        {
            newTicketPart.setFlag( TicketFlag.FORWARDED );
        }

        if ( request.getKdcReqBody().getKdcOptions().get( KdcOptions.PROXIABLE ) )
        {
            if ( !config.isProxiableAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().isProxiable() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            newTicketPart.setFlag( TicketFlag.PROXIABLE );
        }

        if ( request.getKdcReqBody().getKdcOptions().get( KdcOptions.PROXY ) )
        {
            if ( !config.isProxiableAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().isProxiable() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            if ( request.getKdcReqBody().getAddresses() != null
                && request.getKdcReqBody().getAddresses().getAddresses() != null
                && request.getKdcReqBody().getAddresses().getAddresses().length > 0 )
            {
                newTicketPart.setClientAddresses( request.getKdcReqBody().getAddresses() );
            }
            else
            {
                if ( !config.isEmptyAddressesAllowed() )
                {
                    throw new KerberosException( ErrorType.KDC_ERR_POLICY );
                }
            }

            newTicketPart.setFlag( TicketFlag.PROXY );
        }

        if ( request.getKdcReqBody().getKdcOptions().get( KdcOptions.ALLOW_POSTDATE ) )
        {
            if ( !config.isPostdatedAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().isMayPosdate() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            newTicketPart.setFlag( TicketFlag.MAY_POSTDATE );
        }

        /*
         * "Otherwise, if the TGT has the MAY-POSTDATE flag set, then the resulting
         * ticket will be postdated, and the requested starttime is checked against
         * the policy of the local realm.  If acceptable, the ticket's starttime is
         * set as requested, and the INVALID flag is set.  The postdated ticket MUST
         * be validated before use by presenting it to the KDC after the starttime
         * has been reached.  However, in no case may the starttime, endtime, or
         * renew-till time of a newly-issued postdated ticket extend beyond the
         * renew-till time of the TGT."
         */
        if ( request.getKdcReqBody().getKdcOptions().get( KdcOptions.POSTDATED ) )
        {
            if ( !config.isPostdatedAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().isMayPosdate() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            newTicketPart.setFlag( TicketFlag.POSTDATED );
            newTicketPart.setFlag( TicketFlag.INVALID );

            newTicketPart.setStartTime( request.getKdcReqBody().getFrom() );
        }

        if ( request.getKdcReqBody().getKdcOptions().get( KdcOptions.VALIDATE ) )
        {
            if ( !config.isPostdatedAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().isInvalid() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            KerberosTime startTime = ( tgt.getEncTicketPart().getStartTime() != null )
                ? tgt.getEncTicketPart().getStartTime()
                : tgt.getEncTicketPart().getAuthTime();

            if ( startTime.greaterThan( new KerberosTime() ) )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_NYV );
            }

            echoTicket( newTicketPart, tgt );
            newTicketPart.getFlags().clearFlag( TicketFlag.INVALID );
        }

        if ( request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_0 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_7 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_9 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_10 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_11 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_12 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_13 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_14 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_15 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_16 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_17 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_18 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_19 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_20 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_21 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_22 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_23 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_24 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_25 )
            || request.getKdcReqBody().getKdcOptions().get( KdcOptions.RESERVED_29 ) )
        {
            throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
        }
    }


    private static void processTimes( KerberosConfig config, KdcReq request, EncTicketPart newTicketPart,
        Ticket tgt ) throws KerberosException
    {
        KerberosTime now = new KerberosTime();

        newTicketPart.setAuthTime( tgt.getEncTicketPart().getAuthTime() );

        KerberosTime startTime = request.getKdcReqBody().getFrom();

        /*
         * "If the requested starttime is absent, indicates a time in the past,
         * or is within the window of acceptable clock skew for the KDC and the
         * POSTDATE option has not been specified, then the starttime of the
         * ticket is set to the authentication server's current time."
         */
        if ( startTime == null || startTime.lessThan( now ) || startTime.isInClockSkew( config.getAllowableClockSkew() )
            && !request.getKdcReqBody().getKdcOptions().get( KdcOptions.POSTDATED ) )
        {
            startTime = now;
        }

        /*
         * "If it indicates a time in the future beyond the acceptable clock skew,
         * but the POSTDATED option has not been specified or the MAY-POSTDATE flag
         * is not set in the TGT, then the error KDC_ERR_CANNOT_POSTDATE is
         * returned."
         */
        if ( startTime != null
            && startTime.greaterThan( now )
            && !startTime.isInClockSkew( config.getAllowableClockSkew() )
            && ( !request.getKdcReqBody().getKdcOptions().get( KdcOptions.POSTDATED ) || !tgt.getEncTicketPart()
                .getFlags().isMayPosdate() ) )
        {
            throw new KerberosException( ErrorType.KDC_ERR_CANNOT_POSTDATE );
        }

        KerberosTime renewalTime = null;
        KerberosTime kerberosEndTime = null;

        if ( request.getKdcReqBody().getKdcOptions().get( KdcOptions.RENEW ) )
        {
            if ( !config.isRenewableAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().isRenewable() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            if ( tgt.getEncTicketPart().getRenewTill().lessThan( now ) )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_EXPIRED );
            }

            echoTicket( newTicketPart, tgt );

            newTicketPart.setStartTime( now );

            KerberosTime tgtStartTime = ( tgt.getEncTicketPart().getStartTime() != null )
                    ? tgt.getEncTicketPart().getStartTime()
                    : tgt.getEncTicketPart().getAuthTime();

            long oldLife = tgt.getEncTicketPart().getEndTime().getTime() - tgtStartTime.getTime();

            kerberosEndTime = new KerberosTime( Math.min( tgt.getEncTicketPart().getRenewTill().getTime(),
                now.getTime() + oldLife ) );
            newTicketPart.setEndTime( kerberosEndTime );
        }
        else
        {
            if ( newTicketPart.getStartTime() == null )
            {
                newTicketPart.setStartTime( now );
            }

            KerberosTime till;
            if ( request.getKdcReqBody().getTill().isZero() )
            {
                till = KerberosTime.INFINITY;
            }
            else
            {
                till = request.getKdcReqBody().getTill();
            }

            /*
             * The end time is the minimum of (a) the requested till time or (b)
             * the start time plus maximum lifetime as configured in policy or (c)
             * the end time of the TGT.
             */
            List<KerberosTime> minimizer = new ArrayList<KerberosTime>();
            minimizer.add( till );
            minimizer.add( new KerberosTime( startTime.getTime() + config.getMaximumTicketLifetime() ) );
            minimizer.add( tgt.getEncTicketPart().getEndTime() );
            kerberosEndTime = Collections.min( minimizer );

            newTicketPart.setEndTime( kerberosEndTime );

            if ( request.getKdcReqBody().getKdcOptions().get( KdcOptions.RENEWABLE_OK )
                && kerberosEndTime.lessThan( request.getKdcReqBody().getTill() )
                && tgt.getEncTicketPart().getFlags().isRenewable() )
            {
                if ( !config.isRenewableAllowed() )
                {
                    throw new KerberosException( ErrorType.KDC_ERR_POLICY );
                }

                // We set the RENEWABLE option for later processing.
                request.getKdcReqBody().getKdcOptions().set( KdcOptions.RENEWABLE );
                long rtime = Math.min( request.getKdcReqBody().getTill().getTime(), tgt.getEncTicketPart()
                    .getRenewTill().getTime() );
                renewalTime = new KerberosTime( rtime );
            }
        }

        if ( renewalTime == null )
        {
            renewalTime = request.getKdcReqBody().getRTime();
        }

        KerberosTime rtime;
        if ( renewalTime != null && renewalTime.isZero() )
        {
            rtime = KerberosTime.INFINITY;
        }
        else
        {
            rtime = renewalTime;
        }

        if ( request.getKdcReqBody().getKdcOptions().get( KdcOptions.RENEWABLE )
            && tgt.getEncTicketPart().getFlags().isRenewable() )
        {
            if ( !config.isRenewableAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            newTicketPart.setFlag( TicketFlag.RENEWABLE );

            /*
             * The renew-till time is the minimum of (a) the requested renew-till
             * time or (b) the start time plus maximum renewable lifetime as
             * configured in policy or (c) the renew-till time of the TGT.
             */
            List<KerberosTime> minimizer = new ArrayList<KerberosTime>();

            /*
             * 'rtime' KerberosTime is OPTIONAL
             */
            if ( rtime != null )
            {
                minimizer.add( rtime );
            }

            minimizer.add( new KerberosTime( startTime.getTime() + config.getMaximumRenewableLifetime() ) );
            minimizer.add( tgt.getEncTicketPart().getRenewTill() );
            newTicketPart.setRenewTill( Collections.min( minimizer ) );
        }

        /*
         * "If the requested expiration time minus the starttime (as determined
         * above) is less than a site-determined minimum lifetime, an error
         * message with code KDC_ERR_NEVER_VALID is returned."
         */
        if ( kerberosEndTime.lessThan( startTime ) )
        {
            throw new KerberosException( ErrorType.KDC_ERR_NEVER_VALID );
        }

        long ticketLifeTime = Math.abs( startTime.getTime() - kerberosEndTime.getTime() );
        if ( ticketLifeTime < config.getAllowableClockSkew() )
        {
            throw new KerberosException( ErrorType.KDC_ERR_NEVER_VALID );
        }
    }


    /*
     * if (realm_tgt_is_for(tgt) := tgt.realm) then
     *         // tgt issued by local realm
     *         new_tkt.transited := tgt.transited;
     * else
     *         // was issued for this realm by some other realm
     *         if (tgt.transited.tr-type not supported) then
     *                 error_out(KDC_ERR_TRTYPE_NOSUPP);
     *         endif
     * 
     *         new_tkt.transited := compress_transited(tgt.transited + tgt.realm)
     * endif
     */
    private static void processTransited( EncTicketPart newTicketPart, Ticket tgt )
    {
        // TODO - currently no transited support other than local
        newTicketPart.setTransited( tgt.getEncTicketPart().getTransited() );
    }


    private static void echoTicket( EncTicketPart newTicketPart, Ticket tgt )
    {
        EncTicketPart encTicketpart = tgt.getEncTicketPart();
        newTicketPart.setAuthorizationData( encTicketpart.getAuthorizationData() );
        newTicketPart.setAuthTime( encTicketpart.getAuthTime() );
        newTicketPart.setClientAddresses( encTicketpart.getClientAddresses() );
        newTicketPart.setCName( encTicketpart.getCName() );
        newTicketPart.setEndTime( encTicketpart.getEndTime() );
        newTicketPart.setFlags( encTicketpart.getFlags() );
        newTicketPart.setRenewTill( encTicketpart.getRenewTill() );
        newTicketPart.setKey( encTicketpart.getKey() );
        newTicketPart.setTransited( encTicketpart.getTransited() );
    }


    /**
     * Get a PrincipalStoreEntry given a principal.  The ErrorType is used to indicate
     * whether any resulting error pertains to a server or client.
     *
     * @param principal
     * @param store
     * @param errorType
     * @return The PrincipalStoreEntry
     * @throws Exception
     */
    public static PrincipalStoreEntry getEntry( KerberosPrincipal principal, PrincipalStore store, ErrorType errorType )
        throws KerberosException
    {
        PrincipalStoreEntry entry = null;

        try
        {
            entry = store.getPrincipal( principal );
        }
        catch ( Exception e )
        {
            throw new KerberosException( errorType, e );
        }

        if ( entry == null )
        {
            throw new KerberosException( errorType );
        }

        if ( entry.getKeyMap() == null || entry.getKeyMap().isEmpty() )
        {
            throw new KerberosException( ErrorType.KDC_ERR_NULL_KEY );
        }

        return entry;
    }

}
