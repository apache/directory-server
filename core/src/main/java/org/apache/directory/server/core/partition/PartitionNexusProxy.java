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


import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.naming.Context;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.filtering.EntryFilter;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.interceptor.context.AddContextPartitionOperationContext;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.ListSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RemoveContextPartitionOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapSizeLimitExceededException;
import org.apache.directory.shared.ldap.exception.LdapTimeLimitExceededException;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A decorator that wraps other {@link PartitionNexus} to enable
 * {@link InterceptorChain} and {@link InvocationStack} support.
 * All {@link Invocation}s made to this nexus is automatically pushed to
 * {@link InvocationStack} of the current thread, and popped when
 * the operation ends.  All invocations are filtered by {@link InterceptorChain}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PartitionNexusProxy extends PartitionNexus
{

    /**
     * A static object to store the rootDSE entry with all the attributes
     */
    private static ServerEntry ROOT_DSE_ALL;

    /**
     * A static object to store the rootDSE entry without operationnal attributes
     */
    private static ServerEntry ROOT_DSE_NO_OPERATIONNAL;

    /**
     * A mutex to protect the rootDSE construction
     */
    private static final Object ROOT_DSE_ALL_MUTEX = new Object();

    /**
     * A mutex to protect the rootDSE construction
     */
    private static final Object ROOT_DSE_NOOP_MUTEX = new Object();

    private final DirectoryService service;


    /**
     * Creates a new instance.
     *
     * @param caller  a JNDI {@link Context} object that will call this proxy
     * @param service a JNDI service
     */
    public PartitionNexusProxy( DirectoryService service ) throws Exception
    {
        this.service = service;
    }


    public LdapContext getLdapContext()
    {
        return service.getPartitionNexus().getLdapContext();
    }


    public String getId()
    {
        throw new UnsupportedOperationException( "Nexus partition proxy objects do not have an Id." );
    }


    public void setId( String id )
    {
        throw new UnsupportedOperationException( "Not supported by PartitionNexusProxy" );
    }


    public ClonedServerEntry getContextEntry()
    {
        throw new UnsupportedOperationException( "Not supported by PartitionNexusProxy" );
    }


    public void setContextEntry( ServerEntry contextEntry )
    {
        throw new UnsupportedOperationException( "Not supported by PartitionNexusProxy" );
    }


    public String getSuffix()
    {
        throw new UnsupportedOperationException( "Not supported by PartitionNexusProxy" );
    }


    public void setSuffix( String suffix )
    {
        throw new UnsupportedOperationException( "Not supported by PartitionNexusProxy" );
    }


    public void setCacheSize( int cacheSize )
    {
        throw new UnsupportedOperationException( "Not supported by PartitionNexusProxy" );
    }


    public int getCacheSize()
    {
        throw new UnsupportedOperationException( "Not supported by PartitionNexusProxy" );
    }


    public void init( DirectoryService core ) throws Exception
    {
    }


    public void destroy()
    {
    }


    public Partition getSystemPartition()
    {
        return service.getPartitionNexus().getSystemPartition();
    }


    public Partition getPartition( LdapDN dn ) throws Exception
    {
        return service.getPartitionNexus().getPartition( dn );
    }


    public LdapDN getSuffixDn() throws Exception
    {
        return service.getPartitionNexus().getSuffixDn();
    }

    public LdapDN getUpSuffixDn() throws Exception
    {
        return service.getPartitionNexus().getUpSuffixDn();
    }


    public void sync() throws Exception
    {
        this.service.sync();
    }


    public void close() throws Exception
    {
        this.service.shutdown();
    }


    public boolean isInitialized()
    {
        return this.service.isStarted();
    }


    public LdapDN getMatchedName( GetMatchedNameOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            return service.getInterceptorChain().getMatchedName( opContext );
        }
        finally
        {
            pop();
        }
    }
    
    
    public LdapDN getMatchedName( GetMatchedNameOperationContext opContext, Collection<String> byPassed ) throws Exception
    {
        opContext.setByPassed( byPassed );
        return getMatchedName( opContext );
    }


    private void push( OperationContext opContext )
    {
        InvocationStack.getInstance().push( opContext );
    }
    
    
    private OperationContext pop()
    {
        return InvocationStack.getInstance().pop();
    }


    public LdapDN getSuffix( GetSuffixOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            return service.getInterceptorChain().getSuffix( opContext );
        }
        finally
        {
            pop();
        }
    }


    public LdapDN getSuffix( GetSuffixOperationContext opContext, Collection<String> byPassed ) throws Exception
    {
        opContext.setByPassed( byPassed );
        return getSuffix( opContext );
    }


    public Set<String> listSuffixes( ListSuffixOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            return service.getInterceptorChain().listSuffixes( opContext );
        }
        finally
        {
            pop();
        }
    }


    public Set<String> listSuffixes( ListSuffixOperationContext opContext, Collection<String> byPassed ) throws Exception
    {
        opContext.setByPassed( byPassed );
        return listSuffixes( opContext );
    }


    public boolean compare( CompareOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            return service.getInterceptorChain().compare( opContext );
        }
        finally
        {
            pop();
        }
    }


    public boolean compare( CompareOperationContext opContext, Collection<String> byPassed ) throws Exception
    {
        opContext.setByPassed( byPassed );
        return compare( opContext );
    }


    public void delete( DeleteOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            service.getInterceptorChain().delete( opContext );
        }
        finally
        {
            pop();
        }
    }


    public void delete( DeleteOperationContext opContext, Collection<String> byPassed ) throws Exception
    {
        opContext.setByPassed( byPassed );
        delete( opContext );
    }


    public void add( AddOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            service.getInterceptorChain().add( opContext );
        }
        finally
        {
            pop();
        }
    }


    public void add( AddOperationContext opContext, Collection<String> byPassed ) throws Exception
    {
        opContext.setByPassed( byPassed );
        add( opContext );
    }


    public void modify( ModifyOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            service.getInterceptorChain().modify( opContext );
        }
        finally
        {
            pop();
        }
    }


    public void modify( ModifyOperationContext opContext, Collection<String> byPassed ) throws Exception
    {
        opContext.setByPassed( byPassed );
        modify( opContext );
    }


    public EntryFilteringCursor list( ListOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            return service.getInterceptorChain().list( opContext );
        }
        finally
        {
            pop();
        }
    }


    public EntryFilteringCursor list( ListOperationContext opContext, Collection<String> byPassed ) throws Exception
    {
        opContext.setByPassed( byPassed );
        return list( opContext );
    }


    public EntryFilteringCursor search( SearchOperationContext opContext ) throws Exception
    {
        EntryFilteringCursor cursor = search( opContext, null );
        final SearchControls searchCtls = opContext.getSearchControls();

        if ( searchCtls.getTimeLimit() + searchCtls.getCountLimit() > 0 )
        {
            // this will be the last filter added so other filters before it must
            // have passed/approved of the entry to be returned back to the client
            // so the candidate we have is going to be returned for sure
            cursor.addEntryFilter( new EntryFilter()
            {
                final long startTime = System.currentTimeMillis();
                int count = 0; // with prefetch we've missed one which is ok since 1 is the minimum

                public boolean accept( SearchingOperationContext operation, ClonedServerEntry entry )
                        throws Exception
                {
                    if ( searchCtls.getTimeLimit() > 0 )
                    {
                        long runtime = System.currentTimeMillis() - startTime;
                        if ( runtime > searchCtls.getTimeLimit() )
                        {
                            throw new LdapTimeLimitExceededException();
                        }
                    }

                    if ( searchCtls.getCountLimit() > 0 )
                    {
                        if ( count > searchCtls.getCountLimit() )
                        {
                            throw new LdapSizeLimitExceededException();
                        }
                    }

                    count++;
                    return true;
                }
            } );
        }

        return cursor;
    }


    public EntryFilteringCursor search( SearchOperationContext opContext, Collection<String> byPassed )
            throws Exception
    {
        ensureStarted();
        opContext.setByPassed( byPassed );
        push( opContext );
        
        try
        {
            return service.getInterceptorChain().search( opContext );
        }
        finally
        {
            pop();
        }
    }


    public ClonedServerEntry lookup( LookupOperationContext opContext ) throws Exception
    {
        if ( opContext.getDn().size() == 0 )
        {
            List<String> attrs = opContext.getAttrsId();

            if ( ( attrs == null ) || ( attrs.size() == 0 ) )
            {
                synchronized ( ROOT_DSE_NOOP_MUTEX )
                {
                    if ( ROOT_DSE_NO_OPERATIONNAL == null )
                    {
                        ROOT_DSE_NO_OPERATIONNAL = lookup( opContext, null );
                    }
                }

                return new ClonedServerEntry( ROOT_DSE_NO_OPERATIONNAL );
            } 
            else if ( ( attrs.size() == 1 ) && ( attrs.contains( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) ) )
            {
                synchronized ( ROOT_DSE_ALL_MUTEX )
                {
                    if ( ROOT_DSE_ALL == null )
                    {
                        ROOT_DSE_ALL = lookup( opContext, null );
                    }
                }

                return new ClonedServerEntry( ROOT_DSE_ALL );
            }

        }

        return lookup( opContext, null );
    }


    public ClonedServerEntry lookup( LookupOperationContext opContext, Collection<String> byPassed ) throws Exception
    {
        ensureStarted();
        opContext.setByPassed( byPassed );
        push( opContext );
        
        try
        {
            return service.getInterceptorChain().lookup( opContext );
        }
        finally
        {
            pop();
        }
    }

    public boolean hasEntry( EntryOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            return service.getInterceptorChain().hasEntry( opContext );
        }
        finally
        {
            pop();
        }
    }


    public boolean hasEntry( EntryOperationContext opContext, Collection<String> byPassed ) throws Exception
    {
        opContext.setByPassed( byPassed );
        return hasEntry( opContext );
    }


    public void rename( RenameOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            service.getInterceptorChain().rename( opContext );
        }
        finally
        {
            pop();
        }
    }


    public void rename( RenameOperationContext opContext, Collection<String> byPassed ) throws Exception
    {
        opContext.setByPassed( byPassed );
        rename( opContext );
    }


    public void move( MoveOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            service.getInterceptorChain().move( opContext );
        }
        finally
        {
            pop();
        }
    }


    public void move( MoveOperationContext opContext, Collection<String> byPassed ) throws Exception
    {
        opContext.setByPassed( byPassed );
        move( opContext );
    }


    public void moveAndRename( MoveAndRenameOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            service.getInterceptorChain().moveAndRename( opContext );
        }
        finally
        {
            pop();
        }
    }


    public void moveAndRename( MoveAndRenameOperationContext opContext, Collection<String> byPassed )
            throws Exception
    {
        opContext.setByPassed( byPassed );
        moveAndRename( opContext );
    }


    public void unbind( UnbindOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            service.getInterceptorChain().unbind( opContext );
        }
        finally
        {
            pop();
        }
    }


    public void unbind( UnbindOperationContext opContext, Collection<String> byPassed ) throws Exception
    {
        opContext.setByPassed( byPassed );
        unbind( opContext );
    }


    /**
     * TODO : check if we can find another way to protect ourselves from recursion.
     *
     * @param opContext The operation context
     * @param byPassed bypass instructions to skip interceptors
     * @throws Exception if bind fails
     */
    public void bind( BindOperationContext opContext, Collection<String> byPassed )
            throws Exception
    {
        opContext.setByPassed( byPassed );
        bind( opContext );
    }

    
    public void bind( BindOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            service.getInterceptorChain().bind( opContext );
        }
        finally
        {
            pop();
        }
    }


    public ClonedServerEntry getRootDSE( GetRootDSEOperationContext opContext ) throws Exception
    {
        if ( opContext.getDn().size() == 0 )
        {
            synchronized ( ROOT_DSE_ALL_MUTEX )
            {
                if ( ROOT_DSE_ALL == null )
                {
                    ROOT_DSE_ALL = getRootDSE( opContext, null );
                }
            }

            return new ClonedServerEntry( ROOT_DSE_ALL );
        }

        return getRootDSE( opContext, null );
    }


    public ClonedServerEntry getRootDSE( GetRootDSEOperationContext opContext, Collection<String> byPassed )
            throws Exception
    {
        ensureStarted();
        opContext.setByPassed( byPassed );
        push( opContext );
        
        try
        {
            return service.getInterceptorChain().getRootDSE( opContext );
        }
        finally
        {
            pop();
        }
    }


    public void addContextPartition( AddContextPartitionOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            service.getInterceptorChain().addContextPartition( opContext );
        }
        finally
        {
            pop();
        }
    }


    public void addContextPartition( AddContextPartitionOperationContext opContext, Collection<String> byPassed )
            throws Exception
    {
        opContext.setByPassed( byPassed );
        addContextPartition( opContext );
    }


    public void removeContextPartition( RemoveContextPartitionOperationContext opContext ) throws Exception
    {
        ensureStarted();
        push( opContext );
        
        try
        {
            service.getInterceptorChain().removeContextPartition( opContext );
        }
        finally
        {
            pop();
        }
    }


    public void removeContextPartition( RemoveContextPartitionOperationContext opContext, Collection<String> byPassed )
            throws Exception
    {
        opContext.setByPassed( byPassed );
        removeContextPartition( opContext );
    }


    private void ensureStarted() throws ServiceUnavailableException
    {
        if ( !service.isStarted() )
        {
            throw new ServiceUnavailableException( "Directory service is not started." );
        }
    }


    public void registerSupportedExtensions( Set<String> extensionOids ) throws Exception
    {
        service.getPartitionNexus().registerSupportedExtensions( extensionOids );
    }


    public void registerSupportedSaslMechanisms( Set<String> supportedSaslMechanisms ) throws Exception
    {
        service.getPartitionNexus().registerSupportedSaslMechanisms( supportedSaslMechanisms );
    }


    public ClonedServerEntry lookup( Long id ) throws Exception
    {
        // TODO not implemented until we can lookup partition using the 
        // partition id component of the 64 bit identifier
        throw new NotImplementedException();
    }
}
