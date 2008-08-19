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


import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import junit.framework.TestCase;


/**
 * Test cases for the encryption types used by Kerberos "5.2" per RFC 4120,
 * "The Kerberos Network Authentication Service (V5)."
 * 
 * We MUST support:
 * Encryption: AES256-CTS-HMAC-SHA1-96 [RFC3962]
 * Checksums: HMAC-SHA1-96-AES256 [RFC3962]
 * 
 * We SHOULD support:
 * Encryption: AES128-CTS-HMAC-SHA1-96, DES-CBC-MD5, DES3-CBC-SHA1-KD
 * Checksums: DES-MD5, HMAC-SHA1-DES3-KD, HMAC-SHA1-96-AES128
 * 
 * Also important for interoperability is:
 * ArcFour with HMAC/md5, DES-CBC-CRC
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KeyTypeTest extends TestCase
{
    /**
     * Tests that the cipher types used by Kerberos exist, namely
     * DES, DESede, RC4, and AES.
     */
    public void testKeyTypes()
    {
        String[] names = getCryptoImpls( "Cipher" );
        List ciphers = Arrays.asList( names );

        assertTrue( ciphers.contains( "DES" ) );
        assertTrue( ciphers.contains( "DESede" ) );
        assertTrue( ciphers.contains( "TripleDES" ) );
        assertTrue( ciphers.contains( "ARCFOUR" ) );
        assertTrue( ciphers.contains( "RC4" ) );
        assertTrue( ciphers.contains( "AES" ) );
    }


    /**
     * Tests that the message digest types used by Kerberos exist, namely
     * SHA1 and MD5.
     */
    public void testMessageDigestTypes()
    {
        String[] names = getCryptoImpls( "MessageDigest" );
        List ciphers = Arrays.asList( names );

        assertTrue( ciphers.contains( "MD5" ) );
        assertTrue( ciphers.contains( "SHA1" ) );
    }


    /**
     * Tests that the MAC types used by Kerberos exist, namely
     * HmacMD5 and HmacSHA1.
     */
    public void testMacTypes()
    {
        String[] names = getCryptoImpls( "Mac" );
        List ciphers = Arrays.asList( names );

        assertTrue( ciphers.contains( "HmacMD5" ) );
        assertTrue( ciphers.contains( "HmacSHA1" ) );
    }


    /**
     * Tests that DES keys can be generated from bytes.
     *
     * @throws Exception
     */
    public void generateDes() throws Exception
    {
        byte[] desKeyData =
            { ( byte ) 0x01, ( byte ) 0x02, ( byte ) 0x03, ( byte ) 0x04, ( byte ) 0x05, ( byte ) 0x06, ( byte ) 0x07,
                ( byte ) 0x08 };
        DESKeySpec desKeySpec = new DESKeySpec( desKeyData );
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( "DES" );
        SecretKey desKey = keyFactory.generateSecret( desKeySpec );
        assertEquals( "DES key size", 8, desKey.getEncoded().length );
        assertTrue( DESKeySpec.isParityAdjusted( desKey.getEncoded(), 0 ) );
    }


    /**
     * Tests that a CBC-mode DES cipher can be initialized.
     *
     * @throws Exception
     */
    public void testDesCipher() throws Exception
    {
        KeyGenerator keygen = KeyGenerator.getInstance( "DES" );
        SecretKey desKey = keygen.generateKey();

        Cipher ecipher = Cipher.getInstance( "DES/CBC/NoPadding" );
        ecipher.init( Cipher.ENCRYPT_MODE, desKey );
        assertEquals( "Block size", 8, ecipher.getBlockSize() );
    }


    /**
     * Tests that a CBC-mode Triple-DES cipher can be initialized.
     *
     * @throws Exception
     */
    public void testTripleDesCipher() throws Exception
    {
        KeyGenerator keygen = KeyGenerator.getInstance( "DESede" );
        SecretKey desKey = keygen.generateKey();

        Cipher ecipher = Cipher.getInstance( "DESede/CBC/NoPadding" );
        ecipher.init( Cipher.ENCRYPT_MODE, desKey );
        assertEquals( "Block size", 8, ecipher.getBlockSize() );
    }


    /**
     * Tests that a CBC-mode Triple-DES cipher can be initialized.
     *
     * @throws Exception
     */
    public void testArcFourCipher() throws Exception
    {
        KeyGenerator keygen = KeyGenerator.getInstance( "ARCFOUR" );
        SecretKey desKey = keygen.generateKey();

        Cipher ecipher = Cipher.getInstance( "ARCFOUR" );
        ecipher.init( Cipher.ENCRYPT_MODE, desKey );
        assertEquals( "Block size", 0, ecipher.getBlockSize() );
    }


    /**
     * Tests that a CTS-mode AES cipher can be initialized
     * with an AES-128 key.
     *
     * @throws Exception
     */
    public void testAes128Cipher() throws Exception
    {
        KeyGenerator keyGenerator = KeyGenerator.getInstance( "AES" );
        keyGenerator.init( 128 );

        SecretKey key = keyGenerator.generateKey();

        try
        {
            Cipher ecipher = Cipher.getInstance( "AES/CTS/NoPadding" );
            ecipher.init( Cipher.ENCRYPT_MODE, key );
            assertEquals( "Block size", 16, ecipher.getBlockSize() );
        }
        catch ( NoSuchAlgorithmException nsae )
        {
            // Without CTS mode this will throw an Exception.
        }
    }


    /**
     * Tests that a CTS-mode AES cipher can be initialized
     * with an AES-256 key.
     *
     * @throws Exception
     */
    public void testAes256Cipher() throws Exception
    {
        KeyGenerator keyGenerator = KeyGenerator.getInstance( "AES" );
        keyGenerator.init( 256 );

        SecretKey key = keyGenerator.generateKey();

        try
        {
            Cipher ecipher = Cipher.getInstance( "AES/CTS/NoPadding" );
            ecipher.init( Cipher.ENCRYPT_MODE, key );
            assertEquals( "Block size", 16, ecipher.getBlockSize() );
        }
        catch ( InvalidKeyException ike )
        {
            // Without unlimited-strength crypto this will throw an exception.
        }
        catch ( NoSuchAlgorithmException nsae )
        {
            // Without CTS mode this will throw an Exception.
        }
    }


    /**
     * Tests the generation of an HMAC-MD5 MAC.
     * 
     * @throws Exception
     */
    public void testGenerateHmacMd5() throws Exception
    {
        KeyGenerator kg = KeyGenerator.getInstance( "HmacMD5" );
        SecretKey sk = kg.generateKey();

        Mac mac = Mac.getInstance( "HmacMD5" );
        mac.init( sk );
        byte[] result = mac.doFinal( "Hello world!".getBytes() );

        assertEquals( "HmacMD5 size", 16, result.length );
    }


    /**
     * Tests the generation of an HMAC-SHA1 MAC.
     * 
     * @throws Exception
     */
    public void testGenerateHmacSha1() throws Exception
    {
        KeyGenerator kg = KeyGenerator.getInstance( "HmacSHA1" );
        SecretKey sk = kg.generateKey();

        Mac mac = Mac.getInstance( "HmacSHA1" );
        mac.init( sk );
        byte[] result = mac.doFinal( "Hi There".getBytes() );

        assertEquals( "HmacSHA1 size", 20, result.length );
    }


    /**
     * This method returns the available implementations for a service type.
     * 
     * @param serviceType The type of the service.
     * @return Array of the service types as Strings.
     */
    private static String[] getCryptoImpls( String serviceType )
    {
        Set<String> result = new HashSet<String>();

        Provider[] providers = Security.getProviders();
        for ( int i = 0; i < providers.length; i++ )
        {
            // Get services provided by each provider
            Set keys = providers[i].keySet();
            for ( Iterator it = keys.iterator(); it.hasNext(); )
            {
                String key = ( String ) it.next();
                key = key.split( " " )[0];

                if ( key.startsWith( serviceType + "." ) )
                {
                    result.add( key.substring( serviceType.length() + 1 ) );
                }
                else if ( key.startsWith( "Alg.Alias." + serviceType + "." ) )
                {
                    // This is an alias
                    result.add( key.substring( serviceType.length() + 11 ) );
                }
            }
        }

        return result.toArray( new String[result.size()] );
    }
}
