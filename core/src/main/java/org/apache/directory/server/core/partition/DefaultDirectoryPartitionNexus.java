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
package org.apache.directory.server.core.partition;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.ConfigurationException;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.DirectoryPartitionConfiguration;
import org.apache.directory.server.core.configuration.MutableDirectoryPartitionConfiguration;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmDirectoryPartition;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.MultiException;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeIdentifierException;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.message.EntryChangeControl;
import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;
import org.apache.directory.shared.ldap.message.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.PersistentSearchControl;
import org.apache.directory.shared.ldap.message.SubentriesControl;
import org.apache.directory.shared.ldap.message.extended.NoticeOfDisconnect;
import org.apache.directory.shared.ldap.name.LdapName;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.apache.directory.shared.ldap.util.NamespaceTools;
import org.apache.directory.shared.ldap.util.SingletonEnumeration;
import org.apache.directory.shared.ldap.util.StringTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A nexus for partitions dedicated for storing entries specific to a naming
 * context.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultDirectoryPartitionNexus extends DirectoryPartitionNexus
{
    private static final Logger log = LoggerFactory.getLogger( DefaultDirectoryPartitionNexus.class );

    /** the vendorName string proudly set to: Apache Software Foundation*/
    private static final String ASF = "Apache Software Foundation";

    /** the vendorName DSE operational attribute */
    private static final String VENDORNAME_ATTR = "vendorName";

    /** the vendorVersion DSE operational attribute */
    private static final String VENDORVERSION_ATTR = "vendorVersion";

    /** the namingContexts DSE operational attribute */
    private static final String NAMINGCTXS_ATTR = "namingContexts";

    /** the closed state of this partition */
    private boolean initialized;

    private DirectoryServiceConfiguration factoryCfg;

    /** the system backend */
    private DirectoryPartition system;

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
    public DefaultDirectoryPartitionNexus(Attributes rootDSE)
    {
        // setup that root DSE
        this.rootDSE = rootDSE;
        Attribute attr = new LockableAttributeImpl( "subschemaSubentry" );
        attr.add( "cn=schema,ou=system" );
        rootDSE.put( attr );

        attr = new LockableAttributeImpl( "supportedLDAPVersion" );
        rootDSE.put( attr );
        attr.add( "3" );

        attr = new LockableAttributeImpl( "supportedFeatures" );
        rootDSE.put( attr );
        attr.add( "1.3.6.1.4.1.4203.1.5.1" );

        attr = new LockableAttributeImpl( "supportedExtension" );
        rootDSE.put( attr );
        attr.add( NoticeOfDisconnect.EXTENSION_OID );

        attr = new LockableAttributeImpl( "supportedControl" );
        rootDSE.put( attr );
        attr.add( PersistentSearchControl.CONTROL_OID );
        attr.add( EntryChangeControl.CONTROL_OID );
        attr.add( SubentriesControl.CONTROL_OID );
        attr.add( ManageDsaITControl.CONTROL_OID );

        attr = new LockableAttributeImpl( "objectClass" );
        rootDSE.put( attr );
        attr.add( "top" );
        attr.add( "extensibleObject" );

        attr = new LockableAttributeImpl( NAMINGCTXS_ATTR );
        rootDSE.put( attr );

        attr = new LockableAttributeImpl( VENDORNAME_ATTR );
        attr.add( ASF );
        rootDSE.put( attr );

        Properties props = new Properties();
        try
        {
            props.load( getClass().getResourceAsStream( "version.properties" ) );
        }
        catch ( IOException e )
        {
            log.error( "failed to log version properties" );
        }

        attr = new LockableAttributeImpl( VENDORVERSION_ATTR );
        attr.add( props.getProperty( "apacheds.version", "UNKNOWN" ) );
        rootDSE.put( attr );
    }


    public void init( DirectoryServiceConfiguration factoryCfg, DirectoryPartitionConfiguration cfg )
        throws NamingException
    {
        // NOTE: We ignore ContextPartitionConfiguration parameter here.
        if ( initialized )
        {
            return;
        }

        this.factoryCfg = factoryCfg;

        List initializedPartitionCfgs = new ArrayList();
        initializedPartitionCfgs.add( initializeSystemPartition() );

        Iterator i = factoryCfg.getStartupConfiguration().getContextPartitionConfigurations().iterator();
        try
        {
            while ( i.hasNext() )
            {
                DirectoryPartitionConfiguration c = ( DirectoryPartitionConfiguration ) i.next();
                addContextPartition( c );
                initializedPartitionCfgs.add( 0, c );
            }
            initialized = true;
        }
        finally
        {
            if ( !initialized )
            {
                i = initializedPartitionCfgs.iterator();
                while ( i.hasNext() )
                {
                    DirectoryPartitionConfiguration partitionCfg = ( DirectoryPartitionConfiguration ) i.next();
                    DirectoryPartition partition = partitionCfg.getContextPartition();
                    i.remove();
                    try
                    {
                        partition.destroy();
                    }
                    catch ( Exception e )
                    {
                        log.warn( "Failed to destroy a partition: " + partitionCfg.getSuffix(), e );
                    }
                    finally
                    {
                        unregister( partition );
                    }
                }
            }
        }
    }


    private DirectoryPartitionConfiguration initializeSystemPartition() throws NamingException
    {
        // initialize system partition first
        MutableDirectoryPartitionConfiguration systemCfg = new MutableDirectoryPartitionConfiguration();
        system = new JdbmDirectoryPartition(); // using default implementation.
        systemCfg.setName( "system" );
        systemCfg.setSuffix( DirectoryPartitionNexus.SYSTEM_PARTITION_SUFFIX );
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
        Attributes systemEntry = new LockableAttributesImpl();
        Attribute objectClassAttr = new LockableAttributeImpl( "objectClass" );
        objectClassAttr.add( "top" );
        objectClassAttr.add( "organizationalUnit" );
        systemEntry.put( objectClassAttr );
        systemEntry.put( "creatorsName", DirectoryPartitionNexus.ADMIN_PRINCIPAL );
        systemEntry.put( "createTimestamp", DateUtils.getGeneralizedTime() );
        systemEntry.put( NamespaceTools.getRdnAttribute( DirectoryPartitionNexus.SYSTEM_PARTITION_SUFFIX ),
            NamespaceTools.getRdnValue( DirectoryPartitionNexus.SYSTEM_PARTITION_SUFFIX ) );
        systemCfg.setContextEntry( systemEntry );

        system.init( factoryCfg, systemCfg );
        String key = system.getSuffix( true ).toString();
        if ( partitions.containsKey( key ) )
        {
            throw new ConfigurationException( "Duplicate partition suffix: " + key );
        }
        partitions.put( key, system );

        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.add( system.getSuffix( false ).toString() );

        return systemCfg;
    }


    public boolean isInitialized()
    {
        return initialized;
    }


    public synchronized void destroy()
    {
        if ( !initialized )
        {
            return;
        }

        Iterator suffixes = new HashSet( this.partitions.keySet() ).iterator();

        // make sure this loop is not fail fast so all backing stores can
        // have an attempt at closing down and synching their cached entries
        while ( suffixes.hasNext() )
        {
            String suffix = ( String ) suffixes.next();
            try
            {
                removeContextPartition( new LdapName( suffix ) );
            }
            catch ( NamingException e )
            {
                log.warn( "Failed to destroy a partition: " + suffix, e );
            }
        }

        initialized = false;
    }


    /**
     * @see DirectoryPartition#sync()
     */
    public void sync() throws NamingException
    {
        MultiException error = null;
        Iterator list = this.partitions.values().iterator();
        while ( list.hasNext() )
        {
            DirectoryPartition partition = ( DirectoryPartition ) list.next();

            try
            {
                partition.sync();
            }
            catch ( NamingException e )
            {
                log.warn( "Failed to flush partition data out.", e );
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
    // ContextPartitionNexus Method Implementations
    // ------------------------------------------------------------------------

    public boolean compare( Name name, String oid, Object value ) throws NamingException
    {
        DirectoryPartition partition = getBackend( name );
        AttributeTypeRegistry registry = factoryCfg.getGlobalRegistries().getAttributeTypeRegistry();

        // complain if we do not recognize the attribute being compared
        if ( !registry.hasAttributeType( oid ) )
        {
            throw new LdapInvalidAttributeIdentifierException( oid + " not found within the attributeType registry" );
        }

        AttributeType attrType = registry.lookup( oid );
        Attribute attr = partition.lookup( name ).get( attrType.getName() );

        // complain if the attribute being compared does not exist in the entry
        if ( attr == null )
        {
            throw new LdapNoSuchAttributeException();
        }

        // see first if simple match without normalization succeeds
        if ( attr.contains( value ) )
        {
            return true;
        }

        // now must apply normalization to all values (attr and in request) to compare

        /*
         * Get ahold of the normalizer for the attribute and normalize the request
         * assertion value for comparisons with normalized attribute values.  Loop
         * through all values looking for a match.
         */
        Normalizer normalizer = attrType.getEquality().getNormalizer();
        Object reqVal = normalizer.normalize( value );

        for ( int ii = 0; ii < attr.size(); ii++ )
        {
            Object attrValObj = normalizer.normalize( attr.get( ii ) );
            if ( attrValObj instanceof String )
            {
                String attrVal = ( String ) attrValObj;
                if ( ( reqVal instanceof String ) && attrVal.equals( reqVal ) )
                {
                    return true;
                }
            }
            else
            {
                byte[] attrVal = ( byte[] ) attrValObj;
                if ( reqVal instanceof byte[] )
                {
                    return Arrays.equals( attrVal, ( byte[] ) reqVal );
                }
                else if ( reqVal instanceof String )
                {
                    return Arrays.equals( attrVal, StringTools.getBytesUtf8( ( String ) reqVal ) );
                }
            }
        }

        return false;
    }


    public synchronized void addContextPartition( DirectoryPartitionConfiguration config ) throws NamingException
    {
        DirectoryPartition partition = config.getContextPartition();

        // Turn on default indices
        String key = config.getSuffix();
        if ( partitions.containsKey( key ) )
        {
            throw new ConfigurationException( "Duplicate partition suffix: " + key );
        }

        partition.init( factoryCfg, config );
        partitions.put( partition.getSuffix( true ).toString(), partition );

        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.add( partition.getSuffix( false ).toString() );
    }


    public synchronized void removeContextPartition( Name suffix ) throws NamingException
    {
        String key = suffix.toString();
        DirectoryPartition partition = ( DirectoryPartition ) partitions.get( key );
        if ( partition == null )
        {
            throw new NameNotFoundException( "No partition with suffix: " + key );
        }

        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.remove( partition.getSuffix( false ).toString() );
        partitions.remove( key );

        partition.sync();
        partition.destroy();
    }


    public DirectoryPartition getSystemPartition()
    {
        return system;
    }


    /**
     * @see DirectoryPartitionNexus#getLdapContext()
     */
    public LdapContext getLdapContext()
    {
        throw new NotImplementedException();
    }


    /**
     * @see DirectoryPartitionNexus#getMatchedName(javax.naming.Name, boolean)
     */
    public Name getMatchedName( Name dn, boolean normalized ) throws NamingException
    {
        dn = ( Name ) dn.clone();
        while ( dn.size() > 0 )
        {
            if ( hasEntry( dn ) )
            {
                return dn;
            }

            dn = dn.getPrefix( 1 );
        }

        return dn;
    }


    public Name getSuffix( boolean normalized )
    {
        return new LdapName();
    }


    /**
     * @see org.apache.directory.server.core.partition.DirectoryPartitionNexus#getSuffix(javax.naming.Name, boolean)
     */
    public Name getSuffix( Name dn, boolean normalized ) throws NamingException
    {
        DirectoryPartition backend = getBackend( dn );
        return backend.getSuffix( normalized );
    }


    /**
     * @see org.apache.directory.server.core.partition.DirectoryPartitionNexus#listSuffixes(boolean)
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
    private void unregister( DirectoryPartition partition ) throws NamingException
    {
        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.remove( partition.getSuffix( false ).toString() );
        partitions.remove( partition.getSuffix( true ).toString() );
    }


    // ------------------------------------------------------------------------
    // DirectoryPartition Interface Method Implementations
    // ------------------------------------------------------------------------

    public void bind( Name bindDn, byte[] credentials, List mechanisms, String saslAuthId ) throws NamingException
    {
        DirectoryPartition partition = getBackend( bindDn );
        partition.bind( bindDn, credentials, mechanisms, saslAuthId );
    }


    public void unbind( Name bindDn ) throws NamingException
    {
        DirectoryPartition partition = getBackend( bindDn );
        partition.unbind( bindDn );
    }


    /**
     * @see DirectoryPartition#delete(javax.naming.Name)
     */
    public void delete( Name dn ) throws NamingException
    {
        DirectoryPartition backend = getBackend( dn );
        backend.delete( dn );
    }


    /**
     * Looks up the backend corresponding to the entry first, then checks to
     * see if the entry already exists.  If so an exception is thrown.  If not
     * the add operation against the backend proceeds.  This check is performed
     * here so backend implementors do not have to worry about performing these
     * kinds of checks.
     *
     * @see org.apache.directory.server.core.partition.DirectoryPartition#add(String, Name, Attributes)
     */
    public void add( String updn, Name dn, Attributes an_entry ) throws NamingException
    {
        DirectoryPartition backend = getBackend( dn );
        backend.add( updn, dn, an_entry );
    }


    /**
     * @see DirectoryPartition#modify(Name, int,Attributes)
     */
    public void modify( Name dn, int modOp, Attributes mods ) throws NamingException
    {
        DirectoryPartition backend = getBackend( dn );
        backend.modify( dn, modOp, mods );
    }


    /**
     * @see DirectoryPartition#modify(javax.naming.Name,
     * javax.naming.directory.ModificationItem[])
     */
    public void modify( Name dn, ModificationItem[] mods ) throws NamingException
    {
        DirectoryPartition backend = getBackend( dn );
        backend.modify( dn, mods );
    }


    /**
     * @see DirectoryPartition#list(javax.naming.Name)
     */
    public NamingEnumeration list( Name base ) throws NamingException
    {
        DirectoryPartition backend = getBackend( base );
        return backend.list( base );
    }


    /**
     * @see DirectoryPartition#search(Name, Map, ExprNode, SearchControls)
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

        DirectoryPartition backend = getBackend( base );
        return backend.search( base, env, filter, searchCtls );
    }


    /**
     * @see DirectoryPartition#lookup(javax.naming.Name)
     */
    public Attributes lookup( Name dn ) throws NamingException
    {
        if ( dn.size() == 0 )
        {
            return ( Attributes ) rootDSE.clone();
        }

        DirectoryPartition backend = getBackend( dn );
        return backend.lookup( dn );
    }


    /**
     * @see org.apache.directory.server.core.partition.DirectoryPartition#lookup(javax.naming.Name, String[])
     */
    public Attributes lookup( Name dn, String[] attrIds ) throws NamingException
    {
        if ( dn.size() == 0 )
        {
            Attributes retval = new LockableAttributesImpl();
            NamingEnumeration list = rootDSE.getIDs();
            while ( list.hasMore() )
            {
                String id = ( String ) list.next();
                Attribute attr = rootDSE.get( id );
                retval.put( ( Attribute ) attr.clone() );
            }
            return retval;
        }

        DirectoryPartition backend = getBackend( dn );
        return backend.lookup( dn, attrIds );
    }


    /**
     * @see DirectoryPartition#hasEntry(javax.naming.Name)
     */
    public boolean hasEntry( Name dn ) throws NamingException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Check if DN '" + dn + "' exists." );
        }

        if ( dn.size() == 0 )
        {
            return true;
        }

        DirectoryPartition backend = getBackend( dn );
        return backend.hasEntry( dn );
    }


    /**
     * @see DirectoryPartition#isSuffix(javax.naming.Name)
     */
    public boolean isSuffix( Name dn )
    {
        return partitions.containsKey( dn.toString() );
    }


    /**
     * @see DirectoryPartition#modifyRn(Name, String, boolean)
     */
    public void modifyRn( Name dn, String newRdn, boolean deleteOldRdn ) throws NamingException
    {
        DirectoryPartition backend = getBackend( dn );
        backend.modifyRn( dn, newRdn, deleteOldRdn );
    }


    /**
     * @see DirectoryPartition#move(Name, Name)
     */
    public void move( Name oriChildName, Name newParentName ) throws NamingException
    {
        DirectoryPartition backend = getBackend( oriChildName );
        backend.move( oriChildName, newParentName );
    }


    /**
     * @see DirectoryPartition#move(javax.naming.Name,
     * javax.naming.Name, java.lang.String, boolean)
     */
    public void move( Name oldChildDn, Name newParentDn, String newRdn, boolean deleteOldRdn ) throws NamingException
    {
        DirectoryPartition backend = getBackend( oldChildDn );
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
    private DirectoryPartition getBackend( Name dn ) throws NamingException
    {
        Name clonedDn = ( Name ) dn.clone();
        while ( clonedDn.size() > 0 )
        {
            if ( partitions.containsKey( clonedDn.toString() ) )
            {
                return ( DirectoryPartition ) partitions.get( clonedDn.toString() );
            }

            clonedDn.remove( clonedDn.size() - 1 );
        }
        throw new NameNotFoundException( dn.toString() );
    }


    public DirectoryPartition getPartition( Name dn ) throws NamingException
    {
        return getBackend( dn );
    }


    public void registerSupportedExtensions( Set extensionOids )
    {
        Attribute supportedExtension = rootDSE.get( "supportedExtension" );
        if ( supportedExtension == null )
        {
            supportedExtension = new LockableAttributeImpl( "supportedExtension" );
            rootDSE.put( supportedExtension );
        }
        for ( Iterator oids = extensionOids.iterator(); oids.hasNext(); )
        {
            supportedExtension.add( oids.next() );
        }
    }
}
