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
package org.apache.ldap.server.jndi;


import javax.naming.NamingException;


/**
 * The Interceptor is a component through which invocations pass thru.  In 
 * most cases the invocations pass thru a series of Interceptor objects 
 * before the target object is invoked.
 * 
 * Got this idea from a class written by Peter Donald who originally wrote it
 * for XInvoke in the Spice Project at Codehaus.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Interceptor
{
    /**
     * Process a particular invocation.  The method must try to catch and
     * rethrow exceptions as EveInterceptorExceptions and any other exceptions
     * will be caught and placed into the invocation via
     * {@link Invocation#setThrowable} or {@link Invocation#addFailure(Throwable)}.
     *
     * <p>Note: most Interceptors pass control to the next Interceptor in the
     * series.</p>
     *
     * @param invocation the invocation to process
     * @throws NamingException on failures while handling the invokation
     */
    void invoke( Invocation invocation ) throws NamingException;
}
