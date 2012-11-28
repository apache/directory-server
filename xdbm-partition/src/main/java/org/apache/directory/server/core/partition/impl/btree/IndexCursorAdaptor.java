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
package org.apache.directory.server.core.partition.impl.btree;


import java.util.Iterator;

import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.CursorIterator;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor which adapts an underlying Tuple based Cursor to one which returns
 * IndexEntry objects rather than tuples.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IndexCursorAdaptor<K> extends AbstractIndexCursor<K>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    @SuppressWarnings("unchecked")
    final Cursor<Tuple> wrappedCursor;
    final IndexEntry<K, String> forwardEntry;


    /**
     * Creates an IndexCursorAdaptor which wraps and adapts a Cursor from a table to
     * one which returns an IndexEntry.
     *
     * @param wrappedCursor the Cursor being adapted
     * @param forwardIndex true for a cursor over a forward index, false for
     * one over a reverse index
     */
    @SuppressWarnings("unchecked")
    public IndexCursorAdaptor( Cursor<Tuple> wrappedCursor, boolean forwardIndex )
    {
        this.wrappedCursor = wrappedCursor;

        forwardEntry = new IndexEntry<K, String>();

        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating IndexCursorAdaptor {}", this );
        }
    }


    public boolean available()
    {
        return wrappedCursor.available();
    }


    public void before( IndexEntry<K, String> element ) throws Exception
    {
        wrappedCursor.before( element.getTuple() );
    }


    public void after( IndexEntry<K, String> element ) throws Exception
    {
        wrappedCursor.after( element.getTuple() );
    }


    public void beforeFirst() throws Exception
    {
        wrappedCursor.beforeFirst();
    }


    public void afterLast() throws Exception
    {
        wrappedCursor.afterLast();
    }


    public boolean first() throws Exception
    {
        return wrappedCursor.first();
    }


    public boolean last() throws Exception
    {
        return wrappedCursor.last();
    }


    public boolean isClosed() throws Exception
    {
        return wrappedCursor.isClosed();
    }


    public boolean previous() throws Exception
    {
        return wrappedCursor.previous();
    }


    public boolean next() throws Exception
    {
        return wrappedCursor.next();
    }


    @SuppressWarnings("unchecked")
    public IndexEntry<K, String> get() throws Exception
    {
        Tuple<K, String> tuple = wrappedCursor.get();
        forwardEntry.setTuple( tuple );

        return forwardEntry;
    }


    public final void setClosureMonitor( ClosureMonitor monitor )
    {
        wrappedCursor.setClosureMonitor( monitor );
    }


    public void close() throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing IndexCursorAdaptor {}", this );
        }
        
        wrappedCursor.close();
    }


    public void close( Exception reason ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing IndexCursorAdaptor {}", this );
        }
        
        wrappedCursor.close( reason );
    }


    public Iterator<IndexEntry<K, String>> iterator()
    {
        return new CursorIterator<IndexEntry<K, String>>( this );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isAfterLast() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isAfterLast()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isBeforeFirst() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isBeforeFirst()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isFirst() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isFirst()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isLast() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "isLast()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "IndexCursorAdaptor (" );

        if ( available() )
        {
            sb.append( "available)" );
        }
        else
        {
            sb.append( "absent)" );
        }

        sb.append( " :\n" );

        sb.append( wrappedCursor.toString( tabs + "    " ) );

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
