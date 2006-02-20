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
package org.apache.directory.server.kerberos.shared.service;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;


/*
 * Shared by TGS and Changepw
 */
public abstract class VerifyTicket extends CommandBase
{
    public void verifyTicket( Ticket ticket, String primaryRealm, KerberosPrincipal serverPrincipal ) throws Exception
    {
        if ( !ticket.getRealm().equals( primaryRealm ) && !ticket.getServerPrincipal().equals( serverPrincipal ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_NOT_US );
        }
    }
}
