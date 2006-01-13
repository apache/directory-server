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
 * This option specifies whether or not the client should perform subnet
 * mask discovery using ICMP.  A value of 0 indicates that the client
 * should not perform mask discovery.  A value of 1 means that the
 * client should perform mask discovery.
 * 
 * The code for this option is 29, and its length is 1.
 */
public class PerformMaskDiscovery extends DhcpOption
{
	private byte[] performMaskDiscovery;
	
	public PerformMaskDiscovery( byte[] performMaskDiscovery )
	{
		super( 29, 1 );
		this.performMaskDiscovery = performMaskDiscovery;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( performMaskDiscovery );
	}
}

