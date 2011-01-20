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
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.kerberos.codec.paEncTsEnc.PaEncTsEncContainer;
import org.apache.directory.shared.kerberos.components.PaEncTsEnc;
import org.apache.directory.shared.util.Strings;
import org.junit.Test;

/**
 * Test cases for PaEncTsEnc codec.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PaEncTsEncDecoderTest
{
    @Test
    public void testDecodeFullPaEncTsEnc()
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();
        
        ByteBuffer stream = ByteBuffer.allocate( 0x1A );

        stream.put( new byte[]
            { 
                0x30, 0x18,
                  (byte)0xA0, 0x11,                 // PaTimestamp
                    0x18, 0x0F, 
                      '2', '0', '1', '0', '1', '0', '1', '0', '2', '3', '4', '5', '4', '5', 'Z',
                  (byte)0xA1, 0x03,                 // PaUsec
                    0x02, 0x01, 0x01
            } );
        
        String decodedPdu = Strings.dumpBytes(stream.array());
        stream.flip();

        PaEncTsEncContainer paEncTsEncContainer = new PaEncTsEncContainer();
        
        try
        {
            krbDecoder.decode( stream, paEncTsEncContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        PaEncTsEnc paEncTsEnc = paEncTsEncContainer.getPaEncTsEnc();
        
        assertEquals( "20101010234545Z", paEncTsEnc.getPaTimestamp().toString() );
        assertEquals( 1, paEncTsEnc.getPausec() );
        
        ByteBuffer bb = ByteBuffer.allocate( paEncTsEnc.computeLength() );
        
        try
        {
            bb = paEncTsEnc.encode( bb );
    
            // Check the length
            assertEquals( 0x1A, bb.limit() );
    
            String encodedPdu = Strings.dumpBytes(bb.array());
    
            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }

    
    @Test( expected = DecoderException.class )
    public void testDecodePaEncTsEncWithEmptySeq() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();
        
        ByteBuffer stream = ByteBuffer.allocate( 2 );

        stream.put( new byte[]
            { 
                0x30, 0x0
            } );
        
        stream.flip();

        PaEncTsEncContainer paEncTsEncContainer = new PaEncTsEncContainer();

        krbDecoder.decode( stream, paEncTsEncContainer );
        fail();
    }

    
    @Test( expected = DecoderException.class )
    public void testDecodePaEncTsEncEmptyPaTimestamp() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();
        
        ByteBuffer stream = ByteBuffer.allocate( 4 );

        stream.put( new byte[]
            { 
                0x30, 0x02,
                  (byte)0xA0, 0x00
            } );
        
        stream.flip();

        PaEncTsEncContainer paEncTsEncContainer = new PaEncTsEncContainer();

        krbDecoder.decode( stream, paEncTsEncContainer );
        fail();
    }

    
    @Test( expected = DecoderException.class )
    public void testDecodeAdAndOrNullPaTimestamp() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();
        
        ByteBuffer stream = ByteBuffer.allocate( 6 );

        stream.put( new byte[]
            { 
                0x30, 0x04,
                  (byte)0xA0, 0x02,
                    0x18, 0x00
            } );
        
        stream.flip();

        PaEncTsEncContainer paEncTsEncContainer = new PaEncTsEncContainer();

        krbDecoder.decode( stream, paEncTsEncContainer );
        fail();
    }

    
    @Test( expected = DecoderException.class )
    public void testDecodeAdAndOrNoPaTimestamp() throws DecoderException
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();
        
        ByteBuffer stream = ByteBuffer.allocate( 0x07 );

        stream.put( new byte[]
             { 
                 0x30, 0x05,
                   (byte)0xA1, 0x03,                 // PaUsec
                     0x02, 0x01, 0x01
             } );
         
        stream.flip();

        PaEncTsEncContainer paEncTsEncContainer = new PaEncTsEncContainer();

        krbDecoder.decode( stream, paEncTsEncContainer );
        fail();
    }


    @Test
    public void testDecodePaEncTsEncNoPaUsec()
    {
        Asn1Decoder krbDecoder = new Asn1Decoder();
        
        ByteBuffer stream = ByteBuffer.allocate( 0x15 );

        stream.put( new byte[]
            { 
                0x30, 0x13,
                  (byte)0xA0, 0x11,                 // PaTimestamp
                    0x18, 0x0F, 
                      '2', '0', '1', '0', '1', '0', '1', '0', '2', '3', '4', '5', '4', '5', 'Z',
            } );
        
        String decodedPdu = Strings.dumpBytes(stream.array());
        stream.flip();

        PaEncTsEncContainer paEncTsEncContainer = new PaEncTsEncContainer();
        
        try
        {
            krbDecoder.decode( stream, paEncTsEncContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        PaEncTsEnc paEncTsEnc = paEncTsEncContainer.getPaEncTsEnc();
        
        assertEquals( "20101010234545Z", paEncTsEnc.getPaTimestamp().toString() );
        assertEquals( -1, paEncTsEnc.getPausec() );
        
        ByteBuffer bb = ByteBuffer.allocate( paEncTsEnc.computeLength() );
        
        try
        {
            bb = paEncTsEnc.encode( bb );
    
            // Check the length
            assertEquals( 0x15, bb.limit() );
    
            String encodedPdu = Strings.dumpBytes(bb.array());
    
            assertEquals( encodedPdu, decodedPdu );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
}
