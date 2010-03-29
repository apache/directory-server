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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdbm.RecordManager;
import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.MultiException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapAliasDereferencingException;
import org.apache.directory.shared.ldap.exception.LdapAliasException;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.AVA;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.util.NamespaceTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JdbmStore<E> implements Store<E, Long>
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( JdbmStore.class );

    /** The default cache size is set to 10 000 objects */
    static final int DEFAULT_CACHE_SIZE = 10000;

    /** the JDBM record manager used by this database */
    private RecordManager recMan;

    /** the normalized suffix DN of this backend database */
    private DN normSuffix;

    /** the user provided suffix DN of this backend database */
    private DN upSuffix;

    /** the working directory to use for files */
    private File workingDirectory;

    /** the master table storing entries by primary key */
    private JdbmMasterTable<ServerEntry> master;

    /** a map of attributeType numeric ID to user userIndices */
    private Map<String, Index<?, E, Long>> userIndices = new HashMap<String, Index<?, E, Long>>();

    /** a map of attributeType numeric ID to system userIndices */
    private Map<String, Index<?, E, Long>> systemIndices = new HashMap<String, Index<?, E, Long>>();

    /** true if initialized */
    private boolean initialized;

    /** true if we sync disks on every write operation */
    private boolean isSyncOnWrite = true;

    /** the normalized distinguished name index */
    private JdbmIndex<String, E> ndnIdx;

    /** the user provided distinguished name index */
    private JdbmIndex<String, E> updnIdx;

    /** the attribute presence index */
    private JdbmIndex<String, E> presenceIdx;

    /** a system index on aliasedObjectName attribute */
    private JdbmIndex<String, E> aliasIdx;

    /** a system index on the entries of descendants of root DN*/
    private JdbmIndex<Long, E> subLevelIdx;

    /** the parent child relationship index */
    private JdbmIndex<Long, E> oneLevelIdx;

    /** the one level scope alias index */
    private JdbmIndex<Long, E> oneAliasIdx;

    /** the subtree scope alias index */
    private JdbmIndex<Long, E> subAliasIdx;

    /** a system index on objectClass attribute*/
    private JdbmIndex<String, E> objectClassIdx;

    /** a system index on entryCSN attribute */
    private JdbmIndex<String, E> entryCsnIdx;

    /** a system index on entryUUID attribute */
    private JdbmIndex<String, E> entryUuidIdx;

    /** Static declarations to avoid lookup all over the code */
    private static AttributeType OBJECT_CLASS_AT;
    private static AttributeType ENTRY_CSN_AT;
    private static AttributeType ENTRY_UUID_AT;
    private static AttributeType ALIASED_OBJECT_NAME_AT;

    /** A pointer on the schemaManager */
    private SchemaManager schemaManager;

    private String suffixDn;
    private int cacheSize = DEFAULT_CACHE_SIZE;
    private String name;


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
    private void protect( String property )
    {
        if ( initialized )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_576, property ) );
        }
    }


    public void setWorkingDirectory( File workingDirectory )
    {
        protect( "workingDirectory" );
        this.workingDirectory = workingDirectory;
    }


    public File getWorkingDirectory()
    {
        return workingDirectory;
    }


    public void setSuffixDn( String suffixDn )
    {
        protect( "suffixDn" );
        this.suffixDn = suffixDn;
    }


    public String getSuffixDn()
    {
        return suffixDn;
    }


    public void setSyncOnWrite( boolean isSyncOnWrite )
    {
        protect( "syncOnWrite" );
        this.isSyncOnWrite = isSyncOnWrite;
    }


    public boolean isSyncOnWrite()
    {
        return isSyncOnWrite;
    }


    public void setCacheSize( int cacheSize )
    {
        protect( "cacheSize" );
        this.cacheSize = cacheSize;
    }


    public int getCacheSize()
    {
        return cacheSize;
    }


    public void setName( String name )
    {
        protect( "name" );
        this.name = name;
    }


    public String getName()
    {
        return name;
    }


    // -----------------------------------------------------------------------
    // E N D   C O N F I G U R A T I O N   M E T H O D S
    // -----------------------------------------------------------------------

    public Long getDefaultId()
    {
        return 1L;
    };


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

        this.upSuffix = new DN( suffixDn );
        this.normSuffix = DN.normalize( upSuffix, schemaManager.getNormalizerMapping() );
        workingDirectory.mkdirs();

        // First, check if the file storing the data exists
        String path = workingDirectory.getPath() + File.separator + "master";
        BaseRecordManager base = new BaseRecordManager( path );
        base.disableTransactions();

        if ( cacheSize < 0 )
        {
            cacheSize = DEFAULT_CACHE_SIZE;
            LOG.debug( "Using the default entry cache size of {} for {} partition", cacheSize, name );
        }
        else
        {
            LOG.debug( "Using the custom configured cache size of {} for {} partition", cacheSize, name );
        }

        // Now, create the entry cache for this partition
        recMan = new CacheRecordManager( base, new MRU( cacheSize ) );

        // Create the master table (the table containing all the entries)
        master = new JdbmMasterTable<ServerEntry>( recMan, schemaManager );

        // -------------------------------------------------------------------
        // Initializes the user and system indices
        // -------------------------------------------------------------------

        setupSystemIndices();
        setupUserIndices();

        // We are done !
        initialized = true;
    }


    @SuppressWarnings("unchecked")
    private void setupSystemIndices() throws Exception
    {
        if ( systemIndices.size() > 0 )
        {
            HashMap<String, Index<?, E, Long>> tmp = new HashMap<String, Index<?, E, Long>>();

            for ( Index<?, E, Long> index : systemIndices.values() )
            {
                String oid = schemaManager.getAttributeTypeRegistry().getOidByName( index.getAttributeId() );
                tmp.put( oid, index );
                ( ( JdbmIndex ) index ).init( schemaManager, schemaManager.lookupAttributeTypeRegistry( oid ),
                    workingDirectory );
            }
            systemIndices = tmp;
        }

        if ( ndnIdx == null )
        {
            ndnIdx = new JdbmIndex<String, E>();
            ndnIdx.setAttributeId( ApacheSchemaConstants.APACHE_N_DN_AT_OID );
            systemIndices.put( ApacheSchemaConstants.APACHE_N_DN_AT_OID, ndnIdx );
            ndnIdx.init( schemaManager, schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_N_DN_AT_OID ), workingDirectory );
        }

        if ( updnIdx == null )
        {
            updnIdx = new JdbmIndex<String, E>();
            updnIdx.setAttributeId( ApacheSchemaConstants.APACHE_UP_DN_AT_OID );
            systemIndices.put( ApacheSchemaConstants.APACHE_UP_DN_AT_OID, updnIdx );
            updnIdx.init( schemaManager, schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_UP_DN_AT_OID ), workingDirectory );
        }

        if ( presenceIdx == null )
        {
            presenceIdx = new JdbmIndex<String, E>();
            presenceIdx.setAttributeId( ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID );
            systemIndices.put( ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID, presenceIdx );
            presenceIdx.init( schemaManager, schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID ), workingDirectory );
        }

        if ( oneLevelIdx == null )
        {
            oneLevelIdx = new JdbmIndex<Long, E>();
            oneLevelIdx.setAttributeId( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
            systemIndices.put( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID, oneLevelIdx );
            oneLevelIdx.init( schemaManager, schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID ), workingDirectory );
        }

        if ( oneAliasIdx == null )
        {
            oneAliasIdx = new JdbmIndex<Long, E>();
            oneAliasIdx.setAttributeId( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
            systemIndices.put( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID, oneAliasIdx );
            oneAliasIdx.init( schemaManager, schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID ), workingDirectory );
        }

        if ( subAliasIdx == null )
        {
            subAliasIdx = new JdbmIndex<Long, E>();
            subAliasIdx.setAttributeId( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
            systemIndices.put( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID, subAliasIdx );
            subAliasIdx.init( schemaManager, schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID ), workingDirectory );
        }

        if ( aliasIdx == null )
        {
            aliasIdx = new JdbmIndex<String, E>();
            aliasIdx.setAttributeId( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
            systemIndices.put( ApacheSchemaConstants.APACHE_ALIAS_AT_OID, aliasIdx );
            aliasIdx.init( schemaManager, schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_ALIAS_AT_OID ), workingDirectory );
        }

        if ( subLevelIdx == null )
        {
            subLevelIdx = new JdbmIndex<Long, E>();
            subLevelIdx.setAttributeId( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );
            systemIndices.put( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID, subLevelIdx );
            subLevelIdx.init( schemaManager, schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID ), workingDirectory );
        }

        if ( entryCsnIdx == null )
        {
            entryCsnIdx = new JdbmIndex<String, E>();
            entryCsnIdx.setAttributeId( SchemaConstants.ENTRY_CSN_AT_OID );
            systemIndices.put( SchemaConstants.ENTRY_CSN_AT_OID, entryCsnIdx );
            entryCsnIdx.init( schemaManager, schemaManager
                .lookupAttributeTypeRegistry( SchemaConstants.ENTRY_CSN_AT_OID ), workingDirectory );
        }

        if ( entryUuidIdx == null )
        {
            entryUuidIdx = new JdbmIndex<String, E>();
            entryUuidIdx.setAttributeId( SchemaConstants.ENTRY_UUID_AT_OID );
            systemIndices.put( SchemaConstants.ENTRY_UUID_AT_OID, entryUuidIdx );
            entryUuidIdx.init( schemaManager, schemaManager
                .lookupAttributeTypeRegistry( SchemaConstants.ENTRY_UUID_AT_OID ), workingDirectory );
        }

        if ( objectClassIdx == null )
        {
            objectClassIdx = new JdbmIndex<String, E>();
            objectClassIdx.setAttributeId( SchemaConstants.OBJECT_CLASS_AT_OID );
            systemIndices.put( SchemaConstants.OBJECT_CLASS_AT_OID, objectClassIdx );
            objectClassIdx.init( schemaManager, schemaManager
                .lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT_OID ), workingDirectory );
        }
    }


    @SuppressWarnings("unchecked")
    private void setupUserIndices() throws Exception
    {
        if ( ( userIndices != null ) && ( userIndices.size() > 0 ) )
        {
            Map<String, Index<?, E, Long>> tmp = new HashMap<String, Index<?, E, Long>>();

            for ( Index<?, E, Long> index : userIndices.values() )
            {
                String oid = schemaManager.getAttributeTypeRegistry().getOidByName( index.getAttributeId() );

                if ( systemIndices.containsKey( oid ) )
                {
                    // Bypass some specific index for AttributeTypes like ObjectClass hich are already
                    // present in the SystemIndices
                    continue;
                }
                AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( oid );

                // Check that the attributeType has an EQUALITY matchingRule
                MatchingRule mr = attributeType.getEquality();

                if ( mr != null )
                {
                    ( ( JdbmIndex ) index ).init( schemaManager, schemaManager.lookupAttributeTypeRegistry( oid ),
                        workingDirectory );
                    tmp.put( oid, index );
                }
                else
                {
                    LOG.error( I18n.err( I18n.ERR_4, attributeType.getName() ) );
                }
            }

            userIndices = tmp;
        }
        else
        {
            userIndices = new HashMap<String, Index<?, E, Long>>();
        }
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
     * Gets whether the store is initialized.
     *
     * @return true if the partition store is initialized
     */
    public boolean isInitialized()
    {
        return initialized;
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
        array.add( ndnIdx );
        array.add( updnIdx );
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

        master.sync();
        recMan.commit();
    }


    // ------------------------------------------------------------------------
    // I N D E X   M E T H O D S
    // ------------------------------------------------------------------------

    private <K> JdbmIndex<K, E> convertIndex( Index<K, E, Long> index )
    {
        if ( index instanceof JdbmIndex<?, ?> )
        {
            return ( JdbmIndex<K, E> ) index;
        }

        LOG.warn( "Supplied index {} is not a JdbmIndex.  "
            + "Will create new JdbmIndex using copied configuration parameters.", index );
        JdbmIndex<K, E> jdbmIndex = new JdbmIndex<K, E>( index.getAttributeId() );
        jdbmIndex.setCacheSize( index.getCacheSize() );
        jdbmIndex.setNumDupLimit( JdbmIndex.DEFAULT_DUPLICATE_LIMIT );
        jdbmIndex.setWkDirPath( index.getWkDirPath() );
        return jdbmIndex;
    }


    public void setUserIndices( Set<Index<?, E, Long>> userIndices )
    {
        protect( "userIndices" );
        for ( Index<?, E, Long> index : userIndices )
        {
            this.userIndices.put( index.getAttributeId(), convertIndex( index ) );
        }
    }


    public Set<Index<?, E, Long>> getUserIndices()
    {
        return new HashSet<Index<?, E, Long>>( userIndices.values() );
    }


    public void addIndex( Index<?, E, Long> index ) throws Exception
    {
        userIndices.put( index.getAttributeId(), convertIndex( index ) );
    }


    //------------------------------------------------------------------------
    // System index
    //------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public Index<String, E, Long> getPresenceIndex()
    {
        return presenceIdx;
    }


    /**
     * {@inheritDoc}
     */
    public void setPresenceIndex( Index<String, E, Long> index ) throws Exception
    {
        protect( "presenceIndex" );
        presenceIdx = convertIndex( index );
        systemIndices.put( index.getAttributeId(), presenceIdx );
    }


    /**
     * {@inheritDoc}
     */
    public Index<Long, E, Long> getOneLevelIndex()
    {
        return oneLevelIdx;
    }


    /**
     * {@inheritDoc}
     */
    public void setOneLevelIndex( Index<Long, E, Long> index ) throws Exception
    {
        protect( "hierarchyIndex" );
        oneLevelIdx = convertIndex( index );
        systemIndices.put( index.getAttributeId(), oneLevelIdx );
    }


    /**
     * {@inheritDoc}
     */
    public Index<String, E, Long> getAliasIndex()
    {
        return aliasIdx;
    }


    /**
     * {@inheritDoc}
     */
    public void setAliasIndex( Index<String, E, Long> index ) throws LdapException
    {
        protect( "aliasIndex" );
        aliasIdx = convertIndex( index );
        systemIndices.put( index.getAttributeId(), aliasIdx );
    }


    /**
     * {@inheritDoc}
     */
    public Index<Long, E, Long> getOneAliasIndex()
    {
        return oneAliasIdx;
    }


    /**
     * {@inheritDoc}
     */
    public void setOneAliasIndex( Index<Long, E, Long> index ) throws LdapException
    {
        protect( "oneAliasIndex" );
        oneAliasIdx = convertIndex( index );
        systemIndices.put( index.getAttributeId(), oneAliasIdx );
    }


    /**
     * {@inheritDoc}
     */
    public Index<Long, E, Long> getSubAliasIndex()
    {
        return subAliasIdx;
    }


    /**
     * {@inheritDoc}
     */
    public void setSubAliasIndex( Index<Long, E, Long> index ) throws LdapException
    {
        protect( "subAliasIndex" );
        subAliasIdx = convertIndex( index );
        systemIndices.put( index.getAttributeId(), subAliasIdx );
    }


    /**
     * {@inheritDoc}
     */
    public Index<String, E, Long> getUpdnIndex()
    {
        return updnIdx;
    }


    /**
     * {@inheritDoc}
     */
    public void setUpdnIndex( Index<String, E, Long> index ) throws LdapException
    {
        protect( "updnIndex" );
        updnIdx = convertIndex( index );
        systemIndices.put( index.getAttributeId(), updnIdx );
    }


    /**
     * {@inheritDoc}
     */
    public Index<String, E, Long> getNdnIndex()
    {
        return ndnIdx;
    }


    /**
     * {@inheritDoc}
     */
    public void setNdnIndex( Index<String, E, Long> index ) throws LdapException
    {
        protect( "ndnIndex" );
        ndnIdx = convertIndex( index );
        systemIndices.put( index.getAttributeId(), ndnIdx );
    }


    /**
     * {@inheritDoc}
     */
    public Index<Long, E, Long> getSubLevelIndex()
    {
        return subLevelIdx;
    }


    /**
     * {@inheritDoc}
     */
    public void setSubLevelIndex( Index<Long, E, Long> index ) throws LdapException
    {
        protect( "subLevelIndex" );
        subLevelIdx = convertIndex( index );
        systemIndices.put( index.getAttributeId(), subLevelIdx );
    }


    /**
     * {@inheritDoc}
     */
    public Index<String, E, Long> getObjectClassIndex()
    {
        return objectClassIdx;
    }


    /**
     * {@inheritDoc}
     */
    public void setObjectClassIndex( Index<String, E, Long> index ) throws LdapException
    {
        protect( "objectClassIndex" );
        objectClassIdx = convertIndex( index );
        systemIndices.put( index.getAttributeId(), objectClassIdx );
    }


    /**
     * {@inheritDoc}
     */
    public Index<String, E, Long> getEntryUuidIndex()
    {
        return entryUuidIdx;
    }


    /**
     * {@inheritDoc}
     */
    public void setEntryUuidIndex( Index<String, E, Long> index ) throws LdapException
    {
        protect( "entryUuidIndex" );
        entryUuidIdx = convertIndex( index );
        systemIndices.put( index.getAttributeId(), entryUuidIdx );
    }


    /**
     * {@inheritDoc}
     */
    public Index<String, E, Long> getEntryCsnIndex()
    {
        return entryCsnIdx;
    }


    /**
     * {@inheritDoc}
     */
    public void setEntryCsnIndex( Index<String, E, Long> index ) throws LdapException
    {
        protect( "entryCsnIndex" );
        entryCsnIdx = convertIndex( index );
        systemIndices.put( index.getAttributeId(), entryCsnIdx );
    }


    public Iterator<String> userIndices()
    {
        return userIndices.keySet().iterator();
    }


    public Iterator<String> systemIndices()
    {
        return systemIndices.keySet().iterator();
    }


    public boolean hasIndexOn( String id ) throws LdapException
    {
        return hasUserIndexOn( id ) || hasSystemIndexOn( id );
    }


    public boolean hasUserIndexOn( String id ) throws LdapException
    {
        return userIndices.containsKey( schemaManager.getAttributeTypeRegistry().getOidByName( id ) );
    }


    public boolean hasSystemIndexOn( String id ) throws LdapException
    {
        return systemIndices.containsKey( schemaManager.getAttributeTypeRegistry().getOidByName( id ) );
    }


    public Index<?, E, Long> getIndex( String id ) throws IndexNotFoundException
    {
        try
        {
            id = schemaManager.getAttributeTypeRegistry().getOidByName( id );
        }
        catch ( LdapException e )
        {
            String msg = I18n.err( I18n.ERR_128, id );
            LOG.error( msg, e );
            throw new IndexNotFoundException( msg, id, e );
        }

        if ( userIndices.containsKey( id ) )
        {
            return userIndices.get( id );
        }
        if ( systemIndices.containsKey( id ) )
        {
            return systemIndices.get( id );
        }

        throw new IndexNotFoundException( I18n.err( I18n.ERR_3, id, name ) );
    }


    public Index<?, E, Long> getUserIndex( String id ) throws IndexNotFoundException
    {
        try
        {
            id = schemaManager.getAttributeTypeRegistry().getOidByName( id );
        }
        catch ( LdapException e )
        {
            String msg = I18n.err( I18n.ERR_128, id );
            LOG.error( msg, e );
            throw new IndexNotFoundException( msg, id, e );
        }

        if ( userIndices.containsKey( id ) )
        {
            return userIndices.get( id );
        }

        throw new IndexNotFoundException( I18n.err( I18n.ERR_3, id, name ) );
    }


    public Index<?, E, Long> getSystemIndex( String id ) throws IndexNotFoundException
    {
        try
        {
            id = schemaManager.getAttributeTypeRegistry().getOidByName( id );
        }
        catch ( LdapException e )
        {
            String msg = I18n.err( I18n.ERR_128, id );
            LOG.error( msg, e );
            throw new IndexNotFoundException( msg, id, e );
        }

        if ( systemIndices.containsKey( id ) )
        {
            return systemIndices.get( id );
        }

        throw new IndexNotFoundException( I18n.err( I18n.ERR_2, id, name ) );
    }


    public Long getEntryId( String dn ) throws Exception
    {
        return ndnIdx.forwardLookup( dn );
    }


    public String getEntryDn( Long id ) throws Exception
    {
        return ndnIdx.reverseLookup( id );
    }


    /**
     * Gets the Long id of an entry's parent using the child entry's
     * normalized DN. Note that the suffix entry returns 0, which does not
     * map to any entry.
     *
     * @param dn the normalized distinguished name of the child
     * @return the id of the parent entry or zero if the suffix entry the
     * normalized suffix DN string is used
     * @throws Exception on failures to access the underlying store
     */
    public Long getParentId( String dn ) throws Exception
    {
        Long childId = ndnIdx.forwardLookup( dn );
        return oneLevelIdx.reverseLookup( childId );
    }


    public Long getParentId( Long childId ) throws Exception
    {
        return oneLevelIdx.reverseLookup( childId );
    }


    public String getEntryUpdn( Long id ) throws Exception
    {
        return updnIdx.reverseLookup( id );
    }


    public String getEntryUpdn( String dn ) throws Exception
    {
        Long id = ndnIdx.forwardLookup( dn );
        return updnIdx.reverseLookup( id );
    }


    public int count() throws Exception
    {
        return master.count();
    }


    /**
     * Removes the index entries for an alias before the entry is deleted from
     * the master table.
     * 
     * @todo Optimize this by walking the hierarchy index instead of the name 
     * @param aliasId the id of the alias entry in the master table
     * @throws LdapException if we cannot parse ldap names
     * @throws Exception if we cannot delete index values in the database
     */
    private void dropAliasIndices( Long aliasId ) throws Exception
    {
        String targetDn = aliasIdx.reverseLookup( aliasId );
        Long targetId = getEntryId( targetDn );
        String aliasDn = getEntryDn( aliasId );
        DN aliasDN = ( DN ) new DN( aliasDn );

        DN ancestorDn = ( DN ) aliasDN.clone();
        ancestorDn.remove( aliasDN.size() - 1 );
        Long ancestorId = getEntryId( ancestorDn.getNormName() );

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
        oneAliasIdx.drop( ancestorId, targetId );
        subAliasIdx.drop( ancestorId, targetId );

        while ( !ancestorDn.equals( normSuffix ) && ancestorDn.size() > normSuffix.size() )
        {
            ancestorDn = ( DN ) ancestorDn.getPrefix( ancestorDn.size() - 1 );
            ancestorId = getEntryId( ancestorDn.getNormName() );

            subAliasIdx.drop( ancestorId, targetId );
        }

        // Drops all alias tuples pointing to the id of the alias to be deleted
        aliasIdx.drop( aliasId );
    }


    /**
     * Adds userIndices for an aliasEntry to be added to the database while checking
     * for constrained alias constructs like alias cycles and chaining.
     * 
     * @param aliasDn normalized distinguished name for the alias entry
     * @param aliasTarget the user provided aliased entry dn as a string
     * @param aliasId the id of alias entry to add
     * @throws LdapException if index addition fails, and if the alias is
     * not allowed due to chaining or cycle formation.
     * @throws Exception if the wrappedCursor btrees cannot be altered
     */
    private void addAliasIndices( Long aliasId, DN aliasDn, String aliasTarget ) throws Exception
    {
        DN normalizedAliasTargetDn; // Name value of aliasedObjectName
        Long targetId; // Id of the aliasedObjectName
        DN ancestorDn; // Name of an alias entry relative
        Long ancestorId; // Id of an alias entry relative

        // Access aliasedObjectName, normalize it and generate the Name 
        normalizedAliasTargetDn = new DN( aliasTarget );
        normalizedAliasTargetDn.normalize( schemaManager.getNormalizerMapping() );

        /*
         * Check For Cycles
         * 
         * Before wasting time to lookup more values we check using the target
         * dn to see if we have the possible formation of an alias cycle.  This
         * happens when the alias refers back to a target that is also a 
         * relative of the alias entry.  For detection we test if the aliased
         * entry Dn starts with the target Dn.  If it does then we know the 
         * aliased target is a relative and we have a perspecitive cycle.
         */
        if ( aliasDn.isChildOf( normalizedAliasTargetDn ) )
        {
            if ( aliasDn.equals( normalizedAliasTargetDn ) )
            {
                String msg = I18n.err( I18n.ERR_223 );
                LdapAliasDereferencingException e = new LdapAliasDereferencingException( msg );
                //e.setResolvedName( aliasDn );
                throw e;
            }

            String msg = I18n.err( I18n.ERR_224, aliasTarget, aliasDn );
            LdapAliasDereferencingException e = new LdapAliasDereferencingException( msg );
            //e.setResolvedName( aliasDn );
            throw e;
        }

        /*
         * Check For Aliases External To Naming Context
         * 
         * id may be null but the alias may be to a valid entry in 
         * another namingContext.  Such aliases are not allowed and we
         * need to point it out to the user instead of saying the target
         * does not exist when it potentially could outside of this upSuffix.
         */
        if ( !normalizedAliasTargetDn.isChildOf( normSuffix ) )
        {
            String msg = I18n.err( I18n.ERR_225, upSuffix.getName() );
            LdapAliasDereferencingException e = new LdapAliasDereferencingException( msg );
            //e.setResolvedName( aliasDn );
            throw e;
        }

        // L O O K U P   T A R G E T   I D
        targetId = ndnIdx.forwardLookup( normalizedAliasTargetDn.getNormName() );

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
            LdapAliasException e = new LdapAliasException( msg );
            //e.setResolvedName( aliasDn );
            throw e;
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
        if ( null != aliasIdx.reverseLookup( targetId ) )
        {
            String msg = I18n.err( I18n.ERR_227 );
            LdapAliasDereferencingException e = new LdapAliasDereferencingException( msg );
            //e.setResolvedName( aliasDn );
            throw e;
        }

        // Add the alias to the simple alias index
        aliasIdx.add( normalizedAliasTargetDn.getNormName(), aliasId );

        /*
         * Handle One Level Scope Alias Index
         * 
         * The first relative is special with respect to the one level alias
         * index.  If the target is not a sibling of the alias then we add the
         * index entry maping the parent's id to the aliased target id.
         */
        ancestorDn = ( DN ) aliasDn.clone();
        ancestorDn.remove( aliasDn.size() - 1 );
        ancestorId = getEntryId( ancestorDn.getNormName() );

        // check if alias parent and aliased entry are the same
        DN normalizedAliasTargetParentDn = ( DN ) normalizedAliasTargetDn.clone();
        normalizedAliasTargetParentDn.remove( normalizedAliasTargetDn.size() - 1 );
        if ( !aliasDn.isChildOf( normalizedAliasTargetParentDn ) )
        {
            oneAliasIdx.add( ancestorId, targetId );
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
        while ( !ancestorDn.equals( normSuffix ) && null != ancestorId )
        {
            if ( !NamespaceTools.isDescendant( ancestorDn, normalizedAliasTargetDn ) )
            {
                subAliasIdx.add( ancestorId, targetId );
            }

            ancestorDn.remove( ancestorDn.size() - 1 );
            ancestorId = getEntryId( ancestorDn.getNormName() );
        }
    }


    /**
     * {@inheritDoc}
     * TODO : We should be able to revert all the changes made to index 
     * if something went wrong. Also the index should auto-repair : if
     * an entry does not exist in the Master table, then the index must be updated to reflect this.
     */
    @SuppressWarnings("unchecked")
    public synchronized void add( ServerEntry entry ) throws Exception
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        Long parentId;
        Long id = master.getNextId();

        //
        // Suffix entry cannot have a parent since it is the root so it is 
        // capped off using the zero value which no entry can have since 
        // entry sequences start at 1.
        //
        DN entryDn = entry.getDn();
        DN parentDn = null;

        if ( entryDn.getNormName().equals( normSuffix.getNormName() ) )
        {
            parentId = 0L;
        }
        else
        {
            parentDn = ( DN ) entryDn.clone();
            parentDn.remove( parentDn.size() - 1 );
            parentId = getEntryId( parentDn.getNormName() );
        }

        // don't keep going if we cannot find the parent Id
        if ( parentId == null )
        {
            throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_216, parentDn ) );
        }

        EntryAttribute objectClass = entry.get( OBJECT_CLASS_AT );

        if ( objectClass == null )
        {
            String msg = I18n.err( I18n.ERR_217, entryDn.getName(), entry );
            ResultCodeEnum rc = ResultCodeEnum.OBJECT_CLASS_VIOLATION;
            LdapSchemaViolationException e = new LdapSchemaViolationException( rc, msg );
            //e.setResolvedName( entryDn );
            throw e;
        }

        // Start adding the system userIndices
        // Why bother doing a lookup if this is not an alias.
        // First, the ObjectClass index
        for ( Value<?> value : objectClass )
        {
            objectClassIdx.add( value.getString(), id );
        }

        if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
        {
            EntryAttribute aliasAttr = entry.get( ALIASED_OBJECT_NAME_AT );
            addAliasIndices( id, entryDn, aliasAttr.getString() );
        }

        if ( !Character.isDigit( entryDn.getNormName().charAt( 0 ) ) )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_218, entryDn.getNormName() ) );
        }

        ndnIdx.add( entryDn.getNormName(), id );
        updnIdx.add( entryDn.getName(), id );
        oneLevelIdx.add( parentId, id );

        // Update the EntryCsn index
        EntryAttribute entryCsn = entry.get( ENTRY_CSN_AT );

        if ( entryCsn == null )
        {
            String msg = I18n.err( I18n.ERR_219, entryDn.getName(), entry );
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
        }

        entryCsnIdx.add( entryCsn.getString(), id );

        // Update the EntryUuid index
        EntryAttribute entryUuid = entry.get( ENTRY_UUID_AT );

        if ( entryUuid == null )
        {
            String msg = I18n.err( I18n.ERR_220, entryDn.getName(), entry );
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
        }

        entryUuidIdx.add( entryUuid.getString(), id );

        Long tempId = parentId;

        while ( ( tempId != null ) && ( tempId != 0 ) && ( tempId != 1 ) )
        {
            subLevelIdx.add( tempId, id );
            tempId = getParentId( tempId );
        }

        // making entry an ancestor/descendent of itself in sublevel index
        subLevelIdx.add( id, id );

        // Now work on the user defined userIndices
        for ( EntryAttribute attribute : entry )
        {
            String attributeOid = attribute.getAttributeType().getOid();

            if ( hasUserIndexOn( attributeOid ) )
            {
                Index<Object, E, Long> idx = ( Index<Object, E, Long> ) getUserIndex( attributeOid );

                // here lookup by attributeId is OK since we got attributeId from 
                // the entry via the enumeration - it's in there as is for sure

                for ( Value<?> value : attribute )
                {
                    idx.add( value.get(), id );
                }

                // Adds only those attributes that are indexed
                presenceIdx.add( attributeOid, id );
            }
        }

        master.put( id, entry );

        if ( isSyncOnWrite )
        {
            sync();
        }
    }


    public ServerEntry lookup( Long id ) throws Exception
    {
        return ( ServerEntry ) master.get( id );
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void delete( Long id ) throws Exception
    {
        ServerEntry entry = lookup( id );
        Long parentId = getParentId( id );

        EntryAttribute objectClass = entry.get( OBJECT_CLASS_AT );

        if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
        {
            dropAliasIndices( id );
        }

        for ( Value<?> value : objectClass )
        {
            objectClassIdx.drop( value.getString(), id );
        }

        ndnIdx.drop( id );
        updnIdx.drop( id );
        oneLevelIdx.drop( id );
        entryCsnIdx.drop( id );
        entryUuidIdx.drop( id );

        if ( id != 1 )
        {
            subLevelIdx.drop( id );
        }

        // Remove parent's reference to entry only if entry is not the upSuffix
        if ( !parentId.equals( 0L ) )
        {
            oneLevelIdx.drop( parentId, id );
        }

        for ( EntryAttribute attribute : entry )
        {
            String attributeOid = attribute.getAttributeType().getOid();

            if ( hasUserIndexOn( attributeOid ) )
            {
                Index<?, E, Long> index = getUserIndex( attributeOid );

                // here lookup by attributeId is ok since we got attributeId from 
                // the entry via the enumeration - it's in there as is for sure
                for ( Value<?> value : attribute )
                {
                    ( ( JdbmIndex ) index ).drop( value.get(), id );
                }

                presenceIdx.drop( attributeOid, id );
            }
        }

        master.delete( id );

        if ( isSyncOnWrite )
        {
            sync();
        }
    }


    /**
     * Gets an IndexEntry Cursor over the child nodes of an entry.
     *
     * @param id the id of the parent entry
     * @return an IndexEntry Cursor over the child entries
     * @throws Exception on failures to access the underlying store
     */
    public IndexCursor<Long, E, Long> list( Long id ) throws Exception
    {
        IndexCursor<Long, E, Long> cursor = oneLevelIdx.forwardCursor( id );
        cursor.beforeValue( id, null );
        return cursor;
    }


    public int getChildCount( Long id ) throws Exception
    {
        return oneLevelIdx.count( id );
    }


    public DN getSuffix()
    {
        return normSuffix;
    }


    public DN getUpSuffix()
    {
        return upSuffix;
    }


    public void setProperty( String propertyName, String propertyValue ) throws Exception
    {
        master.setProperty( propertyName, propertyValue );
    }


    public String getProperty( String propertyName ) throws Exception
    {
        return master.getProperty( propertyName );
    }


    /**
     * Adds a set of attribute values while affecting the appropriate userIndices.
     * The entry is not persisted: it is only changed in anticipation for a put 
     * into the master table.
     *
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mods the attribute and values to add 
     * @throws Exception if index alteration or attribute addition fails
     */
    @SuppressWarnings("unchecked")
    private void add( Long id, ServerEntry entry, EntryAttribute mods ) throws Exception
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );

        // Special case for the ObjectClass index
        if ( modsOid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
        {
            for ( Value<?> value : mods )
            {
                objectClassIdx.drop( value.getString(), id );
            }
        }
        else if ( hasUserIndexOn( modsOid ) )
        {
            Index<?, E, Long> index = getUserIndex( modsOid );

            for ( Value<?> value : mods )
            {
                ( ( JdbmIndex ) index ).add( value.get(), id );
            }

            // If the attr didn't exist for this id add it to existence index
            if ( !presenceIdx.forward( modsOid, id ) )
            {
                presenceIdx.add( modsOid, id );
            }
        }

        // add all the values in mods to the same attribute in the entry
        AttributeType type = schemaManager.lookupAttributeTypeRegistry( modsOid );

        for ( Value<?> value : mods )
        {
            entry.add( type, value );
        }

        if ( modsOid.equals( SchemaConstants.ALIASED_OBJECT_NAME_AT_OID ) )
        {
            String ndnStr = ndnIdx.reverseLookup( id );
            addAliasIndices( id, new DN( ndnStr ), mods.getString() );
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
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mods the attribute and its values to delete
     * @throws Exception if index alteration or attribute modification fails.
     */
    @SuppressWarnings("unchecked")
    private void remove( Long id, ServerEntry entry, EntryAttribute mods ) throws Exception
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );

        // Special case for the ObjectClass index
        if ( modsOid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
        {
            for ( Value<?> value : mods )
            {
                objectClassIdx.drop( value.getString(), id );
            }
        }
        else if ( hasUserIndexOn( modsOid ) )
        {
            Index<?, E, Long> index = getUserIndex( modsOid );

            for ( Value<?> value : mods )
            {
                ( ( JdbmIndex ) index ).drop( value.get(), id );
            }

            /* 
             * If no attribute values exist for this entryId in the index then
             * we remove the presence index entry for the removed attribute.
             */
            if ( null == index.reverseLookup( id ) )
            {
                presenceIdx.drop( modsOid, id );
            }
        }

        AttributeType attrType = schemaManager.lookupAttributeTypeRegistry( modsOid );
        /*
         * If there are no attribute values in the modifications then this 
         * implies the compelete removal of the attribute from the entry. Else
         * we remove individual attribute values from the entry in mods one 
         * at a time.
         */
        if ( mods.size() == 0 )
        {
            entry.removeAttributes( attrType );
        }
        else
        {
            EntryAttribute entryAttr = entry.get( attrType );

            for ( Value<?> value : mods )
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
        if ( modsOid.equals( SchemaConstants.ALIASED_OBJECT_NAME_AT_OID ) )
        {
            dropAliasIndices( id );
        }
    }


    /**
     * Completely replaces the existing set of values for an attribute with the
     * modified values supplied affecting the appropriate userIndices.  The entry
     * is not persisted: it is only changed in anticipation for a put into the
     * master table.
     *
     * @param id the primary key of the entry
     * @param entry the entry to alter
     * @param mods the replacement attribute and values
     * @throws Exception if index alteration or attribute modification 
     * fails.
     */
    @SuppressWarnings("unchecked")
    private void replace( Long id, ServerEntry entry, EntryAttribute mods ) throws Exception
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );

        // Special case for the ObjectClass index
        if ( modsOid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
        {
            // if the id exists in the index drop all existing attribute 
            // value index entries and add new ones
            if ( objectClassIdx.reverse( id ) )
            {
                objectClassIdx.drop( id );
            }

            for ( Value<?> value : mods )
            {
                objectClassIdx.add( value.getString(), id );
            }
        }
        else if ( hasUserIndexOn( modsOid ) )
        {
            Index<?, E, Long> index = getUserIndex( modsOid );

            // if the id exists in the index drop all existing attribute 
            // value index entries and add new ones
            if ( index.reverse( id ) )
            {
                ( ( JdbmIndex<?, E> ) index ).drop( id );
            }

            for ( Value<?> value : mods )
            {
                ( ( JdbmIndex<Object, E> ) index ).add( value.get(), id );
            }

            /* 
             * If no attribute values exist for this entryId in the index then
             * we remove the presence index entry for the removed attribute.
             */
            if ( null == index.reverseLookup( id ) )
            {
                presenceIdx.drop( modsOid, id );
            }
        }

        String aliasAttributeOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.ALIASED_OBJECT_NAME_AT );

        if ( modsOid.equals( aliasAttributeOid ) )
        {
            dropAliasIndices( id );
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
            String ndnStr = ndnIdx.reverseLookup( id );
            addAliasIndices( id, new DN( ndnStr ), mods.getString() );
        }
    }


    public void modify( DN dn, ModificationOperation modOp, ServerEntry mods ) throws Exception
    {
        if ( mods instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        Long id = getEntryId( dn.getNormName() );
        ServerEntry entry = ( ServerEntry ) master.get( id );

        for ( AttributeType attributeType : mods.getAttributeTypes() )
        {
            EntryAttribute attr = mods.get( attributeType );

            switch ( modOp )
            {
                case ADD_ATTRIBUTE:
                    add( id, entry, attr );
                    break;

                case REMOVE_ATTRIBUTE:
                    remove( id, entry, attr );
                    break;

                case REPLACE_ATTRIBUTE:
                    replace( id, entry, attr );

                    break;

                default:
                    throw new LdapException( I18n.err( I18n.ERR_221 ) );
            }
        }

        master.put( id, entry );

        if ( isSyncOnWrite )
        {
            sync();
        }
    }


    public void modify( DN dn, List<Modification> mods ) throws Exception
    {
        Long id = getEntryId( dn.getNormName() );
        ServerEntry entry = ( ServerEntry ) master.get( id );

        for ( Modification mod : mods )
        {
            EntryAttribute attrMods = mod.getAttribute();

            switch ( mod.getOperation() )
            {
                case ADD_ATTRIBUTE:
                    add( id, entry, attrMods );
                    break;

                case REMOVE_ATTRIBUTE:
                    remove( id, entry, attrMods );
                    break;

                case REPLACE_ATTRIBUTE:
                    replace( id, entry, attrMods );
                    break;

                default:
                    throw new LdapException( I18n.err( I18n.ERR_221 ) );
            }
        }

        master.put( id, entry );

        if ( isSyncOnWrite )
        {
            sync();
        }
    }


    /**
     * Changes the relative distinguished name of an entry specified by a 
     * distinguished name with the optional removal of the old RDN attribute
     * value from the entry.  Name changes propagate down as dn changes to the 
     * descendants of the entry where the RDN changed. 
     * 
     * An RDN change operation does not change parent child relationships.  It 
     * merely propagates a name change at a point in the DIT where the RDN is 
     * changed. The change propagates down the subtree rooted at the 
     * distinguished name specified.
     *
     * @param dn the normalized distinguished name of the entry to alter
     * @param newRdn the new RDN to set
     * @param deleteOldRdn whether or not to remove the old RDN attr/val
     * @throws Exception if there are any errors propagating the name changes
     */
    @SuppressWarnings("unchecked")
    public void rename( DN dn, RDN newRdn, boolean deleteOldRdn ) throws Exception
    {
        Long id = getEntryId( dn.getNormName() );
        ServerEntry entry = lookup( id );
        DN updn = entry.getDn();

        /* 
         * H A N D L E   N E W   R D N
         * ====================================================================
         * Add the new RDN attribute to the entry.  If an index exists on the 
         * new RDN attribute we add the index for this attribute value pair.
         * Also we make sure that the presence index shows the existence of the
         * new RDN attribute within this entry.
         */

        for ( AVA newAtav : newRdn )
        {
            String newNormType = newAtav.getNormType();
            Object newNormValue = newAtav.getNormValue().get();
            AttributeType newRdnAttrType = schemaManager.lookupAttributeTypeRegistry( newNormType );

            entry.add( newRdnAttrType, newAtav.getUpValue() );

            if ( hasUserIndexOn( newNormType ) )
            {
                Index<?, E, Long> index = getUserIndex( newNormType );
                ( ( JdbmIndex ) index ).add( newNormValue, id );

                // Make sure the altered entry shows the existence of the new attrib
                if ( !presenceIdx.forward( newNormType, id ) )
                {
                    presenceIdx.add( newNormType, id );
                }
            }
        }

        /*
         * H A N D L E   O L D   R D N
         * ====================================================================
         * If the old RDN is to be removed we need to get the attribute and 
         * value for it.  Keep in mind the old RDN need not be based on the 
         * same attr as the new one.  We remove the RDN value from the entry
         * and remove the value/id tuple from the index on the old RDN attr
         * if any.  We also test if the delete of the old RDN index tuple 
         * removed all the attribute values of the old RDN using a reverse
         * lookup.  If so that means we blew away the last value of the old 
         * RDN attribute.  In this case we need to remove the attrName/id 
         * tuple from the presence index.
         * 
         * We only remove an ATAV of the old RDN if it is not included in the
         * new RDN.
         */

        if ( deleteOldRdn )
        {
            RDN oldRdn = updn.getRdn();
            for ( AVA oldAtav : oldRdn )
            {
                // check if the new ATAV is part of the old RDN
                // if that is the case we do not remove the ATAV
                boolean mustRemove = true;
                for ( AVA newAtav : newRdn )
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
                    String oldNormValue = oldAtav.getNormValue().getString();
                    AttributeType oldRdnAttrType = schemaManager.lookupAttributeTypeRegistry( oldNormType );
                    entry.remove( oldRdnAttrType, oldNormValue );

                    if ( hasUserIndexOn( oldNormType ) )
                    {
                        Index<?, E, Long> index = getUserIndex( oldNormType );
                        ( ( JdbmIndex ) index ).drop( oldNormValue, id );

                        /*
                         * If there is no value for id in this index due to our
                         * drop above we remove the oldRdnAttr from the presence idx
                         */
                        if ( null == index.reverseLookup( id ) )
                        {
                            presenceIdx.drop( oldNormType, id );
                        }
                    }
                }
            }
        }

        /*
         * H A N D L E   D N   C H A N G E
         * ====================================================================
         * 1) Build the new user defined distinguished name
         *      - clone / copy old updn
         *      - remove old upRdn from copy
         *      - add the new upRdn to the copy
         * 2) Make call to recursive modifyDn method to change the names of the
         *    entry and its descendants
         */

        DN newUpdn = ( DN ) updn.clone(); // copy da old updn
        newUpdn.remove( newUpdn.size() - 1 ); // remove old upRdn
        newUpdn.add( newRdn.getName() ); // add da new upRdn

        // gotta normalize cuz this thang is cloned and not normalized by default
        newUpdn.normalize( schemaManager.getNormalizerMapping() );

        modifyDn( id, newUpdn, false ); // propagate dn changes

        // Update the current entry
        entry.setDn( newUpdn );
        master.put( id, entry );

        if ( isSyncOnWrite )
        {
            sync();
        }
    }


    /*
     * The move operation severs a child from a parent creating a new parent
     * child relationship.  As a consequence the relationships between the 
     * old ancestors of the child and its descendants change.  A descendant is
     *   
     */

    /**
     * Recursively modifies the distinguished name of an entry and the names of
     * its descendants calling itself in the recursion.
     *
     * @param id the primary key of the entry
     * @param updn User provided distinguished name to set as the new DN
     * @param isMove whether or not the name change is due to a move operation
     * which affects alias userIndices.
     * @throws Exception if something goes wrong
     */
    private void modifyDn( Long id, DN updn, boolean isMove ) throws Exception
    {
        String aliasTarget;

        // update normalized DN index
        ndnIdx.drop( id );

        if ( !updn.isNormalized() )
        {
            updn.normalize( schemaManager.getNormalizerMapping() );
        }

        ndnIdx.add( updn.getNormName(), id );

        // update user provided DN index
        updnIdx.drop( id );
        updnIdx.add( updn.getName(), id );

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
        if ( isMove )
        {
            aliasTarget = aliasIdx.reverseLookup( id );

            if ( null != aliasTarget )
            {
                addAliasIndices( id, new DN( getEntryDn( id ) ), aliasTarget );
            }
        }

        Cursor<IndexEntry<Long, E, Long>> children = list( id );

        while ( children.next() )
        {
            // Get the child and its id
            IndexEntry<Long, E, Long> rec = children.get();
            Long childId = rec.getId();

            /* 
             * Calculate the DN for the child's new name by copying the parents
             * new name and adding the child's old upRdn to new name as its RDN
             */
            DN childUpdn = ( DN ) updn.clone();
            DN oldUpdn = new DN( getEntryUpdn( childId ) );

            String rdn = oldUpdn.get( oldUpdn.size() - 1 );
            DN rdnDN = new DN( rdn );
            rdnDN.normalize( schemaManager.getNormalizerMapping() );
            childUpdn.add( rdnDN.getRdn() );

            // Modify the child
            ServerEntry entry = lookup( childId );
            entry.setDn( childUpdn );
            master.put( childId, entry );

            // Recursively change the names of the children below
            modifyDn( childId, childUpdn, isMove );
        }

        children.close();
    }


    public void move( DN oldChildDn, DN newParentDn, RDN newRdn, boolean deleteOldRdn ) throws Exception
    {
        Long childId = getEntryId( oldChildDn.getNormName() );
        rename( oldChildDn, newRdn, deleteOldRdn );
        DN newUpdn = move( oldChildDn, childId, newParentDn );

        // Update the current entry
        ServerEntry entry = lookup( childId );
        entry.setDn( newUpdn );
        master.put( childId, entry );

        if ( isSyncOnWrite )
        {
            sync();
        }
    }


    public void move( DN oldChildDn, DN newParentDn ) throws Exception
    {
        Long childId = getEntryId( oldChildDn.getNormName() );
        DN newUpdn = move( oldChildDn, childId, newParentDn );

        // Update the current entry
        ServerEntry entry = lookup( childId );
        entry.setDn( newUpdn );
        master.put( childId, entry );

        if ( isSyncOnWrite )
        {
            sync();
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
     * @param oldChildDn the normalized dn of the child to be moved
     * @param childId the id of the child being moved
     * @param newParentDn the normalized dn of the new parent for the child
     * @throws Exception if something goes wrong
     */
    private DN move( DN oldChildDn, Long childId, DN newParentDn ) throws Exception
    {
        // Get the child and the new parent to be entries and Ids
        Long newParentId = getEntryId( newParentDn.getNormName() );
        Long oldParentId = getParentId( childId );

        /*
         * All aliases including and below oldChildDn, will be affected by
         * the move operation with respect to one and subtree userIndices since
         * their relationship to ancestors above oldChildDn will be 
         * destroyed.  For each alias below and including oldChildDn we will
         * drop the index tuples mapping ancestor ids above oldChildDn to the
         * respective target ids of the aliases.
         */
        dropMovedAliasIndices( oldChildDn );

        /*
         * Drop the old parent child relationship and add the new one
         * Set the new parent id for the child replacing the old parent id
         */
        oneLevelIdx.drop( oldParentId, childId );
        oneLevelIdx.add( newParentId, childId );

        updateSubLevelIndex( childId, oldParentId, newParentId );

        /*
         * Build the new user provided DN (updn) for the child using the child's
         * user provided RDN & the new parent's UPDN.  Basically add the child's
         * UpRdn String to the tail of the new parent's Updn Name.
         */
        DN childUpdn = new DN( getEntryUpdn( childId ) );
        String childRdn = childUpdn.get( childUpdn.size() - 1 );
        DN newUpdn = new DN( getEntryUpdn( newParentId ) );
        newUpdn.add( newUpdn.size(), childRdn );

        // Call the modifyDn operation with the new updn
        modifyDn( childId, newUpdn, true );

        return newUpdn;
    }


    /**
     * 
     * updates the SubLevel Index as part of a move operation.
     *
     * @param childId child id to be moved
     * @param oldParentId old parent's id
     * @param newParentId new parent's id
     * @throws Exception
     */
    private void updateSubLevelIndex( Long childId, Long oldParentId, Long newParentId ) throws Exception
    {
        Long tempId = oldParentId;
        List<Long> parentIds = new ArrayList<Long>();

        // find all the parents of the oldParentId
        while ( tempId != 0 && tempId != 1 && tempId != null )
        {
            parentIds.add( tempId );
            tempId = getParentId( tempId );
        }

        // find all the children of the childId
        Cursor<IndexEntry<Long, E, Long>> cursor = subLevelIdx.forwardCursor( childId );

        List<Long> childIds = new ArrayList<Long>();
        childIds.add( childId );

        while ( cursor.next() )
        {
            childIds.add( cursor.get().getId() );
        }

        // detach the childId and all its children from oldParentId and all it parents excluding the root
        for ( Long pid : parentIds )
        {
            for ( Long cid : childIds )
            {
                subLevelIdx.drop( pid, cid );
            }
        }

        parentIds.clear();
        tempId = newParentId;

        // find all the parents of the newParentId
        while ( tempId != 0 && tempId != 1 && tempId != null )
        {
            parentIds.add( tempId );
            tempId = getParentId( tempId );
        }

        // attach the childId and all its children to newParentId and all it parents excluding the root
        for ( Long id : parentIds )
        {
            for ( Long cid : childIds )
            {
                subLevelIdx.add( id, cid );
            }
        }
    }


    /**
     * For all aliases including and under the moved base, this method removes
     * one and subtree alias index tuples for old ancestors above the moved base
     * that will no longer be ancestors after the move.
     * 
     * @param movedBase the base at which the move occured - the moved node
     * @throws Exception if system userIndices fail
     */
    private void dropMovedAliasIndices( final DN movedBase ) throws Exception
    {
        //        // Find all the aliases from movedBase down
        //        IndexAssertion<Object,E> isBaseDescendant = new IndexAssertion<Object,E>()
        //        {
        //            public boolean assertCandidate( IndexEntry<Object,E> rec ) throws Exception
        //            {
        //                String dn = getEntryDn( rec.getId() );
        //                return dn.endsWith( movedBase.toString() );
        //            }
        //        };

        Long movedBaseId = getEntryId( movedBase.getNormName() );

        if ( aliasIdx.reverseLookup( movedBaseId ) != null )
        {
            dropAliasIndices( movedBaseId, movedBase );
        }

        //        throw new NotImplementedException( "Fix the code below this line" );

        //        NamingEnumeration<ForwardIndexEntry> aliases =
        //                new IndexAssertionEnumeration( aliasIdx.listIndices( movedBase.toString(), true ), isBaseDescendant );
        //
        //        while ( aliases.hasMore() )
        //        {
        //            ForwardIndexEntry entry = aliases.next();
        //            dropAliasIndices( (Long)entry.getId(), movedBase );
        //        }
    }


    /**
     * For the alias id all ancestor one and subtree alias tuples are moved 
     * above the moved base.
     * 
     * @param aliasId the id of the alias 
     * @param movedBase the base where the move occured
     * @throws Exception if userIndices fail
     */
    private void dropAliasIndices( Long aliasId, DN movedBase ) throws Exception
    {
        String targetDn = aliasIdx.reverseLookup( aliasId );
        Long targetId = getEntryId( targetDn );
        String aliasDn = getEntryDn( aliasId );

        /*
         * Start droping index tuples with the first ancestor right above the 
         * moved base.  This is the first ancestor effected by the move.
         */
        DN ancestorDn = ( DN ) movedBase.getPrefix( 1 );
        Long ancestorId = getEntryId( ancestorDn.getNormName() );

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
        if ( aliasDn.equals( movedBase.toString() ) )
        {
            oneAliasIdx.drop( ancestorId, targetId );
        }

        subAliasIdx.drop( ancestorId, targetId );

        while ( !ancestorDn.equals( upSuffix ) )
        {
            ancestorDn = ( DN ) ancestorDn.getPrefix( 1 );
            ancestorId = getEntryId( ancestorDn.getNormName() );

            subAliasIdx.drop( ancestorId, targetId );
        }
    }


    /**
     * @param schemaManager the schemaManager to set
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }
}
