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
    private Hashtable<String, String> env;
    private InitialContextFactory factory;


    SingleBaseSearch( ServiceConfiguration config, InitialContextFactory factory )
    {
        env = new Hashtable<String, String>( config.toJndiEnvironment() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, config.getInitialContextFactory() );
        env.put( Context.PROVIDER_URL, config.getEntryBaseDn() );

        this.factory = factory;
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
        if ( ctx == null )
        {
            try
            {
                ctx = ( DirContext ) factory.getInitialContext( env );
            }
            catch ( NamingException ne )
            {
                ne.printStackTrace();
                String message = "Failed to get initial context " + ( String ) env.get( Context.PROVIDER_URL );
                throw new ConfigurationException( message, ne );
            }
        }

        return operation.execute( ctx, null );
    }
}
