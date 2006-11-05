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


import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.service.protocol.Constants;
import org.apache.directory.mitosis.service.protocol.message.BaseMessage;
import org.apache.directory.mitosis.service.protocol.message.LoginAckMessage;


public class LoginAckMessageDecoder extends ResponseMessageDecoder
{
    private final CharsetDecoder utf8decoder = Charset.forName( "UTF-8" ).newDecoder();


    public LoginAckMessageDecoder()
    {
        super( Constants.LOGIN_ACK, 1, 64 );
    }


    protected BaseMessage decodeBody( int sequence, int bodyLength, int responseCode, ByteBuffer in ) throws Exception
    {
        return new LoginAckMessage( sequence, responseCode, new ReplicaId( in.getString( utf8decoder ) ) );
    }


    public void finishDecode( IoSession session, ProtocolDecoderOutput out ) throws Exception
    {
    }
}
