/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.dhcp.protocol;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.dhcp.DhcpService;
import org.apache.dhcp.messages.DhcpMessage;
import org.apache.dhcp.service.DhcpServiceImpl;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.DatagramConnector;


public class DhcpProtocolHandler implements IoHandler
{
    public void sessionCreated( IoSession session ) throws Exception
    {
        System.out.println( session.getRemoteAddress() + " CREATED" );
        session.getFilterChain().addFirst(
                "codec",
                new ProtocolCodecFilter( new DhcpProtocolCodecFactory() ) );
    }

    public void sessionOpened( IoSession session )
    {
        System.out.println( session.getRemoteAddress() + " OPENED" );
    }

    public void sessionClosed( IoSession session )
    {
        System.out.println( session.getRemoteAddress() + " CLOSED" );
    }

    public void sessionIdle( IoSession session, IdleStatus status )
    {
        System.out.println( session.getRemoteAddress() + " IDLE(" + status + ")" );
    }

    public void exceptionCaught( IoSession session, Throwable cause )
    {
        System.out.println( session.getRemoteAddress() + " EXCEPTION" );
        cause.printStackTrace( System.out );

        session.close();
    }

    public void messageReceived( IoSession session, Object message ) throws Exception
    {
        System.out.println( session.getRemoteAddress() + " RCVD: " + message );
        
        DhcpMessage request = (DhcpMessage)message;
        
        if ( request.getOpCode() == 1 )
        {
            DhcpService dhcpService = new DhcpServiceImpl();
            DhcpMessage reply = dhcpService.getReplyFor( request );
            
        	int PORT = 68;
            IoConnector connector = new DatagramConnector();
            InetAddress broadcast = InetAddress.getByName( null );
            ConnectFuture future = connector.connect( new InetSocketAddress( broadcast, PORT ), new DhcpProtocolHandler() );
            future.join();
            IoSession replySession = future.getSession();
            replySession.write( reply ).join();
            replySession.close();
        }
    }

    public void messageSent( IoSession session, Object message )
    {
        System.out.println( session.getRemoteAddress() + " SENT: " + message );
    }
}

