/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.kerberos.service;

import java.io.UnsupportedEncodingException;

import org.apache.protocol.common.chain.impl.CommandBase;
import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.DESParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public abstract class DesStringToKey extends CommandBase
{
    public byte[] getKey( String passPhrase )
    {
        return generateKey( passPhrase );
    }

    // This is the concatenation order as designated in RFC 1510
    public byte[] getKey( String password, String realmName, String userName )
    {
        return generateKey( password + realmName + userName );
    }

    private byte[] generateKey( String passPhrase )
    {
        byte encodedByteArray[] = characterEncodeString( passPhrase );

        byte paddedByteArray[] = padString( encodedByteArray );

        byte secretKey[] = fanFold( paddedByteArray );

        DESParameters.setOddParity( secretKey );

        if ( DESParameters.isWeakKey( secretKey, 0 ) )
        {
            secretKey = getStrongKey( secretKey );
        }

        secretKey = encryptSecretKey( paddedByteArray, secretKey );

        DESParameters.setOddParity( secretKey );

        if ( DESParameters.isWeakKey( secretKey, 0 ) )
        {
            secretKey = getStrongKey( secretKey );
        }

        return secretKey;
    }

    private byte[] fanFold( byte[] paddedByteArray )
    {
        byte secretKey[] = new byte[ 8 ];

        int div = paddedByteArray.length / 8;

        for ( int ii = 0; ii < div; ii++ )
        {
            byte blockValue1[] = new byte[ 8 ];
            System.arraycopy( paddedByteArray, ii * 8, blockValue1, 0, 8 );

            if ( ii % 2 == 1 )
            {
                byte tempbyte1 = 0;
                byte tempbyte2 = 0;
                byte blockValue2[] = new byte[ 8 ];

                for ( int jj = 0; jj < 8; jj++ )
                {
                    tempbyte2 = 0;

                    for ( int kk = 0; kk < 4; kk++ )
                    {
                        tempbyte2 = (byte) ( ( 1 << ( 7 - kk ) ) & 0xff );
                        tempbyte1 |= ( blockValue1[ jj ] & tempbyte2 ) >>> ( 7 - 2 * kk );
                        tempbyte2 = 0;
                    }

                    for ( int kk = 4; kk < 8; kk++ )
                    {
                        tempbyte2 = (byte) ( ( 1 << ( 7 - kk ) ) & 0xff );
                        tempbyte1 |= ( blockValue1[ jj ] & tempbyte2 ) << ( 2 * kk - 7 );
                        tempbyte2 = 0;
                    }

                    blockValue2[ 7 - jj ] = tempbyte1;
                    tempbyte1 = 0;
                }

                for ( int jj = 0; jj < 8; jj++ )
                {
                    blockValue2[ jj ] = (byte) ( ( ( blockValue2[ jj ] & 0xff ) >>> 1 ) & 0xff );
                }

                System.arraycopy( blockValue2, 0, blockValue1, 0, blockValue2.length );
            }

            for ( int jj = 0; jj < 8; jj++ )
            {
                blockValue1[ jj ] = (byte) ( ( ( blockValue1[ jj ] & 0xff ) << 1 ) & 0xff );
            }

            // ... eXclusive-ORed with itself to form an 8-byte DES key
            for ( int jj = 0; jj < 8; jj++ )
            {
                secretKey[ jj ] ^= blockValue1[ jj ];
            }
        }

        return secretKey;
    }

    // TODO - Re-evaluate when DES3 keys are supported.  This is duplicated
    //        with parts of EncryptionEngine, but makes this class standalone.
    private byte[] encryptSecretKey( byte data[], byte key[] )
    {
        CBCBlockCipher cipher = new CBCBlockCipher( new DESEngine() );
        KeyParameter kp = new KeyParameter( key );
        ParametersWithIV iv;

        iv = new ParametersWithIV( kp, key );
        cipher.init( true, iv );

        byte encKey[] = new byte[ data.length ];
        byte ivBytes[] = new byte[ 8 ];

        for ( int ii = 0; ii < data.length / 8; ii++ )
        {
            cipher.processBlock( data, ii * 8, encKey, ii * 8 );
            System.arraycopy( encKey, ii * 8, ivBytes, 0, 8 );
            iv = new ParametersWithIV( kp, ivBytes );
            cipher.init( true, iv );
        }

        return ivBytes;
    }

    // Corrects the weak key by exclusive OR with 0xF0 constant.
    private byte[] getStrongKey( byte keyValue[] )
    {
        keyValue[ 7 ] ^= 0xf0;

        return keyValue;
    }

    // Encodes string with ISO-Latin encoding
    private byte[] characterEncodeString( String str )
    {
        byte encodedByteArray[] = new byte[ str.length() ];

        try
        {
            encodedByteArray = str.getBytes( "8859_1" );
        }
        catch ( UnsupportedEncodingException ue )
        {
        }

        return encodedByteArray;
    }

    // Add padding to make an exact multiple of 8.
    // TODO - Re-evaluate when DES3 keys are supported.  This is duplicated
    //        with parts of EncryptionEngine, but makes this class standalone.
    private byte[] padString( byte encodedString[] )
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

        byte paddedByteArray[] = new byte[ ( 8 - length ) + encodedString.length ];

        for ( int ii = paddedByteArray.length - 1; ii > encodedString.length - 1; ii-- )
        {
            paddedByteArray[ ii ] = 0;
        }

        System.arraycopy( encodedString, 0, paddedByteArray, 0, encodedString.length );

        return paddedByteArray;
    }
}
