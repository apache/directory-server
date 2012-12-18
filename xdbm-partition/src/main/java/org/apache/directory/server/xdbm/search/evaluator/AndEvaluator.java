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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.xdbm.search.impl.ScanCountComparator;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.AndNode;
import org.apache.directory.shared.ldap.model.filter.ExprNode;


/**
 * An Evaluator for logical conjunction (AND) expressions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AndEvaluator implements Evaluator<AndNode>
{
    /** The list of evaluators associated with each of the children */
    private final List<Evaluator<? extends ExprNode>> evaluators;

    /** The AndNode */
    private final AndNode node;


    /**
     * Creates an instance of AndEvaluator
     * @param node The And Node
     * @param evaluators The list of evaluators for all the contaned nodes
     */
    public AndEvaluator( AndNode node, List<Evaluator<? extends ExprNode>> evaluators )
    {
        this.node = node;
        this.evaluators = optimize( evaluators );
    }


    /**
     * Takes a set of Evaluators and copies then sorts them in a new list with
     * increasing scan counts on their expression nodes.  This is done to have
     * the Evaluators with the least scan count which have the highest
     * probability of rejecting a candidate first.  That will increase the
     * chance of shorting the checks on evaluators early so extra lookups and
     * comparisons are avoided.
     *
     * @param unoptimized the unoptimized list of Evaluators
     * @return optimized Evaluator list with increasing scan count ordering
     */
    private List<Evaluator<? extends ExprNode>> optimize(
        List<Evaluator<? extends ExprNode>> unoptimized )
    {
        List<Evaluator<? extends ExprNode>> optimized = new ArrayList<Evaluator<? extends ExprNode>>(
            unoptimized.size() );
        optimized.addAll( unoptimized );

        Collections.sort( optimized, new ScanCountComparator() );

        return optimized;
    }


    /**
     * {@inheritDoc}
     */
    public boolean evaluate( Entry entry ) throws Exception
    {
        for ( Evaluator<?> evaluator : evaluators )
        {
            if ( !evaluator.evaluate( entry ) )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean evaluate( IndexEntry<?, String> indexEntry ) throws LdapException
    {
        for ( Evaluator<?> evaluator : evaluators )
        {
            if ( !evaluator.evaluate( indexEntry ) )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public AndNode getExpression()
    {
        return node;
    }


    /**
     * Dumps the evaluators
     */
    private String dumpEvaluators( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        for ( Evaluator<? extends ExprNode> evaluator : evaluators )
        {
            sb.append( evaluator.toString( tabs + "  " ) );
        }

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "AndEvaluator : " ).append( node ).append( "\n" );

        if ( ( evaluators != null ) && ( evaluators.size() > 0 ) )
        {
            sb.append( dumpEvaluators( tabs + "  " ) ).append( "\n" );
        }

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
