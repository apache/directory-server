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

package org.apache.ldap;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.configuration.Configuration;
import org.apache.directory.server.core.configuration.ConfigurationException;

public class LdapConfig extends Configuration
{
    private static final long serialVersionUID = 6738567218407227901L;

    public static String LDAP_PORT_KEY = "ldap.port";
    public static String LDAPS_PORT_KEY = "ldaps.port";

    private static String LDAP_PORT_DEFAULT = "10389";
    private static String LDAPS_PORT_DEFAULT = "10636";

    private static String SERVICE_PID = "service.pid";
    private static String PID = "org.apache.ldap";
    private static String name = "Apache LDAP Service";

    private Map configuration = new HashMap();

    /**
     * Creates a new instance with default settings.
     */
    public LdapConfig()
    {
        this( getDefaultConfig() );
    }

    /**
     * Creates a new instance with default settings that operates on the
     * {@link DirectoryService} with the specified ID.
     */
    public LdapConfig( String instanceId )
    {
        this( getDefaultConfig() );
        setInstanceId( instanceId );
    }

    public LdapConfig( Map properties )
    {
        if ( properties == null )
        {
            configuration = getDefaultConfig();
        }
        else
        {
            configuration.putAll( properties );
        }

        int port = getPort();

        if ( port < 1 || port > 0xFFFF )
        {
            throw new ConfigurationException( "Invalid value:  " + LDAP_PORT_KEY + "=" + port );
        }

        int securePort = getSecurePort();

        if ( securePort < 1 || securePort > 0xFFFF )
        {
            throw new ConfigurationException( "Invalid value:  " + LDAPS_PORT_KEY + "=" + securePort );
        }
    }

    public boolean isDifferent( Dictionary config )
    {
        if ( getPort() != ( (Integer) config.get( LDAP_PORT_KEY ) ).intValue() )
        {
            return true;
        }

        if ( getSecurePort() != ( (Integer) config.get( LDAPS_PORT_KEY ) ).intValue() )
        {
            return true;
        }

        return true;
    }

    public String getName()
    {
        return name;
    }

    public int getPort()
    {
        String key = LDAP_PORT_KEY;

        if ( configuration.containsKey( key ) )
        {
            return Integer.parseInt( get( key ) );
        }

        return Integer.parseInt( LDAP_PORT_DEFAULT );
    }

    public int getSecurePort()
    {
        String key = LDAPS_PORT_KEY;

        if ( configuration.containsKey( key ) )
        {
            return Integer.parseInt( get( key ) );
        }

        return Integer.parseInt( LDAPS_PORT_DEFAULT );
    }

    public static Map getDefaultConfig()
    {
        Map defaults = new HashMap();

        defaults.put( SERVICE_PID, PID );
        defaults.put( LDAP_PORT_KEY, LDAP_PORT_DEFAULT );
        defaults.put( LDAPS_PORT_KEY, LDAPS_PORT_DEFAULT );

        return defaults;
    }

    private String get( String key )
    {
        return (String) configuration.get( key );
    }
}
