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
import java.math.BigInteger;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.ContextNotEmptyException;
import javax.naming.directory.ModificationItem;

import org.apache.ldap.common.filter.ExprNode;

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
    // Protected Mutators
    // ------------------------------------------------------------------------


    /**
     * Sets the Database used by this AtomicBackend.
     *
     * @param database the database
     */
    protected void setDb( Database database )
    {
        db = database;
    }


    /**
     * Sets the search engine to be used by this Backend.
     *
     * @param engine the search engine
     */
    protected void setEngine( SearchEngine engine )
    {
        this.engine = engine;
    }


    // ------------------------------------------------------------------------
    // Backend Interface Method Implementations
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
    public void add( String updn, Name dn, Attributes entry )
        throws NamingException
    {
        db.add( updn, dn, entry );
    }


    /**
     * @see org.apache.eve.BackingStore#modify( Name, int, Attributes )
     */
    public void modify( Name dn, int modOp, Attributes mods )
        throws NamingException
    {
        db.modify( dn, modOp, mods );
    }


    /**
     * @see org.apache.eve.BackingStore#modify( Name,ModificationItem[] )
     */
    public void modify( Name dn, ModificationItem[] mods )
        throws NamingException
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
        
        return new SearchResultEnumeration( attrIds, underlying );
    }


    /**
     * @see org.apache.eve.BackingStore#lookup( Name )
     */
    public Attributes lookup( Name dn ) throws NamingException
    {
        return db.lookup( db.getEntryId( dn.toString() ) );
    }


    /**
     * @see org.apache.eve.BackingStore#hasEntry( Name )
     */
    public boolean hasEntry( Name dn ) throws NamingException
    {
        return null != db.getEntryId( dn.toString() );
    }


    /**
     * @see org.apache.eve.BackingStore#modifyRn( Name, String, boolean )
     */
    public void modifyRdn( Name dn, String newRdn, boolean deleteOldRdn )
        throws NamingException
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
