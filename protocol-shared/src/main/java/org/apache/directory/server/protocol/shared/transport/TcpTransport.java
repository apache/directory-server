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

import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

/**
 * @org.apache.xbean.XBean
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TcpTransport extends AbstractTransport
{
    /**
     * Creates an instance of the TcpTransport class 
     */
    public TcpTransport()
    {
        super();
    }
    
    
    /**
     * Creates an instance of the TcpTransport class on localhost
     * @param tcpPort The port
     */
    public TcpTransport( int tcpPort )
    {
        super( LOCAL_HOST, tcpPort, DEFAULT_NB_THREADS, DEFAULT_BACKLOG_NB );
        
        this.acceptor = createAcceptor( LOCAL_HOST, tcpPort, DEFAULT_NB_THREADS, DEFAULT_BACKLOG_NB );
    }
    
    
    /**
     * Creates an instance of the TcpTransport class on localhost
     * @param tcpPort The port
     * @param nbThreads The number of threads to create in the acceptor
     */
    public TcpTransport( int tcpPort, int nbThreads )
    {
        super( LOCAL_HOST, tcpPort, nbThreads, DEFAULT_BACKLOG_NB );
        
        this.acceptor = createAcceptor( LOCAL_HOST, tcpPort, nbThreads, DEFAULT_BACKLOG_NB );
    }
    
    
    /**
     * Creates an instance of the TcpTransport class 
     * @param address The address
     * @param port The port
     */
    public TcpTransport( String address, int port )
    {
        super( address, port, DEFAULT_NB_THREADS, DEFAULT_BACKLOG_NB );
        this.acceptor = createAcceptor( address, port, DEFAULT_NB_THREADS, DEFAULT_BACKLOG_NB );
    }
    
    
    /**
     * Creates an instance of the TcpTransport class on localhost
     * @param tcpPort The port
     * @param nbThreads The number of threads to create in the acceptor
     * @param backlog The queue size for incoming messages, waiting for the
     * acceptor to be ready
     */
    public TcpTransport( int tcpPort, int nbThreads, int backLog )
    {
        super( LOCAL_HOST, tcpPort, nbThreads, backLog );
        this.acceptor = createAcceptor( LOCAL_HOST, tcpPort, nbThreads, backLog );
    }
    
    
    /**
     * Creates an instance of the TcpTransport class 
     * @param address The address
     * @param tcpPort The port
     * @param nbThreads The number of threads to create in the acceptor
     * @param backlog The queue size for incoming messages, waiting for the
     * acceptor to be ready
     */
    public TcpTransport( String address, int tcpPort, int nbThreads, int backLog )
    {
        super( address, tcpPort, nbThreads, backLog );
        this.acceptor = createAcceptor( address, tcpPort, nbThreads, backLog );
    }
    
    
    /**
     * Initialize the Acceptor if needed
     */
    public void init()
    {
        acceptor = createAcceptor( this.getAddress(), this.getPort(), this.getNbThreads(), this.getBackLog() );
    }
    
    
    /**
     * Helper method to create an IoAcceptor
     */
    private IoAcceptor createAcceptor( String address, int port, int nbThreads, int backLog )
    {
        NioSocketAcceptor acceptor = new NioSocketAcceptor( nbThreads );
        acceptor.setBacklog( backLog );
        InetSocketAddress socketAddress = new InetSocketAddress( address, port ); 
        acceptor.setDefaultLocalAddress( socketAddress );
        
        return acceptor;
    }
    
    
    /**
     * @return The associated SocketAcceptor
     */
    public SocketAcceptor getSocketAcceptor()
    {
        return acceptor == null ? null : (SocketAcceptor)acceptor;
    }
}
