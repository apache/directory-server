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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.lastReq.LastReqContainer;
import org.apache.directory.shared.kerberos.codec.types.LastReqType;
import org.apache.directory.shared.kerberos.components.LastReq;
import org.apache.directory.shared.kerberos.components.LastReqEntry;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Test cases for LastReq decoder.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LastReqDecoderTest
{

    @Test
    public void testLastReqData()
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x36 );

        stream.put( new byte[]
            {
                0x30, 0x34,
                0x30, 0x18,
                ( byte ) 0xA0, 0x03, // lr-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x11, // lr-value
                0x18,
                0x0F,
                '2',
                '0',
                '1',
                '0',
                '1',
                '1',
                '1',
                '0',
                '1',
                '5',
                '4',
                '5',
                '2',
                '5',
                'Z',
                0x30,
                0x18,
                ( byte ) 0xA0,
                0x03, // lr-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x11, // lr-value
                0x18,
                0x0F,
                '2',
                '0',
                '1',
                '0',
                '1',
                '1',
                '1',
                '0',
                '1',
                '5',
                '4',
                '5',
                '2',
                '6',
                'Z'
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        LastReqContainer lastReqContainer = new LastReqContainer();

        // Decode the LastReq PDU
        try
        {
            Asn1Decoder.decode( stream, lastReqContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        LastReq lastReq = lastReqContainer.getLastReq();

        assertNotNull( lastReq.getLastReqs().size() );
        assertEquals( 2, lastReq.getLastReqs().size() );

        String[] expected = new String[]
            { "20101110154525Z", "20101110154526Z" };
        int i = 0;

        for ( LastReqEntry lre : lastReq.getLastReqs() )
        {
            assertEquals( LastReqType.TIME_OF_INITIAL_REQ, lre.getLrType() );
            assertEquals( expected[i++], lre.getLrValue().toString() );

        }

        // Check the encoding
        ByteBuffer bb = ByteBuffer.allocate( lastReq.computeLength() );

        try
        {
            bb = lastReq.encode( bb );

            // Check the length
            assertEquals( 0x36, bb.limit() );

            String encodedPdu = Strings.dumpBytes( bb.array() );

            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    @Test
    public void testLastReqWithoutType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x15 );

        stream.put( new byte[]
            {
                0x30, 0x13,
                ( byte ) 0xA1, 0x11, // lr-value
                0x18,
                0x0F,
                '2',
                '0',
                '1',
                '0',
                '1',
                '1',
                '1',
                '0',
                '1',
                '5',
                '4',
                '5',
                '2',
                '6',
                'Z'
        } );

        stream.flip();

        LastReqContainer lastReqContainer = new LastReqContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, lastReqContainer);
        } );
    }


    @Test
    public void testLastReqWithoutValue() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x07 );

        stream.put( new byte[]
            { 0x30, 0x05,
                ( byte ) 0xA0, 0x03, // lr-type
                0x02,
                0x01,
                0x02
        } );

        stream.flip();

        LastReqContainer lastReqContainer = new LastReqContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, lastReqContainer);
        } );
    }


    @Test
    public void testLastReqWithIncorrectPdu() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            { 0x30, 0x00 } );

        stream.flip();

        LastReqContainer lastReqContainer = new LastReqContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, lastReqContainer);
        } );
    }


    @Test
    public void testLastReqWithEmptyValue() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0xB );

        stream.put( new byte[]
            {
                0x30, 0x09,
                ( byte ) 0xA0, 0x03, // lr-type
                0x02,
                0x01,
                0x02,
                ( byte ) 0xA1,
                0x02, // lr-value
                0x18,
                0x00
        } );

        stream.flip();

        LastReqContainer lastReqContainer = new LastReqContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, lastReqContainer);
        } );
    }


    @Test
    public void testLastReqWithEmptyType() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x19 );

        stream.put( new byte[]
            {
                0x30, 0x17,
                ( byte ) 0xA0, 0x02, // lr-type
                0x02,
                0x00,
                ( byte ) 0xA1,
                0x11, // lr-value
                0x18,
                0x0F,
                '2',
                '0',
                '1',
                '0',
                '1',
                '1',
                '1',
                '0',
                '1',
                '5',
                '4',
                '5',
                '2',
                '6',
                'Z'
        } );

        stream.flip();

        LastReqContainer lastReqContainer = new LastReqContainer();

        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, lastReqContainer);
        } );
    }
}
