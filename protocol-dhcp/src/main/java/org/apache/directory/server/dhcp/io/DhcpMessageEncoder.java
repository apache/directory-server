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

package org.apache.directory.server.dhcp.io;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.options.OptionsField;


public class DhcpMessageEncoder
{
    /**
     * Converts a DhcpMessage object into a byte buffer.
     * 
     * @param byteBuffer ByteBuffer to put DhcpMessage into
     * @param message DhcpMessage to encode into ByteBuffer
     */
    public void encode( ByteBuffer byteBuffer, DhcpMessage message )
    {
        byteBuffer.put( message.getOpCode() );
        byteBuffer.put( message.getHardwareAddressType() );
        byteBuffer.put( message.getHardwareAddressLength() );
        byteBuffer.put( message.getHardwareOptions() );
        byteBuffer.putInt( message.getTransactionId() );
        byteBuffer.putShort( message.getSeconds() );
        byteBuffer.putShort( message.getFlags() );
        byteBuffer.put( message.getActualClientAddress() );
        byteBuffer.put( message.getAssignedClientAddress() );
        byteBuffer.put( message.getNextServerAddress() );
        byteBuffer.put( message.getRelayAgentAddress() );
        byteBuffer.put( message.getClientHardwareAddress() );
        byteBuffer.put( message.getServerHostname() );
        byteBuffer.put( message.getBootFileName() );

        OptionsField options = message.getOptions();

        DhcpOptionsEncoder optionsEncoder = new DhcpOptionsEncoder();

        optionsEncoder.encode( options, byteBuffer );
    }
}
