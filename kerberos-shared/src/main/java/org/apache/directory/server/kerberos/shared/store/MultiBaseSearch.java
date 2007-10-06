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


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.kerberos.shared.store.operations.*;
import org.apache.directory.server.protocol.shared.ServiceConfiguration;
import org.apache.directory.server.protocol.shared.ServiceConfigurationException;
import org.apache.directory.server.protocol.shared.catalog.Catalog;
import org.apache.directory.server.protocol.shared.catalog.GetCatalog;
import org.apache.directory.server.protocol.shared.store.ContextOperation;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.auth.kerberos.KerberosPrincipal;
import java.util.Hashtable;
import java.util.Map;


/**
 * A JNDI-backed search strategy implementation.  This search strategy builds a
 * catalog from configuration in the DIT to determine where realms are to search
 * for Kerberos principals.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
class MultiBaseSearch implements PrincipalStore
{
    private Hashtable<String, Object> env = new Hashtable<String, Object>();
    private Catalog catalog;


    MultiBaseSearch( ServiceConfiguration config, DirectoryService directoryService )
    {
        env.put( Context.INITIAL_CONTEXT_FACTORY, config.getInitialContextFactory() );
        env.put( Context.PROVIDER_URL, config.getCatalogBaseDn() );
        env.put( DirectoryService.JNDI_KEY, directoryService );

        try
        {
            DirContext ctx = new InitialDirContext( env );
            catalog = new KerberosCatalog( ( Map ) execute( ctx, new GetCatalog() ) );
        }
        catch ( Exception e )
        {
            String message = "Failed to get catalog context " + env.get( Context.PROVIDER_URL );
            throw new ServiceConfigurationException( message, e );
        }
    }


    public String addPrincipal( PrincipalStoreEntry entry ) throws Exception
    {
        Hashtable<String, Object> cloned = new Hashtable<String, Object>();
        cloned.putAll( env );
        cloned.put( Context.PROVIDER_URL, catalog.getBaseDn( entry.getRealmName() ) );

        try
        {
            DirContext ctx = new InitialDirContext( cloned );
            return ( String ) execute( ctx, new AddPrincipal( entry ) );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + env.get( Context.PROVIDER_URL );
            throw new ServiceConfigurationException( message, ne );
        }
    }


    public String deletePrincipal( KerberosPrincipal principal ) throws Exception
    {
        Hashtable<String, Object> cloned = new Hashtable<String, Object>();
        cloned.putAll( env );
        cloned.put( Context.PROVIDER_URL, catalog.getBaseDn( principal.getRealm() ) );

        try
        {
            DirContext ctx = new InitialDirContext( cloned );
            return ( String ) execute( ctx, new DeletePrincipal( principal ) );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + env.get( Context.PROVIDER_URL );
            throw new ServiceConfigurationException( message, ne );
        }
    }


    public PrincipalStoreEntry[] getAllPrincipals( String realm ) throws Exception
    {
        Hashtable<String, Object> cloned = new Hashtable<String, Object>();
        cloned.putAll( env );
        cloned.put( Context.PROVIDER_URL, catalog.getBaseDn( realm ) );

        try
        {
            DirContext ctx = new InitialDirContext( cloned );
            return ( PrincipalStoreEntry[] ) execute( ctx, new GetAllPrincipals() );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + env.get( Context.PROVIDER_URL );
            throw new ServiceConfigurationException( message, ne );
        }
    }


    public PrincipalStoreEntry getPrincipal( KerberosPrincipal principal ) throws Exception
    {
        Hashtable<String, Object> cloned = new Hashtable<String, Object>();
        cloned.putAll( env );
        cloned.put( Context.PROVIDER_URL, catalog.getBaseDn( principal.getRealm() ) );

        try
        {
            DirContext ctx = new InitialDirContext( cloned );
            return ( PrincipalStoreEntry ) execute( ctx, new GetPrincipal( principal ) );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + env.get( Context.PROVIDER_URL );
            throw new ServiceConfigurationException( message, ne );
        }
    }


    public String changePassword( KerberosPrincipal principal, String newPassword ) throws Exception
    {
        Hashtable<String, Object> cloned = new Hashtable<String, Object>();
        cloned.putAll( env );
        cloned.put( Context.PROVIDER_URL, catalog.getBaseDn( principal.getRealm() ) );

        try
        {
            DirContext ctx = new InitialDirContext( cloned );
            return ( String ) execute( ctx, new ChangePassword( principal, newPassword ) );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + env.get( Context.PROVIDER_URL );
            throw new ServiceConfigurationException( message, ne );
        }
    }


    private Object execute( DirContext ctx, ContextOperation operation ) throws Exception
    {
        return operation.execute( ctx, null );
    }
}
