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
package org.apache.directory.server.kerberos.shared.crypto.checksum;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * A type-safe enumeration of Kerberos checksum types.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class ChecksumType implements Comparable<ChecksumType>
{
    /**
     * The "unknown" checksum type.
     */
    public static final ChecksumType UNKNOWN = new ChecksumType( -1, "UNKNOWN" );

    /**
     * The "null" checksum type.
     */
    public static final ChecksumType NULL = new ChecksumType( 0, "NULL" );

    /**
     * The CRC32 checksum type.
     */
    public static final ChecksumType CRC32 = new ChecksumType( 1, "CRC32" );

    /**
     * The rsa-md4 checksum type.
     */
    public static final ChecksumType RSA_MD4 = new ChecksumType( 2, "rsa-md4" );

    /**
     * The rsa-md4-des checksum type.
     */
    public static final ChecksumType RSA_MD4_DES = new ChecksumType( 3, "rsa-md4-des" );

    /**
     * The des-mac checksum type.
     */
    public static final ChecksumType DES_MAC = new ChecksumType( 4, "des-mac" );

    /**
     * The des-mac-k checksum type.
     */
    public static final ChecksumType DES_MAC_K = new ChecksumType( 5, "des-mac-k" );

    /**
     * The rsa-md4-des-k checksum type.
     */
    public static final ChecksumType RSA_MD4_DES_K = new ChecksumType( 6, "rsa-md4-des-k" );

    /**
     * The rsa-md5 checksum type.
     */
    public static final ChecksumType RSA_MD5 = new ChecksumType( 7, "rsa-md5" );

    /**
     * The rsa-md5-des checksum type.
     */
    public static final ChecksumType RSA_MD5_DES = new ChecksumType( 8, "rsa-md5-des" );

    /**
     * The rsa-md5-des3 checksum type.
     */
    public static final ChecksumType RSA_MD5_DES3 = new ChecksumType( 9, "rsa-md5-des3" );

    /**
     * The sha1 (unkeyed) checksum type.
     */
    public static final ChecksumType SHA1 = new ChecksumType( 10, "sha1 (unkeyed)" );

    /**
     * The hmac-sha1-des3-kd checksum type.
     */
    public static final ChecksumType HMAC_SHA1_DES3_KD = new ChecksumType( 12, "hmac-sha1-des3-kd" );

    /**
     * The hmac-sha1-des3 checksum type.
     */
    public static final ChecksumType HMAC_SHA1_DES3 = new ChecksumType( 13, "hmac-sha1-des3" );

    /**
     * The sha1 (unkeyed) checksum type.
     */
    public static final ChecksumType SHA1_2 = new ChecksumType( 14, "sha1 (unkeyed)" );

    /**
     * The hmac-sha1-96-aes128 checksum type.
     */
    public static final ChecksumType HMAC_SHA1_96_AES128 = new ChecksumType( 15, "hmac-sha1-96-aes128" );

    /**
     * The hmac-sha1-96-aes256 checksum type.
     */
    public static final ChecksumType HMAC_SHA1_96_AES256 = new ChecksumType( 16, "hmac-sha1-96-aes256" );

    /**
     * The hmac-md5 checksum type.
     */
    public static final ChecksumType HMAC_MD5 = new ChecksumType( -138, "hmac-md5" );

    /**
     * Array for building a List of VALUES.
     */
    private static final ChecksumType[] values =
        { UNKNOWN, NULL, CRC32, RSA_MD4, RSA_MD4_DES, DES_MAC, DES_MAC_K, RSA_MD4_DES_K, RSA_MD5, RSA_MD5_DES,
            RSA_MD5_DES3, SHA1, HMAC_SHA1_DES3_KD, HMAC_SHA1_DES3, SHA1_2, HMAC_SHA1_96_AES128, HMAC_SHA1_96_AES256,
            HMAC_MD5 };

    /**
     * A List of all the checksum type constants.
     */
    public static final List<ChecksumType> VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The name of the checksum type.
     */
    private final String name;

    /**
     * The value/code for the checksum type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private ChecksumType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the checksum type when specified by its ordinal.
     *
     * @param type
     * @return The checksum type.
     */
    public static ChecksumType getTypeByOrdinal( int type )
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
     * Returns the number associated with this checksum type.
     *
     * @return The checksum type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( ChecksumType that )
    {
        return ordinal - that.ordinal;
    }


    public String toString()
    {
        return name;
    }
}
