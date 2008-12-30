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

import org.apache.mina.transport.socket.SocketAcceptor;

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
     * @param port The port
     */
    public TcpTransport( int port )
    {
        super( port );
    }
    
    
    /**
     * Creates an instance of the TcpTransport class 
     * @param address The address
     * @param port The port
     */
    public TcpTransport( String address, int port )
    {
        super( address, port );
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
        super( tcpPort, nbThreads, backLog );
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
    }
    
    
    /**
     * @return The associated SocketAcceptor
     */
    public SocketAcceptor getSocketAcceptor()
    {
        return acceptor == null ? null : (SocketAcceptor)acceptor;
    }
}
