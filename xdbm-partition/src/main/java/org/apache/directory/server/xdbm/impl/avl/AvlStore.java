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
package org.apache.directory.server.xdbm.impl.avl;


import java.net.URI;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.partition.impl.btree.LongComparator;
import org.apache.directory.server.xdbm.AbstractStore;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
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
 */
public class AvlStore<E> extends AbstractStore<E, Long>
{
    /** static logger */
    private static final Logger LOG = LoggerFactory.getLogger( AvlStore.class );


    /**
     * {@inheritDoc}
     */
    public void destroy() throws Exception
    {
        // don't reset initialized flag
        initialized = false;

        if ( master != null )
        {
            master.close();
        }
        
        for ( Index idx : systemIndices.values() )
        {
            idx.close();
        }
        
        for ( Index idx : userIndices.values() )
        {
            idx.close();
        }
    }


    /**
     * {@inheritDoc}
     * TODO why this and initRegistries on Store interface ???
     */
    public void init( SchemaManager schemaManager ) throws Exception
    {
        super.init( schemaManager );

        // Create the master table (the table containing all the entries)
        master = new AvlMasterTable<Entry>( id, new LongComparator(), null, false );

        // -------------------------------------------------------------------
        // Initializes the user and system indices
        // -------------------------------------------------------------------
        setupSystemIndices();
        setupUserIndices();

        // We are done !
        initialized = true;
    }


    @Override
    protected Index<?, E, Long> convertAndInit( Index<?, E, Long> index ) throws Exception
    {
        AvlIndex<?, E> avlIndex;
        
        if ( index.getAttributeId().equals( ApacheSchemaConstants.APACHE_RDN_AT_OID ) )
        {
            avlIndex = new AvlRdnIndex<E>( index.getAttributeId() );
        }
        else if ( index instanceof AvlIndex<?, ?> )
        {
            avlIndex = (org.apache.directory.server.xdbm.impl.avl.AvlIndex<?, E> ) index;
        }
        else
        {
            LOG.debug( "Supplied index {} is not a AvlIndex.  "
                + "Will create new AvlIndex using copied configuration parameters.", index );
            avlIndex = new AvlIndex( index.getAttributeId() );
        }

        avlIndex.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( index.getAttributeId() ) );

        return avlIndex;
    }


    /**
     * Always returns 0 (zero), cause this is a in-memory store
     */
    @Override
    public int getCacheSize()
    {
        return 0;
    }


    /**
     * always returns null, cause this is a in-memory store
     */
    @Override
    public URI getPartitionPath()
    {
        // returns null always
        return null;
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
     * {@inheritDoc}
     */
    public void sync() throws Exception
    {
    }


    /**
     * {@inheritDoc}
     */
    public Long getDefaultId()
    {
        return 1L;
    }


    @Override
    protected Long getRootId()
    {
        return 0L;
    }
}
