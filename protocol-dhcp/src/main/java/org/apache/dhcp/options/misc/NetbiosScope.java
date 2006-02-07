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

package org.apache.dhcp.options.misc;

import java.nio.ByteBuffer;

import org.apache.dhcp.options.DhcpOption;

/**
 * The NetBIOS scope option specifies the NetBIOS over TCP/IP scope
 * parameter for the client as specified in RFC 1001/1002.
 * 
 * The code for this option is 47.  The minimum length of this option is
 * 1.
 */
public class NetbiosScope extends DhcpOption
{
	private byte[] netbiosScope;
	
	public NetbiosScope( byte[] netbiosScope )
	{
		super( 47, 1 );
		this.netbiosScope = netbiosScope;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( netbiosScope );
	}
}

