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
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;

public abstract class BaseMessageEncoder implements MessageEncoder
{
    public BaseMessageEncoder()
    {
    }

    public final void encode( IoSession session, Object in, ProtocolEncoderOutput out) throws Exception
    {
        BaseMessage m = ( BaseMessage ) in;
        ByteBuffer buf = ByteBuffer.allocate( 16 );
        buf.setAutoExpand( true );
        buf.put( ( byte ) m.getType() );
        buf.putInt( m.getSequence() );
        buf.putInt( 0 ); // placeholder for body length field

        final int bodyStartPos = buf.position();
        encodeBody( m, buf );
        final int bodyEndPos = buf.position();
        final int bodyLength = bodyEndPos - bodyStartPos;
        
        // fill bodyLength
        buf.position( bodyStartPos - 4 );
        buf.putInt( bodyLength );
        buf.position( bodyEndPos );
        
        buf.flip();
        out.write( buf );
    }
    
    protected abstract void encodeBody( BaseMessage in, ByteBuffer out ) throws Exception;
}
