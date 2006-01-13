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
package org.apache.changepw.service;

import org.apache.changepw.messages.ChangePasswordRequest;
import org.apache.kerberos.messages.ApplicationRequest;
import org.apache.kerberos.messages.components.Ticket;
import org.apache.protocol.common.chain.Context;
import org.apache.protocol.common.chain.impl.CommandBase;

/*
 * differs from the TGS getAuthHeader by not verifying the presence of TGS_REQ
 */
public class GetAuthHeader extends CommandBase
{
    public boolean execute( Context context ) throws Exception
    {
        ChangePasswordContext changepwContext = (ChangePasswordContext) context;
        ChangePasswordRequest request = (ChangePasswordRequest) changepwContext.getRequest();

        ApplicationRequest authHeader = request.getAuthHeader();
        Ticket ticket = authHeader.getTicket();

        changepwContext.setAuthHeader( authHeader );
        changepwContext.setTicket( ticket );

        return CONTINUE_CHAIN;
    }
}
