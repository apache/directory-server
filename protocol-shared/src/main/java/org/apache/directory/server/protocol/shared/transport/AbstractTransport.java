/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.protocol.shared.transport;


import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.mina.core.service.IoAcceptor;


public abstract class AbstractTransport implements Transport
{
    /** The server address */
    private String address;

    /** The service's port */
    private int port = -1;

    /** A flag set if SSL is enabled */
    private boolean sslEnabled = false;

    /** The number of threads to use for the IoAcceptor executor */
    private int nbThreads;

    /** The backlog for the transport services */
    private int backlog;

    /** The IoAcceptor used to accept requests */
    protected IoAcceptor acceptor;

    /** The default backlog queue size */
    protected static final int DEFAULT_BACKLOG_NB = 50;

    /** The default hostname */
    protected static final String LOCAL_HOST = "localhost";

    /** The default number of threads */
    protected static final int DEFAULT_NB_THREADS = 3;


    /**
     * Creates an instance of an Abstract Transport class.
     */
    public AbstractTransport()
    {
        address = null;
        nbThreads = DEFAULT_NB_THREADS;
        port = -1;
        backlog = DEFAULT_BACKLOG_NB;
    }


    /**
     * Creates an instance of an Abstract Transport class, using localhost
     * and port.
     * 
     * @param port The port
     */
    public AbstractTransport( int port )
    {
        try
        {
            this.address = InetAddress.getLocalHost().getHostAddress();
        }
        catch ( UnknownHostException e )
        {
            this.address = "localhost";
        }
        
        this.port = port;
    }


    /**
     * Creates an instance of an Abstract Transport class, using localhost
     * and port.
     * 
     * @param port The port
     * @param nbThreads The number of threads to create in the acceptor
     */
    public AbstractTransport( int port, int nbThreads )
    {
        try
        {
            this.address = InetAddress.getLocalHost().getHostAddress();
        }
        catch ( UnknownHostException e )
        {
            this.address = "localhost";
        }
        
        this.port = port;
        this.nbThreads = nbThreads;
    }


    /**
     * Creates an instance of an Abstract Transport class, using the given address
     * and port.
     * 
     * @param address The address
     * @param port The port
     */
    public AbstractTransport( String address, int port )
    {
        this.address = address;
        this.port = port;
    }


    /**
     * Creates an instance of the AbstractTransport class on LocalHost
     * @param port The port
     * @param nbThreads The number of threads to create in the acceptor
     * @param backLog The queue size for incoming messages, waiting for the
     * acceptor to be ready
     */
    public AbstractTransport( int port, int nbThreads, int backLog )
    {
        this.address = "localHost";
        this.port = port;
        this.nbThreads = nbThreads;
        this.backlog = backLog;
    }


    /**
     * Creates an instance of the AbstractTransport class 
     * @param address The address
     * @param port The port
     * @param nbThreads The number of threads to create in the acceptor
     * @param backLog The queue size for incoming messages, waiting for the
     * acceptor to be ready
     */
    public AbstractTransport( String address, int port, int nbThreads, int backLog )
    {
        this.address = address;
        this.port = port;
        this.nbThreads = nbThreads;
        this.backlog = backLog;
    }


    /**
     * Initialize the Acceptor if needed
     */
    public abstract void init();


    /**
     * {@inheritDoc}
     */
    public int getPort()
    {
        return port;
    }


    /**
     * {@inheritDoc}
     */
    public void setPort( int port )
    {
        this.port = port;
    }


    /**
     * {@inheritDoc}
     */
    public String getAddress()
    {
        return address;
    }


    /**
     * Stores the Address in this transport
     * 
     * @param address the Address to store
     */
    public void setAddress( String address )
    {
        this.address = address;
    }


    /**
     * {@inheritDoc}
     */
    public abstract IoAcceptor getAcceptor();


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


    /**
     * Enable or disable SSL
     * @param sslEnabled if <code>true</code>, SSL is enabled.
     */
    public void setEnableSSL( boolean sslEnabled )
    {
        this.sslEnabled = sslEnabled;
    }


    /**
     * Enable or disable SSL
     * @param sslEnabled if <code>true</code>, SSL is enabled.
     */
    public void enableSSL( boolean sslEnabled )
    {
        this.sslEnabled = sslEnabled;
    }


    /**
     * @return <code>true</code> id SSL is enabled for this transport
     */
    public boolean isSSLEnabled()
    {
        return sslEnabled;
    }


    /**
     * @return  <code>true</code> id SSL is enabled for this transport
     */
    public boolean getEnableSSL()
    {
        return sslEnabled;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "[<" ).append( address ).append( ':' ).append( port );
        sb.append( ">], backlog=" ).append( backlog );
        sb.append( ", nbThreads = " ).append( nbThreads );

        if ( sslEnabled )
        {
            sb.append( ", SSL" );
        }

        sb.append( ']' );

        return sb.toString();
    }
}
