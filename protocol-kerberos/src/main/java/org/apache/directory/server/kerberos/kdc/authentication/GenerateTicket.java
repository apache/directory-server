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
package org.apache.directory.server.kerberos.kdc.authentication;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;
import org.apache.kerberos.exceptions.ErrorType;
import org.apache.kerberos.exceptions.KerberosException;
import org.apache.kerberos.messages.KdcRequest;
import org.apache.kerberos.messages.components.EncTicketPart;
import org.apache.kerberos.messages.components.EncTicketPartModifier;
import org.apache.kerberos.messages.components.Ticket;
import org.apache.kerberos.messages.value.EncryptedData;
import org.apache.kerberos.messages.value.EncryptionKey;
import org.apache.kerberos.messages.value.KdcOptions;
import org.apache.kerberos.messages.value.KerberosTime;
import org.apache.kerberos.messages.value.TicketFlags;
import org.apache.kerberos.messages.value.TransitedEncoding;
import org.apache.kerberos.service.LockBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateTicket extends CommandBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( GenerateTicket.class );
    
    public boolean execute(Context context) throws Exception
    {
        AuthenticationContext authContext = (AuthenticationContext) context;

        KdcRequest request = authContext.getRequest();
        LockBox lockBox = authContext.getLockBox();
        KerberosPrincipal serverPrincipal = request.getServerPrincipal();
        EncryptionKey serverKey = authContext.getServerEntry().getEncryptionKey();
        KerberosPrincipal ticketPrincipal = request.getServerPrincipal();
        EncTicketPartModifier newTicketBody = new EncTicketPartModifier();
        KdcConfiguration config = authContext.getConfig();
        EncryptionKey sessionKey = authContext.getSessionKey();

        if(request.getKdcOptions().get(KdcOptions.FORWARDABLE))
        {
            newTicketBody.setFlag(TicketFlags.FORWARDABLE);
        }

        if(request.getKdcOptions().get(KdcOptions.PROXIABLE))
        {
            newTicketBody.setFlag(TicketFlags.PROXIABLE);
        }

        if(request.getKdcOptions().get(KdcOptions.ALLOW_POSTDATE))
        {
            newTicketBody.setFlag(TicketFlags.MAY_POSTDATE);
        }

        if(request.getKdcOptions().get(KdcOptions.RENEW) ||
                request.getKdcOptions().get(KdcOptions.VALIDATE) ||
                request.getKdcOptions().get(KdcOptions.PROXY) ||
                request.getKdcOptions().get(KdcOptions.FORWARDED) ||
                request.getKdcOptions().get(KdcOptions.ENC_TKT_IN_SKEY))
        {
            throw new KerberosException( ErrorType.KDC_ERR_BADOPTION );
        }

        newTicketBody.setSessionKey( sessionKey );
        newTicketBody.setClientPrincipal(request.getClientPrincipal());
        newTicketBody.setTransitedEncoding(new TransitedEncoding());

        KerberosTime now = new KerberosTime();
        newTicketBody.setAuthTime(now);

        if (request.getKdcOptions().get(KdcOptions.POSTDATED))
        {
            // TODO - possibly allow req.from range
            if (!config.isPostdateAllowed())
                throw new KerberosException( ErrorType.KDC_ERR_POLICY );
            newTicketBody.setFlag(TicketFlags.INVALID);
            newTicketBody.setStartTime(request.getFrom());
        }

        long till = 0;
        if (request.getTill().getTime() == 0)
                till = Long.MAX_VALUE;
        else
                till = request.getTill().getTime();
        /*
        new_tkt.endtime := min(till,
                              new_tkt.starttime+client.max_life,
                              new_tkt.starttime+server.max_life,
                              new_tkt.starttime+max_life_for_realm);
        */
        long endTime = Math.min(now.getTime() + config.getMaximumTicketLifetime(), till);
        KerberosTime kerberosEndTime = new KerberosTime(endTime);
        newTicketBody.setEndTime(kerberosEndTime);

        long tempRtime = 0;
        if (request.getKdcOptions().get(KdcOptions.RENEWABLE_OK) &&
                request.getTill().greaterThan(kerberosEndTime))
        {
            request.getKdcOptions().set(KdcOptions.RENEWABLE);
            tempRtime = request.getTill().getTime();
        }

        /*
        if (req.kdc-options.RENEWABLE is set) then
                set new_tkt.flags.RENEWABLE;
                new_tkt.renew-till := min(rtime,
                new_tkt.starttime+client.max_rlife,
                new_tkt.starttime+server.max_rlife,
                new_tkt.starttime+max_rlife_for_realm);
        else
                omit new_tkt.renew-till;
        endif
        */

        if (tempRtime == 0)
        {
            tempRtime = Long.MAX_VALUE;
        }
        else
        {
            tempRtime = request.getRtime().getTime();
        }

        if ( request.getKdcOptions().get( KdcOptions.RENEWABLE ) )
        {
            newTicketBody.setFlag( TicketFlags.RENEWABLE );

            /*
             * 'from' KerberosTime is OPTIONAL
             */
            KerberosTime fromTime = request.getFrom();

            if ( fromTime == null )
            {
                fromTime = new KerberosTime();
            }

            long renewTill = Math.min( fromTime.getTime()
                    + config.getMaximumRenewableLifetime(), tempRtime );
            newTicketBody.setRenewTill( new KerberosTime( renewTill ) );
        }

        if (request.getAddresses() != null)
        {
            newTicketBody.setClientAddresses(request.getAddresses());
        }

        EncTicketPart ticketPart = newTicketBody.getEncTicketPart();

        EncryptedData encryptedData = lockBox.seal( serverKey, ticketPart );

        Ticket newTicket = new Ticket(ticketPrincipal, encryptedData);
        newTicket.setEncTicketPart(ticketPart);

        if ( log.isDebugEnabled() )
        {
            log.debug( "Ticket will be issued for access to " + serverPrincipal.toString() + "." );
        }

        authContext.setTicket( newTicket );
        
        return CONTINUE_CHAIN;
    }
}
