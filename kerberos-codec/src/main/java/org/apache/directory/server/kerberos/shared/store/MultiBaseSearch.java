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


import java.util.Map;

import javax.naming.NamingException;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.shared.store.operations.ChangePassword;
import org.apache.directory.server.kerberos.shared.store.operations.GetPrincipal;
import org.apache.directory.server.protocol.shared.ServiceConfigurationException;
import org.apache.directory.server.protocol.shared.catalog.Catalog;
import org.apache.directory.server.protocol.shared.catalog.GetCatalog;
import org.apache.directory.server.protocol.shared.store.DirectoryServiceOperation;


/**
 * A JNDI-backed search strategy implementation.  This search strategy builds a
 * catalog from configuration in the DIT to determine where realms are to search
 * for Kerberos principals.
 *
 * TODO are exception messages reasonable? I changed them to use the catalog key rather than the catalog value.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class MultiBaseSearch implements PrincipalStore
{
    private final Catalog catalog;
    private final DirectoryService directoryService;


    MultiBaseSearch( String catalogBaseDn, DirectoryService directoryService )
    {
        this.directoryService = directoryService;
        try
        {
            catalog = new KerberosCatalog( ( Map<String, String> ) execute( directoryService.getSession(), new GetCatalog() ) );
        }
        catch ( Exception e )
        {
            String message = I18n.err( I18n.ERR_624, catalogBaseDn );
            throw new ServiceConfigurationException( message, e );
        }
    }


    public PrincipalStoreEntry getPrincipal( KerberosPrincipal principal ) throws Exception
    {
        try
        {
            return ( PrincipalStoreEntry ) execute( directoryService.getSession(), new GetPrincipal( principal ) );
        }
        catch ( NamingException ne )
        {
            String message = I18n.err( I18n.ERR_625, principal.getRealm() );
            throw new ServiceConfigurationException( message, ne );
        }
    }


    public String changePassword( KerberosPrincipal principal, String newPassword ) throws Exception
    {
        try
        {
            return ( String ) execute( directoryService.getSession(), new ChangePassword( principal, newPassword ) );
        }
        catch ( NamingException ne )
        {
            String message = I18n.err( I18n.ERR_625, principal.getRealm() );
            throw new ServiceConfigurationException( message, ne );
        }
    }


    private Object execute( CoreSession session, DirectoryServiceOperation operation ) throws Exception
    {
        return operation.execute( session, null );
    }
}
