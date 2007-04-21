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

import java.util.Map;

import javax.naming.directory.SearchControls;

import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * A Search context used for Interceptors. It contains all the informations
 * needed for the search operation, and used by all the interceptors
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SearchOperationContext extends AbstractOperationContext
{
    /** The search environment type */
    private Map env;
    
    /** The filter */
    private ExprNode filter;
    
    /** The controls */
    private SearchControls searchControls;

    /**
     * 
     * Creates a new instance of SearchOperationContext.
     *
     */
    public SearchOperationContext()
    {
    	super();
    }

    /**
     * 
     * Creates a new instance of SearchOperationContext.
     *
     */
    public SearchOperationContext( LdapDN dn, Map env, ExprNode filter, SearchControls searchControls )
    {
        super( dn );
        this.env = env;
        this.filter = filter;
        this.searchControls = searchControls;
    }

    public Map getEnv()
    {
        return env;
    }

    public void setEnv( Map env )
    {
        this.env = env;
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

}
