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
package org.apache.directory.server.protocol.shared;


import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.executor.ExecutorFilter;


/**
 * Extension around a MINA DatagramAcceptor to facilitate better usage with Spring.
 *
 * @version $Rev$ $Date$
 * @org.apache.xbean.XBean
 */
public class DatagramAcceptor extends org.apache.mina.transport.socket.nio.DatagramAcceptor
{
    private static final int DEFAULT_THREADS = 10;


    public DatagramAcceptor( Executor logicExecutor )
    {
        super();
        if ( logicExecutor == null )
        {
            logicExecutor = Executors.newFixedThreadPool( DEFAULT_THREADS );
        }
        getFilterChain().addLast( "executor", new ExecutorFilter( logicExecutor ) );
    }


    public void bind( SocketAddress address, IoHandler ioHandler, IoServiceConfig udpConfig ) throws IOException
    {
        udpConfig.setThreadModel( ThreadModel.MANUAL );
        super.bind( address, ioHandler, udpConfig );
    }


    public void unbind( SocketAddress address )
    {
        super.unbind(address);
    }

}
