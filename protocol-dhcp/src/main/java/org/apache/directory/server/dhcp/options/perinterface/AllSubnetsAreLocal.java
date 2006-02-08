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

package org.apache.directory.server.dhcp.options.perinterface;

import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;

/**
 * This option specifies whether or not the client may assume that all
 * subnets of the IP network to which the client is connected use the
 * same MTU as the subnet of that network to which the client is
 * directly connected.  A value of 1 indicates that all subnets share
 * the same MTU.  A value of 0 means that the client should assume that
 * some subnets of the directly connected network may have smaller MTUs.
 * 
 * The code for this option is 27, and its length is 1.
 */
public class AllSubnetsAreLocal extends DhcpOption
{
	private byte[] allSubnetsAreLocal;
	
	public AllSubnetsAreLocal( byte[] allSubnetsAreLocal )
	{
		super( 27, 1 );
		this.allSubnetsAreLocal = allSubnetsAreLocal;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( allSubnetsAreLocal );
	}
}

