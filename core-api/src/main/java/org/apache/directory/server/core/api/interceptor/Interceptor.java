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
package org.apache.directory.server.core.api.interceptor;


import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDseOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.api.partition.Partition;


/**
 * Filters invocations on DefaultPartitionNexus. Interceptor
 * filters most method calls performed on DefaultPartitionNexus just
 * like Servlet filters do.
 *
 * <h2>Interceptor Chaining</h2>
 * 
 * Interceptors should usually pass the control
 * of current invocation to the next interceptor by calling an appropriate method
 * on NextInterceptor.  The flow control is returned when the next
 * interceptor's filter method returns. You can therefore implement pre-, post-,
 * around- invocation handler by how you place the statement.  Otherwise, you
 * can transform the invocation into other(s).
 * 
 * <h3>Pre-invocation Filtering</h3>
 * <pre>
 * public void delete( NextInterceptor nextInterceptor, Name name )
 * {
 *     System.out.println( "Starting invocation." );
 *     nextInterceptor.delete( name );
 * }
 * </pre>
 * 
 * <h3>Post-invocation Filtering</h3>
 * <pre>
 * public void delete( NextInterceptor nextInterceptor, Name name )
 * {
 *     nextInterceptor.delete( name );
 *     System.out.println( "Invocation ended." );
 * }
 * </pre>
 *
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
 * 
 * <h3>Transforming invocations</h3>
 * <pre>
 * public void delete( NextInterceptor nextInterceptor, Name name )
 * {
 *     // transform deletion into modification.
 *     Attribute mark = new AttributeImpl( "entryDeleted", "true" );
 *     nextInterceptor.modify( name, DirIteratorContext.REPLACE_ATTRIBUTE, mark );
 * }
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface Interceptor
{
    /**
     * Name that must be unique in an interceptor chain
     * @return name of this interceptor, must be unique in an interceptor chain.
     */
    String getName();


    /**
     * Intializes this interceptor.
     *  
     * @param directoryService The DirectoryService instance
     * @throws LdapException If the initialization failed
     */
    void init( DirectoryService directoryService ) throws LdapException;


    /**
     * Deinitializes this interceptor. 
     */
    void destroy();


    /**
     * Filters {@link Partition#add( AddOperationContext )} call.
     * 
     * @param addContext The {@link AddOperationContext} instance
     * @throws LdapException If we had some error while processing the Add operation
     */
    void add( AddOperationContext addContext ) throws LdapException;


    /**
     * Filters {@link BindOperationContext} call.
     * 
     * @param bindContext The {@link BindOperationContext} instance
     * @throws LdapException If we had some error while processing the Bind operation
     */
    void bind( BindOperationContext bindContext ) throws LdapException;


    /**
     * Filters Compare call.
     * 
     * @param compareContext The {@link CompareOperationContext} instance
     * @throws LdapException If we had some error while processing the Compare operation
     * @return <tt>true</tt> if teh comparaison is successful
     */
    boolean compare( CompareOperationContext compareContext ) throws LdapException;


    /**
     * Filters {@link Partition#delete( DeleteOperationContext )} call.
     * 
     * @param deleteContext The {@link DeleteOperationContext} instance
     * @throws LdapException If we had some error while processing the Delete operation
     */
    void delete( DeleteOperationContext deleteContext ) throws LdapException;


    /**
     * Filters getRootDse call.
     * 
     * @param getRootDseContext The getRootDSE operation context
     * @return The RootDSE entry, if found
     * @throws LdapException If we can't get back the RootDSE entry
     */
    Entry getRootDse( GetRootDseOperationContext getRootDseContext ) throws LdapException;


    /**
     * Filters {@link Partition#hasEntry( HasEntryOperationContext )} call.
     * 
     * @param hasEntryContext The {@link HasEntryOperationContext} instance
     * @throws LdapException If we had some error while processing the HasEntry operation
     * @return <tt>true</tt> f the entry is present in the DIT
     */
    boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException;


    /**
     * Filters {@link Partition#lookup( LookupOperationContext )} call.
     * 
     * @param lookupContext The {@link LookupOperationContext} instance
     * @throws LdapException If we had some error while processing the Lookup operation
     * @return The found entry
     */
    Entry lookup( LookupOperationContext lookupContext ) throws LdapException;


    /**
     * Filters {@link Partition#modify( ModifyOperationContext )} call.
     * 
     * @param modifyContext The {@link ModifyOperationContext} instance
     * @throws LdapException If we had some error while processing the Modify operation
     */
    void modify( ModifyOperationContext modifyContext ) throws LdapException;


    /**
     * Filters {@link Partition#move( MoveOperationContext )} call.
     * 
     * @param moveContext The {@link MoveOperationContext} instance
     * @throws LdapException If we had some error while processing the Move operation
     */
    void move( MoveOperationContext moveContext ) throws LdapException;


    /**
     * Filters MoveAndRename call.
     * 
     * @param moveAndRenameContext The {@link MoveAndRenameOperationContext} instance
     * @throws LdapException If we had some error while processing the MoveAndRename operation
     */
    void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException;


    /**
     * Filters {@link Partition#rename( RenameOperationContext )} call.
     * 
     * @param renameContext The {@link RenameOperationContext} instance
     * @throws LdapException If we had some error while processing the Rename operation
     */
    void rename( RenameOperationContext renameContext ) throws LdapException;


    /**
     * Filters {@link Partition#search( SearchOperationContext )} call.
     * 
     * @param searchContext The {@link SearchOperationContext} instance
     * @throws LdapException If we had some error while processing the Search operation
     * @return A cursror over the found entries
     */
    EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException;


    /**
     * Filters {@link Partition#unbind( UnbindOperationContext )} call.
     * 
     * @param unbindContext The {@link UnbindOperationContext} instance
     * @throws LdapException If we had some error while processing the Unbind operation
     */
    void unbind( UnbindOperationContext unbindContext ) throws LdapException;
}
