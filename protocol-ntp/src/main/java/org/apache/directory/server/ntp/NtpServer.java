/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.ntp;


import org.apache.directory.server.ntp.protocol.NtpProtocolCodecFactory;
import org.apache.directory.server.ntp.protocol.NtpProtocolHandler;
import org.apache.directory.server.protocol.shared.AbstractProtocolService;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.io.IOException;
import java.net.InetSocketAddress;


/**
 * Contains the configuration parameters for the NTP protocol provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 * @org.apache.xbean.XBean
 */
public class NtpServer extends AbstractProtocolService
{
    /**
     * The default IP port.
     */
    private static final int IP_PORT_DEFAULT = 123;

    /**
     * The default service pid.
     */
    private static final String SERVICE_PID_DEFAULT = "org.apache.directory.server.ntp";

    /**
     * The default service name.
     */
    private static final String SERVICE_NAME_DEFAULT = "ApacheDS NTP Service";


    /**
     * Creates a new instance of NtpConfiguration.
     */
    public NtpServer()
    {
        super.setServiceId( SERVICE_PID_DEFAULT );
        super.setServiceName( SERVICE_NAME_DEFAULT );
    }

    
    /**
     * Start the NTPServer. We initialize the Datagram and Socket, if necessary.
     * 
     * Note that we don't have any filter in the chain, everything is done
     * in the handler.
     * @throws IOException if there are issues binding
     */
    public void start() throws IOException
    {
        IoHandler ntpProtocolHandler = new NtpProtocolHandler();
        
        // Create the chain for the NTP server
        DefaultIoFilterChainBuilder ntpChain = new DefaultIoFilterChainBuilder();
        ntpChain.addLast( "codec", new ProtocolCodecFilter( NtpProtocolCodecFactory.getInstance() ) );
        
        Transport udpTransport = getTcpTransport();
        
        if ( udpTransport != null )
        {
            // We have to create a DatagramAcceptor
            DatagramAcceptor acceptor = new NioDatagramAcceptor();
            setDatagramAcceptor( (NioDatagramAcceptor)acceptor );
        
            // Set the handler
            acceptor.setHandler( ntpProtocolHandler );
    
            // Allow the port to be reused even if the socket is in TIME_WAIT state
            ((DatagramSessionConfig)acceptor.getSessionConfig()).setReuseAddress( true );
    
            // Inject the chain
            acceptor.setFilterChainBuilder( ntpChain );
            
            // Start the listener
            acceptor.bind( new InetSocketAddress( udpTransport.getAddress(), udpTransport.getPort() ) );
        }

        Transport tcpTransport = getTcpTransport();
        
        if ( tcpTransport != null )
        {
            // It's a SocketAcceptor
            SocketAcceptor acceptor = new NioSocketAcceptor();
            
            // Set the handler
            acceptor.setHandler( ntpProtocolHandler );

            // Disable the disconnection of the clients on unbind
            acceptor.setCloseOnDeactivation( false );
            
            // Allow the port to be reused even if the socket is in TIME_WAIT state
            acceptor.setReuseAddress( true );
            
            // No Nagle's algorithm
            acceptor.getSessionConfig().setTcpNoDelay( true );
            
            // Inject the chain
            acceptor.setFilterChainBuilder( ntpChain );

            setSocketAcceptor( acceptor );

            // Set the backlog size
            acceptor.setBacklog( tcpTransport.getBackLog() );

            // Start the listener
            acceptor.bind( new InetSocketAddress( tcpTransport.getAddress(), tcpTransport.getPort() ) );
        }
    }

    
    public void stop()
    {
        if ( getDatagramAcceptor() != null )
        {
            getDatagramAcceptor().unbind( new InetSocketAddress( 
                getUdpTransport().getAddress(), getUdpTransport().getPort() ) );
        }
        
        if ( getSocketAcceptor() != null )
        {
            getSocketAcceptor().unbind( new InetSocketAddress( 
                getTcpTransport().getAddress(), getTcpTransport().getPort() ) );
        }
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "NTPServer[" ).append( getServiceName() ).append( "] :" ).append( '\n' );
        
        if ( getUdpTransport() != null )
        {
            sb.append( "  Listening on UDP:" ).append( getUdpTransport().getPort() ).append( '\n' );
        }

        if ( getTcpTransport() != null )
        {
            sb.append( "  Listening on TCP:" ).append( getTcpTransport().getPort() ).append( '\n' );
        }
        
        return sb.toString();
    }
}
