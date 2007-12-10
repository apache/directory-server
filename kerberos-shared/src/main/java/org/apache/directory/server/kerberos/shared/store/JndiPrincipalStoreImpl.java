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
import javax.security.auth.kerberos.KerberosPrincipal;


/**
 * A JNDI-backed implementation of the PrincipalStore interface.  This PrincipalStore uses
 * the Strategy pattern to either serve principals based on a single base DN or to lookup
 * catalog mappings from configuration in the DIT.  The strategy is chosen based on the
 * presence of a catalog base DN.  If the catalog base DN is not present, the single
 * entry base DN is searched, instead.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class JndiPrincipalStoreImpl implements PrincipalStore
{
    /** a handle on the search strategy */
    private final PrincipalStore store;


    /**
     * Creates a new instance of JndiPrincipalStoreImpl.
     *
     * @param catalogBaseDn dn for a catalog of search dns.
     * @param searchBaseDn if no catalog, single search dn.
     * @param directoryService the core service
     */
    public JndiPrincipalStoreImpl( String catalogBaseDn, String searchBaseDn, DirectoryService directoryService )
    {
        store = getStore( catalogBaseDn, searchBaseDn, directoryService );
    }


    public String addPrincipal( PrincipalStoreEntry entry ) throws Exception
    {
        return store.addPrincipal( entry );
    }


    public String deletePrincipal( KerberosPrincipal principal ) throws Exception
    {
        return store.deletePrincipal( principal );
    }


    public PrincipalStoreEntry[] getAllPrincipals( String realm ) throws Exception
    {
        return store.getAllPrincipals( realm );
    }


    public PrincipalStoreEntry getPrincipal( KerberosPrincipal principal ) throws Exception
    {
        return store.getPrincipal( principal );
    }


    public String changePassword( KerberosPrincipal principal, String newPassword ) throws Exception
    {
        return store.changePassword( principal, newPassword );
    }


    private static PrincipalStore getStore( String catalogBaseDn, String searchBaseDn, DirectoryService directoryService )
    {
        if ( catalogBaseDn != null )
        {
            // build a catalog from the backing store
            return new MultiBaseSearch( catalogBaseDn, directoryService );
        }

        // search only the configured entry baseDN
        return new SingleBaseSearch( searchBaseDn, directoryService );
    }
}
