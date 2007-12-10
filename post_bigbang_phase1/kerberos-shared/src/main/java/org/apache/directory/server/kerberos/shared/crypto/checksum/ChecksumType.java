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


/**
 * A type-safe enumeration of Kerberos checksum types.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum ChecksumType implements Comparable<ChecksumType>
{
    /**
     * The "unknown" checksum type.
     */
    UNKNOWN( -1 ),

    /**
     * The "null" checksum type.
     */
    NULL( 0 ),

    /**
     * The CRC32 checksum type.
     */
    CRC32( 1 ),

    /**
     * The rsa-md4 checksum type.
     */
    RSA_MD4( 2 ),

    /**
     * The rsa-md4-des checksum type.
     */
    RSA_MD4_DES( 3 ),

    /**
     * The des-mac checksum type.
     */
    DES_MAC( 4 ),

    /**
     * The des-mac-k checksum type.
     */
    DES_MAC_K( 5 ),

    /**
     * The rsa-md4-des-k checksum type.
     */
    RSA_MD4_DES_K( 6 ),

    /**
     * The rsa-md5 checksum type.
     */
    RSA_MD5( 7 ),

    /**
     * The rsa-md5-des checksum type.
     */
    RSA_MD5_DES( 8 ),

    /**
     * The rsa-md5-des3 checksum type.
     */
    RSA_MD5_DES3( 9 ),

    /**
     * The sha1 (unkeyed) checksum type.
     */
    SHA1( 10 ),

    /**
     * The hmac-sha1-des3-kd checksum type.
     */
    HMAC_SHA1_DES3_KD( 12 ),

    /**
     * The hmac-sha1-des3 checksum type.
     */
    HMAC_SHA1_DES3( 13 ),

    /**
     * The sha1 (unkeyed) checksum type.
     */
    SHA1_2 ( 14 ),

    /**
     * The hmac-sha1-96-aes128 checksum type.
     */
    HMAC_SHA1_96_AES128( 15 ),

    /**
     * The hmac-sha1-96-aes256 checksum type.
     */
    HMAC_SHA1_96_AES256( 16 ),

    /**
     * The hmac-md5 checksum type.
     */
    HMAC_MD5( -138 );


    /**
     * The value/code for the checksum type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private ChecksumType( int ordinal )
    {
        this.ordinal = ordinal;
    }


    /**
     * Returns the checksum type when specified by its ordinal.
     *
     * @param type
     * @return The checksum type.
     */
    public static ChecksumType getTypeByOrdinal( int type )
    {
        switch ( type )
        {
            case -1 : return UNKNOWN;
            case 0  : return NULL;
            case 1  : return CRC32;
            case 2  : return RSA_MD4;
            case 3  : return RSA_MD4_DES;
            case 4  : return DES_MAC;
            case 5  : return DES_MAC_K;
            case 6  : return RSA_MD4_DES_K;
            case 7  : return RSA_MD5;
            case 8  : return RSA_MD5_DES;
            case 9  : return RSA_MD5_DES3;
            case 10 : return SHA1;
            case 12 : return HMAC_SHA1_DES3_KD;
            case 13 : return HMAC_SHA1_DES3;
            case 14 : return SHA1_2;
            case 15 : return HMAC_SHA1_96_AES128;
            case 16 : return HMAC_SHA1_96_AES256;
            case -138 : return HMAC_MD5;
            default : return UNKNOWN;
        }
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
}
