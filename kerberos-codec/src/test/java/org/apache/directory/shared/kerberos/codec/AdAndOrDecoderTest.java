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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.adAndOr.AdAndOrContainer;
import org.apache.directory.shared.kerberos.components.AdAndOr;
import org.apache.directory.shared.util.Strings;
import org.junit.Test;


/**
 * Test cases for AD-AND-OR codec.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AdAndOrDecoderTest
{
    @Test
    public void testDecodeFullAdAndOr()
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x2D );

        stream.put( new byte[]
            {
                0x30, 0x2B,
                ( byte ) 0xA0, 0x03, // condition count
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x24, // elements
                0x30,
                0x22,
                0x30,
                0x0F,
                ( byte ) 0xA0,
                0x03, // ad-type
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
                0x0F,
                ( byte ) 0xA0,
                0x03, // ad-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x08, // ad-data
                0x04,
                0x06,
                'g',
                'h',
                'i',
                'j',
                'k',
                'l'
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        AdAndOrContainer adAndOrContainer = new AdAndOrContainer();
        adAndOrContainer.setStream( stream );

        try
        {
            krbDecoder.decode( stream, adAndOrContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        AdAndOr adAndOr = adAndOrContainer.getAdAndOr();

        assertEquals( 2, adAndOr.getConditionCount() );

        ByteBuffer bb = ByteBuffer.allocate( adAndOr.computeLength() );

        try
        {
            bb = adAndOr.encode( bb );

            // Check the length
            assertEquals( 0x2D, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    @Test(expected = DecoderException.class)
    public void testDecodeAdAndOrWithEmptySeq() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 2 );

        stream.put( new byte[]
            {
                0x30, 0x0
        } );

        stream.flip();

        AdAndOrContainer adAndOrContainer = new AdAndOrContainer();
        adAndOrContainer.setStream( stream );

        krbDecoder.decode( stream, adAndOrContainer );
        fail();
    }


    @Test(expected = DecoderException.class)
    public void testDecodeAdAndOrEmptyConditionCount() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 4 );

        stream.put( new byte[]
            {
                0x30, 0x02,
                ( byte ) 0xA0, 0x00
        } );

        stream.flip();

        AdAndOrContainer adAndOrContainer = new AdAndOrContainer();
        adAndOrContainer.setStream( stream );

        krbDecoder.decode( stream, adAndOrContainer );
        fail();
    }


    @Test(expected = DecoderException.class)
    public void testDecodeAdAndOrNullConditionCount() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 6 );

        stream.put( new byte[]
            {
                0x30, 0x04,
                ( byte ) 0xA0, 0x02,
                0x02, 0x00
        } );

        stream.flip();

        AdAndOrContainer adAndOrContainer = new AdAndOrContainer();
        adAndOrContainer.setStream( stream );

        krbDecoder.decode( stream, adAndOrContainer );
        fail();
    }


    @Test(expected = DecoderException.class)
    public void testDecodeAdAndOrNoConditionCount() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x28 );

        stream.put( new byte[]
            {
                0x30, 0x26,
                ( byte ) 0xA1, 0x24, // elements
                0x30,
                0x22,
                0x30,
                0x0F,
                ( byte ) 0xA0,
                0x03, // ad-type
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
                0x0F,
                ( byte ) 0xA0,
                0x03, // ad-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x08, // ad-data
                0x04,
                0x06,
                'g',
                'h',
                'i',
                'j',
                'k',
                'l'
        } );

        stream.flip();

        AdAndOrContainer adAndOrContainer = new AdAndOrContainer();
        adAndOrContainer.setStream( stream );

        krbDecoder.decode( stream, adAndOrContainer );
        fail();
    }


    @Test(expected = DecoderException.class)
    public void testDecodeAdAndOrNoElements() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );

        stream.put( new byte[]
            {
                0x30, 0x05,
                ( byte ) 0xA0, 0x03, // condition count
                0x02,
                0x01,
                0x02,
        } );

        stream.flip();

        AdAndOrContainer adAndOrContainer = new AdAndOrContainer();
        adAndOrContainer.setStream( stream );

        krbDecoder.decode( stream, adAndOrContainer );
        fail();
    }


    @Test(expected = DecoderException.class)
    public void testDecodeAdAndOrEmptyElements() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            {
                0x30, 0x07,
                ( byte ) 0xA0, 0x03, // condition count
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x00 // elements
        } );

        stream.flip();

        AdAndOrContainer adAndOrContainer = new AdAndOrContainer();

        krbDecoder.decode( stream, adAndOrContainer );
        fail();
    }


    @Test(expected = DecoderException.class)
    public void testDecodeAdAndOrNullElements() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x0B );

        stream.put( new byte[]
            {
                0x30, 0x09,
                ( byte ) 0xA0, 0x03, // condition count
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x02, // elements
                0x30,
                0x00
        } );

        stream.flip();

        AdAndOrContainer adAndOrContainer = new AdAndOrContainer();
        adAndOrContainer.setStream( stream );

        krbDecoder.decode( stream, adAndOrContainer );
        fail();
    }
}
