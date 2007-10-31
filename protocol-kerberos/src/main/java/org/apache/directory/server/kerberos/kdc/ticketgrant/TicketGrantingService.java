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


import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.kdc.KdcContext;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.shared.KerberosConstants;
import org.apache.directory.server.kerberos.shared.KerberosUtils;
import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumHandler;
import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.io.decoder.ApplicationRequestDecoder;
import org.apache.directory.server.kerberos.shared.messages.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.KdcReply;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.TicketGrantReply;
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPart;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPartModifier;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.AuthorizationData;
import org.apache.directory.server.kerberos.shared.messages.value.Checksum;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KdcOptions;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.LastRequest;
import org.apache.directory.server.kerberos.shared.messages.value.PaData;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;
import org.apache.directory.server.kerberos.shared.messages.value.flags.TicketFlag;
import org.apache.directory.server.kerberos.shared.messages.value.types.PaDataType;
import org.apache.directory.server.kerberos.shared.replay.InMemoryReplayCache;
import org.apache.directory.server.kerberos.shared.service.GetPrincipalStoreEntry;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 583938 $, $Date: 2007-10-11 21:57:20 +0200 (Thu, 11 Oct 2007) $
 */
public class TicketGrantingService
{
    
    /** the log for this class */
    private static final Logger LOG = LoggerFactory.getLogger( TicketGrantingService.class );
    
    private static final InMemoryReplayCache replayCache = new InMemoryReplayCache();
    private static final CipherTextHandler cipherTextHandler = new CipherTextHandler();

    private static final String CONTEXT_KEY = "context";
    
    private static final String SERVICE_NAME = "Ticket-Granting Service (TGS)";

    private static final ChecksumHandler checksumHandler = new ChecksumHandler();

    
    public static void execute( IoSession session, TicketGrantingContext tgsContext ) throws Exception
    {
        KdcContext kdcContext = ( KdcContext ) session.getAttribute( CONTEXT_KEY );           

        if ( LOG.isDebugEnabled() )
        {
            monitorRequest( kdcContext );
        }

        // This is really strange that we have to configure the replay cache for every single request !!!
        KdcServer config = tgsContext.getConfig();
        long clockSkew = config.getAllowableClockSkew();
        replayCache.setClockSkew( clockSkew );
        tgsContext.setReplayCache( replayCache );

        tgsContext.setCipherTextHandler( cipherTextHandler );

        if ( tgsContext.getRequest().getProtocolVersionNumber() != KerberosConstants.KERBEROS_V5 )
        {
            throw new KerberosException( ErrorType.KDC_ERR_BAD_PVNO );
        }

        selectEncryptionType( kdcContext, tgsContext, session );
        getAuthHeader( tgsContext, session );
        verifyTgt( tgsContext, session );
        getTicketPrincipalEntry( tgsContext, session );
        verifyBodyChecksum( tgsContext, session );
        getRequestPrincipalEntry( tgsContext, session );  
        generateTicket( tgsContext, session );
        buildReply( tgsContext, session );
        
        if ( LOG.isDebugEnabled() )
        {
            monitorContext( tgsContext );
            monitorReply( session );
        }
        
        sealReply( tgsContext, session );
    }
    

    private static void monitorRequest( KdcContext kdcContext ) throws Exception
    {
        KdcRequest request = kdcContext.getRequest();

        try
        {
            String clientAddress = kdcContext.getClientAddress().getHostAddress();

            StringBuffer sb = new StringBuffer();

            sb.append( "Received " + SERVICE_NAME + " request:" );
            sb.append( "\n\t" + "messageType:           " + request.getMessageType() );
            sb.append( "\n\t" + "protocolVersionNumber: " + request.getProtocolVersionNumber() );
            sb.append( "\n\t" + "clientAddress:         " + clientAddress );
            sb.append( "\n\t" + "nonce:                 " + request.getNonce() );
            sb.append( "\n\t" + "kdcOptions:            " + request.getKdcOptions() );
            sb.append( "\n\t" + "clientPrincipal:       " + request.getClientPrincipal() );
            sb.append( "\n\t" + "serverPrincipal:       " + request.getServerPrincipal() );
            sb.append( "\n\t" + "encryptionType:        " + KerberosUtils.getEncryptionTypesString( request.getEType() ) );
            sb.append( "\n\t" + "realm:                 " + request.getRealm() );
            sb.append( "\n\t" + "from time:             " + request.getFrom() );
            sb.append( "\n\t" + "till time:             " + request.getTill() );
            sb.append( "\n\t" + "renew-till time:       " + request.getRtime() );
            sb.append( "\n\t" + "hostAddresses:         " + request.getAddresses() );

            LOG.debug( sb.toString() );
        }
        catch ( Exception e )
        {
            // This is a monitor.  No exceptions should bubble up.
            LOG.error( "Error in request monitor", e );
        }
    }
    
    
    private static void selectEncryptionType( KdcContext kdcContext, TicketGrantingContext tgsContext, IoSession session ) throws Exception
    {
        KdcServer config = kdcContext.getConfig();

        Set<EncryptionType> requestedTypes = kdcContext.getRequest().getEType();

        EncryptionType bestType = KerberosUtils.getBestEncryptionType( requestedTypes, config.getEncryptionTypes() );

        LOG.debug( "Session will use encryption type {}.", bestType );

        if ( bestType == null )
        {
            throw new KerberosException( ErrorType.KDC_ERR_ETYPE_NOSUPP );
        }

        kdcContext.setEncryptionType( bestType );
    }
    
    
    private static void getAuthHeader( TicketGrantingContext tgsContext, IoSession session ) throws Exception
    {
        KdcRequest request = tgsContext.getRequest();

        ApplicationRequest authHeader = getAuthHeader( request );
        Ticket tgt = authHeader.getTicket();

        tgsContext.setAuthHeader( authHeader );
        tgsContext.setTgt( tgt );
    }
    
    
    public static void verifyTgt( TicketGrantingContext tgsContext, IoSession session ) throws KerberosException
    {
        KdcServer config = tgsContext.getConfig();
        Ticket tgt = tgsContext.getTgt();

        // Check primary realm.
        if ( !tgt.getRealm().equals( config.getPrimaryRealm() ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_NOT_US );
        }

        String tgtServerName = tgt.getServerPrincipal().getName();
        String requestServerName = tgsContext.getRequest().getServerPrincipal().getName();

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
    
    
    private static void getTicketPrincipalEntry( TicketGrantingContext tgsContext, IoSession session ) throws KerberosException
    {
        KerberosPrincipal principal = tgsContext.getTgt().getServerPrincipal();
        PrincipalStore store = tgsContext.getStore();

        PrincipalStoreEntry entry = KerberosUtils.getEntry( principal, store, ErrorType.KDC_ERR_S_PRINCIPAL_UNKNOWN );
        tgsContext.setTicketPrincipalEntry( entry );
    }

    
    
    private static void verifyBodyChecksum( TicketGrantingContext tgsContext, IoSession session ) throws KerberosException
    {
        KdcServer config = tgsContext.getConfig();

        if ( config.isBodyChecksumVerified() )
        {
            byte[] bodyBytes = tgsContext.getRequest().getBodyBytes();
            Checksum authenticatorChecksum = tgsContext.getAuthenticator().getChecksum();

            if ( authenticatorChecksum == null || authenticatorChecksum.getChecksumType() == null
                || authenticatorChecksum.getChecksumValue() == null || bodyBytes == null )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_INAPP_CKSUM );
            }

            LOG.debug( "Verifying body checksum type '{}'.", authenticatorChecksum.getChecksumType() );

            checksumHandler.verifyChecksum( authenticatorChecksum, bodyBytes, null, KeyUsage.NUMBER8 );
        }
    }
    

    public static void getRequestPrincipalEntry( TicketGrantingContext tgsContext, IoSession session ) throws KerberosException
    {
        KerberosPrincipal principal = tgsContext.getRequest().getServerPrincipal();
        PrincipalStore store = tgsContext.getStore();

        PrincipalStoreEntry entry = KerberosUtils.getEntry( principal, store, ErrorType.KDC_ERR_S_PRINCIPAL_UNKNOWN );
        tgsContext.setRequestPrincipalEntry( entry );
    }

    
    private static void generateTicket( TicketGrantingContext tgsContext, IoSession session ) throws KerberosException
    {
        KdcRequest request = tgsContext.getRequest();
        Ticket tgt = tgsContext.getTgt();
        Authenticator authenticator = tgsContext.getAuthenticator();
        CipherTextHandler cipherTextHandler = tgsContext.getCipherTextHandler();
        KerberosPrincipal ticketPrincipal = request.getServerPrincipal();

        EncryptionType encryptionType = tgsContext.getEncryptionType();
        EncryptionKey serverKey = tgsContext.getRequestPrincipalEntry().getKeyMap().get( encryptionType );

        KdcServer config = tgsContext.getConfig();

        EncTicketPartModifier newTicketBody = new EncTicketPartModifier();

        newTicketBody.setClientAddresses( tgt.getEncTicketPart().getClientAddresses() );

        processFlags( config, request, tgt, newTicketBody );

        EncryptionKey sessionKey = RandomKeyFactory.getRandomKey( tgsContext.getEncryptionType() );
        newTicketBody.setSessionKey( sessionKey );

        newTicketBody.setClientPrincipal( tgt.getEncTicketPart().getClientPrincipal() );

        if ( request.getEncAuthorizationData() != null )
        {
            AuthorizationData authData = ( AuthorizationData ) cipherTextHandler.unseal( AuthorizationData.class,
                authenticator.getSubSessionKey(), request.getEncAuthorizationData(), KeyUsage.NUMBER4 );
            authData.add( tgt.getEncTicketPart().getAuthorizationData() );
            newTicketBody.setAuthorizationData( authData );
        }

        processTransited( newTicketBody, tgt );

        processTimes( config, request, newTicketBody, tgt );

        EncTicketPart ticketPart = newTicketBody.getEncTicketPart();

        if ( request.getOption( KdcOptions.ENC_TKT_IN_SKEY ) )
        {
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
    
            throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
        }
        else
        {
            EncryptedData encryptedData = cipherTextHandler.seal( serverKey, ticketPart, KeyUsage.NUMBER2 );

            Ticket newTicket = new Ticket( ticketPrincipal, encryptedData );
            newTicket.setEncTicketPart( ticketPart );

            tgsContext.setNewTicket( newTicket );
        }
    }
    

    private static void buildReply( TicketGrantingContext tgsContext, IoSession sessione ) throws KerberosException
    {
        KdcRequest request = tgsContext.getRequest();
        Ticket tgt = tgsContext.getTgt();
        Ticket newTicket = tgsContext.getNewTicket();

        TicketGrantReply reply = new TicketGrantReply();
        reply.setClientPrincipal( tgt.getEncTicketPart().getClientPrincipal() );
        reply.setTicket( newTicket );
        reply.setKey( newTicket.getEncTicketPart().getSessionKey() );
        reply.setNonce( request.getNonce() );
        // TODO - resp.last-req := fetch_last_request_info(client); requires store
        reply.setLastRequest( new LastRequest() );
        reply.setFlags( newTicket.getEncTicketPart().getFlags() );
        reply.setClientAddresses( newTicket.getEncTicketPart().getClientAddresses() );
        reply.setAuthTime( newTicket.getEncTicketPart().getAuthTime() );
        reply.setStartTime( newTicket.getEncTicketPart().getStartTime() );
        reply.setEndTime( newTicket.getEncTicketPart().getEndTime() );
        reply.setServerPrincipal( newTicket.getServerPrincipal() );

        if ( newTicket.getEncTicketPart().getFlags().get( TicketFlags.RENEWABLE ) )
        {
            reply.setRenewTill( newTicket.getEncTicketPart().getRenewTill() );
        }

        tgsContext.setReply( reply );
    }
    
    
    private static void sealReply( TicketGrantingContext tgsContext, IoSession session ) throws KerberosException
    {
        TicketGrantReply reply = ( TicketGrantReply ) tgsContext.getReply();
        Ticket tgt = tgsContext.getTgt();
        CipherTextHandler cipherTextHandler = tgsContext.getCipherTextHandler();
        Authenticator authenticator = tgsContext.getAuthenticator();

        EncryptedData encryptedData;

        if ( authenticator.getSubSessionKey() != null )
        {
            encryptedData = cipherTextHandler.seal( authenticator.getSubSessionKey(), reply, KeyUsage.NUMBER9 );
        }
        else
        {
            encryptedData = cipherTextHandler.seal( tgt.getEncTicketPart().getSessionKey(), reply, KeyUsage.NUMBER8 );
        }

        reply.setEncPart( encryptedData );
    }
    
    
    
    private static void monitorContext( TicketGrantingContext tgsContext )
    {
        try
        {
            Ticket tgt = tgsContext.getTgt();
            long clockSkew = tgsContext.getConfig().getAllowableClockSkew();
            ChecksumType checksumType = tgsContext.getAuthenticator().getChecksum().getChecksumType();
            InetAddress clientAddress = tgsContext.getClientAddress();
            HostAddresses clientAddresses = tgt.getEncTicketPart().getClientAddresses();

            boolean caddrContainsSender = false;
            
            if ( tgt.getEncTicketPart().getClientAddresses() != null )
            {
                caddrContainsSender = tgt.getEncTicketPart().getClientAddresses().contains( new HostAddress( clientAddress ) );
            }

            StringBuffer sb = new StringBuffer();

            sb.append( "Monitoring " + SERVICE_NAME + " context:" );

            sb.append( "\n\t" + "clockSkew              " + clockSkew );
            sb.append( "\n\t" + "checksumType           " + checksumType );
            sb.append( "\n\t" + "clientAddress          " + clientAddress );
            sb.append( "\n\t" + "clientAddresses        " + clientAddresses );
            sb.append( "\n\t" + "caddr contains sender  " + caddrContainsSender );

            KerberosPrincipal requestServerPrincipal = tgsContext.getRequest().getServerPrincipal();
            PrincipalStoreEntry requestPrincipal = tgsContext.getRequestPrincipalEntry();

            sb.append( "\n\t" + "principal              " + requestServerPrincipal );
            sb.append( "\n\t" + "cn                     " + requestPrincipal.getCommonName() );
            sb.append( "\n\t" + "realm                  " + requestPrincipal.getRealmName() );
            sb.append( "\n\t" + "principal              " + requestPrincipal.getPrincipal() );
            sb.append( "\n\t" + "SAM type               " + requestPrincipal.getSamType() );

            KerberosPrincipal ticketServerPrincipal = tgsContext.getTgt().getServerPrincipal();
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

            LOG.debug( sb.toString() );
        }
        catch ( Exception e )
        {
            // This is a monitor.  No exceptions should bubble up.
            LOG.error( "Error in context monitor", e );
        }
    }

    
    private static void monitorReply( IoSession session )
    {
        KdcContext kdcContext = ( KdcContext ) session.getAttribute( CONTEXT_KEY );
        Object reply = kdcContext.getReply();

        if ( reply instanceof KdcReply )
        {
            KdcReply success = ( KdcReply ) reply;

            try
            {
                StringBuffer sb = new StringBuffer();

                sb.append( "Responding with " + SERVICE_NAME + " reply:" );
                sb.append( "\n\t" + "messageType:           " + success.getMessageType() );
                sb.append( "\n\t" + "protocolVersionNumber: " + success.getProtocolVersionNumber() );
                sb.append( "\n\t" + "nonce:                 " + success.getNonce() );
                sb.append( "\n\t" + "clientPrincipal:       " + success.getClientPrincipal() );
                sb.append( "\n\t" + "client realm:          " + success.getClientRealm() );
                sb.append( "\n\t" + "serverPrincipal:       " + success.getServerPrincipal() );
                sb.append( "\n\t" + "server realm:          " + success.getServerRealm() );
                sb.append( "\n\t" + "auth time:             " + success.getAuthTime() );
                sb.append( "\n\t" + "start time:            " + success.getStartTime() );
                sb.append( "\n\t" + "end time:              " + success.getEndTime() );
                sb.append( "\n\t" + "renew-till time:       " + success.getRenewTill() );
                sb.append( "\n\t" + "hostAddresses:         " + success.getClientAddresses() );

                LOG.debug( sb.toString() );
            }
            catch ( Exception e )
            {
                // This is a monitor.  No exceptions should bubble up.
                LOG.error( "Error in reply monitor", e );
            }
        }
    }
    

    
    private static void processFlags( KdcServer config, KdcRequest request, Ticket tgt,
        EncTicketPartModifier newTicketBody ) throws KerberosException
    {
        if ( tgt.getEncTicketPart().getFlags().get( TicketFlags.PRE_AUTHENT ) )
        {
            newTicketBody.setFlag( TicketFlag.PRE_AUTHENT.getOrdinal() );
        }

        if ( request.getOption( KdcOptions.FORWARDABLE ) )
        {
            if ( !config.isForwardableAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().get( TicketFlags.FORWARDABLE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            newTicketBody.setFlag( TicketFlag.FORWARDABLE.getOrdinal() );
        }

        if ( request.getOption( KdcOptions.FORWARDED ) )
        {
            if ( !config.isForwardableAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().get( TicketFlags.FORWARDABLE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            if ( request.getAddresses() != null && request.getAddresses().getAddresses() != null
                && request.getAddresses().getAddresses().length > 0 )
            {
                newTicketBody.setClientAddresses( request.getAddresses() );
            }
            else
            {
                if ( !config.isEmptyAddressesAllowed() )
                {
                    throw new KerberosException( ErrorType.KDC_ERR_POLICY );
                }
            }

            newTicketBody.setFlag( TicketFlag.FORWARDED.getOrdinal() );
        }

        if ( tgt.getEncTicketPart().getFlags().get( TicketFlags.FORWARDED ) )
        {
            newTicketBody.setFlag( TicketFlag.FORWARDED.getOrdinal() );
        }

        if ( request.getOption( KdcOptions.PROXIABLE ) )
        {
            if ( !config.isProxiableAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().get( TicketFlags.PROXIABLE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            newTicketBody.setFlag( TicketFlag.PROXIABLE.getOrdinal() );
        }

        if ( request.getOption( KdcOptions.PROXY ) )
        {
            if ( !config.isProxiableAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().get( TicketFlags.PROXIABLE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            if ( request.getAddresses() != null && request.getAddresses().getAddresses() != null
                && request.getAddresses().getAddresses().length > 0 )
            {
                newTicketBody.setClientAddresses( request.getAddresses() );
            }
            else
            {
                if ( !config.isEmptyAddressesAllowed() )
                {
                    throw new KerberosException( ErrorType.KDC_ERR_POLICY );
                }
            }

            newTicketBody.setFlag( TicketFlag.PROXY.getOrdinal() );
        }

        if ( request.getOption( KdcOptions.ALLOW_POSTDATE ) )
        {
            if ( !config.isPostdatedAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().get( TicketFlags.MAY_POSTDATE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            newTicketBody.setFlag( TicketFlag.MAY_POSTDATE.getOrdinal() );
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
        if ( request.getOption( KdcOptions.POSTDATED ) )
        {
            if ( !config.isPostdatedAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().get( TicketFlags.MAY_POSTDATE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            newTicketBody.setFlag( TicketFlag.POSTDATED.getOrdinal() );
            newTicketBody.setFlag( TicketFlag.INVALID.getOrdinal() );

            newTicketBody.setStartTime( request.getFrom() );
        }

        if ( request.getOption( KdcOptions.VALIDATE ) )
        {
            if ( !config.isPostdatedAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().get( TicketFlags.INVALID ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            KerberosTime startTime = ( tgt.getEncTicketPart().getStartTime() != null ) ? 
                    tgt.getEncTicketPart().getStartTime() : 
                        tgt.getEncTicketPart().getAuthTime();

            if ( startTime.greaterThan( new KerberosTime() ) )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_NYV );
            }

            echoTicket( newTicketBody, tgt );
            newTicketBody.clearFlag( TicketFlag.INVALID.getOrdinal() );
        }

        if ( request.getOption( KdcOptions.RESERVED ) )
        {
            throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
        }
    }


    private static void processTimes( KdcServer config, KdcRequest request, EncTicketPartModifier newTicketBody,
        Ticket tgt ) throws KerberosException
    {
        KerberosTime now = new KerberosTime();

        newTicketBody.setAuthTime( tgt.getEncTicketPart().getAuthTime() );

        KerberosTime startTime = request.getFrom();

        /*
         * "If the requested starttime is absent, indicates a time in the past,
         * or is within the window of acceptable clock skew for the KDC and the
         * POSTDATE option has not been specified, then the starttime of the
         * ticket is set to the authentication server's current time."
         */        
        if ( startTime == null || startTime.lessThan( now ) || startTime.isInClockSkew( config.getAllowableClockSkew() )
            && !request.getOption( KdcOptions.POSTDATED ) )
        {
            startTime = now;
        }

        /*
         * "If it indicates a time in the future beyond the acceptable clock skew,
         * but the POSTDATED option has not been specified or the MAY-POSTDATE flag
         * is not set in the TGT, then the error KDC_ERR_CANNOT_POSTDATE is
         * returned."
         */        
        if ( startTime != null && startTime.greaterThan( now )
            && !startTime.isInClockSkew( config.getAllowableClockSkew() )
            && ( !request.getOption( KdcOptions.POSTDATED ) || !tgt.getEncTicketPart().getFlags().get( TicketFlags.MAY_POSTDATE ) ) )
        {
            throw new KerberosException( ErrorType.KDC_ERR_CANNOT_POSTDATE );
        }

        KerberosTime renewalTime = null;
        KerberosTime kerberosEndTime = null;

        if ( request.getOption( KdcOptions.RENEW ) )
        {
            if ( !config.isRenewableAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( !tgt.getEncTicketPart().getFlags().get( TicketFlags.RENEWABLE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            if ( tgt.getEncTicketPart().getRenewTill().lessThan( now ) )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_EXPIRED );
            }

            echoTicket( newTicketBody, tgt );

            newTicketBody.setStartTime( now );

            KerberosTime tgtStartTime = ( tgt.getEncTicketPart().getStartTime() != null ) ? 
                tgt.getEncTicketPart().getStartTime() : 
                    tgt.getEncTicketPart().getAuthTime();

            long oldLife = tgt.getEncTicketPart().getEndTime().getTime() - tgtStartTime.getTime();

            kerberosEndTime = new KerberosTime( Math.min( tgt.getEncTicketPart().getRenewTill().getTime(), now.getTime() + oldLife ) );
            newTicketBody.setEndTime( kerberosEndTime );
        }
        else
        {
            if ( newTicketBody.getEncTicketPart().getStartTime() == null )
            {
                newTicketBody.setStartTime( now );
            }

            KerberosTime till;
            if ( request.getTill().isZero() )
            {
                till = KerberosTime.INFINITY;
            }
            else
            {
                till = request.getTill();
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

            newTicketBody.setEndTime( kerberosEndTime );

            if ( request.getOption( KdcOptions.RENEWABLE_OK ) && kerberosEndTime.lessThan( request.getTill() )
                && tgt.getEncTicketPart().getFlags().get( TicketFlags.RENEWABLE ) )
            {
                if ( !config.isRenewableAllowed() )
                {
                    throw new KerberosException( ErrorType.KDC_ERR_POLICY );
                }

                // We set the RENEWABLE option for later processing.                           
                request.setOption( KdcOptions.RENEWABLE );
                long rtime = Math.min( request.getTill().getTime(), tgt.getEncTicketPart().getRenewTill().getTime() );
                renewalTime = new KerberosTime( rtime );
            }
        }

        if ( renewalTime == null )
        {
            renewalTime = request.getRtime();
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

        if ( request.getOption( KdcOptions.RENEWABLE ) && tgt.getEncTicketPart().getFlags().get( TicketFlags.RENEWABLE ) )
        {
            if ( !config.isRenewableAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            newTicketBody.setFlag( TicketFlag.RENEWABLE.getOrdinal() );

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
            newTicketBody.setRenewTill( Collections.min( minimizer ) );
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
    private static void processTransited( EncTicketPartModifier newTicketBody, Ticket tgt )
    {
        // TODO - currently no transited support other than local
        newTicketBody.setTransitedEncoding( tgt.getEncTicketPart().getTransitedEncoding() );
    }

    
    private static void echoTicket( EncTicketPartModifier newTicketBody, Ticket tgt )
    {
        EncTicketPart encTicketpart = tgt.getEncTicketPart();
        newTicketBody.setAuthorizationData( encTicketpart.getAuthorizationData() );
        newTicketBody.setAuthTime( encTicketpart.getAuthTime() );
        newTicketBody.setClientAddresses( encTicketpart.getClientAddresses() );
        newTicketBody.setClientPrincipal( encTicketpart.getClientPrincipal() );
        newTicketBody.setEndTime( encTicketpart.getEndTime() );
        newTicketBody.setFlags( encTicketpart.getFlags() );
        newTicketBody.setRenewTill( encTicketpart.getRenewTill() );
        newTicketBody.setSessionKey( encTicketpart.getSessionKey() );
        newTicketBody.setTransitedEncoding( encTicketpart.getTransitedEncoding() );
    }
    
    private static ApplicationRequest getAuthHeader( KdcRequest request ) throws KerberosException, IOException
    {
        PaData[] preAuthData = request.getPreAuthData();

        if ( preAuthData == null || preAuthData.length < 1 )
        {
            throw new KerberosException( ErrorType.KDC_ERR_PADATA_TYPE_NOSUPP );
        }

        byte[] undecodedAuthHeader = null;

        for ( int ii = 0; ii < preAuthData.length; ii++ )
        {
            if ( preAuthData[ii].getPaDataType() == PaDataType.PA_TGS_REQ )
            {
                undecodedAuthHeader = preAuthData[ii].getPaDataValue();
            }
        }

        if ( undecodedAuthHeader == null )
        {
            throw new KerberosException( ErrorType.KDC_ERR_PADATA_TYPE_NOSUPP );
        }

        ApplicationRequestDecoder decoder = new ApplicationRequestDecoder();
        ApplicationRequest authHeader = decoder.decode( undecodedAuthHeader );

        return authHeader;
    }
    

    /**
     * Find the best encryption type, comparing the requested type with
     * configured types.
     */    
    protected static EncryptionType getBestEncryptionType( EncryptionType[] requestedTypes, EncryptionType[] configuredTypes )
    {
        for ( EncryptionType requestedType:requestedTypes )
        {
            for ( EncryptionType configuredType:configuredTypes )
            {
                if ( requestedType == configuredType )
                {
                    return configuredType;
                }
            }
        }

        return null;
    }
}
