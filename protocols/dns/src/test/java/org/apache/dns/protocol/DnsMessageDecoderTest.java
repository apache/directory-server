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

package org.apache.dns.protocol;

import org.apache.dns.AbstractDnsTestCase;
import org.apache.dns.messages.DnsMessage;
import org.apache.mina.common.ByteBuffer;

public class DnsMessageDecoderTest extends AbstractDnsTestCase
{
    private ByteBuffer requestByteBuffer;

    public void testParseQuery() throws Exception
    {
        requestByteBuffer = getByteBufferFromFile( "DNS-QUERY.pdu" );

        DnsDecoder decoder = new DnsDecoder();
        DnsMessage dnsRequest = decoder.decode( requestByteBuffer );

        print( dnsRequest );
    }

    public void testParseResponse() throws Exception
    {
        requestByteBuffer = getByteBufferFromFile( "DNS-RESPONSE.pdu" );

        DnsDecoder decoder = new DnsDecoder();
        DnsMessage dnsRequest = decoder.decode( requestByteBuffer );

        print( dnsRequest );
    }
}
