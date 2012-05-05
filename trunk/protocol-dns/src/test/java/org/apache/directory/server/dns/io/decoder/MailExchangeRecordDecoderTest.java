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


import java.util.Map;

import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


/**
 * Tests for the MX resource record decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MailExchangeRecordDecoderTest
{
    IoBuffer inputBuffer;

    short preference = 10;
    String domainName = "herse.apache.org";
    String[] domainNameParts = new String[]
        { "herse", "apache", "org" };

    MailExchangeRecordDecoder decoder;


    @Before
    public void setUp()
    {
        inputBuffer = IoBuffer.allocate( 128 );
        inputBuffer.putShort( preference );
        inputBuffer.put( ( byte ) domainNameParts[0].length() );
        inputBuffer.put( domainNameParts[0].getBytes() );
        inputBuffer.put( ( byte ) domainNameParts[1].length() );
        inputBuffer.put( domainNameParts[1].getBytes() );
        inputBuffer.put( ( byte ) domainNameParts[2].length() );
        inputBuffer.put( domainNameParts[2].getBytes() );
        inputBuffer.put( ( byte ) 0x00 );
        inputBuffer.flip();

        decoder = new MailExchangeRecordDecoder();
    }


    @Test
    public void testDecode() throws Exception
    {
        Map<String, Object> attributes = decoder.decode( inputBuffer, ( short ) inputBuffer.remaining() );
        assertEquals( preference, attributes.get( DnsAttribute.MX_PREFERENCE ) );
        assertEquals( domainName, attributes.get( DnsAttribute.DOMAIN_NAME ) );
    }
}
