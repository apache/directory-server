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

import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.Constants;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.common.SimpleCSN;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.mitosis.operation.Operation;
import org.apache.directory.mitosis.operation.OperationFactory;
import org.apache.directory.mitosis.service.protocol.codec.ReplicationServerProtocolCodecFactory;
import org.apache.directory.mitosis.service.protocol.handler.ReplicationServerProtocolHandler;
import org.apache.directory.mitosis.store.ReplicationStore;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.enumeration.SearchResultFilteringEnumeration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.filter.FilterParserImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplicationService extends BaseInterceptor
{
    private static final Logger log = LoggerFactory.getLogger( ReplicationService.class );
    private DirectoryServiceConfiguration directoryServiceConfiguration;
    private ReplicationConfiguration configuration;
    private PartitionNexus nexus;
    private OperationFactory operationFactory;
    private ReplicationStore store;
    private IoAcceptor registry;
    private final ClientConnectionManager clientConnectionManager = new ClientConnectionManager( this );
    
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
        
        // Initialize store and service
        store.open( serviceCfg, configuration );
        boolean serviceStarted = false;
        try
        {
            startNetworking();
            serviceStarted = true;
        }
        catch( Exception e )
        {
            throw new ReplicationServiceException( "Failed to initialize MINA ServiceRegistry.", e );
        }
        finally
        {
            if( !serviceStarted )
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
        IoServiceConfig config = new SocketAcceptorConfig();

        config.getFilterChain().addLast(
                "protocol",
                new ProtocolCodecFilter( new ReplicationServerProtocolCodecFactory() ) );
        
        config.getFilterChain().addLast(
                "logger",
                new LoggingFilter());

        // bind server protocol provider
        registry.bind(
                new InetSocketAddress( 10101 ),
                new ReplicationServerProtocolHandler(this),
                config );
        
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
        catch( Exception e )
        {
            log.warn( "Failed to stop the client connection manager.", e );
        }
        registry.unbindAll();
    }
    
    public void purgeAgedData() throws NamingException
    {
        Attributes rootDSE = nexus.getRootDSE();
        Attribute namingContextsAttr = rootDSE.get( "namingContexts" );
        if( namingContextsAttr == null || namingContextsAttr.size() == 0 )
        {
            throw new NamingException( "No namingContexts attributes in rootDSE." );
        }
        
        CSN purgeCSN = new SimpleCSN(
                System.currentTimeMillis() -
                configuration.getLogMaxAge() * 1000L * 60L * 60L * 24L, // convert days to millis
                new ReplicaId( "ZZZZZZZZZZZZZZZZ" ),
                Integer.MAX_VALUE );
        FilterParser parser = new FilterParserImpl();
        ExprNode filter;
        
        try
        {
            filter = parser.parse(
                "(& (entryCSN=<" +
                purgeCSN.toOctetString() +
                ") (entryDeleted=true))" );
        }
        catch( IOException e )
        {
            throw ( NamingException ) new NamingException().initCause( e );
        }
        catch( ParseException e )
        {
            throw ( NamingException ) new NamingException().initCause( e );
        }
        
        // Iterate all context partitions to send all entries of them.
        NamingEnumeration e = namingContextsAttr.getAll();
        while( e.hasMore() )
        {
            Object value = e.next();
            // Convert attribute value to JNDI name.
            Name contextName;
            if( value instanceof Name )
            {
                contextName = ( Name ) value;
            }
            else
            {
                contextName = new LdapDN( String.valueOf( value ) );
            }
            
            log.info( "Purging aged data under '" + contextName + '"');
            purgeAgedData( contextName, filter );
        }
        
        store.removeLogs( purgeCSN, false );
    }
    
    private void purgeAgedData( Name contextName, ExprNode filter ) throws NamingException
    {
        SearchControls ctrl = new SearchControls();
        ctrl.setSearchScope( SearchControls.SUBTREE_SCOPE ); 
        ctrl.setReturningAttributes( new String[]
        { "entryCSN", "entryDeleted" } );
                                                
        NamingEnumeration e = nexus.search(
            (LdapDN)contextName,
                directoryServiceConfiguration.getEnvironment(),
                filter, ctrl );

        List names = new ArrayList();
        try
        {
            while( e.hasMore() )
            {
                SearchResult sr = ( SearchResult ) e.next();
                Name name = new LdapDN( sr.getName() );
                if( name.size() > contextName.size() )
                {
                    names.add( new LdapDN( sr.getName() ) );
                }
            }
        }
        finally
        {
            e.close();
        }
        
        Iterator it = names.iterator();
        while( it.hasNext() )
        {
            Name name = (Name) it.next();
            try
            {
                Attributes entry = nexus.lookup( (LdapDN)name );
                log.info( "Purge: " + name + " (" + entry + ')' );
                nexus.delete( (LdapDN)name );
            }
            catch( NamingException ex )
            {
                log.warn( "Failed to fetch/delete: " + name, ex );
            }
        }
    }
    
    public void add( NextInterceptor nextInterceptor, String userProvidedName, Name normalizedName, Attributes entry ) throws NamingException
    {
        Operation op = operationFactory.newAdd( userProvidedName, normalizedName, entry );
        op.execute( nexus, store );
    }

    public void delete( NextInterceptor nextInterceptor, Name name ) throws NamingException
    {
        Operation op = operationFactory.newDelete( name );
        op.execute( nexus, store );
    }

    public void modify( NextInterceptor next, Name name, int modOp, Attributes attrs ) throws NamingException
    {
        Operation op = operationFactory.newModify( name, modOp, attrs );
        op.execute( nexus, store );
    }

    public void modify( NextInterceptor next, Name name, ModificationItem[] items ) throws NamingException
    {
        Operation op = operationFactory.newModify( name, items );
        op.execute( nexus, store );
    }

    public void modifyRn( NextInterceptor next, Name oldName, String newRDN, boolean deleteOldRDN ) throws NamingException
    {
        Operation op = operationFactory.newModifyRn( oldName, newRDN, deleteOldRDN );
        op.execute( nexus, store );
    }

    public void move( NextInterceptor next, Name oldName, Name newParentName, String newRDN, boolean deleteOldRDN ) throws NamingException
    {
        Operation op = operationFactory.newMove( oldName, newParentName, newRDN, deleteOldRDN );
        op.execute( nexus, store );
    }

    public void move( NextInterceptor next, Name oldName, Name newParentName ) throws NamingException
    {
        Operation op = operationFactory.newMove( oldName, newParentName );
        op.execute( nexus, store );
    }

    public boolean hasEntry( NextInterceptor nextInterceptor, Name name ) throws NamingException
    {
        // Ask others first.
        boolean hasEntry = nextInterceptor.hasEntry( (LdapDN)name );
        
        // If the entry exists,
        if( hasEntry )
        {
            // Check DELETED attribute.
            try
            {
                Attributes entry = nextInterceptor.lookup( (LdapDN)name );
                hasEntry = !isDeleted( entry );
            }
            catch( NameNotFoundException e )
            {
                System.out.println( e.toString( true ) );
                hasEntry = false;
            }
        }
        
        return hasEntry;
    }

    public Attributes lookup( NextInterceptor nextInterceptor, Name name ) throws NamingException
    {
        Attributes result = nextInterceptor.lookup( (LdapDN)name );
        ensureNotDeleted( name, result );
        return result;
    }


    public Attributes lookup( NextInterceptor nextInterceptor, Name name, String[] attrIds ) throws NamingException
    {
        boolean found = false;
        // Look for 'entryDeleted' attribute is in attrIds.
        for( int i = 0; i < attrIds.length; i ++ )
        {
            if( Constants.ENTRY_DELETED.equals( attrIds[i] ) )
            {
                found = true;
                break;
            }
        }
        
        // If not exists, add one.
        if( !found )
        {
            String[] newAttrIds = new String[ attrIds.length + 1 ];
            System.arraycopy( attrIds, 0, newAttrIds, 0, attrIds.length );
            newAttrIds[ attrIds.length ] = Constants.ENTRY_DELETED;
            attrIds = newAttrIds;
        }
        
        Attributes result = nextInterceptor.lookup( (LdapDN)name, attrIds );
        ensureNotDeleted( name, result );
        return result;
    }


    public NamingEnumeration list( NextInterceptor nextInterceptor, Name baseName ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.list( (LdapDN)baseName );
        return new SearchResultFilteringEnumeration( e, new SearchControls(), InvocationStack.getInstance().peek(), Constants.DELETED_ENTRIES_FILTER );
    }


    public NamingEnumeration search( NextInterceptor nextInterceptor, Name baseName, Map environment, ExprNode filter, SearchControls searchControls ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.search( (LdapDN)baseName, environment, filter, searchControls );
        if ( searchControls.getReturningAttributes() != null )
        {
            return e;
        }

        return new SearchResultFilteringEnumeration( e, searchControls, InvocationStack.getInstance().peek(), Constants.DELETED_ENTRIES_FILTER );
    }

    private void ensureNotDeleted( Name name, Attributes entry ) throws NamingException, LdapNameNotFoundException
    {
        if( isDeleted( entry ) )
        {
            LdapNameNotFoundException e = 
                    new LdapNameNotFoundException( "Deleted entry: " + name );
            e.setResolvedName( nexus.getMatchedName( (LdapDN)name ) );
            throw e;
        }
    }    
    
    private boolean isDeleted( Attributes entry ) throws NamingException
    {
        if( entry == null )
        {
            return true;
        }

        Attribute deleted = entry.get( Constants.ENTRY_DELETED );
        return ( deleted != null && "true".equals( deleted.get().toString() ) );
    }

}
