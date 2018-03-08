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
package org.apache.directory.server.core.partition.impl.avl;


import java.net.URI;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.UuidComparator;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionReadTxn;
import org.apache.directory.server.core.api.partition.PartitionWriteTxn;
import org.apache.directory.server.core.partition.impl.btree.AbstractBTreePartition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.server.xdbm.impl.avl.AvlMasterTable;
import org.apache.directory.server.xdbm.impl.avl.AvlRdnIndex;
import org.apache.directory.server.xdbm.search.impl.CursorBuilder;
import org.apache.directory.server.xdbm.search.impl.DefaultOptimizer;
import org.apache.directory.server.xdbm.search.impl.DefaultSearchEngine;
import org.apache.directory.server.xdbm.search.impl.EvaluatorBuilder;
import org.apache.directory.server.xdbm.search.impl.NoOpOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An XDBM Partition backed by in memory AVL Trees.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlPartition extends AbstractBTreePartition
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( AvlPartition.class );


    /**
     * Creates a store based on AVL Trees.
     * 
     * @param schemaManager the schema manager
     */
    public AvlPartition( SchemaManager schemaManager )
    {
        super( schemaManager );
    }


    /**
     * Creates a store based on AVL Trees.
     *
     * @param schemaManager the schema manager
     * @param dnFactory the DN factory
     */
    public AvlPartition( SchemaManager schemaManager, DnFactory dnFactory )
    {
        super( schemaManager, dnFactory );
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRepair() throws LdapException
    {
        // Nothing to do
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInit() throws LdapException
    {
        if ( !initialized )
        {
            EvaluatorBuilder evaluatorBuilder = new EvaluatorBuilder( this, schemaManager );
            CursorBuilder cursorBuilder = new CursorBuilder( this, evaluatorBuilder );

            // setup optimizer and registries for parent
            if ( !optimizerEnabled )
            {
                setOptimizer( new NoOpOptimizer() );
            }
            else
            {
                setOptimizer( new DefaultOptimizer( this ) );
            }

            setSearchEngine( new DefaultSearchEngine( this, cursorBuilder, evaluatorBuilder, getOptimizer() ) );

            if ( isInitialized() )
            {
                return;
            }

            // Create the master table (the table containing all the entries)
            master = new AvlMasterTable( id, UuidComparator.INSTANCE, null, false );

            super.doInit();
        }
    }


    /**
     * {@inheritDoc}
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
     * always returns false, cause this is a in-memory store
     */
    @Override
    public boolean isSyncOnWrite()
    {
        return false;
    }


    /**
     * Always returns 0 (zero), cause this is a in-memory store
     */
    @Override
    public int getCacheSize()
    {
        return 0;
    }


    @Override
    protected Index<?, String> convertAndInit( Index<?, String> index ) throws LdapException
    {
        AvlIndex<?> avlIndex;

        if ( index.getAttributeId().equals( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
        {
            avlIndex = new AvlRdnIndex( index.getAttributeId() );
        }
        else if ( index instanceof AvlIndex<?> )
        {
            avlIndex = ( AvlIndex<?> ) index;
        }
        else
        {
            LOG.debug( "Supplied index {} is not a AvlIndex. "
                + "Will create new AvlIndex using copied configuration parameters.", index );
            avlIndex = new AvlIndex( index.getAttributeId(), true );
        }

        avlIndex.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() ) );

        return avlIndex;
    }


    /**
     * {@inheritDoc}
     */
    protected final Index createSystemIndex( String oid, URI path, boolean withReverse ) throws LdapException
    {
        LOG.debug( "Supplied index {} is not a JdbmIndex.  "
            + "Will create new JdbmIndex using copied configuration parameters." );

        AvlIndex<?> avlIndex;

        if ( oid.equals( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
        {
            avlIndex = new AvlRdnIndex( oid );
        }
        else
        {
            LOG.debug( "Supplied index {} is not a AvlIndex. "
                + "Will create new AvlIndex using copied configuration parameters." );
            avlIndex = new AvlIndex( oid, withReverse );
        }

        return avlIndex;
    }


    /**
     * {@inheritDoc}
     */
    public URI getPartitionPath()
    {
        // It's a in-memory partition, return null
        return null;
    }


    @Override
    public PartitionReadTxn beginReadTransaction()
    {
        return new PartitionReadTxn();
    }


    @Override
    public PartitionWriteTxn beginWriteTransaction()
    {
        return new PartitionWriteTxn();
    }
}
