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
package org.apache.directory.mitosis.service.protocol.codec;


import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import junit.framework.Assert;

import org.apache.directory.mitosis.service.protocol.message.BaseMessage;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterChain;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.IoSessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.common.support.BaseIoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.apache.mina.filter.codec.support.SimpleProtocolEncoderOutput;
import org.junit.BeforeClass;
import org.junit.Test;


public abstract class AbstractMessageCodecTest
{
    private final BaseMessage message;
    private final MessageEncoder encoder;
    private final MessageDecoder decoder;


    private static DefaultDirectoryService service;

    @BeforeClass
    public static void setUp()
    {
        service = new DefaultDirectoryService();
    }
    
    
    protected AbstractMessageCodecTest( BaseMessage message, MessageEncoder encoder, MessageDecoder decoder )
    {
        if ( message == null )
        {
            throw new NullPointerException( "message" );
        }
        if ( encoder == null )
        {
            throw new NullPointerException( "encoder" );
        }
        if ( decoder == null )
        {
            throw new NullPointerException( "decoder" );
        }

        this.message = message;
        this.encoder = encoder;
        this.decoder = decoder;
    }


    @Test 
    public void testMessageCodec() throws Exception
    {
        SimpleProtocolEncoderOutput encoderOut = new SimpleProtocolEncoderOutput()
        {
            protected WriteFuture doFlush( ByteBuffer buf )
            {
                return null;
            }

        };
        
        IoSession session = new  DummySession();
        
        session.setAttribute( "registries", service.getRegistries() );
        encoder.encode( session, message, encoderOut );
        ByteBuffer buf = encoderOut.getBufferQueue().poll();

        buf.mark();
        Assert.assertTrue( decoder.decodable( null, buf ) == MessageDecoder.OK );
        buf.reset();

        ProtocolDecoderOutputImpl decoderOut = new ProtocolDecoderOutputImpl();
        decoder.decode( session, buf, decoderOut );

        Assert.assertTrue( compare( message, ( BaseMessage ) decoderOut.messages.poll() ) );
    }


    protected boolean compare( BaseMessage expected, BaseMessage actual )
    {
        return expected.equals( actual );
    }

    private class ProtocolDecoderOutputImpl implements ProtocolDecoderOutput
    {
        private final Queue<Object> messages = new LinkedBlockingQueue<Object>();


        public void flush()
        {
        }


        public void write( Object message )
        {
            messages.add( message );
        }
    }


    protected static class DummySession extends BaseIoSession
    {
        Object message;


        @Override
        public WriteFuture write( Object message )
        {
            this.message = message;

            return super.write( message );
        }


        protected Object getMessage()
        {
            return message;
        }


        protected void updateTrafficMask()
        {
            // Do nothing.
        }


        public IoService getService()
        {
            return null;
        }


        public IoHandler getHandler()
        {
            return null;
        }


        public IoFilterChain getFilterChain()
        {
            return null;
        }


        public TransportType getTransportType()
        {
            return null;
        }


        public SocketAddress getRemoteAddress()
        {
            return new InetSocketAddress( 10088 );
        }


        public SocketAddress getLocalAddress()
        {
            return null;
        }


        public IoSessionConfig getConfig()
        {
            return null;
        }


        public int getScheduledWriteRequests()
        {
            return 0;
        }


        public SocketAddress getServiceAddress()
        {
            return null;
        }


        public IoServiceConfig getServiceConfig()
        {
            return null;
        }


        public int getScheduledWriteBytes()
        {
            return 0;
        }
    }
}
