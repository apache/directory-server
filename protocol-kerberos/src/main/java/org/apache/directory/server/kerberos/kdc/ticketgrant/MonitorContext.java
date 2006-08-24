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


import java.net.InetAddress;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumType;
import org.apache.directory.server.kerberos.shared.messages.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MonitorContext extends CommandBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( MonitorContext.class );


    public boolean execute( Context context ) throws Exception
    {
        if ( log.isDebugEnabled() )
        {
            try
            {
                TicketGrantingContext tgsContext = ( TicketGrantingContext ) context;

                PrincipalStore store = tgsContext.getStore();
                ApplicationRequest authHeader = tgsContext.getAuthHeader();
                Ticket tgt = tgsContext.getTgt();
                long clockSkew = tgsContext.getConfig().getClockSkew();
                ReplayCache replayCache = tgsContext.getReplayCache();
                ChecksumType checksumType = tgsContext.getAuthenticator().getChecksum().getChecksumType();
                InetAddress clientAddress = tgsContext.getClientAddress();
                HostAddresses clientAddresses = tgt.getClientAddresses();

                boolean caddrContainsSender = false;
                if ( tgt.getClientAddresses() != null )
                {
                    caddrContainsSender = tgt.getClientAddresses().contains( new HostAddress( clientAddress ) );
                }

                StringBuffer sb = new StringBuffer();

                sb.append( "\n\t" + "store                  " + store );
                sb.append( "\n\t" + "authHeader             " + authHeader );
                sb.append( "\n\t" + "tgt                    " + tgt );
                sb.append( "\n\t" + "replayCache            " + replayCache );
                sb.append( "\n\t" + "clockSkew              " + clockSkew );
                sb.append( "\n\t" + "checksumType           " + checksumType );
                sb.append( "\n\t" + "clientAddress          " + clientAddress );
                sb.append( "\n\t" + "clientAddresses        " + clientAddresses );
                sb.append( "\n\t" + "caddr contains sender  " + caddrContainsSender );

                KerberosPrincipal requestServerPrincipal = tgsContext.getRequest().getServerPrincipal();
                PrincipalStoreEntry requestPrincipal = tgsContext.getRequestPrincipalEntry();

                sb.append( "\n\t" + "principal              " + requestServerPrincipal );
                sb.append( "\n\t" + "cn                     " + requestPrincipal.getCommonName() );
                sb.append( "\n\t" + "realm                  " + requestPrincipal.getRealmName() );
                sb.append( "\n\t" + "principal              " + requestPrincipal.getPrincipal() );
                sb.append( "\n\t" + "SAM type               " + requestPrincipal.getSamType() );
                sb.append( "\n\t" + "Key type               " + requestPrincipal.getEncryptionKey().getKeyType() );
                sb.append( "\n\t" + "Key version            " + requestPrincipal.getEncryptionKey().getKeyVersion() );

                KerberosPrincipal ticketServerPrincipal = tgsContext.getTgt().getServerPrincipal();
                PrincipalStoreEntry ticketPrincipal = tgsContext.getTicketPrincipalEntry();

                sb.append( "\n\t" + "principal              " + ticketServerPrincipal );
                sb.append( "\n\t" + "cn                     " + ticketPrincipal.getCommonName() );
                sb.append( "\n\t" + "realm                  " + ticketPrincipal.getRealmName() );
                sb.append( "\n\t" + "principal              " + ticketPrincipal.getPrincipal() );
                sb.append( "\n\t" + "SAM type               " + ticketPrincipal.getSamType() );
                sb.append( "\n\t" + "Key type               " + ticketPrincipal.getEncryptionKey().getKeyType() );
                sb.append( "\n\t" + "Key version            " + ticketPrincipal.getEncryptionKey().getKeyVersion() );

                log.debug( sb.toString() );
            }
            catch ( Exception e )
            {
                // This is a monitor.  No exceptions should bubble up.
                log.error( "Error in context monitor", e );
            }
        }

        return CONTINUE_CHAIN;
    }
}
