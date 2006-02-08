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

import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.kerberos.exceptions.ErrorType;
import org.apache.kerberos.service.GetPrincipalStoreEntry;
import org.apache.kerberos.store.PrincipalStore;

public class GetServerEntry extends GetPrincipalStoreEntry
{
    public boolean execute( Context context ) throws Exception
    {
        ChangePasswordContext changepwContext = (ChangePasswordContext) context;

        KerberosPrincipal principal = changepwContext.getTicket().getServerPrincipal();
        PrincipalStore store = changepwContext.getStore();

        changepwContext.setServerEntry( getEntry( principal, store,
                ErrorType.KDC_ERR_S_PRINCIPAL_UNKNOWN ) );

        return CONTINUE_CHAIN;
    }
}
