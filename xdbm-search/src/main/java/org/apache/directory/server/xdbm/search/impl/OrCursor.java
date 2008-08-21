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


import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.filter.ExprNode;

import java.util.*;


/**
 * A Cursor returning candidates satisfying a logical disjunction expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class OrCursor<V> extends AbstractIndexCursor<V, ServerEntry>
{
    private static final String UNSUPPORTED_MSG =
        "OrCursors are not ordered and do not support positioning by element.";
    private final List<IndexCursor<V,ServerEntry>> cursors;
    private final List<Evaluator<? extends ExprNode,ServerEntry>> evaluators;
    private final List<Set<Long>> blacklists;
    private int cursorIndex = -1;
    private boolean available = false;


    // TODO - do same evaluator fail fast optimization that we do in AndCursor
    public OrCursor( List<IndexCursor<V, ServerEntry>> cursors, List<Evaluator<? extends ExprNode,ServerEntry>> evaluators )
    {
        if ( cursors.size() <= 1 )
        {
            throw new IllegalArgumentException(
                "Must have 2 or more sub-expression Cursors for a disjunction" );
        }

        this.cursors = cursors;
        this.evaluators = evaluators;
        this.blacklists = new ArrayList<Set<Long>>();

        for ( int ii = 0; ii < cursors.size(); ii++ )
        {
            this.blacklists.add( new HashSet<Long>() );
        }
        this.cursorIndex = 0;
    }


    public boolean available()
    {
        return available;
    }


    public void before( IndexEntry<V, ServerEntry> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void after( IndexEntry<V, ServerEntry> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void beforeValue( Long id, V value ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void afterValue( Long id, V value ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
        cursorIndex = 0;
        cursors.get( cursorIndex ).beforeFirst();
        available = false;
    }


    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );
        cursorIndex = cursors.size() - 1;
        cursors.get( cursorIndex ).afterLast();
        available = false;
    }


    public boolean first() throws Exception
    {
        beforeFirst();
        return available = next();
    }


    public boolean last() throws Exception
    {
        afterLast();
        return available = previous();
    }


    private boolean isBlackListed( Long id )
    {
        return blacklists.get( cursorIndex ).contains( id );
    }


    /**
     * The first sub-expression Cursor to advance to an entry adds the entry
     * to the blacklists of other Cursors that might return that entry.
     *
     * @param indexEntry the index entry to blacklist
     * @throws Exception if there are problems accessing underlying db
     */
    private void blackListIfDuplicate( IndexEntry<?, ServerEntry> indexEntry ) throws Exception
    {
        for ( int ii = 0; ii < evaluators.size(); ii++ )
        {
            if ( ii == cursorIndex )
            {
                continue;
            }

            if ( evaluators.get( ii ).evaluate( indexEntry ) )
            {
                blacklists.get( ii ).add( indexEntry.getId() );
            }
        }
    }


    public boolean previous() throws Exception
    {
        while ( cursors.get( cursorIndex ).previous() )
        {
            checkNotClosed( "previous()" );
            IndexEntry<?,ServerEntry> candidate = cursors.get( cursorIndex ).get();
            if ( ! isBlackListed( candidate.getId() ) )
            {
                blackListIfDuplicate( candidate );
                return available = true;
            }
        }

        while ( cursorIndex > 0 )
        {
            checkNotClosed( "previous()" );
            cursorIndex--;
            cursors.get( cursorIndex ).afterLast();

            while ( cursors.get( cursorIndex ).previous() )
            {
                checkNotClosed( "previous()" );
                IndexEntry<?,ServerEntry> candidate = cursors.get( cursorIndex ).get();
                if ( ! isBlackListed( candidate.getId() ) )
                {
                    blackListIfDuplicate( candidate );
                    return available = true;
                }
            }
        }

        return available = false;
    }


    public boolean next() throws Exception
    {
        while ( cursors.get( cursorIndex ).next() )
        {
            checkNotClosed( "next()" );
            IndexEntry<?,ServerEntry> candidate = cursors.get( cursorIndex ).get();
            if ( ! isBlackListed( candidate.getId() ) )
            {
                blackListIfDuplicate( candidate );
                return available = true;
            }
        }

        while ( cursorIndex < cursors.size() - 1 )
        {
            checkNotClosed( "previous()" );
            cursorIndex++;
            cursors.get( cursorIndex ).beforeFirst();

            while ( cursors.get( cursorIndex ).next() )
            {
                checkNotClosed( "previous()" );
                IndexEntry<?,ServerEntry> candidate = cursors.get( cursorIndex ).get();
                if ( ! isBlackListed( candidate.getId() ) )
                {
                    blackListIfDuplicate( candidate );
                    return available = true;
                }
            }
        }

        return available = false;
    }


    public IndexEntry<V, ServerEntry> get() throws Exception
    {
        checkNotClosed( "get()" );
        if ( available )
        {
            return cursors.get( cursorIndex ).get();
        }

        throw new InvalidCursorPositionException( "Cursor has not been positioned yet." );
    }


    public boolean isElementReused()
    {
        return cursors.get( cursorIndex ).isElementReused();
    }


    public void close() throws Exception
    {
        super.close();
        for ( Cursor<?> cursor : cursors )
        {
            cursor.close();
        }
    }
}