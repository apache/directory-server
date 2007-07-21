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


import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;


/**
 * Note that the realm in which the Kerberos server is operating is determined by
 * the instance from the ticket-granting ticket.  The realm in the ticket-granting
 * ticket is the realm under which the ticket granting ticket was issued.  It is
 * possible for a single Kerberos server to support more than one realm.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class VerifyTgt implements IoHandlerCommand
{
    private String contextKey = "context";


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        TicketGrantingContext tgsContext = ( TicketGrantingContext ) session.getAttribute( getContextKey() );
        KdcConfiguration config = tgsContext.getConfig();
        Ticket tgt = tgsContext.getTgt();

        // Check primary realm.
        if ( !tgt.getRealm().equals( config.getPrimaryRealm() ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_NOT_US );
        }

        String tgtServerName = tgt.getServerPrincipal().getName();
        String requestServerName = tgsContext.getRequest().getServerPrincipal().getName();

        /*
         * if (tgt.sname is not a TGT for local realm and is not
         * req.sname) then error_out(KRB_AP_ERR_NOT_US);
         */
        if ( !tgtServerName.equals( config.getServicePrincipal().getName() )
            && !tgtServerName.equals( requestServerName ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_NOT_US );
        }

        next.execute( session, message );
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
