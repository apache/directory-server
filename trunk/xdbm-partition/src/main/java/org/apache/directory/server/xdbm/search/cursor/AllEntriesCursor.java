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
package org.apache.directory.server.xdbm.search.cursor;


import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.partition.impl.btree.IndexCursorAdaptor;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor over all entries in a partition which returns IndexEntries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AllEntriesCursor extends AbstractIndexCursor<String>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    /** The index entry we use to return entries one by one.  */
    private IndexEntry<String, String> indexEntry = new IndexEntry<String, String>();

    /** The cursor on the MsterTable index */
    private final Cursor<IndexEntry<String, String>> wrapped;


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }


    /**
     * Creates a new instance of AllEntriesCursor
     * @param store
     * @throws Exception
     */
    public AllEntriesCursor( Store store ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating AllEntriesCursor {}", this );
        }

        // Uses the MasterTable 
        wrapped = new IndexCursorAdaptor( store.getMasterTable().cursor(), true );
    }


    /**
     * {@inheritDoc}
     */
    public void after( IndexEntry<String, String> indexEntry ) throws LdapException, CursorException
    {
        checkNotClosed( "after()" );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
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
    public void before( IndexEntry<String, String> indexEntry ) throws LdapException, CursorException
    {
        checkNotClosed( "before()" );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        checkNotClosed( "beforeFirst()" );

        wrapped.beforeFirst();
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        checkNotClosed( "first()" );

        return wrapped.first();
    }


    /**
     * {@inheritDoc}
     */
    public IndexEntry<String, String> get() throws CursorException
    {
        checkNotClosed( "get()" );

        // Create the returned IndexEntry, copying what we get from the wrapped cursor
        // As we are using the MasterTable, we have to use the key as the 
        // ID and value
        IndexEntry<?, String> wrappedEntry = wrapped.get();
        indexEntry.setId( ( String ) wrappedEntry.getKey() );
        indexEntry.setKey( ( String ) wrappedEntry.getKey() );
        indexEntry.setEntry( null );

        return indexEntry;
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        checkNotClosed( "last()" );

        return wrapped.last();
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException
    {
        checkNotClosed( "next()" );

        return wrapped.next();
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException
    {
        checkNotClosed( "previous()" );

        return wrapped.previous();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close()
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing AllEntriesCursor {}", this );
        }

        wrapped.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing AllEntriesCursor {}", this );
        }

        wrapped.close( cause );
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "AllEntriesCursor (" );

        if ( available() )
        {
            sb.append( "available)" );
        }
        else
        {
            sb.append( "absent)" );
        }

        sb.append( " :\n" );

        sb.append( wrapped.toString( tabs + "    " ) );

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
