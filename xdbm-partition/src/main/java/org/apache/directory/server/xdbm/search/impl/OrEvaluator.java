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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.OrNode;


/**
 * An Evaluator for logical disjunction (OR) expressions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OrEvaluator implements Evaluator<OrNode>
{
    /** The list of evaluators associated with each of the children */
    private final List<Evaluator<? extends ExprNode>> evaluators;

    /** The OrNode */
    private final OrNode node;


    public OrEvaluator( OrNode node, List<Evaluator<? extends ExprNode>> evaluators )
    {
        this.node = node;
        this.evaluators = optimize( evaluators );
    }


    /**
     * Takes a set of Evaluators and copies then sorts them in a new list with
     * decreasing scan counts on their expression nodes.  This is done to have
     * the Evaluators with the greatest scan counts which have the highest
     * probability of accepting a candidate first.  That will increase the
     * chance of shorting the checks on evaluators early so extra lookups and
     * comparisons are avoided.
     *
     * @param unoptimized the unoptimized list of Evaluators
     * @return optimized Evaluator list with decreasing scan count ordering
     */
    private List<Evaluator<? extends ExprNode>> optimize(
        List<Evaluator<? extends ExprNode>> unoptimized )
    {
        List<Evaluator<? extends ExprNode>> optimized = new ArrayList<Evaluator<? extends ExprNode>>(
            unoptimized.size() );
        optimized.addAll( unoptimized );
        Collections.sort( optimized, new Comparator<Evaluator<? extends ExprNode>>()
        {
            public int compare( Evaluator<? extends ExprNode> e1,
                Evaluator<? extends ExprNode> e2 )
            {
                long scanCount1 = ( Long ) e1.getExpression().get( "count" );
                long scanCount2 = ( Long ) e2.getExpression().get( "count" );

                if ( scanCount1 == scanCount2 )
                {
                    return 0;
                }

                /*
                 * We want the Evaluator with the largest scan count first
                 * since this node has the highest probability of accepting,
                 * or rather the least probability of failing.  That way we
                 * can short the sub-expression evaluation process.
                 */
                if ( scanCount1 < scanCount2 )
                {
                    return 1;
                }

                return -1;
            }
        } );

        return optimized;
    }


    public boolean evaluate( IndexEntry<?> indexEntry ) throws Exception
    {
        for ( Evaluator<?> evaluator : evaluators )
        {
            if ( evaluator.evaluate( indexEntry ) )
            {
                return true;
            }
        }

        return false;
    }


    public boolean evaluateEntry( Entry entry ) throws Exception
    {
        for ( Evaluator<?> evaluator : evaluators )
        {
            if ( evaluator.evaluateEntry( entry ) )
            {
                return true;
            }
        }

        return false;
    }


    public OrNode getExpression()
    {
        return node;
    }
}