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
package org.apache.directory.server.kerberos.shared.store.operations;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.protocol.shared.store.DirectoryServiceOperation;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Command for deleting a principal from a JNDI context.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DeletePrincipal implements DirectoryServiceOperation
{
    private static final long serialVersionUID = -6970986279811261983L;

    /** The Kerberos principal who is to be deleted. */
    protected KerberosPrincipal principal;


    /**
     * Creates the action to be used against the embedded ApacheDS DIT.
     * 
     * @param principal The principal to delete.
     */
    public DeletePrincipal( KerberosPrincipal principal )
    {
        this.principal = principal;
    }


    public Object execute( CoreSession session, LdapDN searchBaseDn ) throws Exception
    {
        if ( principal == null )
        {
            return null;
        }

        ServerEntry entry = StoreUtils.findPrincipalEntry( session, searchBaseDn, principal.getName() );
        session.delete( entry.getDn() );
        return entry.getDn();
    }
}
