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
    private String transportId;
    
    /** The transport address */
    private String transportAddress;
    
    /** The port number */
    private int systemPort = -1;
    
    /** A flag set if SSL is enabled */
    private boolean transportEnableSSL = false;
    
    /** The number of threads to use for the IoAcceptor executor */
    private int transportNbThreads = DEFAULT_NB_THREADS;
    
    /** The backlog for the transport services */
    private int transportBacklog = DEFAULT_BACKLOG_NB;
    
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
    public void setTransportAddress( String transportAddress ) {
        this.transportAddress = transportAddress;
    }

    
    /**
     * @return the address
     */
    public String getTransportAddress() {
        return transportAddress;
    }
    
    
    /**
     * @return <code>true</code> id SSL is enabled for this transport
     */
    public boolean isTransportEnableSSL()
    {
        return transportEnableSSL;
    }
    
    
    /**
     * Enable or disable SSL
     * 
     * @param transportEnableSSL if <code>true</code>, SSL is enabled.
     */
    public void setTransportEnableSSL( boolean transportEnableSSL )
    {
        this.transportEnableSSL = transportEnableSSL;
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
        return transportBacklog;
    }
    
    
    /**
     * Sets the size of the incoming requests waiting queue
     * 
     * @param The size of waiting request queue
     */
    public void setTransportBackLog( int transportBacklog )
    {
        this.transportBacklog = transportBacklog;
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
}
