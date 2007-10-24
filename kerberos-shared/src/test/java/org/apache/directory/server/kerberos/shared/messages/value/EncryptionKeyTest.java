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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.io.encoder.EncryptionKeyEncoder;
import org.apache.directory.shared.asn1.codec.EncoderException;

import junit.framework.TestCase;


/**
 * Test the EncryptionKey encoding and decoding
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 542147 $, $Date: 2007-05-28 10:14:21 +0200 (Mon, 28 May 2007) $
 */
public class EncryptionKeyTest extends TestCase
{
    public void testEncodingFast() throws Exception
    {
        EncryptionKey ec = new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, new byte[]
            { 0x01, 0x02, 0x03 } );

        ByteBuffer encoded = ByteBuffer.allocate( ec.computeLength() );

        ec.encode( encoded );

        byte[] expectedResult = new byte[]
            { 
              0x30, 0x0c, 
                ( byte ) 0xA0, 0x03, 
                  0x02, 0x01, 0x11, 
                ( byte ) 0xA1, 0x05, 
                  0x04, 0x03, 0x01, 0x02, 0x03 
            };

        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }


    public void testEncodingNoStructureFast() throws Exception
    {
        EncryptionKey ec = new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, null );

        ByteBuffer encoded = ByteBuffer.allocate( ec.computeLength() );

        ec.encode( encoded );

        byte[] expectedResult = new byte[]
            { 
              0x30, 0x09, 
                ( byte ) 0xA0, 0x03, 
                  0x02, 0x01, 0x11, 
                ( byte ) 0xA1, 0x02, 
                  0x04, 0x00 
            };

        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }


    /*
     public void testEncodingNoStructureSlow() throws Exception
     {
     EncryptionKey ec = new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, null );
     
     byte[] encoded = EncryptionKeyEncoder.encode( ec );
     
     byte[] expectedResult = new byte[]
     {
     0x30, 0x09, 
     (byte)0xA0, 0x03,
     0x02, 0x01, 0x11,
     (byte)0xA1, 0x02,
     0x04, 0x00
     };

     assertTrue( Arrays.equals( expectedResult, encoded ) );
     }
     */

    public void testEncodingSlow() throws Exception
    {
        EncryptionKey ec = new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, new byte[]
            { 0x01, 0x02, 0x03 } );

        byte[] encoded = EncryptionKeyEncoder.encode( ec );

        byte[] expectedResult = new byte[]
            { 
              0x30, 0x0c, 
                ( byte ) 0xA0, 0x03, 
                  0x02, 0x01, 0x11, 
                ( byte ) 0xA1, 0x05, 
                  0x04, 0x03, 0x01, 0x02, 0x03 
            };

        assertTrue( Arrays.equals( expectedResult, encoded ) );
    }


    public void testPerfSlow() throws IOException
    {
        EncryptionKey ec = new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, new byte[]
            { 0x01, 0x02, 0x03 } );
        EncryptionKeyEncoder.encode( ec );

        long t0 = System.currentTimeMillis();

        //for ( int i = 0; i < 10000000; i++ )
        {
            EncryptionKeyEncoder.encode( ec );
        }

        long t1 = System.currentTimeMillis();

        System.out.println( "Delta = " + ( t1 - t0 ) );
    }


    public void testPerfFast() throws EncoderException
    {
        EncryptionKey ec = new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, new byte[]
            { 0x01, 0x02, 0x03 } );
        ByteBuffer encoded = ByteBuffer.allocate( ec.computeLength() );
        ec.encode( encoded );

        long t0 = System.currentTimeMillis();

        //for ( int i = 0; i < 40000000; i++ )
        {
            encoded = ByteBuffer.allocate( ec.computeLength() );

            ec.encode( encoded );
        }

        long t1 = System.currentTimeMillis();

        System.out.println( "Delta2 = " + ( t1 - t0 ) );
    }
}
