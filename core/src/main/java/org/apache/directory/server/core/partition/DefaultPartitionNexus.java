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
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.partition.impl.btree.MutableBTreePartitionConfiguration;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.MultiException;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeIdentifierException;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.message.EntryChangeControl;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.PersistentSearchControl;
import org.apache.directory.shared.ldap.message.SubentriesControl;
import org.apache.directory.shared.ldap.message.extended.NoticeOfDisconnect;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.UsageEnum;
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
public class DefaultPartitionNexus extends PartitionNexus
{
    private static final Logger log = LoggerFactory.getLogger( DefaultPartitionNexus.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

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
    private Partition system;

    /** the backends keyed by normalized suffix strings */
    private Map<String, Partition> partitions = new HashMap<String, Partition>();

    /** the read only rootDSE attributes */
    private final Attributes rootDSE;

    private AttributeTypeRegistry attrRegistry;
    private OidRegistry oidRegistry;


    /**
     * Creates the root nexus singleton of the entire system.  The root DSE has
     * several attributes that are injected into it besides those that may
     * already exist.  As partitions are added to the system more namingContexts
     * attributes are added to the rootDSE.
     *
     * @see <a href="http://www.faqs.org/rfcs/rfc3045.html">Vendor Information</a>
     */
    public DefaultPartitionNexus( Attributes rootDSE )
    {
        // setup that root DSE
        this.rootDSE = rootDSE;
        Attribute attr = new AttributeImpl( "subschemaSubentry" );
        attr.add( "cn=schema,ou=system" );
        rootDSE.put( attr );

        attr = new AttributeImpl( "supportedLDAPVersion" );
        rootDSE.put( attr );
        attr.add( "3" );

        attr = new AttributeImpl( "supportedFeatures" );
        rootDSE.put( attr );
        attr.add( "1.3.6.1.4.1.4203.1.5.1" );

        attr = new AttributeImpl( "supportedExtension" );
        rootDSE.put( attr );
        attr.add( NoticeOfDisconnect.EXTENSION_OID );

        attr = new AttributeImpl( "supportedControl" );
        rootDSE.put( attr );
        attr.add( PersistentSearchControl.CONTROL_OID );
        attr.add( EntryChangeControl.CONTROL_OID );
        attr.add( SubentriesControl.CONTROL_OID );
        attr.add( ManageDsaITControl.CONTROL_OID );

        attr = new AttributeImpl( "objectClass" );
        rootDSE.put( attr );
        attr.add( "top" );
        attr.add( "extensibleObject" );

        attr = new AttributeImpl( NAMINGCTXS_ATTR );
        rootDSE.put( attr );

        attr = new AttributeImpl( VENDORNAME_ATTR );
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

        attr = new AttributeImpl( VENDORVERSION_ATTR );
        attr.add( props.getProperty( "apacheds.version", "UNKNOWN" ) );
        rootDSE.put( attr );
    }


    public void init( DirectoryServiceConfiguration factoryCfg, PartitionConfiguration cfg )
        throws NamingException
    {
        // NOTE: We ignore ContextPartitionConfiguration parameter here.
        if ( initialized )
        {
            return;
        }

        this.factoryCfg = factoryCfg;
        this.attrRegistry = this.factoryCfg.getRegistries().getAttributeTypeRegistry();
        this.oidRegistry = this.factoryCfg.getRegistries().getOidRegistry();
        
        List<PartitionConfiguration> initializedPartitionCfgs = new ArrayList<PartitionConfiguration>();
        initializedPartitionCfgs.add( initializeSystemPartition() );

        Iterator i = factoryCfg.getStartupConfiguration().getPartitionConfigurations().iterator();
        try
        {
            while ( i.hasNext() )
            {
                PartitionConfiguration c = ( PartitionConfiguration ) i.next();
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
                    PartitionConfiguration partitionCfg = ( PartitionConfiguration ) i.next();
                    Partition partition = partitionCfg.getContextPartition();
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


    private PartitionConfiguration initializeSystemPartition() throws NamingException
    {
        // initialize system partition first
        MutableBTreePartitionConfiguration systemCfg;
        PartitionConfiguration overrides = factoryCfg.getStartupConfiguration().getSystemPartitionConfiguration();
        if ( overrides != null )
        {
            systemCfg = MutableBTreePartitionConfiguration.getConfiguration( overrides );

            // ---------------------------------------------------------------
            // Add some attributes w/ some only if they're missing.  Allows 
            // users to add more attributes to the system namingConext root
            // ---------------------------------------------------------------
            
            Attributes systemEntry = systemCfg.getContextEntry();
            Attribute objectClassAttr = systemEntry.get( "objectClass" );
            if ( objectClassAttr == null )
            {
                objectClassAttr = new AttributeImpl(  "objectClass" );
                systemEntry.put( objectClassAttr );
            }
            objectClassAttr.add( "top" );
            objectClassAttr.add( "organizationalUnit" );
            objectClassAttr.add( "extensibleObject" );
            systemEntry.put( "creatorsName", PartitionNexus.ADMIN_PRINCIPAL );
            systemEntry.put( "createTimestamp", DateUtils.getGeneralizedTime() );
            systemEntry.put( NamespaceTools.getRdnAttribute( PartitionNexus.SYSTEM_PARTITION_SUFFIX ),
                NamespaceTools.getRdnValue( PartitionNexus.SYSTEM_PARTITION_SUFFIX ) );
            systemCfg.setContextEntry( systemEntry );
            
            // ---------------------------------------------------------------
            // check a few things to make sure users configured it properly
            // ---------------------------------------------------------------

            if ( ! systemCfg.getName().equals( "system" ) ) 
            {
                throw new ConfigurationException( "System partition has wrong name: should be 'system'." );
            }
            
            // add all attribute oids of index configs to a hashset
            Set indices = systemCfg.getIndexedAttributes();
            Set<String> indexOids = new HashSet<String>();
            OidRegistry registry = factoryCfg.getRegistries().getOidRegistry();
            for ( Object index : indices )
            {
                indexOids.add( registry.getOid( index.toString() ) );
            }
            
            if ( ! indexOids.contains( Oid.ALIAS ) )
            {
                indices.add( Oid.ALIAS );
            }
            if ( ! indexOids.contains( Oid.EXISTANCE ) )
            {
                indices.add( Oid.EXISTANCE );
            }
            if ( ! indexOids.contains( Oid.HIERARCHY ) )
            {
                indices.add( Oid.HIERARCHY );
            }
            if ( ! indexOids.contains( Oid.NDN ) )
            {
                indices.add( Oid.NDN );
            }
            if ( ! indexOids.contains( Oid.ONEALIAS ) )
            {
                indices.add( Oid.ONEALIAS );
            }
            if ( ! indexOids.contains( Oid.SUBALIAS ) )
            {
                indices.add( Oid.SUBALIAS );
            }
            if ( ! indexOids.contains( Oid.UPDN ) )
            {
                indices.add( Oid.UPDN );
            }
            
            if ( ! indexOids.contains( registry.getOid( "objectClass" ) ) )
            {
                log.warn( "CAUTION: You have not included objectClass as an indexed attribute" +
                        "in the system partition configuration.  This will lead to poor " +
                        "performance.  The server is automatically adding this index for you." );
                indices.add( "objectClass" );
            }
        }
        else
        {
            systemCfg = new MutableBTreePartitionConfiguration();
            systemCfg.setName( "system" );
            
            // @TODO need to make this configurable for the system partition
            systemCfg.setCacheSize( 500 );
            
            systemCfg.setSuffix( PartitionNexus.SYSTEM_PARTITION_SUFFIX );
    
            // Add indexed attributes for system partition
            Set<String> indexedSystemAttrs = new HashSet<String>();
            indexedSystemAttrs.add( Oid.ALIAS );
            indexedSystemAttrs.add( Oid.EXISTANCE );
            indexedSystemAttrs.add( Oid.HIERARCHY );
            indexedSystemAttrs.add( Oid.NDN );
            indexedSystemAttrs.add( Oid.ONEALIAS );
            indexedSystemAttrs.add( Oid.SUBALIAS );
            indexedSystemAttrs.add( Oid.UPDN );
            indexedSystemAttrs.add( "objectClass" );
            systemCfg.setIndexedAttributes( indexedSystemAttrs );
    
            // Add context entry for system partition
            Attributes systemEntry = new AttributesImpl();
            Attribute objectClassAttr = new AttributeImpl( "objectClass" );
            objectClassAttr.add( "top" );
            objectClassAttr.add( "organizationalUnit" );
            objectClassAttr.add( "extensibleObject" );
            systemEntry.put( objectClassAttr );
            systemEntry.put( "creatorsName", PartitionNexus.ADMIN_PRINCIPAL );
            systemEntry.put( "createTimestamp", DateUtils.getGeneralizedTime() );
            systemEntry.put( NamespaceTools.getRdnAttribute( PartitionNexus.SYSTEM_PARTITION_SUFFIX ),
                NamespaceTools.getRdnValue( PartitionNexus.SYSTEM_PARTITION_SUFFIX ) );
            systemCfg.setContextEntry( systemEntry );
        }

        system = new JdbmPartition(); // using default implementation.
        system.init( factoryCfg, systemCfg );
        systemCfg.setContextPartition( system );
        String key = system.getSuffix().toString();
        if ( partitions.containsKey( key ) )
        {
            throw new ConfigurationException( "Duplicate partition suffix: " + key );
        }
        partitions.put( key, system );

        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.add( system.getUpSuffix().toString() );

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

        Iterator<String> suffixes = new HashSet<String>( this.partitions.keySet() ).iterator();

        // make sure this loop is not fail fast so all backing stores can
        // have an attempt at closing down and synching their cached entries
        while ( suffixes.hasNext() )
        {
            String suffix = suffixes.next();
            try
            {
                removeContextPartition( new LdapDN( suffix ) );
            }
            catch ( NamingException e )
            {
                log.warn( "Failed to destroy a partition: " + suffix, e );
            }
        }

        initialized = false;
    }


    /**
     * @see Partition#sync()
     */
    public void sync() throws NamingException
    {
        MultiException error = null;
        Iterator list = this.partitions.values().iterator();
        while ( list.hasNext() )
        {
            Partition partition = ( Partition ) list.next();

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

    public boolean compare( LdapDN name, String oid, Object value ) throws NamingException
    {
        Partition partition = getBackend( name );
        AttributeTypeRegistry registry = factoryCfg.getRegistries().getAttributeTypeRegistry();

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


    public synchronized void addContextPartition( PartitionConfiguration config ) throws NamingException
    {
        Partition partition = config.getContextPartition();

        // Turn on default indices
        String key = config.getSuffix();
        if ( partitions.containsKey( key ) )
        {
            throw new ConfigurationException( "Duplicate partition suffix: " + key );
        }

        if ( ! partition.isInitialized() )
        {
            partition.init( factoryCfg, config );
        }
        
        partitions.put( partition.getSuffix().toString(), partition );

        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.add( partition.getUpSuffix().toString() );
    }


    public synchronized void removeContextPartition( LdapDN suffix ) throws NamingException
    {
        String key = suffix.toString();
        Partition partition = ( Partition ) partitions.get( key );
        if ( partition == null )
        {
            throw new NameNotFoundException( "No partition with suffix: " + key );
        }

        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.remove( partition.getUpSuffix().toString() );
        partitions.remove( key );

        partition.sync();
        partition.destroy();
    }


    public Partition getSystemPartition()
    {
        return system;
    }


    /**
     * @see PartitionNexus#getLdapContext()
     */
    public LdapContext getLdapContext()
    {
        throw new NotImplementedException();
    }


    /**
     * @see PartitionNexus#getMatchedName(org.apache.directory.shared.ldap.name.LdapDN)
     */
    public LdapDN getMatchedName ( LdapDN dn ) throws NamingException
    {
        dn = ( LdapDN ) dn.clone();
        while ( dn.size() > 0 )
        {
            if ( hasEntry( dn ) )
            {
                return dn;
            }

            dn.remove( dn.size() - 1 );
        }

        return dn;
    }


    public LdapDN getSuffix()
    {
        return LdapDN.EMPTY_LDAPDN;
    }

    public LdapDN getUpSuffix()
    {
        return LdapDN.EMPTY_LDAPDN;
    }


    /**
     * @see PartitionNexus#getSuffix(org.apache.directory.shared.ldap.name.LdapDN)
     */
    public LdapDN getSuffix ( LdapDN dn ) throws NamingException
    {
        Partition backend = getBackend( dn );
        return backend.getSuffix();
    }


    /**
     * @see PartitionNexus#listSuffixes()
     */
    public Iterator listSuffixes () throws NamingException
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
    private void unregister( Partition partition ) throws NamingException
    {
        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.remove( partition.getSuffix().toString() );
        partitions.remove( partition.getSuffix().toString() );
    }


    // ------------------------------------------------------------------------
    // DirectoryPartition Interface Method Implementations
    // ------------------------------------------------------------------------

    public void bind( LdapDN bindDn, byte[] credentials, List mechanisms, String saslAuthId ) throws NamingException
    {
        Partition partition = getBackend( bindDn );
        partition.bind( bindDn, credentials, mechanisms, saslAuthId );
    }


    public void unbind( LdapDN bindDn ) throws NamingException
    {
        Partition partition = getBackend( bindDn );
        partition.unbind( bindDn );
    }


    /**
     * @see Partition#delete(org.apache.directory.shared.ldap.name.LdapDN)
     */
    public void delete( LdapDN dn ) throws NamingException
    {
        Partition backend = getBackend( dn );
        backend.delete( dn );
    }


    /**
     * Looks up the backend corresponding to the entry first, then checks to
     * see if the entry already exists.  If so an exception is thrown.  If not
     * the add operation against the backend proceeds.  This check is performed
     * here so backend implementors do not have to worry about performing these
     * kinds of checks.
     *
     * @see Partition#add(org.apache.directory.shared.ldap.name.LdapDN,javax.naming.directory.Attributes)
     */
    public void add( LdapDN dn, Attributes entry ) throws NamingException
    {
        Partition backend = getBackend( dn );
        backend.add( dn, entry );
    }


    /**
     * @see Partition#modify(org.apache.directory.shared.ldap.name.LdapDN,int,javax.naming.directory.Attributes)
     */
    public void modify( LdapDN dn, int modOp, Attributes mods ) throws NamingException
    {
        Partition backend = getBackend( dn );
        backend.modify( dn, modOp, mods );
    }


    /**
     * @see Partition#modify(org.apache.directory.shared.ldap.name.LdapDN,javax.naming.directory.ModificationItem[])
     */
    public void modify( LdapDN dn, ModificationItemImpl[] mods ) throws NamingException
    {
        Partition backend = getBackend( dn );
        backend.modify( dn, mods );
    }


    /**
     * @see Partition#list(org.apache.directory.shared.ldap.name.LdapDN)
     */
    public NamingEnumeration list( LdapDN base ) throws NamingException
    {
        Partition backend = getBackend( base );
        return backend.list( base );
    }


    /**
     * @see Partition#search(org.apache.directory.shared.ldap.name.LdapDN,java.util.Map,org.apache.directory.shared.ldap.filter.ExprNode,javax.naming.directory.SearchControls)
     */
    public NamingEnumeration search( LdapDN base, Map env, ExprNode filter, SearchControls searchCtls )
        throws NamingException
    {

        if ( base.size() == 0 )
        {
            boolean isObjectScope = searchCtls.getSearchScope() == SearchControls.OBJECT_SCOPE;
            
            // test for (objectClass=*)
            boolean isSearchAll = ( ( PresenceNode ) filter ).getAttribute().equalsIgnoreCase( "2.5.4.0" );

            /*
             * if basedn is "", filter is "(objectclass=*)" and scope is object
             * then we have a request for the rootDSE
             */
            if ( filter instanceof PresenceNode && isObjectScope && isSearchAll )
            {
                String[] ids = searchCtls.getReturningAttributes();

                // -----------------------------------------------------------
                // If nothing is asked for then we just return the entry asis.
                // We let other mechanisms filter out operational attributes.
                // -----------------------------------------------------------
                if ( ids == null || ids.length == 0 )
                {
                    SearchResult result = new SearchResult( "", null, ( Attributes ) getRootDSE().clone(), false );
                    return new SingletonEnumeration( result );
                }
                
                // -----------------------------------------------------------
                // Collect all the real attributes besides 1.1, +, and * and
                // note if we've seen these special attributes as well.
                // -----------------------------------------------------------

                Set<String> realIds = new HashSet<String>();
                boolean containsAsterisk = false;
                boolean containsPlus = false;
                boolean containsOneDotOne = false;
                for ( int ii = 0; ii < ids.length; ii++ )
                {
                    String id = ids[ii].trim();
                    if ( id.equals( "*" ) )
                    {
                        containsAsterisk = true;
                    }
                    else if ( id.equals( "+" ) )
                    {
                        containsPlus = true;
                    }
                    else if ( id.equals( "1.1" ) )
                    {
                        containsOneDotOne = true;
                    }
                    else
                    {
                        try
                        {
                            realIds.add( oidRegistry.getOid( id ) );
                        }
                        catch ( NamingException e )
                        {
                            realIds.add( id );
                        }
                    }
                }

                // return nothing
                if ( containsOneDotOne )
                {
                    SearchResult result = new SearchResult( "", null, new AttributesImpl(), false );
                    return new SingletonEnumeration( result );
                }
                
                // return everything
                if ( containsAsterisk && containsPlus )
                {
                    SearchResult result = new SearchResult( "", null, ( Attributes ) getRootDSE().clone(), false );
                    return new SingletonEnumeration( result );
                }
                
                Attributes attrs = new AttributesImpl();
                if ( containsAsterisk )
                {
                    for ( NamingEnumeration ii = getRootDSE().getAll(); ii.hasMore(); /**/ )
                    {
                        // add all user attribute
                        Attribute attr = ( Attribute ) ii.next();
                        AttributeType type = attrRegistry.lookup( attr.getID() );
                        if ( type.getUsage() == UsageEnum.USER_APPLICATIONS )
                        {
                            attrs.put( attr );
                        }
                        // add attributes specifically asked for
                        else if ( realIds.contains( type.getOid() ) )
                        {
                            attrs.put( attr );
                        }
                    }
                }
                else if ( containsPlus )
                {
                    for ( NamingEnumeration ii = getRootDSE().getAll(); ii.hasMore(); /**/ )
                    {
                        // add all operational attributes
                        Attribute attr = ( Attribute ) ii.next();
                        AttributeType type = attrRegistry.lookup( attr.getID() );
                        if ( type.getUsage() != UsageEnum.USER_APPLICATIONS )
                        {
                            attrs.put( attr );
                        }
                        // add user attributes specifically asked for
                        else if ( realIds.contains( type.getOid() ) )
                        {
                            attrs.put( attr );
                        }
                    }
                }
                else
                {
                    for ( NamingEnumeration ii = getRootDSE().getAll(); ii.hasMore(); /**/ )
                    {
                      // add user attributes specifically asked for
                        Attribute attr = ( Attribute ) ii.next();
                        AttributeType type = attrRegistry.lookup( attr.getID() );
                        if ( realIds.contains( type.getOid() ) )
                        {
                            attrs.put( attr );
                        }
                    }
                }

                SearchResult result = new SearchResult( "", null, attrs, false );
                return new SingletonEnumeration( result );
            }

            throw new LdapNameNotFoundException();
        }

        Partition backend = getBackend( base );
        return backend.search( base, env, filter, searchCtls );
    }


    /**
     * @see Partition#lookup(org.apache.directory.shared.ldap.name.LdapDN)
     */
    public Attributes lookup( LdapDN dn ) throws NamingException
    {
        if ( dn.size() == 0 )
        {
            return ( Attributes ) rootDSE.clone();
        }

        Partition backend = getBackend( dn );
        return backend.lookup( dn );
    }


    /**
     * @see Partition#lookup(org.apache.directory.shared.ldap.name.LdapDN,String[])
     */
    public Attributes lookup( LdapDN dn, String[] attrIds ) throws NamingException
    {
        if ( dn.size() == 0 )
        {
            Attributes retval = new AttributesImpl();
            NamingEnumeration list = rootDSE.getIDs();
            while ( list.hasMore() )
            {
                String id = ( String ) list.next();
                Attribute attr = rootDSE.get( id );
                retval.put( ( Attribute ) attr.clone() );
            }
            return retval;
        }

        Partition backend = getBackend( dn );
        return backend.lookup( dn, attrIds );
    }


    /**
     * @see Partition#hasEntry(org.apache.directory.shared.ldap.name.LdapDN)
     */
    public boolean hasEntry( LdapDN dn ) throws NamingException
    {
        if ( IS_DEBUG )
        {
            log.debug( "Check if DN '" + dn + "' exists." );
        }

        if ( dn.size() == 0 )
        {
            return true;
        }

        Partition backend = getBackend( dn );
        return backend.hasEntry( dn );
    }


    /**
     * @see Partition#isSuffix(org.apache.directory.shared.ldap.name.LdapDN)
     */
    public boolean isSuffix( LdapDN dn )
    {
        return partitions.containsKey( dn.toString() );
    }


    /**
     * @see Partition#modifyRn(org.apache.directory.shared.ldap.name.LdapDN,String,boolean)
     */
    public void modifyRn( LdapDN dn, String newRdn, boolean deleteOldRdn ) throws NamingException
    {
        Partition backend = getBackend( dn );
        backend.modifyRn( dn, newRdn, deleteOldRdn );
    }


    /**
     * @see Partition#move(org.apache.directory.shared.ldap.name.LdapDN,org.apache.directory.shared.ldap.name.LdapDN)
     */
    public void move( LdapDN oriChildName, LdapDN newParentName ) throws NamingException
    {
        Partition backend = getBackend( oriChildName );
        backend.move( oriChildName, newParentName );
    }


    /**
     * @see Partition#move(org.apache.directory.shared.ldap.name.LdapDN,org.apache.directory.shared.ldap.name.LdapDN,String,boolean)
     */
    public void move( LdapDN oldChildDn, LdapDN newParentDn, String newRdn, boolean deleteOldRdn ) throws NamingException
    {
        Partition backend = getBackend( oldChildDn );
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
    private Partition getBackend( LdapDN dn ) throws NamingException
    {
        LdapDN clonedDn = ( LdapDN ) dn.clone();
        while ( clonedDn.size() > 0 )
        {
            if ( partitions.containsKey( clonedDn.toString() ) )
            {
                return ( Partition ) partitions.get( clonedDn.toString() );
            }

            clonedDn.remove( clonedDn.size() - 1 );
        }
        throw new LdapNameNotFoundException( dn.getUpName() );
    }


    public Partition getPartition( LdapDN dn ) throws NamingException
    {
        return getBackend( dn );
    }


    public void registerSupportedExtensions( Set extensionOids )
    {
        Attribute supportedExtension = rootDSE.get( "supportedExtension" );
        if ( supportedExtension == null )
        {
            supportedExtension = new AttributeImpl( "supportedExtension" );
            rootDSE.put( supportedExtension );
        }
        for ( Iterator oids = extensionOids.iterator(); oids.hasNext(); )
        {
            supportedExtension.add( ( String ) oids.next() );
        }
    }
}
