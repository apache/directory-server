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
package org.apache.directory.server.kerberos.kdc;


import org.apache.directory.server.kerberos.shared.messages.ErrorMessage;
import org.apache.directory.server.kerberos.shared.messages.KdcReply;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MonitorReply implements IoHandlerCommand
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( MonitorReply.class );

    private String contextKey = "context";


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        KdcContext kdcContext = ( KdcContext ) session.getAttribute( getContextKey() );
        Object reply = kdcContext.getReply();

        if ( reply instanceof KdcReply )
        {
            KdcReply success = ( KdcReply ) reply;

            if ( log.isDebugEnabled() )
            {
                try
                {
                    StringBuffer sb = new StringBuffer();

                    sb.append( "Responding to authentication request with reply:" );
                    sb.append( "\n\t" + "client realm:          " + success.getClientRealm() );
                    sb.append( "\n\t" + "server realm:          " + success.getServerRealm() );
                    sb.append( "\n\t" + "serverPrincipal:       " + success.getServerPrincipal() );
                    sb.append( "\n\t" + "clientPrincipal:       " + success.getClientPrincipal() );
                    sb.append( "\n\t" + "hostAddresses:         " + success.getClientAddresses() );
                    sb.append( "\n\t" + "start time:            " + success.getStartTime() );
                    sb.append( "\n\t" + "end time:              " + success.getEndTime() );
                    sb.append( "\n\t" + "auth time:             " + success.getAuthTime() );
                    sb.append( "\n\t" + "renew till time:       " + success.getRenewTill() );
                    sb.append( "\n\t" + "messageType:           " + success.getMessageType() );
                    sb.append( "\n\t" + "nonce:                 " + success.getNonce() );
                    sb.append( "\n\t" + "protocolVersionNumber: " + success.getProtocolVersionNumber() );

                    log.debug( sb.toString() );
                }
                catch ( Exception e )
                {
                    // This is a monitor.  No exceptions should bubble up.
                    log.error( "Error in reply monitor", e );
                }
            }
        }
        else
        {
            if ( reply instanceof ErrorMessage )
            {
                ErrorMessage error = ( ErrorMessage ) reply;

                if ( log.isDebugEnabled() )
                {
                    try
                    {
                        StringBuffer sb = new StringBuffer();

                        sb.append( "Responding to authentication request with error:" );
                        sb.append( "\n\t" + "serverPrincipal:       " + error.getServerPrincipal() );
                        sb.append( "\n\t" + "clientPrincipal:       " + error.getClientPrincipal() );
                        sb.append( "\n\t" + "server time:           " + error.getClientTime() );
                        sb.append( "\n\t" + "client time:           " + error.getServerTime() );
                        sb.append( "\n\t" + "error code:            " + error.getErrorCode() );
                        sb.append( "\n\t" + "explanatory text:      " + error.getExplanatoryText() );

                        log.debug( sb.toString() );
                    }
                    catch ( Exception e )
                    {
                        // This is a monitor.  No exceptions should bubble up.
                        log.error( "Error in reply monitor", e );
                    }
                }
            }
        }

        next.execute( session, message );
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
