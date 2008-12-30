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


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * An {@link AbstractServerTest} testing the Network Time Protocol (NTP).
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NtpITest extends TestCase
{
    private NtpServer ntpConfig;
    private int port;


    /**
     * Set up a partition for EXAMPLE.COM and enable the NTP service.  The LDAP service is disabled.
     */
    @Before
    public void setUp() throws Exception
    {
        ntpConfig = new NtpServer( );
        port = AvailablePortFinder.getNextAvailable( 10123 );
        ntpConfig.setTcpTransport( new TcpTransport( port ) );
        ntpConfig.setUdpTransport( new UdpTransport( port ) );
        ntpConfig.getDatagramAcceptor().getFilterChain().addLast( "executor", new ExecutorFilter( Executors.newCachedThreadPool() ) );
        ntpConfig.getSocketAcceptor().getFilterChain().addLast( "executor", new ExecutorFilter( Executors.newCachedThreadPool() ) );
        ntpConfig.setEnabled( true );
        ntpConfig.start();

    }

    /**
     * Tests to make sure NTP works when enabled in the server.
     *
     * @throws Exception if there are errors
     */
    @Test
    public void testNtp() throws Exception
    {
        long currentTime = System.currentTimeMillis();

        InetAddress host = InetAddress.getByName( null );

        NTPUDPClient ntp = new NTPUDPClient();
        ntp.setDefaultTimeout( 500000 );

        TimeInfo timeInfo = ntp.getTime( host, port );
        long returnTime = timeInfo.getReturnTime();
        Assert.assertTrue( currentTime - returnTime < 1000 );

        timeInfo.computeDetails();

        Assert.assertTrue( 0 < timeInfo.getOffset() && timeInfo.getOffset() < 1000 );
        Assert.assertTrue( 0 < timeInfo.getDelay() && timeInfo.getDelay() < 1000 );
    }


    /**
     * Tear down.
     */
    @After
    public void tearDown() throws Exception
    {
        ntpConfig.stop();
    }
}
