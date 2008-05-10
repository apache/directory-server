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
package org.apache.directory.server.xdbm.search.impl;


import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.server.xdbm.IndexEntry;


/**
 * Evaluates candidate entries to see if they match a filter expression.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Evaluator<N extends ExprNode,E>
{
    /**
     * Evaluates a candidate to determine if a filter expression selects it.
     * 
     * @param entry the index record of the entry to evaluate
     * @return true if filter selects the candidate false otherwise
     * @throws Exception if there are database faults during evaluation
     */
    boolean evaluate( IndexEntry<?,E> entry ) throws Exception;


    /**
     * Gets the expression used by this expression Evaluator.
     *
     * @return the AST for the expression
     */
    N getExpression();
}
