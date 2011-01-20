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
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.EncKdcRepPartContainer;
import org.apache.directory.shared.kerberos.components.EncKdcRepPart;
import org.apache.directory.shared.util.Strings;
import org.junit.Test;

/**
 * Test cases for EncKrbPrivPart codec.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncKdcRepPartDecoderTest
{
    /**
     * Test an empty EncKdcRepPart
     * @throws Exception
     */
    @Test( expected=DecoderException.class)
    public void testDecodeEncKdcRepPartEmpty() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x02;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
            0x30, 0x00
        });
        
        stream.flip();

        // Allocate a EncKdcRepPart Container
        Asn1Container encKdcRepPartContainer = new EncKdcRepPartContainer( stream );

        // Decode the EncKdcRepPart PDU
        decoder.decode( stream, encKdcRepPartContainer );
        fail();
    }
    
    
    /**
     * Test an EncKdcRepPart with an empty key
     * @throws Exception
     */
    @Test( expected=DecoderException.class)
    public void testDecodeEncKdcRepPartEmptyKey() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x04;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
            0x30, 0x02,
              (byte)0xA0, 0x00
        });
        
        stream.flip();

        // Allocate a EncKdcRepPart Container
        Asn1Container encKdcRepPartContainer = new EncKdcRepPartContainer( stream );

        // Decode the EncKdcRepPart PDU
        decoder.decode( stream, encKdcRepPartContainer );
        fail();
    }
    
    
    /**
     * Test an EncKdcRepPart with a missing key
     * @throws Exception
     */
    @Test( expected=DecoderException.class)
    public void testDecodeEncKdcRepPartMissingKey() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x04;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
            0x30, 0x02,
              (byte)0xA1, 0x00 
        });
        
        stream.flip();

        // Allocate a EncKdcRepPart Container
        Asn1Container encKdcRepPartContainer = new EncKdcRepPartContainer( stream );

        // Decode the EncKdcRepPart PDU
        decoder.decode( stream, encKdcRepPartContainer );
        fail();
    }
    
    
    /**
     * Test an EncKdcRepPart with an empty last-req
     * @throws Exception
     */
    @Test( expected=DecoderException.class)
    public void testDecodeEncKdcRepPartEmptylastReq() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x17;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
            0x30, 0x15,
              (byte)0xA0, 0x11,
                0x30, 0x0F, 
                  (byte)0xA0, 0x03, 
                    0x02, 0x01, 0x11, 
                  (byte)0xA1, 0x08, 
                    0x04, 0x06, 
                      0x61, 0x62, 0x63, 0x64, 0x65, 0x66,
              (byte)0xA1, 0x00 
        });
        
        stream.flip();

        // Allocate a EncKdcRepPart Container
        Asn1Container encKdcRepPartContainer = new EncKdcRepPartContainer( stream );

        // Decode the EncKdcRepPart PDU
        decoder.decode( stream, encKdcRepPartContainer );
        fail();
    }
    
    
    /**
     * Test an EncKdcRepPart with an missing last-req
     * @throws Exception
     */
    @Test( expected=DecoderException.class)
    public void testDecodeEncKdcRepPartLastReqMissing() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x17;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
            0x30, 0x15,
              (byte)0xA0, 0x11,
                0x30, 0x0F, 
                  (byte)0xA0, 0x03, 
                    0x02, 0x01, 0x11, 
                  (byte)0xA1, 0x08, 
                    0x04, 0x06, 
                      0x61, 0x62, 0x63, 0x64, 0x65, 0x66,
              (byte)0xA2, 0x00 
        });
        
        stream.flip();

        // Allocate a EncKdcRepPart Container
        Asn1Container encKdcRepPartContainer = new EncKdcRepPartContainer( stream );

        // Decode the EncKdcRepPart PDU
        decoder.decode( stream, encKdcRepPartContainer );
        fail();
    }
    
    
    /**
     * Test an EncKdcRepPart with an empty nonce
     * @throws Exception
     */
    @Test( expected=DecoderException.class)
    public void testDecodeEncKdcRepPartEmptyNonce() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x4F;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
            0x30, 0x4D,
              (byte)0xA0, 0x11,
                0x30, 0x0F, 
                  (byte)0xA0, 0x03, 
                    0x02, 0x01, 0x11, 
                  (byte)0xA1, 0x08, 
                    0x04, 0x06, 
                      0x61, 0x62, 0x63, 0x64, 0x65, 0x66,
              (byte)0xA1, 0x36,
                0x30, 0x34, 
                  0x30, 0x18, 
                    (byte)0xA0, 0x03, 
                      0x02, 0x01, 0x02, 
                    (byte)0xA1, 0x11, 
                      0x18, 0x0F, 
                        0x32, 0x30, 0x31, 0x30, 0x31, 0x31, 0x32, 0x35, 0x31, 0x36, 0x31, 0x32, 0x35, 0x39, 0x5A,
                  0x30, 0x18,
                    (byte)0xA0, 0x03,
                      0x02, 0x01, 0x02,
                    (byte)0xA1, 0x11,
                      0x18, 0x0F, 
                        0x32, 0x30, 0x31, 0x30, 0x31, 0x31, 0x32, 0x35, 0x31, 0x36, 0x31, 0x32, 0x35, 0x39, 0x5A,
              (byte)0xA2, 0x00
        });
        
        stream.flip();

        // Allocate a EncKdcRepPart Container
        Asn1Container encKdcRepPartContainer = new EncKdcRepPartContainer( stream );

        // Decode the EncKdcRepPart PDU
        decoder.decode( stream, encKdcRepPartContainer );
        fail();
    }
    
    
    /**
     * Test an EncKdcRepPart with an missing nonce
     * @throws Exception
     */
    @Test( expected=DecoderException.class)
    public void testDecodeEncKdcRepPartNonceMissing() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x4F;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
             {
                 0x30, 0x4D,
                   (byte)0xA0, 0x11,
                     0x30, 0x0F, 
                       (byte)0xA0, 0x03, 
                         0x02, 0x01, 0x11, 
                       (byte)0xA1, 0x08, 
                         0x04, 0x06, 
                           0x61, 0x62, 0x63, 0x64, 0x65, 0x66,
                   (byte)0xA1, 0x36,
                     0x30, 0x34, 
                       0x30, 0x18, 
                         (byte)0xA0, 0x03, 
                           0x02, 0x01, 0x02, 
                         (byte)0xA1, 0x11, 
                           0x18, 0x0F, 
                             0x32, 0x30, 0x31, 0x30, 0x31, 0x31, 0x32, 0x35, 0x31, 0x36, 0x31, 0x32, 0x35, 0x39, 0x5A,
                       0x30, 0x18,
                         (byte)0xA0, 0x03,
                           0x02, 0x01, 0x02,
                         (byte)0xA1, 0x11,
                           0x18, 0x0F, 
                             0x32, 0x30, 0x31, 0x30, 0x31, 0x31, 0x32, 0x35, 0x31, 0x36, 0x31, 0x32, 0x35, 0x39, 0x5A,
                   (byte)0xA3, 0x00
             });
        
        stream.flip();

        // Allocate a EncKdcRepPart Container
        Asn1Container encKdcRepPartContainer = new EncKdcRepPartContainer( stream );

        // Decode the EncKdcRepPart PDU
        decoder.decode( stream, encKdcRepPartContainer );
        fail();
    }
    
    
    /**
     * Test an EncKdcRepPart with an empty key-expiration
     * @throws Exception
     */
    @Test( expected=DecoderException.class)
    public void testDecodeEncKdcRepPartEmptyKeyExpiration() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x54;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
        {
            0x30, 0x52,
              (byte)0xA0, 0x11,
                0x30, 0x0F, 
                  (byte)0xA0, 0x03, 
                    0x02, 0x01, 0x11, 
                  (byte)0xA1, 0x08, 
                    0x04, 0x06, 
                      0x61, 0x62, 0x63, 0x64, 0x65, 0x66,
              (byte)0xA1, 0x36,
                0x30, 0x34, 
                  0x30, 0x18, 
                    (byte)0xA0, 0x03, 
                      0x02, 0x01, 0x02, 
                    (byte)0xA1, 0x11, 
                      0x18, 0x0F, 
                        0x32, 0x30, 0x31, 0x30, 0x31, 0x31, 0x32, 0x35, 0x31, 0x36, 0x31, 0x32, 0x35, 0x39, 0x5A,
                  0x30, 0x18,
                    (byte)0xA0, 0x03,
                      0x02, 0x01, 0x02,
                    (byte)0xA1, 0x11,
                      0x18, 0x0F, 
                        0x32, 0x30, 0x31, 0x30, 0x31, 0x31, 0x32, 0x35, 0x31, 0x36, 0x31, 0x32, 0x35, 0x39, 0x5A,
              (byte)0xA2, 0x03,
                0x02, 0x01, 0x01,
              (byte)0xA3, 0x00
        });
        
        stream.flip();

        // Allocate a EncKdcRepPart Container
        Asn1Container encKdcRepPartContainer = new EncKdcRepPartContainer( stream );

        // Decode the EncKdcRepPart PDU
        decoder.decode( stream, encKdcRepPartContainer );
        fail();
    }
    
    
    /**
     * Test an EncKdcRepPart with no optional fields
     * @throws Exception
     */
    @Test
    public void testDecodeEncKdcRepPartNoOptionalFields() throws Exception
    {
        Asn1Decoder decoder = new Asn1Decoder();
        
        int streamLen = 0x9F;
        ByteBuffer stream = ByteBuffer.allocate( streamLen );
        stream.put( new byte[]
             {
                 0x30, (byte)0x81, (byte)0x9C,
                   (byte)0xA0, 0x11,
                     0x30, 0x0F, 
                       (byte)0xA0, 0x03, 
                         0x02, 0x01, 0x11, 
                       (byte)0xA1, 0x08, 
                         0x04, 0x06, 
                           0x61, 0x62, 0x63, 0x64, 0x65, 0x66,
                   (byte)0xA1, 0x36,
                     0x30, 0x34, 
                       0x30, 0x18, 
                         (byte)0xA0, 0x03, 
                           0x02, 0x01, 0x02, 
                         (byte)0xA1, 0x11, 
                           0x18, 0x0F, 
                             0x32, 0x30, 0x31, 0x30, 0x31, 0x31, 0x32, 0x35, 0x31, 0x36, 0x31, 0x32, 0x35, 0x39, 0x5A,
                       0x30, 0x18,
                         (byte)0xA0, 0x03,
                           0x02, 0x01, 0x02,
                         (byte)0xA1, 0x11,
                           0x18, 0x0F, 
                             0x32, 0x30, 0x31, 0x30, 0x31, 0x31, 0x32, 0x35, 0x31, 0x36, 0x31, 0x32, 0x35, 0x39, 0x5A,
                   (byte)0xA2, 0x03,
                     0x02, 0x01, 0x01,
                   (byte)0xA4, 0x07,
                     0x03, 0x05, 0x00, 0x40, 0x00, 0x00, 0x00,
                   (byte)0xA5, 0x11,
                     0x18, 0x0F, 
                       0x32, 0x30, 0x31, 0x30, 0x31, 0x31, 0x32, 0x35, 0x31, 0x36, 0x31, 0x32, 0x35, 0x39, 0x5A,
                   (byte)0xA7, 0x11, 
                     0x18, 0x0F, 
                       0x32, 0x30, 0x31, 0x30, 0x31, 0x31, 0x32, 0x35, 0x31, 0x36, 0x31, 0x32, 0x35, 0x39, 0x5A,
                   (byte)0xA9, 0x06,
                     0x1B, 0x04, 'a', 'b', 'c', 'd',
                   (byte)0xAA, 0x13,
                     0x30, 0x11, 
                       (byte)0xA0, 0x03, 
                         0x02, 0x01, 0x01, 
                       (byte)0xA1, 0x0A, 
                         0x30, 0x08,
                           0x1B, 0x06, 
                             0x61, 0x62, 0x63, 0x64, 0x65, 0x66,
             });
        
        String decoded = Strings.dumpBytes(stream.array());
        stream.flip();

        // Allocate a EncKdcRepPart Container
        EncKdcRepPartContainer encKdcRepPartContainer = new EncKdcRepPartContainer( stream );

        // Decode the EncKdcRepPart PDU
        try
        {
            decoder.decode( stream, encKdcRepPartContainer );
        }
        catch( Exception e )
        {
            e.printStackTrace();
            fail();
        }
        
        EncKdcRepPart encKdcRepPart = encKdcRepPartContainer.getEncKdcRepPart();

        int computedLen = encKdcRepPart.computeLength();
        
        assertEquals( streamLen, computedLen );
        
        try
        {
            ByteBuffer bb = ByteBuffer.allocate( computedLen );
            
            encKdcRepPart.encode( bb );
            
            String encoded = Strings.dumpBytes(bb.array());
            assertEquals( decoded, encoded );
        }
        catch( EncoderException e )
        {
            fail();
        }
    }
}
