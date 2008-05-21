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

import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.server.core.cursor.CursorIterator;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;


/**
 * TODO ServerEntryCursorAdaptor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerEntryCursorAdaptor implements Cursor<ServerEntry>
{
    private final Partition db;
    private final IndexCursor<Long, ServerEntry> indexCursor;

    
    public ServerEntryCursorAdaptor( Partition db, IndexCursor<Long, ServerEntry> indexCursor )
    {
        this.db = db;
        this.indexCursor = indexCursor;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.Cursor#after(java.lang.Object)
     */
    public void after( ServerEntry element ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.Cursor#afterLast()
     */
    public void afterLast() throws Exception
    {
        this.indexCursor.afterLast();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.Cursor#available()
     */
    public boolean available()
    {
        return indexCursor.available();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.Cursor#before(java.lang.Object)
     */
    public void before( ServerEntry element ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.Cursor#beforeFirst()
     */
    public void beforeFirst() throws Exception
    {
        indexCursor.beforeFirst();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.Cursor#close()
     */
    public void close() throws Exception
    {
        indexCursor.close();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.Cursor#first()
     */
    public boolean first() throws Exception
    {
        return indexCursor.first();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.Cursor#get()
     */
    public ServerEntry get() throws Exception
    {
        IndexEntry<Long,ServerEntry> indexEntry = indexCursor.get();

        if ( indexEntry.getObject() == null )
        {
            indexEntry.setObject( db.lookup( indexEntry.getId() ) );
        }

        return indexEntry.getObject();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.Cursor#isClosed()
     */
    public boolean isClosed() throws Exception
    {
        return indexCursor.isClosed();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.Cursor#isElementReused()
     */
    public boolean isElementReused()
    {
        return indexCursor.isElementReused();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.Cursor#last()
     */
    public boolean last() throws Exception
    {
        return indexCursor.last();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.Cursor#next()
     */
    public boolean next() throws Exception
    {
        return indexCursor.next();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.Cursor#previous()
     */
    public boolean previous() throws Exception
    {
        return indexCursor.previous();
    }


    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<ServerEntry> iterator()
    {
        return new CursorIterator<ServerEntry>( this );
    }
}
