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
package org.apache.directory.server.kerberos.shared.service;


import java.net.InetAddress;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPart;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.ApOptions;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.mina.handler.chain.IoHandlerCommand;


/**
 * Shared by TGS and Changepw.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class VerifyAuthHeader implements IoHandlerCommand
{
    private String contextKey = "context";


    /**
     * Verifies an AuthHeader using guidelines from RFC 1510 section A.10., "KRB_AP_REQ verification."
     *
     * @param authHeader
     * @param ticket
     * @param serverKey
     * @param clockSkew
     * @param replayCache
     * @param emptyAddressesAllowed
     * @param clientAddress
     * @param lockBox
     * @param authenticatorKeyUsage
     * @param isValidate
     * @return The authenticator.
     * @throws KerberosException
     */
    public Authenticator verifyAuthHeader( ApplicationRequest authHeader, Ticket ticket, EncryptionKey serverKey,
        long clockSkew, ReplayCache replayCache, boolean emptyAddressesAllowed, InetAddress clientAddress,
        CipherTextHandler lockBox, KeyUsage authenticatorKeyUsage, boolean isValidate ) throws KerberosException
    {
        if ( authHeader.getProtocolVersionNumber() != 5 )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BADVERSION );
        }

        if ( authHeader.getMessageType() != MessageType.KRB_AP_REQ )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_MSG_TYPE );
        }

        if ( authHeader.getTicket().getVersionNumber() != 5 )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BADVERSION );
        }

        EncryptionKey ticketKey = null;

        if ( authHeader.getOption( ApOptions.USE_SESSION_KEY ) )
        {
            ticketKey = authHeader.getTicket().getSessionKey();
        }
        else
        {
            ticketKey = serverKey;
        }

        if ( ticketKey == null )
        {
            // TODO - check server key version number, skvno; requires store
            if ( false )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_BADKEYVER );
            }

            throw new KerberosException( ErrorType.KRB_AP_ERR_NOKEY );
        }

        EncTicketPart encPart = ( EncTicketPart ) lockBox.unseal( EncTicketPart.class, ticketKey, ticket.getEncPart(),
            KeyUsage.NUMBER2 );
        ticket.setEncTicketPart( encPart );

        Authenticator authenticator = ( Authenticator ) lockBox.unseal( Authenticator.class, ticket.getSessionKey(),
            authHeader.getEncPart(), authenticatorKeyUsage );

        if ( !authenticator.getClientPrincipal().getName().equals( ticket.getClientPrincipal().getName() ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BADMATCH );
        }

        if ( ticket.getClientAddresses() != null )
        {
            if ( !ticket.getClientAddresses().contains( new HostAddress( clientAddress ) ) )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_BADADDR );
            }
        }
        else
        {
            if ( !emptyAddressesAllowed )
            {
                throw new KerberosException( ErrorType.KRB_AP_ERR_BADADDR );
            }
        }

        KerberosPrincipal serverPrincipal = ticket.getServerPrincipal();
        KerberosPrincipal clientPrincipal = authenticator.getClientPrincipal();
        KerberosTime clientTime = authenticator.getClientTime();
        int clientMicroSeconds = authenticator.getClientMicroSecond();

        if ( replayCache.isReplay( serverPrincipal, clientPrincipal, clientTime, clientMicroSeconds ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_REPEAT );
        }

        replayCache.save( serverPrincipal, clientPrincipal, clientTime, clientMicroSeconds );

        if ( !authenticator.getClientTime().isInClockSkew( clockSkew ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_SKEW );
        }

        /*
         * "The server computes the age of the ticket: local (server) time minus
         * the starttime inside the Ticket.  If the starttime is later than the
         * current time by more than the allowable clock skew, or if the INVALID
         * flag is set in the ticket, the KRB_AP_ERR_TKT_NYV error is returned."
         */
        KerberosTime startTime = ( ticket.getStartTime() != null ) ? ticket.getStartTime() : ticket.getAuthTime();

        KerberosTime now = new KerberosTime();
        boolean isValidStartTime = startTime.lessThan( now );

        if ( !isValidStartTime || ( ticket.getFlag( TicketFlags.INVALID ) && !isValidate ) )
        {
            // it hasn't yet become valid
            throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_NYV );
        }

        // TODO - doesn't take into account skew
        if ( !ticket.getEndTime().greaterThan( now ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_EXPIRED );
        }

        authHeader.setOption( ApOptions.MUTUAL_REQUIRED );

        return authenticator;
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
