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
 * The router option specifies a list of IP addresses for routers on the
 * client's subnet.  Routers SHOULD be listed in order of preference.
 * 
 * The code for the router option is 3.  The minimum length for the
 * router option is 4 octets, and the length MUST always be a multiple
 * of 4.
 */
package org.apache.directory.server.dhcp.options.vendor;

import org.apache.directory.server.dhcp.options.AddressListOption;

public class Routers extends AddressListOption
{
	public Routers( byte[] routers )
	{
		super( 3, routers );
	}
}

