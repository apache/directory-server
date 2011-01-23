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
package org.apache.directory.server.xdbm.search;


import org.apache.directory.shared.ldap.constants.JndiPropertyConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.Dn;
import org.apache.directory.server.xdbm.IndexCursor;

import javax.naming.directory.SearchControls;


/**
 * Given a search filter and a scope the search engine identifies valid
 * candidate entries returning their ids.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface SearchEngine<E, ID>
{
    /**
     * @todo put this in the right place
     * The alias dereferencing mode key for JNDI providers 
     */
    String ALIASMODE_KEY = JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES;
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
     * @param aliasDerefMode the alias dereferencing mode to use
     * @param filter the search filter AST root
     * @param searchCtls the JNDI search controls
     * @return enumeration over SearchResults
     * @throws Exception if the search fails
     */
    IndexCursor<ID, E, ID> cursor( Dn base, AliasDerefMode aliasDerefMode, ExprNode filter,
        SearchControls searchCtls ) throws Exception;


    /**
     * Builds an Evaluator for a filter expression.
     * 
     * @param filter the filter root AST node
     * @return true if the filter passes the entry, false otherwise
     * @throws Exception if something goes wrong while accessing the db
     */
    Evaluator<? extends ExprNode, Entry, ID> evaluator( ExprNode filter ) throws Exception;
}