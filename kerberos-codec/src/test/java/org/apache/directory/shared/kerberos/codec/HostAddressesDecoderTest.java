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
package org.apache.directory.shared.kerberos.codec;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.hostAddress.HostAddressContainer;
import org.apache.directory.shared.kerberos.codec.hostAddresses.HostAddressesContainer;
import org.apache.directory.shared.kerberos.codec.types.HostAddrType;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.apache.directory.shared.kerberos.components.HostAddresses;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.shared.util.Strings;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the HostAddresses decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class HostAddressesDecoderTest
{
    /**
     * Test the decoding of a HostAddresses
     */
    @Test
    public void testHostAddresses()
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x44 );
        
        stream.put( new byte[]
            { 
              0x30, 0x42,
                0x30, 0x14,
                  (byte)0xA0, 0x03,                 // addr-type
                    0x02, 0x01, 0x02,               // IPV4
                  (byte)0xA1, 0x0D,                 // address : 192.168.0.1
                    0x04, 0x0B, '1', '9', '2', '.', '1', '6', '8', '.', '0', '.', '1',
                0x30, 0x14,
                  (byte)0xA0, 0x03,                 // addr-type
                    0x02, 0x01, 0x02,               // IPV4
                  (byte)0xA1, 0x0D,                 // address : 192.168.0.2
                    0x04, 0x0B, '1', '9', '2', '.', '1', '6', '8', '.', '0', '.', '2',
                0x30, 0x14,
                  (byte)0xA0, 0x03,                 // addr-type
                    0x02, 0x01, 0x02,               // IPV4
                  (byte)0xA1, 0x0D,                 // address : 192.168.0.3
                    0x04, 0x0B, '1', '9', '2', '.', '1', '6', '8', '.', '0', '.', '3'
            } );

        String decodedPdu = Strings.dumpBytes(stream.array());
        stream.flip();

        // Allocate a HostAddresses Container
        Asn1Container hostAddressesContainer = new HostAddressesContainer();
        hostAddressesContainer.setStream( stream );

        // Decode the HostAddresses PDU
        try
        {
            kerberosDecoder.decode( stream, hostAddressesContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        // Check the decoded HostAddress
        HostAddresses hostAddresses = ( ( HostAddressesContainer ) hostAddressesContainer ).getHostAddresses();

        assertEquals( 3, hostAddresses.getAddresses().length );
        
        String[] expected = new String[]{ "192.168.0.1", "192.168.0.2", "192.168.0.3" };
        int i = 0;
        
        for ( HostAddress hostAddress : hostAddresses.getAddresses() )
        {
            assertEquals( HostAddrType.ADDRTYPE_INET, hostAddress.getAddrType() );
            assertTrue( Arrays.equals( StringTools.getBytesUtf8( expected[i] ), hostAddress.getAddress() ) );
            i++;
        }

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( hostAddresses.computeLength() );
        
        try
        {
            bb = hostAddresses.encode( bb );
    
            // Check the length
            assertEquals( 0x44, bb.limit() );
    
            String encodedPdu = Strings.dumpBytes(bb.array());
    
            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    /**
     * Test the decoding of a HostAddresses with nothing in it
     */
    @Test( expected = DecoderException.class)
    public void testHostAddressEmpty() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );
        
        stream.put( new byte[]
            { 0x30, 0x00 } );

        stream.flip();

        // Allocate a HostAddress Container
        Asn1Container hostAddressesContainer = new HostAddressesContainer();

        // Decode the HostAddress PDU
        kerberosDecoder.decode( stream, hostAddressesContainer );
        fail();
    }
    
    
    /**
     * Test the decoding of a HostAddresses with empty hostAddress in it
     */
    @Test( expected = DecoderException.class)
    public void testHostAddressesNoHostAddress() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );
        
        stream.put( new byte[]
            { 0x30, 0x02,
                (byte)0x30, 0x00                  // empty HostAddress
            } );

        stream.flip();

        // Allocate a HostAddress Container
        Asn1Container hostAddressesContainer = new HostAddressContainer();

        // Decode the HostAddresses PDU
        kerberosDecoder.decode( stream, hostAddressesContainer );
        fail();
    }
}
