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
import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.xdbm.IndexEntry;

import java.util.*;


/**
 * A Cursor returning candidates satisfying a logical disjunction expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class OrCursor<E> extends AbstractCursor<IndexEntry<?,E>>
{
    private static final String UNSUPPORTED_MSG =
        "OrCursors are not ordered and do not support positioning by element.";
    private final Cursor<IndexEntry<?,E>>[] cursors;
    private final Evaluator[] evaluators;
    private final List<Set<Long>> blacklists;
    private int cursorIndex = -1;
    private boolean available = false;


    public OrCursor( Cursor<IndexEntry<?,E>>[] cursors, Evaluator[] evaluators )
    {
        if ( cursors.length <= 1 )
        {
            throw new IllegalArgumentException(
                "Must have 2 or more sub-expression Cursors for a disjunction" );
        }

        this.cursors = cursors;
        this.evaluators = evaluators;
        this.blacklists = new ArrayList<Set<Long>>();
        //noinspection ForLoopReplaceableByForEach
        for ( int ii = 0; ii < cursors.length; ii++ )
        {
            this.blacklists.add( new HashSet<Long>() );
        }
    }


    public boolean available()
    {
        return available;
    }


    public void before( IndexEntry<?, E> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void after( IndexEntry<?, E> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void beforeFirst() throws Exception
    {
        cursorIndex = 0;
        cursors[cursorIndex].beforeFirst();
        available = false;
    }


    public void afterLast() throws Exception
    {
        cursorIndex = cursors.length - 1;
        cursors[cursorIndex].afterLast();
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
    private void blackListIfDuplicate( IndexEntry<?,E> indexEntry ) throws Exception
    {
        for ( int ii = 0; ii < evaluators.length; ii++ )
        {
            if ( ii == cursorIndex )
            {
                continue;
            }

            //noinspection unchecked
            if ( evaluators[ii].evaluate( indexEntry ) )
            {
                blacklists.get( ii ).add( indexEntry.getId() );
            }
        }
    }


    public boolean previous() throws Exception
    {
        while ( cursors[cursorIndex].previous() )
        {
            IndexEntry<?,E> candidate = cursors[cursorIndex].get();
            if ( ! isBlackListed( candidate.getId() ) )
            {
                blackListIfDuplicate( candidate );
                return available = true;
            }
        }

        while ( cursorIndex > 0 )
        {
            cursorIndex--;
            cursors[cursorIndex].afterLast();

            while ( cursors[cursorIndex].previous() )
            {
                IndexEntry<?,E> candidate = cursors[cursorIndex].get();
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
        while ( cursors[cursorIndex].next() )
        {
            IndexEntry<?,E> candidate = cursors[cursorIndex].get();
            if ( ! isBlackListed( candidate.getId() ) )
            {
                blackListIfDuplicate( candidate );
                return available = true;
            }
        }

        while ( cursorIndex < cursors.length - 1 )
        {
            cursorIndex++;
            cursors[cursorIndex].beforeFirst();

            while ( cursors[cursorIndex].next() )
            {
                IndexEntry<?,E> candidate = cursors[cursorIndex].get();
                if ( ! isBlackListed( candidate.getId() ) )
                {
                    blackListIfDuplicate( candidate );
                    return available = true;
                }
            }
        }

        return available = false;
    }


    public IndexEntry<?, E> get() throws Exception
    {
        if ( available )
        {
            return cursors[cursorIndex].get();
        }

        throw new InvalidCursorPositionException( "Cursor has not been positioned yet." );
    }


    public boolean isElementReused()
    {
        return cursors[cursorIndex].isElementReused();
    }


    public void close() throws Exception
    {
        for ( Cursor cursor : cursors )
        {
            cursor.close();
        }
        super.close();
    }
}