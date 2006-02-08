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
 * This option specifies the MTU to use on this interface.  The MTU is
 * specified as a 16-bit unsigned integer.  The minimum legal value for
 * the MTU is 68.
 * 
 * The code for this option is 26, and its length is 2.
 */
public class InterfaceMtu extends DhcpOption
{
	private byte[] interfaceMtu;
	
	public InterfaceMtu( byte[] interfaceMtu )
	{
		super( 26, 2 );
		this.interfaceMtu = interfaceMtu;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( interfaceMtu );
	}
}

