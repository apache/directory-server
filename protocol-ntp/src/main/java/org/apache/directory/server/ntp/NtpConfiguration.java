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

package org.apache.directory.server.ntp;


import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.configuration.ConfigurationException;
import org.apache.directory.server.protocol.shared.LoadStrategy;
import org.apache.directory.server.protocol.shared.ServiceConfiguration;


public class NtpConfiguration extends ServiceConfiguration
{
    private static final long serialVersionUID = 2961795205765175775L;

    /** the default port */
    private static final String DEFAULT_IP_PORT = "123";

    /** the default pid */
    private static final String DEFAULT_PID = "org.apache.ntp";

    /** the default name */
    private static final String DEFAULT_NAME = "Apache NTP Service";

    /** the default prefix */
    private static final String DEFAULT_PREFIX = "ntp.";


    /**
     * Creates a new instance with default settings.
     */
    public NtpConfiguration()
    {
        this( getDefaultConfig(), LoadStrategy.LDAP );
    }


    /**
     * Creates a new instance with default settings that operates on the
     * {@link DirectoryService} with the specified ID.
     */
    public NtpConfiguration(String instanceId)
    {
        this( getDefaultConfig(), LoadStrategy.LDAP );
        setInstanceId( instanceId );
    }


    public NtpConfiguration( Map<String, String> properties )
    {
        this( properties, LoadStrategy.LDAP );
    }


    public NtpConfiguration( Map<String, String> properties, int strategy )
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


    public static Map<String, String> getDefaultConfig()
    {
        Map<String, String> defaults = new HashMap<String, String>();

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
}
