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

import javax.naming.directory.DirContext;
import javax.naming.NamingException;
import javax.security.auth.kerberos.KerberosPrincipal;


/**
 * A JNDI-backed search strategy implementation.  This search strategy searches a
 * single base DN for Kerberos principals.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
class SingleBaseSearch implements PrincipalStore
{
    private final DirContext ctx;


    SingleBaseSearch( String searchBaseDn, DirectoryService directoryService )
    {
        try
        {
            ctx = directoryService.getJndiContext(searchBaseDn);
        } catch ( NamingException e )
        {
            throw new IllegalStateException("Can't get context at" + searchBaseDn);
        }
//        env.put( Context.INITIAL_CONTEXT_FACTORY, config.getInitialContextFactory() );
//        env.put( Context.PROVIDER_URL, config.getSearchBaseDn() );
//        env.put( Context.SECURITY_AUTHENTICATION, config.getSecurityAuthentication() );
//        env.put( Context.SECURITY_CREDENTIALS, config.getSecurityCredentials() );
//        env.put( Context.SECURITY_PRINCIPAL, config.getSecurityPrincipal() );
//        env.put( DirectoryService.JNDI_KEY, directoryService );

    }


    public String addPrincipal( PrincipalStoreEntry entry ) throws Exception
    {
        return ( String ) new AddPrincipal( entry ).execute( ctx, null );
    }


    public String deletePrincipal( KerberosPrincipal principal ) throws Exception
    {
        return ( String ) new DeletePrincipal( principal ).execute( ctx, null );
    }


    public PrincipalStoreEntry[] getAllPrincipals( String realm ) throws Exception
    {
        return ( PrincipalStoreEntry[] ) new GetAllPrincipals().execute( ctx, null );
    }


    public PrincipalStoreEntry getPrincipal( KerberosPrincipal principal ) throws Exception
    {
        return ( PrincipalStoreEntry ) new GetPrincipal( principal ).execute( ctx, null );
    }


    public String changePassword( KerberosPrincipal principal, String newPassword ) throws Exception
    {
        return ( String ) new ChangePassword( principal, newPassword ).execute( ctx, null );
    }


}
