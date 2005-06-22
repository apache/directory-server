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
package org.apache.ldap.server.interceptor;


import java.util.Iterator;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.server.configuration.InterceptorConfiguration;
import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.invocation.Invocation;
import org.apache.ldap.server.jndi.ContextFactoryConfiguration;
import org.apache.ldap.server.partition.ContextPartition;


/**
 * Filters any directory operations.  You can filter any {@link Invocation}
 * performed on {@link ContextPartition}s just like Servlet filters do.
 * <p/>
 * <h2>Interceptor Chaining</h2> Interceptors should usually pass the control
 * of current invocation to the next interceptor by calling
 * {@link NextInterceptor#process(Invocation)}. The flow control is returned
 * when the next interceptor's {@link Interceptor#process(NextInterceptor, Invocation)}
 * returns. You can therefore implement pre-, post-, around- invocation handler
 * by how you place the statement.
 * <p/>
 * <h3>Pre-invocation Filtering</h3>
 * <pre>
 * public void process( NextInterceptor nextInterceptor, Invocation invocation )
 * {
 *     System.out.println( "Starting invocation." );
 *     nextInterceptor.process( invocation );
 * }
 * </pre>
 * <p/>
 * <h3>Post-invocation Filtering</h3>
 * <pre>
 * public void process( NextInterceptor nextInterceptor, Invocation invocation )
 * {
 *     nextInterceptor.process( invocation );
 *     System.out.println( "Invocation ended." );
 * }
 * </pre>
 * <p/>
 * <h3>Around-invocation Filtering</h3>
 * <pre>
 * public void process( NextInterceptor nextInterceptor, Invocation invocation )
 * {
 *     long startTime = System.currentTimeMillis();
 *     try
 *     {
 *         nextInterceptor.process( invocation );
 *     }
 *     finally
 *     {
 *         long endTime = System.currentTimeMillis();
 *         System.out.println( ( endTime - startTime ) + "ms elapsed." );
 *     }
 * }
 * </pre>
 * <p/>
 * <h2>Interceptor Naming Convention</h2>
 * <p/>
 * When you create an implementation of Interceptor, you have to follow the
 * basic class naming convention to avoid others' confusion:
 * <ul>
 *  <li>Class name must be an agent noun or end with <code>Interceptor</code> or
 * <code>Service</code>.</li>
 * </ul>
 * Plus, placing your interceptor implementations into relavent packages like
 * <code>interceptor</code> or ones that reflect its purpose would be a good
 * practice.
 * <p/>
 * <h2>Overriding Default Interceptor Settings</h2>
 * <p/>
 * See {@link StartupConfiguration}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 * @see NextInterceptor
 */
public interface Interceptor
{
    /**
     * Intializes this interceptor.  This is invoked by directory
     * service provider when this intercepter is loaded into interceptor chain.
     *
     * @param context the configuration properties for this interceptor
     * @throws NamingException if failed to initialize this interceptor
     */
    void init( ContextFactoryConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException;


    /**
     * Deinitializes this interceptor.  This is invoked by directory
     * service provider when this intercepter is unloaded from interceptor chain.
     */
    void destroy();

    Attributes getRootDSE( NextInterceptor next ) throws NamingException; 
    Name getMatchedDn( NextInterceptor next, Name dn, boolean normalized ) throws NamingException;
    Name getSuffix( NextInterceptor next, Name dn, boolean normalized ) throws NamingException;
    Iterator listSuffixes( NextInterceptor next, boolean normalized ) throws NamingException;
    void delete( NextInterceptor next, Name name ) throws NamingException;
    void add( NextInterceptor next, String upName, Name normName, Attributes entry ) throws NamingException;
    void modify( NextInterceptor next, Name name, int modOp, Attributes mods ) throws NamingException;
    void modify( NextInterceptor next, Name name, ModificationItem [] mods ) throws NamingException;
    NamingEnumeration list( NextInterceptor next, Name base ) throws NamingException;
    NamingEnumeration search( NextInterceptor next, Name base, Map env, ExprNode filter,
                              SearchControls searchCtls ) throws NamingException;
    Attributes lookup( NextInterceptor next, Name name ) throws NamingException;
    Attributes lookup( NextInterceptor next, Name dn, String [] attrIds ) throws NamingException;
    boolean hasEntry( NextInterceptor next, Name name ) throws NamingException;
    boolean isSuffix( NextInterceptor next, Name name ) throws NamingException;
    void modifyRn( NextInterceptor next, Name name, String newRn, boolean deleteOldRn ) throws NamingException;
    void move( NextInterceptor next, Name oriChildName, Name newParentName ) throws NamingException;
    void move( NextInterceptor next, Name oriChildName, Name newParentName, String newRn,
               boolean deleteOldRn ) throws NamingException;
}
