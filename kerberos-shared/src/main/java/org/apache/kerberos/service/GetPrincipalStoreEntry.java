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
package org.apache.kerberos.service;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;
import org.apache.kerberos.exceptions.ErrorType;
import org.apache.kerberos.exceptions.KerberosException;
import org.apache.kerberos.store.PrincipalStore;
import org.apache.kerberos.store.PrincipalStoreEntry;

public abstract class GetPrincipalStoreEntry extends CommandBase
{
    public PrincipalStoreEntry getEntry( KerberosPrincipal principal, PrincipalStore store,
            ErrorType errorType ) throws Exception
    {
        PrincipalStoreEntry entry = null;

        try
        {
            entry = store.getPrincipal( principal );
        }
        catch ( Exception e )
        {
            throw new KerberosException( errorType );
        }

        if ( entry == null || entry.getEncryptionKey() == null )
        {
            throw new KerberosException( errorType );
        }

        return entry;
    }
}
