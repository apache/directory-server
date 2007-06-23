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


import org.apache.directory.server.kerberos.kdc.MonitorReply;
import org.apache.directory.server.kerberos.kdc.MonitorRequest;
import org.apache.directory.server.kerberos.kdc.SelectEncryptionType;
import org.apache.mina.handler.chain.IoHandlerChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * KRB_TGS_REQ verification and KRB_TGS_REP generation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TicketGrantingServiceChain extends IoHandlerChain
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( TicketGrantingServiceChain.class );


    /**
     * Creates a new instance of TicketGrantingServiceChain.
     */
    public TicketGrantingServiceChain()
    {
        if ( log.isDebugEnabled() )
        {
            addLast( "monitorRequest", new MonitorRequest() );
        }

        addLast( "configureTicketGrantingChain", new ConfigureTicketGrantingChain() );
        addLast( "selectEncryptionType", new SelectEncryptionType() );
        addLast( "getAuthHeader", new GetAuthHeader() );
        addLast( "verifyTgt", new VerifyTgt() );
        addLast( "getTicketPrincipalEntry", new GetTicketPrincipalEntry() );
        addLast( "verifyTgtAuthHeader", new VerifyTgtAuthHeader() );
        addLast( "verifyBodyChecksum", new VerifyBodyChecksum() );
        addLast( "getRequestPrincipalEntry", new GetRequestPrincipalEntry() );
        addLast( "generateTicket", new GenerateTicket() );
        addLast( "buildReply", new BuildReply() );

        if ( log.isDebugEnabled() )
        {
            addLast( "monitorContext", new MonitorContext() );
        }

        if ( log.isDebugEnabled() )
        {
            addLast( "monitorReply", new MonitorReply() );
        }

        addLast( "sealReply", new SealReply() );
    }
}
