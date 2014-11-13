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


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.config.ConfigurationElement;


/**
 * A class used to store the Server configuration. It can't be instanciated
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class ServerBean extends AdsBaseBean
{
    /** The server unique identifier */
    @ConfigurationElement(attributeType = "ads-serverId", isRdn = true)
    private String serverId;

    /** The set of transports to use for this server */
    @ConfigurationElement(objectClass = "ads-transport", container = "transports")
    private List<TransportBean> transports = new ArrayList<TransportBean>();


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
        return transports.toArray( new TransportBean[]
            {} );
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
        return serverId;
    }


    /**
     * @param serverId the serverId to set
     */
    public void setServerId( String serverId )
    {
        this.serverId = serverId;
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( super.toString( tabs ) );
        sb.append( tabs ).append( "server id : " ).append( serverId ).append( '\n' );
        sb.append( tabs ).append( "transports : \n" );

        if ( transports != null )
        {
            for ( TransportBean transport : transports )
            {
                sb.append( transport.toString( tabs + "  " ) );
            }
        }

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return toString( "" );
    }
}
