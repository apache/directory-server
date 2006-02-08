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

import org.apache.directory.server.dhcp.options.AddressOption;

/**
 * This option is used in a client request (DHCPDISCOVER) to allow the
 * client to request that a particular IP address be assigned.
 * 
 * The code for this option is 50, and its length is 4.
 */
public class RequestedIpAddress extends AddressOption
{
	public RequestedIpAddress( byte[] requestedIpAddress )
	{
		super( 50, requestedIpAddress );
	}
}

