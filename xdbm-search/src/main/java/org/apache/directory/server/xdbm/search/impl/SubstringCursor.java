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


import org.apache.directory.server.core.cursor.AbstractCursor;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.InvalidCursorPositionException;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.ForwardIndexEntry;

import javax.naming.directory.Attributes;


/**
 * A Cursor traversing candidates matching a Substring assertion expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubstringCursor extends AbstractCursor<IndexEntry<?, Attributes>>
{
    private static final String UNSUPPORTED_MSG =
        "SubstringCursors may not be ordered and do not support positioning by element.";
    private final boolean hasIndex;
    private final Cursor<IndexEntry<String,Attributes>> wrapped;
    private final SubstringEvaluator evaluator;
    private final ForwardIndexEntry<String,Attributes> indexEntry =
        new ForwardIndexEntry<String,Attributes>();
    private boolean available = false;


    public SubstringCursor( Store<Attributes> db,
                            final SubstringEvaluator substringEvaluator ) throws Exception
    {
        evaluator = substringEvaluator;
        hasIndex = db.hasUserIndexOn( evaluator.getExpression().getAttribute() );

        if ( hasIndex )
        {
            //noinspection unchecked
            wrapped = db.getUserIndex( evaluator.getExpression().getAttribute() ).forwardCursor();
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
        if ( evaluator.getExpression().getInitial() != null && hasIndex )
        {
            ForwardIndexEntry<String,Attributes> indexEntry = new ForwardIndexEntry<String,Attributes>();
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
        if ( evaluator.getExpression().getInitial() != null && hasIndex )
        {
            ForwardIndexEntry<String,Attributes> indexEntry = new ForwardIndexEntry<String,Attributes>();
            indexEntry.setValue( evaluator.getExpression().getInitial() );
            wrapped.after( indexEntry );

            /*
             * The above operation advances us past the first index entry
             * matching the initial value.  Lexographically there may still be
             * entries with values ahead that match and are greater than the
             * initial string. So we advance until we cannot match anymore.
             */
            while ( evaluateCandidate( indexEntry ) && wrapped.next() )
            {
                // do nothing but advance
            }
        }
        else
        {
            wrapped.afterLast();
        }

        clear();
    }


    public boolean first() throws Exception
    {
        beforeFirst();
        return next();
    }


    private boolean evaluateCandidate( IndexEntry<String,Attributes> indexEntry ) throws Exception
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
            IndexEntry<String,Attributes> entry = wrapped.get();
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
            IndexEntry<String,Attributes> entry = wrapped.get();
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


    public IndexEntry<?, Attributes> get() throws Exception
    {
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
