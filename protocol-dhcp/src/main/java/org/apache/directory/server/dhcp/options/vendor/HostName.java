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
 * This option specifies the name of the client.  The name may or may
 * not be qualified with the local domain name (see section 3.17 for the
 * preferred way to retrieve the domain name).  See RFC 1035 for
 * character set restrictions.
 * 
 * The code for this option is 12, and its minimum length is 1.
 */
package org.apache.directory.server.dhcp.options.vendor;

import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;

public class HostName extends DhcpOption
{
	private byte[] hostName;
	
	public HostName( byte[] hostName )
	{
		super( 12, 1 );
		this.hostName = hostName;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( hostName );
	}
}

