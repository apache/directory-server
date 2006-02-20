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


import javax.naming.spi.InitialContextFactory;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.protocol.shared.ServiceConfiguration;


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
    /** a handle on the configuration */
    private ServiceConfiguration config;
    /** a handle on the provider factory */
    private InitialContextFactory factory;
    /** a handle on the search strategy */
    private PrincipalStore store;


    public JndiPrincipalStoreImpl(ServiceConfiguration config, InitialContextFactory factory)
    {
        this.config = config;
        this.factory = factory;

        store = getStore();
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


    public String changePassword( KerberosPrincipal principal, KerberosKey newKey ) throws Exception
    {
        return store.changePassword( principal, newKey );
    }


    private PrincipalStore getStore()
    {
        if ( config.getCatalogBaseDn() != null )
        {
            // build a catalog from the backing store
            return new MultiBaseSearch( config, factory );
        }

        // search only the configured entry baseDN
        return new SingleBaseSearch( config, factory );
    }
}
