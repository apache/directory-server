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


import org.apache.ldap.server.invocation.Invocation;

import javax.naming.NamingException;


/**
 * Represents the next {@link org.apache.ldap.server.interceptor.Interceptor} in the interceptor chain.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 * @see org.apache.ldap.server.interceptor.Interceptor
 * @see org.apache.ldap.server.interceptor.InterceptorChain
 */
public interface NextInterceptor
{
    /**
     * Passes the control of current invocation to the next {@link org.apache.ldap.server.interceptor.Interceptor} in the {@link org.apache.ldap.server.interceptor.InterceptorChain}.
     *
     * @param incovation
     * @throws NamingException
     */
    void process( Invocation incovation ) throws NamingException;
}
