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
package org.apache.directory.server.kerberos.kdc.authentication;


import org.apache.directory.server.kerberos.kdc.MonitorReply;
import org.apache.directory.server.kerberos.kdc.MonitorRequest;
import org.apache.directory.server.kerberos.kdc.SelectEncryptionType;
import org.apache.directory.server.kerberos.kdc.preauthentication.PreAuthenticationChain;
import org.apache.mina.handler.chain.IoHandlerChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * KRB_AS_REQ verification and KRB_AS_REP generation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AuthenticationServiceChain extends IoHandlerChain
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( AuthenticationServiceChain.class );

    private String serviceName = "Authentication Service (AS)";


    /**
     * Creates a new instance of AuthenticationServiceChain.
     */
    public AuthenticationServiceChain()
    {
        if ( log.isDebugEnabled() )
        {
            addLast( "monitorRequest", new MonitorRequest( serviceName ) );
        }

        addLast( "configureAuthenticationChain", new ConfigureAuthenticationChain() );
        addLast( "selectEncryptionType", new SelectEncryptionType() );
        addLast( "getClientEntry", new GetClientEntry() );
        addLast( "verifyPolicy", new VerifyPolicy() );
        addLast( "preAuthenticationChain", new PreAuthenticationChain() );
        addLast( "getServerEntry", new GetServerEntry() );
        addLast( "generateTicket", new GenerateTicket() );
        addLast( "buildReply", new BuildReply() );

        if ( log.isDebugEnabled() )
        {
            addLast( "monitorContext", new MonitorContext( serviceName ) );
        }

        if ( log.isDebugEnabled() )
        {
            addLast( "monitorReply", new MonitorReply( serviceName ) );
        }

        addLast( "sealReply", new SealReply() );
    }
}
