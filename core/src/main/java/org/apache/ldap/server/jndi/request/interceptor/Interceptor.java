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
package org.apache.ldap.server.jndi.request.interceptor;

import java.util.Properties;

import javax.naming.NamingException;

import org.apache.ldap.server.jndi.request.Request;

/**
 * Processes or filters any directory operations.  You can intercept the
 * {@link Invocation}s and perform 'before', 'after', 'around' any any other
 * filtering operations.
 *
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
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
     * Process a particular invocation.  You can pass control to
     * <code>nextInterceptor</code> by invoking {@link #invoke(RequestProcessor, Invocation)}. 
     *
     * @param nextProcessor the next processor in the processor chain
     * @param invocation the invocation to process
     * @throws NamingException on failures while handling the invocation
     */
    void process( NextInterceptor nextProcessor, Request request )
            throws NamingException;
}
