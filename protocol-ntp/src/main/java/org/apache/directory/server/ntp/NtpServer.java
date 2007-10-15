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


import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.directory.server.ntp.protocol.NtpProtocolHandler;
import org.apache.directory.server.protocol.shared.ServiceConfiguration;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.transport.socket.nio.DatagramAcceptor;
import org.apache.mina.transport.socket.nio.DatagramAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;


/**
 * Contains the configuration parameters for the NTP protocol provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 * @org.apache.xbean.XBean
 */
public class NtpServer extends ServiceConfiguration
{
    private static final long serialVersionUID = 2961795205765175775L;

    /**
     * The default IP port.
     */
    private static final int IP_PORT_DEFAULT = 123;

    /**
     * The default service pid.
     */
    private static final String SERVICE_PID_DEFAULT = "org.apache.directory.server.ntp";

    /**
     * The default service name.
     */
    private static final String SERVICE_NAME_DEFAULT = "ApacheDS NTP Service";

    /** DatagramAcceptor input for this server */
    private DatagramAcceptor datagramAcceptor;

    /** SocketAcceptor input for this server */
    private SocketAcceptor socketAcceptor;


    /**
     * Creates a new instance of NtpConfiguration.
     */
    public NtpServer()
    {
        super.setIpPort( IP_PORT_DEFAULT );
        super.setServicePid( SERVICE_PID_DEFAULT );
        super.setServiceName( SERVICE_NAME_DEFAULT );
    }

    /**
     * Returns the DatagramAcceptor input for this server
     * @return DatagramAcceptor input for this server
     */
    public DatagramAcceptor getDatagramAcceptor()
    {
        return datagramAcceptor;
    }

    /**
     * Set the DatagramAcceptor for this server
     * @param datagramAcceptor the DatagramAcceptor input for this server
     */
    public void setDatagramAcceptor( DatagramAcceptor datagramAcceptor )
    {
        this.datagramAcceptor = datagramAcceptor;
    }

    /**
     * Returns the SocketAcceptor for this server
     * @return SocketAcceptor input for this server
     */
    public SocketAcceptor getSocketAcceptor()
    {
        return socketAcceptor;
    }

    /**
     * Set the SocketAcceptor for this server
     * @param socketAcceptor the SocketAcceptor input for this server
     */
    public void setSocketAcceptor( SocketAcceptor socketAcceptor )
    {
        this.socketAcceptor = socketAcceptor;
    }
    /**
     * @org.apache.xbean.InitMethod
     */
    public void start() throws IOException
    {
        //If appropriate, the udp and tcp servers could be enabled with boolean flags.
        if ( datagramAcceptor != null )
        {
            DatagramAcceptorConfig udpConfig = new DatagramAcceptorConfig();
            datagramAcceptor.bind( new InetSocketAddress( getIpPort() ), new NtpProtocolHandler(), udpConfig );
        }

        if ( socketAcceptor != null )
        {
            SocketAcceptorConfig tcpConfig = new SocketAcceptorConfig();
            tcpConfig.setDisconnectOnUnbind( false );
            tcpConfig.setReuseAddress( true );
            socketAcceptor.bind( new InetSocketAddress( getIpPort() ), new NtpProtocolHandler(), tcpConfig );
        }
    }

    /**
     * @org.apache.xbean.DestroyMethod
     */
    public void stop()
    {
        if ( datagramAcceptor != null )
        {
            datagramAcceptor.unbind( new InetSocketAddress( getIpPort() ));
        }
        if ( socketAcceptor != null )
        {
            socketAcceptor.unbind( new InetSocketAddress( getIpPort() ));
        }
    }

}
