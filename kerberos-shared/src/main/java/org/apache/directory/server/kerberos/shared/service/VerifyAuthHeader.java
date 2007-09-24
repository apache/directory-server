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

import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.application.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPart;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.flags.ApOption;
import org.apache.directory.server.kerberos.shared.messages.value.types.KerberosErrorType;
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
     * @return The authenticator.
     * @throws KerberosException
     */
    public Authenticator verifyAuthHeader( ApplicationRequest authHeader, Ticket ticket, EncryptionKey serverKey,
        long clockSkew, ReplayCache replayCache, boolean emptyAddressesAllowed, InetAddress clientAddress,
        CipherTextHandler lockBox, KeyUsage authenticatorKeyUsage ) throws KerberosException
    {
        if ( authHeader.getProtocolVersionNumber() != 5 )
        {
            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_BADVERSION );
        }

        if ( authHeader.getMessageType() != MessageType.KRB_AP_REQ )
        {
            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_MSG_TYPE );
        }

        if ( authHeader.getTicket().getVersionNumber() != 5 )
        {
            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_BADVERSION );
        }

        EncryptionKey ticketKey = null;

        if ( authHeader.getOption( ApOption.USE_SESSION_KEY ) )
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
                throw new KerberosException( KerberosErrorType.KRB_AP_ERR_BADKEYVER );
            }

            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_NOKEY );
        }

        EncTicketPart encPart = ( EncTicketPart ) lockBox.unseal( EncTicketPart.class, ticketKey, ticket.getEncPart(),
            KeyUsage.NUMBER2 );
        ticket.setEncTicketPart( encPart );

        Authenticator authenticator = ( Authenticator ) lockBox.unseal( Authenticator.class, ticket.getSessionKey(),
            authHeader.getEncPart(), authenticatorKeyUsage );

        if ( !authenticator.getClientPrincipal().getName().equals( ticket.getClientPrincipal().getName() ) )
        {
            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_BADMATCH );
        }

        if ( ticket.getClientAddresses() != null )
        {
            if ( !ticket.getClientAddresses().contains( new HostAddress( clientAddress ) ) )
            {
                throw new KerberosException( KerberosErrorType.KRB_AP_ERR_BADADDR );
            }
        }
        else
        {
            if ( !emptyAddressesAllowed )
            {
                throw new KerberosException( KerberosErrorType.KRB_AP_ERR_BADADDR );
            }
        }

        if ( replayCache.isReplay( authenticator.getClientTime(), authenticator.getClientPrincipal() ) )
        {
            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_REPEAT );
        }

        replayCache.save( authenticator.getClientTime(), authenticator.getClientPrincipal() );

        if ( !authenticator.getClientTime().isInClockSkew( clockSkew ) )
        {
            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_SKEW );
        }

        if ( ticket.getStartTime() != null && !ticket.getStartTime().isInClockSkew( clockSkew )
            || ticket.getFlags().isInvalid() )
        {
            // it hasn't yet become valid
            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_TKT_NYV );
        }

        // TODO - doesn't take into account skew
        if ( !ticket.getEndTime().greaterThan( new KerberosTime() ) )
        {
            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_TKT_EXPIRED );
        }

        authHeader.setOption( ApOption.MUTUAL_REQUIRED );

        return authenticator;
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
