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
package org.apache.directory.server.core.schema;


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.AbstractPartition;
import org.apache.directory.server.core.partition.ByPassConstants;
import org.apache.directory.server.core.partition.NullPartition;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.schema.registries.synchronizers.RegistrySynchronizerAdaptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.controls.Cascade;
import org.apache.directory.shared.ldap.model.name.Dn;
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
    private static final String ID = "schema";

    /** the wrapped Partition */
    private Partition wrapped = new NullPartition();

    /** registry synchronizer adaptor */
    private RegistrySynchronizerAdaptor synchronizer;

    /** A static Dn for the ou=schemaModifications entry */
    private static Dn schemaModificationDn;

    /** A static Dn for the ou=schema partition */
    private static Dn schemaDn;


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
     * Get's the ID which is fixed: 'schema'.
     */
    public String getId()
    {
        return ID;
    }


    /**
     * Has no affect: the id is fixed at {@link SchemaPartition#ID}: 'schema'.
     * A warning is logged.
     */
    public void setId( String id )
    {
        LOG.warn( "This partition's ID is fixed: {}", ID );
    }


    /**
     * Always returns {@link ServerDNConstants#OU_SCHEMA_DN_NORMALIZED}: '2.5.4.11=schema'.
     */
    public Dn getSuffix()
    {
        return wrapped.getSuffix();
    }


    /**
     * Has no affect: just logs a warning.
     */
    public void setSuffix( Dn suffix )
    {
        LOG.warn( "This partition's suffix is fixed: {}", SchemaConstants.OU_SCHEMA );
    }


    // -----------------------------------------------------------------------
    // Partition Interface Method Overrides
    // -----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
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
        // -----------------------------------------------------------------------
        // Load apachemeta schema from within the ldap-schema Jar with all the
        // schema it depends on.  This is a minimal mandatory set of schemas.
        // -----------------------------------------------------------------------
        schemaDn = new Dn( schemaManager, SchemaConstants.OU_SCHEMA );
        
        wrapped.setId( ID );
        wrapped.setSuffix( schemaDn );
        wrapped.setSchemaManager( schemaManager );

        try
        {
            wrapped.initialize();

            synchronizer = new RegistrySynchronizerAdaptor( schemaManager );

            if ( wrapped instanceof NullPartition )
            {
                LOG.warn( "BYPASSING CRITICAL SCHEMA PROCESSING CODE DURING HEAVY DEV.  "
                    + "PLEASE REMOVE THIS CONDITION BY USING A VALID SCHEMA PARTITION!!!" );
                return;
            }
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_90 ), e );
            throw new RuntimeException( e );
        }

        schemaModificationDn = new Dn( schemaManager, SchemaConstants.SCHEMA_MODIFICATIONS_DN );
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
    public void bind( BindOperationContext bindContext ) throws LdapException
    {
        wrapped.bind( bindContext );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
        boolean cascade = deleteContext.hasRequestControl( Cascade.OID );

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
    public boolean hasEntry( EntryOperationContext hasEntryContext ) throws LdapException
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
            LookupOperationContext lookupCtx = new LookupOperationContext( modifyContext.getSession(), modifyContext.getDn() );
            entry = wrapped.lookup( lookupCtx );
        }

        Entry targetEntry = ( Entry ) SchemaUtils.getTargetEntry( modifyContext.getModItems(), entry );

        boolean cascade = modifyContext.hasRequestControl( Cascade.OID );

        boolean hasModification = synchronizer.modify( modifyContext, targetEntry, cascade );

        if ( hasModification )
        {
            wrapped.modify( modifyContext );
        }

        if ( !modifyContext.getDn().equals(schemaModificationDn) )
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
        Entry entry = moveContext.lookup( moveContext.getDn(), ByPassConstants.LOOKUP_BYPASS, SchemaConstants.ALL_ATTRIBUTES_ARRAY );
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
        Entry entry = moveAndRenameContext.lookup( moveAndRenameContext.getDn(), ByPassConstants.LOOKUP_BYPASS, SchemaConstants.ALL_ATTRIBUTES_ARRAY );
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
     * ou=schema,cn=schemaModifications.  This entry is hardcoded at that 
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

        opContext.modify(schemaModificationDn, mods, ByPassConstants.SCHEMA_MODIFICATION_ATTRIBUTES_UPDATE_BYPASS );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "Partition : " + ID;
    }
}
