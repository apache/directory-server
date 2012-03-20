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

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.partition.impl.btree.AbstractBTreePartition;
import org.apache.directory.server.core.partition.impl.btree.LongComparator;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.impl.avl.AvlIndex;
import org.apache.directory.server.xdbm.impl.avl.AvlMasterTable;
import org.apache.directory.server.xdbm.impl.avl.AvlRdnIndex;
import org.apache.directory.server.xdbm.search.impl.CursorBuilder;
import org.apache.directory.server.xdbm.search.impl.DefaultOptimizer;
import org.apache.directory.server.xdbm.search.impl.DefaultSearchEngine;
import org.apache.directory.server.xdbm.search.impl.EvaluatorBuilder;
import org.apache.directory.server.xdbm.search.impl.NoOpOptimizer;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An XDBM Partition backed by in memory AVL Trees.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AvlPartition extends AbstractBTreePartition<Long>
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( AvlPartition.class );


    /**
     * Creates a store based on AVL Trees.
     */
    public AvlPartition( SchemaManager schemaManager )
    {
        super( schemaManager );
    }


    /**
     * {@inheritDoc}
     */
    protected void doInit() throws Exception
    {
        if ( !initialized )
        {
            EvaluatorBuilder<Long> evaluatorBuilder = new EvaluatorBuilder<Long>( this, schemaManager );
            CursorBuilder<Long> cursorBuilder = new CursorBuilder<Long>( this, evaluatorBuilder );

            // setup optimizer and registries for parent
            if ( !optimizerEnabled )
            {
                optimizer = new NoOpOptimizer();
            }
            else
            {
                optimizer = new DefaultOptimizer<Entry, Long>( this );
            }

            searchEngine = new DefaultSearchEngine<Long>( this, cursorBuilder, evaluatorBuilder, optimizer );

            if ( isInitialized() )
            {
                return;
            }

            // Create the master table (the table containing all the entries)
            master = new AvlMasterTable<Entry>( id, new LongComparator(), null, false );

            super.doInit();
        }
    }


    /**
     * {@inheritDoc}
     */
    public Long getDefaultId()
    {
        return 1L;
    }


    /**
     * {@inheritDoc}
     */
    public Long getRootId()
    {
        return 0L;
    }


    /**
     * {@inheritDoc}
     */
    public void sync() throws Exception
    {
        // Nothing to do
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
    protected Index<?, Entry, Long> convertAndInit( Index<?, Entry, Long> index ) throws Exception
    {
        AvlIndex<?, Entry> avlIndex;

        if ( index.getAttributeId().equals( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
        {
            avlIndex = new AvlRdnIndex<Entry>( index.getAttributeId() );
        }
        else if ( index instanceof AvlIndex<?, ?> )
        {
            avlIndex = ( AvlIndex<?, Entry> ) index;
        }
        else
        {
            LOG.debug( "Supplied index {} is not a AvlIndex. "
                + "Will create new AvlIndex using copied configuration parameters.", index );
            avlIndex = new AvlIndex( index.getAttributeId() );
        }

        avlIndex.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() ) );

        return avlIndex;
    }

    
    /**
     * {@inheritDoc}
     */
    protected final Index createSystemIndex( String oid, URI path ) throws Exception
    {
        LOG.debug( "Supplied index {} is not a JdbmIndex.  "
            + "Will create new JdbmIndex using copied configuration parameters." );

        AvlIndex<?, Entry>  avlIndex;

        if ( oid.equals( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
        {
            avlIndex = new AvlRdnIndex<Entry>( oid );
        }
        else
        {
            LOG.debug( "Supplied index {} is not a AvlIndex. "
                + "Will create new AvlIndex using copied configuration parameters." );
            avlIndex = new AvlIndex( oid );
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
}
