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


import java.util.HashSet; 
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.AbstractPartition;
import org.apache.directory.server.core.partition.ByPassConstants;
import org.apache.directory.server.core.partition.NullPartition;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.message.control.CascadeControl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaUtils;
import org.apache.directory.shared.ldap.schema.comparators.SerializableComparator;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.shared.schema.loader.ldif.JarLdifSchemaLoader;
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
    
    /** the registries managed by this SchemaPartition */
    private Registries registries = new Registries();
    
    /** the schema loader used: gets swapped out right after init */
    private SchemaLoader loader = new JarLdifSchemaLoader();

    private SchemaOperationControl schemaManager;
    
    
    /**
     * Creates a new instance of SchemaPartition.
     */
    public SchemaPartition() throws Exception
    {
        // -----------------------------------------------------------------------
        // Load apachemeta schema from within the ldap-schema Jar with all the
        // schema it depends on.  This is a minimal mandatory set of schemas.
        // -----------------------------------------------------------------------

//        loader.loadWithDependencies( loader.getSchema( MetaSchemaConstants.SCHEMA_NAME ), registries );
//        loader.loadWithDependencies( loader.getSchema( CoreSchemaConstants.SCHEMA_NAME ), registries );
        loader.loadAllEnabled( registries );  // @TODO remove this once we get the LDIF partition in place
        SerializableComparator.setRegistry( registries.getComparatorRegistry() );
    }

    
    public SchemaOperationControl getSchemaControl()
    {
        return schemaManager;
    }
    
    
    public Registries getRegistries()
    {
        return registries;
    }
    
    
    public void setWrappedPartition( Partition wrapped )
    {
        if ( this.isInitialized() )
        {
            throw new IllegalStateException( "Not allowed to set the wrappedPartition after initialization." );
        }
        this.wrapped = wrapped;
    }
    
    
    public Partition getWrappedPartition()
    {
        return this.wrapped;
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
    protected void doInit() throws InvalidNameException
    {
        wrapped.setId( ID );
        wrapped.setSuffix( ServerDNConstants.OU_SCHEMA_DN );
        
        try
        {
            wrapped.init( getDirectoryService() );
            PartitionSchemaLoader partitionLoader = new PartitionSchemaLoader( wrapped, registries );
            partitionLoader.loadAllEnabled( registries );
            loader = partitionLoader;

            SchemaPartitionDao dao = new SchemaPartitionDao( wrapped, registries );
            schemaManager = new SchemaOperationControl( registries, partitionLoader, dao );
            
            if ( wrapped instanceof NullPartition )
            {
                LOG.warn( "BYPASSING CRITICAL SCHEMA PROCESSING CODE DURING HEAVY DEV.  " +
                		"PLEASE REMOVE THIS CONDITION BY USING A VALID SCHEMA PARTITION!!!" );
                return;
            }
            
            // --------------------------------------------------------------------
            // Make sure all schema with attributes that are indexed are enabled
            // --------------------------------------------------------------------

            /*
             * We need to make sure that every attribute indexed by a partition is
             * loaded into the registries on the next step.  So here we must enable
             * the schemas of those attributes so they are loaded into the global
             * registries.
             */
            Map<String,Schema> schemaMap = dao.getSchemas();
            Set<Partition> partitions = new HashSet<Partition>();
            partitions.add( getDirectoryService().getSystemPartition() );
            partitions.addAll( getDirectoryService().getPartitions() );

            for ( Partition partition : partitions )
            {
                if ( partition instanceof BTreePartition )
                {
                    BTreePartition btpconf = ( BTreePartition ) partition;
                    for ( Index<?,ServerEntry> index : btpconf.getIndexedAttributes() )
                    {
                        String schemaName = null;
                        
                        try
                        {
                            // Try to retrieve the AT in the registries
                            AttributeType at = registries.getAttributeTypeRegistry().lookup( index.getAttributeId() );
                            schemaName = dao.findSchema( at.getOid() );
                        }
                        catch ( Exception e )
                        {
                            // It does not exists: just use the attribute ID
                            schemaName = dao.findSchema( index.getAttributeId() );
                        }
                        
                        if ( schemaName == null )
                        {
                            throw new NamingException( "Index on unidentified attribute: " + index.toString() );
                        }

                        Schema schema = schemaMap.get( schemaName );
                        if ( schema.isDisabled() )
                        {
                            dao.enableSchema( schemaName );
                        }
                    }
                }
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to initialize wrapped partition.", e );
            throw new RuntimeException( e );
        }
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
            LOG.error( "Attempt to destroy wrapped partition failed.", e );
            throw new RuntimeException( e );
        }
    }
    
    
    // -----------------------------------------------------------------------
    // Partition Interface Methods
    // -----------------------------------------------------------------------

    
    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#add(org.apache.directory.server.core.interceptor.context.AddOperationContext)
     */
    public void add( AddOperationContext opContext ) throws Exception
    {
        schemaManager.add( opContext );
        wrapped.add( opContext );
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
        ClonedServerEntry entry = directoryService.getPartitionNexus().lookup( opContext.newLookupContext( opContext.getDn() ) );
        schemaManager.delete( opContext, entry, opContext.hasRequestControl( CascadeControl.CONTROL_OID ) );
        wrapped.delete( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#getCacheSize()
     */
    public int getCacheSize()
    {
        return wrapped.getCacheSize();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#getId()
     */
    public final String getId()
    {
        return ID;
    }


    /**
     * {@inheritDoc}
     */
    public final LdapDN getSuffix()
    {
        return wrapped.getSuffix();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#list(org.apache.directory.server.core.interceptor.context.ListOperationContext)
     */
    public EntryFilteringCursor list( ListOperationContext opContext ) throws Exception
    {
        return wrapped.list( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#lookup(java.lang.Long)
     */
    public ClonedServerEntry lookup( Long id ) throws Exception
    {
        return wrapped.lookup( id );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#modify(org.apache.directory.server.core.interceptor.context.ModifyOperationContext)
     */
    public void modify( ModifyOperationContext opContext ) throws Exception
    {
        LOG.debug( "Modification attempt on schema partition {}: \n{}", opContext.getDn(), opContext );

        ServerEntry targetEntry = ( ServerEntry ) SchemaUtils.getTargetEntry( 
            opContext.getModItems(), opContext.getEntry() );

        schemaManager.modify( opContext, opContext.getEntry(), targetEntry, opContext
            .hasRequestControl( CascadeControl.CONTROL_OID ) );
        wrapped.modify( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#move(org.apache.directory.server.core.interceptor.context.MoveOperationContext)
     */
    public void move( MoveOperationContext opContext ) throws Exception
    {
        LdapDN oriChildName = opContext.getDn();
        ClonedServerEntry entry = opContext.lookup( oriChildName, ByPassConstants.LOOKUP_BYPASS );
        schemaManager.replace( opContext, entry, opContext.hasRequestControl( CascadeControl.CONTROL_OID ) );
        wrapped.move( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#moveAndRename(org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext)
     */
    public void moveAndRename( MoveAndRenameOperationContext opContext ) throws Exception
    {
        LdapDN oriChildName = opContext.getDn();
        ClonedServerEntry entry = opContext.lookup( oriChildName, ByPassConstants.LOOKUP_BYPASS );
        schemaManager.move( opContext, entry, opContext.hasRequestControl( CascadeControl.CONTROL_OID ) );
        wrapped.moveAndRename( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#rename(org.apache.directory.server.core.interceptor.context.RenameOperationContext)
     */
    public void rename( RenameOperationContext opContext ) throws Exception
    {
        schemaManager.modifyRn( opContext, opContext.getEntry(), opContext.hasRequestControl( CascadeControl.CONTROL_OID ) );
        wrapped.rename( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#search(org.apache.directory.server.core.interceptor.context.SearchOperationContext)
     */
    public EntryFilteringCursor search( SearchOperationContext opContext ) throws Exception
    {
        return wrapped.search( opContext );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#setCacheSize(int)
     */
    public void setCacheSize( int cacheSize )
    {
        wrapped.setCacheSize( cacheSize );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#setId(java.lang.String)
     */
    public void setId( String id )
    {
        throw new UnsupportedOperationException( "This partition's ID is fixed: " + ID );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.server.core.partition.Partition#setSuffix(java.lang.String)
     */
    public void setSuffix( String suffix )
    {
        throw new UnsupportedOperationException( "This partition's suffix is fixed: " + 
            ServerDNConstants.OU_SCHEMA_DN );
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
}
