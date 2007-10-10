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
import javax.naming.directory.DirContext;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.kerberos.shared.store.operations.AddPrincipal;
import org.apache.directory.server.kerberos.shared.store.operations.ChangePassword;
import org.apache.directory.server.kerberos.shared.store.operations.DeletePrincipal;
import org.apache.directory.server.kerberos.shared.store.operations.GetAllPrincipals;
import org.apache.directory.server.kerberos.shared.store.operations.GetPrincipal;
import org.apache.directory.server.protocol.shared.ServiceConfigurationException;
import org.apache.directory.server.protocol.shared.catalog.Catalog;
import org.apache.directory.server.protocol.shared.catalog.GetCatalog;
import org.apache.directory.server.protocol.shared.store.ContextOperation;


/**
 * A JNDI-backed search strategy implementation.  This search strategy builds a
 * catalog from configuration in the DIT to determine where realms are to search
 * for Kerberos principals.
 *
 * TODO are exception messages reasonable? I changed them to use the catalog key rather than the catalog value.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
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
            DirContext ctx = directoryService.getJndiContext(catalogBaseDn);
            catalog = new KerberosCatalog( ( Map ) execute( ctx, new GetCatalog() ) );
        }
        catch ( Exception e )
        {
            String message = "Failed to get catalog context " + catalogBaseDn;
            throw new ServiceConfigurationException( message, e );
        }
    }


    public String addPrincipal( PrincipalStoreEntry entry ) throws Exception
    {
        try
        {
            return ( String ) execute( getDirContext( entry.getRealmName() ), new AddPrincipal( entry ) );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + entry.getRealmName();
            throw new ServiceConfigurationException( message, ne );
        }
    }

    public String deletePrincipal( KerberosPrincipal principal ) throws Exception
    {
        try
        {
            return ( String ) execute( getDirContext( principal.getRealm() ), new DeletePrincipal( principal ) );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + principal.getRealm();
            throw new ServiceConfigurationException( message, ne );
        }
    }


    public PrincipalStoreEntry[] getAllPrincipals( String realm ) throws Exception
    {
        try
        {
            return ( PrincipalStoreEntry[] ) execute( getDirContext( realm ), new GetAllPrincipals() );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + realm;
            throw new ServiceConfigurationException( message, ne );
        }
    }


    public PrincipalStoreEntry getPrincipal( KerberosPrincipal principal ) throws Exception
    {
        try
        {
            return ( PrincipalStoreEntry ) execute( getDirContext( principal.getRealm() ), new GetPrincipal( principal ) );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + principal.getRealm();
            throw new ServiceConfigurationException( message, ne );
        }
    }


    public String changePassword( KerberosPrincipal principal, String newPassword ) throws Exception
    {
        try
        {
            return ( String ) execute( getDirContext( principal.getRealm() ), new ChangePassword( principal, newPassword ) );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + principal.getRealm();
            throw new ServiceConfigurationException( message, ne );
        }
    }


    private Object execute( DirContext ctx, ContextOperation operation ) throws Exception
    {
        return operation.execute( ctx, null );
    }

    private DirContext getDirContext( String name ) throws NamingException
    {
        return directoryService.getJndiContext(catalog.getBaseDn( name ));
    }

}
