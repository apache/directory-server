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


import java.util.*;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.NameNotFoundException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.ModificationItem;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.NotImplementedException;

                                
/**
 * A nexus for partitions dedicated for storing entries specific to a naming
 * context.  The decision was made to rename this to RootNexus because of the
 * following improvement request in <a
 * href="http://nagoya.apache.org/jira/browse/DIREVE-23">JIRA</a>.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class RootNexus implements PartitionNexus
{
    /** Handle on the singleton instance of this class within the entire JVM. */
    private static RootNexus s_singleton = null;
    
    /** the system backend */
    private ContextPartition system;
    /** the backends keyed by normalized suffix strings */
    private HashMap backends = new HashMap();
    
    
    /**
     * Default constructor that checks to make sure that there is only one
     * instance of this class within the entire JVM.
     */
    public RootNexus( ContextPartition system )
    {
        if ( null != s_singleton )
        {
            throw new IllegalStateException();
        }
        
        s_singleton = this;
        this.system = system;
        register( this.system );
    }
    

    // ------------------------------------------------------------------------
    // BackendNexus Interface Method Implementations
    // ------------------------------------------------------------------------
    

    /**
     * @see PartitionNexus#getLdapContext()
     */
    public LdapContext getLdapContext() 
    {
        throw new NotImplementedException();
    }


    /**
     * @see PartitionNexus#getMatchedDn(javax.naming.Name,
     * boolean)
     */
    public Name getMatchedDn( Name dn, boolean normalized ) throws NamingException
    {
        throw new NotImplementedException();
    }


    /**
     * @see PartitionNexus#getSuffix(javax.naming.Name,
     * boolean)
     */
    public Name getSuffix( Name dn, boolean normalized ) throws NamingException
    {
        ContextPartition backend = getBackend( dn );
        return backend.getSuffix( normalized );
    }


    /**
     * @see PartitionNexus#listSuffixes(boolean)
     */
    public Iterator listSuffixes( boolean normalized )
        throws NamingException 
    {
        return Collections.unmodifiableSet( backends.keySet() ).iterator();
    }


    /**
     * @see PartitionNexus#getRootDSE()
     */
    public Attributes getRootDSE() 
    {
        throw new NotImplementedException();
    }


    /**
     * @see PartitionNexus#register(
     * ContextPartition)
     */
    public void register( ContextPartition backend )
    {
        backends.put( backend.getSuffix( true ).toString(), backend );
    }


    /**
     * @see PartitionNexus#unregister(
     * ContextPartition)
     */
    public void unregister( ContextPartition backend )
    {
        backends.remove( backend.getSuffix( true ).toString() );
    }


    // ------------------------------------------------------------------------
    // Backend Interface Method Implementations
    // ------------------------------------------------------------------------
    
    
    /**
     * @see BackingStore#delete(javax.naming.Name)
     */
    public void delete( Name dn ) throws NamingException
    {
        ContextPartition backend = getBackend( dn );
        backend.delete( dn );
    }


    /**
     * @see BackingStore#add(String, Name, Attributes)
     */
    public void add( String updn, Name dn, Attributes an_entry ) throws NamingException
    {
        ContextPartition backend = getBackend( dn );
        backend.add( updn, dn, an_entry );
    }


    /**
     * @see BackingStore#modify(Name, int,Attributes)
     */
    public void modify( Name dn, int modOp, Attributes mods ) throws NamingException
    {
        ContextPartition backend = getBackend( dn );
        backend.modify( dn, modOp, mods );
    }


    /**
     * @see BackingStore#modify(javax.naming.Name,
     * javax.naming.directory.ModificationItem[])
     */
    public void modify( Name dn, ModificationItem[] mods ) throws NamingException
    {
        ContextPartition backend = getBackend( dn );
        backend.modify( dn, mods );
    }

    
    /**
     * @see BackingStore#list(javax.naming.Name)
     */
    public NamingEnumeration list( Name base ) throws NamingException
    {
        ContextPartition backend = getBackend( base );
        return backend.list( base );
    }
    

    /**
     * @see BackingStore#search(Name, Map, ExprNode, SearchControls)
     */
    public NamingEnumeration search( Name base, Map env, ExprNode filter,
                                     SearchControls searchCtls )
        throws NamingException
    {
        ContextPartition backend = getBackend( base );
        return backend.search( base, env, filter, searchCtls );
    }


    /**
     * @see BackingStore#lookup(Name)
     */
    public Attributes lookup( Name dn, String [] attrIds ) throws NamingException
    {
        throw new NotImplementedException();
    }


    /**
     * @see BackingStore#lookup(javax.naming.Name)
     */
    public Attributes lookup( Name dn )  throws NamingException
    {
        ContextPartition backend = getBackend( dn );
        return backend.lookup( dn );
    }


    /**
     * @see BackingStore#hasEntry(javax.naming.Name)
     */
    public boolean hasEntry( Name dn ) throws NamingException
    {
        ContextPartition backend = getBackend( dn );
        return backend.hasEntry( dn );
    }

    
    /**
     * @see BackingStore#isSuffix(javax.naming.Name)
     */
    public boolean isSuffix( Name dn ) throws NamingException
    {
        return backends.containsKey( dn.toString() );
    }

    
    /**
     * @see BackingStore#modifyRn(Name, String, boolean)
     */
    public void modifyRn( Name dn, String newRdn, boolean deleteOldRdn ) throws NamingException
    {
        ContextPartition backend = getBackend( dn );
        backend.modifyRn( dn, newRdn, deleteOldRdn );
    }
    
    
    /**
     * @see BackingStore#move(Name, Name)
     */
    public void move( Name oriChildName, Name newParentName ) throws NamingException
    {
        ContextPartition backend = getBackend( oriChildName );
        backend.move( oriChildName, newParentName );
    }
    
    
    /**
     * @see BackingStore#move(javax.naming.Name,
     * javax.naming.Name, java.lang.String, boolean)
     */
    public void move( Name oldChildDn, Name newParentDn, String newRdn,
        boolean deleteOldRdn ) throws NamingException
    {
        ContextPartition backend = getBackend( oldChildDn );
        backend.move( oldChildDn, newParentDn, newRdn, deleteOldRdn );
    }
    

    // ------------------------------------------------------------------------
    // Private Methods
    // ------------------------------------------------------------------------
    
    
    /**
     * Gets the backend partition associated with a dn.
     * 
     * @param dn the name to resolve to a backend
     * @return the backend partition associated with the dn
     * @throws NamingException if the name cannot be resolved to a backend
     */
    private ContextPartition getBackend( Name dn ) throws NamingException
    {
        Name clonedDn = ( Name ) dn.clone();

        while ( clonedDn.size() > 0 )
        {
            if ( backends.containsKey( clonedDn.toString() ) )
            {
                return ( ContextPartition ) backends.get( clonedDn.toString() );
            }
            
            clonedDn.remove( 0 );
        }
        
        throw new NameNotFoundException();
    }
}
