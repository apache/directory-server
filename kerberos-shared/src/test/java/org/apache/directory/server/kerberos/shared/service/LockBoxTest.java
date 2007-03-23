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
package org.apache.directory.server.kerberos.shared.service;


import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import junit.framework.TestCase;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedTimeStamp;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;


/**
 * Test case for sealing and unsealing Kerberos CipherText.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LockBoxTest extends TestCase
{
    private byte[] encryptedTimeStamp =
        { ( byte ) 0x97, ( byte ) 0x21, ( byte ) 0x58, ( byte ) 0x5f, ( byte ) 0x81, ( byte ) 0x46, ( byte ) 0x17,
            ( byte ) 0xa6, ( byte ) 0x4e, ( byte ) 0x8a, ( byte ) 0x5d, ( byte ) 0xe2, ( byte ) 0xf3, ( byte ) 0xd1,
            ( byte ) 0x40, ( byte ) 0x30, ( byte ) 0x38, ( byte ) 0x5e, ( byte ) 0xb8, ( byte ) 0xf6, ( byte ) 0xad,
            ( byte ) 0xd8, ( byte ) 0x7c, ( byte ) 0x30, ( byte ) 0xb0, ( byte ) 0x0d, ( byte ) 0x69, ( byte ) 0x71,
            ( byte ) 0x08, ( byte ) 0xd5, ( byte ) 0x6a, ( byte ) 0x61, ( byte ) 0x1f, ( byte ) 0xee, ( byte ) 0x38,
            ( byte ) 0xad, ( byte ) 0x43, ( byte ) 0x99, ( byte ) 0xae, ( byte ) 0xc2, ( byte ) 0xd2, ( byte ) 0xf5,
            ( byte ) 0xb2, ( byte ) 0xb7, ( byte ) 0x95, ( byte ) 0x22, ( byte ) 0x93, ( byte ) 0x12, ( byte ) 0x63,
            ( byte ) 0xd5, ( byte ) 0xf4, ( byte ) 0x39, ( byte ) 0xfa, ( byte ) 0x27, ( byte ) 0x6e, ( byte ) 0x8e };


    /**
     * Tests the unsealing of Kerberos CipherText with a good password.  After decryption and
     * an integrity check, an attempt is made to decode the bytes as an EncryptedTimestamp.  The
     * result is timestamp data.
     */
    public void testGoodPassword()
    {
        LockBox lockBox = new LockBox();
        Class hint = EncryptedTimeStamp.class;
        KerberosPrincipal principal = new KerberosPrincipal( "erodriguez@EXAMPLE.COM" );
        KerberosKey kerberosKey = new KerberosKey( principal, "kerby".toCharArray(), "DES" );
        EncryptionKey key = new EncryptionKey( EncryptionType.DES_CBC_MD5, kerberosKey.getEncoded() );
        EncryptedData data = new EncryptedData( EncryptionType.DES_CBC_MD5, 0, encryptedTimeStamp );

        try
        {
            EncryptedTimeStamp object = ( EncryptedTimeStamp ) lockBox.unseal( hint, key, data );
            assertEquals( "TimeStamp", "20070322233107Z", object.getTimeStamp().toString() );
            assertEquals( "MicroSeconds", 291067, object.getMicroSeconds() );
        }
        catch ( KerberosException ke )
        {
            fail( "Should not have caught exception." );
        }
    }


    /**
     * Tests the unsealing of Kerberos CipherText with a bad password.  After decryption, the
     * checksum is tested and should fail on comparison, resulting in an integrity check error.
     */
    public void testBadPassword()
    {
        LockBox lockBox = new LockBox();
        Class hint = EncryptedTimeStamp.class;
        KerberosPrincipal principal = new KerberosPrincipal( "erodriguez@EXAMPLE.COM" );
        KerberosKey kerberosKey = new KerberosKey( principal, "badpassword".toCharArray(), "DES" );
        EncryptionKey key = new EncryptionKey( EncryptionType.DES_CBC_MD5, kerberosKey.getEncoded() );
        EncryptedData data = new EncryptedData( EncryptionType.DES_CBC_MD5, 0, encryptedTimeStamp );

        try
        {
            lockBox.unseal( hint, key, data );
            fail( "Should have thrown exception." );
        }
        catch ( KerberosException ke )
        {
            assertEquals( "ErrorCode", 31, ke.getErrorCode() );
        }
    }
}
