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

package org.apache.dhcp.options.dhcp;

import java.nio.ByteBuffer;

import org.apache.dhcp.options.DhcpOption;

/**
 * This option is used to convey the type of the DHCP message.  The code
 * for this option is 53, and its length is 1.  Legal values for this
 * option are:
 * 
 *         Value   Message Type
 *         -----   ------------
 *           1     DHCPDISCOVER
 *           2     DHCPOFFER
 *           3     DHCPREQUEST
 *           4     DHCPDECLINE
 *           5     DHCPACK
 *           6     DHCPNAK
 *           7     DHCPRELEASE
 *           8     DHCPINFORM
 */
public class DhcpMessageType extends DhcpOption
{
	private byte[] messageType;
	
	public DhcpMessageType( byte[] messageType )
	{
		super( 53, 1 );
		this.messageType = messageType;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( messageType );
	}
}

