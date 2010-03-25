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

import javax.naming.NamingException;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.entry.ClonedServerEntry;
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
import org.apache.directory.shared.ldap.codec.controls.CascadeControl;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultServerAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.ServerModification;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SchemaUtils;
import org.apache.directory.shared.ldap.util.DateUtils;
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
 * @version $Rev$, $Date$
 */
public final class SchemaPartition extends AbstractPartition
{
    /** the logger */
    private static final Logger LOG = LoggerFactory.getLogger( SchemaPartition.class );

    /** the fixed id: 'schema' */
    private static final String ID = "schema";

    /** the wrapped Partition */
    private Partition wrapped = new NullPartition();

    /** schema manager */
    private SchemaManager schemaManager;

    /** registry synchronizer adaptor */
    private RegistrySynchronizerAdaptor synchronizer;

    /** A static DN for the ou=schemaModifications entry */
    private static DN schemaModificationDN;


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
    public final String getId()
    {
        return ID;
    }


    /**
     * Has no affect: the id is fixed at {@link SchemaPartition#ID}: 'schema'.
     * A warning is logged.
     */
    public final void setId( String id )
    {
        LOG.warn( "This partition's ID is fixed: {}", ID );
    }


    /**
     * Always returns {@link ServerDNConstants#OU_SCHEMA_DN_NORMALIZED}: '2.5.4.11=schema'.
     */
    public final DN getSuffixDn()
    {
        return wrapped.getSuffixDn();
    }


    /**
     * Always returns {@link ServerDNConstants#OU_SCHEMA_DN}: 'ou=schema'.
     */
    public final String getSuffix()
    {
        return SchemaConstants.OU_SCHEMA;
    }


    /**
     * Has no affect: just logs a warning.
     */
    public final void setSuffix( String suffix )
    {
        LOG.warn( "This partition's suffix is fixed: {}", SchemaConstants.OU_SCHEMA );
    }


    // -----------------------------------------------------------------------
    // Partition Interface Method Overrides
    // -----------------------------------------------------------------------

    @Override
    public void sync() throws Exception
    {
        wrapped.sync();
    }


    @Override
    protected void doInit() throws Exception
    {
        // -----------------------------------------------------------------------
        // Load apachemeta schema from within the ldap-schema Jar with all the
        // schema it depends on.  This is a minimal mandatory set of schemas.
        // -----------------------------------------------------------------------
        //SerializableComparator.setSchemaManager( schemaManager );

        wrapped.setId( ID );
        wrapped.setSuffix( SchemaConstants.OU_SCHEMA );
        wrapped.getSuffixDn().normalize( schemaManager.getNormalizerMapping() );
        wrapped.setSchemaManager( schemaManager );

        try
        {
            wrapped.initialize();

            PartitionSchemaLoader partitionLoader = new PartitionSchemaLoader( wrapped, schemaManager );
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

        schemaModificationDN = new DN( ServerDNConstants.SCHEMA_MODIFICATIONS_DN );
        schemaModificationDN.normalize( schemaManager.getNormalizerMapping() );
    }


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
    public void add( AddOperationContext opContext ) throws Exception
    {
        // At this point, the added SchemaObject does not exist in the partition
        // We have to check if it's enabled and then inject it into the registries
        // but only if it does not break the server.
        synchronizer.add( opContext );

        // Now, write the newly added SchemaObject into the schemaPartition
        try
        {
            wrapped.add( opContext );
        }
        catch ( Exception e )
        {
            // If something went wrong, we have to unregister the schemaObject
            // from the registries
            // TODO : deregister the newly added element.
            throw e;
        }

        updateSchemaModificationAttributes( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#bind(org.apache.directory.server.core.interceptor.context.BindOperationContext)
     */
    public void bind( BindOperationContext opContext ) throws Exception
    {
        wrapped.bind( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#delete(org.apache.directory.server.core.interceptor.context.DeleteOperationContext)
     */
    public void delete( DeleteOperationContext opContext ) throws Exception
    {
        boolean cascade = opContext.hasRequestControl( CascadeControl.CONTROL_OID );

        // The SchemaObject always exist when we reach this method.
        synchronizer.delete( opContext, cascade );

        try
        {
            wrapped.delete( opContext );
        }
        catch ( Exception e )
        {
            // TODO : If something went wrong, what should we do here ?
            throw e;
        }

        updateSchemaModificationAttributes( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#list(org.apache.directory.server.core.interceptor.context.ListOperationContext)
     */
    public EntryFilteringCursor list( ListOperationContext opContext ) throws Exception
    {
        return wrapped.list( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEntry( EntryOperationContext entryContext ) throws Exception
    {
        return wrapped.hasEntry( entryContext );
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext opContext ) throws Exception
    {
        ServerEntry entry = opContext.getEntry();

        if ( entry == null )
        {
            LookupOperationContext lookupCtx = new LookupOperationContext( opContext.getSession(), opContext.getDn() );
            entry = wrapped.lookup( lookupCtx );
        }

        ServerEntry targetEntry = ( ServerEntry ) SchemaUtils.getTargetEntry( opContext.getModItems(), entry );

        boolean cascade = opContext.hasRequestControl( CascadeControl.CONTROL_OID );

        boolean hasModification = synchronizer.modify( opContext, targetEntry, cascade );

        if ( hasModification )
        {
            wrapped.modify( opContext );
        }

        if ( !opContext.getDn().equals( schemaModificationDN ) )
        {
            updateSchemaModificationAttributes( opContext );
        }
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#move(org.apache.directory.server.core.interceptor.context.MoveOperationContext)
     */
    public void move( MoveOperationContext opContext ) throws Exception
    {
        boolean cascade = opContext.hasRequestControl( CascadeControl.CONTROL_OID );
        ClonedServerEntry entry = opContext.lookup( opContext.getDn(), ByPassConstants.LOOKUP_BYPASS );
        synchronizer.move( opContext, entry, cascade );
        wrapped.move( opContext );
        updateSchemaModificationAttributes( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#moveAndRename(org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext)
     */
    public void moveAndRename( MoveAndRenameOperationContext opContext ) throws Exception
    {
        boolean cascade = opContext.hasRequestControl( CascadeControl.CONTROL_OID );
        ClonedServerEntry entry = opContext.lookup( opContext.getDn(), ByPassConstants.LOOKUP_BYPASS );
        synchronizer.moveAndRename( opContext, entry, cascade );
        wrapped.moveAndRename( opContext );
        updateSchemaModificationAttributes( opContext );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( RenameOperationContext opContext ) throws Exception
    {
        boolean cascade = opContext.hasRequestControl( CascadeControl.CONTROL_OID );

        // First update the registries
        synchronizer.rename( opContext, cascade );

        // Update the schema partition
        wrapped.rename( opContext );

        // Update the SSSE operational attributes
        updateSchemaModificationAttributes( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#search(org.apache.directory.server.core.interceptor.context.SearchOperationContext)
     */
    public EntryFilteringCursor search( SearchOperationContext opContext ) throws Exception
    {
        return wrapped.search( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#unbind(org.apache.directory.server.core.interceptor.context.UnbindOperationContext)
     */
    public void unbind( UnbindOperationContext opContext ) throws Exception
    {
        wrapped.unbind( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#lookup(org.apache.directory.server.core.interceptor.context.LookupOperationContext)
     */
    public ClonedServerEntry lookup( LookupOperationContext lookupContext ) throws Exception
    {
        return wrapped.lookup( lookupContext );
    }


    /**
     * Updates the schemaModifiersName and schemaModifyTimestamp attributes of
     * the schemaModificationAttributes entry for the global schema at 
     * ou=schema,cn=schemaModifications.  This entry is hardcoded at that 
     * position for now.
     * 
     * The current time is used to set the timestamp and the DN of current user
     * is set for the modifiersName.
     * 
     * @throws NamingException if the update fails
     */
    private void updateSchemaModificationAttributes( OperationContext opContext ) throws Exception
    {
        String modifiersName = opContext.getSession().getEffectivePrincipal().getName();
        String modifyTimestamp = DateUtils.getGeneralizedTime();

        List<Modification> mods = new ArrayList<Modification>( 2 );

        mods.add( new ServerModification( ModificationOperation.REPLACE_ATTRIBUTE, new DefaultServerAttribute(
            ApacheSchemaConstants.SCHEMA_MODIFY_TIMESTAMP_AT, schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.SCHEMA_MODIFY_TIMESTAMP_AT ), modifyTimestamp ) ) );

        mods.add( new ServerModification( ModificationOperation.REPLACE_ATTRIBUTE, new DefaultServerAttribute(
            ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT, schemaManager
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.SCHEMA_MODIFIERS_NAME_AT ), modifiersName ) ) );

        opContext.modify( schemaModificationDN, mods, ByPassConstants.SCHEMA_MODIFICATION_ATTRIBUTES_UPDATE_BYPASS );
    }


    /**
     * @param schemaManager the SchemaManager to set
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }


    /**
     * @return The schemaManager
     */
    public SchemaManager getSchemaManager()
    {
        return schemaManager;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "Partition : " + ID;
    }
}
