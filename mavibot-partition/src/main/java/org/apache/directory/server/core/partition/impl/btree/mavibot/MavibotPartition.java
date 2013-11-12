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
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.exception.MultiException;
import org.apache.directory.mavibot.btree.managed.RecordManager;
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
 * 
 * TODO MavibotPartition.
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
    private Cache entryCache;


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


    @Override
    protected void doInit() throws Exception
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
                setOptimizer( new DefaultOptimizer<Entry>( this ) );
            }

            EvaluatorBuilder evaluatorBuilder = new EvaluatorBuilder( this, schemaManager );
            CursorBuilder cursorBuilder = new CursorBuilder( this, evaluatorBuilder );

            setSearchEngine( new DefaultSearchEngine( this, cursorBuilder, evaluatorBuilder, getOptimizer() ) );

            // Create the underlying directories (only if needed)
            File partitionDir = new File( getPartitionPath() );

            if ( !partitionDir.exists() && !partitionDir.mkdirs() )
            {
                throw new IOException( I18n.err( I18n.ERR_112_COULD_NOT_CREATE_DIRECORY, partitionDir ) );
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

            // Create the master table (the table containing all the entries)
            master = new MavibotMasterTable( recordMan, schemaManager, "master", cacheSize );

            // get all index db files first
            File[] allIndexDbFiles = partitionDir.listFiles( DB_FILTER );

            // get the names of the db files also
            List<String> indexDbFileNameList = Arrays.asList( partitionDir.list( DB_FILTER ) );

            // then add all index objects to a list
            List<String> allIndices = new ArrayList<String>();

            for ( Index<?, String> index : systemIndices.values() )
            {
                allIndices.add( index.getAttribute().getOid() );
            }

            List<Index<?, String>> indexToBuild = new ArrayList<Index<?, String>>();

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
                entryCache = cacheService.getCache( getId() );
                aliasCache = cacheService.getCache( "alias" );
            }

            // We are done !
            initialized = true;
        }
    }


    @Override
    protected Index<?, String> convertAndInit( Index<?, String> index ) throws Exception
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

        mavibotIndex.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() ) );

        return mavibotIndex;
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
                entryCache.removeAll();
            }
        }

        if ( errors.size() > 0 )
        {
            throw errors;
        }
    }


    @Override
    protected Index createSystemIndex( String indexOid, URI path, boolean withReverse ) throws Exception
    {
        LOG.debug( "Supplied index {} is not a MavibotIndex.  " +
            "Will create new MavibotIndex using copied configuration parameters." );
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


    @Override
    public void sync() throws Exception
    {
        if ( !initialized )
        {
            return;
        }

        // Sync all system indices
        for ( Index<?, String> idx : systemIndices.values() )
        {
            idx.sync();
        }

        // Sync all user defined userIndices
        for ( Index<?, String> idx : userIndices.values() )
        {
            idx.sync();
        }

        ( ( MavibotMasterTable ) master ).sync();
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
    private void buildUserIndex( List<Index<?, String>> userIndexes ) throws Exception
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


    public Entry lookupCache( String id )
    {
        if ( entryCache == null )
        {
            return null;
        }

        Element el = entryCache.get( id );

        if ( el != null )
        {
            return ( Entry ) el.getValue();
        }

        return null;
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

        entryCache.put( new Element( id, entry ) );
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

                entryCache.replace( new Element( id, entry ) );
            }
            else if ( ( opCtx instanceof MoveOperationContext ) ||
                ( opCtx instanceof MoveAndRenameOperationContext ) ||
                ( opCtx instanceof RenameOperationContext ) )
            {
                // clear the cache it is not worth updating all the children
                entryCache.removeAll();
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
}