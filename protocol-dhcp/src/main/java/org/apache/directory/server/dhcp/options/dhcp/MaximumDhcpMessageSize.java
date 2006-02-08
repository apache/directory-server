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

package org.apache.directory.server.dhcp.options.dhcp;

import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;

/**
 * This option specifies the maximum length DHCP message that it is
 * willing to accept.  The length is specified as an unsigned 16-bit
 * integer.  A client may use the maximum DHCP message size option in
 * DHCPDISCOVER or DHCPREQUEST messages, but should not use the option
 * in DHCPDECLINE messages.
 * 
 * The code for this option is 57, and its length is 2.  The minimum
 * legal value is 576 octets.
 */
public class MaximumDhcpMessageSize extends DhcpOption
{
	private byte[] maximumDhcpMessageSize;
	
	public MaximumDhcpMessageSize( byte[] maximumDhcpMessageSize )
	{
		super( 57, 2 );
		this.maximumDhcpMessageSize = maximumDhcpMessageSize;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( maximumDhcpMessageSize );
	}
}

