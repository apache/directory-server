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
package org.apache.directory.mitosis.service;


import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.CSNFactory;
import org.apache.directory.mitosis.common.Constants;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.mitosis.operation.Operation;
import org.apache.directory.mitosis.operation.OperationFactory;
import org.apache.directory.mitosis.service.protocol.codec.ReplicationServerProtocolCodecFactory;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationClientContextHandler;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationServerContextHandler;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationServerProtocolHandler;
import org.apache.directory.mitosis.store.ReplicationStore;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultCoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


/**
 * An {@link Interceptor} that intercepts LDAP operations and propagates the
 * changes occurred by the operations into other {@link ReplicaId}s so the DIT
 * of each {@link ReplicaId} in the cluster has the same content without any
 * conflict.
 * <p>
 * Once an operation is invoked, this interceptor transforms it into one or
 * more operations that makes the requested operation more proper and robust
 * for replication.  The transformation process is actually just calling a
 * respective factory method in {@link OperationFactory}.  The methods in
 * {@link OperationFactory} returns a new {@link Operation} instance.
 * </p>
 * <p>
 * The newly created {@link Operation} is used for three purposes.
 * <ul>
 * <li>To perform the requested operation to the local {@link PartitionNexus}</li>
 * <li>To store the created {@link Operation} itself to
 *     {@link ReplicationStore} so that it can be retrieved later by
 *     {@link ReplicationLogCleanJob} and {@link ReplicationClientContextHandler}</li>
 * <li>To transfer itself to other {@link ReplicaId}s via TCP/IP communication
 *     between {@link ReplicationClientContextHandler} and
 *     {@link ReplicationServerContextHandler}</li>
 * </ul>
 * The first two actions (modifying the local DIT and storing the
 * {@link Operation} to {@link ReplicationStore}) are performed automatically
 * when
 * {@link Operation#execute(PartitionNexus, ReplicationStore, Registries)}
 * method is invoked.  {@link ReplicationInterceptor} always call it instead of
 * forwarding the requested operation to the next {@link Interceptor}.
 * </p>
 * <p>
 * The last action takes place by {@link ReplicationClientContextHandler},
 * which handles TCP/IP connection managed by {@link ClientConnectionManager}.
 * </p>
 * <p>
 * There are two special attributes in the entries to be replicated:
 * <ul>
 * <li><tt>entryCSN</tt> - stores {@link CSN} of the entry.  This attribute is
 *     used to compare the incoming operation from other replica is still
 *     valid.  If the local <tt>entryCSN</tt> value is bigger then that of the
 *     incoming operation, it means conflict, and therefore an appropriate
 *     conflict resolution mechanism should get engaged.</li>
 * <li><tt>entryDeleted</tt> - is <tt>TRUE</tt> if and only if the entry is
 *     deleted.  The entry is not deleted immediately by a delete operation
 *     because <tt>entryCSN</tt> attribute should be retained for certain
 *     amount of time to determine whether the incoming change LOG, which
 *     affects an entry with the same DN, is a conflict (modification on a
 *     deleted entry) or not (creation of a new entry). You can purge old
 *     deleted entries and related change logs in {@link ReplicationStore} by
 *     calling {@link #purgeAgedData()}, or they will be purged automatically
 *     by periodic manner as you configured with {@link ReplicationConfiguration}.
 *     by calling {@link ReplicationConfiguration#setLogMaxAge(int)}.
 *     Because of this attribute, <tt>lookup</tt> and <tt>search</tt>
 *     operations are overrided to ignore entries with <tt>entryDeleted</tt>
 *     set to <tt>TRUE</tt>.</li>
 * </ul>
 * </p>
 *
 * @org.apache.xbean.XBean
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ReplicationInterceptor extends BaseInterceptor
{
    private static final Logger LOG = LoggerFactory.getLogger( ReplicationInterceptor.class );

    /** The service name */
    public static final String DEFAULT_SERVICE_NAME = "replicationService";


    private static final String ENTRY_CSN_OID = "1.3.6.1.4.1.18060.0.4.1.2.30";
    private static final String ENTRY_DELETED_OID = "1.3.6.1.4.1.18060.0.4.1.2.31";

    /**
     * default name is the service name?
     */
    private String name = DEFAULT_SERVICE_NAME;

    /** The Directory service instance */
    private DirectoryService directoryService;
    
    /** The registries */
    private Registries registries;

    /** The directory nexus */
    private PartitionNexus nexus;

    /** A reference to the replication configuration */
    private ReplicationConfiguration configuration;
    
    
    private OperationFactory operationFactory;
    private ReplicationStore store;
    private NioSocketAcceptor registry;
    private final ClientConnectionManager clientConnectionManager = new ClientConnectionManager( this );

    /** A unique CSN factory instance */
    private CSNFactory csnFactory;
    
    
    /** Stores the number of milli seconds per day */
    private static final long MS_PER_DAY = 24L * 60L * 60L * 1000L;

    /**
     * Creates a new instance of ReplicationInterceptor.
     */    
    public ReplicationInterceptor()
    {
    }

    /**
     * This interceptor has configuration so it might be useful to allow several instances in a chain.
     * 
     * @return configured name for this interceptor.
     */
    public String getName() 
    {
        return name;
    }

        
    /**
     * Set the name for this service instance
     *
     * @param name The new name
     */
    public void setName(String name) 
    {
        this.name = name;
    }
    

    public ReplicationConfiguration getConfiguration()
    {
        return configuration;
    }


    public void setConfiguration(ReplicationConfiguration configuration) 
    {
        this.configuration = configuration;
    }

    
    /**
     * Initialize the Replication service. We have to check that the configuration
     * is valid, initialize a store for pending operations, and start the communication
     * with the other LDAP servers.
     * 
     * @param directoryService the DirectoryService instance 
     */
    public void init( DirectoryService directoryService ) throws Exception
    {
        // First check the configuration
        configuration.validate();
        
        // and then preserve frequently used ones
        this.directoryService = directoryService;
        registries = directoryService.getRegistries();
        nexus = directoryService.getPartitionNexus();
        store = configuration.getStore();
        operationFactory = new OperationFactory( directoryService, configuration );

        // Initialize store and service
        store.open( directoryService, configuration );
        boolean serviceStarted = false;
        
        // Create the CSN factory
        csnFactory = new CSNFactory();
        
        // Start the network mayer
        try
        {
            startNetworking();
            serviceStarted = true;
        }
        catch ( Exception e )
        {
            throw new ReplicationServiceException( "Failed to initialize MINA ServiceRegistry.", e );
        }
        finally
        {
            if ( !serviceStarted )
            {
                // roll back
                store.close();
            }
        }

        // Finally, purge old entries
        purgeAgedData();
    }


    private void startNetworking() throws Exception
    {
        registry = new NioSocketAcceptor();
        registry.setReuseAddress( true );
        registry.getFilterChain().addLast( "protocol",
            new ProtocolCodecFilter( new ReplicationServerProtocolCodecFactory() ) );

        registry.getFilterChain().addLast( "logger", new LoggingFilter() );
        registry.setHandler( new ReplicationServerProtocolHandler( this  ) );
        
        // bind server protocol provider
        registry.bind( new InetSocketAddress( configuration.getServerPort() ) );

        clientConnectionManager.start( configuration );
    }


    public void destroy()
    {
        stopNetworking();
        store.close();
    }


    private void stopNetworking()
    {
        // close all open connections, deactivate all filters and service registry
        try
        {
            clientConnectionManager.stop();
        }
        catch ( Exception e )
        {
            LOG.error( "[Replica-{}] Failed to stop the client connection manager.", configuration.getReplicaId() );
            LOG.error( "Stop failure exception: ", e );
        }
        registry.unbind();
    }


    /**
     * Forces this context to send replication data to the peer replica immediately.
     */
    public void replicate()
    {
        LOG.info( "[Replica-{}] Forcing replication...", configuration.getReplicaId() );
        this.clientConnectionManager.replicate();
    }

    
    /**
     * Wake the sleeping (unconnected) replicas.
     */
    public void interruptConnectors()
    {
        LOG.info( "[Replica-{}] Waking sleeping replicas...", configuration.getReplicaId() );
        this.clientConnectionManager.interruptConnectors();
    }


    /**
     * Purges old replication logs and the old entries marked as 'deleted'
     * (i.e. {@link Constants#ENTRY_DELETED} is <tt>TRUE</tt>).  This method
     * should be called periodically to make sure the size of the DIT and
     * {@link ReplicationStore} increase limitlessly.
     *
     * @see ReplicationConfiguration#setLogMaxAge(int)
     * @see ReplicationLogCleanJob
     * @throws javax.naming.NamingException on error
     */
    public void purgeAgedData() throws Exception
    {
        ServerEntry rootDSE = nexus.getRootDSE( null );
        EntryAttribute namingContextsAttr = rootDSE.get( SchemaConstants.NAMING_CONTEXTS_AT );
        
        if ( ( namingContextsAttr == null ) || ( namingContextsAttr.size() == 0 ) )
        {
            throw new NamingException( "No namingContexts attributes in rootDSE." );
        }

        long timeout = System.currentTimeMillis() - configuration.getLogMaxAge() * MS_PER_DAY;
        CSN purgeCSN = csnFactory.newInstance( timeout );
        
        ExprNode filter;

        try
        {
            filter = FilterParser.parse( "(&(" + ENTRY_CSN_OID + "<=" + purgeCSN.toOctetString() + ")(" + ENTRY_DELETED_OID
                + "=TRUE))" );
        }
        catch ( ParseException e )
        {
            throw ( NamingException ) new NamingException().initCause( e );
        }

        // Iterate all context partitions to send all entries of them.
        for ( Value<?> namingContext:namingContextsAttr )
        {
            // Convert attribute value to JNDI name.
            LdapDN contextName;
            
            contextName = new LdapDN( (String)namingContext.get() );

            contextName.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
            LOG.info( "[Replica-{}] Purging aged data under '{}'", configuration.getReplicaId(), contextName );
            purgeAgedData( contextName, filter );
        }

        store.removeLogs( purgeCSN, false );
    }


    private void purgeAgedData( LdapDN contextName, ExprNode filter ) throws Exception
    {
        SearchControls ctrl = new SearchControls();
        ctrl.setSearchScope( SearchControls.SUBTREE_SCOPE );
        ctrl.setReturningAttributes( new String[] { "entryCSN", ApacheSchemaConstants.ENTRY_DELETED_AT } );

        LdapDN adminDn = new LdapDN( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED );
        adminDn.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        CoreSession adminSession = 
            new DefaultCoreSession( new LdapPrincipal( adminDn, AuthenticationLevel.STRONG ), directoryService );

        EntryFilteringCursor cursor = nexus.search(
            new SearchOperationContext( adminSession, contextName, AliasDerefMode.DEREF_ALWAYS, filter, ctrl ) );

        List<LdapDN> names = new ArrayList<LdapDN>();
        
        try
        {
            while ( cursor.next() )
            {
                ServerEntry entry = cursor.get();
                LdapDN name = entry.getDn();
                
                if ( name.size() > contextName.size() )
                {
                    names.add( name );
                }
            }
        }
        finally
        {
            cursor.close();
        }

        for ( LdapDN name : names )
        {
            try
            {
                name.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
                ServerEntry entry = nexus.lookup( new LookupOperationContext( adminSession, name ) );
                LOG.info( "[Replica-{}] Purge: " + name + " (" + entry + ')', configuration.getReplicaId() );
                nexus.delete( new DeleteOperationContext( adminSession, name ) );
            }
            catch ( NamingException ex )
            {
                LOG.error( "[Replica-{}] Failed to fetch/delete: " + name, configuration.getReplicaId(), ex );
            }
        }
    }


    public void add( NextInterceptor nextInterceptor, AddOperationContext addContext ) throws Exception
    {
        Operation op = operationFactory.newAdd( 
            addContext.getDn(), addContext.getEntry() );
        op.execute( nexus, store, addContext.getSession(), csnFactory );
    }


    @Override
    public void delete( NextInterceptor next, DeleteOperationContext deleteContext ) throws Exception
    {
        Operation op = operationFactory.newDelete( deleteContext.getDn() );
        op.execute( nexus, store, deleteContext.getSession(), csnFactory );
    }


    public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws Exception
    {
        Operation op = operationFactory.newModify( modifyContext );
        op.execute( nexus, store, modifyContext.getSession(), csnFactory );
    }


    @Override
    public void move( NextInterceptor next, MoveOperationContext moveOpContext ) throws Exception
    {
        Operation op = operationFactory.newMove( moveOpContext.getDn(), moveOpContext.getParent() );
        op.execute( nexus, store, moveOpContext.getSession(), csnFactory );
    }


    @Override
    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameOpContext ) throws Exception
    {
        Operation op = operationFactory.newMove( moveAndRenameOpContext.getDn(),
                moveAndRenameOpContext.getParent(), moveAndRenameOpContext.getNewRdn(),
                moveAndRenameOpContext.getDelOldDn() );
        op.execute( nexus, store, moveAndRenameOpContext.getSession(), csnFactory );
    }


    @Override
    public void rename( NextInterceptor next, RenameOperationContext renameOpContext ) throws Exception
    {
        Operation op = operationFactory.newModifyRn( renameOpContext.getDn(), renameOpContext.getNewRdn(), renameOpContext.getDelOldDn() );
        op.execute( nexus, store, renameOpContext.getSession(), csnFactory );
    }


    public boolean hasEntry( NextInterceptor nextInterceptor, EntryOperationContext entryContext ) throws Exception
    {
        // Ask others first.
        boolean hasEntry = nextInterceptor.hasEntry( entryContext );

        // If the entry exists,
        if ( hasEntry )
        {
            // Check DELETED attribute.
            try
            {
                ServerEntry entry = nextInterceptor.lookup( new LookupOperationContext( entryContext.getSession(), 
                    entryContext.getDn() ) );
                hasEntry = !isDeleted( entry );
            }
            catch ( NameNotFoundException e )
            {
                hasEntry = false;
            }
        }

        return hasEntry;
    }


    public ClonedServerEntry lookup( NextInterceptor nextInterceptor, LookupOperationContext lookupContext ) throws Exception
    {
        if ( lookupContext.getAttrsId() != null )
        {
            boolean found = false;

            String[] attrIds = lookupContext.getAttrsIdArray();

            // Look for 'entryDeleted' attribute is in attrIds.
            for ( String attrId:attrIds )
            {
                if ( ApacheSchemaConstants.ENTRY_DELETED_AT.equalsIgnoreCase( attrId ) )
                {
                    found = true;
                    break;
                }
            }

            // If not exists, add one.
            if ( !found )
            {
                String[] newAttrIds = new String[attrIds.length + 1];
                System.arraycopy( attrIds, 0, newAttrIds, 0, attrIds.length );
                newAttrIds[attrIds.length] = ApacheSchemaConstants.ENTRY_DELETED_AT;
                lookupContext.setAttrsId( newAttrIds );
            }
        }

        ClonedServerEntry entry = nextInterceptor.lookup( lookupContext );
        ensureNotDeleted( lookupContext, entry );
        return entry;
    }


    @Override
    public EntryFilteringCursor list( NextInterceptor nextInterceptor, ListOperationContext opContext ) throws Exception
    {
        EntryFilteringCursor cursor = nextInterceptor.search(
                new SearchOperationContext(
                    opContext.getSession(), opContext.getDn(), opContext.getAliasDerefMode(),
                    new PresenceNode( SchemaConstants.OBJECT_CLASS_AT_OID ),
                    new SearchControls() ) );

        cursor.addEntryFilter( Constants.DELETED_ENTRIES_FILTER );
        return cursor;
    }


    @Override
    public EntryFilteringCursor search( NextInterceptor nextInterceptor, SearchOperationContext opContext ) 
        throws Exception
    {
        SearchControls searchControls = opContext.getSearchControls();

        if ( searchControls.getReturningAttributes() != null )
        {
            String[] oldAttrIds = searchControls.getReturningAttributes();
            String[] newAttrIds = new String[oldAttrIds.length + 1];
            System.arraycopy( oldAttrIds, 0, newAttrIds, 0, oldAttrIds.length );
            newAttrIds[oldAttrIds.length] = ApacheSchemaConstants.ENTRY_DELETED_AT.toLowerCase();
            searchControls.setReturningAttributes( newAttrIds );
        }

        EntryFilteringCursor cursor = nextInterceptor.search( new SearchOperationContext( opContext.getSession(), 
            opContext.getDn(), opContext.getAliasDerefMode(), opContext.getFilter(), searchControls ) );
        cursor.addEntryFilter( Constants.DELETED_ENTRIES_FILTER );
        return cursor;
    }


    private void ensureNotDeleted( OperationContext opContext, ServerEntry entry ) throws Exception 
    {
        if ( isDeleted( entry ) )
        {
            LdapNameNotFoundException e = new LdapNameNotFoundException( "Deleted entry: " 
                + opContext.getDn().getUpName() );
            e.setResolvedName( nexus.getMatchedName( 
                new GetMatchedNameOperationContext( opContext.getSession(), opContext.getDn() ) ) );
            throw e;
        }
    }


    private boolean isDeleted( ServerEntry entry ) throws NamingException
    {
        if ( entry == null )
        {
            return true;
        }

        return entry.contains( ApacheSchemaConstants.ENTRY_DELETED_AT, "TRUE" );
    }

    
    /**
     * @return The CSNFactory
     */
    public CSNFactory getCsnFactory()
    {
        return csnFactory;
    }

    
    public DirectoryService getDirectoryService()
    {
        return directoryService;
    }
}
