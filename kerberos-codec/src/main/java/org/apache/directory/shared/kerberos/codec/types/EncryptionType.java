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
package org.apache.directory.shared.kerberos.codec.types;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.util.Strings;


/**
 * A type-safe enumeration of Kerberos encryption types.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum EncryptionType
{
    /**
     * The "unknown" encryption type.
     */
    UNKNOWN(-1, "UNKNOWN"),

    /**
     * The "null" encryption type.
     */
    NULL(0, "null"),

    /**
     * The des-cbc-crc encryption type.
     */
    DES_CBC_CRC(1, "des-cbc-crc"),

    /**
     * The des-cbc-md4 encryption type.
     */
    DES_CBC_MD4(2, "des-cbc-md4"),

    /**
     * The des-cbc-md5 encryption type.
     */
    DES_CBC_MD5(3, "des-cbc-md5"),

    /**
     * The reserved (4) encryption type.
     */
    RESERVED4(4, "[reserved]"),

    /**
     * The des3-cbc-md5 encryption type.
     */
    DES3_CBC_MD5(5, "des3-cbc-md5"),

    /**
     * The reserved (6) encryption type.
     */
    RESERVED6(6, "[reserved]"),

    /**
     * The des3-cbc-sha1 encryption type.
     */
    DES3_CBC_SHA1(7, "des3-cbc-sha1"),

    /**
     * The dsaWithSHA1-CmsOID encryption type.
     */
    DSAWITHSHA1_CMSOID(9, "dsaWithSHA1-CmsOID"),

    /**
     * The md5WithRSAEncryption-CmsOID encryption type.
     */
    MD5WITHRSAENCRYPTION_CMSOID(10, "md5WithRSAEncryption-CmsOID"),

    /**
     * The sha1WithRSAEncryption-CmsOID encryption type.
     */
    SHA1WITHRSAENCRYPTION_CMSOID(11, "sha1WithRSAEncryption-CmsOID"),

    /**
     * The rc2CBC-EnvOID encryption type.
     */
    RC2CBC_ENVOID(12, "rc2CBC-EnvOID"),

    /**
     * The rsaEncryption-EnvOID encryption type.
     */
    RSAENCRYPTION_ENVOID(13, "rsaEncryption-EnvOID"),

    /**
     * The rsaES-OAEP-ENV-OID encryption type.
     */
    RSAES_OAEP_ENV_OID(14, "rsaES-OAEP-ENV-OID"),

    /**
     * The des-ede3-cbc-Env-OID encryption type.
     */
    DES_EDE3_CBC_ENV_OID(15, "des-ede3-cbc-Env-OID"),

    /**
     * The des3-cbc-sha1-kd encryption type.
     */
    DES3_CBC_SHA1_KD(16, "des3-cbc-sha1-kd"),

    /**
     * The aes128-cts-hmac-sha1-96 encryption type.
     */
    AES128_CTS_HMAC_SHA1_96(17, "aes128-cts-hmac-sha1-96"),

    /**
     * The aes256-cts-hmac-sha1-96 encryption type.
     */
    AES256_CTS_HMAC_SHA1_96(18, "aes256-cts-hmac-sha1-96"),

    /**
     * The rc4-hmac encryption type.
     */
    RC4_HMAC(23, "rc4-hmac"),

    /**
     * The rc4-hmac-exp encryption type.
     */
    RC4_HMAC_EXP(24, "rc4-hmac-exp"),

    /**
     * The subkey-keymaterial encryption type.
     */
    SUBKEY_KEYMATERIAL(65, "subkey-keymaterial"),

    /**
     * The rc4-md4 encryption type.
     */
    RC4_MD4(-128, "rc4-md4"),

    /**
     * The c4-hmac-old encryption type.
     */
    RC4_HMAC_OLD(-133, "rc4-hmac-old"),

    /**
     * The rc4-hmac-old-exp encryption type.
     */
    RC4_HMAC_OLD_EXP(-135, "rc4-hmac-old-exp");

    /**
     * The value/code for the encryption type.
     */
    private final int value;

    /**
     * The name
     */
    private final String name;

    /** A map containing all the values */
    private static Map<String, EncryptionType> encryptionTypesByName = new HashMap<String, EncryptionType>();

    /** A map containing all the values */
    private static Map<Integer, EncryptionType> encryptionTypesByValue = new HashMap<Integer, EncryptionType>();

    /** Initialization of the previous map */
    static
    {
        for ( EncryptionType type : EncryptionType.values() )
        {
            encryptionTypesByName.put( Strings.toLowerCase( type.getName() ), type );
            encryptionTypesByValue.put( type.getValue(), type );
        }
    }


    /**
     * Private constructor prevents construction outside of this class.
     */
    private EncryptionType( int value, String name )
    {
        this.value = value;
        this.name = name;
    }


    /**
     * Get all the encryption types
     *
     * @return A set of encryption types.
     */
    public static Collection<EncryptionType> getEncryptionTypes()
    {
        return encryptionTypesByName.values();
    }


    /**
     * Returns the encryption type when specified by its value.
     *
     * @param type
     * @return The encryption type.
     */
    public static EncryptionType getTypeByValue( int type )
    {
        if ( encryptionTypesByValue.containsKey( type ) )
        {
            return encryptionTypesByValue.get( type );
        }
        else
        {
            return UNKNOWN;
        }
    }


    /**
     * Returns the number associated with this encryption type.
     *
     * @return The encryption type number.
     */
    public int getValue()
    {
        return value;
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


    /**
     * Get the EncryptionType given a String.
     * @param type The encryption string we want to find
     * @return The found EncryptionType, or UNKNOWN
     */
    public static EncryptionType getByName( String type )
    {
        if ( type == null )
        {
            return UNKNOWN;
        }

        String lcType = Strings.toLowerCase( type );

        if ( encryptionTypesByName.containsKey( lcType ) )
        {
            return encryptionTypesByName.get( lcType );
        }
        else
        {
            return UNKNOWN;
        }
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return getName() + " (" + value + ")";
    }
}
