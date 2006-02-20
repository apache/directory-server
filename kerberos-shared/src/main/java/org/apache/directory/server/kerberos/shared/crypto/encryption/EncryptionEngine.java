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
package org.apache.directory.server.kerberos.shared.crypto.encryption;


import java.security.SecureRandom;

import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumEngine;
import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;


public abstract class EncryptionEngine
{
    private static final SecureRandom random = new SecureRandom();


    public abstract ChecksumEngine getChecksumEngine();


    public abstract BlockCipher getBlockCipher();


    public abstract EncryptionType encryptionType();


    public abstract ChecksumType checksumType();


    public abstract CipherType keyType();


    public abstract int confounderSize();


    public abstract int checksumSize();


    public abstract int blockSize();


    public abstract int minimumPadSize();


    public abstract int keySize();


    public byte[] getDecryptedData( EncryptionKey key, EncryptedData data )
    {
        byte[] decryptedData = decrypt( data.getCipherText(), key.getKeyValue() );

        return removeBytes( decryptedData, confounderSize(), checksumSize() );
    }


    public EncryptedData getEncryptedData( EncryptionKey key, byte[] plainText )
    {
        byte[] conFounder = getRandomBytes( confounderSize() );
        byte[] zeroedChecksum = new byte[checksumSize()];
        byte[] paddedPlainText = padString( plainText );
        byte[] dataBytes = concatenateBytes( conFounder, concatenateBytes( zeroedChecksum, paddedPlainText ) );
        byte[] checksumBytes = calculateChecksum( dataBytes );
        byte[] paddedDataBytes = padString( dataBytes );

        // lay the checksum into the ciphertext
        for ( int i = confounderSize(); i < confounderSize() + checksumSize(); i++ )
        {
            paddedDataBytes[i] = checksumBytes[i - confounderSize()];
        }

        byte[] encryptedData = encrypt( paddedDataBytes, key.getKeyValue() );

        return new EncryptedData( encryptionType(), key.getKeyVersion(), encryptedData );
    }


    private byte[] encrypt( byte[] data, byte[] key )
    {
        return processBlockCipher( true, data, key, null );
    }


    private byte[] decrypt( byte[] data, byte[] key )
    {
        return processBlockCipher( false, data, key, null );
    }


    private byte[] getRandomBytes( int size )
    {
        byte[] bytes = new byte[size];

        // SecureRandom.nextBytes is already synchronized
        random.nextBytes( bytes );

        return bytes;
    }


    private byte[] padString( byte encodedString[] )
    {
        int x;
        if ( encodedString.length < 8 )
        {
            x = encodedString.length;
        }
        else
        {
            x = encodedString.length % 8;
        }

        if ( x == 0 )
        {
            return encodedString;
        }

        byte paddedByteArray[] = new byte[( 8 - x ) + encodedString.length];

        for ( int y = paddedByteArray.length - 1; y > encodedString.length - 1; y-- )
        {
            paddedByteArray[y] = 0;
        }

        System.arraycopy( encodedString, 0, paddedByteArray, 0, encodedString.length );

        return paddedByteArray;
    }


    private byte[] concatenateBytes( byte[] array1, byte[] array2 )
    {
        byte concatenatedBytes[] = new byte[array1.length + array2.length];

        for ( int i = 0; i < array1.length; i++ )
        {
            concatenatedBytes[i] = array1[i];
        }

        for ( int j = array1.length; j < concatenatedBytes.length; j++ )
        {
            concatenatedBytes[j] = array2[j - array1.length];
        }

        return concatenatedBytes;
    }


    private byte[] calculateChecksum( byte[] data )
    {
        ChecksumEngine digester = getChecksumEngine();

        return digester.calculateChecksum( data );
    }


    private byte[] removeBytes( byte[] array, int confounder, int checksum )
    {
        byte lessBytes[] = new byte[array.length - confounder - checksum];

        int j = 0;
        for ( int i = confounder + checksum; i < array.length; i++ )
        {
            lessBytes[j] = array[i];
            j++;
        }

        return lessBytes;
    }


    private byte[] processBlockCipher( boolean encrypt, byte[] data, byte[] key, byte[] ivec )
    {
        byte[] returnData = new byte[data.length];
        CBCBlockCipher cbcCipher = new CBCBlockCipher( getBlockCipher() );
        KeyParameter keyParameter = new KeyParameter( key );

        if ( ivec != null )
        {
            ParametersWithIV kpWithIV = new ParametersWithIV( keyParameter, ivec );
            cbcCipher.init( encrypt, kpWithIV );
        }
        else
        {
            cbcCipher.init( encrypt, keyParameter );
        }

        int offset = 0;
        int processedBytesLength = 0;

        while ( offset < returnData.length )
        {
            try
            {
                processedBytesLength = cbcCipher.processBlock( data, offset, returnData, offset );
                offset += processedBytesLength;
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                break;
            }
        }

        return returnData;
    }
}
