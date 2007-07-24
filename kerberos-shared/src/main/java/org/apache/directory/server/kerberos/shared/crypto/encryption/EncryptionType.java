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


/**
 * A type-safe enumeration of Kerberos encryption types.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class EncryptionType implements Comparable<EncryptionType>
{
    /**
     * The "unknown" encryption type.
     */
    public static final EncryptionType UNKNOWN = new EncryptionType( -1, "UNKNOWN" );

    /**
     * The "null" encryption type.
     */
    public static final EncryptionType NULL = new EncryptionType( 0, "NULL" );

    /**
     * The des-cbc-crc encryption type.
     */
    public static final EncryptionType DES_CBC_CRC = new EncryptionType( 1, "des-cbc-crc" );

    /**
     * The des-cbc-md4 encryption type.
     */
    public static final EncryptionType DES_CBC_MD4 = new EncryptionType( 2, "des-cbc-md4" );

    /**
     * The des-cbc-md5 encryption type.
     */
    public static final EncryptionType DES_CBC_MD5 = new EncryptionType( 3, "des-cbc-md5" );

    /**
     * The reserved (4) encryption type.
     */
    public static final EncryptionType RESERVED4 = new EncryptionType( 4, "[reserved]" );

    /**
     * The des3-cbc-md5 encryption type.
     */
    public static final EncryptionType DES3_CBC_MD5 = new EncryptionType( 5, "des3-cbc-md5" );

    /**
     * The reserved (6) encryption type.
     */
    public static final EncryptionType RESERVED6 = new EncryptionType( 6, "[reserved]" );

    /**
     * The des3-cbc-sha1 encryption type.
     */
    public static final EncryptionType DES3_CBC_SHA1 = new EncryptionType( 7, "des3-cbc-sha1" );

    /**
     * The id-dsa-with-sha1-CmsOID encryption type.
     */
    public static final EncryptionType ID_DSA_WITH_SHA1_CMSOID = new EncryptionType( 9, "id-dsa-with-sha1-CmsOID" );

    /**
     * The md5WithRSAEncryption-CmsOID encryption type.
     */
    public static final EncryptionType MD5WITHRSAENCRYPTION_CMSOID = new EncryptionType( 10,
        "md5WithRSAEncryption-CmsOID" );

    /**
     * The sha-1WithRSAEncryption-CmsOID encryption type.
     */
    public static final EncryptionType SHA_1WITHRSAENCRYPTION_CMSOID = new EncryptionType( 11,
        "sha-1WithRSAEncryption-CmsOID" );

    /**
     * The rc2-cbc-EnvOID encryption type.
     */
    public static final EncryptionType RC2_CBC_ENVOID = new EncryptionType( 12, "rc2-cbc-EnvOID" );

    /**
     * The rsaEncryption-EnvOID encryption type.
     */
    public static final EncryptionType RSAENCRYPTION_ENVOID = new EncryptionType( 13, "rsaEncryption-EnvOID" );

    /**
     * The id-RSAES-OAEP-EnvOID encryption type.
     */
    public static final EncryptionType ID_RSAES_OAEP_ENVOID = new EncryptionType( 14, "id-RSAES-OAEP-EnvOID" );

    /**
     * The des-ede3-cbc-EnvOID encryption type.
     */
    public static final EncryptionType DES_EDE3_CBC_ENVOID = new EncryptionType( 15, "des-ede3-cbc-EnvOID" );

    /**
     * The des3-cbc-sha1-kd encryption type.
     */
    public static final EncryptionType DES3_CBC_SHA1_KD = new EncryptionType( 16, "des3-cbc-sha1-kd" );

    /**
     * The aes128-cts-hmac-sha1-96 encryption type.
     */
    public static final EncryptionType AES128_CTS_HMAC_SHA1_96 = new EncryptionType( 17, "aes128-cts-hmac-sha1-96" );

    /**
     * The aes256-cts-hmac-sha1-96 encryption type.
     */
    public static final EncryptionType AES256_CTS_HMAC_SHA1_96 = new EncryptionType( 18, "aes256-cts-hmac-sha1-96" );

    /**
     * The rc4-hmac encryption type.
     */
    public static final EncryptionType RC4_HMAC = new EncryptionType( 23, "rc4-hmac" );

    /**
     * The rc4-hmac-exp encryption type.
     */
    public static final EncryptionType RC4_HMAC_EXP = new EncryptionType( 24, "rc4-hmac-exp" );

    /**
     * The subkey-keymaterial encryption type.
     */
    public static final EncryptionType SUBKEY_KEYMATERIAL = new EncryptionType( 65, "subkey-keymaterial" );

    /**
     * The rc4-md4 encryption type.
     */
    public static final EncryptionType RC4_MD4 = new EncryptionType( -128, "rc4-md4" );

    /**
     * The c4-hmac-old encryption type.
     */
    public static final EncryptionType RC4_HMAC_OLD = new EncryptionType( -133, "rc4-hmac-old" );

    /**
     * The rc4-hmac-old-exp encryption type.
     */
    public static final EncryptionType RC4_HMAC_OLD_EXP = new EncryptionType( -135, "rc4-hmac-old-exp" );

    /**
     * Array for building a List of VALUES.
     */
    private static final EncryptionType[] values =
        { UNKNOWN, NULL, DES_CBC_CRC, DES_CBC_MD4, DES_CBC_MD5, RESERVED4, DES3_CBC_MD5, RESERVED6, DES3_CBC_SHA1,
            ID_DSA_WITH_SHA1_CMSOID, MD5WITHRSAENCRYPTION_CMSOID, SHA_1WITHRSAENCRYPTION_CMSOID, RC2_CBC_ENVOID,
            RSAENCRYPTION_ENVOID, ID_RSAES_OAEP_ENVOID, DES_EDE3_CBC_ENVOID, DES3_CBC_SHA1_KD, AES128_CTS_HMAC_SHA1_96,
            AES256_CTS_HMAC_SHA1_96, RC4_HMAC, RC4_HMAC_EXP, SUBKEY_KEYMATERIAL, RC4_MD4, RC4_HMAC_OLD,
            RC4_HMAC_OLD_EXP };

    /**
     * A List of all the encryption type constants.
     */
    public static final List<EncryptionType> VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The name of the encryption type.
     */
    private final String name;

    /**
     * The value/code for the encryption type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private EncryptionType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the encryption type when specified by its ordinal.
     *
     * @param type
     * @return The encryption type.
     */
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


    /**
     * Returns the number associated with this encryption type.
     *
     * @return The encryption type number.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    /**
     * Returns the name associated with this encryption type.
     *
     * @return The name.
     */
    public String getName()
    {
        return name;
    }


    public int compareTo( EncryptionType that )
    {
        return ordinal - that.ordinal;
    }


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }
}
