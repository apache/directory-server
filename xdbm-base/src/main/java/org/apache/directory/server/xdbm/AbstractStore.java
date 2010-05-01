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


import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapAliasDereferencingException;
import org.apache.directory.shared.ldap.exception.LdapAliasException;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.util.NamespaceTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base implementation of a {@link Store}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public abstract class AbstractStore<E, ID extends Comparable<ID>> implements Store<E, ID>
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( AbstractStore.class );

    /** The default cache size is set to 10 000 objects */
    public static final int DEFAULT_CACHE_SIZE = 10000;

    /** true if initialized */
    protected boolean initialized;

    /** the partition directory to use for files */
    protected File partitionDir;

    /** true if we sync disks on every write operation */
    protected boolean isSyncOnWrite = true;

    /** The store cache size */
    protected int cacheSize = DEFAULT_CACHE_SIZE;

    /** The store unique identifier */
    protected String id;

    /** The suffix DN */
    protected DN suffixDn;

    /** A pointer on the schemaManager */
    protected SchemaManager schemaManager;

    /** a map of attributeType numeric ID to user userIndices */
    protected Map<String, Index<?, E, ID>> userIndices = new HashMap<String, Index<?, E, ID>>();

    /** a map of attributeType numeric ID to system userIndices */
    protected Map<String, Index<?, E, ID>> systemIndices = new HashMap<String, Index<?, E, ID>>();

    /** the normalized distinguished name index */
    protected Index<String, E, ID> ndnIdx;

    /** the attribute presence index */
    protected Index<String, E, ID> presenceIdx;

    /** a system index on the entries of descendants of root DN*/
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


    protected void protect( String property )
    {
        if ( initialized )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_576, property ) );
        }
    }


    public boolean isInitialized()
    {
        return initialized;
    }


    public void setPartitionDir( File partitionDir )
    {
        protect( "partitionDir" );
        this.partitionDir = partitionDir;
    }


    public File getPartitionDir()
    {
        return partitionDir;
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


    public void setId( String id )
    {
        protect( "id" );
        this.id = id;
    }


    public String getId()
    {
        return id;
    }


    public void setSuffixDn( DN suffixDn )
    {
        protect( "suffixDn" );
        if ( !suffixDn.isNormalized() )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_218, suffixDn.getName() ) );
        }
        this.suffixDn = suffixDn;
    }


    public DN getSuffixDn()
    {
        return suffixDn;
    }


    //------------------------------------------------------------------------
    // Index
    //------------------------------------------------------------------------

    /**
     * Sets up the user indices.
     */
    @SuppressWarnings("unchecked")
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
    protected void setupSystemIndices() throws Exception
    {
        // add missing system indices
        if ( getPresenceIndex() == null )
        {
            addIndex( new GenericIndex<String, E, ID>( ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID ) );
        }
        if ( getOneLevelIndex() == null )
        {
            addIndex( new GenericIndex<ID, E, ID>( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID ) );
        }
        if ( getSubLevelIndex() == null )
        {
            addIndex( new GenericIndex<ID, E, ID>( ApacheSchemaConstants.APACHE_SUB_LEVEL_AT_OID ) );
        }
        if ( getRdnIndex() == null )
        {
            addIndex( new GenericIndex<ParentIdAndRdn<ID>, E, ID>( ApacheSchemaConstants.APACHE_RDN_AT_OID ) );
        }
        if ( getNdnIndex() == null )
        {
            addIndex( new GenericIndex<String, E, ID>( ApacheSchemaConstants.APACHE_N_DN_AT_OID ) );
        }
        if ( getAliasIndex() == null )
        {
            addIndex( new GenericIndex<String, E, ID>( ApacheSchemaConstants.APACHE_ALIAS_AT_OID ) );
        }
        if ( getOneAliasIndex() == null )
        {
            addIndex( new GenericIndex<ID, E, ID>( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID ) );
        }
        if ( getSubAliasIndex() == null )
        {
            addIndex( new GenericIndex<ID, E, ID>( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID ) );
        }
        if ( getObjectClassIndex() == null )
        {
            addIndex( new GenericIndex<String, E, ID>( SchemaConstants.OBJECT_CLASS_AT_OID ) );
        }
        if ( getEntryUuidIndex() == null )
        {
            addIndex( new GenericIndex<String, E, ID>( SchemaConstants.ENTRY_UUID_AT_OID ) );
        }
        if ( getEntryCsnIndex() == null )
        {
            addIndex( new GenericIndex<String, E, ID>( SchemaConstants.ENTRY_CSN_AT_OID ) );
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
        ndnIdx = ( Index<String, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_N_DN_AT_OID );
        presenceIdx = ( Index<String, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID );
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
    public boolean hasIndexOn( String id ) throws LdapException
    {
        return hasUserIndexOn( id ) || hasSystemIndexOn( id );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasUserIndexOn( String id ) throws LdapException
    {
        return userIndices.containsKey( schemaManager.getAttributeTypeRegistry().getOidByName( id ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasSystemIndexOn( String id ) throws LdapException
    {
        return systemIndices.containsKey( schemaManager.getAttributeTypeRegistry().getOidByName( id ) );
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
    public Index<?, E, ID> getIndex( String id ) throws IndexNotFoundException
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

        throw new IndexNotFoundException( I18n.err( I18n.ERR_3, id, id ) );
    }


    /**
     * {@inheritDoc}
     */
    public Index<?, E, ID> getUserIndex( String id ) throws IndexNotFoundException
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

        throw new IndexNotFoundException( I18n.err( I18n.ERR_3, id, id ) );
    }


    /**
     * {@inheritDoc}
     */
    public Index<?, E, ID> getSystemIndex( String id ) throws IndexNotFoundException
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

        throw new IndexNotFoundException( I18n.err( I18n.ERR_2, id, id ) );
    }


    /**
     * {@inheritDoc}
     */
    public void addIndex( Index<?, E, ID> index ) throws Exception
    {
        protect( "addIndex" );

        String oid = index.getAttributeId();
        if ( !OID.isOID( oid ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_309, oid ) );
        }

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
    public Index<String, E, ID> getNdnIndex()
    {
        return ( Index<String, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_N_DN_AT_OID );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Index<String, E, ID> getPresenceIndex()
    {
        return ( Index<String, E, ID> ) systemIndices.get( ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID );
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


    ////////////////////////////////////////////////7

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
    protected void add( ID id, Entry entry, EntryAttribute mods ) throws Exception
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
            Index<?, E, ID> index = getUserIndex( modsOid );

            for ( Value<?> value : mods )
            {
                ( ( Index ) index ).add( value.get(), id );
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
            DN ndn = getEntryDn( id );
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
    protected void replace( ID id, Entry entry, EntryAttribute mods ) throws Exception
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
            Index<?, E, ID> index = getUserIndex( modsOid );

            // if the id exists in the index drop all existing attribute 
            // value index entries and add new ones
            if ( index.reverse( id ) )
            {
                ( ( Index<?, E, ID> ) index ).drop( id );
            }

            for ( Value<?> value : mods )
            {
                ( ( Index<Object, E, ID> ) index ).add( value.get(), id );
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
            DN entryDn = getEntryDn( id );
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
    protected void remove( ID id, Entry entry, EntryAttribute mods ) throws Exception
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
            Index<?, E, ID> index = getUserIndex( modsOid );

            for ( Value<?> value : mods )
            {
                ( ( Index ) index ).drop( value.get(), id );
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
    protected void addAliasIndices( ID aliasId, DN aliasDn, String aliasTarget ) throws Exception
    {
        DN normalizedAliasTargetDn; // Name value of aliasedObjectName
        ID targetId; // Id of the aliasedObjectName
        DN ancestorDn; // Name of an alias entry relative
        ID ancestorId; // Id of an alias entry relative

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
        if ( !normalizedAliasTargetDn.isChildOf( suffixDn ) )
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
        ancestorDn = ( DN ) aliasDn.clone();
        ancestorDn.remove( aliasDn.size() - 1 );
        ancestorId = getEntryId( ancestorDn );

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
        ID targetId = getEntryId( new DN( targetDn ).normalize( schemaManager.getNormalizerMapping() ) );
        DN aliasDN = getEntryDn( aliasId );

        DN ancestorDn = ( DN ) aliasDN.clone();
        ancestorDn.remove( aliasDN.size() - 1 );
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
            ancestorDn = ( DN ) ancestorDn.getPrefix( ancestorDn.size() - 1 );
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
    protected void dropMovedAliasIndices( final DN movedBase ) throws Exception
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

        ID movedBaseId = getEntryId( movedBase );

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
    protected void dropAliasIndices( ID aliasId, DN movedBase ) throws Exception
    {
        String targetDn = aliasIdx.reverseLookup( aliasId );
        ID targetId = getEntryId( new DN( targetDn ).normalize( schemaManager.getNormalizerMapping() ) );
        DN aliasDn = getEntryDn( aliasId );

        /*
         * Start droping index tuples with the first ancestor right above the 
         * moved base.  This is the first ancestor effected by the move.
         */
        DN ancestorDn = ( DN ) movedBase.getPrefix( 1 );
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
            ancestorDn = ( DN ) ancestorDn.getPrefix( 1 );
            ancestorId = getEntryId( ancestorDn );

            subAliasIdx.drop( ancestorId, targetId );
        }
    }
}
