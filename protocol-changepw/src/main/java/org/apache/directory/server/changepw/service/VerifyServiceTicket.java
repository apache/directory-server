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


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.changepw.ChangePasswordConfiguration;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.types.KerberosErrorType;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class VerifyServiceTicket implements IoHandlerCommand
{
    private String contextKey = "context";


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        ChangePasswordContext changepwContext = ( ChangePasswordContext ) session.getAttribute( getContextKey() );
        ChangePasswordConfiguration config = changepwContext.getConfig();
        Ticket ticket = changepwContext.getTicket();
        String primaryRealm = config.getPrimaryRealm();
        KerberosPrincipal changepwPrincipal = config.getServicePrincipal();

        if ( !ticket.getRealm().equals( primaryRealm ) || !ticket.getServerPrincipal().equals( changepwPrincipal ) )
        {
            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_NOT_US );
        }

        next.execute( session, message );
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
