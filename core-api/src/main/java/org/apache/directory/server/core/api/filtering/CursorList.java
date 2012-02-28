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


import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.server.core.api.interceptor.context.SearchingOperationContext;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An implementation of a Cursor based on a {@link List} of {@link Cursor}s.  Optionally, the
 * Cursor may be limited to a specific range within the list.
 * 
 * This class is modeled based on the implementation of {@link org.apache.directory.shared.ldap.model.cursor.ListCursor}
 * 
 * WARN this is only used internally 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CursorList extends AbstractEntryFilteringCursor
{
    /** The inner List */
    private final List<EntryFilteringCursor> list;

    /** The starting position for the cursor in the list. It can be > 0 */
    private final int start;

    /** The ending position for the cursor in the list. It can be < List.size() */
    private final int end;

    /** The current position in the list */
    private int index = -1;

    /** flag to detect the closed cursor */
    private boolean closed;

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
    public CursorList( int start, List<EntryFilteringCursor> list, int end, SearchingOperationContext searchContext )
    {
        super( searchContext );

        if ( list != null )
        {
            this.list = list;
        }
        else
        {
            this.list = Collections.emptyList();
        }

        if ( ( start < 0 ) || ( start > this.list.size() ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_02005_START_INDEX_OUT_OF_RANGE, start ) );
        }

        if ( ( end < 0 ) || ( end > this.list.size() ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_02006_END_INDEX_OUT_OF_RANGE, end ) );
        }

        // check list is not empty list since the empty list is the only situation
        // where we allow for start to equal the end: in other cases it makes no sense
        if ( ( this.list.size() > 0 ) && ( start >= end ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_02007_START_INDEX_ABOVE_END_INDEX, start, end ) );
        }

        this.start = start;
        this.end = end;
    }


    /**
     * Creates a new ListCursor without specific bounds: the bounds are
     * acquired from the size of the list.
     *
     * @param list the backing for this ListCursor
     */
    public CursorList( List<EntryFilteringCursor> list, SearchingOperationContext searchContext )
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
     * @throws IllegalStateException if the underlying list is not sorted
     * and/or a comparator is not provided.
     */
    public void before( Entry element ) throws Exception
    {
        // checkNotClosed( "before()" );
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02008_LIST_MAY_BE_SORTED ) );
    }


    /**
     * {@inheritDoc}
     */
    public void after( Entry element ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02008_LIST_MAY_BE_SORTED ) );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws Exception
    {
        boolean setCurTxn = maybeSetCurTxn();
        
        try
        {
            this.index = 0;
            list.get( index ).beforeFirst();
        }
        finally
        {
            if ( setCurTxn )
            {
                txnBusy.set( false );
                txnManager.setCurTxn( null );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws Exception
    {
        boolean setCurTxn = maybeSetCurTxn();
        
        try
        {
            this.index = end - 1;
            list.get( index ).afterLast();
        }
        finally
        {
            if ( setCurTxn )
            {
                txnBusy.set( false );
                txnManager.setCurTxn( null );
            }
        }

         
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws Exception
    {
        boolean setCurTxn = maybeSetCurTxn();
        
        try
        {
            if ( list.size() > 0 )
            {
                index = start;
                return list.get( index ).first();
            }
    
            return false;
        }
        finally
        {
            if ( setCurTxn )
            {
                txnBusy.set( false );
                txnManager.setCurTxn( null );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception
    {
        boolean setCurTxn = maybeSetCurTxn();
        
        try
        {
            if ( list.size() > 0 )
            {
                index = end - 1;
                return list.get( index ).last();
            }
    
            return false;
        }
        finally
        {
            if ( setCurTxn )
            {
                txnBusy.set( false );
                txnManager.setCurTxn( null );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isFirst() throws Exception
    {
        boolean setCurTxn = maybeSetCurTxn();
        
        try
        {
            return ( list.size() > 0 ) && ( index == start ) && list.get( index ).first();
        }
        finally
        {
            if ( setCurTxn )
            {
                txnBusy.set( false );
                txnManager.setCurTxn( null );
            }
        }
        
    }


    /**
     * {@inheritDoc}
     */
    public boolean isLast() throws Exception
    {
        boolean setCurTxn = maybeSetCurTxn();
        
        try
        {
            return ( list.size() > 0 ) && ( index == end - 1 ) && list.get( index ).last();
        }
        finally
        {
            if ( setCurTxn )
            {
                txnBusy.set( false );
                txnManager.setCurTxn( null );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isAfterLast() throws Exception
    {
        return index == end;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isBeforeFirst() throws Exception
    {
        return index == -1;
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        boolean setCurTxn = maybeSetCurTxn();
        
        try
        {
            // if parked at -1 we cannot go backwards
            if ( index == -1 )
            {
                return false;
            }
    
            // if the index moved back is still greater than or eq to start then OK
            if ( index - 1 >= start )
            {
                if ( index == end )
                {
                    index--;
                }
    
                if ( !list.get( index ).previous() )
                {
                    index--;
                    if ( index != -1 )
                    {
                        return list.get( index ).previous();
                    }
                    else
                    {
                        return false;
                    }
                }
                else
                {
                    return true;
                }
            }
    
            // if the index currently less than or equal to start we need to park it at -1 and return false
            if ( index <= start )
            {
                if ( !list.get( index ).previous() )
                {
                    index = -1;
                    return false;
                }
                else
                {
                    return true;
                }
            }
    
            if ( list.size() <= 0 )
            {
                index = -1;
            }
    
            return false;
        
        }
        finally
        {
            if ( setCurTxn )
            {
                txnBusy.set( false );
                txnManager.setCurTxn( null );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        boolean setCurTxn = maybeSetCurTxn();
        
        try
        {
            // if parked at -1 we advance to the start index and return true
            if ( list.size() > 0 && index == -1 )
            {
                index = start;
                return list.get( index ).next();
            }
    
            // if the index plus one is less than the end then increment and return true
            if ( list.size() > 0 && index + 1 < end )
            {
                if ( !list.get( index ).next() )
                {
                    index++;
                    if ( index < end )
                    {
                        return list.get( index ).next();
                    }
                    else
                    {
                        return false;
                    }
                }
                else
                {
                    return true;
                }
            }
    
            // if the index plus one is equal to the end then increment and return false
            if ( list.size() > 0 && index + 1 == end )
            {
                if ( !list.get( index ).next() )
                {
                    index++;
                    return false;
                }
                else
                {
                    return true;
                }
            }
    
            if ( list.size() <= 0 )
            {
                index = end;
            }
    
            return false;
        }
        finally
        {
            if ( setCurTxn )
            {
                txnBusy.set( false );
                txnManager.setCurTxn( null );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public Entry get() throws Exception
    {
        if ( index < start || index >= end )
        {
            throw new IOException( I18n.err( I18n.ERR_02009_CURSOR_NOT_POSITIONED ) );
        }

        if ( list.get( index ).available() )
        {
            return list.get( index ).get();
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
    public boolean removeEntryFilter( EntryFilter filter )
    {
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public void close() throws Exception
    {
        close( null );
    }


    /**
     * {@inheritDoc}
     */
    /**
     * {@inheritDoc}
     */
    public void close( Exception reason ) throws Exception
    {
        closed = true;

        for ( Cursor<?> c : list )
        {
            try
            {
                if ( reason != null )
                {
                    c.close( reason );
                }
                else
                {
                    c.close();
                }
            }
            catch ( Exception e )
            {
                LOG.warn( "Failed to close the cursor" );
            }
        }

        if ( reason == null )
        {
            this.endTxnAtClose( false );
        }
        else
        {
            this.endTxnAtClose( true );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isClosed() throws Exception
    {
        return closed;
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<Entry> iterator()
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public void setClosureMonitor( ClosureMonitor monitor )
    {
        for ( Cursor c : list )
        {
            c.setClosureMonitor( monitor );
        }
    }
}
