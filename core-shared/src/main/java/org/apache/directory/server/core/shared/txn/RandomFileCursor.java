package org.apache.directory.server.core.shared.txn;


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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;

import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.AbstractCursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


public class RandomFileCursor extends AbstractCursor<Entry>
{
    /* Path to the file containing entries */
    private File file;

    /* Size of the previous entry(if a next is done). Used to go back on the cursor */
    private int lastEntrySize;

    /* Current offset into the file */
    private int currentOffset;

    /* Prefetched entry */
    private Entry prefetched;

    /* Whether cursor can move to beforefirst */
    private boolean canMoveBeforeFirst;
    
    private SchemaManager schemaManager;


    RandomFileCursor( File file, EntryFilteringCursor cursor, boolean canMoveBeforeFirst, 
            SchemaManager schemaManager ) throws Exception
    {
        this.file = file;
        this.canMoveBeforeFirst = canMoveBeforeFirst;
        this.schemaManager = schemaManager;

        RandomAccessFile raf = new RandomAccessFile( file, "rw" );

        Entry entry;

        if ( cursor.available() )
        {
            entry = cursor.get();
            prefetched = (( ClonedServerEntry )entry ).getClonedEntry();
            lastEntrySize = writeEntry( raf, entry );
            currentOffset = lastEntrySize + 4;
        }

        try
        {
            while ( cursor.next() )
            {
                entry = cursor.get();
                writeEntry( raf, entry );

            }
        }
        finally
        {
            raf.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean available()
    {
        return prefetched != null;
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        RandomAccessFile raf = new RandomAccessFile( file, "r" );
        byte[] data;
        int length;

        ObjectInputStream in = null;
        ByteArrayInputStream bin = null;

        try
        {
            if ( currentOffset >= raf.length() )
                return false;

            raf.seek( currentOffset );
            length = raf.readInt();

            data = new byte[length];
            raf.read( data, 0, length );

            bin = new ByteArrayInputStream( data );
            in = new ObjectInputStream( bin );

            prefetched = new DefaultEntry();
            prefetched.readExternal( in );

            lastEntrySize = length;
            currentOffset += 4 + length;
        }
        finally
        {
            if ( bin != null )
            {
                bin.close();
            }

            if ( in != null )
            {
                in.close();
            }

            raf.close();
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        if ( lastEntrySize == 0 )
        {
            return false;
        }

        if ( currentOffset < ( lastEntrySize + 4 ) )
        {
            throw new IllegalStateException( "RandomFileCursor currenOffset: " + currentOffset + " lastEntrySize " +
                lastEntrySize );
        }

        currentOffset -= lastEntrySize + 4;
        next();
        currentOffset -= lastEntrySize + 4;
        lastEntrySize = 0;
        return true;

    }


    /**
     * {@inheritDoc}
     */
    public Entry get() throws Exception
    {
        if ( available() )
        {   
            return new ClonedServerEntry( schemaManager, prefetched );
        }

        throw new InvalidCursorPositionException();
    }


    /**
     * {@inheritDoc}
     */
    public void after( Entry entry ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "after()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "afterLast()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "last()" ) ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws Exception
    {
        beforeFirst();
        return next();
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws Exception
    {
        if ( !canMoveBeforeFirst )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass()
                .getName()
                .concat( "." ).concat( "beforeFirst()" ) ) );
        }
        else
        {
            currentOffset = 0;
            lastEntrySize = 0;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void before( Entry entry ) throws Exception
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_02014_UNSUPPORTED_OPERATION, getClass().getName()
            .concat( "." ).concat( "beforeEntry()" ) ) );
    }


    private int writeEntry( RandomAccessFile raf, Entry entry ) throws Exception
    {
        entry = ( ( ClonedServerEntry )entry ).getOriginalEntry();    
        byte[] data;

        ObjectOutputStream out = null;
        ByteArrayOutputStream bout = null;
        try
        {
            bout = new ByteArrayOutputStream();
            out = new ObjectOutputStream( bout );
            entry.writeExternal( out );

            out.flush();
            data = bout.toByteArray();
        }
        finally
        {
            if ( bout != null )
            {
                bout.close();
            }

            if ( out != null )
            {
                out.close();
            }
        }

        raf.writeInt( data.length );
        raf.write( data, 0, data.length );

        return data.length;
    }
}
