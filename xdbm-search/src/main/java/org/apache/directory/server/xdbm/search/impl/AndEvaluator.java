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


import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.core.entry.ServerEntry;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/**
 * An Evaluator for logical conjunction (AND) expressions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class AndEvaluator implements Evaluator<AndNode, ServerEntry>
{
    private final List<Evaluator<? extends ExprNode,ServerEntry>> evaluators;

    private final AndNode node;


    public AndEvaluator( AndNode node, List<Evaluator<? extends ExprNode, ServerEntry>> evaluators )
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
    private List<Evaluator<? extends ExprNode,ServerEntry>>
        optimize( List<Evaluator<? extends ExprNode,ServerEntry>> unoptimized )
    {
        List<Evaluator<? extends ExprNode,ServerEntry>> optimized =
            new ArrayList<Evaluator<? extends ExprNode,ServerEntry>>( unoptimized.size() );
        optimized.addAll( unoptimized );
        Collections.sort( optimized, new Comparator<Evaluator<?,ServerEntry>>()
        {
            public int compare( Evaluator<?, ServerEntry> e1, Evaluator<?, ServerEntry> e2 )
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
        });

        return optimized;
    }


    public boolean evaluate( Long id ) throws Exception
    {
        for ( Evaluator<?,ServerEntry> evaluator : evaluators )
        {
            if ( ! evaluator.evaluate( id ) )
            {
                return false;
            }
        }

        return true;
    }


    public boolean evaluate( ServerEntry entry ) throws Exception
    {
        for ( Evaluator<?,ServerEntry> evaluator : evaluators )
        {
            if ( ! evaluator.evaluate( entry ) )
            {
                return false;
            }
        }

        return true;
    }


    public boolean evaluate( IndexEntry<?, ServerEntry> indexEntry ) throws Exception
    {
        for ( Evaluator<?,ServerEntry> evaluator : evaluators )
        {
            if ( ! evaluator.evaluate( indexEntry ) )
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
