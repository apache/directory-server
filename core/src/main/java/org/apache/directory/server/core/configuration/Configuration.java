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


import java.io.Serializable;
import java.util.Hashtable;

import org.apache.directory.server.core.DirectoryService;


/**
 * A configuration that provides required, optional, or default properties
 * to configure {@link DirectoryService}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class Configuration implements Cloneable, Serializable
{
    /**
     * A JNDI environment key that configuration instance is put on. 
     */
    public static final String JNDI_KEY = Configuration.class.getName();

    /**
     * The default ID of {@link DirectoryService} that is used
     * when no instance ID is specified. 
     */
    public static final String DEFAULT_INSTANCE_ID = "default";


    /**
     * Gets {@link Configuration} instance from the specified JNDI environment
     * {@link Hashtable}.  If a configuration instance is not present the default
     * StartupConfiguration is returned and injected into the environment.
     * 
     * @throws ConfigurationException if the specified environment doesn't
     *                                contain the proper configuration instance.
     */
    public static Configuration toConfiguration( Hashtable jndiEnvironment )
    {
        Object value = jndiEnvironment.get( JNDI_KEY );
        
        if ( value == null )
        {
            MutableStartupConfiguration msc = new MutableStartupConfiguration();
            jndiEnvironment.put( JNDI_KEY, msc );
            return msc;
        }
        
        if ( !( value instanceof Configuration ) )
        {
            throw new ConfigurationException( "Not an ApacheDS configuration: " + value );
        }

        return ( Configuration ) value;
    }

    private String instanceId = DEFAULT_INSTANCE_ID;


    /**
     * Creates a new instance.
     */
    protected Configuration()
    {
    }


    /**
     * Returns the ID of {@link DirectoryService} instance to configure.
     */
    public String getInstanceId()
    {
        return instanceId;
    }


    /**
     * Sets the ID of {@link DirectoryService} instance to configure.
     */
    protected void setInstanceId( String instanceId )
    {
        instanceId = instanceId.trim();
        this.instanceId = instanceId;
    }


    /**
     * Validates this configuration.
     * @throws ConfigurationException if this configuration is not valid
     */
    public void validate()
    {
    }


    /**
     * Converts this configuration to JNDI environment {@link Hashtable}.
     * This method simple returns a {@link Hashtable} that contains an entry
     * whose key is {@link #JNDI_KEY} and whose value is <tt>this</tt>.
     */
    public Hashtable toJndiEnvironment()
    {
        Hashtable env = new Hashtable();
        env.put( JNDI_KEY, this );
        return env;
    }


    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch ( CloneNotSupportedException e )
        {
            throw new InternalError();
        }
    }
}
