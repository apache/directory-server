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
import java.util.ArrayList;
import java.util.List;

import jdbm.RecordManager;
import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractStore;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.MultiException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JdbmStore<E> extends AbstractStore<E, Long>
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( JdbmStore.class );

    /** the JDBM record manager used by this database */
    private RecordManager recMan;


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
     * @param schemaManager the schema manager
     * @throws Exception on failure to lookup elements in schemaManager or create database files
     */
    public synchronized void init( SchemaManager schemaManager ) throws Exception
    {
        this.schemaManager = schemaManager;

        // Initialize Attribute types used all over this method
        OBJECT_CLASS_AT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );
        ALIASED_OBJECT_NAME_AT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ALIASED_OBJECT_NAME_AT );
        ENTRY_CSN_AT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_CSN_AT );
        ENTRY_UUID_AT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_UUID_AT );

        partitionDir.mkdirs();

        // First, check if the file storing the data exists
        String path = partitionDir.getPath() + File.separator + "master";
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
        setupSystemIndices();
        setupUserIndices();

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
        }
        else
        {
            LOG.warn( "Supplied index {} is not a JdbmIndex.  "
                + "Will create new JdbmIndex using copied configuration parameters.", index );
            jdbmIndex = new JdbmIndex( index.getAttributeId() );
            jdbmIndex.setCacheSize( index.getCacheSize() );
            jdbmIndex.setNumDupLimit( JdbmIndex.DEFAULT_DUPLICATE_LIMIT );
            jdbmIndex.setWkDirPath( index.getWkDirPath() );
        }

        jdbmIndex.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() ),
            partitionDir );

        return jdbmIndex;
    }

}
