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


import org.apache.mina.core.service.IoAcceptor;

public abstract class AbstractTransport implements Transport
{
    /** The server address */
    private String address;
    
    /** The service's port */
    private int port = -1;
    
    /** The number of threads to use for the IoAcceptor executor */
    private int nbThreads;
    
    /** The backlog for the transport services */
    private int backlog;
    
    /** The IoAcceptor used to accept requests */
    protected IoAcceptor acceptor;

    /**
     * Creates an instance of an Abstract Transport class.
     */
    public AbstractTransport()
    {
        address = "localHost";
    }
    
    
    /**
     * Creates an instance of an Abstract Transport class, using localhost
     * and port.
     * 
     * @param port The port
     */
    public AbstractTransport( int port )
    {
       this.address = "localhost";
       this.port = port;
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
     * @param tcpPort The port
     * @param nbThreads The number of threads to create in the acceptor
     * @param backlog The queue size for incoming messages, waiting for the
     * acceptor to be ready
     */
    public AbstractTransport( int port, int nbThreads, int backLog )
    {
        this.address ="localHost";
        this.port = port;
        this.nbThreads = nbThreads;
        this.backlog = backLog;
    }
    
    
    /**
     * Creates an instance of the AbstractTransport class 
     * @param address The address
     * @param tcpPort The port
     * @param nbThreads The number of threads to create in the acceptor
     * @param backlog The queue size for incoming messages, waiting for the
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
    public IoAcceptor getAcceptor()
    {
        return acceptor;
    }
    
    
    /**
     * Set the IoAcceptor
     * @param acceptor The IoAcceptor to set
     */
    public void setAcceptor ( IoAcceptor acceptor )
    {
        this.acceptor = acceptor;
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
