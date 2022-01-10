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


import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.InvalidKeyException;
import java.util.Arrays;

import javax.crypto.spec.DESKeySpec;

import org.junit.jupiter.api.Test;


/**
 * Test cases for the DES string-to-key function as described in RFC 3961,
 * "Encryption and Checksum Specifications for Kerberos 5."
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DesStringToKeyTest
{
    private static final byte[] FAN_FOLD1 =
        { ( byte ) 0xC0, ( byte ) 0x1E, ( byte ) 0x38, ( byte ) 0x68, ( byte ) 0x8A, ( byte ) 0xC8, ( byte ) 0x6C,
            ( byte ) 0x2E };
    private static final byte[] INTERMEDIATE_KEY1 =
        { ( byte ) 0xC1, ( byte ) 0x1F, ( byte ) 0x38, ( byte ) 0x68, ( byte ) 0x8A, ( byte ) 0xC8, ( byte ) 0x6D,
            ( byte ) 0x2F };
    private static final byte[] DES_KEY1 =
        { ( byte ) 0xCB, ( byte ) 0xC2, ( byte ) 0x2F, ( byte ) 0xAE, ( byte ) 0x23, ( byte ) 0x52, ( byte ) 0x98,
            ( byte ) 0xE3 };

    private static final byte[] FAN_FOLD2 =
        { ( byte ) 0xA0, ( byte ) 0x28, ( byte ) 0x94, ( byte ) 0x4E, ( byte ) 0xE6, ( byte ) 0x3C, ( byte ) 0x04,
            ( byte ) 0x16 };
    private static final byte[] INTERMEDIATE_KEY2 =
        { ( byte ) 0xA1, ( byte ) 0x29, ( byte ) 0x94, ( byte ) 0x4F, ( byte ) 0xE6, ( byte ) 0x3D, ( byte ) 0x04,
            ( byte ) 0x16 };
    private static final byte[] DES_KEY2 =
        { ( byte ) 0xDF, ( byte ) 0x3D, ( byte ) 0x32, ( byte ) 0xA7, ( byte ) 0x4F, ( byte ) 0xD9, ( byte ) 0x2A,
            ( byte ) 0x01 };

    private static final DesStringToKey stringToKey = new DesStringToKey();


    /**
     * Tests DES StringToKey test vector 1 from RFC 3961.
     */
    @Test
    public void testDesStringToKeyVector1()
    {
        byte[] key = stringToKey.getKey( "password", "ATHENA.MIT.EDU", "raeburn" );

        assertTrue( Arrays.equals( DES_KEY1, key ), "Key match" );
    }


    /**
     * Tests DES StringToKey test vector 2 from RFC 3961.
     */
    @Test
    public void testDesStringToKeyVector2()
    {
        byte[] key = stringToKey.getKey( "potatoe", "WHITEHOUSE.GOV", "danny" );

        assertTrue( Arrays.equals( DES_KEY2, key ), "Key match" );
    }


    /**
     * Tests DES StringToKey test vector 1 from RFC 3961 with intermediate step checks.
     *
     * @throws InvalidKeyException
     */
    @Test
    public void testIntermediateDesStringToKeyVector1() throws InvalidKeyException
    {
        String passPhrase = "passwordATHENA.MIT.EDUraeburn";

        byte[] encodedByteArray = stringToKey.characterEncodeString( passPhrase );
        byte[] paddedByteArray = stringToKey.padString( encodedByteArray );
        byte[] fanFold = stringToKey.fanFold( paddedByteArray );

        assertTrue( Arrays.equals( FAN_FOLD1, fanFold ), "Key match" );

        fanFold = stringToKey.setParity( fanFold );
        assertTrue( Arrays.equals( INTERMEDIATE_KEY1, fanFold ), "Key match" );

        byte[] secretKey = getDesKey( paddedByteArray, fanFold );
        assertTrue( Arrays.equals( DES_KEY1, secretKey ), "Key match" );
    }


    /**
     * Tests DES StringToKey test vector 2 from RFC 3961 with intermediate step checks.
     * 
     * @throws InvalidKeyException
     */
    @Test
    public void testIntermediateDesStringToKeyVector2() throws InvalidKeyException
    {
        String passPhrase = "potatoeWHITEHOUSE.GOVdanny";

        byte[] encodedByteArray = stringToKey.characterEncodeString( passPhrase );
        byte[] paddedByteArray = stringToKey.padString( encodedByteArray );
        byte[] fanFold = stringToKey.fanFold( paddedByteArray );

        assertTrue( Arrays.equals( FAN_FOLD2, fanFold ), "Key match" );

        fanFold = stringToKey.setParity( fanFold );
        assertTrue( Arrays.equals( INTERMEDIATE_KEY2, fanFold ), "Key match" );

        byte[] secretKey = getDesKey( paddedByteArray, fanFold );
        assertTrue( Arrays.equals( DES_KEY2, secretKey ), "Key match" );
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
