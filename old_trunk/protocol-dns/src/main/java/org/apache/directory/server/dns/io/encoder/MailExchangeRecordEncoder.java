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

package org.apache.directory.server.dns.io.encoder;


import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.mina.common.ByteBuffer;


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
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MailExchangeRecordEncoder extends ResourceRecordEncoder
{
    protected void putResourceRecordData( ByteBuffer byteBuffer, ResourceRecord record )
    {
        byteBuffer.putShort( Short.parseShort( record.get( DnsAttribute.MX_PREFERENCE ) ) );
        putDomainName( byteBuffer, record.get( DnsAttribute.DOMAIN_NAME ) );
    }
}
