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

package org.apache.directory.server.dhcp.options.tcp;

import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;

/**
 * This option specifies the whether or not the client should send TCP
 * keepalive messages with a octet of garbage for compatibility with
 * older implementations.  A value of 0 indicates that a garbage octet
 * should not be sent. A value of 1 indicates that a garbage octet
 * should be sent.
 * 
 * The code for this option is 39, and its length is 1.
 */
public class TcpKeepaliveInterval extends DhcpOption
{
	private byte[] tcpKeepaliveInterval;
	
	public TcpKeepaliveInterval( byte[] tcpKeepaliveInterval )
	{
		super( 39, 1 );
		this.tcpKeepaliveInterval = tcpKeepaliveInterval;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( tcpKeepaliveInterval );
	}
}

