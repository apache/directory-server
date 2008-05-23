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
package org.apache.directory.server.core.interceptor.context;


import javax.naming.directory.SearchControls;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A context used for search related operations and used by all 
 * the Interceptors.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class SearchingOperationContext extends AbstractOperationContext
{
    private AliasDerefMode aliasDerefMode = AliasDerefMode.DEREF_ALWAYS;

    /** The controls */
    protected SearchControls searchControls = new SearchControls();

    
    /**
     * Creates a new instance of ListOperationContext.
     */
    public SearchingOperationContext( Registries registries )
    {
        super( registries );
    }


    /**
     * Creates a new instance of ListOperationContext.
     *
     * @param dn The DN to get the suffix from
     */
    public SearchingOperationContext( Registries registries, LdapDN dn )
    {
        super( registries, dn );
    }


    /**
     * Creates a new instance of ListOperationContext.
     *
     * @param dn The DN to get the suffix from
     * @param aliasDerefMode the alias dereferencing mode to use
     */
    public SearchingOperationContext( Registries registries, LdapDN dn, AliasDerefMode aliasDerefMode )
    {
        super( registries, dn );
        this.aliasDerefMode = aliasDerefMode;
    }

    
    /**
     * Creates a new instance of ListOperationContext.
     *
     * @param dn The DN to get the suffix from
     * @param aliasDerefMode the alias dereferencing mode to use
     */
    public SearchingOperationContext( Registries registries, LdapDN dn, AliasDerefMode aliasDerefMode, 
        SearchControls searchControls )
    {
        super( registries, dn );
        this.aliasDerefMode = aliasDerefMode;
        this.searchControls = searchControls;
    }

    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "ListOperationContext with DN '" + getDn().getUpName() + "'";
    }


    /**
     *  @return The search controls
     */
    public SearchControls getSearchControls()
    {
        return searchControls;
    }


    /**
     * Set the search controls
     *
     * @param searchControls The search controls
     */
    public void setSearchControls( SearchControls searchControls )
    {
        this.searchControls = searchControls;
    }

    
    public AliasDerefMode getAliasDerefMode()
    {
        return aliasDerefMode;
    }
}
