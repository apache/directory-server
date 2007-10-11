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

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.directory.server.protocol.shared.DatagramAcceptor;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.mina.util.AvailablePortFinder;


/**
 * An {@link AbstractServerTest} testing the Network Time Protocol (NTP).
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NtpITest extends TestCase
{
    private NtpConfiguration ntpConfig;
    private int port;


    /**
     * Set up a partition for EXAMPLE.COM and enable the NTP service.  The LDAP service is disabled.
     */
    public void setUp() throws Exception
    {
        DatagramAcceptor datagramAcceptor = new DatagramAcceptor( null );
        ntpConfig = new NtpConfiguration( datagramAcceptor, null );
        ntpConfig.setEnabled( true );
        port = AvailablePortFinder.getNextAvailable( 10123 );
        ntpConfig.setIpPort( port );
        ntpConfig.start();

    }

    /**
     * Tests to make sure NTP works when enabled in the server.
     *
     * @throws Exception if there are errors
     */
    public void testNtp() throws Exception
    {
        long currentTime = System.currentTimeMillis();

        InetAddress host = InetAddress.getByName( null );

        NTPUDPClient ntp = new NTPUDPClient();
        ntp.setDefaultTimeout( 5000 );

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
    public void tearDown() throws Exception
    {
        ntpConfig.stop();
    }
}
