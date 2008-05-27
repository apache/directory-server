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
package org.apache.directory.server.core.partition.impl.btree;


import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.gui.PartitionViewer;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.xdbm.*;
import org.apache.directory.server.xdbm.search.Optimizer;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.shared.ldap.exception.LdapContextNotEmptyException;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;

import javax.naming.directory.SearchControls;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * An abstract {@link Partition} that uses general BTree operations.
 *
 * @org.apache.xbean.XBean
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class BTreePartition implements Partition
{
    protected static final Set<String> SYS_INDEX_OIDS;

    static
    {
        Set<String> set = new HashSet<String>();
        set.add( Store.ALIAS );
        set.add( Store.PRESENCE );
        set.add( Store.ONELEVEL );
        set.add( Store.NDN );
        set.add( Store.ONEALIAS );
        set.add( Store.SUBALIAS );
        set.add( Store.UPDN );
        SYS_INDEX_OIDS = Collections.unmodifiableSet( set );
    }

    /** the search engine used to search the database */
    protected SearchEngine<ServerEntry> searchEngine;
    protected Optimizer optimizer;

    protected Registries registries;

    protected String id;
    protected int cacheSize = -1;
    protected LdapDN suffixDn;
    protected String suffix;
    
    /** The rootDSE context */
    protected ServerEntry contextEntry;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a B-tree based context partition.
     */
    protected BTreePartition()
    {
    }

    
    // ------------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------


    /**
     * Used to specify the entry cache size for a Partition.  Various Partition
     * implementations may interpret this value in different ways: i.e. total cache
     * size limit verses the number of entries to cache.
     *
     * @param cacheSize the maximum size of the cache in the number of entries
     */
    public void setCacheSize( int cacheSize )
    {
        this.cacheSize = cacheSize;
    }


    /**
     * Gets the entry cache size for this BTreePartition.
     *
     * @return the maximum size of the cache as the number of entries maximum before paging out
     */
    public int getCacheSize()
    {
        return cacheSize;
    }


    /**
     * Returns root entry for this BTreePartition.
     *
     * @return the root suffix entry for this BTreePartition
     */
    public ServerEntry getContextEntry()
    {
        if ( contextEntry != null )
        {
            return new ClonedServerEntry( contextEntry );
        }
        else
        {
            return null;
        }
    }


    /**
     * Sets root entry for this BTreePartition.
     *
     * @param rootEntry the root suffix entry of this BTreePartition
     */
    public void setContextEntry( ServerEntry rootEntry )
    {
        this.contextEntry = ( ServerEntry ) rootEntry.clone();
    }

    
    /**
     * Sets root entry for this BTreePartition.
     *
     * @param rootEntry the root suffix entry of this BTreePartition
     */
    public void setContextEntry( String rootEntry )
    {
        System.out.println( rootEntry );
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
    
    
    // -----------------------------------------------------------------------
    // E N D   C O N F I G U R A T I O N   M E T H O D S
    // -----------------------------------------------------------------------


    /**
     * Allows for schema entity registries to be swapped out during runtime.  This is 
     * primarily here to facilitate the swap out of a temporary bootstrap registry.  
     * Registry changes require swapping out the search engine used by a partition 
     * since the registries are used by elements in the search engine.
     * 
     * @param registries the schema entity registries
     * @throws Exception 
     */
    public abstract void setRegistries( Registries registries ) throws Exception;

    
    // ------------------------------------------------------------------------
    // Public Accessors - not declared in any interfaces just for this class
    // ------------------------------------------------------------------------

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


    public abstract void add( AddOperationContext opContext ) throws Exception;


    public abstract void modify( ModifyOperationContext opContext ) throws Exception;


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

        for ( AttributeType attributeType : entry.getOriginalEntry().getAttributeTypes() )
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


    public abstract void rename( RenameOperationContext opContext ) throws Exception;


    public abstract void move( MoveOperationContext opContext ) throws Exception;


    public abstract void moveAndRename( MoveAndRenameOperationContext opContext ) throws Exception;


    public abstract void sync() throws Exception;


    public abstract void destroy() throws Exception;


    public abstract boolean isInitialized();


    public void inspect() throws Exception
    {
        PartitionViewer viewer = new PartitionViewer( this, registries );
        viewer.execute();
    }


    ////////////////////
    // public abstract methods

    // ------------------------------------------------------------------------
    // Index Operations 
    // ------------------------------------------------------------------------

    public abstract void addIndexOn( Index<Long,ServerEntry> index ) throws Exception;


    public abstract boolean hasUserIndexOn( String attribute ) throws Exception;


    public abstract boolean hasSystemIndexOn( String attribute ) throws Exception;


    public abstract Index<String,ServerEntry> getPresenceIndex();


    /**
     * Gets the Index mapping the Long primary keys of parents to the 
     * Long primary keys of their children.
     *
     * @return the one level Index
     */
    public abstract Index<Long,ServerEntry> getOneLevelIndex();


    /**
     * Gets the Index mapping the Long primary keys of ancestors to the 
     * Long primary keys of their descendants.
     *
     * @return the sub tree level Index
     */
    public abstract Index<Long,ServerEntry> getSubLevelIndex();


    /**
     * Gets the Index mapping user provided distinguished names of entries as 
     * Strings to the BigInteger primary keys of entries.
     *
     * @return the user provided distinguished name Index
     */
    public abstract Index<String,ServerEntry> getUpdnIndex();


    /**
     * Gets the Index mapping the normalized distinguished names of entries as
     * Strings to the BigInteger primary keys of entries.  
     *
     * @return the normalized distinguished name Index
     */
    public abstract Index<String,ServerEntry> getNdnIndex();


    /**
     * Gets the alias index mapping parent entries with scope expanding aliases 
     * children one level below them; this system index is used to dereference
     * aliases on one/single level scoped searches.
     * 
     * @return the one alias index
     */
    public abstract Index<Long,ServerEntry> getOneAliasIndex();


    /**
     * Gets the alias index mapping relative entries with scope expanding 
     * alias descendents; this system index is used to dereference aliases on 
     * subtree scoped searches.
     * 
     * @return the sub alias index
     */
    public abstract Index<Long,ServerEntry> getSubAliasIndex();


    /**
     * Gets the system index defined on the ALIAS_ATTRIBUTE which for LDAP would
     * be the aliasedObjectName and for X.500 would be aliasedEntryName.
     * 
     * @return the index on the ALIAS_ATTRIBUTE
     */
    public abstract Index<String,ServerEntry> getAliasIndex();


    /**
     * Sets the system index defined on the ALIAS_ATTRIBUTE which for LDAP would
     * be the aliasedObjectName and for X.500 would be aliasedEntryName.
     * 
     * @param index the index on the ALIAS_ATTRIBUTE
     * @throws Exception if there is a problem setting up the index
     */
    public abstract void setAliasIndexOn( Index<String,ServerEntry> index ) throws Exception;


    /**
     * Sets the attribute existance Index.
     *
     * @param index the attribute existance Index
     * @throws Exception if there is a problem setting up the index
     */
    public abstract void setPresenceIndexOn( Index<String,ServerEntry> index ) throws Exception;


    /**
     * Sets the one level Index.
     *
     * @param index the one level Index
     * @throws Exception if there is a problem setting up the index
     */
    public abstract void setOneLevelIndexOn( Index<Long,ServerEntry> index ) throws Exception;

    // TODO - add sub level index setter

    /**
     * Sets the user provided distinguished name Index.
     *
     * @param index the updn Index
     * @throws Exception if there is a problem setting up the index
     */
    public abstract void setUpdnIndexOn( Index<String,ServerEntry> index ) throws Exception;


    /**
     * Sets the normalized distinguished name Index.
     *
     * @param index the ndn Index
     * @throws Exception if there is a problem setting up the index
     */
    public abstract void setNdnIndexOn( Index<String,ServerEntry> index ) throws Exception;


    /**
     * Sets the alias index mapping parent entries with scope expanding aliases 
     * children one level below them; this system index is used to dereference
     * aliases on one/single level scoped searches.
     * 
     * @param index a one level alias index
     * @throws Exception if there is a problem setting up the index
     */
    public abstract void setOneAliasIndexOn( Index<Long,ServerEntry> index ) throws Exception;


    /**
     * Sets the alias index mapping relative entries with scope expanding 
     * alias descendents; this system index is used to dereference aliases on 
     * subtree scoped searches.
     * 
     * @param index a subtree alias index
     * @throws Exception if there is a problem setting up the index
     */
    public abstract void setSubAliasIndexOn( Index<Long,ServerEntry> index ) throws Exception;


    public abstract Index<?,ServerEntry> getUserIndex( String attribute ) throws Exception;


    public abstract Index<?,ServerEntry> getSystemIndex( String attribute ) throws Exception;


    public abstract Long getEntryId( String dn ) throws Exception;


    public abstract String getEntryDn( Long id ) throws Exception;


    public abstract Long getParentId( String dn ) throws Exception;


    public abstract Long getParentId( Long childId ) throws Exception;


    /**
     * Gets the user provided distinguished name.
     *
     * @param id the entry id
     * @return the user provided distinguished name
     * @throws Exception if the updn index cannot be accessed
     */
    public abstract String getEntryUpdn( Long id ) throws Exception;


    /**
     * Gets the user provided distinguished name.
     *
     * @param dn the normalized distinguished name
     * @return the user provided distinguished name
     * @throws Exception if the updn and ndn indices cannot be accessed
     */
    public abstract String getEntryUpdn( String dn ) throws Exception;


    public abstract ClonedServerEntry lookup( Long id ) throws Exception;


    public abstract void delete( Long id ) throws Exception;


    public abstract IndexCursor<Long,ServerEntry> list( Long id ) throws Exception;


    public abstract int getChildCount( Long id ) throws Exception;


    public abstract ServerEntry getSuffixEntry() throws Exception;


    public abstract void setProperty( String key, String value ) throws Exception;


    public abstract String getProperty( String key ) throws Exception;


    public abstract Iterator<String> getUserIndices();


    public abstract Iterator<String> getSystemIndices();


    /**
     * Gets the count of the total number of entries in the database.
     *
     * TODO shouldn't this be a BigInteger instead of an int? 
     * 
     * @return the number of entries in the database 
     * @throws Exception if there is a failure to read the count
     */
    public abstract int count() throws Exception;
}
