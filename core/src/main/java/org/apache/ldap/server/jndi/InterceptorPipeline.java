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


import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingException;


/**
 * An Interceptor composed of a pipeline of interceptors that can be called 
 * sequentially with one invokation.
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class InterceptorPipeline implements Interceptor
{
    /** The List of Interceptors */
    private List list = new ArrayList();
    
    
    /**
     * Adds a Interceptor to this InterceptorPipeline at a specific position.
     *
     * @param posn the 0 index based position
     * @param service the Interceptor to add.
     */
    public void add( int posn, Interceptor service )
    {
        list.add( posn, service );
    }


    /**
     * Adds a Interceptor to the end of this InterceptorPipeline.
     *
     * @param service the Interceptor to add.
     */
    public void add( Interceptor service )
    {
        list.add( service );
    }


    /**
     * Removes an Interceptor from this InterceptorPipeline.
     *
     * @param posn the 0 index based position
     * @return the removed Interceptor
     */
    public Interceptor remove( int posn )
    {
        return ( Interceptor ) list.remove( posn );
    }
    
    
    /**
     * Gets the Interceptor in this InterceptorPipeline at a specified.
     *
     * @param posn the 0 index based position
     * @return the Interceptor at posn
     */
    public Interceptor get( int posn )
    {
        return ( Interceptor ) list.get( posn );
    }
    
    
    /**
     * Gets the list of interceptors in this pipeline.
     *
     * @return the list of interceptors in this pipeline.
     */
    protected List getList()
    {
        return list;
    }

    
    // ------------------------------------------------------------------------
    // Abstract Methods 
    // ------------------------------------------------------------------------
    
    
    /**
     * Invokes this InterceptorPipeline which sequencially begins the chain
     * of Interceptor invocations on the Interceptors with it. 
     *
     * @see Interceptor#invoke(Invocation)
     */
    public abstract void invoke( Invocation invocation ) throws NamingException;

    /**
     * Get whether this pipeline is fail fast or not.
     * 
     * @return whether or not this pipeline is failfast
     */
    public abstract boolean isFailFast();
}
