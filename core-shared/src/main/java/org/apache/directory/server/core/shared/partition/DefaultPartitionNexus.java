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
import org.apache.directory.api.ldap.model.schema.Normalizer;
import org.apache.directory.api.ldap.model.schema.UsageEnum;
import org.apache.directory.api.ldap.util.tree.DnNode;
import org.apache.directory.api.util.DateUtils;
import org.apache.directory.api.util.exception.MultiException;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InterceptorEnum;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursorImpl;
import org.apache.directory.server.core.api.filtering.CursorList;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
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
    private static final String ID = "NEXUS";

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** the vendorName string proudly set to: Apache Software Foundation*/
    private static final String ASF = "Apache Software Foundation";

    /** the read only rootDSE attributes */
    private final Entry rootDse;

    /** The DirectoryService instance */
    private DirectoryService directoryService;

    /** the partitions keyed by normalized suffix strings */
    private Map<String, Partition> partitions = new HashMap<String, Partition>();

    /** A structure to hold all the partitions */
    private DnNode<Partition> partitionLookupTree = new DnNode<Partition>();

    /** the system partition */
    //private Partition system;

    /** A reference to the EntryCSN attributeType */
    private static AttributeType ENTRY_CSN_AT;

    /** A reference to the ObjectClass attributeType */
    private static AttributeType OBJECT_CLASS_AT;

    private final List<Modification> mods = new ArrayList<Modification>( 2 );

    private String lastSyncedCtxCsn = null;

    /** The cn=schema Dn */
    private Dn subschemSubentryDn;


    /**
     * Creates the root nexus singleton of the entire system.  The root DSE has
     * several attributes that are injected into it besides those that may
     * already exist.  As partitions are added to the system more namingContexts
     * attributes are added to the rootDSE.
     *
     * @see <a href="http://www.faqs.org/rfcs/rfc3045.html">Vendor Information</a>
     * @param rootDse the root entry for the DSA
     * @throws javax.naming.Exception on failure to initialize
     */
    public DefaultPartitionNexus( Entry rootDse ) throws Exception
    {
        id = ID;
        suffixDn = null;

        // setup that root DSE
        this.rootDse = rootDse;

        // Add the basic informations
        rootDse.put( SchemaConstants.SUBSCHEMA_SUBENTRY_AT, ServerDNConstants.CN_SCHEMA_DN );
        rootDse.put( SchemaConstants.SUPPORTED_LDAP_VERSION_AT, "3" );
        rootDse.put( SchemaConstants.SUPPORTED_FEATURES_AT, SchemaConstants.FEATURE_ALL_OPERATIONAL_ATTRIBUTES );
        rootDse.put( SchemaConstants.SUPPORTED_EXTENSION_AT, NoticeOfDisconnect.EXTENSION_OID );

        // Add the objectClasses
        rootDse.put( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, SchemaConstants.EXTENSIBLE_OBJECT_OC );

        // Add the 'vendor' name and version infos
        rootDse.put( SchemaConstants.VENDOR_NAME_AT, ASF );

        Properties props = new Properties();

        try
        {
            props.load( getClass().getResourceAsStream( "version.properties" ) );
        }
        catch ( IOException e )
        {
            LOG.error( I18n.err( I18n.ERR_33 ) );
        }

        rootDse.put( SchemaConstants.VENDOR_VERSION_AT, props.getProperty( "apacheds.version", "UNKNOWN" ) );

        // The rootDSE uuid has been randomly created
        rootDse.put( SchemaConstants.ENTRY_UUID_AT, "f290425c-8272-4e62-8a67-92b06f38dbf5" );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#initialize()
     */
    protected void doInit() throws Exception
    {
        // NOTE: We ignore ContextPartitionConfiguration parameter here.
        if ( !initialized )
        {
            // Add the supported controls
            Iterator<String> ctrlOidItr = directoryService.getLdapCodecService().registeredControls();

            while ( ctrlOidItr.hasNext() )
            {
                rootDse.add( SchemaConstants.SUPPORTED_CONTROL_AT, ctrlOidItr.next() );
            }

            schemaManager = directoryService.getSchemaManager();
            ENTRY_CSN_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_CSN_AT );
            OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );

            // Initialize and normalize the localy used DNs
            Dn adminDn = directoryService.getDnFactory().create( ServerDNConstants.ADMIN_SYSTEM_DN );
            adminDn.apply( schemaManager );

            Value<?> attr = rootDse.get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ).get();
            subschemSubentryDn = directoryService.getDnFactory().create( attr.getString() );

            List<Partition> initializedPartitions = new ArrayList<Partition>();

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
                removeContextPartition( directoryService.getDnFactory().create( suffix ) );
            }
            catch ( Exception e )
            {
                LOG.warn( "Failed to destroy a partition: " + suffix, e );
            }
        }

        initialized = false;
    }


    /**
     * {@inheritDoc}
     */
    public void setId( String id )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_264 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void setSuffixDn( Dn suffix )
    {
        throw new UnsupportedOperationException();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.PartitionNexus#sync()
     */
    public void sync() throws Exception
    {
        MultiException error = null;

        for ( Partition partition : this.partitions.values() )
        {
            try
            {
                partition.saveContextCsn();
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
    /**
     * {@inheritDoc}
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        Partition partition = getPartition( addContext.getDn() );
        partition.add( addContext );
    }


    /**
     * {@inheritDoc}
     */
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


    /**
     * {@inheritDoc}
     */
    public Entry delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        Partition partition = getPartition( deleteContext.getDn() );
        Entry deletedEntry = partition.delete( deleteContext );

        return deletedEntry;
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        Dn dn = hasEntryContext.getDn();

        if ( IS_DEBUG )
        {
            LOG.debug( "Check if Dn '" + dn + "' exists." );
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
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        Dn dn = lookupContext.getDn();

        if ( dn.equals( subschemSubentryDn ) )
        {
            return new ClonedServerEntry( rootDse.clone() );
        }

        // This is for the case we do a lookup on the rootDSE
        if ( dn.isRootDse() )
        {
            Entry retval = new ClonedServerEntry( rootDse );

            return retval;
        }

        Partition partition = getPartition( dn );
        Entry entry = partition.lookup( lookupContext );

        if ( entry == null )
        {
            LdapNoSuchObjectException e = new LdapNoSuchObjectException( "Attempt to lookup non-existant entry: "
                + dn.getName() );

            throw e;
        }

        return entry;
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        // Special case : if we don't have any modification to apply, just return
        if ( modifyContext.getModItems().size() == 0 )
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
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        // Get the current partition
        Partition partition = getPartition( moveContext.getDn() );

        partition.move( moveContext );
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        Partition partition = getPartition( moveAndRenameContext.getDn() );
        partition.moveAndRename( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
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
        if ( ( ids == null ) || ( ids.size() == 0 ) )
        {
            Entry rootDse = getRootDse( null );
            return new EntryFilteringCursorImpl( new SingletonCursor<Entry>( rootDse ), searchContext,
                directoryService.getSchemaManager() );
        }

        // -----------------------------------------------------------
        // Collect all the real attributes besides 1.1, +, and * and
        // note if we've seen these special attributes as well.
        // -----------------------------------------------------------

        Set<String> realIds = new HashSet<String>();
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
            Entry rootDse = getRootDse( null );
            return new EntryFilteringCursorImpl( new SingletonCursor<Entry>( rootDse ), searchContext,
                directoryService.getSchemaManager() );
        }

        Entry serverEntry = new DefaultEntry( schemaManager, Dn.ROOT_DSE );

        Entry rootDse = getRootDse( new GetRootDseOperationContext( searchContext.getSession() ) );

        for ( Attribute attribute : rootDse )
        {
            AttributeType type = schemaManager.lookupAttributeTypeRegistry( attribute.getId() );

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

        return new EntryFilteringCursorImpl( new SingletonCursor<Entry>( serverEntry ), searchContext,
            directoryService.getSchemaManager() );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        Dn base = searchContext.getDn();

        // TODO since we're handling the *, and + in the EntryFilteringCursor
        // we may not need this code: we need see if this is actually the
        // case and remove this code.
        if ( base.size() == 0 )
        {
            return searchFromRoot( searchContext );
        }

        // Not sure we need this code...
        base.apply( schemaManager );

        // Normal case : do a search on the specific partition
        Partition backend = getPartition( base );

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
            List<EntryFilteringCursor> cursors = new ArrayList<EntryFilteringCursor>();

            for ( Partition partition : partitions.values() )
            {
                Dn contextDn = partition.getSuffixDn();
                HasEntryOperationContext hasEntryContext = new HasEntryOperationContext(
                    searchContext.getSession(), contextDn );

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
            List<EntryFilteringCursor> cursors = new ArrayList<EntryFilteringCursor>();

            for ( Partition partition : partitions.values() )
            {
                Dn contextDn = partition.getSuffixDn();
                HasEntryOperationContext hasEntryContext = new HasEntryOperationContext(
                    searchContext.getSession(), contextDn );

                if ( partition.hasEntry( hasEntryContext ) )
                {
                    searchContext.setDn( contextDn );
                    EntryFilteringCursor cursor = partition.search( searchContext );
                    cursors.add( cursor );
                }
            }

            // don't feed the above Cursors' list to a BaseEntryFilteringCursor it is skipping the naming context entry of each partition
            return new CursorList( cursors, searchContext );
        }
    }


    /**
     * {@inheritDoc}
     */
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
    public Entry getRootDse( GetRootDseOperationContext getRootDseContext )
    {
        return rootDse.clone();
    }


    /**
     * {@inheritDoc}
     */
    public Value<?> getRootDseValue( AttributeType attributeType )
    {
        Value<?> value = rootDse.get( attributeType ).get();

        return value.clone();
    }


    /**
     * {@inheritDoc}
     */
    public synchronized void addContextPartition( Partition partition ) throws LdapException
    {
        // Turn on default indices
        String key = partition.getSuffixDn().getNormName();

        if ( partitions.containsKey( key ) )
        {
            throw new LdapOtherException( I18n.err( I18n.ERR_263, key ) );
        }

        if ( !partition.isInitialized() )
        {
            partition.setCacheService( directoryService.getCacheService() );
            partition.initialize();
        }

        synchronized ( partitionLookupTree )
        {
            Dn partitionSuffix = partition.getSuffixDn();

            if ( partitionSuffix == null )
            {
                throw new LdapOtherException( I18n.err( I18n.ERR_267, partition.getId() ) );
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
    public synchronized void removeContextPartition( Dn partitionDn )
        throws LdapException
    {
        // Get the Partition name. It's a Dn.
        String key = partitionDn.getNormName();

        // Retrieve this partition from the aprtition's table
        Partition partition = partitions.get( key );

        if ( partition == null )
        {
            String msg = I18n.err( I18n.ERR_34, key );
            LOG.error( msg );
            throw new LdapNoSuchObjectException( msg );
        }

        String partitionSuffix = partition.getSuffixDn().getName();

        // Retrieve the namingContexts from the RootDSE : the partition
        // suffix must be present in those namingContexts
        Attribute namingContexts = rootDse.get( SchemaConstants.NAMING_CONTEXTS_AT );

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
        synchronized ( partitionLookupTree )
        {
            partitionLookupTree.remove( partition.getSuffixDn() );
        }

        partitions.remove( key );

        try
        {
            partition.destroy();
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    public Partition getPartition( Dn dn ) throws LdapException
    {
        Partition parent = null;

        if ( !dn.isSchemaAware() )
        {
           dn.apply( schemaManager ); 
        }
        
        synchronized ( partitionLookupTree )
        {
            parent = partitionLookupTree.getElement( dn );
        }

        if ( parent == null )
        {
            throw new LdapNoSuchObjectException( I18n.err( I18n.ERR_268, dn ) );
        }
        else
        {
            return parent;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Dn getSuffixDn( Dn dn ) throws LdapException
    {
        Partition partition = getPartition( dn );

        return partition.getSuffixDn();
    }


    /* (non-Javadoc)
     */
    public Set<String> listSuffixes() throws LdapException
    {
        return Collections.unmodifiableSet( partitions.keySet() );
    }


    /**
     * {@inheritDoc}
     */
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
    public void registerSupportedSaslMechanisms( Set<String> supportedSaslMechanisms ) throws LdapException
    {
        Attribute supportedSaslMechanismsAt = null;

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
    private void unregister( Partition partition ) throws Exception
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


    @Override
    public String getContextCsn()
    {
        // nexus doesn't contain a contextCSN
        return null;
    }


    @Override
    public void saveContextCsn() throws Exception
    {
    }
}
