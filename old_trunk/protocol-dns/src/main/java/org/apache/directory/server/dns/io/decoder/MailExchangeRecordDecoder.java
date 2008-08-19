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

package org.apache.directory.server.dns.io.decoder;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.mina.common.ByteBuffer;


/**
 * A decoder for MX records.  MX records are encoded as per RFC-1035:
 * 
 * <pre>
 *   3.3.9. MX RDATA format
 *
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                  PREFERENCE                   |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     /                   EXCHANGE                    /
 *     /                                               /
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *
 *   where:
 *
 *   PREFERENCE
 *     A 16 bit integer which specifies the preference given to this RR among 
 *     others at the same owner. Lower values are preferred. 
 *     
 *   EXCHANGE
 *     A <domain-name> which specifies a host willing to act as a mail exchange
 *     for the owner name.
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MailExchangeRecordDecoder implements RecordDecoder
{
    public Map<String, Object> decode( ByteBuffer byteBuffer, short length ) throws IOException
    {
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put( DnsAttribute.MX_PREFERENCE, byteBuffer.getShort() );
        attributes.put( DnsAttribute.DOMAIN_NAME, DnsMessageDecoder.getDomainName( byteBuffer ) );
        return attributes;
    }
}
