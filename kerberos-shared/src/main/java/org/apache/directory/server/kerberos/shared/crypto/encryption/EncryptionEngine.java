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


import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumEngine;
import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumType;
import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class EncryptionEngine
{
    private static final SecureRandom random = new SecureRandom();


    public abstract ChecksumEngine getChecksumEngine();


    public abstract Cipher getCipher() throws GeneralSecurityException;


    public abstract EncryptionType encryptionType();


    public abstract ChecksumType checksumType();


    public abstract CipherType keyType();


    public abstract int confounderSize();


    public abstract int checksumSize();


    public abstract int blockSize();


    public abstract int minimumPadSize();


    public abstract int keySize();


    public byte[] getDecryptedData( EncryptionKey key, EncryptedData data ) throws KerberosException
    {
        byte[] decryptedData = decrypt( data.getCipherText(), key.getKeyValue() );

        // extract the old checksum
        byte[] oldChecksum = new byte[checksumSize()];
        System.arraycopy( decryptedData, confounderSize(), oldChecksum, 0, oldChecksum.length );

        // zero out the old checksum in the cipher text
        for ( int i = confounderSize(); i < confounderSize() + checksumSize(); i++ )
        {
            decryptedData[i] = 0;
        }

        // calculate a new checksum
        byte[] newChecksum = calculateChecksum( decryptedData );

        // compare checksums
        if ( !Arrays.equals( oldChecksum, newChecksum ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY );
        }

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
        return processCipher( true, data, key );
    }


    private byte[] decrypt( byte[] data, byte[] key )
    {
        return processCipher( false, data, key );
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


    private byte[] processCipher( boolean encrypt, byte[] data, byte[] keyBytes )
    {
        try
        {
            Cipher cipher = getCipher();
            SecretKey key = new SecretKeySpec( keyBytes, "DES" );

            byte[] iv = new byte[]
                { ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00,
                    ( byte ) 0x00, ( byte ) 0x00 };
            AlgorithmParameterSpec paramSpec = new IvParameterSpec( iv );

            if ( encrypt )
            {
                cipher.init( Cipher.ENCRYPT_MODE, key, paramSpec );
            }
            else
            {
                cipher.init( Cipher.DECRYPT_MODE, key, paramSpec );
            }

            byte[] finalBytes = cipher.doFinal( data );

            return finalBytes;
        }
        catch ( GeneralSecurityException nsae )
        {
            return null;
        }
    }
}
