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
 * A class used to store the Server configuration. It can't be instanciated
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class ServerBean extends AdsBaseBean
{
    /** The server unique identifier */
    private String serverid;
    
    /** The set of transports to use for this server */
    private Set<TransportBean> transports = new HashSet<TransportBean>();;

    /**
     * Create a new ServerBean instance
     */
    protected ServerBean()
    {
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


    /**
     * @return the serverId
     */
    public String getServerId()
    {
        return serverid;
    }


    /**
     * @param serverId the serverId to set
     */
    public void setServerId( String serverId )
    {
        this.serverid = serverId;
    }
}
