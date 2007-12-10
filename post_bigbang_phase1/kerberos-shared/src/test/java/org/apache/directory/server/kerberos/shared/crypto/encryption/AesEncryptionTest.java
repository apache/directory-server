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
package org.apache.directory.server.kerberos.shared.crypto.encryption;


import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import junit.framework.TestCase;


/**
 * Tests the use of AES for Kerberos, using test vectors from RFC 3962,
 * "Advanced Encryption Standard (AES) Encryption for Kerberos 5."
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AesEncryptionTest extends TestCase
{
    private byte[] keyBytes =
        { ( byte ) 0x63, ( byte ) 0x68, ( byte ) 0x69, ( byte ) 0x63, ( byte ) 0x6b, ( byte ) 0x65, ( byte ) 0x6e,
            ( byte ) 0x20, ( byte ) 0x74, ( byte ) 0x65, ( byte ) 0x72, ( byte ) 0x69, ( byte ) 0x79, ( byte ) 0x61,
            ( byte ) 0x6b, ( byte ) 0x69 };

    private SecretKey key = new SecretKeySpec( keyBytes, "AES" );

    private byte[] iv =
        { ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00,
            ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00,
            ( byte ) 0x00, ( byte ) 0x00, };

    private AlgorithmParameterSpec paramSpec = new IvParameterSpec( iv );


    /**
     * Tests the first test vector from RFC 3962,
     * "Advanced Encryption Standard (AES) Encryption for Kerberos 5."
     */
    public void testFirstAesVector()
    {
        if ( !VendorHelper.isCtsSupported() )
        {
            return;
        }

        byte[] input =
            { ( byte ) 0x49, ( byte ) 0x20, ( byte ) 0x77, ( byte ) 0x6f, ( byte ) 0x75, ( byte ) 0x6c, ( byte ) 0x64,
                ( byte ) 0x20, ( byte ) 0x6c, ( byte ) 0x69, ( byte ) 0x6b, ( byte ) 0x65, ( byte ) 0x20,
                ( byte ) 0x74, ( byte ) 0x68, ( byte ) 0x65, ( byte ) 0x20 };

        byte[] output =
            { ( byte ) 0xc6, ( byte ) 0x35, ( byte ) 0x35, ( byte ) 0x68, ( byte ) 0xf2, ( byte ) 0xbf, ( byte ) 0x8c,
                ( byte ) 0xb4, ( byte ) 0xd8, ( byte ) 0xa5, ( byte ) 0x80, ( byte ) 0x36, ( byte ) 0x2d,
                ( byte ) 0xa7, ( byte ) 0xff, ( byte ) 0x7f, ( byte ) 0x97 };

        byte[] result = aesCipher( key, input );

        assertEquals( "Length", input.length, result.length );
        assertTrue( Arrays.equals( output, result ) );
    }


    /**
     * Tests the last test vector from RFC 3962,
     * "Advanced Encryption Standard (AES) Encryption for Kerberos 5."
     */
    public void testLastAesVector()
    {
        if ( !VendorHelper.isCtsSupported() )
        {
            return;
        }

        byte[] input =
            { ( byte ) 0x49, ( byte ) 0x20, ( byte ) 0x77, ( byte ) 0x6f, ( byte ) 0x75, ( byte ) 0x6c, ( byte ) 0x64,
                ( byte ) 0x20, ( byte ) 0x6c, ( byte ) 0x69, ( byte ) 0x6b, ( byte ) 0x65, ( byte ) 0x20,
                ( byte ) 0x74, ( byte ) 0x68, ( byte ) 0x65, ( byte ) 0x20, ( byte ) 0x47, ( byte ) 0x65,
                ( byte ) 0x6e, ( byte ) 0x65, ( byte ) 0x72, ( byte ) 0x61, ( byte ) 0x6c, ( byte ) 0x20,
                ( byte ) 0x47, ( byte ) 0x61, ( byte ) 0x75, ( byte ) 0x27, ( byte ) 0x73, ( byte ) 0x20,
                ( byte ) 0x43, ( byte ) 0x68, ( byte ) 0x69, ( byte ) 0x63, ( byte ) 0x6b, ( byte ) 0x65,
                ( byte ) 0x6e, ( byte ) 0x2c, ( byte ) 0x20, ( byte ) 0x70, ( byte ) 0x6c, ( byte ) 0x65,
                ( byte ) 0x61, ( byte ) 0x73, ( byte ) 0x65, ( byte ) 0x2c, ( byte ) 0x20, ( byte ) 0x61,
                ( byte ) 0x6e, ( byte ) 0x64, ( byte ) 0x20, ( byte ) 0x77, ( byte ) 0x6f, ( byte ) 0x6e,
                ( byte ) 0x74, ( byte ) 0x6f, ( byte ) 0x6e, ( byte ) 0x20, ( byte ) 0x73, ( byte ) 0x6f,
                ( byte ) 0x75, ( byte ) 0x70, ( byte ) 0x2e };

        byte[] output =
            { ( byte ) 0x97, ( byte ) 0x68, ( byte ) 0x72, ( byte ) 0x68, ( byte ) 0xd6, ( byte ) 0xec, ( byte ) 0xcc,
                ( byte ) 0xc0, ( byte ) 0xc0, ( byte ) 0x7b, ( byte ) 0x25, ( byte ) 0xe2, ( byte ) 0x5e,
                ( byte ) 0xcf, ( byte ) 0xe5, ( byte ) 0x84, ( byte ) 0x39, ( byte ) 0x31, ( byte ) 0x25,
                ( byte ) 0x23, ( byte ) 0xa7, ( byte ) 0x86, ( byte ) 0x62, ( byte ) 0xd5, ( byte ) 0xbe,
                ( byte ) 0x7f, ( byte ) 0xcb, ( byte ) 0xcc, ( byte ) 0x98, ( byte ) 0xeb, ( byte ) 0xf5,
                ( byte ) 0xa8, ( byte ) 0x48, ( byte ) 0x07, ( byte ) 0xef, ( byte ) 0xe8, ( byte ) 0x36,
                ( byte ) 0xee, ( byte ) 0x89, ( byte ) 0xa5, ( byte ) 0x26, ( byte ) 0x73, ( byte ) 0x0d,
                ( byte ) 0xbc, ( byte ) 0x2f, ( byte ) 0x7b, ( byte ) 0xc8, ( byte ) 0x40, ( byte ) 0x9d,
                ( byte ) 0xad, ( byte ) 0x8b, ( byte ) 0xbb, ( byte ) 0x96, ( byte ) 0xc4, ( byte ) 0xcd,
                ( byte ) 0xc0, ( byte ) 0x3b, ( byte ) 0xc1, ( byte ) 0x03, ( byte ) 0xe1, ( byte ) 0xa1,
                ( byte ) 0x94, ( byte ) 0xbb, ( byte ) 0xd8 };

        byte[] result = aesCipher( key, input );

        assertEquals( "Length", input.length, result.length );
        assertTrue( Arrays.equals( output, result ) );
    }


    private byte[] aesCipher( SecretKey key, byte[] input )
    {
        try
        {
            Cipher ecipher = Cipher.getInstance( "AES/CTS/NoPadding" );
            ecipher.init( Cipher.ENCRYPT_MODE, key, paramSpec );
            return ecipher.doFinal( input );
        }
        catch ( GeneralSecurityException gse )
        {
            return new byte[]
                { 0x00 };
        }
    }
}
