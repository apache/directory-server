/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.partition.avl;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.partition.impl.btree.LongComparator;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
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


/**
 * A Store implementation backed by in memory AVL trees.
 * 
 * TODO - this class is extremely like the JdbmStore implementation of the
 * Store interface which tells us that it's best for us to have some kind 
 * of abstract class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlStore<E> implements Store<E, Long>
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( AvlStore.class );

    /** Two static declaration to avoid lookup all over the code */
    private static AttributeType OBJECT_CLASS_AT;
    private static AttributeType ALIASED_OBJECT_NAME_AT;

    /** the master table storing entries by primary key */
    private AvlMasterTable<ServerEntry> master;

    /** the normalized distinguished name index */
    private AvlIndex<String, E> ndnIdx;

    /** the user provided distinguished name index */
    private AvlIndex<String, E> updnIdx;

    /** the attribute existence index */
    private AvlIndex<String, E> existenceIdx;

    /** a system index on aliasedObjectName attribute */
    private AvlIndex<String, E> aliasIdx;

    /** a system index on the entries of descendants of root DN*/
    private AvlIndex<Long, E> subLevelIdx;

    /** the parent child relationship index */
    private AvlIndex<Long, E> oneLevelIdx;

    /** the one level scope alias index */
    private AvlIndex<Long, E> oneAliasIdx;

    /** the subtree scope alias index */
    private AvlIndex<Long, E> subAliasIdx;

    /** a system index on objectClass attribute*/
    private AvlIndex<String, E> objectClassIdx;

    /** a system index on entryCSN attribute */
    private AvlIndex<String, E> entryCsnIdx;

    /** a system index on entryUUID attribute */
    private AvlIndex<String, E> entryUuidIdx;

    /** a map of attributeType numeric ID to user userIndices */
    private Map<String, AvlIndex<? extends Object, E>> userIndices = new HashMap<String, AvlIndex<? extends Object, E>>();

    /** a map of attributeType numeric ID to system userIndices */
    private Map<String, AvlIndex<? extends Object, E>> systemIndices = new HashMap<String, AvlIndex<? extends Object, E>>();

    /** true if initialized */
    private boolean initialized;

    /** A pointer on the schemaManager */
    private SchemaManager schemaManager;

    /** 
     * TODO we need to check out why we have so many suffix 
     * dn and string accessor/mutators on both Store and Partition
     * interfaces.  I think a lot of this comes from the fact 
     * that we implemented DN to have both the up and norm
     * names.
     */
    private DN suffixDn;

    private String name;


    /**
     * {@inheritDoc}
     */
    public void add( ServerEntry entry ) throws Exception
    {
        if ( entry instanceof ClonedServerEntry )
        {
            throw new Exception( I18n.err( I18n.ERR_215 ) );
        }

        DN normName = entry.getDn();

        Long id;
        Long parentId;

        id = master.getNextId();

        //
        // Suffix entry cannot have a parent since it is the root so it is 
        // capped off using the zero value which no entry can have since 
        // entry sequences start at 1.
        //

        DN parentDn = null;

        if ( normName.getNormName().equals( suffixDn.getNormName() ) )
        {
            parentId = 0L;
        }
        else
        {
            parentDn = ( DN ) normName.clone();
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
            String msg = I18n.err( I18n.ERR_217, normName.getName(), entry );
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
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
            addAliasIndices( id, normName, aliasAttr.getString() );
        }

        if ( !Character.isDigit( normName.getNormName().charAt( 0 ) ) )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_218, normName.getNormName() ) );
        }

        ndnIdx.add( normName.getNormName(), id );
        updnIdx.add( normName.getName(), id );
        oneLevelIdx.add( parentId, id );

        // Update the EntryCsn index
        EntryAttribute entryCsn = entry.get( SchemaConstants.ENTRY_CSN_AT );

        if ( entryCsn == null )
        {
            String msg = I18n.err( I18n.ERR_219, normName.getName(), entry );
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
        }

        entryCsnIdx.add( entryCsn.getString(), id );

        // Update the EntryUuid index
        EntryAttribute entryUuid = entry.get( SchemaConstants.ENTRY_UUID_AT );

        if ( entryUuid == null )
        {
            String msg = I18n.err( I18n.ERR_220, normName.getName(), entry );
            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, msg );
        }

        entryUuidIdx.add( entryUuid.getString(), id );

        Long tempId = parentId;
        while ( tempId != null && tempId != 0 && tempId != 1 )
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
                existenceIdx.add( attributeOid, id );
            }
        }

        master.put( id, entry );
    }


    /**
     * {@inheritDoc}
     */
    public void addIndex( Index<? extends Object, E, Long> index ) throws Exception
    {
        if ( index instanceof AvlIndex<?, ?> )
        {
            userIndices.put( index.getAttributeId(), ( AvlIndex<? extends Object, E> ) index );
        }
        else
        {
            userIndices.put( index.getAttributeId(), ( AvlIndex<? extends Object, E> ) convert( index ) );
        }
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
    @SuppressWarnings("unchecked")
    public void delete( Long id ) throws Exception
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
                    ( ( AvlIndex ) index ).drop( value.get(), id );
                }

                existenceIdx.drop( attributeOid, id );
            }
        }

        master.delete( id );
    }


    /**
     * {@inheritDoc}
     */
    public void destroy() throws Exception
    {
        // don't reset initialized flag
        //initialized = false;
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
    public int getChildCount( Long id ) throws Exception
    {
        return oneLevelIdx.count( id );
    }


    /**
     * {@inheritDoc}
     */
    public String getEntryDn( Long id ) throws Exception
    {
        return ndnIdx.reverseLookup( id );
    }


    /**
     * {@inheritDoc}
     */
    public Long getEntryId( String dn ) throws Exception
    {
        return ndnIdx.forwardLookup( dn );
    }


    /**
     * {@inheritDoc}
     */
    public String getEntryUpdn( Long id ) throws Exception
    {
        return updnIdx.reverseLookup( id );
    }


    /**
     * {@inheritDoc}
     */
    public String getEntryUpdn( String dn ) throws Exception
    {
        Long id = ndnIdx.forwardLookup( dn );
        return updnIdx.reverseLookup( id );
    }


    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return name;
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
    public Index<Long, E, Long> getOneAliasIndex()
    {
        return oneAliasIdx;
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
    public Long getParentId( String dn ) throws Exception
    {
        Long childId = ndnIdx.forwardLookup( dn );
        return oneLevelIdx.reverseLookup( childId );
    }


    /**
     * {@inheritDoc}
     */
    public Long getParentId( Long childId ) throws Exception
    {
        return oneLevelIdx.reverseLookup( childId );
    }


    /**
     * {@inheritDoc}
     */
    public Index<String, E, Long> getPresenceIndex()
    {
        return existenceIdx;
    }


    /**
     * {@inheritDoc}
     */
    public String getProperty( String propertyName ) throws Exception
    {
        return master.getProperty( propertyName );
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
    public Index<Long, E, Long> getSubLevelIndex()
    {
        return subLevelIdx;
    }


    /**
     * {@inheritDoc}
     */
    public DN getSuffix()
    {
        if ( suffixDn == null )
        {
            return null;
        }

        try
        {
            return new DN( suffixDn.getNormName() );
        }
        catch ( LdapInvalidDnException e )
        {
            // shouldn't happen
            LOG.error( "", e );
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public DN getUpSuffix()
    {
        if ( suffixDn == null )
        {
            return null;
        }

        try
        {
            return new DN( suffixDn.getName() );
        }
        catch ( LdapInvalidDnException e )
        {
            // shouldn't happen
            LOG.error( "", e );
        }

        return null;
    }


    public String getSuffixDn()
    {
        if ( suffixDn == null )
        {
            return null;
        }

        return suffixDn.getName();
    }


    /**
     * {@inheritDoc}
     */
    public Index<?, E, Long> getSystemIndex( String id ) throws IndexNotFoundException
    {
        try
        {
            id = schemaManager.getAttributeTypeRegistry().getOidByName( id );
        }
        catch ( LdapException e )
        {
            LOG.error( I18n.err( I18n.ERR_1, id ), e.getLocalizedMessage() );
            throw new IndexNotFoundException( I18n.err( I18n.ERR_1, id ), id, e );
        }

        if ( systemIndices.containsKey( id ) )
        {
            return systemIndices.get( id );
        }

        throw new IndexNotFoundException( I18n.err( I18n.ERR_2, id, name ) );
    }


    /**
     * {@inheritDoc}
     */
    public Index<?, E, Long> getIndex( String id ) throws IndexNotFoundException
    {
        try
        {
            id = schemaManager.getAttributeTypeRegistry().getOidByName( id );
        }
        catch ( LdapException e )
        {
            LOG.error( I18n.err( I18n.ERR_1, id ), e.getLocalizedMessage() );
            throw new IndexNotFoundException( I18n.err( I18n.ERR_1, id ), id, e );
        }

        if ( userIndices.containsKey( id ) )
        {
            return userIndices.get( id );
        }
        if ( systemIndices.containsKey( id ) )
        {
            return systemIndices.get( id );
        }

        throw new IndexNotFoundException( I18n.err( I18n.ERR_2, id, name ) );
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
    public Index<? extends Object, E, Long> getUserIndex( String id ) throws IndexNotFoundException
    {
        try
        {
            id = schemaManager.getAttributeTypeRegistry().getOidByName( id );
        }
        catch ( LdapException e )
        {
            LOG.error( I18n.err( I18n.ERR_1, id ), e.getLocalizedMessage() );
            throw new IndexNotFoundException( I18n.err( I18n.ERR_1, id ), id, e );
        }

        if ( userIndices.containsKey( id ) )
        {
            return userIndices.get( id );
        }

        throw new IndexNotFoundException( I18n.err( I18n.ERR_3, id, name ) );
    }


    /**
     * {@inheritDoc}
     */
    public Set<Index<? extends Object, E, Long>> getUserIndices()
    {
        return new HashSet<Index<? extends Object, E, Long>>( userIndices.values() );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasIndexOn( String id ) throws Exception
    {
        return hasUserIndexOn( id ) || hasSystemIndexOn( id );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasSystemIndexOn( String id ) throws Exception
    {
        return systemIndices.containsKey( id );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasUserIndexOn( String id ) throws Exception
    {
        return userIndices.containsKey( id );
    }


    /**
     * {@inheritDoc}
     * TODO why this and initRegistries on Store interface ???
     */
    public void init( SchemaManager schemaManager ) throws Exception
    {
        this.schemaManager = schemaManager;

        OBJECT_CLASS_AT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );
        ALIASED_OBJECT_NAME_AT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ALIASED_OBJECT_NAME_AT );

        // Create the master table (the table containing all the entries)
        master = new AvlMasterTable<ServerEntry>( name, new LongComparator(), null, false );

        suffixDn.normalize( schemaManager.getNormalizerMapping() );
        // -------------------------------------------------------------------
        // Initializes the user and system indices
        // -------------------------------------------------------------------

        setupSystemIndices();
        setupUserIndices();

        // We are done !
        initialized = true;
    }


    private void setupSystemIndices() throws Exception
    {
        // let's check and make sure the supplied indices are OK

        if ( ndnIdx == null )
        {
            AttributeType attributeType = schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_N_DN_AT_OID );
            ndnIdx = new AvlIndex<String, E>();
            ndnIdx.setAttributeId( ApacheSchemaConstants.APACHE_N_DN_AT_OID );
            ndnIdx.initialize( attributeType );
            systemIndices.put( ApacheSchemaConstants.APACHE_N_DN_AT_OID, ndnIdx );
        }

        if ( updnIdx == null )
        {
            AttributeType attributeType = schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_UP_DN_AT_OID );
            updnIdx = new AvlIndex<String, E>();
            updnIdx.setAttributeId( ApacheSchemaConstants.APACHE_UP_DN_AT_OID );
            updnIdx.initialize( attributeType );
            systemIndices.put( ApacheSchemaConstants.APACHE_UP_DN_AT_OID, updnIdx );
        }

        if ( existenceIdx == null )
        {
            AttributeType attributeType = schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID );
            existenceIdx = new AvlIndex<String, E>();
            existenceIdx.setAttributeId( ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID );
            existenceIdx.initialize( attributeType );
            systemIndices.put( ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID, existenceIdx );
        }

        if ( oneLevelIdx == null )
        {
            AttributeType attributeType = schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
            oneLevelIdx = new AvlIndex<Long, E>();
            oneLevelIdx.setAttributeId( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID );
            oneLevelIdx.initialize( attributeType );
            systemIndices.put( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID, oneLevelIdx );
        }

        if ( oneAliasIdx == null )
        {
            AttributeType attributeType = schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
            oneAliasIdx = new AvlIndex<Long, E>();
            oneAliasIdx.setAttributeId( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID );
            oneAliasIdx.initialize( attributeType );
            systemIndices.put( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID, oneAliasIdx );
        }

        if ( subAliasIdx == null )
        {
            AttributeType attributeType = schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
            subAliasIdx = new AvlIndex<Long, E>();
            subAliasIdx.setAttributeId( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID );
            subAliasIdx.initialize( attributeType );
            systemIndices.put( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID, subAliasIdx );
        }

        if ( aliasIdx == null )
        {
            AttributeType attributeType = schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
            aliasIdx = new AvlIndex<String, E>();
            aliasIdx.setAttributeId( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
            aliasIdx.initialize( attributeType );
            systemIndices.put( ApacheSchemaConstants.APACHE_ALIAS_AT_OID, aliasIdx );
        }

        if ( subLevelIdx == null )
        {
            AttributeType attributeType = schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );
            subLevelIdx = new AvlIndex<Long, E>();
            subLevelIdx.setAttributeId( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID );
            subLevelIdx.initialize( attributeType );
            systemIndices.put( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID, subLevelIdx );
        }

        if ( entryCsnIdx == null )
        {
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_CSN_AT_OID );
            entryCsnIdx = new AvlIndex<String, E>();
            entryCsnIdx.setAttributeId( SchemaConstants.ENTRY_CSN_AT_OID );
            entryCsnIdx.initialize( attributeType );
            systemIndices.put( SchemaConstants.ENTRY_CSN_AT_OID, entryCsnIdx );
        }

        if ( entryUuidIdx == null )
        {
            AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.ENTRY_UUID_AT_OID );
            entryUuidIdx = new AvlIndex<String, E>();
            entryUuidIdx.setAttributeId( SchemaConstants.ENTRY_UUID_AT_OID );
            entryUuidIdx.initialize( attributeType );
            systemIndices.put( SchemaConstants.ENTRY_UUID_AT_OID, entryUuidIdx );
        }

        if ( objectClassIdx == null )
        {
            AttributeType attributeType = schemaManager
                .lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT_OID );
            objectClassIdx = new AvlIndex<String, E>();
            objectClassIdx.setAttributeId( SchemaConstants.OBJECT_CLASS_AT_OID );
            objectClassIdx.initialize( attributeType );
            systemIndices.put( SchemaConstants.OBJECT_CLASS_AT_OID, objectClassIdx );
        }

    }


    private void setupUserIndices() throws Exception
    {
        if ( userIndices != null && userIndices.size() > 0 )
        {
            Map<String, AvlIndex<? extends Object, E>> tmp = new HashMap<String, AvlIndex<? extends Object, E>>();

            for ( AvlIndex<? extends Object, E> index : userIndices.values() )
            {
                String oid = schemaManager.getAttributeTypeRegistry().getOidByName( index.getAttributeId() );
                AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( oid );

                // Check that the attributeType has an EQUALITY matchingRule
                MatchingRule mr = attributeType.getEquality();

                if ( mr != null )
                {
                    index.initialize( schemaManager.lookupAttributeTypeRegistry( oid ) );
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
            userIndices = new HashMap<String, AvlIndex<? extends Object, E>>();
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean isInitialized()
    {
        return initialized;
    }


    /**
     * {@inheritDoc}
     */
    public IndexCursor<Long, E, Long> list( Long id ) throws Exception
    {
        IndexCursor<Long, E, Long> cursor = oneLevelIdx.forwardCursor( id );
        cursor.beforeValue( id, null );
        return cursor;
    }


    /**
     * {@inheritDoc}
     */
    public ServerEntry lookup( Long id ) throws Exception
    {
        return master.get( id );
    }


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
                ( ( AvlIndex ) index ).add( value.get(), id );
            }

            // If the attr didn't exist for this id add it to existence index
            if ( !existenceIdx.forward( modsOid, id ) )
            {
                existenceIdx.add( modsOid, id );
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
                ( ( AvlIndex ) index ).drop( value.get(), id );
            }

            /* 
             * If no attribute values exist for this entryId in the index then
             * we remove the existance index entry for the removed attribute.
             */
            if ( null == index.reverseLookup( id ) )
            {
                existenceIdx.drop( modsOid, id );
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
                if ( value instanceof StringValue )
                {
                    entryAttr.remove( ( String ) value.get() );
                }
                else
                {
                    entryAttr.remove( ( byte[] ) value.get() );
                }
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

            // if the id exists in the index drop all existing attribute value index entries and add new ones
            if ( index.reverse( id ) )
            {
                ( ( AvlIndex<?, E> ) index ).drop( id );
            }

            for ( Value<?> value : mods )
            {
                ( ( AvlIndex<Object, E> ) index ).add( value.get(), id );
            }

            /* 
             * If no attribute values exist for this entryId in the index then
             * we remove the existance index entry for the removed attribute.
             */
            if ( null == index.reverseLookup( id ) )
            {
                existenceIdx.drop( modsOid, id );
            }
        }

        String aliasAttributeOid = SchemaConstants.ALIASED_OBJECT_NAME_AT_OID;

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
                    throw new Exception( I18n.err( I18n.ERR_221 ) );
            }
        }

        master.put( id, entry );
    }


    public void modify( DN dn, List<Modification> mods ) throws Exception
    {
        Long id = getEntryId( dn.getNormName() );
        modify( id, mods );
    }


    public void modify( long entryId, List<Modification> mods ) throws Exception
    {
        ServerEntry entry = ( ServerEntry ) master.get( entryId );

        for ( Modification mod : mods )
        {
            EntryAttribute attrMods = mod.getAttribute();

            switch ( mod.getOperation() )
            {
                case ADD_ATTRIBUTE:
                    add( entryId, entry, attrMods );
                    break;

                case REMOVE_ATTRIBUTE:
                    remove( entryId, entry, attrMods );
                    break;

                case REPLACE_ATTRIBUTE:
                    replace( entryId, entry, attrMods );
                    break;

                default:
                    throw new Exception( I18n.err( I18n.ERR_221 ) );
            }
        }

        master.put( entryId, entry );
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
    }


    public void move( DN oldChildDn, DN newParentDn ) throws Exception
    {
        Long childId = getEntryId( oldChildDn.getNormName() );
        DN newUpdn = move( oldChildDn, childId, newParentDn );

        // Update the current entry
        ServerEntry entry = lookup( childId );
        entry.setDn( newUpdn );
        master.put( childId, entry );
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
         * Also we make sure that the existance index shows the existance of the
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
                ( ( Index ) index ).add( newNormValue, id );

                // Make sure the altered entry shows the existence of the new attrib
                if ( !existenceIdx.forward( newNormType, id ) )
                {
                    existenceIdx.add( newNormType, id );
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
         * tuple from the existance index.
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
                        ( ( AvlIndex ) index ).drop( oldNormValue, id );

                        /*
                         * If there is no value for id in this index due to our
                         * drop above we remove the oldRdnAttr from the existance idx
                         */
                        if ( null == index.reverseLookup( id ) )
                        {
                            existenceIdx.drop( oldNormType, id );
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
    }


    /**
     * {@inheritDoc}
     */
    public void setAliasIndex( Index<String, E, Long> index ) throws Exception
    {
        protect( "aliasIndex" );
        if ( index instanceof AvlIndex<?, ?> )
        {
            this.aliasIdx = ( AvlIndex<String, E> ) index;
        }
        else
        {
            this.aliasIdx = ( AvlIndex<String, E> ) convert( index );
        }

        // FIXME is this attribute ID or its OID
        systemIndices.put( index.getAttributeId(), aliasIdx );
    }


    /**
     * {@inheritDoc}
     */
    public void setName( String name )
    {
        protect( "name" );
        this.name = name;
    }


    /**
     * {@inheritDoc}
     */
    public void setNdnIndex( Index<String, E, Long> index ) throws Exception
    {
        protect( "ndnIndex" );
        if ( index instanceof AvlIndex<?, ?> )
        {
            this.ndnIdx = ( AvlIndex<String, E> ) index;
        }
        else
        {
            this.ndnIdx = ( AvlIndex<String, E> ) convert( index );
        }

        systemIndices.put( index.getAttributeId(), ndnIdx );
    }


    /**
     * {@inheritDoc}
     */
    public void setOneAliasIndex( Index<Long, E, Long> index ) throws Exception
    {
        protect( "oneAliasIndex" );
        if ( index instanceof AvlIndex<?, ?> )
        {
            this.oneAliasIdx = ( AvlIndex<Long, E> ) index;
        }
        else
        {
            this.oneAliasIdx = ( AvlIndex<Long, E> ) convert( index );
        }

        systemIndices.put( index.getAttributeId(), oneAliasIdx );
    }


    /**
     * {@inheritDoc}
     */
    public void setOneLevelIndex( Index<Long, E, Long> index ) throws Exception
    {
        protect( "oneLevelIndex" );
        if ( index instanceof AvlIndex<?, ?> )
        {
            this.oneLevelIdx = ( AvlIndex<Long, E> ) index;
        }
        else
        {
            this.oneLevelIdx = ( AvlIndex<Long, E> ) convert( index );
        }

        systemIndices.put( index.getAttributeId(), oneLevelIdx );
    }


    /**
     * {@inheritDoc}
     */
    public void setPresenceIndex( Index<String, E, Long> index ) throws Exception
    {
        protect( "presenceIndex" );
        if ( index instanceof AvlIndex<?, ?> )
        {
            this.existenceIdx = ( AvlIndex<String, E> ) index;
        }
        else
        {
            this.existenceIdx = ( AvlIndex<String, E> ) convert( index );
        }

        systemIndices.put( index.getAttributeId(), existenceIdx );
    }


    /**
     * {@inheritDoc}
     */
    public void setProperty( String propertyName, String propertyValue ) throws Exception
    {
        master.setProperty( propertyName, propertyValue );
    }


    /**
     * {@inheritDoc}
     */
    public void setSubAliasIndex( Index<Long, E, Long> index ) throws Exception
    {
        protect( "subAliasIndex" );
        if ( index instanceof AvlIndex<?, ?> )
        {
            this.subAliasIdx = ( AvlIndex<Long, E> ) index;
        }
        else
        {
            this.subAliasIdx = ( AvlIndex<Long, E> ) convert( index );
        }

        systemIndices.put( index.getAttributeId(), subAliasIdx );
    }


    /**
     * {@inheritDoc}
     */
    public void setSubLevelIndex( Index<Long, E, Long> index ) throws Exception
    {
        protect( "subLevelIndex" );
        if ( index instanceof AvlIndex<?, ?> )
        {
            this.subLevelIdx = ( AvlIndex<Long, E> ) index;
        }
        else
        {
            this.subLevelIdx = ( AvlIndex<Long, E> ) convert( index );
        }

        systemIndices.put( index.getAttributeId(), subLevelIdx );
    }


    /**
     * {@inheritDoc}
     */
    public void setSuffixDn( String suffixDn )
    {
        protect( "suffixDn" );
        try
        {
            this.suffixDn = new DN( suffixDn );
        }
        catch ( LdapInvalidDnException e )
        {
            throw new IllegalArgumentException( e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void setUpdnIndex( Index<String, E, Long> index ) throws Exception
    {
        protect( "updnIndex" );
        if ( index instanceof AvlIndex<?, ?> )
        {
            this.updnIdx = ( AvlIndex<String, E> ) index;
        }
        else
        {
            this.updnIdx = ( AvlIndex<String, E> ) convert( index );
        }

        systemIndices.put( index.getAttributeId(), updnIdx );
    }


    /**
     * {@inheritDoc}
     */
    public void setUserIndices( Set<Index<? extends Object, E, Long>> userIndices )
    {
        protect( "setUserIndices" );

        for ( Index<? extends Object, E, Long> index : userIndices )
        {
            if ( index instanceof AvlIndex<?, ?> )
            {
                this.userIndices.put( index.getAttributeId(), ( AvlIndex<? extends Object, E> ) index );
                continue;
            }

            LOG.warn( "Supplied index {} is not a AvlIndex.  "
                + "Will create new AvlIndex using copied configuration parameters.", index );

            AvlIndex<Object, E> avlIndex = ( AvlIndex<Object, E> ) convert( index );

            this.userIndices.put( index.getAttributeId(), avlIndex );
        }
    }


    private <K> AvlIndex<K, E> convert( Index<K, E, Long> index )
    {
        AvlIndex<K, E> avlIndex = new AvlIndex<K, E>();
        avlIndex.setAttributeId( index.getAttributeId() );
        return avlIndex;
    }


    private void protect( String method )
    {
        if ( initialized )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_222, method ) );
        }
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
    public Iterator<String> userIndices()
    {
        return userIndices.keySet().iterator();
    }


    /**
     * Adds userIndices for an aliasEntry to be added to the database while checking
     * for constrained alias constructs like alias cycles and chaining.
     * 
     * @param aliasDn normalized distinguished name for the alias entry
     * @param aliasTarget the user provided aliased entry dn as a string
     * @param aliasId the id of alias entry to add
     * @throws Exception if index addition fails, and if the alias is
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
                throw new Exception( I18n.err( I18n.ERR_223 ) );
            }

            throw new Exception( I18n.err( I18n.ERR_224, aliasTarget, aliasDn ) );
        }

        /*
         * Check For Aliases External To Naming Context
         * 
         * id may be null but the alias may be to a valid entry in 
         * another namingContext.  Such aliases are not allowed and we
         * need to point it out to the user instead of saying the target
         * does not exist when it potentially could outside of this upSuffix.
         */
        if ( !normalizedAliasTargetDn.isChildOf( suffixDn ) )
        {
            // Complain specifically about aliases to outside naming contexts
            throw new Exception( I18n.err( I18n.ERR_225, suffixDn.getName() ) );
        }

        // L O O K U P   T A R G E T   I D
        targetId = ndnIdx.forwardLookup( normalizedAliasTargetDn.getNormName() );

        /*
         * Check For Target Existance
         * 
         * We do not allow the creation of inconsistant aliases.  Aliases should
         * not be broken links.  If the target does not exist we start screaming
         */
        if ( null == targetId )
        {
            // Complain about target not existing
            throw new Exception( I18n.err( I18n.ERR_226 ) );
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
            // Complain about illegal alias chain
            throw new Exception( I18n.err( I18n.ERR_227 ) );
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
        while ( !ancestorDn.equals( suffixDn ) && null != ancestorId )
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
     * Removes the index entries for an alias before the entry is deleted from
     * the master table.
     * 
     * @todo Optimize this by walking the hierarchy index instead of the name 
     * @param aliasId the id of the alias entry in the master table
     * @throws Exception if we cannot parse ldap names
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

        while ( !ancestorDn.equals( suffixDn ) && ancestorDn.size() > suffixDn.size() )
        {
            ancestorDn = ( DN ) ancestorDn.getPrefix( ancestorDn.size() - 1 );
            ancestorId = getEntryId( ancestorDn.getNormName() );

            subAliasIdx.drop( ancestorId, targetId );
        }

        // Drops all alias tuples pointing to the id of the alias to be deleted
        aliasIdx.drop( aliasId );
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

        while ( !ancestorDn.equals( suffixDn ) )
        {
            ancestorDn = ( DN ) ancestorDn.getPrefix( 1 );
            ancestorId = getEntryId( ancestorDn.getNormName() );

            subAliasIdx.drop( ancestorId, targetId );
        }
    }


    /**
     * always returns 0 (zero), cause this is a inmemory store
     */
    public int getCacheSize()
    {
        return 0;
    }


    public Index<String, E, Long> getEntryCsnIndex()
    {
        return entryCsnIdx;
    }


    public Index<String, E, Long> getEntryUuidIndex()
    {
        return entryUuidIdx;
    }


    public Index<String, E, Long> getObjectClassIndex()
    {
        return objectClassIdx;
    }


    public void setEntryCsnIndex( Index<String, E, Long> index ) throws Exception
    {
        protect( "entryCsnIndex" );

        if ( index instanceof AvlIndex<?, ?> )
        {
            this.entryCsnIdx = ( AvlIndex<String, E> ) index;
        }
        else
        {
            this.entryCsnIdx = ( AvlIndex<String, E> ) convert( index );
        }

        systemIndices.put( index.getAttributeId(), entryCsnIdx );
    }


    public void setSyncOnWrite( boolean sync )
    {
        // do nothing
    }


    public void setWorkingDirectory( File wkDir )
    {
        //do nothing
    }


    public File getWorkingDirectory()
    {
        // returns null always
        return null;
    }


    public boolean isSyncOnWrite()
    {
        return false;
    }


    public void setCacheSize( int size )
    {
        // do nothing
    }


    public void setObjectClassIndex( Index<String, E, Long> index )
    {
        protect( "objectClassIndex" );

        if ( index instanceof AvlIndex<?, ?> )
        {
            this.objectClassIdx = ( AvlIndex<String, E> ) index;
        }
        else
        {
            objectClassIdx = convert( index );
        }

        systemIndices.put( index.getAttributeId(), objectClassIdx );
    }


    public void setEntryUuidIndex( Index<String, E, Long> index )
    {
        protect( "entryUuidIndex" );
        if ( index instanceof AvlIndex<?, ?> )
        {
            this.entryUuidIdx = ( AvlIndex<String, E> ) index;
        }
        else
        {
            entryUuidIdx = convert( index );
        }

        systemIndices.put( index.getAttributeId(), entryUuidIdx );
    }


    /**
     * @{inhertDoc}
     */
    public void sync() throws Exception
    {
    }


    /**
     * @{inhertDoc}
     */
    public Long getDefaultId()
    {
        return 1L;
    }

}
