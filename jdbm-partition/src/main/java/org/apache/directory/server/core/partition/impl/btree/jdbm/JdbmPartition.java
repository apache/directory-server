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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jdbm.RecordManager;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.SnapshotRecordManager;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.AbstractBTreePartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.search.impl.CursorBuilder;
import org.apache.directory.server.xdbm.search.impl.DefaultOptimizer;
import org.apache.directory.server.xdbm.search.impl.DefaultSearchEngine;
import org.apache.directory.server.xdbm.search.impl.EvaluatorBuilder;
import org.apache.directory.server.xdbm.search.impl.NoOpOptimizer;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.exception.MultiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link Partition} that stores entries in
 * <a href="http://jdbm.sourceforge.net/">JDBM</a> database.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmPartition extends AbstractBTreePartition<Long>
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( JdbmPartition.class );

    private static final String JDBM_DB_FILE_EXTN = ".db";

    private static final FilenameFilter DB_FILTER = new FilenameFilter()
    {

        public boolean accept( File dir, String name )
        {
            // really important to filter master.db and master.lg files
            return ( name.endsWith( JDBM_DB_FILE_EXTN ) && !name.startsWith( "master." ) );
        }
    };

    /** the JDBM record manager used by this database */
    private RecordManager recMan;


    /**
     * Creates a store based on JDBM B+Trees.
     */
    public JdbmPartition( SchemaManager schemaManager )
    {
        super( schemaManager );

        // Initialize the cache size
        if ( cacheSize < 0 )
        {
            cacheSize = DEFAULT_CACHE_SIZE;
            LOG.debug( "Using the default entry cache size of {} for {} partition", cacheSize, id );
        }
        else
        {
            LOG.debug( "Using the custom configured cache size of {} for {} partition", cacheSize, id );
        }
    }


    protected void doInit() throws Exception
    {
        if ( !initialized )
        {
            // setup optimizer and registries for parent
            if ( !optimizerEnabled )
            {
                optimizer = new NoOpOptimizer();
            }
            else
            {
                optimizer = new DefaultOptimizer<Entry, Long>( this );
            }

            EvaluatorBuilder<Long> evaluatorBuilder = new EvaluatorBuilder<Long>( this, schemaManager );
            CursorBuilder<Long> cursorBuilder = new CursorBuilder<Long>( this, evaluatorBuilder );

            searchEngine = new DefaultSearchEngine<Long>( this, cursorBuilder, evaluatorBuilder, optimizer );

            // Create the underlying directories (only if needed)
            File partitionDir = new File( getPartitionPath() );
            if ( !partitionDir.exists() && !partitionDir.mkdirs() )
            {
                throw new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECORY, partitionDir ) );
            }

            // Initialize the indexes
            super.doInit();

            // First, check if the file storing the data exists
            String path = partitionDir.getPath() + File.separator + "master";
            BaseRecordManager baseRecordManager = new BaseRecordManager( path );
            baseRecordManager.disableTransactions();

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
            recMan = new SnapshotRecordManager( baseRecordManager, cacheSize );

            // Create the master table (the table containing all the entries)
            master = new JdbmMasterTable<Entry>( recMan, schemaManager );

            // get all index db files first
            File[] allIndexDbFiles = partitionDir.listFiles( DB_FILTER );

            // get the names of the db files also
            List<String> indexDbFileNameList = Arrays.asList( partitionDir.list( DB_FILTER ) );

            // then add all index objects to a list
            List<String> allIndices = new ArrayList<String>();

            for ( Index<?, Entry, Long> index : systemIndices.values() )
            {
                allIndices.add( index.getAttribute().getOid() );
            }

            // this loop is used for two purposes
            // one for collecting all user indices
            // two for finding a new index to be built
            // just to avoid another iteration for determining which is the new index
            for ( Index<?, Entry, Long> index : userIndices.values() )
            {
                allIndices.add( index.getAttributeId() );

                // take the part after removing .db from the
                String name = index.getAttributeId() + JDBM_DB_FILE_EXTN;

                // if the name doesn't exist in the list of index DB files
                // this is a new index and we need to build it
                if ( !indexDbFileNameList.contains( name ) )
                {
                    buildUserIndex( index );
                }
            }

            deleteUnusedIndexFiles( allIndices, allIndexDbFiles );

            // We are done !
            initialized = true;
        }
    }


    /**
     * {@inheritDoc}}
     */
    public Long getDefaultId()
    {
        return 1L;
    }


    /**
     * {@inheritDoc}
     */
    public Long getRootId()
    {
        return 0L;
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

        // Sync all system indices
        for ( Index<?, Entry, Long> idx : systemIndices.values() )
        {
            idx.sync();
        }

        // Sync all user defined userIndices
        for ( Index<?, Entry, Long> idx : userIndices.values() )
        {
            idx.sync();
        }

        ( ( JdbmMasterTable<Entry> ) master ).sync();
        recMan.commit();
    }


    /**
     * builds a user defined index on a attribute by browsing all the entries present in master db
     * 
     * @param userIdx then user defined index
     * @throws Exception in case of any problems while building the index
     */
    private void buildUserIndex( Index userIdx ) throws Exception
    {
        AttributeType atType = userIdx.getAttribute();

        LOG.info( "building the index for attribute type {}", atType );

        Cursor<Tuple<Long, Entry>> cursor = master.cursor();
        cursor.beforeFirst();

        String attributeOid = userIdx.getAttribute().getOid();

        while ( cursor.next() )
        {
            Tuple<Long, Entry> tuple = cursor.get();

            Long id = tuple.getKey();
            Entry entry = tuple.getValue();

            Attribute entryAttr = entry.get( atType );

            if ( entryAttr != null )
            {
                for ( Value<?> value : entryAttr )
                {
                    userIdx.add( value.getValue(), id );
                }

                // Adds only those attributes that are indexed
                presenceIdx.add( attributeOid, id );
            }
        }

        cursor.close();
    }


    /**
     * removes any unused/removed attribute index files present under the partition's
     * working directory
     */
    private void deleteUnusedIndexFiles( List<String> allIndices, File[] dbFiles )
    {
        for ( File file : dbFiles )
        {
            String name = file.getName();
            // take the part after removing .db from the
            name = name.substring( 0, name.lastIndexOf( JDBM_DB_FILE_EXTN ) );

            // remove the file if not found in the list of names of indices
            if ( !allIndices.contains( name ) )
            {
                boolean deleted = file.delete();

                if ( deleted )
                {
                    LOG.info( "Deleted unused index file {}", file.getAbsolutePath() );

                    try
                    {
                        String atName = schemaManager.lookupAttributeTypeRegistry( name ).getName();
                        File txtFile = new File( file.getParent(), name + "-" + atName + ".txt" );

                        deleted = txtFile.delete();

                        if ( !deleted )
                        {
                            LOG.info( "couldn't delete the index name helper file {}", txtFile );
                        }
                    }
                    catch ( Exception e )
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


    /**
     * {@inheritDoc}
     */
    protected Index<?, Entry, Long> convertAndInit( Index<?, Entry, Long> index ) throws Exception
    {
        JdbmIndex<?, Entry> jdbmIndex;

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
            jdbmIndex = ( JdbmIndex<?, Entry> ) index;

            if ( jdbmIndex.getWkDirPath() == null )
            {
                jdbmIndex.setWkDirPath( partitionPath );
            }
        }
        else
        {
            LOG.debug( "Supplied index {} is not a JdbmIndex.  "
                + "Will create new JdbmIndex using copied configuration parameters.", index );
            jdbmIndex = new JdbmIndex( index.getAttributeId(), true );
            jdbmIndex.setCacheSize( index.getCacheSize() );
            jdbmIndex.setNumDupLimit( JdbmIndex.DEFAULT_DUPLICATE_LIMIT );
            jdbmIndex.setWkDirPath( index.getWkDirPath() );
        }

        jdbmIndex.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() ) );

        return jdbmIndex;
    }


    /**
     * {@inheritDoc}
     */
    protected synchronized void doDestroy() throws Exception
    {
        MultiException errors = new MultiException( I18n.err( I18n.ERR_577 ) );

        if ( !initialized )
        {
            return;
        }

        try
        {
            super.doDestroy();
        }
        catch ( Exception e )
        {
            errors.addThrowable( e );
        }

        // This is specific to the JDBM store : close the record manager
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
    }


    /**
     * {@inheritDoc}
     */
    protected final Index createSystemIndex( String oid, URI path, boolean withReverse )  throws Exception
    {
        LOG.debug( "Supplied index {} is not a JdbmIndex.  " +
         "Will create new JdbmIndex using copied configuration parameters." );
        JdbmIndex<?, Entry> jdbmIndex;

        if ( oid.equals( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
        {
            jdbmIndex = new JdbmRdnIndex();
            jdbmIndex.setAttributeId( ApacheSchemaConstants.APACHE_RDN_AT_OID );
            jdbmIndex.setNumDupLimit( JdbmIndex.DEFAULT_DUPLICATE_LIMIT );
        }
        else
        {
            jdbmIndex = new JdbmIndex( oid, withReverse );
            jdbmIndex.setNumDupLimit( JdbmIndex.DEFAULT_DUPLICATE_LIMIT );
        }

        jdbmIndex.setWkDirPath( path );

        return jdbmIndex;
    }
}
