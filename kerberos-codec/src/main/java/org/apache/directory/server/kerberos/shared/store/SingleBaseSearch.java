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
package org.apache.directory.server.kerberos.shared.store;



import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.shared.store.operations.ChangePassword;
import org.apache.directory.server.kerberos.shared.store.operations.GetPrincipal;
import org.apache.directory.server.protocol.shared.ServiceConfigurationException;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A JNDI-backed search strategy implementation. This search strategy searches 
 * for Kerberos principals.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class SingleBaseSearch implements PrincipalStore
{
    private final CoreSession session;
    private final Dn searchBaseDn;


    SingleBaseSearch( DirectoryService directoryService, Dn searchBaseDn )
    {
        try
        {
            session = directoryService.getAdminSession();
            this.searchBaseDn = searchBaseDn;
        }
        catch ( Exception e )
        {
            throw new ServiceConfigurationException( I18n.err( I18n.ERR_627 ), e );
        }

    }


    public PrincipalStoreEntry getPrincipal( KerberosPrincipal principal ) throws Exception
    {
        return ( PrincipalStoreEntry ) new GetPrincipal( principal ).execute( session, searchBaseDn );
    }


    public String changePassword( KerberosPrincipal principal, String newPassword ) throws Exception
    {
        return (String) new ChangePassword( principal, newPassword ).execute( session, searchBaseDn );
    }
}
