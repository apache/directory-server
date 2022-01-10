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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.apRep.ApRepContainer;
import org.apache.directory.shared.kerberos.messages.ApRep;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Test the decoder for a AP-REP
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ApRepDecoderTest
{
    /**
     * Test the decoding of a AP-REP message
     */
    @Test
    public void testDecodeFullApRep() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x21 );

        stream.put( new byte[]
            {
                0x6F, 0x1F,
                0x30, 0x1D,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x0F,
                ( byte ) 0xA2,
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
        ApRepContainer apRepContainer = new ApRepContainer( stream );

        // Decode the ApRep PDU
        try
        {
            Asn1Decoder.decode( stream, apRepContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        ApRep apRep = apRepContainer.getApRep();

        // Check the encoding
        int length = apRep.computeLength();

        // Check the length
        assertEquals( 0x21, length );

        // Check the encoding
        ByteBuffer encodedPdu = ByteBuffer.allocate( length );

        try
        {
            encodedPdu = apRep.encode( encodedPdu );

            // Check the length
            assertEquals( 0x21, encodedPdu.limit() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a AP-REP message with a wrong msg-type
     */
    @Test
    public void testDecodeFullApRepWrongMsgType() throws Exception
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x21 );

        stream.put( new byte[]
            {
                0x6F, 0x1F,
                0x30, 0x1D,
                ( byte ) 0xA0, 0x03, // pvno
                0x02,
                0x01,
                0x05,
                ( byte ) 0xA1,
                0x03, // msg-type
                0x02,
                0x01,
                0x0F,
                ( byte ) 0xA2,
                0x11, // enc-part
                0x30,
                0x0E,
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
        ApRepContainer container = new ApRepContainer( stream );

        // Decode the ApRep PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    /**
     * Test the decoding of a AP-REP with nothing in it
     */
    @Test
    public void testApRepEmpty() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            { 0x6F, 0x00 } );

        stream.flip();

        // Allocate a AP-REP Container
        Asn1Container container = new ApRepContainer( stream );

        // Decode the AP-REP PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    /**
     * Test the decoding of a AP-REP with empty SEQ
     */
    @Test
    public void testApRepEmptSEQ() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            {
                0x6F, 0x02,
                0x30, 0x00,
        } );

        stream.flip();

        // Allocate a AP-REP Container
        Asn1Container container = new ApRepContainer( stream );

        // Decode the AP-REP PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    /**
     * Test the decoding of a AP-REP with empty Pvno tag
     */
    @Test
    public void testApRepEmptyPvnoTag() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            {
                0x6F, 0x04,
                0x30, 0x02,
                ( byte ) 0xA0, 0x00
        } );

        stream.flip();

        // Allocate a AP-REP Container
        Asn1Container container = new ApRepContainer( stream );

        // Decode the AP-REP PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }


    /**
     * Test the decoding of a AP-REP with empty Pvno value
     */
    @Test
    public void testAsRepEmptyPvnoValue() throws DecoderException
    {

        ByteBuffer stream = ByteBuffer.allocate( 0x08 );

        stream.put( new byte[]
            {
                0x6E, 0x06,
                0x30, 0x04,
                ( byte ) 0xA0, 0x02,
                0x02, 0x00
        } );

        stream.flip();

        // Allocate a AP-REP Container
        Asn1Container container = new ApRepContainer( stream );

        // Decode the AP-REP PDU
        Assertions.assertThrows( DecoderException.class, () -> {
            Asn1Decoder.decode(stream, container);
        } );
    }
}
