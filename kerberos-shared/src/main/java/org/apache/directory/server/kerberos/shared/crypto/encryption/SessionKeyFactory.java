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
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;


/**
 * Generates new random keys, suitable for use as session keys.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SessionKeyFactory
{
    /**
     * SecureRandom.nextBytes() is synchronized, making this safe for static use.
     */
    private static final SecureRandom random = new SecureRandom();


    /**
     * Get a new random session key.
     *
     * @return The new random session key.
     */
    public static EncryptionKey getSessionKey()
    {
        // Only need 7 bytes.  With parity will result in 8 bytes.
        byte[] raw = new byte[7];

        // SecureRandom.nextBytes is already synchronized
        random.nextBytes( raw );

        byte[] keyBytes = addParity( raw );

        try
        {
            // check for weakness
            if ( DESKeySpec.isWeak( keyBytes, 0 ) )
            {
                keyBytes = getStrongKey( keyBytes );
            }
        }
        catch ( InvalidKeyException ike )
        {
            /*
             * Will only get here if the key is null or less
             * than 8 bytes, which won't ever happen.
             */
            return null;
        }

        SecretKey key = new SecretKeySpec( keyBytes, "DES" );
        byte[] subSessionKey = key.getEncoded();

        return new EncryptionKey( EncryptionType.DES_CBC_MD5, subSessionKey );
    }


    /**
     * Adds parity to 7-bytes to form an 8-byte DES key.
     *
     * @param sevenBytes
     * @return The 8-byte DES key with parity.
     */
    static byte[] addParity( byte[] sevenBytes )
    {
        byte[] result = new byte[8];

        // Keeps track of the bit position in the result.
        int resultIndex = 1;

        // Used to keep track of the number of 1 bits in each 7-bit chunk.
        int bitCount = 0;

        // Process each of the 56 bits.
        for ( int i = 0; i < 56; i++ )
        {
            // Get the bit at bit position i
            boolean bit = ( sevenBytes[6 - i / 8] & ( 1 << ( i % 8 ) ) ) > 0;

            // If set, set the corresponding bit in the result.
            if ( bit )
            {
                result[7 - resultIndex / 8] |= ( 1 << ( resultIndex % 8 ) ) & 0xFF;
                bitCount++;
            }

            // Set the parity bit after every 7 bits.
            if ( ( i + 1 ) % 7 == 0 )
            {
                if ( bitCount % 2 == 0 )
                {
                    // Set low-order bit (parity bit) if bit count is even.
                    result[7 - resultIndex / 8] |= 1;
                }
                resultIndex++;
                bitCount = 0;
            }
            resultIndex++;
        }

        return result;
    }


    /**
     * Corrects the weak key by exclusive OR with 0xF0 constant.
     */
    private static byte[] getStrongKey( byte keyValue[] )
    {
        keyValue[7] ^= 0xf0;

        return keyValue;
    }
}
