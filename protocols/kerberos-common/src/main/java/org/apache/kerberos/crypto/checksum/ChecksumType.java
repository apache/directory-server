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
package org.apache.kerberos.crypto.checksum;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ChecksumType implements Comparable
{
	/**
	 * Enumeration elements are constructed once upon class loading.
	 * Order of appearance here determines the order of compareTo.
	 */
    public static final ChecksumType UNKNOWN        = new ChecksumType(-1, "UNKNOWN");
    public static final ChecksumType NULL           = new ChecksumType(0, "NULL");
    public static final ChecksumType CRC32          = new ChecksumType(1, "CRC32");
    public static final ChecksumType RSA_MD4        = new ChecksumType(2, "rsa-md4");
    public static final ChecksumType RSA_MD4_DES    = new ChecksumType(3, "rsa-md4-des");
    public static final ChecksumType DES_MAC        = new ChecksumType(4, "des-mac");
    public static final ChecksumType DES_MAC_K      = new ChecksumType(5, "des-mac-k");
    public static final ChecksumType RSA_MD4_DES_K  = new ChecksumType(6, "rsa-md4-des-k");
    public static final ChecksumType RSA_MD5        = new ChecksumType(7, "rsa-md5");
    public static final ChecksumType RSA_MD5_DES    = new ChecksumType(8, "rsa-md5-des");
    public static final ChecksumType RSA_MD5_DES3   = new ChecksumType(9, "rsa-md5-des3");
    public static final ChecksumType SHA1           = new ChecksumType(10, "sha1 (unkeyed)");
    public static final ChecksumType HMAC_SHA1_DES3_KD   = new ChecksumType(12, "hmac-sha1-des3-kd");
    public static final ChecksumType HMAC_SHA1_DES3      = new ChecksumType(13, "hmac-sha1-des3");
    public static final ChecksumType SHA1_2              = new ChecksumType(14, "sha1 (unkeyed)");
    public static final ChecksumType HMAC_SHA1_96_AES128 = new ChecksumType(15, "hmac-sha1-96-aes128");
    public static final ChecksumType HMAC_SHA1_96_AES256 = new ChecksumType(16, "hmac-sha1-96-aes256");

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final ChecksumType[] values = { UNKNOWN, NULL, CRC32, RSA_MD4, RSA_MD4_DES, DES_MAC, DES_MAC_K,
            RSA_MD4_DES_K, RSA_MD5, RSA_MD5_DES, RSA_MD5_DES3, SHA1, HMAC_SHA1_DES3_KD, HMAC_SHA1_DES3, SHA1_2,
            HMAC_SHA1_96_AES128, HMAC_SHA1_96_AES256 };
    // VALUES needs to be located here, otherwise illegal forward reference
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final String name;
    private final int ordinal;

    /**
     * Private constructor prevents construction outside of this class.
     */
    private ChecksumType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }

    public static ChecksumType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ ii ].ordinal == type )
            {
                return values[ ii ];
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
        return ordinal - ( (ChecksumType) that ).ordinal;
    }

    public String toString()
    {
        return name;
    }
}
