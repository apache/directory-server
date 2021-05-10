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


import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.apache.directory.api.asn1.Asn1Object;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Hashed Adapter encapsulating ASN.1 cipher text engines to
 * perform encrypt() and decrypt() operations.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CipherTextHandler
{
    /** The loggers for this class */
    private static final Logger LOG_KRB = LoggerFactory.getLogger( Loggers.KERBEROS_LOG.getName() );

    /** a map of the default encryption types to the encryption engine class names */
    private static final Map<EncryptionType, Class<? extends EncryptionEngine>> DEFAULT_CIPHERS;

    // Initialize the list of encyption mechanisms
    static
    {
        EnumMap<EncryptionType, Class<? extends EncryptionEngine>> map = new EnumMap<>( EncryptionType.class );

        map.put( EncryptionType.RC4_HMAC, ArcFourHmacMd5Encryption.class );

        DEFAULT_CIPHERS = Collections.unmodifiableMap( map );
    }


    /**
     * Performs an encode and an encrypt.
     *
     * @param key The key to use for encrypting.
     * @param message The Kerberos object to encode.
     * @param usage The key usage.
     * @return The Kerberos EncryptedData.
     * @throws KerberosException if the seal failed
     */
    public EncryptedData seal( EncryptionKey key, Asn1Object message, KeyUsage usage ) throws KerberosException
    {
        try
        {
            int bufferSize = message.computeLength();
            ByteBuffer buffer = ByteBuffer.allocate( bufferSize );
            byte[] encoded = message.encode( buffer ).array();
            return encrypt( key, encoded, usage );
        }
        catch ( EncoderException ioe )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, ioe );
        }
        catch ( ClassCastException cce )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, cce );
        }
    }


    public EncryptedData encrypt( EncryptionKey key, byte[] plainText, KeyUsage usage ) throws KerberosException
    {
        EncryptionEngine engine = getEngine( key );

        return engine.getEncryptedData( key, plainText, usage );
    }


    /**
     * Decrypt a block of data.
     *
     * @param key The key used to decrypt the data
     * @param data The data to decrypt
     * @param usage The key usage number
     * @return The decrypted data as a byte[]
     * @throws KerberosException If the decoding failed
     */
    public byte[] decrypt( EncryptionKey key, EncryptedData data, KeyUsage usage ) throws KerberosException
    {
        LOG_KRB.debug( "Decrypting data using key {} and usage {}", key.getKeyType(), usage );
        EncryptionEngine engine = getEngine( key );

        return engine.getDecryptedData( key, data, usage );
    }


    private EncryptionEngine getEngine( EncryptionKey key ) throws KerberosException
    {
        EncryptionType encryptionType = key.getKeyType();

        Class<?> clazz = DEFAULT_CIPHERS.get( encryptionType );

        if ( clazz == null )
        {
            throw new KerberosException( ErrorType.KDC_ERR_ETYPE_NOSUPP );
        }

        try
        {
            return ( EncryptionEngine ) clazz.newInstance();
        }
        catch ( IllegalAccessException iae )
        {
            throw new KerberosException( ErrorType.KDC_ERR_ETYPE_NOSUPP, iae );
        }
        catch ( InstantiationException ie )
        {
            throw new KerberosException( ErrorType.KDC_ERR_ETYPE_NOSUPP, ie );
        }
    }
}
