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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.padata.PaDataContainer;
import org.apache.directory.shared.kerberos.codec.types.PaDataType;
import org.apache.directory.shared.kerberos.components.PaData;
import org.apache.directory.shared.util.Strings;
import org.junit.Test;


/**
 * Test cases for PaData codec.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PaDataDecoderTest
{
    @Test
    public void testDecodePaData()
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x11 );

        stream.put( new byte[]
            {
                0x30, 0x0F,
                ( byte ) 0xA1, 0x03, // padata-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA2,
                0x08, // padata-value
                0x04,
                0x06,
                'p',
                'a',
                'd',
                'a',
                't',
                'a'
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        PaDataContainer container = new PaDataContainer();

        try
        {
            krbDecoder.decode( stream, container );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();

            fail( de.getMessage() );
        }

        PaData checksum = container.getPaData();

        assertEquals( PaDataType.getTypeByValue( 2 ), checksum.getPaDataType() );
        assertTrue( Arrays.equals( Strings.getBytesUtf8( "padata" ), checksum.getPaDataValue() ) );

        ByteBuffer bb = ByteBuffer.allocate( checksum.computeLength() );

        try
        {
            bb = checksum.encode( bb );

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


    @Test(expected = DecoderException.class)
    public void testDecodePaDataWithoutType() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0xC );

        stream.put( new byte[]
            {
                0x30, 0xA,
                ( byte ) 0xA2, 0x08, // padata-value
                0x04,
                0x06,
                'p',
                'a',
                'd',
                'a',
                't',
                'a'
        } );

        stream.flip();

        PaDataContainer chkContainer = new PaDataContainer();

        krbDecoder.decode( stream, chkContainer );
        fail();
    }


    @Test(expected = DecoderException.class)
    public void testDecodeChecksumWithoutPaDataValue() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );

        stream.put( new byte[]
            {
                0x30, 0x05,
                ( byte ) 0xA1, 0x03, // padata-type
                0x02,
                0x01,
                0x02
        } );

        stream.flip();

        PaDataContainer container = new PaDataContainer();

        krbDecoder.decode( stream, container );
        fail();
    }


    @Test
    public void testDecodePaDataWithEmptySeq() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x11 );

        stream.put( new byte[]
            {
                0x30, 0x09,
                ( byte ) 0xA1, 0x03, // padata-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA2,
                0x02, // padata-value
                0x04,
                0x00
        } );

        stream.flip();

        PaDataContainer container = new PaDataContainer();

        krbDecoder.decode( stream, container );
    }

}
