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
 * This option specifies policy filters for non-local source routing.
 * The filters consist of a list of IP addresses and masks which specify
 * destination/mask pairs with which to filter incoming source routes.
 * 
 * Any source routed datagram whose next-hop address does not match one
 * of the filters should be discarded by the client.
 * 
 * The code for this option is 21.  The minimum length of this option is
 * 8, and the length MUST be a multiple of 8.
 */
package org.apache.directory.server.dhcp.options.perhost;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


public class PolicyFilter extends DhcpOption
{
    private byte[] policyFilter;


    public PolicyFilter(byte[] policyFilter)
    {
        super( 21, 8 );
        this.policyFilter = policyFilter;
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        out.put( policyFilter );
    }
}
