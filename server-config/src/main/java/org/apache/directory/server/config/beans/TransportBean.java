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
public class TransportBean 
{
    /** The server address */
    private String address;
    
    /** The port number */
    private int port = -1;

    /** A flag set if SSL is enabled */
    private boolean sslEnabled = false;
    
    /** The default backlog queue size */
    protected static final int DEFAULT_BACKLOG_NB = 50;
    
    /** The default number of threads */
    protected static final int DEFAULT_NB_THREADS = 3;

    /** The number of threads to use for the IoAcceptor executor */
    private int nbThreads = DEFAULT_NB_THREADS;
    
    /** The backlog for the transport services */
    private int backlog = DEFAULT_BACKLOG_NB;
    

    /**
     * Create a new TransportBean instance
     */
    public TransportBean()
    {
    }

    
    /**
     * @param port the port to set
     */
    public void setPort( int port ) 
    {
        this.port = port;
    }

    
    /**
     * @return the port
     */
    public int getPort() 
    {
        return port;
    }

    
    /**
     * @param address the address to set
     */
    public void setAddress( String address ) {
        this.address = address;
    }

    
    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }
    
    
    /**
     * @return <code>true</code> id SSL is enabled for this transport
     */
    public boolean isSSLEnabled()
    {
        return sslEnabled;
    }
    
    
    /**
     * Enable or disable SSL
     * @param sslEnabled if <code>true</code>, SSL is enabled.
     */
    public void setEnableSSL( boolean sslEnabled )
    {
        this.sslEnabled = sslEnabled;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int getNbThreads() 
    {
        return nbThreads;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void setNbThreads( int nbThreads )
    {
        this.nbThreads = nbThreads;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int getBackLog()
    {
        return backlog;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void setBackLog( int backLog )
    {
        this.backlog = backLog;
    }
}
