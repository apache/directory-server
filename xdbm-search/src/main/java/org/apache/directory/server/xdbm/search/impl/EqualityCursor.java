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
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.entry.ServerEntry;


/**
 * A Cursor over entry candidates matching an equality assertion filter.  This
 * Cursor operates in two modes.  The first is when an index exists for the
 * attribute the equality assertion is built on.  The second is when the user
 * index for the assertion attribute does not exist.  Different Cursors are
 * used in each of these cases where the other remains null.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public class EqualityCursor extends AbstractCursor<IndexEntry<?, ServerEntry>>
{
    private static final String UNSUPPORTED_MSG =
        "EqualityCursors only support positioning by element when a user index exists on the asserted attribute.";

    /** An equality evaluator for candidates */
    private final EqualityEvaluator equalityEvaluator;

    /** Cursor over attribute entry matching filter: set when index present */
    private final Cursor<IndexEntry<?,ServerEntry>> userIdxCursor;

    /** NDN Cursor on all entries in  (set when no index on user attribute) */
    private final Cursor<IndexEntry<String,ServerEntry>> ndnIdxCursor;

    /** used only when ndnIdxCursor is used (no index on attribute) */
    private boolean available = false;


    public EqualityCursor( Store<ServerEntry> db, EqualityEvaluator equalityEvaluator ) throws Exception
    {
        this.equalityEvaluator = equalityEvaluator;

        String attribute = equalityEvaluator.getExpression().getAttribute();
        Object value = equalityEvaluator.getExpression().getValue();
        if ( db.hasUserIndexOn( attribute ) )
        {
            //noinspection unchecked
            userIdxCursor = db.getUserIndex( attribute ).forwardCursor( value );
            ndnIdxCursor = null;
        }
        else
        {
            ndnIdxCursor = db.getNdnIndex().forwardCursor();
            userIdxCursor = null;
        }
    }


    public boolean available()
    {
        if ( userIdxCursor != null )
        {
            return userIdxCursor.available();
        }

        return available;
    }


    public void before( IndexEntry<?, ServerEntry> element ) throws Exception
    {
        if ( userIdxCursor != null )
        {
            userIdxCursor.before( element );
        }
        else
        {
            throw new UnsupportedOperationException( UNSUPPORTED_MSG );
        }
    }


    public void after( IndexEntry<?, ServerEntry> element ) throws Exception
    {
        if ( userIdxCursor != null )
        {
            userIdxCursor.after( element );
        }
        else
        {
            throw new UnsupportedOperationException( UNSUPPORTED_MSG );
        }
    }


    public void beforeFirst() throws Exception
    {
        if ( userIdxCursor != null )
        {
            userIdxCursor.beforeFirst();
        }
        else
        {
            ndnIdxCursor.beforeFirst();
            available = false;
        }
    }


    public void afterLast() throws Exception
    {
        if ( userIdxCursor != null )
        {
            userIdxCursor.afterLast();
        }
        else
        {
            ndnIdxCursor.afterLast();
            available = false;
        }
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
        if ( userIdxCursor != null )
        {
            return userIdxCursor.previous();
        }

        while( ndnIdxCursor.previous() )
        {
            IndexEntry<?,ServerEntry> candidate = ndnIdxCursor.get();
            if ( equalityEvaluator.evaluate( candidate ) )
            {
                 return available = true;
            }
        }

        return available = false;
    }


    public boolean next() throws Exception
    {
        if ( userIdxCursor != null )
        {
            return userIdxCursor.next();
        }

        while( ndnIdxCursor.next() )
        {
            IndexEntry<?,ServerEntry> candidate = ndnIdxCursor.get();
            if ( equalityEvaluator.evaluate( candidate ) )
            {
                 return available = true;
            }
        }

        return available = false;
    }


    public IndexEntry<?, ServerEntry> get() throws Exception
    {
        if ( userIdxCursor != null )
        {
            return userIdxCursor.get();
        }

        if ( available )
        {
            return ndnIdxCursor.get();
        }

        throw new InvalidCursorPositionException( "Cursor has not been positioned yet." );
    }


    public boolean isElementReused()
    {
        if ( userIdxCursor != null )
        {
            return userIdxCursor.isElementReused();
        }

        return ndnIdxCursor.isElementReused();
    }


    public void close() throws Exception
    {
        super.close();

        if ( userIdxCursor != null )
        {
            userIdxCursor.close();
        }
        else
        {
            ndnIdxCursor.close();
        }
    }
}
