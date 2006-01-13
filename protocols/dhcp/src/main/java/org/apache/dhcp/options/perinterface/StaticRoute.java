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

package org.apache.dhcp.options.perinterface;

import java.nio.ByteBuffer;

import org.apache.dhcp.options.DhcpOption;

/**
 * This option specifies a list of static routes that the client should
 * install in its routing cache.  If multiple routes to the same
 * destination are specified, they are listed in descending order of
 * priority.
 * 
 * The routes consist of a list of IP address pairs.  The first address
 * is the destination address, and the second address is the router for
 * the destination.
 * 
 * The default route (0.0.0.0) is an illegal destination for a static
 * route.  See section 3.5 for information about the router option.
 * 
 * The code for this option is 33.  The minimum length of this option is
 * 8, and the length MUST be a multiple of 8.
 */
public class StaticRoute extends DhcpOption
{
	private byte[] staticRoute;
	
	public StaticRoute( byte[] staticRoute )
	{
		super( 33, 8 );
		this.staticRoute = staticRoute;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( staticRoute );
	}
}

