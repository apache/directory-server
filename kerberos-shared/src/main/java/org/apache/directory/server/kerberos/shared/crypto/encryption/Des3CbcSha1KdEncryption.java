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

import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumEngine;
import org.apache.directory.server.kerberos.shared.crypto.checksum.ChecksumType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.types.KerberosErrorType;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Des3CbcSha1KdEncryption extends EncryptionEngine implements ChecksumEngine
{
    private static final byte[] iv = new byte[]
        { ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00,
            ( byte ) 0x00 };


    public EncryptionType getEncryptionType()
    {
        return EncryptionType.DES3_CBC_SHA1_KD;
    }


    public int getConfounderLength()
    {
        return 8;
    }


    public int getChecksumLength()
    {
        return 20;
    }


    public ChecksumType checksumType()
    {
        return ChecksumType.HMAC_SHA1_DES3_KD;
    }


    public byte[] calculateChecksum( byte[] data, byte[] key, KeyUsage usage )
    {
        byte[] Kc = deriveKey( key, getUsageKc( usage ), 64, 168 );

        return processChecksum( data, Kc );
    }


    public byte[] calculateIntegrity( byte[] data, byte[] key, KeyUsage usage )
    {
        byte[] Ki = deriveKey( key, getUsageKi( usage ), 64, 168 );

        return processChecksum( data, Ki );
    }


    public byte[] getDecryptedData( EncryptionKey key, EncryptedData data, KeyUsage usage ) throws KerberosException
    {
        byte[] Ke = deriveKey( key.getKeyValue(), getUsageKe( usage ), 64, 168 );

        byte[] encryptedData = data.getCipher();

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
        byte[] newChecksum = calculateIntegrity( decryptedData, key.getKeyValue(), usage );

        // compare checksums
        if ( !Arrays.equals( oldChecksum, newChecksum ) )
        {
            throw new KerberosException( KerberosErrorType.KRB_AP_ERR_BAD_INTEGRITY );
        }

        return withoutConfounder;
    }


    public EncryptedData getEncryptedData( EncryptionKey key, byte[] plainText, KeyUsage usage )
    {
        byte[] Ke = deriveKey( key.getKeyValue(), getUsageKe( usage ), 64, 168 );

        // build the ciphertext structure
        byte[] conFounder = getRandomBytes( getConfounderLength() );
        byte[] paddedPlainText = padString( plainText );
        byte[] dataBytes = concatenateBytes( conFounder, paddedPlainText );
        byte[] checksumBytes = calculateIntegrity( dataBytes, key.getKeyValue(), usage );

        //byte[] encryptedData = encrypt( paddedDataBytes, key.getKeyValue() );
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


    /**
     * Derived Key = DK(Base Key, Well-Known Constant)
     * DK(Key, Constant) = random-to-key(DR(Key, Constant))
     * DR(Key, Constant) = k-truncate(E(Key, Constant, initial-cipher-state))
     */
    protected byte[] deriveKey( byte[] baseKey, byte[] usage, int n, int k )
    {
        byte[] result = deriveRandom( baseKey, usage, n, k );
        result = randomToKey( result );

        return result;
    }


    protected byte[] randomToKey( byte[] seed )
    {
        int kBytes = 24;
        byte[] result = new byte[kBytes];

        byte[] fillingKey = new byte[0];

        int pos = 0;

        for ( int i = 0; i < kBytes; i++ )
        {
            if ( pos < fillingKey.length )
            {
                result[i] = fillingKey[pos];
                pos++;
            }
            else
            {
                fillingKey = getBitGroup( seed, i / 8 );
                fillingKey = setParity( fillingKey );
                pos = 0;
                result[i] = fillingKey[pos];
                pos++;
            }
        }

        return result;
    }


    protected byte[] getBitGroup( byte[] seed, int group )
    {
        int srcPos = group * 7;

        byte[] result = new byte[7];

        System.arraycopy( seed, srcPos, result, 0, 7 );

        return result;
    }


    protected byte[] setParity( byte[] in )
    {
        byte[] expandedIn = new byte[8];

        System.arraycopy( in, 0, expandedIn, 0, in.length );

        setBit( expandedIn, 62, getBit( in, 7 ) );
        setBit( expandedIn, 61, getBit( in, 15 ) );
        setBit( expandedIn, 60, getBit( in, 23 ) );
        setBit( expandedIn, 59, getBit( in, 31 ) );
        setBit( expandedIn, 58, getBit( in, 39 ) );
        setBit( expandedIn, 57, getBit( in, 47 ) );
        setBit( expandedIn, 56, getBit( in, 55 ) );

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
                int val = getBit( expandedIn, index );
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


    private byte[] processCipher( boolean isEncrypt, byte[] data, byte[] keyBytes )
    {
        try
        {
            Cipher cipher = Cipher.getInstance( "DESede/CBC/NoPadding" );
            SecretKey key = new SecretKeySpec( keyBytes, "DESede" );

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


    private byte[] processChecksum( byte[] data, byte[] key )
    {
        try
        {
            SecretKey sk = new SecretKeySpec( key, "DESede" );

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
}
