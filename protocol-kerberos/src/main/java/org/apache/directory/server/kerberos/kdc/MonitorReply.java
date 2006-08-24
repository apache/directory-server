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
import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MonitorReply extends CommandBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( MonitorReply.class );


    public boolean execute( Context context ) throws Exception
    {
        KdcContext kdcContext = ( KdcContext ) context;
        Object message = kdcContext.getReply();

        if ( message instanceof KdcReply )
        {
            KdcReply reply = ( KdcReply ) message;

            if ( log.isDebugEnabled() )
            {
                log.debug( "Responding to authentication request with reply:" + "\n\tclient realm:          "
                    + reply.getClientRealm() + "\n\tserver realm:          " + reply.getServerRealm()
                    + "\n\tserverPrincipal:       " + reply.getServerPrincipal() + "\n\tclientPrincipal:       "
                    + reply.getClientPrincipal() + "\n\thostAddresses:         " + reply.getClientAddresses()
                    + "\n\tstart time:            " + reply.getStartTime() + "\n\tend time:              "
                    + reply.getEndTime() + "\n\tauth time:             " + reply.getAuthTime()
                    + "\n\trenew till time:       " + reply.getRenewTill() + "\n\tmessageType:           "
                    + reply.getMessageType() + "\n\tnonce:                 " + reply.getNonce()
                    + "\n\tprotocolVersionNumber: " + reply.getProtocolVersionNumber() );
            }
        }
        else
        {
            if ( message instanceof ErrorMessage )
            {
                ErrorMessage error = ( ErrorMessage ) message;

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

        return CONTINUE_CHAIN;
    }
}
