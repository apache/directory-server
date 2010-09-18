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

package org.apache.directory.server.core.partition.ldif;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.naming.InvalidNameException;

import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.partition.impl.avl.AvlPartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.impl.avl.AvlStore;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.ldif.LdapLdifException;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Partition implementation backed by a single LDIF file.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SingleFileLdifPartition extends AbstractLdifPartition
{

    /** the LDIF file holding the partition's data */
    private RandomAccessFile ldifFile;

    /** offset map for entries in the ldif file */
    Map<Comparable, EntryOffset> offsetMap = new HashMap<Comparable, EntryOffset>();

    private static Logger LOG = LoggerFactory.getLogger( SingleFileLdifPartition.class );


    /**
     * 
     * Creates a new instance of SingleFileLdifPartition.
     *
     * @param file the LDIF file containing the partition's data (can also be empty)
     */
    public SingleFileLdifPartition( String file )
    {
        if ( file == null )
        {
            throw new IllegalArgumentException( "ldif file cannot be null" );
        }

        try
        {
            ldifFile = new RandomAccessFile( file, "rws" );
        }
        catch ( FileNotFoundException e )
        {
            throw new RuntimeException( e );
        }

        wrappedPartition = new AvlPartition();
    }


    @Override
    protected void doInit() throws InvalidNameException, Exception
    {
        // Initialize the AvlPartition
        wrappedPartition.setId( id );
        wrappedPartition.setSuffix( suffix );
        wrappedPartition.setSchemaManager( schemaManager );
        wrappedPartition.initialize();

        this.searchEngine = wrappedPartition.getSearchEngine();

        LOG.debug( "id is : {}", wrappedPartition.getId() );

        // Initialize the suffixDirectory : it's a composition
        // of the workingDirectory followed by the suffix
        if ( ( suffix == null ) || ( suffix.isEmpty() ) )
        {
            String msg = I18n.err( I18n.ERR_150 );
            LOG.error( msg );
            throw new LdapInvalidDnException( msg );
        }

        if ( !suffix.isNormalized() )
        {
            suffix.normalize( schemaManager );
        }

        loadEntries();
    }


    /**
     * load the entries from the LDIF file if present
     * @throws Exception
     */
    private void loadEntries() throws Exception
    {
        RandomAccessLdifReader parser = new RandomAccessLdifReader();

        Iterator<LdifEntry> itr = parser.iterator();

        if ( !itr.hasNext() )
        {
            return;
        }

        long curEnryStart = 0;
        long curEntryEnd = parser.prevEntryEnd;

        LdifEntry ldifEntry = itr.next();

        contextEntry = new DefaultEntry( schemaManager, ldifEntry.getEntry() );

        AvlStore store = wrappedPartition.getStore();

        if ( suffix.equals( contextEntry.getDn() ) )
        {
            addMandatoryOpAt( contextEntry );
            store.add( contextEntry );
            offsetMap.put( store.getEntryId( suffix ), new EntryOffset( curEnryStart, curEntryEnd ) );
        }
        else
        {
            throw new LdapException( "The given LDIF file doesn't contain the context entry" );
        }

        curEnryStart = curEntryEnd;
        curEntryEnd = parser.prevEntryEnd;

        while ( itr.hasNext() )
        {
            EntryOffset offset = new EntryOffset( curEnryStart, curEntryEnd );

            ldifEntry = itr.next();

            curEnryStart = curEntryEnd;
            curEntryEnd = parser.prevEntryEnd;

            Entry entry = new DefaultEntry( schemaManager, ldifEntry.getEntry() );

            curEntryEnd = ldifFile.getFilePointer();

            addMandatoryOpAt( entry );
            store.add( entry );
            offsetMap.put( store.getEntryId( entry.getDn() ), offset );
        }

        parser.close();
    }


    public void bind( BindOperationContext bindContext ) throws LdapException
    {
        wrappedPartition.bind( bindContext );
    }


    @Override
    public synchronized void add( AddOperationContext addContext ) throws LdapException
    {
        wrappedPartition.add( addContext );

        Entry entry = addContext.getEntry();

        DN dn = entry.getDn();
        Long id = wrappedPartition.getEntryId( dn );

        String ldif = LdifUtils.convertEntryToLdif( entry );

        try
        {
            if ( dn.equals( suffix ) )
            {
                contextEntry = entry;
                appendLdif( id, null, ldif );
                return;
            }

            // entry has a parent
            Long parentId = wrappedPartition.getEntryId( dn.getParent() );
            EntryOffset parentOffset = offsetMap.get( parentId );
            if ( parentOffset.end == ldifFile.length() )
            {
                appendLdif( id, parentOffset, ldif );
            }
            else
            {
                System.out.println( "====================implement insertion====================" );
            }
        }
        catch ( IOException e )
        {
            throw new LdapException( e );
        }
    }


    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        wrappedPartition.modify( modifyContext );
    }


    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        wrappedPartition.rename( renameContext );
    }


    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        wrappedPartition.move( moveContext );
    }


    @Override
    public void moveAndRename( MoveAndRenameOperationContext opContext ) throws LdapException
    {
        wrappedPartition.moveAndRename( opContext );
    }


    @Override
    public void delete( Long id ) throws LdapException
    {
        wrappedPartition.delete( id );
    }


    /**
     * append data to the LDIF file
     *
     * @param entryId
     * @param parentOffset
     * @param ldif
     * @throws LdapException
     */
    private void appendLdif( Comparable<Long> entryId, EntryOffset parentOffset, String ldif ) throws LdapException
    {
        try
        {
            long pos = 0L;

            if ( parentOffset != null )
            {
                pos = parentOffset.end;
            }

            ldifFile.seek( pos );

            ldifFile.write( StringTools.getBytesUtf8( ldif + "\n" ) );

            offsetMap.put( entryId, new EntryOffset( pos, ldifFile.getFilePointer() ) );
        }
        catch ( IOException e )
        {
            throw new LdapException( e );
        }
    }
    

    /**
     * a holder class for containing an entry's start and end offset positions
     * in the LDIF file
     */
    private class EntryOffset
    {
        /** starting position */
        long start;
        
        /** ending position */
        long end;


        public EntryOffset( long start, long end )
        {
            this.start = start;
            this.end = end;
        }


        @Override
        public String toString()
        {
            return "EntryOffset [start=" + start + ", end=" + end + "]";
        }
    }

    /**
     * an LdifReader backed by a RandomAccessFile
     */
    private class RandomAccessLdifReader extends LdifReader
    {
        private long prevEntryEnd;
        private long len;


        public RandomAccessLdifReader() throws LdapException
        {
            try
            {
                len = ldifFile.length();
                super.init();
            }
            catch ( IOException e )
            {
                throw new LdapException( e );
            }
        }


        @Override
        protected void readLines() throws LdapLdifException
        {
            try
            {
                super.readLines();
                prevEntryEnd = ldifFile.getFilePointer();
            }
            catch ( IOException e )
            {
                LdapLdifException ldife = new LdapLdifException( e.getMessage() );
                ldife.initCause( e );
                throw ldife;
            }
        }


        @Override
        protected String getLine() throws IOException
        {
            if ( len == 0 )
            {
                return null;
            }

            return ldifFile.readLine();
        }

    }


    /**
     * add the CSN and UUID attributes to the entry if they are not present
     */
    private void addMandatoryOpAt( Entry entry ) throws LdapException
    {
        if ( entry.get( SchemaConstants.ENTRY_CSN_AT ) == null )
        {
            entry.add( SchemaConstants.ENTRY_CSN_AT, defaultCSNFactory.newInstance().toString() );
        }

        if ( entry.get( SchemaConstants.ENTRY_UUID_AT ) == null )
        {
            String uuid = UUID.randomUUID().toString();
            entry.add( SchemaConstants.ENTRY_UUID_AT, uuid );
        }
    }
}
