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
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Transport instance for UDP based protocols.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UdpTransport extends AbstractTransport
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( UdpTransport.class );


    /**
     * Creates an instance of the UdpTransport class 
     */
    public UdpTransport()
    {
        super();
    }


    /**
     * Creates an instance of the UdpTransport class on localhost
     * @param udpPort The port
     */
    public UdpTransport( int udpPort )
    {
        super( udpPort );

        this.acceptor = createAcceptor( null, udpPort );

        LOG.debug( "UDP Transport created : <*:{},>", udpPort );
    }


    /**
     * Creates an instance of the UdpTransport class 
     * @param address The address
     * @param udpPort The port
     */
    public UdpTransport( String address, int udpPort )
    {
        super( address, udpPort );

        this.acceptor = createAcceptor( address, udpPort );

        LOG.debug( "UDP Transport created : <{}:{},>", address, udpPort );
    }


    /**
     * Initialize the Acceptor if needed
     */
    public void init()
    {
        acceptor = createAcceptor( getAddress(), getPort() );
        LOG.debug( "UDP Transport created : <{}:{},>", getAddress(), getPort() );
    }


    /**
     * @return The associated DatagramAcceptor
     */
    public DatagramAcceptor getAcceptor()
    {
        if ( ( acceptor != null ) && acceptor.isDisposed() )
        {
            acceptor = createAcceptor( getAddress(), getPort() );
        }

        return acceptor == null ? null : ( DatagramAcceptor ) acceptor;
    }


    /**
     * Helper method to create an IoAcceptor
     */
    private IoAcceptor createAcceptor( String address, int port )
    {
        NioDatagramAcceptor acceptor = new NioDatagramAcceptor();

        InetSocketAddress socketAddress = null;

        // The address can be null here, if one want to connect using the wildcard address
        if ( address == null )
        {
            // Create a socket listening on the wildcard address
            socketAddress = new InetSocketAddress( port );
        }
        else
        {
            socketAddress = new InetSocketAddress( address, port );
        }

        acceptor.setDefaultLocalAddress( socketAddress );

        return acceptor;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "UdpTransport" + super.toString();
    }
}
