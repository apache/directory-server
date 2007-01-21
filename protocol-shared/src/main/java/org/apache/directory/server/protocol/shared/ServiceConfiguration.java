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

package org.apache.directory.server.protocol.shared;


import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.directory.server.core.configuration.Configuration;


/**
 * Base class shared by all protocol providers for configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class ServiceConfiguration extends Configuration
{
    /** the prop key const for the port */
    public static final String IP_PORT_KEY = "ipPort";

    /** the prop key const for the port */
    public static final String IP_ADDRESS_KEY = "ipAddress";

    /** the prop key const for the catalog's base DN */
    public static final String CATALOG_BASEDN_KEY = "catalogBaseDn";

    /**
     * The key of the property specifying the single location where entries
     * are stored.  If this property is not set the store will search the system
     * partition configuration for catalog entries.
     */
    public static final String ENTRY_BASEDN_KEY = "entryBaseDn";

    public static final String INITIAL_CONTEXT_FACTORY_KEY = "initialContextFactory";

    public static final String APACHE_SERVICE_PID_KEY = "apacheServicePid";
    public static final String APACHE_FACTORY_PID_KEY = "apacheServiceFactoryPid";

    /** the prop key const for buffer.size */
    public static final String BUFFER_SIZE_KEY = "buffer.size";

    public static final String DEFAULT_ENTRY_BASEDN = "dc=example,dc=com";

    public static final String DEFAULT_INITIAL_CONTEXT_FACTORY = "org.apache.directory.server.core.jndi.CoreContextFactory";

    public static final String APACHE_SERVICE_CONFIGURATION = "apacheServiceConfiguration";

    public static final String SERVICE_PID = "service.pid";
    public static final String SERVICE_FACTORYPID = "service.factoryPid";

    /** the default buffer size */
    public static final int DEFAULT_BUFFER_SIZE = 1024;

    /** the number of milliseconds in a minute */
    public static final int MINUTE = 60000;

    /** the map of configuration */
    protected Map configuration = new HashMap();


    public String getCatalogBaseDn()
    {
        String key = CATALOG_BASEDN_KEY;

        if ( configuration.containsKey( key ) )
        {
            return get( key );
        }

        return null;
    }


    public String getEntryBaseDn()
    {
        String key = ENTRY_BASEDN_KEY;

        if ( configuration.containsKey( key ) )
        {
            return get( key );
        }

        return DEFAULT_ENTRY_BASEDN;
    }


    public String getInitialContextFactory()
    {
        String key = INITIAL_CONTEXT_FACTORY_KEY;

        if ( configuration.containsKey( key ) )
        {
            return get( key );
        }

        return DEFAULT_INITIAL_CONTEXT_FACTORY;
    }


    public Hashtable toJndiEnvironment()
    {
        Hashtable env = new Hashtable();
        env.put( JNDI_KEY, this );
        env.putAll( configuration );

        return env;
    }


    protected void loadProperties( String prefix, Map properties, int strategy )
    {
        LoadStrategy loader;

        switch ( strategy )
        {
            case LoadStrategy.LDAP:
                loader = new LdapLoader();
                break;
            case LoadStrategy.PROPS:
            default:
                loader = new PropsLoader();
                break;
        }

        configuration.putAll( loader.load( prefix, properties ) );
    }


    protected String get( String key )
    {
        return ( String ) configuration.get( key );
    }
}
