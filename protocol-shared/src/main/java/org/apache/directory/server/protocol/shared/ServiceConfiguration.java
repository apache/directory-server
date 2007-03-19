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


import java.util.Dictionary;
import java.util.Hashtable;

import javax.naming.spi.InitialContextFactory;

import org.apache.directory.server.core.configuration.Configuration;
import org.apache.directory.server.core.configuration.ConfigurationUtil;


/**
 * Base class shared by all protocol providers for configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class ServiceConfiguration extends Configuration
{
    /** The prop key const for the port. */
    public static final String IP_PORT_KEY = "ipPort";

    /** The number of milliseconds in a minute. */
    public static final int MINUTE = 60000;

    /** The default MINA buffer size. */
    public static final int DEFAULT_BUFFER_SIZE = 1024;

    protected static final String APACHE_SERVICE_CONFIGURATION = "apacheServiceConfiguration";
    protected static final String SERVICE_PID = "service.pid";
    protected static final String SERVICE_FACTORYPID = "service.factoryPid";

    /** The MINA buffer size for this service. */
    private int bufferSize;

    /** The IP port for this service. */
    private int ipPort;

    /** The IP address for this service. */
    private String ipAddress;

    /**
     * The single location where catalog entries are stored.  If this
     * property is not set the store will expect a single search base
     * DN to be set.
     */
    private String catalogBaseDn;

    /**
     * The single location where entries are stored.  If this
     * property is not set the store will search the system
     * partition configuration for catalog entries.
     */
    private String searchBaseDn = "ou=users,dc=example,dc=com";

    /** The JNDI initial context factory to use. */
    private String initialContextFactory = "org.apache.directory.server.core.jndi.CoreContextFactory";

    /** The authentication mechanism to use for establishing a JNDI context. */
    private String securityAuthentication = "simple";

    /** The principal to use for establishing a JNDI context. */
    private String securityPrincipal = "uid=admin,ou=system";

    /** The credentials to use for establishing a JNDI context. */
    private String securityCredentials = "secret";

    /** The friendly name of this service. */
    private String serviceName;

    /** The PID for this service. */
    private String servicePid;

    /** Whether this service is enabled. */
    private boolean isEnabled = false;


    /**
     * Returns the buffer size.
     * 
     * @return The bufferSize.
     */
    public int getBufferSize()
    {
        return bufferSize;
    }


    /**
     * Sets the buffer size.
     * 
     * @param bufferSize The bufferSize to set.
     */
    public void setBufferSize( int bufferSize )
    {
        this.bufferSize = bufferSize;
    }


    /**
     * Returns whether this service is enabled or not.
     * 
     * @return True if this service is enabled.
     */
    public boolean isEnabled()
    {
        return isEnabled;
    }


    /**
     * Sets whether this service is enabled or not.
     * 
     * @param isEnabled True if this service is to be enabled.
     */
    public void setEnabled( boolean isEnabled )
    {
        this.isEnabled = isEnabled;
    }


    /**
     * Returns the service PID.
     * 
     * @return The servicePid.
     */
    public String getServicePid()
    {
        return servicePid;
    }


    /**
     * Sets the service PID.
     * 
     * @param servicePid The servicePid to set.
     */
    public void setServicePid( String servicePid )
    {
        this.servicePid = servicePid;
    }


    /**
     * Compares whether a Dictionary of configuration is 
     * different from the current instance of configuration.
     *
     * @param config
     * @return Whether the configuration is different.
     */
    public boolean isDifferent( Dictionary config )
    {
        int port = getIpPort();

        if ( port == Integer.parseInt( ( String ) config.get( IP_PORT_KEY ) ) )
        {
            return false;
        }

        return true;
    }


    /**
     * Returns the service name.
     * 
     * @return The serviceName.
     */
    public String getServiceName()
    {
        return serviceName;
    }


    /**
     * Sets the service name.
     * 
     * @param serviceName The service name to set.
     */
    public void setServiceName( String serviceName )
    {
        this.serviceName = serviceName;
    }


    /**
     * Returns the IP address.
     * 
     * @return The IP address.
     */
    public String getIpAddress()
    {
        return ipAddress;
    }


    /**
     * Returns the IP port.
     * 
     * @return The IP port.
     */
    public int getIpPort()
    {
        return ipPort;
    }


    /**
     * Returns the catalog base DN.
     *
     * @return The catalog base DN.
     */
    public String getCatalogBaseDn()
    {
        return catalogBaseDn;
    }


    /**
     * Returns the search base DN.
     *
     * @return The search base DN.
     */
    public String getSearchBaseDn()
    {
        return searchBaseDn;
    }


    /**
     * Returns the {@link InitialContextFactory}.
     *
     * @return The {@link InitialContextFactory}.
     */
    public String getInitialContextFactory()
    {
        return initialContextFactory;
    }


    /**
     * Returns The authentication mechanism.
     * 
     * @return The securityAuthentication.
     */
    public String getSecurityAuthentication()
    {
        return securityAuthentication;
    }


    /**
     * @return The securityCredentials.
     */
    public String getSecurityCredentials()
    {
        return securityCredentials;
    }


    /**
     * @return The securityPrincipal.
     */
    public String getSecurityPrincipal()
    {
        return securityPrincipal;
    }


    /**
     * @param catalogBaseDn The catalogBaseDn to set.
     */
    public void setCatalogBaseDn( String catalogBaseDn )
    {
        this.catalogBaseDn = catalogBaseDn;
    }


    /**
     * @param initialContextFactory The initialContextFactory to set.
     */
    public void setInitialContextFactory( String initialContextFactory )
    {
        this.initialContextFactory = initialContextFactory;
    }


    /**
     * @param ipAddress The ipAddress to set.
     */
    public void setIpAddress( String ipAddress )
    {
        this.ipAddress = ipAddress;
    }


    /**
     * @param ipPort The ipPort to set.
     */
    public void setIpPort( int ipPort )
    {
        ConfigurationUtil.validatePortNumber( ipPort );
        this.ipPort = ipPort;
    }


    /**
     * @param searchBaseDn The searchBaseDn to set.
     */
    public void setSearchBaseDn( String searchBaseDn )
    {
        this.searchBaseDn = searchBaseDn;
    }


    /**
     * @param securityAuthentication The securityAuthentication to set.
     */
    public void setSecurityAuthentication( String securityAuthentication )
    {
        this.securityAuthentication = securityAuthentication;
    }


    /**
     * @param securityCredentials The securityCredentials to set.
     */
    public void setSecurityCredentials( String securityCredentials )
    {
        this.securityCredentials = securityCredentials;
    }


    /**
     * @param securityPrincipal The securityPrincipal to set.
     */
    public void setSecurityPrincipal( String securityPrincipal )
    {
        this.securityPrincipal = securityPrincipal;
    }


    public Hashtable<String, Object> toJndiEnvironment()
    {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( JNDI_KEY, this );

        return env;
    }
}
