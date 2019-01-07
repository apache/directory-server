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
package org.apache.directory.server.core.partition.impl.btree.mavibot;


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ehcache.Cache;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.exception.MultiException;
import org.apache.directory.mavibot.btree.RecordManager;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionReadTxn;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.api.partition.PartitionWriteTxn;
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
 * A Mavibot partition
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MavibotPartition extends AbstractBTreePartition
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( MavibotPartition.class );

    private static final String MAVIBOT_DB_FILE_EXTN = ".data";
    
    private static final FilenameFilter DB_FILTER = new FilenameFilter()
    {

        public boolean accept( File dir, String name )
        {
            // really important to filter master.db and master.lg files
            return ( name.endsWith( MAVIBOT_DB_FILE_EXTN ) && !name.startsWith( "master." ) );
        }
    };

    private RecordManager recordMan;

    /** the entry cache */
    private Cache< String, Entry > entryCache;


    public MavibotPartition( SchemaManager schemaManager, DnFactory dnFactory )
    {
        super( schemaManager, dnFactory );

        MavibotEntrySerializer.setSchemaManager( schemaManager );

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
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRepair() throws LdapException
    {
        // Nothing to do
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInit() throws LdapException
    {
        if ( !initialized )
        {
            // setup optimizer and registries for parent
            if ( !isOptimizerEnabled() )
            {
                setOptimizer( new NoOpOptimizer() );
            }
            else
            {
                setOptimizer( new DefaultOptimizer( this ) );
            }

            EvaluatorBuilder evaluatorBuilder = new EvaluatorBuilder( this, schemaManager );
            CursorBuilder cursorBuilder = new CursorBuilder( this, evaluatorBuilder );

            setSearchEngine( new DefaultSearchEngine( this, cursorBuilder, evaluatorBuilder, getOptimizer() ) );

            // Create the underlying directories (only if needed)
            File partitionDir = new File( getPartitionPath() );

            if ( !partitionDir.exists() && !partitionDir.mkdirs() )
            {
                throw new LdapOtherException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECTORY, partitionDir ) );
            }

            if ( cacheSize < 0 )
            {
                cacheSize = DEFAULT_CACHE_SIZE;
                LOG.debug( "Using the default entry cache size of {} for {} partition", cacheSize, id );
            }
            else
            {
                LOG.debug( "Using the custom configured cache size of {} for {} partition", cacheSize, id );
            }

            recordMan = new RecordManager( partitionDir.getPath() );

            // Initialize the indexes
            super.doInit();

            // First, check if the file storing the data exists

            try
            {
                master = new MavibotMasterTable( recordMan, schemaManager, "master", cacheSize );
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }

            // get all index db files first
            File[] allIndexDbFiles = partitionDir.listFiles( DB_FILTER );

            // get the names of the db files also
            List<String> indexDbFileNameList = Arrays.asList( partitionDir.list( DB_FILTER ) );

            // then add all index objects to a list
            List<String> allIndices = new ArrayList<>();

            for ( Index<?, String> index : systemIndices.values() )
            {
                allIndices.add( index.getAttribute().getOid() );
            }

            List<Index<?, String>> indexToBuild = new ArrayList<>();

            // this loop is used for two purposes
            // one for collecting all user indices
            // two for finding a new index to be built
            // just to avoid another iteration for determining which is the new index
            /* FIXME the below code needs to be modified to suit Mavibot
                        for ( Index<?, Entry, String> index : userIndices.values() )
                        {
                            String indexOid = index.getAttribute().getOid();
                            allIndices.add( indexOid );

                            // take the part after removing .db from the
                            String name = indexOid + MAVIBOT_DB_FILE_EXTN;

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
            */

            if ( cacheService != null )
            {
                entryCache = cacheService.getCache( getId(), String.class, Entry.class );
            }

            // We are done !
            initialized = true;
        }
    }


    @Override
    protected Index<?, String> convertAndInit( Index<?, String> index ) throws LdapException
    {
        MavibotIndex<?> mavibotIndex;

        if ( index instanceof MavibotRdnIndex )
        {
            mavibotIndex = ( MavibotRdnIndex ) index;
        }
        else if ( index instanceof MavibotDnIndex )
        {
            mavibotIndex = ( MavibotDnIndex ) index;
        }
        else if ( index instanceof MavibotIndex<?> )
        {
            mavibotIndex = ( MavibotIndex<?> ) index;

            if ( mavibotIndex.getWkDirPath() == null )
            {
                mavibotIndex.setWkDirPath( partitionPath );
            }
        }
        else
        {
            LOG.debug( "Supplied index {} is not a MavibotIndex.  "
                + "Will create new MavibotIndex using copied configuration parameters.", index );
            mavibotIndex = new MavibotIndex( index.getAttributeId(), true );
            mavibotIndex.setCacheSize( index.getCacheSize() );
            mavibotIndex.setWkDirPath( index.getWkDirPath() );
        }

        mavibotIndex.setRecordManager( recordMan );

        try
        {
            mavibotIndex.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() ) );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        return mavibotIndex;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void doDestroy( PartitionTxn partitionTxn ) throws LdapException
    {
        MultiException errors = new MultiException( I18n.err( I18n.ERR_577 ) );

        if ( !initialized )
        {
            return;
        }

        try
        {
            super.doDestroy( partitionTxn );
        }
        catch ( Exception e )
        {
            errors.addThrowable( e );
        }

        // This is specific to the MAVIBOT store : close the record manager
        try
        {
            recordMan.close();
            LOG.debug( "Closed record manager for {} partition.", suffixDn );
        }
        catch ( Throwable t )
        {
            LOG.error( I18n.err( I18n.ERR_127 ), t );
            errors.addThrowable( t );
        }
        finally
        {
            if ( entryCache != null )
            {
                entryCache.clear();
            }
        }

        if ( errors.size() > 0 )
        {
            throw new LdapOtherException( errors.getMessage(), errors );
        }
    }


    @Override
    protected Index createSystemIndex( String indexOid, URI path, boolean withReverse ) throws LdapException
    {
        LOG.debug( "Supplied index {} is not a MavibotIndex.  "
            + "Will create new MavibotIndex using copied configuration parameters." );
        MavibotIndex<?> mavibotIndex;

        if ( indexOid.equals( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
        {
            mavibotIndex = new MavibotRdnIndex();
            mavibotIndex.setAttributeId( ApacheSchemaConstants.APACHE_RDN_AT_OID );
        }
        else if ( indexOid.equals( ApacheSchemaConstants.APACHE_ALIAS_AT_OID ) )
        {
            mavibotIndex = new MavibotDnIndex( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
            mavibotIndex.setAttributeId( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
        }
        else
        {
            mavibotIndex = new MavibotIndex( indexOid, withReverse );
        }

        mavibotIndex.setWkDirPath( path );

        return mavibotIndex;
    }


    /**jdbm
     * removes any unused/removed attribute index files present under the partition's
     * working directory
     */
    private void deleteUnusedIndexFiles( List<String> allIndices, File[] dbFiles )
    {

    }


    /**
     * Builds user defined indexes on a attributes by browsing all the entries present in master db
     * 
     * @param userIndexes then user defined indexes to create
     * @throws Exception in case of any problems while building the index
     */
    private void buildUserIndex( PartitionTxn partitionTxn, List<Index<?, String>> userIndexes ) throws Exception
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
                    for ( Value value : entryAttr )
                    {
                        index.add( partitionTxn, value.getString(), id );
                    }

                    // Adds only those attributes that are indexed
                    presenceIdx.add( partitionTxn, attributeOid, id );
                }
            }
        }

        cursor.close();
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


    public RecordManager getRecordMan()
    {
        return recordMan;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry lookupCache( String id )
    {
        return ( entryCache != null ) ? entryCache.get( id ) : null;
    }


    @Override
    public void addToCache( String id, Entry entry )
    {
        if ( entryCache == null )
        {
            return;
        }

        if ( entry instanceof ClonedServerEntry )
        {
            entry = ( ( ClonedServerEntry ) entry ).getOriginalEntry();
        }

        entryCache.put( id, entry );
    }


    @Override
    public void updateCache( OperationContext opCtx )
    {
        if ( entryCache == null )
        {
            return;
        }

        try
        {
            if ( opCtx instanceof ModifyOperationContext )
            {
                // replace the entry
                ModifyOperationContext modCtx = ( ModifyOperationContext ) opCtx;
                Entry entry = modCtx.getAlteredEntry();
                String id = entry.get( SchemaConstants.ENTRY_UUID_AT ).getString();

                if ( entry instanceof ClonedServerEntry )
                {
                    entry = ( ( ClonedServerEntry ) entry ).getOriginalEntry();
                }

                entryCache.replace( id, entry );
            }
            else if ( ( opCtx instanceof MoveOperationContext ) || ( opCtx instanceof MoveAndRenameOperationContext )
                || ( opCtx instanceof RenameOperationContext ) )
            {
                // clear the cache it is not worth updating all the children
                entryCache.clear();
            }
            else if ( opCtx instanceof DeleteOperationContext )
            {
                // delete the entry
                DeleteOperationContext delCtx = ( DeleteOperationContext ) opCtx;
                entryCache.remove( delCtx.getEntry().get( SchemaConstants.ENTRY_UUID_AT ).getString() );
            }
        }
        catch ( LdapException e )
        {
            LOG.warn( "Failed to update entry cache", e );
        }
    }

    
    /**
     * @return The set of system and user indexes
     */
    public Set<Index<?, String>> getAllIndices()
    {
        Set<Index<?, String>> all = new HashSet<>( systemIndices.values() );
        all.addAll( userIndices.values() );
        
        return all;
    }


    @Override
    public PartitionReadTxn beginReadTransaction()
    {
        return new PartitionReadTxn();
    }


    @Override
    public PartitionWriteTxn beginWriteTransaction()
    {
        return new PartitionWriteTxn();
    }
}