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


import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.XdbmPartition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.search.impl.CursorBuilder;
import org.apache.directory.server.xdbm.search.impl.DefaultOptimizer;
import org.apache.directory.server.xdbm.search.impl.DefaultSearchEngine;
import org.apache.directory.server.xdbm.search.impl.EvaluatorBuilder;
import org.apache.directory.server.xdbm.search.impl.NoOpOptimizer;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationNotSupportedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.NamingException;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
    private JdbmStore<ServerEntry> store;
    private boolean optimizerEnabled = true;
    private Set<Index<?,ServerEntry>> indexedAttributes;

    
    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a store based on JDBM B+Trees.
     */
    public JdbmPartition()
    {
        store = new JdbmStore<ServerEntry>();
        indexedAttributes = new HashSet<Index<?,ServerEntry>>();
    }


    // ------------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------


    public String getSuffix()
    {
        return super.suffix;
    }


    public void setSuffix( String suffix )
    {
        super.suffix = suffix;
    }


    public void setIndexedAttributes( Set<Index<?,ServerEntry>> indexedAttributes )
    {
        this.indexedAttributes = indexedAttributes;
    }


    public Set<Index<?,ServerEntry>> getIndexedAttributes()
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
        store.setSyncOnWrite( syncOnWrite );
    }


    public boolean isSyncOnWrite()
    {
        return store.isSyncOnWrite();
    }


    // ------------------------------------------------------------------------
    // E N D   C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public void setRegistries( Registries registries ) throws Exception
    {
        initRegistries( registries );
    }


    protected void initRegistries( Registries registries ) throws Exception
    {
        this.registries = registries;
        store.initRegistries( registries );
    }


    @SuppressWarnings("unchecked")
    public final void init( DirectoryService directoryService ) throws Exception
    {
        initRegistries( directoryService.getRegistries() );

        EvaluatorBuilder evaluatorBuilder = new EvaluatorBuilder( store, registries );
        CursorBuilder cursorBuilder = new CursorBuilder( store, evaluatorBuilder );

        // setup optimizer and registries for parent
        if ( ! optimizerEnabled )
        {
            optimizer = new NoOpOptimizer();
        }
        else
        {
            optimizer = new DefaultOptimizer<ServerEntry>( store );
        }

        searchEngine = new DefaultSearchEngine( store, cursorBuilder, evaluatorBuilder, optimizer );
        
        // initialize the store
        store.setCacheSize( cacheSize );
        store.setName( id );
        store.setSuffixDn( suffix );
        store.setWorkingDirectory( new File( directoryService.getWorkingDirectory().getPath() + File.separator + id ) );

        Set<Index<?,ServerEntry>> userIndices = new HashSet<Index<?,ServerEntry>>();
        
        for ( Index<?,ServerEntry> obj : indexedAttributes )
        {
            Index<?,ServerEntry> index;

            if ( obj instanceof JdbmIndex )
            {
                index = ( JdbmIndex<?,ServerEntry> ) obj;
            }
            else
            {
                index = new JdbmIndex<Object,ServerEntry>();
                index.setAttributeId( obj.getAttributeId() );
                index.setCacheSize( obj.getCacheSize() );
                index.setWkDirPath( obj.getWkDirPath() );
            }

            String oid = registries.getOidRegistry().getOid( index.getAttributeId() );
            
            if ( SYS_INDEX_OIDS.contains( registries.getOidRegistry().getOid( index.getAttributeId() ) ) )
            {
                if ( oid.equals( ApacheSchemaConstants.APACHE_ALIAS_OID ) )
                {
                    store.setAliasIndex( ( Index<String,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_EXISTANCE_OID ) )
                {
                    store.setPresenceIndex( ( Index<String,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_ONE_LEVEL_OID ) )
                {
                    store.setOneLevelIndex( ( Index<Long,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_N_DN_OID ) )
                {
                    store.setNdnIndex( ( Index<String,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_ONE_ALIAS_OID ) )
                {
                    store.setOneAliasIndex( ( Index<Long,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_SUB_ALIAS_OID ) )
                {
                    store.setSubAliasIndex( ( Index<Long,ServerEntry> ) index );
                }
                else if ( oid.equals( ApacheSchemaConstants.APACHE_UP_DN_OID ) )
                {
                    store.setUpdnIndex( ( Index<String,ServerEntry> ) index );
                }
                else if ( oid.equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
                {
                    store.addIndex( ( Index<String,ServerEntry> ) index );
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

        store.init( registries );
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


    public final Index<String, ServerEntry> getExistanceIndex()
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


    public final Index<Long,ServerEntry> getOneAliasIndex()
    {
        return store.getOneAliasIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setOneAliasIndexOn( Index<Long,ServerEntry> index ) throws NamingException
    {
        store.setOneAliasIndex( ( Index<Long,ServerEntry> ) index );
    }


    public final Index<Long,ServerEntry> getSubAliasIndex()
    {
        return store.getSubAliasIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setSubAliasIndexOn( Index<Long,ServerEntry> index ) throws NamingException
    {
            store.setSubAliasIndex( ( Index<Long,ServerEntry> ) index );
    }


    public final Index<String,ServerEntry> getUpdnIndex()
    {
        return store.getUpdnIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setUpdnIndexOn( Index<String,ServerEntry> index ) throws NamingException
    {
        store.setUpdnIndex( ( Index<String,ServerEntry> ) index );
    }


    public final Index<String,ServerEntry> getNdnIndex()
    {
        return store.getNdnIndex();
    }


    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public final void setNdnIndexOn( Index<String,ServerEntry> index ) throws NamingException
    {
        store.setNdnIndex( ( Index<String,ServerEntry> ) index );
    }


    public final Iterator<String> getUserIndices()
    {
        return store.userIndices();
    }


    public final Iterator<String> getSystemIndices()
    {
        return store.systemIndices();
    }


    public final boolean hasUserIndexOn( String id ) throws NamingException
    {
        return store.hasUserIndexOn( id );
    }


    public final boolean hasSystemIndexOn( String id ) throws NamingException
    {
        return store.hasSystemIndexOn( id );
    }


    /**
     * @see org.apache.directory.server.core.partition.impl.btree.XdbmPartition#getUserIndex(String)
     */
    public final Index<?,ServerEntry> getUserIndex( String id ) throws IndexNotFoundException
    {
        return store.getUserIndex( id );
    }


    /**
     * @see XdbmPartition#getEntryId(String)
     */
    public final Index<?,ServerEntry> getSystemIndex( String id ) throws IndexNotFoundException
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
        store.add( addContext.getDn(), (ServerEntry)((ClonedServerEntry)addContext.getEntry()).getClonedEntry() );
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


    public final LdapDN getSuffixDn()
    {
        return store.getSuffix();
    }

    public final LdapDN getUpSuffixDn()
    {
        return store.getUpSuffix();
    }


    public final void setProperty( String propertyName, String propertyValue ) throws Exception
    {
        store.setProperty( propertyName, propertyValue );
    }


    public final String getProperty( String propertyName ) throws Exception
    {
        return store.getProperty( propertyName );
    }

    
    public final void modify( ModifyOperationContext modifyContext ) throws Exception
    {
        store.modify( modifyContext.getDn(), modifyContext.getModItems() );
    }

    public final void rename( RenameOperationContext renameContext ) throws Exception
    {
        store.rename( renameContext.getDn(), renameContext.getNewRdn(), renameContext.getDelOldDn() );
    }


    public final void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws Exception
    {
        store.move( moveAndRenameContext.getDn(), 
            moveAndRenameContext.getParent(), 
            moveAndRenameContext.getNewRdn(), 
            moveAndRenameContext.getDelOldDn() );
    }


    public final void move( MoveOperationContext moveContext ) throws Exception
    {
        store.move( moveContext.getDn(), moveContext.getParent() );
    }


    public final void bind( LdapDN bindDn, byte[] credentials, List<String> mechanisms, String saslAuthId ) throws Exception
    {
        if ( bindDn == null || credentials == null || mechanisms == null ||  saslAuthId == null )
        {
            // do nothing just using variables to prevent yellow lights : bad :)
        }
        
        // does nothing
        throw new LdapAuthenticationNotSupportedException(
                "Bind requests only tunnel down into partitions if there are no authenticators to handle the mechanism.\n"
                        + "Check to see if you have correctly configured authenticators for the server.",
                ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED );
    }
    

    public final void bind( BindOperationContext bindContext ) throws Exception
    {
        // does nothing
        throw new LdapAuthenticationNotSupportedException(
            "Bind requests only tunnel down into partitions if there are no authenticators to handle the mechanism.\n"
                + "Check to see if you have correctly configured authenticators for the server.",
            ResultCodeEnum.AUTH_METHOD_NOT_SUPPORTED );
    }


    public final void unbind( UnbindOperationContext unbindContext ) throws Exception
    {
    }


    public Index<String, ServerEntry> getPresenceIndex()
    {
        return store.getPresenceIndex();
    }


    public Index<Long, ServerEntry> getSubLevelIndex()
    {
        return store.getSubLevelIndex();
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "Partition<" + id + ">"; 
    }
}
