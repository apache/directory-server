/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.partition.impl.btree;


import java.util.Iterator;

import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.shared.ldap.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.cursor.CursorIterator;
import org.apache.directory.shared.ldap.entry.ServerEntry;


/**
 * Adapts index cursors to return just ServerEntry objects.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerEntryCursorAdaptor<ID> implements Cursor<ServerEntry>
{
    private final BTreePartition<ID> db;
    private final IndexCursor<ID, ServerEntry, ID> indexCursor;


    public ServerEntryCursorAdaptor( BTreePartition<ID> db, IndexCursor<ID, ServerEntry, ID> indexCursor )
    {
        this.db = db;
        this.indexCursor = indexCursor;
    }


    /* 
     * @see Cursor#after(java.lang.Object)
     */
    public void after( ServerEntry element ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /* 
     * @see Cursor#afterLast()
     */
    public void afterLast() throws Exception
    {
        this.indexCursor.afterLast();
    }


    /* 
     * @see Cursor#available()
     */
    public boolean available()
    {
        return indexCursor.available();
    }


    /* 
     * @see Cursor#before(java.lang.Object)
     */
    public void before( ServerEntry element ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /* 
     * @see Cursor#beforeFirst()
     */
    public void beforeFirst() throws Exception
    {
        indexCursor.beforeFirst();
    }


    public final void setClosureMonitor( ClosureMonitor monitor )
    {
        indexCursor.setClosureMonitor( monitor );
    }


    /* 
     * @see Cursor#close()
     */
    public void close() throws Exception
    {
        indexCursor.close();
    }


    /* 
     * @see Cursor#close()
     */
    public void close( Exception e ) throws Exception
    {
        indexCursor.close( e );
    }


    /* 
     * @see Cursor#first()
     */
    public boolean first() throws Exception
    {
        return indexCursor.first();
    }


    /* 
     * @see Cursor#get()
     */
    public ServerEntry get() throws Exception
    {
        IndexEntry<ID, ServerEntry, ID> indexEntry = indexCursor.get();

        if ( indexEntry.getObject() == null )
        {
            indexEntry.setObject( db.lookup( indexEntry.getId() ) );
        }

        return indexEntry.getObject();
    }


    /* 
     * @see Cursor#isClosed()
     */
    public boolean isClosed() throws Exception
    {
        return indexCursor.isClosed();
    }


    /* 
     * @see Cursor#isElementReused()
     */
    public boolean isElementReused()
    {
        return indexCursor.isElementReused();
    }


    /* 
     * @see Cursor#last()
     */
    public boolean last() throws Exception
    {
        return indexCursor.last();
    }


    /* 
     * @see Cursor#next()
     */
    public boolean next() throws Exception
    {
        return indexCursor.next();
    }


    /* 
     * @see Cursor#previous()
     */
    public boolean previous() throws Exception
    {
        return indexCursor.previous();
    }


    /* 
     * @see Iterable#iterator()
     */
    public Iterator<ServerEntry> iterator()
    {
        return new CursorIterator<ServerEntry>( this );
    }
}
