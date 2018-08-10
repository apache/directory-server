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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.UUID;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.ldif.LdifUtils;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
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

    /** flag to enable/disable re-writing in-memory partition data back to file, default is set to true */
    private volatile boolean enableRewriting = true;

    /** flag used internally to detect if partition data was updated in memory but not on disk */
    private boolean dirty = false;

    /** lock for serializing the operations on the backing LDIF file */
    private Object lock = new Object();

    private static final Logger LOG = LoggerFactory.getLogger( SingleFileLdifPartition.class );


    /**
     * Creates a new instance of SingleFileLdifPartition.
     * 
     * @param schemaManager The SchemaManager instance
     * @param dnFactory The DN factory
     */
    public SingleFileLdifPartition( SchemaManager schemaManager, DnFactory dnFactory )
    {
        super( schemaManager, dnFactory );
    }


    @Override
    protected void doInit() throws LdapException
    {
        if ( !initialized )
        {
            if ( getPartitionPath() == null )
            {
                throw new IllegalArgumentException( "Partition path cannot be null" );
            }

            File partitionFile = new File( getPartitionPath() );

            if ( partitionFile.exists() && !partitionFile.isFile() )
            {
                throw new IllegalArgumentException( "Partition path must be a LDIF file" );
            }

            try
            {
                ldifFile = new RandomAccessFile( partitionFile, "rws" );
            }
            catch ( FileNotFoundException fnfe )
            {
                throw new LdapOtherException( fnfe.getMessage(), fnfe );
            }
            
            LOG.debug( "id is : {}", getId() );

            // Initialize the suffixDirectory : it's a composition
            // of the workingDirectory followed by the suffix
            if ( ( suffixDn == null ) || ( suffixDn.isEmpty() ) )
            {
                String msg = I18n.err( I18n.ERR_150 );
                LOG.error( msg );
                throw new LdapInvalidDnException( msg );
            }

            if ( !suffixDn.isSchemaAware() )
            {
                suffixDn = new Dn( schemaManager, suffixDn );
            }

            super.doInit();

            loadEntries();
        }
    }


    /**
     * load the entries from the LDIF file if present
     * @throws Exception
     */
    private void loadEntries() throws LdapException
    {
        try ( RandomAccessLdifReader parser = new RandomAccessLdifReader( schemaManager ) )
        {
            Iterator<LdifEntry> itr = parser.iterator();
    
            if ( !itr.hasNext() )
            {
                return;
            }
    
            LdifEntry ldifEntry = itr.next();
    
            contextEntry = new DefaultEntry( schemaManager, ldifEntry.getEntry() );
    
            if ( suffixDn.equals( contextEntry.getDn() ) )
            {
                addMandatoryOpAt( contextEntry );
    
                AddOperationContext addContext = new AddOperationContext( null, contextEntry );
                addContext.setPartition( this );
                addContext.setTransaction( this.beginWriteTransaction() );

                super.add( addContext );
            }
            else
            {
                throw new LdapException( "The given LDIF file doesn't contain the context entry" );
            }
    
            while ( itr.hasNext() )
            {
                ldifEntry = itr.next();
    
                Entry entry = new DefaultEntry( schemaManager, ldifEntry.getEntry() );
    
                addMandatoryOpAt( entry );
    
                AddOperationContext addContext = new AddOperationContext( null, entry );
                addContext.setPartition( this );
                addContext.setTransaction( this.beginWriteTransaction() );

                super.add( addContext );
            }
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }
    }


    //---------------------------------------------------------------------------------------------
    // Operations
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        synchronized ( lock )
        {
            super.add( addContext );

            if ( contextEntry == null )
            {
                Entry entry = addContext.getEntry();

                if ( entry.getDn().equals( suffixDn ) )
                {
                    contextEntry = entry;
                }
            }

            dirty = true;
            rewritePartitionData( addContext.getTransaction() );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        PartitionTxn partitionTxn = modifyContext.getTransaction();
        
        synchronized ( lock )
        {
            try
            {
                Entry modifiedEntry = super.modify( partitionTxn, modifyContext.getDn(),
                    modifyContext.getModItems().toArray( new Modification[]
                        {} ) );

                // Remove the EntryDN
                modifiedEntry.removeAttributes( entryDnAT );

                modifyContext.setAlteredEntry( modifiedEntry );
            }
            catch ( Exception e )
            {
                throw new LdapOperationException( e.getMessage(), e );
            }

            dirty = true;
            rewritePartitionData( partitionTxn );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        synchronized ( lock )
        {
            super.rename( renameContext );
            dirty = true;
            rewritePartitionData( renameContext.getTransaction() );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        synchronized ( lock )
        {
            super.move( moveContext );
            dirty = true;
            rewritePartitionData( moveContext.getTransaction() );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext opContext ) throws LdapException
    {
        synchronized ( lock )
        {
            super.moveAndRename( opContext );
            dirty = true;
            rewritePartitionData( opContext.getTransaction() );
        }
    }


    @Override
    public Entry delete( PartitionTxn partitionTxn, String id ) throws LdapException
    {
        synchronized ( lock )
        {
            Entry deletedEntry = super.delete( partitionTxn, id );
            dirty = true;
            rewritePartitionData( partitionTxn );

            return deletedEntry;
        }
    }


    /**
     * writes the partition's data to the file if {@link #enableRewriting} is set to true
     * and partition was modified since the last write or {@link #dirty} data. 
     * 
     * @throws LdapException
     */
    private void rewritePartitionData( PartitionTxn partitionTxn ) throws LdapException
    {
        synchronized ( lock )
        {
            if ( !enableRewriting || !dirty )
            {
                return;
            }

            try
            {
                ldifFile.setLength( 0 ); // wipe the file clean

                String suffixId = getEntryId( partitionTxn, suffixDn );

                if ( suffixId == null )
                {
                    contextEntry = null;
                    return;
                }

                ParentIdAndRdn suffixEntry = rdnIdx.reverseLookup( partitionTxn, suffixId );

                if ( suffixEntry != null )
                {
                    Entry entry = master.get( partitionTxn, suffixId );

                    // Don't write the EntryDN attribute
                    entry.removeAttributes( entryDnAT );

                    entry.setDn( suffixDn );

                    appendLdif( entry );

                    appendRecursive( partitionTxn, suffixId, suffixEntry.getNbChildren() );
                }

                dirty = false;
            }
            catch ( LdapException e )
            {
                throw e;
            }
            catch ( Exception e )
            {
                throw new LdapException( e );
            }
        }
    }


    private void appendRecursive( PartitionTxn partitionTxn, String id, int nbSibbling ) throws Exception
    {
        // Start with the root
        Cursor<IndexEntry<ParentIdAndRdn, String>> cursor = rdnIdx.forwardCursor( partitionTxn );

        IndexEntry<ParentIdAndRdn, String> startingPos = new IndexEntry<>();
        startingPos.setKey( new ParentIdAndRdn( id, ( Rdn[] ) null ) );
        cursor.before( startingPos );
        int countChildren = 0;

        while ( cursor.next() && ( countChildren < nbSibbling ) )
        {
            IndexEntry<ParentIdAndRdn, String> element = cursor.get();
            String childId = element.getId();
            Entry entry = fetch( partitionTxn, childId );

            // Remove the EntryDn
            entry.removeAttributes( SchemaConstants.ENTRY_DN_AT );

            appendLdif( entry );

            countChildren++;

            // And now, the children
            int nbChildren = element.getKey().getNbChildren();

            if ( nbChildren > 0 )
            {
                appendRecursive( partitionTxn, childId, nbChildren );
            }
        }

        cursor.close();
    }


    /**
     * append data to the LDIF file
     *
     * @param entry the entry to be written
     * @throws LdapException
     */
    private void appendLdif( Entry entry ) throws IOException, LdapException
    {
        synchronized ( lock )
        {
            String ldif = LdifUtils.convertToLdif( entry );
            ldifFile.write( Strings.getBytesUtf8( ldif + "\n" ) );
        }
    }

    /**
     * an LdifReader backed by a RandomAccessFile
     */
    private class RandomAccessLdifReader extends LdifReader
    {
        private long len;


        RandomAccessLdifReader() throws LdapException
        {
            try
            {
                len = ldifFile.length();
                super.init();
            }
            catch ( IOException e )
            {
                LdapException le = new LdapException( e.getMessage(), e );
                le.initCause( e );

                throw le;
            }
        }


        RandomAccessLdifReader( SchemaManager schemaManager ) throws LdapException
        {
            try
            {
                this.schemaManager = schemaManager;
                len = ldifFile.length();
                super.init();
            }
            catch ( IOException e )
            {
                LdapException le = new LdapException( e.getMessage(), e );
                le.initCause( e );

                throw le;
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


    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDestroy( PartitionTxn partitionTxn ) throws LdapException
    {
        super.doDestroy( partitionTxn );
        
        try
        {
            ldifFile.close();
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }
    }


    /**
     * enable/disable the re-writing of partition data.
     * This method internally calls the rewritePartitionData() method to save any dirty data if present
     * 
     * @param partitionTxn The transaction to use
     * @param enableRewriting flag to enable/disable re-writing
     * @throws LdapException If we weren't able to save the dirty data
     */
    public void setEnableRewriting( PartitionTxn partitionTxn, boolean enableRewriting ) throws LdapException
    {
        this.enableRewriting = enableRewriting;

        // save data if found dirty 
        rewritePartitionData( partitionTxn );
    }
}
