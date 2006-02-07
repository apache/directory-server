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

import org.apache.dns.messages.ResourceRecord;
import org.apache.dns.store.DnsAttribute;

/**
 * 3.3.12. PTR RDATA format
 * 
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     /                   PTRDNAME                    /
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * 
 * where:
 * 
 * PTRDNAME        A <domain-name> which points to some location in the
 *                 domain name space.
 * 
 * PTR records cause no additional section processing.  These RRs are used
 * in special domains to point to some other location in the domain space.
 * These records are simple data, and don't imply any special processing
 * similar to that performed by CNAME, which identifies aliases.  See the
 * description of the IN-ADDR.ARPA domain for an example.
 */
public class PointerRecordEncoder extends ResourceRecordEncoder
{
    protected byte[] encodeResourceData( ResourceRecord record )
    {
        String domainName = record.get( DnsAttribute.DOMAIN_NAME );

        return encodeDomainName( domainName );
    }
}
