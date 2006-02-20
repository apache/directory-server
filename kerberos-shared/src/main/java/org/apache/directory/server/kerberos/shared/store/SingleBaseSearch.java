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

package org.apache.directory.server.kerberos.shared.store;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.spi.InitialContextFactory;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.core.configuration.ConfigurationException;
import org.apache.directory.server.kerberos.shared.store.operations.AddPrincipal;
import org.apache.directory.server.kerberos.shared.store.operations.ChangePassword;
import org.apache.directory.server.kerberos.shared.store.operations.DeletePrincipal;
import org.apache.directory.server.kerberos.shared.store.operations.GetAllPrincipals;
import org.apache.directory.server.kerberos.shared.store.operations.GetPrincipal;
import org.apache.directory.server.protocol.shared.ServiceConfiguration;
import org.apache.directory.server.protocol.shared.store.ContextOperation;


/**
 * A JNDI-backed search strategy implementation.  This search strategy searches a
 * single base DN for Kerberos principals.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
class SingleBaseSearch implements PrincipalStore
{
    private DirContext ctx;


    SingleBaseSearch(ServiceConfiguration config, InitialContextFactory factory)
    {
        Hashtable env = new Hashtable( config.toJndiEnvironment() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, config.getInitialContextFactory() );
        env.put( Context.PROVIDER_URL, config.getEntryBaseDn() );

        try
        {
            ctx = ( DirContext ) factory.getInitialContext( env );
        }
        catch ( NamingException ne )
        {
            String message = "Failed to get initial context " + ( String ) env.get( Context.PROVIDER_URL );
            throw new ConfigurationException( message, ne );
        }
    }


    public String addPrincipal( PrincipalStoreEntry entry ) throws Exception
    {
        return ( String ) execute( new AddPrincipal( entry ) );
    }


    public String deletePrincipal( KerberosPrincipal principal ) throws Exception
    {
        return ( String ) execute( new DeletePrincipal( principal ) );
    }


    public PrincipalStoreEntry[] getAllPrincipals( String realm ) throws Exception
    {
        return ( PrincipalStoreEntry[] ) execute( new GetAllPrincipals() );
    }


    public PrincipalStoreEntry getPrincipal( KerberosPrincipal principal ) throws Exception
    {
        return ( PrincipalStoreEntry ) execute( new GetPrincipal( principal ) );
    }


    public String changePassword( KerberosPrincipal principal, KerberosKey newKey ) throws Exception
    {
        return ( String ) execute( new ChangePassword( principal, newKey ) );
    }


    private Object execute( ContextOperation operation ) throws Exception
    {
        return operation.execute( ctx, null );
    }
}
