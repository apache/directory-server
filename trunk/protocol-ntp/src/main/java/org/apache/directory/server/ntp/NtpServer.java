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


import java.io.IOException;

import org.apache.directory.server.ntp.protocol.NtpProtocolCodecFactory;
import org.apache.directory.server.ntp.protocol.NtpProtocolHandler;
import org.apache.directory.server.protocol.shared.AbstractProtocolService;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Contains the configuration parameters for the NTP protocol provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NtpServer extends AbstractProtocolService
{
    /** logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( NtpServer.class.getName() );

    /**
     * The default IP port.
     */
    private static final int IP_PORT_DEFAULT = 123;

    /** The default service pid. */
    private static final String SERVICE_PID_DEFAULT = "org.apache.directory.server.ntp";

    /** The default service name. */
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

        if ( ( transports == null ) || ( transports.size() == 0 ) )
        {
            // Default to UDP with port 123
            // We have to create a DatagramAcceptor
            UdpTransport transport = new UdpTransport( IP_PORT_DEFAULT );
            setTransports( transport );

            DatagramAcceptor acceptor = ( DatagramAcceptor ) transport.getAcceptor();

            // Set the handler
            acceptor.setHandler( ntpProtocolHandler );

            // Allow the port to be reused even if the socket is in TIME_WAIT state
            ( ( DatagramSessionConfig ) acceptor.getSessionConfig() ).setReuseAddress( true );

            // Inject the chain
            acceptor.setFilterChainBuilder( ntpChain );

            // Start the listener
            acceptor.bind();
        }
        else
        {
            for ( Transport transport : transports )
            {
                IoAcceptor acceptor = transport.getAcceptor();

                // Set the handler
                acceptor.setHandler( ntpProtocolHandler );

                if ( transport instanceof UdpTransport )
                {
                    // Allow the port to be reused even if the socket is in TIME_WAIT state
                    ( ( DatagramSessionConfig ) acceptor.getSessionConfig() ).setReuseAddress( true );
                }
                else
                {
                    // Disable the disconnection of the clients on unbind
                    acceptor.setCloseOnDeactivation( false );

                    // Allow the port to be reused even if the socket is in TIME_WAIT state
                    ( ( SocketAcceptor ) acceptor ).setReuseAddress( true );

                    // No Nagle's algorithm
                    ( ( SocketAcceptor ) acceptor ).getSessionConfig().setTcpNoDelay( true );
                }

                // Inject the chain
                acceptor.setFilterChainBuilder( ntpChain );

                // Start the listener
                acceptor.bind();
            }
        }

        LOG.info( "NTP server started." );
    }


    /**
     * {@inheritDoc}
     */
    public void stop()
    {
        for ( Transport transport : getTransports() )
        {
            IoAcceptor acceptor = transport.getAcceptor();

            if ( acceptor != null )
            {
                acceptor.dispose();
            }
        }

        LOG.info( "NTP Server stopped." );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "NTPServer[" ).append( getServiceName() ).append( "], listening on :" ).append( '\n' );

        if ( getTransports() != null )
        {
            for ( Transport transport : getTransports() )
            {
                sb.append( "    " ).append( transport ).append( '\n' );
            }
        }

        return sb.toString();
    }
}
