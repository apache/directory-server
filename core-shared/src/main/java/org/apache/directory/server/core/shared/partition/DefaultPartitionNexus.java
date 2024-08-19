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
package org.apache.directory.server.core.shared.partition;


import java.io.InputStream;
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

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EmptyCursor;
import org.apache.directory.api.ldap.model.cursor.SingletonCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchAttributeException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.ObjectClassNode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.extended.NoticeOfDisconnect;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.AttributeTypeOptions;
import org.apache.directory.api.ldap.model.schema.UsageEnum;
import org.apache.directory.api.ldap.util.tree.DnNode;
import org.apache.directory.api.util.exception.MultiException;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.CursorList;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDseOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.api.partition.AbstractPartition;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.api.partition.PartitionReadTxn;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.core.api.partition.PartitionWriteTxn;
import org.apache.directory.server.core.api.partition.Subordinates;
import org.apache.directory.server.i18n.I18n;
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

    /** the fixed id: 'NEXUS' */
    private static final String NEXUS_ID = "NEXUS";

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** the vendorName string proudly set to: Apache Software Foundation*/
    private static final String ASF = "Apache Software Foundation";

    /** the read only rootDSE attributes */
    private final Entry rootDse;

    /** The DirectoryService instance */
    private DirectoryService directoryService;

    /** the partitions keyed by normalized suffix strings */
    private Map<String, Partition> partitions = new HashMap<>();

    /** A structure to hold all the partitions */
    private DnNode<Partition> partitionLookupTree = new DnNode<>();

    private final List<Modification> mods = new ArrayList<>( 2 );

    /** The cn=schema Dn */
    private Dn subschemaSubentryDn;


    /**
     * Creates the root nexus singleton of the entire system.  The root DSE has
     * several attributes that are injected into it besides those that may
     * already exist.  As partitions are added to the system more namingContexts
     * attributes are added to the rootDSE.
     *
     * @see <a href="http://www.faqs.org/rfcs/rfc3045.html">Vendor Information</a>
     * @param rootDse the root entry for the DSA
     * @throws LdapException on failure to initialize
     */
    public DefaultPartitionNexus( Entry rootDse ) throws LdapException
    {
        id = NEXUS_ID;
        suffixDn = null;

        // setup that root DSE
        this.rootDse = rootDse;

        // Add the basic informations
        rootDse.put( SchemaConstants.SUBSCHEMA_SUBENTRY_AT, ServerDNConstants.CN_SCHEMA_DN );
        rootDse.put( SchemaConstants.SUPPORTED_LDAP_VERSION_AT, "3" );
        rootDse.put( SchemaConstants.SUPPORTED_FEATURES_AT, 
            SchemaConstants.FEATURE_ALL_OPERATIONAL_ATTRIBUTES,
            SchemaConstants.FEATURE_MODIFY_INCREMENT );
        rootDse.put( SchemaConstants.SUPPORTED_EXTENSION_AT, NoticeOfDisconnect.EXTENSION_OID );

        // Add the objectClasses
        rootDse.put( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, SchemaConstants.EXTENSIBLE_OBJECT_OC );

        // Add the 'vendor' name and version infos
        rootDse.put( SchemaConstants.VENDOR_NAME_AT, ASF );

        Properties props = new Properties();

        try ( InputStream inputStream = getClass().getResourceAsStream( "version.properties" ) )
        {
            props.load( inputStream );
        }
        catch ( IOException e )
        {
            LOG.error( I18n.err( I18n.ERR_07001_FAILED_TO_LOG_VERSION ) );
        }

        rootDse.put( SchemaConstants.VENDOR_VERSION_AT, props.getProperty( "apacheds.version", "UNKNOWN" ) );

        // The rootDSE uuid has been randomly created
        rootDse.put( SchemaConstants.ENTRY_UUID_AT, "f290425c-8272-4e62-8a67-92b06f38dbf5" );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void repair() throws LdapException
    {
        // Nothing to do
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void doRepair() throws LdapException
    {
        // Nothing to do
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInit() throws LdapException
    {
        // NOTE: We ignore ContextPartitionConfiguration parameter here.
        if ( !initialized )
        {
            // Add the supported request controls
            Iterator<String> ctrlOidItr = directoryService.getLdapCodecService().registeredRequestControls();

            while ( ctrlOidItr.hasNext() )
            {
                rootDse.add( SchemaConstants.SUPPORTED_CONTROL_AT, ctrlOidItr.next() );
            }

            // Add the supported response controls
            ctrlOidItr = directoryService.getLdapCodecService().registeredResponseControls();

            while ( ctrlOidItr.hasNext() )
            {
                rootDse.add( SchemaConstants.SUPPORTED_CONTROL_AT, ctrlOidItr.next() );
            }

            schemaManager = directoryService.getSchemaManager();

            Value attr = rootDse.get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ).get();
            subschemaSubentryDn = directoryService.getDnFactory().create( attr.getString() );

            List<Partition> initializedPartitions = new ArrayList<>();

            initializedPartitions.add( 0, directoryService.getSystemPartition() );
            addContextPartition( directoryService.getSystemPartition() );

            try
            {
                for ( Partition partition : directoryService.getPartitions() )
                {
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
                            partition.destroy( partition.beginReadTransaction() );
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
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void doDestroy( PartitionTxn partitionTxn )
    {
        if ( !initialized )
        {
            return;
        }

        // make sure this loop is not fail fast so all backing stores can
        // have an attempt at closing down and synching their cached entries
        for ( String suffix : new HashSet<>( this.partitions.keySet() ) )
        {
            try
            {
                removeContextPartition( suffix );
            }
            catch ( Exception e )
            {
                LOG.warn( "Failed to destroy a partition: " + suffixDn, e );
            }
        }

        initialized = false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setId( String id )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_07005_PARTITION_ID_CANNOT_BE_SET ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSuffixDn( Dn suffix )
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void sync() throws LdapException
    {
        MultiException errors = null;

        for ( Partition partition : this.partitions.values() )
        {
            try
            {
                partition.saveContextCsn( partition.beginReadTransaction() );
                partition.sync();
            }
            catch ( Exception e )
            {
                LOG.warn( "Failed to flush partition data out.", e );

                if ( errors == null )
                {
                    //noinspection ThrowableInstanceNeverThrown
                    errors = new MultiException( I18n.err( I18n.ERR_07006_GROUPING_EXCEPTIONS_ON_ROOT_NEXUS ) );
                }

                // @todo really need to send this info to a monitor
                errors.addThrowable( e );
            }
        }

        if ( errors != null )
        {
            throw new LdapOtherException( errors.getMessage(), errors );
        }
    }


    // ------------------------------------------------------------------------
    // DirectoryPartition Interface Method Implementations
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        Partition partition = addContext.getPartition();
        partition.add( addContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean compare( CompareOperationContext compareContext ) throws LdapException
    {
        Attribute attr = compareContext.getOriginalEntry().get( compareContext.getAttributeType() );

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
        Value reqVal = compareContext.getValue();

        for ( Value value : attr )
        {
            if ( value.equals( reqVal ) )
            {
                return true;
            }
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        Partition partition = getPartition( deleteContext.getDn() );
        return partition.delete( deleteContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        Dn dn = hasEntryContext.getDn();

        if ( IS_DEBUG )
        {
            LOG.debug( "Check if Dn '{}' exists.", dn );
        }

        if ( dn.isRootDse() )
        {
            return true;
        }

        Partition partition = getPartition( dn );

        return partition.hasEntry( hasEntryContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        Dn dn = lookupContext.getDn();

        if ( dn.getNormName().equals( subschemaSubentryDn.getNormName() ) )
        {
            return new ClonedServerEntry( rootDse.clone() );
        }

        // This is for the case we do a lookup on the rootDSE
        if ( dn.isRootDse() )
        {
            return new ClonedServerEntry( rootDse );
        }

        Partition partition = getPartition( dn );
        Entry entry = partition.lookup( lookupContext );

        if ( entry == null )
        {
            throw new LdapNoSuchObjectException( "Attempt to lookup non-existant entry: "
                + dn.getName() );
        }

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        // Special case : if we don't have any modification to apply, just return
        if ( modifyContext.getModItems().isEmpty() )
        {
            return;
        }

        Partition partition = getPartition( modifyContext.getDn() );

        partition.modify( modifyContext );

        if ( modifyContext.isPushToEvtInterceptor() )
        {
            directoryService.getInterceptor( InterceptorEnum.EVENT_INTERCEPTOR.getName() ).modify( modifyContext );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        // Get the current partition
        Partition partition = getPartition( moveContext.getDn() );

        partition.move( moveContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Partition partition = getPartition( moveAndRenameContext.getDn() );
        partition.moveAndRename( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        Partition partition = getPartition( renameContext.getDn() );
        partition.rename( renameContext );
    }


    private EntryFilteringCursor searchRootDse( SearchOperationContext searchContext ) throws LdapException
    {
        Set<AttributeTypeOptions> ids = searchContext.getReturningAttributes();

        // -----------------------------------------------------------
        // If nothing is asked for then we just return the entry asis.
        // We let other mechanisms filter out operational attributes.
        // -----------------------------------------------------------
        if ( ( ids == null ) || ( ids.isEmpty() ) )
        {
            return new EntryFilteringCursorImpl( new SingletonCursor<Entry>( getRootDse( null ) ), searchContext,
                directoryService.getSchemaManager() );
        }

        // -----------------------------------------------------------
        // Collect all the real attributes besides 1.1, +, and * and
        // note if we've seen these special attributes as well.
        // -----------------------------------------------------------

        Set<String> realIds = new HashSet<>();
        boolean allUserAttributes = searchContext.isAllUserAttributes();
        boolean allOperationalAttributes = searchContext.isAllOperationalAttributes();
        boolean noAttribute = searchContext.isNoAttributes();

        for ( AttributeTypeOptions id : ids )
        {
            try
            {
                realIds.add( id.getAttributeType().getOid() );
            }
            catch ( Exception e )
            {
                realIds.add( id.getAttributeType().getName() );
            }
        }

        // return nothing
        if ( noAttribute )
        {
            Entry serverEntry = new DefaultEntry( schemaManager, Dn.ROOT_DSE );
            return new EntryFilteringCursorImpl( new SingletonCursor<Entry>( serverEntry ), searchContext,
                directoryService.getSchemaManager() );
        }

        // return everything
        if ( allUserAttributes && allOperationalAttributes )
        {
            Entry foundRootDse = getRootDse( null );
            return new EntryFilteringCursorImpl( new SingletonCursor<Entry>( foundRootDse ), searchContext,
                directoryService.getSchemaManager() );
        }

        Entry serverEntry = new DefaultEntry( schemaManager, Dn.ROOT_DSE );
        GetRootDseOperationContext getRootDseContext = new GetRootDseOperationContext( searchContext.getSession() );
        getRootDseContext.setPartition( searchContext.getPartition() );
        getRootDseContext.setTransaction( searchContext.getTransaction() );

        Entry foundRootDse = getRootDse( getRootDseContext );

        for ( Attribute attribute : foundRootDse )
        {
            AttributeType type = schemaManager.lookupAttributeTypeRegistry( attribute.getId() );

            if ( realIds.contains( type.getOid() )
                    || ( allUserAttributes && ( type.getUsage() == UsageEnum.USER_APPLICATIONS ) )
                    || ( allOperationalAttributes && ( type.getUsage() != UsageEnum.USER_APPLICATIONS ) ) )
            {
                serverEntry.put( attribute );
            }
        }

        return new EntryFilteringCursorImpl( new SingletonCursor<Entry>( serverEntry ), searchContext,
            directoryService.getSchemaManager() );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        Dn baseDn = searchContext.getDn();

        // TODO since we're handling the *, and + in the EntryFilteringCursor
        // we may not need this code: we need see if this is actually the
        // case and remove this code.
        if ( baseDn.size() == 0 )
        {
            return searchFromRoot( searchContext );
        }

        // Not sure we need this code...
        if ( !baseDn.isSchemaAware() )
        {
            searchContext.setDn( new Dn( schemaManager, baseDn ) );
        }

        // Normal case : do a search on the specific partition
        Partition backend = searchContext.getPartition();

        return backend.search( searchContext );
    }


    /**
     * Do a search from the root of the DIT. We have a few use cases to consider :
     * A) The scope is OBJECT
     * If the filter is (ObjectClass = *), then this is a RootDSE fetch, otherwise, we just
     * return nothing.
     * B) The scope is ONELEVEL
     * We just return the contextEntries of all the existing partitions
     * C) The scope is SUBLEVEL :
     * In this case, we have to do a search in each of the existing partition. We will get
     * back a list of cursors and we will wrap this list in the resulting EntryFilteringCursor.
     *
     * @param searchContext
     * @return
     * @throws LdapException
     */
    private EntryFilteringCursor searchFromRoot( SearchOperationContext searchContext )
        throws LdapException
    {
        ExprNode filter = searchContext.getFilter();

        // We are searching from the rootDSE. We have to distinguish three cases :
        // 1) The scope is OBJECT : we have to return the rootDSE entry, filtered
        // 2) The scope is ONELEVEL : we have to return all the Naming Contexts
        boolean isObjectScope = searchContext.getScope() == SearchScope.OBJECT;

        boolean isOnelevelScope = searchContext.getScope() == SearchScope.ONELEVEL;

        // test for (objectClass=*)
        boolean isSearchAll = false;

        // We have to be careful, as we may have a filter which is not a PresenceFilter
        if ( filter instanceof ObjectClassNode )
        {
            isSearchAll = true;
        }

        if ( isObjectScope )
        {
            if ( isSearchAll )
            {
                // if basedn is "", filter is "(objectclass=*)" and scope is object
                // then we have a request for the rootDSE
                return searchRootDse( searchContext );
            }
            else
            {
                // Nothing to return in this case
                return new EntryFilteringCursorImpl( new EmptyCursor<Entry>(), searchContext,
                    directoryService.getSchemaManager() );
            }
        }
        else if ( isOnelevelScope )
        {
            // Loop on all the partitions
            // We will look into all the partitions, thus we create a list of cursors.
            List<EntryFilteringCursor> cursors = new ArrayList<>();

            for ( Partition partition : partitions.values() )
            {
                Dn contextDn = partition.getSuffixDn();
                PartitionTxn partitionTxn = partition.beginReadTransaction();
                HasEntryOperationContext hasEntryContext = new HasEntryOperationContext(
                    searchContext.getSession(), contextDn );
                hasEntryContext.setPartition( partition );
                hasEntryContext.setTransaction( partitionTxn );
                searchContext.setPartition( partition );
                searchContext.setTransaction( partitionTxn );

                // search only if the context entry exists
                if ( partition.hasEntry( hasEntryContext ) )
                {
                    searchContext.setDn( contextDn );
                    searchContext.setScope( SearchScope.OBJECT );
                    cursors.add( partition.search( searchContext ) );
                }
            }

            return new CursorList( cursors, searchContext );
        }
        else
        {
            // This is a SUBLEVEL search. We will do multiple searches and wrap
            // a CursorList into the EntryFilteringCursor
            List<EntryFilteringCursor> cursors = new ArrayList<>();

            for ( Partition partition : partitions.values() )
            {
                PartitionTxn partitionTxn = partition.beginReadTransaction();
                Dn contextDn = partition.getSuffixDn();
                HasEntryOperationContext hasEntryContext = new HasEntryOperationContext(
                    searchContext.getSession(), contextDn );
                hasEntryContext.setPartition( partition );
                hasEntryContext.setTransaction( partitionTxn );
                searchContext.setPartition( partition );
                searchContext.setTransaction( partitionTxn );

                if ( partition.hasEntry( hasEntryContext ) )
                {
                    searchContext.setDn( contextDn );
                    EntryFilteringCursor cursor = partition.search( searchContext );

                    try
                    {
                        if ( cursor.first() )
                        {
                            cursor.beforeFirst();
                            cursors.add( cursor );
                        }
                    }
                    catch ( CursorException e )
                    {
                        // Do nothing
                    }
                }
            }

            // don't feed the above Cursors' list to a BaseEntryFilteringCursor it is skipping the naming context entry of each partition
            if ( cursors.isEmpty() )
            {
                // No candidate, return an emtpy cursor
                return new EntryFilteringCursorImpl( new EmptyCursor<Entry>(), searchContext,
                    directoryService.getSchemaManager() );
            }
            else
            {
                return new CursorList( cursors, searchContext );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        Dn unbindContextDn = unbindContext.getDn();

        if ( !Dn.isNullOrEmpty( unbindContextDn ) )
        {
            Partition partition = getPartition( unbindContext.getDn() );
            partition.unbind( unbindContext );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry getRootDse( GetRootDseOperationContext getRootDseContext )
    {
        return rootDse.clone();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Value getRootDseValue( AttributeType attributeType )
    {
        return rootDse.get( attributeType ).get();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void addContextPartition( Partition partition ) throws LdapException
    {
        // Turn on default indices
        String key = partition.getSuffixDn().getNormName();

        if ( partitions.containsKey( key ) )
        {
            throw new LdapOtherException( I18n.err( I18n.ERR_07004_DUPLICATE_PARTITION_SUFFIX, key ) );
        }

        if ( !partition.isInitialized() )
        {
            partition.initialize();
        }

        synchronized ( partitionLookupTree )
        {
            Dn partitionSuffix = partition.getSuffixDn();

            if ( partitionSuffix == null )
            {
                throw new LdapOtherException( I18n.err( I18n.ERR_07007_PARTITION_HAS_NO_SUFFIX, partition.getId() ) );
            }

            partitions.put( partitionSuffix.getNormName(), partition );
            partitionLookupTree.add( partition.getSuffixDn(), partition );

            Attribute namingContexts = rootDse.get( SchemaConstants.NAMING_CONTEXTS_AT );

            if ( namingContexts == null )
            {
                namingContexts = new DefaultAttribute( schemaManager
                    .lookupAttributeTypeRegistry( SchemaConstants.NAMING_CONTEXTS_AT ), partitionSuffix.getName() );
                rootDse.put( namingContexts );
            }
            else
            {
                namingContexts.add( partitionSuffix.getName() );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void removeContextPartition( String partitionDn )
        throws LdapException
    {
        // Retrieve this partition from the aprtition's table
        Partition partition = partitions.get( partitionDn );

        if ( partition == null )
        {
            String msg = I18n.err( I18n.ERR_07002_NO_PARTITION_WITH_SUFFIX, partitionDn );
            LOG.error( msg );
            throw new LdapNoSuchObjectException( msg );
        }

        String partitionSuffix = partition.getSuffixDn().getNormName();

        // Retrieve the namingContexts from the RootDSE : the partition
        // suffix must be present in those namingContexts
        Attribute namingContexts = rootDse.get( SchemaConstants.NAMING_CONTEXTS_AT );

        if ( namingContexts != null )
        {
            Value foundNC = null;

            for ( Value namingContext : namingContexts )
            {
                String normalizedNC = new Dn( schemaManager, namingContext.getString() ).getNormName();

                if ( partitionSuffix.equals( normalizedNC ) )
                {
                    foundNC = namingContext;
                    break;
                }
            }

            if ( foundNC != null )
            {
                namingContexts.remove( foundNC );
            }
            else
            {
                String msg = I18n.err( I18n.ERR_07003_NO_PARTITION_WITH_SUFFIX_IN_NAMING_CONTEXTS, partitionDn );
                LOG.error( msg );
                throw new LdapNoSuchObjectException( msg );
            }
        }

        // Update the partition tree
        synchronized ( partitionLookupTree )
        {
            partitionLookupTree.remove( partition.getSuffixDn() );
        }

        partitions.remove( partitionDn );

        try
        {
            partition.destroy( partition.beginReadTransaction() );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Partition getPartition( Dn dn ) throws LdapException
    {
        Partition parent;

        if ( dn == null )
        {
            dn = Dn.ROOT_DSE;
        }

        if ( !dn.isSchemaAware() )
        {
            dn = new Dn( schemaManager, dn );
        }

        if ( dn.isRootDse() || dn.getNormName().equals( subschemaSubentryDn.getNormName() ) )
        {
            return new RootPartition( schemaManager );
        }

        synchronized ( partitionLookupTree )
        {
            parent = partitionLookupTree.getElement( dn );
        }

        if ( parent == null )
        {
            throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_07008_CANNOT_FIND_PARTITION, dn ) );
        }
        else
        {
            return parent;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Dn getSuffixDn( Dn dn ) throws LdapException
    {
        Partition partition = getPartition( dn );

        return partition.getSuffixDn();
    }


    /* (non-Javadoc)
     */
    @Override
    public Set<String> listSuffixes() throws LdapException
    {
        return Collections.unmodifiableSet( partitions.keySet() );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void registerSupportedExtensions( Set<String> extensionOids ) throws LdapException
    {
        Attribute supportedExtension = rootDse.get( SchemaConstants.SUPPORTED_EXTENSION_AT );

        if ( supportedExtension == null )
        {
            rootDse.put( SchemaConstants.SUPPORTED_EXTENSION_AT, ( String ) null );
            supportedExtension = rootDse.get( SchemaConstants.SUPPORTED_EXTENSION_AT );
        }

        for ( String extensionOid : extensionOids )
        {
            supportedExtension.add( extensionOid );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void registerSupportedSaslMechanisms( Set<String> supportedSaslMechanisms ) throws LdapException
    {
        Attribute supportedSaslMechanismsAt;

        supportedSaslMechanismsAt = new DefaultAttribute(
            schemaManager.lookupAttributeTypeRegistry( SchemaConstants.SUPPORTED_SASL_MECHANISMS_AT ) );

        for ( String saslMechanism : supportedSaslMechanisms )
        {
            supportedSaslMechanismsAt.add( saslMechanism );
        }

        rootDse.add( supportedSaslMechanismsAt );
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
    private void unregister( Partition partition )
    {
        Attribute namingContexts = rootDse.get( SchemaConstants.NAMING_CONTEXTS_AT );

        if ( namingContexts != null )
        {
            namingContexts.remove( partition.getSuffixDn().getName() );
        }

        partitions.remove( partition.getSuffixDn().getName() );
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
        DefaultAttribute contextCsnAt = new DefaultAttribute( schemaManager
            .lookupAttributeTypeRegistry( SchemaConstants.CONTEXT_CSN_AT ) );
        contextCsnMod.setAttribute( contextCsnAt );

        mods.add( contextCsnMod );

        Modification timeStampMod = new DefaultModification();
        timeStampMod.setOperation( ModificationOperation.REPLACE_ATTRIBUTE );
        DefaultAttribute timeStampAt = new DefaultAttribute( schemaManager
            .lookupAttributeTypeRegistry( SchemaConstants.MODIFY_TIMESTAMP_AT ) );
        timeStampMod.setAttribute( timeStampAt );

        mods.add( timeStampMod );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getContextCsn( PartitionTxn partitionTxn )
    {
        // nexus doesn't contain a contextCSN
        return null;
    }


    @Override
    public void saveContextCsn( PartitionTxn partitionTxn ) throws LdapException
    {
    }


    /**
     * Return the number of children and subordinates for a given entry
     *
     * @param partitionTxn The Partition transaction
     * @param entry The entry for which we want to find the subordinates
     * @return The Subordinate instance that contains the values.
     * @throws LdapException If we had an issue while processing the request
     */
    @Override
    public Subordinates getSubordinates( PartitionTxn partitionTxn, Entry entry ) throws LdapException
    {
        return new Subordinates();
    }


    @Override
    public PartitionReadTxn beginReadTransaction()
    {
        return new PartitionReadTxn();
    }


    @Override
    public PartitionWriteTxn beginWriteTransaction()
    {
        return new PartitionWriteTxn();
    }
}
