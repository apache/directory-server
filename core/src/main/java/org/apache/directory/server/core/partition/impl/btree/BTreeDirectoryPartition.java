/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.partition.impl.btree;


import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.DirectoryPartitionConfiguration;
import org.apache.directory.server.core.enumeration.SearchResultEnumeration;
import org.apache.directory.server.core.partition.DirectoryPartition;
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


/**
 * An abstract {@link DirectoryPartition} that uses general BTree operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class BTreeDirectoryPartition implements DirectoryPartition
{
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
    protected BTreeDirectoryPartition()
    {
    }
    

    public void init( DirectoryServiceConfiguration factoryCfg, DirectoryPartitionConfiguration cfg ) throws NamingException
    {
        attributeTypeRegistry = factoryCfg.getGlobalRegistries().getAttributeTypeRegistry();
        OidRegistry oidRegistry = factoryCfg.getGlobalRegistries().getOidRegistry();
        ExpressionEvaluator evaluator = new ExpressionEvaluator( this, oidRegistry, attributeTypeRegistry );
        ExpressionEnumerator enumerator = new ExpressionEnumerator( this, attributeTypeRegistry, evaluator );
        this.searchEngine = new DefaultSearchEngine( this, evaluator, enumerator );

        HashSet sysOidSet = new HashSet();
        sysOidSet.add( Oid.EXISTANCE );
        sysOidSet.add( Oid.HIERARCHY );
        sysOidSet.add( Oid.UPDN );
        sysOidSet.add( Oid.NDN );
        sysOidSet.add( Oid.ONEALIAS );
        sysOidSet.add( Oid.SUBALIAS );
        sysOidSet.add( Oid.ALIAS );

        Iterator i = cfg.getIndexedAttributes().iterator();
        while( i.hasNext() )
        {
            String name = ( String ) i.next();
            String oid = oidRegistry.getOid( name );
            AttributeType type = attributeTypeRegistry.lookup( oid );

            // check if attribute is a system attribute
            if ( sysOidSet.contains( oid ) )
            {
                if ( oid.equals( Oid.EXISTANCE ) )
                {
                    setExistanceIndexOn( type );
                }
                else if ( oid.equals( Oid.HIERARCHY ) )
                {
                    setHierarchyIndexOn( type );
                }
                else if ( oid.equals( Oid.UPDN ) )
                {
                    setUpdnIndexOn( type );
                }
                else if ( oid.equals( Oid.NDN ) )
                {
                    setNdnIndexOn( type );
                }
                else if ( oid.equals( Oid.ONEALIAS ) )
                {
                    setOneAliasIndexOn( type );
                }
                else if ( oid.equals( Oid.SUBALIAS ) )
                {
                    setSubAliasIndexOn( type );
                }
                else if ( oid.equals( Oid.ALIAS ) )
                {
                    setAliasIndexOn( type );
                }
                else
                {
                    throw new NamingException( "Unidentified system index "
                        + oid );
                }
            }
            else
            {
                addIndexOn( type );
            }
        }

        // add entry for context, if it does not exist
        Attributes suffixOnDisk = getSuffixEntry();
        if ( suffixOnDisk == null )
        {
            add( cfg.getSuffix(),
                 cfg.getNormalizedSuffix( factoryCfg.getGlobalRegistries().getMatchingRuleRegistry() ),
                 cfg.getContextEntry() );
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


    public void delete( Name dn ) throws NamingException
    {
        BigInteger id = getEntryId( dn.toString() );

        // don't continue if id is null
        if ( id == null )
        {
            throw new LdapNameNotFoundException( "Could not find entry at '"
                    + dn + "' to delete it!");
        }

        if ( getChildCount( id ) > 0 )
        {
            LdapContextNotEmptyException cnee = new LdapContextNotEmptyException(
                "[66] Cannot delete entry " + dn + " it has children!" );
            cnee.setRemainingName( dn );
            throw cnee;
        }
        
        delete( id );
    }
    
    public abstract void add( String updn, Name dn, Attributes entry ) throws NamingException;
    public abstract void modify( Name dn, int modOp, Attributes mods ) throws NamingException;
    public abstract void modify( Name dn, ModificationItem[] mods ) throws NamingException;


    public NamingEnumeration list( Name base ) throws NamingException
    {
        SearchResultEnumeration list;
        list = new BTreeSearchResultEnumeration( ArrayUtils.EMPTY_STRING_ARRAY,
                list( getEntryId( base.toString() ) ), this, attributeTypeRegistry );
        return list;
    }
    
    
    public NamingEnumeration search( Name base, Map env, ExprNode filter,
                                     SearchControls searchCtls )
        throws NamingException
    {
        String [] attrIds = searchCtls.getReturningAttributes();
        NamingEnumeration underlying = null;
        
        underlying = searchEngine.search( base, env, filter, searchCtls );
        
        return new BTreeSearchResultEnumeration( attrIds, underlying, this, attributeTypeRegistry );
    }


    public Attributes lookup( Name dn ) throws NamingException
    {
        return lookup( getEntryId( dn.toString() ) );
    }


    public Attributes lookup( Name dn, String [] attrIds ) throws NamingException
    {
        if ( attrIds == null || attrIds.length == 0 )
        {
            return lookup( dn );
        }

        Attributes entry = lookup( dn );
        Attributes retval = new LockableAttributesImpl();

        for ( int ii = 0; ii < attrIds.length; ii++ )
        {
            Attribute attr = entry.get( attrIds[0] );

            if ( attr != null )
            {
                retval.put( attr );
            }
        }

        return retval;
    }


    public boolean hasEntry( Name dn ) throws NamingException
    {
        return null != getEntryId( dn.toString() );
    }


    public abstract void modifyRn( Name dn, String newRdn, boolean deleteOldRdn ) throws NamingException;
    public abstract void move( Name oldChildDn, Name newParentDn ) throws NamingException;
    public abstract void move( Name oldChildDn, Name newParentDn, String newRdn,
        boolean deleteOldRdn ) throws NamingException;


    public abstract void sync() throws NamingException;
    public abstract void destroy();
    public abstract boolean isInitialized();

    public boolean isSuffix( Name dn ) throws NamingException
    {
        return getSuffix( true ).equals( dn ) ;
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


    public abstract void addIndexOn( AttributeType attribute ) throws NamingException;
    public abstract boolean hasUserIndexOn( String attribute );
    public abstract boolean hasSystemIndexOn( String attribute );
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
    public abstract void setAliasIndexOn( AttributeType attrType ) throws NamingException;

    /**
     * Sets the attribute existance Index.
     *
     * @param attrType the attribute existance Index
     */    
    public abstract void setExistanceIndexOn( AttributeType attrType ) throws NamingException;

    /**
     * Sets the hierarchy Index.
     *
     * @param attrType the hierarchy Index
     */    
    public abstract void setHierarchyIndexOn( AttributeType attrType ) throws NamingException;

    /**
     * Sets the user provided distinguished name Index.
     *
     * @param attrType the updn Index
     */    
    public abstract void setUpdnIndexOn( AttributeType attrType ) throws NamingException;

    /**
     * Sets the normalized distinguished name Index.
     *
     * @param attrType the ndn Index
     */    
    public abstract void setNdnIndexOn( AttributeType attrType ) throws NamingException;
    
    /**
     * Sets the alias index mapping parent entries with scope expanding aliases 
     * children one level below them; this system index is used to dereference
     * aliases on one/single level scoped searches.
     * 
     * @param attrType a one level alias index
     */
    public abstract void setOneAliasIndexOn( AttributeType attrType ) throws NamingException;
    
    /**
     * Sets the alias index mapping relative entries with scope expanding 
     * alias descendents; this system index is used to dereference aliases on 
     * subtree scoped searches.
     * 
     * @param attrType a subtree alias index
     */
    public abstract void setSubAliasIndexOn( AttributeType attrType ) throws NamingException;
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
