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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.etypeInfo2Entry.ETypeInfo2EntryContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.ETypeInfo2Entry;
import org.apache.directory.shared.util.Strings;
import org.junit.Test;


/**
 * Test cases for ETYPE-INFO2-ENTRY codec.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ETypeInfo2EntryDecoderTest
{
    /**
     * Test the decoding of a full ETYPE-INFO2-ENTRY
     */
    @Test
    public void testDecodeETypeInfoEntry()
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x17 );

        stream.put( new byte[]
            {
                0x30, 0x15,
                ( byte ) 0xA0, 0x03, // etype
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x06, // salt
                0x1B,
                0x04,
                'a',
                'b',
                'c',
                'd',
                ( byte ) 0xA2,
                0x06, // s2kparams
                0x04,
                0x04,
                0x31,
                0x32,
                0x33,
                0x34
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        ETypeInfo2EntryContainer container = new ETypeInfo2EntryContainer();

        try
        {
            krbDecoder.decode( stream, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();

            fail( de.getMessage() );
        }

        ETypeInfo2Entry etypeInfo2Entry = container.getETypeInfo2Entry();

        assertEquals( EncryptionType.DES3_CBC_MD5, etypeInfo2Entry.getEType() );
        assertEquals( "abcd", etypeInfo2Entry.getSalt() );
        assertTrue( Arrays.equals( Strings.getBytesUtf8( "1234" ), etypeInfo2Entry.getS2kparams() ) );

        ByteBuffer bb = ByteBuffer.allocate( etypeInfo2Entry.computeLength() );

        try
        {
            bb = etypeInfo2Entry.encode( bb );

            // Check the length
            assertEquals( 0x17, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a ETYPE-INFO2-ENTRY with no salt nor s2kparams
     */
    @Test
    public void testDecodeETypeInfo2EntryNoSaltNoS2KParams()
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );

        stream.put( new byte[]
            {
                0x30, 0x05,
                ( byte ) 0xA0, 0x03, // etype
                0x02,
                0x01,
                0x05
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        ETypeInfo2EntryContainer container = new ETypeInfo2EntryContainer();

        try
        {
            krbDecoder.decode( stream, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();

            fail( de.getMessage() );
        }

        ETypeInfo2Entry etypeInfo2Entry = container.getETypeInfo2Entry();

        assertEquals( EncryptionType.DES3_CBC_MD5, etypeInfo2Entry.getEType() );
        assertNull( etypeInfo2Entry.getSalt() );
        assertNull( etypeInfo2Entry.getS2kparams() );

        ByteBuffer bb = ByteBuffer.allocate( etypeInfo2Entry.computeLength() );

        try
        {
            bb = etypeInfo2Entry.encode( bb );

            // Check the length
            assertEquals( 0x07, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a ETYPE-INFO2-ENTRY with an empty salt
     */
    @Test(expected = DecoderException.class)
    public void testDecodeETypeInfo2EntryEmptySalt() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            {
                0x30, 0x07,
                ( byte ) 0xA0, 0x03, // etype
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x00 // salt
        } );

        stream.flip();

        ETypeInfo2EntryContainer container = new ETypeInfo2EntryContainer();

        krbDecoder.decode( stream, container );
        fail();
    }


    /**
     * Test the decoding of a ETYPE-INFO2-ENTRY with an null salt
     */
    @Test
    public void testDecodeETypeInfo2EntryNullSalt()
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x0B );

        stream.put( new byte[]
            {
                0x30, 0x09,
                ( byte ) 0xA0, 0x03, // etype
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x02, // salt
                0x1B,
                0x00
        } );

        stream.flip();

        ETypeInfo2EntryContainer container = new ETypeInfo2EntryContainer();

        try
        {
            krbDecoder.decode( stream, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();

            fail( de.getMessage() );
        }

        ETypeInfo2Entry etypeInfo2Entry = container.getETypeInfo2Entry();

        assertEquals( EncryptionType.DES3_CBC_MD5, etypeInfo2Entry.getEType() );
        assertNull( etypeInfo2Entry.getSalt() );
        assertNull( etypeInfo2Entry.getS2kparams() );

        ByteBuffer bb = ByteBuffer.allocate( etypeInfo2Entry.computeLength() );

        try
        {
            bb = etypeInfo2Entry.encode( bb );

            // Check the length
            assertEquals( 0x07, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            ByteBuffer stream2 = ByteBuffer.allocate( 0x07 );

            stream2.put( new byte[]
                {
                    0x30, 0x05,
                    ( byte ) 0xA0, 0x03, // etype
                    0x02,
                    0x01,
                    0x05
            } );

            String decodedPdu2 = Strings.dumpBytes( stream2.array() );

            assertEquals( encodedPdu, decodedPdu2 );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of an empty ETYPE-INFO2-ENTRY
     * @throws DecoderException
     */
    @Test(expected = DecoderException.class)
    public void testDecodeEmptyETypeInfo2Entry() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            {
                0x30, 0x00
        } );

        stream.flip();

        ETypeInfo2EntryContainer container = new ETypeInfo2EntryContainer();

        krbDecoder.decode( stream, container );
        fail();
    }


    /**
     * Test the decoding of an ETYPE-INFO2-ENTRY with no etype
     * @throws DecoderException
     */
    @Test(expected = DecoderException.class)
    public void testDecodeEmptyETypeInfo2EntryNoEType() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            {
                0x30, 0x04,
                ( byte ) 0xA2, 0x04,
                0x04, 0x00
        } );

        stream.flip();

        ETypeInfo2EntryContainer container = new ETypeInfo2EntryContainer();

        krbDecoder.decode( stream, container );
        fail();
    }


    /**
     * Test the decoding of an ETYPE-INFO2-ENTRY with an empty etype
     * @throws DecoderException
     */
    @Test(expected = DecoderException.class)
    public void testDecodeEmptyETypeInfo2EntryEmptyEType() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            {
                0x30, 0x04,
                ( byte ) 0xA0, 0x00
        } );

        stream.flip();

        ETypeInfo2EntryContainer container = new ETypeInfo2EntryContainer();

        krbDecoder.decode( stream, container );
        fail();
    }


    /**
     * Test the decoding of an ETYPE-INFO2-ENTRY with a empty etype tag
     * @throws DecoderException
     */
    @Test(expected = DecoderException.class)
    public void testDecodeEmptyETypeInfo2EntryEmptyETypeTag() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            {
                0x30, 0x04,
                ( byte ) 0xA0, 0x02,
                0x02, 0x00
        } );

        stream.flip();

        ETypeInfo2EntryContainer container = new ETypeInfo2EntryContainer();

        krbDecoder.decode( stream, container );
        fail();
    }


    /**
     * Test the decoding of an ETYPE-INFO2-ENTRY with a bad etype
     * @throws DecoderException
     */
    @Test
    public void testDecodeEmptyETypeInfo2EntryBadEType() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );

        stream.put( new byte[]
            {
                0x30, 0x05,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x40
        } );

        stream.flip();

        ETypeInfo2EntryContainer container = new ETypeInfo2EntryContainer();

        try
        {
            krbDecoder.decode( stream, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();

            fail( de.getMessage() );
        }

        ETypeInfo2Entry etypeInfo2Entry = container.getETypeInfo2Entry();

        assertEquals( EncryptionType.UNKNOWN, etypeInfo2Entry.getEType() );
        assertNull( etypeInfo2Entry.getSalt() );
        assertNull( etypeInfo2Entry.getS2kparams() );

        ByteBuffer bb = ByteBuffer.allocate( etypeInfo2Entry.computeLength() );

        try
        {
            bb = etypeInfo2Entry.encode( bb );

            // Check the length
            assertEquals( 0x07, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            ByteBuffer stream2 = ByteBuffer.allocate( 0x07 );

            stream2.put( new byte[]
                {
                    0x30, 0x05,
                    ( byte ) 0xA0, 0x03, // etype
                    0x02,
                    0x01,
                    ( byte ) 0xFF
            } );

            String decodedPdu2 = Strings.dumpBytes( stream2.array() );
            assertEquals( decodedPdu2, encodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
}
