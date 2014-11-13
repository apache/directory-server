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

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.encApRepPart.EncApRepPartContainer;
import org.apache.directory.shared.kerberos.messages.EncApRepPart;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the decoder for a EncApRepPart message
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class EncApRepPartDecoderTest
{
    /**
     * Test the decoding of a EncApRepPart message
     */
    @Test
    public void testDecodeFullEncApRepPart() throws Exception
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x33 );

        stream.put( new byte[]
            {
                0x7B, 0x31,
                0x30, 0x2F,
                ( byte ) 0xA0, 0x11, // ctime 
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
                ( byte ) 0xA1,
                0x03, // cusec
                0x02,
                0x01,
                0x7F,
                ( byte ) 0xA2,
                0x0F, // subkey
                0x30,
                0x0D,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x06,
                0x04,
                0x04,
                'A',
                'B',
                'C',
                'D',
                ( byte ) 0xA3,
                0x04, // seq-number
                0x02,
                0x02,
                0x30,
                0x39,
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a EncApRepPart Container
        Asn1Container encApRepPartContainer = new EncApRepPartContainer( stream );

        // Decode the EncApRepPart PDU
        try
        {
            kerberosDecoder.decode( stream, encApRepPartContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        EncApRepPart encApRepPart = ( ( EncApRepPartContainer ) encApRepPartContainer ).getEncApRepPart();

        // Check the encoding
        int length = encApRepPart.computeLength();

        // Check the length
        assertEquals( 0x33, length );

        // Check the encoding
        ByteBuffer encodedPdu = ByteBuffer.allocate( length );

        try
        {
            encodedPdu = encApRepPart.encode( encodedPdu );

            // Check the length
            assertEquals( 0x33, encodedPdu.limit() );
            assertEquals( decodedPdu, Strings.dumpBytes( encodedPdu.array() ) );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a EncApRepPart with nothing in it
     */
    @Test(expected = DecoderException.class)
    public void testAuthenticatorEmpty() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );

        stream.put( new byte[]
            { 0x7B, 0x00 } );

        stream.flip();

        // Allocate a EncApRepPart Container
        Asn1Container encApRepPartContainer = new EncApRepPartContainer( stream );

        // Decode the EncApRepPart PDU
        kerberosDecoder.decode( stream, encApRepPartContainer );
        fail();
    }


    /**
     * Test the decoding of a EncApRepPart with empty sequence
     */
    @Test(expected = DecoderException.class)
    public void testEncApRepPartEmptySequence() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x04 );

        stream.put( new byte[]
            {
                0x7B, 0x02,
                0x30, 0x00
        } );

        stream.flip();

        // Allocate a EncApRepPart Container
        Asn1Container encApRepPartContainer = new EncApRepPartContainer( stream );

        // Decode the EncApRepPart PDU
        kerberosDecoder.decode( stream, encApRepPartContainer );
        fail();
    }


    /**
     * Test the decoding of a EncApRepPart with empty ctime tag
     */
    @Test(expected = DecoderException.class)
    public void testEncApRepPartEmptyCTimeg() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x06 );

        stream.put( new byte[]
            {
                0x7B, 0x04,
                0x30, 0x02,
                ( byte ) 0xA0, 0x00
        } );

        stream.flip();

        // Allocate a EncApRepPart Container
        Asn1Container encApRepPartContainer = new EncApRepPartContainer( stream );

        // Decode the EncApRepPart PDU
        kerberosDecoder.decode( stream, encApRepPartContainer );
        fail();
    }


    /**
     * Test the decoding of a EncApRepPart with no CTime
     */
    @Test(expected = DecoderException.class)
    public void testEncApRepPartNoCtime() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x20 );

        stream.put( new byte[]
            {
                0x7B, 0x1E,
                0x30, 0x1C,
                ( byte ) 0xA1, 0x03, // cusec
                0x02,
                0x01,
                0x7F,
                ( byte ) 0xA2,
                0x0F, // subkey
                0x30,
                0x0D,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x06,
                0x04,
                0x04,
                'A',
                'B',
                'C',
                'D',
                ( byte ) 0xA3,
                0x04, // seq-number
                0x02,
                0x02,
                0x30,
                0x39,
        } );

        stream.flip();

        // Allocate a EncApRepPart Container
        Asn1Container encApRepPartContainer = new EncApRepPartContainer( stream );

        // Decode the EncApRepPart PDU
        kerberosDecoder.decode( stream, encApRepPartContainer );
        fail();
    }


    /**
     * Test the decoding of a EncApRepPart with no cusec
     */
    @Test(expected = DecoderException.class)
    public void testEncApRepPartNoCusec() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x2F );

        stream.put( new byte[]
            {
                0x7B, 0x2C,
                0x30, 0x2A,
                ( byte ) 0xA0, 0x11, // ctime 
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
                ( byte ) 0xA2,
                0x0F, // subkey
                0x30,
                0x0D,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x06,
                0x04,
                0x04,
                'A',
                'B',
                'C',
                'D',
                ( byte ) 0xA3,
                0x04, // seq-number
                0x02,
                0x02,
                0x30,
                0x39,
        } );

        stream.flip();

        // Allocate a EncApRepPart Container
        Asn1Container encApRepPartContainer = new EncApRepPartContainer( stream );

        // Decode the EncApRepPart PDU
        kerberosDecoder.decode( stream, encApRepPartContainer );
        fail();
    }


    /**
     * Test the decoding of a EncApRepPart message with no subKey
     */
    @Test
    public void testDecodeEncApRepPartNoSubKey() throws Exception
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x22 );

        stream.put( new byte[]
            {
                0x7B, 0x20,
                0x30, 0x1E,
                ( byte ) 0xA0, 0x11, // ctime 
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
                ( byte ) 0xA1,
                0x03, // cusec
                0x02,
                0x01,
                0x7F,
                ( byte ) 0xA3,
                0x04, // seq-number
                0x02,
                0x02,
                0x30,
                0x39,
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a EncApRepPart Container
        Asn1Container encApRepPartContainer = new EncApRepPartContainer( stream );

        // Decode the EncApRepPart PDU
        try
        {
            kerberosDecoder.decode( stream, encApRepPartContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        EncApRepPart encApRepPart = ( ( EncApRepPartContainer ) encApRepPartContainer ).getEncApRepPart();

        // Check the encoding
        int length = encApRepPart.computeLength();

        // Check the length
        assertEquals( 0x22, length );

        // Check the encoding
        ByteBuffer encodedPdu = ByteBuffer.allocate( length );

        try
        {
            encodedPdu = encApRepPart.encode( encodedPdu );

            // Check the length
            assertEquals( 0x22, encodedPdu.limit() );
            assertEquals( decodedPdu, Strings.dumpBytes( encodedPdu.array() ) );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a EncApRepPart message with no seq-number
     */
    @Test
    public void testDecodeEncApRepPartNoSeqNumber() throws Exception
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x2D );

        stream.put( new byte[]
            {
                0x7B, 0x2B,
                0x30, 0x29,
                ( byte ) 0xA0, 0x11, // ctime 
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
                ( byte ) 0xA1,
                0x03, // cusec
                0x02,
                0x01,
                0x7F,
                ( byte ) 0xA2,
                0x0F, // subkey
                0x30,
                0x0D,
                ( byte ) 0xA0,
                0x03,
                0x02,
                0x01,
                0x01,
                ( byte ) 0xA1,
                0x06,
                0x04,
                0x04,
                'A',
                'B',
                'C',
                'D',
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a EncApRepPart Container
        Asn1Container encApRepPartContainer = new EncApRepPartContainer( stream );

        // Decode the EncApRepPart PDU
        try
        {
            kerberosDecoder.decode( stream, encApRepPartContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        EncApRepPart encApRepPart = ( ( EncApRepPartContainer ) encApRepPartContainer ).getEncApRepPart();

        // Check the encoding
        int length = encApRepPart.computeLength();

        // Check the length
        assertEquals( 0x2D, length );

        // Check the encoding
        ByteBuffer encodedPdu = ByteBuffer.allocate( length );

        try
        {
            encodedPdu = encApRepPart.encode( encodedPdu );

            // Check the length
            assertEquals( 0x2D, encodedPdu.limit() );
            assertEquals( decodedPdu, Strings.dumpBytes( encodedPdu.array() ) );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }


    /**
     * Test the decoding of a EncApRepPart message with no subKey nor seq-number
     */
    @Test
    public void testDecodeEncApRepPartNoSubKeyNoSeqNumber() throws Exception
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x1C );

        stream.put( new byte[]
            {
                0x7B, 0x1A,
                0x30, 0x18,
                ( byte ) 0xA0, 0x11, // ctime 
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
                ( byte ) 0xA1,
                0x03, // cusec
                0x02,
                0x01,
                0x7F,
        } );

        String decodedPdu = Strings.dumpBytes( stream.array() );
        stream.flip();

        // Allocate a EncApRepPart Container
        Asn1Container encApRepPartContainer = new EncApRepPartContainer( stream );

        // Decode the EncApRepPart PDU
        try
        {
            kerberosDecoder.decode( stream, encApRepPartContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        EncApRepPart encApRepPart = ( ( EncApRepPartContainer ) encApRepPartContainer ).getEncApRepPart();

        // Check the encoding
        int length = encApRepPart.computeLength();

        // Check the length
        assertEquals( 0x1C, length );

        // Check the encoding
        ByteBuffer encodedPdu = ByteBuffer.allocate( length );

        try
        {
            encodedPdu = encApRepPart.encode( encodedPdu );

            // Check the length
            assertEquals( 0x1C, encodedPdu.limit() );
            assertEquals( decodedPdu, Strings.dumpBytes( encodedPdu.array() ) );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
}
