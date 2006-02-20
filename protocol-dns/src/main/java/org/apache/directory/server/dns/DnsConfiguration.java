/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.directory.server.dns;


import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.configuration.ConfigurationException;
import org.apache.directory.server.protocol.shared.LoadStrategy;
import org.apache.directory.server.protocol.shared.ServiceConfiguration;


public class DnsConfiguration extends ServiceConfiguration
{
    private static final long serialVersionUID = 6943138644427163149L;

    /** the default port */
    private static final String DEFAULT_IP_PORT = "53";

    /** the default pid */
    private static final String DEFAULT_PID = "org.apache.dns";

    /** the default name */
    private static final String DEFAULT_NAME = "Apache DNS Service";

    /** the default prefix */
    private static final String DEFAULT_PREFIX = "dns.";


    /**
     * Creates a new instance with default settings.
     */
    public DnsConfiguration()
    {
        this( getDefaultConfig(), LoadStrategy.LDAP );
    }


    /**
     * Creates a new instance with default settings that operates on the
     * {@link DirectoryService} with the specified ID.
     */
    public DnsConfiguration(String instanceId)
    {
        this( getDefaultConfig(), LoadStrategy.LDAP );
        setInstanceId( instanceId );
    }


    public DnsConfiguration(Map properties)
    {
        this( properties, LoadStrategy.LDAP );
    }


    public DnsConfiguration(Map properties, int strategy)
    {
        if ( properties == null )
        {
            configuration = getDefaultConfig();
        }
        else
        {
            loadProperties( DEFAULT_PREFIX, properties, strategy );
        }

        int port = getPort();

        if ( port < 1 || port > 0xFFFF )
        {
            throw new ConfigurationException( "Invalid value:  " + IP_PORT_KEY + "=" + port );
        }
    }


    public static Map getDefaultConfig()
    {
        Map defaults = new HashMap();

        defaults.put( SERVICE_PID, DEFAULT_PID );
        defaults.put( IP_PORT_KEY, DEFAULT_IP_PORT );

        return defaults;
    }


    public boolean isDifferent( Dictionary config )
    {
        int port = getPort();

        if ( port == Integer.parseInt( ( String ) config.get( IP_PORT_KEY ) ) )
        {
            return false;
        }

        return true;
    }


    public String getName()
    {
        return DEFAULT_NAME;
    }


    public int getPort()
    {
        String key = IP_PORT_KEY;

        if ( configuration.containsKey( key ) )
        {
            return Integer.parseInt( get( key ) );
        }

        return Integer.parseInt( DEFAULT_IP_PORT );
    }


    public int getBufferSize()
    {
        String key = BUFFER_SIZE_KEY;

        if ( configuration.containsKey( key ) )
        {
            return Integer.parseInt( get( key ) );
        }

        return DEFAULT_BUFFER_SIZE;
    }
}
