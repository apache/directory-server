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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESKeySpec;

import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.junit.jupiter.api.Test;


/**
 * Test cases for random-to-key functions for DES-, DES3-, AES-, and RC4-based
 * encryption types.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RandomKeyFactoryTest
{
    /**
     * Tests that random DES keys can be generated.
     *
     * @throws Exception
     */
    @Test
    public void testGenerateDesKey() throws Exception
    {
        KeyGenerator keygen = KeyGenerator.getInstance( "DES" );
        SecretKey key = keygen.generateKey();
        assertEquals( 8, key.getEncoded().length, "DES key size" );
        assertTrue( DESKeySpec.isParityAdjusted( key.getEncoded(), 0 ) );
    }


    /**
     * Tests that random Triple-DES keys can be generated.
     *
     * @throws Exception
     */
    @Test
    public void testGenerateTripleDesKey() throws Exception
    {
        KeyGenerator keygen = KeyGenerator.getInstance( "DESede" );
        SecretKey key = keygen.generateKey();
        assertEquals( 24, key.getEncoded().length, "DESede key size" );
    }


    /**
     * Tests that random AES128 keys can be generated.
     *
     * @throws Exception
     */
    @Test
    public void testGenerateAes128Key() throws Exception
    {
        KeyGenerator keygen = KeyGenerator.getInstance( "AES" );
        keygen.init( 128 );
        SecretKey key = keygen.generateKey();
        assertEquals( 16, key.getEncoded().length, "AES key size" );
    }


    /**
     * Tests that random AES256 keys can be generated.
     *
     * @throws Exception
     */
    @Test
    public void testGenerateAes256Key() throws Exception
    {
        KeyGenerator keygen = KeyGenerator.getInstance( "AES" );
        keygen.init( 256 );
        SecretKey key = keygen.generateKey();
        assertEquals( 32, key.getEncoded().length, "AES key size" );
    }


    /**
     * Tests that random ARCFOUR keys can be generated.
     *
     * @throws Exception
     */
    @Test
    public void testGenerateArcFourKey() throws Exception
    {
        if ( !VendorHelper.isArcFourHmacSupported() )
        {
            return;
        }

        KeyGenerator keygen = KeyGenerator.getInstance( "ARCFOUR" );
        SecretKey key = keygen.generateKey();
        assertEquals( 16, key.getEncoded().length, "ARCFOUR key size" );
    }


    /**
     * Tests that random RC4 keys can be generated.
     *
     * @throws Exception
     */
    @Test
    public void testGenerateRc4Key() throws Exception
    {
        if ( !VendorHelper.isArcFourHmacSupported() )
        {
            return;
        }

        KeyGenerator keygen = KeyGenerator.getInstance( "RC4" );
        SecretKey key = keygen.generateKey();
        assertEquals( 16, key.getEncoded().length, "RC4 key size" );
    }


    /**
     * Tests that random key generation can be performed by the factory for multiple cipher types.
     * 
     * @throws Exception
     */
    @Test
    public void testRandomKeyFactory() throws Exception
    {
        Map<EncryptionType, EncryptionKey> map = RandomKeyFactory.getRandomKeys();

        EncryptionKey kerberosKey = map.get( EncryptionType.DES_CBC_MD5 );

        EncryptionType keyType = kerberosKey.getKeyType();
        int keyLength = kerberosKey.getKeyValue().length;

        assertEquals( EncryptionType.DES_CBC_MD5, keyType );
        assertEquals( 8, keyLength );

        kerberosKey = map.get( EncryptionType.DES3_CBC_SHA1_KD );
        keyType = kerberosKey.getKeyType();
        keyLength = kerberosKey.getKeyValue().length;

        assertEquals( EncryptionType.DES3_CBC_SHA1_KD, keyType );
        assertEquals( 24, keyLength );

        kerberosKey = map.get( EncryptionType.RC4_HMAC );
        keyType = kerberosKey.getKeyType();
        keyLength = kerberosKey.getKeyValue().length;

        if ( VendorHelper.isArcFourHmacSupported() )
        {
            assertEquals( EncryptionType.RC4_HMAC, keyType );
            assertEquals( 16, keyLength );
        }

        kerberosKey = map.get( EncryptionType.AES128_CTS_HMAC_SHA1_96 );
        keyType = kerberosKey.getKeyType();
        keyLength = kerberosKey.getKeyValue().length;

        assertEquals( EncryptionType.AES128_CTS_HMAC_SHA1_96, keyType );
        assertEquals( 16, keyLength );

        kerberosKey = map.get( EncryptionType.AES256_CTS_HMAC_SHA1_96 );
        keyType = kerberosKey.getKeyType();
        keyLength = kerberosKey.getKeyValue().length;

        assertEquals( EncryptionType.AES256_CTS_HMAC_SHA1_96, keyType );
        assertEquals( 32, keyLength );
    }


    /**
     * Tests that random key generation can be performed by the factory for a specified cipher type.
     * 
     * @throws Exception
     */
    @Test
    public void testRandomKeyFactoryOnlyDes() throws Exception
    {
        Set<EncryptionType> encryptionTypes = new HashSet<EncryptionType>();
        encryptionTypes.add( EncryptionType.DES_CBC_MD5 );

        Map<EncryptionType, EncryptionKey> map = RandomKeyFactory.getRandomKeys( encryptionTypes );

        assertEquals( 1, map.values().size(), "List length" );

        EncryptionKey kerberosKey = map.get( EncryptionType.DES_CBC_MD5 );

        EncryptionType keyType = kerberosKey.getKeyType();
        int keyLength = kerberosKey.getKeyValue().length;

        assertEquals( EncryptionType.DES_CBC_MD5, keyType);
        assertEquals( 8, keyLength );
    }
}
