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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.etypeInfoEntry.ETypeInfoEntryContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.ETypeInfoEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Test cases for ETYPE-INFO-ENTRY codec.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ETypeInfoEntryDecoderTest
{
    /**
     * Test the decoding of a full ETYPE-INFO-ENTRY
     */
    @Test
    public void testDecodeETypeInfoEntry()
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x0F );

        stream.put( new byte[]
            {
                0x30, 0x0D,
                ( byte ) 0xA0, 0x03, // etype
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x06, // salt
                0x04,
                0x04,
                0x31,
                0x32,
                0x33,
                0x34
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        ETypeInfoEntryContainer container = new ETypeInfoEntryContainer();

        try
        {
            Asn1Decoder.decode( stream, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();

            fail( de.getMessage() );
        }

        ETypeInfoEntry etypeInforEntry = container.getETypeInfoEntry();

        assertEquals( EncryptionType.DES3_CBC_MD5, etypeInforEntry.getEType() );
        assertTrue( Arrays.equals( Strings.getBytesUtf8( "1234" ), etypeInforEntry.getSalt() ) );

        ByteBuffer bb = ByteBuffer.allocate( etypeInforEntry.computeLength() );

        try
        {
            bb = etypeInforEntry.encode( bb );

            // Check the length
            assertEquals( 0x0F, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a ETYPE-INFO-ENTRY with no salt
     */
    @Test
    public void testDecodeETypeInfoEntryNoSalt()
    {

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

        ETypeInfoEntryContainer container = new ETypeInfoEntryContainer();

        try
        {
            Asn1Decoder.decode( stream, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();

            fail( de.getMessage() );
        }

        ETypeInfoEntry etypeInforEntry = container.getETypeInfoEntry();

        assertEquals( EncryptionType.DES3_CBC_MD5, etypeInforEntry.getEType() );
        assertNull( etypeInforEntry.getSalt() );

        ByteBuffer bb = ByteBuffer.allocate( etypeInforEntry.computeLength() );

        try
        {
            bb = etypeInforEntry.encode( bb );

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
     * Test the decoding of a ETYPE-INFO-ENTRY with an empty salt
     */
    @Test
    public void testDecodeETypeInfoEntryEmptySalt() throws DecoderException
    {

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

        ETypeInfoEntryContainer container = new ETypeInfoEntryContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    /**
     * Test the decoding of a ETYPE-INFO-ENTRY with an null salt
     */
    @Test
    public void testDecodeETypeInfoEntryNullSalt()
    {

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
                0x04,
                0x00
        } );

        stream.flip();

        ETypeInfoEntryContainer container = new ETypeInfoEntryContainer();

        try
        {
            Asn1Decoder.decode( stream, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();

            fail( de.getMessage() );
        }

        ETypeInfoEntry etypeInforEntry = container.getETypeInfoEntry();

        assertEquals( EncryptionType.DES3_CBC_MD5, etypeInforEntry.getEType() );
        assertNull( etypeInforEntry.getSalt() );

        ByteBuffer bb = ByteBuffer.allocate( etypeInforEntry.computeLength() );

        try
        {
            bb = etypeInforEntry.encode( bb );

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
     * Test the decoding of an empty ETYPE-INFO-ENTRY
     * @throws DecoderException
     */
    @Test
    public void testDecodeEmptyETypeInforEntry() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            {
                0x30, 0x00
        } );

        stream.flip();

        ETypeInfoEntryContainer container = new ETypeInfoEntryContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    /**
     * Test the decoding of an ETYPE-INFO-ENTRY with no etype
     * @throws DecoderException
     */
    @Test
    public void testDecodeEmptyETypeInfoEntryNoEType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            {
                0x30, 0x04,
                ( byte ) 0xA2, 0x04,
                0x04, 0x00
        } );

        stream.flip();

        ETypeInfoEntryContainer container = new ETypeInfoEntryContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    /**
     * Test the decoding of an ETYPE-INFO-ENTRY with an empty etype
     * @throws org.apache.directory.api.asn1.DecoderException
     */
    @Test
    public void testDecodeEmptyETypeInfoEntryEmptyEType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            {
                0x30, 0x04,
                ( byte ) 0xA0, 0x00
        } );

        stream.flip();

        ETypeInfoEntryContainer container = new ETypeInfoEntryContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    /**
     * Test the decoding of an ETYPE-INFO-ENTRY with a empty etype tag
     * @throws DecoderException
     */
    @Test
    public void testDecodeEmptyETypeInfoEntryEmptyETypeTag() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            {
                0x30, 0x04,
                ( byte ) 0xA0, 0x02,
                0x02, 0x00
        } );

        stream.flip();

        ETypeInfoEntryContainer container = new ETypeInfoEntryContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    /**
     * Test the decoding of an ETYPE-INFO-ENTRY with a bad etype
     * @throws DecoderException
     */
    @Test
    public void testDecodeEmptyETypeInfoEntryBadEType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );

        stream.put( new byte[]
            {
                0x30, 0x05,
                ( byte ) 0xA0, 0x03,
                0x02, 0x01, 0x40
        } );

        stream.flip();

        ETypeInfoEntryContainer container = new ETypeInfoEntryContainer();

        try
        {
            Asn1Decoder.decode( stream, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();

            fail( de.getMessage() );
        }

        ETypeInfoEntry etypeInforEntry = container.getETypeInfoEntry();

        assertEquals( EncryptionType.UNKNOWN, etypeInforEntry.getEType() );
        assertNull( etypeInforEntry.getSalt() );

        ByteBuffer bb = ByteBuffer.allocate( etypeInforEntry.computeLength() );

        try
        {
            bb = etypeInforEntry.encode( bb );

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
