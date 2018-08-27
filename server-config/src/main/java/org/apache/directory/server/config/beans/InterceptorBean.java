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


import org.apache.directory.server.config.ConfigurationElement;


/**
 * A class used to store the Interceptors configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class InterceptorBean extends AdsBaseBean implements Comparable<InterceptorBean>
{
    /** The Interceptor ID */
    @ConfigurationElement(attributeType = "ads-interceptorId", isRdn = true)
    private String interceptorId;

    /** The interceptor FQCN */
    @ConfigurationElement(attributeType = "ads-interceptorClassName")
    private String interceptorClassName;

    /** The interceptor position in the chain */
    @ConfigurationElement(attributeType = "ads-interceptorOrder")
    private int interceptorOrder;


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
        if ( interceptorOrder > o.interceptorOrder )
        {
            return 1;
        }
        else if ( interceptorOrder < o.interceptorOrder )
        {
            return -1;
        }

        return 0;
    }


    /**
     * @return the id
     */
    public String getInterceptorId()
    {
        return interceptorId;
    }


    /**
     * @param id the id to set
     */
    public void setInterceptorId( String id )
    {
        this.interceptorId = id;
    }


    /**
     * @return the interceptor Order
     */
    public int getInterceptorOrder()
    {
        return interceptorOrder;
    }


    /**
     * @param interceptorOrder the interceptor Order to set
     */
    public void setInterceptorOrder( int interceptorOrder )
    {
        this.interceptorOrder = interceptorOrder;
    }


    /**
     * @return the interceptor ClassName
     */
    public String getInterceptorClassName()
    {
        return interceptorClassName;
    }


    /**
     * @param interceptorClassName the interceptor ClassName to set
     */
    public void setInterceptorClassName( String interceptorClassName )
    {
        this.interceptorClassName = interceptorClassName;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "interceptor[" ).append( interceptorOrder ).append( "] : " ).append( '\n' );
        sb.append( tabs ).append( "  interceptor id : " ).append( interceptorId ).append( '\n' );
        sb.append( tabs ).append( "  class name : " ).append( interceptorClassName ).append( '\n' );

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return toString( "" );
    }
}
