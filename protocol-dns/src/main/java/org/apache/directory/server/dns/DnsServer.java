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
package org.apache.directory.server.dns;


import java.io.IOException;

import org.apache.directory.server.dns.protocol.DnsProtocolHandler;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.directory.server.dns.store.jndi.JndiRecordStoreImpl;
import org.apache.directory.server.protocol.shared.DirectoryBackedService;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Contains the configuration parameters for the DNS protocol provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DnsServer extends DirectoryBackedService
{
    private static final long serialVersionUID = 6943138644427163149L;

    /** logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DnsServer.class );

    /** The default IP port. */
    private static final int DEFAULT_IP_PORT = 53;

    /** The default service pid. */
    private static final String SERVICE_PID_DEFAULT = "org.apache.directory.server.dns";

    /** The default service name. */
    private static final String SERVICE_NAME_DEFAULT = "ApacheDS DNS Service";


    /**
     * Creates a new instance of DnsConfiguration.
     */
    public DnsServer()
    {
        super.setServiceId( SERVICE_PID_DEFAULT );
        super.setServiceName( SERVICE_NAME_DEFAULT );
    }


    /**
     * @throws IOException if we cannot bind to the specified ports
     */
    public void start() throws IOException
    {
        RecordStore store = new JndiRecordStoreImpl( getSearchBaseDn(), getSearchBaseDn(), getDirectoryService() );

        if ( ( transports == null ) || ( transports.size() == 0 ) )
        {
            // Default to UDP with port 53
            // We have to create a DatagramAcceptor
            UdpTransport transport = new UdpTransport( DEFAULT_IP_PORT );
            setTransports( transport );

            DatagramAcceptor acceptor = transport.getAcceptor();

            // Set the handler
            acceptor.setHandler( new DnsProtocolHandler( this, store ) );

            // Allow the port to be reused even if the socket is in TIME_WAIT state
            ( ( DatagramSessionConfig ) acceptor.getSessionConfig() ).setReuseAddress( true );

            // Start the listener
            acceptor.bind();
        }
        else
        {
            for ( Transport transport : transports )
            {
                // Get the acceptor
                IoAcceptor acceptor = transport.getAcceptor();

                // Set the handler
                acceptor.setHandler( new DnsProtocolHandler( this, store ) );

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
                    ( ( NioSocketAcceptor ) acceptor ).setReuseAddress( true );

                    // No Nagle's algorithm
                    ( ( NioSocketAcceptor ) acceptor ).getSessionConfig().setTcpNoDelay( true );
                }

                // Start the listener
                acceptor.bind();
            }
        }

        LOG.info( "DNS service started." );
    }


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

        LOG.info( "DNS service stopped." );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "DNSServer[" ).append( getServiceName() ).append( "], listening on :" ).append( '\n' );

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
