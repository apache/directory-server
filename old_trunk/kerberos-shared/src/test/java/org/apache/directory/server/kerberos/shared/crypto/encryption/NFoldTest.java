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

import junit.framework.TestCase;


/**
 * Tests the use of "n-folding" using test vectors from RFC 3961,
 * "Encryption and Checksum Specifications for Kerberos 5."
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NFoldTest extends TestCase
{
    /**
     * Tests an n-fold test vector from RFC 3961.
     */
    public void testNFold1()
    {
        int n = 64;
        String passPhrase = "012345";

        int k = passPhrase.getBytes().length * 8;
        int lcm = NFold.getLcm( n, k );
        assertEquals( "LCM", 192, lcm );

        byte[] nFoldValue = NFold.nFold( n, passPhrase.getBytes() );

        byte[] testVector =
            { ( byte ) 0xbe, ( byte ) 0x07, ( byte ) 0x26, ( byte ) 0x31, ( byte ) 0x27, ( byte ) 0x6b, ( byte ) 0x19,
                ( byte ) 0x55 };
        assertTrue( Arrays.equals( nFoldValue, testVector ) );
    }


    /**
     * Tests an n-fold test vector from RFC 3961.
     */
    public void testNFold2()
    {
        int n = 56;
        String passPhrase = "password";

        int k = passPhrase.getBytes().length * 8;
        int lcm = NFold.getLcm( n, k );
        assertEquals( "LCM", 448, lcm );

        byte[] nFoldValue = NFold.nFold( n, passPhrase.getBytes() );

        byte[] testVector =
            { ( byte ) 0x78, ( byte ) 0xa0, ( byte ) 0x7b, ( byte ) 0x6c, ( byte ) 0xaf, ( byte ) 0x85, ( byte ) 0xfa };
        assertTrue( Arrays.equals( nFoldValue, testVector ) );
    }


    /**
     * Tests an n-fold test vector from RFC 3961.
     */
    public void testNFold3()
    {
        int n = 64;
        String passPhrase = "Rough Consensus, and Running Code";

        int k = passPhrase.getBytes().length * 8;
        int lcm = NFold.getLcm( n, k );
        assertEquals( "LCM", 2112, lcm );

        byte[] nFoldValue = NFold.nFold( n, passPhrase.getBytes() );

        byte[] testVector =
            { ( byte ) 0xbb, ( byte ) 0x6e, ( byte ) 0xd3, ( byte ) 0x08, ( byte ) 0x70, ( byte ) 0xb7, ( byte ) 0xf0,
                ( byte ) 0xe0 };
        assertTrue( Arrays.equals( nFoldValue, testVector ) );
    }


    /**
     * Tests an n-fold test vector from RFC 3961.
     */
    public void testNFold4()
    {
        int n = 168;
        String passPhrase = "password";

        int k = passPhrase.getBytes().length * 8;
        int lcm = NFold.getLcm( n, k );
        assertEquals( "LCM", 1344, lcm );

        byte[] nFoldValue = NFold.nFold( n, passPhrase.getBytes() );

        byte[] testVector =
            { ( byte ) 0x59, ( byte ) 0xe4, ( byte ) 0xa8, ( byte ) 0xca, ( byte ) 0x7c, ( byte ) 0x03, ( byte ) 0x85,
                ( byte ) 0xc3, ( byte ) 0xc3, ( byte ) 0x7b, ( byte ) 0x3f, ( byte ) 0x6d, ( byte ) 0x20,
                ( byte ) 0x00, ( byte ) 0x24, ( byte ) 0x7c, ( byte ) 0xb6, ( byte ) 0xe6, ( byte ) 0xbd,
                ( byte ) 0x5b, ( byte ) 0x3e };
        assertTrue( Arrays.equals( nFoldValue, testVector ) );
    }


    /**
     * Tests an n-fold test vector from RFC 3961.
     */
    public void testNFold5()
    {
        int n = 192;
        String passPhrase = "MASSACHVSETTS INSTITVTE OF TECHNOLOGY";

        int k = passPhrase.getBytes().length * 8;
        int lcm = NFold.getLcm( n, k );
        assertEquals( "LCM", 7104, lcm );

        byte[] nFoldValue = NFold.nFold( n, passPhrase.getBytes() );

        byte[] testVector =
            { ( byte ) 0xdb, ( byte ) 0x3b, ( byte ) 0x0d, ( byte ) 0x8f, ( byte ) 0x0b, ( byte ) 0x06, ( byte ) 0x1e,
                ( byte ) 0x60, ( byte ) 0x32, ( byte ) 0x82, ( byte ) 0xb3, ( byte ) 0x08, ( byte ) 0xa5,
                ( byte ) 0x08, ( byte ) 0x41, ( byte ) 0x22, ( byte ) 0x9a, ( byte ) 0xd7, ( byte ) 0x98,
                ( byte ) 0xfa, ( byte ) 0xb9, ( byte ) 0x54, ( byte ) 0x0c, ( byte ) 0x1b };
        assertTrue( Arrays.equals( nFoldValue, testVector ) );

    }


    /**
     * Tests an n-fold test vector from RFC 3961.
     */
    public void testNFold6()
    {
        int n = 168;
        String passPhrase = "Q";

        int k = passPhrase.getBytes().length * 8;
        int lcm = NFold.getLcm( n, k );
        assertEquals( "LCM", 168, lcm );

        byte[] nFoldValue = NFold.nFold( n, passPhrase.getBytes() );

        byte[] testVector =
            { ( byte ) 0x51, ( byte ) 0x8a, ( byte ) 0x54, ( byte ) 0xa2, ( byte ) 0x15, ( byte ) 0xa8, ( byte ) 0x45,
                ( byte ) 0x2a, ( byte ) 0x51, ( byte ) 0x8a, ( byte ) 0x54, ( byte ) 0xa2, ( byte ) 0x15,
                ( byte ) 0xa8, ( byte ) 0x45, ( byte ) 0x2a, ( byte ) 0x51, ( byte ) 0x8a, ( byte ) 0x54,
                ( byte ) 0xa2, ( byte ) 0x15 };
        assertTrue( Arrays.equals( nFoldValue, testVector ) );
    }


    /**
     * Tests an n-fold test vector from RFC 3961.
     */
    public void testNFold7()
    {
        int n = 168;
        String passPhrase = "ba";

        int k = passPhrase.getBytes().length * 8;
        int lcm = NFold.getLcm( n, k );
        assertEquals( "LCM", 336, lcm );

        byte[] nFoldValue = NFold.nFold( n, passPhrase.getBytes() );

        byte[] testVector =
            { ( byte ) 0xfb, ( byte ) 0x25, ( byte ) 0xd5, ( byte ) 0x31, ( byte ) 0xae, ( byte ) 0x89, ( byte ) 0x74,
                ( byte ) 0x49, ( byte ) 0x9f, ( byte ) 0x52, ( byte ) 0xfd, ( byte ) 0x92, ( byte ) 0xea,
                ( byte ) 0x98, ( byte ) 0x57, ( byte ) 0xc4, ( byte ) 0xba, ( byte ) 0x24, ( byte ) 0xcf,
                ( byte ) 0x29, ( byte ) 0x7e };
        assertTrue( Arrays.equals( nFoldValue, testVector ) );
    }


    /**
     * Tests an n-fold test vector from RFC 3961.
     */
    public void testNFoldKerberos64()
    {
        int n = 64;
        String passPhrase = "kerberos";

        int k = passPhrase.getBytes().length * 8;
        int lcm = NFold.getLcm( n, k );
        assertEquals( "LCM", 64, lcm );

        byte[] nFoldValue = NFold.nFold( n, passPhrase.getBytes() );

        byte[] testVector =
            { ( byte ) 0x6b, ( byte ) 0x65, ( byte ) 0x72, ( byte ) 0x62, ( byte ) 0x65, ( byte ) 0x72, ( byte ) 0x6f,
                ( byte ) 0x73 };
        assertTrue( Arrays.equals( nFoldValue, testVector ) );
    }


    /**
     * Tests an n-fold test vector from RFC 3961.
     */
    public void testNFoldKerberos128()
    {
        int n = 128;
        String passPhrase = "kerberos";

        int k = passPhrase.getBytes().length * 8;
        int lcm = NFold.getLcm( n, k );
        assertEquals( "LCM", 128, lcm );

        byte[] nFoldValue = NFold.nFold( n, passPhrase.getBytes() );

        byte[] testVector =
            { ( byte ) 0x6b, ( byte ) 0x65, ( byte ) 0x72, ( byte ) 0x62, ( byte ) 0x65, ( byte ) 0x72, ( byte ) 0x6f,
                ( byte ) 0x73, ( byte ) 0x7b, ( byte ) 0x9b, ( byte ) 0x5b, ( byte ) 0x2b, ( byte ) 0x93,
                ( byte ) 0x13, ( byte ) 0x2b, ( byte ) 0x93 };
        assertTrue( Arrays.equals( nFoldValue, testVector ) );
    }


    /**
     * Tests an n-fold test vector from RFC 3961.
     */
    public void testNFoldKerberos168()
    {
        int n = 168;
        String passPhrase = "kerberos";

        int k = passPhrase.getBytes().length * 8;
        int lcm = NFold.getLcm( n, k );
        assertEquals( "LCM", 1344, lcm );

        byte[] nFoldValue = NFold.nFold( n, passPhrase.getBytes() );

        byte[] testVector =
            { ( byte ) 0x83, ( byte ) 0x72, ( byte ) 0xc2, ( byte ) 0x36, ( byte ) 0x34, ( byte ) 0x4e, ( byte ) 0x5f,
                ( byte ) 0x15, ( byte ) 0x50, ( byte ) 0xcd, ( byte ) 0x07, ( byte ) 0x47, ( byte ) 0xe1,
                ( byte ) 0x5d, ( byte ) 0x62, ( byte ) 0xca, ( byte ) 0x7a, ( byte ) 0x5a, ( byte ) 0x3b,
                ( byte ) 0xce, ( byte ) 0xa4 };
        assertTrue( Arrays.equals( nFoldValue, testVector ) );
    }


    /**
     * Tests an n-fold test vector from RFC 3961.
     */
    public void testNFoldKerberos256()
    {
        int n = 256;
        String passPhrase = "kerberos";

        int k = passPhrase.getBytes().length * 8;
        int lcm = NFold.getLcm( n, k );
        assertEquals( "LCM", 256, lcm );

        byte[] nFoldValue = NFold.nFold( n, passPhrase.getBytes() );

        byte[] testVector =
            { ( byte ) 0x6b, ( byte ) 0x65, ( byte ) 0x72, ( byte ) 0x62, ( byte ) 0x65, ( byte ) 0x72, ( byte ) 0x6f,
                ( byte ) 0x73, ( byte ) 0x7b, ( byte ) 0x9b, ( byte ) 0x5b, ( byte ) 0x2b, ( byte ) 0x93,
                ( byte ) 0x13, ( byte ) 0x2b, ( byte ) 0x93, ( byte ) 0x5c, ( byte ) 0x9b, ( byte ) 0xdc,
                ( byte ) 0xda, ( byte ) 0xd9, ( byte ) 0x5c, ( byte ) 0x98, ( byte ) 0x99, ( byte ) 0xc4,
                ( byte ) 0xca, ( byte ) 0xe4, ( byte ) 0xde, ( byte ) 0xe6, ( byte ) 0xd6, ( byte ) 0xca, ( byte ) 0xe4 };
        assertTrue( Arrays.equals( nFoldValue, testVector ) );
    }


    /**
     * Test one's complement addition (addition with end-around carry).  Note
     * that for purposes of n-folding, we do not actually complement the
     * result of the addition.
     */
    public void testSum()
    {
        byte[] n1 =
            { ( byte ) 0x86, ( byte ) 0x5E };
        byte[] n2 =
            { ( byte ) 0xAC, ( byte ) 0x60 };
        byte[] n3 =
            { ( byte ) 0x71, ( byte ) 0x2A };
        byte[] n4 =
            { ( byte ) 0x81, ( byte ) 0xB5 };

        byte[] sum = NFold.sum( n1, n2, n1.length * 8 );
        sum = NFold.sum( sum, n3, sum.length * 8 );
        sum = NFold.sum( sum, n4, sum.length * 8 );

        byte[] result = new byte[]
            { ( byte ) 0x25, ( byte ) 0x9F };
        assertTrue( Arrays.equals( sum, result ) );
    }
}
