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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * A simple integration test which uses the Apache Commons Net client to talk to the NTP server. 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NtpServerUdpClientTest
{

    private NtpServer ntpServer = null;

    protected int port = 0;

    public static final long DELTA_MS = 2500;


    /** Starts the NTP server on UDP, uses the first port available starting from 1024.
     */
    @Before public void startNTPServerOnUDP()
    {

        NtpConfiguration ntpCfg = new NtpConfiguration();

        port = AvailablePortFinder.getNextAvailable( 1024 );
        ntpCfg.setIpPort( port );

        DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
        udpConfig.setThreadModel( ThreadModel.MANUAL );

        ExecutorService logicExecutor = Executors.newFixedThreadPool( 10 );

        DatagramAcceptor udpAcceptor = new DatagramAcceptor();
        udpAcceptor.getFilterChain().addLast( "executor", new ExecutorFilter( logicExecutor ) );

        ntpServer = new NtpServer( ntpCfg, udpAcceptor, udpConfig );
    }


    /**
     * Uses the NTPUDPClient class from Apache Commons Net to request the current time.
     * 
     * @throws UnknownHostException
     * @throws IOException
     */
    @Test public void shouldReturnCurrentTimeToClient() throws UnknownHostException, IOException
    {

        // Get time via client call
        NTPUDPClient client = new NTPUDPClient();
        InetAddress localhost = InetAddress.getByName( null );
        TimeInfo time = client.getTime( localhost, port );
        long returnTime = time.getReturnTime();

        long now = System.currentTimeMillis();

        // Check whether the time returned by the server is close to system time. 
        long delta = Math.abs( now - returnTime );
        assertTrue( delta < DELTA_MS );
    }


    /** Shuts the NTP server down
     */
    @After public void shutdownNTPServer()
    {
        ntpServer.destroy();
    }
}
