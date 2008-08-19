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


import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.xdbm.*;


/**
 * A Cursor traversing candidates matching a Substring assertion expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubstringCursor extends AbstractIndexCursor<String, ServerEntry>
{
    private static final String UNSUPPORTED_MSG =
        "SubstringCursors may not be ordered and do not support positioning by element.";
    private final boolean hasIndex;
    private final IndexCursor<String,ServerEntry> wrapped;
    private final SubstringEvaluator evaluator;
    private final ForwardIndexEntry<String,ServerEntry> indexEntry =
        new ForwardIndexEntry<String,ServerEntry>();
    private boolean available = false;


    @SuppressWarnings("unchecked")
    public SubstringCursor( Store<ServerEntry> db,
                            final SubstringEvaluator substringEvaluator ) throws Exception
    {
        evaluator = substringEvaluator;
        hasIndex = db.hasUserIndexOn( evaluator.getExpression().getAttribute() );

        if ( hasIndex )
        {
            wrapped = ( ( Index<String,ServerEntry> ) db.getUserIndex( evaluator.getExpression().getAttribute() ) )
                .forwardCursor();
        }
        else
        {
            /*
             * There is no index on the attribute here.  We have no choice but
             * to perform a full table scan but need to leverage an index for the
             * wrapped Cursor.  We know that all entries are listed under
             * the ndn index and so this will enumerate over all entries.  The
             * substringEvaluator is used in an assertion to constrain the
             * result set to only those entries matching the pattern.  The
             * substringEvaluator handles all the details of normalization and
             * knows to use it, when it itself detects the lack of an index on
             * the node's attribute.
             */
            wrapped = db.getNdnIndex().forwardCursor();
        }
    }


    public boolean available()
    {
        return available;
    }


    public void beforeValue( Long id, String value ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void afterValue( Long id, String value ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void before( IndexEntry<String, ServerEntry> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void after( IndexEntry<String, ServerEntry> element ) throws Exception
    {
        throw new UnsupportedOperationException( UNSUPPORTED_MSG );
    }


    public void beforeFirst() throws Exception
    {
        checkClosed( "beforeFirst()" );
        if ( evaluator.getExpression().getInitial() != null && hasIndex )
        {
            ForwardIndexEntry<String,ServerEntry> indexEntry = new ForwardIndexEntry<String,ServerEntry>();
            indexEntry.setValue( evaluator.getExpression().getInitial() );
            wrapped.before( indexEntry );
        }
        else
        {
            wrapped.beforeFirst();
        }

        clear();
    }


    private void clear()
    {
        available = false;
        indexEntry.setObject( null );
        indexEntry.setId( null );
        indexEntry.setValue( null );
    }


    public void afterLast() throws Exception
    {
        checkClosed( "afterLast()" );

        // to keep the cursor always *after* the last matched tuple
        // This fixes an issue if the last matched tuple is also the last record present in the 
        // index. In this case the wrapped cursor is positioning on the last tuple instead of positioning after that
        wrapped.afterLast();
        clear();
    }


    public boolean first() throws Exception
    {
        beforeFirst();
        return next();
    }


    private boolean evaluateCandidate( IndexEntry<String,ServerEntry> indexEntry ) throws Exception
    {
        if ( hasIndex )
        {
            return evaluator.getPattern().matcher( indexEntry.getValue() ).matches();
        }
        else
        {
            return evaluator.evaluate( indexEntry );
        }
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
            checkClosed( "previous()" );
            IndexEntry<String,ServerEntry> entry = wrapped.get();
            if ( evaluateCandidate( entry ) )
            {
                available = true;
                this.indexEntry.setId( entry.getId() );
                this.indexEntry.setValue( entry.getValue() );
                this.indexEntry.setObject( entry.getObject() );
                return true;
            }
        }

        clear();
        return false;
    }


    public boolean next() throws Exception
    {
        while ( wrapped.next() )
        {
            checkClosed( "next()" );
            IndexEntry<String,ServerEntry> entry = wrapped.get();
            if ( evaluateCandidate( entry ) )
            {
                available = true;
                this.indexEntry.setId( entry.getId() );
                this.indexEntry.setValue( entry.getValue() );
                this.indexEntry.setObject( entry.getObject() );
                return true;
            }
        }

        clear();
        return false;
    }


    public IndexEntry<String, ServerEntry> get() throws Exception
    {
        checkClosed( "get()" );
        if ( available )
        {
            return indexEntry;
        }

        throw new InvalidCursorPositionException( "Cursor has yet to be positioned." );
    }


    public boolean isElementReused()
    {
        return wrapped.isElementReused();
    }


    public void close() throws Exception
    {
        super.close();
        wrapped.close();
        clear();
    }
}
