/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.directory.server.dns;


import java.io.IOException;
import java.util.Dictionary;

import org.apache.directory.server.dns.protocol.DnsProtocolHandler;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.TransportType;
import org.apache.mina.registry.Service;
import org.apache.mina.registry.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DnsServer
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( DnsServer.class );

    private DnsConfiguration config;
    private ServiceRegistry registry;
    private RecordStore store;

    private IoHandler handler;
    private Service tcpService;
    private Service udpService;


    public DnsServer(DnsConfiguration config, ServiceRegistry registry, RecordStore store)
    {
        this.config = config;
        this.registry = registry;
        this.store = store;

        String name = config.getName();
        int port = config.getPort();

        try
        {
            handler = new DnsProtocolHandler( config, this.store );

            udpService = new Service( name, TransportType.DATAGRAM, port );
            tcpService = new Service( name, TransportType.SOCKET, port );

            registry.bind( udpService, handler );
            registry.bind( tcpService, handler );

            log.debug( "{} listening on port {}", name, new Integer( port ) );
        }
        catch ( IOException ioe )
        {
            log.error( ioe.getMessage(), ioe );
        }
    }


    public boolean isDifferent( Dictionary newConfig )
    {
        return config.isDifferent( newConfig );
    }


    public void destroy()
    {
        registry.unbind( udpService );
        registry.unbind( tcpService );

        registry = null;
        handler = null;
        udpService = null;
        tcpService = null;

        log.debug( "{} has stopped listening on port {}", config.getName(), new Integer( config.getPort() ) );
    }
}
