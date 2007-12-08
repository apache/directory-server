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


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.context.*;
import org.apache.directory.server.core.partition.impl.btree.Index;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.tree.BranchNode;
import org.apache.directory.server.core.partition.tree.LeafNode;
import org.apache.directory.server.core.partition.tree.Node;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.MultiException;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.constants.ServerDNConstants;
import org.apache.directory.shared.ldap.constants.SupportedSASLMechanisms;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeIdentifierException;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.message.*;
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

import javax.naming.ConfigurationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import java.io.IOException;
import java.util.*;


/**
 * A nexus for partitions dedicated for storing entries specific to a naming
 * context.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultPartitionNexus extends PartitionNexus
{
    private static final Logger LOG = LoggerFactory.getLogger( DefaultPartitionNexus.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

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

    private DirectoryService directoryService;

    /** the system partition */
    private Partition system;

    /** the partitions keyed by normalized suffix strings */
    private Map<String, Partition> partitions = new HashMap<String, Partition>();
    
    /** A structure to hold all the partitions */
    private BranchNode partitionLookupTree = new BranchNode();
    
    /** the read only rootDSE attributes */
    private final Attributes rootDSE;

    private AttributeTypeRegistry attrRegistry;
    private OidRegistry oidRegistry;
    private Object partitionLookupTreeLock = new Object();


    /**
     * Creates the root nexus singleton of the entire system.  The root DSE has
     * several attributes that are injected into it besides those that may
     * already exist.  As partitions are added to the system more namingContexts
     * attributes are added to the rootDSE.
     *
     * @see <a href="http://www.faqs.org/rfcs/rfc3045.html">Vendor Information</a>
     * @param rootDSE the root entry for the DSA
     */
    public DefaultPartitionNexus( Attributes rootDSE )
    {
        // setup that root DSE
        this.rootDSE = rootDSE;
        Attribute attr = new AttributeImpl( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );
        attr.add( GLOBAL_SCHEMA_SUBENTRY_DN );
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

        // Add the supportedSASLMechanisms attribute to rootDSE
        attr = new AttributeImpl( SupportedSASLMechanisms.ATTRIBUTE );
        rootDSE.put( attr );
        attr.add( SupportedSASLMechanisms.GSSAPI );
        attr.add( SupportedSASLMechanisms.DIGEST_MD5 );
        attr.add( SupportedSASLMechanisms.CRAM_MD5 );

        attr = new AttributeImpl( "supportedControl" );
        rootDSE.put( attr );
        attr.add( PersistentSearchControl.CONTROL_OID );
        attr.add( EntryChangeControl.CONTROL_OID );
        attr.add( SubentriesControl.CONTROL_OID );
        attr.add( ManageDsaITControl.CONTROL_OID );
        attr.add( CascadeControl.CONTROL_OID );

        attr = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
        rootDSE.put( attr );
        attr.add( SchemaConstants.TOP_OC );
        attr.add( SchemaConstants.EXTENSIBLE_OBJECT_OC );

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
            LOG.error( "failed to LOG version properties" );
        }

        attr = new AttributeImpl( VENDORVERSION_ATTR );
        attr.add( props.getProperty( "apacheds.version", "UNKNOWN" ) );
        rootDSE.put( attr );
    }

    
    /**
     * Always returns the string "NEXUS".
     *
     * @return the string "NEXUS"
     */
    public String getId()
    {
        return "NEXUS";
    }


    // -----------------------------------------------------------------------
    // C O N F I G U R A T I O N   M E T H O D S
    // -----------------------------------------------------------------------


    /**
     * Not supported!
     *
     * @throws UnsupportedOperationException everytime
     */
    public void setId( String id )
    {
        throw new UnsupportedOperationException( "The id cannot be set for the partition nexus." );
    }


    /**
     * Returns root the rootDSE.
     *
     * @return the root entry for the DSA
     */
    public Attributes getContextEntry()
    {
        return rootDSE;
    }


    /**
     * Sets root entry for this BTreePartition.
     *
     * @throws UnsupportedOperationException everytime
     */
    public void setContextEntry( Attributes rootEntry )
    {
        throw new UnsupportedOperationException( "Setting the RootDSE is not allowed." );
    }


    /**
     * Always returns the empty String "".
     * @return the empty String ""
     */
    public String getSuffix()
    {
        return "";
    }


    /**
     * Unsupported operation on the Nexus.
     * @throws UnsupportedOperationException everytime
     */
    public void setSuffix( String suffix )
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Not support!
     */
    public void setCacheSize( int cacheSize )
    {
        throw new UnsupportedOperationException( "You cannot set the cache size of the nexus" );
    }


    /**
     * Not supported!
     *
     * @throws UnsupportedOperationException always
     */
    public int getCacheSize()
    {
        throw new UnsupportedOperationException( "There is no cache size associated with the nexus" );
    }



    public void init( DirectoryService directoryService )
        throws NamingException
    {
        // NOTE: We ignore ContextPartitionConfiguration parameter here.
        if ( initialized )
        {
            return;
        }

        this.directoryService = directoryService;
        this.attrRegistry = directoryService.getRegistries().getAttributeTypeRegistry();
        this.oidRegistry = directoryService.getRegistries().getOidRegistry();
        
        initializeSystemPartition();
        List<Partition> initializedPartitions = new ArrayList<Partition>();
        initializedPartitions.add( 0, this.system );

        //noinspection unchecked
        Iterator<? extends Partition> partitions = ( Iterator<? extends Partition> ) directoryService.getPartitions().iterator();
        try
        {
            while ( partitions.hasNext() )
            {
                Partition p = partitions.next();
                AddContextPartitionOperationContext opCtx = new AddContextPartitionOperationContext( p );
                addContextPartition( opCtx );
                initializedPartitions.add( opCtx.getPartition() );
            }
            initialized = true;
        }
        finally
        {
            if ( !initialized )
            {
                Iterator<Partition> i = initializedPartitions.iterator();
                while ( i.hasNext() )
                {
                    Partition partition = i.next();
                    i.remove();
                    try
                    {
                        partition.destroy();
                    }
                    catch ( Exception e )
                    {
                        LOG.warn( "Failed to destroy a partition: " + partition.getSuffixDn(), e );
                    }
                    finally
                    {
                        unregister( partition );
                    }
                }
            }
        }
    }


    private Partition initializeSystemPartition() throws NamingException
    {
        // initialize system partition first
        Partition override = directoryService.getSystemPartition();
        if ( override != null )
        {
            Attributes systemEntry = override.getContextEntry();
            Attribute objectClassAttr = systemEntry.get( SchemaConstants.OBJECT_CLASS_AT );
            if ( objectClassAttr == null )
            {
                objectClassAttr = new AttributeImpl(  SchemaConstants.OBJECT_CLASS_AT );
                systemEntry.put( objectClassAttr );
            }
            objectClassAttr.add( SchemaConstants.TOP_OC );
            objectClassAttr.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
            objectClassAttr.add( SchemaConstants.EXTENSIBLE_OBJECT_OC );
            systemEntry.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN );
            systemEntry.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
            systemEntry.put( NamespaceTools.getRdnAttribute( ServerDNConstants.SYSTEM_DN ),
                NamespaceTools.getRdnValue( ServerDNConstants.SYSTEM_DN ) );
            override.setContextEntry( systemEntry );
            
            // ---------------------------------------------------------------
            // check a few things to make sure users configured it properly
            // ---------------------------------------------------------------

            if ( ! override.getId().equals( "system" ) )
            {
                throw new ConfigurationException( "System partition has wrong name: should be 'system' not '"
                        + override.getId() + "'." );
            }
            
            // add all attribute oids of index configs to a hashset
            if ( override instanceof JdbmPartition )
            {
                Set<Index> indices = ( ( JdbmPartition ) override ).getIndexedAttributes();
                Set<String> indexOids = new HashSet<String>();
                OidRegistry registry = directoryService.getRegistries().getOidRegistry();

                for ( Index index : indices )
                {
                    indexOids.add( registry.getOid( index.getAttributeId() ) );
                }

                if ( ! indexOids.contains( registry.getOid( SchemaConstants.OBJECT_CLASS_AT ) ) )
                {
                    LOG.warn( "CAUTION: You have not included objectClass as an indexed attribute" +
                            "in the system partition configuration.  This will lead to poor " +
                            "performance.  The server is automatically adding this index for you." );
                    JdbmIndex index = new JdbmIndex();
                    index.setAttributeId( SchemaConstants.OBJECT_CLASS_AT );
                    indices.add( index );
                }

                ( ( JdbmPartition ) override ).setIndexedAttributes( indices );

                system = override;
            }
        }
        else
        {
            system = new JdbmPartition();
            system.setId( "system" );
            system.setCacheSize( 500 );
            system.setSuffix( ServerDNConstants.SYSTEM_DN );
    
            // Add objectClass attribute for the system partition
            Set<Index> indexedAttrs = new HashSet<Index>();
            indexedAttrs.add( new JdbmIndex( SchemaConstants.OBJECT_CLASS_AT ) );
            ( ( JdbmPartition ) system ).setIndexedAttributes( indexedAttrs );
    
            // Add context entry for system partition
            Attributes systemEntry = new AttributesImpl();
            Attribute objectClassAttr = new AttributeImpl( SchemaConstants.OBJECT_CLASS_AT );
            objectClassAttr.add( SchemaConstants.TOP_OC );
            objectClassAttr.add( SchemaConstants.ORGANIZATIONAL_UNIT_OC );
            objectClassAttr.add( SchemaConstants.EXTENSIBLE_OBJECT_OC );
            systemEntry.put( objectClassAttr );
            systemEntry.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN );
            systemEntry.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
            systemEntry.put( NamespaceTools.getRdnAttribute( ServerDNConstants.SYSTEM_DN ),
                NamespaceTools.getRdnValue( ServerDNConstants.SYSTEM_DN ) );
            system.setContextEntry( systemEntry );
        }

        system.init( directoryService );
        String key = system.getSuffixDn().toString();
        
        if ( partitions.containsKey( key ) )
        {
            throw new ConfigurationException( "Duplicate partition suffix: " + key );
        }
        
        synchronized ( partitionLookupTree )
        {
            partitions.put( key, system );
            partitionLookupTree.recursivelyAddPartition( partitionLookupTree, system.getSuffixDn(), 0, system );
            Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
            namingContexts.add( system.getUpSuffixDn().getUpName() );
        }

        return system;
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

        // make sure this loop is not fail fast so all backing stores can
        // have an attempt at closing down and synching their cached entries
        for ( String suffix : new HashSet<String>( this.partitions.keySet() ) )
        {
            try
            {
                removeContextPartition( new RemoveContextPartitionOperationContext( new LdapDN( suffix ) ) );
            }
            catch ( NamingException e )
            {
                LOG.warn( "Failed to destroy a partition: " + suffix, e );
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

        for ( Partition partition : this.partitions.values() )
        {
            try
            {
                partition.sync();
            }
            catch ( NamingException e )
            {
                LOG.warn( "Failed to flush partition data out.", e );
                if ( error == null )
                {
                    //noinspection ThrowableInstanceNeverThrown
                    error = new MultiException( "Grouping many exceptions on root nexus sync()" );
                }

                // @todo really need to send this info to a monitor
                error.addThrowable( e );
            }
        }

        if ( error != null )
        {
            String msg = "Encountered failures while performing a sync() operation on backing stores";
            //noinspection ThrowableInstanceNeverThrown
            NamingException total = new NamingException( msg );
            total.setRootCause( error );
        }
    }


    // ------------------------------------------------------------------------
    // ContextPartitionNexus Method Implementations
    // ------------------------------------------------------------------------

    public boolean compare( CompareOperationContext compareContext ) throws NamingException
    {
        Partition partition = getPartition( compareContext.getDn() );
        AttributeTypeRegistry registry = directoryService.getRegistries().getAttributeTypeRegistry();
        
        // complain if we do not recognize the attribute being compared
        if ( !registry.hasAttributeType( compareContext.getOid() ) )
        {
            throw new LdapInvalidAttributeIdentifierException( compareContext.getOid() + " not found within the attributeType registry" );
        }

        AttributeType attrType = registry.lookup( compareContext.getOid() );
        
        Attribute attr = partition.lookup( new LookupOperationContext( compareContext.getDn() ) ).get( attrType.getName() );

        // complain if the attribute being compared does not exist in the entry
        if ( attr == null )
        {
            throw new LdapNoSuchAttributeException();
        }

        // see first if simple match without normalization succeeds
        // TODO Fix DIRSERVER-832
        if ( attr.contains( compareContext.getValue() ) )
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
        Object reqVal = normalizer.normalize( compareContext.getValue() );

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


    public synchronized void addContextPartition( AddContextPartitionOperationContext opContext ) throws NamingException
    {
        Partition partition = opContext.getPartition();

        // Turn on default indices
        String key = partition.getSuffix();
        
        if ( partitions.containsKey( key ) )
        {
            throw new ConfigurationException( "Duplicate partition suffix: " + key );
        }

        if ( ! partition.isInitialized() )
        {
            partition.init( directoryService );
        }
        
        synchronized ( partitionLookupTree )
        {
        	LdapDN partitionSuffix = partition.getSuffixDn();
        	
        	if ( partitionSuffix == null )
        	{
        		throw new ConfigurationException( "The current partition does not have any suffix: " + partition.getId() );
        	}
        	
            partitions.put( partitionSuffix.toString(), partition );
            partitionLookupTree.recursivelyAddPartition( partitionLookupTree, partition.getSuffixDn(), 0, partition );

            Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );

        	LdapDN partitionUpSuffix = partition.getUpSuffixDn();
        	if ( partitionUpSuffix == null )
        	{
        		throw new ConfigurationException( "The current partition does not have any user provided suffix: " + partition.getId() );
        	}
        	
            namingContexts.add( partitionUpSuffix.getUpName() );
        }
    }


    public synchronized void removeContextPartition( RemoveContextPartitionOperationContext removeContextPartition ) throws NamingException
    {
        String key = removeContextPartition.getDn().getNormName();
        Partition partition = partitions.get( key );
        
        if ( partition == null )
        {
            throw new NameNotFoundException( "No partition with suffix: " + key );
        }

        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.remove( partition.getUpSuffixDn().getUpName() );

        // Create a new partition list. 
        // This is easier to create a new structure from scratch than to reorganize
        // the current structure. As this strcuture is not modified often
        // this is an acceptable solution.
        synchronized ( partitionLookupTree )
        {
            partitions.remove( key );
            partitionLookupTree = new BranchNode();
            
            for ( Partition part : partitions.values() )
            {
                partitionLookupTree.recursivelyAddPartition( partitionLookupTree, part.getSuffixDn(), 0, partition );
            }
    
            partition.sync();
            partition.destroy();
        }
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
     * @see PartitionNexus#getMatchedName( GetMatchedNameOperationContext )
     */
    public LdapDN getMatchedName ( GetMatchedNameOperationContext getMatchedNameContext ) throws NamingException
    {
        LdapDN dn = ( LdapDN ) getMatchedNameContext.getDn().clone();
        
        while ( dn.size() > 0 )
        {
            if ( hasEntry( new EntryOperationContext( dn ) ) )
            {
                return dn;
            }

            dn.remove( dn.size() - 1 );
        }

        return dn;
    }


    public LdapDN getSuffixDn()
    {
        return LdapDN.EMPTY_LDAPDN;
    }

    public LdapDN getUpSuffixDn()
    {
        return LdapDN.EMPTY_LDAPDN;
    }


    /**
     * @see PartitionNexus#getSuffix( GetSuffixOperationContext )
     */
    public LdapDN getSuffix ( GetSuffixOperationContext getSuffixContext ) throws NamingException
    {
        Partition backend = getPartition( getSuffixContext.getDn() );
        return backend.getSuffixDn();
    }


    /**
     * @see PartitionNexus#listSuffixes( ListSuffixOperationContext )
     */
    public Iterator<String> listSuffixes ( ListSuffixOperationContext emptyContext ) throws NamingException
    {
        return Collections.unmodifiableSet( partitions.keySet() ).iterator();
    }


    public Attributes getRootDSE( GetRootDSEOperationContext getRootDSEContext )
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
     * @throws NamingException if there are problems unregistering the partition
     */
    private void unregister( Partition partition ) throws NamingException
    {
        Attribute namingContexts = rootDSE.get( NAMINGCTXS_ATTR );
        namingContexts.remove( partition.getSuffixDn().getUpName() );
        partitions.remove( partition.getSuffixDn().toString() );
    }


    // ------------------------------------------------------------------------
    // DirectoryPartition Interface Method Implementations
    // ------------------------------------------------------------------------
    public void bind( BindOperationContext bindContext ) throws NamingException
    {
        Partition partition = getPartition( bindContext.getDn() );
        partition.bind( bindContext );
    }

    public void unbind( UnbindOperationContext unbindContext ) throws NamingException
    {
        Partition partition = getPartition( unbindContext.getDn() );
        partition.unbind( unbindContext );
    }


    /**
     * @see Partition#delete(DeleteOperationContext)
     */
    public void delete( DeleteOperationContext deleteContext ) throws NamingException
    {
        Partition backend = getPartition( deleteContext.getDn() );
        backend.delete( deleteContext );
    }


    /**
     * Looks up the backend corresponding to the entry first, then checks to
     * see if the entry already exists.  If so an exception is thrown.  If not
     * the add operation against the backend proceeds.  This check is performed
     * here so backend implementors do not have to worry about performing these
     * kinds of checks.
     *
     * @see Partition#add( AddOperationContext )
     */
    public void add( AddOperationContext addContext ) throws NamingException
    {
        Partition backend = getPartition( addContext.getDn() );
        backend.add( addContext );
    }


    public void modify( ModifyOperationContext modifyContext ) throws NamingException
    {
        Partition backend = getPartition( modifyContext.getDn() );
        backend.modify( modifyContext );
    }


    /**
     * @see Partition#list(ListOperationContext)
     */
    public NamingEnumeration<SearchResult> list( ListOperationContext opContext ) throws NamingException
    {
        Partition backend = getPartition( opContext.getDn() );
        return backend.list( opContext );
    }


    public NamingEnumeration<SearchResult> search( SearchOperationContext opContext )
        throws NamingException
    {
        LdapDN base = opContext.getDn();
        SearchControls searchCtls = opContext.getSearchControls();
        ExprNode filter = opContext.getFilter();
        
        if ( base.size() == 0 )
        {
            boolean isObjectScope = searchCtls.getSearchScope() == SearchControls.OBJECT_SCOPE;
            
            // test for (objectClass=*)
            boolean isSearchAll = ( ( PresenceNode ) filter ).getAttribute().equals( SchemaConstants.OBJECT_CLASS_AT_OID );

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
                    SearchResult result = new ServerSearchResult( "", null, ( Attributes ) getRootDSE( null ).clone(), false );
                    return new SingletonEnumeration<SearchResult>( result );
                }
                
                // -----------------------------------------------------------
                // Collect all the real attributes besides 1.1, +, and * and
                // note if we've seen these special attributes as well.
                // -----------------------------------------------------------

                Set<String> realIds = new HashSet<String>();
                boolean containsAsterisk = false;
                boolean containsPlus = false;
                boolean containsOneDotOne = false;
                
                for ( String id:ids )
                {
                    String idTrimmed = id.trim();
                    
                    if ( idTrimmed.equals( SchemaConstants.ALL_USER_ATTRIBUTES ) )
                    {
                        containsAsterisk = true;
                    }
                    else if ( idTrimmed.equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
                    {
                        containsPlus = true;
                    }
                    else if ( idTrimmed.equals( SchemaConstants.NO_ATTRIBUTE ) )
                    {
                        containsOneDotOne = true;
                    }
                    else
                    {
                        try
                        {
                            realIds.add( oidRegistry.getOid( idTrimmed ) );
                        }
                        catch ( NamingException e )
                        {
                            realIds.add( idTrimmed );
                        }
                    }
                }

                // return nothing
                if ( containsOneDotOne )
                {
                    SearchResult result = new ServerSearchResult( "", null, new AttributesImpl(), false );
                    return new SingletonEnumeration<SearchResult>( result );
                }
                
                // return everything
                if ( containsAsterisk && containsPlus )
                {
                    SearchResult result = new ServerSearchResult( "", null, ( Attributes ) getRootDSE( null ).clone(), false );
                    return new SingletonEnumeration<SearchResult>( result );
                }
                
                Attributes attrs = new AttributesImpl();
                if ( containsAsterisk )
                {
                    for ( NamingEnumeration<? extends Attribute> ii = getRootDSE( null ).getAll(); ii.hasMore(); /**/ )
                    {
                        // add all user attribute
                        Attribute attr = ii.next();
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
                    for ( NamingEnumeration<? extends Attribute> ii = getRootDSE( null ).getAll(); ii.hasMore(); /**/ )
                    {
                        // add all operational attributes
                        Attribute attr = ii.next();
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
                    for ( NamingEnumeration<? extends Attribute> ii = getRootDSE( null ).getAll(); ii.hasMore(); /**/ )
                    {
                      // add user attributes specifically asked for
                        Attribute attr = ii.next();
                        AttributeType type = attrRegistry.lookup( attr.getID() );
                        if ( realIds.contains( type.getOid() ) )
                        {
                            attrs.put( attr );
                        }
                    }
                }

                SearchResult result = new ServerSearchResult( "", null, attrs, false );
                return new SingletonEnumeration<SearchResult>( result );
            }

            throw new LdapNameNotFoundException();
        }

        Partition backend = getPartition( base );
        return backend.search( opContext );
    }


    public Attributes lookup( LookupOperationContext opContext ) throws NamingException
    {
        LdapDN dn = opContext.getDn();
        
        if ( dn.size() == 0 )
        {
            Attributes retval = new AttributesImpl();
            NamingEnumeration<String> list = rootDSE.getIDs();
     
            if ( opContext.getAttrsId() != null )
            {
                while ( list.hasMore() )
                {
                    String id = list.next();
                    
                    if ( opContext.getAttrsId().contains( id ) )
                    {
                        Attribute attr = rootDSE.get( id );
                        retval.put( ( Attribute ) attr.clone() );
                    }
                }
            }
            else
            {
                while ( list.hasMore() )
                {
                    String id = list.next();
                    Attribute attr = rootDSE.get( id );
                    retval.put( ( Attribute ) attr.clone() );
                }
            }
            
            return retval;
        }

        Partition backend = getPartition( dn );
        return backend.lookup( opContext );
    }


    /**
     * @see Partition#hasEntry(EntryOperationContext)
     */
    public boolean hasEntry( EntryOperationContext opContext ) throws NamingException
    {
        LdapDN dn = opContext.getDn();
        
        if ( IS_DEBUG )
        {
            LOG.debug( "Check if DN '" + dn + "' exists." );
        }

        if ( dn.size() == 0 )
        {
            return true;
        }

        Partition backend = getPartition( dn );
        return backend.hasEntry( opContext );
    }


    /**
     * @see Partition#rename(RenameOperationContext)
     */
    public void rename( RenameOperationContext opContext ) throws NamingException
    {
        Partition backend = getPartition( opContext.getDn() );
        backend.rename( opContext );
    }


    /**
     * @see Partition#move(MoveOperationContext)
     */
    public void move( MoveOperationContext opContext ) throws NamingException
    {
        Partition backend = getPartition( opContext.getDn() );
        backend.move( opContext );
    }


    public void moveAndRename( MoveAndRenameOperationContext opContext ) throws NamingException
    {
        Partition backend = getPartition( opContext.getDn() );
        backend.moveAndRename( opContext );
    }


    /**
     * Gets the partition associated with a normalized dn.
     *
     * @param dn the normalized distinguished name to resolve to a partition
     * @return the backend partition associated with the normalized dn
     * @throws NamingException if the name cannot be resolved to a partition
     */
    public Partition getPartition( LdapDN dn ) throws NamingException
    {
        Enumeration<String> rdns = dn.getAll();
        Node currentNode = partitionLookupTree;
        
        // This is synchronized so that we can't read the
        // partitionList when it is modified.
        synchronized ( partitionLookupTreeLock )
        {
            // Iterate through all the RDN until we find the associated partition
            while ( rdns.hasMoreElements() )
            {
                String rdn = rdns.nextElement();

                if ( currentNode == null )
                {
                    break;
                }

                if ( currentNode instanceof LeafNode )
                {
                    return ( ( LeafNode ) currentNode ).getPartition();
                }

                BranchNode currentBranch = ( BranchNode ) currentNode;
                if ( currentBranch.contains( rdn ) )
                {
                    currentNode = currentBranch.getChild( rdn );
                    
                    if ( currentNode instanceof LeafNode )
                    {
                        return ( ( LeafNode ) currentNode ).getPartition();
                    }
                }
            }
        }
        
        throw new LdapNameNotFoundException( dn.getUpName() );
    }


    // ------------------------------------------------------------------------
    // Private Methods
    // ------------------------------------------------------------------------


    public void registerSupportedExtensions( Set<String> extensionOids )
    {
        Attribute supportedExtension = rootDSE.get( "supportedExtension" );
        if ( supportedExtension == null )
        {
            supportedExtension = new AttributeImpl( "supportedExtension" );
            rootDSE.put( supportedExtension );
        }
        
        for ( String extensionOid : extensionOids )
        {
            supportedExtension.add( extensionOid );
        }
    }
}
