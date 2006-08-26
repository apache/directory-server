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

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.DhcpMessageModifier;
import org.apache.directory.server.dhcp.messages.MessageType;


public class DhcpMessageDecoder
{
    /**
     * Convert a byte buffer into a DhcpMessage.
     * 
     * @return a DhcpMessage.
     * @param buffer ByteBuffer to convert to a DhcpMessage object
     */
    public DhcpMessage decode( ByteBuffer buffer ) throws DhcpException
    {
        /**
         * TODO - need to figure out why the buffer needs to be rewound.
         */
        buffer.rewind();

        DhcpMessageModifier modifier = new DhcpMessageModifier();

        modifier.setMessageType( MessageType.DHCPDISCOVER );

        modifier.setOpCode( buffer.get() );
        modifier.setHardwareAddressType( buffer.get() );

        short hardwareAddressLength = ( short ) ( buffer.get() & 0xff );

        modifier.setHardwareAddressLength( ( byte ) hardwareAddressLength );
        modifier.setHardwareOptions( buffer.get() );

        modifier.setTransactionId( buffer.getInt() );
        modifier.setSeconds( buffer.getShort() );
        modifier.setFlags( buffer.getShort() );

        byte[] nextFourBytes = new byte[4];

        buffer.get( nextFourBytes );
        modifier.setActualClientAddress( nextFourBytes );

        buffer.get( nextFourBytes );
        modifier.setAssignedClientAddress( nextFourBytes );

        buffer.get( nextFourBytes );
        modifier.setNextServerAddress( nextFourBytes );

        buffer.get( nextFourBytes );
        modifier.setRelayAgentAddress( nextFourBytes );

        byte[] clientHardwareAddress = new byte[16];

        buffer.get( clientHardwareAddress );
        modifier.setClientHardwareAddress( clientHardwareAddress );

        byte[] serverHostname = new byte[64];
        buffer.get( serverHostname );
        modifier.setServerHostname( serverHostname );

        byte[] bootFileName = new byte[128];
        buffer.get( bootFileName );
        modifier.setBootFileName( bootFileName );

        DhcpOptionsDecoder decoder = new DhcpOptionsDecoder();
        modifier.setOptions( decoder.decode( buffer ) );

        return modifier.getDhcpMessage();
    }
}
