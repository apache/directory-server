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


import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Main
{
    /** Logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( Main.class );

    private static DnsServer dnsConfiguration;


    /**
     * Entry point for the DNS server.
     *
     * @param args
     * @throws Exception
     */
    public static void main( String[] args ) throws Exception
    {
        new Main().go();
    }


    /**
     * Start an instance of the DNS server.
     */
    public void go() throws Exception
    {
        LOG.debug( "Starting the DNS server" );

        DirectoryService directoryService = new DefaultDirectoryService();
        dnsConfiguration = new DnsServer();
        dnsConfiguration.setDirectoryService( directoryService );
        dnsConfiguration.setEnabled( true );
        dnsConfiguration.setTransports( new TcpTransport( 10053 ), new UdpTransport( 10053 ) );
        dnsConfiguration.start();
    }


    protected void shutdown()
    {
        LOG.debug( "Stopping the DNS server" );
        dnsConfiguration.stop();
    }
}
