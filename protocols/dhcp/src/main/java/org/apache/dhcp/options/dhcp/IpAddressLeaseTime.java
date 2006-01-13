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
 * This option is used in a client request (DHCPDISCOVER or DHCPREQUEST)
 * to allow the client to request a lease time for the IP address.  In a
 * server reply (DHCPOFFER), a DHCP server uses this option to specify
 * the lease time it is willing to offer.
 * 
 * The time is in units of seconds, and is specified as a 32-bit
 * unsigned integer.
 * 
 * The code for this option is 51, and its length is 4.
 */
public class IpAddressLeaseTime extends DhcpOption
{
	private byte[] ipAddressLeaseTime;
	
	public IpAddressLeaseTime( byte[] ipAddressLeaseTime )
	{
		super( 51, 4 );
		this.ipAddressLeaseTime = ipAddressLeaseTime;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( ipAddressLeaseTime );
	}
}

