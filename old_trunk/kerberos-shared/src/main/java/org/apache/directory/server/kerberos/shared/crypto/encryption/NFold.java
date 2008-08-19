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


/**
 * An implementation of the n-fold algorithm, as required by RFC 3961,
 * "Encryption and Checksum Specifications for Kerberos 5."
 * 
 * "To n-fold a number X, replicate the input value to a length that
 * is the least common multiple of n and the length of X.  Before
 * each repetition, the input is rotated to the right by 13 bit
 * positions.  The successive n-bit chunks are added together using
 * 1's-complement addition (that is, with end-around carry) to yield
 * a n-bit result."
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NFold
{
    /**
     * N-fold the data n times.
     * 
     * @param n The number of times to n-fold the data.
     * @param data The data to n-fold.
     * @return The n-folded data.
     */
    public static byte[] nFold( int n, byte[] data )
    {
        int k = data.length * 8;
        int lcm = getLcm( n, k );
        int replicate = lcm / k;
        byte[] sumBytes = new byte[lcm / 8];

        for ( int i = 0; i < replicate; i++ )
        {
            int rotation = 13 * i;

            byte[] temp = rotateRight( data, data.length * 8, rotation );

            for ( int j = 0; j < temp.length; j++ )
            {
                sumBytes[j + i * temp.length] = temp[j];
            }
        }

        byte[] sum = new byte[n / 8];
        byte[] nfold = new byte[n / 8];

        for ( int m = 0; m < lcm / n; m++ )
        {
            for ( int o = 0; o < n / 8; o++ )
            {
                sum[o] = sumBytes[o + ( m * n / 8 )];
            }

            nfold = sum( nfold, sum, nfold.length * 8 );

        }

        return nfold;
    }


    /**
     * For 2 numbers, return the least-common multiple.
     *
     * @param n1 The first number.
     * @param n2 The second number.
     * @return The least-common multiple.
     */
    protected static int getLcm( int n1, int n2 )
    {
        int temp;
        int product;

        product = n1 * n2;

        do
        {
            if ( n1 < n2 )
            {
                temp = n1;
                n1 = n2;
                n2 = temp;
            }
            n1 = n1 % n2;
        }
        while ( n1 != 0 );

        return product / n2;
    }


    /**
     * Right-rotate the given byte array.
     *
     * @param in The byte array to right-rotate.
     * @param len The length of the byte array to rotate.
     * @param step The number of positions to rotate the byte array.
     * @return The right-rotated byte array.
     */
    private static byte[] rotateRight( byte[] in, int len, int step )
    {
        int numOfBytes = ( len - 1 ) / 8 + 1;
        byte[] out = new byte[numOfBytes];

        for ( int i = 0; i < len; i++ )
        {
            int val = getBit( in, i );
            setBit( out, ( i + step ) % len, val );
        }
        return out;
    }


    /**
     * Perform one's complement addition (addition with end-around carry).  Note
     * that for purposes of n-folding, we do not actually complement the
     * result of the addition.
     * 
     * @param n1 The first number.
     * @param n2 The second number.
     * @param len The length of the byte arrays to sum.
     * @return The sum with end-around carry.
     */
    protected static byte[] sum( byte[] n1, byte[] n2, int len )
    {
        int numOfBytes = ( len - 1 ) / 8 + 1;
        byte[] out = new byte[numOfBytes];
        int carry = 0;

        for ( int i = len - 1; i > -1; i-- )
        {
            int n1b = getBit( n1, i );
            int n2b = getBit( n2, i );

            int sum = n1b + n2b + carry;

            if ( sum == 0 || sum == 1 )
            {
                setBit( out, i, sum );
                carry = 0;
            }
            else if ( sum == 2 )
            {
                carry = 1;
            }
            else if ( sum == 3 )
            {
                setBit( out, i, 1 );
                carry = 1;
            }
        }

        if ( carry == 1 )
        {
            byte[] carryArray = new byte[n1.length];
            carryArray[carryArray.length - 1] = 1;
            out = sum( out, carryArray, n1.length * 8 );
        }

        return out;
    }


    /**
     * Get a bit from a byte array at a given position.
     *
     * @param data The data to get the bit from.
     * @param pos The position to get the bit at.
     * @return The value of the bit.
     */
    private static int getBit( byte[] data, int pos )
    {
        int posByte = pos / 8;
        int posBit = pos % 8;

        byte valByte = data[posByte];
        int valInt = valByte >> ( 8 - ( posBit + 1 ) ) & 0x0001;
        return valInt;
    }


    /**
     * Set a bit in a byte array at a given position.
     *
     * @param data The data to set the bit in.
     * @param pos The position of the bit to set.
     * @param The value to set the bit to.
     */
    private static void setBit( byte[] data, int pos, int val )
    {
        int posByte = pos / 8;
        int posBit = pos % 8;
        byte oldByte = data[posByte];
        oldByte = ( byte ) ( ( ( 0xFF7F >> posBit ) & oldByte ) & 0x00FF );
        byte newByte = ( byte ) ( ( val << ( 8 - ( posBit + 1 ) ) ) | oldByte );
        data[posByte] = newByte;
    }
}
