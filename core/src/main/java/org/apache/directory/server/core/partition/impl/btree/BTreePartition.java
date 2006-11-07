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


import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.enumeration.SearchResultEnumeration;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.Oid;
import org.apache.directory.server.core.partition.impl.btree.gui.PartitionViewer;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.OidRegistry;
import org.apache.directory.shared.ldap.exception.LdapContextNotEmptyException;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.ArrayUtils;
import org.apache.directory.shared.ldap.name.LdapDN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An abstract {@link Partition} that uses general BTree operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class BTreePartition implements Partition
{
    private static final Logger log = LoggerFactory.getLogger( BTreePartition.class );

    /** ===================================================================

     The following OID branch is reserved for the directory TLP once it
     graduates the incubator:

     1.2.6.1.4.1.18060.1.1

     The following branch is reserved for the apache directory server:

     1.2.6.1.4.1.18060.1.1.1

     The following branch is reserved for use by apache directory server Syntaxes:

     1.2.6.1.4.1.18060.1.1.1.1

     The following branch is reserved for use by apache directory server MatchingRules:

     1.2.6.1.4.1.18060.1.1.1.2

     The following branch is reserved for use by apache directory server AttributeTypes:

     1.2.6.1.4.1.18060.1.1.1.3

     * 1.2.6.1.4.1.18060.1.1.1.3.1 - apacheNdn
     * 1.2.6.1.4.1.18060.1.1.1.3.2 - apacheUpdn
     * 1.2.6.1.4.1.18060.1.1.1.3.3 - apacheExistance
     * 1.2.6.1.4.1.18060.1.1.1.3.4 - apacheHierarchy
     * 1.2.6.1.4.1.18060.1.1.1.3.5 - apacheOneAlias
     * 1.2.6.1.4.1.18060.1.1.1.3.6 - apacheSubAlias
     * 1.2.6.1.4.1.18060.1.1.1.3.7 - apacheAlias

     The following branch is reserved for use by apache directory server ObjectClasses:

     1.2.6.1.4.1.18060.1.1.1.4

     ==================================================================== */

    /**
     * the search engine used to search the database
     */
    private SearchEngine searchEngine = null;
    private AttributeTypeRegistry attributeTypeRegistry = null;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates a B-tree based context partition.
     */
    protected BTreePartition()
    {
    }


    public void init( DirectoryServiceConfiguration factoryCfg, PartitionConfiguration cfg )
        throws NamingException
    {
        attributeTypeRegistry = factoryCfg.getGlobalRegistries().getAttributeTypeRegistry();
        OidRegistry oidRegistry = factoryCfg.getGlobalRegistries().getOidRegistry();
        ExpressionEvaluator evaluator = new ExpressionEvaluator( this, oidRegistry, attributeTypeRegistry );
        ExpressionEnumerator enumerator = new ExpressionEnumerator( this, attributeTypeRegistry, evaluator );
        BTreePartitionConfiguration btConfig = BTreePartitionConfiguration.convert( cfg );
        
        
        Optimizer optimizer;
        if ( btConfig.isOptimizerEnabled() )
        {
            optimizer = new DefaultOptimizer( this );
        }
        else
        {
            optimizer = new NoOpOptimizer();
        }
        
        this.searchEngine = new DefaultSearchEngine( this, evaluator, enumerator, optimizer );

        HashSet sysOidSet = new HashSet();
        sysOidSet.add( Oid.EXISTANCE );
        sysOidSet.add( Oid.HIERARCHY );
        sysOidSet.add( Oid.UPDN );
        sysOidSet.add( Oid.NDN );
        sysOidSet.add( Oid.ONEALIAS );
        sysOidSet.add( Oid.SUBALIAS );
        sysOidSet.add( Oid.ALIAS );

        // Used to calculate the system indices we must automatically add
        HashSet customAddedSystemIndices = new HashSet();
        
        for ( Iterator ii = cfg.getIndexedAttributes().iterator(); ii.hasNext(); /**/ )
        {
            /*
             * NOTE
             * ====
             * 
             * The object returned by the indexedAttributes property
             * of the configuration may include just a simple set of <String> 
             * names for the attributes being index OR may include a set 
             * of IndexConfiguration objects.
             * 
             * If the objects are strings extra information about the
             * cacheSize of an index is not available and so the default is
             * used.  If an IndexConfiguration is available then the custom
             * cacheSize is used.
             */
            
            Object nextObject = ii.next();
            String name = null;
            int cacheSize = IndexConfiguration.DEFAULT_INDEX_CACHE_SIZE;
            int numDupLimit = IndexConfiguration.DEFAULT_DUPLICATE_LIMIT;
            
            // no custom cacheSize info is available so default sticks
            if ( nextObject instanceof String ) 
            {
                name = ( String ) nextObject;
                log.warn( "Using default cache size of {} for index on attribute {}", 
                    new Integer( cacheSize ), name );
            }
            // custom cache size is used
            else if ( nextObject instanceof IndexConfiguration )
            {
                IndexConfiguration indexConfiguration = ( IndexConfiguration ) nextObject;
                name = indexConfiguration.getAttributeId();
                cacheSize = indexConfiguration.getCacheSize();
                numDupLimit = indexConfiguration.getDuplicateLimit();
                
                if ( cacheSize <= 0 ) 
                {
                    log.warn( "Cache size {} for index on attribute is null or negative. Using default value.", 
                        new Integer(cacheSize), name );
                    cacheSize = IndexConfiguration.DEFAULT_INDEX_CACHE_SIZE;
                }
                else
                {
                    log.info( "Using cache size of {} for index on attribute {}", 
                        new Integer( cacheSize ), name );
                }
                
                if ( cacheSize <= 0 ) 
                {
                    log.warn( "Duplicate limit {} for index on attribute is null or negative. Using default value.", 
                        new Integer(numDupLimit), name );
                    cacheSize = IndexConfiguration.DEFAULT_INDEX_CACHE_SIZE;
                }
                else
                {
                    log.info( "Using duplicate limit of {} for index on attribute {}", 
                        new Integer( numDupLimit ), name );
                }
            }
            
            String oid = oidRegistry.getOid( name );
            AttributeType type = attributeTypeRegistry.lookup( oid );

            // check if attribute is a system attribute
            if ( sysOidSet.contains( oid ) )
            {
                if ( oid.equals( Oid.EXISTANCE ) )
                {
                    setExistanceIndexOn( type, cacheSize, numDupLimit );
                    customAddedSystemIndices.add( Oid.EXISTANCE );
                }
                else if ( oid.equals( Oid.HIERARCHY ) )
                {
                    setHierarchyIndexOn( type, cacheSize, numDupLimit );
                    customAddedSystemIndices.add( Oid.HIERARCHY );
                }
                else if ( oid.equals( Oid.UPDN ) )
                {
                    setUpdnIndexOn( type, cacheSize, numDupLimit );
                    customAddedSystemIndices.add( Oid.UPDN );
                }
                else if ( oid.equals( Oid.NDN ) )
                {
                    setNdnIndexOn( type, cacheSize, numDupLimit );
                    customAddedSystemIndices.add( Oid.NDN );
                }
                else if ( oid.equals( Oid.ONEALIAS ) )
                {
                    setOneAliasIndexOn( type, cacheSize, numDupLimit );
                    customAddedSystemIndices.add( Oid.ONEALIAS );
                }
                else if ( oid.equals( Oid.SUBALIAS ) )
                {
                    setSubAliasIndexOn( type, cacheSize, numDupLimit );
                    customAddedSystemIndices.add( Oid.SUBALIAS);
                }
                else if ( oid.equals( Oid.ALIAS ) )
                {
                    setAliasIndexOn( type, cacheSize, numDupLimit );
                    customAddedSystemIndices.add( Oid.ALIAS );
                }
                else
                {
                    throw new NamingException( "Unidentified system index " + oid );
                }
            }
            else
            {
                addIndexOn( type, cacheSize, numDupLimit );
            }
        }
        
        // -------------------------------------------------------------------
        // Add all system indices that were not custom configured by iterating
        // through all system index oids and checking of that index is 
        // contained within the customAddedSystemIndices set.  If it is not
        // contained in this set then the system index was not custom 
        // configured above and must be configured with defaults below.
        // -------------------------------------------------------------------
        
        for ( Iterator ii = sysOidSet.iterator(); ii.hasNext(); /**/ )
        {
            String systemIndexName = ( String ) ii.next();
            if ( ! customAddedSystemIndices.contains( systemIndexName ) )
            {
                AttributeType type = attributeTypeRegistry.lookup( systemIndexName );
                log.warn( "Using default cache size of {} for index on attribute {}", 
                    new Integer( IndexConfiguration.DEFAULT_INDEX_CACHE_SIZE ), systemIndexName );
                if ( systemIndexName.equals( Oid.EXISTANCE ) )
                {
                    setExistanceIndexOn( type, IndexConfiguration.DEFAULT_INDEX_CACHE_SIZE, 
                        IndexConfiguration.DEFAULT_DUPLICATE_LIMIT );
                }
                else if ( systemIndexName.equals( Oid.HIERARCHY ) )
                {
                    setHierarchyIndexOn( type, IndexConfiguration.DEFAULT_INDEX_CACHE_SIZE,
                        IndexConfiguration.DEFAULT_DUPLICATE_LIMIT );
                }
                else if ( systemIndexName.equals( Oid.UPDN ) )
                {
                    setUpdnIndexOn( type, IndexConfiguration.DEFAULT_INDEX_CACHE_SIZE,
                        IndexConfiguration.DEFAULT_DUPLICATE_LIMIT );
                }
                else if ( systemIndexName.equals( Oid.NDN ) )
                {
                    setNdnIndexOn( type, IndexConfiguration.DEFAULT_INDEX_CACHE_SIZE,
                        IndexConfiguration.DEFAULT_DUPLICATE_LIMIT );
                }
                else if ( systemIndexName.equals( Oid.ONEALIAS ) )
                {
                    setOneAliasIndexOn( type, IndexConfiguration.DEFAULT_INDEX_CACHE_SIZE,
                        IndexConfiguration.DEFAULT_DUPLICATE_LIMIT );
                }
                else if ( systemIndexName.equals( Oid.SUBALIAS ) )
                {
                    setSubAliasIndexOn( type, IndexConfiguration.DEFAULT_INDEX_CACHE_SIZE,
                        IndexConfiguration.DEFAULT_DUPLICATE_LIMIT );
                }
                else if ( systemIndexName.equals( Oid.ALIAS ) )
                {
                    setAliasIndexOn( type, IndexConfiguration.DEFAULT_INDEX_CACHE_SIZE,
                        IndexConfiguration.DEFAULT_DUPLICATE_LIMIT );
                }
                else
                {
                    throw new NamingException( "Unidentified system index " + systemIndexName );
                }
            }
        }

        // add entry for context, if it does not exist
        Attributes suffixOnDisk = getSuffixEntry();
        if ( suffixOnDisk == null )
        {
            LdapDN suffix = new LdapDN( cfg.getSuffix() );
            LdapDN normalizedSuffix = LdapDN.normalize( suffix, attributeTypeRegistry.getNormalizerMapping() );
            add( normalizedSuffix, cfg.getContextEntry() );
        }
    }


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
    // ContextPartition Interface Method Implementations
    // ------------------------------------------------------------------------

    public void delete( LdapDN dn ) throws NamingException
    {
        BigInteger id = getEntryId( dn.toString() );

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


    public abstract void add(LdapDN dn, Attributes entry) throws NamingException;


    public abstract void modify( LdapDN dn, int modOp, Attributes mods ) throws NamingException;


    public abstract void modify( LdapDN dn, ModificationItem[] mods ) throws NamingException;


    private static final String[] ENTRY_DELETED_ATTRS = new String[] { "entrydeleted" };
    public NamingEnumeration list( LdapDN base ) throws NamingException
    {
        SearchResultEnumeration list;
        list = new BTreeSearchResultEnumeration( ENTRY_DELETED_ATTRS, list( getEntryId( base.toString() ) ),
            this, attributeTypeRegistry );
        return list;
    }


    public NamingEnumeration search( LdapDN base, Map env, ExprNode filter, SearchControls searchCtls )
        throws NamingException
    {
        String[] attrIds = searchCtls.getReturningAttributes();
        NamingEnumeration underlying = null;

        underlying = searchEngine.search( base, env, filter, searchCtls );

        return new BTreeSearchResultEnumeration( attrIds, underlying, this, attributeTypeRegistry );
    }


    public Attributes lookup( LdapDN dn ) throws NamingException
    {
        return lookup( getEntryId( dn.toString() ) );
    }


    public Attributes lookup( LdapDN dn, String[] attrIds ) throws NamingException
    {
        if ( attrIds == null || attrIds.length == 0 )
        {
            return lookup( dn );
        }

        Attributes entry = lookup( dn );
        Attributes retval = new LockableAttributesImpl();

        for ( int ii = 0; ii < attrIds.length; ii++ )
        {
            Attribute attr = entry.get( attrIds[ii] );

            if ( attr != null )
            {
                retval.put( attr );
            }
        }

        return retval;
    }


    public boolean hasEntry( LdapDN dn ) throws NamingException
    {
        return null != getEntryId( dn.toString() );
    }


    public abstract void modifyRn( LdapDN dn, String newRdn, boolean deleteOldRdn ) throws NamingException;


    public abstract void move( LdapDN oldChildDn, LdapDN newParentDn ) throws NamingException;


    public abstract void move( LdapDN oldChildDn, LdapDN newParentDn, String newRdn, boolean deleteOldRdn )
        throws NamingException;


    public abstract void sync() throws NamingException;


    public abstract void destroy();


    public abstract boolean isInitialized();


    public boolean isSuffix( LdapDN dn ) throws NamingException
    {
        return getSuffix().equals( dn );
    }


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

    public abstract void addIndexOn( AttributeType attribute, int cacheSize, int numDupLimit ) throws NamingException;


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
     * @param attrType the index on the ALIAS_ATTRIBUTE
     */
    public abstract void setAliasIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException;


    /**
     * Sets the attribute existance Index.
     *
     * @param attrType the attribute existance Index
     */
    public abstract void setExistanceIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException;


    /**
     * Sets the hierarchy Index.
     *
     * @param attrType the hierarchy Index
     */
    public abstract void setHierarchyIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException;


    /**
     * Sets the user provided distinguished name Index.
     *
     * @param attrType the updn Index
     */
    public abstract void setUpdnIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException;


    /**
     * Sets the normalized distinguished name Index.
     *
     * @param attrType the ndn Index
     */
    public abstract void setNdnIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException;


    /**
     * Sets the alias index mapping parent entries with scope expanding aliases 
     * children one level below them; this system index is used to dereference
     * aliases on one/single level scoped searches.
     * 
     * @param attrType a one level alias index
     */
    public abstract void setOneAliasIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException;


    /**
     * Sets the alias index mapping relative entries with scope expanding 
     * alias descendents; this system index is used to dereference aliases on 
     * subtree scoped searches.
     * 
     * @param attrType a subtree alias index
     */
    public abstract void setSubAliasIndexOn( AttributeType attrType, int cacheSize, int numDupLimit ) throws NamingException;


    public abstract Index getUserIndex( String attribute ) throws IndexNotFoundException;


    public abstract Index getSystemIndex( String attribute ) throws IndexNotFoundException;


    public abstract BigInteger getEntryId( String dn ) throws NamingException;


    public abstract String getEntryDn( BigInteger id ) throws NamingException;


    public abstract BigInteger getParentId( String dn ) throws NamingException;


    public abstract BigInteger getParentId( BigInteger childId ) throws NamingException;


    /**
     * Gets the user provided distinguished name.
     *
     * @param id the entry id
     * @return the user provided distinguished name
     * @throws NamingException if the updn index cannot be accessed
     */
    public abstract String getEntryUpdn( BigInteger id ) throws NamingException;


    /**
     * Gets the user provided distinguished name.
     *
     * @param dn the normalized distinguished name
     * @return the user provided distinguished name
     * @throws NamingException if the updn and ndn indices cannot be accessed
     */
    public abstract String getEntryUpdn( String dn ) throws NamingException;


    public abstract Attributes lookup( BigInteger id ) throws NamingException;


    public abstract void delete( BigInteger id ) throws NamingException;


    public abstract NamingEnumeration list( BigInteger id ) throws NamingException;


    public abstract int getChildCount( BigInteger id ) throws NamingException;


    public abstract Attributes getSuffixEntry() throws NamingException;


    public abstract void setProperty( String key, String value ) throws NamingException;


    public abstract String getProperty( String key ) throws NamingException;


    public abstract Iterator getUserIndices();


    public abstract Iterator getSystemIndices();


    public abstract Attributes getIndices( BigInteger id ) throws NamingException;


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
