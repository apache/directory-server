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
package org.apache.directory.server.changepw.service;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.changepw.ChangePasswordConfiguration;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.service.VerifyTicket;
import org.apache.directory.server.protocol.shared.chain.Context;


public class VerifyServiceTicket extends VerifyTicket
{
    public boolean execute( Context context ) throws Exception
    {
        ChangePasswordContext changepwContext = ( ChangePasswordContext ) context;
        ChangePasswordConfiguration config = changepwContext.getConfig();
        Ticket ticket = changepwContext.getTicket();
        String primaryRealm = config.getPrimaryRealm();
        KerberosPrincipal changepwPrincipal = config.getChangepwPrincipal();

        verifyTicket( ticket, primaryRealm, changepwPrincipal );

        return CONTINUE_CHAIN;
    }
}
