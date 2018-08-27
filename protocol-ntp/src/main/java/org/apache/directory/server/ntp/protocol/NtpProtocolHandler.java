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
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The NTP protocol handler. It implements the {@link org.apache.mina.core.service.IoHandler#messageReceived} method,
 * which returns the NTP reply. The {@link org.apache.mina.core.service.IoHandler#exceptionCaught} is also implemented,
 * all the other methods are handled by the {@link IoHandlerAdapter} class.<br>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NtpProtocolHandler extends IoHandlerAdapter
{
    /** the log for this class */
    private static final Logger LOG = LoggerFactory.getLogger( NtpProtocolHandler.class );

    /** The NtpService instance */
    private NtpService ntpService = new NtpServiceImpl();


    /**
     * {@inheritDoc}
     */
    @Override
    public void exceptionCaught( IoSession session, Throwable cause )
    {
        LOG.error( session.getRemoteAddress() + " EXCEPTION", cause );
        session.closeNow();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void messageReceived( IoSession session, Object message )
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "{} RCVD:  {}", session.getRemoteAddress(), message );
        }

        NtpMessage reply = ntpService.getReplyFor( ( NtpMessage ) message );

        session.write( reply );
    }
}
