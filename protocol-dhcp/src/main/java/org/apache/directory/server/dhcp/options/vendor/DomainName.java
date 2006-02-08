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

/**
 * This option specifies the domain name that client should use when
 * resolving hostnames via the Domain Name System.
 * 
 * The code for this option is 15.  Its minimum length is 1.
 */
package org.apache.directory.server.dhcp.options.vendor;

import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;

public class DomainName extends DhcpOption
{
	private byte[] domainName;
	
	public DomainName( byte[] domainName )
	{
		super( 15, 1 );
		this.domainName = domainName;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( domainName );
	}
}

