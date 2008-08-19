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


import java.net.InetAddress;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.mina.common.ByteBuffer;


/**
 * Tests for the A resource record decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 501160 $, $Date: 2007-01-29 12:41:33 -0700 (Mon, 29 Jan 2007) $
 */
public class AddressRecordDecoderTest extends TestCase
{
    InetAddress address;
    ByteBuffer inputBuffer;

    AddressRecordDecoder decoder;


    public void setUp() throws Exception
    {
        address = InetAddress.getByName( "127.0.0.1" );
        inputBuffer = ByteBuffer.allocate( address.getAddress().length );
        inputBuffer.put( address.getAddress() );
        inputBuffer.flip();

        decoder = new AddressRecordDecoder();
    }


    public void testDecode() throws Exception
    {
        Map attributes = decoder.decode( inputBuffer, ( short ) address.getAddress().length );
        assertEquals( address, attributes.get( DnsAttribute.IP_ADDRESS ) );
    }
}
