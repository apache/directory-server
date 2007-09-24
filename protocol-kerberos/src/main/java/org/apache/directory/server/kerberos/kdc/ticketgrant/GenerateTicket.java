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


import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPart;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.AuthorizationData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.flags.KdcOption;
import org.apache.directory.server.kerberos.shared.messages.value.flags.TicketFlag;
import org.apache.directory.server.kerberos.shared.messages.value.flags.TicketFlags;
import org.apache.directory.server.kerberos.shared.messages.value.types.KerberosErrorType;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GenerateTicket implements IoHandlerCommand
{
    private String contextKey = "context";


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        TicketGrantingContext tgsContext = ( TicketGrantingContext ) session.getAttribute( getContextKey() );

        KdcRequest request = tgsContext.getRequest();
        Ticket tgt = tgsContext.getTgt();
        Authenticator authenticator = tgsContext.getAuthenticator();
        CipherTextHandler cipherTextHandler = tgsContext.getCipherTextHandler();
        KerberosPrincipal ticketPrincipal = request.getServerPrincipal();

        EncryptionType encryptionType = tgsContext.getEncryptionType();
        EncryptionKey serverKey = tgsContext.getRequestPrincipalEntry().getKeyMap().get( encryptionType );

        KdcConfiguration config = tgsContext.getConfig();

        EncTicketPart ticketPart = new EncTicketPart();

        ticketPart.setClientAddresses( tgt.getClientAddresses() );

        processFlags( config, request, tgt, ticketPart );

        EncryptionKey sessionKey = RandomKeyFactory.getRandomKey( tgsContext.getEncryptionType() );
        ticketPart.setSessionKey( sessionKey );

        ticketPart.setClientPrincipal( tgt.getClientPrincipal() );

        if ( request.getEncAuthorizationData() != null )
        {
            AuthorizationData authData = ( AuthorizationData ) cipherTextHandler.unseal( AuthorizationData.class,
                authenticator.getSubSessionKey(), request.getEncAuthorizationData(), KeyUsage.NUMBER4 );
            authData.add( tgt.getAuthorizationData() );
            ticketPart.setAuthorizationData( authData );
        }

        processTransited( ticketPart, tgt );

        processTimes( config, request, ticketPart, tgt );

        if ( request.getOption( KdcOption.ENC_TKT_IN_SKEY ) )
        {
            /*
             if (server not specified) then
             server = req.second_ticket.client;
             endif
             if ((req.second_ticket is not a TGT) or
             (req.second_ticket.client != server)) then
             error_out(KDC_ERR_POLICY);
             endif
             new_tkt.enc-part := encrypt OCTET STRING
             using etype_for_key(second-ticket.key), second-ticket.key;
             */
            throw new KerberosException( KerberosErrorType.KDC_ERR_SVC_UNAVAILABLE );
        }

        EncryptedData encryptedData = cipherTextHandler.seal( serverKey, ticketPart, KeyUsage.NUMBER2 );

        Ticket newTicket = new Ticket( ticketPrincipal, encryptedData );
        newTicket.setEncTicketPart( ticketPart );

        tgsContext.setNewTicket( newTicket );

        next.execute( session, message );
    }


    private void processFlags( KdcConfiguration config, KdcRequest request, Ticket tgt,
        EncTicketPart ticketPart ) throws KerberosException
    {
    	TicketFlags tgtFlags = tgt.getFlags();

        if ( tgtFlags.isFlagSet( TicketFlag.PRE_AUTHENT ) )
        {
            ticketPart.setFlag( TicketFlag.PRE_AUTHENT );
        }

        if ( request.getOption( KdcOption.FORWARDABLE ) )
        {
            if ( !tgtFlags.isForwardable() )
            {
                throw new KerberosException( KerberosErrorType.KDC_ERR_BADOPTION );
            }

            ticketPart.setFlag( TicketFlag.FORWARDABLE );
        }

        if ( request.getOption( KdcOption.FORWARDED ) )
        {
            if ( !tgtFlags.isForwardable() )
            {
                throw new KerberosException( KerberosErrorType.KDC_ERR_BADOPTION );
            }
            
            ticketPart.setFlag( TicketFlag.FORWARDED );
            ticketPart.setClientAddresses( request.getAddresses() );
        }

        if ( tgtFlags.isForwarded() )
        {
            ticketPart.setFlag( TicketFlag.FORWARDED );
        }

        if ( request.getOption( KdcOption.PROXIABLE ) )
        {
            if ( !tgtFlags.isProxiable() )
            {
                throw new KerberosException( KerberosErrorType.KDC_ERR_BADOPTION );
            }

            ticketPart.setFlag( TicketFlag.PROXIABLE );
        }

        if ( request.getOption( KdcOption.PROXY ) )
        {
            if ( !tgtFlags.isProxiable() )
            {
                throw new KerberosException( KerberosErrorType.KDC_ERR_BADOPTION );
            }

            ticketPart.setFlag( TicketFlag.PROXY );
            ticketPart.setClientAddresses( request.getAddresses() );
        }

        if ( request.getOption( KdcOption.ALLOW_POSTDATE ) )
        {
            if ( !tgtFlags.isMayPosdate() )
            {
                throw new KerberosException( KerberosErrorType.KDC_ERR_BADOPTION );
            }

            ticketPart.setFlag( TicketFlag.MAY_POSTDATE );
        }

        if ( request.getOption( KdcOption.POSTDATED ) )
        {
            if ( !tgtFlags.isMayPosdate() )
            {
                throw new KerberosException( KerberosErrorType.KDC_ERR_BADOPTION );
            }

            ticketPart.setFlag( TicketFlag.POSTDATED );
            ticketPart.setFlag( TicketFlag.INVALID );

            if ( !config.isPostdateAllowed() )
            {
                throw new KerberosException( KerberosErrorType.KDC_ERR_POLICY );
            }

            ticketPart.setStartTime( request.getFrom() );
        }

        if ( request.getOption( KdcOption.VALIDATE ) )
        {
            if ( !tgtFlags.isInvalid() )
            {
                throw new KerberosException( KerberosErrorType.KDC_ERR_POLICY );
            }

            if ( tgt.getStartTime().greaterThan( new KerberosTime() ) )
            {
                throw new KerberosException( KerberosErrorType.KRB_AP_ERR_TKT_NYV );
            }

            echoTicket( ticketPart, tgt );
            ticketPart.clearFlag( TicketFlag.INVALID );
        }

        if ( request.getOption( KdcOption.RESERVED ) || request.getOption( KdcOption.RENEWABLE_OK ) )
        {
            throw new KerberosException( KerberosErrorType.KDC_ERR_BADOPTION );
        }
    }


    private void processTimes( KdcConfiguration config, KdcRequest request, EncTicketPart ticketPart,
        Ticket tgt ) throws KerberosException
    {
        KerberosTime now = new KerberosTime();

        ticketPart.setAuthTime( tgt.getAuthTime() );

        KerberosTime renewalTime = null;

        if ( request.getOption( KdcOption.RENEW ) )
        {
            if ( !tgt.getFlags().isRenewable() )
            {
                throw new KerberosException( KerberosErrorType.KDC_ERR_BADOPTION );
            }

            if ( tgt.getRenewTill().greaterThan( now ) )
            {
                throw new KerberosException( KerberosErrorType.KRB_AP_ERR_TKT_EXPIRED );
            }

            echoTicket( ticketPart, tgt );

            ticketPart.setStartTime( now );
            long oldLife = tgt.getEndTime().getTime() - tgt.getStartTime().getTime();
            ticketPart.setEndTime( new KerberosTime( Math
                .min( tgt.getRenewTill().getTime(), now.getTime() + oldLife ) ) );
        }
        else
        {
            ticketPart.setStartTime( now );
            KerberosTime till;
            
            if ( request.getTill().isZero() )
            {
                till = KerberosTime.INFINITY;
            }
            else
            {
                till = request.getTill();
            }

            // TODO - config; requires store
            List<KerberosTime> minimizer = new ArrayList<KerberosTime>();
            minimizer.add( till );
            minimizer.add( new KerberosTime( now.getTime() + config.getMaximumTicketLifetime() ) );
            minimizer.add( tgt.getEndTime() );
            KerberosTime minTime = Collections.min( minimizer );
            ticketPart.setEndTime( minTime );

            if ( request.getOption( KdcOption.RENEWABLE_OK ) && minTime.lessThan( request.getTill() )
                && tgt.getFlags().isRenewable() )
            {
                // we set the RENEWABLE option for later processing                           
                request.setOption( KdcOption.RENEWABLE );
                long rtime = Math.min( request.getTill().getTime(), tgt.getRenewTill().getTime() );
                renewalTime = new KerberosTime( rtime );
            }
        }

        if ( renewalTime == null )
        {
            renewalTime = request.getRenewtime();
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

        if ( request.getOption( KdcOption.RENEWABLE ) && ( tgt.getFlags().isRenewable() ) )
        {
            ticketPart.setFlag( TicketFlag.RENEWABLE );

            // TODO - client and server configurable; requires store
            List<KerberosTime> minimizer = new ArrayList<KerberosTime>();

            /*
             * 'rtime' KerberosTime is OPTIONAL
             */
            if ( rtime != null )
            {
                minimizer.add( rtime );
            }

            minimizer.add( new KerberosTime( now.getTime() + config.getMaximumRenewableLifetime() ) );
            minimizer.add( tgt.getRenewTill() );
            ticketPart.setRenewTill( Collections.min( minimizer ) );
        }
    }


    private void processTransited( EncTicketPart ticketPart, Ticket tgt )
    {
        // TODO - currently no transited support other than local
        ticketPart.setTransitedEncoding( tgt.getTransitedEncoding() );
    }


    protected void echoTicket( EncTicketPart ticketPart, Ticket tgt ) 
    {
        ticketPart.setAuthorizationData( tgt.getAuthorizationData() );
        ticketPart.setAuthTime( tgt.getAuthTime() );
        ticketPart.setClientAddresses( tgt.getClientAddresses() );
        
        try
        {
            ticketPart.setClientPrincipal( tgt.getClientPrincipal() );
        }
        catch ( ParseException pe )
        {
            // Do nothing
        }
        
        ticketPart.setEndTime( tgt.getEndTime() );
        ticketPart.setFlags( tgt.getFlags() );
        ticketPart.setRenewTill( tgt.getRenewTill() );
        ticketPart.setSessionKey( tgt.getSessionKey() );
        ticketPart.setTransitedEncoding( tgt.getTransitedEncoding() );
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
