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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.search.impl.CursorBuilder;
import org.apache.directory.server.xdbm.search.impl.DefaultOptimizer;
import org.apache.directory.server.xdbm.search.impl.DefaultSearchEngine;
import org.apache.directory.server.xdbm.search.impl.EvaluatorBuilder;
import org.apache.directory.server.xdbm.search.impl.NoOpOptimizer;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * An XDBM Partition backed by in memory AVL Trees.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AvlPartition extends BTreePartition
{
    private boolean optimizerEnabled = true;
    private Set<AvlIndex<?, ServerEntry>> indexedAttributes;

    private AvlStore<ServerEntry> store;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a store based on AVL Trees.
     */
    public AvlPartition()
    {
        store = new AvlStore<ServerEntry>();
        indexedAttributes = new HashSet<AvlIndex<?, ServerEntry>>();
    }


    public void initialize( ) throws Exception
    {
        setSchemaManager( schemaManager );

        EvaluatorBuilder evaluatorBuilder = new EvaluatorBuilder( store, schemaManager );
        CursorBuilder cursorBuilder = new CursorBuilder( store, evaluatorBuilder );

        // setup optimizer and registries for parent
        if ( !optimizerEnabled )
        {
            optimizer = new NoOpOptimizer();
        }
        else
        {
            optimizer = new DefaultOptimizer<ServerEntry>( store );
        }

        searchEngine = new DefaultSearchEngine( store, cursorBuilder, evaluatorBuilder, optimizer );

        // initialize the store
        store.setName( getId() );
        store.setSuffixDn( suffix.getName() );

        Set<Index<?, ServerEntry>> userIndices = new HashSet<Index<?, ServerEntry>>();

        for ( AvlIndex<?, ServerEntry> obj : indexedAttributes )
        {
            AvlIndex<?, ServerEntry> index;

            if ( obj instanceof AvlIndex )
            {
                index = ( AvlIndex<?, ServerEntry> ) obj;
            }
            else
            {
                index = new AvlIndex<Object, ServerEntry>();
                index.setAttributeId( obj.getAttributeId() );
            }

            String oid = schemaManager.getAttributeTypeRegistry().getOidByName( index.getAttributeId() );

            if ( SYS_INDEX_OIDS.contains( schemaManager.getAttributeTypeRegistry().getOidByName( index.getAttributeId() ) ) )
            {
                if ( oid.equals( ApacheSchemaConstants.APACHE_ALIAS_AT_OID ) )
                {
                    store.setAliasIndex( ( Index<String, ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID ) )
                {
                    store.setPresenceIndex( ( Index<String, ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_ONE_LEVEL_AT_OID ) )
                {
                    store.setOneLevelIndex( ( Index<Long, ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_N_DN_AT_OID ) )
                {
                    store.setNdnIndex( ( Index<String, ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_ONE_ALIAS_AT_OID ) )
                {
                    store.setOneAliasIndex( ( Index<Long, ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_SUB_ALIAS_AT_OID ) )
                {
                    store.setSubAliasIndex( ( Index<Long, ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_UP_DN_AT_OID ) )
                {
                    store.setUpdnIndex( ( Index<String, ServerEntry> ) index );
                }
                else if ( oid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
                {
                    store.addIndex( ( Index<String, ServerEntry> ) index );
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


    public final void destroy() throws Exception
    {
        store.destroy();
    }


    public final boolean isInitialized()
    {
        return store.isInitialized();
    }


    public final void sync() throws Exception
    {
        store.sync();
    }


    // ------------------------------------------------------------------------
    // I N D E X   M E T H O D S
    // ------------------------------------------------------------------------

    public final void addIndexOn( Index<Long, ServerEntry> index ) throws Exception
    {
        store.addIndex( index );
    }


    public Index<String, ServerEntry> getPresenceIndex()
    {
        return store.getPresenceIndex();
    }


    public Index<Long, ServerEntry> getSubLevelIndex()
    {
        return store.getSubLevelIndex();
    }


    public final Index<String, ServerEntry> getExistenceIndex()
    {
        return store.getPresenceIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setPresenceIndexOn( Index<String, ServerEntry> index ) throws Exception
    {
        store.setPresenceIndex( index );
    }


    public final Index<Long, ServerEntry> getOneLevelIndex()
    {
        return store.getOneLevelIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setOneLevelIndexOn( Index<Long, ServerEntry> index ) throws Exception
    {
        store.setOneLevelIndex( index );
    }


    public final Index<String, ServerEntry> getAliasIndex()
    {
        return store.getAliasIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setAliasIndexOn( Index<String, ServerEntry> index ) throws Exception
    {
        store.setAliasIndex( index );
    }


    public final Index<Long, ServerEntry> getOneAliasIndex()
    {
        return store.getOneAliasIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setOneAliasIndexOn( Index<Long, ServerEntry> index ) throws Exception
    {
        store.setOneAliasIndex( ( Index<Long, ServerEntry> ) index );
    }


    public final Index<Long, ServerEntry> getSubAliasIndex()
    {
        return store.getSubAliasIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setSubAliasIndexOn( Index<Long, ServerEntry> index ) throws Exception
    {
        store.setSubAliasIndex( ( Index<Long, ServerEntry> ) index );
    }


    public final Index<String, ServerEntry> getUpdnIndex()
    {
        return store.getUpdnIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setUpdnIndexOn( Index<String, ServerEntry> index ) throws Exception
    {
        store.setUpdnIndex( ( Index<String, ServerEntry> ) index );
    }


    public final Index<String, ServerEntry> getNdnIndex()
    {
        return store.getNdnIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setNdnIndexOn( Index<String, ServerEntry> index ) throws Exception
    {
        store.setNdnIndex( ( Index<String, ServerEntry> ) index );
    }


    public final Iterator<String> getUserIndices()
    {
        return store.userIndices();
    }


    public final Iterator<String> getSystemIndices()
    {
        return store.systemIndices();
    }


    public final boolean hasUserIndexOn( String id ) throws Exception
    {
        return store.hasUserIndexOn( id );
    }


    public final boolean hasSystemIndexOn( String id ) throws Exception
    {
        return store.hasSystemIndexOn( id );
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.BTreePartition#getUserIndex(String)
     */
    public final Index<?, ServerEntry> getUserIndex( String id ) throws IndexNotFoundException
    {
        return store.getUserIndex( id );
    }


    /**
     * @see BTreePartition#getEntryId(String)
     */
    public final Index<?, ServerEntry> getSystemIndex( String id ) throws IndexNotFoundException
    {
        return store.getSystemIndex( id );
    }


    public final Long getEntryId( String dn ) throws Exception
    {
        return store.getEntryId( dn );
    }


    public final String getEntryDn( Long id ) throws Exception
    {
        return store.getEntryDn( id );
    }


    public final Long getParentId( String dn ) throws Exception
    {
        return store.getParentId( dn );
    }


    public final Long getParentId( Long childId ) throws Exception
    {
        return store.getParentId( childId );
    }


    public final String getEntryUpdn( Long id ) throws Exception
    {
        return store.getEntryUpdn( id );
    }


    public final String getEntryUpdn( String dn ) throws Exception
    {
        return store.getEntryUpdn( dn );
    }


    public final int count() throws Exception
    {
        return store.count();
    }


    public final void add( AddOperationContext addContext ) throws Exception
    {
        store.add( ( ServerEntry ) ( ( ClonedServerEntry ) addContext.getEntry() ).getClonedEntry() );
    }


    public final void addContextEntry( ServerEntry contextEntry ) throws Exception
    {
        store.add( contextEntry );
    }


    public final ClonedServerEntry lookup( Long id ) throws Exception
    {
        return new ClonedServerEntry( store.lookup( id ) );
    }


    public final void delete( Long id ) throws Exception
    {
        store.delete( id );
    }


    public final IndexCursor<Long, ServerEntry> list( Long id ) throws Exception
    {
        return store.list( id );
    }


    public final int getChildCount( Long id ) throws Exception
    {
        return store.getChildCount( id );
    }


    public final void setProperty( String propertyName, String propertyValue ) throws Exception
    {
        store.setProperty( propertyName, propertyValue );
    }


    public final String getProperty( String propertyName ) throws Exception
    {
        return store.getProperty( propertyName );
    }

    
    /**
     * {@inheritDoc}
     */
    public final void modify( ModifyOperationContext modifyContext ) throws Exception
    {
        store.modify( modifyContext.getDn(), modifyContext.getModItems() );
    }

    
    /**
     * {@inheritDoc}
     */
    public final void modify( long entryId, List<Modification> modifications ) throws Exception
    {
        store.modify( entryId, modifications );
    }


    public final void rename( RenameOperationContext renameContext ) throws Exception
    {
        store.rename( renameContext.getDn(), renameContext.getNewRdn(), renameContext.getDelOldDn() );
    }


    public final void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws Exception
    {
        store.move( moveAndRenameContext.getDn(), moveAndRenameContext.getParent(), moveAndRenameContext.getNewRdn(),
            moveAndRenameContext.getDelOldDn() );
    }


    public final void move( MoveOperationContext moveContext ) throws Exception
    {
        store.move( moveContext.getDn(), moveContext.getParent() );
    }


    public void bind( BindOperationContext opContext ) throws Exception
    {
        throw new UnsupportedOperationException( "bind is not supported at the partition level" );
    }


    public void unbind( UnbindOperationContext opContext ) throws Exception
    {
        throw new UnsupportedOperationException( "unbind is not supported at the partition level" );
    }


    /*
     * TODO requires review 
     * 
     * This getter deviates from the norm. all the partitions
     * so far written never return a reference to store but I think that in this 
     * case the presence of this method gives significant ease and advantage to perform
     * add/delete etc. operations without creating a operation context.
     */
    public AvlStore<ServerEntry> getStore()
    {
        return store;
    }
    
}
