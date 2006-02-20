/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.interceptor;


import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.DirectoryPartitionConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.partition.DirectoryPartition;
import org.apache.directory.server.core.partition.DirectoryPartitionNexus;
import org.apache.directory.shared.ldap.filter.ExprNode;


/**
 * Filters invocations on {@link DirectoryPartitionNexus}.  {@link Interceptor}
 * filters most method calls performed on {@link DirectoryPartitionNexus} just
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
 *     Attribute mark = new BasicAttribute( "entryDeleted", "true" );
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
     * Filters {@link DirectoryPartitionNexus#getRootDSE()} call.
     */
    Attributes getRootDSE( NextInterceptor next ) throws NamingException;


    /**
     * Filters {@link DirectoryPartitionNexus#getMatchedName(Name, boolean)} call.
     */
    Name getMatchedName( NextInterceptor next, Name name, boolean normalized ) throws NamingException;


    /**
     * Filters {@link DirectoryPartitionNexus#getSuffix(Name, boolean)} call.
     */
    Name getSuffix( NextInterceptor next, Name name, boolean normalized ) throws NamingException;


    /**
     * Filters {@link DirectoryPartitionNexus#listSuffixes(boolean)} call.
     */
    Iterator listSuffixes( NextInterceptor next, boolean normalized ) throws NamingException;


    /**
     * Filters {@link DirectoryPartitionNexus#addContextPartition(DirectoryPartitionConfiguration)} call.
     */
    void addContextPartition( NextInterceptor next, DirectoryPartitionConfiguration cfg ) throws NamingException;


    /**
     * Filters {@link DirectoryPartitionNexus#removeContextPartition(Name)} call.
     */
    void removeContextPartition( NextInterceptor next, Name suffix ) throws NamingException;


    /**
     * Filters {@link DirectoryPartitionNexus#compare(Name,String,Object)} call.
     */
    boolean compare( NextInterceptor next, Name name, String oid, Object value ) throws NamingException;


    /**
     * Filters {@link DirectoryPartition#delete(Name)} call.
     */
    void delete( NextInterceptor next, Name name ) throws NamingException;


    /**
     * Filters {@link DirectoryPartition#add(String, Name, Attributes)} call.
     */
    void add( NextInterceptor next, String userProvidedName, Name normalizedName, Attributes entry )
        throws NamingException;


    /**
     * Filters {@link DirectoryPartition#modify(Name, int, Attributes)} call.
     */
    void modify( NextInterceptor next, Name name, int modOp, Attributes attributes ) throws NamingException;


    /**
     * Filters {@link DirectoryPartition#modify(Name, ModificationItem[])} call.
     */
    void modify( NextInterceptor next, Name name, ModificationItem[] items ) throws NamingException;


    /**
     * Filters {@link DirectoryPartition#list(Name)} call.
     */
    NamingEnumeration list( NextInterceptor next, Name baseName ) throws NamingException;


    /**
     * Filters {@link DirectoryPartition#search(Name, Map, ExprNode, SearchControls)} call.
     */
    NamingEnumeration search( NextInterceptor next, Name baseName, Map environment, ExprNode filter,
        SearchControls searchControls ) throws NamingException;


    /**
     * Filters {@link DirectoryPartition#lookup(Name)} call.
     */
    Attributes lookup( NextInterceptor next, Name name ) throws NamingException;


    /**
     * Filters {@link DirectoryPartition#lookup(Name, String[])} call.
     */
    Attributes lookup( NextInterceptor next, Name dn, String[] attrIds ) throws NamingException;


    /**
     * Filters {@link DirectoryPartition#lookup(Name, String[])} call.
     */
    boolean hasEntry( NextInterceptor next, Name name ) throws NamingException;


    /**
     * Filters {@link DirectoryPartition#isSuffix(Name)} call.
     */
    boolean isSuffix( NextInterceptor next, Name name ) throws NamingException;


    /**
     * Filters {@link DirectoryPartition#modifyRn(Name, String, boolean)} call.
     */
    void modifyRn( NextInterceptor next, Name name, String newRn, boolean deleteOldRn ) throws NamingException;


    /**
     * Filters {@link DirectoryPartition#move(Name, Name)} call.
     */
    void move( NextInterceptor next, Name oldName, Name newParentName ) throws NamingException;


    /**
     * Filters {@link DirectoryPartition#move(Name, Name, String, boolean)} call.
     */
    void move( NextInterceptor next, Name oldName, Name newParentName, String newRn, boolean deleteOldRn )
        throws NamingException;


    /**
     * Filters {@link DirectoryPartition#bind(Name, byte[], List, String)} call.
     */
    void bind( NextInterceptor next, Name bindDn, byte[] credentials, List mechanisms, String saslAuthId )
        throws NamingException;


    /**
     * Filters {@link DirectoryPartition#unbind(Name)} call.
     */
    void unbind( NextInterceptor next, Name bindDn ) throws NamingException;
}
