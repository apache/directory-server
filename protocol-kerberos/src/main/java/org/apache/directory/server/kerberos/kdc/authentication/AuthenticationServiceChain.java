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


import org.apache.directory.server.kerberos.kdc.MonitorRequest;
import org.apache.directory.server.kerberos.kdc.preauthentication.PreAuthenticationChain;
import org.apache.mina.handler.chain.IoHandlerChain;


public class AuthenticationServiceChain extends IoHandlerChain
{
    public AuthenticationServiceChain()
    {
        addLast( "monitorRequest", new MonitorRequest() );
        addLast( "configureAuthenticationChain", new ConfigureAuthenticationChain() );
        addLast( "getClientEntry", new GetClientEntry() );
        addLast( "verifyPolicy", new VerifyPolicy() );
        addLast( "preAuthenticationChain", new PreAuthenticationChain() );
        addLast( "getServerEntry", new GetServerEntry() );
        addLast( "getSessionKey", new GetSessionKey() );
        addLast( "generateTicket", new GenerateTicket() );
        addLast( "buildReply", new BuildReply() );
        addLast( "sealReply", new SealReply() );
    }
}
