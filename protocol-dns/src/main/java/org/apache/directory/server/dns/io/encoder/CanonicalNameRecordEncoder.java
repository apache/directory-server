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

package org.apache.directory.server.dns.io.encoder;

import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.store.DnsAttribute;

/**
 * 3.3.1. CNAME RDATA format
 * 
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     /                     CNAME                     /
 *     /                                               /
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * 
 * where:
 * 
 * CNAME           A <domain-name> which specifies the canonical or primary
 *                 name for the owner.  The owner name is an alias.
 * 
 * CNAME RRs cause no additional section processing, but name servers may
 * choose to restart the query at the canonical name in certain cases.  See
 * the description of name server logic in [RFC-1034] for details.
 */
public class CanonicalNameRecordEncoder extends ResourceRecordEncoder
{
    protected byte[] encodeResourceData( ResourceRecord record )
    {
        String domainName = record.get( DnsAttribute.DOMAIN_NAME );

        return encodeDomainName( domainName );
    }
}
