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

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.apache.directory.shared.asn1.DecoderException;
import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.kerberos.codec.apReq.ApReqContainer;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the decoder for a ApReq
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class ApReqDecoderTest
{
    /**
     * Test the decoding of a ApReq message
     */
    @Test
    public void testDecodeFullApReq() throws Exception
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x6C );
        
        stream.put( new byte[]
        {
          0x6E, 0x6A,
            0x30, 0x68,
              (byte)0xA0, 0x03,                 // pvno
                0x02, 0x01, 0x05,
              (byte)0xA1, 0x03,                 // msg-type
                0x02, 0x01, 0x0E,
              (byte)0xA2, 0x07,                 // APOptions
                0x03, 0x05, 0x00, 0x60, 0x00, 0x00, 0x00,
              (byte)0xA3, 0x40,                 // Ticket
                0x61, 0x3E, 
                  0x30, 0x3C, 
                    (byte)0xA0, 0x03, 
                      0x02, 0x01, 0x05, 
                    (byte)0xA1, 0x0D, 
                      0x1B, 0x0B, 
                        'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M', 
                    (byte)0xA2, 0x13, 
                      0x30, 0x11, 
                        (byte)0xA0, 0x03, 
                          0x02, 0x01, 0x01, 
                        (byte)0xA1, 0x0A, 
                          0x30, 0x08, 
                            0x1B, 0x06, 
                              'c', 'l', 'i', 'e', 'n', 't', 
                    (byte)0xA3, 0x11, 
                      0x30, 0x0F, 
                        (byte)0xA0, 0x03, 
                          0x02, 0x01, 0x11, 
                        (byte)0xA2, 0x08, 
                          0x04, 0x06, 
                            'a', 'b', 'c', 'd', 'e', 'f', 
              (byte)0xA4, 0x11,                 // Authenticator
                0x30, 0x0F, 
                  (byte)0xA0, 0x03, 
                    0x02, 0x01, 0x11, 
                  (byte)0xA2, 0x08, 
                    0x04, 0x06, 
                      'a', 'b', 'c', 'd', 'e', 'f', 
        });

        stream.flip();

        // Allocate a ApReq Container
        ApReqContainer apReqContainer = new ApReqContainer( stream );
        
        // Decode the ApReq PDU
        try
        {
            kerberosDecoder.decode( stream, apReqContainer );
        }
        catch ( DecoderException de )
        {
            fail( de.getMessage() );
        }

        ApReq apReq = apReqContainer.getApReq();
        
        assertTrue( apReq instanceof ApReq );
        
        // Check the encoding
        int length = apReq.computeLength();

        // Check the length
        assertEquals( 0x6C, length );
        
        // Check the encoding
        ByteBuffer encodedPdu = ByteBuffer.allocate( length );
        
        try
        {
            encodedPdu = apReq.encode( encodedPdu );
    
            // Check the length
            assertEquals( 0x6C, encodedPdu.limit() );
        }
        catch ( EncoderException ee )
        {
            fail();
        }
    }
    
    
    /**
     * Test the decoding of a ApReq message with a bad MsgType
     */
    @Test( expected = DecoderException.class)
    public void testDecodeFullApReqBadMsgType() throws Exception
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x193 );
        
        stream.put( new byte[]
        {
            0x6E, 0x6A,
            0x30, 0x68,
              (byte)0xA0, 0x03,                 // pvno
                0x02, 0x01, 0x05,
              (byte)0xA1, 0x03,                 // msg-type (wrong...)
                0x02, 0x01, 0x0D,
              (byte)0xA2, 0x07,                 // APOptions
                0x03, 0x05, 0x00, 0x60, 0x00, 0x00, 0x00,
              (byte)0xA3, 0x40,                 // Ticket
                0x61, 0x3E, 
                  0x30, 0x3C, 
                    (byte)0xA0, 0x03, 
                      0x02, 0x01, 0x05, 
                    (byte)0xA1, 0x0D, 
                      0x1B, 0x0B, 
                        'E', 'X', 'A', 'M', 'P', 'L', 'E', '.', 'C', 'O', 'M', 
                    (byte)0xA2, 0x13, 
                      0x30, 0x11, 
                        (byte)0xA0, 0x03, 
                          0x02, 0x01, 0x01, 
                        (byte)0xA1, 0x0A, 
                          0x30, 0x08, 
                            0x1B, 0x06, 
                              'c', 'l', 'i', 'e', 'n', 't', 
                    (byte)0xA3, 0x11, 
                      0x30, 0x0F, 
                        (byte)0xA0, 0x03, 
                          0x02, 0x01, 0x11, 
                        (byte)0xA2, 0x08, 
                          0x04, 0x06, 
                            'a', 'b', 'c', 'd', 'e', 'f', 
              (byte)0xA4, 0x11,                 // Authenticator
                0x30, 0x0F, 
                  (byte)0xA0, 0x03, 
                    0x02, 0x01, 0x11, 
                  (byte)0xA2, 0x08, 
                    0x04, 0x06, 
                      'a', 'b', 'c', 'd', 'e', 'f', 
        });

        stream.flip();

        // Allocate a ApReq Container
        ApReqContainer apReqContainer = new ApReqContainer( stream );
        
        // Decode the ApReq PDU
        kerberosDecoder.decode( stream, apReqContainer );
        fail();
    }
    
    
    /**
     * Test the decoding of a AP-REQ with nothing in it
     */
    @Test( expected = DecoderException.class)
    public void testApReqEmpty() throws DecoderException
    {
        Asn1Decoder kerberosDecoder = new Asn1Decoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x02 );
        
        stream.put( new byte[]
            { 0x6A, 0x00 } );

        stream.flip();

        // Allocate a AP-REQ Container
        Asn1Container apReqContainer = new ApReqContainer( stream );

        // Decode the AP-REQ PDU
        kerberosDecoder.decode( stream, apReqContainer );
        fail();
    }
}
