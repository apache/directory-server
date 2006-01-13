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
 * This option is used by DHCP clients to optionally identify the vendor
 * type and configuration of a DHCP client.  The information is a string
 * of n octets, interpreted by servers.  Vendors may choose to define
 * specific vendor class identifiers to convey particular configuration
 * or other identification information about a client.  For example, the
 * identifier may encode the client's hardware configuration.  Servers
 * not equipped to interpret the class-specific information sent by a
 * client MUST ignore it (although it may be reported). Servers that
 * 
 * respond SHOULD only use option 43 to return the vendor-specific
 * information to the client.
 * 
 * The code for this option is 60, and its minimum length is 1.
 */
public class VendorClassIdentifier extends DhcpOption
{
	private byte[] vendorClassIdentifier;
	
	public VendorClassIdentifier( byte[] vendorClassIdentifier )
	{
		super( 60, 1 );
		this.vendorClassIdentifier = vendorClassIdentifier;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( vendorClassIdentifier );
	}
}

