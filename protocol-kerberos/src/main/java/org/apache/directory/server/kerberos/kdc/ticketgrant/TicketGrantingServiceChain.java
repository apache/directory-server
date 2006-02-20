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
package org.apache.directory.server.kerberos.kdc.ticketgrant;


import org.apache.directory.server.kerberos.kdc.MonitorReply;
import org.apache.directory.server.kerberos.kdc.MonitorRequest;
import org.apache.directory.server.protocol.shared.chain.impl.ChainBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * KRB_TGS_REQ verification and KRB_TGS_REP generation
 */
public class TicketGrantingServiceChain extends ChainBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( TicketGrantingServiceChain.class );


    public TicketGrantingServiceChain()
    {
        super();
        addCommand( new TicketGrantingExceptionHandler() );

        if ( log.isDebugEnabled() )
        {
            addCommand( new MonitorRequest() );
        }

        addCommand( new ConfigureTicketGrantingChain() );
        addCommand( new GetAuthHeader() );
        addCommand( new VerifyTgt() );
        addCommand( new GetTicketPrincipalEntry() );
        addCommand( new VerifyTgtAuthHeader() );
        addCommand( new VerifyBodyChecksum() );
        addCommand( new GetRequestPrincipalEntry() );
        addCommand( new GetSessionKey() );
        addCommand( new GenerateTicket() );
        addCommand( new BuildReply() );

        if ( log.isDebugEnabled() )
        {
            addCommand( new MonitorContext() );
        }

        if ( log.isDebugEnabled() )
        {
            addCommand( new MonitorReply() );
        }

        addCommand( new SealReply() );
    }
}
