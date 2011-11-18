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
import java.util.List;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.core.api.partition.index.AbstractIndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;


/**
 * A Cursor returning candidates satisfying a logical conjunction expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AndCursor<V> extends AbstractIndexCursor<V>
{
    /** The message for unsupported operations */
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_707 );
    
    /** */
    private final IndexCursor<V> wrapped;
    
    /** The evaluators used for the members of the And filter */
    private final List<Evaluator<? extends ExprNode>> evaluators;


    /**
     * Creates an instance of a AndCursor. It wraps an index cursor and the list
     * of evaluators associated with all the elements connected by the And.
     * 
     * @param wrapped The encapsulated IndexCursor
     * @param evaluators The list of evaluators associated wth the elements
     */
    public AndCursor( IndexCursor<V> wrapped,
        List<Evaluator<? extends ExprNode>> evaluators )
    {
        this.wrapped = wrapped;
        this.evaluators = optimize( evaluators );
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }

    
    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
        wrapped.beforeFirst();
        setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );
        wrapped.afterLast();
        setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws Exception
    {
        beforeFirst();
        
        return next();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception
    {
        afterLast();
        
        return previous();
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        while ( wrapped.previous() )
        {
            checkNotClosed( "previous()" );

            IndexEntry<V> candidate = wrapped.get();
            
            if ( matches( candidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        while ( wrapped.next() )
        {
            checkNotClosed( "next()" );
            IndexEntry<V> candidate = wrapped.get();
            
            if ( matches( candidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false);
    }


    /**
     * {@inheritDoc}
     */
    public IndexEntry<V> get() throws Exception
    {
        checkNotClosed( "get()" );
        
        if ( available() )
        {
            return wrapped.get();
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void close() throws Exception
    {
        super.close();
        wrapped.close();
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
     * Checks if the entry is a valid candidate by using the evaluators.
     */
    private boolean matches( IndexEntry<V> indexEntry ) throws Exception
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
}
