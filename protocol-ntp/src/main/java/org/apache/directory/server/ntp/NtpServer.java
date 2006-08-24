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
import java.util.Dictionary;

import org.apache.directory.server.ntp.protocol.NtpProtocolHandler;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NtpServer
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( NtpServer.class );

    private NtpConfiguration config;
    private IoAcceptor acceptor;

    private IoHandler handler;


    public NtpServer( NtpConfiguration config, IoAcceptor acceptor )
    {
        this.config = config;
        this.acceptor = acceptor;

        String name = config.getName();
        int port = config.getPort();

        try
        {
            handler = new NtpProtocolHandler();

            acceptor.bind( new InetSocketAddress( port ), handler );

            log.debug( name + " listening on port " + port );
        }
        catch ( IOException ioe )
        {
            log.error( ioe.getMessage(), ioe );
        }
    }


    public boolean isDifferent( Dictionary newConfig )
    {
        return config.isDifferent( newConfig );
    }


    public void destroy()
    {
        acceptor.unbind( new InetSocketAddress( config.getPort() ) );

        acceptor = null;
        handler = null;

        log.debug( config.getName() + " has stopped listening on port " + config.getPort() );
    }
}
