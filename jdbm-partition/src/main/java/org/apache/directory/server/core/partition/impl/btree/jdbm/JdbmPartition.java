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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.xdbm.AbstractXdbmPartition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.search.impl.CursorBuilder;
import org.apache.directory.server.xdbm.search.impl.DefaultOptimizer;
import org.apache.directory.server.xdbm.search.impl.DefaultSearchEngine;
import org.apache.directory.server.xdbm.search.impl.EvaluatorBuilder;
import org.apache.directory.server.xdbm.search.impl.NoOpOptimizer;
import org.apache.directory.shared.ldap.model.entry.Entry;


/**
 * A {@link Partition} that stores entries in
 * <a href="http://jdbm.sourceforge.net/">JDBM</a> database.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmPartition extends AbstractXdbmPartition<Long>
{

    /**
     * Creates a store based on JDBM B+Trees.
     */
    public JdbmPartition()
    {
        super( new JdbmStore<Entry>() );
    }


    protected void doInit() throws Exception
    {
        store.setPartitionPath( getPartitionPath() );

        EvaluatorBuilder<Long> evaluatorBuilder = new EvaluatorBuilder<Long>( store, schemaManager );
        CursorBuilder<Long> cursorBuilder = new CursorBuilder<Long>( store, evaluatorBuilder );

        // setup optimizer and registries for parent
        if ( !optimizerEnabled )
        {
            optimizer = new NoOpOptimizer();
        }
        else
        {
            optimizer = new DefaultOptimizer<Entry, Long>( store );
        }

        searchEngine = new DefaultSearchEngine<Long>( store, cursorBuilder, evaluatorBuilder, optimizer );

        // initialize the store
        store.setCacheSize( cacheSize );
        store.setId( id );

        // Normalize the suffix
        suffix.applySchemaManager( schemaManager );
        store.setSuffixDn( suffix );
        store.setPartitionPath( getPartitionPath() );

        for ( Index<?, Entry, Long> index : getIndexedAttributes() )
        {
            String oid = schemaManager.getAttributeTypeRegistry().getOidByName( index.getAttributeId() );
            
            if ( !index.getAttributeId().equals( oid ) )
            {
                index.setAttributeId( oid );
            }
            
            store.addIndex( index );
        }

        store.init( schemaManager );
    }


    public Index<String, Entry, Long> getObjectClassIndex()
    {
        return store.getObjectClassIndex();
    }


    public Index<String, Entry, Long> getEntryCsnIndex()
    {
        return store.getEntryCsnIndex();
    }


    public Index<String, Entry, Long> getEntryUuidIndex()
    {
        return store.getEntryUuidIndex();
    }

}
