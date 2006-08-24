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
 * This option specifies whether the client should configure its IP
 * layer for packet forwarding.  A value of 0 means disable IP
 * forwarding, and a value of 1 means enable IP forwarding.
 * 
 * The code for this option is 19, and its length is 1.
 */
package org.apache.directory.server.dhcp.options.perhost;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


public class IpForwarding extends DhcpOption
{
    private byte[] ipForwarding;


    public IpForwarding(byte[] ipForwarding)
    {
        super( 19, 1 );
        this.ipForwarding = ipForwarding;
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        out.put( ipForwarding );
    }
}
