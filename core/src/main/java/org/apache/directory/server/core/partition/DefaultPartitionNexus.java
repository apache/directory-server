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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.naming.ConfigurationException;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.filtering.CursorList;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.name.DnUtils;
import org.apache.directory.shared.util.exception.MultiException;
import org.apache.directory.shared.util.exception.NotImplementedException;
import org.apache.directory.shared.ldap.codec.controls.CascadeControl;
import org.apache.directory.shared.ldap.codec.controls.ManageDsaITControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue.SyncDoneValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncRequestValue.SyncRequestValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControl;
import org.apache.directory.shared.ldap.codec.search.controls.entryChange.EntryChangeControl;
import org.apache.directory.shared.ldap.codec.search.controls.pagedSearch.PagedResultsControl;
import org.apache.directory.shared.ldap.codec.search.controls.persistentSearch.PersistentSearchControl;
import org.apache.directory.shared.ldap.codec.search.controls.subentries.SubentriesControl;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.cursor.SingletonCursor;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.entry.DefaultModification;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.exception.LdapNoSuchObjectException;
import org.apache.directory.shared.ldap.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.exception.LdapOtherException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.extended.NoticeOfDisconnect;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.util.DateUtils;
import org.apache.directory.shared.ldap.util.tree.DnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A root {@link Partition} that contains all other partitions, and
 * routes all operations to the child partition that matches to its base suffixes.
 * It also provides some extended operations such as accessing rootDSE and
 * listing base suffixes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DefaultPartitionNexus extends AbstractPartition implements PartitionNexus
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultPartitionNexus.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** the vendorName string proudly set to: Apache Software Foundation*/
    private static final String ASF = "Apache Software Foundation";

    /** the read only rootDSE attributes */
    private final Entry rootDSE;

    /** The DirectoryService instance */
    private DirectoryService directoryService;

    /** the partitions keyed by normalized suffix strings */
    private Map<String, Partition> partitions = new HashMap<String, Partition>();

    /** A structure to hold all the partitions */
    private DnNode<Partition> partitionLookupTree = new DnNode<Partition>();

    /** the system partition */
    private Partition system;

    /** the closed state of this partition */
    private boolean initialized;

    /** A reference to the EntryCSN attributeType */
    private static AttributeType ENTRY_CSN_AT;

    /** A reference to the ObjectClass attributeType */
    private static AttributeType OBJECT_CLASS_AT;

    final List<Modification> mods = new ArrayList<Modification>( 2 );

    private String lastSyncedCtxCsn = null;
    
    /** The cn=schema DN */
    private DN subschemSubentryDn;



    /**
     * Creates the root nexus singleton of the entire system.  The root DSE has
     * several attributes that are injected into it besides those that may
     * already exist.  As partitions are added to the system more namingContexts
     * attributes are added to the rootDSE.
     *
     * @see <a href="http://www.faqs.org/rfcs/rfc3045.html">Vendor Information</a>
     * @param rootDSE the root entry for the DSA
     * @throws javax.naming.Exception on failure to initialize
     */
    public DefaultPartitionNexus( Entry rootDSE ) throws Exception
    {
        // setup that root DSE
        this.rootDSE = rootDSE;

        // Add the basic informations
        rootDSE.put( SchemaConstants.SUBSCHEMA_SUBENTRY_AT, ServerDNConstants.CN_SCHEMA_DN );
        rootDSE.put( SchemaConstants.SUPPORTED_LDAP_VERSION_AT, "3" );
        rootDSE.put( SchemaConstants.SUPPORTED_FEATURES_AT, SchemaConstants.FEATURE_ALL_OPERATIONAL_ATTRIBUTES );
        rootDSE.put( SchemaConstants.SUPPORTED_EXTENSION_AT, NoticeOfDisconnect.EXTENSION_OID );

        // Add the supported controls
        rootDSE.put( SchemaConstants.SUPPORTED_CONTROL_AT, PersistentSearchControl.CONTROL_OID,
            EntryChangeControl.CONTROL_OID, SubentriesControl.CONTROL_OID, ManageDsaITControl.CONTROL_OID,
            CascadeControl.CONTROL_OID, PagedResultsControl.CONTROL_OID,
            // Replication controls
            SyncDoneValueControl.CONTROL_OID, SyncInfoValueControl.CONTROL_OID, SyncRequestValueControl.CONTROL_OID,
            SyncStateValueControl.CONTROL_OID );

        // Add the objectClasses
        rootDSE.put( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, SchemaConstants.EXTENSIBLE_OBJECT_OC );

        // Add the 'vendor' name and version infos
        rootDSE.put( SchemaConstants.VENDOR_NAME_AT, ASF );

        Properties props = new Properties();

        try
        {
            props.load( getClass().getResourceAsStream( "version.properties" ) );
        }
        catch ( IOException e )
        {
            LOG.error( I18n.err( I18n.ERR_33 ) );
        }

        rootDSE.put( SchemaConstants.VENDOR_VERSION_AT, props.getProperty( "apacheds.version", "UNKNOWN" ) );
        
        // The rootDSE uuid has been randomly created
        rootDSE.put( SchemaConstants.ENTRY_UUID_AT, "f290425c-8272-4e62-8a67-92b06f38dbf5" );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#initialize()
     */
    protected void doInit() throws Exception
    {
        // NOTE: We ignore ContextPartitionConfiguration parameter here.
        if ( initialized )
        {
            return;
        }

        //this.directoryService = directoryService;
        schemaManager = directoryService.getSchemaManager();
        ENTRY_CSN_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_CSN_AT );
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );

        // Initialize and normalize the localy used DNs
        DN adminDn = directoryService.getDNFactory().create( ServerDNConstants.ADMIN_SYSTEM_DN );
        adminDn.normalize( schemaManager );

        Value<?> attr = rootDSE.get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ).get();
        subschemSubentryDn = directoryService.getDNFactory().create( attr.getString() );

        initializeSystemPartition( directoryService );

        List<Partition> initializedPartitions = new ArrayList<Partition>();
        initializedPartitions.add( 0, this.system );

        try
        {
            for ( Partition partition : directoryService.getPartitions() )
            {
                partition.setSchemaManager( schemaManager );
                addContextPartition( partition );
                initializedPartitions.add( partition );
            }

            createContextCsnModList();

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
                        LOG.warn( "Failed to destroy a partition: " + partition.getSuffix(), e );
                    }
                    finally
                    {
                        unregister( partition );
                    }
                }
            }
        }
    }


    private Partition initializeSystemPartition( DirectoryService directoryService ) throws Exception
    {
        // initialize system partition first
        Partition override = directoryService.getSystemPartition();

        if ( override != null )
        {

            // ---------------------------------------------------------------
            // check a few things to make sure users configured it properly
            // ---------------------------------------------------------------

            if ( !override.getId().equals( "system" ) )
            {
                throw new ConfigurationException( I18n.err( I18n.ERR_262, override.getId() ) );
            }

            system = override;
        }
        else
        {
            // TODO : we have to deal with this case !
        }

        system.setSchemaManager( schemaManager );
        system.initialize();

        // Add root context entry for system partition
        DN systemSuffixDn = directoryService.getDNFactory().create( ServerDNConstants.SYSTEM_DN );
        CoreSession adminSession = directoryService.getAdminSession();

        if ( !system.hasEntry( new EntryOperationContext( adminSession, systemSuffixDn ) ) )
        {
            Entry systemEntry = new DefaultEntry( schemaManager, systemSuffixDn );
            
            // Add the ObjectClasses
            systemEntry.put( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC,
                SchemaConstants.ORGANIZATIONAL_UNIT_OC, SchemaConstants.EXTENSIBLE_OBJECT_OC );
            
            // Add some operational attributes
            systemEntry.put( SchemaConstants.CREATORS_NAME_AT, ServerDNConstants.ADMIN_SYSTEM_DN );
            systemEntry.put( SchemaConstants.CREATE_TIMESTAMP_AT, DateUtils.getGeneralizedTime() );
            systemEntry.add( SchemaConstants.ENTRY_CSN_AT, directoryService.getCSN().toString() );
            systemEntry.add( SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );
            systemEntry.put( DnUtils.getRdnAttribute(ServerDNConstants.SYSTEM_DN), DnUtils
                .getRdnValue(ServerDNConstants.SYSTEM_DN) );
            
            AddOperationContext addOperationContext = new AddOperationContext( adminSession, systemEntry );
            system.add( addOperationContext );
        }

        String key = system.getSuffix().getNormName();

        if ( partitions.containsKey( key ) )
        {
            throw new ConfigurationException( I18n.err( I18n.ERR_263, key ) );
        }

        synchronized ( partitionLookupTree )
        {
            partitions.put( key, system );
            partitionLookupTree.add( system.getSuffix(), system );
            EntryAttribute namingContexts = rootDSE.get( SchemaConstants.NAMING_CONTEXTS_AT );

            if ( namingContexts == null )
            {
                namingContexts = new DefaultEntryAttribute( schemaManager
                    .getAttributeType( SchemaConstants.NAMING_CONTEXTS_AT ), system.getSuffix().getName() );
                rootDSE.put( namingContexts );
            }
            else
            {
                namingContexts.add( system.getSuffix().getName() );
            }
        }

        return system;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#destroy()
     */
    protected synchronized void doDestroy()
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
                removeContextPartition(  directoryService.getDNFactory().create( suffix ) );
            }
            catch ( Exception e )
            {
                LOG.warn( "Failed to destroy a partition: " + suffix, e );
            }
        }

        initialized = false;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#getId()
     */
    public String getId()
    {
        return "NEXUS";
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#setId(java.lang.String)
     */
    public void setId( String id )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_264 ) );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#setSuffix(java.lang.String)
     */
    public void setSuffix( DN suffix )
    {
        throw new UnsupportedOperationException();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#isInitialized()
     */
    public boolean isInitialized()
    {
        return initialized;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#sync()
     */
    public void sync() throws Exception
    {
        MultiException error = null;

        // store the contextCSN value in the entry ou=system
        // note that this modification shouldn't change the entryCSN value of ou=system entry
        try
        {
            // update only if the CSN changes
            if ( ( lastSyncedCtxCsn != null ) && !lastSyncedCtxCsn.equals( directoryService.getContextCsn() ) )
            {
                lastSyncedCtxCsn = directoryService.getContextCsn();

                EntryAttribute contextCsnAt = mods.get( 0 ).getAttribute();
                contextCsnAt.clear();
                contextCsnAt.add( lastSyncedCtxCsn );

                EntryAttribute timeStampAt = mods.get( 1 ).getAttribute();
                timeStampAt.clear();
                timeStampAt.add( DateUtils.getGeneralizedTime() );

                ModifyOperationContext csnModContext = new ModifyOperationContext( directoryService.getAdminSession(),
                    system.getSuffix(), mods );
                system.modify( csnModContext );
            }
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to save the contextCSN attribute value in ou=system entry.", e );
            if ( error == null )
            {
                error = new MultiException( I18n.err( I18n.ERR_265 ) );
            }

            error.addThrowable( e );
        }

        for ( Partition partition : this.partitions.values() )
        {
            try
            {
                partition.sync();
            }
            catch ( Exception e )
            {
                LOG.warn( "Failed to flush partition data out.", e );
                if ( error == null )
                {
                    //noinspection ThrowableInstanceNeverThrown
                    error = new MultiException( I18n.err( I18n.ERR_265 ) );
                }

                // @todo really need to send this info to a monitor
                error.addThrowable( e );
            }
        }

        if ( error != null )
        {
            throw error;
        }
    }


    // ------------------------------------------------------------------------
    // DirectoryPartition Interface Method Implementations
    // ------------------------------------------------------------------------
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#add(org.apache.directory.server.core.interceptor.context.AddOperationContext)
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        Partition backend = getPartition( addContext.getDn() );
        backend.add( addContext );

        EntryAttribute at = addContext.getEntry().get( SchemaConstants.ENTRY_CSN_AT );
        directoryService.setContextCsn( at.getString() );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#bind(org.apache.directory.server.core.interceptor.context.BindOperationContext)
     */
    public void bind( BindOperationContext bindContext ) throws LdapException
    {
        Partition partition = getPartition( bindContext.getDn() );
        partition.bind( bindContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#compare(org.apache.directory.server.core.interceptor.context.CompareOperationContext)
     */
    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        EntryAttribute attr = compareContext.getOriginalEntry().get( compareContext.getAttributeType() );

        // complain if the attribute being compared does not exist in the entry
        if ( attr == null )
        {
            throw new LdapNoSuchAttributeException();
        }

        // see first if simple match without normalization succeeds
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
        Normalizer normalizer = compareContext.getAttributeType().getEquality().getNormalizer();
        Value<?> reqVal = normalizer.normalize( compareContext.getValue() );

        for ( Value<?> value : attr )
        {
            Value<?> attrValObj = normalizer.normalize( value );

            if ( attrValObj.equals( reqVal ) )
            {
                return true;
            }
        }

        return false;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#delete(org.apache.directory.server.core.interceptor.context.DeleteOperationContext)
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        Partition backend = getPartition( deleteContext.getDn() );
        backend.delete( deleteContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#hasEntry(org.apache.directory.server.core.interceptor.context.EntryOperationContext)
     */
    public boolean hasEntry( EntryOperationContext hasEntryContext ) throws LdapException
    {
        DN dn = hasEntryContext.getDn();

        if ( IS_DEBUG )
        {
            LOG.debug( "Check if DN '" + dn + "' exists." );
        }

        if ( dn.isRootDSE() )
        {
            return true;
        }

        Partition backend = getPartition( dn );
        return backend.hasEntry( hasEntryContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#list(org.apache.directory.server.core.interceptor.context.ListOperationContext)
     */
    public EntryFilteringCursor list( ListOperationContext listContext ) throws LdapException
    {
        Partition backend = getPartition( listContext.getDn() );
        return backend.list( listContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#lookup(org.apache.directory.server.core.interceptor.context.LookupOperationContext)
     */
    public ClonedServerEntry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        DN dn = lookupContext.getDn();

        if ( dn.equals( subschemSubentryDn ) )
        {
            return new ClonedServerEntry( rootDSE.clone() );
        }

        // This is for the case we do a lookup on the rootDSE
        if ( dn.size() == 0 )
        {
            ClonedServerEntry retval = new ClonedServerEntry( rootDSE );
            Set<AttributeType> attributeTypes = rootDSE.getAttributeTypes();

            if ( lookupContext.getAttrsId() != null && !lookupContext.getAttrsId().isEmpty() )
            {
                for ( AttributeType attributeType : attributeTypes )
                {
                    String oid = attributeType.getOid();

                    if ( !lookupContext.getAttrsId().contains( oid ) )
                    {
                        retval.removeAttributes( attributeType );
                    }
                }
                return retval;
            }
            else
            {
                return new ClonedServerEntry( rootDSE );
            }
        }

        Partition backend = getPartition( dn );
        ClonedServerEntry entry =  backend.lookup( lookupContext );
        
        if ( entry == null )
        {
            LdapNoSuchObjectException e = new LdapNoSuchObjectException( "Attempt to lookup non-existant entry: "
                + dn.getName() );

            throw e;
        }

        return entry;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#lookup(java.lang.Long)
     */
    public ClonedServerEntry lookup( Long id ) throws LdapException
    {
        // TODO not implemented until we can use id to figure out the partition using
        // the partition ID component of the 64 bit Long identifier
        throw new NotImplementedException();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#modify(org.apache.directory.server.core.interceptor.context.ModifyOperationContext)
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        // Special case : if we don't have any modification to apply, just return
        if ( modifyContext.getModItems().size() == 0 )
        {
            return;
        }

        Partition backend = getPartition( modifyContext.getDn() );

        backend.modify( modifyContext );
        
        Entry alteredEntry = modifyContext.getAlteredEntry();
        
        if ( alteredEntry != null )
        {
            directoryService.setContextCsn( alteredEntry.get( ENTRY_CSN_AT ).getString() );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#move(org.apache.directory.server.core.interceptor.context.MoveOperationContext)
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        // Get the current partition
        Partition backend = getPartition( moveContext.getDn() );

        // We also have to get the new partition as it can be different
        //Partition newBackend = getPartition( opContext.getNewDn() );

        backend.move( moveContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#moveAndRename(org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext)
     */
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Partition backend = getPartition( moveAndRenameContext.getDn() );
        backend.moveAndRename( moveAndRenameContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#rename(org.apache.directory.server.core.interceptor.context.RenameOperationContext)
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        Partition backend = getPartition( renameContext.getDn() );
        backend.rename( renameContext );
    }


    private EntryFilteringCursor searchRootDSE( SearchOperationContext searchContext ) throws LdapException
    {
        SearchControls searchControls = searchContext.getSearchControls();

        String[] ids = searchControls.getReturningAttributes();

        // -----------------------------------------------------------
        // If nothing is asked for then we just return the entry asis.
        // We let other mechanisms filter out operational attributes.
        // -----------------------------------------------------------
        if ( ( ids == null ) || ( ids.length == 0 ) )
        {
            Entry rootDSE = getRootDSE( null );
            return new BaseEntryFilteringCursor( new SingletonCursor<Entry>( rootDSE ), searchContext );
        }

        // -----------------------------------------------------------
        // Collect all the real attributes besides 1.1, +, and * and
        // note if we've seen these special attributes as well.
        // -----------------------------------------------------------

        Set<String> realIds = new HashSet<String>();
        boolean allUserAttributes = searchContext.isAllUserAttributes();
        boolean allOperationalAttributes = searchContext.isAllOperationalAttributes();
        boolean noAttribute = searchContext.isNoAttributes();

        for ( String id : ids )
        {
            String idTrimmed = id.trim();

            try
            {
                realIds.add( schemaManager.getAttributeTypeRegistry().getOidByName( idTrimmed ) );
            }
            catch ( Exception e )
            {
                realIds.add( idTrimmed );
            }
        }

        // return nothing
        if ( noAttribute )
        {
            Entry serverEntry = new DefaultEntry( schemaManager, DN.EMPTY_DN );
            return new BaseEntryFilteringCursor( new SingletonCursor<Entry>( serverEntry ), searchContext );
        }

        // return everything
        if ( allUserAttributes && allOperationalAttributes )
        {
            Entry rootDSE = getRootDSE( null );
            return new BaseEntryFilteringCursor( new SingletonCursor<Entry>( rootDSE ), searchContext );
        }

        Entry serverEntry = new DefaultEntry( schemaManager, DN.EMPTY_DN );

        Entry rootDSE = getRootDSE( new GetRootDSEOperationContext( searchContext.getSession() ) );

        for ( EntryAttribute attribute : rootDSE )
        {
            AttributeType type = schemaManager.lookupAttributeTypeRegistry( attribute.getUpId() );

            if ( realIds.contains( type.getOid() ) )
            {
                serverEntry.put( attribute );
            }
            else if ( allUserAttributes && ( type.getUsage() == UsageEnum.USER_APPLICATIONS ) )
            {
                serverEntry.put( attribute );
            }
            else if ( allOperationalAttributes && ( type.getUsage() != UsageEnum.USER_APPLICATIONS ) )
            {
                serverEntry.put( attribute );
            }
        }

        return new BaseEntryFilteringCursor( new SingletonCursor<Entry>( serverEntry ), searchContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#search(org.apache.directory.server.core.interceptor.context.SearchOperationContext)
     */
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        DN base = searchContext.getDn();
        SearchControls searchCtls = searchContext.getSearchControls();
        ExprNode filter = searchContext.getFilter();

        // TODO since we're handling the *, and + in the EntryFilteringCursor
        // we may not need this code: we need see if this is actually the
        // case and remove this code.
        if ( base.size() == 0 )
        {
            // We are searching from the rootDSE. We have to distinguish three cases :
            // 1) The scope is OBJECT : we have to return the rootDSE entry, filtered
            // 2) The scope is ONELEVEL : we have to return all the Namin
            boolean isObjectScope = searchCtls.getSearchScope() == SearchControls.OBJECT_SCOPE;

            boolean isOnelevelScope = searchCtls.getSearchScope() == SearchControls.ONELEVEL_SCOPE;

            boolean isSublevelScope = searchCtls.getSearchScope() == SearchControls.SUBTREE_SCOPE;

            // test for (objectClass=*)
            boolean isSearchAll = false;

            // We have to be careful, as we may have a filter which is not a PresenceFilter
            if ( filter instanceof PresenceNode )
            {
                isSearchAll = ( ( PresenceNode ) filter ).getAttributeType().equals( OBJECT_CLASS_AT );
            }

            /*
             * if basedn is "", filter is "(objectclass=*)" and scope is object
             * then we have a request for the rootDSE
             */
            if ( ( filter instanceof PresenceNode ) && isObjectScope && isSearchAll )
            {
                return searchRootDSE( searchContext );
            }
            else if ( isObjectScope && ( !isSearchAll ) )
            {
                return new BaseEntryFilteringCursor( new EmptyCursor<Entry>(), searchContext );
            }
            else if ( isOnelevelScope )
            {
                List<EntryFilteringCursor> cursors = new ArrayList<EntryFilteringCursor>();

                for ( Partition partition : partitions.values() )
                {
                    DN contextDn = partition.getSuffix();
                    EntryOperationContext hasEntryContext = new EntryOperationContext( null, contextDn );
                    // search only if the context entry exists
                    if( partition.hasEntry( hasEntryContext ) )
                    {
                        searchContext.setDn( contextDn );
                        searchContext.setScope( SearchScope.OBJECT );
                        cursors.add( partition.search( searchContext ) );
                    }
                }

                return new CursorList( cursors, searchContext );
            }
            else if ( isSublevelScope )
            {
                List<EntryFilteringCursor> cursors = new ArrayList<EntryFilteringCursor>();

                for ( Partition partition : partitions.values() )
                {
                    ClonedServerEntry entry = partition.lookup( new LookupOperationContext( directoryService.getAdminSession(),
                        partition.getSuffix() ) );

                    if ( entry != null )
                    {
                        Partition backend = getPartition( entry.getDn() );
                        searchContext.setDn( entry.getDn() );
                        cursors.add( backend.search( searchContext ) );
                    }
                }

                // don't feed the above Cursors' list to a BaseEntryFilteringCursor it is skipping the naming context entry of each partition
                return new CursorList( cursors, searchContext );
            }

            // TODO : handle searches based on the RootDSE
            throw new LdapNoSuchObjectException();
        }

        if ( !base.isNormalized() )
        {
            base.normalize( schemaManager );
        }

        Partition backend = getPartition( base );

        return backend.search( searchContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#unbind(org.apache.directory.server.core.interceptor.context.UnbindOperationContext)
     */
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        Partition partition = getPartition( unbindContext.getDn() );
        partition.unbind( unbindContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#getRootDSE(org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext)
     */
    public Entry getRootDSE( GetRootDSEOperationContext getRootDSEContext )
    {
        return rootDSE.clone();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#addContextPartition(org.apache.directory.server.core.interceptor.context.AddContextPartitionOperationContext)
     */
    public synchronized void addContextPartition( Partition partition ) throws LdapException
    {
        // Turn on default indices
        String key = partition.getSuffix().getNormName();

        if ( partitions.containsKey( key ) )
        {
            throw new LdapOtherException( I18n.err( I18n.ERR_263, key ) );
        }

        if ( !partition.isInitialized() )
        {
            partition.initialize();
        }

        synchronized ( partitionLookupTree )
        {
            DN partitionSuffix = partition.getSuffix();

            if ( partitionSuffix == null )
            {
                throw new LdapOtherException( I18n.err( I18n.ERR_267, partition.getId() ) );
            }

            partitions.put( partitionSuffix.getNormName(), partition );
            partitionLookupTree.add( partition.getSuffix(), partition );

            EntryAttribute namingContexts = rootDSE.get( SchemaConstants.NAMING_CONTEXTS_AT );

            if ( namingContexts == null )
            {
                namingContexts = new DefaultEntryAttribute( schemaManager
                    .lookupAttributeTypeRegistry( SchemaConstants.NAMING_CONTEXTS_AT ), partitionSuffix.getName() );
                rootDSE.put( namingContexts );
            }
            else
            {
                namingContexts.add( partitionSuffix.getName() );
            }
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#removeContextPartition(DN partitionDN)
     */
    public synchronized void removeContextPartition( DN partitionDn )
        throws LdapException
    {
        // Get the Partition name. It's a DN.
        String key = partitionDn.getNormName();

        // Retrieve this partition from the aprtition's table
        Partition partition = partitions.get( key );

        if ( partition == null )
        {
            String msg = I18n.err( I18n.ERR_34, key );
            LOG.error( msg );
            throw new LdapNoSuchObjectException( msg );
        }

        String partitionSuffix = partition.getSuffix().getName();

        // Retrieve the namingContexts from the RootDSE : the partition
        // suffix must be present in those namingContexts
        EntryAttribute namingContexts = rootDSE.get( SchemaConstants.NAMING_CONTEXTS_AT );

        if ( namingContexts != null )
        {
            if ( namingContexts.contains( partitionSuffix ) )
            {
                namingContexts.remove( partitionSuffix );
            }
            else
            {
                String msg = I18n.err( I18n.ERR_35, key );
                LOG.error( msg );
                throw new LdapNoSuchObjectException( msg );
            }
        }

        // Update the partition tree
        partitionLookupTree.remove( partition.getSuffix() );
        partitions.remove( key );

        try
        {
            partition.destroy();
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage() );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#getSystemPartition()
     */
    public Partition getSystemPartition()
    {
        return system;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#getPartition(org.apache.directory.shared.ldap.name.DN)
     */
    public Partition getPartition( DN dn ) throws LdapException
    {
        Partition parent = partitionLookupTree.getElement( dn );

        if ( parent == null )
        {
            throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_268, dn ) );
        }
        else
        {
            return parent;
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#getSuffix(org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext)
     */
    public DN findSuffix( DN dn ) throws LdapException
    {
        Partition backend = getPartition( dn );

        return backend.getSuffix();
    }


    public DN getSuffix()
    {
        return null;
    }


    /* (non-Javadoc)
     */
    public Set<String> listSuffixes() throws LdapException
    {
        return Collections.unmodifiableSet( partitions.keySet() );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#registerSupportedExtensions(java.util.Set)
     */
    public void registerSupportedExtensions( Set<String> extensionOids ) throws LdapException
    {
        EntryAttribute supportedExtension = rootDSE.get( SchemaConstants.SUPPORTED_EXTENSION_AT );

        if ( supportedExtension == null )
        {
            rootDSE.set( SchemaConstants.SUPPORTED_EXTENSION_AT );
            supportedExtension = rootDSE.get( SchemaConstants.SUPPORTED_EXTENSION_AT );
        }

        for ( String extensionOid : extensionOids )
        {
            supportedExtension.add( extensionOid );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#registerSupportedSaslMechanisms(java.util.Set)
     */
    public void registerSupportedSaslMechanisms( Set<String> supportedSaslMechanisms ) throws LdapException
    {
        EntryAttribute supportedSaslMechanismsAttribute = rootDSE.get( SchemaConstants.SUPPORTED_SASL_MECHANISMS_AT );

        if ( supportedSaslMechanismsAttribute == null )
        {
            rootDSE.set( SchemaConstants.SUPPORTED_SASL_MECHANISMS_AT );
            supportedSaslMechanismsAttribute = rootDSE.get( SchemaConstants.SUPPORTED_SASL_MECHANISMS_AT );
        }

        for ( String saslMechanism : supportedSaslMechanisms )
        {
            supportedSaslMechanismsAttribute.add( saslMechanism );
        }
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
     * @throws Exception if there are problems unregistering the partition
     */
    private void unregister( Partition partition ) throws Exception
    {
        EntryAttribute namingContexts = rootDSE.get( SchemaConstants.NAMING_CONTEXTS_AT );

        if ( namingContexts != null )
        {
            namingContexts.remove( partition.getSuffix().getName() );
        }

        partitions.remove( partition.getSuffix().getName() );
    }


    /**
     * @return the directoryService
     */
    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }


    /**
     * @param directoryService the directoryService to set
     */
    public void setDirectoryService( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
    }


    private void createContextCsnModList() throws LdapException
    {
        Modification contextCsnMod = new DefaultModification();
        contextCsnMod.setOperation( ModificationOperation.REPLACE_ATTRIBUTE );
        DefaultEntryAttribute contextCsnAt = new DefaultEntryAttribute( schemaManager
            .lookupAttributeTypeRegistry( SchemaConstants.CONTEXT_CSN_AT ) );
        contextCsnMod.setAttribute( contextCsnAt );

        mods.add( contextCsnMod );

        Modification timeStampMod = new DefaultModification();
        timeStampMod.setOperation( ModificationOperation.REPLACE_ATTRIBUTE );
        DefaultEntryAttribute timeStampAt = new DefaultEntryAttribute( schemaManager
            .lookupAttributeTypeRegistry( SchemaConstants.MODIFY_TIMESTAMP_AT ) );
        timeStampMod.setAttribute( timeStampAt );

        mods.add( timeStampMod );
    }
}
