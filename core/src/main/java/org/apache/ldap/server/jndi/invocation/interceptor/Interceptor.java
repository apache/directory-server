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
package org.apache.ldap.server.jndi.invocation.interceptor;

import java.util.Properties;

import javax.naming.NamingException;

import org.apache.ldap.server.jndi.invocation.Invocation;

/**
 * Filters any directory operations.  You can filter any
 * {@link Invocation}s performed on {@link BackingStore}s just like Servlet
 * filters do.
 * 
 * <h2>Interceptor Naming Convention</h2>
 * <p>
 * When you create an implementation of Interceptor, you have to follow
 * the basic class naming convention to avoid others' confusion:
 * <ul>
 *   <li>Class name must be an agent noun or end with '<code>Interceptor</code>'.</li>
 *   <li>If the role of the interceptor is to add or modify attributes of
 *       entries, class name must end with '<code>Tagger</code>.' (e.g. EntryUUIDTagger)</li>
 * </ul>
 * Plus, placing your interceptor implementations to packages like
 * '<code>interceptor</code>' would be the best practice.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 * 
 * @see InvocationChain
 */
public interface Interceptor
{
    /**
     * Intializes this interceptor.  This is invoked by directory service
     * provider when this intercepter is loaded into interceptor chain.
     * 
     * @param config the configuration properties for this interceptor
     * @throws NamingException if failed to initialize this interceptor
     */
    void init( Properties config ) throws NamingException;

    /**
     * Deinitialized this interceptor.  This is invoked by directory service
     * provider when this intercepter is unloaded from interceptor chain.
     */
    void destroy();

    /**
     * Filters a particular invocation.  You can pass control to
     * <code>nextInterceptor</code> by calling {@link NextInterceptor#invoke(Invocation)}. 
     *
     * @param nextInterceptor the next interceptor in the interceptor chain
     * @param invocation the invocation to process
     * @throws NamingException on failures while handling the invocation
     */
    void process( NextInterceptor nextInterceptor, Invocation invocation )
            throws NamingException;
}
