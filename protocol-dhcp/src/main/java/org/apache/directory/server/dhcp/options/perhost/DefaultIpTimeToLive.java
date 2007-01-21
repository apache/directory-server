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

/**
 * This option specifies the default time-to-live that the client should
 * use on outgoing datagrams.  The TTL is specified as an octet with a
 * value between 1 and 255.
 * 
 * The code for this option is 23, and its length is 1.
 */
package org.apache.directory.server.dhcp.options.perhost;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


public class DefaultIpTimeToLive extends DhcpOption
{
    private byte[] defaultIpTimeToLive;


    public DefaultIpTimeToLive(byte[] defaultIpTimeToLive)
    {
        super( 23, 1 );
        this.defaultIpTimeToLive = defaultIpTimeToLive;
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        out.put( defaultIpTimeToLive );
    }
}
