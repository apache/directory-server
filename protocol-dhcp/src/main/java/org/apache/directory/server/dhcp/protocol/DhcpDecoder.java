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


import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.io.DhcpMessageDecoder;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;


public class DhcpDecoder implements ProtocolDecoder
{
    public void decode( IoSession session, ByteBuffer in, ProtocolDecoderOutput out ) throws DhcpException
    {
        DhcpMessageDecoder decoder = new DhcpMessageDecoder();
        out.write( decoder.decode( in.buf() ) );
    }


    public void dispose( IoSession arg0 ) throws Exception
    {
    }


    public void finishDecode( IoSession session, ProtocolDecoderOutput out ) throws Exception
    {
        // TODO Auto-generated method stub
    }
}
