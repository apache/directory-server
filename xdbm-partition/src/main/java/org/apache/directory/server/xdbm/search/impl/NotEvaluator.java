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


import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.NotNode;


/**
 * An Evaluator for logical negation (NOT) expressions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NotEvaluator implements Evaluator<NotNode>
{
    /** The ExprNode to evaluate */
    private final NotNode node;

    /** The Evaluator to use for the inner Node */
    private final Evaluator<? extends ExprNode> childEvaluator;


    public NotEvaluator( NotNode node, Evaluator<? extends ExprNode> childEvaluator )
    {
        this.node = node;
        this.childEvaluator = childEvaluator;
    }


    public boolean evaluateEntry( Entry entry ) throws Exception
    {
        return !childEvaluator.evaluateEntry( entry );
    }


    public boolean evaluate( IndexEntry<?> indexEntry ) throws Exception
    {
        return !childEvaluator.evaluate( indexEntry );
    }


    public NotNode getExpression()
    {
        return node;
    }
}
