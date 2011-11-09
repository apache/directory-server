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
import java.util.UUID;

import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.ReverseIndexEntry;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.CursorIterator;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.cursor.TupleCursor;


/**
 * A Cursor which adapts an underlying Tuple based Cursor to one which returns
 * IndexEntry objects rather than tuples.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IndexCursorAdaptor<K> implements IndexCursor<K>
{
    @SuppressWarnings("unchecked")
    final Cursor<Tuple> wrappedCursor;
    final ForwardIndexEntry<K> forwardEntry;
    final ReverseIndexEntry<K> reverseEntry;


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
        
        if ( forwardIndex )
        {
            forwardEntry = new ForwardIndexEntry<K>();
            reverseEntry = null;
        }
        else
        {
            forwardEntry = null;
            reverseEntry = new ReverseIndexEntry<K>();
        }
    }


    public boolean available()
    {
        return wrappedCursor.available();
    }


    @SuppressWarnings("unchecked")
    public void beforeValue( UUID id, K key ) throws Exception
    {
        if ( wrappedCursor instanceof TupleCursor )
        {
            ( ( TupleCursor ) wrappedCursor ).beforeValue( key, id );
        }
    }


    @SuppressWarnings("unchecked")
    public void afterValue( UUID id, K key ) throws Exception
    {
        if ( wrappedCursor instanceof TupleCursor )
        {
            ( (TupleCursor) wrappedCursor ).afterValue( key, id );
        }
    }


    public void before( IndexEntry<K> element ) throws Exception
    {
        wrappedCursor.before( element.getTuple() );
    }


    public void after( IndexEntry<K> element ) throws Exception
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
    public IndexEntry<K> get() throws Exception
    {
        if ( forwardEntry != null )
        {
            Tuple<K, UUID> tuple = wrappedCursor.get();
            forwardEntry.setTuple( tuple, null );
            return forwardEntry;
        }
        else
        {
            Tuple<UUID, K> tuple = wrappedCursor.get();
            reverseEntry.setTuple( tuple, null );
            return reverseEntry;
        }
    }


    public final void setClosureMonitor( ClosureMonitor monitor )
    {
        wrappedCursor.setClosureMonitor( monitor );
    }


    public void close() throws Exception
    {
        wrappedCursor.close();
    }


    public void close( Exception reason ) throws Exception
    {
        wrappedCursor.close( reason );
    }


    public Iterator<IndexEntry<K>> iterator()
    {
        return new CursorIterator<IndexEntry<K>>( this );
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
}
