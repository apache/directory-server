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


import javax.naming.NamingException;


/**
 * A {@link NamingException} that wraps uncaught runtime exceptions thrown
 * from {@link Interceptor}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InterceptorException extends NamingException
{
    private static final long serialVersionUID = 3258690996517746233L;

    /**
     * The Interceptor causing the failure
     */
    private final Interceptor interceptor;


    /**
     * Creates an InterceptorException without a message.
     *
     * @param interceptor the Interceptor causing the failure
     */
    public InterceptorException( Interceptor interceptor )
    {
        this.interceptor = interceptor;
    }


    /**
     * Creates an InterceptorException with a custom message.
     *
     * @param interceptor the Interceptor causing the failure
     * @param explanation String explanation of why the Interceptor failed
     */
    public InterceptorException( Interceptor interceptor, String explanation )
    {
        super( explanation );
        this.interceptor = interceptor;
    }


    /**
     * Creates an InterceptorException without a message.
     *
     * @param interceptor the Interceptor causing the failure
     * @param rootCause   the root cause of this exception
     */
    public InterceptorException( Interceptor interceptor, Throwable rootCause )
    {
        this( interceptor );
        super.setRootCause( rootCause );
    }


    /**
     * Creates an InterceptorException without a message.
     *
     * @param interceptor the Interceptor causing the failure
     * @param explanation String explanation of why the Interceptor failed
     * @param rootCause   the root cause of this exception
     */
    public InterceptorException( Interceptor interceptor, String explanation,
                                 Throwable rootCause )
    {
        this( interceptor, explanation );
        super.setRootCause( rootCause );
    }


    /**
     * Gets the interceptor this exception is associated with.
     *
     * @return the interceptor this exception is associated with
     */
    public Interceptor getInterceptor()
    {
        return interceptor;
    }
}
