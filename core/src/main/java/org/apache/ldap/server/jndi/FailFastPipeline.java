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

import org.apache.ldap.server.exception.EveInterceptorException;


/**
 * A fast failing InterceptorPipeline implementation where the first Interceptor
 * to fail within the invocation chain of the pipeline shorts the invocation of
 * interceptors downstream of the error.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class FailFastPipeline extends InterceptorPipeline
{
    /**
     * Returns true all the time! 
     *
     * @see InterceptorPipeline#isFailFast()
     */
    public final boolean isFailFast() 
    {
        return true;
    }


    /**
     * This invoke method fails and throws at the first failure within the 
     * pipeline.  If an unexpected Throwable other than an EveInterceptorException
     * results this method catches it and wraps it within an 
     * EveInterceptorException and rethrows the new exception.
     *
     * @see Interceptor#invoke(Invocation)
     */
    public void invoke( Invocation invocation ) throws NamingException
    {
        for ( int ii = 0; ii < getList().size(); ii++ )
        {
            Interceptor service = ( Interceptor ) getList().get( ii );
            
            try
            {
                service.invoke( invocation );
            }
            catch ( Throwable throwable )
            {
                if ( throwable instanceof NamingException )
                {
                    throw ( NamingException ) throwable;
                }
                
                EveInterceptorException ie;
                ie = new EveInterceptorException( service, invocation );
                ie.setRootCause( throwable );
                throw ie;
            }
        }
    }
}
