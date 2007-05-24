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
import java.util.Arrays;

import javax.crypto.spec.DESKeySpec;

import junit.framework.TestCase;


/**
 * Test cases for the DES string-to-key function as described in RFC 3961,
 * "Encryption and Checksum Specifications for Kerberos 5."
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DesStringToKeyTest extends TestCase
{
    private byte[] fanFold1 =
        { ( byte ) 0xC0, ( byte ) 0x1E, ( byte ) 0x38, ( byte ) 0x68, ( byte ) 0x8A, ( byte ) 0xC8, ( byte ) 0x6C,
            ( byte ) 0x2E };
    private byte[] intermediateKey1 =
        { ( byte ) 0xC1, ( byte ) 0x1F, ( byte ) 0x38, ( byte ) 0x68, ( byte ) 0x8A, ( byte ) 0xC8, ( byte ) 0x6D,
            ( byte ) 0x2F };
    private byte[] desKey1 =
        { ( byte ) 0xCB, ( byte ) 0xC2, ( byte ) 0x2F, ( byte ) 0xAE, ( byte ) 0x23, ( byte ) 0x52, ( byte ) 0x98,
            ( byte ) 0xE3 };

    private byte[] fanFold2 =
        { ( byte ) 0xA0, ( byte ) 0x28, ( byte ) 0x94, ( byte ) 0x4E, ( byte ) 0xE6, ( byte ) 0x3C, ( byte ) 0x04,
            ( byte ) 0x16 };
    private byte[] intermediateKey2 =
        { ( byte ) 0xA1, ( byte ) 0x29, ( byte ) 0x94, ( byte ) 0x4F, ( byte ) 0xE6, ( byte ) 0x3D, ( byte ) 0x04,
            ( byte ) 0x16 };
    private byte[] desKey2 =
        { ( byte ) 0xDF, ( byte ) 0x3D, ( byte ) 0x32, ( byte ) 0xA7, ( byte ) 0x4F, ( byte ) 0xD9, ( byte ) 0x2A,
            ( byte ) 0x01 };

    private DesStringToKey stringToKey = new DesStringToKey();


    /**
     * Tests DES StringToKey test vector 1 from RFC 3961.
     */
    public void testDesStringToKeyVector1()
    {
        byte[] key = stringToKey.getKey( "password", "ATHENA.MIT.EDU", "raeburn" );

        assertTrue( "Key match", Arrays.equals( desKey1, key ) );
    }


    /**
     * Tests DES StringToKey test vector 2 from RFC 3961.
     */
    public void testDesStringToKeyVector2()
    {
        byte[] key = stringToKey.getKey( "potatoe", "WHITEHOUSE.GOV", "danny" );

        assertTrue( "Key match", Arrays.equals( desKey2, key ) );
    }


    /**
     * Tests DES StringToKey test vector 1 from RFC 3961 with intermediate step checks.
     *
     * @throws InvalidKeyException
     */
    public void testIntermediateDesStringToKeyVector1() throws InvalidKeyException
    {
        String passPhrase = "passwordATHENA.MIT.EDUraeburn";

        byte[] encodedByteArray = stringToKey.characterEncodeString( passPhrase );
        byte[] paddedByteArray = stringToKey.padString( encodedByteArray );
        byte[] fanFold = stringToKey.fanFold( paddedByteArray );

        assertTrue( "Key match", Arrays.equals( fanFold1, fanFold ) );

        fanFold = stringToKey.setParity( fanFold );
        assertTrue( "Key match", Arrays.equals( intermediateKey1, fanFold ) );

        byte[] secretKey = getDesKey( paddedByteArray, fanFold );
        assertTrue( "Key match", Arrays.equals( desKey1, secretKey ) );
    }


    /**
     * Tests DES StringToKey test vector 2 from RFC 3961 with intermediate step checks.
     * 
     * @throws InvalidKeyException
     */
    public void testIntermediateDesStringToKeyVector2() throws InvalidKeyException
    {
        String passPhrase = "potatoeWHITEHOUSE.GOVdanny";

        byte[] encodedByteArray = stringToKey.characterEncodeString( passPhrase );
        byte[] paddedByteArray = stringToKey.padString( encodedByteArray );
        byte[] fanFold = stringToKey.fanFold( paddedByteArray );

        assertTrue( "Key match", Arrays.equals( fanFold2, fanFold ) );

        fanFold = stringToKey.setParity( fanFold );
        assertTrue( "Key match", Arrays.equals( intermediateKey2, fanFold ) );

        byte[] secretKey = getDesKey( paddedByteArray, fanFold );
        assertTrue( "Key match", Arrays.equals( desKey2, secretKey ) );
    }


    /**
     * Test harness method for checking intermediate key state, which is not
     * exposed from {@link DesStringToKey}.
     *
     * @param paddedByteArray The input passphrase.
     * @param intermediateKey The intermediate key generated by fan-folding and parity-adjustment.
     * @return The final DES key.
     * @throws InvalidKeyException
     */
    private byte[] getDesKey( byte[] paddedByteArray, byte[] intermediateKey ) throws InvalidKeyException
    {
        if ( DESKeySpec.isWeak( intermediateKey, 0 ) )
        {
            intermediateKey = stringToKey.getStrongKey( intermediateKey );
        }

        byte[] secretKey = stringToKey.calculateChecksum( paddedByteArray, intermediateKey );

        secretKey = stringToKey.setParity( secretKey );

        if ( DESKeySpec.isWeak( secretKey, 0 ) )
        {
            secretKey = stringToKey.getStrongKey( secretKey );
        }

        return secretKey;
    }
}
