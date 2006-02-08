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

package org.apache.directory.server.dhcp.options.misc;

import org.apache.directory.server.dhcp.options.AddressListOption;

/**
 * This option specifies a list of IP addresses indicating mobile IP
 * home agents available to the client.  Agents SHOULD be listed in
 * order of preference.
 * 
 * The code for this option is 68.  Its minimum length is 0 (indicating
 * no home agents are available) and the length MUST be a multiple of 4.
 * It is expected that the usual length will be four octets, containing
 * a single home agent's address.
 */
public class MobileIpHomeAgents extends AddressListOption
{
	public MobileIpHomeAgents( byte[] mobileIpHomeAgent )
	{
		super( 68, mobileIpHomeAgent );
	}
}

