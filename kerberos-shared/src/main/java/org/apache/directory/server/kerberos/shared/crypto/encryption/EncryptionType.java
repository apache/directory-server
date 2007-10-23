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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * A type-safe enumeration of Kerberos encryption types.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum EncryptionType
{
    /**
     * The "unknown" encryption type.
     */
    UNKNOWN( -1 ),

    /**
     * The "null" encryption type.
     */
    NULL( 0 ),

    /**
     * The des-cbc-crc encryption type.
     */
    DES_CBC_CRC( 1 ),

    /**
     * The des-cbc-md4 encryption type.
     */
    DES_CBC_MD4( 2 ),

    /**
     * The des-cbc-md5 encryption type.
     */
    DES_CBC_MD5( 3 ),

    /**
     * The reserved (4) encryption type.
     */
    RESERVED4( 4 ),

    /**
     * The des3-cbc-md5 encryption type.
     */
    DES3_CBC_MD5( 5 ),

    /**
     * The reserved (6) encryption type.
     */
    RESERVED6( 6 ),

    /**
     * The des3-cbc-sha1 encryption type.
     */
    DES3_CBC_SHA1( 7 ),

    /**
     * The dsaWithSHA1-CmsOID encryption type.
     */
    DSAWITHSHA1_CMSOID( 9 ),

    /**
     * The md5WithRSAEncryption-CmsOID encryption type.
     */
    MD5WITHRSAENCRYPTION_CMSOID( 10 ),

    /**
     * The sha1WithRSAEncryption-CmsOID encryption type.
     */
    SHA1WITHRSAENCRYPTION_CMSOID( 11 ),

    /**
     * The rc2CBC-EnvOID encryption type.
     */
    RC2CBC_ENVOID( 12 ),

    /**
     * The rsaEncryption-EnvOID encryption type.
     */
    RSAENCRYPTION_ENVOID( 13 ),

    /**
     * The rsaES-OAEP-ENV-OID encryption type.
     */
    RSAES_OAEP_ENV_OID( 14 ),

    /**
     * The des-ede3-cbc-Env-OID encryption type.
     */
    DES_EDE3_CBC_ENV_OID( 15 ),

    /**
     * The des3-cbc-sha1-kd encryption type.
     */
    DES3_CBC_SHA1_KD( 16 ),

    /**
     * The aes128-cts-hmac-sha1-96 encryption type.
     */
    AES128_CTS_HMAC_SHA1_96( 17 ),

    /**
     * The aes256-cts-hmac-sha1-96 encryption type.
     */
    AES256_CTS_HMAC_SHA1_96( 18 ),

    /**
     * The rc4-hmac encryption type.
     */
    RC4_HMAC( 23 ),

    /**
     * The rc4-hmac-exp encryption type.
     */
    RC4_HMAC_EXP( 24 ),

    /**
     * The subkey-keymaterial encryption type.
     */
    SUBKEY_KEYMATERIAL( 65 ),

    /**
     * The rc4-md4 encryption type.
     */
    RC4_MD4( -128 ),

    /**
     * The c4-hmac-old encryption type.
     */
    RC4_HMAC_OLD( -133 ),

    /**
     * The rc4-hmac-old-exp encryption type.
     */
    RC4_HMAC_OLD_EXP( -135 );

    /**
     * The value/code for the encryption type.
     */
    private final int ordinal;

    /** A map containing all the values */
    private static Map<String, EncryptionType> encryptionTypes = new HashMap<String, EncryptionType>();
    
    /** Initialization of the previous map */
    static
    {
        encryptionTypes.put( "null", NULL );
        encryptionTypes.put( "des-cbc-crc", DES_CBC_CRC ); 
        encryptionTypes.put( "des-cbc-md4", DES_CBC_MD4 );          
        encryptionTypes.put( "des-cbc-md5", DES_CBC_MD5 );          
        encryptionTypes.put( "[reserved]", RESERVED4 );         
        encryptionTypes.put( "des3-cbc-md5", DES3_CBC_MD5 );            
        encryptionTypes.put( "[reserved]", RESERVED6 );         
        encryptionTypes.put( "des3-cbc-sha1", DES3_CBC_SHA1 );          
        encryptionTypes.put( "dsaWithSHA1-CmsOID", DSAWITHSHA1_CMSOID );            
        encryptionTypes.put( "md5WithRSAEncryption-CmsOID", MD5WITHRSAENCRYPTION_CMSOID );          
        encryptionTypes.put( "sha1WithRSAEncryption-CmsOID", SHA1WITHRSAENCRYPTION_CMSOID );            
        encryptionTypes.put( "rc2CBC-EnvOID", RC2CBC_ENVOID );          
        encryptionTypes.put( "rsaEncryption-EnvOID", RSAENCRYPTION_ENVOID );            
        encryptionTypes.put( "rsaES-OAEP-ENV-OID", RSAES_OAEP_ENV_OID );        
        encryptionTypes.put( "des-ede3-cbc-Env-OID", DES_EDE3_CBC_ENV_OID );            
        encryptionTypes.put( "des3-cbc-sha1-kd", DES3_CBC_SHA1_KD );        
        encryptionTypes.put( "aes128-cts-hmac-sha1-96", AES128_CTS_HMAC_SHA1_96 );          
        encryptionTypes.put( "aes256-cts-hmac-sha1-96", AES256_CTS_HMAC_SHA1_96 );          
        encryptionTypes.put( "rc4-hmac", RC4_HMAC );            
        encryptionTypes.put( "rc4-hmac-exp", RC4_HMAC_EXP );            
        encryptionTypes.put( "subkey-keymaterial", SUBKEY_KEYMATERIAL );            
        encryptionTypes.put( "rc4-md4", RC4_MD4 );      
        encryptionTypes.put( "rc4-hmac-old", RC4_HMAC_OLD );            
        encryptionTypes.put( "rc4-hmac-old-exp", RC4_HMAC_OLD_EXP );            
        encryptionTypes.put( "UNKNOWN", UNKNOWN );
    }

    /**
     * Private constructor prevents construction outside of this class.
     */
    private EncryptionType( int ordinal )
    {
        this.ordinal = ordinal;
    }

    
    /**
     * Get all the encryption types
     *
     * @return A set of encryption types.
     */
    public static Collection<EncryptionType> getEncryptionTypes()
    {
        return encryptionTypes.values();
    }

    /**
     * Returns the encryption type when specified by its ordinal.
     *
     * @param type
     * @return The encryption type.
     */
    public static EncryptionType getTypeByOrdinal( int type )
    {
        switch ( type )
        {
            case 0 : return NULL; 
            case 1 : return DES_CBC_CRC; 
            case 2 : return DES_CBC_MD4; 
            case 3 : return DES_CBC_MD5; 
            case 4 : return RESERVED4; 
            case 5 : return DES3_CBC_MD5; 
            case 6 : return RESERVED6; 
            case 7 : return DES3_CBC_SHA1; 
            case 9 : return DSAWITHSHA1_CMSOID; 
            case 10 : return MD5WITHRSAENCRYPTION_CMSOID; 
            case 11 : return SHA1WITHRSAENCRYPTION_CMSOID; 
            case 12 : return RC2CBC_ENVOID; 
            case 13 : return RSAENCRYPTION_ENVOID; 
            case 14 : return RSAES_OAEP_ENV_OID; 
            case 15 : return DES_EDE3_CBC_ENV_OID; 
            case 16 : return DES3_CBC_SHA1_KD; 
            case 17 : return AES128_CTS_HMAC_SHA1_96; 
            case 18 : return AES256_CTS_HMAC_SHA1_96; 
            case 23 : return RC4_HMAC; 
            case 24 : return RC4_HMAC_EXP; 
            case 65 : return SUBKEY_KEYMATERIAL; 
            case -128 : return RC4_MD4; 
            case -133 : return RC4_HMAC_OLD; 
            case -135 : return RC4_HMAC_OLD_EXP; 
            default : return UNKNOWN; 
        }
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
        switch (this )
        {
            case NULL                           : return "NULL"; 
            case DES_CBC_CRC                    : return "des-cbc-crc"; 
            case DES_CBC_MD4                    : return "des-cbc-md4";          
            case DES_CBC_MD5                    : return "des-cbc-md5";          
            case RESERVED4                      : return "[reserved]";           
            case DES3_CBC_MD5                   : return "des3-cbc-md5";         
            case RESERVED6                      : return "[reserved]";           
            case DES3_CBC_SHA1                  : return "des3-cbc-sha1";            
            case DSAWITHSHA1_CMSOID             : return "dsaWithSHA1-CmsOID";           
            case MD5WITHRSAENCRYPTION_CMSOID    : return "md5WithRSAEncryption-CmsOID";          
            case SHA1WITHRSAENCRYPTION_CMSOID   : return "sha1WithRSAEncryption-CmsOID";         
            case RC2CBC_ENVOID                  : return "rc2CBC-EnvOID";            
            case RSAENCRYPTION_ENVOID           : return "rsaEncryption-EnvOID";         
            case RSAES_OAEP_ENV_OID             : return "rsaES-OAEP-ENV-OID";       
            case DES_EDE3_CBC_ENV_OID           : return "des-ede3-cbc-Env-OID";         
            case DES3_CBC_SHA1_KD               : return "des3-cbc-sha1-kd";     
            case AES128_CTS_HMAC_SHA1_96        : return "aes128-cts-hmac-sha1-96";          
            case AES256_CTS_HMAC_SHA1_96        : return "aes256-cts-hmac-sha1-96";          
            case RC4_HMAC                       : return "rc4-hmac";         
            case RC4_HMAC_EXP                   : return "rc4-hmac-exp";         
            case SUBKEY_KEYMATERIAL             : return "subkey-keymaterial";           
            case RC4_MD4                        : return "rc4-md4";      
            case RC4_HMAC_OLD                   : return "rc4-hmac-old";         
            case RC4_HMAC_OLD_EXP               : return "rc4-hmac-old-exp";         
            case UNKNOWN                        : return "UNKNOWN";
            default                             : return "UNKNOWN";
        }
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
        
        String lcType = type.toLowerCase();
        
        if ( encryptionTypes.containsKey( lcType ) )
        {
            return encryptionTypes.get( lcType );
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
        return getName() + " (" + ordinal + ")";
    }
}
