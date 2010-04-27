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
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base implementation of a {@link Store}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $$Rev$$
 */
public abstract class AbstractStore<E, ID> implements Store<E, ID>
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

}
