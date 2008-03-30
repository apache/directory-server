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


import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.filter.ExprNode;

import javax.naming.directory.Attributes;
import java.util.*;


/**
 * A Cursor returning candidates satisfying a logical conjunction expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AndCursor extends AbstractCursor<IndexEntry<?,Attributes>>
{
    private static final String UNSUPPORTED_MSG =
        "AndCursors are not ordered and do not support positioning by element.";
    private final Cursor<IndexEntry<?,Attributes>> wrapped;
    private final List<Evaluator<? extends ExprNode, Attributes>> evaluators;
    private boolean available = false;


    public AndCursor( Cursor<IndexEntry<?, Attributes>> wrapped,
                      List<Evaluator<? extends ExprNode, Attributes>> evaluators )
    {
        this.wrapped = wrapped;
        this.evaluators = optimize( evaluators );
    }


    public boolean available()
    {
        return available;
    }


    public void before( IndexEntry<?, Attributes> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void after( IndexEntry<?, Attributes> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void beforeFirst() throws Exception
    {
        wrapped.beforeFirst();
        available = false;
    }


    public void afterLast() throws Exception
    {
        wrapped.afterLast();
        available = false;
    }


    public boolean first() throws Exception
    {
        beforeFirst();
        return next();
    }


    public boolean last() throws Exception
    {
        afterLast();
        return previous();
    }


    public boolean previous() throws Exception
    {
        while ( wrapped.previous() )
        {
            IndexEntry<?,Attributes> candidate = wrapped.get();
            if ( matches( candidate ) )
            {
                return available = true;
            }
        }

        return available = false;
    }


    public boolean next() throws Exception
    {
        while ( wrapped.next() )
        {
            IndexEntry<?,Attributes> candidate = wrapped.get();
            if ( matches( candidate ) )
            {
                return available = true;
            }
        }

        return available = false;
    }


    public IndexEntry<?, Attributes> get() throws Exception
    {
        if ( available )
        {
            return wrapped.get();
        }

        throw new InvalidCursorPositionException( "Cursor has not been positioned yet." );
    }


    public boolean isElementReused()
    {
        return wrapped.isElementReused();
    }


    public void close() throws Exception
    {
        super.close();
        wrapped.close();
    }


    /**
     * TODO - duplicate code from AndEvaluator just make utility for this and
     * for the same code in the OrEvaluator once done.
     *
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
    private List<Evaluator<? extends ExprNode,Attributes>>
        optimize( List<Evaluator<? extends ExprNode,Attributes>> unoptimized )
    {
        List<Evaluator<? extends ExprNode,Attributes>> optimized =
            new ArrayList<Evaluator<? extends ExprNode,Attributes>>( unoptimized.size() );
        optimized.addAll( unoptimized );
        Collections.sort( optimized, new Comparator<Evaluator<?,Attributes>>()
        {
            public int compare( Evaluator<?, Attributes> e1, Evaluator<?, Attributes> e2 )
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


    private boolean matches( IndexEntry<?, Attributes> indexEntry ) throws Exception
    {
        for ( Evaluator<?,Attributes> evaluator : evaluators )
        {
            if ( ! evaluator.evaluate( indexEntry ) )
            {
                return false;
            }
        }

        return true;
    }
}
