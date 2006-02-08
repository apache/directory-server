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

package org.apache.directory.server.dhcp.options.dhcp;

import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;

/**
 * This option specifies the time interval from address assignment until
 * the client transitions to the RENEWING state.
 * 
 * The value is in units of seconds, and is specified as a 32-bit
 * unsigned integer.
 * 
 * The code for this option is 58, and its length is 4.
 */
public class RenewalTimeValue extends DhcpOption
{
	private int renewalTimeValue;
	
	public RenewalTimeValue( int renewalTimeValue )
	{
		super( 58, 4 );
		this.renewalTimeValue = renewalTimeValue;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.putInt( renewalTimeValue );
	}
}

