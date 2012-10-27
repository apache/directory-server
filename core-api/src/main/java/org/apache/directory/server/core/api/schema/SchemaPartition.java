/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.api.schema;


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.api.partition.AbstractPartition;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.registries.synchronizers.RegistrySynchronizerAdaptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.message.controls.Cascade;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.SchemaUtils;
import org.apache.directory.shared.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A special partition designed to contain the portion of the DIT where schema
 * information for the server is stored.
 * 
 * In an effort to make sure that all Partition implementations are equal
 * citizens to ApacheDS we want to be able to swap in and out any kind of
 * Partition to store schema.  This also has the added advantage of making
 * sure the core, and hence the server is not dependent on any specific
 * partition, which reduces coupling in the server's modules.
 * 
 * The SchemaPartition achieves this by not really being a backing store
 * itself for the schema entries.  It instead delegates to another Partition
 * via containment.  It delegates all calls to this contained Partition. While
 * doing so it also manages certain things:
 * 
 * <ol>
 *   <li>Checks that schema changes are valid.</li>
 *   <li>Updates the schema Registries on valid schema changes making sure
 *       the schema on disk is in sync with the schema in memory.
 *   </li>
 *   <li>Will eventually manage transaction based changes to schema where
 *       between some sequence of operations the schema may be inconsistent.
 *   </li>
 *   <li>Delegates read/write operations to contained Partition.</li>
 *   <li>
 *       Responsible for initializing schema for the entire server.  ApacheDS
 *       cannot start up other partitions until this Partition is started
 *       without having access to the Registries.  This Partition supplies the
 *       Registries on initialization for the server.  That's one of it's core
 *       responsibilities.
 *   </li>
 * 
 * So by containing another Partition, we abstract the storage mechanism away
 * from the management responsibilities while decoupling the server from a
 * specific partition implementation and removing complexity in the Schema
 * interceptor service which before managed synchronization.
 * </ol>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class SchemaPartition extends AbstractPartition
{
    /** the logger */
    private static final Logger LOG = LoggerFactory.getLogger( SchemaPartition.class );

    /** the fixed id: 'schema' */
    private static final String SCHEMA_ID = "schema";

    /** the wrapped Partition */
    private Partition wrapped;

    /** registry synchronizer adaptor */
    private RegistrySynchronizerAdaptor synchronizer;

    /** A static Dn for the ou=schemaModifications entry */
    private static Dn SCHEMA_MODIFICATION_DN;

    /** A static Dn for the ou=schema partition */
    private static Dn SCHEMA_DN;

    /** The ObjectClass AttributeType */
    private static AttributeType OBJECT_CLASS_AT;


    public SchemaPartition( SchemaManager schemaManager )
    {
        try
        {
            SCHEMA_DN = new Dn( schemaManager, SchemaConstants.OU_SCHEMA );
        }
        catch ( LdapInvalidDnException lide )
        {
            // Nothing to do : this is a valid DN anyways
        }

        id = SCHEMA_ID;
        suffixDn = SCHEMA_DN;
        this.schemaManager = schemaManager;
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT_OID );
    }


    /**
     * Sets the wrapped {@link Partition} which must be supplied or
     * {@link Partition#initialize()} will fail with a NullPointerException.
     *
     * @param wrapped the Partition being wrapped
     */
    public void setWrappedPartition( Partition wrapped )
    {
        if ( this.isInitialized() )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_429 ) );
        }

        this.wrapped = wrapped;
    }


    /**
     * Gets the {@link Partition} being wrapped.
     *
     * @return the wrapped Partition
     */
    public Partition getWrappedPartition()
    {
        return wrapped;
    }


    /**
     * Has no affect: the id is fixed at {@link SchemaPartition#SCHEMA_ID}: 'schema'.
     * A warning is logged.
     */
    public void setId( String id )
    {
        LOG.warn( "This partition's ID is fixed: {}", SCHEMA_ID );
    }


    // -----------------------------------------------------------------------
    // Partition Interface Method Overrides
    // -----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void sync() throws Exception
    {
        wrapped.sync();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInit() throws Exception
    {
        if ( !initialized )
        {
            // -----------------------------------------------------------------------
            // Load apachemeta schema from within the ldap-schema Jar with all the
            // schema it depends on.  This is a minimal mandatory set of schemas.
            // -----------------------------------------------------------------------
            wrapped.setId( SCHEMA_ID );
            wrapped.setSuffixDn( SCHEMA_DN );
            wrapped.setSchemaManager( schemaManager );

            try
            {
                wrapped.initialize();

                synchronizer = new RegistrySynchronizerAdaptor( schemaManager );
            }
            catch ( Exception e )
            {
                LOG.error( I18n.err( I18n.ERR_90 ), e );
                throw new RuntimeException( e );
            }

            SCHEMA_MODIFICATION_DN = new Dn( schemaManager, SchemaConstants.SCHEMA_MODIFICATIONS_DN );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDestroy()
    {
        try
        {
            wrapped.destroy();
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_91 ), e );
            throw new RuntimeException( e );
        }

        initialized = false;
    }


    // -----------------------------------------------------------------------
    // Partition Interface Methods
    // -----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        // At this point, the added SchemaObject does not exist in the partition
        // We have to check if it's enabled and then inject it into the registries
        // but only if it does not break the server.
        synchronizer.add( addContext );

        // Now, write the newly added SchemaObject into the schemaPartition
        try
        {
            wrapped.add( addContext );
        }
        catch ( LdapException e )
        {
            // If something went wrong, we have to unregister the schemaObject
            // from the registries
            // TODO : deregister the newly added element.
            throw e;
        }

        updateSchemaModificationAttributes( addContext );
    }


    /**
     * {@inheritDoc}
     */
    public final int getChildCount( DeleteOperationContext deleteContext ) throws LdapException
    {
        try
        {
            Dn dn = deleteContext.getDn();
            SearchRequest searchRequest = new SearchRequestImpl();
            searchRequest.setBase( dn );
            ExprNode node = new PresenceNode( OBJECT_CLASS_AT );
            searchRequest.setFilter( node );
            searchRequest.setTypesOnly( true );
            searchRequest.setScope( SearchScope.ONELEVEL );

            SearchOperationContext searchContext = new SearchOperationContext( deleteContext.getSession(),
                searchRequest );

            EntryFilteringCursor cursor = wrapped.search( searchContext );

            cursor.beforeFirst();
            int nbEntry = 0;

            while ( cursor.next() && ( nbEntry < 2 ) )
            {
                nbEntry++;
            }
            
            cursor.close();

            return nbEntry;
        }
        catch ( Exception e )
        {
            LOG.warn( "Failed to get the children of entry {}", deleteContext.getDn() );
            return 0;
        }
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        boolean cascade = deleteContext.hasRequestControl( Cascade.OID );

        // We have to check if the entry we want to delete has children, or not
        int nbChild = getChildCount( deleteContext );

        if ( nbChild > 1 )
        {
            throw new LdapUnwillingToPerformException( "There are children under the entry " + deleteContext.getDn() );
        }

        // The SchemaObject always exist when we reach this method.
        synchronizer.delete( deleteContext, cascade );

        try
        {
            wrapped.delete( deleteContext );
        }
        catch ( LdapException e )
        {
            // TODO : If something went wrong, what should we do here ?
            throw e;
        }

        updateSchemaModificationAttributes( deleteContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor list( ListOperationContext listContext ) throws LdapException
    {
        return wrapped.list( listContext );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException
    {
        return wrapped.hasEntry( hasEntryContext );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        Entry entry = modifyContext.getEntry();

        if ( entry == null )
        {
            LookupOperationContext lookupCtx = new LookupOperationContext( modifyContext.getSession(),
                modifyContext.getDn() );
            entry = wrapped.lookup( lookupCtx );
            modifyContext.setEntry( entry );
        }

        Entry targetEntry = SchemaUtils.getTargetEntry( modifyContext.getModItems(), entry );

        boolean cascade = modifyContext.hasRequestControl( Cascade.OID );

        boolean hasModification = synchronizer.modify( modifyContext, targetEntry, cascade );

        if ( hasModification )
        {
            wrapped.modify( modifyContext );
        }

        if ( !modifyContext.getDn().equals( SCHEMA_MODIFICATION_DN ) )
        {
            updateSchemaModificationAttributes( modifyContext );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        boolean cascade = moveContext.hasRequestControl( Cascade.OID );

        CoreSession session = moveContext.getSession();
        LookupOperationContext lookupContext = new LookupOperationContext( session, moveContext.getDn(),
            SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        Entry entry = session.getDirectoryService().getPartitionNexus().lookup( lookupContext );
        synchronizer.move( moveContext, entry, cascade );
        wrapped.move( moveContext );
        updateSchemaModificationAttributes( moveContext );
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        boolean cascade = moveAndRenameContext.hasRequestControl( Cascade.OID );
        CoreSession session = moveAndRenameContext.getSession();
        LookupOperationContext lookupContext = new LookupOperationContext( session, moveAndRenameContext.getDn(),
            SchemaConstants.ALL_ATTRIBUTES_ARRAY );
        Entry entry = session.getDirectoryService().getPartitionNexus().lookup( lookupContext );
        synchronizer.moveAndRename( moveAndRenameContext, entry, cascade );
        wrapped.moveAndRename( moveAndRenameContext );
        updateSchemaModificationAttributes( moveAndRenameContext );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        boolean cascade = renameContext.hasRequestControl( Cascade.OID );

        // First update the registries
        synchronizer.rename( renameContext, cascade );

        // Update the schema partition
        wrapped.rename( renameContext );

        // Update the SSSE operational attributes
        updateSchemaModificationAttributes( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    public EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException
    {
        return wrapped.search( searchContext );
    }


    /**
     * {@inheritDoc}
     */
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        wrapped.unbind( unbindContext );
    }


    /**
     * {@inheritDoc}
     */
    public Entry lookup( LookupOperationContext lookupContext ) throws LdapException
    {
        return wrapped.lookup( lookupContext );
    }


    /**
     * Updates the schemaModifiersName and schemaModifyTimestamp attributes of
     * the schemaModificationAttributes entry for the global schema at
     * ou=schemaModifications,ou=schema.  This entry is hardcoded at that
     * position for now.
     * 
     * The current time is used to set the timestamp and the Dn of current user
     * is set for the modifiersName.
     * 
     * @throws LdapException if the update fails
     */
    private void updateSchemaModificationAttributes( OperationContext opContext ) throws LdapException
    {
        String modifiersName = opContext.getSession().getEffectivePrincipal().getName();
        String modifyTimestamp = DateUtils.getGeneralizedTime();

        List<Modification> mods = new ArrayList<Modification>( 2 );

        mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, new DefaultAttribute(
            ApacheSchemaConstants.SCHEMA_MODIFY_TIMESTAMP_AT, schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.SCHEMA_MODIFY_TIMESTAMP_AT ), modifyTimestamp ) ) );

        mods.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, new DefaultAttribute(
            ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT, schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT ), modifiersName ) ) );

        // operation reached this level means all the necessary ACI and other checks should
        // have been done, so we can perform the below modification directly on the partition nexus
        // without using a a bypass list
        CoreSession session = opContext.getSession();
        ModifyOperationContext modifyContext = new ModifyOperationContext( session, SCHEMA_MODIFICATION_DN, mods );
        session.getDirectoryService().getPartitionNexus().modify( modifyContext );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "Partition : " + SCHEMA_ID;
    }
}
