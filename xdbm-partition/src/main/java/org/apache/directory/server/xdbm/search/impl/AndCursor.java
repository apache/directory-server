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

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.filter.ExprNode;


/**
 * A Cursor returning candidates satisfying a logical conjunction expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AndCursor<V, ID> extends AbstractIndexCursor<V, Entry, ID>
{
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_707 );
    private final IndexCursor<V, Entry, ID> wrapped;
    private final List<Evaluator<? extends ExprNode, Entry, ID>> evaluators;
    private boolean available = false;


    public AndCursor( IndexCursor<V, Entry, ID> wrapped,
        List<Evaluator<? extends ExprNode, Entry, ID>> evaluators )
    {
        this.wrapped = wrapped;
        this.evaluators = optimize( evaluators );
    }


    public boolean available()
    {
        return available;
    }


    public void beforeValue( ID id, V value )
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void afterValue( ID id, V value )
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void before( IndexEntry<V, Entry, ID> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void after( IndexEntry<V, Entry, ID> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
        wrapped.beforeFirst();
        available = false;
    }


    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );
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
            checkNotClosed( "previous()" );

            IndexEntry<?, Entry, ID> candidate = wrapped.get();
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
            checkNotClosed( "next()" );
            IndexEntry<?, Entry, ID> candidate = wrapped.get();
            
            if ( matches( candidate ) )
            {
                return available = true;
            }
        }

        return available = false;
    }


    public IndexEntry<V, Entry, ID> get() throws Exception
    {
        checkNotClosed( "get()" );
        if ( available )
        {
            return wrapped.get();
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
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


    private boolean matches( IndexEntry<?, Entry, ID> indexEntry ) throws Exception
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
}
