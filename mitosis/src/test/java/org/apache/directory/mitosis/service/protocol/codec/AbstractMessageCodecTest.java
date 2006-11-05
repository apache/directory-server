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


import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.mitosis.service.protocol.message.BaseMessage;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.apache.mina.filter.codec.support.SimpleProtocolEncoderOutput;
import org.apache.mina.util.Queue;


public abstract class AbstractMessageCodecTest extends TestCase
{
    private final BaseMessage message;
    private final MessageEncoder encoder;
    private final MessageDecoder decoder;


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


    public void testMessageCodec() throws Exception
    {
        SimpleProtocolEncoderOutput encoderOut = new SimpleProtocolEncoderOutput()
        {
            protected WriteFuture doFlush( ByteBuffer buf )
            {
                return null;
            }

        };
        encoder.encode( null, message, encoderOut );
        ByteBuffer buf = ( ByteBuffer ) encoderOut.getBufferQueue().pop();

        buf.mark();
        Assert.assertTrue( decoder.decodable( null, buf ) == MessageDecoder.OK );
        buf.reset();

        ProtocolDecoderOutputImpl decoderOut = new ProtocolDecoderOutputImpl();
        decoder.decode( null, buf, decoderOut );

        Assert.assertTrue( compare( message, ( BaseMessage ) decoderOut.messages.pop() ) );
    }


    protected boolean compare( BaseMessage expected, BaseMessage actual )
    {
        return expected.equals( actual );
    }

    private class ProtocolDecoderOutputImpl implements ProtocolDecoderOutput
    {
        private final Queue messages = new Queue();


        public void flush()
        {
        }


        public void write( Object message )
        {
            messages.push( message );
        }
    }
}
