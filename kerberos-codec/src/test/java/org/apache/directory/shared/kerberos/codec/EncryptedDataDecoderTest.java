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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;

import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.kerberos.codec.encryptedData.EncryptedDataContainer;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.util.Strings;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the EncryptedData decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class EncryptedDataDecoderTest
{
    /**
     * Test the decoding of a EncryptedData
     */
    @Test
    public void testEncryptedData()
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x16 );

        stream.put( new byte[]
            {
                0x30, 0x14,
                ( byte ) 0xA0, 0x03, // etype
                0x02,
                0x01,
                0x12, //
                ( byte ) 0xA1,
                0x03, // kvno
                0x02,
                0x01,
                0x05, //
                ( byte ) 0xA2,
                0x08, // cipher
                0x04,
                0x06,
                'a',
                'b',
                'c',
                'd',
                'e',
                'f'
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a EncryptedData Container
        Asn1Container encryptedDataContainer = new EncryptedDataContainer();

        // Decode the EncryptedData PDU
        try
        {
            kerberosDecoder.decode( stream, encryptedDataContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        // Check the decoded EncryptedData
        EncryptedData encryptedData = ( ( EncryptedDataContainer ) encryptedDataContainer ).getEncryptedData();

        assertEquals( EncryptionType.AES256_CTS_HMAC_SHA1_96, encryptedData.getEType() );
        assertEquals( 5, encryptedData.getKvno() );
        assertTrue( Arrays.equals( Strings.getBytesUtf8( "abcdef" ), encryptedData.getCipher() ) );

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( encryptedData.computeLength() );

        try
        {
            bb = encryptedData.encode( bb );

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
     * Test the decoding of a EncryptedData with no kvno
     */
    @Test
    public void testEncryptedDataNoKvno()
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x11 );

        stream.put( new byte[]
            {
                0x30, 0x0F,
                ( byte ) 0xA0, 0x03, // etype
                0x02,
                0x01,
                0x12, //
                ( byte ) 0xA2,
                0x08, // cipher
                0x04,
                0x06,
                'a',
                'b',
                'c',
                'd',
                'e',
                'f'
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a EncryptedData Container
        Asn1Container encryptedDataContainer = new EncryptedDataContainer();

        // Decode the EncryptedData PDU
        try
        {
            kerberosDecoder.decode( stream, encryptedDataContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        // Check the decoded EncryptedData
        EncryptedData encryptedData = ( ( EncryptedDataContainer ) encryptedDataContainer ).getEncryptedData();

        assertEquals( EncryptionType.AES256_CTS_HMAC_SHA1_96, encryptedData.getEType() );
        assertFalse( encryptedData.hasKvno() );
        assertTrue( Arrays.equals( Strings.getBytesUtf8( "abcdef" ), encryptedData.getCipher() ) );

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( encryptedData.computeLength() );

        try
        {
            bb = encryptedData.encode( bb );

            // Check the length
            assertEquals( 0x11, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a EncryptedData with nothing in it
     */
    @Test(expected = DecoderException.class)
    public void testEncryptedDataEmpty() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            { 0x30, 0x00 } );

        stream.flip();

        // Allocate a EncryptedData Container
        Asn1Container encryptedDataContainer = new EncryptedDataContainer();

        // Decode the EncryptedData PDU
        kerberosDecoder.decode( stream, encryptedDataContainer );
        fail();
    }


    /**
     * Test the decoding of a EncryptedData with no type
     */
    @Test(expected = DecoderException.class)
    public void testEncryptedDataNoEType() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            { 0x30, 0x02,
                ( byte ) 0xA0, 0x00 // etype
        } );

        stream.flip();

        // Allocate a EncryptedData Container
        Asn1Container encryptedDataContainer = new EncryptedDataContainer();

        // Decode the EncryptedData PDU
        kerberosDecoder.decode( stream, encryptedDataContainer );
        fail();
    }


    /**
     * Test the decoding of a EncryptedData with a missing type
     */
    @Test(expected = DecoderException.class)
    public void testEncryptedDataMissingEType() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x11 );

        stream.put( new byte[]
            {
                0x30, 0x0F,
                ( byte ) 0xA1, 0x03, // kvno
                0x02,
                0x01,
                0x05, //
                ( byte ) 0xA2,
                0x08, // cipher
                0x04,
                0x06,
                'a',
                'b',
                'c',
                'd',
                'e',
                'f'
        } );

        stream.flip();

        // Allocate a EncryptedData Container
        Asn1Container encryptedDataContainer = new EncryptedDataContainer();

        // Decode the EncryptedData PDU
        kerberosDecoder.decode( stream, encryptedDataContainer );
        fail();
    }


    /**
     * Test the decoding of a EncryptedData with an empty type
     */
    @Test(expected = DecoderException.class)
    public void testEncryptedDataEmptyType() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x0B );

        stream.put( new byte[]
            { 0x30, 0x04,
                ( byte ) 0xA0, 0x02, // etype
                0x02,
                0x00 // 
        } );

        stream.flip();

        // Allocate a EncryptedData Container
        Asn1Container encryptedDataContainer = new EncryptedDataContainer();

        // Decode the EncryptedData PDU
        kerberosDecoder.decode( stream, encryptedDataContainer );
        fail();
    }


    /**
     * Test the decoding of a EncryptedData with an empty kvno
     */
    @Test(expected = DecoderException.class)
    public void testEncryptedDataEmptyKvno() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            { 0x30, 0x07,
                ( byte ) 0xA0, 0x03, // etype
                0x02,
                0x01,
                0x01, // 
                ( byte ) 0xA1,
                0x00 // kvno
        } );

        stream.flip();

        // Allocate a EncryptedData Container
        Asn1Container encryptedDataContainer = new EncryptedDataContainer();

        // Decode the EncryptedData PDU
        kerberosDecoder.decode( stream, encryptedDataContainer );
        fail();
    }


    /**
     * Test the decoding of a EncryptedData with no cipher
     */
    @Test(expected = DecoderException.class)
    public void testEncryptedDataNoCipher() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x0C );

        stream.put( new byte[]
            { 0x30, 0x0A,
                ( byte ) 0xA0, 0x03, // etype
                0x02,
                0x01,
                0x01, // 
                ( byte ) 0xA1,
                0x02, // kvno
                0x02,
                0x01,
                0x05 //
        } );

        stream.flip();

        // Allocate a EncryptedData Container
        Asn1Container encryptedDataContainer = new EncryptedDataContainer();

        // Decode the EncryptedData PDU
        kerberosDecoder.decode( stream, encryptedDataContainer );
        fail();
    }


    /**
     * Test the decoding of a EncryptedData empty cipher
     */
    @Test(expected = DecoderException.class)
    public void testEncryptedDataEmptyCipher() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x0E );

        stream.put( new byte[]
            { 0x30, 0x0C,
                ( byte ) 0xA0, 0x03, // etype
                0x02,
                0x01,
                0x01, // 
                ( byte ) 0xA1,
                0x03, // kvno
                0x02,
                0x01,
                0x01, //
                ( byte ) 0xA2,
                0x00 // cipher
        } );

        stream.flip();

        // Allocate a EncryptedData Container
        Asn1Container encryptedDataContainer = new EncryptedDataContainer();

        // Decode the EncryptedData PDU
        kerberosDecoder.decode( stream, encryptedDataContainer );
        fail();
    }


    /**
     * Test the decoding of a EncryptedData with a null cipher
     */
    @Test(expected = DecoderException.class)
    public void testEncryptedDataNullCipher() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x10 );

        stream.put( new byte[]
            { 0x30, 0x0E,
                ( byte ) 0xA0, 0x03, // etype
                0x02,
                0x01,
                0x01, // 
                ( byte ) 0xA1,
                0x03, // kvno
                0x02,
                0x01,
                0x01, //
                ( byte ) 0xA2,
                0x02, // cipher
                0x04,
                0x00
        } );

        stream.flip();

        // Allocate a EncryptedData Container
        Asn1Container encryptedDataContainer = new EncryptedDataContainer();

        // Decode the EncryptedData PDU
        kerberosDecoder.decode( stream, encryptedDataContainer );
    }
}
