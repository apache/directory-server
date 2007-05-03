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


import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESKeySpec;

import junit.framework.TestCase;


/**
 * Test cases for random-to-key functions for DES-, DES3-, AES-, and RC4-based
 * encryption types.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RandomKeyFactoryTest extends TestCase
{
    /**
     * Tests that random DES keys can be generated.
     *
     * @throws Exception
     */
    public void testGenerateDesKey() throws Exception
    {
        KeyGenerator keygen = KeyGenerator.getInstance( "DES" );
        SecretKey key = keygen.generateKey();
        assertEquals( "DES key size", 8, key.getEncoded().length );
        assertTrue( DESKeySpec.isParityAdjusted( key.getEncoded(), 0 ) );
    }


    /**
     * Tests that random Triple-DES keys can be generated.
     *
     * @throws Exception
     */
    public void testGenerateTripleDesKey() throws Exception
    {
        KeyGenerator keygen = KeyGenerator.getInstance( "DESede" );
        SecretKey key = keygen.generateKey();
        assertEquals( "DESede key size", 24, key.getEncoded().length );
    }


    /**
     * Tests that random AES128 keys can be generated.
     *
     * @throws Exception
     */
    public void testGenerateAes128Key() throws Exception
    {
        KeyGenerator keygen = KeyGenerator.getInstance( "AES" );
        keygen.init( 128 );
        SecretKey key = keygen.generateKey();
        assertEquals( "AES key size", 16, key.getEncoded().length );
    }


    /**
     * Tests that random AES256 keys can be generated.
     *
     * @throws Exception
     */
    public void testGenerateAes256Key() throws Exception
    {
        // KeyGenerator keygen = KeyGenerator.getInstance( "AES" );
        // keygen.init( 256 );
        // SecretKey key = keygen.generateKey();
        // assertEquals( "AES key size", 32, key.getEncoded().length );
    }


    /**
     * Tests that random ARCFOUR keys can be generated.
     *
     * @throws Exception
     */
    public void testGenerateArcFourKey() throws Exception
    {
        KeyGenerator keygen = KeyGenerator.getInstance( "ARCFOUR" );
        SecretKey key = keygen.generateKey();
        assertEquals( "ARCFOUR key size", 16, key.getEncoded().length );
    }


    /**
     * Tests that random RC4 keys can be generated.
     *
     * @throws Exception
     */
    public void testGenerateRc4Key() throws Exception
    {
        KeyGenerator keygen = KeyGenerator.getInstance( "RC4" );
        SecretKey key = keygen.generateKey();
        assertEquals( "RC4 key size", 16, key.getEncoded().length );
    }
}
