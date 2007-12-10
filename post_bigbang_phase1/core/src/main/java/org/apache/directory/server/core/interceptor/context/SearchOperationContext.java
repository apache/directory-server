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


import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.directory.SearchControls;


/**
 * A Search context used for Interceptors. It contains all the informations
 * needed for the search operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SearchOperationContext extends AbstractOperationContext
{
    /** The filter */
    private ExprNode filter;
    
    /** The controls */
    private SearchControls searchControls;

    private AliasDerefMode aliasDerefMode;


    /**
     * Creates a new instance of SearchOperationContext.
     */
    public SearchOperationContext()
    {
    	super();
    }


    /**
     * Creates a new instance of SearchOperationContext.
     * @param aliasDerefMode the alias dereferencing mode
     * @param dn the dn of the search base
     * @param filter the filter AST to use for the search
     * @param searchControls the search controls
     */
    public SearchOperationContext( LdapDN dn, AliasDerefMode aliasDerefMode, ExprNode filter,
                                   SearchControls searchControls )
    {
        super( dn );
        this.filter = filter;
        this.aliasDerefMode = aliasDerefMode;
        this.searchControls = searchControls;
    }


    public ExprNode getFilter()
    {
        return filter;
    }


    public void setFilter( ExprNode filter )
    {
        this.filter = filter;
    }


    public SearchControls getSearchControls()
    {
        return searchControls;
    }


    public void setSearchControls( SearchControls searchControls )
    {
        this.searchControls = searchControls;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "SearchContext for DN '" + getDn().getUpName() + "', filter :'"
        + filter + "'"; 
    }


    public AliasDerefMode getAliasDerefMode()
    {
        return aliasDerefMode;
    }


    public void setAliasDerefMode( AliasDerefMode aliasDerefMode )
    {
        this.aliasDerefMode = aliasDerefMode;
    }
}
