/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.configuration;


import org.apache.directory.server.core.interceptor.Interceptor;


/**
 * A configuration for {@link Interceptor}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InterceptorConfiguration
{
    private String name;
    private Interceptor interceptor;


    /**
     * Creates a new instance.
     */
    protected InterceptorConfiguration()
    {
    }


    /**
     * Returns the {@link Interceptor} that this configuration
     * configures.
     */
    public Interceptor getInterceptor()
    {
        return interceptor;
    }


    /**
     * Sets the {@link Interceptor} that this configuration
     * configures.
     */
    protected void setInterceptor( Interceptor authenticator )
    {
        this.interceptor = authenticator;
    }


    /**
     * Returns the name of the {@link Interceptor}.
     */
    public String getName()
    {
        return name;
    }


    /**
     * Sets the name of the {@link Interceptor}.
     */
    protected void setName( String name )
    {
        this.name = name.trim();
    }


    /**
     * Validates this configuration.
     *
     * @throws ConfigurationException if this configuration is not valid.
     */
    public void validate()
    {
        if ( name == null )
        {
            throw new ConfigurationException( "Name is not specified." );
        }

        if ( interceptor == null )
        {
            throw new ConfigurationException( "Interceptor is not specified." );
        }
    }
}
