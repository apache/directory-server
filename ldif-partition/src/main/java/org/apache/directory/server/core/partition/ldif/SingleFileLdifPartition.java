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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.naming.InvalidNameException;

import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.shared.partition.OperationExecutionManagerFactory;
import org.apache.directory.server.core.shared.txn.TxnManagerFactory;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.ldif.LdifReader;
import org.apache.directory.shared.ldap.model.ldif.LdifUtils;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.Strings;
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
    private File partitionFile;
    
    /** Shadow File */
    private String ldifShadowFile;

    /** flag to enable/disable re-writing in-memory partition data back to file, default is set to true */
    private volatile boolean enableRewriting = true;

    /** flag used internally to detect if partition data was updated in memory but not on disk */
    private boolean dirty = false;

    /** lock for serializing the operations on the backing LDIF file */
    private Object lock = new Object();

    private static Logger LOG = LoggerFactory.getLogger( SingleFileLdifPartition.class );


    /**
     * Creates a new instance of SingleFileLdifPartition.
     */
    public SingleFileLdifPartition( SchemaManager schemaManager, TxnManagerFactory txnManagerFactory,
        OperationExecutionManagerFactory executionManagerFactory )
    {
        super( schemaManager, txnManagerFactory, executionManagerFactory );
    }


    @Override
    protected void doInit() throws InvalidNameException, Exception
    {
        if ( !initialized )
        {
            if ( getPartitionPath() == null )
            {
                throw new IllegalArgumentException( "Partition path cannot be null" );
            }
    
            partitionFile = new File( getPartitionPath() );
            
            if ( partitionFile.exists() && !partitionFile.isFile() )
            {
                throw new IllegalArgumentException( "Partition path must be a LDIF file" );
            }
    
            ldifShadowFile = partitionFile.getPath() + CONF_SHADOW_FILE_EXTN;
    
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
                suffixDn.apply( schemaManager );
            }
    
            super.doInit();
            
            loadEntries();
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

        LdifEntry ldifEntry = itr.next();

        Entry contextEntry = new DefaultEntry( schemaManager, ldifEntry.getEntry() );

        if ( suffixDn.equals( contextEntry.getDn() ) )
        {
            addMandatoryOpAt( contextEntry );

            AddOperationContext addContext = new AddOperationContext( null, contextEntry );
            executionManager.add( this, addContext );
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
            executionManager.add( this,  addContext );
        }

        parser.close();
    }


    //---------------------------------------------------------------------------------------------
    // Operations
    //---------------------------------------------------------------------------------------------
    
    /**
     * writes the partition's data to the file if {@link #enableRewriting} is set to true
     * and partition was modified since the last write or {@link #dirty} data. 
     * 
     * @throws LdapException
     */
    private void rewritePartitionData() throws LdapException
    {
        synchronized ( lock )
        {

            RandomAccessFile ldifFile = null;
            IndexCursor<UUID> cursor = null;
            
            try
            {
                File shadowFile = new File( ldifShadowFile );
                
                if ( shadowFile.exists() == false )
                {
                    if ( shadowFile.createNewFile() == false )
                    {
                        throw new LdapException( "SingleFileLdifPartition: failed to create shadow file" ); 
                    }
                }
                                 
                ldifFile = new RandomAccessFile( shadowFile, "rw" );
                ldifFile.setLength( 0 ); // wipe the file clean

                UUID suffixId = getEntryId( suffixDn );

                if( suffixId == null )
                {
                    return;
                }
               
                
                cursor = getOneLevelIndex().forwardCursor( suffixId );


                appendLdif( lookup( suffixId ), ldifFile );

                while ( cursor.next() )
                {
                    UUID childId = cursor.get().getId();

                    Entry entry = lookup( childId );

                    appendLdif( entry, ldifFile );

                    appendRecursive( childId, null, ldifFile );
                }
                
                // Finally rename to the partition file
                boolean succeeded = shadowFile.renameTo( partitionFile );
                
                if ( succeeded == false )
                {
                    throw new LdapException( "SingleFileLdifPartition: reanme of shadow file to partition file failed" );
                }

            }
            catch ( LdapException e )
            {
                throw e;
            }
            catch ( Exception e )
            {
                throw new LdapException( e );
            }
            finally
            {
                try
                {
                    if ( ldifFile != null )
                    {
                        ldifFile.close();
                    }
                    
                    if ( cursor != null )
                    {
                        cursor.close();
                    }
                }
                catch( Exception e )
                {
                    throw new LdapException( e );
                }
            }
        }
    }


    /**
     * appends all the entries present under a given entry, recursively
     *
     * @param entryId the base entry's id
     * @param cursorMap the open cursor map
     * @throws Exception
     */
    private void appendRecursive( UUID entryId, Map<UUID, IndexCursor<UUID>> cursorMap, RandomAccessFile ldifFile ) throws Exception
    {
        synchronized ( lock )
        {

            IndexCursor<UUID> cursor = null;
            if ( cursorMap == null )
            {
                cursorMap = new HashMap<UUID, IndexCursor<UUID>>();
            }

            cursor = cursorMap.get( entryId );

            if ( cursor == null )
            {
                cursor = getOneLevelIndex().forwardCursor( entryId );
                cursor.beforeFirst();
                cursorMap.put( entryId, cursor );
            }

            if ( !cursor.next() ) // if this is a leaf entry's Dn
            {
                cursorMap.remove( entryId );
                cursor.close();
            }
            else
            {
                do
                {
                    IndexEntry<UUID> idxEntry = cursor.get();
                    Entry entry = lookup( idxEntry.getId() );

                    UUID childId = getEntryId( entry.getDn() );

                    appendLdif( entry, ldifFile );

                    appendRecursive( childId, cursorMap, ldifFile );
                }
                while ( cursor.next() );
                cursorMap.remove( entryId );
                cursor.close();
            }
        }
    }


    /**
     * append data to the LDIF file
     *
     * @param entry the entry to be written
     * @throws LdapException
     */
    private void appendLdif( Entry entry, RandomAccessFile ldifFile ) throws IOException, LdapException
    {
        synchronized ( lock )
        {
            String ldif = LdifUtils.convertToLdif( entry );
            ldifFile.write( Strings.getBytesUtf8(ldif + "\n") );
        }
    }

    /**
     * an LdifReader backed by a RandomAccessFile
     */
    private class RandomAccessLdifReader extends LdifReader
    {
        private long len;

        RandomAccessFile ldifFile;
        
        public RandomAccessLdifReader() throws LdapException
        {
            try
            {
                ldifFile = new RandomAccessFile( partitionFile, "rw" );
            }
            catch ( FileNotFoundException e )
            {
                throw new LdapException( e.getMessage(), e );
            }
            
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


        @Override
        protected String getLine() throws IOException
        {
            if ( len == 0 )
            {
                return null;
            }

            return ldifFile.readLine();
        }
        
        @Override
        public void close() throws IOException
        {
            if ( ldifFile != null )
            {
                ldifFile.close();
            }
            
            super.close();
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
     * enable/disable the re-writing of partition data.
     * This method internally calls the @see {@link #rewritePartitionData()} to save any dirty data if present
     * 
     * @param enableRewriting flag to enable/disable re-writing
     * @throws LdapException
     */
    public void setEnableRewriting( boolean enableRewriting ) throws LdapException
    {
        this.enableRewriting = enableRewriting;

        // save data if found dirty 
        rewritePartitionData();
    }
    
    /**
     * {@inheritDoc}
     */
    public void sync() throws Exception
    {
        rewritePartitionData();
    }

}
