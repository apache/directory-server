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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPart;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPartModifier;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.AuthorizationData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KdcOptions;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;
import org.apache.directory.server.kerberos.shared.service.LockBox;
import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;


public class GenerateTicket extends CommandBase
{
    public boolean execute( Context context ) throws Exception
    {
        TicketGrantingContext tgsContext = ( TicketGrantingContext ) context;

        KdcRequest request = tgsContext.getRequest();
        Ticket tgt = tgsContext.getTgt();
        Authenticator authenticator = tgsContext.getAuthenticator();
        LockBox lockBox = tgsContext.getLockBox();
        KerberosPrincipal ticketPrincipal = request.getServerPrincipal();
        EncryptionKey serverKey = tgsContext.getRequestPrincipalEntry().getEncryptionKey();
        KdcConfiguration config = tgsContext.getConfig();
        EncryptionKey sessionKey = tgsContext.getSessionKey();

        EncTicketPartModifier newTicketBody = new EncTicketPartModifier();

        newTicketBody.setClientAddresses( tgt.getClientAddresses() );

        processFlags( config, request, tgt, newTicketBody );

        newTicketBody.setSessionKey( sessionKey );
        newTicketBody.setClientPrincipal( tgt.getClientPrincipal() );

        if ( request.getEncAuthorizationData() != null )
        {
            AuthorizationData authData = ( AuthorizationData ) lockBox.unseal( AuthorizationData.class, authenticator
                .getSubSessionKey(), request.getEncAuthorizationData() );
            authData.add( tgt.getAuthorizationData() );
            newTicketBody.setAuthorizationData( authData );
        }

        processTransited( newTicketBody, tgt );

        processTimes( config, request, newTicketBody, tgt );

        EncTicketPart ticketPart = newTicketBody.getEncTicketPart();

        if ( request.getOption( KdcOptions.ENC_TKT_IN_SKEY ) )
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
            throw new KerberosException( ErrorType.KDC_ERR_SVC_UNAVAILABLE );
        }

        EncryptedData encryptedData = lockBox.seal( serverKey, ticketPart );

        Ticket newTicket = new Ticket( ticketPrincipal, encryptedData );
        newTicket.setEncTicketPart( ticketPart );

        tgsContext.setNewTicket( newTicket );

        return CONTINUE_CHAIN;
    }


    private void processFlags( KdcConfiguration config, KdcRequest request, Ticket tgt,
        EncTicketPartModifier newTicketBody ) throws KerberosException
    {
        if ( request.getOption( KdcOptions.FORWARDABLE ) )
        {
            if ( !tgt.getFlag( TicketFlags.FORWARDABLE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            newTicketBody.setFlag( TicketFlags.FORWARDABLE );
        }

        if ( request.getOption( KdcOptions.FORWARDED ) )
        {
            if ( !tgt.getFlag( TicketFlags.FORWARDABLE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }
            newTicketBody.setFlag( TicketFlags.FORWARDED );
            newTicketBody.setClientAddresses( request.getAddresses() );
        }

        if ( tgt.getFlag( TicketFlags.FORWARDED ) )
        {
            newTicketBody.setFlag( TicketFlags.FORWARDED );
        }

        if ( request.getOption( KdcOptions.PROXIABLE ) )
        {
            if ( !tgt.getFlag( TicketFlags.PROXIABLE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            newTicketBody.setFlag( TicketFlags.PROXIABLE );
        }

        if ( request.getOption( KdcOptions.PROXY ) )
        {
            if ( !tgt.getFlag( TicketFlags.PROXIABLE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            newTicketBody.setFlag( TicketFlags.PROXY );
            newTicketBody.setClientAddresses( request.getAddresses() );
        }

        if ( request.getOption( KdcOptions.ALLOW_POSTDATE ) )
        {
            if ( !tgt.getFlag( TicketFlags.MAY_POSTDATE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            newTicketBody.setFlag( TicketFlags.MAY_POSTDATE );
        }

        if ( request.getOption( KdcOptions.POSTDATED ) )
        {
            if ( !tgt.getFlag( TicketFlags.MAY_POSTDATE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            newTicketBody.setFlag( TicketFlags.POSTDATED );
            newTicketBody.setFlag( TicketFlags.INVALID );

            if ( !config.isPostdateAllowed() )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            newTicketBody.setStartTime( request.getFrom() );
        }

        if ( request.getOption( KdcOptions.VALIDATE ) )
        {
            if ( !tgt.getFlag( TicketFlags.INVALID ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            }

            if ( tgt.getStartTime().greaterThan( new KerberosTime() ) )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_NYV );
            }

            /*
             if (check_hot_list(tgt)) then
             error_out(KRB_AP_ERR_REPEAT);
             endif
             */

            echoTicket( newTicketBody, tgt );
            newTicketBody.clearFlag( TicketFlags.INVALID );
        }

        if ( request.getOption( KdcOptions.RESERVED ) || request.getOption( KdcOptions.RENEWABLE_OK ) )
        {
            throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
        }
    }


    private void processTimes( KdcConfiguration config, KdcRequest request, EncTicketPartModifier newTicketBody,
        Ticket tgt ) throws KerberosException
    {
        KerberosTime now = new KerberosTime();

        newTicketBody.setAuthTime( tgt.getAuthTime() );

        KerberosTime renewalTime = null;

        if ( request.getOption( KdcOptions.RENEW ) )
        {
            if ( !tgt.getFlag( TicketFlags.RENEWABLE ) )
            {
                throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
            }

            if ( tgt.getRenewTill().greaterThan( now ) )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_EXPIRED );
            }

            echoTicket( newTicketBody, tgt );

            newTicketBody.setStartTime( now );
            long oldLife = tgt.getEndTime().getTime() - tgt.getStartTime().getTime();
            newTicketBody.setEndTime( new KerberosTime( Math
                .min( tgt.getRenewTill().getTime(), now.getTime() + oldLife ) ) );
        }
        else
        {
            newTicketBody.setStartTime( now );
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
            /*
             new_tkt.starttime+client.max_life,
             new_tkt.starttime+server.max_life,
             */
            List minimizer = new ArrayList();
            minimizer.add( till );
            minimizer.add( new KerberosTime( now.getTime() + config.getMaximumTicketLifetime() ) );
            minimizer.add( tgt.getEndTime() );
            KerberosTime minTime = ( KerberosTime ) Collections.min( minimizer );
            newTicketBody.setEndTime( minTime );

            if ( request.getOption( KdcOptions.RENEWABLE_OK ) && minTime.lessThan( request.getTill() )
                && tgt.getFlag( TicketFlags.RENEWABLE ) )
            {
                // we set the RENEWABLE option for later processing                           
                request.setOption( KdcOptions.RENEWABLE );
                long rtime = Math.min( request.getTill().getTime(), tgt.getRenewTill().getTime() );
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

        if ( request.getOption( KdcOptions.RENEWABLE ) && tgt.getFlag( TicketFlags.RENEWABLE ) )
        {
            newTicketBody.setFlag( TicketFlags.RENEWABLE );

            /*
             new_tkt.starttime+client.max_rlife,
             new_tkt.starttime+server.max_rlife,
             */
            // TODO - client and server configurable; requires store
            List minimizer = new ArrayList();

            /*
             * 'rtime' KerberosTime is OPTIONAL
             */
            if ( rtime != null )
            {
                minimizer.add( rtime );
            }

            minimizer.add( new KerberosTime( now.getTime() + config.getMaximumRenewableLifetime() ) );
            minimizer.add( tgt.getRenewTill() );
            newTicketBody.setRenewTill( ( KerberosTime ) Collections.min( minimizer ) );
        }
    }


    /*
     if (realm_tgt_is_for(tgt) := tgt.realm) then
     // tgt issued by local realm
     new_tkt.transited := tgt.transited;
     else
     // was issued for this realm by some other realm
     if (tgt.transited.tr-type not supported) then
     error_out(KDC_ERR_TRTYPE_NOSUPP);
     endif
     new_tkt.transited := compress_transited(tgt.transited + tgt.realm)
     endif
     */
    private void processTransited( EncTicketPartModifier newTicketBody, Ticket tgt )
    {
        // TODO - currently no transited support other than local
        newTicketBody.setTransitedEncoding( tgt.getTransitedEncoding() );
    }


    protected void echoTicket( EncTicketPartModifier newTicketBody, Ticket tgt )
    {
        newTicketBody.setAuthorizationData( tgt.getAuthorizationData() );
        newTicketBody.setAuthTime( tgt.getAuthTime() );
        newTicketBody.setClientAddresses( tgt.getClientAddresses() );
        newTicketBody.setClientPrincipal( tgt.getClientPrincipal() );
        newTicketBody.setEndTime( tgt.getEndTime() );
        newTicketBody.setFlags( tgt.getFlags() );
        newTicketBody.setRenewTill( tgt.getRenewTill() );
        newTicketBody.setSessionKey( tgt.getSessionKey() );
        newTicketBody.setTransitedEncoding( tgt.getTransitedEncoding() );
    }
}
