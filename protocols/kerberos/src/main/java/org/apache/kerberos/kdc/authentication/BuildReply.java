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
package org.apache.kerberos.kdc.authentication;

import org.apache.kerberos.messages.AuthenticationReply;
import org.apache.kerberos.messages.KdcRequest;
import org.apache.kerberos.messages.components.Ticket;
import org.apache.kerberos.messages.value.LastRequest;
import org.apache.kerberos.messages.value.TicketFlags;
import org.apache.protocol.common.chain.Context;
import org.apache.protocol.common.chain.impl.CommandBase;

public class BuildReply extends CommandBase
{
    public boolean execute( Context ctx ) throws Exception
    {
        AuthenticationContext authContext = (AuthenticationContext) ctx;
        KdcRequest request = authContext.getRequest();
        Ticket ticket = authContext.getTicket();

        AuthenticationReply reply = new AuthenticationReply();

        reply.setClientPrincipal( request.getClientPrincipal() );
        reply.setTicket( ticket );
        reply.setKey( ticket.getSessionKey() );

        // TODO - fetch lastReq for this client; requires store
        reply.setLastRequest( new LastRequest() );
        // TODO    - resp.key-expiration := client.expiration; requires store

        reply.setNonce( request.getNonce() );

        reply.setFlags( ticket.getFlags() );
        reply.setAuthTime( ticket.getAuthTime() );
        reply.setStartTime( ticket.getStartTime() );
        reply.setEndTime( ticket.getEndTime() );

        if ( ticket.getFlags().get( TicketFlags.RENEWABLE ) )
        {
            reply.setRenewTill( ticket.getRenewTill() );
        }

        reply.setServerPrincipal( ticket.getServerPrincipal() );
        reply.setClientAddresses( ticket.getClientAddresses() );

        authContext.setReply( reply );

        return CONTINUE_CHAIN;
    }
}
