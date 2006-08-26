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

package org.apache.directory.server.dhcp.options.perinterface;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


/**
 * This option specifies the broadcast address in use on the client's
 * subnet.
 * 
 * The code for this option is 28, and its length is 4.
 */
public class BroadcastAddress extends DhcpOption
{
    private byte[] broadcastAddress;


    public BroadcastAddress(byte[] broadcastAddress)
    {
        super( 28, 4 );
        this.broadcastAddress = broadcastAddress;
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        out.put( broadcastAddress );
    }
}
