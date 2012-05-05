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


import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.shared.ldap.model.filter.ExprNode;


/**
 * Evaluates candidate entries to see if they match a filter expression.
 *
 * Evaluators contain various overloads to the evaluate method.  Often a
 * developer working in this region of the code may wonder when to use one
 * override verses another.  The overload taking an IndexEntry argument is
 * specifically suited for use when there is the possibility of multiple entry
 * lookups from the master table.  If the same candidate in the form of an
 * IndexEntry is evaluated more then this overload is more efficient since it
 * stores the looked up entry in the IndexEntry preventing multiple lookups.
 *
 * If the index entry is already populated with an entry object, and some
 * evaluation is required then it is preferrable to use the overload that
 * takes a Long id instead.  Likewise if it is known in advance that the
 * expression is a leaf node built on an indexed attribute then the overload
 * with the Long id argument is also preferrable unless an IndexEntry already
 * exists in which case it makes no difference.
 *
 * The overload taking the ServerEntry itself is a last resort option and ok
 * to use when it is known that no index exists for the attributes of
 * Evaluators based on leaf expressions.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface Evaluator<N extends ExprNode, E, ID>
{
    /**
     * Evaluates a candidate to determine if a filter expression selects it.
     * If an IndexEntry does has a null reference to the entry object, this
     * Evaluator may set it if it has to access the full entry within the
     * master table of the store.  Subsequent evaluations on the IndexEntry
     * then need not access the store to retreive the entry if they need to
     * access it's attributes.
     * 
     * @param entry the index record of the entry to evaluate
     * @return true if filter selects the candidate false otherwise
     * @throws Exception if there are faults during evaluation
     */
    boolean evaluate( IndexEntry<?, ID> entry ) throws Exception;


    /**
     * Evaluates whether or not a candidate, satisfies the expression
     * associated with this Evaluator .
     *
     * @param entry the candidate entry
     * @return true if filter selects the candidate false otherwise
     * @throws Exception if there are faults during evaluation
     */
    boolean evaluateEntry( E entry ) throws Exception;


    /**
     * Gets the expression used by this expression Evaluator.
     *
     * @return the AST for the expression
     */
    N getExpression();
}
