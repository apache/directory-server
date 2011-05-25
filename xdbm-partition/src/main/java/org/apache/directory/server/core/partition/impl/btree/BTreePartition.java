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


import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
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
import org.apache.directory.server.core.partition.AbstractPartition;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.search.Optimizer;
import org.apache.directory.server.xdbm.search.SearchEngine;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapContextNotEmptyException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.UsageEnum;


/**
 * An abstract {@link Partition} that uses general BTree operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class BTreePartition<ID> extends AbstractPartition
{

    /** the search engine used to search the database */
    protected SearchEngine<Entry, ID> searchEngine;
    
    protected Optimizer optimizer;

    /** The partition ID */
    protected String id;
    
    /** The Entry cache size for this partition */
    protected int cacheSize = -1;
    
    /** The root Dn for this partition */
    protected Dn suffix;
    
    /** The path in which this Partition stores files */
    protected URI partitionPath;

    /** The set of indexed attributes */
    private Set<Index<?, Entry, ID>> indexedAttributes;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a B-tree based context partition.
     */
    protected BTreePartition()
    {
        indexedAttributes = new HashSet<Index<?, Entry, ID>>();
    }


    // ------------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // ------------------------------------------------------------------------
    /**
     * Gets the path in which this Partition stores data.
     *
     * @return the path in which this Partition stores data.
     */
    public URI getPartitionPath()
    {
        return partitionPath;
    }


    /**
     * Sets the path in which this Partition stores data. This may be an URL to
     * a file or directory, or an JDBC URL.
     *
     * @param partitionDir the path in which this Partition stores data.
     */
    public void setPartitionPath( URI partitionPath )
    {
        this.partitionPath = partitionPath;
    }


    public void setIndexedAttributes( Set<Index<?, Entry, ID>> indexedAttributes )
    {
        this.indexedAttributes = indexedAttributes;
    }


    public void addIndexedAttributes( Index<?, Entry, ID>... indexes )
    {
        for ( Index<?, Entry, ID> index : indexes )
        {
            indexedAttributes.add( index );
        }
    }


    public Set<Index<?, Entry, ID>> getIndexedAttributes()
    {
        return indexedAttributes;
    }


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

    // ------------------------------------------------------------------------
    // Public Accessors - not declared in any interfaces just for this class
    // ------------------------------------------------------------------------

    /**
     * Gets the DefaultSearchEngine used by this ContextPartition to search the
     * Database.
     *
     * @return the search engine
     */
    public SearchEngine<Entry, ID> getSearchEngine()
    {
        return searchEngine;
    }


    // ------------------------------------------------------------------------
    // Partition Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        Dn dn = deleteContext.getDn();

        ID id = getEntryId( dn );

        // don't continue if id is null
        if ( id == null )
        {
            throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_699, dn ) );
        }

        if ( getChildCount( id ) > 0 )
        {
            LdapContextNotEmptyException cnee = new LdapContextNotEmptyException( I18n.err( I18n.ERR_700, dn ) );
            //cnee.setRemainingName( dn );
            throw cnee;
        }

        delete( id );
    }


    public abstract void add( AddOperationContext addContext ) throws LdapException;


    public abstract void modify( ModifyOperationContext modifyContext ) throws LdapException;


    public EntryFilteringCursor list( ListOperationContext listContext ) throws LdapException
    {
        return new BaseEntryFilteringCursor( new ServerEntryCursorAdaptor<ID>( this, list( getEntryId( listContext
            .getDn() ) ) ), listContext );
    }


    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        try
        {
            SearchControls searchCtls = searchContext.getSearchControls();
            IndexCursor<ID, Entry, ID> underlying;
            Dn dn = searchContext.getDn();
            AliasDerefMode derefMode = searchContext.getAliasDerefMode();
            ExprNode filter = searchContext.getFilter();

            underlying = searchEngine.cursor( dn, derefMode, filter, searchCtls );

            return new BaseEntryFilteringCursor( new ServerEntryCursorAdaptor<ID>( this, underlying ), searchContext );
        }
        catch ( LdapException le )
        {
            // TODO: SearchEngine.cursor() should only throw LdapException, then the exception handling here can be removed 
            throw le;
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage() );
        }
    }


    public ClonedServerEntry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        ID id = getEntryId( lookupContext.getDn() );

        if ( id == null )
        {
            return null;
        }

        ClonedServerEntry entry = lookup( id );

        // Remove all the attributes if the NO_ATTRIBUTE flag is set
        if ( lookupContext.hasNoAttribute() )
        {
            entry.clear();

            return entry;
        }

        if ( ( lookupContext.getAttrsId() == null ) || ( lookupContext.getAttrsId().size() == 0 ) 
            && !lookupContext.hasAllOperational() && !lookupContext.hasAllUser() )
        {
            return entry;
        }

        for ( AttributeType attributeType : ( entry.getOriginalEntry() ).getAttributeTypes() )
        {
            String oid = attributeType.getOid();
            
            if ( !lookupContext.getAttrsId().contains( oid ) )
            {
                if ( attributeType.getUsage() == UsageEnum.USER_APPLICATIONS ) 
                {
                    if ( !lookupContext.hasAllUser() )
                    {
                        entry.removeAttributes( attributeType );
                    }
                }
                else if ( ( !lookupContext.hasAllOperational() ) )
                {
                    entry.removeAttributes( attributeType );
                }
            }
        }

        return entry;
    }


    public boolean hasEntry( EntryOperationContext hasEntryContext ) throws LdapException
    {
        return null != getEntryId( hasEntryContext.getDn() );
    }


    public abstract void rename( RenameOperationContext renameContext ) throws LdapException;


    public abstract void move( MoveOperationContext moveContext ) throws LdapException;


    public abstract void moveAndRename( MoveAndRenameOperationContext opContext ) throws LdapException;


    public abstract void sync() throws Exception;


    ////////////////////
    // public abstract methods

    // ------------------------------------------------------------------------
    // Index Operations
    // ------------------------------------------------------------------------

    public abstract void addIndexOn( Index<?, Entry, ID> index ) throws Exception;


    public abstract boolean hasUserIndexOn( AttributeType attributeType ) throws Exception;


    public abstract boolean hasSystemIndexOn( AttributeType attributeType ) throws Exception;


    public abstract Index<String, Entry, ID> getPresenceIndex();


    /**
     * Gets the Index mapping the primary keys of parents to the
     * primary keys of their children.
     *
     * @return the one level Index
     */
    public abstract Index<ID, Entry, ID> getOneLevelIndex();


    /**
     * Gets the Index mapping the primary keys of ancestors to the
     * primary keys of their descendants.
     *
     * @return the sub tree level Index
     */
    public abstract Index<ID, Entry, ID> getSubLevelIndex();


    /**
     * Gets the alias index mapping parent entries with scope expanding aliases
     * children one level below them; this system index is used to dereference
     * aliases on one/single level scoped searches.
     *
     * @return the one alias index
     */
    public abstract Index<ID, Entry, ID> getOneAliasIndex();


    /**
     * Gets the alias index mapping relative entries with scope expanding
     * alias descendents; this system index is used to dereference aliases on
     * subtree scoped searches.
     *
     * @return the sub alias index
     */
    public abstract Index<ID, Entry, ID> getSubAliasIndex();


    /**
     * Gets the system index defined on the ALIAS_ATTRIBUTE which for LDAP would
     * be the aliasedObjectName and for X.500 would be aliasedEntryName.
     *
     * @return the index on the ALIAS_ATTRIBUTE
     */
    public abstract Index<String, Entry, ID> getAliasIndex();


    /**
     * {@inheritDoc}
     */
    public void setSuffix( Dn suffix ) throws LdapInvalidDnException
    {
        this.suffix = suffix;

        if ( schemaManager != null )
        {
            this.suffix.apply( schemaManager );
        }
    }


    /**
     * {@inheritDoc}
     */
    public Dn getSuffix()
    {
        return suffix;
    }


    public abstract Index<?, Entry, ID> getUserIndex( AttributeType attributeType ) throws Exception;


    public abstract Index<?, Entry, ID> getSystemIndex( AttributeType attributeType ) throws Exception;


    public abstract ID getEntryId( Dn dn ) throws LdapException;


    public abstract Dn getEntryDn( ID id ) throws Exception;


    public abstract ClonedServerEntry lookup( ID id ) throws LdapException;


    public abstract void delete( ID id ) throws LdapException;


    public abstract IndexCursor<ID, Entry, ID> list( ID id ) throws LdapException;


    public abstract int getChildCount( ID id ) throws LdapException;


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
