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


import java.util.Arrays;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import junit.framework.TestCase;


/**
 * Tests the use of Triple DES for Kerberos, using test vectors from RFC 3961,
 * "Encryption and Checksum Specifications for Kerberos 5."
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Des3CbcSha1KdEncryptionTest extends TestCase
{
    private Des3CbcSha1KdEncryption keyDerivationFunction = new Des3CbcSha1KdEncryption();


    /**
     * Tests setting parity as defined in RFC 3961.
     */
    public void testParity()
    {
        byte[] test =
            { ( byte ) 0x93, ( byte ) 0x50, ( byte ) 0x79, ( byte ) 0xd1, ( byte ) 0x44, ( byte ) 0x90, ( byte ) 0xa7 };
        byte[] expected =
            { ( byte ) 0x92, ( byte ) 0x51, ( byte ) 0x79, ( byte ) 0xd0, ( byte ) 0x45, ( byte ) 0x91, ( byte ) 0xa7,
                ( byte ) 0x9b };

        byte[] result = keyDerivationFunction.setParity( test );

        assertTrue( Arrays.equals( expected, result ) );
    }


    /**
     * Tests 'deriveRandom' and 'randomToKey' functions. 
     */
    public void testDerivedKey()
    {
        byte[] key =
            { ( byte ) 0xdc, ( byte ) 0xe0, ( byte ) 0x6b, ( byte ) 0x1f, ( byte ) 0x64, ( byte ) 0xc8, ( byte ) 0x57,
                ( byte ) 0xa1, ( byte ) 0x1c, ( byte ) 0x3d, ( byte ) 0xb5, ( byte ) 0x7c, ( byte ) 0x51,
                ( byte ) 0x89, ( byte ) 0x9b, ( byte ) 0x2c, ( byte ) 0xc1, ( byte ) 0x79, ( byte ) 0x10,
                ( byte ) 0x08, ( byte ) 0xce, ( byte ) 0x97, ( byte ) 0x3b, ( byte ) 0x92 };

        byte[] usage =
            { ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x01, ( byte ) 0x55 };

        byte[] DR =
            { ( byte ) 0x93, ( byte ) 0x50, ( byte ) 0x79, ( byte ) 0xd1, ( byte ) 0x44, ( byte ) 0x90, ( byte ) 0xa7,
                ( byte ) 0x5c, ( byte ) 0x30, ( byte ) 0x93, ( byte ) 0xc4, ( byte ) 0xa6, ( byte ) 0xe8,
                ( byte ) 0xc3, ( byte ) 0xb0, ( byte ) 0x49, ( byte ) 0xc7, ( byte ) 0x1e, ( byte ) 0x6e,
                ( byte ) 0xe7, ( byte ) 0x05 };

        byte[] DK =
            { ( byte ) 0x92, ( byte ) 0x51, ( byte ) 0x79, ( byte ) 0xd0, ( byte ) 0x45, ( byte ) 0x91, ( byte ) 0xa7,
                ( byte ) 0x9b, ( byte ) 0x5d, ( byte ) 0x31, ( byte ) 0x92, ( byte ) 0xc4, ( byte ) 0xa7,
                ( byte ) 0xe9, ( byte ) 0xc2, ( byte ) 0x89, ( byte ) 0xb0, ( byte ) 0x49, ( byte ) 0xc7,
                ( byte ) 0x1f, ( byte ) 0x6e, ( byte ) 0xe6, ( byte ) 0x04, ( byte ) 0xcd };

        byte[] result = keyDerivationFunction.deriveRandom( key, usage, 64, 168 );
        assertTrue( Arrays.equals( DR, result ) );

        result = keyDerivationFunction.randomToKey( result );
        assertTrue( Arrays.equals( DK, result ) );
    }


    /**
     * Tests 'deriveRandom' and 'randomToKey' functions. 
     */
    public void testDerivedKey2()
    {
        byte[] key =
            { ( byte ) 0x5e, ( byte ) 0x13, ( byte ) 0xd3, ( byte ) 0x1c, ( byte ) 0x70, ( byte ) 0xef, ( byte ) 0x76,
                ( byte ) 0x57, ( byte ) 0x46, ( byte ) 0x57, ( byte ) 0x85, ( byte ) 0x31, ( byte ) 0xcb,
                ( byte ) 0x51, ( byte ) 0xc1, ( byte ) 0x5b, ( byte ) 0xf1, ( byte ) 0x1c, ( byte ) 0xa8,
                ( byte ) 0x2c, ( byte ) 0x97, ( byte ) 0xce, ( byte ) 0xe9, ( byte ) 0xf2 };

        byte[] usage =
            { ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x01, ( byte ) 0xaa };

        byte[] DR =
            { ( byte ) 0x9f, ( byte ) 0x58, ( byte ) 0xe5, ( byte ) 0xa0, ( byte ) 0x47, ( byte ) 0xd8, ( byte ) 0x94,
                ( byte ) 0x10, ( byte ) 0x1c, ( byte ) 0x46, ( byte ) 0x98, ( byte ) 0x45, ( byte ) 0xd6,
                ( byte ) 0x7a, ( byte ) 0xe3, ( byte ) 0xc5, ( byte ) 0x24, ( byte ) 0x9e, ( byte ) 0xd8,
                ( byte ) 0x12, ( byte ) 0xf2 };

        byte[] DK =
            { ( byte ) 0x9e, ( byte ) 0x58, ( byte ) 0xe5, ( byte ) 0xa1, ( byte ) 0x46, ( byte ) 0xd9, ( byte ) 0x94,
                ( byte ) 0x2a, ( byte ) 0x10, ( byte ) 0x1c, ( byte ) 0x46, ( byte ) 0x98, ( byte ) 0x45,
                ( byte ) 0xd6, ( byte ) 0x7a, ( byte ) 0x20, ( byte ) 0xe3, ( byte ) 0xc4, ( byte ) 0x25,
                ( byte ) 0x9e, ( byte ) 0xd9, ( byte ) 0x13, ( byte ) 0xf2, ( byte ) 0x07 };

        byte[] result = keyDerivationFunction.deriveRandom( key, usage, 64, 168 );
        assertTrue( Arrays.equals( DR, result ) );

        result = keyDerivationFunction.randomToKey( result );
        assertTrue( Arrays.equals( DK, result ) );
    }


    /**
     * Tests that key derivation can be performed for a Triple-DES key.
     */
    public void testTestVectorsTripleDesKerberosKey1()
    {
        byte[] expectedKey =
            { ( byte ) 0x85, ( byte ) 0x0B, ( byte ) 0xB5, ( byte ) 0x13, ( byte ) 0x58, ( byte ) 0x54, ( byte ) 0x8C,
                ( byte ) 0xD0, ( byte ) 0x5E, ( byte ) 0x86, ( byte ) 0x76, ( byte ) 0x8C, ( byte ) 0x31,
                ( byte ) 0x3E, ( byte ) 0x3B, ( byte ) 0xFE, ( byte ) 0xF7, ( byte ) 0x51, ( byte ) 0x19,
                ( byte ) 0x37, ( byte ) 0xDC, ( byte ) 0xF7, ( byte ) 0x2C, ( byte ) 0x3E };

        KerberosPrincipal principal = new KerberosPrincipal( "raeburn@ATHENA.MIT.EDU" );
        String algorithm = VendorHelper.getTripleDesAlgorithm();
        KerberosKey key = new KerberosKey( principal, "password".toCharArray(), algorithm );

        assertEquals( "DESede key length", 24, key.getEncoded().length );
        assertTrue( "Key match", Arrays.equals( expectedKey, key.getEncoded() ) );
    }


    /**
     * Tests that key derivation can be performed for a Triple-DES key.
     */
    public void testTestVectorsTripleDesKerberosKey2()
    {
        byte[] expectedKey =
            { ( byte ) 0xDF, ( byte ) 0xCD, ( byte ) 0x23, ( byte ) 0x3D, ( byte ) 0xD0, ( byte ) 0xA4, ( byte ) 0x32,
                ( byte ) 0x04, ( byte ) 0xEA, ( byte ) 0x6D, ( byte ) 0xC4, ( byte ) 0x37, ( byte ) 0xFB,
                ( byte ) 0x15, ( byte ) 0xE0, ( byte ) 0x61, ( byte ) 0xB0, ( byte ) 0x29, ( byte ) 0x79,
                ( byte ) 0xC1, ( byte ) 0xF7, ( byte ) 0x4F, ( byte ) 0x37, ( byte ) 0x7A };

        KerberosPrincipal principal = new KerberosPrincipal( "danny@WHITEHOUSE.GOV" );
        String algorithm = VendorHelper.getTripleDesAlgorithm();
        KerberosKey key = new KerberosKey( principal, "potatoe".toCharArray(), algorithm );

        assertEquals( "DESede key length", 24, key.getEncoded().length );
        assertTrue( "Key match", Arrays.equals( expectedKey, key.getEncoded() ) );
    }


    /**
     * Tests that key derivation can be performed for a Triple-DES key.
     */
    public void testTestVectorsTripleDesKerberosKey3()
    {
        byte[] expectedKey =
            { ( byte ) 0x6D, ( byte ) 0x2F, ( byte ) 0xCD, ( byte ) 0xF2, ( byte ) 0xD6, ( byte ) 0xFB, ( byte ) 0xBC,
                ( byte ) 0x3D, ( byte ) 0xDC, ( byte ) 0xAD, ( byte ) 0xB5, ( byte ) 0xDA, ( byte ) 0x57,
                ( byte ) 0x10, ( byte ) 0xA2, ( byte ) 0x34, ( byte ) 0x89, ( byte ) 0xB0, ( byte ) 0xD3,
                ( byte ) 0xB6, ( byte ) 0x9D, ( byte ) 0x5D, ( byte ) 0x9D, ( byte ) 0x4A };

        KerberosPrincipal principal = new KerberosPrincipal( "buckaroo@EXAMPLE.COM" );
        String algorithm = VendorHelper.getTripleDesAlgorithm();
        KerberosKey key = new KerberosKey( principal, "penny".toCharArray(), algorithm );

        assertEquals( "DESede key length", 24, key.getEncoded().length );
        assertTrue( "Key match", Arrays.equals( expectedKey, key.getEncoded() ) );
    }


    /**
     * Tests that key derivation can be performed for a Triple-DES key.
     */
    public void testTestVectorsTripleDesKerberosKey4()
    {
        if ( VendorHelper.isIbm() )
        {
            return;
        }

        byte[] expectedKey =
            { ( byte ) 0x16, ( byte ) 0xD5, ( byte ) 0xA4, ( byte ) 0x0E, ( byte ) 0x1C, ( byte ) 0xE3, ( byte ) 0xBA,
                ( byte ) 0xCB, ( byte ) 0x61, ( byte ) 0xB9, ( byte ) 0xDC, ( byte ) 0xE0, ( byte ) 0x04,
                ( byte ) 0x70, ( byte ) 0x32, ( byte ) 0x4C, ( byte ) 0x83, ( byte ) 0x19, ( byte ) 0x73,
                ( byte ) 0xA7, ( byte ) 0xB9, ( byte ) 0x52, ( byte ) 0xFE, ( byte ) 0xB0 };

        KerberosPrincipal principal = new KerberosPrincipal( "Juri\u0161i\u0107@ATHENA.MIT.EDU" );
        String algorithm = VendorHelper.getTripleDesAlgorithm();
        KerberosKey key = new KerberosKey( principal, "\u00DF".toCharArray(), algorithm );

        assertEquals( "DESede key length", 24, key.getEncoded().length );
        assertTrue( "Key match", Arrays.equals( expectedKey, key.getEncoded() ) );
    }


    /**
     * Tests that key derivation can be performed for a Triple-DES key.
     */
    public void testTestVectorsTripleDesKerberosKey5()
    {
        if ( VendorHelper.isIbm() )
        {
            return;
        }

        byte[] expectedKey =
            { ( byte ) 0x85, ( byte ) 0x76, ( byte ) 0x37, ( byte ) 0x26, ( byte ) 0x58, ( byte ) 0x5D, ( byte ) 0xBC,
                ( byte ) 0x1C, ( byte ) 0xCE, ( byte ) 0x6E, ( byte ) 0xC4, ( byte ) 0x3E, ( byte ) 0x1F,
                ( byte ) 0x75, ( byte ) 0x1F, ( byte ) 0x07, ( byte ) 0xF1, ( byte ) 0xC4, ( byte ) 0xCB,
                ( byte ) 0xB0, ( byte ) 0x98, ( byte ) 0xF4, ( byte ) 0x0B, ( byte ) 0x19 };

        KerberosPrincipal principal = new KerberosPrincipal( "pianist@EXAMPLE.COM" );
        String algorithm = VendorHelper.getTripleDesAlgorithm();
        KerberosKey key = new KerberosKey( principal, "\uD834\uDD1E".toCharArray(), algorithm );

        assertEquals( "DESede key length", 24, key.getEncoded().length );
        assertTrue( "Key match", Arrays.equals( expectedKey, key.getEncoded() ) );
    }
}
