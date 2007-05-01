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
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AesCtsSha1Encryption extends EncryptionEngine
{
    private static final byte[] usageKe =
        { ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x01, ( byte ) 0xaa };

    private static final byte[] usageKi =
        { ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x01, ( byte ) 0x55 };

    private static final byte[] iv = new byte[]
        { ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00,
            ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00,
            ( byte ) 0x00, ( byte ) 0x00 };


    public int getConfounderLength()
    {
        return 16;
    }


    public int getChecksumLength()
    {
        return 12;
    }


    protected abstract int getKeyLength();


    protected byte[] deriveKey( byte[] baseKey, byte[] usage, int n, int k )
    {
        return deriveRandom( baseKey, usage, n, k );
    }


    public byte[] getDecryptedData( EncryptionKey key, EncryptedData data ) throws KerberosException
    {
        byte[] Ke = deriveKey( key.getKeyValue(), usageKe, 128, getKeyLength() );
        byte[] Ki = deriveKey( key.getKeyValue(), usageKi, 128, getKeyLength() );

        byte[] encryptedData = data.getCipherText();

        // extract the old checksum
        byte[] oldChecksum = new byte[getChecksumLength()];
        System
            .arraycopy( encryptedData, encryptedData.length - getChecksumLength(), oldChecksum, 0, oldChecksum.length );

        // remove trailing checksum
        encryptedData = removeTrailingBytes( encryptedData, 0, getChecksumLength() );

        // decrypt the data
        byte[] decryptedData = decrypt( encryptedData, Ke );

        // remove leading confounder
        byte[] withoutConfounder = removeLeadingBytes( decryptedData, getConfounderLength(), 0 );

        // calculate a new checksum
        byte[] newChecksum = calculateChecksum( decryptedData, Ki );
        newChecksum = removeTrailingBytes( newChecksum, 0, newChecksum.length - getChecksumLength() );

        // compare checksums
        if ( !Arrays.equals( oldChecksum, newChecksum ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY );
        }

        return withoutConfounder;
    }


    public EncryptedData getEncryptedData( EncryptionKey key, byte[] plainText )
    {
        byte[] Ke = deriveKey( key.getKeyValue(), usageKe, 128, getKeyLength() );
        byte[] Ki = deriveKey( key.getKeyValue(), usageKi, 128, getKeyLength() );

        // build the ciphertext structure
        byte[] conFounder = getRandomBytes( getConfounderLength() );
        byte[] dataBytes = concatenateBytes( conFounder, plainText );

        byte[] checksumBytes = calculateChecksum( dataBytes, Ki );
        checksumBytes = removeTrailingBytes( checksumBytes, 0, checksumBytes.length - getChecksumLength() );

        byte[] encryptedData = encrypt( dataBytes, Ke );
        byte[] cipherText = concatenateBytes( encryptedData, checksumBytes );

        return new EncryptedData( getEncryptionType(), key.getKeyVersion(), cipherText );
    }


    public byte[] encrypt( byte[] plainText, byte[] keyBytes )
    {
        return processCipher( true, plainText, keyBytes );
    }


    public byte[] decrypt( byte[] cipherText, byte[] keyBytes )
    {
        return processCipher( false, cipherText, keyBytes );
    }


    public byte[] calculateChecksum( byte[] data, byte[] key )
    {
        try
        {
            SecretKey sk = new SecretKeySpec( key, "AES" );

            Mac mac = Mac.getInstance( "HmacSHA1" );
            mac.init( sk );

            return mac.doFinal( data );
        }
        catch ( GeneralSecurityException nsae )
        {
            nsae.printStackTrace();
            return null;
        }
    }


    private byte[] processCipher( boolean isEncrypt, byte[] data, byte[] keyBytes )
    {
        try
        {
            Cipher cipher = Cipher.getInstance( "AES/CTS/NoPadding" );
            SecretKey key = new SecretKeySpec( keyBytes, "AES" );

            AlgorithmParameterSpec paramSpec = new IvParameterSpec( iv );

            if ( isEncrypt )
            {
                cipher.init( Cipher.ENCRYPT_MODE, key, paramSpec );
            }
            else
            {
                cipher.init( Cipher.DECRYPT_MODE, key, paramSpec );
            }

            return cipher.doFinal( data );
        }
        catch ( GeneralSecurityException nsae )
        {
            nsae.printStackTrace();
            return null;
        }
    }
}
