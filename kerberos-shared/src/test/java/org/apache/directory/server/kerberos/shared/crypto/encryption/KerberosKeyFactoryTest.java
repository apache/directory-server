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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import junit.framework.TestCase;


/**
 * Test cases for string-to-key functions for DES-, DES3-, AES-, and RC4-based
 * encryption types.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KerberosKeyFactoryTest extends TestCase
{
    /**
     * Tests that key derivation can be performed for a DES key.
     */
    public void testDesKerberosKey()
    {
        KerberosPrincipal principal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosKey key = new KerberosKey( principal, "secret".toCharArray(), "DES" );

        assertEquals( "DES key length", 8, key.getEncoded().length );
    }


    /**
     * Tests that key derivation can be performed for a Triple-DES key.
     */
    public void testTripleDesKerberosKey()
    {
        KerberosPrincipal principal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosKey key = new KerberosKey( principal, "secret".toCharArray(), "DESede" );

        assertEquals( "DESede key length", 24, key.getEncoded().length );
    }


    /**
     * Tests that key derivation can be performed for an RC4-HMAC key.
     */
    public void testArcFourHmacKerberosKey()
    {
        KerberosPrincipal principal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosKey key = new KerberosKey( principal, "secret".toCharArray(), "ArcFourHmac" );

        assertEquals( "ArcFourHmac key length", 16, key.getEncoded().length );
    }


    /**
     * Tests that key derivation can be performed for an AES-128 key.
     *
     * @throws Exception
     */
    public void testAes128KerberosKey() throws Exception
    {
        KerberosPrincipal principal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosKey key = new KerberosKey( principal, "secret".toCharArray(), "AES128" );

        assertEquals( "AES128 key length", 16, key.getEncoded().length );
    }


    /**
     * Tests that key derivation can be performed for an AES-256 key.  This test
     * will fail if "unlimited strength" policy is not installed.
     *
     * @throws Exception
     */
    public void testAes256KerberosKey() throws Exception
    {
        // KerberosPrincipal principal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        // KerberosKey key = new KerberosKey( principal, "secret".toCharArray(), "AES256" );
        //
        // assertEquals( "AES256 key length", 32, key.getEncoded().length );
        //
        // SecretKey skey = new SecretKeySpec( key.getEncoded(), "AES" );
        //
        // aesCipher( skey );
    }


    /**
     * Tests that key derivation can be performed by the factory for a list of cipher types.
     */
    public void testKerberosKeyFactory()
    {
        String principalName = "hnelson@EXAMPLE.COM";
        String passPhrase = "secret";

        List<KerberosKey> kerberosKeys = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase );

        Map<EncryptionType, KerberosKey> map = getMapForList( kerberosKeys );

        KerberosKey kerberosKey = map.get( EncryptionType.DES_CBC_MD5 );

        int keyType = kerberosKey.getKeyType();
        int keyLength = kerberosKey.getEncoded().length;
        byte[] keyBytes = kerberosKey.getEncoded();

        assertEquals( keyType, 3 );
        assertEquals( keyLength, 8 );
        byte[] expectedBytes = new byte[]
            { ( byte ) 0xF4, ( byte ) 0xA7, ( byte ) 0x13, ( byte ) 0x64, ( byte ) 0x8A, ( byte ) 0x61, ( byte ) 0xCE,
                ( byte ) 0x5B };
        assertTrue( Arrays.equals( expectedBytes, keyBytes ) );

        kerberosKey = map.get( EncryptionType.DES3_CBC_SHA1_KD );
        keyType = kerberosKey.getKeyType();
        keyLength = kerberosKey.getEncoded().length;
        keyBytes = kerberosKey.getEncoded();

        assertEquals( keyType, 16 );
        assertEquals( keyLength, 24 );
        expectedBytes = new byte[]
            { ( byte ) 0x57, ( byte ) 0x07, ( byte ) 0xCE, ( byte ) 0x29, ( byte ) 0x52, ( byte ) 0x92, ( byte ) 0x2C,
                ( byte ) 0x1C, ( byte ) 0x8C, ( byte ) 0xBF, ( byte ) 0x43, ( byte ) 0xC2, ( byte ) 0x3D,
                ( byte ) 0x8F, ( byte ) 0x8C, ( byte ) 0x5E, ( byte ) 0x9E, ( byte ) 0x8C, ( byte ) 0xF7,
                ( byte ) 0x5D, ( byte ) 0x3E, ( byte ) 0x4A, ( byte ) 0x5E, ( byte ) 0x25 };
        assertTrue( Arrays.equals( expectedBytes, keyBytes ) );

        kerberosKey = map.get( EncryptionType.RC4_HMAC );
        keyType = kerberosKey.getKeyType();
        keyLength = kerberosKey.getEncoded().length;
        keyBytes = kerberosKey.getEncoded();

        assertEquals( keyType, 23 );
        assertEquals( keyLength, 16 );
        expectedBytes = new byte[]
            { ( byte ) 0x87, ( byte ) 0x8D, ( byte ) 0x80, ( byte ) 0x14, ( byte ) 0x60, ( byte ) 0x6C, ( byte ) 0xDA,
                ( byte ) 0x29, ( byte ) 0x67, ( byte ) 0x7A, ( byte ) 0x44, ( byte ) 0xEF, ( byte ) 0xA1,
                ( byte ) 0x35, ( byte ) 0x3F, ( byte ) 0xC7 };
        assertTrue( Arrays.equals( expectedBytes, keyBytes ) );

        kerberosKey = map.get( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        keyType = kerberosKey.getKeyType();
        keyLength = kerberosKey.getEncoded().length;
        keyBytes = kerberosKey.getEncoded();

        assertEquals( keyType, 17 );
        assertEquals( keyLength, 16 );
        expectedBytes = new byte[]
            { ( byte ) 0xAD, ( byte ) 0x21, ( byte ) 0x4B, ( byte ) 0x38, ( byte ) 0xB6, ( byte ) 0x9D, ( byte ) 0xFC,
                ( byte ) 0xCA, ( byte ) 0xAC, ( byte ) 0xF1, ( byte ) 0x5F, ( byte ) 0x34, ( byte ) 0x6D,
                ( byte ) 0x41, ( byte ) 0x7B, ( byte ) 0x90 };
        assertTrue( Arrays.equals( expectedBytes, keyBytes ) );

        // kerberosKey = map.get( EncryptionType.AES256_CTS_HMAC_SHA1_96 );
        // keyType = kerberosKey.getKeyType();
        // keyLength = kerberosKey.getEncoded().length;
        // keyBytes = kerberosKey.getEncoded();
        //
        // assertEquals( keyType, 18 );
        // assertEquals( keyLength, 32 );
        // expectedBytes = new byte[]
        //     { ( byte ) 0x3D, ( byte ) 0x33, ( byte ) 0x31, ( byte ) 0x8F, ( byte ) 0xBE, ( byte ) 0x47, ( byte ) 0xE5,
        //         ( byte ) 0x2A, ( byte ) 0x21, ( byte ) 0x50, ( byte ) 0x77, ( byte ) 0xA4, ( byte ) 0x15,
        //         ( byte ) 0x58, ( byte ) 0xCA, ( byte ) 0xE7, ( byte ) 0x36, ( byte ) 0x50, ( byte ) 0x1F,
        //         ( byte ) 0xA7, ( byte ) 0xA4, ( byte ) 0x85, ( byte ) 0x82, ( byte ) 0x05, ( byte ) 0xF6,
        //         ( byte ) 0x8F, ( byte ) 0x67, ( byte ) 0xA2, ( byte ) 0xB5, ( byte ) 0xEA, ( byte ) 0x0E, ( byte ) 0xBF };
        // assertTrue( Arrays.equals( expectedBytes, keyBytes ) );
    }


    /**
     * Tests that key derivation can be performed by the factory for a specified cipher type.
     */
    public void testKerberosKeyFactoryOnlyDes()
    {
        String principalName = "hnelson@EXAMPLE.COM";
        String passPhrase = "secret";

        List<String> ciphers = new ArrayList<String>();
        ciphers.add( "DES" );

        List<KerberosKey> kerberosKeys = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase, ciphers );

        assertEquals( "List length", 1, kerberosKeys.size() );

        Map<EncryptionType, KerberosKey> map = getMapForList( kerberosKeys );

        KerberosKey kerberosKey = map.get( EncryptionType.DES_CBC_MD5 );

        int keyType = kerberosKey.getKeyType();
        int keyLength = kerberosKey.getEncoded().length;
        byte[] keyBytes = kerberosKey.getEncoded();

        assertEquals( keyType, 3 );
        assertEquals( keyLength, 8 );
        byte[] expectedBytes = new byte[]
            { ( byte ) 0xF4, ( byte ) 0xA7, ( byte ) 0x13, ( byte ) 0x64, ( byte ) 0x8A, ( byte ) 0x61, ( byte ) 0xCE,
                ( byte ) 0x5B };
        assertTrue( Arrays.equals( expectedBytes, keyBytes ) );
    }


    private Map<EncryptionType, KerberosKey> getMapForList( List<KerberosKey> kerberosKeys )
    {
        Map<EncryptionType, KerberosKey> map = new HashMap<EncryptionType, KerberosKey>();

        Iterator<KerberosKey> it = kerberosKeys.iterator();
        while ( it.hasNext() )
        {
            KerberosKey kerberosKey = it.next();
            EncryptionType type = EncryptionType.getTypeByOrdinal( kerberosKey.getKeyType() );
            map.put( type, kerberosKey );
        }

        return map;
    }
}
