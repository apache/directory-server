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

package org.apache.kerberos.store;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.spi.InitialContextFactory;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.core.configuration.ConfigurationException;
import org.apache.directory.server.protocol.shared.ServiceConfiguration;
import org.apache.directory.server.protocol.shared.catalog.Catalog;
import org.apache.directory.server.protocol.shared.catalog.GetCatalog;
import org.apache.directory.server.protocol.shared.store.ContextOperation;
import org.apache.kerberos.store.operations.AddPrincipal;
import org.apache.kerberos.store.operations.ChangePassword;
import org.apache.kerberos.store.operations.DeletePrincipal;
import org.apache.kerberos.store.operations.GetAllPrincipals;
import org.apache.kerberos.store.operations.GetPrincipal;

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
    private InitialContextFactory factory;
    private Hashtable env;

    private Catalog catalog;

    MultiBaseSearch( ServiceConfiguration config, InitialContextFactory factory )
    {
        this.factory = factory;

        env = new Hashtable( config.toJndiEnvironment() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, config.getInitialContextFactory() );
        env.put( Context.PROVIDER_URL, config.getCatalogBaseDn() );

        try
        {
            DirContext ctx = (DirContext) factory.getInitialContext( env );
            catalog = new KerberosCatalog( (Map) execute( ctx, new GetCatalog() ) );
        }
        catch ( Exception e )
        {
            String message = "Failed to get catalog context " + (String) env.get( Context.PROVIDER_URL );
            throw new ConfigurationException( message, e );
        }
    }

    public String addPrincipal( PrincipalStoreEntry entry ) throws Exception
    {
        env.put( Context.PROVIDER_URL, catalog.getBaseDn( entry.getRealmName() ) );

        try
        {
            DirContext ctx = (DirContext) factory.getInitialContext( env );
            return (String) execute( ctx, new AddPrincipal( entry ) );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + (String) env.get( Context.PROVIDER_URL );
            throw new ConfigurationException( message, ne );
        }
    }

    public String deletePrincipal( KerberosPrincipal principal ) throws Exception
    {
        env.put( Context.PROVIDER_URL, catalog.getBaseDn( principal.getRealm() ) );

        try
        {
            DirContext ctx = (DirContext) factory.getInitialContext( env );
            return (String) execute( ctx, new DeletePrincipal( principal ) );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + (String) env.get( Context.PROVIDER_URL );
            throw new ConfigurationException( message, ne );
        }
    }

    public PrincipalStoreEntry[] getAllPrincipals( String realm ) throws Exception
    {
        env.put( Context.PROVIDER_URL, catalog.getBaseDn( realm ) );

        try
        {
            DirContext ctx = (DirContext) factory.getInitialContext( env );
            return (PrincipalStoreEntry[]) execute( ctx, new GetAllPrincipals() );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + (String) env.get( Context.PROVIDER_URL );
            throw new ConfigurationException( message, ne );
        }
    }

    public PrincipalStoreEntry getPrincipal( KerberosPrincipal principal ) throws Exception
    {
        env.put( Context.PROVIDER_URL, catalog.getBaseDn( principal.getRealm() ) );

        try
        {
            DirContext ctx = (DirContext) factory.getInitialContext( env );
            return (PrincipalStoreEntry) execute( ctx, new GetPrincipal( principal ) );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + (String) env.get( Context.PROVIDER_URL );
            throw new ConfigurationException( message, ne );
        }
    }

    public String changePassword( KerberosPrincipal principal, KerberosKey newKey ) throws Exception
    {
        env.put( Context.PROVIDER_URL, catalog.getBaseDn( principal.getRealm() ) );

        try
        {
            DirContext ctx = (DirContext) factory.getInitialContext( env );
            return (String) execute( ctx, new ChangePassword( principal, newKey ) );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + (String) env.get( Context.PROVIDER_URL );
            throw new ConfigurationException( message, ne );
        }
    }

    private Object execute( DirContext ctx, ContextOperation operation ) throws Exception
    {
        return operation.execute( ctx, null );
    }
}
