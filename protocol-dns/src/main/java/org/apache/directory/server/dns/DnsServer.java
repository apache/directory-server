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
import java.net.InetSocketAddress;

import org.apache.directory.server.dns.protocol.DnsProtocolHandler;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.directory.server.dns.store.jndi.JndiRecordStoreImpl;
import org.apache.directory.server.protocol.shared.DirectoryBackedService;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;


/**
 * Contains the configuration parameters for the DNS protocol provider.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DnsServer extends DirectoryBackedService
{
    @SuppressWarnings ( { "UnusedDeclaration" } )
    private static final long serialVersionUID = 6943138644427163149L;

    /** The default IP port. */
    private static final int IP_PORT_DEFAULT = 53;

    /** The default service pid. */
    private static final String SERVICE_PID_DEFAULT = "org.apache.directory.server.dns";

    /** The default service name. */
    private static final String SERVICE_NAME_DEFAULT = "ApacheDS DNS Service";


    /**
     * Creates a new instance of DnsConfiguration.
     */
    public DnsServer()
    {
        super.setIpPort( IP_PORT_DEFAULT );
        super.setServiceId( SERVICE_PID_DEFAULT );
        super.setServiceName( SERVICE_NAME_DEFAULT );
    }


    /**
     * @org.apache.xbean.InitMethod
     * @throws IOException if we cannot bind to the specified ports
     */
    public void start() throws IOException
    {
        RecordStore store = new JndiRecordStoreImpl( getSearchBaseDn(), getSearchBaseDn(), getDirectoryService() );

        if ( getDatagramAcceptor() != null )
        {
            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            getDatagramAcceptor().bind( new InetSocketAddress( getIpPort() ), new DnsProtocolHandler( this, store ), udpConfig );
        }

        if ( getSocketAcceptor() != null )
        {
            SocketAcceptorConfig tcpConfig = new SocketAcceptorConfig();
            tcpConfig.setDisconnectOnUnbind( false );
            tcpConfig.setReuseAddress( true );
            getSocketAcceptor().bind( new InetSocketAddress( getIpPort() ), new DnsProtocolHandler( this, store ), tcpConfig );
        }
    }

    /**
     * @org.apache.xbean.DestroyMethod
     */
    public void stop() {
        if ( getDatagramAcceptor() != null )
        {
            getDatagramAcceptor().unbind( new InetSocketAddress( getIpPort() ));
        }
        if ( getSocketAcceptor() != null )
        {
            getSocketAcceptor().unbind( new InetSocketAddress( getIpPort() ));
        }
    }
}
