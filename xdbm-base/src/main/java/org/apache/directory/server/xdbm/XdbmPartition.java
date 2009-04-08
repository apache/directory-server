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
package org.apache.directory.server.xdbm;


import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.xdbm.search.Optimizer;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationNotSupportedException;
import org.apache.directory.shared.ldap.exception.LdapContextNotEmptyException;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;

import javax.naming.directory.SearchControls;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * An abstract {@link Partition} that uses general BTree operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class XdbmPartition implements Partition
{
    protected static final Set<String> SYS_INDEX_OIDS;

    static
    {
        Set<String> set = new HashSet<String>();
        set.add( SchemaConstants.OBJECT_CLASS_AT_OID );
        set.add( ApacheSchemaConstants.APACHE_ALIAS_OID );
        set.add( ApacheSchemaConstants.APACHE_EXISTANCE_OID );
        set.add( ApacheSchemaConstants.APACHE_ONE_LEVEL_OID );
        set.add( ApacheSchemaConstants.APACHE_N_DN_OID );
        set.add( ApacheSchemaConstants.APACHE_ONE_ALIAS_OID );
        set.add( ApacheSchemaConstants.APACHE_SUB_ALIAS_OID );
        set.add( ApacheSchemaConstants.APACHE_UP_DN_OID );
        SYS_INDEX_OIDS = Collections.unmodifiableSet( set );
    }

    /** the search engine used to search the database */
    protected SearchEngine<ServerEntry> searchEngine;
    protected Optimizer optimizer;

    private Store<ServerEntry> store;
    private Registries registries;

    private String id;



    /**
     * Creates a B-tree based context partition.
     */
    protected XdbmPartition()
    {
    }

    
    /**
     * Gets the unique identifier for this partition.
     *
     * @return the unique identifier for this partition
     */
    public String getId()
    {
        return id;
    }


    /**
     * Sets the unique identifier for this partition.
     *
     * @param id the unique identifier for this partition
     */
    public void setId( String id )
    {
        this.id = id;
    }
    
    
    protected void setStore( Store<ServerEntry> store )
    {
        this.store = store;
    }
    
    
    protected Store<ServerEntry> getStore()
    {
        return this.store;
    }
    
    
    // ------------------------------------------------------------------------
    // Public methods - not declared in any interfaces just for this class
    // ------------------------------------------------------------------------


    /**
     * Allows for schema entity registries to be swapped out during runtime.  This is 
     * primarily here to facilitate the swap out of a temporary bootstrap registry.  
     * Registry changes require swapping out the search engine used by a partition 
     * since the registries are used by elements in the search engine.
     * 
     * @org.apache.xbean.Property hidden="true"
     * @param registries the schema entity registries
     * @throws Exception 
     */
    public void setRegistries( Registries registries ) throws Exception
    {
        this.registries = registries;
    }

    
    public Registries getRegistries()
    {
        return registries;
    }
    
    
    /**
     * Gets the DefaultSearchEngine used by this ContextPartition to search the
     * Database. 
     *
     * @return the search engine
     */
    public SearchEngine<ServerEntry> getSearchEngine()
    {
        return searchEngine;
    }


    // ------------------------------------------------------------------------
    // Partition Interface Method Implementations
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
    public final void setOneAliasIndexOn( Index<Long,ServerEntry> index ) throws Exception
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
    public final void setSubAliasIndexOn( Index<Long,ServerEntry> index ) throws Exception
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
    public final void setUpdnIndexOn( Index<String,ServerEntry> index ) throws Exception
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
    public final void setNdnIndexOn( Index<String,ServerEntry> index ) throws Exception
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


    public final boolean hasUserIndexOn( String id ) throws Exception
    {
        return store.hasUserIndexOn( id );
    }


    public final boolean hasSystemIndexOn( String id ) throws Exception
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

    public final LdapDN getUpSuffixDn() throws Exception
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
        return "Partition<" + getId() + ">"; 
    }
    

    public void delete( DeleteOperationContext opContext ) throws Exception
    {
        LdapDN dn = opContext.getDn();
        
        Long id = getEntryId( dn.getNormName() );

        // don't continue if id is null
        if ( id == null )
        {
            throw new LdapNameNotFoundException( "Could not find entry at '" + dn + "' to delete it!" );
        }

        if ( getChildCount( id ) > 0 )
        {
            LdapContextNotEmptyException cnee = new LdapContextNotEmptyException( "[66] Cannot delete entry " + dn
                + " it has children!" );
            cnee.setRemainingName( dn );
            throw cnee;
        }

        delete( id );
    }


    public EntryFilteringCursor list( ListOperationContext opContext ) throws Exception
    {
        return new BaseEntryFilteringCursor( new ServerEntryCursorAdaptor( this, 
            list( getEntryId( opContext.getDn().getNormName() ) ) ), opContext );
    }


    public EntryFilteringCursor search( SearchOperationContext opContext ) throws Exception
    {
        SearchControls searchCtls = opContext.getSearchControls();
        IndexCursor<Long,ServerEntry> underlying;

        underlying = searchEngine.cursor( 
            opContext.getDn(),
            opContext.getAliasDerefMode(),
            opContext.getFilter(), 
            searchCtls );

        return new BaseEntryFilteringCursor( new ServerEntryCursorAdaptor( this, underlying ), opContext );
    }


    public ClonedServerEntry lookup( LookupOperationContext opContext ) throws Exception
    {
        Long id = getEntryId( opContext.getDn().getNormName() );
        
        if ( id == null )
        {
            return null;
        }
        
        ClonedServerEntry entry = lookup( id );

        if ( ( opContext.getAttrsId() == null ) || ( opContext.getAttrsId().size() == 0 ) )
        {
            return entry;
        }

        for ( AttributeType attributeType : ((ServerEntry)entry.getOriginalEntry()).getAttributeTypes() )
        {
            if ( ! opContext.getAttrsId().contains( attributeType.getOid() ) )
            {
                entry.removeAttributes( attributeType );
            }
        }

        return entry;
    }


    public boolean hasEntry( EntryOperationContext opContext ) throws Exception
    {
        return null != getEntryId( opContext.getDn().getNormName() );
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
}
