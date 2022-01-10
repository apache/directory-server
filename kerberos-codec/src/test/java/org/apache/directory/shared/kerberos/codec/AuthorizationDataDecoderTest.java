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
import org.apache.directory.shared.kerberos.codec.authorizationData.AuthorizationDataContainer;
import org.apache.directory.shared.kerberos.codec.types.AuthorizationType;
import org.apache.directory.shared.kerberos.components.AuthorizationData;
import org.apache.directory.shared.kerberos.components.AuthorizationDataEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Test cases for AuthorizationData decoder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthorizationDataDecoderTest
{

    @Test
    public void testAuthorizationData()
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x21 );

        stream.put( new byte[]
            {
                0x30, 0x1F,
                0x30, 0x0F,
                ( byte ) 0xA0, 0x03, // ad-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x08, // ad-data
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
                0x03, // ad-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x05, // ad-data
                0x04,
                0x03,
                'g',
                'h',
                'i'
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();

        // Decode the AuthorizationData PDU
        try
        {
            Asn1Decoder.decode( stream, authDataContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        AuthorizationData authData = authDataContainer.getAuthorizationData();

        assertNotNull( authData.getAuthorizationData().size() );
        assertEquals( 2, authData.getAuthorizationData().size() );

        String[] expected = new String[]
            { "abcdef", "ghi" };
        int i = 0;

        for ( AuthorizationDataEntry ad : authData.getAuthorizationData() )
        {
            assertEquals( AuthorizationType.AD_INTENDED_FOR_SERVER, ad.getAdType() );
            assertTrue( Arrays.equals( Strings.getBytesUtf8( expected[i++] ), ad.getAdData() ) );

        }

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( authData.computeLength() );

        try
        {
            bb = authData.encode( bb );

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
    public void testAuthorizationDataEmptyPdu() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            {
                0x30, 0x00,
        } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, authDataContainer);
        } );
    }


    @Test
    public void testAuthorizationDataWithNoInnerData() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            {
                0x30, 0x02,
                0x30, 0x00
        } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, authDataContainer);
        } );;
    }


    @Test
    public void testAuthorizationDataEmptyTypeTag() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            {
                0x30, 0x04,
                0x30, 0x02,
                ( byte ) 0xA0, 0x00 // ad-data
        } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, authDataContainer);
        } );
    }


    @Test
    public void testAuthorizationDataEmptyTypeValue() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x08 );

        stream.put( new byte[]
            {
                0x30, 0x06,
                0x30, 0x04,
                ( byte ) 0xA0, 0x02, // ad-data
                0x02,
                0x00
        } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, authDataContainer);
        } );
    }


    @Test
    public void testAuthorizationDataWithoutType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            {
                0x30, 0x07,
                0x30, 0x05,
                ( byte ) 0xA1, 0x03, // ad-data
                0x04,
                0x01,
                'a'
        } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, authDataContainer);
        } );
    }


    @Test
    public void testAuthorizationDataWithoutData() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            {
                0x30, 0x07,
                0x30, 0x05,
                ( byte ) 0xA0, 0x03, // ad-data
                0x02,
                0x01,
                0x02
        } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, authDataContainer);
        } );
    }


    @Test
    public void testAuthorizationDataWithEmptyDataTag() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0xB );

        stream.put( new byte[]
            {
                0x30, 0x09,
                0x30, 0x07,
                ( byte ) 0xA0, 0x03, // ad-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x00 // ad-data
        } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, authDataContainer);
        } );
    }


    @Test
    public void testAuthorizationDataWithEmptyData() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0xD );

        stream.put( new byte[]
            {
                0x30, 0x0B,
                0x30, 0x09,
                ( byte ) 0xA0, 0x03, // ad-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x02, // ad-data
                0x04,
                0x00
        } );

        stream.flip();

        AuthorizationDataContainer authDataContainer = new AuthorizationDataContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, authDataContainer);
        } );
    }
}
