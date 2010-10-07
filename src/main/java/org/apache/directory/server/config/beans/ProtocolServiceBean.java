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

import java.util.HashSet;
import java.util.Set;

/**
 * A class used to store the ProtocolService configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ProtocolServiceBean 
{
    /** A flag set to tell if the server is enabled or not */
    private boolean enabled;
    
    /** The server ID */
    private String serviceId;
    
    /** The service name */
    private String serviceName;
    
    /** The service transports. We may have more than one */
    protected Set<TransportBean> transports = new HashSet<TransportBean>();

    /**
     * Create a new JournalBean instance
     */
    public ProtocolServiceBean()
    {
        // Not enabled by default
        enabled = false;
    }
    
    
    /**
     * Services can be enabled or disabled. If enabled they will be started, if
     * not they will not.
     *
     * @return true if this service is to be started, false otherwise
     */
    public boolean isEnabled()
    {
        return enabled;
    }


    /**
     * Sets whether or not this ProtocolService is enabled.
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }


    /**
     * Gets the instance identifier for this ProtocolService.
     *
     * @return the identifier for the service instance
     */
    public String getServiceId()
    {
        return serviceId;
    }


    /**
     * Sets the instance identifier for this ProtocolService.
     *
     * @param serviceId an identifier for the service instance
     */
    public void setServiceId( String serviceId )
    {
        this.serviceId = serviceId;
    }
    
    
    /**
     * @return the transport
     */
    public TransportBean[] getTransports()
    {
        return transports.toArray( new TransportBean[]{} );
    }


    /**
     * Set the underlying transports
     * @param transports The transports
     */
    public void setTransports( TransportBean... transports )
    {
        for ( TransportBean transport : transports ) 
        {
            this.transports.add( transport );
        }
    }
    
    
    /**
     * Add underlying transports
     * @param transports The transports
     */
    public void addTransports( TransportBean... transports )
    {
        for ( TransportBean transport : transports )
        {
            this.transports.add( transport );
        }
    }
}
