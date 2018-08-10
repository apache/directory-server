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
import jdbm.recman.TransactionManager;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.exception.MultiException;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.DnFactory;
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
import org.apache.directory.server.core.api.partition.PartitionReadTxn;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.api.partition.PartitionWriteTxn;
import org.apache.directory.server.core.partition.impl.btree.AbstractBTreePartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
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
        @Override
        public boolean accept( File dir, String name )
        {
            // really important to filter master.db and master.lg files
            return name.endsWith( JDBM_DB_FILE_EXTN ) && !name.startsWith( "master." );
        }
    };

    /** the JDBM record manager used by this database */
    private RecordManager recMan;

    /** the entry cache */
    private Cache entryCache;


    /**
     * Creates a store based on JDBM B+Trees.
     * 
     * @param schemaManager The SchemaManager instance
     * @param dnFactory The DN factory instance
     */
    public JdbmPartition( SchemaManager schemaManager, DnFactory dnFactory )
    {
        super( schemaManager, dnFactory );

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
     * Rebuild the indexes 
     */
    private int rebuildIndexes( PartitionTxn partitionTxn ) throws LdapException, IOException
    {
        Cursor<Tuple<String, Entry>> cursor = getMasterTable().cursor();

        int masterTableCount = 0;
        int repaired = 0;

        System.out.println( "Re-building indices..." );

        boolean ctxEntryLoaded = false;

        try
        {
            while ( cursor.next() )
            {
                masterTableCount++;
                Tuple<String, Entry> tuple = cursor.get();
                String id = tuple.getKey();

                Entry entry = tuple.getValue();
                
                // Start with the RdnIndex
                String parentId = entry.get( ApacheSchemaConstants.ENTRY_PARENT_ID_OID ).getString();
                System.out.println( "Read entry " + entry.getDn() + " with ID " + id + " and parent ID " + parentId );

                Dn dn = entry.getDn();
                
                ParentIdAndRdn parentIdAndRdn = null;

                // context entry may have more than one RDN
                if ( !ctxEntryLoaded && getSuffixDn().getName().startsWith( dn.getName() ) )
                {
                    // If the read entry is the context entry, inject a tuple that have one or more RDNs
                    parentIdAndRdn = new ParentIdAndRdn( parentId, getSuffixDn().getRdns() );
                    ctxEntryLoaded = true;
                }
                else
                {
                    parentIdAndRdn = new ParentIdAndRdn( parentId, dn.getRdn() );
                }

                // Inject the parentIdAndRdn in the rdnIndex
                rdnIdx.add( partitionTxn, parentIdAndRdn, id );
                
                // Process the ObjectClass index
                // Update the ObjectClass index
                Attribute objectClass = entry.get( objectClassAT );

                if ( objectClass == null )
                {
                    String msg = I18n.err( I18n.ERR_217, dn, entry );
                    ResultCodeEnum rc = ResultCodeEnum.OBJECT_CLASS_VIOLATION;
                    throw new LdapSchemaViolationException( rc, msg );
                }

                for ( Value value : objectClass )
                {
                    String valueStr = value.getValue();

                    if ( valueStr.equals( SchemaConstants.TOP_OC ) )
                    {
                        continue;
                    }

                    objectClassIdx.add( partitionTxn, valueStr, id );
                }
                
                // The Alias indexes
                if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
                {
                    Attribute aliasAttr = entry.get( aliasedObjectNameAT );
                    addAliasIndices( partitionTxn, id, dn, new Dn( schemaManager, aliasAttr.getString() ) );
                }
                
                // The entryCSN index
                // Update the EntryCsn index
                Attribute entryCsn = entry.get( entryCsnAT );

                if ( entryCsn == null )
                {
                    String msg = I18n.err( I18n.ERR_219, dn, entry );
                    throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
                }

                entryCsnIdx.add( partitionTxn, entryCsn.getString(), id );

                // The AdministrativeRole index
                // Update the AdministrativeRole index, if needed
                if ( entry.containsAttribute( administrativeRoleAT ) )
                {
                    // We may have more than one role
                    Attribute adminRoles = entry.get( administrativeRoleAT );

                    for ( Value value : adminRoles )
                    {
                        adminRoleIdx.add( partitionTxn, value.getValue(), id );
                    }

                    // Adds only those attributes that are indexed
                    presenceIdx.add( partitionTxn, administrativeRoleAT.getOid(), id );
                }

                // And the user indexess
                // Now work on the user defined userIndices
                for ( Attribute attribute : entry )
                {
                    AttributeType attributeType = attribute.getAttributeType();
                    String attributeOid = attributeType.getOid();

                    if ( hasUserIndexOn( attributeType ) )
                    {
                        Index<Object, String> idx = ( Index<Object, String> ) getUserIndex( attributeType );

                        // here lookup by attributeId is OK since we got attributeId from
                        // the entry via the enumeration - it's in there as is for sure

                        for ( Value value : attribute )
                        {
                            idx.add( partitionTxn, value.getValue(), id );
                        }

                        // Adds only those attributes that are indexed
                        presenceIdx.add( partitionTxn, attributeOid, id );
                    }
                }
            }
            
        }
        catch ( Exception e )
        {
            System.out.println( "Exiting after fetching entries " + repaired );
            throw new LdapOtherException( e.getMessage(), e );
        }
        finally
        {
            cursor.close();
        }
        
        return masterTableCount;
    }
    
    
    /**
     * Update the children and descendant counters in the RDN index
     */
    private void updateRdnIndexCounters( PartitionTxn partitionTxn ) throws LdapException, IOException
    {
        Cursor<Tuple<String, Entry>> cursor = getMasterTable().cursor();

        System.out.println( "Updating the RDN index counters..." );

        try
        {
            while ( cursor.next() )
            {
                Tuple<String, Entry> tuple = cursor.get();

                Entry entry = tuple.getValue();

                // Update the parent's nbChildren and nbDescendants values
                // Start with the RdnIndex
                String parentId = entry.get( ApacheSchemaConstants.ENTRY_PARENT_ID_OID ).getString();
                
                if ( parentId != Partition.ROOT_ID )
                {
                    updateRdnIdx( partitionTxn, parentId, ADD_CHILD, 0 );
                }
            }
        }
        catch ( Exception e )
        {
            System.out.println( "Exiting, wasn't able to update the RDN index counters" );
            throw new LdapOtherException( e.getMessage(), e );
        }
        finally
        {
            cursor.close();
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRepair() throws LdapException
    {
        BaseRecordManager base;

        try
        {
            base = new BaseRecordManager( getPartitionPath().getPath() );
            TransactionManager transactionManager = base.getTransactionManager();
            transactionManager.setMaximumTransactionsInLog( 2000 );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        // Find the underlying directories
        File partitionDir = new File( getPartitionPath() );
        
        // get the names of the db files
        List<String> indexDbFileNameList = Arrays.asList( partitionDir.list( DB_FILTER ) );

        // then add all index objects to a list
        List<String> allIndices = new ArrayList<>();

        try
        {
            // Iterate on the declared indexes, deleting the old ones
            for ( Index<?, String> index : getIndexedAttributes() )
            {
                // Index won't be initialized at this time, so lookup AT registry to get the OID
                AttributeType indexAT = schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() );
                String oid = indexAT.getOid();
                allIndices.add( oid );
                
                // take the part after removing .db from the
                String name = oid + JDBM_DB_FILE_EXTN;
                
                // if the name doesn't exist in the list of index DB files
                // this is a new index and we need to build it
                if ( indexDbFileNameList.contains( name ) )
                {
                    ( ( JdbmIndex<?> ) index ).close( null );
                    
                    File indexFile = new File( partitionDir, name );
                    indexFile.delete();
                    
                    // Recreate the index
                    ( ( JdbmIndex<?> ) index ).init( base, schemaManager, indexAT );
                }
            }
            // Ok, now, rebuild the indexes.
            int masterTableCount = rebuildIndexes( null );
            
            // Now that the RdnIndex has been rebuilt, we have to update the nbChildren and nbDescendants values
            // We loop again on the MasterTable 
            updateRdnIndexCounters( null );

            // Flush the indexes on disk
            sync();

            System.out.println( "Total entries present in the partition " + masterTableCount );
            System.out.println( "Repair complete" );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }
    }


    @Override
    protected void doInit() throws LdapException
    {
        if ( !initialized )
        {
            BaseRecordManager base;

            // setup optimizer and registries for parent
            if ( !optimizerEnabled )
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

            // First, check if the file storing the data exists
            String path = partitionDir.getPath() + File.separator + id;

            try
            {
                base = new BaseRecordManager( path );
                TransactionManager transactionManager = base.getTransactionManager();
                transactionManager.setMaximumTransactionsInLog( 2000 );
                
                // prevent the OOM when more than 50k users are loaded at a stretch
                // adding this system property to make it configurable till JDBM gets replaced by Mavibot
                String cacheSizeVal = System.getProperty( "jdbm.recman.cache.size", "100" );
                
                int recCacheSize = Integer.parseInt( cacheSizeVal );
                
                LOG.info( "Setting CacheRecondManager's cache size to {}", recCacheSize );
                
                recMan = new CacheRecordManager( base, new MRU( recCacheSize ) );
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }

            // Iterate on the declared indexes
            List<String> allIndices = new ArrayList<>();
            List<Index<?, String>> indexToBuild = new ArrayList<>();

            for ( Index<?, String> index : getIndexedAttributes() )
            {
                String oid = schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() ).getOid();
                allIndices.add( oid );
                
                // if the name doesn't exist in the database
                // this is a new index and we need to build it
                try
                {
                    // Check the forward index only (we suppose we never will add a reverse index later on)
                    String forwardIndex = oid + "_forward";
                    
                    if ( recMan.getNamedObject( forwardIndex ) == 0 )
                    {
                        // The index does not exist in the database, we need to build it
                        indexToBuild.add( index );
                    }
                }
                catch ( IOException ioe )
                {
                    throw new LdapOtherException( ioe.getMessage(), ioe );
                }
            }

            /*
            // get all index db files first
            File[] allIndexDbFiles = partitionDir.listFiles( DB_FILTER );

            // get the names of the db files also
            List<String> indexDbFileNameList = Arrays.asList( partitionDir.list( DB_FILTER ) );

            // then add all index objects to a list
            List<String> allIndices = new ArrayList<>();

            List<Index<?, String>> indexToBuild = new ArrayList<>();
            
            // Iterate on the declared indexes
            for ( Index<?, String> index : getIndexedAttributes() )
            {
                // Index won't be initialized at this time, so lookup AT registry to get the OID
                String oid = schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() ).getOid();
                allIndices.add( oid );
                
                // take the part after removing .db from the
                String name = oid + JDBM_DB_FILE_EXTN;
                
                // if the name doesn't exist in the list of index DB files
                // this is a new index and we need to build it
                if ( !indexDbFileNameList.contains( name ) )
                {
                    indexToBuild.add( index );
                }
            }
            */

            // Initialize the indexes
            super.doInit();

            if ( cacheSize < 0 )
            {
                cacheSize = DEFAULT_CACHE_SIZE;
                LOG.debug( "Using the default entry cache size of {} for {} partition", cacheSize, id );
            }
            else
            {
                LOG.debug( "Using the custom configured cache size of {} for {} partition", cacheSize, id );
            }

            // Create the master table (the table containing all the entries)
            try
            {
                master = new JdbmMasterTable( recMan, schemaManager );
            }
            catch ( IOException ioe )
            {
                throw new LdapOtherException( ioe.getMessage(), ioe );
            }

            if ( !indexToBuild.isEmpty() )
            {
                buildUserIndex( beginReadTransaction(), indexToBuild );
            }

            //deleteUnusedIndexFiles( allIndices, allIndexDbFiles );

            if ( cacheService != null )
            {
                entryCache = cacheService.getCache( getId() );

                int cacheSizeConfig = ( int ) entryCache.getCacheConfiguration().getMaxEntriesLocalHeap();

                if ( cacheSizeConfig < cacheSize )
                {
                    entryCache.getCacheConfiguration().setMaxEntriesLocalHeap( cacheSize );
                }
            }

            // Initialization of the context entry
            if ( ( suffixDn != null ) && ( contextEntry != null ) )
            {
                Dn contextEntryDn = contextEntry.getDn();

                // Checking if the context entry DN is schema aware
                if ( !contextEntryDn.isSchemaAware() )
                {
                    contextEntryDn = new Dn( schemaManager, contextEntryDn );
                }

                // We're only adding the entry if the two DNs are equal
                if ( suffixDn.equals( contextEntryDn ) )
                {
                    // Looking for the current context entry
                    Entry suffixEntry;
                    LookupOperationContext lookupContext = new LookupOperationContext( null, suffixDn );
                    lookupContext.setPartition( this );
                    
                    try ( PartitionTxn partitionTxn = beginReadTransaction() )
                    {
                        lookupContext.setTransaction( partitionTxn );
                        suffixEntry = lookup( lookupContext );
                    }
                    catch ( IOException ioe )
                    {
                        throw new LdapOtherException( ioe.getMessage(), ioe );
                    }

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
                        PartitionTxn partitionTxn = null;
                        AddOperationContext addContext = new AddOperationContext( null, contextEntry );
                        
                        try
                        {
                            partitionTxn = beginWriteTransaction();
                            addContext.setTransaction( partitionTxn );

                            add( addContext );
                            partitionTxn.commit();
                        }
                        catch ( LdapException le )
                        {
                            if ( partitionTxn != null )
                            {
                                try
                                { 
                                    partitionTxn.abort();
                                }
                                catch ( IOException ioe )
                                {
                                    throw new LdapOtherException( ioe.getMessage(), ioe );
                                }
                            }
                            
                            throw le;
                        }
                        catch ( IOException ioe )
                        {
                            try
                            { 
                                partitionTxn.abort();
                            }
                            catch ( IOException ioe2 )
                            {
                                throw new LdapOtherException( ioe2.getMessage(), ioe2 );
                            }

                            throw new LdapOtherException( ioe.getMessage(), ioe );
                        }
                    }
                }
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
     * @throws LdapException on failures to sync database files to disk
     */
    @Override
    public synchronized void sync() throws LdapException
    {
        if ( !initialized )
        {
            return;
        }
        
        try
        {
            // Commit
            recMan.commit();
    
            // And flush the journal
            BaseRecordManager baseRecordManager = null;
    
            if ( recMan instanceof CacheRecordManager )
            {
                baseRecordManager = ( ( BaseRecordManager ) ( ( CacheRecordManager ) recMan ).getRecordManager() );
            }
            else
            {
                baseRecordManager = ( ( BaseRecordManager ) recMan );
            }
    
            baseRecordManager.getTransactionManager().synchronizeLog();
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }
    }


    /**
     * Builds user defined indexes on a attributes by browsing all the entries present in master db
     * 
     * Note: if the given list of indices contains any system index that will be skipped.
     * 
     * WARN: MUST be called after calling super.doInit()
     * 
     * @param indices then selected indexes that need to be built
     * @throws Exception in case of any problems while building the index
     */
    private void buildUserIndex( PartitionTxn partitionTxn, List<Index<?, String>> indices ) throws LdapException
    {
        try
        {
            Cursor<Tuple<String, Entry>> cursor = master.cursor();
            cursor.beforeFirst();
    
            while ( cursor.next() )
            {
                for ( Index index : indices )
                {
                    AttributeType atType = index.getAttribute();
    
                    String attributeOid = index.getAttribute().getOid();
    
                    if ( systemIndices.get( attributeOid ) != null )
                    {
                        // skipping building of the system index
                        continue;
                    }
                    
                    LOG.info( "building the index for attribute type {}", atType );
    
                    Tuple<String, Entry> tuple = cursor.get();
    
                    String id = tuple.getKey();
                    Entry entry = tuple.getValue();
    
                    Attribute entryAttr = entry.get( atType );
    
                    if ( entryAttr != null )
                    {
                        for ( Value value : entryAttr )
                        {
                            index.add( partitionTxn, value.getValue(), id );
                        }
    
                        // Adds only those attributes that are indexed
                        presenceIdx.add( partitionTxn, attributeOid, id );
                    }
                }
            }
    
            cursor.close();
        }
        catch ( CursorException | IOException e )
        {
            throw new LdapOtherException( e.getMessage(), e );
        }
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

            if ( systemIndices.get( name ) != null )
            {
                // do not delete the system index file
                continue;
            }

            // remove the file if not found in the list of names of indices
            if ( !allIndices.contains( name ) )
            {
                boolean deleted = file.delete();

                if ( deleted )
                {
                    LOG.info( "Deleted unused index file {}", file.getAbsolutePath() );
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
    @Override
    protected Index<?, String> convertAndInit( Index<?, String> index ) throws LdapException
    {
        JdbmIndex<?> jdbmIndex;

        if ( index instanceof JdbmRdnIndex )
        {
            jdbmIndex = ( JdbmRdnIndex ) index;
        }
        else if ( index instanceof JdbmDnIndex )
        {
            jdbmIndex = ( JdbmDnIndex ) index;
        }
        else if ( index instanceof JdbmIndex<?> )
        {
            jdbmIndex = ( JdbmIndex<?> ) index;
        }
        else
        {
            LOG.debug( "Supplied index {} is not a JdbmIndex.  "
                + "Will create new JdbmIndex using copied configuration parameters.", index );
            jdbmIndex = new JdbmIndex( index.getAttributeId(), true );
            jdbmIndex.setCacheSize( index.getCacheSize() );
            jdbmIndex.setNumDupLimit( JdbmIndex.DEFAULT_DUPLICATE_LIMIT );
        }

        try
        {
            jdbmIndex.init( recMan, schemaManager, schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() ) );
        }
        catch ( IOException ioe )
        {
            throw new LdapOtherException( ioe.getMessage(), ioe );
        }

        return jdbmIndex;
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

        // This is specific to the JDBM store : close the record manager
        try
        {
            recMan.close();
            LOG.debug( "Closed record manager for {} partition.", suffixDn );
        }
        catch ( IOException t )
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
            throw new LdapOtherException( errors.getMessage(), errors );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected final Index createSystemIndex( String oid, URI path, boolean withReverse ) throws LdapException
    {
        LOG.debug( "Supplied index {} is not a JdbmIndex.  "
            + "Will create new JdbmIndex using copied configuration parameters." );
        JdbmIndex<?> jdbmIndex;

        if ( oid.equals( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
        {
            jdbmIndex = new JdbmRdnIndex();
            jdbmIndex.setAttributeId( ApacheSchemaConstants.APACHE_RDN_AT_OID );
            jdbmIndex.setNumDupLimit( JdbmIndex.DEFAULT_DUPLICATE_LIMIT );
        }
        else if ( oid.equals( ApacheSchemaConstants.APACHE_ALIAS_AT_OID ) )
        {
            jdbmIndex = new JdbmDnIndex( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
            jdbmIndex.setAttributeId( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
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
            else if ( ( opCtx instanceof MoveOperationContext )
                || ( opCtx instanceof MoveAndRenameOperationContext )
                || ( opCtx instanceof RenameOperationContext ) )
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


    @Override
    public Entry lookupCache( String id )
    {
        if ( entryCache == null )
        {
            return null;
        }

        Element el = entryCache.get( id );

        if ( el != null )
        {
            return ( Entry ) el.getObjectValue();
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

        Entry addedEntry = entry;
        
        if ( entry instanceof ClonedServerEntry )
        {
            addedEntry = ( ( ClonedServerEntry ) entry ).getOriginalEntry();
        }

        entryCache.put( new Element( id, addedEntry ) );
    }


    @Override
    public PartitionReadTxn beginReadTransaction()
    {
        return new PartitionReadTxn();
    }


    @Override
    public PartitionWriteTxn beginWriteTransaction()
    {
        return new JdbmPartitionWriteTxn( recMan, isSyncOnWrite() );
    }
}
