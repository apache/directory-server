/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.config.beans;


/**
 * A class used to store the Interceptors configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class InterceptorBean implements Comparable<InterceptorBean>
{
    /** The Interceptor ID */
    private String id;
    
    /** The interceptor FQCN */
    private String fqcn;
    
    /** The interceptor position in the chain */
    private int order;


    /**
     * Creates a new InterceptorBean
     */
    public InterceptorBean()
    {
    }


    /**
     * Compares the order of the interceptor with the given one
     * @param o An interceptor 
     * @return -1 if the current interceptor is below the given one, 1 if 
     * it's above, 0 if it's equal
     */
    public int compareTo( InterceptorBean o )
    {
        if ( order > o.order )
        {
            return 1;
        }
        else if ( order < o.order )
        {
            return -1;
        }

        return 0;
    }


    /**
     * @return the id
     */
    public String getId() 
    {
        return id;
    }


    /**
     * @param id the id to set
     */
    public void setId( String id ) 
    {
        this.id = id;
    }


    /**
     * @return the order
     */
    public int getOrder() 
    {
        return order;
    }


    /**
     * @param order the order to set
     */
    public void setOrder( int order ) 
    {
        this.order = order;
    }


    /**
     * @return the fqcn
     */
    public String getFqcn()
    {
        return fqcn;
    }


    /**
     * @param fqcn the fqcn to set
     */
    public void setFqcn( String fqcn )
    {
        this.fqcn = fqcn;
    }
}
