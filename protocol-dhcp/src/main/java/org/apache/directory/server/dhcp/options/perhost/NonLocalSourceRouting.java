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
 * This option specifies whether the client should configure its IP
 * layer to allow forwarding of datagrams with non-local source routes.
 * A value of 0 means disallow forwarding of such datagrams, and a value
 * of 1 means allow forwarding.
 * 
 * The code for this option is 20, and its length is 1.
 */
package org.apache.directory.server.dhcp.options.perhost;

import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;

public class NonLocalSourceRouting extends DhcpOption
{
	private byte[] nonLocalSourceRouting;
	
	public NonLocalSourceRouting( byte[] nonLocalSourceRouting )
	{
		super( 20, 1 );
		this.nonLocalSourceRouting = nonLocalSourceRouting;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( nonLocalSourceRouting );
	}
}

