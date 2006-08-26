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
                log.debug( "Responding to authentication request with reply:" + "\n\tclient realm:          "
                    + success.getClientRealm() + "\n\tserver realm:          " + success.getServerRealm()
                    + "\n\tserverPrincipal:       " + success.getServerPrincipal() + "\n\tclientPrincipal:       "
                    + success.getClientPrincipal() + "\n\thostAddresses:         " + success.getClientAddresses()
                    + "\n\tstart time:            " + success.getStartTime() + "\n\tend time:              "
                    + success.getEndTime() + "\n\tauth time:             " + success.getAuthTime()
                    + "\n\trenew till time:       " + success.getRenewTill() + "\n\tmessageType:           "
                    + success.getMessageType() + "\n\tnonce:                 " + success.getNonce()
                    + "\n\tprotocolVersionNumber: " + success.getProtocolVersionNumber() );
            }
        }
        else
        {
            if ( reply instanceof ErrorMessage )
            {
                ErrorMessage error = ( ErrorMessage ) reply;

                if ( log.isDebugEnabled() )
                {
                    log.debug( "Responding to authentication request with error:" + "\n\tserverPrincipal:       "
                        + error.getServerPrincipal() + "\n\tclientPrincipal:       " + error.getClientPrincipal()
                        + "\n\tserver time:           " + error.getClientTime() + "\n\tclient time:           "
                        + error.getServerTime() + "\n\terror code:            " + error.getErrorCode()
                        + "\n\texplanatory text:      " + error.getExplanatoryText() );
                }
            }
        }

        next.execute( session, message );
    }


    public String getContextKey()
    {
        return ( this.contextKey );
    }
}
