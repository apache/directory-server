/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.shared.ldap.filter;

import javax.naming.directory.SearchControls;

/**
 * A search scope enumerated type.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum SearchScope
{
    OBJECT( SearchControls.OBJECT_SCOPE ), 
    ONELEVEL( SearchControls.ONELEVEL_SCOPE ), 
    SUBTREE( SearchControls.SUBTREE_SCOPE );
    
    /** 
     * The corresponding JNDI scope constant value as defined in 
     * SearchControls.
     * 
     * @see javax.naming.directory.SearchControls#OBJECT_SCOPE
     * @see javax.naming.directory.SearchControls#ONELEVEL_SCOPE
     * @see javax.naming.directory.SearchControls#SUBTREE_SCOPE
     */ 
    private final int jndiScope;
    

    /**
     * Creates a new instance of SearchScope based on the respective 
     * SearchControls scope constant.
     *
     * @param jndiScope the JNDI scope constant
     */
    private SearchScope( int jndiScope )
    {
        this.jndiScope = jndiScope;
    }


    /**
     * Gets the corresponding JNDI scope constant value as defined in 
     * SearchControls.
     * 
     * @return the jndiScope
     * @see javax.naming.directory.SearchControls#OBJECT_SCOPE
     * @see javax.naming.directory.SearchControls#ONELEVEL_SCOPE
     * @see javax.naming.directory.SearchControls#SUBTREE_SCOPE
     */
    public int getJndiScope()
    {
        return jndiScope;
    }
    
    
    /**
     * Gets the SearchScope enumerated type for the corresponding 
     * SearchControls scope setting.
     *
     * @param searchControls the search controls to get SearchScope for
     * @return the SearchScope enumerated type for the SearchControls
     */
    public static SearchScope getSearchScope( SearchControls searchControls )
    {
        SearchScope scope = OBJECT;
        
        switch( searchControls.getSearchScope() )
        {
            case( SearchControls.OBJECT_SCOPE ): 
                scope = OBJECT;
                break;
            case( SearchControls.ONELEVEL_SCOPE ):
                scope = ONELEVEL;
                break;
            case( SearchControls.SUBTREE_SCOPE ):
                scope = SUBTREE;
                break;
        }
        
        return scope;
    }
}
