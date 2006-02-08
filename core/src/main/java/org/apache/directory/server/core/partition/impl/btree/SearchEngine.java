/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.server.core.partition.impl.btree;


import java.math.BigInteger;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.apache.directory.shared.ldap.filter.ExprNode;


/**
 * Given a search filter and a scope the search engine identifies valid
 * candidate entries returning their ids.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface SearchEngine 
{
    /**
     * @todo put this in the right place
     * The alias dereferencing mode key for JNDI providers 
     */
    String ALIASMODE_KEY = "java.naming.ldap.derefAliases";
    /** 
     * @todo put this in the right place
     * The alias dereferencing mode value for JNDI providers 
     */
    String ALWAYS = "always";
    /** 
     * @todo put this in the right place
     * The alias dereferencing mode value for JNDI providers 
     */
    String NEVER = "never";
    /** 
     * @todo put this in the right place
     * The alias dereferencing mode value for JNDI providers 
     */
    String FINDING = "finding";
    /** 
     * @todo put this in the right place
     * The alias dereferencing mode value for JNDI providers 
     */
    String SEARCHING = "searching";
    
    /**
     * Gets the optimizer for this DefaultSearchEngine.
     *
     * @return the optimizer
     */
    Optimizer getOptimizer();
    
    /**
     * Conducts a search on a database.
     * 
     * @param base the search base
     * @param env the environment for the search
     * @param filter the search filter AST root
     * @param searchCtls the JNDI search controls
     * @return enumeration over SearchResults
     * @throws NamingException if the search fails
     */
    NamingEnumeration search( Name base, Map env, ExprNode filter,
        SearchControls searchCtls ) throws NamingException;

    /**
     * Evaluates a filter on an entry with a id.
     * 
     * @param filter the filter root AST node
     * @param id the id of the entry to test
     * @return true if the filter passes the entry, false otherwise
     * @throws NamingException if something goes wrong while accessing the db
     */
    boolean evaluate( ExprNode filter, BigInteger id )  throws NamingException;
}