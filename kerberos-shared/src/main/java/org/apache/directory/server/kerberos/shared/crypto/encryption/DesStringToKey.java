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


import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * An implementation of the DES string-to-key function as originally described
 * in RFC 1510, "The Kerberos Network Authentication Service (V5)," and clarified
 * in RFC 3961, "Encryption and Checksum Specifications for Kerberos 5."
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 502338 $, $Date: 2007-02-01 11:59:43 -0800 (Thu, 01 Feb 2007) $
 */
public class DesStringToKey
{
    /**
     * Returns a DES symmetric key for the given passphrase.
     *
     * @param passPhrase The passphrase to derive a symmetric DES key from.
     * @return The derived symmetric DES key.
     */
    public byte[] getKey( String passPhrase )
    {
        return generateKey( passPhrase );
    }


    /**
     * Returns a DES symmetric key for the given input String components,
     * which will be concatenated in the order described in RFC's 1510 and 3961,
     * namely password+realm+username.
     *
     * @param password The password.
     * @param realmName The name of the realm.
     * @param userName The username.
     * @return The derived symmetric DES key.
     */
    public byte[] getKey( String password, String realmName, String userName )
    {
        return generateKey( password + realmName + userName );
    }


    /**
     * Returns a DES symmetric key for the given input String.
     *
     * @param passPhrase The passphrase.
     * @return The DES key.
     * @throws Exception
     */
    protected byte[] generateKey( String passPhrase )
    {
        byte encodedByteArray[] = characterEncodeString( passPhrase );

        byte paddedByteArray[] = padString( encodedByteArray );

        byte[] secretKey = fanFold( paddedByteArray );

        secretKey = setParity( secretKey );
        secretKey = getStrongKey( secretKey );
        secretKey = calculateChecksum( paddedByteArray, secretKey );
        secretKey = setParity( secretKey );
        secretKey = getStrongKey( secretKey );

        return secretKey;
    }


    /**
     * Set odd parity on an eight-byte array.
     *
     * @param in The byte array to set parity on.
     * @return The parity-adjusted byte array.
     */
    protected byte[] setParity( byte[] in )
    {
        byte[] out = new byte[8];

        int bitCount = 0;
        int index = 0;

        for ( int i = 0; i < 64; i++ )
        {
            if ( ( i + 1 ) % 8 == 0 )
            {
                if ( bitCount % 2 == 0 )
                {
                    setBit( out, i, 1 );
                }

                index++;
                bitCount = 0;
            }
            else
            {
                int val = getBit( in, index );
                boolean bit = val > 0;

                if ( bit )
                {
                    setBit( out, i, val );
                    bitCount++;
                }

                index++;
            }
        }

        return out;
    }


    /**
     * Gets a bit at a given position.
     *
     * @param data
     * @param pos
     * @return The value of the bit.
     */
    protected int getBit( byte[] data, int pos )
    {
        int posByte = pos / 8;
        int posBit = pos % 8;

        byte valByte = data[posByte];
        int valInt = valByte >> ( 8 - ( posBit + 1 ) ) & 0x0001;
        return valInt;
    }


    /**
     * Sets a bit at a given position.
     *
     * @param data
     * @param pos
     * @param val
     */
    protected void setBit( byte[] data, int pos, int val )
    {
        int posByte = pos / 8;
        int posBit = pos % 8;
        byte oldByte = data[posByte];
        oldByte = ( byte ) ( ( ( 0xFF7F >> posBit ) & oldByte ) & 0x00FF );
        byte newByte = ( byte ) ( ( val << ( 8 - ( posBit + 1 ) ) ) | oldByte );
        data[posByte] = newByte;
    }


    /**
     * "The top bit of each octet (always zero if the password is plain
     * ASCII, as was assumed when the original specification was written) is
     * discarded, and the remaining seven bits of each octet form a
     * bitstring.  This is then fan-folded and eXclusive-ORed with itself to
     * produce a 56-bit string.  An eight-octet key is formed from this
     * string, each octet using seven bits from the bitstring, leaving the
     * least significant bit unassigned."
     *
     * @param paddedByteArray The padded byte array.
     * @return The fan-folded intermediate DES key.
     */
    protected byte[] fanFold( byte[] paddedByteArray )
    {
        byte secretKey[] = new byte[8];

        int div = paddedByteArray.length / 8;

        for ( int ii = 0; ii < div; ii++ )
        {
            byte blockValue1[] = new byte[8];
            System.arraycopy( paddedByteArray, ii * 8, blockValue1, 0, 8 );

            if ( ii % 2 == 1 )
            {
                byte tempbyte1 = 0;
                byte tempbyte2 = 0;
                byte blockValue2[] = new byte[8];

                for ( int jj = 0; jj < 8; jj++ )
                {
                    tempbyte2 = 0;

                    for ( int kk = 0; kk < 4; kk++ )
                    {
                        tempbyte2 = ( byte ) ( ( 1 << ( 7 - kk ) ) & 0xff );
                        tempbyte1 |= ( blockValue1[jj] & tempbyte2 ) >>> ( 7 - 2 * kk );
                        tempbyte2 = 0;
                    }

                    for ( int kk = 4; kk < 8; kk++ )
                    {
                        tempbyte2 = ( byte ) ( ( 1 << ( 7 - kk ) ) & 0xff );
                        tempbyte1 |= ( blockValue1[jj] & tempbyte2 ) << ( 2 * kk - 7 );
                        tempbyte2 = 0;
                    }

                    blockValue2[7 - jj] = tempbyte1;
                    tempbyte1 = 0;
                }

                for ( int jj = 0; jj < 8; jj++ )
                {
                    blockValue2[jj] = ( byte ) ( ( ( blockValue2[jj] & 0xff ) >>> 1 ) & 0xff );
                }

                System.arraycopy( blockValue2, 0, blockValue1, 0, blockValue2.length );
            }

            for ( int jj = 0; jj < 8; jj++ )
            {
                blockValue1[jj] = ( byte ) ( ( ( blockValue1[jj] & 0xff ) << 1 ) & 0xff );
            }

            // ... eXclusive-ORed with itself to form an 8-byte DES key
            for ( int jj = 0; jj < 8; jj++ )
            {
                secretKey[jj] ^= blockValue1[jj];
            }
        }

        return secretKey;
    }


    /**
     * Calculates the checksum as described in "String or Random-Data to
     * Key Transformation."  An intermediate key is used to generate a DES CBC
     * "checksum" on the initial passphrase+salt.  The encryption key is also
     * used as the IV.  The final eight-byte block is returned as the "checksum."
     *
     * @param data The data to encrypt.
     * @param keyBytes The bytes of the intermediate key.
     * @return The final eight-byte block as the checksum.
     */
    protected byte[] calculateChecksum( byte[] data, byte[] keyBytes )
    {
        try
        {
            Cipher cipher = Cipher.getInstance( "DES/CBC/NoPadding" );
            SecretKey key = new SecretKeySpec( keyBytes, "DES" );

            AlgorithmParameterSpec paramSpec = new IvParameterSpec( keyBytes );

            cipher.init( Cipher.ENCRYPT_MODE, key, paramSpec );

            byte[] result = cipher.doFinal( data );

            byte[] checksum = new byte[8];
            System.arraycopy( result, result.length - 8, checksum, 0, 8 );

            return checksum;
        }
        catch ( GeneralSecurityException nsae )
        {
            nsae.printStackTrace();
            return null;
        }
    }


    /**
     * If the secret key is weak, correct by exclusive OR'ing
     * with the constant 0xF0.
     * 
     * @param keyValue The key to correct, if necessary.
     * @return The corrected key.
     */
    protected byte[] getStrongKey( byte[] secretKey )
    {
        try
        {
            if ( DESKeySpec.isWeak( secretKey, 0 ) )
            {
                secretKey[7] ^= 0xf0;
            }
        }
        catch ( InvalidKeyException ike )
        {
            return new byte[8];
        }

        return secretKey;
    }


    /**
     * Encodes string with UTF-8 encoding.
     *
     * @param string The String to encode.
     * @return The encoded String.
     */
    protected byte[] characterEncodeString( String string )
    {
        byte encodedByteArray[] = new byte[string.length()];

        try
        {
            encodedByteArray = string.getBytes( "UTF-8" );
        }
        catch ( UnsupportedEncodingException ue )
        {
        }

        return encodedByteArray;
    }


    /**
     * Add padding to make an exact multiple of 8 bytes.
     *
     * @param encodedString
     * @return The padded byte array.
     */
    protected byte[] padString( byte encodedString[] )
    {
        int length;

        if ( encodedString.length < 8 )
        {
            length = encodedString.length;
        }
        else
        {
            length = encodedString.length % 8;
        }

        if ( length == 0 )
        {
            return encodedString;
        }

        byte paddedByteArray[] = new byte[( 8 - length ) + encodedString.length];

        for ( int ii = paddedByteArray.length - 1; ii > encodedString.length - 1; ii-- )
        {
            paddedByteArray[ii] = 0;
        }

        System.arraycopy( encodedString, 0, paddedByteArray, 0, encodedString.length );

        return paddedByteArray;
    }
}
