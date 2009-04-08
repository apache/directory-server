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
package org.apache.directory.server.core.partition.jdbm;


import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.xdbm.XdbmPartition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.search.impl.CursorBuilder;
import org.apache.directory.server.xdbm.search.impl.DefaultOptimizer;
import org.apache.directory.server.xdbm.search.impl.DefaultSearchEngine;
import org.apache.directory.server.xdbm.search.impl.EvaluatorBuilder;
import org.apache.directory.server.xdbm.search.impl.NoOpOptimizer;
import org.apache.directory.shared.ldap.constants.SchemaConstants;

import java.io.File;
import java.util.HashSet;
import java.util.Set;


/**
 * A {@link Partition} that stores entries in
 * <a href="http://jdbm.sourceforge.net/">JDBM</a> database.
 *
 * @org.apache.xbean.XBean
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmPartition extends XdbmPartition
{
    private boolean optimizerEnabled = true;
    private Set<JdbmIndex<?,ServerEntry>> indexedAttributes;
    private int cacheSize;
    private String suffix;
    
    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a store based on JDBM B+Trees.
     */
    public JdbmPartition()
    {
        super();
        super.setStore( new JdbmStore<ServerEntry>() );
        indexedAttributes = new HashSet<JdbmIndex<?,ServerEntry>>();
    }


    // ------------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------


    public void setSuffix( String suffix )
    {
        this.suffix = suffix;
    }


    /**
     * @param cacheSize the cacheSize to set
     */
    public void setCacheSize( int cacheSize )
    {
        this.cacheSize = cacheSize;
    }


    /**
     * @return the cacheSize
     */
    public int getCacheSize()
    {
        return cacheSize;
    }


    public void setIndexedAttributes( Set<JdbmIndex<?,ServerEntry>> indexedAttributes )
    {
        this.indexedAttributes = indexedAttributes;
    }


    public Set<JdbmIndex<?,ServerEntry>> getIndexedAttributes()
    {
        return indexedAttributes;
    }


    public boolean isOptimizerEnabled()
    {
        return optimizerEnabled;
    }


    public void setOptimizerEnabled( boolean optimizerEnabled )
    {
        this.optimizerEnabled = optimizerEnabled;
    }


    public void setSyncOnWrite( boolean syncOnWrite )
    {
        getJdbmStore().setSyncOnWrite( syncOnWrite );
    }


    public boolean isSyncOnWrite()
    {
        return getJdbmStore().isSyncOnWrite();
    }


    private JdbmStore<ServerEntry> getJdbmStore()
    {
        return ( JdbmStore<ServerEntry> ) getStore();
    }
    
    
    // ------------------------------------------------------------------------
    // E N D   C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------


    @SuppressWarnings("unchecked")
    public final void init( DirectoryService directoryService ) throws Exception
    {
        setRegistries( directoryService.getRegistries() );

        EvaluatorBuilder evaluatorBuilder = new EvaluatorBuilder( getJdbmStore(), getRegistries() );
        CursorBuilder cursorBuilder = new CursorBuilder( getJdbmStore(), evaluatorBuilder );

        // setup optimizer and registries for parent
        if ( ! optimizerEnabled )
        {
            optimizer = new NoOpOptimizer();
        }
        else
        {
            optimizer = new DefaultOptimizer<ServerEntry>( getJdbmStore() );
        }

        searchEngine = new DefaultSearchEngine( getJdbmStore(), cursorBuilder, evaluatorBuilder, optimizer );
        
        // initialize the store
        getJdbmStore().setCacheSize( getCacheSize() );
        getJdbmStore().setName( getId() );
        getJdbmStore().setSuffixDn( suffix );
        getJdbmStore().setWorkingDirectory( new File( directoryService.getWorkingDirectory().getPath() + File.separator + getId() ) );

        Set<JdbmIndex<?,ServerEntry>> userIndices = new HashSet<JdbmIndex<?,ServerEntry>>();
        
        for ( JdbmIndex<?,ServerEntry> obj : indexedAttributes )
        {
            JdbmIndex<?,ServerEntry> index;

            if ( obj instanceof JdbmIndex )
            {
                index = (org.apache.directory.server.core.partition.jdbm.JdbmIndex<?,ServerEntry> ) obj;
            }
            else
            {
                index = new JdbmIndex<Object,ServerEntry>();
                index.setAttributeId( obj.getAttributeId() );
                index.setCacheSize( obj.getCacheSize() );
                index.setWkDirPath( obj.getWkDirPath() );
            }

            String oid = getRegistries().getOidRegistry().getOid( index.getAttributeId() );
            
            if ( SYS_INDEX_OIDS.contains( getRegistries().getOidRegistry().getOid( index.getAttributeId() ) ) )
            {
                if ( oid.equals( ApacheSchemaConstants.APACHE_ALIAS_OID ) )
                {
                    getJdbmStore().setAliasIndex( ( Index<String,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_EXISTANCE_OID ) )
                {
                    getJdbmStore().setPresenceIndex( ( Index<String,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_ONE_LEVEL_OID ) )
                {
                    getJdbmStore().setOneLevelIndex( ( Index<Long,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_N_DN_OID ) )
                {
                    getJdbmStore().setNdnIndex( ( Index<String,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_ONE_ALIAS_OID ) )
                {
                    getJdbmStore().setOneAliasIndex( ( Index<Long,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_SUB_ALIAS_OID ) )
                {
                    getJdbmStore().setSubAliasIndex( ( Index<Long,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_UP_DN_OID ) )
                {
                    getJdbmStore().setUpdnIndex( ( Index<String,ServerEntry> ) index );
                }
                else if ( oid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
                {
                    getJdbmStore().addIndex( ( Index<String,ServerEntry> ) index );
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
            
            getJdbmStore().setUserIndices( userIndices );
        }

        getJdbmStore().init( getRegistries() );
    }
}
