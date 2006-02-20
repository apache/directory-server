/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.directory.server.dhcp.options.misc;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


/**
 * The NetBIOS node type option allows NetBIOS over TCP/IP clients which
 * are configurable to be configured as described in RFC 1001/1002.  The
 * value is specified as a single octet which identifies the client type
 * as follows:
 * 
 *    Value         Node Type
 *    -----         ---------
 *    0x1           B-node
 *    0x2           P-node
 *    0x4           M-node
 *    0x8           H-node
 * 
 * In the above chart, the notation '0x' indicates a number in base-16
 * (hexadecimal).
 * 
 * The code for this option is 46.  The length of this option is always
 * 1.
 */
public class NetbiosNodeType extends DhcpOption
{
    private byte[] netbiosNodeType;


    public NetbiosNodeType(byte[] netbiosNodeType)
    {
        super( 46, 1 );
        this.netbiosNodeType = netbiosNodeType;
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        out.put( netbiosNodeType );
    }
}
