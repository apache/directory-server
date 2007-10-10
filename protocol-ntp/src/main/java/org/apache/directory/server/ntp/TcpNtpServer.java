/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.directory.server.ntp;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.directory.server.configuration.ApacheDS;
import org.apache.directory.server.ntp.protocol.NtpProtocolHandler;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;

/**
 * @version $Rev$ $Date$
 * @org.apache.xbean.XBean
 */
public class TcpNtpServer extends AbstractNtpServer
{
    /**
     * Creates a new instance of NtpConfiguration.
     */
    public TcpNtpServer( ApacheDS apacheDS )
    {
        super( apacheDS );
    }

    /**
     * @org.apache.xbean.InitMethod
     */
    public void start() throws IOException
        {
        SocketAcceptorConfig tcpConfig = getTcpConfig();
        getApacheDS().getTcpAcceptor().bind( new InetSocketAddress( getIpPort() ), new NtpProtocolHandler(), tcpConfig );
    }

    /**
     * @org.apache.xbean.DestroyMethod
     */
    public void stop()
    {
        getApacheDS().getTcpAcceptor().unbind( new InetSocketAddress( getIpPort() ) );
    }

}
