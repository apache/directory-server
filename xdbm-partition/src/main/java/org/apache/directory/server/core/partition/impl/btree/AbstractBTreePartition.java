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
package org.apache.directory.server.core.partition.impl.btree;


import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.ehcache.Cache;
import org.ehcache.config.CacheConfiguration;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapAliasDereferencingException;
import org.apache.directory.api.ldap.model.exception.LdapAliasException;
import org.apache.directory.api.ldap.model.exception.LdapContextNotEmptyException;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Ava;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.Normalizer;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.MultiException;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModDnAva;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.api.partition.AbstractPartition;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.api.partition.PartitionWriteTxn;
import org.apache.directory.server.core.api.partition.Subordinates;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.MasterTable;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Optimizer;
import org.apache.directory.server.xdbm.search.PartitionSearchResult;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An abstract {@link Partition} that uses general BTree operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractBTreePartition extends AbstractPartition implements Store
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractBTreePartition.class );

    /** the search engine used to search the database */
    private SearchEngine searchEngine;

    /** The optimizer to use during search operation */
    private Optimizer optimizer;

    /** Tells if the Optimizer is enabled */
    protected boolean optimizerEnabled = true;

    /** The default cache size is set to 10 000 objects */
    public static final int DEFAULT_CACHE_SIZE = 10000;

    /** The Entry cache size for this partition */
    protected int cacheSize = DEFAULT_CACHE_SIZE;

    /** The alias cache */
    protected Cache< String, Dn > aliasCache;

    /** The ParentIdAndRdn cache */
    protected Cache< String, ParentIdAndRdn > piarCache;

    /** true if we sync disks on every write operation */
    protected AtomicBoolean isSyncOnWrite = new AtomicBoolean( true );

    /** The suffix UUID */
    private volatile String suffixId;

    /** The path in which this Partition stores files */
    protected URI partitionPath;

    /** The set of indexed attributes */
    private Set<Index<?, String>> indexedAttributes;

    /** the master table storing entries by primary key */
    protected MasterTable master;

    /** a map of attributeType numeric UUID to user userIndices */
    protected Map<String, Index<?, String>> userIndices = new HashMap<>();

    /** a map of attributeType numeric UUID to system userIndices */
    protected Map<String, Index<?, String>> systemIndices = new HashMap<>();

    /** the relative distinguished name index */
    protected Index<ParentIdAndRdn, String> rdnIdx;

    /** a system index on objectClass attribute*/
    protected Index<String, String> objectClassIdx;

    /** the attribute presence index */
    protected Index<String, String> presenceIdx;

    /** a system index on entryCSN attribute */
    protected Index<String, String> entryCsnIdx;

    /** a system index on aliasedObjectName attribute */
    protected Index<Dn, String> aliasIdx;

    /** the subtree scope alias index */
    protected Index<String, String> subAliasIdx;

    /** the one level scope alias index */
    protected Index<String, String> oneAliasIdx;

    /** a system index on administrativeRole attribute */
    protected Index<String, String> adminRoleIdx;

    /** Cached attributes types to avoid lookup all over the code */
    protected AttributeType objectClassAT;
    private Normalizer objectClassNormalizer;
    protected AttributeType presenceAT;
    private Normalizer presenceNormalizer;
    protected AttributeType entryCsnAT;
    protected AttributeType entryDnAT;
    protected AttributeType entryUuidAT;
    protected AttributeType aliasedObjectNameAT;
    protected AttributeType administrativeRoleAT;
    protected AttributeType contextCsnAT;
    
    /** Cached value for TOP */
    private Value topOCValue;

    private static final boolean NO_REVERSE = Boolean.FALSE;
    private static final boolean WITH_REVERSE = Boolean.TRUE;

    protected static final boolean ADD_CHILD = true;
    protected static final boolean REMOVE_CHILD = false;

    /** A lock to protect the backend from concurrent reads/writes */
    private ReadWriteLock rwLock;

    /** a cache to hold <entryUUID, Dn> pairs, this is used for speeding up the buildEntryDn() method */
    private Cache<String, Dn> entryDnCache;
    
    /** a semaphore to serialize the writes on context entry while updating contextCSN attribute */
    private Semaphore ctxCsnSemaphore = new Semaphore( 1 );
    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a B-tree based context partition.
     * 
     * @param schemaManager the schema manager
     */
    protected AbstractBTreePartition( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;

        initInstance();
    }


    /**
     * Creates a B-tree based context partition.
     * 
     * @param schemaManager the schema manager
     * @param dnFactory the DN factory
     */
    protected AbstractBTreePartition( SchemaManager schemaManager, DnFactory dnFactory )
    {
        this.schemaManager = schemaManager;
        this.dnFactory = dnFactory;

        initInstance();
    }


    /**
     * Intializes the instance.
     */
    private void initInstance()
    {
        indexedAttributes = new HashSet<>();

        // Initialize Attribute types used all over this method
        objectClassAT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        objectClassNormalizer = objectClassAT.getEquality().getNormalizer();
        presenceAT = schemaManager.getAttributeType( ApacheSchemaConstants.APACHE_PRESENCE_AT );
        presenceNormalizer = presenceAT.getEquality().getNormalizer();
        aliasedObjectNameAT = schemaManager.getAttributeType( SchemaConstants.ALIASED_OBJECT_NAME_AT );
        entryCsnAT = schemaManager.getAttributeType( SchemaConstants.ENTRY_CSN_AT );
        entryDnAT = schemaManager.getAttributeType( SchemaConstants.ENTRY_DN_AT );
        entryUuidAT = schemaManager.getAttributeType( SchemaConstants.ENTRY_UUID_AT );
        administrativeRoleAT = schemaManager.getAttributeType( SchemaConstants.ADMINISTRATIVE_ROLE_AT );
        contextCsnAT = schemaManager.getAttributeType( SchemaConstants.CONTEXT_CSN_AT );
        
        // Initialize a Value for TOP_OC
        try
        {
            topOCValue = new Value( schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT_OID ), SchemaConstants.TOP_OC_OID );
        }
        catch ( LdapInvalidAttributeValueException e )
        {
            // There is nothing we can do...
        }
        
        // Relax the entryDnAT so that we don't check the EntryDN twice
        entryDnAT.setRelaxed( true );
    }


    // ------------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------
    /**
     * Gets the entry cache size for this BTreePartition.
     *
     * @return the maximum size of the cache as the number of entries maximum before paging out
     */
    @Override
    public int getCacheSize()
    {
        return cacheSize;
    }


    /**
     * Used to specify the entry cache size for a Partition.  Various Partition
     * implementations may interpret this value in different ways: i.e. total cache
     * size limit verses the number of entries to cache.
     *
     * @param cacheSize the maximum size of the cache in the number of entries
     */
    @Override
    public void setCacheSize( int cacheSize )
    {
        this.cacheSize = cacheSize;
    }


    /**
     * Tells if the Optimizer is enabled or not
     * @return true if the optimizer is enabled
     */
    public boolean isOptimizerEnabled()
    {
        return optimizerEnabled;
    }


    /**
     * Set the optimizer flag
     * @param optimizerEnabled The flag
     */
    public void setOptimizerEnabled( boolean optimizerEnabled )
    {
        this.optimizerEnabled = optimizerEnabled;
    }


    /**
     * Sets the path in which this Partition stores data. This may be an URL to
     * a file or directory, or an JDBC URL.
     *
     * @param partitionPath the path in which this Partition stores data.
     */
    @Override
    public void setPartitionPath( URI partitionPath )
    {
        checkInitialized( "partitionPath" );
        this.partitionPath = partitionPath;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSyncOnWrite()
    {
        return isSyncOnWrite.get();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSyncOnWrite( boolean isSyncOnWrite )
    {
        checkInitialized( "syncOnWrite" );
        this.isSyncOnWrite.set( isSyncOnWrite );
    }


    /**
     * Sets up the system indices.
     * 
     * @throws LdapException If the setup failed
     */
    @SuppressWarnings("unchecked")
    protected void setupSystemIndices() throws LdapException
    {
        // add missing system indices
        if ( getPresenceIndex() == null )
        {
            Index<String, String> index = createSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID,
                partitionPath, NO_REVERSE );
            addIndex( index );
        }

        if ( getRdnIndex() == null )
        {
            Index<ParentIdAndRdn, String> index = createSystemIndex(
                ApacheSchemaConstants.APACHE_RDN_AT_OID,
                partitionPath, WITH_REVERSE );
            addIndex( index );
        }

        if ( getAliasIndex() == null )
        {
            Index<Dn, String> index = createSystemIndex( ApacheSchemaConstants.APACHE_ALIAS_AT_OID,
                partitionPath, WITH_REVERSE );
            addIndex( index );
        }

        if ( getOneAliasIndex() == null )
        {
            Index<String, String> index = createSystemIndex( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID,
                partitionPath, NO_REVERSE );
            addIndex( index );
        }

        if ( getSubAliasIndex() == null )
        {
            Index<String, String> index = createSystemIndex( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID,
                partitionPath, NO_REVERSE );
            addIndex( index );
        }

        if ( getObjectClassIndex() == null )
        {
            Index<String, String> index = createSystemIndex( SchemaConstants.OBJECT_CLASS_AT_OID, partitionPath,
                NO_REVERSE );
            addIndex( index );
        }

        if ( getEntryCsnIndex() == null )
        {
            Index<String, String> index = createSystemIndex( SchemaConstants.ENTRY_CSN_AT_OID, partitionPath,
                NO_REVERSE );
            addIndex( index );
        }

        if ( getAdministrativeRoleIndex() == null )
        {
            Index<String, String> index = createSystemIndex( SchemaConstants.ADMINISTRATIVE_ROLE_AT_OID,
                partitionPath,
                NO_REVERSE );
            addIndex( index );
        }

        // convert and initialize system indices
        for ( Map.Entry<String, Index<?, String>> elem : systemIndices.entrySet() )
        {
            Index<?, String> index = elem.getValue();
            index = convertAndInit( index );
            systemIndices.put( elem.getKey(), index );
        }

        // set index shortcuts
        rdnIdx = ( Index<ParentIdAndRdn, String> ) systemIndices
            .get( ApacheSchemaConstants.APACHE_RDN_AT_OID );
        presenceIdx = ( Index<String, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
        aliasIdx = ( Index<Dn, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
        oneAliasIdx = ( Index<String, String> ) systemIndices
            .get( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
        subAliasIdx = ( Index<String, String> ) systemIndices
            .get( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
        objectClassIdx = ( Index<String, String> ) systemIndices.get( SchemaConstants.OBJECT_CLASS_AT_OID );
        entryCsnIdx = ( Index<String, String> ) systemIndices.get( SchemaConstants.ENTRY_CSN_AT_OID );
        adminRoleIdx = ( Index<String, String> ) systemIndices.get( SchemaConstants.ADMINISTRATIVE_ROLE_AT_OID );
    }


    /**
     * Sets up the user indices.
     * 
     * @throws LdapException If the setup failed
     */
    protected void setupUserIndices() throws LdapException
    {
        // convert and initialize system indices
        Map<String, Index<?, String>> tmp = new HashMap<>();

        for ( Map.Entry<String, Index<?, String>> elem : userIndices.entrySet() )
        {
            String oid = elem.getKey();
            
            // check that the attributeType has an EQUALITY matchingRule
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( oid );
            MatchingRule mr = attributeType.getEquality();

            if ( mr != null )
            {
                Index<?, String> index = elem.getValue();
                index = convertAndInit( index );
                tmp.put( oid, index );
            }
            else
            {
                LOG.error( I18n.err( I18n.ERR_4, attributeType.getName() ) );
            }
        }

        userIndices = tmp;
    }


    /**
     * Gets the DefaultSearchEngine used by this ContextPartition to search the
     * Database.
     *
     * @return the search engine
     */
    public SearchEngine getSearchEngine()
    {
        return searchEngine;
    }


    // -----------------------------------------------------------------------
    // Miscellaneous abstract methods
    // -----------------------------------------------------------------------
    /**
     * Convert and initialize an index for a specific store implementation.
     *
     * @param index the index
     * @return the converted and initialized index
     * @throws LdapException If teh conversion failed
     */
    protected abstract Index<?, String> convertAndInit( Index<?, String> index ) throws LdapException;


    /**
     * Gets the path in which this Partition stores data.
     *
     * @return the path in which this Partition stores data.
     */
    @Override
    public URI getPartitionPath()
    {
        return partitionPath;
    }


    // ------------------------------------------------------------------------
    // Partition Interface Method Implementations
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDestroy( PartitionTxn partitionTxn ) throws LdapException
    {
        LOG.debug( "destroy() called on store for {}", this.suffixDn );

        if ( !initialized )
        {
            return;
        }

        // don't reset initialized flag
        initialized = false;

        entryDnCache.clear();
        
        MultiException errors = new MultiException( I18n.err( I18n.ERR_577 ) );

        for ( Index<?, String> index : userIndices.values() )
        {
            try
            {
                index.close( partitionTxn );
                LOG.debug( "Closed {} user index for {} partition.", index.getAttributeId(), suffixDn );
            }
            catch ( Throwable t )
            {
                LOG.error( I18n.err( I18n.ERR_124 ), t );
                errors.addThrowable( t );
            }
        }

        for ( Index<?, String> index : systemIndices.values() )
        {
            try
            {
                index.close( partitionTxn );
                LOG.debug( "Closed {} system index for {} partition.", index.getAttributeId(), suffixDn );
            }
            catch ( Throwable t )
            {
                LOG.error( I18n.err( I18n.ERR_124 ), t );
                errors.addThrowable( t );
            }
        }

        try
        {
            master.close( partitionTxn );
            
            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( I18n.err( I18n.ERR_125, suffixDn ) );
            }
        }
        catch ( Throwable t )
        {
            LOG.error( I18n.err( I18n.ERR_126 ), t );
            errors.addThrowable( t );
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
    public void repair() throws LdapException
    {
        // Do nothing by default
        doRepair();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInit() throws LdapException
    {
        // First, inject the indexed attributes if any
        if ( ( indexedAttributes != null ) && ( !indexedAttributes.isEmpty() ) )
        {
            for ( Index index : indexedAttributes )
            {
                addIndex( index );
            }
        }

        // Now, initialize the configured index
        setupSystemIndices();
        setupUserIndices();

        if ( cacheService != null )
        {
            aliasCache = cacheService.getCache( "alias", String.class, Dn.class );
    
            CacheConfiguration< String, Dn > aliasCacheConfig = aliasCache.getRuntimeConfiguration();

            //int cacheSizeConfig = ( int ) aliasCacheConfig.getMaxEntriesLocalHeap();
    
            //if ( cacheSizeConfig < cacheSize )
            //{
            //    aliasCacheConfig.setMaxEntriesLocalHeap( cacheSize );
            //}
            
            piarCache = cacheService.getCache( "piar", String.class, ParentIdAndRdn.class );
            
            //cacheSizeConfig = ( int ) piarCache.getRuntimeConfiguration().getMaxEntriesLocalHeap();
    
            //if ( cacheSizeConfig < cacheSize )
            //{
            //    piarCache.getRuntimeConfiguration().setMaxEntriesLocalHeap( cacheSize * 3L );
            //}
            
            entryDnCache = cacheService.getCache( "entryDn", String.class, Dn.class );
            //CacheRuntimeConfiguration<String, Dn> entryDnCacheConfig = entryDnCache.getRuntimeConfiguration();
            //entryDnCache.setMemoryStoreEvictionPolicy( new LruPolicy() );
            //entryDnCache.getRuntimeConfiguration().setMaxEntriesLocalHeap( cacheSize );
        }
    }


    private void dumpAllRdnIdx( PartitionTxn partitionTxn ) throws LdapException, CursorException, IOException
    {
        if ( LOG.isDebugEnabled() )
        {
            dumpRdnIdx( partitionTxn, Partition.ROOT_ID, "" );
            System.out.println( "-----------------------------" );
        }
    }


    private void dumpRdnIdx( PartitionTxn partitionTxn ) throws LdapException, CursorException, IOException
    {
        if ( LOG.isDebugEnabled() )
        {
            dumpRdnIdx( partitionTxn, Partition.ROOT_ID, 1, "" );
            System.out.println( "-----------------------------" );
        }
    }


    /**
     * Dump the RDN index content
     *  
     * @param partitionTxn The transaction to use
     * @param id The root ID
     * @param tabs The space prefix
     * @throws LdapException If we had an issue while dumping the Rdn index
     * @throws CursorException If the cursor failed to browse the Rdn Index
     * @throws IOException If we weren't able to read teh Rdn Index file
     */
    public void dumpRdnIdx( PartitionTxn partitionTxn, String id, String tabs ) throws LdapException, CursorException, IOException
    {
        // Start with the root
        Cursor<IndexEntry<ParentIdAndRdn, String>> cursor = rdnIdx.forwardCursor( partitionTxn );

        IndexEntry<ParentIdAndRdn, String> startingPos = new IndexEntry<>();
        startingPos.setKey( new ParentIdAndRdn( id, ( Rdn[] ) null ) );
        cursor.before( startingPos );

        while ( cursor.next() )
        {
            IndexEntry<ParentIdAndRdn, String> entry = cursor.get();
            System.out.println( tabs + entry );
        }

        cursor.close();
    }


    private void dumpRdnIdx( PartitionTxn partitionTxn, String id, int nbSibbling, String tabs ) 
        throws LdapException, CursorException, IOException
    {
        // Start with the root
        Cursor<IndexEntry<ParentIdAndRdn, String>> cursor = rdnIdx.forwardCursor( partitionTxn );

        IndexEntry<ParentIdAndRdn, String> startingPos = new IndexEntry<>();
        startingPos.setKey( new ParentIdAndRdn( id, ( Rdn[] ) null ) );
        cursor.before( startingPos );
        int countChildren = 0;

        while ( cursor.next() && ( countChildren < nbSibbling ) )
        {
            IndexEntry<ParentIdAndRdn, String> entry = cursor.get();
            System.out.println( tabs + entry );
            countChildren++;

            // And now, the children
            int nbChildren = entry.getKey().getNbChildren();

            if ( nbChildren > 0 )
            {
                dumpRdnIdx( partitionTxn, entry.getId(), nbChildren, tabs + "  " );
            }
        }

        cursor.close();
    }


    //---------------------------------------------------------------------------------------------
    // The Add operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        PartitionTxn partitionTxn = addContext.getTransaction();
        
        assert ( partitionTxn != null );
        assert ( partitionTxn instanceof PartitionWriteTxn );

        try
        {
            setRWLock( addContext );
            Entry entry = ( ( ClonedServerEntry ) addContext.getEntry() ).getClonedEntry();

            Dn entryDn = entry.getDn();

            // check if the entry already exists
            lockRead();

            try
            {
                if ( getEntryId( partitionTxn, entryDn ) != null )
                {
                    throw new LdapEntryAlreadyExistsException(
                        I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, entryDn.getName() ) );
                }
            }
            finally
            {
                unlockRead();
            }

            String parentId = null;

            //
            // Suffix entry cannot have a parent since it is the root so it is
            // capped off using the zero value which no entry can have since
            // entry sequences start at 1.
            //
            Dn parentDn = null;
            ParentIdAndRdn key;

            if ( entryDn.getNormName().equals( suffixDn.getNormName() ) )
            {
                parentId = Partition.ROOT_ID;
                key = new ParentIdAndRdn( parentId, suffixDn.getRdns() );
            }
            else
            {
                parentDn = entryDn.getParent();

                lockRead();

                try
                {
                    parentId = getEntryId( partitionTxn, parentDn );
                }
                finally
                {
                    unlockRead();
                }

                key = new ParentIdAndRdn( parentId, entryDn.getRdn() );
            }

            // don't keep going if we cannot find the parent Id
            if ( parentId == null )
            {
                throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_216_ID_FOR_PARENT_NOT_FOUND, parentDn ) );
            }

            // Get a new UUID for the added entry if it does not have any already
            Attribute entryUUID = entry.get( entryUuidAT );

            String id;

            if ( entryUUID == null )
            {
                id = master.getNextId( entry );
            }
            else
            {
                id = entryUUID.getString();
            }
            
            if ( entryDn.getNormName().equals( suffixDn.getNormName() ) )
            {
                suffixId = id;
            }

            // Update the ObjectClass index
            Attribute objectClass = entry.get( objectClassAT );

            if ( objectClass == null )
            {
                String msg = I18n.err( I18n.ERR_217, entryDn.getName(), entry );
                ResultCodeEnum rc = ResultCodeEnum.OBJECT_CLASS_VIOLATION;
                
                throw new LdapSchemaViolationException( rc, msg );
            }

            for ( Value value : objectClass )
            {
                if ( value.equals( topOCValue ) )
                {
                    continue;
                }
                
                String normalizedOc = objectClassNormalizer.normalize( value.getValue() );

                objectClassIdx.add( partitionTxn, normalizedOc, id );
            }

            if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
            {
                Attribute aliasAttr = entry.get( aliasedObjectNameAT );
                
                addAliasIndices( partitionTxn, id, entryDn, new Dn( schemaManager, aliasAttr.getString() ) );
            }

            // Update the EntryCsn index
            Attribute entryCsn = entry.get( entryCsnAT );

            if ( entryCsn == null )
            {
                String msg = I18n.err( I18n.ERR_219, entryDn.getName(), entry );
                throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
            }

            entryCsnIdx.add( partitionTxn, entryCsn.getString(), id );

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

            // Now work on the user defined userIndices
            for ( Attribute attribute : entry )
            {
                AttributeType attributeType = attribute.getAttributeType();
                String attributeOid = attributeType.getOid();

                if ( hasUserIndexOn( attributeType ) )
                {
                    Index<Object, String> userIndex = ( Index<Object, String> ) getUserIndex( attributeType );

                    // here lookup by attributeId is OK since we got attributeId from
                    // the entry via the enumeration - it's in there as is for sure

                    for ( Value value : attribute )
                    {
                        String normalized = value.getNormalized();
                        userIndex.add( partitionTxn, normalized, id );
                    }

                    // Adds only those attributes that are indexed
                    presenceIdx.add( partitionTxn, attributeOid, id );
                }
            }

            // Add the parentId in the entry
            entry.put( ApacheSchemaConstants.ENTRY_PARENT_ID_AT, parentId );

            lockWrite();

            try
            {
                // Update the RDN index
                rdnIdx.add( partitionTxn, key, id );

                // Update the parent's nbChildren and nbDescendants values
                if ( parentId != Partition.ROOT_ID )
                {
                    updateRdnIdx( partitionTxn, parentId, ADD_CHILD, 0 );
                }

                // Remove the EntryDN attribute
                entry.removeAttributes( entryDnAT );

                Attribute at = entry.get( SchemaConstants.ENTRY_CSN_AT );
                setContextCsn( at.getString() );

                // And finally add the entry into the master table
                master.put( partitionTxn, id, entry );
            }
            finally
            {
                unlockWrite();
            }
        }
        catch ( LdapException le )
        {
            throw le;
        }
        catch ( Exception e )
        {
            throw new LdapException( e );
        }
    }


    //---------------------------------------------------------------------------------------------
    // The Delete operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public Entry delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        PartitionTxn partitionTxn = deleteContext.getTransaction();
        
        assert ( partitionTxn != null );
        assert ( partitionTxn instanceof PartitionWriteTxn );

        setRWLock( deleteContext );
        Dn dn = deleteContext.getDn();
        String id = null;

        lockRead();

        try
        {
            id = getEntryId( partitionTxn, dn );
        }
        finally
        {
            unlockRead();
        }

        // don't continue if id is null
        if ( id == null )
        {
            throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_699, dn ) );
        }

        long childCount = getChildCount( partitionTxn, id );

        if ( childCount > 0 )
        {
            throw new LdapContextNotEmptyException( I18n.err( I18n.ERR_700, dn ) );
        }

        // We now defer the deletion to the implementing class
        Entry deletedEntry = delete( partitionTxn, id );

        updateCache( deleteContext );
        
        return deletedEntry;
    }


    protected void updateRdnIdx( PartitionTxn partitionTxn, String parentId, boolean addRemove, int nbDescendant ) throws LdapException
    {
        boolean isFirst = true;
        ////dumpRdnIdx();

        if ( parentId.equals( Partition.ROOT_ID ) )
        {
            return;
        }

        ParentIdAndRdn parent = rdnIdx.reverseLookup( partitionTxn, parentId );

        while ( parent != null )
        {
            rdnIdx.drop( partitionTxn, parentId );
            ////dumpRdnIdx();
            
            if ( isFirst )
            {
                if ( addRemove == ADD_CHILD )
                {
                    parent.setNbChildren( parent.getNbChildren() + 1 );
                }
                else
                {
                    parent.setNbChildren( parent.getNbChildren() - 1 );
                }

                isFirst = false;
            }

            if ( addRemove == ADD_CHILD )
            {
                parent.setNbDescendants( parent.getNbDescendants() + ( nbDescendant + 1 ) );
            }
            else
            {
                parent.setNbDescendants( parent.getNbDescendants() - ( nbDescendant + 1 ) );
            }

            // Inject the modified element into the index
            rdnIdx.add( partitionTxn, parent, parentId );

            ////dumpRdnIdx();

            parentId = parent.getParentId();
            parent = rdnIdx.reverseLookup( partitionTxn, parentId );
        }
    }


    /**
     * Delete the entry associated with a given Id
     * 
     * @param partitionTxn The transaction to use
     * @param id The id of the entry to delete
     * @return the deleted entry if found
     * @throws LdapException If the deletion failed
     */
    @Override
    public Entry delete( PartitionTxn partitionTxn, String id ) throws LdapException
    {
        try
        {
            // First get the entry
            Entry entry = null;

            lockRead();

            try
            {
                 entry = master.get( partitionTxn, id );
            }
            finally
            {
                unlockRead();
            }

            if ( entry == null )
            {
                // Not allowed
                throw new LdapNoSuchObjectException( "Cannot find an entry for UUID " + id );
            }

            Attribute objectClass = entry.get( objectClassAT );

            if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
            {
                dropAliasIndices( partitionTxn, id );
            }

            // Update the ObjectClass index
            for ( Value value : objectClass )
            {
                if ( value.equals( topOCValue ) )
                {
                    continue;
                }
                
                String normalizedOc = objectClassNormalizer.normalize( value.getValue() );

                objectClassIdx.drop( partitionTxn, normalizedOc, id );
            }

            // Update the parent's nbChildren and nbDescendants values
            ParentIdAndRdn parent = rdnIdx.reverseLookup( partitionTxn, id );
            updateRdnIdx( partitionTxn, parent.getParentId(), REMOVE_CHILD, 0 );

            // Update the rdn, oneLevel, subLevel, and entryCsn indexes
            entryCsnIdx.drop( partitionTxn, entry.get( entryCsnAT ).getString(), id );

            // Update the AdministrativeRole index, if needed
            if ( entry.containsAttribute( administrativeRoleAT ) )
            {
                // We may have more than one role
                Attribute adminRoles = entry.get( administrativeRoleAT );

                for ( Value value : adminRoles )
                {
                    adminRoleIdx.drop( partitionTxn, value.getValue(), id );
                }

                // Deletes only those attributes that are indexed
                presenceIdx.drop( partitionTxn, administrativeRoleAT.getOid(), id );
            }

            // Update the user indexes
            for ( Attribute attribute : entry )
            {
                AttributeType attributeType = attribute.getAttributeType();
                String attributeOid = attributeType.getOid();

                if ( hasUserIndexOn( attributeType ) )
                {
                    Index<?, String> userIndex = getUserIndex( attributeType );

                    // here lookup by attributeId is ok since we got attributeId from
                    // the entry via the enumeration - it's in there as is for sure
                    for ( Value value : attribute )
                    {
                        String normalized =  value.getNormalized();
                        ( ( Index ) userIndex ).drop( partitionTxn, normalized, id );
                    }

                    presenceIdx.drop( partitionTxn, attributeOid, id );
                }
            }

            lockWrite();

            try
            {
                rdnIdx.drop( partitionTxn, id );

                ////dumpRdnIdx();

                entryDnCache.remove( id );
                
                Attribute csn = entry.get( entryCsnAT );
                // can be null while doing subentry deletion
                if ( csn != null )
                {
                    setContextCsn( csn.getString() );
                }

                master.remove( partitionTxn, id );
            }
            finally
            {
                unlockWrite();
            }

            if ( isSyncOnWrite.get() )
            {
                sync();
            }

            return entry;
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    //---------------------------------------------------------------------------------------------
    // The Search operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        PartitionTxn partitionTxn = searchContext.getTransaction();
        
        assert ( partitionTxn != null );

        try
        {
            setRWLock( searchContext );

            if ( ctxCsnChanged && getSuffixDn().equals( searchContext.getDn() ) )
            {
                try
                {
                    ctxCsnSemaphore.acquire();
                    saveContextCsn( partitionTxn );
                    ctxCsnChanged = false;
                }
                catch ( Exception e )
                {
                    throw new LdapOperationErrorException( e.getMessage(), e );
                }
                finally
                {
                    ctxCsnSemaphore.release();
                }
            }
            
            PartitionSearchResult searchResult = searchEngine.computeResult( partitionTxn, schemaManager, searchContext );

            Cursor<Entry> result = new EntryCursorAdaptor( partitionTxn, this, searchResult );

            return new EntryFilteringCursorImpl( result, searchContext, schemaManager );
        }
        catch ( LdapException le )
        {
            // TODO: SearchEngine.cursor() should only throw LdapException, then the exception handling here can be removed
            throw le;
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    //---------------------------------------------------------------------------------------------
    // The Lookup operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        PartitionTxn partitionTxn = lookupContext.getTransaction();
        
        assert ( partitionTxn != null );

        try
        {
            setRWLock( lookupContext );
            String id = getEntryId( partitionTxn, lookupContext.getDn() );
    
            if ( id == null )
            {
                return null;
            }
    
            if ( ctxCsnChanged && getSuffixDn().getNormName().equals( lookupContext.getDn().getNormName() ) )
            {
                try
                {
                    ctxCsnSemaphore.acquire();
                    saveContextCsn( partitionTxn );
                }
                catch ( Exception e )
                {
                    throw new LdapOperationErrorException( e.getMessage(), e );
                }
                finally
                {
                    ctxCsnSemaphore.release();
                }
            }
    
            return fetch( partitionTxn, id, lookupContext.getDn() );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage() );
        }
    }


    /**
     * Get back an entry knowing its UUID
     *
     * @param partitionTxn The transaction to use
     * @param id The Entry UUID we want to get back
     * @return The found Entry, or null if not found
     * @throws LdapException If the lookup failed for any reason (except a not found entry)
     */
    @Override
    public Entry fetch( PartitionTxn partitionTxn, String id ) throws LdapException
    {
        try
        {
            rwLock.readLock().lock();

            Dn dn = buildEntryDn( partitionTxn, id );

            return fetch( partitionTxn, id, dn );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
        finally
        {
            rwLock.readLock().unlock();
        }
    }


    /**
     * Get back an entry knowing its UUID
     *
     * @param partitionTxn The transaction to use
     * @param id The Entry UUID we want to get back
     * @return The found Entry, or null if not found
     * @throws LdapException If the lookup failed for any reason (except a not found entry)
     */
    @Override
    public Entry fetch( PartitionTxn partitionTxn, String id, Dn dn ) throws LdapException
    {
        try
        {
            Entry entry = lookupCache( id );

            if ( entry != null )
            {
                entry.setDn( dn );

                entry = new ClonedServerEntry( entry );

                // Replace the entry's DN with the provided one
                Attribute entryDnAt = entry.get( entryDnAT );
                Value dnValue = new Value( entryDnAT, dn.getName(), dn.getNormName() );

                if ( entryDnAt == null )
                {
                    entry.add( entryDnAT, dnValue );
                }
                else
                {
                    entryDnAt.clear();
                    entryDnAt.add( dnValue );
                }

                return entry;
            }

            try
            {
                rwLock.readLock().lock();
                entry = master.get( partitionTxn, id );
            }
            finally
            {
                rwLock.readLock().unlock();
            }

            if ( entry != null )
            {
                // We have to store the DN in this entry
                entry.setDn( dn );

                // always store original entry in the cache
                addToCache( id, entry );

                entry = new ClonedServerEntry( entry );

                if ( !entry.containsAttribute( entryDnAT ) )
                {
                    entry.add( entryDnAT, dn.getName() );
                }

                return entry;
            }

            return null;
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    //---------------------------------------------------------------------------------------------
    // The Modify operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        PartitionTxn partitionTxn = modifyContext.getTransaction();
        
        assert ( partitionTxn != null );
        assert ( partitionTxn instanceof PartitionWriteTxn );

        try
        {
            setRWLock( modifyContext );

            Entry modifiedEntry = modify( partitionTxn, modifyContext.getDn(),
                modifyContext.getModItems().toArray( new Modification[]
                    {} ) );

            modifyContext.setAlteredEntry( modifiedEntry );

            updateCache( modifyContext );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final synchronized Entry modify( PartitionTxn partitionTxn, Dn dn, Modification... mods ) throws LdapException
    {
        String id = getEntryId( partitionTxn, dn );
        Entry entry = master.get( partitionTxn, id );

        for ( Modification mod : mods )
        {
            Attribute attrMods = mod.getAttribute();

            try
            { 
                switch ( mod.getOperation() )
                {
                    case ADD_ATTRIBUTE:
                        modifyAdd( partitionTxn, id, entry, attrMods );
                        break;
    
                    case REMOVE_ATTRIBUTE:
                        modifyRemove( partitionTxn, id, entry, attrMods );
                        break;
    
                    case REPLACE_ATTRIBUTE:
                        modifyReplace( partitionTxn, id, entry, attrMods );
                        break;
    
                    default:
                        throw new LdapException( I18n.err( I18n.ERR_221 ) );
                }
            }
            catch ( IndexNotFoundException infe )
            {
                throw new LdapOtherException( infe.getMessage(), infe );
            }
        }

        updateCsnIndex( partitionTxn, entry, id );

        // Remove the EntryDN
        entry.removeAttributes( entryDnAT );

        setContextCsn( entry.get( entryCsnAT ).getString() );
        
        master.put( partitionTxn, id, entry );

        return entry;
    }


    /**
     * Adds a set of attribute values while affecting the appropriate userIndices.
     * The entry is not persisted: it is only changed in anticipation for a put
     * into the master table.
     *
     * @param partitionTxn The transaction to use
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mods the attribute and values to add
     * @throws Exception if index alteration or attribute addition fails
     */
    @SuppressWarnings("unchecked")
    private void modifyAdd( PartitionTxn partitionTxn, String id, Entry entry, Attribute mods ) 
        throws LdapException, IndexNotFoundException
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new LdapOtherException( I18n.err( I18n.ERR_215_CANNOT_STORE_CLONED_SERVER_ENTRY ) );
        }

        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );
        String normalizedModsOid = presenceNormalizer.normalize( modsOid );

        AttributeType attributeType = mods.getAttributeType();

        // Special case for the ObjectClass index
        if ( modsOid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
        {
            for ( Value value : mods )
            {
                if ( value.equals( topOCValue ) )
                {
                    continue;
                }
                
                String normalizedOc = objectClassNormalizer.normalize( value.getValue() );

                objectClassIdx.add( partitionTxn, normalizedOc, id );
            }
        }
        else if ( hasUserIndexOn( attributeType ) )
        {
            Index<?, String> userIndex = getUserIndex( attributeType );

            if ( mods.size() > 0 )
            {
                for ( Value value : mods )
                {
                    String normalized = value.getNormalized();
                    ( ( Index ) userIndex ).add( partitionTxn, normalized, id );
                }
            }
            else
            {
                // Special case when we have null values
                ( ( Index ) userIndex ).add( partitionTxn, null, id );
            }

            // If the attr didn't exist for this id add it to presence index
            if ( !presenceIdx.forward( partitionTxn, normalizedModsOid, id ) )
            {
                presenceIdx.add( partitionTxn, normalizedModsOid, id );
            }
        }
        // Special case for the AdministrativeRole index
        else if ( modsOid.equals( SchemaConstants.ADMINISTRATIVE_ROLE_AT_OID ) )
        {
            // We may have more than one role 
            for ( Value value : mods )
            {
                adminRoleIdx.add( partitionTxn, value.getValue(), id );
            }

            // If the attr didn't exist for this id add it to presence index
            if ( !presenceIdx.forward( partitionTxn, normalizedModsOid, id ) )
            {
                presenceIdx.add( partitionTxn, normalizedModsOid, id );
            }
        }

        // add all the values in mods to the same attribute in the entry
        if ( mods.size() > 0 )
        {
            for ( Value value : mods )
            {
                entry.add( mods.getAttributeType(), value );
            }
        }
        else
        {
            // Special cases for null values
            if ( mods.getAttributeType().getSyntax().isHumanReadable() )
            {
                entry.add( mods.getAttributeType(), new Value( mods.getAttributeType(), ( String ) null ) );
            }
            else
            {
                entry.add( mods.getAttributeType(), new Value( mods.getAttributeType(), ( byte[] ) null ) );
            }
        }

        if ( modsOid.equals( SchemaConstants.ALIASED_OBJECT_NAME_AT_OID ) )
        {
            Dn ndn = getEntryDn( partitionTxn, id );
            addAliasIndices( partitionTxn, id, ndn, new Dn( schemaManager, mods.getString() ) );
        }
    }


    /**
     * Completely replaces the existing set of values for an attribute with the
     * modified values supplied affecting the appropriate userIndices.  The entry
     * is not persisted: it is only changed in anticipation for a put into the
     * master table.
     *
     * @param partitionTxn The transaction to use
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mods the replacement attribute and values
     * @throws Exception if index alteration or attribute modification
     * fails.
     */
    @SuppressWarnings("unchecked")
    private void modifyReplace( PartitionTxn partitionTxn, String id, Entry entry, Attribute mods ) 
        throws LdapException, IndexNotFoundException
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new LdapOtherException( I18n.err( I18n.ERR_215_CANNOT_STORE_CLONED_SERVER_ENTRY ) );
        }

        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );
        AttributeType attributeType = mods.getAttributeType();

        // Special case for the ObjectClass index
        if ( attributeType.equals( objectClassAT ) )
        {
            // if the id exists in the index drop all existing attribute
            // value index entries and add new ones
            for ( Value value : entry.get( objectClassAT ) )
            {
                if ( value.equals( topOCValue ) )
                {
                    continue;
                }
                
                String normalizedOc = objectClassNormalizer.normalize( value.getValue() );

                objectClassIdx.drop( partitionTxn, normalizedOc, id );
            }

            for ( Value value : mods )
            {
                if ( value.equals( topOCValue ) )
                {
                    continue;
                }
                
                String normalizedOc = objectClassNormalizer.normalize( value.getValue() );

                objectClassIdx.add( partitionTxn, normalizedOc, id );
            }
        }
        else if ( hasUserIndexOn( attributeType ) )
        {
            Index<?, String> userIndex = getUserIndex( attributeType );

            // Drop all the previous values
            Attribute oldAttribute = entry.get( mods.getAttributeType() );

            if ( oldAttribute != null )
            {
                for ( Value value : oldAttribute )
                {
                    String normalized = value.getNormalized();
                    ( ( Index<Object, String> ) userIndex ).drop( partitionTxn, normalized, id );
                }
            }

            // And add the new ones
            for ( Value value : mods )
            {
                String normalized = value.getNormalized();
                ( ( Index ) userIndex ).add( partitionTxn, normalized, id );
            }

            /*
             * If we have no new value, we have to drop the AT fro the presence index
             */
            if ( mods.size() == 0 )
            {
                presenceIdx.drop( partitionTxn, modsOid, id );
            }
        }
        // Special case for the AdministrativeRole index
        else if ( attributeType.equals( administrativeRoleAT ) )
        {
            // Remove the previous values
            for ( Value value : entry.get( administrativeRoleAT ) )
            {
                if ( value.equals( topOCValue ) )
                {
                    continue;
                }
                
                String normalizedOc = objectClassNormalizer.normalize( value.getValue() );

                objectClassIdx.drop( partitionTxn, normalizedOc, id );
            }

            // And add the new ones 
            for ( Value value : mods )
            {
                String valueStr = value.getValue();

                if ( valueStr.equals( topOCValue ) )
                {
                    continue;
                }
                
                adminRoleIdx.add( partitionTxn, valueStr, id );
            }
        }

        String aliasAttributeOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.ALIASED_OBJECT_NAME_AT );

        if ( mods.getAttributeType().equals( aliasedObjectNameAT ) )
        {
            dropAliasIndices( partitionTxn, id );
        }

        // replaces old attributes with new modified ones if they exist
        if ( mods.size() > 0 )
        {
            entry.put( mods );
        }
        else
        // removes old attributes if new replacements do not exist
        {
            entry.remove( mods );
        }

        if ( modsOid.equals( aliasAttributeOid ) && mods.size() > 0 )
        {
            Dn entryDn = getEntryDn( partitionTxn, id );
            addAliasIndices( partitionTxn, id, entryDn, new Dn( schemaManager, mods.getString() ) );
        }
    }


    /**
     * Completely removes the set of values for an attribute having the values
     * supplied while affecting the appropriate userIndices.  The entry is not
     * persisted: it is only changed in anticipation for a put into the master
     * table.  Note that an empty attribute w/o values will remove all the
     * values within the entry where as an attribute w/ values will remove those
     * attribute values it contains.
     *
     * @param partitionTxn The transaction to use
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mods the attribute and its values to delete
     * @throws Exception if index alteration or attribute modification fails.
     */
    @SuppressWarnings("unchecked")
    private void modifyRemove( PartitionTxn partitionTxn, String id, Entry entry, Attribute mods ) 
        throws LdapException, IndexNotFoundException
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new LdapOtherException( I18n.err( I18n.ERR_215_CANNOT_STORE_CLONED_SERVER_ENTRY ) );
        }

        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );
        AttributeType attributeType = mods.getAttributeType();

        // Special case for the ObjectClass index
        if ( attributeType.equals( objectClassAT ) )
        {
            /*
             * If there are no attribute values in the modifications then this
             * implies the complete removal of the attribute from the index. Else
             * we remove individual tuples from the index.
             */
            if ( mods.size() == 0 )
            {
                for ( Value value : entry.get( objectClassAT ) )
                {
                    if ( value.equals( topOCValue ) )
                    {
                        continue;
                    }
                    
                    String normalizedOc = objectClassNormalizer.normalize( value.getValue() );

                    objectClassIdx.drop( partitionTxn, normalizedOc, id );
                }
            }
            else
            {
                for ( Value value : mods )
                {
                    if ( value.equals( topOCValue ) )
                    {
                        continue;
                    }
                    
                    String normalizedOc = objectClassNormalizer.normalize( value.getValue() );

                    objectClassIdx.drop( partitionTxn, normalizedOc, id );
                }
            }
        }
        else if ( hasUserIndexOn( attributeType ) )
        {
            Index<?, String> userIndex = getUserIndex( attributeType );

            Attribute attribute = entry.get( attributeType ).clone();
            int nbValues = 0;

            if ( attribute != null )
            {
                nbValues = attribute.size();
            }

            /*
             * If there are no attribute values in the modifications then this
             * implies the complete removal of the attribute from the index. Else
             * we remove individual tuples from the index.
             */
            if ( mods.size() == 0 )
            {
                ( ( Index ) userIndex ).drop( partitionTxn, id );
                nbValues = 0;
            }
            else if ( nbValues > 0 )
            {
                for ( Value value : mods )
                {
                    if ( attribute.contains( value ) )
                    {
                        nbValues--;
                        attribute.remove( value );
                    }

                    String normalized = value.getNormalized();
                    ( ( Index ) userIndex ).drop( partitionTxn, normalized, id );
                }
            }

            /*
             * If no attribute values exist for this entryId in the index then
             * we remove the presence index entry for the removed attribute.
             */
            if ( nbValues == 0 )
            {
                presenceIdx.drop( partitionTxn, modsOid, id );
            }
        }
        // Special case for the AdministrativeRole index
        else if ( modsOid.equals( SchemaConstants.ADMINISTRATIVE_ROLE_AT_OID ) )
        {
            // We may have more than one role 
            for ( Value value : mods )
            {
                adminRoleIdx.drop( partitionTxn, value.getValue(), id );
            }

            /*
             * If no attribute values exist for this entryId in the index then
             * we remove the presence index entry for the removed attribute.
             */
            if ( null == adminRoleIdx.reverseLookup( partitionTxn, id ) )
            {
                presenceIdx.drop( partitionTxn, modsOid, id );
            }
        }

        /*
         * If there are no attribute values in the modifications then this
         * implies the complete removal of the attribute from the entry. Else
         * we remove individual attribute values from the entry in mods one
         * at a time.
         */
        if ( mods.size() == 0 )
        {
            entry.removeAttributes( mods.getAttributeType() );
        }
        else
        {
            Attribute entryAttr = entry.get( mods.getAttributeType() );

            for ( Value value : mods )
            {
                entryAttr.remove( value );
            }

            // if nothing is left just remove empty attribute
            if ( entryAttr.size() == 0 )
            {
                entry.removeAttributes( entryAttr.getId() );
            }
        }

        // Aliases->single valued comp/partial attr removal is not relevant here
        if ( mods.getAttributeType().equals( aliasedObjectNameAT ) )
        {
            dropAliasIndices( partitionTxn, id );
        }
    }


    //---------------------------------------------------------------------------------------------
    // The Move operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        if ( moveContext.getNewSuperior().isDescendantOf( moveContext.getDn() ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                "cannot place an entry below itself" );
        }
        
        PartitionTxn partitionTxn = moveContext.getTransaction();

        assert ( partitionTxn != null );
        assert ( partitionTxn instanceof PartitionWriteTxn );

        try
        {
            setRWLock( moveContext );
            Dn oldDn = moveContext.getDn();
            Dn newSuperior = moveContext.getNewSuperior();
            Dn newDn = moveContext.getNewDn();
            Entry modifiedEntry = moveContext.getModifiedEntry();

            move( partitionTxn, oldDn, newSuperior, newDn, modifiedEntry );
            updateCache( moveContext );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final synchronized void move( PartitionTxn partitionTxn, Dn oldDn, Dn newSuperiorDn, Dn newDn, Entry modifiedEntry )
        throws LdapException
    {
        // Check that the parent Dn exists
        String newParentId = getEntryId( partitionTxn, newSuperiorDn );

        if ( newParentId == null )
        {
            // This is not allowed : the parent must exist
            throw new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_256_NO_SUCH_OBJECT, newSuperiorDn.getName() ) );
        }

        // Now check that the new entry does not exist
        String newId = getEntryId( partitionTxn, newDn );

        if ( newId != null )
        {
            // This is not allowed : we should not be able to move an entry
            // to an existing position
            throw new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, newSuperiorDn.getName() ) );
        }

        // Get the entry and the old parent IDs
        String entryId = getEntryId( partitionTxn, oldDn );
        String oldParentId = getParentId( partitionTxn, entryId );

        /*
         * All aliases including and below oldChildDn, will be affected by
         * the move operation with respect to one and subtree userIndices since
         * their relationship to ancestors above oldChildDn will be
         * destroyed.  For each alias below and including oldChildDn we will
         * drop the index tuples mapping ancestor ids above oldChildDn to the
         * respective target ids of the aliases.
         */
        dropMovedAliasIndices( partitionTxn, oldDn );

        // Update the Rdn index
        // First drop the old entry
        ParentIdAndRdn movedEntry = rdnIdx.reverseLookup( partitionTxn, entryId );

        updateRdnIdx( partitionTxn, oldParentId, REMOVE_CHILD, movedEntry.getNbDescendants() );

        rdnIdx.drop( partitionTxn, entryId );

        // Now, add the new entry at the right position
        movedEntry.setParentId( newParentId );
        rdnIdx.add( partitionTxn, movedEntry, entryId );

        updateRdnIdx( partitionTxn, newParentId, ADD_CHILD, movedEntry.getNbDescendants() );

        /*
         * Read Alias Index Tuples
         *
         * If this is a name change due to a move operation then the one and
         * subtree userIndices for aliases were purged before the aliases were
         * moved.  Now we must add them for each alias entry we have moved.
         *
         * aliasTarget is used as a marker to tell us if we're moving an
         * alias.  If it is null then the moved entry is not an alias.
         */
        Dn aliasTarget = aliasIdx.reverseLookup( partitionTxn, entryId );

        if ( null != aliasTarget )
        {
            if ( !aliasTarget.isSchemaAware() )
            {
                aliasTarget = new Dn( schemaManager, aliasTarget );
            }
            

            addAliasIndices( partitionTxn, entryId, buildEntryDn( partitionTxn, entryId ), aliasTarget );
        }

        // the below case arises only when the move( Dn oldDn, Dn newSuperiorDn, Dn newDn  ) is called
        // directly using the Store API, in this case the value of modified entry will be null
        // we need to lookup the entry to update the parent UUID
        if ( modifiedEntry == null )
        {
            modifiedEntry = fetch( partitionTxn, entryId );
        }

        // Update the master table with the modified entry
        modifiedEntry.put( ApacheSchemaConstants.ENTRY_PARENT_ID_AT, newParentId );

        // Remove the EntryDN
        modifiedEntry.removeAttributes( entryDnAT );

        entryDnCache.clear();
        
        setContextCsn( modifiedEntry.get( entryCsnAT ).getString() );

        master.put( partitionTxn, entryId, modifiedEntry );

        if ( isSyncOnWrite.get() )
        {
            sync();
        }
    }


    //---------------------------------------------------------------------------------------------
    // The MoveAndRename operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        if ( moveAndRenameContext.getNewSuperiorDn().isDescendantOf( moveAndRenameContext.getDn() ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                "cannot place an entry below itself" );
        }

        PartitionTxn partitionTxn = moveAndRenameContext.getTransaction();

        assert ( partitionTxn != null );
        assert ( partitionTxn instanceof PartitionWriteTxn );

        try
        {
            setRWLock( moveAndRenameContext );
            Dn oldDn = moveAndRenameContext.getDn();
            Dn newSuperiorDn = moveAndRenameContext.getNewSuperiorDn();
            Rdn newRdn = moveAndRenameContext.getNewRdn();
            Entry modifiedEntry = moveAndRenameContext.getModifiedEntry();
            Map<String, List<ModDnAva>> modAvas = moveAndRenameContext.getModifiedAvas();

            moveAndRename( partitionTxn, oldDn, newSuperiorDn, newRdn, modAvas, modifiedEntry );
            updateCache( moveAndRenameContext );
        }
        catch ( LdapException le )
        {
            // In case we get an LdapException, just rethrow it as is to
            // avoid having it lost
            throw le;
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * Moves an entry under a new parent.  The operation causes a shift in the
     * parent child relationships between the old parent, new parent and the
     * child moved.  All other descendant entries under the child never change
     * their direct parent child relationships.  Hence after the parent child
     * relationship changes are broken at the old parent and set at the new
     * parent a modifyDn operation is conducted to handle name changes
     * propagating down through the moved child and its descendants.
     *
     * @param oldDn the normalized dn of the child to be moved
     * @param newSuperiorDn the id of the child being moved
     * @param newRdn the normalized dn of the new parent for the child
     * @param modAvas The modified Avas
     * @param modifiedEntry the modified entry
     * @throws LdapException if something goes wrong
     */
    @Override
    public void moveAndRename( PartitionTxn partitionTxn, Dn oldDn, Dn newSuperiorDn, Rdn newRdn, Map<String, 
            List<ModDnAva>> modAvas, Entry modifiedEntry ) throws LdapException
    {
        // Get the child and the new parent to be entries and Ids
        Attribute entryIdAt = modifiedEntry.get( SchemaConstants.ENTRY_UUID_AT );
        String entryId;
        
        if ( entryIdAt == null )
        {
            entryId = getEntryId( partitionTxn, modifiedEntry.getDn() );
        }
        else
        {
            entryId = modifiedEntry.get( SchemaConstants.ENTRY_UUID_AT ).getString();
        }

        Attribute oldParentIdAt = modifiedEntry.get( ApacheSchemaConstants.ENTRY_PARENT_ID_AT );
        String oldParentId;
        
        if ( oldParentIdAt == null )
        {
            oldParentId = getEntryId( partitionTxn, oldDn.getParent() );
        }
        else
        {
            oldParentId = oldParentIdAt.getString();
        }

        String newParentId = getEntryId( partitionTxn, newSuperiorDn );

        //Get the info about the moved entry
        ParentIdAndRdn movedEntry = rdnIdx.reverseLookup( partitionTxn, entryId );
        
        // First drop the moved entry from the rdn index
        rdnIdx.drop( partitionTxn, entryId );

        //
        // The update the Rdn index. We will remove the ParentIdAndRdn associated with the
        // moved entry, and update the nbChilden of its parent and the nbSubordinates
        // of all its ascendant, up to the common superior.
        // Then we will add a ParentidAndRdn for the moved entry under the new superior,
        // update its children number and the nbSubordinates of all the new ascendant.
        updateRdnIdx( partitionTxn, oldParentId, REMOVE_CHILD, movedEntry.getNbDescendants() );

        /*
         * All aliases including and below oldChildDn, will be affected by
         * the move operation with respect to one and subtree userIndices since
         * their relationship to ancestors above oldChildDn will be
         * destroyed.  For each alias below and including oldChildDn we will
         * drop the index tuples mapping ancestor ids above oldChildDn to the
         * respective target ids of the aliases.
         */
        dropMovedAliasIndices( partitionTxn, oldDn );

        // Now, add the new entry at the right position
        // First
        movedEntry.setParentId( newParentId );
        movedEntry.setRdns( new Rdn[]
            { newRdn } );
        rdnIdx.add( partitionTxn, movedEntry, entryId );

        updateRdnIdx( partitionTxn, newParentId, ADD_CHILD, movedEntry.getNbDescendants() );

        // Process the modified indexes now
        try
        {
            processModifiedAvas( partitionTxn, modAvas, entryId );
        }
        catch ( IndexNotFoundException infe )
        {
            throw new LdapOtherException( infe.getMessage(), infe );
        }

        /*
         * Read Alias Index Tuples
         *
         * If this is a name change due to a move operation then the one and
         * subtree userIndices for aliases were purged before the aliases were
         * moved.  Now we must add them for each alias entry we have moved.
         *
         * aliasTarget is used as a marker to tell us if we're moving an
         * alias.  If it is null then the moved entry is not an alias.
         */
        Dn aliasTarget = aliasIdx.reverseLookup( partitionTxn, entryId );

        if ( null != aliasTarget )
        {
            if ( !aliasTarget.isSchemaAware() )
            {
                aliasTarget = new Dn( schemaManager, aliasTarget );
            }
            
            addAliasIndices( partitionTxn, entryId, buildEntryDn( partitionTxn, entryId ), aliasTarget );
        }

        // Remove the EntryDN
        modifiedEntry.removeAttributes( entryDnAT );
        
        // Update the entryParentId attribute
        modifiedEntry.removeAttributes( ApacheSchemaConstants.ENTRY_PARENT_ID_OID );
        modifiedEntry.add( ApacheSchemaConstants.ENTRY_PARENT_ID_OID, newParentId );
        
        // Doom the DN cache now
        entryDnCache.clear();

        setContextCsn( modifiedEntry.get( entryCsnAT ).getString() );

        // save the modified entry at the new place
        master.put( partitionTxn, entryId, modifiedEntry );
    }
    
    
    /**
     * Update the index accordingly to the changed Attribute in the old and new RDN
     * 
     * @param partitionTxn The transaction to use
     * @param modAvs The modified AVAs
     * @param entryId The Entry ID
     * @throws {@link LdapException} If the AVA cannt be processed properly
     * @throws IndexNotFoundException If teh index is not found
     */
    private void processModifiedAvas( PartitionTxn partitionTxn, Map<String, List<ModDnAva>> modAvas, String entryId ) 
        throws LdapException, IndexNotFoundException
    {
        for ( List<ModDnAva> modDnAvas : modAvas.values() )
        {
            for ( ModDnAva modDnAva : modDnAvas )
            {
                AttributeType attributeType = modDnAva.getAva().getAttributeType();
                
                if ( !hasIndexOn( attributeType ) )
                {
                    break;
                }

                Index<?, String> index = getUserIndex( attributeType );
                
                switch ( modDnAva.getType() )
                {
                    case ADD :
                    case UPDATE_ADD :
                        // Add Value in the index
                        ( ( Index ) index ).add( partitionTxn, modDnAva.getAva().getValue().getNormalized(), entryId );

                        /*
                         * If there is no value for id in this index due to our
                         * add above we add the entry in the presence idx
                         */
                        if ( null == index.reverseLookup( partitionTxn, entryId ) )
                        {
                            presenceIdx.add( partitionTxn, attributeType.getOid(), entryId );
                        }
                        
                        break;

                    case DELETE :
                    case UPDATE_DELETE :
                        ( ( Index ) index ).drop( partitionTxn, modDnAva.getAva().getValue().getNormalized(), entryId );

                        /*
                         * If there is no value for id in this index due to our
                         * drop above we remove the oldRdnAttr from the presence idx
                         */
                        if ( null == index.reverseLookup( partitionTxn, entryId ) )
                        {
                            presenceIdx.drop( partitionTxn, attributeType.getOid(), entryId );
                        }
                        
                        break;
                        
                    default :
                        break;
                }
            }
        }
    }
    
    
    //---------------------------------------------------------------------------------------------
    // The Rename operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        PartitionTxn partitionTxn = renameContext.getTransaction();

        assert ( partitionTxn != null );
        assert ( partitionTxn instanceof PartitionWriteTxn );

        try
        {
            setRWLock( renameContext );
            Dn oldDn = renameContext.getDn();
            Rdn newRdn = renameContext.getNewRdn();
            boolean deleteOldRdn = renameContext.getDeleteOldRdn();

            if ( renameContext.getEntry() != null )
            {
                Entry modifiedEntry = renameContext.getModifiedEntry();
                rename( partitionTxn, oldDn, newRdn, deleteOldRdn, modifiedEntry );
            }
            else
            {
                rename( partitionTxn, oldDn, newRdn, deleteOldRdn, null );
            }

            updateCache( renameContext );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * This will rename the entry, and deal with the deleteOldRdn flag. If set to true, we have
     * to remove the AVA which are not part of the new RDN from the entry.
     * If this flag is set to false, we have to take care of the special case of an AVA
     * which attributeType is SINGLE-VALUE : in this case, we remove the old value.
     */
    private void rename( PartitionTxn partitionTxn, String oldId, Rdn newRdn, boolean deleteOldRdn, Entry entry ) 
        throws LdapException, IndexNotFoundException
    {
        if ( entry == null )
        {
            entry = master.get( partitionTxn, oldId );
        }

        Dn updn = entry.getDn();

        if ( !newRdn.isSchemaAware() )
        {
            newRdn = new Rdn( schemaManager, newRdn );
        }

        /*
         * H A N D L E   N E W   R D N
         * ====================================================================
         * Add the new Rdn attribute to the entry.  If an index exists on the
         * new Rdn attribute we add the index for this attribute value pair.
         * Also we make sure that the presence index shows the existence of the
         * new Rdn attribute within this entry.
         * Last, not least, if the AttributeType is single value, take care
         * of removing the old value.
         */
        for ( Ava newAtav : newRdn )
        {
            String newNormType = newAtav.getNormType();
            Object newNormValue = newAtav.getValue().getValue();
            //boolean oldRemoved = false;

            AttributeType newRdnAttrType = schemaManager.lookupAttributeTypeRegistry( newNormType );

            if ( newRdnAttrType.isSingleValued() && entry.containsAttribute( newRdnAttrType ) )
            {
                Attribute oldAttribute = entry.get( newRdnAttrType );
                AttributeType oldAttributeType = oldAttribute.getAttributeType();
                
                // We have to remove the old attribute value, if we have some
                entry.removeAttributes( newRdnAttrType );
                
                // Deal with the index
                if ( hasUserIndexOn( newRdnAttrType ) )
                {
                    Index<?, String> userIndex = getUserIndex( newRdnAttrType );

                    String normalized = oldAttributeType.getEquality().getNormalizer().normalize( oldAttribute.get().getValue() );
                    ( ( Index ) userIndex ).drop( partitionTxn, normalized, id );

                    /*
                     * If there is no value for id in this index due to our
                     * drop above we remove the oldRdnAttr from the presence idx
                     */
                    if ( null == userIndex.reverseLookup( partitionTxn, oldId ) )
                    {
                        presenceIdx.drop( partitionTxn, newRdnAttrType.getOid(), oldId );
                    }

                }
            }

            if ( newRdnAttrType.getSyntax().isHumanReadable() )
            {
                entry.add( newRdnAttrType, newAtav.getValue().getValue() );
            }
            else
            {
                entry.add( newRdnAttrType, newAtav.getValue().getBytes() );
            }

            if ( hasUserIndexOn( newRdnAttrType ) )
            {
                Index<?, String> userIndex = getUserIndex( newRdnAttrType );
                
                /*
                if ( oldRemoved )
                {
                    String normalized = newRdnAttrType.getEquality().getNormalizer().normalize( newNormValue );
                    ( ( Index ) userIndex ).add( normalized, id );
                    ( ( Index ) index ).drop( newNormValue, oldId );
                }
                */
                
                String normalized = newRdnAttrType.getEquality().getNormalizer().normalize( ( String ) newNormValue );
                ( ( Index ) userIndex ).add( partitionTxn, normalized, oldId );
                
                
                //( ( Index ) index ).add( newNormValue, oldId );

                // Make sure the altered entry shows the existence of the new attrib
                String normTypeOid = presenceNormalizer.normalize( newNormType );
                
                if ( !presenceIdx.forward( partitionTxn, normTypeOid, oldId ) )
                {
                    presenceIdx.add( partitionTxn, normTypeOid, oldId );
                }
            }
        }

        /*
         * H A N D L E   O L D   R D N
         * ====================================================================
         * If the old Rdn is to be removed we need to get the attribute and
         * value for it.  Keep in mind the old Rdn need not be based on the
         * same attr as the new one.  We remove the Rdn value from the entry
         * and remove the value/id tuple from the index on the old Rdn attr
         * if any.  We also test if the delete of the old Rdn index tuple
         * removed all the attribute values of the old Rdn using a reverse
         * lookup.  If so that means we blew away the last value of the old
         * Rdn attribute.  In this case we need to remove the attrName/id
         * tuple from the presence index.
         *
         * We only remove an ATAV of the old Rdn if it is not included in the
         * new Rdn.
         */

        if ( deleteOldRdn )
        {
            Rdn oldRdn = updn.getRdn();

            for ( Ava oldAtav : oldRdn )
            {
                // check if the new ATAV is part of the old Rdn
                // if that is the case we do not remove the ATAV
                boolean mustRemove = true;

                for ( Ava newAtav : newRdn )
                {
                    if ( oldAtav.equals( newAtav ) )
                    {
                        mustRemove = false;
                        break;
                    }
                }

                if ( mustRemove )
                {
                    String oldNormType = oldAtav.getNormType();
                    String oldNormValue = oldAtav.getValue().getValue();
                    AttributeType oldRdnAttrType = schemaManager.lookupAttributeTypeRegistry( oldNormType );
                    entry.remove( oldRdnAttrType, oldNormValue );

                    if ( hasUserIndexOn( oldRdnAttrType ) )
                    {
                        Index<?, String> userIndex = getUserIndex( oldRdnAttrType );
                        
                        String normalized = oldRdnAttrType.getEquality().getNormalizer().normalize( oldNormValue );
                        ( ( Index ) userIndex ).drop( partitionTxn, normalized, id );

                        /*
                         * If there is no value for id in this index due to our
                         * drop above we remove the oldRdnAttr from the presence idx
                         */
                        if ( null == userIndex.reverseLookup( partitionTxn, oldId ) )
                        {
                            String oldNormTypeOid = presenceNormalizer.normalize( oldNormType );
                            presenceIdx.drop( partitionTxn, oldNormTypeOid, oldId );
                        }
                    }
                }
            }
        }

        // Remove the EntryDN
        entry.removeAttributes( entryDnAT );

        setContextCsn( entry.get( entryCsnAT ).getString() );

        // And save the modified entry
        master.put( partitionTxn, oldId, entry );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public final synchronized void rename( PartitionTxn partitionTxn, Dn dn, Rdn newRdn, boolean deleteOldRdn, Entry entry ) 
        throws LdapException
    {
        String oldId = getEntryId( partitionTxn, dn );

        try
        {
            rename( partitionTxn, oldId, newRdn, deleteOldRdn, entry );
        }
        catch ( IndexNotFoundException infe )
        {
            throw new LdapOtherException( infe.getMessage(), infe );
        }

        /*
         * H A N D L E   D N   C H A N G E
         * ====================================================================
         * We only need to update the Rdn index.
         * No need to calculate the new Dn.
         */
        String parentId = getParentId( partitionTxn, oldId );

        // Get the old parentIdAndRdn to get the nb of children and descendant
        ParentIdAndRdn parentIdAndRdn = rdnIdx.reverseLookup( partitionTxn, oldId );

        // Now we can drop it
        rdnIdx.drop( partitionTxn, oldId );

        // Update the descendants
        parentIdAndRdn.setParentId( parentId );
        parentIdAndRdn.setRdns( newRdn );

        rdnIdx.add( partitionTxn, parentIdAndRdn, oldId );

        entryDnCache.clear();
        
        if ( isSyncOnWrite.get() )
        {
            sync();
        }
    }


    //---------------------------------------------------------------------------------------------
    // The Unbind operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public final void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        // does nothing
    }


    /**
     * This method calls {@link Partition#lookup(LookupOperationContext)} and return <tt>true</tt>
     * if it returns an entry by default.  Please override this method if
     * there is more effective way for your implementation.
     */
    @Override
    public boolean hasEntry( HasEntryOperationContext entryContext ) throws LdapException
    {
        PartitionTxn partitionTxn = entryContext.getTransaction();
        
        assert ( partitionTxn != null );

        try
        {
            setRWLock( entryContext );

            String id = getEntryId( partitionTxn, entryContext.getDn() );

            Entry entry = fetch( partitionTxn, id, entryContext.getDn() );

            return entry != null;
        }
        catch ( LdapException e )
        {
            return false;
        }
    }


    //---------------------------------------------------------------------------------------------
    // Helper methods
    //---------------------------------------------------------------------------------------------
    /**
     * updates the CSN index
     *
     * @param partitionTxn The transaction to use
     * @param entry the entry having entryCSN attribute
     * @param id UUID of the entry
     * @throws Exception
     */
    private void updateCsnIndex( PartitionTxn partitionTxn, Entry entry, String id ) throws LdapException
    {
        String entryCsn = entry.get( SchemaConstants.ENTRY_CSN_AT ).getString();
        entryCsnIdx.drop( partitionTxn, id );
        entryCsnIdx.add( partitionTxn, entryCsn, id );
    }


    // ------------------------------------------------------------------------
    // Index and master table Operations
    // ------------------------------------------------------------------------
    /**
     * builds the Dn of the entry identified by the given id
     *
     * @param partitionTxn The transaction to use
     * @param id the entry's id
     * @return the normalized Dn of the entry
     * @throws LdapException If we can't build the entry Dn
     */
    protected Dn buildEntryDn( PartitionTxn partitionTxn, String id ) throws LdapException
    {
        String parentId = id;
        String rootId = Partition.ROOT_ID;

        // Create an array of 10 rdns, just in case. We will extend it if needed
        Rdn[] rdnArray = new Rdn[10];
        int pos = 0;

        Dn dn = null;
        
        try
        {
            rwLock.readLock().lock();

            if ( entryDnCache != null )
            {
                Dn cachedDn = entryDnCache.get( id );
                
                if ( cachedDn != null )
                {
                    return cachedDn;
                }
            }
            
            do
            {
                ParentIdAndRdn cur = null;
            
                if ( piarCache != null )
                {
                   cur = piarCache.get( parentId );
                    
                    if ( cur == null )
                    {
                        cur = rdnIdx.reverseLookup( partitionTxn, parentId );
                        
                        if ( cur == null )
                        {
                            return null;
                        }
                        
                        piarCache.put( parentId, cur );
                    }
                }
                else
                {
                    cur = rdnIdx.reverseLookup( partitionTxn, parentId );
                    
                    if ( cur == null )
                    {
                        return null;
                    }
                }

                Rdn[] rdns = cur.getRdns();

                for ( Rdn rdn : rdns )
                {
                    if ( ( pos > 0 ) && ( pos % 10 == 0 ) )
                    {
                        // extend the array
                        Rdn[] newRdnArray = new Rdn[pos + 10];
                        System.arraycopy( rdnArray, 0, newRdnArray, 0, pos );
                        rdnArray = newRdnArray;
                    }

                    rdnArray[pos++] = rdn;
                }

                parentId = cur.getParentId();
            }
            while ( !parentId.equals( rootId ) );
            
            dn = new Dn( schemaManager, Arrays.copyOf( rdnArray, pos ) );
            
            entryDnCache.put( id, dn );
            return dn;
        }
        finally
        {
            rwLock.readLock().unlock();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long count( PartitionTxn partitionTxn ) throws LdapException
    {
        return master.count( partitionTxn );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final long getChildCount( PartitionTxn partitionTxn, String id ) throws LdapException
    {
        try
        {
            ParentIdAndRdn parentIdAndRdn = rdnIdx.reverseLookup( partitionTxn, id );

            return parentIdAndRdn.getNbChildren();
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final Dn getEntryDn( PartitionTxn partitionTxn, String id ) throws LdapException
    {
        return buildEntryDn( partitionTxn, id );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final String getEntryId( PartitionTxn partitionTxn, Dn dn ) throws LdapException
    {
        try
        {
            if ( Dn.isNullOrEmpty( dn ) )
            {
                return Partition.ROOT_ID;
            }

            ParentIdAndRdn suffixKey = new ParentIdAndRdn( Partition.ROOT_ID, suffixDn.getRdns() );

            // Check into the Rdn index, starting with the partition Suffix
            try
            {
                rwLock.readLock().lock();
                String currentId = rdnIdx.forwardLookup( partitionTxn, suffixKey );

                for ( int i = dn.size() - suffixDn.size(); i > 0; i-- )
                {
                    Rdn rdn = dn.getRdn( i - 1 );
                    ParentIdAndRdn currentRdn = new ParentIdAndRdn( currentId, rdn );
                    
                    currentId = rdnIdx.forwardLookup( partitionTxn, currentRdn );

                    if ( currentId == null )
                    {
                        break;
                    }
                }

                return currentId;
            }
            finally
            {
                rwLock.readLock().unlock();
            }
        }
        catch ( Exception e )
        {
            throw new LdapException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getParentId( PartitionTxn partitionTxn, String childId ) throws LdapException
    {
        try
        {
            rwLock.readLock().lock();
            ParentIdAndRdn key = rdnIdx.reverseLookup( partitionTxn, childId );

            if ( key == null )
            {
                return null;
            }

            return key.getParentId();
        }
        finally
        {
            rwLock.readLock().unlock();
        }
    }


    /**
     * Retrieve the SuffixID
     * 
     * @param partitionTxn The transaction to use
     * @return The Suffix ID
     * @throws LdapException If we weren't able to retrieve the Suffix ID
     */
    public String getSuffixId( PartitionTxn partitionTxn ) throws LdapException
    {
        if ( suffixId == null )
        {
            ParentIdAndRdn key = new ParentIdAndRdn( Partition.ROOT_ID, suffixDn.getRdns() );

            try
            {
                rwLock.readLock().lock();
                suffixId = rdnIdx.forwardLookup( partitionTxn, key );
            }
            finally
            {
                rwLock.readLock().unlock();
            }
        }

        return suffixId;
    }


    //------------------------------------------------------------------------
    // Index handling
    //------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void addIndex( Index<?, String> index ) throws LdapException
    {
        checkInitialized( "addIndex" );

        // Check that the index String is valid
        AttributeType attributeType = null;

        try
        {
            attributeType = schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() );
        }
        catch ( LdapNoSuchAttributeException lnsae )
        {
            LOG.error( "Cannot initialize the index for AttributeType {}, this value does not exist",
                index.getAttributeId() );

            return;
        }

        String oid = attributeType.getOid();

        if ( SYS_INDEX_OIDS.contains( oid ) )
        {
            if ( !systemIndices.containsKey( oid ) )
            {
                systemIndices.put( oid, index );
            }
        }
        else
        {
            if ( !userIndices.containsKey( oid ) )
            {
                userIndices.put( oid, index );
            }
        }
    }


    /**
     * Add some new indexes
     * @param indexes The added indexes
     */
    public void addIndexedAttributes( Index<?, String>... indexes )
    {
        for ( Index<?, String> index : indexes )
        {
            indexedAttributes.add( index );
        }
    }


    /**
     * Set the list of indexes for this partition
     * @param indexedAttributes The list of indexes
     */
    public void setIndexedAttributes( Set<Index<?, String>> indexedAttributes )
    {
        this.indexedAttributes = indexedAttributes;
    }


    /**
     * @return The list of indexed attributes
     */
    public Set<Index<?, String>> getIndexedAttributes()
    {
        return indexedAttributes;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> getUserIndices()
    {
        return userIndices.keySet().iterator();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<String> getSystemIndices()
    {
        return systemIndices.keySet().iterator();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Index<?, String> getIndex( AttributeType attributeType ) throws IndexNotFoundException
    {
        String id = attributeType.getOid();

        if ( userIndices.containsKey( id ) )
        {
            return userIndices.get( id );
        }

        if ( systemIndices.containsKey( id ) )
        {
            return systemIndices.get( id );
        }

        throw new IndexNotFoundException( I18n.err( I18n.ERR_3, id, id ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Index<?, String> getUserIndex( AttributeType attributeType ) throws IndexNotFoundException
    {
        if ( attributeType == null )
        {
            throw new IndexNotFoundException( I18n.err( I18n.ERR_3, attributeType, attributeType ) );
        }

        String oid = attributeType.getOid();

        if ( userIndices.containsKey( oid ) )
        {
            return userIndices.get( oid );
        }

        throw new IndexNotFoundException( I18n.err( I18n.ERR_3, attributeType, attributeType ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Index<?, String> getSystemIndex( AttributeType attributeType ) throws IndexNotFoundException
    {
        if ( attributeType == null )
        {
            throw new IndexNotFoundException( I18n.err( I18n.ERR_2, attributeType, attributeType ) );
        }

        String oid = attributeType.getOid();

        if ( systemIndices.containsKey( oid ) )
        {
            return systemIndices.get( oid );
        }

        throw new IndexNotFoundException( I18n.err( I18n.ERR_2, attributeType, attributeType ) );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Index<Dn, String> getAliasIndex()
    {
        return ( Index<Dn, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Index<String, String> getOneAliasIndex()
    {
        return ( Index<String, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Index<String, String> getSubAliasIndex()
    {
        return ( Index<String, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Index<String, String> getObjectClassIndex()
    {
        return ( Index<String, String> ) systemIndices.get( SchemaConstants.OBJECT_CLASS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Index<String, String> getEntryCsnIndex()
    {
        return ( Index<String, String> ) systemIndices.get( SchemaConstants.ENTRY_CSN_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, String> getAdministrativeRoleIndex()
    {
        return ( Index<String, String> ) systemIndices.get( SchemaConstants.ADMINISTRATIVE_ROLE_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Index<String, String> getPresenceIndex()
    {
        return ( Index<String, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Index<ParentIdAndRdn, String> getRdnIndex()
    {
        return ( Index<ParentIdAndRdn, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_RDN_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasUserIndexOn( AttributeType attributeType ) throws LdapException
    {
        String oid = attributeType.getOid();
        return userIndices.containsKey( oid );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasSystemIndexOn( AttributeType attributeType ) throws LdapException
    {
        return systemIndices.containsKey( attributeType.getOid() );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasIndexOn( AttributeType attributeType ) throws LdapException
    {
        return hasUserIndexOn( attributeType ) || hasSystemIndexOn( attributeType );
    }


    //---------------------------------------------------------------------------------------------
    // Alias index manipulation
    //---------------------------------------------------------------------------------------------
    /**
     * Adds userIndices for an aliasEntry to be added to the database while checking
     * for constrained alias constructs like alias cycles and chaining.
     *
     * @param partitionTxn The transaction to use
     * @param aliasDn normalized distinguished name for the alias entry
     * @param aliasTarget the user provided aliased entry dn as a string
     * @param aliasId the id of alias entry to add
     * @throws LdapException if index addition fails, and if the alias is
     * not allowed due to chaining or cycle formation.
     * @throws LdapException if the wrappedCursor btrees cannot be altered
     */
    protected void addAliasIndices( PartitionTxn partitionTxn, String aliasId, Dn aliasDn, Dn aliasTarget ) 
            throws LdapException
    {
        String targetId; // Id of the aliasedObjectName
        Dn ancestorDn; // Name of an alias entry relative
        String ancestorId; // Id of an alias entry relative

        /*
         * Check For Aliases External To Naming Context
         *
         * id may be null but the alias may be to a valid entry in
         * another namingContext.  Such aliases are not allowed and we
         * need to point it out to the user instead of saying the target
         * does not exist when it potentially could outside of this upSuffix.
         */
        if ( !aliasTarget.isDescendantOf( suffixDn ) )
        {
            String msg = I18n.err( I18n.ERR_225, suffixDn.getName() );
            throw new LdapAliasDereferencingException( msg );
        }

        // L O O K U P   T A R G E T   I D
        targetId = getEntryId( partitionTxn, aliasTarget );

        /*
         * Check For Target Existence
         *
         * We do not allow the creation of inconsistent aliases.  Aliases should
         * not be broken links.  If the target does not exist we start screaming
         */
        if ( null == targetId )
        {
            // Complain about target not existing
            String msg = I18n.err( I18n.ERR_581, aliasDn.getName(), aliasTarget );
            throw new LdapAliasException( msg );
        }

        /*
         * Detect Direct Alias Chain Creation
         *
         * Rather than resusitate the target to test if it is an alias and fail
         * due to chaing creation we use the alias index to determine if the
         * target is an alias.  Hence if the alias we are about to create points
         * to another alias as its target in the aliasedObjectName attribute,
         * then we have a situation where an alias chain is being created.
         * Alias chaining is not allowed so we throw and exception.
         */
        if ( null != aliasIdx.reverseLookup( partitionTxn, targetId ) )
        {
            String msg = I18n.err( I18n.ERR_227 );
            throw new LdapAliasDereferencingException( msg );
        }

        // Add the alias to the simple alias index
        aliasIdx.add( partitionTxn, aliasTarget, aliasId );
        
        if ( aliasCache != null )
        {
            aliasCache.put( aliasId, aliasTarget );
        }

        /*
         * Handle One Level Scope Alias Index
         *
         * The first relative is special with respect to the one level alias
         * index.  If the target is not a sibling of the alias then we add the
         * index entry maping the parent's id to the aliased target id.
         */
        ancestorDn = aliasDn.getParent();
        ancestorId = getEntryId( partitionTxn, ancestorDn );

        // check if alias parent and aliased entry are the same
        Dn normalizedAliasTargetParentDn = aliasTarget.getParent();

        if ( !aliasDn.isDescendantOf( normalizedAliasTargetParentDn ) )
        {
            oneAliasIdx.add( partitionTxn, ancestorId, targetId );
        }

        /*
         * Handle Sub Level Scope Alias Index
         *
         * Walk the list of relatives from the parents up to the upSuffix, testing
         * to see if the alias' target is a descendant of the relative.  If the
         * alias target is not a descentant of the relative it extends the scope
         * and is added to the sub tree scope alias index.  The upSuffix node is
         * ignored since everything is under its scope.  The first loop
         * iteration shall handle the parents.
         */
        while ( !ancestorDn.equals( suffixDn ) && null != ancestorId )
        {
            if ( !aliasTarget.isDescendantOf( ancestorDn ) )
            {
                subAliasIdx.add( partitionTxn, ancestorId, targetId );
            }

            ancestorDn = ancestorDn.getParent();
            ancestorId = getEntryId( partitionTxn, ancestorDn );
        }
    }


    /**
     * Removes the index entries for an alias before the entry is deleted from
     * the master table.
     *
     * TODO Optimize this by walking the hierarchy index instead of the name
     * 
     * @param partitionTxn The transaction to use
     * @param aliasId the id of the alias entry in the master table
     * @throws LdapException if we cannot delete index values in the database
     */
    protected void dropAliasIndices( PartitionTxn partitionTxn, String aliasId ) throws LdapException
    {
        Dn targetDn = aliasIdx.reverseLookup( partitionTxn, aliasId );
        
        if ( !targetDn.isSchemaAware() )
        {
            targetDn = new Dn( schemaManager, targetDn );
        }

        String targetId = getEntryId( partitionTxn, targetDn );

        if ( targetId == null )
        {
            // the entry doesn't exist, probably it has been deleted or renamed
            // TODO: this is just a workaround for now, the alias indices should be updated when target entry is deleted or removed
            return;
        }

        Dn aliasDn = getEntryDn( partitionTxn, aliasId );

        Dn ancestorDn = aliasDn.getParent();
        String ancestorId = getEntryId( partitionTxn, ancestorDn );

        /*
         * We cannot just drop all tuples in the one level and subtree userIndices
         * linking baseIds to the targetId.  If more than one alias refers to
         * the target then droping all tuples with a value of targetId would
         * make all other aliases to the target inconsistent.
         *
         * We need to walk up the path of alias ancestors until we reach the
         * upSuffix, deleting each ( ancestorId, targetId ) tuple in the
         * subtree scope alias.  We only need to do this for the direct parent
         * of the alias on the one level subtree.
         */
        oneAliasIdx.drop( partitionTxn, ancestorId, targetId );
        subAliasIdx.drop( partitionTxn, ancestorId, targetId );

        while ( !ancestorDn.equals( suffixDn ) && ancestorDn.size() > suffixDn.size() )
        {
            ancestorDn = ancestorDn.getParent();
            ancestorId = getEntryId( partitionTxn, ancestorDn );

            subAliasIdx.drop( partitionTxn, ancestorId, targetId );
        }

        // Drops all alias tuples pointing to the id of the alias to be deleted
        aliasIdx.drop( partitionTxn, aliasId );

        if ( aliasCache != null )
        {
            aliasCache.remove( aliasId );
        }
    }


    /**
     * For all aliases including and under the moved base, this method removes
     * one and subtree alias index tuples for old ancestors above the moved base
     * that will no longer be ancestors after the move.
     *
     * @param partitionTxn The transaction to use
     * @param movedBase the base at which the move occurred - the moved node
     * @throws LdapException if system userIndices fail
     */
    protected void dropMovedAliasIndices( PartitionTxn partitionTxn, Dn movedBase ) throws LdapException
    {
        String movedBaseId = getEntryId( partitionTxn, movedBase );

        Dn targetDn = aliasIdx.reverseLookup( partitionTxn, movedBaseId );
        
        if ( targetDn != null )
        {
            if ( !targetDn.isSchemaAware() )
            {
                targetDn = new Dn( schemaManager, targetDn );
            }

            String targetId = getEntryId( partitionTxn, targetDn );
            Dn aliasDn = getEntryDn( partitionTxn, movedBaseId );

            /*
             * Start droping index tuples with the first ancestor right above the
             * moved base.  This is the first ancestor effected by the move.
             */
            Dn ancestorDn = movedBase.getParent();
            String ancestorId = getEntryId( partitionTxn, ancestorDn );

            /*
             * We cannot just drop all tuples in the one level and subtree userIndices
             * linking baseIds to the targetId.  If more than one alias refers to
             * the target then droping all tuples with a value of targetId would
             * make all other aliases to the target inconsistent.
             *
             * We need to walk up the path of alias ancestors right above the moved
             * base until we reach the upSuffix, deleting each ( ancestorId,
             * targetId ) tuple in the subtree scope alias.  We only need to do
             * this for the direct parent of the alias on the one level subtree if
             * the moved base is the alias.
             */
            if ( aliasDn.equals( movedBase ) )
            {
                oneAliasIdx.drop( partitionTxn, ancestorId, targetId );
            }

            subAliasIdx.drop( partitionTxn, ancestorId, targetId );

            while ( !ancestorDn.equals( suffixDn ) )
            {
                ancestorDn = ancestorDn.getParent();
                ancestorId = getEntryId( partitionTxn, ancestorDn );

                subAliasIdx.drop( partitionTxn, ancestorId, targetId );
            }
        }
    }


    //---------------------------------------------------------------------------------------------
    // Debug methods
    //---------------------------------------------------------------------------------------------
    private void dumpIndex( PartitionTxn partitionTxn, OutputStream stream, Index<?, String> index )
    {
        try
        {
            Cursor<IndexEntry<?, String>> cursor = ( Cursor ) index.forwardCursor( partitionTxn );

            while ( cursor.next() )
            {
                IndexEntry<?, String> entry = cursor.get();

                System.out.println( entry );
            }
        }
        catch ( Exception e )
        {
            // TODO : fixme
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void dumpIndex( PartitionTxn partitionTxn, OutputStream stream, String name ) throws IOException
    {
        try
        {
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( name );

            if ( attributeType == null )
            {
                stream.write( Strings.getBytesUtf8( "Cannot find an index for AttributeType names " + name ) );

                return;
            }

            if ( attributeType.getOid().equals( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
            {
                dumpIndex( partitionTxn, stream, rdnIdx );
            }
        }
        catch ( LdapException le )
        {
            stream.write( Strings.getBytesUtf8( "Cannot find an index for AttributeType names " + name ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "Partition<" + id + ">";
    }


    /**
     * Create a new Index for a given OID
     * 
     * @param indexOid The Attribute OID
     * @param path The working directory where this index will be stored
     * @param withReverse If the Reverse index must be created or not
     * @return The created index
     * @throws LdapException If the index can't be created
     */
    protected abstract Index createSystemIndex( String indexOid, URI path, boolean withReverse ) throws LdapException;


    /**
     * {@inheritDoc}
     */
    @Override
    public MasterTable getMasterTable()
    {
        return master;
    }


    /**
     * Acquire a Read lock
     */
    private void lockRead()
    {
        rwLock.readLock().lock();
    }


    /**
     * Release a Read lock
     */
    private void unlockRead()
    {
        rwLock.readLock().unlock();
    }


    /**
     * Acquire a Write lock
     */
    private void lockWrite()
    {
        rwLock.writeLock().lock();
    }


    /**
     * Release a Write lock
     */
    private void unlockWrite()
    {
        rwLock.writeLock().unlock();
    }


    /**
     * updates the cache based on the type of OperationContext
     * 
     * @param opCtx the operation's context
     */
    public void updateCache( OperationContext opCtx )
    {
        // partition implementations should override this if they want to use cache
    }


    /**
     * looks up for the entry with the given ID in the cache
     *
     * @param id the ID of the entry
     * @return the Entry if exists, null otherwise
     */
    public Entry lookupCache( String id )
    {
        return null;
    }


    /**
     * adds the given entry to cache
     *  
     * Note: this method is not called during add operation to avoid filling the cache
     *       with all the added entries
     *       
     * @param id ID of the entry
     * @param entry the Entry
     */
    public void addToCache( String id, Entry entry )
    {
    }


    /**
     * @return the optimizer
     */
    public Optimizer getOptimizer()
    {
        return optimizer;
    }


    /**
     * @param optimizer the optimizer to set
     */
    public void setOptimizer( Optimizer optimizer )
    {
        this.optimizer = optimizer;
    }


    /**
     * @param searchEngine the searchEngine to set
     */
    public void setSearchEngine( SearchEngine searchEngine )
    {
        this.searchEngine = searchEngine;
    }


    /**
     * Set and return the ReadWrite lock we use to protect the backend against concurrent modifications
     * 
     * @param operationContext The OperationContext which contain the reference to the OperationManager
     */
    private void setRWLock( OperationContext operationContext )
    {
        if ( operationContext.getSession() != null )
        {
            rwLock = operationContext.getSession().getDirectoryService().getOperationManager().getRWLock();
        }
        else
        {
            if ( rwLock == null )
            {
                // Create a ReadWrite lock from scratch
                rwLock = new ReentrantReadWriteLock();
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ReadWriteLock getReadWriteLock()
    {
        return rwLock;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public Cache<String, Dn> getAliasCache()
    {
        return aliasCache;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getContextCsn( PartitionTxn partitionTxn )
    {
        if ( super.getContextCsn( partitionTxn ) == null )
        {
           loadContextCsn( partitionTxn ); 
        }
        
        return super.getContextCsn( partitionTxn );
    }


    /**
     * Loads the current context CSN present in the context entry of the partition
     *
     * @param partitionTxn The transaction to use
     */
    protected void loadContextCsn( PartitionTxn partitionTxn )
    {
        try
        {
            if ( rwLock == null )
            {
                // Create a ReadWrite lock from scratch
                rwLock = new ReentrantReadWriteLock();
            }

            // load the last stored valid CSN value
            String contextEntryId = getEntryId( partitionTxn, getSuffixDn() );
            
            if ( contextEntryId == null )
            {
                return;
            }
            
            Entry entry = fetch( partitionTxn, contextEntryId );
            
            Attribute ctxCsnAt = entry.get( contextCsnAT );
            
            if ( ctxCsnAt != null )
            {
                setContextCsn( ctxCsnAt.getString() );
                ctxCsnChanged = false; // this is just loaded, not new
            }
        }
        catch ( LdapException e )
        {
            throw new RuntimeException( e );
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    // store the contextCSN value in the context entry 
    // note that this modification shouldn't change the entryCSN value of the context entry
    @Override
    public void saveContextCsn( PartitionTxn partitionTxn ) throws LdapException
    {
        if ( !ctxCsnChanged )
        {
            return;
        }
        
        String contextCsn = super.getContextCsn( partitionTxn );
        
        if ( contextCsn == null )
        {
            return;
        }
        
        try
        {
            // we don't need to use the ctxCsnSemaphore here cause
            // the only other place this is called is from PartitionNexus.sync()
            // but that is protected by write lock in DefaultDirectoryService.shutdown()
            
            String contextEntryId = getEntryId( partitionTxn, getSuffixDn() );
            Entry origEntry = fetch( partitionTxn, contextEntryId );
            
            // The Context Entry may have been deleted. Get out if we don't find it
            if ( origEntry == null )
            {
                return;
            }

            origEntry = ( ( ClonedServerEntry ) origEntry ).getOriginalEntry();
            
            origEntry.removeAttributes( contextCsnAT, entryDnAT );
            
            origEntry.add( contextCsnAT, contextCsn );
            
            master.put( partitionTxn, contextEntryId, origEntry );
            
            ctxCsnChanged = false;
            
            LOG.debug( "Saved context CSN {} for the partition {}", contextCsn, suffixDn );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Subordinates getSubordinates( PartitionTxn partitionTxn, Entry entry ) throws LdapException
    {
        Subordinates subordinates = new Subordinates();
        
        try
        {
            // Check into the Rdn index, starting with the partition Suffix
            try
            {
                rwLock.readLock().lock();
                ParentIdAndRdn parentIdAndRdn = rdnIdx.reverseLookup( partitionTxn, entry.get( SchemaConstants.ENTRY_UUID_AT ).getString() );

                subordinates.setNbChildren( parentIdAndRdn.getNbChildren() );
                subordinates.setNbSubordinates( parentIdAndRdn.getNbDescendants() );
            }
            finally
            {
                rwLock.readLock().unlock();
            }
        }
        catch ( Exception e )
        {
            throw new LdapException( e.getMessage(), e );
        }

        return subordinates;
    }
}
