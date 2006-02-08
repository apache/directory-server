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
 * This option is used by DHCP clients to specify their unique
 * identifier.  DHCP servers use this value to index their database of
 * address bindings.  This value is expected to be unique for all
 * clients in an administrative domain.
 * 
 * Identifiers SHOULD be treated as opaque objects by DHCP servers.
 * 
 * The client identifier MAY consist of type-value pairs similar to the
 * 'htype'/'chaddr' fields. For instance, it MAY consist
 * of a hardware type and hardware address. In this case the type field
 * SHOULD be one of the ARP hardware types defined in STD2.  A
 * hardware type of 0 (zero) should be used when the value field
 * contains an identifier other than a hardware address (e.g. a fully
 * qualified domain name).
 * 
 * For correct identification of clients, each client's client-
 * identifier MUST be unique among the client-identifiers used on the
 * subnet to which the client is attached.  Vendors and system
 * administrators are responsible for choosing client-identifiers that
 * meet this requirement for uniqueness.
 * 
 * The code for this option is 61, and its minimum length is 2.
 */
public class ClientIdentifier extends DhcpOption
{
	private byte[] clientIdentifier;
	
	public ClientIdentifier( byte[] clientIdentifier )
	{
		super( 61, 2 );
		this.clientIdentifier = clientIdentifier;
	}
	
	protected void valueToByteBuffer( ByteBuffer out )
	{
		out.put( clientIdentifier );
	}
}

