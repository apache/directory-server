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

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.transitedEncoding.TransitedEncodingContainer;
import org.apache.directory.shared.kerberos.codec.types.TransitedEncodingType;
import org.apache.directory.shared.kerberos.components.TransitedEncoding;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the TransitedEncoding decoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class TransitedEncodingDecoderTest
{
    /**
     * Test the decoding of a full TransitedEncoding
     */
    @Test
    public void testTransitedEncoding()
    {
        int len = 0x11;

        ByteBuffer stream = ByteBuffer.allocate( len );

        stream.put( new byte[]
            { 0x30, 0x0F,
                ( byte ) 0xA0, 0x03, // tr-type
                0x02,
                0x01,
                0x01, //
                ( byte ) 0xA1,
                0x08, // contents
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

        // Allocate a TransitedEncoding Container
        Asn1Container transitedEncodingContainer = new TransitedEncodingContainer();

        // Decode the TransitedEncoding PDU
        try
        {
            Asn1Decoder.decode( stream, transitedEncodingContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        // Check the decoded TransitedEncoding
        TransitedEncoding transitedEncoding = ( ( TransitedEncodingContainer ) transitedEncodingContainer )
            .getTransitedEncoding();

        assertEquals( TransitedEncodingType.DOMAIN_X500_COMPRESS, transitedEncoding.getTrType() );
        assertTrue( Arrays.equals( Strings.getBytesUtf8( "abcdef" ), transitedEncoding.getContents() ) );

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( transitedEncoding.computeLength() );

        try
        {
            bb = transitedEncoding.encode( bb );

            // Check the length
            assertEquals( len, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a TransitedEncoding with nothing in it
     */
    @Test(expected = DecoderException.class)
    public void testTransitedEncodingEmpty() throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            { 0x30, 0x00 } );

        stream.flip();

        // Allocate a TransitedEncoding Container
        Asn1Container transitedEncodingContainer = new TransitedEncodingContainer();

        // Decode the TransitedEncoding PDU
        Asn1Decoder.decode( stream, transitedEncodingContainer );
        fail();
    }


    /**
     * Test the decoding of a TransitedEncoding with an empty tr-type tag
     */
    @Test(expected = DecoderException.class)
    public void testTransitedEncodingEmptyTrTypeTag() throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            { 0x30, 0x02,
                ( byte ) 0xA0, 0x00 // tr-type
        } );

        stream.flip();

        // Allocate a TransitedEncoding Container
        Asn1Container transitedEncodingContainer = new TransitedEncodingContainer();

        // Decode the TransitedEncoding PDU
        Asn1Decoder.decode( stream, transitedEncodingContainer );
        fail();
    }


    /**
     * Test the decoding of a TransitedEncoding with no type
     */
    @Test(expected = DecoderException.class)
    public void testTransitedEncodingNoTrType() throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            { 0x30, 0x04,
                ( byte ) 0xA0, 0x02, // tr-type
                0x02,
                0x00
        } );

        stream.flip();

        // Allocate a TransitedEncoding Container
        Asn1Container transitedEncodingContainer = new TransitedEncodingContainer();

        // Decode the TransitedEncoding PDU
        Asn1Decoder.decode( stream, transitedEncodingContainer );
        fail();
    }


    /**
     * Test the decoding of a TransitedEncoding with no tr-type tag
     */
    @Test(expected = DecoderException.class)
    public void testTransitedEncodingNoTrTypeTag() throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            { 0x30, 0x04,
                ( byte ) 0xA1, 0x02, // contents
                0x04,
                0x00
        } );

        stream.flip();

        // Allocate a TransitedEncoding Container
        Asn1Container transitedEncodingContainer = new TransitedEncodingContainer();

        // Decode the TransitedEncoding PDU
        Asn1Decoder.decode( stream, transitedEncodingContainer );
        fail();
    }


    /**
     * Test the decoding of a TransitedEncoding with an empty contents tag
     */
    @Test(expected = DecoderException.class)
    public void testTransitedEncodingEmptyContentsTag() throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x09 );

        stream.put( new byte[]
            { 0x30, 0x07,
                ( byte ) 0xA0, 0x03, // tr-type
                0x02,
                0x01,
                0x01, // 
                ( byte ) 0xA1,
                0x00 // contents
        } );

        stream.flip();

        // Allocate a TransitedEncoding Container
        Asn1Container transitedEncodingContainer = new TransitedEncodingContainer();

        // Decode the TransitedEncoding PDU
        Asn1Decoder.decode( stream, transitedEncodingContainer );
        fail();
    }


    /**
     * Test the decoding of a TransitedEncoding with something else than a contents
     */
    @Test(expected = DecoderException.class)
    public void testTransitedEncodingBadTag() throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.allocate( 0x0C );

        stream.put( new byte[]
            { 0x30, 0x0A,
                ( byte ) 0xA0, 0x03, // tr-type
                0x02,
                0x01,
                0x01, // 
                ( byte ) 0xA2,
                0x03, // ???
                0x02,
                0x01,
                0x01, //
            } );

        stream.flip();

        // Allocate a TransitedEncoding Container
        Asn1Container transitedEncodingContainer = new TransitedEncodingContainer();

        // Decode the TransitedEncoding PDU
        Asn1Decoder.decode( stream, transitedEncodingContainer );
        fail();
    }
}
