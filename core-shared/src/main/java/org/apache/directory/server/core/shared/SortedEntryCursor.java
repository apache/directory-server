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
package org.apache.directory.server.core.shared;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.directory.api.ldap.model.cursor.AbstractCursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.mavibot.btree.BTree;
import org.apache.directory.mavibot.btree.RecordManager;
import org.apache.directory.mavibot.btree.Tuple;
import org.apache.directory.mavibot.btree.TupleCursor;
import org.apache.directory.server.core.api.filtering.EntryFilter;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO SortedEntryCursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SortedEntryCursor extends AbstractCursor<Entry> implements EntryFilteringCursor
{

    private static final Logger LOG = LoggerFactory.getLogger( SortedEntryCursor.class );
    
    private TupleCursor<Entry, String> wrapped;

    private Tuple<Entry, String> tuple;

    private RecordManager recMan;

    private File dataFile;
    
    public SortedEntryCursor( BTree<Entry,String> btree, RecordManager recMan, File dataFile ) throws IOException
    {
        this.recMan = recMan;
        this.dataFile = dataFile;
        wrapped = btree.browse();
    }


    @Override
    public boolean available()
    {
        return ( tuple != null );
    }


    @Override
    public void before( Entry element ) throws LdapException, CursorException
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void after( Entry element ) throws LdapException, CursorException
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void beforeFirst() throws LdapException, CursorException
    {
        try
        {
            tuple = null;
            wrapped.beforeFirst();
        }
        catch ( IOException e )
        {
            throw new CursorException( e );
        }
    }


    @Override
    public void afterLast() throws LdapException, CursorException
    {
        try
        {
            tuple = null;
            wrapped.afterLast();
        }
        catch ( IOException e )
        {
            throw new CursorException( e );
        }

    }


    @Override
    public boolean first() throws LdapException, CursorException
    {
        try
        {
            wrapped.beforeFirst();
            return next();
        }
        catch ( IOException e )
        {
            throw new CursorException( e );
        }

    }


    @Override
    public boolean last() throws LdapException, CursorException
    {
        try
        {
            wrapped.afterLast();
            return previous();
        }
        catch ( IOException e )
        {
            throw new CursorException( e );
        }

    }


    @Override
    public boolean previous() throws LdapException, CursorException
    {
        try
        {
            tuple = wrapped.prev();
            return true;
        }
        catch ( IOException e )
        {
            throw new CursorException( e );
        }
        catch ( NoSuchElementException e )
        {
            // ignore this is due to the call wrapped.prev()
            // instead of doing a check like if(wrapped.hasPrev())
        }

        tuple = null;
        return false;
    }


    @Override
    public boolean next() throws LdapException, CursorException
    {
        try
        {
            tuple = wrapped.next();
            return true;
        }
        catch ( IOException e )
        {
            throw new CursorException( e );
        }
        catch ( NoSuchElementException e )
        {
            // ignore, this is due to the call wrapped.prev()
            // instead of doing a check like if(wrapped.hasNext())
        }

        tuple = null;
        return false;
    }


    @Override
    public Entry get() throws CursorException
    {
        if ( tuple == null )
        {
            throw new InvalidCursorPositionException();
        }

        return tuple.getKey();
    }


    @Override
    public void close()
    {
        wrapped.close();
        deleteFile();
        super.close();
    }


    @Override
    public void close( Exception cause )
    {
        wrapped.close();
        deleteFile();
        super.close( cause );
    }


    @Override
    public boolean addEntryFilter( EntryFilter filter )
    {
        return false;
    }


    @Override
    public List<EntryFilter> getEntryFilters()
    {
        return null;
    }


    @Override
    public SearchOperationContext getOperationContext()
    {
        return null;
    }

    private void deleteFile()
    {
        if( recMan == null )
        {
            return;
        }
        
        try
        {
            recMan.close();
            dataFile.delete();
        }
        catch( IOException e )
        {
            LOG.warn( "Failed to delete the sorted entry data file {}", dataFile, e );
        }
    }
}
