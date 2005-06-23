/*
 *   @(#) $Id$
 *
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
package org.apache.ldap.server.configuration;

import java.io.Serializable;
import java.util.Hashtable;

/**
 * A configuration that provides required, optional, or default properties
 * to configure context factory.
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
     * Gets {@link Configuration} instance from the specified JNDI environment
     * {@link Hashtable}.
     * 
     * @throws ConfigurationException if the specified environment doesn't
     *                                contain the configuration instance.
     */
    public static Configuration toConfiguration( Hashtable jndiEnvironment )
    {
        Object value = jndiEnvironment.get( JNDI_KEY );
        if( value == null || !( value instanceof Configuration ) )
        {
            throw new ConfigurationException( "Not an ApacheDS configuration: " + value );
        }
        
        return ( Configuration ) value;
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
        catch( CloneNotSupportedException e )
        {
            throw new InternalError();
        }
    }
}
