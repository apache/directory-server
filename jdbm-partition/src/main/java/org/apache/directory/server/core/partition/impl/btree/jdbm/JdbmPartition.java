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


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.AbstractXdbmPartition;
import org.apache.directory.server.xdbm.search.impl.CursorBuilder;
import org.apache.directory.server.xdbm.search.impl.DefaultOptimizer;
import org.apache.directory.server.xdbm.search.impl.DefaultSearchEngine;
import org.apache.directory.server.xdbm.search.impl.EvaluatorBuilder;
import org.apache.directory.server.xdbm.search.impl.NoOpOptimizer;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.ServerEntry;


/**
 * A {@link Partition} that stores entries in
 * <a href="http://jdbm.sourceforge.net/">JDBM</a> database.
 *
 * @org.apache.xbean.XBean
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmPartition extends AbstractXdbmPartition<Long>
{

    /**
     * Creates a store based on JDBM B+Trees.
     */
    public JdbmPartition()
    {
        super( new JdbmStore<ServerEntry>() );
    }


    @SuppressWarnings("unchecked")
    protected void doInit() throws Exception
    {
        store.setPartitionDir( getPartitionDir() );

        EvaluatorBuilder<Long> evaluatorBuilder = new EvaluatorBuilder<Long>( store, schemaManager );
        CursorBuilder<Long> cursorBuilder = new CursorBuilder<Long>( store, evaluatorBuilder );

        // setup optimizer and registries for parent
        if ( !optimizerEnabled )
        {
            optimizer = new NoOpOptimizer();
        }
        else
        {
            optimizer = new DefaultOptimizer<ServerEntry, Long>( store );
        }

        searchEngine = new DefaultSearchEngine<Long>( store, cursorBuilder, evaluatorBuilder, optimizer );

        // initialize the store
        store.setCacheSize( cacheSize );
        store.setName( id );

        // Normalize the suffix
        suffix.normalize( schemaManager.getNormalizerMapping() );
        store.setSuffixDn( suffix.getNormName() );
        store.setPartitionDir( getPartitionDir() );

        Set<Index<?, ServerEntry, Long>> userIndices = new HashSet<Index<?, ServerEntry, Long>>();

        for ( Index<?, ServerEntry, Long> obj : getIndexedAttributes() )
        {
            Index<?, ServerEntry, Long> index;

            if ( obj instanceof JdbmIndex<?, ?> )
            {
                index = ( JdbmIndex<?, ServerEntry> ) obj;
            }
            else
            {
                index = new JdbmIndex<Object, ServerEntry>();
                index.setAttributeId( obj.getAttributeId() );
                index.setCacheSize( obj.getCacheSize() );
                index.setWkDirPath( obj.getWkDirPath() );
            }

            String oid = schemaManager.getAttributeTypeRegistry().getOidByName( index.getAttributeId() );

            if ( SYS_INDEX_OIDS.contains( oid ) )
            {
                if ( oid.equals( ApacheSchemaConstants.APACHE_ALIAS_AT_OID ) )
                {
                    store.setAliasIndex( ( Index<String, ServerEntry, Long> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID ) )
                {
                    store.setPresenceIndex( ( Index<String, ServerEntry, Long> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID ) )
                {
                    store.setOneLevelIndex( ( Index<Long, ServerEntry, Long> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_N_DN_AT_OID ) )
                {
                    store.setNdnIndex( ( Index<String, ServerEntry, Long> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID ) )
                {
                    store.setOneAliasIndex( ( Index<Long, ServerEntry, Long> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID ) )
                {
                    store.setSubAliasIndex( ( Index<Long, ServerEntry, Long> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_UP_DN_AT_OID ) )
                {
                    store.setUpdnIndex( ( Index<String, ServerEntry, Long> ) index );
                }
                else if ( oid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
                {
                    store.setObjectClassIndex( ( Index<String, ServerEntry, Long> ) index );
                }
                else if ( oid.equals( SchemaConstants.ENTRY_CSN_AT_OID ) )
                {
                    store.setEntryCsnIndex( ( Index<String, ServerEntry, Long> ) index );
                }
                else if ( oid.equals( SchemaConstants.ENTRY_UUID_AT_OID ) )
                {
                    store.setEntryUuidIndex( ( Index<String, ServerEntry, Long> ) index );
                }
                else
                {
                    throw new IllegalStateException( "Unrecognized system index " + oid );
                }
            }
            else
            {
                userIndices.add( index );
            }

            store.setUserIndices( userIndices );
        }

        store.init( schemaManager );
    }


    public Index<String, ServerEntry, Long> getObjectClassIndex()
    {
        return store.getObjectClassIndex();
    }


    public Index<String, ServerEntry, Long> getEntryCsnIndex()
    {
        return store.getEntryCsnIndex();
    }


    public Index<String, ServerEntry, Long> getEntryUuidIndex()
    {
        return store.getEntryUuidIndex();
    }

}
