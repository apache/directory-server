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


import java.nio.ByteBuffer;

import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.store.DnsAttribute;


/**
 * 3.3.9. MX RDATA format
 * 
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                  PREFERENCE                   |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     /                   EXCHANGE                    /
 *     /                                               /
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * 
 * where:
 * 
 * PREFERENCE      A 16 bit integer which specifies the preference given to
 *                 this RR among others at the same owner.  Lower values
 *                 are preferred.
 * 
 * EXCHANGE        A <domain-name> which specifies a host willing to act as
 *                 a mail exchange for the owner name.
 * 
 * MX records cause type A additional section processing for the host
 * specified by EXCHANGE.  The use of MX RRs is explained in detail in
 * [RFC-974].
 */
public class MailExchangeRecordEncoder extends ResourceRecordEncoder
{
    protected byte[] encodeResourceData( ResourceRecord record )
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate( 256 );
        byteBuffer.putShort( Short.parseShort( record.get( DnsAttribute.MX_PREFERENCE ) ) );
        byteBuffer.put( encodeDomainName( record.get( DnsAttribute.DOMAIN_NAME ) ) );

        byteBuffer.flip();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get( bytes, 0, bytes.length );

        return bytes;
    }
}
