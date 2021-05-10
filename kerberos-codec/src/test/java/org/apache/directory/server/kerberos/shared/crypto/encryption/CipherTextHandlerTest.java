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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.Date;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.KerberosDecoder;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.PaEncTsEnc;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.junit.jupiter.api.Test;


import org.apache.directory.shared.kerberos.KerberosUtils;

/**
 * Test case for sealing and unsealing Kerberos CipherText.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CipherTextHandlerTest
{
    private static final byte[] DES_ENCRYPTED_TIME_STAMP =
        { ( byte ) 0x97, ( byte ) 0x21, ( byte ) 0x58, ( byte ) 0x5f, ( byte ) 0x81, ( byte ) 0x46, ( byte ) 0x17,
            ( byte ) 0xa6, ( byte ) 0x4e, ( byte ) 0x8a, ( byte ) 0x5d, ( byte ) 0xe2, ( byte ) 0xf3, ( byte ) 0xd1,
            ( byte ) 0x40, ( byte ) 0x30, ( byte ) 0x38, ( byte ) 0x5e, ( byte ) 0xb8, ( byte ) 0xf6, ( byte ) 0xad,
            ( byte ) 0xd8, ( byte ) 0x7c, ( byte ) 0x30, ( byte ) 0xb0, ( byte ) 0x0d, ( byte ) 0x69, ( byte ) 0x71,
            ( byte ) 0x08, ( byte ) 0xd5, ( byte ) 0x6a, ( byte ) 0x61, ( byte ) 0x1f, ( byte ) 0xee, ( byte ) 0x38,
            ( byte ) 0xad, ( byte ) 0x43, ( byte ) 0x99, ( byte ) 0xae, ( byte ) 0xc2, ( byte ) 0xd2, ( byte ) 0xf5,
            ( byte ) 0xb2, ( byte ) 0xb7, ( byte ) 0x95, ( byte ) 0x22, ( byte ) 0x93, ( byte ) 0x12, ( byte ) 0x63,
            ( byte ) 0xd5, ( byte ) 0xf4, ( byte ) 0x39, ( byte ) 0xfa, ( byte ) 0x27, ( byte ) 0x6e, ( byte ) 0x8e };

    private static final byte[] TRIPLE_DES_ENCRYPTED_TIME_STAMP =
        { ( byte ) 0x96, ( byte ) 0xcb, ( byte ) 0x38, ( byte ) 0xb3, ( byte ) 0xc9, ( byte ) 0xb5, ( byte ) 0x78,
            ( byte ) 0x17, ( byte ) 0xba, ( byte ) 0x0a, ( byte ) 0x64, ( byte ) 0x49, ( byte ) 0x18, ( byte ) 0x39,
            ( byte ) 0x57, ( byte ) 0x1e, ( byte ) 0xcf, ( byte ) 0xfc, ( byte ) 0x6e, ( byte ) 0x0f, ( byte ) 0x53,
            ( byte ) 0xe2, ( byte ) 0x9c, ( byte ) 0x96, ( byte ) 0xfd, ( byte ) 0xbc, ( byte ) 0xc6, ( byte ) 0x1e,
            ( byte ) 0x10, ( byte ) 0x35, ( byte ) 0xe0, ( byte ) 0x8f, ( byte ) 0xc1, ( byte ) 0x7f, ( byte ) 0xbd,
            ( byte ) 0x86, ( byte ) 0x55, ( byte ) 0xf2, ( byte ) 0x22, ( byte ) 0x48, ( byte ) 0x86, ( byte ) 0xfb,
            ( byte ) 0x92, ( byte ) 0x22, ( byte ) 0xe7, ( byte ) 0xbe, ( byte ) 0xd1, ( byte ) 0xec, ( byte ) 0x2e,
            ( byte ) 0x37, ( byte ) 0xd8, ( byte ) 0x47, ( byte ) 0x1e, ( byte ) 0xa0, ( byte ) 0x16, ( byte ) 0x70,
            ( byte ) 0x5f, ( byte ) 0x6b, ( byte ) 0x18, ( byte ) 0xf3 };

    private static final byte[] AES128_ENCRYPTED_TIME_STAMP =
        { ( byte ) 0x4f, ( byte ) 0x1e, ( byte ) 0x52, ( byte ) 0xf5, ( byte ) 0xe0, ( byte ) 0xee, ( byte ) 0xe5,
            ( byte ) 0xe2, ( byte ) 0x2c, ( byte ) 0x9b, ( byte ) 0xf4, ( byte ) 0xdc, ( byte ) 0x58, ( byte ) 0x5f,
            ( byte ) 0x00, ( byte ) 0x96, ( byte ) 0x31, ( byte ) 0xfe, ( byte ) 0xc7, ( byte ) 0xf7, ( byte ) 0x89,
            ( byte ) 0x38, ( byte ) 0x88, ( byte ) 0xf5, ( byte ) 0x25, ( byte ) 0xaf, ( byte ) 0x09, ( byte ) 0x9f,
            ( byte ) 0xfd, ( byte ) 0x78, ( byte ) 0x68, ( byte ) 0x3b, ( byte ) 0xb4, ( byte ) 0x1e, ( byte ) 0xc2,
            ( byte ) 0xfc, ( byte ) 0x2d, ( byte ) 0xf3, ( byte ) 0x41, ( byte ) 0x88, ( byte ) 0x92, ( byte ) 0x7e,
            ( byte ) 0xd7, ( byte ) 0xed, ( byte ) 0xe1, ( byte ) 0xe0, ( byte ) 0x0c, ( byte ) 0xad, ( byte ) 0xe5,
            ( byte ) 0x06, ( byte ) 0xbf, ( byte ) 0x30, ( byte ) 0x1e, ( byte ) 0xbf, ( byte ) 0xf2, ( byte ) 0xec };

    private static final byte[] AES256_ENCRYPTED_TIME_STAMP =
        { ( byte ) 0xa8, ( byte ) 0x40, ( byte ) 0x73, ( byte ) 0xfc, ( byte ) 0xe5, ( byte ) 0x45, ( byte ) 0x66,
            ( byte ) 0xd6, ( byte ) 0x83, ( byte ) 0xb4, ( byte ) 0xed, ( byte ) 0xb6, ( byte ) 0x18, ( byte ) 0x5a,
            ( byte ) 0xd2, ( byte ) 0x24, ( byte ) 0xd6, ( byte ) 0xef, ( byte ) 0x38, ( byte ) 0xac, ( byte ) 0xdf,
            ( byte ) 0xcd, ( byte ) 0xed, ( byte ) 0x6d, ( byte ) 0x32, ( byte ) 0xf6, ( byte ) 0x00, ( byte ) 0xd1,
            ( byte ) 0xc0, ( byte ) 0xb0, ( byte ) 0x1e, ( byte ) 0x70, ( byte ) 0x13, ( byte ) 0x48, ( byte ) 0x0a,
            ( byte ) 0x5a, ( byte ) 0xbb, ( byte ) 0xd2, ( byte ) 0x2a, ( byte ) 0x6b, ( byte ) 0x16, ( byte ) 0x29,
            ( byte ) 0x63, ( byte ) 0xba, ( byte ) 0xea, ( byte ) 0xb7, ( byte ) 0x1a, ( byte ) 0x90, ( byte ) 0x7b,
            ( byte ) 0xf4, ( byte ) 0x89, ( byte ) 0x94, ( byte ) 0x7a, ( byte ) 0x2d, ( byte ) 0x6a, ( byte ) 0xf1 };

    private static final byte[] ARCFOUR_ENCRYPTED_TIME_STAMP =
        { ( byte ) 0xa2, ( byte ) 0x4f, ( byte ) 0x04, ( byte ) 0x6d, ( byte ) 0x93, ( byte ) 0x31, ( byte ) 0x19,
            ( byte ) 0x77, ( byte ) 0x3f, ( byte ) 0x9d, ( byte ) 0xf9, ( byte ) 0x6f, ( byte ) 0x7e, ( byte ) 0x86,
            ( byte ) 0x2c, ( byte ) 0x99, ( byte ) 0x63, ( byte ) 0xc5, ( byte ) 0xcf, ( byte ) 0xe2, ( byte ) 0xf1,
            ( byte ) 0x54, ( byte ) 0x05, ( byte ) 0x6a, ( byte ) 0xea, ( byte ) 0x20, ( byte ) 0x37, ( byte ) 0x31,
            ( byte ) 0xa2, ( byte ) 0xdc, ( byte ) 0xe8, ( byte ) 0x79, ( byte ) 0xaa, ( byte ) 0xae, ( byte ) 0x1c,
            ( byte ) 0xfa, ( byte ) 0x93, ( byte ) 0x02, ( byte ) 0xbe, ( byte ) 0x11, ( byte ) 0x14, ( byte ) 0x22,
            ( byte ) 0x65, ( byte ) 0x92, ( byte ) 0xbd, ( byte ) 0xf5, ( byte ) 0x52, ( byte ) 0x9f, ( byte ) 0x94,
            ( byte ) 0x67, ( byte ) 0x10, ( byte ) 0xd2 };


    /**
     * Tests the lengths of the test vectors for encrypted timestamps for each
     * of the supported encryption types.  The length of the Kerberos Cipher Text
     * is relevant to the structure of the underlying plaintext.
     */
    @Test
    public void testTestVectorLengths()
    {
        assertEquals( "DES length", 56, DES_ENCRYPTED_TIME_STAMP.length );
        assertEquals( "DES3 length", 60, TRIPLE_DES_ENCRYPTED_TIME_STAMP.length );
        assertEquals( "AES128 length", 56, AES128_ENCRYPTED_TIME_STAMP.length );
        assertEquals( "AES256 length", 56, AES256_ENCRYPTED_TIME_STAMP.length );
        assertEquals( "RC4-HMAC length", 52, ARCFOUR_ENCRYPTED_TIME_STAMP.length );
    }

    /**
     * Tests the unsealing of Kerberos CipherText with a good password.  After decryption and
     * an integrity check, an attempt is made to decode the bytes as an EncryptedTimestamp.  The
     * result is timestamp data.
     */
    @Test
    public void testAes128GoodPasswordDecrypt()
    {
        if ( !VendorHelper.isCtsSupported() )
        {
            return;
        }

        CipherTextHandler lockBox = new CipherTextHandler();
        KerberosPrincipal principal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosKey kerberosKey = new KerberosKey( principal, "secret".toCharArray(), "AES128" );
        EncryptionKey key = new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, kerberosKey.getEncoded() );
        EncryptedData data = new EncryptedData( EncryptionType.AES128_CTS_HMAC_SHA1_96, 0, AES128_ENCRYPTED_TIME_STAMP );

        try
        {
            byte[] paEncTsEncData = lockBox.decrypt( key, data, KeyUsage.AS_REQ_PA_ENC_TIMESTAMP_WITH_CKEY );
            PaEncTsEnc object = KerberosDecoder.decodePaEncTsEnc( paEncTsEncData );
            assertEquals( "TimeStamp", "20070410212557Z", object.getPaTimestamp().toString() );
            assertEquals( "MicroSeconds", 379386, object.getPausec() );
        }
        catch ( KerberosException ke )
        {
            fail( "Should not have caught exception." );
        }
    }


    /**
     * Tests the encryption and subsequent unsealing of an ASN.1 encoded timestamp with a
     * good password.  After encryption, an attempt is made to unseal the encrypted bytes
     * as an EncryptedTimestamp.  The result is timestamp data.
     * 
     * @throws ParseException 
     */
    @Test
    public void testAes128GoodPasswordEncrypt() throws ParseException
    {
        if ( !VendorHelper.isCtsSupported() )
        {
            return;
        }

        CipherTextHandler lockBox = new CipherTextHandler();
        KerberosPrincipal principal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
        KerberosKey kerberosKey = new KerberosKey( principal, "secret".toCharArray(), "AES128" );
        EncryptionKey key = new EncryptionKey( EncryptionType.AES128_CTS_HMAC_SHA1_96, kerberosKey.getEncoded() );

        String zuluTime = "20070410190400Z";
        int microSeconds = 460450;
        PaEncTsEnc encryptedTimeStamp = getEncryptedTimeStamp( zuluTime, microSeconds );

        EncryptedData encryptedData = null;

        try
        {
            encryptedData = lockBox.seal( key, encryptedTimeStamp, KeyUsage.AS_REQ_PA_ENC_TIMESTAMP_WITH_CKEY );
        }
        catch ( KerberosException ke )
        {
            fail( "Should not have caught exception." );
        }

        try
        {
            byte[] paEncTsEncData = lockBox.decrypt( key, encryptedData, KeyUsage.AS_REQ_PA_ENC_TIMESTAMP_WITH_CKEY );
            PaEncTsEnc object = KerberosDecoder.decodePaEncTsEnc( paEncTsEncData );
            assertEquals( "TimeStamp", "20070410190400Z", object.getPaTimestamp().toString() );
            assertEquals( "MicroSeconds", 460450, object.getPausec() );
        }
        catch ( KerberosException ke )
        {
            fail( "Should not have caught exception." );
        }
    }


    /**
     * Tests the unsealing of Kerberos CipherText with a good password.  After decryption and
     * an integrity check, an attempt is made to decode the bytes as an EncryptedTimestamp.  The
     * result is timestamp data.
     */
    @Test
    public void testAes256GoodPasswordDecrypt()
    {
        if ( !VendorHelper.isCtsSupported() )
        {
            return;
        }

        CipherTextHandler lockBox = new CipherTextHandler();
        KerberosKey kerberosKey;

        try
        {
            KerberosPrincipal principal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
            kerberosKey = new KerberosKey( principal, "secret".toCharArray(), "AES256" );
        }
        catch ( IllegalArgumentException iae )
        {
            // Algorithm AES256 not enabled
            return;
        }

        EncryptionKey key = new EncryptionKey( EncryptionType.AES256_CTS_HMAC_SHA1_96, kerberosKey.getEncoded() );
        EncryptedData data = new EncryptedData( EncryptionType.AES256_CTS_HMAC_SHA1_96, 0, AES256_ENCRYPTED_TIME_STAMP );

        try
        {
            byte[] paEncTsEncData = lockBox.decrypt( key, data, KeyUsage.AS_REQ_PA_ENC_TIMESTAMP_WITH_CKEY );
            PaEncTsEnc object = KerberosDecoder.decodePaEncTsEnc( paEncTsEncData );
            assertEquals( "TimeStamp", "20070410212809Z", object.getPaTimestamp().toString() );
            assertEquals( "MicroSeconds", 298294, object.getPausec() );
        }
        catch ( KerberosException ke )
        {
            fail( "Should not have caught exception." );
        }
    }


    /**
     * Tests the encryption and subsequent unsealing of an ASN.1 encoded timestamp with a
     * good password.  After encryption, an attempt is made to unseal the encrypted bytes
     * as an EncryptedTimestamp.  The result is timestamp data.
     * 
     * @throws ParseException 
     */
    @Test
    public void testAes256GoodPasswordEncrypt() throws ParseException
    {
        if ( !VendorHelper.isCtsSupported() )
        {
            return;
        }

        CipherTextHandler lockBox = new CipherTextHandler();

        KerberosKey kerberosKey;

        try
        {
            KerberosPrincipal principal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
            kerberosKey = new KerberosKey( principal, "secret".toCharArray(), "AES256" );
        }
        catch ( IllegalArgumentException iae )
        {
            // Algorithm AES256 not enabled
            return;
        }

        EncryptionKey key = new EncryptionKey( EncryptionType.AES256_CTS_HMAC_SHA1_96, kerberosKey.getEncoded() );

        String zuluTime = "20070410190400Z";
        int microSeconds = 460450;
        PaEncTsEnc encryptedTimeStamp = getEncryptedTimeStamp( zuluTime, microSeconds );

        EncryptedData encryptedData = null;

        try
        {
            encryptedData = lockBox.seal( key, encryptedTimeStamp, KeyUsage.AS_REQ_PA_ENC_TIMESTAMP_WITH_CKEY );
        }
        catch ( KerberosException ke )
        {
            fail( "Should not have caught exception." );
        }

        try
        {
            byte[] paEncTsEncData = lockBox.decrypt( key, encryptedData, KeyUsage.AS_REQ_PA_ENC_TIMESTAMP_WITH_CKEY );
            PaEncTsEnc object = KerberosDecoder.decodePaEncTsEnc( paEncTsEncData );
            assertEquals( "TimeStamp", "20070410190400Z", object.getPaTimestamp().toString() );
            assertEquals( "MicroSeconds", 460450, object.getPausec() );
        }
        catch ( KerberosException ke )
        {
            fail( "Should not have caught exception." );
        }
    }


    protected PaEncTsEnc getEncryptedTimeStamp( String zuluTime, int microSeconds ) throws ParseException
    {
        Date date = null;

        synchronized ( KerberosUtils.UTC_DATE_FORMAT )
        {
            date = KerberosUtils.UTC_DATE_FORMAT.parse( zuluTime );
        }

        KerberosTime timeStamp = new KerberosTime( date );

        return new PaEncTsEnc( timeStamp, microSeconds );
    }

    /*
     public void testArcFourGoodPassword()
     {
     LockBox lockBox = new LockBox();
     KerberosPrincipal principal = new KerberosPrincipal( "hnelson@EXAMPLE.COM" );
     KerberosKey kerberosKey = new KerberosKey( principal, "secret".toCharArray(), "ArcFourHmac" );
     EncryptionKey key = new EncryptionKey( EncryptionType.RC4_HMAC, kerberosKey.getEncoded() );
     EncryptedData data = new EncryptedData( EncryptionType.RC4_HMAC, 0, arcfourEncryptedTimeStamp );

     try
     {
     PaEncTsEnc object = ( PaEncTsEnc ) lockBox.unseal( hint, key, data );
     assertEquals( "TimeStamp", "20070322233107Z", object.getTimeStamp().toString() );
     assertEquals( "MicroSeconds", 291067, object.getPausec() );
     }
     catch ( KerberosException ke )
     {
     fail( "Should not have caught exception." );
     }
     }*/
}
