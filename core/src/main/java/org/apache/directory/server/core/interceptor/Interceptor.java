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


import java.util.Iterator;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.name.LdapDN;


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
     * Intializes this interceptor.  This is invoked by {@link InterceptorChain}
     * when this intercepter is loaded into interceptor chain.
     */
    void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException;


    /**
     * Deinitializes this interceptor.  This is invoked by {@link InterceptorChain}
     * when this intercepter is unloaded from interceptor chain.
     */
    void destroy();


    /**
     * Filters {@link PartitionNexus#getRootDSE( OperationContext )} call.
     */
    Attributes getRootDSE( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#getMatchedName( OperationContext )} call.
     */
    LdapDN getMatchedName( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#getSuffix( OperationContext )} call.
     */
    LdapDN getSuffix ( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#listSuffixes( OperationContext )} call.
     */
    Iterator listSuffixes( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#addContextPartition( OperationContext )} call.
     */
    void addContextPartition( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#removeContextPartition( OperationContext )} call.
     */
    void removeContextPartition( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#compare( OperationContext )} call.
     */
    boolean compare( NextInterceptor next, OperationContext opContext) throws NamingException;


    /**
     * Filters {@link Partition#delete( OperationContext )} call.
     */
    void delete( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#add( OperationContext )} call.
     */
    void add( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#modify( OperationContext )} call.
     */
    void modify( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#list( OperationContext )} call.
     */
    NamingEnumeration list( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#search( OperationContext )} call.
     */
    NamingEnumeration<SearchResult> search( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#lookup( OperationContext )} call.
     */
    Attributes lookup( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#hasEntry( OperationContext )} call.
     */
    boolean hasEntry( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#rename( OperationContext )} call.
     */
    void rename( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#move( OperationContext )} call.
     */
    void move( NextInterceptor next, OperationContext opContext ) throws NamingException;


    /**
     * Filters {@link Partition#moveAndRename( OperationContext) } call.
     */
    void moveAndRename( NextInterceptor next, OperationContext opContext )
        throws NamingException;

    /**
     * Filters {@link Partition#bind( OperationContext )} call.
     */
    void bind( NextInterceptor next, OperationContext opContext )
        throws NamingException;

    /**
     * Filters {@link Partition#unbind( OperationContext )} call.
     */
    void unbind( NextInterceptor next, OperationContext opContext ) throws NamingException;
}
