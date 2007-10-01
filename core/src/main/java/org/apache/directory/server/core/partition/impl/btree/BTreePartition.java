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


import org.apache.directory.server.core.enumeration.SearchResultEnumeration;
import org.apache.directory.server.core.interceptor.context.*;
import org.apache.directory.server.core.partition.Oid;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.gui.PartitionViewer;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.exception.LdapContextNotEmptyException;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * An abstract {@link Partition} that uses general BTree operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class BTreePartition implements Partition
{
    protected static final Set<String> SYS_INDEX_OIDS;

    static
    {
        Set<String> set = new HashSet<String>();
        set.add( Oid.ALIAS );
        set.add( Oid.EXISTANCE );
        set.add( Oid.HIERARCHY );
        set.add( Oid.NDN );
        set.add( Oid.ONEALIAS );
        set.add( Oid.SUBALIAS );
        set.add( Oid.UPDN );
        SYS_INDEX_OIDS = Collections.unmodifiableSet( set );
    }

    /** the search engine used to search the database */
    protected SearchEngine searchEngine;
    protected Optimizer optimizer;

    protected AttributeTypeRegistry attributeTypeRegistry;
    protected OidRegistry oidRegistry;

    protected String id;
    protected int cacheSize = -1;
    protected LdapDN suffixDn;
    protected String suffix;
    protected Attributes contextEntry = new AttributesImpl( true );


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
    public Attributes getContextEntry()
    {
        return ( Attributes ) contextEntry.clone();
    }


    /**
     * Sets root entry for this BTreePartition.
     *
     * @param rootEntry the root suffix entry of this BTreePartition
     */
    public void setContextEntry( Attributes rootEntry )
    {
        this.contextEntry = ( Attributes ) rootEntry.clone();
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
     */
    public abstract void setRegistries( Registries registries );

    
    // ------------------------------------------------------------------------
    // Public Accessors - not declared in any interfaces just for this class
    // ------------------------------------------------------------------------

    /**
     * Gets the DefaultSearchEngine used by this ContextPartition to search the
     * Database. 
     *
     * @return the search engine
     */
    public SearchEngine getSearchEngine()
    {
        return searchEngine;
    }


    // ------------------------------------------------------------------------
    // Partition Interface Method Implementations
    // ------------------------------------------------------------------------


    public void delete( DeleteOperationContext opContext ) throws NamingException
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


    public abstract void add( AddOperationContext opContext ) throws NamingException;


    public abstract void modify( ModifyOperationContext opContext ) throws NamingException;


    private static final String[] ENTRY_DELETED_ATTRS = new String[] { "entrydeleted" };


    public NamingEnumeration<SearchResult> list( ListOperationContext opContext ) throws NamingException
    {
        SearchResultEnumeration list;
        list = new BTreeSearchResultEnumeration( ENTRY_DELETED_ATTRS, list( getEntryId( opContext.getDn().getNormName() ) ),
            this, attributeTypeRegistry );
        return list;
    }


    public NamingEnumeration<SearchResult> search( SearchOperationContext opContext )
        throws NamingException
    {
        SearchControls searchCtls = opContext.getSearchControls();
        String[] attrIds = searchCtls.getReturningAttributes();
        NamingEnumeration underlying;

        underlying = searchEngine.search( 
            opContext.getDn(), 
            opContext.getEnv(), 
            opContext.getFilter(), 
            searchCtls );

        //noinspection unchecked
        return new BTreeSearchResultEnumeration( attrIds, underlying, this, attributeTypeRegistry );
    }


    public Attributes lookup( LookupOperationContext opContext ) throws NamingException
    {
        Attributes entry = lookup( getEntryId( opContext.getDn().getNormName() ) );

        if ( ( opContext.getAttrsId() == null ) || ( opContext.getAttrsId().size() == 0 ) )
        {
            return entry;
        }

        Attributes retval = new AttributesImpl();

        for ( String attrId:opContext.getAttrsId() )
        {
            Attribute attr = entry.get( attrId );

            if ( attr != null )
            {
                retval.put( attr );
            }
        }

        return retval;
    }


    public boolean hasEntry( EntryOperationContext opContext ) throws NamingException
    {
        return null != getEntryId( opContext.getDn().getNormName() );
    }


    public abstract void rename( RenameOperationContext opContext ) throws NamingException;


    public abstract void move( MoveOperationContext opContext ) throws NamingException;


    public abstract void moveAndRename( MoveAndRenameOperationContext opContext )
        throws NamingException;


    public abstract void sync() throws NamingException;


    public abstract void destroy();


    public abstract boolean isInitialized();


    public void inspect() throws Exception
    {
        PartitionViewer viewer = new PartitionViewer( this, searchEngine );
        viewer.execute();
    }


    ////////////////////
    // public abstract methods

    // ------------------------------------------------------------------------
    // Index Operations 
    // ------------------------------------------------------------------------

    public abstract void addIndexOn( Index index ) throws NamingException;


    public abstract boolean hasUserIndexOn( String attribute ) throws NamingException;


    public abstract boolean hasSystemIndexOn( String attribute ) throws NamingException;


    public abstract Index getExistanceIndex();


    /**
     * Gets the Index mapping the BigInteger primary keys of parents to the 
     * BigInteger primary keys of their children.
     *
     * @return the hierarchy Index
     */
    public abstract Index getHierarchyIndex();


    /**
     * Gets the Index mapping user provided distinguished names of entries as 
     * Strings to the BigInteger primary keys of entries.
     *
     * @return the user provided distinguished name Index
     */
    public abstract Index getUpdnIndex();


    /**
     * Gets the Index mapping the normalized distinguished names of entries as
     * Strings to the BigInteger primary keys of entries.  
     *
     * @return the normalized distinguished name Index
     */
    public abstract Index getNdnIndex();


    /**
     * Gets the alias index mapping parent entries with scope expanding aliases 
     * children one level below them; this system index is used to dereference
     * aliases on one/single level scoped searches.
     * 
     * @return the one alias index
     */
    public abstract Index getOneAliasIndex();


    /**
     * Gets the alias index mapping relative entries with scope expanding 
     * alias descendents; this system index is used to dereference aliases on 
     * subtree scoped searches.
     * 
     * @return the sub alias index
     */
    public abstract Index getSubAliasIndex();


    /**
     * Gets the system index defined on the ALIAS_ATTRIBUTE which for LDAP would
     * be the aliasedObjectName and for X.500 would be aliasedEntryName.
     * 
     * @return the index on the ALIAS_ATTRIBUTE
     */
    public abstract Index getAliasIndex();


    /**
     * Sets the system index defined on the ALIAS_ATTRIBUTE which for LDAP would
     * be the aliasedObjectName and for X.500 would be aliasedEntryName.
     * 
     * @param index the index on the ALIAS_ATTRIBUTE
     * @throws NamingException if there is a problem setting up the index
     */
    public abstract void setAliasIndexOn( Index index ) throws NamingException;


    /**
     * Sets the attribute existance Index.
     *
     * @param index the attribute existance Index
     * @throws NamingException if there is a problem setting up the index
     */
    public abstract void setExistanceIndexOn( Index index ) throws NamingException;


    /**
     * Sets the hierarchy Index.
     *
     * @param index the hierarchy Index
     * @throws NamingException if there is a problem setting up the index
     */
    public abstract void setHierarchyIndexOn( Index index ) throws NamingException;


    /**
     * Sets the user provided distinguished name Index.
     *
     * @param index the updn Index
     * @throws NamingException if there is a problem setting up the index
     */
    public abstract void setUpdnIndexOn( Index index ) throws NamingException;


    /**
     * Sets the normalized distinguished name Index.
     *
     * @param index the ndn Index
     * @throws NamingException if there is a problem setting up the index
     */
    public abstract void setNdnIndexOn( Index index ) throws NamingException;


    /**
     * Sets the alias index mapping parent entries with scope expanding aliases 
     * children one level below them; this system index is used to dereference
     * aliases on one/single level scoped searches.
     * 
     * @param index a one level alias index
     * @throws NamingException if there is a problem setting up the index
     */
    public abstract void setOneAliasIndexOn( Index index ) throws NamingException;


    /**
     * Sets the alias index mapping relative entries with scope expanding 
     * alias descendents; this system index is used to dereference aliases on 
     * subtree scoped searches.
     * 
     * @param index a subtree alias index
     * @throws NamingException if there is a problem setting up the index
     */
    public abstract void setSubAliasIndexOn( Index index ) throws NamingException;


    public abstract Index getUserIndex( String attribute ) throws IndexNotFoundException;


    public abstract Index getSystemIndex( String attribute ) throws IndexNotFoundException;


    public abstract Long getEntryId( String dn ) throws NamingException;


    public abstract String getEntryDn( Long id ) throws NamingException;


    public abstract Long getParentId( String dn ) throws NamingException;


    public abstract Long getParentId( Long childId ) throws NamingException;


    /**
     * Gets the user provided distinguished name.
     *
     * @param id the entry id
     * @return the user provided distinguished name
     * @throws NamingException if the updn index cannot be accessed
     */
    public abstract String getEntryUpdn( Long id ) throws NamingException;


    /**
     * Gets the user provided distinguished name.
     *
     * @param dn the normalized distinguished name
     * @return the user provided distinguished name
     * @throws NamingException if the updn and ndn indices cannot be accessed
     */
    public abstract String getEntryUpdn( String dn ) throws NamingException;


    public abstract Attributes lookup( Long id ) throws NamingException;


    public abstract void delete( Long id ) throws NamingException;


    public abstract NamingEnumeration<IndexRecord> list( Long id ) throws NamingException;


    public abstract int getChildCount( Long id ) throws NamingException;


    public abstract Attributes getSuffixEntry() throws NamingException;


    public abstract void setProperty( String key, String value ) throws NamingException;


    public abstract String getProperty( String key ) throws NamingException;


    public abstract Iterator getUserIndices();


    public abstract Iterator getSystemIndices();


    public abstract Attributes getIndices( Long id ) throws NamingException;


    /**
     * Gets the count of the total number of entries in the database.
     *
     * TODO shouldn't this be a BigInteger instead of an int? 
     * 
     * @return the number of entries in the database 
     * @throws NamingException if there is a failure to read the count
     */
    public abstract int count() throws NamingException;
}
