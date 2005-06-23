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
package org.apache.ldap.server.partition;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.ConfigurationException;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.MultiException;
import org.apache.ldap.common.NotImplementedException;
import org.apache.ldap.common.exception.LdapNameNotFoundException;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.PresenceNode;
import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.message.LockableAttributes;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.util.DateUtils;
import org.apache.ldap.common.util.NamespaceTools;
import org.apache.ldap.common.util.SingletonEnumeration;
import org.apache.ldap.server.configuration.ContextPartitionConfiguration;
import org.apache.ldap.server.configuration.MutableContextPartitionConfiguration;
import org.apache.ldap.server.jndi.ContextFactoryConfiguration;
import org.apache.ldap.server.partition.impl.btree.jdbm.JdbmContextPartition;

                                
/**
 * A nexus for partitions dedicated for storing entries specific to a naming
 * context.
 * 
 * TODO init() should initialize all mounted child partitions. (destroy too)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultContextPartitionNexus extends ContextPartitionNexus
{
    /** the vendorName string proudly set to: Apache Software Foundation*/
    private static final String ASF = "Apache Software Foundation";

    /** the vendorName DSE operational attribute */
    private static final String VENDORNAME_ATTR = "vendorName";

    /** the namingContexts DSE operational attribute */
    private static final String NAMINGCTXS_ATTR = "namingContexts";

    /** the closed state of this partition */
    private boolean initialized;

    /** the system backend */
    private ContextPartition system;

    /** the backends keyed by normalized suffix strings */
    private HashMap partitions = new HashMap();

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
    public DefaultContextPartitionNexus( Attributes rootDSE )
    {
        // setup that root DSE
        this.rootDSE = rootDSE;
        Attribute attr = new LockableAttributeImpl( "subschemaSubentry" );
        attr.add( "cn=schema,ou=system" );
        rootDSE.put( attr );

        attr = new LockableAttributeImpl( "supportedLDAPVersion" );
        rootDSE.put( attr );
        attr.add( "3" );

        attr = new LockableAttributeImpl( "objectClass" );
        rootDSE.put( attr );
        attr.add( "top" );
        attr.add( "extensibleObject" );

        attr = new LockableAttributeImpl( NAMINGCTXS_ATTR );
        rootDSE.put( attr );

        attr = new LockableAttributeImpl( VENDORNAME_ATTR );
        attr.add( ASF );
        rootDSE.put( attr );
    }


    public void init( ContextFactoryConfiguration factoryCfg, ContextPartitionConfiguration cfg ) throws NamingException
    {
        // NOTE: We ignore ContextPartitionConfiguration parameter here.
        if( initialized )
        {
            return;
        }
        
        List initializedPartitions = new ArrayList();
        initializeSystemPartition( factoryCfg );
        initializedPartitions.add( system );
        
        Iterator i = factoryCfg.getConfiguration().getContextPartitionConfigurations().iterator();
        try
        {
            while( i.hasNext() )
            {
                cfg = ( ContextPartitionConfiguration ) i.next();
                ContextPartition partition = cfg.getContextPartition();
                
                // Turn on default indices
                MutableContextPartitionConfiguration mcfg =
                    new MutableContextPartitionConfiguration();
                mcfg.setName( cfg.getName() );
                mcfg.setSuffix( cfg.getSuffix() );
                mcfg.setContextEntry( cfg.getContextEntry() );
                mcfg.setContextPartition( partition );
                
                Set indexedAttrs = cfg.getIndexedAttributes();
                indexedAttrs.add( Oid.ALIAS );
                indexedAttrs.add( Oid.EXISTANCE );
                indexedAttrs.add( Oid.HIERARCHY );
                indexedAttrs.add( Oid.NDN );
                indexedAttrs.add( Oid.ONEALIAS );
                indexedAttrs.add( Oid.SUBALIAS );
                indexedAttrs.add( Oid.UPDN );
                mcfg.setIndexedAttributes( indexedAttrs );
                
                partition.init( factoryCfg, mcfg );
                initializedPartitions.add( 0, partition );
                register( partition );
            }
            initialized = true;
        }
        finally
        {
            if( !initialized )
            {
                i = initializedPartitions.iterator();
                while( i.hasNext() )
                {
                    ContextPartition partition = ( ContextPartition ) i.next();
                    i.remove();
                    try
                    {
                        partition.destroy();
                    }
                    catch( Exception e )
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        unregister( partition );
                    }
                }
            }
        }
    }


    private void initializeSystemPartition( ContextFactoryConfiguration factoryCfg ) throws NamingException
    {
        // initialize system partition first
        MutableContextPartitionConfiguration systemCfg = new MutableContextPartitionConfiguration();
        system = new JdbmContextPartition(); // using default implementation.
        systemCfg.setName( "system" );
        systemCfg.setSuffix( ContextPartitionNexus.SYSTEM_PARTITION_SUFFIX );
        systemCfg.setContextPartition( system );
        
        // Add indexed attributes for system partition
        Set indexedSystemAttrs = new HashSet();
        indexedSystemAttrs.add( Oid.ALIAS );
        indexedSystemAttrs.add( Oid.EXISTANCE );
        indexedSystemAttrs.add( Oid.HIERARCHY );
        indexedSystemAttrs.add( Oid.NDN );
        indexedSystemAttrs.add( Oid.ONEALIAS );
        indexedSystemAttrs.add( Oid.SUBALIAS );
        indexedSystemAttrs.add( Oid.UPDN );
        systemCfg.setIndexedAttributes( indexedSystemAttrs );
        
        // Add context entry for system partition
        Attributes systemEntry = new BasicAttributes();
        Attribute objectClassAttr = new BasicAttribute( "objectClass" );
        objectClassAttr.add( "top" );
        objectClassAttr.add( "organizationalUnit" );
        systemEntry.put( objectClassAttr );
        systemEntry.put( "creatorsName", ContextPartitionNexus.ADMIN_PRINCIPAL ) ;
        systemEntry.put( "createTimestamp", DateUtils.getGeneralizedTime() ) ;
        systemEntry.put(
                NamespaceTools.getRdnAttribute( ContextPartitionNexus.SYSTEM_PARTITION_SUFFIX ),
                NamespaceTools.getRdnValue( ContextPartitionNexus.SYSTEM_PARTITION_SUFFIX ) ) ;
        systemCfg.setContextEntry( systemEntry );

        system.init( factoryCfg, systemCfg );
        register( system );
    }


    public boolean isInitialized()
    {
        return initialized;
    }


    public synchronized void destroy() throws NamingException
    {
        if ( !initialized )
        {
            return;
        }

        MultiException error = null;

        Iterator list = this.partitions.values().iterator();

        // make sure this loop is not fail fast so all backing stores can
        // have an attempt at closing down and synching their cached entries
        while ( list.hasNext() )
        {
            ContextPartition partition = ( ContextPartition ) list.next();

            try
            {
                partition.sync();
                partition.destroy();
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

        initialized = false;

        if ( error != null )
        {
            String msg = "Encountered failures while performing a close() operation on backing stores";

            NamingException total = new NamingException( msg );

            total.setRootCause( error );

            throw total;
        }
    }


    /**
     * @see ContextPartition#sync()
     */
    public void sync() throws NamingException
    {
        MultiException error = null;

        Iterator list = this.partitions.values().iterator();

        while ( list.hasNext() )
        {
            ContextPartition store = ( ContextPartition ) list.next();

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
            String msg = "Encountered failures while performing a sync() operation on backing stores";

            NamingException total = new NamingException( msg );

            total.setRootCause( error );
        }
    }


    // ------------------------------------------------------------------------
    // BackendNexus Interface Method Implementations
    // ------------------------------------------------------------------------
    
    
    public ContextPartition getSystemPartition()
    {
        return system;
    }

    /**
     * @see ContextPartitionNexus#getLdapContext()
     */
    public LdapContext getLdapContext() 
    {
        throw new NotImplementedException();
    }


    /**
     * @see ContextPartitionNexus#getMatchedDn(javax.naming.Name, boolean)
     */
    public Name getMatchedDn( Name dn, boolean normalized ) throws NamingException
    {
        dn = ( Name ) dn.clone();

        while ( dn.size() > 0 )
        {
            if ( hasEntry( dn ) )
            {
                return dn;
            }

            dn = dn.getSuffix( 1 );
        }

        return dn;
    }


    public Name getSuffix( boolean normalized )
    {
        return new LdapName();
    }


    /**
     * @see org.apache.ldap.server.partition.ContextPartitionNexus#getSuffix(javax.naming.Name, boolean)
     */
    public Name getSuffix( Name dn, boolean normalized ) throws NamingException
    {
        ContextPartition backend = getBackend( dn );

        return backend.getSuffix( normalized );
    }


    /**
     * @see org.apache.ldap.server.partition.ContextPartitionNexus#listSuffixes(boolean)
     */
    public Iterator listSuffixes( boolean normalized ) throws NamingException
    {
        return Collections.unmodifiableSet( partitions.keySet() ).iterator();
    }


    public Attributes getRootDSE() 
    {
        return rootDSE;
    }


    /**
     * Registers an ContextPartition with this BackendManager.  Called by each
     * ContextPartition implementation after it has started to register for
     * backend operation calls.  This method effectively puts the 
     * ContextPartition's naming context online.
     *
     * Operations against the naming context should result in an LDAP BUSY
     * result code in the returnValue if the naming context is not online.
     *
     * @param partition ContextPartition component to register with this
     * BackendNexus.
     * @throws ConfigurationException 
     */
    private void register( ContextPartition partition ) throws NamingException
    {
        String key = partition.getSuffix( true ).toString();
        if( partitions.containsKey( key ) )
        {
            throw new ConfigurationException( "Duplicate partition suffix: " + key );
        }
        partitions.put( key, partition );

        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.add( partition.getSuffix( false ).toString() );
    }


    /**
     * Unregisters an ContextPartition with this BackendManager.  Called for each
     * registered Backend right befor it is to be stopped.  This prevents
     * protocol server requests from reaching the Backend and effectively puts
     * the ContextPartition's naming context offline.
     *
     * Operations against the naming context should result in an LDAP BUSY
     * result code in the returnValue if the naming context is not online.
     *
     * @param partition ContextPartition component to unregister with this
     * BackendNexus.
     */
    private void unregister( ContextPartition partition ) throws NamingException
    {
        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.remove( partition.getSuffix( false ).toString() );
        partitions.remove( partition.getSuffix( true ).toString() );
    }


    // ------------------------------------------------------------------------
    // Backend Interface Method Implementations
    // ------------------------------------------------------------------------
    
    
    /**
     * @see ContextPartition#delete(javax.naming.Name)
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
     * @see org.apache.ldap.server.partition.ContextPartition#add(String, Name, Attributes)
     */
    public void add( String updn, Name dn, Attributes an_entry ) throws NamingException
    {
        ContextPartition backend = getBackend( dn );

        backend.add( updn, dn, an_entry );
    }


    /**
     * @see ContextPartition#modify(Name, int,Attributes)
     */
    public void modify( Name dn, int modOp, Attributes mods ) throws NamingException
    {
        ContextPartition backend = getBackend( dn );

        backend.modify( dn, modOp, mods );
    }


    /**
     * @see ContextPartition#modify(javax.naming.Name,
     * javax.naming.directory.ModificationItem[])
     */
    public void modify( Name dn, ModificationItem[] mods ) throws NamingException
    {
        ContextPartition backend = getBackend( dn );

        backend.modify( dn, mods );
    }

    
    /**
     * @see ContextPartition#list(javax.naming.Name)
     */
    public NamingEnumeration list( Name base ) throws NamingException
    {
        ContextPartition backend = getBackend( base );

        return backend.list( base );
    }
    

    /**
     * @see ContextPartition#search(Name, Map, ExprNode, SearchControls)
     */
    public NamingEnumeration search( Name base, Map env, ExprNode filter, SearchControls searchCtls )
            throws NamingException
    {

        if ( base.size() == 0 )
        {
            boolean isObjectScope = searchCtls.getSearchScope() == SearchControls.OBJECT_SCOPE;

            boolean isSearchAll = ( ( PresenceNode ) filter ).getAttribute().equalsIgnoreCase( "objectclass" );

            /*
             * if basedn is "", filter is "(objectclass=*)" and scope is object
             * then we have a request for the rootDSE
             */
            if ( filter instanceof PresenceNode && isObjectScope && isSearchAll )
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
     * @see ContextPartition#lookup(javax.naming.Name)
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
     * @see org.apache.ldap.server.partition.ContextPartition#lookup(javax.naming.Name, String[])
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
     * @see ContextPartition#hasEntry(javax.naming.Name)
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
     * @see ContextPartition#isSuffix(javax.naming.Name)
     */
    public boolean isSuffix( Name dn )
    {
        return partitions.containsKey( dn.toString() );
    }

    
    /**
     * @see ContextPartition#modifyRn(Name, String, boolean)
     */
    public void modifyRn( Name dn, String newRdn, boolean deleteOldRdn ) throws NamingException
    {
        ContextPartition backend = getBackend( dn );

        backend.modifyRn( dn, newRdn, deleteOldRdn );
    }
    
    
    /**
     * @see ContextPartition#move(Name, Name)
     */
    public void move( Name oriChildName, Name newParentName ) throws NamingException
    {
        ContextPartition backend = getBackend( oriChildName );

        backend.move( oriChildName, newParentName );
    }
    
    
    /**
     * @see ContextPartition#move(javax.naming.Name,
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
     * Gets the backend partition associated with a normalized dn.
     *
     * @param dn the normalized distinguished name to resolve to a backend
     * @return the backend partition associated with the normalized dn
     * @throws NamingException if the name cannot be resolved to a backend
     */
    private ContextPartition getBackend( Name dn ) throws NamingException
    {
        Name clonedDn = ( Name ) dn.clone();

        while ( clonedDn.size() > 0 )
        {
            if ( partitions.containsKey( clonedDn.toString() ) )
            {
                return ( ContextPartition ) partitions.get( clonedDn.toString() );
            }
            
            clonedDn.remove( clonedDn.size() - 1 );
        }
        
        throw new NameNotFoundException( dn.toString() );
    }
}
