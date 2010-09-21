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


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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

    /** a temporary file used for swapping contents while performing update operations */
    private RandomAccessFile tempBufFile;

    /** offset map for entries in the ldif file */
    Map<Comparable, EntryOffset> offsetMap = new HashMap<Comparable, EntryOffset>();

    /** file name of the underlying LDIF store */
    private String fileName;

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
            fileName = file;

            File tmpFile = File.createTempFile( "ldifpartition", ".buf" );
            tmpFile.deleteOnExit();

            tempBufFile = new RandomAccessFile( tmpFile.getAbsolutePath(), "rws" );
        }
        catch ( IOException e )
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

            Comparable id = store.getEntryId( suffix );
            EntryOffset entryOffset = new EntryOffset( id, curEnryStart, curEntryEnd );

            offsetMap.put( id, entryOffset );
        }
        else
        {
            throw new LdapException( "The given LDIF file doesn't contain the context entry" );
        }

        curEnryStart = curEntryEnd;
        curEntryEnd = parser.prevEntryEnd;

        while ( itr.hasNext() )
        {
            long tmpStart = curEnryStart;
            long tmpEnd = curEntryEnd;

            ldifEntry = itr.next();

            curEnryStart = curEntryEnd;
            curEntryEnd = parser.prevEntryEnd;

            Entry entry = new DefaultEntry( schemaManager, ldifEntry.getEntry() );

            curEntryEnd = ldifFile.getFilePointer();

            addMandatoryOpAt( entry );
            store.add( entry );

            Comparable id = store.getEntryId( entry.getDn() );
            EntryOffset offset = new EntryOffset( id, tmpStart, tmpEnd );

            offsetMap.put( id, offset );
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
            if ( parentOffset.getEnd() == ldifFile.length() )
            {
                appendLdif( id, parentOffset, ldif );
            }
            else
            {
                insertNAppendLdif( id, parentOffset, ldif );
            }
        }
        catch ( IOException e )
        {
            throw new LdapException( e );
        }
    }


    @Override
    public synchronized void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        wrappedPartition.modify( modifyContext );

        Entry entry = modifyContext.getAlteredEntry();

        DN dn = entry.getDn();
        Long id = wrappedPartition.getEntryId( dn );

        String ldif = LdifUtils.convertEntryToLdif( entry );

        byte[] utf8Data = StringTools.getBytesUtf8( ldif + "\n" );

        EntryOffset entryOffset = offsetMap.get( id );

        try
        {
            long fileLen = ldifFile.length();
            long diff = utf8Data.length - entryOffset.length();

            // check if modified entry occupies the same space
            if ( diff == 0 )
            {
                ldifFile.seek( entryOffset.getStart() );
                ldifFile.write( utf8Data );
            }
            else if ( fileLen == entryOffset.getEnd() ) // modified entry is at the end of file
            {
                ldifFile.setLength( entryOffset.getStart() );
                ldifFile.write( utf8Data );

                fileLen = ldifFile.length();

                // change the offsets, the modified entry size changed
                if ( fileLen != entryOffset.getEnd() )
                {
                    entryOffset = new EntryOffset( id, entryOffset.getStart(), fileLen );
                    offsetMap.put( id, entryOffset );
                }
            }
            else
            // modified entry size got changed and is in the middle somewhere
            {
                FileChannel tmpBufChannel = tempBufFile.getChannel();
                FileChannel mainChannel = ldifFile.getChannel();

                // clear the buffer
                tempBufFile.setLength( 0 );

                long count = ( ldifFile.length() - entryOffset.getEnd() );

                mainChannel.transferTo( entryOffset.getEnd(), count, tmpBufChannel );
                ldifFile.setLength( entryOffset.getStart() );

                Set<EntryOffset> belowParentOffsets = greaterThan( entryOffset );

                entryOffset = appendLdif( id, getAboveEntry( entryOffset ), ldif );

                for ( EntryOffset o : belowParentOffsets )
                {
                    o.changeOffsetsBy( diff );
                }

                tmpBufChannel.transferTo( 0, tmpBufChannel.size(), mainChannel );
            }
        }
        catch ( IOException e )
        {
            throw new LdapException( e );
        }
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
    public synchronized void delete( Long id ) throws LdapException
    {
        wrappedPartition.delete( id );

        try
        {
            EntryOffset entryOffset = offsetMap.get( id );

            // check if the entry to be removed is present at the end of file
            if ( entryOffset.getEnd() == ldifFile.length() )
            {
                ldifFile.setLength( entryOffset.getStart() );
                // check if entry is the context entry
                if ( entryOffset.getStart() == 0 )
                {
                    contextEntry = null;
                }
            }
            else
            {
                FileChannel tmpBufChannel = tempBufFile.getChannel();
                FileChannel mainChannel = ldifFile.getChannel();

                // clear the buffer
                tempBufFile.setLength( 0 );

                long count = ( ldifFile.length() - entryOffset.getEnd() );

                mainChannel.transferTo( entryOffset.getEnd(), count, tmpBufChannel );
                ldifFile.setLength( entryOffset.getStart() );

                Set<EntryOffset> belowParentOffsets = greaterThan( entryOffset );

                long diff = entryOffset.length();
                diff -= (2 * diff); // this offset change should always be negative
                
                for ( EntryOffset o : belowParentOffsets )
                {
                    o.changeOffsetsBy( diff );
                }

                tmpBufChannel.transferTo( 0, tmpBufChannel.size(), mainChannel );
            }
        }
        catch( IOException e )
        {
            throw new LdapException( e );
        }
    }


    /**
     * @return the backing LDIF file's name
     */
    public String getFileName()
    {
        return fileName;
    }


    private EntryOffset getAboveEntry( EntryOffset offset ) throws LdapException
    {
        for ( EntryOffset e : offsetMap.values() )
        {
            if( e.getEnd() == offset.getStart() )
            {
                return e;
            }
        }

        // if non exists
        return null;
    }


    /**
     * inserts a given LDIF entry in the middle of the LDIF entries
     *
     * @param entryId the entry's id
     * @param aboveEntryOffset the immediate top entry's offsets
     * @param ldif the entry's ldif to be injected
     * @throws LdapException
     */
    private void insertNAppendLdif( Comparable<Long> entryId, EntryOffset aboveEntryOffset, String ldif )
        throws LdapException
    {
        if ( aboveEntryOffset == null )
        {
            throw new IllegalStateException( "parent offset is null" );
        }

        try
        {
            FileChannel tmpBufChannel = tempBufFile.getChannel();
            FileChannel mainChannel = ldifFile.getChannel();

            // clear the buffer
            tempBufFile.setLength( 0 );

            long count = ( ldifFile.length() - aboveEntryOffset.getEnd() );

            mainChannel.transferTo( aboveEntryOffset.getEnd(), count, tmpBufChannel );
            ldifFile.setLength( aboveEntryOffset.getEnd() );

            Set<EntryOffset> belowParentOffsets = greaterThan( aboveEntryOffset );

            EntryOffset entryOffset = appendLdif( entryId, aboveEntryOffset, ldif );

            long diff = entryOffset.length();

            for ( EntryOffset o : belowParentOffsets )
            {
                o.changeOffsetsBy( diff );
            }

            tmpBufChannel.transferTo( 0, tmpBufChannel.size(), mainChannel );
        }
        catch ( IOException e )
        {
            throw new LdapException( e );
        }
    }


    /**
     * append data to the LDIF file
     *
     * @param entryId
     * @param aboveEntryOffset
     * @param ldif
     * @throws LdapException
     */
    private EntryOffset appendLdif( Comparable<Long> entryId, EntryOffset aboveEntryOffset, String ldif )
        throws LdapException
    {
        try
        {
            long pos = 0L;

            if ( aboveEntryOffset != null )
            {
                pos = aboveEntryOffset.getEnd();
            }
            else
            {
                // erase file
                ldifFile.setLength( 0 );
            }

            ldifFile.seek( pos );

            ldifFile.write( StringTools.getBytesUtf8( ldif + "\n" ) );

            EntryOffset entryOffset = new EntryOffset( entryId, pos, ldifFile.getFilePointer() );
            offsetMap.put( entryId, entryOffset );

            return entryOffset;
        }
        catch ( IOException e )
        {
            throw new LdapException( e );
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
     * gets all the EntryOffset objects whose start pos is greater than the given offset mark's start pos 
     * 
     * @param mark an EntryOffset object which is considered as a mark
     * 
     * @return a sorted set of EntryOffset objects
     */
    private Set<EntryOffset> greaterThan( EntryOffset mark )
    {
        Set<EntryOffset> gtSet = new TreeSet<EntryOffset>();

        for ( EntryOffset o : offsetMap.values() )
        {
            if ( o.getStart() > mark.getStart() )
            {
                gtSet.add( o );
            }
        }

        return gtSet;
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

/**
 * a holder class for containing an entry's start and end offset positions
 * in the LDIF file
 */
class EntryOffset implements Comparable<EntryOffset>
{
    /** starting position */
    private long start;

    /** ending position */
    private long end;

    /** entry id */
    private Comparable id;


    public EntryOffset( Comparable id, long start, long end )
    {
        this.start = start;
        this.end = end;
        this.id = id;
    }


    public int compareTo( EntryOffset o )
    {
        if ( end > o.end )
        {
            return 1;
        }
        else if ( end < o.end )
        {
            return -1;
        }

        return 0;
    }


    public long getStart()
    {
        return start;
    }


    public long getEnd()
    {
        return end;
    }


    public Comparable getId()
    {
        return id;
    }


    public void changeOffsetsBy( long val )
    {
        start += val;
        end += val;
    }


    public long length()
    {
        return ( end - start );
    }


    @Override
    public String toString()
    {
        return "EntryOffset [start=" + start + ", end=" + end + ", id=" + id + "]";
    }
}
