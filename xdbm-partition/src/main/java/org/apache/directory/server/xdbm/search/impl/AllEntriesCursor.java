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


import java.util.UUID;

import org.apache.directory.server.core.api.partition.index.AbstractIndexCursor;
import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.model.entry.Entry;


/**
 * A Cursor over all entries in a partition which returns IndexEntries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AllEntriesCursor extends AbstractIndexCursor<UUID>
{
    /** The index entry we use to return entries one by one.  */
    private IndexEntry<UUID> indexEntry = new ForwardIndexEntry<UUID>();
    
    /** The cursor on the EntryUUID index */
    private final IndexCursor<String> wrapped;


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }

    
    /**
     * Creates a new instance of AllEntriesCursor
     * @param db
     * @throws Exception
     */
    public AllEntriesCursor( Store db ) throws Exception
    {
        // Get a reverse cursor because we want to sort by ID
        wrapped = db.getEntryUuidIndex().reverseCursor();
    }


    /**
     * {@inheritDoc}
     */
    public void afterValue( UUID key, UUID value ) throws Exception
    {
        checkNotClosed( "afterValue()" );
        
        wrapped.afterValue( key, null );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeValue( UUID id, UUID value ) throws Exception
    {
        checkNotClosed( "beforeValue()" );
        
        wrapped.beforeValue( id, null );
    }


    /**
     * {@inheritDoc}
     */
    public void after( IndexEntry<UUID> indexEntry ) throws Exception
    {
        checkNotClosed( "after()" );
        
        wrapped.afterValue( indexEntry.getId(), null );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );
        
        wrapped.afterLast();
    }


    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return wrapped.available();
    }


    /**
     * {@inheritDoc}
     */
    public void before( IndexEntry<UUID> indexEntry ) throws Exception
    {
        checkNotClosed( "before()" );
        
        wrapped.beforeValue( indexEntry.getId(), null );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
        
        wrapped.beforeFirst();
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws Exception
    {
        checkNotClosed( "first()" );
        
        return wrapped.first();
    }


    /**
     * {@inheritDoc}
     */
    public IndexEntry<UUID> get() throws Exception
    {
        checkNotClosed( "get()" );
        
        // Create the returned IndexEntry, copying what we get from the wrapped cursor
        IndexEntry<String> wrappedEntry = wrapped.get();
        indexEntry.setId( wrappedEntry.getId() );
        indexEntry.setValue( wrappedEntry.getId() );
        indexEntry.setEntry( wrappedEntry.getEntry() );
        
        return indexEntry;
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception
    {
        checkNotClosed( "last()" );
        
        return wrapped.last();
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        checkNotClosed( "next()" );
        
        return wrapped.next();
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        checkNotClosed( "previous()" );
        
        return wrapped.previous();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception
    {
        wrapped.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause ) throws Exception
    {
        wrapped.close( cause );
    }
}
