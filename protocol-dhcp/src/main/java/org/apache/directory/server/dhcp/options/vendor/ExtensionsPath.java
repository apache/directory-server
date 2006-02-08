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
 * A string to specify a file, retrievable via TFTP, which contains
 * information which can be interpreted in the same way as the 64-octet
 * vendor-extension field within the BOOTP response, with the following
 * exceptions:
 * 
 *        - the length of the file is unconstrained;
 *        - all references to Tag 18 (i.e., instances of the
 *          BOOTP Extensions Path field) within the file are
 *          ignored.
 * 
 * The code for this option is 18.  Its minimum length is 1.
 */
package org.apache.directory.server.dhcp.options.vendor;

import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;

public class ExtensionsPath extends DhcpOption
{
	private byte[] extensionsPath;
	
	public ExtensionsPath( byte[] extensionsPath )
	{
		super( 18, 1 );
		this.extensionsPath = extensionsPath;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( extensionsPath );
	}
}

