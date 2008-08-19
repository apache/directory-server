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
package org.apache.directory.server.xdbm.search.impl;


import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;


/**
 * A Cursor over all entries in a partition which returns IndexEntries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AllEntriesCursor extends AbstractIndexCursor<Long,ServerEntry>
{
    private IndexEntry<Long, ServerEntry> indexEntry = new ForwardIndexEntry<Long, ServerEntry>();
    private final IndexCursor<String,ServerEntry> wrapped;

    
    public AllEntriesCursor( Store<ServerEntry> db ) throws Exception
    {
        // Get a reverse cursor because we want to sort by ID
        wrapped = db.getNdnIndex().reverseCursor();
    }
    
    
    /* 
     * @see org.apache.directory.server.xdbm.IndexCursor#afterValue(Long, Object)
     */
    public void afterValue( Long key, Long value ) throws Exception
    {
        wrapped.afterValue( key, null );
    }


    /* 
     * @see org.apache.directory.server.xdbm.IndexCursor#beforeValue(java.lang.Long, java.lang.Object)
     */
    public void beforeValue( Long id, Long value ) throws Exception
    {
        wrapped.beforeValue( id, null );
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#after(java.lang.Object)
     */
    public void after( IndexEntry<Long,ServerEntry> indexEntry ) throws Exception
    {
        wrapped.afterValue( indexEntry.getId(), null );
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#afterLast()
     */
    public void afterLast() throws Exception
    {
        wrapped.afterLast();
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#available()
     */
    public boolean available()
    {
        return wrapped.available();
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#before(java.lang.Object)
     */
    public void before( IndexEntry<Long,ServerEntry> indexEntry ) throws Exception
    {
        wrapped.beforeValue( indexEntry.getId(), null );
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#beforeFirst()
     */
    public void beforeFirst() throws Exception
    {
        wrapped.beforeFirst();
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#first()
     */
    public boolean first() throws Exception
    {
        return wrapped.first();
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#get()
     */
    public IndexEntry<Long,ServerEntry> get() throws Exception
    {
        IndexEntry<String,ServerEntry> wrappedEntry = wrapped.get();
        indexEntry.setId( wrappedEntry.getId() );
        indexEntry.setValue( wrappedEntry.getId() );
        indexEntry.setObject( wrappedEntry.getObject() );
        return indexEntry;
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#isElementReused()
     */
    public boolean isElementReused()
    {
        return true;
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#last()
     */
    public boolean last() throws Exception
    {
        return wrapped.last();
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#next()
     */
    public boolean next() throws Exception
    {
        return wrapped.next();
    }


    /* 
     * @see org.apache.directory.server.core.cursor.Cursor#previous()
     */
    public boolean previous() throws Exception
    {
        return wrapped.previous();
    }
}
