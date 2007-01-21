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


import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.Constants;
import org.apache.directory.mitosis.common.Replica;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.common.DefaultCSN;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.mitosis.operation.Operation;
import org.apache.directory.mitosis.operation.OperationFactory;
import org.apache.directory.mitosis.service.protocol.codec.ReplicationServerProtocolCodecFactory;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationClientContextHandler;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationServerContextHandler;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationServerProtocolHandler;
import org.apache.directory.mitosis.store.ReplicationStore;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.enumeration.SearchResultFilteringEnumeration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.FilterParserImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link Interceptor} that intercepts LDAP operations and propagates the
 * changes occurred by the operations into other {@link Replica}s so the DIT
 * of each {@link Replica} in the cluster has the same content without any
 * conflict.
 * <p>
 * Once an operation is invoked, this interceptor transforms it into one or
 * more operations that makes the requested operation more proper and robust
 * for replication.  The transformation process is actually just calling a
 * respective factory method in {@link OperationFactory}.  The methods in 
 * {@link OperationFactory} returns a new {@link Operation} instance.
 * <p>
 * The newly created {@link Operation} is used for three purposes.
 * <ul>
 * <li>To perform the requested operation to the local {@link PartitionNexus}
 * <li>To store the created {@link Operation} itself to
 *     {@link ReplicationStore} so that it can be retrieved later by
 *     {@link ReplicationLogCleanJob} and {@link ReplicationClientContextHandler}
 * <li>To transfer itself to other {@link Replica}s via TCP/IP communication
 *     between {@link ReplicationClientContextHandler} and
 *     {@link ReplicationServerContextHandler}
 * </ul>
 * The first two actions (modifying the local DIT and storing the
 * {@link Operation} to {@link ReplicationStore}) are performed automatically
 * when
 * {@link Operation#execute(PartitionNexus, ReplicationStore, AttributeTypeRegistry)}
 * method is invoked.  {@link ReplicationService} always call it instead of
 * forwarding the requested operation to the next {@link Interceptor}.
 * <p>
 * The last action takes place by {@link ReplicationClientContextHandler},
 * which handles TCP/IP connection managed by {@link ClientConnectionManager}.
 * <p>
 * There are two special attributes in the entries to be replicated:
 * <ul>
 * <li><tt>entryCSN</tt> - stores {@link CSN} of the entry.  This attribute is
 *     used to compare the incoming operation from other replica is still
 *     valid.  If the local <tt>entryCSN</tt> value is bigger then that of the
 *     incoming operation, it means conflict, and therefore an appropriate
 *     conflict resolution mechanism should get engaged.</li>
 * <li><tt>entryDeleted</tt> - is <tt>true</tt> if and only if the entry is
 *     deleted.  The entry is not deleted immediately by a delete operation
 *     because <tt>entryCSN</tt> attribute should be retained for certain
 *     amount of time to determine whether the incoming change log, which
 *     affects an entry with the same DN, is a conflict (modification on a
 *     deleted entry) or not (creation of a new entry). You can purge old
 *     deleted entries and related change logs in {@link ReplicationStore} by
 *     calling {@link #purgeAgedData()}, or they will be purged automatically
 *     by periodic manner as you configured with {@link ReplicationConfiguration}.
 *     by calling {@link ReplicationConfiguration#setLogMaxAge(int)}.
 *     Because of this attribute, <tt>lookup</tt> and <tt>search</tt>
 *     operations are overrided to ignore entries with <tt>entryDeleted</tt>
 *     set to <tt>true</tt>.</li>
 * </ul>
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ReplicationService extends BaseInterceptor
{
    private static final Logger log = LoggerFactory.getLogger( ReplicationService.class );

    private static final String ENTRY_CSN_OID = "1.3.6.1.4.1.18060.0.4.1.2.30";
    private static final String ENTRY_DELETED_OID = "1.3.6.1.4.1.18060.0.4.1.2.31";

    private DirectoryServiceConfiguration directoryServiceConfiguration;
    private ReplicationConfiguration configuration;
    private PartitionNexus nexus;
    private OperationFactory operationFactory;
    private ReplicationStore store;
    private IoAcceptor registry;
    private final ClientConnectionManager clientConnectionManager = new ClientConnectionManager( this );
    private AttributeTypeRegistry attrRegistry;


    public ReplicationService()
    {
    }


    public ReplicationConfiguration getConfiguration()
    {
        return configuration;
    }


    public void setConfiguration( ReplicationConfiguration cfg )
    {
        cfg.validate();
        this.configuration = cfg;
    }


    public DirectoryServiceConfiguration getFactoryConfiguration()
    {
        return directoryServiceConfiguration;
    }


    public void init( DirectoryServiceConfiguration serviceCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        configuration.validate();
        // and then preserve frequently used ones
        directoryServiceConfiguration = serviceCfg;
        nexus = serviceCfg.getPartitionNexus();
        store = configuration.getStore();
        operationFactory = new OperationFactory( serviceCfg, configuration );
        attrRegistry = serviceCfg.getRegistries().getAttributeTypeRegistry();

        // Initialize store and service
        store.open( serviceCfg, configuration );
        boolean serviceStarted = false;
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

        purgeAgedData();
    }


    private void startNetworking() throws Exception
    {
        registry = new SocketAcceptor();
        SocketAcceptorConfig config = new SocketAcceptorConfig();
        config.setReuseAddress( true );

        config.getFilterChain().addLast( "protocol",
            new ProtocolCodecFilter( new ReplicationServerProtocolCodecFactory() ) );

        config.getFilterChain().addLast( "logger", new LoggingFilter() );

        // bind server protocol provider
        registry.bind( new InetSocketAddress( configuration.getServerPort() ), new ReplicationServerProtocolHandler(
            this ), config );

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
            log.warn( "Failed to stop the client connection manager.", e );
        }
        registry.unbindAll();
    }
    
    
    /**
     * Forces this context to send replication data to the peer replica immediately.
     */
    public void replicate()
    {
        log.info( "Forcing replication..." );
        this.clientConnectionManager.replicate();
    }


    /**
     * Purges old replication logs and the old entries marked as 'deleted'
     * (i.e. {@link Constants#ENTRY_DELETED} is <tt>true</tt>).  This method
     * should be called periodically to make sure the size of the DIT and
     * {@link ReplicationStore} increase limitlessly.
     * 
     * @see ReplicationConfiguration#setLogMaxAge(int)
     * @see ReplicationLogCleanJob
     */
    public void purgeAgedData() throws NamingException
    {
        Attributes rootDSE = nexus.getRootDSE();
        Attribute namingContextsAttr = rootDSE.get( "namingContexts" );
        if ( namingContextsAttr == null || namingContextsAttr.size() == 0 )
        {
            throw new NamingException( "No namingContexts attributes in rootDSE." );
        }

        CSN purgeCSN = new DefaultCSN( System.currentTimeMillis() - configuration.getLogMaxAge() * 1000L * 60L * 60L
            * 24L, // convert days to millis
            new ReplicaId( "ZZZZZZZZZZZZZZZZ" ), Integer.MAX_VALUE );
        FilterParser parser = new FilterParserImpl();
        ExprNode filter;

        try
        {
            filter = parser.parse( "(& (" + ENTRY_CSN_OID + "=<" + purgeCSN.toOctetString() + ") (" + ENTRY_DELETED_OID
                + "=true))" );
        }
        catch ( IOException e )
        {
            throw ( NamingException ) new NamingException().initCause( e );
        }
        catch ( ParseException e )
        {
            throw ( NamingException ) new NamingException().initCause( e );
        }

        // Iterate all context partitions to send all entries of them.
        NamingEnumeration e = namingContextsAttr.getAll();
        while ( e.hasMore() )
        {
            Object value = e.next();
            // Convert attribute value to JNDI name.
            LdapDN contextName;
            if ( value instanceof LdapDN )
            {
                contextName = ( LdapDN ) value;
            }
            else
            {
                contextName = new LdapDN( String.valueOf( value ) );
            }

            contextName.normalize( attrRegistry.getNormalizerMapping() );
            log.info( "Purging aged data under '" + contextName + '"' );
            purgeAgedData( contextName, filter );
        }

        store.removeLogs( purgeCSN, false );
    }


    private void purgeAgedData( LdapDN contextName, ExprNode filter ) throws NamingException
    {
        SearchControls ctrl = new SearchControls();
        ctrl.setSearchScope( SearchControls.SUBTREE_SCOPE );
        ctrl.setReturningAttributes( new String[] { "entryCSN", "entryDeleted" } );

        NamingEnumeration e = nexus.search( contextName, directoryServiceConfiguration.getEnvironment(), filter, ctrl );

        List<LdapDN> names = new ArrayList<LdapDN>();
        try
        {
            while ( e.hasMore() )
            {
                SearchResult sr = ( SearchResult ) e.next();
                LdapDN name = new LdapDN( sr.getName() );
                if ( name.size() > contextName.size() )
                {
                    names.add( new LdapDN( sr.getName() ) );
                }
            }
        }
        finally
        {
            e.close();
        }

        Iterator<LdapDN> it = names.iterator();
        while ( it.hasNext() )
        {
            LdapDN name = it.next();
            try
            {
                Attributes entry = nexus.lookup( name );
                log.info( "Purge: " + name + " (" + entry + ')' );
                nexus.delete( name );
            }
            catch ( NamingException ex )
            {
                log.warn( "Failed to fetch/delete: " + name, ex );
            }
        }
    }


    public void add( NextInterceptor nextInterceptor, LdapDN normalizedName, Attributes entry ) throws NamingException
    {
        Operation op = operationFactory.newAdd( normalizedName, entry );
        op.execute( nexus, store, attrRegistry );
    }


    public void delete( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        Operation op = operationFactory.newDelete( name );
        op.execute( nexus, store, attrRegistry );
    }


    public void modify( NextInterceptor next, LdapDN name, int modOp, Attributes attrs ) throws NamingException
    {
        Operation op = operationFactory.newModify( name, modOp, attrs );
        op.execute( nexus, store, attrRegistry );
    }


    public void modify( NextInterceptor next, LdapDN name, ModificationItemImpl[] items ) throws NamingException
    {
        Operation op = operationFactory.newModify( name, items );
        op.execute( nexus, store, attrRegistry );
    }


    public void modifyRn( NextInterceptor next, LdapDN oldName, String newRDN, boolean deleteOldRDN )
        throws NamingException
    {
        Operation op = operationFactory.newModifyRn( oldName, newRDN, deleteOldRDN );
        op.execute( nexus, store, attrRegistry );
    }


    public void move( NextInterceptor next, LdapDN oldName, LdapDN newParentName, String newRDN, boolean deleteOldRDN )
        throws NamingException
    {
        Operation op = operationFactory.newMove( oldName, newParentName, newRDN, deleteOldRDN );
        op.execute( nexus, store, attrRegistry );
    }


    public void move( NextInterceptor next, LdapDN oldName, LdapDN newParentName ) throws NamingException
    {
        Operation op = operationFactory.newMove( oldName, newParentName );
        op.execute( nexus, store, attrRegistry );
    }


    public boolean hasEntry( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        // Ask others first.
        boolean hasEntry = nextInterceptor.hasEntry( name );

        // If the entry exists,
        if ( hasEntry )
        {
            // Check DELETED attribute.
            try
            {
                Attributes entry = nextInterceptor.lookup( name );
                hasEntry = !isDeleted( entry );
            }
            catch ( NameNotFoundException e )
            {
                System.out.println( e.toString( true ) );
                hasEntry = false;
            }
        }

        return hasEntry;
    }


    public Attributes lookup( NextInterceptor nextInterceptor, LdapDN name ) throws NamingException
    {
        Attributes result = nextInterceptor.lookup( name );
        ensureNotDeleted( name, result );
        return result;
    }


    public Attributes lookup( NextInterceptor nextInterceptor, LdapDN name, String[] attrIds ) throws NamingException
    {
        boolean found = false;
        
        // Look for 'entryDeleted' attribute is in attrIds.
        for ( int i = 0; i < attrIds.length; i++ )
        {
            if ( Constants.ENTRY_DELETED.equals( attrIds[i] ) )
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
            newAttrIds[attrIds.length] = Constants.ENTRY_DELETED;
            attrIds = newAttrIds;
        }

        Attributes result = nextInterceptor.lookup( name, attrIds );
        ensureNotDeleted( name, result );
        return result;
    }


    public NamingEnumeration list( NextInterceptor nextInterceptor, LdapDN baseName ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.list( baseName );
        return new SearchResultFilteringEnumeration( e, new SearchControls(), InvocationStack.getInstance().peek(),
            Constants.DELETED_ENTRIES_FILTER );
    }


    public NamingEnumeration search( NextInterceptor nextInterceptor, LdapDN baseName, Map environment, ExprNode filter,
        SearchControls searchControls ) throws NamingException
    {
        if ( searchControls.getReturningAttributes() != null )
        {
            String[] oldAttrIds = searchControls.getReturningAttributes();
            String[] newAttrIds = new String[oldAttrIds.length + 1];
            System.arraycopy( oldAttrIds, 0, newAttrIds, 0, oldAttrIds.length );
            newAttrIds[oldAttrIds.length] = Constants.ENTRY_DELETED.toLowerCase();
            searchControls.setReturningAttributes( newAttrIds );
        }
        
        NamingEnumeration e = nextInterceptor.search( baseName, environment, filter, searchControls );
        return new SearchResultFilteringEnumeration( e, searchControls, InvocationStack.getInstance().peek(),
            Constants.DELETED_ENTRIES_FILTER );
    }


    private void ensureNotDeleted( LdapDN name, Attributes entry ) throws NamingException, LdapNameNotFoundException
    {
        if ( isDeleted( entry ) )
        {
            LdapNameNotFoundException e = new LdapNameNotFoundException( "Deleted entry: " + name );
            e.setResolvedName( nexus.getMatchedName( name ) );
            throw e;
        }
    }


    private boolean isDeleted( Attributes entry ) throws NamingException
    {
        if ( entry == null )
        {
            return true;
        }

        Attribute deleted = entry.get( Constants.ENTRY_DELETED );
        return ( deleted != null && "true".equalsIgnoreCase( deleted.get().toString() ) );
    }
}
