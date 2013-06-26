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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.BinaryValue;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.StringValue;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapAliasDereferencingException;
import org.apache.directory.api.ldap.model.exception.LdapAliasException;
import org.apache.directory.api.ldap.model.exception.LdapContextNotEmptyException;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.api.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Ava;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.api.util.exception.MultiException;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.api.partition.AbstractPartition;
import org.apache.directory.server.core.api.partition.Partition;
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
    protected int cacheSize = -1;

    /** true if we sync disks on every write operation */
    protected AtomicBoolean isSyncOnWrite = new AtomicBoolean( true );

    /** The suffix UUID */
    private volatile String suffixId;

    /** The path in which this Partition stores files */
    protected URI partitionPath;

    /** The set of indexed attributes */
    private Set<Index<?, ?, String>> indexedAttributes;

    /** the master table storing entries by primary key */
    protected MasterTable master;

    /** a map of attributeType numeric UUID to user userIndices */
    protected Map<String, Index<?, Entry, String>> userIndices = new HashMap<String, Index<?, Entry, String>>();

    /** a map of attributeType numeric UUID to system userIndices */
    protected Map<String, Index<?, Entry, String>> systemIndices = new HashMap<String, Index<?, Entry, String>>();

    /** the relative distinguished name index */
    protected Index<ParentIdAndRdn, Entry, String> rdnIdx;

    /** a system index on objectClass attribute*/
    protected Index<String, Entry, String> objectClassIdx;

    /** the attribute presence index */
    protected Index<String, Entry, String> presenceIdx;

    /** a system index on entryCSN attribute */
    protected Index<String, Entry, String> entryCsnIdx;

    /** a system index on aliasedObjectName attribute */
    protected Index<Dn, Entry, String> aliasIdx;

    /** the subtree scope alias index */
    protected Index<String, Entry, String> subAliasIdx;

    /** the one level scope alias index */
    protected Index<String, Entry, String> oneAliasIdx;

    /** a system index on administrativeRole attribute */
    protected Index<String, Entry, String> adminRoleIdx;

    /** Cached attributes types to avoid lookup all over the code */
    protected AttributeType OBJECT_CLASS_AT;
    protected AttributeType ENTRY_CSN_AT;
    protected AttributeType ENTRY_DN_AT;
    protected AttributeType ENTRY_UUID_AT;
    protected AttributeType ALIASED_OBJECT_NAME_AT;
    protected AttributeType ADMINISTRATIVE_ROLE_AT;

    private static final boolean NO_REVERSE = Boolean.FALSE;
    private static final boolean WITH_REVERSE = Boolean.TRUE;

    protected static final boolean ADD_CHILD = true;
    protected static final boolean REMOVE_CHILD = false;

    /** A lock to protect the backend from concurrent reads/writes */
    private ReadWriteLock rwLock;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a B-tree based context partition.
     */
    protected AbstractBTreePartition( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;

        indexedAttributes = new HashSet<Index<?, ?, String>>();

        // Initialize Attribute types used all over this method
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        ALIASED_OBJECT_NAME_AT = schemaManager.getAttributeType( SchemaConstants.ALIASED_OBJECT_NAME_AT );
        ENTRY_CSN_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_CSN_AT );
        ENTRY_DN_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_DN_AT );
        ENTRY_UUID_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_UUID_AT );
        ADMINISTRATIVE_ROLE_AT = schemaManager.getAttributeType( SchemaConstants.ADMINISTRATIVE_ROLE_AT );
    }


    // ------------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------
    /**
     * Gets the entry cache size for this BTreePartition.
     *
     * @return the maximum size of the cache as the number of entries maximum before paging out
     */
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
     * @param partitionDir the path in which this Partition stores data.
     */
    public void setPartitionPath( URI partitionPath )
    {
        checkInitialized( "partitionPath" );
        this.partitionPath = partitionPath;
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


    /**
     * Sets up the system indices.
     */
    @SuppressWarnings("unchecked")
    protected void setupSystemIndices() throws Exception
    {
        // add missing system indices
        if ( getPresenceIndex() == null )
        {
            Index<String, Entry, String> index = createSystemIndex( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID,
                partitionPath, NO_REVERSE );
            addIndex( index );
        }

        if ( getRdnIndex() == null )
        {
            Index<ParentIdAndRdn, Entry, String> index = createSystemIndex(
                ApacheSchemaConstants.APACHE_RDN_AT_OID,
                partitionPath, WITH_REVERSE );
            addIndex( index );
        }

        if ( getAliasIndex() == null )
        {
            Index<Dn, Entry, String> index = createSystemIndex( ApacheSchemaConstants.APACHE_ALIAS_AT_OID,
                partitionPath, WITH_REVERSE );
            addIndex( index );
        }

        if ( getOneAliasIndex() == null )
        {
            Index<String, Entry, String> index = createSystemIndex( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID,
                partitionPath, NO_REVERSE );
            addIndex( index );
        }

        if ( getSubAliasIndex() == null )
        {
            Index<String, Entry, String> index = createSystemIndex( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID,
                partitionPath, NO_REVERSE );
            addIndex( index );
        }

        if ( getObjectClassIndex() == null )
        {
            Index<String, Entry, String> index = createSystemIndex( SchemaConstants.OBJECT_CLASS_AT_OID, partitionPath,
                NO_REVERSE );
            addIndex( index );
        }

        if ( getEntryCsnIndex() == null )
        {
            Index<String, Entry, String> index = createSystemIndex( SchemaConstants.ENTRY_CSN_AT_OID, partitionPath,
                NO_REVERSE );
            addIndex( index );
        }

        if ( getAdministrativeRoleIndex() == null )
        {
            Index<String, Entry, String> index = createSystemIndex( SchemaConstants.ADMINISTRATIVE_ROLE_AT_OID,
                partitionPath,
                NO_REVERSE );
            addIndex( index );
        }

        // convert and initialize system indices
        for ( String oid : systemIndices.keySet() )
        {
            Index<?, Entry, String> index = systemIndices.get( oid );
            index = convertAndInit( index );
            systemIndices.put( oid, index );
        }

        // set index shortcuts
        rdnIdx = ( Index<ParentIdAndRdn, Entry, String> ) systemIndices
            .get( ApacheSchemaConstants.APACHE_RDN_AT_OID );
        presenceIdx = ( Index<String, Entry, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
        aliasIdx = ( Index<Dn, Entry, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
        oneAliasIdx = ( Index<String, Entry, String> ) systemIndices
            .get( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
        subAliasIdx = ( Index<String, Entry, String> ) systemIndices
            .get( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
        objectClassIdx = ( Index<String, Entry, String> ) systemIndices.get( SchemaConstants.OBJECT_CLASS_AT_OID );
        entryCsnIdx = ( Index<String, Entry, String> ) systemIndices.get( SchemaConstants.ENTRY_CSN_AT_OID );
        adminRoleIdx = ( Index<String, Entry, String> ) systemIndices.get( SchemaConstants.ADMINISTRATIVE_ROLE_AT_OID );
    }


    /**
     * Sets up the user indices.
     */
    protected void setupUserIndices() throws Exception
    {
        // convert and initialize system indices
        Map<String, Index<?, Entry, String>> tmp = new HashMap<String, Index<?, Entry, String>>();

        for ( String oid : userIndices.keySet() )
        {
            // check that the attributeType has an EQUALITY matchingRule
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( oid );
            MatchingRule mr = attributeType.getEquality();

            if ( mr != null )
            {
                Index<?, Entry, String> index = userIndices.get( oid );
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
     * @throws Exception
     */
    protected abstract Index<?, Entry, String> convertAndInit( Index<?, Entry, String> index ) throws Exception;


    /**
     * Gets the path in which this Partition stores data.
     *
     * @return the path in which this Partition stores data.
     */
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
    protected void doDestroy() throws LdapException, Exception
    {
        LOG.debug( "destroy() called on store for {}", this.suffixDn );

        if ( !initialized )
        {
            return;
        }

        // don't reset initialized flag
        initialized = false;

        MultiException errors = new MultiException( I18n.err( I18n.ERR_577 ) );

        for ( Index<?, Entry, String> index : userIndices.values() )
        {
            try
            {
                index.close();
                LOG.debug( "Closed {} user index for {} partition.", index.getAttributeId(), suffixDn );
            }
            catch ( Throwable t )
            {
                LOG.error( I18n.err( I18n.ERR_124 ), t );
                errors.addThrowable( t );
            }
        }

        for ( Index<?, Entry, String> index : systemIndices.values() )
        {
            try
            {
                index.close();
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
            master.close();
            LOG.debug( I18n.err( I18n.ERR_125, suffixDn ) );
        }
        catch ( Throwable t )
        {
            LOG.error( I18n.err( I18n.ERR_126 ), t );
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
    protected void doInit() throws Exception
    {
        // First, inject the indexed attributes if any
        if ( ( indexedAttributes != null ) && ( indexedAttributes.size() > 0 ) )
        {
            for ( Index index : indexedAttributes )
            {
                addIndex( index );
            }
        }

        // Now, initialize the configured index
        setupSystemIndices();
        setupUserIndices();

    }


    private void dumpAllRdnIdx() throws Exception
    {
        if ( LOG.isDebugEnabled() )
        {
            dumpRdnIdx( Partition.ROOT_ID, "" );
            System.out.println( "-----------------------------" );
        }
    }


    private void dumpRdnIdx() throws Exception
    {
        if ( LOG.isDebugEnabled() )
        {
            dumpRdnIdx( Partition.ROOT_ID, 1, "" );
            System.out.println( "-----------------------------" );
        }
    }


    public void dumpRdnIdx( String id, String tabs ) throws Exception
    {
        // Start with the root
        Cursor<IndexEntry<ParentIdAndRdn, String>> cursor = rdnIdx.forwardCursor();

        IndexEntry<ParentIdAndRdn, String> startingPos = new IndexEntry<ParentIdAndRdn, String>();
        startingPos.setKey( new ParentIdAndRdn( id, ( Rdn[] ) null ) );
        cursor.before( startingPos );

        while ( cursor.next() )
        {
            IndexEntry<ParentIdAndRdn, String> entry = cursor.get();
            System.out.println( tabs + entry );
        }

        cursor.close();
    }


    private void dumpRdnIdx( String id, int nbSibbling, String tabs ) throws Exception
    {
        // Start with the root
        Cursor<IndexEntry<ParentIdAndRdn, String>> cursor = rdnIdx.forwardCursor();

        IndexEntry<ParentIdAndRdn, String> startingPos = new IndexEntry<ParentIdAndRdn, String>();
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
                dumpRdnIdx( entry.getId(), nbChildren, tabs + "  " );
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
    public void add( AddOperationContext addContext ) throws LdapException
    {
        try
        {
            setRWLock( addContext );
            Entry entry = ( ( ClonedServerEntry ) addContext.getEntry() ).getClonedEntry();

            Dn entryDn = entry.getDn();

            // check if the entry already exists
            lockRead();

            try
            {
                if ( getEntryId( entryDn ) != null )
                {
                    LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                        I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, entryDn.getName() ) );
                    throw ne;
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
            ParentIdAndRdn key = null;

            if ( entryDn.equals( suffixDn ) )
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
                    parentId = getEntryId( parentDn );
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
            Attribute entryUUID = entry.get( ENTRY_UUID_AT );

            String id = null;

            if ( entryUUID == null )
            {
                id = master.getNextId( entry );
            }
            else
            {
                id = entryUUID.getString();
            }

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
                if ( value.equals( SchemaConstants.TOP_OC ) )
                {
                    continue;
                }

                objectClassIdx.add( ( String ) value.getNormValue(), id );
            }

            if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
            {
                Attribute aliasAttr = entry.get( ALIASED_OBJECT_NAME_AT );
                addAliasIndices( id, entryDn, new Dn( schemaManager, aliasAttr.getString() ) );
            }

            // Update the EntryCsn index
            Attribute entryCsn = entry.get( ENTRY_CSN_AT );

            if ( entryCsn == null )
            {
                String msg = I18n.err( I18n.ERR_219, entryDn.getName(), entry );
                throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
            }

            entryCsnIdx.add( entryCsn.getString(), id );

            // Update the AdministrativeRole index, if needed
            if ( entry.containsAttribute( ADMINISTRATIVE_ROLE_AT ) )
            {
                // We may have more than one role
                Attribute adminRoles = entry.get( ADMINISTRATIVE_ROLE_AT );

                for ( Value<?> value : adminRoles )
                {
                    adminRoleIdx.add( ( String ) value.getNormValue(), id );
                }

                // Adds only those attributes that are indexed
                presenceIdx.add( ADMINISTRATIVE_ROLE_AT.getOid(), id );
            }

            // Now work on the user defined userIndices
            for ( Attribute attribute : entry )
            {
                AttributeType attributeType = attribute.getAttributeType();
                String attributeOid = attributeType.getOid();

                if ( hasUserIndexOn( attributeType ) )
                {
                    Index<Object, Entry, String> idx = ( Index<Object, Entry, String> ) getUserIndex( attributeType );

                    // here lookup by attributeId is OK since we got attributeId from
                    // the entry via the enumeration - it's in there as is for sure

                    for ( Value<?> value : attribute )
                    {
                        idx.add( value.getNormValue(), id );
                    }

                    // Adds only those attributes that are indexed
                    presenceIdx.add( attributeOid, id );
                }
            }

            // Add the parentId in the entry
            entry.put( SchemaConstants.ENTRY_PARENT_ID_AT, parentId );

            lockWrite();

            try
            {
                // Update the RDN index
                rdnIdx.add( key, id );

                // Update the parent's nbChildren and nbDescendants values
                if ( parentId != Partition.ROOT_ID )
                {
                    updateRdnIdx( parentId, ADD_CHILD, 0 );
                }

                // Remove the EntryDN attribute
                entry.removeAttributes( ENTRY_DN_AT );

                // And finally add the entry into the master table
                master.put( id, entry );
            }
            finally
            {
                unlockWrite();
            }

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


    //---------------------------------------------------------------------------------------------
    // The Delete operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public Entry delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        try
        {
            setRWLock( deleteContext );
            Dn dn = deleteContext.getDn();
            String id = null;

            lockRead();

            try
            {
                id = getEntryId( dn );
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

            int childCount = getChildCount( id );

            if ( childCount > 0 )
            {
                LdapContextNotEmptyException cnee = new LdapContextNotEmptyException( I18n.err( I18n.ERR_700, dn ) );
                //cnee.setRemainingName( dn );
                throw cnee;
            }

            // We now defer the deletion to the implementing class
            Entry deletedEntry = delete( id );

            updateCache( deleteContext );

            return deletedEntry;
        }
        catch ( LdapException le )
        {
            throw le;
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage() );
        }
    }


    protected void updateRdnIdx( String parentId, boolean addRemove, int nbDescendant ) throws Exception
    {
        boolean isFirst = true;

        if ( parentId.equals( Partition.ROOT_ID ) )
        {
            return;
        }

        ParentIdAndRdn parent = rdnIdx.reverseLookup( parentId );

        while ( parent != null )
        {
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
            // we first need to drop it so that the key can be replaced
            rdnIdx.drop( parentId );
            rdnIdx.add( parent, parentId );

            parentId = parent.getParentId();
            parent = rdnIdx.reverseLookup( parentId );
        }
    }


    /**
     * Delete the entry associated with a given Id
     * @param id The id of the entry to delete
     * @return the deleted entry if found
     * @throws Exception If the deletion failed
     */
    public Entry delete( String id ) throws LdapException
    {
        try
        {
            // First get the entry
            Entry entry = null;

            lockRead();

            try
            {
                entry = master.get( id );
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

            Attribute objectClass = entry.get( OBJECT_CLASS_AT );

            if ( objectClass.contains( SchemaConstants.ALIAS_OC ) )
            {
                dropAliasIndices( id );
            }

            // Update the ObjectClass index
            for ( Value<?> value : objectClass )
            {
                objectClassIdx.drop( value.getString(), id );
            }

            // Update the parent's nbChildren and nbDescendants values
            ParentIdAndRdn parent = rdnIdx.reverseLookup( id );
            updateRdnIdx( parent.getParentId(), REMOVE_CHILD, 0 );

            // Update the rdn, oneLevel, subLevel, and entryCsn indexes
            entryCsnIdx.drop( entry.get( ENTRY_CSN_AT ).getString(), id );

            // Update the AdministrativeRole index, if needed
            if ( entry.containsAttribute( ADMINISTRATIVE_ROLE_AT ) )
            {
                // We may have more than one role
                Attribute adminRoles = entry.get( ADMINISTRATIVE_ROLE_AT );

                for ( Value<?> value : adminRoles )
                {
                    adminRoleIdx.drop( ( String ) value.getNormValue(), id );
                }

                // Deletes only those attributes that are indexed
                presenceIdx.drop( ADMINISTRATIVE_ROLE_AT.getOid(), id );
            }

            // Update the user indexes
            for ( Attribute attribute : entry )
            {
                AttributeType attributeType = attribute.getAttributeType();
                String attributeOid = attributeType.getOid();

                if ( hasUserIndexOn( attributeType ) )
                {
                    Index<?, Entry, String> index = getUserIndex( attributeType );

                    // here lookup by attributeId is ok since we got attributeId from
                    // the entry via the enumeration - it's in there as is for sure
                    for ( Value<?> value : attribute )
                    {
                        ( ( Index ) index ).drop( value.getValue(), id );
                    }

                    presenceIdx.drop( attributeOid, id );
                }
            }

            lockWrite();

            try
            {
                rdnIdx.drop( id );

                dumpRdnIdx();

                master.remove( id );
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
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        try
        {
            setRWLock( searchContext );

            PartitionSearchResult searchResult = searchEngine.computeResult( schemaManager, searchContext );

            Cursor<Entry> result = new EntryCursorAdaptor( this, searchResult );

            return new BaseEntryFilteringCursor( result, searchContext, schemaManager );
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
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        setRWLock( lookupContext );
        String id = getEntryId( lookupContext.getDn() );

        if ( id == null )
        {
            return null;
        }

        Entry entry = fetch( id, lookupContext.getDn() );

        return entry;
    }


    /**
     * Get back an entry knowing its UUID
     *
     * @param id The Entry UUID we want to get back
     * @return The found Entry, or null if not found
     * @throws Exception If the lookup failed for any reason (except a not found entry)
     */
    public Entry fetch( String id ) throws LdapException
    {
        try
        {
            rwLock.readLock().lock();

            Dn dn = buildEntryDn( id );

            return fetch( id, dn );
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
     * @param id The Entry UUID we want to get back
     * @return The found Entry, or null if not found
     * @throws Exception If the lookup failed for any reason (except a not found entry)
     */
    public Entry fetch( String id, Dn dn ) throws LdapException
    {
        try
        {
            Entry entry = lookupCache( id );

            if ( entry != null )
            {
                if ( !entry.containsAttribute( ENTRY_DN_AT ) )
                {
                    entry.add( ENTRY_DN_AT, dn.getName() );
                }

                return new ClonedServerEntry( entry );
            }

            try
            {
                rwLock.readLock().lock();
                entry = master.get( id );
            }
            finally
            {
                rwLock.readLock().unlock();
            }

            if ( entry != null )
            {
                // We have to store the DN in this entry
                entry.setDn( dn );

                if ( !entry.containsAttribute( ENTRY_DN_AT ) )
                {
                    entry.add( ENTRY_DN_AT, dn.getName() );
                }

                addToCache( id, entry );

                return new ClonedServerEntry( entry );
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
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        try
        {
            setRWLock( modifyContext );

            Entry modifiedEntry = modify( modifyContext.getDn(),
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
    public synchronized final Entry modify( Dn dn, Modification... mods ) throws Exception
    {
        String id = getEntryId( dn );
        Entry entry = master.get( id );

        for ( Modification mod : mods )
        {
            Attribute attrMods = mod.getAttribute();

            switch ( mod.getOperation() )
            {
                case ADD_ATTRIBUTE:
                    modifyAdd( id, entry, attrMods );
                    break;

                case REMOVE_ATTRIBUTE:
                    modifyRemove( id, entry, attrMods );
                    break;

                case REPLACE_ATTRIBUTE:
                    modifyReplace( id, entry, attrMods );
                    break;

                default:
                    throw new LdapException( I18n.err( I18n.ERR_221 ) );
            }
        }

        updateCsnIndex( entry, id );

        // Remove the EntryDN
        entry.removeAttributes( ENTRY_DN_AT );

        master.put( id, entry );

        if ( isSyncOnWrite.get() )
        {
            sync();
        }

        return entry;
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
    private void modifyAdd( String id, Entry entry, Attribute mods ) throws Exception
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );
        AttributeType attributeType = mods.getAttributeType();

        // Special case for the ObjectClass index
        if ( modsOid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
        {
            for ( Value<?> value : mods )
            {
                objectClassIdx.add( ( String ) value.getNormValue(), id );
            }
        }
        else if ( hasUserIndexOn( attributeType ) )
        {
            Index<?, Entry, String> index = getUserIndex( attributeType );

            if ( mods.size() > 0 )
            {
                for ( Value<?> value : mods )
                {
                    ( ( Index ) index ).add( value.getNormValue(), id );
                }
            }
            else
            {
                // Special case when we have null values
                ( ( Index ) index ).add( null, id );
            }

            // If the attr didn't exist for this id add it to presence index
            if ( !presenceIdx.forward( modsOid, id ) )
            {
                presenceIdx.add( modsOid, id );
            }
        }
        // Special case for the AdministrativeRole index
        else if ( modsOid.equals( SchemaConstants.ADMINISTRATIVE_ROLE_AT_OID ) )
        {
            // We may have more than one role 
            for ( Value<?> value : mods )
            {
                adminRoleIdx.add( ( String ) value.getNormValue(), id );
            }

            // If the attr didn't exist for this id add it to presence index
            if ( !presenceIdx.forward( modsOid, id ) )
            {
                presenceIdx.add( modsOid, id );
            }
        }

        // add all the values in mods to the same attribute in the entry
        if ( mods.size() > 0 )
        {
            for ( Value<?> value : mods )
            {
                entry.add( mods.getAttributeType(), value );
            }
        }
        else
        {
            // Special cases for null values
            if ( mods.getAttributeType().getSyntax().isHumanReadable() )
            {
                entry.add( mods.getAttributeType(), new StringValue( null ) );
            }
            else
            {
                entry.add( mods.getAttributeType(), new BinaryValue( null ) );
            }
        }

        if ( modsOid.equals( SchemaConstants.ALIASED_OBJECT_NAME_AT_OID ) )
        {
            Dn ndn = getEntryDn( id );
            addAliasIndices( id, ndn, new Dn( schemaManager, mods.getString() ) );
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
    private void modifyReplace( String id, Entry entry, Attribute mods ) throws Exception
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );
        AttributeType attributeType = mods.getAttributeType();

        // Special case for the ObjectClass index
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            // if the id exists in the index drop all existing attribute
            // value index entries and add new ones
            for ( Value<?> value : entry.get( OBJECT_CLASS_AT ) )
            {
                objectClassIdx.drop( ( String ) value.getNormValue(), id );
            }

            for ( Value<?> value : mods )
            {
                if ( value.equals( SchemaConstants.TOP_OC ) )
                {
                    continue;
                }

                objectClassIdx.add( ( String ) value.getNormValue(), id );
            }
        }
        else if ( hasUserIndexOn( attributeType ) )
        {
            Index<?, Entry, String> index = getUserIndex( attributeType );

            // Drop all the previous values
            Attribute oldAttribute = entry.get( mods.getAttributeType() );

            if ( oldAttribute != null )
            {
                for ( Value<?> value : oldAttribute )
                {
                    ( ( Index<Object, Entry, String> ) index ).drop( value.getNormValue(), id );
                }
            }

            // And add the new ones
            for ( Value<?> value : mods )
            {
                ( ( Index<Object, Entry, String> ) index ).add( value.getNormValue(), id );
            }

            /*
             * If we have no new value, we have to drop the AT fro the presence index
             */
            if ( mods.size() == 0 )
            {
                presenceIdx.drop( modsOid, id );
            }
        }
        // Special case for the AdministrativeRole index
        else if ( attributeType.equals( ADMINISTRATIVE_ROLE_AT ) )
        {
            // Remove the previous values
            for ( Value<?> value : entry.get( ADMINISTRATIVE_ROLE_AT ) )
            {
                objectClassIdx.drop( ( String ) value.getNormValue(), id );
            }

            // And add the new ones 
            for ( Value<?> value : mods )
            {
                adminRoleIdx.add( ( String ) value.getNormValue(), id );
            }
        }

        String aliasAttributeOid = schemaManager.getAttributeTypeRegistry().getOidByName(
            SchemaConstants.ALIASED_OBJECT_NAME_AT );

        if ( mods.getAttributeType().equals( ALIASED_OBJECT_NAME_AT ) )
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
            Dn entryDn = getEntryDn( id );
            addAliasIndices( id, entryDn, new Dn( schemaManager, mods.getString() ) );
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
    private void modifyRemove( String id, Entry entry, Attribute mods ) throws Exception
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        String modsOid = schemaManager.getAttributeTypeRegistry().getOidByName( mods.getId() );
        AttributeType attributeType = mods.getAttributeType();

        // Special case for the ObjectClass index
        if ( attributeType.equals( OBJECT_CLASS_AT ) )
        {
            /*
             * If there are no attribute values in the modifications then this
             * implies the complete removal of the attribute from the index. Else
             * we remove individual tuples from the index.
             */
            if ( mods.size() == 0 )
            {
                for ( Value<?> objectClass : entry.get( OBJECT_CLASS_AT ) )
                {
                    objectClassIdx.drop( ( String ) objectClass.getNormValue(), id );
                }
            }
            else
            {
                for ( Value<?> value : mods )
                {
                    objectClassIdx.drop( ( String ) value.getNormValue(), id );
                }
            }
        }
        else if ( hasUserIndexOn( attributeType ) )
        {
            Index<?, Entry, String> index = getUserIndex( attributeType );

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
                ( ( Index ) index ).drop( id );
                nbValues = 0;
            }
            else
            {
                for ( Value<?> value : mods )
                {
                    if ( attribute.contains( value ) )
                    {
                        nbValues--;
                        attribute.remove( value );
                    }

                    ( ( Index ) index ).drop( value.getNormValue(), id );
                }
            }

            /*
             * If no attribute values exist for this entryId in the index then
             * we remove the presence index entry for the removed attribute.
             */
            if ( nbValues == 0 )
            {
                presenceIdx.drop( modsOid, id );
            }
        }
        // Special case for the AdministrativeRole index
        else if ( modsOid.equals( SchemaConstants.ADMINISTRATIVE_ROLE_AT_OID ) )
        {
            // We may have more than one role 
            for ( Value<?> value : mods )
            {
                adminRoleIdx.drop( ( String ) value.getNormValue(), id );
            }

            /*
             * If no attribute values exist for this entryId in the index then
             * we remove the presence index entry for the removed attribute.
             */
            if ( null == adminRoleIdx.reverseLookup( id ) )
            {
                presenceIdx.drop( modsOid, id );
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
        if ( mods.getAttributeType().equals( ALIASED_OBJECT_NAME_AT ) )
        {
            dropAliasIndices( id );
        }
    }


    //---------------------------------------------------------------------------------------------
    // The Move operation
    //---------------------------------------------------------------------------------------------
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
            setRWLock( moveContext );
            Dn oldDn = moveContext.getDn();
            Dn newSuperior = moveContext.getNewSuperior();
            Dn newDn = moveContext.getNewDn();
            Entry modifiedEntry = moveContext.getModifiedEntry();

            move( oldDn, newSuperior, newDn, modifiedEntry );
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
    public synchronized final void move( Dn oldDn, Dn newSuperiorDn, Dn newDn, Entry modifiedEntry )
        throws Exception
    {
        // Check that the parent Dn exists
        String newParentId = getEntryId( newSuperiorDn );

        if ( newParentId == null )
        {
            // This is not allowed : the parent must exist
            LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_256_NO_SUCH_OBJECT, newSuperiorDn.getName() ) );
            throw ne;
        }

        // Now check that the new entry does not exist
        String newId = getEntryId( newDn );

        if ( newId != null )
        {
            // This is not allowed : we should not be able to move an entry
            // to an existing position
            LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, newSuperiorDn.getName() ) );
            throw ne;
        }

        // Get the entry and the old parent IDs
        String entryId = getEntryId( oldDn );
        String oldParentId = getParentId( entryId );

        /*
         * All aliases including and below oldChildDn, will be affected by
         * the move operation with respect to one and subtree userIndices since
         * their relationship to ancestors above oldChildDn will be
         * destroyed.  For each alias below and including oldChildDn we will
         * drop the index tuples mapping ancestor ids above oldChildDn to the
         * respective target ids of the aliases.
         */
        dropMovedAliasIndices( oldDn );

        // Update the Rdn index
        // First drop the old entry
        ParentIdAndRdn movedEntry = rdnIdx.reverseLookup( entryId );

        updateRdnIdx( oldParentId, REMOVE_CHILD, movedEntry.getNbDescendants() );

        rdnIdx.drop( entryId );

        // Now, add the new entry at the right position
        movedEntry.setParentId( newParentId );
        rdnIdx.add( movedEntry, entryId );

        updateRdnIdx( newParentId, ADD_CHILD, movedEntry.getNbDescendants() );

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
        Dn aliasTarget = aliasIdx.reverseLookup( entryId );

        if ( null != aliasTarget )
        {
            aliasTarget.apply( schemaManager );
            addAliasIndices( entryId, buildEntryDn( entryId ), aliasTarget );
        }

        // the below case arises only when the move( Dn oldDn, Dn newSuperiorDn, Dn newDn  ) is called
        // directly using the Store API, in this case the value of modified entry will be null
        // we need to lookup the entry to update the parent UUID
        if ( modifiedEntry == null )
        {
            modifiedEntry = fetch( entryId );
        }

        // Update the master table with the modified entry
        modifiedEntry.put( SchemaConstants.ENTRY_PARENT_ID_AT, newParentId );

        // Remove the EntryDN
        modifiedEntry.removeAttributes( ENTRY_DN_AT );

        master.put( entryId, modifiedEntry );

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
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        if ( moveAndRenameContext.getNewSuperiorDn().isDescendantOf( moveAndRenameContext.getDn() ) )
        {
            throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM,
                "cannot place an entry below itself" );
        }

        try
        {
            setRWLock( moveAndRenameContext );
            Dn oldDn = moveAndRenameContext.getDn();
            Dn newSuperiorDn = moveAndRenameContext.getNewSuperiorDn();
            Rdn newRdn = moveAndRenameContext.getNewRdn();
            boolean deleteOldRdn = moveAndRenameContext.getDeleteOldRdn();
            Entry modifiedEntry = moveAndRenameContext.getModifiedEntry();

            moveAndRename( oldDn, newSuperiorDn, newRdn, modifiedEntry, deleteOldRdn );
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
     * {@inheritDoc}
     */
    public synchronized final void moveAndRename( Dn oldDn, Dn newSuperiorDn, Rdn newRdn,
        Entry modifiedEntry,
        boolean deleteOldRdn ) throws Exception
    {
        // Check that the old entry exists
        String oldId = getEntryId( oldDn );

        if ( oldId == null )
        {
            // This is not allowed : the old entry must exist
            LdapNoSuchObjectException nse = new LdapNoSuchObjectException(
                I18n.err( I18n.ERR_256_NO_SUCH_OBJECT, oldDn ) );
            throw nse;
        }

        // Check that the new superior exist
        String newSuperiorId = getEntryId( newSuperiorDn );

        if ( newSuperiorId == null )
        {
            // This is not allowed : the new superior must exist
            LdapNoSuchObjectException nse = new LdapNoSuchObjectException(
                I18n.err( I18n.ERR_256_NO_SUCH_OBJECT, newSuperiorDn ) );
            throw nse;
        }

        Dn newDn = newSuperiorDn.add( newRdn );

        // Now check that the new entry does not exist
        String newId = getEntryId( newDn );

        if ( newId != null )
        {
            // This is not allowed : we should not be able to move an entry
            // to an existing position
            LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, newSuperiorDn.getName() ) );
            throw ne;
        }

        // First, rename
        // Get the old UUID
        if ( modifiedEntry == null )
        {
            modifiedEntry = master.get( oldId );
        }

        rename( oldId, newRdn, deleteOldRdn, modifiedEntry );
        moveAndRename( oldDn, oldId, newSuperiorDn, newRdn, modifiedEntry );

        if ( isSyncOnWrite.get() )
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
     * @param oldDn the normalized dn of the child to be moved
     * @param childId the id of the child being moved
     * @param newRdn the normalized dn of the new parent for the child
     * @param modifiedEntry the modified entry
     * @throws Exception if something goes wrong
     */
    private void moveAndRename( Dn oldDn, String entryId, Dn newSuperior, Rdn newRdn,
        Entry modifiedEntry )
        throws Exception
    {
        // Get the child and the new parent to be entries and Ids
        String newParentId = getEntryId( newSuperior );
        String oldParentId = getParentId( entryId );

        /*
         * All aliases including and below oldChildDn, will be affected by
         * the move operation with respect to one and subtree userIndices since
         * their relationship to ancestors above oldChildDn will be
         * destroyed.  For each alias below and including oldChildDn we will
         * drop the index tuples mapping ancestor ids above oldChildDn to the
         * respective target ids of the aliases.
         */
        dropMovedAliasIndices( oldDn );

        /*
         * Update the Rdn index
         */
        // First drop the old entry
        ParentIdAndRdn movedEntry = rdnIdx.reverseLookup( entryId );

        updateRdnIdx( oldParentId, REMOVE_CHILD, movedEntry.getNbDescendants() );

        rdnIdx.drop( entryId );

        // Now, add the new entry at the right position
        movedEntry.setParentId( newParentId );
        movedEntry.setRdns( new Rdn[]
            { newRdn } );
        rdnIdx.add( movedEntry, entryId );

        updateRdnIdx( newParentId, ADD_CHILD, movedEntry.getNbDescendants() );

        dumpRdnIdx();

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
        Dn aliasTarget = aliasIdx.reverseLookup( entryId );

        if ( null != aliasTarget )
        {
            aliasTarget.apply( schemaManager );
            addAliasIndices( entryId, buildEntryDn( entryId ), aliasTarget );
        }
    }


    //---------------------------------------------------------------------------------------------
    // The Rename operation
    //---------------------------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        try
        {
            setRWLock( renameContext );
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

            updateCache( renameContext );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    private void rename( String oldId, Rdn newRdn, boolean deleteOldRdn, Entry entry ) throws Exception
    {
        if ( entry == null )
        {
            entry = master.get( oldId );
        }

        Dn updn = entry.getDn();

        newRdn.apply( schemaManager );

        /*
         * H A N D L E   N E W   R D N
         * ====================================================================
         * Add the new Rdn attribute to the entry.  If an index exists on the
         * new Rdn attribute we add the index for this attribute value pair.
         * Also we make sure that the presence index shows the existence of the
         * new Rdn attribute within this entry.
         */
        for ( Ava newAtav : newRdn )
        {
            String newNormType = newAtav.getNormType();
            Object newNormValue = newAtav.getNormValue().getValue();

            AttributeType newRdnAttrType = schemaManager.lookupAttributeTypeRegistry( newNormType );

            entry.add( newRdnAttrType, newAtav.getValue() );

            if ( hasUserIndexOn( newRdnAttrType ) )
            {
                Index<?, Entry, String> index = getUserIndex( newRdnAttrType );
                ( ( Index ) index ).add( newNormValue, oldId );

                // Make sure the altered entry shows the existence of the new attrib
                if ( !presenceIdx.forward( newNormType, oldId ) )
                {
                    presenceIdx.add( newNormType, oldId );
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
                    String oldNormValue = oldAtav.getNormValue().getString();
                    AttributeType oldRdnAttrType = schemaManager.lookupAttributeTypeRegistry( oldNormType );
                    entry.remove( oldRdnAttrType, oldNormValue );

                    if ( hasUserIndexOn( oldRdnAttrType ) )
                    {
                        Index<?, Entry, String> index = getUserIndex( oldRdnAttrType );
                        ( ( Index ) index ).drop( oldNormValue, id );

                        /*
                         * If there is no value for id in this index due to our
                         * drop above we remove the oldRdnAttr from the presence idx
                         */
                        if ( null == index.reverseLookup( oldId ) )
                        {
                            presenceIdx.drop( oldNormType, oldId );
                        }
                    }
                }
            }
        }

        // Remove the EntryDN
        entry.removeAttributes( ENTRY_DN_AT );

        // And save the modified entry
        master.put( oldId, entry );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public synchronized final void rename( Dn dn, Rdn newRdn, boolean deleteOldRdn, Entry entry ) throws Exception
    {
        String oldId = getEntryId( dn );

        rename( oldId, newRdn, deleteOldRdn, entry );

        /*
         * H A N D L E   D N   C H A N G E
         * ====================================================================
         * We only need to update the Rdn index.
         * No need to calculate the new Dn.
         */
        String parentId = getParentId( oldId );

        // Get the old parentIdAndRdn to get the nb of children and descendant
        ParentIdAndRdn parentIdAndRdn = rdnIdx.reverseLookup( oldId );

        // Now we can drop it
        rdnIdx.drop( oldId );

        // Update the descendants
        parentIdAndRdn.setParentId( parentId );
        parentIdAndRdn.setRdns( newRdn );

        rdnIdx.add( parentIdAndRdn, oldId );

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
    public final void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        // does nothing
    }


    /**
     * This method calls {@link Partition#lookup(LookupOperationContext)} and return <tt>true</tt>
     * if it returns an entry by default.  Please override this method if
     * there is more effective way for your implementation.
     */
    public boolean hasEntry( HasEntryOperationContext entryContext ) throws LdapException
    {
        try
        {
            setRWLock( entryContext );

            String id = getEntryId( entryContext.getDn() );

            Entry entry = fetch( id, entryContext.getDn() );

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
     * @param entry the entry having entryCSN attribute
     * @param id UUID of the entry
     * @throws Exception
     */
    private void updateCsnIndex( Entry entry, String id ) throws Exception
    {
        String entryCsn = entry.get( SchemaConstants.ENTRY_CSN_AT ).getString();
        entryCsnIdx.drop( id );
        entryCsnIdx.add( entryCsn, id );
    }


    // ------------------------------------------------------------------------
    // Index and master table Operations
    // ------------------------------------------------------------------------
    /**
     * builds the Dn of the entry identified by the given id
     *
     * @param id the entry's id
     * @return the normalized Dn of the entry
     * @throws Exception
     */
    protected Dn buildEntryDn( String id ) throws Exception
    {
        String parentId = id;
        String rootId = Partition.ROOT_ID;

        // Create an array of 10 rdns, just in case. We will extend it if needed
        Rdn[] rdnArray = new Rdn[10];
        int pos = 0;

        try
        {
            rwLock.readLock().lock();

            do
            {
                ParentIdAndRdn cur = rdnIdx.reverseLookup( parentId );

                if ( cur == null )
                {
                    return null;
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
        }
        finally
        {
            rwLock.readLock().unlock();
        }

        Dn dn = new Dn( schemaManager, Arrays.copyOf( rdnArray, pos ) );

        return dn;
    }


    /**
     * {@inheritDoc}
     */
    public int count() throws Exception
    {
        return master.count();
    }


    /**
     * {@inheritDoc}
     */
    public final int getChildCount( String id ) throws LdapException
    {
        try
        {
            ParentIdAndRdn parentIdAndRdn = rdnIdx.reverseLookup( id );

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
    public final Dn getEntryDn( String id ) throws Exception
    {
        return buildEntryDn( id );
    }


    /**
     * {@inheritDoc}
     */
    public final String getEntryId( Dn dn ) throws LdapException
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
                String currentId = rdnIdx.forwardLookup( suffixKey );

                for ( int i = dn.size() - suffixDn.size(); i > 0; i-- )
                {
                    Rdn rdn = dn.getRdn( i - 1 );
                    ParentIdAndRdn currentRdn = new ParentIdAndRdn( currentId, rdn );
                    currentId = rdnIdx.forwardLookup( currentRdn );

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
    public String getParentId( String childId ) throws Exception
    {
        try
        {
            rwLock.readLock().lock();
            ParentIdAndRdn key = rdnIdx.reverseLookup( childId );

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
     */
    protected String getSuffixId() throws Exception
    {
        if ( suffixId == null )
        {
            ParentIdAndRdn key = new ParentIdAndRdn( Partition.ROOT_ID, suffixDn.getRdns() );

            try
            {
                rwLock.readLock().lock();
                suffixId = rdnIdx.forwardLookup( key );
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
    public void addIndex( Index<?, Entry, String> index ) throws Exception
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
    public void addIndexedAttributes( Index<?, Entry, String>... indexes )
    {
        for ( Index<?, Entry, String> index : indexes )
        {
            indexedAttributes.add( index );
        }
    }


    /**
     * Set the list of indexes for this partition
     * @param indexedAttributes The list of indexes
     */
    public void setIndexedAttributes( Set<Index<?, ?, String>> indexedAttributes )
    {
        this.indexedAttributes = indexedAttributes;
    }


    /**
     * @return The list of indexed attributes
     */
    public Set<Index<?, ?, String>> getIndexedAttributes()
    {
        return indexedAttributes;
    }


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
    public Index<?, Entry, String> getIndex( AttributeType attributeType ) throws IndexNotFoundException
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
    public Index<?, Entry, String> getUserIndex( AttributeType attributeType ) throws IndexNotFoundException
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
    public Index<?, Entry, String> getSystemIndex( AttributeType attributeType ) throws IndexNotFoundException
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
    public Index<Dn, Entry, String> getAliasIndex()
    {
        return ( Index<Dn, Entry, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, Entry, String> getOneAliasIndex()
    {
        return ( Index<String, Entry, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, Entry, String> getSubAliasIndex()
    {
        return ( Index<String, Entry, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, Entry, String> getObjectClassIndex()
    {
        return ( Index<String, Entry, String> ) systemIndices.get( SchemaConstants.OBJECT_CLASS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, Entry, String> getEntryCsnIndex()
    {
        return ( Index<String, Entry, String> ) systemIndices.get( SchemaConstants.ENTRY_CSN_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, Entry, String> getAdministrativeRoleIndex()
    {
        return ( Index<String, Entry, String> ) systemIndices.get( SchemaConstants.ADMINISTRATIVE_ROLE_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, Entry, String> getPresenceIndex()
    {
        return ( Index<String, Entry, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<ParentIdAndRdn, Entry, String> getRdnIndex()
    {
        return ( Index<ParentIdAndRdn, Entry, String> ) systemIndices.get( ApacheSchemaConstants.APACHE_RDN_AT_OID );
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
    protected void addAliasIndices( String aliasId, Dn aliasDn, Dn aliasTarget ) throws Exception
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
            LdapAliasDereferencingException e = new LdapAliasDereferencingException( msg );
            //e.setResolvedName( aliasDn );
            throw e;
        }

        // L O O K U P   T A R G E T   I D
        targetId = getEntryId( aliasTarget );

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
        aliasIdx.add( aliasTarget, aliasId );

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
        Dn normalizedAliasTargetParentDn = aliasTarget.getParent();

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
            if ( !aliasTarget.isDescendantOf( ancestorDn ) )
            {
                subAliasIdx.add( ancestorId, targetId );
            }

            ancestorDn = ancestorDn.getParent();
            ancestorId = getEntryId( ancestorDn );
        }
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
    protected void dropAliasIndices( String aliasId ) throws Exception
    {
        Dn targetDn = aliasIdx.reverseLookup( aliasId );
        targetDn.apply( schemaManager );
        String targetId = getEntryId( targetDn );

        if ( targetId == null )
        {
            // the entry doesn't exist, probably it has been deleted or renamed
            // TODO: this is just a workaround for now, the alias indices should be updated when target entry is deleted or removed
            return;
        }

        Dn aliasDn = getEntryDn( aliasId );

        Dn ancestorDn = aliasDn.getParent();
        String ancestorId = getEntryId( ancestorDn );

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

        while ( !ancestorDn.equals( suffixDn ) && ancestorDn.size() > suffixDn.size() )
        {
            ancestorDn = ancestorDn.getParent();
            ancestorId = getEntryId( ancestorDn );

            subAliasIdx.drop( ancestorId, targetId );
        }

        // Drops all alias tuples pointing to the id of the alias to be deleted
        aliasIdx.drop( aliasId );
    }


    /**
     * For all aliases including and under the moved base, this method removes
     * one and subtree alias index tuples for old ancestors above the moved base
     * that will no longer be ancestors after the move.
     *
     * @param movedBase the base at which the move occurred - the moved node
     * @throws Exception if system userIndices fail
     */
    protected void dropMovedAliasIndices( final Dn movedBase ) throws Exception
    {
        String movedBaseId = getEntryId( movedBase );

        if ( aliasIdx.reverseLookup( movedBaseId ) != null )
        {
            dropAliasIndices( movedBaseId, movedBase );
        }
    }


    /**
     * For the alias id all ancestor one and subtree alias tuples are moved
     * above the moved base.
     *
     * @param aliasId the id of the alias
     * @param movedBase the base where the move occurred
     * @throws Exception if userIndices fail
     */
    protected void dropAliasIndices( String aliasId, Dn movedBase ) throws Exception
    {
        Dn targetDn = aliasIdx.reverseLookup( aliasId );
        targetDn.apply( schemaManager );
        String targetId = getEntryId( targetDn );
        Dn aliasDn = getEntryDn( aliasId );

        /*
         * Start droping index tuples with the first ancestor right above the
         * moved base.  This is the first ancestor effected by the move.
         */
        Dn ancestorDn = new Dn( schemaManager, movedBase.getRdn( movedBase.size() - 1 ) );
        String ancestorId = getEntryId( ancestorDn );

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
            oneAliasIdx.drop( ancestorId, targetId );
        }

        subAliasIdx.drop( ancestorId, targetId );

        while ( !ancestorDn.equals( suffixDn ) )
        {
            ancestorDn = new Dn( schemaManager, ancestorDn.getRdn( ancestorDn.size() - 1 ) );
            ancestorId = getEntryId( ancestorDn );

            subAliasIdx.drop( ancestorId, targetId );
        }
    }


    //---------------------------------------------------------------------------------------------
    // Debug methods
    //---------------------------------------------------------------------------------------------
    private void dumpIndex( OutputStream stream, Index<?, Entry, String> index )
    {
        try
        {
            Cursor<IndexEntry<?, String>> cursor = ( Cursor ) index.forwardCursor();

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

            if ( attributeType.getOid().equals( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
            {
                dumpIndex( stream, rdnIdx );
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
    public String toString()
    {
        return "Partition<" + id + ">";
    }


    /**
     * Create a new Index for a given OID
     * 
     * @param indexOid The Attribute OID
     * @param path The working directory where this indew will be stored
     * @return The created index
     * @throws Exception If the index can't be created
     */
    protected abstract Index createSystemIndex( String indexOid, URI path, boolean withReverse ) throws Exception;


    /**
     * {@inheritDoc}
     */
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
    public ReadWriteLock getReadWriteLock()
    {
        return rwLock;
    }
}
