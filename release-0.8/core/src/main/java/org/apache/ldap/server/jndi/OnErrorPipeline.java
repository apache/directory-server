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

import org.apache.ldap.server.exception.InterceptorException;


/**
 * A slow failing InterceptorPipeline implementation where all Interceptors 
 * within the invocation chain are invoked regardless of errors upstream - 
 * exceptions are added to the list of exceptions on the Invocation.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class OnErrorPipeline extends InterceptorPipeline
{
    /**
     * Returns false all the time! 
     *
     * @see InterceptorPipeline#isFailFast()
     */
    public final boolean isFailFast() 
    {
        return false;
    }


    /**
     * This invoke method does not fail and throw exceptions until all 
     * Interceptors within the pipeline have been invoked.  If an unexpected 
     * Throwable other than an InterceptorException results this method
     * catches it and wraps it within an InterceptorException and adds it to
     * the Invocation's list of afterFailure exceptions.  The last error if any
     * to result after all Interceptors have run is thrown.
     *
     * @see Interceptor#invoke(Invocation)
     */
    public void invoke( Invocation invocation ) throws NamingException
    {
        NamingException last = null;
        
        for ( int ii = 0; ii < getList().size(); ii++ )
        {
            Interceptor service = ( Interceptor ) getList().get( ii );
            
            try
            {
                service.invoke( invocation );
            }
            catch ( Throwable throwable )
            {
                /*
                 * If exception is InterceptorException we add it to the list
                 * of afterFailure exceptions on the Invocation.  Otherwise we
                 * wrap the unexpected exception as an InterceptorException and
                 * add it to the list of afterFailure exceptions on the 
                 * Invocation
                 */

                if ( throwable instanceof InterceptorException )
                {
                    last = ( InterceptorException ) throwable;
                    invocation.addFailure( last );
                }
                else
                {
                    last = new InterceptorException( service, invocation );
                    last.setRootCause( throwable );
                    invocation.addFailure( last );
                }
            }
        }
        
        // Throw the last exception if any after all Interceptors are invoked
        if ( null != last )
        {
            throw last;
        }
    }
}
