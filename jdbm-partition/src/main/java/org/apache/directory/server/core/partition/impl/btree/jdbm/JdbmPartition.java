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
import java.util.UUID;

import jdbm.RecordManager;
import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.exception.MultiException;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.AbstractBTreePartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.search.impl.CursorBuilder;
import org.apache.directory.server.xdbm.search.impl.DefaultOptimizer;
import org.apache.directory.server.xdbm.search.impl.DefaultSearchEngine;
import org.apache.directory.server.xdbm.search.impl.EvaluatorBuilder;
import org.apache.directory.server.xdbm.search.impl.NoOpOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link Partition} that stores entries in
 * <a href="http://jdbm.sourceforge.net/">JDBM</a> database.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmPartition extends AbstractBTreePartition
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

    /** the entry cache */
    private Cache entryCache;

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
                optimizer = new DefaultOptimizer<Entry>( this );
            }

            EvaluatorBuilder evaluatorBuilder = new EvaluatorBuilder( this, schemaManager );
            CursorBuilder cursorBuilder = new CursorBuilder( this, evaluatorBuilder );

            searchEngine = new DefaultSearchEngine( this, cursorBuilder, evaluatorBuilder, optimizer );

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

            recMan = new CacheRecordManager( base, new MRU( cacheSize ) );

            // Create the master table (the table containing all the entries)
            master = new JdbmMasterTable( recMan, schemaManager );

            // get all index db files first
            File[] allIndexDbFiles = partitionDir.listFiles( DB_FILTER );

            // get the names of the db files also
            List<String> indexDbFileNameList = Arrays.asList( partitionDir.list( DB_FILTER ) );

            // then add all index objects to a list
            List<String> allIndices = new ArrayList<String>();

            for ( Index<?, Entry, String> index : systemIndices.values() )
            {
                allIndices.add( index.getAttribute().getOid() );
            }

            List<Index<?, Entry, String>> indexToBuild = new ArrayList<Index<?, Entry, String>>();

            // this loop is used for two purposes
            // one for collecting all user indices
            // two for finding a new index to be built
            // just to avoid another iteration for determining which is the new index
            for ( Index<?, Entry, String> index : userIndices.values() )
            {
                String indexOid = index.getAttribute().getOid();
                allIndices.add( indexOid );

                // take the part after removing .db from the
                String name = indexOid + JDBM_DB_FILE_EXTN;

                // if the name doesn't exist in the list of index DB files
                // this is a new index and we need to build it
                if ( !indexDbFileNameList.contains( name ) )
                {
                    indexToBuild.add( index );
                }
            }

            if ( indexToBuild.size() > 0 )
            {
                buildUserIndex( indexToBuild );
            }

            deleteUnusedIndexFiles( allIndices, allIndexDbFiles );

            // Initialization of the context entry
            if ( ( suffixDn != null ) && ( contextEntry != null ) )
            {
                Dn contextEntryDn = contextEntry.getDn();

                // Checking if the context entry DN is schema aware
                if ( !contextEntryDn.isSchemaAware() )
                {
                    contextEntryDn.apply( schemaManager );
                }

                // We're only adding the entry if the two DNs are equal
                if ( suffixDn.equals( contextEntryDn ) )
                {
                    // Looking for the current context entry
                    Entry suffixEntry = lookup( new LookupOperationContext( null, suffixDn ) );

                    // We're only adding the context entry if it doesn't already exist
                    if ( suffixEntry == null )
                    {
                        // Checking of the context entry is schema aware
                        if ( !contextEntry.isSchemaAware() )
                        {
                            // Making the context entry schema aware
                            contextEntry = new DefaultEntry( schemaManager, contextEntry );
                        }

                        // Adding the 'entryCsn' attribute
                        if ( contextEntry.get( SchemaConstants.ENTRY_CSN_AT ) == null )
                        {
                            contextEntry.add( SchemaConstants.ENTRY_CSN_AT, new CsnFactory( 0 ).newInstance()
                                .toString() );
                        }

                        // Adding the 'entryUuid' attribute
                        if ( contextEntry.get( SchemaConstants.ENTRY_UUID_AT ) == null )
                        {
                            String uuid = UUID.randomUUID().toString();
                            contextEntry.add( SchemaConstants.ENTRY_UUID_AT, uuid );
                        }

                        // And add this entry to the underlying partition
                        add( new AddOperationContext( null, contextEntry ) );
                    }
                }
            }

            if( cacheService != null )
            {
                entryCache = cacheService.getCache( getId() );
            }
            
            // We are done !
            initialized = true;
        }
    }


    /**
     * {@inheritDoc}}
     */
    public String getDefaultId()
    {
        return Partition.DEFAULT_ID;
    }


    /**
     * {@inheritDoc}
     */
    public String getRootId()
    {
        return Partition.ROOT_ID;
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
        for ( Index<?, Entry, String> idx : systemIndices.values() )
        {
            idx.sync();
        }

        // Sync all user defined userIndices
        for ( Index<?, Entry, String> idx : userIndices.values() )
        {
            idx.sync();
        }

        ( ( JdbmMasterTable ) master ).sync();
        recMan.commit();
    }


    /**
     * Builds user defined indexes on a attributes by browsing all the entries present in master db
     * 
     * @param userIndexes then user defined indexes to create
     * @throws Exception in case of any problems while building the index
     */
    private void buildUserIndex( List<Index<?, Entry, String>> userIndexes ) throws Exception
    {
        Cursor<Tuple<String, Entry>> cursor = master.cursor();
        cursor.beforeFirst();

        while ( cursor.next() )
        {
            for ( Index index : userIndexes )
            {
                AttributeType atType = index.getAttribute();

                String attributeOid = index.getAttribute().getOid();

                LOG.info( "building the index for attribute type {}", atType );

                Tuple<String, Entry> tuple = cursor.get();

                String id = tuple.getKey();
                Entry entry = tuple.getValue();

                Attribute entryAttr = entry.get( atType );

                if ( entryAttr != null )
                {
                    for ( Value<?> value : entryAttr )
                    {
                        index.add( value.getValue(), id );
                    }

                    // Adds only those attributes that are indexed
                    presenceIdx.add( attributeOid, id );
                }
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
    protected Index<?, Entry, String> convertAndInit( Index<?, Entry, String> index ) throws Exception
    {
        JdbmIndex<?, Entry> jdbmIndex;

        if ( index instanceof JdbmRdnIndex )
        {
            jdbmIndex = ( JdbmRdnIndex ) index;
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
        finally
        {
            if( entryCache != null )
            {
                entryCache.removeAll();
            }
        }
        
        if ( errors.size() > 0 )
        {
            throw errors;
        }
    }


    /**
     * {@inheritDoc}
     */
    protected final Index createSystemIndex( String oid, URI path, boolean withReverse ) throws Exception
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


    @Override
    public void updateCache( OperationContext opCtx )
    {
        if( entryCache == null )
        {
            return;
        }
        
        try
        {
            if( opCtx instanceof ModifyOperationContext )
            {
                // replace the entry
                ModifyOperationContext modCtx = ( ModifyOperationContext ) opCtx;
                Entry entry = modCtx.getAlteredEntry();
                String id = entry.get( SchemaConstants.ENTRY_UUID_AT ).getString();
                
                if( entry instanceof ClonedServerEntry )
                {
                    entry = ( ( ClonedServerEntry ) entry ).getOriginalEntry();
                }
                
                entryCache.replace( new Element( id, entry ) );
            }
            else if( ( opCtx instanceof MoveOperationContext ) ||
                ( opCtx instanceof MoveAndRenameOperationContext ) ||
                ( opCtx instanceof RenameOperationContext ) )
            {
                // clear the cache it is not worth updating all the children
                entryCache.removeAll();
            }
            else if( opCtx instanceof DeleteOperationContext )
            {
                // delete the entry
                DeleteOperationContext delCtx = ( DeleteOperationContext ) opCtx;
                entryCache.remove( delCtx.getEntry().get( SchemaConstants.ENTRY_UUID_AT ).getString() );
            }
        }
        catch( LdapException e )
        {
            LOG.warn( "Failed to update entry cache", e );
        }
    }


    @Override
    public Entry lookupCache( String id )
    {
        if( entryCache == null )
        {
            return null;
        }

        Element el = entryCache.get( id );
        
        if( el != null )
        {
            return ( Entry ) el.getValue();
        }
        
        return null;
    }


    @Override
    public void addToCache( String id, Entry entry )
    {
        if( entryCache == null )
        {
            return;
        }

        if( entry instanceof ClonedServerEntry )
        {
            entry = ( ( ClonedServerEntry ) entry ).getOriginalEntry();
        }
        
        entryCache.put( new Element( id, entry ) );
    }
    
}
