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


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.XdbmPartition;
import org.apache.directory.server.xdbm.search.impl.CursorBuilder;
import org.apache.directory.server.xdbm.search.impl.DefaultOptimizer;
import org.apache.directory.server.xdbm.search.impl.DefaultSearchEngine;
import org.apache.directory.server.xdbm.search.impl.EvaluatorBuilder;
import org.apache.directory.server.xdbm.search.impl.NoOpOptimizer;
import org.apache.directory.shared.ldap.constants.SchemaConstants;


/**
 * An XDBM Partition backed by in memory AVL Trees.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlPartition extends XdbmPartition
{
    private boolean optimizerEnabled = true;
    private Set<AvlIndex<?,ServerEntry>> indexedAttributes;
    private String suffix;
    
    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a store based on AVL Trees.
     */
    public AvlPartition()
    {
        super();
        super.setStore( new AvlStore<ServerEntry>() );
        indexedAttributes = new HashSet<AvlIndex<?,ServerEntry>>();
    }


    /**
     * @{inhertDoc}
     */
    @SuppressWarnings("unchecked")
    public void initialize( Registries registries ) throws Exception
    {
        setRegistries( registries );
        
        EvaluatorBuilder evaluatorBuilder = new EvaluatorBuilder( getStore(), getRegistries() );
        CursorBuilder cursorBuilder = new CursorBuilder( getStore(), evaluatorBuilder );

        // setup optimizer and registries for parent
        if ( ! optimizerEnabled )
        {
            optimizer = new NoOpOptimizer();
        }
        else
        {
            optimizer = new DefaultOptimizer<ServerEntry>( getStore() );
        }

        searchEngine = new DefaultSearchEngine( getStore(), cursorBuilder, evaluatorBuilder, optimizer );
        
        // initialize the store
        getStore().setName( getId() );
        getStore().setUpSuffixString( suffix );

        Set<AvlIndex<?,ServerEntry>> userIndices = new HashSet<AvlIndex<?,ServerEntry>>();
        
        for ( AvlIndex<?,ServerEntry> obj : indexedAttributes )
        {
            AvlIndex<?,ServerEntry> index;

            if ( obj instanceof AvlIndex )
            {
                index = ( AvlIndex<?,ServerEntry> ) obj;
            }
            else
            {
                index = new AvlIndex<Object,ServerEntry>();
                index.setAttributeId( obj.getAttributeId() );
            }

            String oid = getRegistries().getOidRegistry().getOid( index.getAttributeId() );
            
            if ( SYS_INDEX_OIDS.contains( getRegistries().getOidRegistry().getOid( index.getAttributeId() ) ) )
            {
                if ( oid.equals( ApacheSchemaConstants.APACHE_ALIAS_OID ) )
                {
                    getStore().setAliasIndex( ( Index<String,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_EXISTANCE_OID ) )
                {
                    getStore().setPresenceIndex( ( Index<String,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_ONE_LEVEL_OID ) )
                {
                    getStore().setOneLevelIndex( ( Index<Long,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_N_DN_OID ) )
                {
                    getStore().setNdnIndex( ( Index<String,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_ONE_ALIAS_OID ) )
                {
                    getStore().setOneAliasIndex( ( Index<Long,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_SUB_ALIAS_OID ) )
                {
                    getStore().setSubAliasIndex( ( Index<Long,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_UP_DN_OID ) )
                {
                    getStore().setUpdnIndex( ( Index<String,ServerEntry> ) index );
                }
                else if ( oid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
                {
                    getStore().addIndex( ( Index<String,ServerEntry> ) index );
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
            
            getStore().setUserIndices( userIndices );
        }

        getStore().initialize( getRegistries() );
    }
}
