/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.api.filtering;


import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.api.i18n.I18n;
import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An implementation of a Cursor based on a {@link List} of {@link Cursor}s.  Optionally, the
 * Cursor may be limited to a specific range within the list.
 * 
 * This class is modeled based on the implementation of {@link org.apache.directory.api.ldap.model.cursor.ListCursor}
 * 
 * WARN this is only used internally !
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CursorList extends AbstractCursor<Entry> implements EntryFilteringCursor
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    /** The inner List */
    private final List<EntryFilteringCursor> list;

    /** The starting position for the cursor in the list. It can be > 0 */
    private final int start;

    /** The ending position for the cursor in the list. It can be < List.size() */
    private final int end;

    /** The number of cursors in the list */
    private final int listSize;

    /** The current position in the list */
    private int index;

    /** The current cursor being used */
    private EntryFilteringCursor currentCursor;

    /** the operation context */
    private SearchOperationContext searchContext;

    /** flag to detect the closed cursor */
    private boolean closed;

    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( CursorList.class );


    /**
     * Creates a new ListCursor with lower (inclusive) and upper (exclusive)
     * bounds.
     *
     * As with all Cursors, this ListCursor requires a successful return from
     * advance operations (next() or previous()) to properly return values
     * using the get() operation.
     *
     * @param start the lower bound index
     * @param list the list this ListCursor operates on
     * @param end the upper bound index
     */
    public CursorList( int start, List<EntryFilteringCursor> list, int end, SearchOperationContext searchContext )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating CursorList {}", this );
        }

        if ( list != null )
        {
            this.list = list;
        }
        else
        {
            this.list = Collections.emptyList();
        }

        listSize = this.list.size();

        if ( ( start < 0 ) || ( start > listSize ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_02005_START_INDEX_OUT_OF_RANGE, start ) );
        }

        if ( ( end < 0 ) || ( end > listSize ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_02006_END_INDEX_OUT_OF_RANGE, end ) );
        }

        // check list is not empty list since the empty list is the only situation
        // where we allow for start to equal the end: in other cases it makes no sense
        if ( ( listSize > 0 ) && ( start >= end ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_02007_START_INDEX_ABOVE_END_INDEX, start, end ) );
        }

        this.start = start;
        this.end = end;
        this.searchContext = searchContext;
        index = start;
        currentCursor = this.list.get( index );
    }


    /**
     * Creates a new ListCursor without specific bounds: the bounds are
     * acquired from the size of the list.
     *
     * @param list the backing for this ListCursor
     */
    public CursorList( List<EntryFilteringCursor> list, SearchOperationContext searchContext )
    {
        this( 0, list, list.size(), searchContext );
    }


    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        if ( ( index >= 0 ) && ( index < end ) )
        {
            return list.get( index ).available();
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    public void before( Entry element ) throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02008_LIST_MAY_BE_SORTED ) );
    }


    /**
     * {@inheritDoc}
     */
    public void after( Entry element ) throws LdapException, CursorException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02008_LIST_MAY_BE_SORTED ) );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        index = 0;
        currentCursor = list.get( index );
        currentCursor.beforeFirst();
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        index = end - 1;
        currentCursor = list.get( index );
        currentCursor.afterLast();
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        if ( listSize > 0 )
        {
            index = start;

            return list.get( index ).first();
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        if ( listSize > 0 )
        {
            index = end - 1;
            currentCursor = list.get( index );

            return currentCursor.last();
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isFirst()
    {
        return ( listSize > 0 ) && ( index == start ) && list.get( index ).isFirst();
    }


    /**
     * {@inheritDoc}
     */
    public boolean isLast()
    {
        return ( listSize > 0 ) && ( index == end - 1 ) && list.get( index ).isLast();
    }


    /**
     * {@inheritDoc}
     */
    public boolean isAfterLast()
    {
        return ( index == end );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isBeforeFirst()
    {
        return index == -1;
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException
    {
        // if parked at -1 we cannot go backwards
        if ( index == -1 )
        {
            return false;
        }

        // if the index moved back is still greater than or eq to start then OK
        if ( index > start )
        {
            if ( index == end )
            {
                index--;
                currentCursor = list.get( index );
            }

            if ( !currentCursor.previous() )
            {
                index--;
                currentCursor = list.get( index );

                return currentCursor.previous();
            }
            else
            {
                return true;
            }
        }

        // if the index currently less than or equal to start we need to park it at -1 and return false
        if ( index <= start )
        {
            if ( !currentCursor.previous() )
            {
                index = -1;
                currentCursor = null;

                return false;
            }
            else
            {
                return true;
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException
    {
        if ( listSize > 0 )
        {
            if ( index == -1 )
            {
                index = start;
            }

            while ( index < end )
            {
                currentCursor = list.get( index );

                if ( currentCursor.next() )
                {
                    return true;
                }
                else
                {
                    index++;
                }
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    public Entry get() throws CursorException
    {
        if ( ( index < start ) || ( index >= end ) )
        {
            throw new CursorException( I18n.err( I18n.ERR_02009_CURSOR_NOT_POSITIONED ) );
        }

        if ( currentCursor.available() )
        {
            return currentCursor.get();
        }

        throw new InvalidCursorPositionException();
    }


    /**
     * {@inheritDoc}
     */
    public boolean addEntryFilter( EntryFilter filter )
    {
        for ( EntryFilteringCursor efc : list )
        {
            efc.addEntryFilter( filter );
        }

        // returning hard coded value, shouldn't be a problem
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public List<EntryFilter> getEntryFilters()
    {
        throw new UnsupportedOperationException( "CursorList doesn't support this operation" );
    }


    /**
     * {@inheritDoc}
     */
    public SearchOperationContext getOperationContext()
    {
        return searchContext;
    }


    public boolean isAbandoned()
    {
        return searchContext.isAbandoned();
    }


    public void setAbandoned( boolean abandoned )
    {
        searchContext.setAbandoned( abandoned );

        if ( abandoned )
        {
            LOG.info( "Cursor has been abandoned." );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void close()
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing CursorList {}", this );
        }

        close( null );
    }


    /**
     * {@inheritDoc}
     */
    public void close( Exception reason )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing CursorList {}", this );
        }

        closed = true;

        for ( EntryFilteringCursor cursor : list )
        {
            try
            {
                if ( reason != null )
                {
                    cursor.close( reason );
                }
                else
                {
                    cursor.close();
                }
            }
            catch ( Exception e )
            {
                LOG.warn( "Failed to close the cursor" );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isClosed()
    {
        return closed;
    }


    public Iterator<Entry> iterator()
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void setClosureMonitor( ClosureMonitor monitor )
    {
        for ( EntryFilteringCursor c : list )
        {
            c.setClosureMonitor( monitor );
        }
    }
}
