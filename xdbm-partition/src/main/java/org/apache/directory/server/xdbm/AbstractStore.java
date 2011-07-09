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
package org.apache.directory.server.xdbm;


import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapAliasDereferencingException;
import org.apache.directory.shared.ldap.model.exception.LdapAliasException;
import org.apache.directory.shared.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Ava;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.MatchingRule;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.exception.MultiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base implementation of a {@link Store}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractStore<E, ID extends Comparable<ID>> implements Store<E, ID>
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractStore.class );

    /** The default cache size is set to 10 000 objects */
    public static final int DEFAULT_CACHE_SIZE = 10000;

    /** Cached attributes types to avoid lookup all over the code */
    protected AttributeType OBJECT_CLASS_AT;
    protected AttributeType ENTRY_CSN_AT;
    protected AttributeType ENTRY_UUID_AT;
    protected AttributeType ALIASED_OBJECT_NAME_AT;

    /** true if initialized */
    protected boolean initialized;

    /** the partition path to use for files */
    protected URI partitionPath;

    /** true if we sync disks on every write operation */
    protected AtomicBoolean isSyncOnWrite = new AtomicBoolean( true );

    /** The store cache size */
    protected int cacheSize = DEFAULT_CACHE_SIZE;

    /** The store unique identifier */
    protected String id;

    /** The suffix Dn */
    protected Dn suffixDn;

    /** The suffix ID */
    private volatile ID suffixId;

    /** A pointer on the schemaManager */
    protected SchemaManager schemaManager;

    /** the master table storing entries by primary key */
    protected MasterTable<ID, Entry> master;

    /** a map of attributeType numeric ID to user userIndices */
    protected Map<String, Index<?, E, ID>> userIndices = new HashMap<String, Index<?, E, ID>>();

    /** a map of attributeType numeric ID to system userIndices */
    protected Map<String, Index<?, E, ID>> systemIndices = new HashMap<String, Index<?, E, ID>>();

    /** the attribute presence index */
    protected Index<String, E, ID> presenceIdx;

    /** a system index on the entries of descendants of root Dn*/
    protected Index<ID, E, ID> subLevelIdx;

    /** the parent child relationship index */
    protected Index<ID, E, ID> oneLevelIdx;

    /** a system index on aliasedObjectName attribute */
    protected Index<String, E, ID> aliasIdx;

    /** the one level scope alias index */
    protected Index<ID, E, ID> oneAliasIdx;

    /** the subtree scope alias index */
    protected Index<ID, E, ID> subAliasIdx;

    /** a system index on objectClass attribute*/
    protected Index<String, E, ID> objectClassIdx;

    /** a system index on entryUUID attribute */
    protected Index<String, E, ID> entryUuidIdx;

    /** a system index on entryCSN attribute */
    protected Index<String, E, ID> entryCsnIdx;

    /** the relative distinguished name index */
    protected Index<ParentIdAndRdn<ID>, E, ID> rdnIdx;

    /**
     * a flag to enable/disable hasEntry() check before adding the entry
     * Note: This kind of check is already present in ExceptionInterceptor's
     * add() method. This flag needs to be enabled only in cases where interceptor chain
     * is not used or not yet effective at the time of adding entries into this store.
     */
    private boolean checkHasEntryDuringAdd = false;

    /**
     * {@inheritDoc}
     */
    public void init( SchemaManager schemaManager ) throws Exception
    {
        this.schemaManager = schemaManager;

        // Initialize Attribute types used all over this method
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        ALIASED_OBJECT_NAME_AT = schemaManager.getAttributeType( SchemaConstants.ALIASED_OBJECT_NAME_AT );
        ENTRY_CSN_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_CSN_AT );
        ENTRY_UUID_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_UUID_AT );
        
        // -------------------------------------------------------------------
        // Initializes the user and system indices
        // -------------------------------------------------------------------
        setupSystemIndices();
        setupUserIndices();
    }
    

    protected void protect( String property )
    {
        if ( initialized )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_576, property ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isInitialized()
    {
        return initialized;
    }


    public void setPartitionPath( URI partitionPath )
    {
        protect( "partitionPath" );
        this.partitionPath = partitionPath;
    }


    public URI getPartitionPath()
    {
        return partitionPath;
    }


    public void setSyncOnWrite( boolean isSyncOnWrite )
    {
        protect( "syncOnWrite" );
        this.isSyncOnWrite.set( isSyncOnWrite );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isSyncOnWrite()
    {
        return isSyncOnWrite.get();
    }


    public void setCacheSize( int cacheSize )
    {
        protect( "cacheSize" );
        this.cacheSize = cacheSize;
    }


    /**
     * {@inheritDoc}
     */
    public int getCacheSize()
    {
        return cacheSize;
    }


    public void setId( String id )
    {
        protect( "id" );
        this.id = id;
    }


    /**
     * {@inheritDoc}
     */
    public String getId()
    {
        return id;
    }


    public void setSuffixDn( Dn suffixDn )
    {
        protect( "suffixDn" );

        if ( !suffixDn.isSchemaAware() )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_218, suffixDn.getName() ) );
        }

        this.suffixDn = suffixDn;
    }


    /**
     * {@inheritDoc}
     */
    public Dn getSuffixDn()
    {
        return suffixDn;
    }


    //------------------------------------------------------------------------
    // Index handling
    //------------------------------------------------------------------------
    /**
     * Sets up the user indices.
     */
    protected void setupUserIndices() throws Exception
    {
        // convert and initialize system indices
        Map<String, Index<?, E, ID>> tmp = new HashMap<String, Index<?, E, ID>>();

        for ( String oid : userIndices.keySet() )
        {
            // check that the attributeType has an EQUALITY matchingRule
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( oid );
            MatchingRule mr = attributeType.getEquality();

            if ( mr != null )
            {
                Index<?, E, ID> index = userIndices.get( oid );
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
     * Sets up the system indices.
     */
    @SuppressWarnings("unchecked")
    private void setupSystemIndices() throws Exception
    {
        // add missing system indices
        if ( getPresenceIndex() == null )
        {
            Index<String, E, ID> index = new GenericIndex<String, E, ID>( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID ) ;
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getOneLevelIndex() == null )
        {
            Index<ID, E, ID> index = new GenericIndex<ID, E, ID>( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getSubLevelIndex() == null )
        {
            Index<ID, E, ID> index = new GenericIndex<ID, E, ID>( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getRdnIndex() == null )
        {
            Index<ParentIdAndRdn<ID>, E, ID> index = new GenericIndex<ParentIdAndRdn<ID>, E, ID>( ApacheSchemaConstants.APACHE_RDN_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getAliasIndex() == null )
        {
            Index<String, E, ID> index = new GenericIndex<String, E, ID>( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getOneAliasIndex() == null )
        {
            Index<ID, E, ID> index = new GenericIndex<ID, E, ID>( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getSubAliasIndex() == null )
        {
            Index<ID, E, ID> index = new GenericIndex<ID, E, ID>( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getObjectClassIndex() == null )
        {
            Index<String, E, ID> index = new GenericIndex<String, E, ID>( SchemaConstants.OBJECT_CLASS_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getEntryUuidIndex() == null )
        {
            Index<String, E, ID> index = new GenericIndex<String, E, ID>( SchemaConstants.ENTRY_UUID_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        if ( getEntryCsnIndex() == null )
        {
            Index<String, E, ID> index = new GenericIndex<String, E, ID>( SchemaConstants.ENTRY_CSN_AT_OID );
            index.setWkDirPath( partitionPath );
            addIndex( index );
        }

        // convert and initialize system indices
        for ( String oid : systemIndices.keySet() )
        {
            Index<?, E, ID> index = systemIndices.get( oid );
            index = convertAndInit( index );
            systemIndices.put( oid, index );
        }

        // set index shortcuts
        rdnIdx = ( Index<ParentIdAndRdn<ID>, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_RDN_AT_OID );
        presenceIdx = ( Index<String, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
        oneLevelIdx = ( Index<ID, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
        subLevelIdx = ( Index<ID, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );
        aliasIdx = ( Index<String, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
        oneAliasIdx = ( Index<ID, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
        subAliasIdx = ( Index<ID, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
        objectClassIdx = ( Index<String, E, ID> ) systemIndices.get( SchemaConstants.OBJECT_CLASS_AT_OID );
        entryUuidIdx = ( Index<String, E, ID> ) systemIndices.get( SchemaConstants.ENTRY_UUID_AT_OID );
        entryCsnIdx = ( Index<String, E, ID> ) systemIndices.get( SchemaConstants.ENTRY_CSN_AT_OID );
    }


    /**
     * Convert and initialize an index for a specific store implementation.
     *
     * @param index the index
     * @return the converted and initialized index
     * @throws Exception
     */
    protected abstract Index<?, E, ID> convertAndInit( Index<?, E, ID> index ) throws Exception;


    /**
     * {@inheritDoc}
     */
    public Iterator<String> userIndices()
    {
        return userIndices.keySet().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<String> systemIndices()
    {
        return systemIndices.keySet().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasIndexOn( AttributeType attributeType ) throws LdapException
    {
        return hasUserIndexOn( attributeType ) || hasSystemIndexOn( attributeType );
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
    public Set<Index<?, E, ID>> getUserIndices()
    {
        return new HashSet<Index<?, E, ID>>( userIndices.values() );
    }


    /**
     * {@inheritDoc}
     */
    public Index<?, E, ID> getIndex( AttributeType attributeType ) throws IndexNotFoundException
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
    public Index<?, E, ID> getUserIndex( AttributeType attributeType ) throws IndexNotFoundException
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
    public Index<?, E, ID> getSystemIndex( AttributeType attributeType ) throws IndexNotFoundException
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
    public void addIndex( Index<?, E, ID> index ) throws Exception
    {
        protect( "addIndex" );

        // Check that the index ID is valid
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() );

        if ( attributeType == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_309, index.getAttributeId() ) );
        }

        String oid = attributeType.getOid();

        if ( SYS_INDEX_OIDS.contains( oid ) )
        {
            systemIndices.put( oid, index );
        }
        else
        {
            userIndices.put( oid, index );
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<ParentIdAndRdn<ID>, E, ID> getRdnIndex()
    {
        return ( Index<ParentIdAndRdn<ID>, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_RDN_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, E, ID> getPresenceIndex()
    {
        return ( Index<String, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_PRESENCE_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<ID, E, ID> getOneLevelIndex()
    {
        return ( Index<ID, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<ID, E, ID> getSubLevelIndex()
    {
        return ( Index<ID, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, E, ID> getAliasIndex()
    {
        return ( Index<String, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<ID, E, ID> getOneAliasIndex()
    {
        return ( Index<ID, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<ID, E, ID> getSubAliasIndex()
    {
        return ( Index<ID, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, E, ID> getObjectClassIndex()
    {
        return ( Index<String, E, ID> ) systemIndices.get( SchemaConstants.OBJECT_CLASS_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, E, ID> getEntryUuidIndex()
    {
        return ( Index<String, E, ID> ) systemIndices.get( SchemaConstants.ENTRY_UUID_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, E, ID> getEntryCsnIndex()
    {
        return ( Index<String, E, ID> ) systemIndices.get( SchemaConstants.ENTRY_CSN_AT_OID );
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
    public int getChildCount( ID id ) throws Exception
    {
        return oneLevelIdx.count( id );
    }


    //------------------------------------------------------------------------
    // Dn and ID handling
    //------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public ID getEntryId( Dn dn ) throws Exception
    {
        if ( Dn.isNullOrEmpty( dn ) )
        {
            return getRootId();
        }

        ParentIdAndRdn<ID> suffixKey = new ParentIdAndRdn<ID>( getRootId(), suffixDn.getRdns() );

        // Check into the Rdn index, starting with the partition Suffix
        ID currentId = rdnIdx.forwardLookup( suffixKey );

        for ( int i = dn.size() - suffixDn.size(); i > 0; i-- )
        {
            Rdn rdn = dn.getRdn( i - 1 );
            ParentIdAndRdn<ID> currentRdn = new ParentIdAndRdn<ID>( currentId, rdn );
            currentId = rdnIdx.forwardLookup( currentRdn );

            if ( currentId == null )
            {
                break;
            }
        }

        return currentId;
    }


    /**
     * {@inheritDoc}
     */
    public Dn getEntryDn( ID id ) throws Exception
    {
        return buildEntryDn( id );
    }


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


    //------------------------------------------------------------------------
    // Operations
    //------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public Entry lookup( ID id ) throws Exception
    {
        Entry entry = master.get( id );

        if ( entry != null )
        {
            Dn dn = buildEntryDn( id );
            entry.setDn( dn );
            return entry;
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public IndexCursor<ID, E, ID> list( ID id ) throws Exception
    {
        // We use the OneLevl index to get all the entries from a starting point
        // and below
        IndexCursor<ID, E, ID> cursor = oneLevelIdx.forwardCursor( id );
        cursor.beforeValue( id, null );

        return cursor;
    }
    
    
    /**
     * {@inheritDoc}
     * 
     * Adding an entry involves many steps :
     * - first we must check if the entry exists or not
     * - then we must get a new ID for the added entry
     * - update the RDN index
     * - update the oneLevel index
     * - update the subLevel index
     * - update the ObjectClass index
     * - update the entryCsn index
     * - update the entryUuid index
     * - update the user's index
     * 
     * TODO : We should be able to revert all the changes made to index
     * if something went wrong. Also the index should auto-repair : if
     * an entry does not exist in the Master table, then the index must be updated to reflect this.
     */
    @SuppressWarnings("unchecked")
    public synchronized void add( Entry entry ) throws Exception
    {
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
                Index<Object, E, ID> idx = ( Index<Object, E, ID> ) getUserIndex( attributeType );

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


    /**
     * {@inheritDoc}
     */
    public synchronized void modify( Dn dn, ModificationOperation modOp, Entry mods ) throws Exception
    {
        if ( mods instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        ID id = getEntryId( dn );
        Entry entry = master.get( id );

        for ( AttributeType attributeType : mods.getAttributeTypes() )
        {
            Attribute attr = mods.get( attributeType );

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

        updateCsnIndex( entry, id );
        master.put( id, entry );

        if ( isSyncOnWrite.get() )
        {
            sync();
        }
    }


    /**
     * {@inheritDoc}
     */
    public synchronized Entry modify( Dn dn, List<Modification> mods ) throws Exception
    {
        ID id = getEntryId( dn );
        Entry entry = master.get( id );

        for ( Modification mod : mods )
        {
            Attribute attrMods = mod.getAttribute();

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

        updateCsnIndex( entry, id );
        master.put( id, entry );

        if ( isSyncOnWrite.get() )
        {
            sync();
        }

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public synchronized void delete( ID id ) throws Exception
    {
        // First get the entry
        Entry entry = master.get( id );
        
        if ( entry == null )
        {
            // Not allowed
            throw new LdapNoSuchObjectException( "Cannot find an entry for ID " + id );
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

        // Update the rdn, oneLevel, subLevel, entryCsn and entryUuid indexes
        rdnIdx.drop( id );
        oneLevelIdx.drop( id );
        subLevelIdx.drop( id );
        entryCsnIdx.drop( id );
        entryUuidIdx.drop( id );

        // Update the user indexes
        for ( Attribute attribute : entry )
        {
            AttributeType attributeType = attribute.getAttributeType();
            String attributeOid = attributeType.getOid();

            if ( hasUserIndexOn( attributeType ) )
            {
                Index<?, E, ID> index = getUserIndex( attributeType );

                // here lookup by attributeId is ok since we got attributeId from
                // the entry via the enumeration - it's in there as is for sure
                for ( Value<?> value : attribute )
                {
                    ( ( Index ) index ).drop( value.getValue(), id );
                }

                presenceIdx.drop( attributeOid, id );
            }
        }

        master.remove( id );
        
        // if this is a context entry reset the master table counter
        if ( id.equals( getDefaultId() ) )
        {
            master.resetCounter();
        }

        if ( isSyncOnWrite.get() )
        {
            sync();
        }
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public synchronized void rename( Dn dn, Rdn newRdn, boolean deleteOldRdn, Entry entry ) throws Exception
    {
        ID id = getEntryId( dn );

        if ( entry == null )
        {
            entry = lookup( id );
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

            entry.add( newRdnAttrType, newAtav.getNormValue() );

            if ( hasUserIndexOn( newRdnAttrType ) )
            {
                Index<?, E, ID> index = getUserIndex( newRdnAttrType );
                ( ( Index ) index ).add( newNormValue, id );

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
                        Index<?, E, ID> index = getUserIndex( oldRdnAttrType );
                        ( ( Index ) index ).drop( oldNormValue, id );

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
         * We only need to update the Rdn index.
         * No need to calculate the new Dn.
         */

        ID parentId = getParentId( id );
        rdnIdx.drop( id );
        ParentIdAndRdn<ID> key = new ParentIdAndRdn<ID>( parentId, newRdn );
        rdnIdx.add( key, id );

        master.put( id, entry );

        if ( isSyncOnWrite.get() )
        {
            sync();
        }
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void moveAndRename( Dn oldDn, Dn newSuperiorDn, Rdn newRdn, Entry modifiedEntry, boolean deleteOldRdn ) throws Exception
    {
    	// Check that the old entry exists
        ID oldId = getEntryId( oldDn );

        if ( oldId == null )
        {
            // This is not allowed : the old entry must exist
        	LdapNoSuchObjectException nse = new LdapNoSuchObjectException(
                I18n.err( I18n.ERR_256_NO_SUCH_OBJECT, oldDn ) );
            throw nse;
        }

        // Check that the new superior exist
        ID newSuperiorId = getEntryId( newSuperiorDn );

        if ( newSuperiorId == null )
        {
            // This is not allowed : the new superior must exist
        	LdapNoSuchObjectException nse = new LdapNoSuchObjectException(
                I18n.err( I18n.ERR_256_NO_SUCH_OBJECT, newSuperiorDn ) );
            throw nse;
        }

        Dn newDn = newSuperiorDn.add( newRdn );

        // Now check that the new entry does not exist
        ID newId = getEntryId( newDn );

        if ( newId != null )
        {
            // This is not allowed : we should not be able to move an entry
            // to an existing position
            LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, newSuperiorDn.getName() ) );
            throw ne;
        }

        rename( oldDn, newRdn, deleteOldRdn, modifiedEntry );
        moveAndRename( oldDn, oldId, newSuperiorDn, newRdn, modifiedEntry );

        if ( isSyncOnWrite.get() )
        {
            sync();
        }
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void move( Dn oldDn, Dn newSuperiorDn, Dn newDn, Entry modifiedEntry ) throws Exception
    {
        // Check that the parent Dn exists
        ID newParentId = getEntryId( newSuperiorDn );

        if ( newParentId == null )
        {
            // This is not allowed : the parent must exist
            LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_256_NO_SUCH_OBJECT, newSuperiorDn.getName() ) );
            throw ne;
        }

        // Now check that the new entry does not exist
        ID newId = getEntryId( newDn );

        if ( newId != null )
        {
            // This is not allowed : we should not be able to move an entry
            // to an existing position
            LdapEntryAlreadyExistsException ne = new LdapEntryAlreadyExistsException(
                I18n.err( I18n.ERR_250_ENTRY_ALREADY_EXISTS, newSuperiorDn.getName() ) );
            throw ne;
        }

        // Get the entry and the old parent IDs
        ID entryId = getEntryId( oldDn );
        ID oldParentId = getParentId( entryId );

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
         * Drop the old parent child relationship and add the new one
         * Set the new parent id for the child replacing the old parent id
         */
        oneLevelIdx.drop( oldParentId, entryId );
        oneLevelIdx.add( newParentId, entryId );

        updateSubLevelIndex( entryId, oldParentId, newParentId );

        // Update the Rdn index
        rdnIdx.drop( entryId );
        ParentIdAndRdn<ID> key = new ParentIdAndRdn<ID>( newParentId, oldDn.getRdn() );
        rdnIdx.add( key, entryId );


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
        String aliasTarget = aliasIdx.reverseLookup( entryId );

        if ( null != aliasTarget )
        {
            addAliasIndices( entryId, buildEntryDn( entryId ), aliasTarget );
        }

        // the below case arises only when the move( Dn oldDn, Dn newSuperiorDn, Dn newDn  ) is called
        // directly using the Store API, in this case the value of modified entry will be null
        // we need to lookup the entry to update the parent ID
        if ( modifiedEntry == null )
        {
            modifiedEntry = lookup( entryId );
        }
        
        // Update the master table with the modified entry
        modifiedEntry.put( SchemaConstants.ENTRY_PARENT_ID_AT, newParentId.toString() );
        master.put( entryId, modifiedEntry );

        if ( isSyncOnWrite.get() )
        {
            sync();
        }
    }


    //------------------------------------------------------------------------
    // Helpers
    //------------------------------------------------------------------------

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
    private void add( ID id, Entry entry, Attribute mods ) throws Exception
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
                objectClassIdx.add( value.getString(), id );
            }
        }
        else if ( hasUserIndexOn( attributeType ) )
        {
            Index<?, E, ID> index = getUserIndex( attributeType );

            for ( Value<?> value : mods )
            {
                ( ( Index ) index ).add( value.getValue(), id );
            }

            // If the attr didn't exist for this id add it to presence index
            if ( !presenceIdx.forward( modsOid, id ) )
            {
                presenceIdx.add( modsOid, id );
            }
        }

        // add all the values in mods to the same attribute in the entry

        for ( Value<?> value : mods )
        {
            entry.add( mods.getAttributeType(), value );
        }

        if ( modsOid.equals( SchemaConstants.ALIASED_OBJECT_NAME_AT_OID ) )
        {
            Dn ndn = getEntryDn( id );
            addAliasIndices( id, ndn, mods.getString() );
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
    private void replace( ID id, Entry entry, Attribute mods ) throws Exception
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
            if ( objectClassIdx.reverse( id ) )
            {
                objectClassIdx.drop( id );
            }

            for ( Value<?> value : mods )
            {
                objectClassIdx.add( value.getString(), id );
            }
        }
        else if ( hasUserIndexOn( attributeType ) )
        {
            Index<?, E, ID> index = getUserIndex( attributeType );

            // if the id exists in the index drop all existing attribute
            // value index entries and add new ones
            if ( index.reverse( id ) )
            {
                ( ( Index<?, E, ID> ) index ).drop( id );
            }

            for ( Value<?> value : mods )
            {
                ( ( Index<Object, E, ID> ) index ).add( value.getValue(), id );
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
            addAliasIndices( id, entryDn, mods.getString() );
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
    private void remove( ID id, Entry entry, Attribute mods ) throws Exception
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
                objectClassIdx.drop( id );
            }
            else
            {
                for ( Value<?> value : mods )
                {
                    objectClassIdx.drop( value.getString(), id );
                }
            }
        }
        else if ( hasUserIndexOn( attributeType ) )
        {
            Index<?, E, ID> index = getUserIndex( attributeType );

            /*
             * If there are no attribute values in the modifications then this
             * implies the complete removal of the attribute from the index. Else
             * we remove individual tuples from the index.
             */
            if ( mods.size() == 0 )
            {
                ( ( Index ) index ).drop( id );
            }
            else
            {
                for ( Value<?> value : mods )
                {
                    ( ( Index ) index ).drop( value.getValue(), id );
                }
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


    /////////////////////////////////////////////////////////

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


    /**
     * Removes the index entries for an alias before the entry is deleted from
     * the master table.
     *
     * @todo Optimize this by walking the hierarchy index instead of the name
     * @param aliasId the id of the alias entry in the master table
     * @throws LdapException if we cannot parse ldap names
     * @throws Exception if we cannot delete index values in the database
     */
    protected void dropAliasIndices( ID aliasId ) throws Exception
    {
        String targetDn = aliasIdx.reverseLookup( aliasId );
        ID targetId = getEntryId( new Dn( schemaManager, targetDn ) );

        if ( targetId == null )
        {
            // the entry doesn't exist, probably it has been deleted or renamed
            // TODO: this is just a workaround for now, the alias indices should be updated when target entry is deleted or removed
            return;
        }

        Dn aliasDn = getEntryDn( aliasId );

        Dn ancestorDn = aliasDn.getParent();
        ID ancestorId = getEntryId( ancestorDn );

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
     * @param movedBase the base at which the move occured - the moved node
     * @throws Exception if system userIndices fail
     */
    protected void dropMovedAliasIndices( final Dn movedBase ) throws Exception
    {
        ID movedBaseId = getEntryId( movedBase );

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
     * @param movedBase the base where the move occured
     * @throws Exception if userIndices fail
     */
    protected void dropAliasIndices( ID aliasId, Dn movedBase ) throws Exception
    {
        String targetDn = aliasIdx.reverseLookup( aliasId );
        ID targetId = getEntryId( new Dn( schemaManager, targetDn ) );
        Dn aliasDn = getEntryDn( aliasId );

        /*
         * Start droping index tuples with the first ancestor right above the
         * moved base.  This is the first ancestor effected by the move.
         */
        Dn ancestorDn = new Dn( schemaManager, movedBase.getRdn( movedBase.size() - 1 ) );
        ID ancestorId = getEntryId( ancestorDn );

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
    protected void moveAndRename( Dn oldDn, ID childId, Dn newSuperior, Rdn newRdn, Entry modifiedEntry ) throws Exception
    {
        // Get the child and the new parent to be entries and Ids
        ID newParentId = getEntryId( newSuperior );
        ID oldParentId = getParentId( childId );

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
         * Drop the old parent child relationship and add the new one
         * Set the new parent id for the child replacing the old parent id
         */
        oneLevelIdx.drop( oldParentId, childId );
        oneLevelIdx.add( newParentId, childId );

        updateSubLevelIndex( childId, oldParentId, newParentId );

        /*
         * Update the Rdn index
         */
        rdnIdx.drop( childId );
        ParentIdAndRdn<ID> key = new ParentIdAndRdn<ID>( newParentId, newRdn );
        rdnIdx.add( key, childId );

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
        String aliasTarget = aliasIdx.reverseLookup( childId );

        if ( null != aliasTarget )
        {
            addAliasIndices( childId, buildEntryDn( childId ), aliasTarget );
        }

        // Update the master table with the modified entry
        // Warning : this test is an hack. As we may call the Store API directly
        // we may not have a modified entry to update. For instance, if the ModifierName
        // or ModifyTimeStamp AT are not updated, there is no reason we want to update the
        // master table.
        if ( modifiedEntry != null )
        {
            modifiedEntry.put( SchemaConstants.ENTRY_PARENT_ID_AT, newParentId.toString() );
            master.put( childId, modifiedEntry );
        }
    }


    /**
     * Updates the SubLevel Index as part of a move operation.
     *
     * @param entryId child id to be moved
     * @param oldParentId old parent's id
     * @param newParentId new parent's id
     * @throws Exception
     */
    protected void updateSubLevelIndex( ID entryId, ID oldParentId, ID newParentId ) throws Exception
    {
        ID tempId = oldParentId;
        List<ID> parentIds = new ArrayList<ID>();

        // find all the parents of the oldParentId
        while ( ( tempId != null ) && !tempId.equals( getRootId() ) && !tempId.equals( getSuffixId() ) )
        {
            parentIds.add( tempId );
            tempId = getParentId( tempId );
        }

        // find all the children of the childId
        Cursor<IndexEntry<ID, E, ID>> cursor = subLevelIdx.forwardCursor( entryId );

        List<ID> childIds = new ArrayList<ID>();
        childIds.add( entryId );

        while ( cursor.next() )
        {
            childIds.add( cursor.get().getId() );
        }

        // detach the childId and all its children from oldParentId and all it parents excluding the root
        for ( ID pid : parentIds )
        {
            for ( ID cid : childIds )
            {
                subLevelIdx.drop( pid, cid );
            }
        }

        parentIds.clear();
        tempId = newParentId;

        // find all the parents of the newParentId
        while ( ( tempId != null)  && !tempId.equals( getRootId() ) && !tempId.equals( getSuffixId() ) )
        {
            parentIds.add( tempId );
            tempId = getParentId( tempId );
        }

        // attach the childId and all its children to newParentId and all it parents excluding the root
        for ( ID id : parentIds )
        {
            for ( ID cid : childIds )
            {
                subLevelIdx.add( id, cid );
            }
        }
    }


    /**
     * updates the CSN index
     *
     * @param entry the entry having entryCSN attribute
     * @param id ID of the entry
     * @throws Exception
     */
    private void updateCsnIndex( Entry entry, ID id ) throws Exception
    {
        entryCsnIdx.drop( id );
        entryCsnIdx.add( entry.get( SchemaConstants.ENTRY_CSN_AT ).getString(), id );
    }

    
    /**
     * set the flag to nable/disable checking of entry existence before actually adding it
     * @param checkHasEntryDuringAdd
     */
    public void setCheckHasEntryDuringAdd( boolean checkHasEntryDuringAdd )
    {
        this.checkHasEntryDuringAdd = checkHasEntryDuringAdd;
    }


    /**
     * @return the schemaManager
     */
    public SchemaManager getSchemaManager()
    {
        return schemaManager;
    }


    /**
     * @param schemaManager the schemaManager to set
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }

    
    /**
     * {@inheritDoc}
     */
    public void destroy() throws LdapException, Exception
    {
        LOG.debug( "destroy() called on store for {}", this.suffixDn );

        if ( !initialized )
        {
            return;
        }

        // don't reset initialized flag
        initialized = false;

        MultiException errors = new MultiException( I18n.err( I18n.ERR_577 ) );

        for ( Index<?, E, ID> index : userIndices.values() )
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

        for ( Index<?, E, ID> index : systemIndices.values() )
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
     * Retrieve the SuffixID
     */
    private ID getSuffixId() throws Exception
    {
        if ( suffixId == null )
        {
            ParentIdAndRdn<ID> key = new ParentIdAndRdn<ID>( getRootId(), suffixDn.getRdns() );
            
            suffixId = rdnIdx.forwardLookup( key );
        }

        return suffixId;
    }
}
