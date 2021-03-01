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


import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;

import org.apache.directory.api.util.Strings;
import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResourceRecordImpl;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * A base class for testing different types of ResourceRecordEncoders.  It 
 * handles setting up the expected output buffer not having to do specifically
 * with the resource data, and handles creating the ResourceRecord to be tested.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractResourceRecordEncoderTest
{
    IoBuffer expectedData;
    String domainName = "herse.apache.org";
    String[] domainNameParts = domainName.split( "\\." );
    int timeToLive = 3400;
    ResourceRecord record;


    @BeforeEach
    public void setUp() throws UnknownHostException
    {
        setUpResourceData();
        record = new ResourceRecordImpl( domainName, RecordType.A, RecordClass.IN, timeToLive, getAttributes() );

        expectedData = IoBuffer.allocate( 128 );
        expectedData.put( ( byte ) 18 );
        expectedData.put( ( byte ) domainNameParts[0].length() ); // 1
        expectedData.put( Strings.getBytesUtf8( domainNameParts[0] ) ); // + 5
        expectedData.put( ( byte ) domainNameParts[1].length() ); // + 1
        expectedData.put( Strings.getBytesUtf8( domainNameParts[1] ) ); // + 6
        expectedData.put( ( byte ) domainNameParts[2].length() ); // + 1
        expectedData.put( Strings.getBytesUtf8( domainNameParts[2] ) ); // + 3esourceRecordEncoder
        expectedData.put( ( byte ) 0x00 ); // + 1 = 18
        expectedData.putShort( RecordType.A.convert() );
        expectedData.putShort( RecordClass.IN.convert() );
        expectedData.putInt( timeToLive );
        putExpectedResourceData( expectedData );
    }


    @Test
    public void testEncode() throws IOException
    {
        IoBuffer outBuffer = IoBuffer.allocate( 128 );
        getEncoder().put(outBuffer, record);
        assertEquals(expectedData, outBuffer);
    }


    /**
     * A method that implementers can override if they need to do some setup 
     * before the resource record and expected data buffer are created, such
     * as initialize an ip address.
     */
    protected void setUpResourceData()
    {
    }


    /**
     * @return the encoder to be tested
     */
    protected abstract ResourceRecordEncoder getEncoder();


    /**
     * @return the attributes to be used as the resource data for the resource
     *         record
     */
    protected abstract Map<String, Object> getAttributes();


    /**
     * Put the encoded resource data into a buffer that will compared to the
     * result of using the encoder under test.
     * 
     * @param expectedData buffer where the expected resource data should be put
     */
    protected abstract void putExpectedResourceData( IoBuffer expectedData );
}
