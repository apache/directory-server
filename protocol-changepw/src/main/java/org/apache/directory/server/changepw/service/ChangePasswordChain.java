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
package org.apache.directory.server.changepw.service;


import org.apache.mina.handler.chain.IoHandlerChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Kerberos Change Password and Set Password Protocols (RFC 3244)
 */
public class ChangePasswordChain extends IoHandlerChain
{
    /** the logger for this class */
    private static final Logger log = LoggerFactory.getLogger( ChangePasswordChain.class );


    public ChangePasswordChain()
    {
        if ( log.isDebugEnabled() )
        {
            addLast( "monitorRequest", new MonitorRequest() );
        }

        addLast( "configureChangePasswordChain", new ConfigureChangePasswordChain() );
        addLast( "getAuthHeader", new GetAuthHeader() );
        addLast( "verifyServiceTicket", new VerifyServiceTicket() );
        addLast( "getServerEntry", new GetServerEntry() );
        addLast( "verifyServiceTicketAuthHeader", new VerifyServiceTicketAuthHeader() );

        addLast( "extractPassword", new ExtractPassword() );

        if ( log.isDebugEnabled() )
        {
            addLast( "monitorContext", new MonitorContext() );
        }

        addLast( "checkPasswordPolicy", new CheckPasswordPolicy() );
        addLast( "processPasswordChange", new ProcessPasswordChange() );
        addLast( "buildReply", new BuildReply() );

        if ( log.isDebugEnabled() )
        {
            addLast( "monitorReply", new MonitorReply() );
        }
    }
}
