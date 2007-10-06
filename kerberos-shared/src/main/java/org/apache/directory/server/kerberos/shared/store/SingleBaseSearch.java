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
import org.apache.directory.server.protocol.shared.store.ContextOperation;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.auth.kerberos.KerberosPrincipal;
import java.util.Hashtable;


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
    private Hashtable<String, Object> env = new Hashtable<String, Object>();


    SingleBaseSearch( ServiceConfiguration config, DirectoryService directoryService )
    {
        env.put( Context.INITIAL_CONTEXT_FACTORY, config.getInitialContextFactory() );
        env.put( Context.PROVIDER_URL, config.getSearchBaseDn() );
        env.put( Context.SECURITY_AUTHENTICATION, config.getSecurityAuthentication() );
        env.put( Context.SECURITY_CREDENTIALS, config.getSecurityCredentials() );
        env.put( Context.SECURITY_PRINCIPAL, config.getSecurityPrincipal() );
        env.put( DirectoryService.JNDI_KEY, directoryService );

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


    public String changePassword( KerberosPrincipal principal, String newPassword ) throws Exception
    {
        return ( String ) execute( new ChangePassword( principal, newPassword ) );
    }


    private Object execute( ContextOperation operation ) throws Exception
    {
        if ( ctx == null )
        {
            try
            {
                ctx = new InitialDirContext( env );
            }
            catch ( NamingException ne )
            {
                String message = "Failed to get initial context " + env.get( Context.PROVIDER_URL );
                throw new ServiceConfigurationException( message, ne );
            }
        }

        return operation.execute( ctx, null );
    }
}
