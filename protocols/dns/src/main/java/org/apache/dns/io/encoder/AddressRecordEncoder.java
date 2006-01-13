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

package org.apache.dns.io.encoder;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.dns.messages.ResourceRecord;
import org.apache.dns.store.DnsAttribute;

/**
 * 3.4.1. A RDATA format
 * 
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *  |                    ADDRESS                    |
 *  +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * 
 * where:
 * 
 * ADDRESS         A 32 bit Internet address.
 * 
 * Hosts that have multiple Internet addresses will have multiple A
 * records.
 * 
 * A records cause no additional section processing.  The RDATA section of
 * an A line in a master file is an Internet address expressed as four
 * decimal numbers separated by dots without any imbedded spaces (e.g.,
 * "10.2.0.52" or "192.0.5.6").
 */
public class AddressRecordEncoder extends ResourceRecordEncoder
{
    protected byte[] encodeResourceData( ResourceRecord record )
    {
        String ipAddress = record.get( DnsAttribute.IP_ADDRESS );

        try
        {
            return InetAddress.getByName( ipAddress ).getAddress();
        }
        catch ( UnknownHostException uhe )
        {
            return null;
        }
    }
}
