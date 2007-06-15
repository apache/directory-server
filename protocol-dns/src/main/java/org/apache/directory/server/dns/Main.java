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


import org.apache.directory.server.dns.store.RecordStore;
import org.apache.directory.server.dns.store.RecordStoreStub;
import org.apache.mina.common.ExecutorThreadModel;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Main
{
    /** Logger for this class */
    private static final Logger log = LoggerFactory.getLogger( Main.class );

    private static final int MAX_THREADS_DEFAULT = 4;

    protected static IoAcceptor udpAcceptor;
    protected static ThreadPoolExecutor threadPoolExecutor;
    protected static ExecutorThreadModel threadModel = ExecutorThreadModel.getInstance( "ApacheDS" );

    private static DnsServer udpDnsServer;


    /**
     * Entry point for the DNS server.
     *
     * @param args
     * @throws Exception
     */
    public static void main( String[] args ) throws Exception
    {
        int maxThreads = MAX_THREADS_DEFAULT;
        threadPoolExecutor = new ThreadPoolExecutor( maxThreads, maxThreads, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue() );
        threadModel.setExecutor( threadPoolExecutor );

        udpAcceptor = new DatagramAcceptor();

        new Main().go();
    }


    /**
     * Start an instance of the DNS server.
     */
    public void go()
    {
        DnsConfiguration dnsConfiguration = new DnsConfiguration();
        dnsConfiguration.setEnabled( true );
        dnsConfiguration.setIpPort( 10053 );

        RecordStore store = new RecordStoreStub();

        startup( dnsConfiguration, store );
    }


    private void startup( DnsConfiguration dnsConfig, RecordStore store )
    {
        // Skip if disabled
        if ( !dnsConfig.isEnabled() )
        {
            return;
        }

        try
        {
            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            udpConfig.setThreadModel( threadModel );

            udpDnsServer = new DnsServer( dnsConfig, udpAcceptor, udpConfig, store );
        }
        catch ( Throwable t )
        {
            log.error( "Failed to start the DNS service", t );
        }
    }


    protected void shutdown()
    {
        if ( udpDnsServer != null )
        {
            udpDnsServer.destroy();

            if ( log.isInfoEnabled() )
            {
                log.info( "Unbind of DNS Service complete: " + udpDnsServer );
            }

            udpDnsServer = null;
        }
    }
}
