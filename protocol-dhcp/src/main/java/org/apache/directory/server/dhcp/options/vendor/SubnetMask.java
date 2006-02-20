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
 * The subnet mask option specifies the client's subnet mask as per RFC
 * 950.
 * 
 * If both the subnet mask and the router option are specified in a DHCP
 * reply, the subnet mask option MUST be first.
 * 
 * The code for the subnet mask option is 1, and its length is 4 octets.
 */
package org.apache.directory.server.dhcp.options.vendor;


import org.apache.directory.server.dhcp.options.AddressOption;


public class SubnetMask extends AddressOption
{
    public SubnetMask(byte[] subnetMask)
    {
        super( 1, subnetMask );
    }
}
