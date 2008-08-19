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


import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import junit.framework.TestCase;

import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;


/**
 * Test case for the DES-CBC-MD5 encryption type.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DesCbcMd5EncryptionTest extends TestCase
{
    private static final char[] PASSWORD = "password".toCharArray();


    /**
     * Test successful encryption and decryption when the plaintext size is less than the block size.
     *
     * @throws Exception
     */
    public void testPlainTextSizeLessThanBlockSize() throws Exception
    {
        KerberosKey key = new KerberosKey( new KerberosPrincipal( "hnelson@EXAMPLE.COM" ), PASSWORD, "DES" );
        byte[] keyBytes = key.getEncoded();
        EncryptionKey encryptionKey = new EncryptionKey( EncryptionType.DES_CBC_MD5, keyBytes );

        byte[] plainText =
            { 1, 2, 3, 4, 5, 6, 7 };

        DesCbcMd5Encryption encryption = new DesCbcMd5Encryption();
        EncryptedData encryptedData = encryption.getEncryptedData( encryptionKey, plainText, null );

        byte[] recoveredText = encryption.getDecryptedData( encryptionKey, encryptedData, null );

        assertTrue( beginsWith( plainText, recoveredText ) );
    }


    /**
     * Test successful encryption and decryption when the plaintext size equals the block size.
     *
     * @throws Exception
     */
    public void testPlainTextSizeEqualsBlockSize() throws Exception
    {
        KerberosKey key = new KerberosKey( new KerberosPrincipal( "hnelson@EXAMPLE.COM" ), PASSWORD, "DES" );
        byte[] keyBytes = key.getEncoded();
        EncryptionKey encryptionKey = new EncryptionKey( EncryptionType.DES_CBC_MD5, keyBytes );

        byte[] plainText =
            { 1, 2, 3, 4, 5, 6, 7, 8 };

        DesCbcMd5Encryption encryption = new DesCbcMd5Encryption();
        EncryptedData encryptedData = encryption.getEncryptedData( encryptionKey, plainText, null );

        byte[] recoveredText = encryption.getDecryptedData( encryptionKey, encryptedData, null );

        assertTrue( beginsWith( plainText, recoveredText ) );
    }


    /**
     * Test successful encryption and decryption when the plaintext size is greater than the block size.
     *
     * @throws Exception
     */
    public void testPlainTextSizeGreaterThanBlockSize() throws Exception
    {
        KerberosKey key = new KerberosKey( new KerberosPrincipal( "hnelson@EXAMPLE.COM" ), PASSWORD, "DES" );
        byte[] keyBytes = key.getEncoded();
        EncryptionKey encryptionKey = new EncryptionKey( EncryptionType.DES_CBC_MD5, keyBytes );

        byte[] plainText =
            { 1, 2, 3, 4, 5, 6, 7, 8, 9 };

        DesCbcMd5Encryption encryption = new DesCbcMd5Encryption();
        EncryptedData encryptedData = encryption.getEncryptedData( encryptionKey, plainText, null );

        byte[] recoveredText = encryption.getDecryptedData( encryptionKey, encryptedData, null );

        assertTrue( beginsWith( plainText, recoveredText ) );
    }


    private boolean beginsWith( byte[] plainText, byte[] recoveredText )
    {
        for ( int i = 0; i < plainText.length; i++ )
        {
            if ( plainText[i] != recoveredText[i] )
            {
                return false;
            }
        }

        return true;
    }
}
