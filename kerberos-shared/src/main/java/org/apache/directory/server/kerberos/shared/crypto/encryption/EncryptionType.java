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


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public final class EncryptionType implements Comparable
{
    /**
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */
    public static final EncryptionType UNKNOWN = new EncryptionType( -1, "UNKNOWN" );
    public static final EncryptionType NULL = new EncryptionType( 0, "NULL" );
    public static final EncryptionType DES_CBC_CRC = new EncryptionType( 1, "des-cbc-crc" );
    public static final EncryptionType DES_CBC_MD4 = new EncryptionType( 2, "des-cbc-md4" );
    public static final EncryptionType DES_CBC_MD5 = new EncryptionType( 3, "des-cbc-md5" );
    public static final EncryptionType RESERVED4 = new EncryptionType( 4, "[reserved]" );
    public static final EncryptionType DES3_CBC_MD5 = new EncryptionType( 5, "des3-cbc-md5" );
    public static final EncryptionType RESERVED6 = new EncryptionType( 6, "[reserved]" );
    public static final EncryptionType DES3_CBC_SHA1 = new EncryptionType( 7, "des3-cbc-sha1" );
    public static final EncryptionType DSAWITHSHA1_CMSOID = new EncryptionType( 9, "dsaWithSHA1-CmsOID" );
    public static final EncryptionType MD5WITHRSAENCRYPTION_CMSOID = new EncryptionType( 10,
        "md5WithRSAEncryption-CmsOID" );
    public static final EncryptionType SHA1WITHRSAENCRYPTION_CMSOID = new EncryptionType( 11,
        "sha1WithRSAEncryption-CmsOID" );
    public static final EncryptionType RC2CBC_ENVOID = new EncryptionType( 12, "rc2CBC-EnvOID" );
    public static final EncryptionType RSAENCRYPTION_ENVOID = new EncryptionType( 13, "rsaEncryption-EnvOID" );
    public static final EncryptionType RSAES_OAEP_ENV_OID = new EncryptionType( 14, "rsaES-OAEP-ENV-OID" );
    public static final EncryptionType DES_EDE3_CBC_ENV_OID = new EncryptionType( 15, "des-ede3-cbc-Env-OID" );
    public static final EncryptionType DES3_CBC_SHA1_KD = new EncryptionType( 16, "des3-cbc-sha1-kd" );
    public static final EncryptionType AES128_CTS_HMAC_SHA1_96 = new EncryptionType( 17, "aes128-cts-hmac-sha1-96" );
    public static final EncryptionType AES256_CTS_HMAC_SHA1_96 = new EncryptionType( 18, "aes256-cts-hmac-sha1-96" );
    public static final EncryptionType RC4_HMAC = new EncryptionType( 23, "rc4-hmac" );
    public static final EncryptionType RC4_HMAC_EXP = new EncryptionType( 24, "rc4-hmac-exp" );
    public static final EncryptionType SUBKEY_KEYMATERIAL = new EncryptionType( 65, "subkey-keymaterial" );
    public static final EncryptionType RC4_MD4 = new EncryptionType( -128, "rc4-md4" );
    public static final EncryptionType RC4_HMAC_OLD = new EncryptionType( -133, "rc4-hmac-old" );
    public static final EncryptionType RC4_HMAC_OLD_EXP = new EncryptionType( -135, "rc4-hmac-old-exp" );

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final EncryptionType[] values =
        { UNKNOWN, NULL, DES_CBC_CRC, DES_CBC_MD4, DES_CBC_MD5, RESERVED4, DES3_CBC_MD5, RESERVED6, DES3_CBC_SHA1,
            DSAWITHSHA1_CMSOID, MD5WITHRSAENCRYPTION_CMSOID, SHA1WITHRSAENCRYPTION_CMSOID, RC2CBC_ENVOID,
            RSAENCRYPTION_ENVOID, RSAES_OAEP_ENV_OID, DES_EDE3_CBC_ENV_OID, DES3_CBC_SHA1_KD, AES128_CTS_HMAC_SHA1_96,
            AES256_CTS_HMAC_SHA1_96, RC4_HMAC, RC4_HMAC_EXP, SUBKEY_KEYMATERIAL, RC4_MD4, RC4_HMAC_OLD,
            RC4_HMAC_OLD_EXP };

    public static final List<EncryptionType> VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final String name;
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private EncryptionType(int ordinal, String name)
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    public static EncryptionType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == type )
            {
                return values[ii];
            }
        }

        return UNKNOWN;
    }


    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( EncryptionType ) that ).ordinal;
    }


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }
}
