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
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.asRep.AsRepContainer;
import org.apache.directory.shared.kerberos.messages.AsRep;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * Test the decoder for a AS-REP
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class AsRepDecoderTest
{
    /**
     * Test the decoding of a AS-REP message
     */
    @Test
    public void testDecodeFullAsRep() throws Exception
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0xAC );

        stream.put( new byte[]
            {
                0x6B, ( byte ) 0x81, ( byte ) 0xA9,
                0x30, ( byte ) 0x81, ( byte ) 0xA6,
                ( byte ) 0xA0, 0x03, // PVNO
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x0B,
                ( byte ) 0xA2,
                0x20, // PA-DATA
                0x30,
                0x1E,
                0x30,
                0x0D,
                ( byte ) 0xA1,
                0x03,
                0x02,
                0x01,
                01,
                ( byte ) 0xA2,
                0x06,
                0x04,
                0x04,
                'a',
                'b',
                'c',
                'd',
                0x30,
                0x0D,
                ( byte ) 0xA1,
                0x03,
                0x02,
                0x01,
                01,
                ( byte ) 0xA2,
                0x06,
                0x04,
                0x04,
                'e',
                'f',
                'g',
                'h',
                ( byte ) 0xA3,
                0x0D, // crealm
                0x1B,
                0x0B,
                'E',
                'X',
                'A',
                'M',
                'P',
                'L',
                'E',
                '.',
                'C',
                'O',
                'M',
                ( byte ) 0xA4,
                0x14, // cname
                0x30,
                0x12,
                ( byte ) 0xA0,
                0x03, // name-type
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x0B, // name-string
                0x30,
                0x09,
                0x1B,
                0x07,
                'h',
                'n',
                'e',
                'l',
                's',
                'o',
                'n',
                ( byte ) 0xA5,
                0x40, // Ticket
                0x61,
                0x3E,
                0x30,
                0x3C,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x0D,
                0x1B,
                0x0B,
                'E',
                'X',
                'A',
                'M',
                'P',
                'L',
                'E',
                '.',
                'C',
                'O',
                'M',
                ( byte ) 0xA2,
                0x13,
                0x30,
                0x11,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x0A,
                0x30,
                0x08,
                0x1B,
                0x06,
                'c',
                'l',
                'i',
                'e',
                'n',
                't',
                ( byte ) 0xA3,
                0x11,
                0x30,
                0x0F,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x11,
                ( byte ) 0xA2,
                0x08,
                0x04,
                0x06,
                'a',
                'b',
                'c',
                'd',
                'e',
                'f',
                ( byte ) 0xA6,
                0x11, // enc-part
                0x30,
                0x0F,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x11,
                ( byte ) 0xA2,
                0x08,
                0x04,
                0x06,
                'a',
                'b',
                'c',
                'd',
                'e',
                'f',
        } );

        stream.flip();

        // Allocate a AsRep Container
        AsRepContainer asRepContainer = new AsRepContainer( stream );

        // Decode the AsRep PDU
        try
        {
            kerberosDecoder.decode( stream, asRepContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        AsRep asRep = asRepContainer.getAsRep();

        // Check the encoding
        int length = asRep.computeLength();

        // Check the length
        assertEquals( 0xAC, length );

        // Check the encoding
        ByteBuffer encodedPdu = ByteBuffer.allocate( length );

        try
        {
            encodedPdu = asRep.encode( encodedPdu );

            // Check the length
            assertEquals( 0xAC, encodedPdu.limit() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a AS-REP with nothing in it
     */
    @Test(expected = DecoderException.class)
    public void testAsRepEmpty() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            { 0x30, 0x00 } );

        stream.flip();

        // Allocate a AS-REP Container
        Asn1Container asRepContainer = new AsRepContainer( stream );

        // Decode the AS-REP PDU
        kerberosDecoder.decode( stream, asRepContainer );
        fail();
    }


    /**
     * Test the decoding of a AS-REP with empty Pvno tag
     */
    @Test(expected = DecoderException.class)
    public void testAsRepEmptyPvnoTag() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            {
                0x30, 0x02,
                ( byte ) 0xA0, 0x00
        } );

        stream.flip();

        // Allocate a AS-REP Container
        Asn1Container asRepContainer = new AsRepContainer( stream );

        // Decode the AS-REP PDU
        kerberosDecoder.decode( stream, asRepContainer );
        fail();
    }


    /**
     * Test the decoding of a AS-REP with empty Pvno value
     */
    @Test(expected = DecoderException.class)
    public void testAsRepEmptyPvnoValue() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            {
                0x30, 0x04,
                ( byte ) 0xA0, 0x02,
                0x02, 0x00
        } );

        stream.flip();

        // Allocate a AS-REP Container
        Asn1Container asRepContainer = new AsRepContainer( stream );

        // Decode the AS-REP PDU
        kerberosDecoder.decode( stream, asRepContainer );
        fail();
    }


    /**
     * Test the decoding of a AS-REP message
     */
    @Test
    public void testDecodeFullAsRep2() throws Exception
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x239 );

        stream.put( new byte[]
            {
                0x6b,
                ( byte ) 0x82,
                0x02,
                0x35,
                0x30,
                ( byte ) 0x82,
                0x02,
                0x31,
                ( byte ) 0xa0,
                0x03,
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03,
                0x02,
                0x01,
                0x0b,
                ( byte ) 0xA2,
                0x16,
                0x30,
                0x14,
                0x30,
                0x12,
                ( byte ) 0xA1,
                0x03,
                0x02,
                0x01,
                0x13,
                ( byte ) 0xA2,
                0x0b,
                0x04,
                0x09,
                0x30,
                0x07,
                0x30,
                0x05,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x10,
                ( byte ) 0xA3,
                0x0e,
                0x1b,
                0x0c,
                0x66,
                0x6f,
                0x70,
                0x73,
                0x2e,
                0x70,
                0x73,
                0x75,
                0x2e,
                0x65,
                0x64,
                0x75,
                ( byte ) 0xA4,
                0x14,
                0x30,
                0x12,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x0b,
                0x30,
                0x09,
                0x1b,
                0x07,
                0x71,
                0x71,
                0x67,
                0x35,
                0x30,
                0x31,
                0x38,
                ( byte ) 0xA5,
                ( byte ) 0x82,
                0x01,
                0x00,
                0x61,
                ( byte ) 0x81,
                ( byte ) 0xFd,
                0x30,
                ( byte ) 0x81,
                ( byte ) 0xFa,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x0e,
                0x1b,
                0x0c,
                0x66,
                0x6f,
                0x70,
                0x73,
                0x2e,
                0x70,
                0x73,
                0x75,
                0x2e,
                0x65,
                0x64,
                0x75,
                ( byte ) 0xA2,
                0x21,
                0x30,
                0x1f,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x18,
                0x30,
                0x16,
                0x1b,
                0x06,
                0x6b,
                0x72,
                0x62,
                0x74,
                0x67,
                0x74,
                0x1b,
                0x0c,
                0x66,
                0x6f,
                0x70,
                0x73,
                0x2e,
                0x70,
                0x73,
                0x75,
                0x2e,
                0x65,
                0x64,
                0x75,
                ( byte ) 0xA3,
                ( byte ) 0x81,
                ( byte ) 0xBf,
                0x30,
                ( byte ) 0x81,
                ( byte ) 0xBc,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x10,
                ( byte ) 0xA1,
                0x03,
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA2,
                ( byte ) 0x81,
                ( byte ) 0xAf,
                0x04,
                ( byte ) 0x81,
                ( byte ) 0xAc,
                0x20,
                ( byte ) 0xFa,
                ( byte ) 0x8e,
                0x26,
                ( byte ) 0x99,
                0x5c,
                0x15,
                ( byte ) 0xDb,
                ( byte ) 0x82,
                ( byte ) 0xDc,
                0x47,
                0x77,
                0x6a,
                ( byte ) 0xC4,
                ( byte ) 0x89,
                ( byte ) 0xC5,
                ( byte ) 0xF6,
                0x00,
                ( byte ) 0xFa,
                ( byte ) 0xAc,
                0x1d,
                ( byte ) 0xD8,
                0x1e,
                0x32,
                0x78,
                ( byte ) 0xFe,
                0x1a,
                0x32,
                ( byte ) 0xA6,
                0x6f,
                ( byte ) 0xB4,
                0x0b,
                0x36,
                ( byte ) 0xE1,
                0x4c,
                ( byte ) 0xC4,
                0x7f,
                ( byte ) 0x8a,
                0x49,
                0x7a,
                0x5c,
                ( byte ) 0xB7,
                0x55,
                0x35,
                0x64,
                0x4e,
                ( byte ) 0x88,
                ( byte ) 0xEa,
                0x05,
                0x04,
                0x5a,
                0x63,
                0x15,
                ( byte ) 0xE6,
                ( byte ) 0xEc,
                0x4f,
                0x40,
                ( byte ) 0x8b,
                0x2f,
                ( byte ) 0x8a,
                0x42,
                ( byte ) 0x9c,
                0x7f,
                0x09,
                ( byte ) 0xDe,
                0x4e,
                0x35,
                0x43,
                0x4a,
                0x50,
                ( byte ) 0x8f,
                0x03,
                ( byte ) 0xC3,
                ( byte ) 0xC6,
                0x69,
                ( byte ) 0x8d,
                0x6b,
                0x6e,
                0x14,
                ( byte ) 0x8b,
                0x15,
                0x59,
                ( byte ) 0xF2,
                ( byte ) 0xF1,
                ( byte ) 0xBe,
                ( byte ) 0x9d,
                0x30,
                0x4d,
                0x14,
                ( byte ) 0xE4,
                0x23,
                ( byte ) 0xB6,
                0x68,
                ( byte ) 0xE3,
                0x67,
                0x24,
                0x4a,
                ( byte ) 0xD1,
                ( byte ) 0xC1,
                0x54,
                ( byte ) 0xF5,
                ( byte ) 0x93,
                0x53,
                ( byte ) 0xC0,
                ( byte ) 0xCc,
                ( byte ) 0xC1,
                ( byte ) 0xAf,
                0x32,
                ( byte ) 0xBb,
                ( byte ) 0xD0,
                0x66,
                ( byte ) 0xCa,
                ( byte ) 0xAf,
                0x68,
                ( byte ) 0xBf,
                0x47,
                ( byte ) 0xC2,
                ( byte ) 0x8a,
                ( byte ) 0xD7,
                0x3e,
                0x0a,
                ( byte ) 0xD6,
                ( byte ) 0x9a,
                0x22,
                ( byte ) 0xB3,
                ( byte ) 0x8e,
                ( byte ) 0xEa,
                ( byte ) 0xEc,
                ( byte ) 0x9e,
                ( byte ) 0xBc,
                ( byte ) 0xC3,
                ( byte ) 0x83,
                0x43,
                ( byte ) 0x93,
                ( byte ) 0xFe,
                0x1e,
                0x47,
                ( byte ) 0xB3,
                ( byte ) 0xAf,
                ( byte ) 0xD8,
                0x77,
                ( byte ) 0xB5,
                ( byte ) 0xAa,
                ( byte ) 0xBe,
                0x2b,
                ( byte ) 0xC0,
                ( byte ) 0xE7,
                ( byte ) 0xBb,
                0x69,
                0x28,
                0x30,
                0x04,
                0x07,
                ( byte ) 0xB6,
                ( byte ) 0xC0,
                ( byte ) 0xE7,
                ( byte ) 0x8a,
                ( byte ) 0x98,
                0x36,
                0x73,
                0x5e,
                0x09,
                ( byte ) 0x87,
                0x32,
                ( byte ) 0xC8,
                0x65,
                0x64,
                0x66,
                ( byte ) 0xC1,
                ( byte ) 0x9a,
                ( byte ) 0xAe,
                ( byte ) 0x89,
                ( byte ) 0xA6,
                ( byte ) 0x81,
                ( byte ) 0xE2,
                0x30,
                ( byte ) 0x81,
                ( byte ) 0xDf,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x10,
                ( byte ) 0xA2,
                ( byte ) 0x81,
                ( byte ) 0xD7,
                0x04,
                ( byte ) 0x81,
                ( byte ) 0xD4,
                ( byte ) 0x9f,
                ( byte ) 0xC6,
                0x12,
                0x73,
                0x7f,
                0x4a,
                0x44,
                ( byte ) 0xA7,
                ( byte ) 0xBb,
                ( byte ) 0xD4,
                ( byte ) 0x82,
                ( byte ) 0xF0,
                0x1d,
                0x17,
                ( byte ) 0xF5,
                ( byte ) 0xC1,
                0x19,
                0x0f,
                ( byte ) 0xD7,
                0x3c,
                0x03,
                ( byte ) 0x96,
                ( byte ) 0xF4,
                0x19,
                0x72,
                ( byte ) 0xD3,
                ( byte ) 0xCa,
                ( byte ) 0xD7,
                0x70,
                0x18,
                0x35,
                ( byte ) 0x9a,
                0x61,
                0x67,
                0x78,
                ( byte ) 0x80,
                0x11,
                ( byte ) 0x80,
                0x6a,
                ( byte ) 0xFb,
                0x19,
                ( byte ) 0xF8,
                ( byte ) 0x89,
                ( byte ) 0xB3,
                0x5e,
                0x2e,
                ( byte ) 0xC6,
                ( byte ) 0x82,
                0x2d,
                0x17,
                ( byte ) 0xAa,
                0x6a,
                0x07,
                0x03,
                0x18,
                0x3e,
                ( byte ) 0xCe,
                0x3e,
                0x7c,
                ( byte ) 0x9b,
                0x17,
                0x4d,
                ( byte ) 0xF5,
                ( byte ) 0xBe,
                ( byte ) 0xB8,
                0x6b,
                ( byte ) 0xF4,
                0x52,
                0x25,
                0x28,
                ( byte ) 0x9a,
                ( byte ) 0x91,
                ( byte ) 0x9e,
                ( byte ) 0xAa,
                ( byte ) 0xE3,
                ( byte ) 0x9b,
                ( byte ) 0xAb,
                ( byte ) 0xD4,
                0x68,
                0x3e,
                ( byte ) 0x88,
                0x65,
                0x2c,
                0x06,
                0x71,
                0x52,
                ( byte ) 0xEc,
                ( byte ) 0xE2,
                ( byte ) 0xA4,
                0x3a,
                0x23,
                ( byte ) 0xE0,
                0x68,
                0x57,
                0x5d,
                ( byte ) 0xF6,
                0x2d,
                0x5b,
                0x16,
                ( byte ) 0xBa,
                ( byte ) 0xCd,
                ( byte ) 0xA2,
                0x71,
                0x54,
                0x52,
                ( byte ) 0xE2,
                ( byte ) 0xF6,
                ( byte ) 0x9d,
                ( byte ) 0x98,
                0x18,
                0x4c,
                ( byte ) 0xDa,
                0x64,
                ( byte ) 0xE2,
                0x05,
                0x01,
                ( byte ) 0xEe,
                0x35,
                ( byte ) 0x9d,
                0x75,
                0x4d,
                ( byte ) 0xD8,
                0x64,
                ( byte ) 0x8d,
                ( byte ) 0xCc,
                0x3d,
                ( byte ) 0xAd,
                ( byte ) 0xE6,
                0x52,
                0x49,
                ( byte ) 0xFb,
                ( byte ) 0xF5,
                0x34,
                0x65,
                ( byte ) 0x91,
                0x05,
                0x38,
                ( byte ) 0x80,
                0x5d,
                ( byte ) 0xB4,
                0x06,
                0x63,
                0x63,
                ( byte ) 0xDb,
                ( byte ) 0xEa,
                0x5e,
                ( byte ) 0xF3,
                ( byte ) 0xB2,
                0x65,
                0x7e,
                ( byte ) 0xB9,
                ( byte ) 0x94,
                ( byte ) 0xA9,
                ( byte ) 0xD9,
                0x5b,
                ( byte ) 0xEc,
                0x18,
                0x5b,
                0x4f,
                0x59,
                ( byte ) 0xEa,
                0x6a,
                0x7a,
                ( byte ) 0xEf,
                ( byte ) 0xE1,
                ( byte ) 0xFd,
                0x09,
                0x37,
                ( byte ) 0xBb,
                0x18,
                0x2f,
                ( byte ) 0x87,
                ( byte ) 0x98,
                0x53,
                0x4e,
                0x24,
                0x55,
                ( byte ) 0xF5,
                ( byte ) 0xF7,
                0x36,
                0x13,
                ( byte ) 0xBe,
                ( byte ) 0xC1,
                ( byte ) 0xF0,
                0x31,
                0x3a,
                0x65,
                0x5c,
                0x75,
                0x7b,
                ( byte ) 0x84,
                0x3f,
                ( byte ) 0xA4,
                ( byte ) 0x9c,
                ( byte ) 0xBa,
                0x06,
                ( byte ) 0xDb,
                0x18,
                ( byte ) 0xB6,
                ( byte ) 0xBd,
                0x25,
                ( byte ) 0x95,
                0x60,
                ( byte ) 0xF3,
                0x16,
                ( byte ) 0xFa,
                ( byte ) 0xBc,
                0x30,
                0x53,
                ( byte ) 0xD7,
                0x68,
                0x0c
        } );

        stream.flip();

        // Allocate a AsRep Container
        AsRepContainer asRepContainer = new AsRepContainer( stream );

        // Decode the AsRep PDU
        try
        {
            kerberosDecoder.decode( stream, asRepContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        AsRep asRep = asRepContainer.getAsRep();

        // Check the encoding
        int length = asRep.computeLength();

        // Check the length
        assertEquals( 0x239, length );

        // Check the encoding
        ByteBuffer encodedPdu = ByteBuffer.allocate( length );

        try
        {
            encodedPdu = asRep.encode( encodedPdu );

            // Check the length
            assertEquals( 0x239, encodedPdu.limit() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    };
}
