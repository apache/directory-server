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
package org.apache.kerberos.kdc.authentication;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.kerberos.exceptions.ErrorType;
import org.apache.kerberos.service.GetPrincipalStoreEntry;
import org.apache.kerberos.store.PrincipalStore;
import org.apache.protocol.common.chain.Context;

public class GetServerEntry extends GetPrincipalStoreEntry
{
    public boolean execute(Context context) throws Exception
    {
        AuthenticationContext authContext = (AuthenticationContext) context;
        
        KerberosPrincipal principal = authContext.getRequest().getServerPrincipal();
        PrincipalStore store = authContext.getStore();
        
        authContext.setServerEntry( getEntry( principal, store, ErrorType.KDC_ERR_S_PRINCIPAL_UNKNOWN ) );
        
        return CONTINUE_CHAIN;
    }
}
