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


import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.concurrent.Executors;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


/**
 * An test testing the Network Time Protocol (NTP).
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NtpITest
{
    private NtpServer ntpConfig;
    private int port;


    /**
     * Set up a partition for EXAMPLE.COM and enable the NTP service.  The LDAP service is disabled.
     */
    @Before
    public void setUp() throws Exception
    {
        ntpConfig = new NtpServer();
        port = AvailablePortFinder.getNextAvailable();
        TcpTransport tcpTransport = new TcpTransport( port );
        UdpTransport udpTransport = new UdpTransport( port );
        ntpConfig.setTransports( tcpTransport, udpTransport );
        ntpConfig.getDatagramAcceptor( udpTransport ).getFilterChain().addLast( "executor",
            new ExecutorFilter( Executors.newCachedThreadPool() ) );
        ntpConfig.getSocketAcceptor( tcpTransport ).getFilterChain().addLast( "executor",
            new ExecutorFilter( Executors.newCachedThreadPool() ) );
        ntpConfig.setEnabled( true );
        ntpConfig.start();
    }


    /**
     * Tests to make sure NTP works when enabled in the server.
     *
     * @throws Exception if there are errors
     */
    @Test
    @Ignore
    // Fails with a timeout !!!
    public void testNtp() throws Exception
    {
        InetAddress host = InetAddress.getByName( null );

        NTPUDPClient ntp = new NTPUDPClient();
        ntp.setDefaultTimeout( 500000 );

        long currentTime = System.currentTimeMillis();
        TimeInfo timeInfo = ntp.getTime( host, port );
        long returnTime = timeInfo.getReturnTime();
        assertTrue( Math.abs( currentTime - returnTime ) < 1000 );

        timeInfo.computeDetails();

        String offsetMsg = "Expected offset in range (-1000, 1000), but was " + timeInfo.getOffset();
        assertTrue( offsetMsg, -1000 < timeInfo.getOffset() && timeInfo.getOffset() < 1000 );
        String delayMsg = "Expected delay in range [0, 1000), but was " + timeInfo.getOffset();
        assertTrue( delayMsg, 0 <= timeInfo.getDelay() && timeInfo.getDelay() < 1000 );
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
