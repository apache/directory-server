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


import org.apache.directory.server.config.ConfigurationElement;


/**
 * A class used to store the Transport configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TransportBean extends AdsBaseBean
{
    /** The default backlog queue size */
    private static final int DEFAULT_BACKLOG_NB = 50;

    /** The default number of threads */
    private static final int DEFAULT_NB_THREADS = 3;

    /** The unique identifier for this transport */
    @ConfigurationElement(attributeType = "ads-transportId", isRdn = true)
    private String transportId;

    /** The transport address */
    @ConfigurationElement(attributeType = "ads-transportAddress")
    private String transportAddress;

    /** The port number */
    @ConfigurationElement(attributeType = "ads-systemPort")
    private int systemPort = -1;

    /** A flag set if SSL is enabled */
    @ConfigurationElement(attributeType = "ads-transportEnableSsl", isOptional = true, defaultValue = "false")
    private boolean transportEnableSsl = false;

    /** The number of threads to use for the IoAcceptor executor */
    @ConfigurationElement(attributeType = "ads-transportNbThreads", isOptional = true, defaultValue = "3")
    private int transportNbThreads = DEFAULT_NB_THREADS;

    /** The backlog for the transport services */
    @ConfigurationElement(attributeType = "ads-transportBackLog", isOptional = true, defaultValue = "50")
    private int transportBackLog = DEFAULT_BACKLOG_NB;


    /**
     * Create a new TransportBean instance
     */
    public TransportBean()
    {
    }


    /**
     * @param systemPort the port to set
     */
    public void setSystemPort( int systemPort )
    {
        this.systemPort = systemPort;
    }


    /**
     * @return the port
     */
    public int getSystemPort()
    {
        return systemPort;
    }


    /**
     * @param transportAddress the address to set
     */
    public void setTransportAddress( String transportAddress )
    {
        this.transportAddress = transportAddress;
    }


    /**
     * @return the address
     */
    public String getTransportAddress()
    {
        return transportAddress;
    }


    /**
     * @return <code>true</code> id SSL is enabled for this transport
     */
    public boolean isTransportEnableSSL()
    {
        return transportEnableSsl;
    }


    /**
     * Enable or disable SSL
     * 
     * @param transportEnableSSL if <code>true</code>, SSL is enabled.
     */
    public void setTransportEnableSSL( boolean transportEnableSSL )
    {
        this.transportEnableSsl = transportEnableSSL;
    }


    /**
     * @return The number of threads used to handle the incoming requests
     */
    public int getTransportNbThreads()
    {
        return transportNbThreads;
    }


    /**
     * Sets the number of thread to use to process incoming requests
     * 
     * @param The number of threads
     */
    public void setTransportNbThreads( int transportNbThreads )
    {
        this.transportNbThreads = transportNbThreads;
    }


    /**
     * @return the size of the incoming request waiting queue
     */
    public int getTransportBackLog()
    {
        return transportBackLog;
    }


    /**
     * Sets the size of the incoming requests waiting queue
     * 
     * @param The size of waiting request queue
     */
    public void setTransportBackLog( int transportBacklog )
    {
        this.transportBackLog = transportBacklog;
    }


    /**
     * @return the transportId
     */
    public String getTransportId()
    {
        return transportId;
    }


    /**
     * @param transportId the transportId to set
     */
    public void setTransportId( String transportId )
    {
        this.transportId = transportId;
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( toString( tabs, "transport id", transportId ) );
        sb.append( tabs ).append( "transport address : " );

        if ( transportAddress == null )
        {
            sb.append( "localhost" ).append( '\n' );
        }
        else
        {
            sb.append( transportAddress ).append( '\n' );
        }

        sb.append( tabs ).append( "transport port : " ).append( systemPort ).append( '\n' );
        sb.append( tabs ).append( "transport backlog : " ).append( transportBackLog ).append( '\n' );
        sb.append( tabs ).append( "transport nb threads : " ).append( transportNbThreads ).append( '\n' );
        sb.append( toString( tabs, "SSL enabled", transportEnableSsl ) );

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
