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


import org.apache.directory.server.kerberos.shared.messages.KdcRequest;
import org.apache.directory.server.kerberos.shared.messages.TicketGrantReply;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.LastRequest;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;
import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;


public class BuildReply extends CommandBase
{
    public boolean execute( Context context ) throws Exception
    {
        TicketGrantingContext tgsContext = ( TicketGrantingContext ) context;
        KdcRequest request = tgsContext.getRequest();
        Ticket tgt = tgsContext.getTgt();
        Ticket newTicket = tgsContext.getNewTicket();
        EncryptionKey sessionKey = tgsContext.getSessionKey();

        TicketGrantReply reply = new TicketGrantReply();
        reply.setClientPrincipal( tgt.getClientPrincipal() );
        reply.setTicket( newTicket );
        reply.setKey( sessionKey );
        reply.setNonce( request.getNonce() );
        // TODO - resp.last-req := fetch_last_request_info(client); requires store
        reply.setLastRequest( new LastRequest() );
        reply.setFlags( newTicket.getFlags() );
        reply.setClientAddresses( newTicket.getClientAddresses() );
        reply.setAuthTime( newTicket.getAuthTime() );
        reply.setStartTime( newTicket.getStartTime() );
        reply.setEndTime( newTicket.getEndTime() );
        reply.setServerPrincipal( newTicket.getServerPrincipal() );

        if ( newTicket.getFlag( TicketFlags.RENEWABLE ) )
        {
            reply.setRenewTill( newTicket.getRenewTill() );
        }

        tgsContext.setReply( reply );

        return CONTINUE_CHAIN;
    }
}
