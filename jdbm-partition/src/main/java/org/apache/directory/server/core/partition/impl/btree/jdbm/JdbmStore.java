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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jdbm.RecordManager;
import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractStore;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.util.exception.MultiException;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An implementation of the Store interface using the Jdbm backend.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmStore<E> extends AbstractStore<E, Long>
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( JdbmStore.class );

    /** the JDBM record manager used by this database */
    private RecordManager recMan;

    private static final String JDBM_DB_FILE_EXTN = ".db";
    
    private static final FilenameFilter DB_FILTER = new FilenameFilter()
    {
        
        public boolean accept( File dir, String name )
        {
            // really important to filter master.db and master.lg files
            return ( name.endsWith( JDBM_DB_FILE_EXTN ) && !name.startsWith( "master." ) );
        }
    };

    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------
    /**
     * Creates a store based on JDBM B+Trees.
     */
    public JdbmStore()
    {
    }


    // -----------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // -----------------------------------------------------------------------
    public Long getDefaultId()
    {
        return 1L;
    };


    @Override
    protected Long getRootId()
    {
        return 0L;
    }


    /**
     * Initialize the JDBM storage system.
     *
     * @param schemaManager The server schemaManager
     * @throws Exception on failure to lookup elements in schemaManager or create database files
     */
    public synchronized void init( SchemaManager schemaManager ) throws Exception
    {
        super.init( schemaManager );

        getPartitionDir().mkdirs();

        // First, check if the file storing the data exists
        String path = getPartitionDir().getPath() + File.separator + "master";
        BaseRecordManager base = new BaseRecordManager( path );
        base.disableTransactions();

        if ( cacheSize < 0 )
        {
            cacheSize = DEFAULT_CACHE_SIZE;
            LOG.debug( "Using the default entry cache size of {} for {} partition", cacheSize, id );
        }
        else
        {
            LOG.debug( "Using the custom configured cache size of {} for {} partition", cacheSize, id );
        }

        // Now, create the entry cache for this partition
        recMan = new CacheRecordManager( base, new MRU( cacheSize ) );

        // Create the master table (the table containing all the entries)
        master = new JdbmMasterTable<Entry>( recMan, schemaManager );

        // -------------------------------------------------------------------
        // Initializes the user and system indices
        // -------------------------------------------------------------------

        // get all index db files first
        File[] allIndexDbFiles = getPartitionDir().listFiles( DB_FILTER );
        
        // get the names of the db files also
        List<String> indexDbFileNameList = Arrays.asList( getPartitionDir().list( DB_FILTER ) );
        
        setupSystemIndices();
        setupUserIndices();

        // then add all index objects to a list
        List<String> allIndices = new ArrayList<String>();
        for( Index i : systemIndices.values() )
        {
            allIndices.add( i.getAttribute().getOid() );
        }

        // this loop is used for two purposes
        // one for collecting all user indices
        // two for finding a new index to be built
        // just to avoid another iteration for determining which is the new index
        for( Index i : userIndices.values() )
        {
            allIndices.add( i.getAttribute().getOid() );

            // take the part after removing .db from the  
            String name = i.getAttribute().getOid() + JDBM_DB_FILE_EXTN;

            // if the name doesn't exist in the list of index DB files
            // this is a new index and we need to build it
            if( !indexDbFileNameList.contains( name ) )
            {
                buildUserIndex( i );
            }
        }

        deleteUnusedIndexFiles( allIndices, allIndexDbFiles );
        
        // We are done !
        initialized = true;
    }


    /**
     * Close the partition : we have to close all the userIndices and the master table.
     * 
     * @throws Exception lazily thrown on any closer failures to avoid leaving
     * open files
     */
    public synchronized void destroy() throws Exception
    {
        LOG.debug( "destroy() called on store for {}", this.suffixDn );

        if ( !initialized )
        {
            return;
        }

        List<Index<?, E, Long>> array = new ArrayList<Index<?, E, Long>>();
        array.addAll( userIndices.values() );
        array.addAll( systemIndices.values() );
        MultiException errors = new MultiException( I18n.err( I18n.ERR_577 ) );

        for ( Index<?, E, Long> index : array )
        {
            try
            {
                index.close();
                LOG.debug( "Closed {} index for {} partition.", index.getAttributeId(), suffixDn );
            }
            catch ( Throwable t )
            {
                LOG.error( I18n.err( I18n.ERR_124 ), t );
                errors.addThrowable( t );
            }
        }

        try
        {
            master.close();
            LOG.debug( I18n.err( I18n.ERR_125, suffixDn ) );
        }
        catch ( Throwable t )
        {
            LOG.error( I18n.err( I18n.ERR_126 ), t );
            errors.addThrowable( t );
        }

        try
        {
            recMan.close();
            LOG.debug( "Closed record manager for {} partition.", suffixDn );
        }
        catch ( Throwable t )
        {
            LOG.error( I18n.err( I18n.ERR_127 ), t );
            errors.addThrowable( t );
        }

        if ( errors.size() > 0 )
        {
            throw errors;
        }

        initialized = false;
    }


    /**
     * This method is called when the synch thread is waking up, to write
     * the modified data.
     * 
     * @throws Exception on failures to sync database files to disk
     */
    public synchronized void sync() throws Exception
    {
        if ( !initialized )
        {
            return;
        }

        List<Index<?, E, Long>> array = new ArrayList<Index<?, E, Long>>();
        array.addAll( userIndices.values() );
        array.add( aliasIdx );
        array.add( oneAliasIdx );
        array.add( subAliasIdx );
        array.add( oneLevelIdx );
        array.add( presenceIdx );
        array.add( subLevelIdx );
        array.add( entryCsnIdx );
        array.add( entryUuidIdx );
        array.add( objectClassIdx );

        // Sync all user defined userIndices
        for ( Index<?, E, Long> idx : array )
        {
            idx.sync();
        }

        rdnIdx.sync();

        ( ( JdbmMasterTable<Entry> ) master ).sync();
        recMan.commit();
    }


    // ------------------------------------------------------------------------
    // I N D E X   M E T H O D S
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    protected Index<?, E, Long> convertAndInit( Index<?, E, Long> index ) throws Exception
    {
        JdbmIndex<?, E> jdbmIndex;
        
        if ( index.getAttributeId().equals( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
        {
            jdbmIndex = new JdbmRdnIndex();
            jdbmIndex.setAttributeId( ApacheSchemaConstants.APACHE_RDN_AT_OID );
            jdbmIndex.setCacheSize( index.getCacheSize() );
            jdbmIndex.setNumDupLimit( JdbmIndex.DEFAULT_DUPLICATE_LIMIT );
            jdbmIndex.setWkDirPath( index.getWkDirPath() );
        }
        else if ( index instanceof JdbmIndex<?, ?> )
        {
            jdbmIndex = ( JdbmIndex<?, E> ) index;
            
            if ( jdbmIndex.getWkDirPath() == null )
            {
                jdbmIndex.setWkDirPath( partitionPath );
            }
        }
        else
        {
            LOG.debug( "Supplied index {} is not a JdbmIndex.  "
                + "Will create new JdbmIndex using copied configuration parameters.", index );
            jdbmIndex = new JdbmIndex( index.getAttributeId() );
            jdbmIndex.setCacheSize( index.getCacheSize() );
            jdbmIndex.setNumDupLimit( JdbmIndex.DEFAULT_DUPLICATE_LIMIT );
            jdbmIndex.setWkDirPath( index.getWkDirPath() );
        }

        jdbmIndex.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() ) );

        return jdbmIndex;
    }

    
    /**
     * removes any unused/removed attribute index files present under the partition's
     * working directory
     */
    private void deleteUnusedIndexFiles( List<String> allIndices, File[] dbFiles )
    {
        for( File file : dbFiles )
        {
            String name = file.getName();
            // take the part after removing .db from the  
            name = name.substring( 0, name.lastIndexOf( JDBM_DB_FILE_EXTN ) );
            
            // remove the file if not found in the list of names of indices
            if( !allIndices.contains( name ) )
            {
                boolean deleted = file.delete();
                if( deleted )
                {
                    LOG.info( "Deleted unused index file {}", file.getAbsolutePath() );

                    try
                    {
                        String atName = schemaManager.lookupAttributeTypeRegistry( name ).getName();
                        File txtFile = new File( file.getParent(), name + "-" + atName + ".txt" );
                        
                        deleted = txtFile.delete();
                        
                        if( !deleted )
                        {
                            LOG.info( "couldn't delete the index name helper file {}", txtFile );
                        }
                    }
                    catch( Exception e )
                    {
                        LOG.warn( "couldn't find the attribute's name with oid {}", name );
                        LOG.warn( "", e );
                    }
                    
                    
                }
                else
                {
                    LOG.warn( "Failed to delete unused index file {}", file.getAbsolutePath() );
                }
            }
        }
    }


    private File getPartitionDir()
    {
        return new File( getPartitionPath() );
    }


    /**
     * builds a user defined index on a attribute by browsing all the entries present in master db
     * 
     * @param userIdx then user defined index
     * @throws Exception in case of any problems while building the index
     */
    public void buildUserIndex( Index userIdx ) throws Exception
    {
        AttributeType atType = userIdx.getAttribute();

        LOG.info( "building the index for attribute type {}", atType );
        
        Cursor<Tuple<Long,Entry>> cursor = master.cursor();
        cursor.beforeFirst();
        
        String attributeOid = userIdx.getAttribute().getOid();
        
        while( cursor.next() )
        {
            Tuple<Long,Entry> tuple = cursor.get();
            
            Long id = tuple.getKey();
            Entry entry = tuple.getValue();
            
            EntryAttribute entryAttr = entry.get( atType );
            if( entryAttr != null )
            {
                for ( Value<?> value : entryAttr )
                {
                    userIdx.add( value.get(), id );
                }
                
                // Adds only those attributes that are indexed
                presenceIdx.add( attributeOid, id );
            }
        }
        
        cursor.close();
    }
}
