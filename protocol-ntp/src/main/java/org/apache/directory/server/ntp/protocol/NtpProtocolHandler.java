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

package org.apache.directory.server.ntp.protocol;


import org.apache.directory.server.ntp.NtpService;
import org.apache.directory.server.ntp.messages.NtpMessage;
import org.apache.directory.server.ntp.service.NtpServiceImpl;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NtpProtocolHandler implements IoHandler
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( NtpProtocolHandler.class );

    private NtpService ntpService = new NtpServiceImpl();


    public void sessionCreated( IoSession session ) throws Exception
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( session.getRemoteAddress() + " CREATED" );
        }

        session.getFilterChain().addFirst( "codec", new ProtocolCodecFilter( NtpProtocolCodecFactory.getInstance() ) );
    }


    public void sessionOpened( IoSession session )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( session.getRemoteAddress() + " OPENED" );
        }
    }


    public void sessionClosed( IoSession session )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( session.getRemoteAddress() + " CLOSED" );
        }
    }


    public void sessionIdle( IoSession session, IdleStatus status )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( session.getRemoteAddress() + " IDLE(" + status + ")" );
        }
    }


    public void exceptionCaught( IoSession session, Throwable cause )
    {
        log.error( session.getRemoteAddress() + " EXCEPTION", cause );
        session.close();
    }


    public void messageReceived( IoSession session, Object message )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( session.getRemoteAddress() + " RCVD: " + message );
        }

        NtpMessage reply = ntpService.getReplyFor( ( NtpMessage ) message );

        session.write( reply );
    }


    public void messageSent( IoSession session, Object message )
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( session.getRemoteAddress() + " SENT: " + message );
        }
    }
}
