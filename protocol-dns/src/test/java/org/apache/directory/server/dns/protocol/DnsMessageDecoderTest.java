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

package org.apache.directory.server.dns.protocol;


import java.io.IOException;

import org.apache.directory.server.dns.AbstractDnsTestCase;
import org.apache.directory.server.dns.io.decoder.DnsMessageDecoder;


public class DnsMessageDecoderTest extends AbstractDnsTestCase
{
    private DnsMessageDecoder decoder = new DnsMessageDecoder();


    public void testParseQuery() throws Exception
    {
        assertEquals( getTestQuery(), decoder.decode( getTestQueryByteBuffer() ) );
    }
    

    public void testParseResponse() throws Exception
    {
        //assertEquals( getTestResponse(), decoder.decode( getTestResponseByteBuffer() ) );
    }
    
    
    public void testParseMxQuery() throws Exception
    {
        //assertEquals( getTestMxQuery(), decoder.decode( getTestMxQueryByteBuffer() ) );
    }
    
    public void testParseMxResponse() throws IOException
    {
        //assertEquals( getTestMxResponse(), decoder.decode( getTestMxResponseByteBuffer() ) );
    }
}
