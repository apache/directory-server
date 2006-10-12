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

import org.apache.directory.mitosis.service.protocol.message.BaseMessage;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;

public abstract class BaseMessageDecoder implements MessageDecoder
{
    private final int type;
    private final int minBodyLength;
    private final int maxBodyLength;
    private boolean readHeader;
    private int sequence;
    private int bodyLength;

    protected BaseMessageDecoder( int type, int minBodyLength, int maxBodyLength )
    {
        this.type = type;
        this.minBodyLength = minBodyLength;
        this.maxBodyLength = maxBodyLength;
    }

    public final MessageDecoderResult decodable( IoSession session, ByteBuffer buf )
    {
        return type == buf.get()? OK : NOT_OK;
    }

    public final MessageDecoderResult decode( IoSession session, ByteBuffer in,
                                              ProtocolDecoderOutput out ) throws Exception
    {
        if( !readHeader )
        {
            if( in.remaining() < 9 )
            {
                return NEED_DATA;
            }
            
            in.get(); // skip type field
            sequence = in.getInt();
            bodyLength = in.getInt();
            
            if( bodyLength < minBodyLength || bodyLength > maxBodyLength )
            {
                throw new ProtocolDecoderException( "Wrong bodyLength: " + bodyLength );
            }
            
            readHeader = true;
        }
        
        if( readHeader )
        {
            if( in.remaining() < bodyLength )
            {
                return NEED_DATA;
            }
            
            int oldLimit = in.limit();
            
            try
            {
                in.limit( in.position() + bodyLength );
                out.write( decodeBody( sequence, bodyLength, in ) );
                return OK;
            }
            finally
            {
                readHeader = false;
                in.limit( oldLimit );
            }
        }
        
        throw new InternalError();
    }
    
    protected abstract BaseMessage decodeBody( int sequence, int bodyLength, ByteBuffer in ) throws Exception;
}
