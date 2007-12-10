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

package org.apache.directory.server.dhcp.protocol;


import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.directory.server.dhcp.DhcpService;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.service.DhcpServiceImpl;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.DatagramConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DhcpProtocolHandler implements IoHandler
{
    private static final Logger log = LoggerFactory.getLogger( DhcpProtocolHandler.class );


    public void sessionCreated( IoSession session ) throws Exception
    {
        log.debug( "{} CREATED", session.getRemoteAddress() );
        session.getFilterChain().addFirst( "codec", new ProtocolCodecFilter( new DhcpProtocolCodecFactory() ) );
    }


    public void sessionOpened( IoSession session )
    {
        log.debug( "{} OPENED", session.getRemoteAddress() );
    }


    public void sessionClosed( IoSession session )
    {
        log.debug( "{} CLOSED", session.getRemoteAddress() );
    }


    public void sessionIdle( IoSession session, IdleStatus status )
    {
        log.debug( "{} IDLE ({})", session.getRemoteAddress(), status );
    }


    public void exceptionCaught( IoSession session, Throwable cause )
    {
        log.debug( session.getRemoteAddress() + " EXCEPTION", cause );
        session.close();
    }


    public void messageReceived( IoSession session, Object message ) throws Exception
    {
        log.debug( "{} RCVD:  {}", session.getRemoteAddress(), message );

        DhcpMessage request = ( DhcpMessage ) message;

        if ( request.getOpCode() == 1 )
        {
            DhcpService dhcpService = new DhcpServiceImpl();
            DhcpMessage reply = dhcpService.getReplyFor( request );

            int PORT = 68;
            IoConnector connector = new DatagramConnector();
            InetAddress broadcast = InetAddress.getByName( null );

            ConnectFuture future = connector.connect( new InetSocketAddress( broadcast, PORT ),
                new DhcpProtocolHandler() );
            future.join();
            IoSession replySession = future.getSession();
            replySession.write( reply ).join();
            replySession.close();
        }
    }


    public void messageSent( IoSession session, Object message )
    {
        log.debug( "{} SENT:  {}", session.getRemoteAddress(), message );
    }
}
