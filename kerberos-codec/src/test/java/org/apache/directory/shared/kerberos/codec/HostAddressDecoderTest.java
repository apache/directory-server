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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.hostAddress.HostAddressContainer;
import org.apache.directory.shared.kerberos.codec.types.HostAddrType;
import org.apache.directory.shared.kerberos.components.HostAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Test the HostAddress decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class HostAddressDecoderTest
{
    /**
     * Test the decoding of a full HostAddress
     */
    @Test
    public void testHostAddress()
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x16 );

        stream.put( new byte[]
            {
                0x30, 0x14,
                ( byte ) 0xA0, 0x03, // addr-type
                0x02,
                0x01,
                0x02, // IPV4
                ( byte ) 0xA1,
                0x0D, // address : 192.168.0.1
                0x04,
                0x0B,
                '1',
                '9',
                '2',
                '.',
                '1',
                '6',
                '8',
                '.',
                '0',
                '.',
                '1'
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a HostAddress Container
        Asn1Container hostAddressContainer = new HostAddressContainer();

        // Decode the HostAddress PDU
        try
        {
            Asn1Decoder.decode( stream, hostAddressContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        // Check the decoded HostAddress
        HostAddress hostAddress = ( ( HostAddressContainer ) hostAddressContainer ).getHostAddress();

        assertEquals( HostAddrType.ADDRTYPE_INET, hostAddress.getAddrType() );
        assertTrue( Arrays.equals( Strings.getBytesUtf8( "192.168.0.1" ), hostAddress.getAddress() ) );

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( hostAddress.computeLength() );

        try
        {
            bb = hostAddress.encode( bb );

            // Check the length
            assertEquals( 0x16, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a HostAddress with nothing in it
     */
    @Test
    public void testHostAddressEmpty() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            { 0x30, 0x00 } );

        stream.flip();

        // Allocate a HostAddress Container
        Asn1Container hostAddressContainer = new HostAddressContainer();

        // Decode the HostAddress PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, hostAddressContainer);
        } );
    }


    /**
     * Test the decoding of a HostAddress with no addr-type
     */
    @Test
    public void testHostAddressNoAddrType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            {
                0x30, 0x02,
                ( byte ) 0xA0, 0x00 // addr-type
        } );

        stream.flip();

        // Allocate a HostAddress Container
        Asn1Container hostAddressContainer = new HostAddressContainer();

        // Decode the HostAddress PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, hostAddressContainer);
        } );
    }


    /**
     * Test the decoding of a HostAddress with an empty addr-type
     */
    @Test
    public void testHostAddressEmptyAddrType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x0B );

        stream.put( new byte[]
            {
                0x30, 0x04,
                ( byte ) 0xA0, 0x03, // addr-type
                0x02,
                0x00 // 
        } );

        stream.flip();

        // Allocate a HostAddress Container
        Asn1Container hostAddressContainer = new HostAddressContainer();

        // Decode the HostAddress PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, hostAddressContainer);
        } );
    }


    /**
     * Test the decoding of a HostAddress with no address
     */
    @Test
    public void testHostAddressNoAddress() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            { 0x30, 0x07,
                ( byte ) 0xA0, 0x03, // addr-type
                0x02,
                0x01,
                0x02, // 
                ( byte ) 0xA1,
                0x00 // address
        } );

        stream.flip();

        // Allocate a HostAddress Container
        Asn1Container hostAddressContainer = new HostAddressContainer();

        // Decode the HostAddress PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, hostAddressContainer);
        } );
    }


    /**
     * Test the decoding of a HostAddress empty address
     */
    @Test
    public void testHostAddressEmptyAddress() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x0E );

        stream.put( new byte[]
            {
                0x30, 0x14,
                ( byte ) 0xA0, 0x03, // addr-type
                0x02,
                0x01,
                0x02, // IPV4
                ( byte ) 0xA1,
                0x02, // address
                0x04,
                0x00
        } );

        stream.flip();

        // Allocate a HostAddress Container
        Asn1Container hostAddressContainer = new HostAddressContainer();

        // Decode the HostAddress PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, hostAddressContainer);
        } );
    }


    /**
     * Test the decoding of a HostAddress with no add-type
     */
    @Test
    public void testHostAddressMissingAddrType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x11 );

        stream.put( new byte[]
            {
                0x30, 0x0F,
                ( byte ) 0xA1, 0x0D, // address : 192.168.0.1
                0x04,
                0x0B,
                '1',
                '9',
                '2',
                '.',
                '1',
                '6',
                '8',
                '.',
                '0',
                '.',
                '1'
        } );

        stream.flip();

        // Allocate a HostAddress Container
        Asn1Container hostAddressContainer = new HostAddressContainer();

        // Decode the HostAddress PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, hostAddressContainer);
        } );
    }


    /**
     * Test the decoding of a HostAddress with no adddress
     */
    @Test
    public void testHostAddressMissingAddress() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );

        stream.put( new byte[]
            {
                0x30, 0x05,
                ( byte ) 0xA0, 0x03, // addr-type
                0x02,
                0x01,
                0x02, // IPV4
            } );

        stream.flip();

        // Allocate a HostAddress Container
        Asn1Container hostAddressContainer = new HostAddressContainer();

        // Decode the HostAddress PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, hostAddressContainer);
        } );
    }
}
