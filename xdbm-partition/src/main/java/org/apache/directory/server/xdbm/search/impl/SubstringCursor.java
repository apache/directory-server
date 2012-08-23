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


import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor traversing candidates matching a Substring assertion expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SubstringCursor<ID extends Comparable<ID>> extends AbstractIndexCursor<String, ID>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );

    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_725 );
    private final boolean hasIndex;
    private final IndexCursor<String, ID> wrapped;
    private final SubstringEvaluator<ID> evaluator;
    private final ForwardIndexEntry<String, ID> indexEntry = new ForwardIndexEntry<String, ID>();


    @SuppressWarnings("unchecked")
    public SubstringCursor( Store<Entry, ID> store, final SubstringEvaluator<ID> substringEvaluator )
        throws Exception
    {
        LOG_CURSOR.debug( "Creating SubstringCursor {}", this );
        evaluator = substringEvaluator;
        hasIndex = store.hasIndexOn( evaluator.getExpression().getAttributeType() );

        if ( hasIndex )
        {
            wrapped = ( ( Index<String, Entry, ID> ) store.getIndex( evaluator.getExpression().getAttributeType() ) )
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
            wrapped = store.getEntryUuidIndex().forwardCursor();
        }
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }


    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
        if ( evaluator.getExpression().getInitial() != null && hasIndex )
        {
            ForwardIndexEntry<String, ID> indexEntry = new ForwardIndexEntry<String, ID>();
            indexEntry.setKey( evaluator.getExpression().getInitial() );
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
        setAvailable( false );
        indexEntry.setEntry( null );
        indexEntry.setId( null );
        indexEntry.setKey( null );
    }


    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );

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


    private boolean evaluateCandidate( IndexEntry<String, ID> indexEntry ) throws Exception
    {
        if ( hasIndex )
        {
            String key = indexEntry.getKey();
            return evaluator.getPattern().matcher( key ).matches();
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
            checkNotClosed( "previous()" );
            IndexEntry<String, ID> entry = wrapped.get();

            if ( evaluateCandidate( entry ) )
            {
                setAvailable( true );
                this.indexEntry.setId( entry.getId() );
                this.indexEntry.setKey( entry.getKey() );
                this.indexEntry.setEntry( entry.getEntry() );
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
            checkNotClosed( "next()" );
            IndexEntry<String, ID> entry = wrapped.get();

            if ( evaluateCandidate( entry ) )
            {
                setAvailable( true );
                this.indexEntry.setId( entry.getId() );
                this.indexEntry.setKey( entry.getKey() );
                this.indexEntry.setEntry( entry.getEntry() );

                return true;
            }
        }

        clear();
        return false;
    }


    public IndexEntry<String, ID> get() throws Exception
    {
        checkNotClosed( "get()" );

        if ( available() )
        {
            return indexEntry;
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void close() throws Exception
    {
        LOG_CURSOR.debug( "Closing SubstringCursor {}", this );
        super.close();
        wrapped.close();
        clear();
    }


    /**
     * {@inheritDoc}
     */
    public void close( Exception cause ) throws Exception
    {
        LOG_CURSOR.debug( "Closing SubstringCursor {}", this );
        super.close( cause );
        wrapped.close( cause );
        clear();
    }
}
