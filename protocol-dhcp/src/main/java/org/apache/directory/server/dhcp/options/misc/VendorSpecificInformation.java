/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */

package org.apache.directory.server.dhcp.options.misc;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


/**
 * This option is used by clients and servers to exchange vendor-
 * specific information.  The information is an opaque object of n
 * octets, presumably interpreted by vendor-specific code on the clients
 * and servers.  The definition of this information is vendor specific.
 * The vendor is indicated in the vendor class identifier option.
 * Servers not equipped to interpret the vendor-specific information
 * sent by a client MUST ignore it (although it may be reported).
 * Clients which do not receive desired vendor-specific information
 * SHOULD make an attempt to operate without it, although they may do so
 * (and announce they are doing so) in a degraded mode.
 * 
 * If a vendor potentially encodes more than one item of information in
 * this option, then the vendor SHOULD encode the option using
 * "Encapsulated vendor-specific options" as described below:
 * The Encapsulated vendor-specific options field SHOULD be encoded as a
 * sequence of code/length/value fields of identical syntax to the DHCP
 * options field with the following exceptions:
 * 
 *    1) There SHOULD NOT be a "magic cookie" field in the encapsulated
 *       vendor-specific extensions field.
 * 
 *    2) Codes other than 0 or 255 MAY be redefined by the vendor within
 *       the encapsulated vendor-specific extensions field, but SHOULD
 *       conform to the tag-length-value syntax defined in section 2.
 * 
 *    3) Code 255 (END), if present, signifies the end of the
 *       encapsulated vendor extensions, not the end of the vendor
 *       extensions field. If no code 255 is present, then the end of
 *       the enclosing vendor-specific information field is taken as the
 *       end of the encapsulated vendor-specific extensions field.
 * 
 * The code for this option is 43 and its minimum length is 1.
 */
public class VendorSpecificInformation extends DhcpOption
{
    private byte[] vendorSpecificInformation;


    public VendorSpecificInformation(byte[] vendorSpecificInformation)
    {
        super( 43, 1 );
        this.vendorSpecificInformation = vendorSpecificInformation;
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        out.put( vendorSpecificInformation );
    }
}
