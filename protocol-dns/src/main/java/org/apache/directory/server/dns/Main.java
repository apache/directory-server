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


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.directory.server.dns.store.RecordStore;
import org.apache.directory.server.dns.store.RecordStoreStub;
import org.apache.directory.server.configuration.ApacheDS;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.mina.common.ExecutorThreadModel;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Main
{
    /** Logger for this class */
    private static final Logger log = LoggerFactory.getLogger( Main.class );

    private static DnsConfiguration dnsConfiguration;

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
    public void go()
    {
        ApacheDS apacheDS = new ApacheDS();
        DirectoryService directoryService = new DefaultDirectoryService();
        dnsConfiguration = new DnsConfiguration( apacheDS, directoryService );
        dnsConfiguration.setEnabled( true );
        dnsConfiguration.setIpPort( 10053 );

        RecordStore store = new RecordStoreStub();

    }


    protected void shutdown()
    {
        dnsConfiguration.stop();
    }
}
