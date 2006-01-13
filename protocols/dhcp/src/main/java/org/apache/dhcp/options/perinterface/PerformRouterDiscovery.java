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
 * This option specifies whether or not the client should solicit
 * routers using the Router Discovery mechanism defined in RFC 1256.
 * A value of 0 indicates that the client should not perform router
 * discovery.  A value of 1 means that the client should perform
 * router discovery.
 * 
 * The code for this option is 31, and its length is 1.
 */
public class PerformRouterDiscovery extends DhcpOption
{
	private byte[] performRouterDiscovery;
	
	public PerformRouterDiscovery( byte[] performRouterDiscovery )
	{
		super( 31, 1 );
		this.performRouterDiscovery = performRouterDiscovery;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( performRouterDiscovery );
	}
}

