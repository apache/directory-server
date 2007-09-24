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
package org.apache.directory.server.kerberos.shared.service;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.types.KerberosErrorType;
import org.apache.mina.handler.chain.IoHandlerCommand;


/**
 * Shared by TGS and Changepw.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public abstract class VerifyTicket implements IoHandlerCommand
{
    private String contextKey = "context";


    /**
     * Verifies a Ticket given a realm and the server principal.
     *
     * @param ticket
     * @param primaryRealm
     * @param serverPrincipal
     * @throws Exception
     */
    public void verifyTicket( Ticket ticket, String primaryRealm, KerberosPrincipal serverPrincipal ) throws Exception
    {
        if ( !ticket.getRealm().equals( primaryRealm ) && !ticket.getServerPrincipal().equals( serverPrincipal ) )
        {
            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_NOT_US );
        }
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
