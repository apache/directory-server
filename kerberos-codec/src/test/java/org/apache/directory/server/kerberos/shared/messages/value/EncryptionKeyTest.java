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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the EncryptionKey encoding and decoding
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class EncryptionKeyTest
{
    private static EncryptionKey encryptionA;
    private static EncryptionKey encryptionACopy;
    private static EncryptionKey encryptionB;
    private static EncryptionKey encryptionC;
    private static EncryptionKey encryptionD;

    private static final byte[] ENCRYPTION_VALUE_A = { 0x01, 0x02, 0x03 };
    private static final byte[] ENCRYPTION_VALUE_B = { 0x01, 0x02, 0x03 };
    private static final byte[] ENCRYPTION_VALUE_C = { 0x01, 0x02, 0x04 };

    
    /**
     * Initialize name instances
     */
    @BeforeClass
    public static void initNames() throws Exception
    {
        encryptionA = new EncryptionKey ( EncryptionType.AES128_CTS_HMAC_SHA1_96, ENCRYPTION_VALUE_A );
        encryptionACopy = new EncryptionKey ( EncryptionType.AES128_CTS_HMAC_SHA1_96, ENCRYPTION_VALUE_A );
        encryptionB = new EncryptionKey ( EncryptionType.AES128_CTS_HMAC_SHA1_96, ENCRYPTION_VALUE_B );
        encryptionC = new EncryptionKey ( EncryptionType.AES128_CTS_HMAC_SHA1_96, ENCRYPTION_VALUE_C );
        encryptionD = new EncryptionKey ( EncryptionType.AES256_CTS_HMAC_SHA1_96, ENCRYPTION_VALUE_A );

    }

    
    @Test
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


    @Test
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

    @Test
    public void testEncodingSlow() throws Exception
    {
        EncryptionKey ec = new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, new byte[]
            { 0x01, 0x02, 0x03 } );

        ByteBuffer buffer = ByteBuffer.allocate( ec.computeLength() );

        ec.encode( buffer );

        byte[] expectedResult = new byte[]
            { 
              0x30, 0x0c, 
                ( byte ) 0xA0, 0x03, 
                  0x02, 0x01, 0x11, 
                ( byte ) 0xA1, 0x05, 
                  0x04, 0x03, 0x01, 0x02, 0x03 
            };

        assertTrue( Arrays.equals( expectedResult, buffer.array() ) );
    }


    @Test
    public void testPerfSlow() throws EncoderException
    {
        EncryptionKey ec = new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, new byte[]
            { 0x01, 0x02, 0x03 } );
        ByteBuffer buffer = ByteBuffer.allocate( ec.computeLength() );
        ec.encode( buffer );

        // long t0 = System.currentTimeMillis();

        for ( int i = 0; i < 10000000; i++ )
        {
            buffer.flip();
            ec.encode( buffer );
        }

        // long t1 = System.currentTimeMillis();

        //System.out.println( "Delta = " + ( t1 - t0 ) );
    }


    @Test
    public void testPerfFast() throws EncoderException
    {
        EncryptionKey ec = new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, new byte[]
            { 0x01, 0x02, 0x03 } );
        ByteBuffer encoded = ByteBuffer.allocate( ec.computeLength() );
        ec.encode( encoded );

        // long t0 = System.currentTimeMillis();

        //for ( int i = 0; i < 40000000; i++ )
        {
            encoded = ByteBuffer.allocate( ec.computeLength() );

            ec.encode( encoded );
        }

        // long t1 = System.currentTimeMillis();

        //System.out.println( "Delta2 = " + ( t1 - t0 ) );
    }


    @Test
    public void testEqualsNull() throws Exception
    {
        assertFalse( encryptionA.equals( null ) );
    }


    @Test
    public void testEqualsReflexive() throws Exception
    {
        assertEquals( encryptionA, encryptionA );
    }


    @Test
    public void testHashCodeReflexive() throws Exception
    {
        assertEquals( encryptionA.hashCode(), encryptionA.hashCode() );
    }


    @Test
    public void testEqualsSymmetric() throws Exception
    {
        assertEquals( encryptionA, encryptionACopy );
        assertEquals( encryptionACopy, encryptionA );
    }


    @Test
    public void testHashCodeSymmetric() throws Exception
    {
        assertEquals( encryptionA.hashCode(), encryptionACopy.hashCode() );
        assertEquals( encryptionACopy.hashCode(), encryptionA.hashCode() );
    }


    @Test
    public void testEqualsTransitive() throws Exception
    {
        assertEquals( encryptionA, encryptionACopy );
        assertEquals( encryptionACopy, encryptionB );
        assertEquals( encryptionA, encryptionB );
    }


    @Test
    public void testHashCodeTransitive() throws Exception
    {
        assertEquals( encryptionA.hashCode(), encryptionACopy.hashCode() );
        assertEquals( encryptionACopy.hashCode(), encryptionB.hashCode() );
        assertEquals( encryptionA.hashCode(), encryptionB.hashCode() );
    }


    @Test
    public void testNotEqualDiffValue() throws Exception
    {
        assertFalse( encryptionA.equals( encryptionC ) );
        assertFalse( encryptionC.equals( encryptionA ) );
        assertFalse( encryptionA.equals( encryptionD ) );
        assertFalse( encryptionD.equals( encryptionA ) );
    }
}
