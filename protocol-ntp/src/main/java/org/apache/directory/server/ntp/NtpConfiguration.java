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


import java.net.InetSocketAddress;
import java.io.IOException;

import org.apache.directory.server.protocol.shared.ServiceConfiguration;
import org.apache.directory.server.ntp.protocol.NtpProtocolHandler;
import org.apache.directory.server.configuration.ApacheDS;
import org.apache.directory.server.core.DirectoryService;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.common.ThreadModel;


/**
 * Contains the configuration parameters for the NTP protocol provider.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NtpConfiguration extends ServiceConfiguration
{
    private static final long serialVersionUID = 2961795205765175775L;

    /** The default IP port. */
    private static final int IP_PORT_DEFAULT = 123;

    /** The default service pid. */
    private static final String SERVICE_PID_DEFAULT = "org.apache.directory.server.ntp";

    /** The default service name. */
    private static final String SERVICE_NAME_DEFAULT = "ApacheDS NTP Service";

    private final ApacheDS apacheDS;

    /**
     * Creates a new instance of NtpConfiguration.
     */
    public NtpConfiguration( ApacheDS apacheDS )
    {
        this.apacheDS = apacheDS;
        super.setIpPort( IP_PORT_DEFAULT );
        super.setServicePid( SERVICE_PID_DEFAULT );
        super.setServiceName( SERVICE_NAME_DEFAULT );
    }

    /**
     * @org.apache.xbean.InitMethod
     */
    public void start() throws IOException
    {
        //If appropriate, the udp and tcp servers could be enabled with boolean flags.
        DatagramAcceptorConfig udpConfig = getUdpConfig();
        apacheDS.getUdpAcceptor().bind( new InetSocketAddress( getIpPort() ), new NtpProtocolHandler(), udpConfig );

        SocketAcceptorConfig tcpConfig = getTcpConfig();
        apacheDS.getTcpAcceptor().bind( new InetSocketAddress( getIpPort() ), new NtpProtocolHandler(), tcpConfig );
    }

    /**
     * @org.apache.xbean.DestroyMethod
     */
    public void stop()
    {
        apacheDS.getUdpAcceptor().unbind( new InetSocketAddress( getIpPort() ) );
        apacheDS.getTcpAcceptor().unbind( new InetSocketAddress( getIpPort() ) );
    }

    private SocketAcceptorConfig getTcpConfig()
    {
        SocketAcceptorConfig tcpConfig = new SocketAcceptorConfig();
        tcpConfig.setDisconnectOnUnbind( false );
        tcpConfig.setReuseAddress( true );
        tcpConfig.setThreadModel( ThreadModel.MANUAL );
        return tcpConfig;
    }

    private DatagramAcceptorConfig getUdpConfig()
    {
        DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
        udpConfig.setThreadModel( ThreadModel.MANUAL );
        return udpConfig;
    }

}
