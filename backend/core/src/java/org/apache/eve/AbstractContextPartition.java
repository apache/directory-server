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
package org.apache.eve;


import java.util.Map;
import java.util.HashSet;
import java.math.BigInteger;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.ContextNotEmptyException;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.Attribute;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.message.LockableAttributesImpl;

import org.apache.eve.db.Database;
import org.apache.eve.db.SearchEngine;
import org.apache.eve.db.SearchResultEnumeration;


/**
 * An Abstract BackingStore using a formal database and a search engine.  All
 * the common code between a SystemBackingStore and a DefaultBackingStore
 * will be added to this super class.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractContextPartition implements ContextPartition
{
    /** ===================================================================

     The following OID branch is reserved for the directory TLP once it
     graduates the incubator:

       1.2.6.1.4.1.18060.1.1

     The following branch is reserved for eve:

       1.2.6.1.4.1.18060.1.1.1

      The following branch is reserved for use by eve Syntaxes:

        1.2.6.1.4.1.18060.1.1.1.1

      The following branch is reserved for use by eve MatchingRules:

        1.2.6.1.4.1.18060.1.1.1.2

      The following branch is reserved for use by even AttributeTypes:

        1.2.6.1.4.1.18060.1.1.1.3

          * 1.2.6.1.4.1.18060.1.1.1.3.1 - eveNdn
          * 1.2.6.1.4.1.18060.1.1.1.3.2 - eveUpdn
          * 1.2.6.1.4.1.18060.1.1.1.3.3 - eveExistance
          * 1.2.6.1.4.1.18060.1.1.1.3.4 - eveHierarchy
          * 1.2.6.1.4.1.18060.1.1.1.3.5 - eveOneAlias
          * 1.2.6.1.4.1.18060.1.1.1.3.6 - eveSubAlias
          * 1.2.6.1.4.1.18060.1.1.1.3.7 - eveAlias

      The following branch is reserved for use by eve ObjectClasses:

        1.2.6.1.4.1.18060.1.1.1.4

    ==================================================================== */


    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.1) for _ndn op attrib */
    public static final String NDN_OID       = "1.2.6.1.4.1.18060.1.1.1.3.1" ;
    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.2) for _updn op attrib */
    public static final String UPDN_OID      = "1.2.6.1.4.1.18060.1.1.1.3.2" ;
    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.3) for _existance op attrib */
    public static final String EXISTANCE_OID = "1.2.6.1.4.1.18060.1.1.1.3.3" ;
    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.4) for _hierarchy op attrib */
    public static final String HIERARCHY_OID = "1.2.6.1.4.1.18060.1.1.1.3.4" ;
    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.5) for _oneAlias index */
    public static final String ONEALIAS_OID  = "1.2.6.1.4.1.18060.1.1.1.3.5" ;
    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.6) for _subAlias index */
    public static final String SUBALIAS_OID  = "1.2.6.1.4.1.18060.1.1.1.3.6" ;
    /** Private OID (1.2.6.1.4.1.18060.1.1.1.3.7) for _alias index */
    public static final String ALIAS_OID     = "1.2.6.1.4.1.18060.1.1.1.3.7" ;

    /**
     * the database used for this backing store which is also initialized during
     * configuration time
     */
    private Database db = null;
    
    /**
     * the search engine used to search the database
     */
    private SearchEngine engine = null;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a context partition with a new database and a search engine.
     *
     * @param db the dedicated database for this backing store
     * @param searchEngine the search engine for this backing store
     */
    public AbstractContextPartition( Database db, SearchEngine searchEngine,
                                     AttributeType[] indexAttributes )
        throws NamingException
    {
        this.db = db;
        this.engine = searchEngine;

        HashSet sysOidSet = new HashSet();
        sysOidSet.add( EXISTANCE_OID );
        sysOidSet.add( HIERARCHY_OID );
        sysOidSet.add( UPDN_OID );
        sysOidSet.add( NDN_OID );
        sysOidSet.add( ONEALIAS_OID );
        sysOidSet.add( SUBALIAS_OID );
        sysOidSet.add( ALIAS_OID );

        for ( int ii = 0; ii < indexAttributes.length; ii ++ )
        {
            String oid = indexAttributes[ii].getOid();

            // check if attribute is a system attribute
            if ( sysOidSet.contains( oid ) )
            {
                if ( oid.equals( EXISTANCE_OID ) )
                {
                    db.setExistanceIndexOn( indexAttributes[ii] );
                }
                else if ( oid.equals( HIERARCHY_OID ) )
                {
                    db.setHeirarchyIndexOn( indexAttributes[ii] );
                }
                else if ( oid.equals( UPDN_OID ) )
                {
                    db.setUpdnIndexOn( indexAttributes[ii] );
                }
                else if ( oid.equals( NDN_OID ) )
                {
                    db.setNdnIndexOn( indexAttributes[ii] );
                }
                else if ( oid.equals( ONEALIAS_OID ) )
                {
                    db.setOneAliasIndexOn( indexAttributes[ii] );
                }
                else if ( oid.equals( SUBALIAS_OID ) )
                {
                    db.setSubAliasIndexOn( indexAttributes[ii] );
                }
                else if ( oid.equals( ALIAS_OID ) )
                {
                    db.setAliasIndexOn( indexAttributes[ii] );
                }
                else
                {
                    throw new NamingException( "Unidentified system index "
                        + oid );
                }
            }
            else
            {
                db.addIndexOn( indexAttributes[ii] );
            }
        }
    }


    // ------------------------------------------------------------------------
    // Public Accessors - not declared in any interfaces just for this class
    // ------------------------------------------------------------------------


    /**
     * Gets the Database used by this ContextPartition.
     *
     * @return the database used
     */
    public Database getDb()
    {
        return db;
    }


    /**
     * Gets the DefaultSearchEngine used by this ContextPartition to search the
     * Database. 
     *
     * @return the search engine
     */
    public SearchEngine getEngine()
    {
        return engine;
    }


    // ------------------------------------------------------------------------
    // BackingStore Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * @see org.apache.eve.BackingStore#delete( Name )
     */
    public void delete( Name dn ) throws NamingException
    {
        BigInteger id = db.getEntryId( dn.toString() );
        
        if ( db.getChildCount( id ) > 0 )
        {
            ContextNotEmptyException cnee = new ContextNotEmptyException(
                "[66] Cannot delete entry " + dn + " it has children!" );
            cnee.setRemainingName( dn );
            throw cnee;
        }
        
        db.delete( id );
    }
    

    /**
     * @see org.apache.eve.BackingStore#add( String, Name, Attributes )
     */
    public void add( String updn, Name dn, Attributes entry ) throws NamingException
    {
        db.add( updn, dn, entry );
    }


    /**
     * @see org.apache.eve.BackingStore#modify( Name, int, Attributes )
     */
    public void modify( Name dn, int modOp, Attributes mods ) throws NamingException
    {
        db.modify( dn, modOp, mods );
    }


    /**
     * @see org.apache.eve.BackingStore#modify( Name,ModificationItem[] )
     */
    public void modify( Name dn, ModificationItem[] mods ) throws NamingException
    {
        db.modify( dn, mods );
    }


    /**
     * @see org.apache.eve.BackingStore#list( Name )
     */
    public NamingEnumeration list( Name base ) throws NamingException
    {
        return db.list( db.getEntryId( base.toString() ) );
    }
    
    
    /**
     * @see org.apache.eve.BackingStore#search(Name, Map, ExprNode, SearchControls)
     */
    public NamingEnumeration search( Name base, Map env, ExprNode filter,
                                     SearchControls searchCtls )
        throws NamingException
    {
        String [] attrIds = searchCtls.getReturningAttributes();
        NamingEnumeration underlying = null;
        
        underlying = engine.search( base, env, filter, searchCtls );
        
        return new SearchResultEnumeration( attrIds, underlying, db );
    }


    /**
     * @see org.apache.eve.BackingStore#lookup( Name )
     */
    public Attributes lookup( Name dn ) throws NamingException
    {
        return db.lookup( db.getEntryId( dn.toString() ) );
    }


    /**
     * @see BackingStore#lookup(Name,String[])
     */
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


    /**
     * @see org.apache.eve.BackingStore#hasEntry( Name )
     */
    public boolean hasEntry( Name dn ) throws NamingException
    {
        return null != db.getEntryId( dn.toString() );
    }


    /**
     * @see BackingStore#modifyRn( Name, String, boolean )
     */
    public void modifyRn( Name dn, String newRdn, boolean deleteOldRdn ) throws NamingException
    {
        db.modifyRdn( dn, newRdn, deleteOldRdn );
    }


    /**
     * @see org.apache.eve.BackingStore#move( Name, Name )
     */
    public void move( Name oldChildDn, Name newParentDn ) throws NamingException
    {
        db.move( oldChildDn, newParentDn );
    }
    

    /**
     * @see org.apache.eve.BackingStore#move( Name, Name, String, boolean )
     */
    public void move( Name oldChildDn, Name newParentDn, String newRdn,
        boolean deleteOldRdn ) throws NamingException
    {
        db.move( oldChildDn, newParentDn, newRdn, deleteOldRdn );
    }
}
