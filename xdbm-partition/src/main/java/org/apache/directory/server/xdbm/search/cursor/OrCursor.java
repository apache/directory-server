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
package org.apache.directory.server.xdbm.search.cursor;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor returning candidates satisfying a logical disjunction expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OrCursor<V> extends AbstractIndexCursor<V>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_722 );
    private final List<Cursor<IndexEntry<V, String>>> cursors;
    private final List<Evaluator<? extends ExprNode>> evaluators;
    private final List<Set<String>> blacklists;
    private int cursorIndex = -1;

    /** The candidate we have fetched in the next/previous call */
    private IndexEntry<V, String> prefetched;


    // TODO - do same evaluator fail fast optimization that we do in AndCursor
    public OrCursor( List<Cursor<IndexEntry<V, String>>> cursors,
        List<Evaluator<? extends ExprNode>> evaluators )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating OrCursor {}", this );
        }

        if ( cursors.size() <= 1 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_723 ) );
        }

        this.cursors = cursors;
        this.evaluators = evaluators;
        this.blacklists = new ArrayList<Set<String>>();

        for ( int i = 0; i < cursors.size(); i++ )
        {
            this.blacklists.add( new HashSet<String>() );
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


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        checkNotClosed( "beforeFirst()" );
        cursorIndex = 0;
        cursors.get( cursorIndex ).beforeFirst();
        setAvailable( false );
        prefetched = null;
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        checkNotClosed( "afterLast()" );
        cursorIndex = cursors.size() - 1;
        cursors.get( cursorIndex ).afterLast();
        setAvailable( false );
        prefetched = null;
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        beforeFirst();

        return setAvailable( next() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        afterLast();

        return setAvailable( previous() );
    }


    private boolean isBlackListed( String id )
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
    private void blackListIfDuplicate( IndexEntry<?, String> indexEntry ) throws LdapException
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


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException
    {
        while ( cursors.get( cursorIndex ).previous() )
        {
            checkNotClosed( "previous()" );
            IndexEntry<V, String> candidate = cursors.get( cursorIndex ).get();

            if ( !isBlackListed( candidate.getId() ) )
            {
                blackListIfDuplicate( candidate );

                prefetched = candidate;
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
                IndexEntry<V, String> candidate = cursors.get( cursorIndex ).get();

                if ( !isBlackListed( candidate.getId() ) )
                {
                    blackListIfDuplicate( candidate );

                    prefetched = candidate;
                    return setAvailable( true );
                }
            }
        }

        prefetched = null;

        return setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException
    {
        while ( cursors.get( cursorIndex ).next() )
        {
            checkNotClosed( "next()" );
            IndexEntry<V, String> candidate = cursors.get( cursorIndex ).get();

            if ( !isBlackListed( candidate.getId() ) )
            {
                blackListIfDuplicate( candidate );

                prefetched = candidate;

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
                IndexEntry<V, String> candidate = cursors.get( cursorIndex ).get();

                if ( !isBlackListed( candidate.getId() ) )
                {
                    blackListIfDuplicate( candidate );

                    prefetched = candidate;

                    return setAvailable( true );
                }
            }
        }

        prefetched = null;

        return setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public IndexEntry<V, String> get() throws CursorException
    {
        checkNotClosed( "get()" );

        if ( available() )
        {
            return prefetched;
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void close()
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing OrCursor {}", this );
        }

        super.close();

        for ( Cursor<?> cursor : cursors )
        {
            cursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public void close( Exception cause )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing OrCursor {}", this );
        }

        super.close( cause );

        for ( Cursor<?> cursor : cursors )
        {
            cursor.close( cause );
        }
    }


    /**
     * Dumps the evaluators
     */
    private String dumpEvaluators( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        for ( Evaluator<? extends ExprNode> evaluator : evaluators )
        {
            sb.append( evaluator.toString( tabs + "  >>" ) );
        }

        return sb.toString();
    }


    /**
     * Dumps the cursors
     */
    private String dumpCursors( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        for ( Cursor<IndexEntry<V, String>> cursor : cursors )
        {
            sb.append( cursor.toString( tabs + "  " ) );
            sb.append( "\n" );
        }

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "OrCursor (" );

        if ( available() )
        {
            sb.append( "available)" );
        }
        else
        {
            sb.append( "absent)" );
        }

        sb.append( "#" ).append( cursorIndex ).append( " : \n" );

        if ( ( evaluators != null ) && ( evaluators.size() > 0 ) )
        {
            sb.append( dumpEvaluators( tabs ) );
        }

        if ( ( cursors != null ) && ( cursors.size() > 0 ) )
        {
            sb.append( dumpCursors( tabs ) ).append( '\n' );
        }

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}