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
package org.apache.directory.server.core.interceptor;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.context.*;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import java.util.Iterator;


/**
 * Filters invocations on {@link PartitionNexus}.  {@link Interceptor}
 * filters most method calls performed on {@link PartitionNexus} just
 * like Servlet filters do.
 * <p/>
 * <h2>Interceptor Chaining</h2>
 * 
 * Interceptors should usually pass the control
 * of current invocation to the next interceptor by calling an appropriate method
 * on {@link NextInterceptor}.  The flow control is returned when the next 
 * interceptor's filter method returns. You can therefore implement pre-, post-,
 * around- invocation handler by how you place the statement.  Otherwise, you
 * can transform the invocation into other(s).
 * <p/>
 * <h3>Pre-invocation Filtering</h3>
 * <pre>
 * public void delete( NextInterceptor nextInterceptor, Name name )
 * {
 *     System.out.println( "Starting invocation." );
 *     nextInterceptor.delete( name );
 * }
 * </pre>
 * <p/>
 * <h3>Post-invocation Filtering</h3>
 * <pre>
 * public void delete( NextInterceptor nextInterceptor, Name name )
 * {
 *     nextInterceptor.delete( name );
 *     System.out.println( "Invocation ended." );
 * }
 * </pre>
 * <p/>
 * <h3>Around-invocation Filtering</h3>
 * <pre>
 * public void delete( NextInterceptor nextInterceptor, Name name )
 * {
 *     long startTime = System.currentTimeMillis();
 *     try
 *     {
 *         nextInterceptor.delete( name );
 *     }
 *     finally
 *     {
 *         long endTime = System.currentTimeMillis();
 *         System.out.println( ( endTime - startTime ) + "ms elapsed." );
 *     }
 * }
 * </pre>
 * <p/>
 * <h3>Transforming invocations</h3>
 * <pre>
 * public void delete( NextInterceptor nextInterceptor, Name name )
 * {
 *     // transform deletion into modification.
 *     Attribute mark = new AttributeImpl( "entryDeleted", "true" );
 *     nextInterceptor.modify( name, DirContext.REPLACE_ATTRIBUTE, mark );
 * }
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 * @see NextInterceptor
 */
public interface Interceptor
{
    /**
     * Name that must be unique in an interceptor chain
     * @return name of this interceptor, must be unique in an interceptor chain.
     */
    String getName();

    /**
     * Intializes this interceptor.  This is invoked by {@link InterceptorChain}
     * when this intercepter is loaded into interceptor chain.
     */
    void init( DirectoryService directoryService ) throws NamingException;


    /**
     * Deinitializes this interceptor.  This is invoked by {@link InterceptorChain}
     * when this intercepter is unloaded from interceptor chain.
     */
    void destroy();


    /**
     * Filters {@link PartitionNexus#getRootDSE( GetRootDSEOperationContext )} call.
     */
    Attributes getRootDSE( NextInterceptor next, GetRootDSEOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#getMatchedName( GetMatchedNameOperationContext )} call.
     */
    LdapDN getMatchedName( NextInterceptor next, GetMatchedNameOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#getSuffix( GetSuffixOperationContext )} call.
     */
    LdapDN getSuffix ( NextInterceptor next, GetSuffixOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#listSuffixes( ListSuffixOperationContext )} call.
     */
    Iterator<String> listSuffixes( NextInterceptor next, ListSuffixOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#addContextPartition( AddContextPartitionOperationContext )} call.
     */
    void addContextPartition( NextInterceptor next, AddContextPartitionOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#removeContextPartition( RemoveContextPartitionOperationContext )} call.
     */
    void removeContextPartition( NextInterceptor next, RemoveContextPartitionOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#compare( CompareOperationContext )} call.
     */
    boolean compare( NextInterceptor next, CompareOperationContext opContext) throws NamingException;


    /**
     * Filters {@link Partition#delete( DeleteOperationContext )} call.
     */
    void delete( NextInterceptor next, DeleteOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#add( AddOperationContext )} call.
     */
    void add( NextInterceptor next, AddOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#modify( ModifyOperationContext )} call.
     */
    void modify( NextInterceptor next, ModifyOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#list( ListOperationContext )} call.
     */
    NamingEnumeration<SearchResult> list( NextInterceptor next, ListOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#search( SearchOperationContext )} call.
     */
    NamingEnumeration<SearchResult> search( NextInterceptor next, SearchOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#lookup( LookupOperationContext )} call.
     */
    Attributes lookup( NextInterceptor next, LookupOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#hasEntry( EntryOperationContext )} call.
     */
    boolean hasEntry( NextInterceptor next, EntryOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#rename( RenameOperationContext )} call.
     */
    void rename( NextInterceptor next, RenameOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#move( MoveOperationContext )} call.
     */
    void move( NextInterceptor next, MoveOperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#moveAndRename( MoveAndRenameOperationContext) } call.
     */
    void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext )
        throws NamingException;

    /**
     * Filters {@link Partition#bind( BindOperationContext )} call.
     */
    void bind( NextInterceptor next, BindOperationContext opContext )
        throws NamingException;

    /**
     * Filters {@link Partition#unbind( UnbindOperationContext )} call.
     */
    void unbind( NextInterceptor next, UnbindOperationContext opContext ) throws NamingException;
}
