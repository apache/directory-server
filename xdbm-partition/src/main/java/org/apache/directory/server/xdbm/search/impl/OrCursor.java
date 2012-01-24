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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.ExprNode;


/**
 * A Cursor returning candidates satisfying a logical disjunction expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OrCursor<V, ID> extends AbstractIndexCursor<V, Entry, ID>
{
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_722 );
    private final List<IndexCursor<V, Entry, ID>> cursors;
    private final List<Evaluator<? extends ExprNode, Entry, ID>> evaluators;
    private final List<Set<ID>> blacklists;
    private int cursorIndex = -1;


    // TODO - do same evaluator fail fast optimization that we do in AndCursor
    public OrCursor( List<IndexCursor<V, Entry, ID>> cursors,
        List<Evaluator<? extends ExprNode, Entry, ID>> evaluators )
    {
        if ( cursors.size() <= 1 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_723 ) );
        }

        this.cursors = cursors;
        this.evaluators = evaluators;
        this.blacklists = new ArrayList<Set<ID>>();

        for ( int ii = 0; ii < cursors.size(); ii++ )
        {
            this.blacklists.add( new HashSet<ID>() );
        }
        this.cursorIndex = 0;
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
        cursorIndex = 0;
        cursors.get( cursorIndex ).beforeFirst();
        setAvailable( false );
    }


    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );
        cursorIndex = cursors.size() - 1;
        cursors.get( cursorIndex ).afterLast();
        setAvailable( false );
    }


    public boolean first() throws Exception
    {
        beforeFirst();

        return setAvailable( next() );
    }


    public boolean last() throws Exception
    {
        afterLast();

        return setAvailable( previous() );
    }


    private boolean isBlackListed( ID id )
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
    private void blackListIfDuplicate( IndexEntry<?, ID> indexEntry ) throws Exception
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
            IndexEntry<?, ID> candidate = cursors.get( cursorIndex ).get();

            if ( !isBlackListed( candidate.getId() ) )
            {
                blackListIfDuplicate( candidate );

                return setAvailable( true );
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
                IndexEntry<?, ID> candidate = cursors.get( cursorIndex ).get();

                if ( !isBlackListed( candidate.getId() ) )
                {
                    blackListIfDuplicate( candidate );

                    return setAvailable( true );
                }
            }
        }

        return setAvailable( false );
    }


    public boolean next() throws Exception
    {
        while ( cursors.get( cursorIndex ).next() )
        {
            checkNotClosed( "next()" );
            IndexEntry<?, ID> candidate = cursors.get( cursorIndex ).get();
            if ( !isBlackListed( candidate.getId() ) )
            {
                blackListIfDuplicate( candidate );

                return setAvailable( true );
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
                IndexEntry<?, ID> candidate = cursors.get( cursorIndex ).get();
                if ( !isBlackListed( candidate.getId() ) )
                {
                    blackListIfDuplicate( candidate );

                    return setAvailable( true );
                }
            }
        }

        return setAvailable( false );
    }


    public IndexEntry<V, ID> get() throws Exception
    {
        checkNotClosed( "get()" );

        if ( available() )
        {
            return cursors.get( cursorIndex ).get();
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
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