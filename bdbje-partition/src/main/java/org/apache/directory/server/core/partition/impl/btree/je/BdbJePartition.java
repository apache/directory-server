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

package org.apache.directory.server.core.partition.impl.btree.je;


import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.Tuple;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.DnFactory;
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

import com.sleepycat.je.Database;
import com.sleepycat.je.Transaction;


/**
 * A partition implementation backed by bdb je.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BdbJePartition extends AbstractBTreePartition
{

    private static final Logger LOG = LoggerFactory.getLogger( BdbJePartition.class );

    private BdbJePartitionEnviroment partitionEnv;


    /**
     * Creates a new instance of BdbJePartition.
     */
    public BdbJePartition( SchemaManager schemaManager, DnFactory dnFactory )
    {
        super( schemaManager, dnFactory );
    }


    @Override
    protected void doInit() throws LdapException
    {
        if ( initialized )
        {
            return;
        }

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

        File partitionDir = getPartitionDir();
        partitionDir.mkdirs();

        partitionEnv = new BdbJePartitionEnviroment( schemaManager, partitionDir );

        Database db = partitionEnv.createDb( "masterDb" );
        master = new BdbJeMasterTable( db, schemaManager );

        Set<String> existingIndices = partitionEnv.getIndexOids();

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

        //entryCache = partitionEnv.createCache( "entryCache" );

        // then add all index objects to a list
        List<String> allIndices = new ArrayList<String>();
        for ( Index i : systemIndices.values() )
        {
            allIndices.add( i.getAttribute().getOid() );
        }

        Set<Index> newUserIndices = new HashSet<Index>();

        for ( Index i : userIndices.values() )
        {
            allIndices.add( i.getAttribute().getOid() );

            String name = i.getAttribute().getOid();

            // if the name doesn't exist in the list of indexed OIDs
            // this is a new index and we need to build it
            if ( !existingIndices.contains( name ) )
            {
                newUserIndices.add( i );
            }
        }

        if ( !newUserIndices.isEmpty() )
        {
            try
            {
                buildNewIndices( newUserIndices );
            }
            catch ( Exception e )
            {
                throw new LdapException( e );
            }
        }

        deleteUnusedIndexData( allIndices, existingIndices );

        //printDb( masterDb, masterKeySer, masterValSer );
        //printDb( ( BdbJeIndex ) getNdnIndex() );
        initialized = true;
    }


    private File getPartitionDir()
    {
        return new File( getPartitionPath() );
    }


    @Override
    public void sync()
    {
    }


    @Override
    protected void doDestroy( PartitionTxn txn ) throws LdapException
    {
        if ( !initialized )
        {
            return;
        }

        // FIXME this code MUST be commented out before releasing
        if ( !JeTransaction.txnStack.isEmpty() )
        {
            System.out.println( "there are " + JeTransaction.txnStack.size() + " open txns" );
            int i = 0;
            for ( StackTraceElement[] st : JeTransaction.txnStack )
            {
                System.out.println( ">>>>>>>>>>>>>>>>>>>>> " + i );
                for ( StackTraceElement se : st )
                {
                    String s = se.toString();
                    if ( s.startsWith( "org.eclipse." ) )
                    {
                        break;
                    }
                    System.out.println( se );
                }
                System.out.println( "<<<<<<<<<<<<<<<<<<<<< " + i );
                i++;
            }
        }
        super.doDestroy( txn );

        try
        {
            partitionEnv.close();
            LOG.debug( "Closed environment for {} partition.", suffixDn );
        }
        catch ( Throwable t )
        {
            LOG.error( I18n.err( I18n.ERR_127 ), t );
        }
    }


    /**
     * builds a user defined index on a attribute by browsing all the entries present in master db
     * 
     * @param userIdx then user defined index
     * @throws Exception in case of any problems while building the index
     */
    private void buildNewIndices( Set<Index> userIndices ) throws Exception
    {
        LOG.info( "building the index for attribute type(s) {}", userIndices );

        PartitionTxn tx = beginWriteTransaction();
        Cursor<Tuple<String, Entry>> cursor = ( ( BdbJeMasterTable ) master ).cursor( tx );
        while ( cursor.next() )
        {
            Tuple<String, Entry> t = cursor.get();
            Entry entry = t.getValue();

            BdbJeIndex prIdx = ( BdbJeIndex ) presenceIdx;

            for ( Index bji : userIndices )
            {
                AttributeType atType = bji.getAttribute();
                Attribute entryAttr = entry.get( atType );
                if ( entryAttr != null )
                {
                    for ( Value value : entryAttr )
                    {
                        bji.add( tx, value.getValue(), id );
                    }

                    // Adds only those attributes that are indexed
                    prIdx.add( tx, atType.getOid(), id );
                }
            }
        }

        cursor.close();
        tx.commit();
    }


    /**
     * removes any unused/removed user attribute index data present in the partition's database
     * Note: only removes user defined indices
     */
    private void deleteUnusedIndexData( List<String> allIndices, Set<String> existingIndexOids )
    {
        for ( String oid : existingIndexOids )
        {
            // remove the index data if not found in the list of names of indices
            if ( !allIndices.contains( oid ) )
            {
                try
                {
                    partitionEnv.deleteIndexDb( oid );
                    LOG.info( "Deleted data of index with oid {}", oid );
                }
                catch ( Exception e )
                {
                    LOG.warn( "", e );
                }
            }
        }
    }


    protected BdbJePartitionEnviroment getPartitionEnv()
    {
        return partitionEnv;
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


    @Override
    public PartitionReadTxn beginReadTransaction()
    {
        return beginTransaction();
    }


    @Override
    public PartitionWriteTxn beginWriteTransaction()
    {
        return beginTransaction();
    }


    private JeTransaction beginTransaction()
    {
        Transaction txn = partitionEnv.createTxn();
        JeTransaction jeTxn = new JeTransaction( txn );
        System.out.println( "************ txn created" );
        return jeTxn;
    }


    @Override
    protected Index<?, String> convertAndInit( Index<?, String> index )
        throws LdapException
    {
        BdbJeIndex jeIndex;

        if ( index.getAttributeId().equals( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
        {
            jeIndex = new BdbJeRdnIndex();
            jeIndex.setAttributeId( ApacheSchemaConstants.APACHE_RDN_AT_OID );
            jeIndex.setCacheSize( index.getCacheSize() );
        }
        else if ( index instanceof BdbJeIndex )
        {
            jeIndex = ( BdbJeIndex<?> ) index;
        }
        else
        {
            LOG.debug( "Supplied index {} is not a BdbJeIndex.  "
                + "Will create new BdbJeIndex using copied configuration parameters.", index );
            jeIndex = new BdbJeIndex<>( index.getAttributeId(), true );
            jeIndex.setCacheSize( index.getCacheSize() );
        }

        jeIndex.init( schemaManager, partitionEnv );

        return jeIndex;
    }


    @Override
    protected Index createSystemIndex( String oid, URI path, boolean withReverse ) throws LdapException
    {
        LOG.debug( "Creating a bdb JE based system Index for attribute {}", oid );
        BdbJeIndex<?> jeIndex = null;

        if ( oid.equals( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
        {
            jeIndex = new BdbJeRdnIndex();
            jeIndex.setAttributeId( ApacheSchemaConstants.APACHE_RDN_AT_OID );
        }
        else if ( oid.equals( ApacheSchemaConstants.APACHE_ALIAS_AT_OID ) )
        {
            jeIndex = new BdbJeDnIndex( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
            jeIndex.setAttributeId( ApacheSchemaConstants.APACHE_ALIAS_AT_OID );
        }
        else
        {
            jeIndex = new BdbJeIndex( oid, withReverse );
        }

        jeIndex.setWkDirPath( path );

        return jeIndex;
    }


    @Override
    protected void doRepair() throws LdapException
    {
    }
}
