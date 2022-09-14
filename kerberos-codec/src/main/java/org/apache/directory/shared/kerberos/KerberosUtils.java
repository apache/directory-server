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
package org.apache.directory.shared.kerberos;


import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES3_CBC_MD5;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES3_CBC_SHA1;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES3_CBC_SHA1_KD;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES_CBC_CRC;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES_CBC_MD4;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES_CBC_MD5;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES_EDE3_CBC_ENV_OID;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DSAWITHSHA1_CMSOID;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.MD5WITHRSAENCRYPTION_CMSOID;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.RC2CBC_ENVOID;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.RC4_HMAC;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.RSAENCRYPTION_ENVOID;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.RSAES_OAEP_ENV_OID;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.SHA1WITHRSAENCRYPTION_CMSOID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.api.util.Strings;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;


/**
 * An utility class for Kerberos.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosUtils
{
    /** A constant for integer optional values */
    public static final int NULL = -1;

    /** An empty list of principal names */
    public static final List<String> EMPTY_PRINCIPAL_NAME = new ArrayList<>();

    /** 
     * an order preserved map containing cipher names to the corresponding algorithm 
     * names in the descending order of strength
     */
    private static final Map<String, String> cipherAlgoMap = new LinkedHashMap<>();

    private static final Set<EncryptionType> oldEncTypes = new HashSet<>();

    static
    {
        cipherAlgoMap.put( "rc4", "ArcFourHmac" );
        cipherAlgoMap.put( "aes256", "AES256" );
        cipherAlgoMap.put( "aes128", "AES128" );
        cipherAlgoMap.put( "des3", "DESede" );
        cipherAlgoMap.put( "des", "DES" );

        oldEncTypes.add( DES_CBC_CRC );
        oldEncTypes.add( DES_CBC_MD4 );
        oldEncTypes.add( DES_CBC_MD5 );
        oldEncTypes.add( DES_EDE3_CBC_ENV_OID );
        oldEncTypes.add( DES3_CBC_MD5 );
        oldEncTypes.add( DES3_CBC_SHA1 );
        oldEncTypes.add( DES3_CBC_SHA1_KD );
        oldEncTypes.add( DSAWITHSHA1_CMSOID );
        oldEncTypes.add( MD5WITHRSAENCRYPTION_CMSOID );
        oldEncTypes.add( SHA1WITHRSAENCRYPTION_CMSOID );
        oldEncTypes.add( RC2CBC_ENVOID );
        oldEncTypes.add( RSAENCRYPTION_ENVOID );
        oldEncTypes.add( RSAES_OAEP_ENV_OID );
        oldEncTypes.add( RC4_HMAC );
    }


    public static boolean isKerberosString( byte[] value )
    {
        if ( value == null )
        {
            return false;
        }

        for ( byte b : value )
        {
            if ( ( b < 0x20 ) || ( b > 0x7E ) )
            {
                return false;
            }
        }

        return true;
    }


    public static String getAlgoNameFromEncType( EncryptionType encType )
    {
        String cipherName = Strings.toLowerCaseAscii( encType.getName() );

        for ( Map.Entry<String, String> entry : cipherAlgoMap.entrySet() )
        {
            if ( cipherName.startsWith( entry.getKey() ) )
            {
                return entry.getValue();
            }
        }

        throw new IllegalArgumentException( "Unknown algorithm name for the encryption type " + encType );
    }
}
