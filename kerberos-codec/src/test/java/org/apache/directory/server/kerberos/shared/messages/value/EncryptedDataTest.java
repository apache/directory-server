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
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.util.Strings;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the EncryptedData encoding and decoding
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class EncryptedDataTest
{
    @Test
    public void testEncodingEncryptedData() throws Exception
    {
        EncryptedData ed = new EncryptedData( EncryptionType.AES128_CTS_HMAC_SHA1_96, 1, new byte[]
            { 0x01, 0x02, 0x03, 0x04 } );

        ByteBuffer encoded = ByteBuffer.allocate( ed.computeLength() );

        ed.encode( encoded );

        byte[] expectedResult = new byte[]
            { 
                0x30, 0x12, 
                  ( byte ) 0xA0, 0x03, 
                    0x02, 0x01, 0x11, 
                  ( byte ) 0xA1, 0x03, 
                    0x02, 0x01, 0x01, 
                  ( byte ) 0xA2, 0x06, 
                    0x04, 0x04, 0x01, 0x02, 0x03, 0x04 
            };

        assertEquals( Strings.dumpBytes(expectedResult), Strings.dumpBytes(encoded.array()) );
    }


    @Test
    public void testEncodingEncryptedDataNullCipher() throws Exception
    {
        EncryptedData ed = new EncryptedData( EncryptionType.AES128_CTS_HMAC_SHA1_96, 1, null );

        ByteBuffer encoded = ByteBuffer.allocate( ed.computeLength() );

        ed.encode( encoded );

        byte[] expectedResult = new byte[]
            { 
              0x30, 0x0E, 
                ( byte ) 0xA0, 0x03, 
                  0x02, 0x01, 0x11, 
                ( byte ) 0xA1, 0x03, 
                  0x02, 0x01, 0x01, 
                ( byte ) 0xA2, 0x02, 
                  0x04, 0x00 
            };

        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }


    @Test
    public void testEncodingEncryptedDataNoKvno() throws Exception
    {
        EncryptedData ed = new EncryptedData( EncryptionType.AES128_CTS_HMAC_SHA1_96, new byte[]
            { 0x01, 0x02, 0x03, 0x04 } );

        ByteBuffer encoded = ByteBuffer.allocate( ed.computeLength() );

        ed.encode( encoded );

        byte[] expectedResult = new byte[]
            { 
              0x30, 0x0D, 
                ( byte ) 0xA0, 0x03, 
                  0x02, 0x01, 0x11, 
                ( byte ) 0xA2, 0x06, 
                  0x04, 0x04, 0x01, 0x02, 0x03, 0x04 
            };

        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }


    @Test
    public void testEncodingEncryptedDataNoKvnoNullCipher() throws Exception
    {
        EncryptedData ed = new EncryptedData( EncryptionType.AES128_CTS_HMAC_SHA1_96, null );

        ByteBuffer encoded = ByteBuffer.allocate( ed.computeLength() );

        ed.encode( encoded );

        byte[] expectedResult = new byte[]
            { 
              0x30, 0x09, 
                ( byte ) 0xA0, 0x03, 
                  0x02, 0x01, 0x11, 
                ( byte ) 0xA2, 0x02, 
                  0x04, 0x00 
            };

        assertTrue( Arrays.equals( expectedResult, encoded.array() ) );
    }
}
