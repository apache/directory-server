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
import org.apache.mina.common.ByteBuffer;


/**
 * 3.3.13. SOA RDATA format
 * 
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     /                     MNAME                     /
 *     /                                               /
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     /                     RNAME                     /
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                    SERIAL                     |
 *     |                                               |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                    REFRESH                    |
 *     |                                               |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                     RETRY                     |
 *     |                                               |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                    EXPIRE                     |
 *     |                                               |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                    MINIMUM                    |
 *     |                                               |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * 
 * where:
 * 
 * MNAME           The <domain-name> of the name server that was the
 *                 original or primary source of data for this zone.
 * 
 * RNAME           A <domain-name> which specifies the mailbox of the
 *                 person responsible for this zone.
 * 
 * SERIAL          The unsigned 32 bit version number of the original copy
 *                 of the zone.  Zone transfers preserve this value.  This
 *                 value wraps and should be compared using sequence space
 *                 arithmetic.
 * 
 * REFRESH         A 32 bit time interval before the zone should be
 *                 refreshed.
 * 
 * RETRY           A 32 bit time interval that should elapse before a
 *                 failed refresh should be retried.
 * 
 * EXPIRE          A 32 bit time value that specifies the upper limit on
 *                 the time interval that can elapse before the zone is no
 *                 longer authoritative.
 * 
 * MINIMUM         The unsigned 32 bit minimum TTL field that should be
 *                 exported with any RR from this zone.
 * 
 * SOA records cause no additional section processing.
 * 
 * All times are in units of seconds.
 * 
 * Most of these fields are pertinent only for name server maintenance
 * operations.  However, MINIMUM is used in all query operations that
 * retrieve RRs from a zone.  Whenever a RR is sent in a response to a
 * query, the TTL field is set to the maximum of the TTL field from the RR
 * and the MINIMUM field in the appropriate SOA.  Thus MINIMUM is a lower
 * bound on the TTL field for all RRs in a zone.  Note that this use of
 * MINIMUM should occur when the RRs are copied into the response and not
 * when the zone is loaded from a master file or via a zone transfer.  The
 * reason for this provison is to allow future dynamic update facilities to
 * change the SOA RR with known semantics.
 */
public class StartOfAuthorityRecordEncoder extends ResourceRecordEncoder
{
    protected byte[] encodeResourceData( ResourceRecord record )
    {
        String mName = record.get( DnsAttribute.SOA_M_NAME );
        String rName = record.get( DnsAttribute.SOA_R_NAME );
        long serial = Long.parseLong( record.get( DnsAttribute.SOA_SERIAL ) );
        int refresh = Integer.parseInt( record.get( DnsAttribute.SOA_REFRESH ) );
        int retry = Integer.parseInt( record.get( DnsAttribute.SOA_RETRY ) );
        int expire = Integer.parseInt( record.get( DnsAttribute.SOA_EXPIRE ) );
        long minimum = Long.parseLong( record.get( DnsAttribute.SOA_MINIMUM ) );

        ByteBuffer byteBuffer = ByteBuffer.allocate( 256 );

        byteBuffer.put( encodeDomainName( mName ) );
        byteBuffer.put( encodeDomainName( rName ) );

        putUnsignedInt( byteBuffer, serial );

        byteBuffer.putInt( refresh );
        byteBuffer.putInt( retry );
        byteBuffer.putInt( expire );

        putUnsignedInt( byteBuffer, minimum );

        byteBuffer.flip();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get( bytes, 0, bytes.length );

        return bytes;
    }
}
