/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.kerberos.codec;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.typedData.TypedDataContainer;
import org.apache.directory.shared.kerberos.components.TypedData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Test cases for TypedData decoder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TypedDataDecoderTest
{

    @Test
    public void testTypedData()
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x21 );

        stream.put( new byte[]
            {
                0x30, 0x1F,
                0x30, 0x0F,
                ( byte ) 0xA0, 0x03, // data-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x08, // data-value
                0x04,
                0x06,
                'a',
                'b',
                'c',
                'd',
                'e',
                'f',
                0x30,
                0x0C,
                ( byte ) 0xA0,
                0x03, // data-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x05, // data-value
                0x04,
                0x03,
                'g',
                'h',
                'i'
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        TypedDataContainer typedDataContainer = new TypedDataContainer();

        // Decode the TypedData PDU
        try
        {
            Asn1Decoder.decode( stream, typedDataContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        TypedData typedData = typedDataContainer.getTypedData();

        assertNotNull( typedData.getTypedData().size() );
        assertEquals( 2, typedData.getTypedData().size() );

        String[] expected = new String[]
            { "abcdef", "ghi" };
        int i = 0;

        for ( TypedData.TD td : typedData.getTypedData() )
        {
            assertEquals( 2, td.getDataType() );
            assertTrue( Arrays.equals( Strings.getBytesUtf8( expected[i++] ), td.getDataValue() ) );
        }

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( typedData.computeLength() );

        try
        {
            bb = typedData.encode( bb );

            // Check the length
            assertEquals( 0x21, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    @Test
    public void testTypedDataWithoutType() throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            { 0x30, 0x7,
                0x30, 0x5,
                ( byte ) 0xA1, 0x03, // data-value
                0x04,
                0x01,
                'a'
        } );

        stream.flip();

        TypedDataContainer container = new TypedDataContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    @Test
    public void testTypedDataWithoutData() throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            { 0x30, 0x7,
                0x30, 0x05,
                ( byte ) 0xA0, 0x03, // data-value
                0x02,
                0x01,
                0x02
        } );

        stream.flip();

        TypedDataContainer typedDataContainer = new TypedDataContainer();

        Asn1Decoder.decode( stream, typedDataContainer );

        TypedData typedData = typedDataContainer.getTypedData();

        assertNotNull( typedData.getTypedData() );
        assertEquals( 1, typedData.getTypedData().size() );
        assertEquals( 2, typedData.getCurrentTD().getDataType() );
    }


    @Test
    public void testTypedDataWithIncorrectPdu() throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            {
                0x30, 0x2,
                0x30, 0x0
        } );

        stream.flip();

        TypedDataContainer container = new TypedDataContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    @Test
    public void testTypedDataWithEmptyData() throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.allocate( 0xD );

        stream.put( new byte[]
            {
                0x30, 0x0B,
                0x30, 0x09,
                ( byte ) 0xA0, 0x03, // data-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x02, // data-value
                0x04,
                0x00
        } );

        stream.flip();

        TypedDataContainer container = new TypedDataContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    @Test
    public void testTypedDataWithEmptyType() throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.allocate( 0xD );

        stream.put( new byte[]
            {
                0x30, 0x0B,
                0x30, 0x09,
                ( byte ) 0xA0, 0x02, // data-type
                0x02,
                0x00,
                ( byte ) 0xA1,
                0x03, // data-value
                0x04,
                0x01,
                0x02
        } );

        stream.flip();

        TypedDataContainer container = new TypedDataContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }
}
