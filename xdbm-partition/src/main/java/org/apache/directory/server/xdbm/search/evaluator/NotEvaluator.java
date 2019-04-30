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
package org.apache.directory.server.xdbm.search.evaluator;


import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.NotNode;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;


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


    /**
     * Creates a new NotEvaluator
     * 
     * @param node The NotNode
     * @param childEvaluator The included evaluator
     */
    public NotEvaluator( NotNode node, Evaluator<? extends ExprNode> childEvaluator )
    {
        this.node = node;
        this.childEvaluator = childEvaluator;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate( Entry entry ) throws LdapException
    {
        return !childEvaluator.evaluate( entry );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate( PartitionTxn partitionTxn, IndexEntry<?, String> indexEntry ) throws LdapException
    {
        return !childEvaluator.evaluate( partitionTxn, indexEntry );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public NotNode getExpression()
    {
        return node;
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "NotEvaluator : " ).append( node ).append( '\n' );

        sb.append( childEvaluator.toString( tabs + "  " ) );

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}
