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


import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResourceRecordImpl;
import org.apache.mina.common.ByteBuffer;


/**
 * A base class for testing different types of ResourceRecordEncoders.  It 
 * handles setting up the expected output buffer not having to do specifically
 * with the resource data, and handles creating the ResourceRecord to be tested.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 501160 $, $Date: 2007-01-29 12:41:33 -0700 (Mon, 29 Jan 2007) $
 */
public abstract class AbstractResourceRecordEncoderTest extends TestCase
{

    ByteBuffer expectedData;
    String domainName = "herse.apache.org";
    String[] domainNameParts = domainName.split( "\\." );
    int timeToLive = 3400;
    ResourceRecord record;


    public void setUp() throws UnknownHostException
    {
        setUpResourceData();
        record = new ResourceRecordImpl( domainName, RecordType.A, RecordClass.IN, timeToLive, getAttributes() );

        expectedData = ByteBuffer.allocate( 128 );
        expectedData.put( ( byte ) 18 );
        expectedData.put( ( byte ) domainNameParts[0].length() ); // 1
        expectedData.put( domainNameParts[0].getBytes() ); // + 5
        expectedData.put( ( byte ) domainNameParts[1].length() ); // + 1
        expectedData.put( domainNameParts[1].getBytes() ); // + 6
        expectedData.put( ( byte ) domainNameParts[2].length() ); // + 1
        expectedData.put( domainNameParts[2].getBytes() ); // + 3
        expectedData.put( ( byte ) 0x00 ); // + 1 = 18
        expectedData.putShort( RecordType.A.convert() );
        expectedData.putShort( RecordClass.IN.convert() );
        expectedData.putInt( timeToLive );
        putExpectedResourceData( expectedData );
    }


    public void testEncode() throws IOException
    {
        ByteBuffer outBuffer = ByteBuffer.allocate( 128 );
        getEncoder().put( outBuffer, record );
        assertEquals( expectedData, outBuffer );
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
    protected abstract Map getAttributes();


    /**
     * Put the encoded resource data into a buffer that will compared to the
     * result of using the encoder under test.
     * 
     * @param expectedData buffer where the expected resource data should be put
     */
    protected abstract void putExpectedResourceData( ByteBuffer expectedData );
}
