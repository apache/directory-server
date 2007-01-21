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

package org.apache.directory.server.dhcp.options.tcp;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


/**
 * This option specifies the default TTL that the client should use when
 * sending TCP segments.  The value is represented as an 8-bit unsigned
 * integer.  The minimum value is 1.
 * 
 * The code for this option is 37, and its length is 1.
 */
public class TcpDefaultTimeToLive extends DhcpOption
{
    private byte[] tcpDefaultTimeToLive;


    public TcpDefaultTimeToLive(byte[] tcpDefaultTimeToLive)
    {
        super( 37, 1 );
        this.tcpDefaultTimeToLive = tcpDefaultTimeToLive;
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        out.put( tcpDefaultTimeToLive );
    }
}
