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


import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;


/**
 * Generates new random keys, suitable for use as session keys.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RandomKeyFactory
{
    /** a map of the default encryption types to the encryption engine class names */
    private static final Map<EncryptionType, String> DEFAULT_CIPHERS;

    static
    {
        Map<EncryptionType, String> map = new HashMap<EncryptionType, String>();

        map.put( EncryptionType.DES_CBC_MD5, "DES" );
        map.put( EncryptionType.DES3_CBC_SHA1_KD, "DESede" );
        map.put( EncryptionType.RC4_HMAC, "RC4" );
        map.put( EncryptionType.AES128_CTS_HMAC_SHA1_96, "AES" );
        map.put( EncryptionType.AES256_CTS_HMAC_SHA1_96, "AES" );

        DEFAULT_CIPHERS = Collections.unmodifiableMap( map );
    }


    /**
     * Get a new random session key for a given {@link EncryptionType}.
     * 
     * @param encryptionType 
     * 
     * @return The new random session key.
     * @throws KerberosException 
     */
    public static EncryptionKey getRandomKey( EncryptionType encryptionType ) throws KerberosException
    {
        String algorithm = DEFAULT_CIPHERS.get( encryptionType );

        if ( algorithm == null )
        {
            throw new KerberosException( ErrorType.KDC_ERR_ETYPE_NOSUPP, encryptionType.getName()
                + " is not a supported encryption type." );
        }

        try
        {
            KeyGenerator keyGenerator = KeyGenerator.getInstance( algorithm );

            if ( encryptionType.equals( EncryptionType.AES128_CTS_HMAC_SHA1_96 ) )
            {
                keyGenerator.init( 128 );
            }

            if ( encryptionType.equals( EncryptionType.AES256_CTS_HMAC_SHA1_96 ) )
            {
                keyGenerator.init( 256 );
            }

            SecretKey key = keyGenerator.generateKey();

            byte[] keyBytes = key.getEncoded();

            return new EncryptionKey( encryptionType, keyBytes );
        }
        catch ( NoSuchAlgorithmException nsae )
        {
            throw new KerberosException( ErrorType.KDC_ERR_ETYPE_NOSUPP, nsae.getMessage() );
        }
    }
}
