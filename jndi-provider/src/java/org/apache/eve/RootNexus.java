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
import javax.naming.NameNotFoundException;
import javax.naming.directory.*;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.PresenceNode;
import org.apache.ldap.common.NotImplementedException;
import org.apache.ldap.common.MultiException;
import org.apache.ldap.common.exception.LdapNameNotFoundException;
import org.apache.ldap.common.util.SingletonEnumeration;
import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.message.LockableAttributes;
import org.apache.ldap.common.message.LockableAttributesImpl;

                                
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
    /** the vendorName string proudly set to: Apache Software Foundation*/
    private static final String ASF = "Apache Software Foundation";
    /** the vendorName DSE operational attribute */
    private static final String VENDORNAME_ATTR = "vendorName";
    /** the namingContexts DSE operational attribute */
    private static final String NAMINGCTXS_ATTR = "namingContexts";
    /** Handle on the singleton instance of this class within the entire JVM. */
    private static RootNexus s_singleton = null;
    
    /** the closed state of this partition */
    private boolean closed = false;

    /** the system backend */
    private SystemPartition system;
    /** the backends keyed by normalized suffix strings */
    private HashMap backends = new HashMap();
    /** the read only rootDSE attributes */
    private final Attributes rootDSE;


    /**
     * Creates the root nexus singleton of the entire system.  The root DSE has
     * several attributes that are injected into it besides those that may
     * already exist.  As partitions are added to the system more namingContexts
     * attributes are added to the rootDSE.
     *
     * @see <a href="http://www.faqs.org/rfcs/rfc3045.html">Vendor Information</a>
     */
    public RootNexus( SystemPartition system, Attributes rootDSE )
    {
        if ( null != s_singleton )
        {
            throw new IllegalStateException();
        }
        
        s_singleton = this;
        this.system = system;

        // setup that root DSE
        this.rootDSE = rootDSE;
        Attribute attr = new LockableAttributeImpl( "subschemaSubentry" );
        attr.add( "cn=schema,ou=system" );
        rootDSE.put( attr );

        attr = new LockableAttributeImpl( NAMINGCTXS_ATTR );
        attr.add( "" );
        rootDSE.put( attr );

        attr = new LockableAttributeImpl( VENDORNAME_ATTR );
        attr.add( ASF );
        rootDSE.put( attr );

        // register will add to the list of namingContexts as well
        register( this.system );

        Runtime.getRuntime().addShutdownHook( new Thread( new Runnable() {
            public void run()
            {
                try
                {
                    if ( ! isClosed() )
                    {
                        RootNexus.this.close();
                    }
                }
                catch ( NamingException e )
                {
                    e.printStackTrace();
                    // @todo again we need to monitor this failure and report
                    // that it occured on shutdown specifically
                }
            }
        }, "RootNexusShutdownHook" ) );
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
    public Iterator listSuffixes( boolean normalized ) throws NamingException
    {
        return Collections.unmodifiableSet( backends.keySet() ).iterator();
    }


    /**
     * Get's the RootDSE entry for the DSA.
     *
     * @return the attributes of the RootDSE
     */
    public Attributes getRootDSE() 
    {
        return rootDSE;
    }


    /**
     * @see PartitionNexus#register(
     * ContextPartition)
     */
    public void register( ContextPartition backend )
    {
        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.add( backend.getSuffix( false ).toString() );
        backends.put( backend.getSuffix( true ).toString(), backend );
    }


    /**
     * @see PartitionNexus#unregister(
     * ContextPartition)
     */
    public void unregister( ContextPartition backend )
    {
        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.remove( backend.getSuffix( false ).toString() );
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
     * Looks up the backend corresponding to the entry first, then checks to
     * see if the entry already exists.  If so an exception is thrown.  If not
     * the add operation against the backend proceeds.  This check is performed
     * here so backend implementors do not have to worry about performing these
     * kinds of checks.
     *
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

        if ( base.size() == 0 )
        {
            /*
             * if basedn is "", filter is "(objectclass=*)" and scope is object
             * then we have a request for the rootDSE
             */
            if ( filter instanceof PresenceNode &&
                 searchCtls.getSearchScope() == SearchControls.OBJECT_SCOPE &&
                 ( ( PresenceNode ) filter ).getAttribute().equalsIgnoreCase( "objectclass" ) )
            {
                Attributes attrs = ( Attributes ) getRootDSE().clone();

                String[] ids = searchCtls.getReturningAttributes();
                if ( ids != null && ids.length > 0 )
                {
                    boolean doSwap = true;
                    Attributes askedFor = new LockableAttributesImpl();

                    for ( int ii = 0; ii < ids.length; ii++ )
                    {
                        if ( ids[ii].trim().equals( "*" ) )
                        {
                            doSwap = false;
                            break;
                        }

                        if ( attrs.get( ids[ii] ) != null )
                        {
                            askedFor.put( attrs.get( ids[ii] ) );
                        }
                    }

                    if ( doSwap )
                    {
                        attrs = askedFor;
                    }
                }

                SearchResult result = new SearchResult( "", null, attrs, false );
                return new SingletonEnumeration( result );
            }

            throw new LdapNameNotFoundException();
        }

        ContextPartition backend = getBackend( base );
        return backend.search( base, env, filter, searchCtls );
    }


    /**
     * @see BackingStore#lookup(javax.naming.Name)
     */
    public Attributes lookup( Name dn )  throws NamingException
    {
        if ( dn.size() == 0 )
        {
            LockableAttributes retval = ( LockableAttributes ) rootDSE.clone();
            retval.setLocked( true );
            return retval;
        }

        ContextPartition backend = getBackend( dn );
        return backend.lookup( dn );
    }


    /**
     * @see BackingStore#lookup(javax.naming.Name, String[])
     */
    public Attributes lookup( Name dn, String[] attrIds )  throws NamingException
    {
        if ( dn.size() == 0 )
        {
            LockableAttributes retval = new LockableAttributesImpl();
            NamingEnumeration list = rootDSE.getIDs();
            while ( list.hasMore() )
            {
                String id = ( String ) list.next();
                Attribute attr = rootDSE.get( id );
                retval.put( ( Attribute ) attr.clone() );
            }

            retval.setLocked( true );
            return retval;
        }

        ContextPartition backend = getBackend( dn );
        return backend.lookup( dn, attrIds );
    }


    /**
     * @see BackingStore#hasEntry(javax.naming.Name)
     */
    public boolean hasEntry( Name dn ) throws NamingException
    {
        if ( dn.size() == 0 )
        {
            return true;
        }

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


    /**
     * @see BackingStore#sync()
     */
    public void sync() throws NamingException
    {
        MultiException error = null;
        Iterator list = this.backends.values().iterator();
        while ( list.hasNext() )
        {
            BackingStore store = ( BackingStore ) list.next();

            try
            {
                store.sync();
            }
            catch ( NamingException e )
            {
                e.printStackTrace();

                if ( error == null )
                {
                    error = new MultiException( "Grouping many exceptions on root nexus sync()" );
                }

                // @todo really need to send this info to a monitor
                error.addThrowable( e );
            }
        }

        if ( error != null )
        {
            NamingException total = new NamingException( "Encountered failures "
                    + "while performing a sync() operation on backing stores" );
            total.setRootCause( error );
        }
    }


    /**
     * @see ContextPartition#isClosed()
     */
    public boolean isClosed()
    {
        return closed;
    }


    /**
     * @see BackingStore#close()
     */
    public synchronized void close() throws NamingException
    {
        if ( closed )
        {
            return;
        }

        MultiException error = null;
        Iterator list = this.backends.values().iterator();

        // make sure this loop is not fail fast so all backing stores can
        // have an attempt at closing down and synching their cached entries
        while ( list.hasNext() )
        {
            BackingStore store = ( BackingStore ) list.next();

            try
            {
                store.sync();
                store.close();
            }
            catch ( NamingException e )
            {
                e.printStackTrace();

                if ( error == null )
                {
                    error = new MultiException( "Grouping many exceptions on root nexus close()" );
                }

                // @todo really need to send this info to a monitor
                error.addThrowable( e );
            }
        }

        s_singleton = null;

        closed = true;

        if ( error != null )
        {
            NamingException total = new NamingException( "Encountered failures " 
                    + "while performing a close() operation on backing stores" );
            total.setRootCause( error );
            throw total;
        }
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
            
            clonedDn.remove( clonedDn.size() - 1 );
        }
        
        throw new NameNotFoundException();
    }
}
