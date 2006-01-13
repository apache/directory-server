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
 * This option specifies the path-name of a file to which the client's
 * core image should be dumped in the event the client crashes.  The
 * path is formatted as a character string consisting of characters from
 * the NVT ASCII character set.
 * 
 * The code for this option is 14.  Its minimum length is 1.
 */
package org.apache.dhcp.options.vendor;

import java.nio.ByteBuffer;

import org.apache.dhcp.options.DhcpOption;

public class MeritDumpFile extends DhcpOption
{
	private byte[] meritDumpFile;
	
	public MeritDumpFile( byte[] meritDumpFile )
	{
		super( 14, 1 );
		this.meritDumpFile = meritDumpFile;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( meritDumpFile );
	}
}

