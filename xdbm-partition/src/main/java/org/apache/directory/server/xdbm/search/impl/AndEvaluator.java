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

import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.AndNode;
import org.apache.directory.shared.ldap.model.filter.ExprNode;


/**
 * An Evaluator for logical conjunction (AND) expressions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AndEvaluator<ID> implements Evaluator<AndNode, Entry, ID>
{
    /** The list of evaluators associated with each of the children */
    private final List<Evaluator<? extends ExprNode, Entry, ID>> evaluators;

    /** The AndNode */
    private final AndNode node;


    public AndEvaluator( AndNode node, List<Evaluator<? extends ExprNode, Entry, ID>> evaluators )
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
    private List<Evaluator<? extends ExprNode, Entry, ID>> optimize(
        List<Evaluator<? extends ExprNode, Entry, ID>> unoptimized )
    {
        List<Evaluator<? extends ExprNode, Entry, ID>> optimized = new ArrayList<Evaluator<? extends ExprNode, Entry, ID>>(
            unoptimized.size() );
        optimized.addAll( unoptimized );
        
        Collections.sort( optimized, new Comparator<Evaluator<?, Entry, ID>>()
        {
            public int compare( Evaluator<?, Entry, ID> e1, Evaluator<?, Entry, ID> e2 )
            {
                long scanCount1 = ( Long ) e1.getExpression().get( "count" );
                long scanCount2 = ( Long ) e2.getExpression().get( "count" );

                if ( scanCount1 == scanCount2 )
                {
                    return 0;
                }

                /*
                 * We want the Evaluator with the smallest scan count first
                 * since this node has the highest probability of failing, or
                 * rather the least probability of succeeding.  That way we
                 * can short the sub-expression evaluation process.
                 */
                if ( scanCount1 < scanCount2 )
                {
                    return -1;
                }

                return 1;
            }
        } );

        return optimized;
    }


    public boolean evaluateId( ID id ) throws Exception
    {
        for ( Evaluator<?, Entry, ID> evaluator : evaluators )
        {
            if ( !evaluator.evaluateId( id ) )
            {
                return false;
            }
        }

        return true;
    }


    public boolean evaluateEntry( Entry entry ) throws Exception
    {
        for ( Evaluator<?, Entry, ID> evaluator : evaluators )
        {
            if ( !evaluator.evaluateEntry( entry ) )
            {
                return false;
            }
        }

        return true;
    }


    public boolean evaluate( IndexEntry<?, Entry, ID> indexEntry ) throws Exception
    {
        for ( Evaluator<?, Entry, ID> evaluator : evaluators )
        {
            if ( !evaluator.evaluate( indexEntry ) )
            {
                return false;
            }
        }

        return true;
    }


    public AndNode getExpression()
    {
        return node;
    }
}
