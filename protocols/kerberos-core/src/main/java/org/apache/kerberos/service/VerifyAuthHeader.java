/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.kerberos.service;

import java.net.InetAddress;

import org.apache.kerberos.exceptions.ErrorType;
import org.apache.kerberos.exceptions.KerberosException;
import org.apache.kerberos.messages.ApplicationRequest;
import org.apache.kerberos.messages.MessageType;
import org.apache.kerberos.messages.components.Authenticator;
import org.apache.kerberos.messages.components.EncTicketPart;
import org.apache.kerberos.messages.components.Ticket;
import org.apache.kerberos.messages.value.ApOptions;
import org.apache.kerberos.messages.value.EncryptionKey;
import org.apache.kerberos.messages.value.HostAddress;
import org.apache.kerberos.messages.value.KerberosTime;
import org.apache.kerberos.messages.value.TicketFlags;
import org.apache.kerberos.replay.ReplayCache;
import org.apache.protocol.common.chain.impl.CommandBase;

/*
 * Shared by TGS and Changepw
 */
public abstract class VerifyAuthHeader extends CommandBase
{
    // RFC 1510 A.10.  KRB_AP_REQ verification
    public Authenticator verifyAuthHeader( ApplicationRequest authHeader, Ticket ticket, EncryptionKey serverKey,
            long clockSkew, ReplayCache replayCache, boolean emptyAddressesAllowed, InetAddress clientAddress, LockBox lockBox )
            throws KerberosException
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

        EncTicketPart encPart = (EncTicketPart) lockBox.unseal( EncTicketPart.class, ticketKey, ticket.getEncPart() );
        ticket.setEncTicketPart( encPart );

        Authenticator authenticator = (Authenticator) lockBox.unseal( Authenticator.class, ticket.getSessionKey(), authHeader.getEncPart() );

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

        if ( replayCache.isReplay( authenticator.getClientTime(), authenticator.getClientPrincipal() ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_REPEAT );
        }

        replayCache.save( authenticator.getClientTime(), authenticator.getClientPrincipal() );

        if ( !authenticator.getClientTime().isInClockSkew( clockSkew ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_SKEW );
        }

        if ( ticket.getStartTime() != null && !ticket.getStartTime().isInClockSkew( clockSkew )
                || ticket.getFlag( TicketFlags.INVALID ) )
        {
            // it hasn't yet become valid
            throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_NYV );
        }

        // TODO - doesn't take into account skew
        if ( !ticket.getEndTime().greaterThan( new KerberosTime() ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_TKT_EXPIRED );
        }

        authHeader.setOption( ApOptions.MUTUAL_REQUIRED );

        return authenticator;
    }
}
