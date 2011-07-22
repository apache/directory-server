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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.directory.SearchControls;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.AbstractPartition;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.GenericIndex;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.MasterTable;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Optimizer;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapAliasDereferencingException;
import org.apache.directory.shared.ldap.model.exception.LdapAliasException;
import org.apache.directory.shared.ldap.model.exception.LdapAuthenticationNotSupportedException;
import org.apache.directory.shared.ldap.model.exception.LdapContextNotEmptyException;
import org.apache.directory.shared.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.MatchingRule;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.UsageEnum;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An abstract {@link Partition} that uses general BTree operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractBTreePartition<ID extends Comparable<ID>> extends AbstractPartition implements Store<Entry, ID>
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractBTreePartition.class );

    /** the search engine used to search the database */
    protected SearchEngine<Entry, ID> searchEngine;
    
    protected Optimizer optimizer;
    
    /** The default cache size is set to 10 000 objects */
    public static final int DEFAULT_CACHE_SIZE = 10000;

    /** The Entry cache size for this partition */
    protected int cacheSize = -1;

    /** true if we sync disks on every write operation */
    protected AtomicBoolean isSyncOnWrite = new AtomicBoolean( true );

    /** The suffix ID */
    private volatile ID suffixId;

    /** The path in which this Partition stores files */
    protected URI partitionPath;

    /** The set of indexed attributes */
    private Set<Index<?, Entry, ID>> indexedAttributes;

    /** the master table storing entries by primary key */
    protected MasterTable<ID, Entry> master;

    /** a map of attributeType numeric ID to user userIndices */
    protected Map<String, Index<?, Entry, ID>> userIndices = new HashMap<String, Index<?, Entry, ID>>();

    /** a map of attributeType numeric ID to system userIndices */
    protected Map<String, Index<?, Entry, ID>> systemIndices = new HashMap<String, Index<?, Entry, ID>>();

    /** the relative distinguished name index */
    protected Index<ParentIdAndRdn<ID>, Entry, ID> rdnIdx;

    /** a system index on objectClass attribute*/
    protected Index<String, Entry, ID> objectClassIdx;

    /** the parent child relationship index */
    protected Index<ID, Entry, ID> oneLevelIdx;

    /** a system index on the entries of descendants of root Dn */
    protected Index<ID, Entry, ID> subLevelIdx;

    /** the attribute presence index */
    protected Index<String, Entry, ID> presenceIdx;

    /** a system index on entryUUID attribute */
    protected Index<String, Entry, ID> entryUuidIdx;

    /** a system index on entryCSN attribute */
    protected Index<String, Entry, ID> entryCsnIdx;

    /** a system index on aliasedObjectName attribute */
    protected Index<String, Entry, ID> aliasIdx;

    /** the subtree scope alias index */
    protected Index<ID, Entry, ID> subAliasIdx;

    /** the one level scope alias index */
    protected Index<ID, Entry, ID> oneAliasIdx;
    
    /** Cached attributes types to avoid lookup all over the code */
    protected AttributeType OBJECT_CLASS_AT;
    protected AttributeType ENTRY_CSN_AT;
    protected AttributeType ENTRY_UUID_AT;
    protected AttributeType ALIASED_OBJECT_NAME_AT;

    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a B-tree based context partition.
     */
    protected AbstractBTreePartition( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;

        indexedAttributes = new HashSet<Index<?, Entry, ID>>();

        // Initialize Attribute types used all over this method
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        ALIASED_OBJECT_NAME_AT = schemaManager.getAttributeType( SchemaConstants.ALIASED_OBJECT_NAME_AT );
        ENTRY_CSN_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_CSN_AT );
        ENTRY_UUID_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_UUID_AT );
    }


    /**
     * Sets up the system indices.
     */
    @SuppressWarnings("unchecked")
    protected void setupSystemIndices() throws Exception
    {
        // add missing system indices
        if ( getPresenceIndex() == null )
        {
            Index<String, Entry, ID> index = new GenericIndex<String, Entry, ID>( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID ) ;
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getOneLevelIndex() == null )
        {
            Index<ID, Entry, ID> index = new GenericIndex<ID, Entry, ID>( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getSubLevelIndex() == null )
        {
            Index<ID, Entry, ID> index = new GenericIndex<ID, Entry, ID>( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getRdnIndex() == null )
        {
            Index<ParentIdAndRdn<ID>, Entry, ID> index = new GenericIndex<ParentIdAndRdn<ID>, Entry, ID>( ApacheSchemaConstants.APACHE_RDN_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getAliasIndex() == null )
        {
            Index<String, Entry, ID> index = new GenericIndex<String, Entry, ID>( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getOneAliasIndex() == null )
        {
            Index<ID, Entry, ID> index = new GenericIndex<ID, Entry, ID>( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getSubAliasIndex() == null )
        {
            Index<ID, Entry, ID> index = new GenericIndex<ID, Entry, ID>( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getObjectClassIndex() == null )
        {
            Index<String, Entry, ID> index = new GenericIndex<String, Entry, ID>( SchemaConstants.OBJECT_CLASS_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getEntryUuidIndex() == null )
        {
            Index<String, Entry, ID> index = new GenericIndex<String, Entry, ID>( SchemaConstants.ENTRY_UUID_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getEntryCsnIndex() == null )
        {
            Index<String, Entry, ID> index = new GenericIndex<String, Entry, ID>( SchemaConstants.ENTRY_CSN_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        // convert and initialize system indices
        for ( String oid : systemIndices.keySet() )
        {
            Index<?, Entry, ID> index = systemIndices.get( oid );
            index = convertAndInit( index );
            systemIndices.put( oid, index );
        }

        // set index shortcuts
        rdnIdx = ( Index<ParentIdAndRdn<ID>, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_RDN_AT_OID );
        presenceIdx = ( Index<String, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
        oneLevelIdx = ( Index<ID, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
        subLevelIdx = ( Index<ID, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );
        aliasIdx = ( Index<String, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
        oneAliasIdx = ( Index<ID, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
        subAliasIdx = ( Index<ID, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
        objectClassIdx = ( Index<String, Entry, ID> ) systemIndices.get( SchemaConstants.OBJECT_CLASS_AT_OID );
        entryUuidIdx = ( Index<String, Entry, ID> ) systemIndices.get( SchemaConstants.ENTRY_UUID_AT_OID );
        entryCsnIdx = ( Index<String, Entry, ID> ) systemIndices.get( SchemaConstants.ENTRY_CSN_AT_OID );
    }


    /**
     * Sets up the user indices.
     */
    protected void setupUserIndices() throws Exception
    {
        // convert and initialize system indices
        Map<String, Index<?, Entry, ID>> tmp = new HashMap<String, Index<?, Entry, ID>>();

        for ( String oid : userIndices.keySet() )
        {
            // check that the attributeType has an EQUALITY matchingRule
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( oid );
            MatchingRule mr = attributeType.getEquality();

            if ( mr != null )
            {
                Index<?, Entry, ID> index = userIndices.get( oid );
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
     * Convert and initialize an index for a specific store implementation.
     *
     * @param index the index
     * @return the converted and initialized index
     * @throws Exception
     */
    protected abstract Index<?, Entry, ID> convertAndInit( Index<?, Entry, ID> index ) throws Exception;


    /**
     * builds the Dn of the entry identified by the given id
     *
     * @param id the entry's id
     * @return the normalized Dn of the entry
     * @throws Exception
     */
    protected Dn buildEntryDn( ID id ) throws Exception
    {
        ID parentId = id;
        ID rootId = getRootId();

        StringBuilder upName = new StringBuilder();
        boolean isFirst = true;

        do
        {
            ParentIdAndRdn<ID> cur = rdnIdx.reverseLookup( parentId );
            Rdn[] rdns = cur.getRdns();

            for ( Rdn rdn : rdns )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    upName.append( ',' );
                }
                
                upName.append( rdn.getName() );
            }

            parentId = cur.getParentId();
        }
        while ( !parentId.equals( rootId ) );

        Dn dn = new Dn( schemaManager, upName.toString() );

        return dn;
    }


    // ------------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void addIndex( Index<?, Entry, ID> index ) throws Exception
    {
        checkInitialized( "addIndex" );

        // Check that the index ID is valid
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() );

        if ( attributeType == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_309, index.getAttributeId() ) );
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
     * Gets the path in which this Partition stores data.
     *
     * @return the path in which this Partition stores data.
     */
    public abstract URI getPartitionPath();


    /**
     * Sets the path in which this Partition stores data. This may be an URL to
     * a file or directory, or an JDBC URL.
     *
     * @param partitionDir the path in which this Partition stores data.
     */
    public void setPartitionPath( URI partitionPath )
    {
        checkInitialized( "partitionPath" );
        this.partitionPath = partitionPath;
    }


    public void setIndexedAttributes( Set<Index<?, Entry, ID>> indexedAttributes )
    {
        this.indexedAttributes = indexedAttributes;
    }


    public void addIndexedAttributes( Index<?, Entry, ID>... indexes )
    {
        for ( Index<?, Entry, ID> index : indexes )
        {
            indexedAttributes.add( index );
        }
    }


    public Set<Index<?, Entry, ID>> getIndexedAttributes()
    {
        return indexedAttributes;
    }


    /**
     * Used to specify the entry cache size for a Partition.  Various Partition
     * implementations may interpret this value in different ways: i.e. total cache
     * size limit verses the number of entries to cache.
     *
     * @param cacheSize the maximum size of the cache in the number of entries
     */
    public void setCacheSize( int cacheSize )
    {
        this.cacheSize = cacheSize;
    }


    /**
     * Gets the entry cache size for this BTreePartition.
     *
     * @return the maximum size of the cache as the number of entries maximum before paging out
     */
    public int getCacheSize()
    {
        return cacheSize;
    }

    
    public abstract ID getEntryId( Dn dn ) throws LdapException;
    
    public abstract int getChildCount( ID id ) throws LdapException;

    
    /**
     * {@inheritDoc}
     */
    public ID getParentId( ID childId ) throws Exception
    {
        ParentIdAndRdn<ID> key = rdnIdx.reverseLookup( childId );

        if ( key == null )
        {
            return null;
        }

        return key.getParentId();
    }


    /**
     * Get back an entry knowing its ID
     *
     * @param id The Entry ID we want to get back
     * @return The found Entry, or null if not found
     * @throws Exception If the lookup failed for any reason (except a not found entry)
     */
    public final Entry lookup( ID id ) throws LdapException
    {
        try
        {
            Entry entry = master.get( id );
    
            if ( entry != null )
            {
                // We have to store the DN in this entry
                Dn dn = buildEntryDn( id );
                entry.setDn( dn );
                
                return new ClonedServerEntry( entry );
            }
    
            return null;
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }
    

    public abstract Dn getEntryDn( ID id ) throws Exception;

    // -----------------------------------------------------------------------
    // E N D   C O N F I G U R A T I O N   M E T H O D S
    // -----------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public int count() throws Exception
    {
        return master.count();
    }


    // ------------------------------------------------------------------------
    // Public Accessors - not declared in any interfaces just for this class
    // ------------------------------------------------------------------------

    /**
     * Gets the DefaultSearchEngine used by this ContextPartition to search the
     * Database.
     *
     * @return the search engine
     */
    public SearchEngine<Entry, ID> getSearchEngine()
    {
        return searchEngine;
    }


    // ------------------------------------------------------------------------
    // Partition Interface Method Implementations
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        try
        {
            Entry entry = ( ( ClonedServerEntry ) addContext.getEntry() ).getClonedEntry();
            Dn entryDn = entry.getDn();
    
            // check if the entry already exists
            if ( getEntryId( entryDn ) != null )
            {
                LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                    I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, entryDn.getName() ) );
                throw ne;
            }
    
            ID parentId = null;
    
            //
            // Suffix entry cannot have a parent since it is the root so it is
            // capped off using the zero value which no entry can have since
            // entry sequences start at 1.
            //
            Dn parentDn = null;
            ParentIdAndRdn<ID> key = null;
    
            if ( entryDn.equals( suffixDn ) )
            {
                parentId = getRootId();
                key = new ParentIdAndRdn<ID>( parentId, suffixDn.getRdns() );
            }
            else
            {
                parentDn = entryDn.getParent();
                parentId = getEntryId( parentDn );
                
                key = new ParentIdAndRdn<ID>( parentId, entryDn.getRdn() );
            }
    
            // don't keep going if we cannot find the parent Id
            if ( parentId == null )
            {
                throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_216_ID_FOR_PARENT_NOT_FOUND, parentDn ) );
            }
            
            // Get a new ID for the added entry
            ID id = master.getNextId( entry );
    
            // Update the RDN index
            rdnIdx.add( key, id );
    
            // Update the ObjectClass index
            Attribute objectClass = entry.get( OBJECT_CLASS_AT );
            
            if ( objectClass == null )
            {
                String msg = I18n.err( I18n.ERR_217, entryDn.getName(), entry );
                ResultCodeEnum rc = ResultCodeEnum.OBJECT_CLASS_VIOLATION;
                LdapSchemaViolationException e = new LdapSchemaViolationException( rc, msg );
                //e.setResolvedName( entryDn );
                throw e;
            }
    
            for ( Value<?> value : objectClass )
            {
                objectClassIdx.add( value.getString(), id );
            }
    
            if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
            {
                Attribute aliasAttr = entry.get( ALIASED_OBJECT_NAME_AT );
                addAliasIndices( id, entryDn, aliasAttr.getString() );
            }
    
            // Update the OneLevel index
            oneLevelIdx.add( parentId, id );
    
            // Update the SubLevel index
            ID tempId = parentId;
    
            while ( ( tempId != null ) && ( !tempId.equals( getRootId() ) ) && ( !tempId.equals( getSuffixId() ) ) )
            {
                subLevelIdx.add( tempId, id );
                tempId = getParentId( tempId );
            }
    
            // making entry an ancestor/descendent of itself in sublevel index
            subLevelIdx.add( id, id );
    
            // Update the EntryCsn index
            Attribute entryCsn = entry.get( ENTRY_CSN_AT );
    
            if ( entryCsn == null )
            {
                String msg = I18n.err( I18n.ERR_219, entryDn.getName(), entry );
                throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
            }
    
            entryCsnIdx.add( entryCsn.getString(), id );
    
            // Update the EntryUuid index
            Attribute entryUuid = entry.get( ENTRY_UUID_AT );
    
            if ( entryUuid == null )
            {
                String msg = I18n.err( I18n.ERR_220, entryDn.getName(), entry );
                throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
            }
    
            entryUuidIdx.add( entryUuid.getString(), id );
    
            // Now work on the user defined userIndices
            for ( Attribute attribute : entry )
            {
                AttributeType attributeType = attribute.getAttributeType();
                String attributeOid = attributeType.getOid();
    
                if ( hasUserIndexOn( attributeType ) )
                {
                    Index<Object, Entry, ID> idx = ( Index<Object, Entry, ID> ) getUserIndex( attributeType );
    
                    // here lookup by attributeId is OK since we got attributeId from
                    // the entry via the enumeration - it's in there as is for sure
    
                    for ( Value<?> value : attribute )
                    {
                        idx.add( value.getValue(), id );
                    }
    
                    // Adds only those attributes that are indexed
                    presenceIdx.add( attributeOid, id );
                }
            }
    
            // Add the parentId in the entry
            entry.put( SchemaConstants.ENTRY_PARENT_ID_AT, parentId.toString() );
            
            // And finally add the entry into the master table
            master.put( id, entry );
    
            if ( isSyncOnWrite.get() )
            {
                sync();
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


    /**
     * {@inheritDoc}
     */
    public final void bind( BindOperationContext bindContext ) throws LdapException
    {
        // does nothing
        throw new LdapAuthenticationNotSupportedException( ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED, I18n
            .err( I18n.ERR_702 ) );
    }

    
    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        Dn dn = deleteContext.getDn();

        ID id = getEntryId( dn );

        // don't continue if id is null
        if ( id == null )
        {
            throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_699, dn ) );
        }

        if ( getChildCount( id ) > 0 )
        {
            LdapContextNotEmptyException cnee = new LdapContextNotEmptyException( I18n.err( I18n.ERR_700, dn ) );
            //cnee.setRemainingName( dn );
            throw cnee;
        }

        // We now defer the deletion to the implementing class
        delete( id );
    }


    /**
     * Delete the entry associated with a given Id
     * @param id The id of the entry to delete
     * @throws Exception If the deletion failed
     */
    public abstract void delete( ID id ) throws LdapException;

    
    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor list( ListOperationContext listContext ) throws LdapException
    {
        return new BaseEntryFilteringCursor( 
            new ServerEntryCursorAdaptor<ID>( this, 
                list( getEntryId( listContext.getDn() ) ) ), listContext );
    }


    /**
     * {@inheritDoc}
     */
    public abstract IndexCursor<ID, Entry, ID> list( ID id ) throws LdapException;

    
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        try
        {
            SearchControls searchCtls = searchContext.getSearchControls();
            IndexCursor<ID, Entry, ID> underlying;
            Dn dn = searchContext.getDn();
            AliasDerefMode derefMode = searchContext.getAliasDerefMode();
            ExprNode filter = searchContext.getFilter();

            underlying = searchEngine.cursor( dn, derefMode, filter, searchCtls );

            return new BaseEntryFilteringCursor( new ServerEntryCursorAdaptor<ID>( this, underlying ), searchContext );
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


    /**
     * {@inheritDoc}
     */
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        ID id = getEntryId( lookupContext.getDn() );

        if ( id == null )
        {
            return null;
        }

        Entry entry = lookup( id );

        // Remove all the attributes if the NO_ATTRIBUTE flag is set
        if ( lookupContext.hasNoAttribute() )
        {
            entry.clear();

            return entry;
        }

        if ( lookupContext.hasAllUser() )
        {
            if ( lookupContext.hasAllOperational() )
            {
                return entry;
            }
            else
            {
                for ( AttributeType attributeType : ( ((ClonedServerEntry)entry).getOriginalEntry() ).getAttributeTypes() )
                {
                    String oid = attributeType.getOid();

                    if ( attributeType.getUsage() != UsageEnum.USER_APPLICATIONS ) 
                    {
                        if ( !lookupContext.getAttrsId().contains( oid ) )
                        {
                            entry.removeAttributes( attributeType );
                        }
                    }
                }
            }
        }
        else
        {
            if ( lookupContext.hasAllOperational() )
            {
                for ( AttributeType attributeType : ( ((ClonedServerEntry)entry).getOriginalEntry() ).getAttributeTypes() )
                {
                    if ( attributeType.getUsage() == UsageEnum.USER_APPLICATIONS ) 
                    {
                        entry.removeAttributes( attributeType );
                    }
                }
            }
            else
            {
                if ( lookupContext.getAttrsId().size() == 0 )
                {
                    for ( AttributeType attributeType : ( ((ClonedServerEntry)entry).getOriginalEntry() ).getAttributeTypes() )
                    {
                        if ( attributeType.getUsage() != UsageEnum.USER_APPLICATIONS ) 
                        {
                            entry.removeAttributes( attributeType );
                        }
                    }
                }
                else
                {
                    for ( AttributeType attributeType : ( ((ClonedServerEntry)entry).getOriginalEntry() ).getAttributeTypes() )
                    {
                        String oid = attributeType.getOid();
                        
                        if ( !lookupContext.getAttrsId().contains( oid ) )
                        {
                            entry.removeAttributes( attributeType );
                        }
                    }
                }
            }
        }

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        try
        {
            Entry modifiedEntry = modify( modifyContext.getDn(), modifyContext.getModItems().toArray( new Modification[]{}) );
            modifyContext.setAlteredEntry( modifiedEntry );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        if ( moveContext.getNewSuperior().isDescendantOf( moveContext.getDn() ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                "cannot place an entry below itself" );
        }

        try
        {
            Dn oldDn = moveContext.getDn();
            Dn newSuperior = moveContext.getNewSuperior();
            Dn newDn = moveContext.getNewDn();
            Entry modifiedEntry = moveContext.getModifiedEntry();
            
            move( oldDn, newSuperior, newDn, modifiedEntry );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        if ( moveAndRenameContext.getNewSuperiorDn().isDescendantOf( moveAndRenameContext.getDn() ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                "cannot place an entry below itself" );
        }

        try
        {
            Dn oldDn = moveAndRenameContext.getDn();
            Dn newSuperiorDn = moveAndRenameContext.getNewSuperiorDn();
            Rdn newRdn = moveAndRenameContext.getNewRdn();
            boolean deleteOldRdn = moveAndRenameContext.getDeleteOldRdn();
            Entry modifiedEntry = moveAndRenameContext.getModifiedEntry();
            
            moveAndRename( oldDn, newSuperiorDn, newRdn, modifiedEntry, deleteOldRdn );
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
     * {@inheritDoc}
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        try
        {
            Dn oldDn = renameContext.getDn();
            Rdn newRdn = renameContext.getNewRdn();
            boolean deleteOldRdn = renameContext.getDeleteOldRdn();

            if ( renameContext.getEntry() != null )
            {
                Entry modifiedEntry = renameContext.getModifiedEntry();
                rename( oldDn, newRdn, deleteOldRdn, modifiedEntry );
            }
            else
            {
                rename( oldDn, newRdn, deleteOldRdn, null );
            }
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public final void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        // does nothing
    }


    /**
     * This method calls {@link Partition#lookup(LookupOperationContext)} and return <tt>true</tt>
     * if it returns an entry by default.  Please override this method if
     * there is more effective way for your implementation.
     */
    public final boolean hasEntry( EntryOperationContext entryContext ) throws LdapException
    {
        try
        {
            ID id = getEntryId( entryContext.getDn() );

            Entry entry = lookup( id );
            
            return entry != null; 
        }
        catch ( LdapException e )
        {
            return false;
        }
    }


    // ------------------------------------------------------------------------
    // Index Operations
    // ------------------------------------------------------------------------
    /**
     * Retrieve the SuffixID
     */
    protected ID getSuffixId() throws Exception
    {
        if ( suffixId == null )
        {
            ParentIdAndRdn<ID> key = new ParentIdAndRdn<ID>( getRootId(), suffixDn.getRdns() );
            
            suffixId = rdnIdx.forwardLookup( key );
        }

        return suffixId;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isSyncOnWrite()
    {
        return isSyncOnWrite.get();
    }

    /**
     * {@inheritDoc}
     */
    public void setSyncOnWrite( boolean isSyncOnWrite )
    {
        checkInitialized( "syncOnWrite" );
        this.isSyncOnWrite.set( isSyncOnWrite );
    }


    //------------------------------------------------------------------------
    // Index handling
    //------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public Iterator<String> getUserIndices()
    {
        return userIndices.keySet().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<String> getSystemIndices()
    {
        return systemIndices.keySet().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public Index<?, Entry, ID> getIndex( AttributeType attributeType ) throws IndexNotFoundException
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
    public Index<?, Entry, ID> getUserIndex( AttributeType attributeType ) throws IndexNotFoundException
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
    public Index<?, Entry, ID> getSystemIndex( AttributeType attributeType ) throws IndexNotFoundException
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
    public Index<ID, Entry, ID> getOneLevelIndex()
    {
        return ( Index<ID, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<ID, Entry, ID> getSubLevelIndex()
    {
        return ( Index<ID, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, Entry, ID> getAliasIndex()
    {
        return ( Index<String, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<ID, Entry, ID> getOneAliasIndex()
    {
        return ( Index<ID, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<ID, Entry, ID> getSubAliasIndex()
    {
        return ( Index<ID, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, Entry, ID> getObjectClassIndex()
    {
        return ( Index<String, Entry, ID> ) systemIndices.get( SchemaConstants.OBJECT_CLASS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, Entry, ID> getEntryUuidIndex()
    {
        return ( Index<String, Entry, ID> ) systemIndices.get( SchemaConstants.ENTRY_UUID_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, Entry, ID> getEntryCsnIndex()
    {
        return ( Index<String, Entry, ID> ) systemIndices.get( SchemaConstants.ENTRY_CSN_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, Entry, ID> getPresenceIndex()
    {
        return ( Index<String, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<ParentIdAndRdn<ID>, Entry, ID> getRdnIndex()
    {
        return ( Index<ParentIdAndRdn<ID>, Entry, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_RDN_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasUserIndexOn( AttributeType attributeType ) throws LdapException
    {
        return userIndices.containsKey( attributeType.getOid() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasSystemIndexOn( AttributeType attributeType ) throws LdapException
    {
        return systemIndices.containsKey( attributeType.getOid() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasIndexOn( AttributeType attributeType ) throws LdapException
    {
        return hasUserIndexOn( attributeType ) || hasSystemIndexOn( attributeType );
    }
    
    
    /**
     * {@inheritDoc}}
     */
    public abstract ID getDefaultId();


    /**
     * {@inheritDoc}
     */
    public abstract ID getRootId();
    
    
    //---------------------------------------------------------------------------------------------
    // Alias index manipulation
    //---------------------------------------------------------------------------------------------
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
    protected void addAliasIndices( ID aliasId, Dn aliasDn, String aliasTarget ) throws Exception
    {
        Dn normalizedAliasTargetDn; // Name value of aliasedObjectName
        ID targetId; // Id of the aliasedObjectName
        Dn ancestorDn; // Name of an alias entry relative
        ID ancestorId; // Id of an alias entry relative

        // Access aliasedObjectName, normalize it and generate the Name
        normalizedAliasTargetDn = new Dn( schemaManager, aliasTarget );

        /*
         * Check For Aliases External To Naming Context
         *
         * id may be null but the alias may be to a valid entry in
         * another namingContext.  Such aliases are not allowed and we
         * need to point it out to the user instead of saying the target
         * does not exist when it potentially could outside of this upSuffix.
         */
        if ( !normalizedAliasTargetDn.isDescendantOf( suffixDn ) )
        {
            String msg = I18n.err( I18n.ERR_225, suffixDn.getName() );
            LdapAliasDereferencingException e = new LdapAliasDereferencingException( msg );
            //e.setResolvedName( aliasDn );
            throw e;
        }

        // L O O K U P   T A R G E T   I D
        targetId = getEntryId( normalizedAliasTargetDn );

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
        ancestorDn = aliasDn.getParent();
        ancestorId = getEntryId( ancestorDn );

        // check if alias parent and aliased entry are the same
        Dn normalizedAliasTargetParentDn = normalizedAliasTargetDn.getParent();

        if ( !aliasDn.isDescendantOf( normalizedAliasTargetParentDn ) )
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
        while ( !ancestorDn.equals( suffixDn ) && null != ancestorId )
        {
            if ( !normalizedAliasTargetDn.isDescendantOf( ancestorDn ) )
            {
                subAliasIdx.add( ancestorId, targetId );
            }

            ancestorDn = ancestorDn.getParent();
            ancestorId = getEntryId( ancestorDn );
        }
    }
    
    
    private void dumpIndex( OutputStream stream, Index<?, Entry, ID> index )
    {
        try
        {
            IndexCursor<?, Entry, ID> cursor = index.forwardCursor();
            
            while ( cursor.next() )
            {
                IndexEntry<?, Entry, ID> entry = cursor.get();
                
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
    public void dumpIndex( OutputStream stream, String name ) throws IOException
    {
        try
        {
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( name );
            
            if ( attributeType == null )
            {
                stream.write( Strings.getBytesUtf8( "Cannot find an index for AttributeType names " + name ) );
                
                return;
            }
            
            if ( attributeType.getOid().equals( ApacheSchemaConstants.APACHE_RDN_AT_OID  ))
            {
                dumpIndex( stream, rdnIdx );
            }
        }
        catch ( LdapException le )
        {
            stream.write( Strings.getBytesUtf8( "Cannot find an index for AttributeType names " + name ) );
        }
    }
}
