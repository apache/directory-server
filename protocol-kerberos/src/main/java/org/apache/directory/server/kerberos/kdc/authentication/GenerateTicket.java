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
package org.apache.directory.server.kerberos.kdc.authentication;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPart;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalName;
import org.apache.directory.server.kerberos.shared.messages.value.TransitedEncoding;
import org.apache.directory.server.kerberos.shared.messages.value.flags.KdcOption;
import org.apache.directory.server.kerberos.shared.messages.value.flags.TicketFlag;
import org.apache.directory.server.kerberos.shared.messages.value.types.KerberosErrorType;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GenerateTicket implements IoHandlerCommand
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( GenerateTicket.class );

    private String contextKey = "context";


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        AuthenticationContext authContext = ( AuthenticationContext ) session.getAttribute( getContextKey() );

        KdcRequest request = authContext.getRequest();
        CipherTextHandler cipherTextHandler = authContext.getCipherTextHandler();
        PrincipalName serverPrincipal = request.getServerPrincipalName();

        EncryptionType encryptionType = authContext.getEncryptionType();
        EncryptionKey serverKey = authContext.getServerEntry().getKeyMap().get( encryptionType );

        KerberosPrincipal ticketPrincipal = request.getServerPrincipal();
        EncTicketPart ticketPart = new EncTicketPart();
        KdcConfiguration config = authContext.getConfig();

        // The INITIAL flag indicates that a ticket was issued using the AS protocol.
        ticketPart.setFlag( TicketFlag.INITIAL );

        // The PRE-AUTHENT flag indicates that the client used pre-authentication.
        if ( authContext.isPreAuthenticated() )
        {
            ticketPart.setFlag( TicketFlag.PRE_AUTHENT );
        }

        if ( request.getKdcOptions().isFlagSet( KdcOption.FORWARDABLE ) )
        {
            ticketPart.setFlag( TicketFlag.FORWARDABLE );
        }

        if ( request.getKdcOptions().isFlagSet( KdcOption.PROXIABLE ) )
        {
            ticketPart.setFlag( TicketFlag.PROXIABLE );
        }

        if ( request.getKdcOptions().isFlagSet( KdcOption.ALLOW_POSTDATE ) )
        {
            ticketPart.setFlag( TicketFlag.MAY_POSTDATE );
        }

        if ( request.getKdcOptions().isFlagSet( KdcOption.RENEW ) || 
             request.getKdcOptions().isFlagSet( KdcOption.VALIDATE ) || 
             request.getKdcOptions().isFlagSet( KdcOption.PROXY ) || 
             request.getKdcOptions().isFlagSet( KdcOption.FORWARDED ) || 
             request.getKdcOptions().isFlagSet( KdcOption.ENC_TKT_IN_SKEY ) )
        {
            throw new KerberosException( KerberosErrorType.KDC_ERR_BADOPTION );
        }

        EncryptionKey sessionKey = RandomKeyFactory.getRandomKey( authContext.getEncryptionType() );
        ticketPart.setSessionKey( sessionKey );

        ticketPart.setClientPrincipal( request.getClientPrincipal() );
        ticketPart.setTransitedEncoding( new TransitedEncoding() );

        KerberosTime now = new KerberosTime();
        ticketPart.setAuthTime( now );

        if ( request.getKdcOptions().isFlagSet( KdcOption.POSTDATED ) )
        {
            // TODO - possibly allow req.from range
            if ( !config.isPostdateAllowed() )
            {
                throw new KerberosException( KerberosErrorType.KDC_ERR_POLICY );
            }

            ticketPart.setFlag( TicketFlag.INVALID );
            ticketPart.setStartTime( request.getFrom() );
        }

        long till = 0;
        
        if ( request.getTill().getTime() == 0 )
        {
            till = Long.MAX_VALUE;
        }
        else
        {
            till = request.getTill().getTime();
        }
        
        long endTime = Math.min( now.getTime() + config.getMaximumTicketLifetime(), till );
        KerberosTime kerberosEndTime = new KerberosTime( endTime );
        ticketPart.setEndTime( kerberosEndTime );

        long tempRenewtime = 0;
        
        if ( request.getKdcOptions().isFlagSet( KdcOption.RENEWABLE_OK ) && 
            request.getTill().greaterThan( kerberosEndTime ) )
        {
            request.getKdcOptions().setFlag( KdcOption.RENEWABLE );
            tempRenewtime = request.getTill().getTime();
        }

        if ( tempRenewtime == 0 || request.getRenewtime() == null )
        {
            tempRenewtime = request.getTill().getTime();
        }
        else
        {
            tempRenewtime = request.getRenewtime().getTime();
        }

        if ( request.getKdcOptions().isFlagSet( KdcOption.RENEWABLE ) )
        {
            ticketPart.setFlag( TicketFlag.RENEWABLE );

            /*
             * 'from' KerberosTime is OPTIONAL
             */
            KerberosTime fromTime = request.getFrom();

            if ( fromTime == null )
            {
                fromTime = new KerberosTime();
            }

            long renewTill = Math.min( fromTime.getTime() + config.getMaximumRenewableLifetime(), tempRenewtime );
            ticketPart.setRenewTill( new KerberosTime( renewTill ) );
        }

        if ( request.getAddresses() != null )
        {
            ticketPart.setClientAddresses( request.getAddresses() );
        }

        //EncTicketPart ticketPart = newTicketBody.getEncTicketPart();

        EncryptedData encryptedData = cipherTextHandler.seal( serverKey, ticketPart, KeyUsage.NUMBER2 );

        Ticket newTicket = new Ticket( ticketPrincipal, encryptedData );
        newTicket.setEncTicketPart( ticketPart );

        if ( log.isDebugEnabled() )
        {
            log.debug( "Ticket will be issued for access to {}.", serverPrincipal.toString() );
        }

        authContext.setTicket( newTicket );

        next.execute( session, message );
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
