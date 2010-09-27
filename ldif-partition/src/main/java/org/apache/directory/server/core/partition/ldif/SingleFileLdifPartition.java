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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
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
    Map<Long, EntryOffset> offsetMap = new HashMap<Long, EntryOffset>();

    /** file name of the underlying LDIF store */
    private String fileName;

    /** lock for serializing the operations on the backing LDIF file */
    private Object lock = new Object();

    /** the list of temporary buffers */
    private Map<File,RandomAccessFile> tempBufMap = new HashMap<File,RandomAccessFile>();

    private Comparator<EntryOffset> comparator = new Comparator<EntryOffset>()
    {
        public int compare( EntryOffset o1, EntryOffset o2 )
        {
            if ( o1.getEnd() > o2.getEnd() )
            {
                return 1;
            }
            if ( o1.getEnd() < o2.getEnd() )
            {
                return -1;
            }

            return 0;
        }
    };

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
            
            File partitionDir = new File( file ).getParentFile();
            setPartitionDir( partitionDir );
            setWorkingDirectory( partitionDir.getAbsolutePath() );
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


    @Override
    public void setPartitionDir( File partitionDir )
    {
        // partition directory will always be the directory
        // in which the backing LDIF file is present 
        if( getPartitionDir() != null )
        {
            super.setPartitionDir( partitionDir );
        }
    }

    
    @Override
    public void setWorkingDirectory( String workingDirectory )
    {
        if( getWorkingDirectory() != null )
        {
            super.setWorkingDirectory( workingDirectory );
        }
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

            Long entryId = ( Long ) store.getEntryId( suffix );
            EntryOffset entryOffset = new EntryOffset( entryId, curEnryStart, curEntryEnd );

            offsetMap.put( entryId, entryOffset );
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

            Long entryId = ( Long ) store.getEntryId( entry.getDn() );
            EntryOffset offset = new EntryOffset( entryId, tmpStart, tmpEnd );

            offsetMap.put( entryId, offset );
        }

        parser.close();
    }


    public void bind( BindOperationContext bindContext ) throws LdapException
    {
        wrappedPartition.bind( bindContext );
    }


    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        synchronized ( lock )
        {
            wrappedPartition.add( addContext );

            Entry entry = addContext.getEntry();

            DN dn = entry.getDn();
            Long entryId = wrappedPartition.getEntryId( dn );

            String ldif = LdifUtils.convertEntryToLdif( entry );

            try
            {
                if ( dn.equals( suffix ) )
                {
                    contextEntry = entry;
                    appendLdif( entryId, null, ldif );
                    return;
                }

                // entry has a parent
                Long parentId = wrappedPartition.getEntryId( dn.getParent() );
                EntryOffset parentOffset = offsetMap.get( parentId );
                if ( parentOffset.getEnd() == ldifFile.length() )
                {
                    appendLdif( entryId, parentOffset, ldif );
                }
                else
                {
                    insertNAppendLdif( entryId, parentOffset, ldif );
                }
            }
            catch ( IOException e )
            {
                throw new LdapException( e );
            }
        }
    }


    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        synchronized ( lock )
        {
            wrappedPartition.modify( modifyContext );

            Entry entry = modifyContext.getAlteredEntry();

            DN dn = entry.getDn();
            Long entryId = wrappedPartition.getEntryId( dn );

            String ldif = LdifUtils.convertEntryToLdif( entry );

            replaceLdif( entryId, ldif );
        }
    }


    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        synchronized ( lock )
        {
            Long id = wrappedPartition.getEntryId( renameContext.getDn() );

            wrappedPartition.rename( renameContext );

            try
            {
                // perform for the first id
                Entry entry = wrappedPartition.lookup( id );
                String ldif = LdifUtils.convertEntryToLdif( entry );
                replaceLdif( id, ldif );

                IndexCursor<Long, Entry, Long> cursor = wrappedPartition.getOneLevelIndex().forwardCursor( id );
                cursor.beforeFirst();

                while ( cursor.next() )
                {
                    IndexEntry<Long, Entry, Long> idxEntry = cursor.get();

                    Long tmpId = idxEntry.getId();
                    entry = wrappedPartition.lookup( tmpId );
                    ldif = LdifUtils.convertEntryToLdif( entry );
                    replaceLdif( tmpId, ldif );

                    replaceRecursive( tmpId, null );
                }

                cursor.close();
            }
            catch ( Exception e )
            {
                throw new LdapException( e );
            }
        }
    }


    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        synchronized ( lock )
        {
            Long entryId = wrappedPartition.getEntryId( moveContext.getDn() );

            wrappedPartition.move( moveContext );

            Long parentId = wrappedPartition.getEntryId( moveContext.getNewSuperior() );

            move( entryId, parentId );
        }
    }


    @Override
    public void moveAndRename( MoveAndRenameOperationContext opContext ) throws LdapException
    {
        synchronized ( lock )
        {

            Long entryId = wrappedPartition.getEntryId( opContext.getDn() );

            wrappedPartition.moveAndRename( opContext );

            Long parentId = wrappedPartition.getEntryId( opContext.getNewSuperiorDn() );

            move( entryId, parentId );
        }
    }


    /**
     * 
     * changes the LDIF entry content changed as part of move or moveAndRename
     * operations 
     * 
     * FIXME explain in detail the algorithm used to perform move inside the LDIF file
     * 
     * @param entryId the moved entry's ID
     * @param parentId the moved entry's new parent id
     * @throws LdapException
     */
    private synchronized void move( Long entryId, Long parentId ) throws LdapException
    {
        synchronized ( lock )
        {
            try
            {
                EntryOffset entryOffset = offsetMap.get( entryId );

                // delete the entries which were moved in the wrapped parttion
                IndexCursor<Long, Entry, Long> cursor = wrappedPartition.getSubLevelIndex().forwardCursor( entryId );
                cursor.beforeFirst();

                Set<EntryOffset> movedTreeOffsets = new TreeSet<EntryOffset>( comparator );

                while ( cursor.next() )
                {
                    EntryOffset o = offsetMap.get( cursor.get().getId() );
                    movedTreeOffsets.add( o );
                }

                cursor.close();

                EntryOffset[] movedTreeOffsetArray = movedTreeOffsets.toArray( new EntryOffset[0] );
                EntryOffset endMovOffset = movedTreeOffsetArray[movedTreeOffsetArray.length - 1];

                long count = ( ldifFile.length() - endMovOffset.getEnd() );

                RandomAccessFile tmpMovFile = createTempFile();

                FileChannel tempBuf = tmpMovFile.getChannel();
                FileChannel mainChannel = ldifFile.getChannel();
                tempBuf.truncate( 0 );
                
                if ( count > 0 )
                {
                    mainChannel.transferTo( endMovOffset.getEnd(), count, tempBuf );
                    ldifFile.setLength( movedTreeOffsetArray[0].getStart() ); // delete the now obsolete(cause of move operation) entries

                    tempBuf.transferTo( 0, tempBuf.size(), mainChannel );
                    tempBuf.truncate( 0 );
                    tmpMovFile.setLength( 0 );

                    EntryOffset[] belowOffsets = greaterThan( endMovOffset );
                    EntryOffset aboveOffset = getAboveEntry( movedTreeOffsetArray[0] );

                    for ( EntryOffset o : belowOffsets )
                    {
                        Long id = o.getId();

                        o = new EntryOffset( id, aboveOffset.getEnd(), aboveOffset.getEnd() + o.length() );
                        offsetMap.put( id, o );
                        aboveOffset = o;
                    }
                }
                else
                // in case if nothing exists after the moving tree
                {
                    ldifFile.setLength( movedTreeOffsetArray[0].getStart() );
                }

                for ( EntryOffset o : movedTreeOffsets )
                {
                    Long childId = o.getId();
                    Entry entry = wrappedPartition.lookup( childId );
                    String ldif = LdifUtils.convertEntryToLdif( entry );

                    long pos = tmpMovFile.getFilePointer();
                    tmpMovFile.write( StringTools.getBytesUtf8( ldif + "\n" ) );

                    entryOffset = new EntryOffset( childId, pos, tmpMovFile.getFilePointer() );
                    offsetMap.put( childId, entryOffset );
                }

                EntryOffset parentOffset = offsetMap.get( parentId );
                count = ( ldifFile.length() - parentOffset.getEnd() );

                EntryOffset[] belowParentOffsets = greaterThan( parentOffset );

                if ( count > 0 )
                {
                    mainChannel.transferTo( parentOffset.getEnd(), count, tempBuf );
                }

                ldifFile.setLength( parentOffset.getEnd() );

                // copy back to main file
                tempBuf.transferTo( 0, tempBuf.size(), mainChannel );
                tempBuf.truncate( 0 );

                // change offset of the copied entries
                Set<Long> idset = new HashSet<Long>();
                for ( EntryOffset o : movedTreeOffsets )
                {
                    o = new EntryOffset( o.getId(), parentOffset.getEnd(), parentOffset.getEnd() + o.length() );
                    offsetMap.put( o.getId(), o );
                    parentOffset = o;
                    idset.add( o.getId() );
                }

                for ( EntryOffset o : belowParentOffsets )
                {
                    Long tmpId = o.getId();
                    if ( idset.contains( tmpId ) )
                    {
                        continue;
                    }

                    o = new EntryOffset( tmpId, parentOffset.getEnd(), parentOffset.getEnd() + o.length() );
                    offsetMap.put( tmpId, o );
                    parentOffset = o;
                    idset.add( o.getId() );
                }
            }
            catch ( Exception e )
            {
                throw new LdapException( e );
            }
            finally
            {
                deleteTempFiles();
            }
        }
    }


    @Override
    public void delete( Long id ) throws LdapException
    {
        synchronized ( lock )
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
                    RandomAccessFile tempBufFile = createTempFile();
                    FileChannel tmpBufChannel = tempBufFile.getChannel();
                    FileChannel mainChannel = ldifFile.getChannel();

                    long count = ( ldifFile.length() - entryOffset.getEnd() );

                    mainChannel.transferTo( entryOffset.getEnd(), count, tmpBufChannel );
                    ldifFile.setLength( entryOffset.getStart() );

                    EntryOffset[] belowParentOffsets = greaterThan( entryOffset );

                    long diff = -( entryOffset.length() ); // this offset change should always be negative

                    for ( EntryOffset o : belowParentOffsets )
                    {
                        o.changeOffsetsBy( diff );
                    }

                    tmpBufChannel.transferTo( 0, tmpBufChannel.size(), mainChannel );
                    tmpBufChannel.truncate( 0 );
                }
            }
            catch ( IOException e )
            {
                throw new LdapException( e );
            }
            finally
            {
                deleteTempFiles();
            }
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
        synchronized ( lock )
        {

            for ( EntryOffset e : offsetMap.values() )
            {
                if ( e.getEnd() == offset.getStart() )
                {
                    return e;
                }
            }

            // if non exists
            return null;
        }
    }


    /**
     * replaces entries present at ONE level scope of a parent entry in a recursive manner
     *
     * @param id the parent entry's id
     * @param cursorMap the map which holds open cursors
     * @throws Exception
     */
    private void replaceRecursive( Long id, Map<Long, IndexCursor<Long, Entry, Long>> cursorMap ) throws Exception
    {
        synchronized ( lock )
        {

            IndexCursor<Long, Entry, Long> cursor = null;
            if ( cursorMap == null )
            {
                cursorMap = new HashMap<Long, IndexCursor<Long, Entry, Long>>();
            }

            cursor = cursorMap.get( id );

            if ( cursor == null )
            {
                cursor = wrappedPartition.getOneLevelIndex().forwardCursor( id );
                cursor.beforeFirst();
                cursorMap.put( id, cursor );
            }

            if ( !cursor.next() ) // if this is a leaf entry's DN
            {
                cursorMap.remove( id );
                cursor.close();
            }
            else
            {
                do
                {
                    IndexEntry<Long, Entry, Long> idxEntry = cursor.get();
                    Entry entry = wrappedPartition.lookup( idxEntry.getId() );

                    Long entryId = wrappedPartition.getEntryId( entry.getDn() );
                    String ldif = LdifUtils.convertEntryToLdif( entry );

                    replaceLdif( entryId, ldif );

                    replaceRecursive( entryId, cursorMap );
                }
                while ( cursor.next() );
                cursorMap.remove( id );
                cursor.close();
            }
        }
    }


    /**
     * appends all the entries present under a given entry, recursively
     *
     * @param entryId the base entry's id
     * @param cursorMap the cursor map
     * @param tmpMovFile a RandomAccessFile to be used as temporary buffer
     * @throws Exception
     */
    private void appendRecursive( Long entryId, Map<Long, IndexCursor<Long, Entry, Long>> cursorMap,
        RandomAccessFile tmpMovFile ) throws Exception
    {
        synchronized ( lock )
        {

            IndexCursor<Long, Entry, Long> cursor = null;
            if ( cursorMap == null )
            {
                cursorMap = new HashMap<Long, IndexCursor<Long, Entry, Long>>();
            }

            cursor = cursorMap.get( entryId );

            if ( cursor == null )
            {
                cursor = wrappedPartition.getOneLevelIndex().forwardCursor( entryId );
                cursor.beforeFirst();
                cursorMap.put( entryId, cursor );
            }

            if ( !cursor.next() ) // if this is a leaf entry's DN
            {
                cursorMap.remove( entryId );
                cursor.close();
            }
            else
            {
                do
                {
                    IndexEntry<Long, Entry, Long> idxEntry = cursor.get();
                    Entry entry = wrappedPartition.lookup( idxEntry.getId() );

                    Long childId = wrappedPartition.getEntryId( entry.getDn() );
                    String ldif = LdifUtils.convertEntryToLdif( entry );

                    long pos = tmpMovFile.getFilePointer();
                    tmpMovFile.write( StringTools.getBytesUtf8( ldif + "\n" ) );

                    EntryOffset entryOffset = new EntryOffset( childId, pos, tmpMovFile.getFilePointer() );
                    offsetMap.put( childId, entryOffset );

                    appendRecursive( childId, cursorMap, tmpMovFile );
                }
                while ( cursor.next() );
                cursorMap.remove( entryId );
                cursor.close();
            }
        }
    }


    /**
     * inserts a given LDIF entry in the middle of the LDIF entries
     *
     * @param entryId the entry's id
     * @param aboveEntryOffset the immediate top entry's offsets
     * @param ldif the entry's ldif to be injected
     * @throws LdapException
     */
    private void insertNAppendLdif( Long entryId, EntryOffset aboveEntryOffset, String ldif )
        throws LdapException
    {
        synchronized ( lock )
        {
            if ( aboveEntryOffset == null )
            {
                throw new IllegalStateException( "parent offset is null" );
            }

            try
            {
                RandomAccessFile tempBufFile = createTempFile();
                FileChannel tmpBufChannel = tempBufFile.getChannel();
                FileChannel mainChannel = ldifFile.getChannel();

                long count = ( ldifFile.length() - aboveEntryOffset.getEnd() );

                mainChannel.transferTo( aboveEntryOffset.getEnd(), count, tmpBufChannel );
                ldifFile.setLength( aboveEntryOffset.getEnd() );

                EntryOffset[] belowParentOffsets = greaterThan( aboveEntryOffset );

                EntryOffset entryOffset = appendLdif( entryId, aboveEntryOffset, ldif );

                long diff = entryOffset.length();

                for ( EntryOffset o : belowParentOffsets )
                {
                    o.changeOffsetsBy( diff );
                }

                tmpBufChannel.transferTo( 0, tmpBufChannel.size(), mainChannel );
                tmpBufChannel.truncate( 0 );
            }
            catch ( IOException e )
            {
                throw new LdapException( e );
            }
            finally
            {
                deleteTempFiles();
            }
        }
    }


    /**
     * replaces an existing entry
     *
     * @param entryId the entry's id
     * @param ldif entry data in LDIF
     * @throws LdapException
     */
    private void replaceLdif( Long entryId, String ldif ) throws LdapException
    {
        synchronized ( lock )
        {
            try
            {
                EntryOffset entryOffset = offsetMap.get( entryId );
                byte[] utf8Data = StringTools.getBytesUtf8( ldif + "\n" );
                //long fileLen = ldifFile.length();
                long diff = utf8Data.length - entryOffset.length();

                /* WARN for some unknown reason the below commented
                 * optimization block is causing data corruption in
                 * highly concurrent situations, commenting till we
                 * understand why!
                // check if modified entry occupies the same space
                if ( diff == 0 )
                {
                    System.out.println( "RENAME DATA is same ============" );
                    ldifFile.seek( entryOffset.getStart() );
                    ldifFile.write( utf8Data );
                    System.out.print( "" );
                }
                else if ( fileLen == entryOffset.getEnd() ) // modified entry is at the end of file
                {
                    ldifFile.setLength( entryOffset.getStart() );
                    ldifFile.write( utf8Data );

                    fileLen = ldifFile.length();

                    // change the offsets, the modified entry size changed
                    //if ( fileLen != entryOffset.getEnd() )
                    {
                        entryOffset = new EntryOffset( entryId, entryOffset.getStart(), ldifFile.getFilePointer() );
                        offsetMap.put( entryId, entryOffset );
                    }
                }
                else
                */
                // modified entry size got changed and is in the middle somewhere
                {
                    RandomAccessFile tempBufFile = createTempFile();
                    FileChannel tmpBufChannel = tempBufFile.getChannel();
                    FileChannel mainChannel = ldifFile.getChannel();

                    long count = ( ldifFile.length() - entryOffset.getEnd() );
                    
                    mainChannel.transferTo( entryOffset.getEnd(), count, tmpBufChannel );
                    ldifFile.setLength( entryOffset.getStart() );

                    EntryOffset[] belowParentOffsets = greaterThan( entryOffset );

                    entryOffset = appendLdif( entryId, getAboveEntry( entryOffset ), ldif );

                    for ( EntryOffset o : belowParentOffsets )
                    {
                        o.changeOffsetsBy( diff );
                    }

                    tmpBufChannel.transferTo( 0, tmpBufChannel.size(), mainChannel );
                    tmpBufChannel.truncate( 0 );
                }
            }
            catch ( IOException e )
            {
                throw new LdapException( e );
            }
            finally
            {
                deleteTempFiles();
            }
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
    private EntryOffset appendLdif( Long entryId, EntryOffset aboveEntryOffset, String ldif )
        throws LdapException
    {
        synchronized ( lock )
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
    private EntryOffset[] greaterThan( EntryOffset mark )
    {
        synchronized ( lock )
        {

            Set<EntryOffset> gtSet = new TreeSet<EntryOffset>( comparator );

            for ( EntryOffset o : offsetMap.values() )
            {
                if ( o.getStart() > mark.getStart() )
                {
                    gtSet.add( o );
                }
            }

            EntryOffset[] array = gtSet.toArray( new EntryOffset[0] );
            Arrays.sort( array, comparator );

            return array;
        }
    }


    private EntryOffset[] getBetween( EntryOffset topMark, EntryOffset bottomMmark )
    {
        synchronized ( lock )
        {

            Set<EntryOffset> gtSet = new TreeSet<EntryOffset>( comparator );

            for ( EntryOffset o : offsetMap.values() )
            {
                if ( ( o.getStart() > topMark.getStart() ) && ( o.getEnd() < bottomMmark.getEnd() ) )
                {
                    gtSet.add( o );
                }
            }

            EntryOffset[] array = gtSet.toArray( new EntryOffset[0] );
            Arrays.sort( array, comparator );

            return array;
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


    /** a temporary file used for swapping contents while performing update operations */
    private RandomAccessFile createTempFile() throws IOException
    {
        synchronized ( lock )
        {
            File tmpFile = File.createTempFile( "ldifpartition", ".buf" );
            tmpFile.deleteOnExit();

            RandomAccessFile tempBufFile = new RandomAccessFile( tmpFile.getAbsolutePath(), "rws" );
            tempBufFile.setLength( 0 );

            tempBufMap.put( tmpFile, tempBufFile );
            return tempBufFile;
        }
    }


    /**
     * clears the temp files used as temporary randomaccess buffers
     */
    private void deleteTempFiles()
    {
        synchronized ( lock )
        {
            for ( java.util.Map.Entry<File,RandomAccessFile> f : tempBufMap.entrySet() )
            {
                try
                {
                    f.getValue().close();
                    f.getKey().delete();
                }
                catch( IOException e )
                {
                    LOG.warn( "failed to close the random accessfile {}", f.getKey() );
                }
            }
            
            tempBufMap.clear();
        }
    }
}


/**
 * a holder class for containing an entry's start and end offset positions
 * in the LDIF file
 */
class EntryOffset
{
    /** starting position */
    private long start;

    /** ending position */
    private long end;

    /** entry id */
    private Long id;


    public EntryOffset( Long id, long start, long end )
    {
        this.start = start;
        this.end = end;
        this.id = id;
    }


    public long getStart()
    {
        return start;
    }


    public long getEnd()
    {
        return end;
    }


    public Long getId()
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
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( !( obj instanceof EntryOffset ) )
        {
            return false;
        }

        EntryOffset other = ( EntryOffset ) obj;

        if ( id == null )
        {
            if ( other.id != null )
            {
                return false;
            }
        }
        else if ( !id.equals( other.id ) )
        {
            return false;
        }

        return true;
    }


    @Override
    public String toString()
    {
        return "EntryOffset [start=" + start + ", end=" + end + ", id=" + id + "]";
    }
}
