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


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.io.decoder.Decoder;
import org.apache.directory.server.kerberos.shared.io.decoder.DecoderFactory;
import org.apache.directory.server.kerberos.shared.io.decoder.EncApRepPartDecoder;
import org.apache.directory.server.kerberos.shared.io.decoder.EncKdcRepPartDecoder;
import org.apache.directory.server.kerberos.shared.io.decoder.EncKrbPrivPartDecoder;
import org.apache.directory.server.kerberos.shared.io.decoder.EncTicketPartDecoder;
import org.apache.directory.server.kerberos.shared.io.decoder.EncryptedTimestampDecoder;
import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.server.kerberos.shared.messages.components.EncKrbPrivPart;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.AuthorizationData;
import org.apache.directory.shared.kerberos.components.EncKdcRepPart;
import org.apache.directory.shared.kerberos.components.EncTicketPart;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.PaEncTsEnc;
import org.apache.directory.shared.kerberos.messages.Authenticator;
import org.apache.directory.shared.kerberos.messages.EncApRepPart;


/**
 * A Hashed Adapter encapsulating ASN.1 encoders and decoders and cipher text engines to
 * perform seal() and unseal() operations.  A seal() operation performs an encode and an
 * encrypt, while an unseal() operation performs a decrypt and a decode.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CipherTextHandler
{
    /** a map of the default encodable class names to the decoder class names */
    private static final Map DEFAULT_DECODERS;
    
    /** a map of the default encryption types to the encryption engine class names */
    private static final Map DEFAULT_CIPHERS;

    static
    {
        Map<Class, Class> map = new HashMap<Class, Class>();

        map.put( EncTicketPart.class, EncTicketPartDecoder.class );
        map.put( Authenticator.class, AuthenticatorDecoder.class );
        map.put( PaEncTsEnc.class, EncryptedTimestampDecoder.class );
        map.put( AuthorizationData.class, AuthorizationDataDecoder.class );
        map.put( EncKrbPrivPart.class, EncKrbPrivPartDecoder.class );
        map.put( EncApRepPart.class, EncApRepPartDecoder.class );
        map.put( EncKdcRepPart.class, EncKdcRepPartDecoder.class );

        DEFAULT_DECODERS = Collections.unmodifiableMap( map );
    }

    static
    {
        Map<EncryptionType, Class> map = new HashMap<EncryptionType, Class>();

        map.put( EncryptionType.DES_CBC_MD5, DesCbcMd5Encryption.class );
        map.put( EncryptionType.DES3_CBC_SHA1_KD, Des3CbcSha1KdEncryption.class );
        map.put( EncryptionType.AES128_CTS_HMAC_SHA1_96, Aes128CtsSha1Encryption.class );
        map.put( EncryptionType.AES256_CTS_HMAC_SHA1_96, Aes256CtsSha1Encryption.class );
        map.put( EncryptionType.RC4_HMAC, ArcFourHmacMd5Encryption.class );

        DEFAULT_CIPHERS = Collections.unmodifiableMap( map );
    }


    /**
     * Performs an encode and an encrypt.
     *
     * @param key The key to use for encrypting.
     * @param encodable The Kerberos object to encode.
     * @param usage The key usage.
     * @return The Kerberos EncryptedData.
     * @throws KerberosException
     */
    public EncryptedData seal( EncryptionKey key, AbstractAsn1Object message, KeyUsage usage ) throws KerberosException
    {
        try
        {
            int bufferSize = message.computeLength();
            ByteBuffer buffer = ByteBuffer.allocate( bufferSize );
            return encrypt( key, message.encode( buffer ).array(), usage );
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


    /**
     * Perform a decrypt and a decode.
     *
     * @param hint The class the encrypted data is expected to contain.
     * @param key The key to use for decryption.
     * @param data The data to decrypt.
     * @param usage The key usage.
     * @return The Kerberos object resulting from a successful decrypt and decode.
     * @throws KerberosException
     */
    public Encodable unseal( Class hint, EncryptionKey key, EncryptedData data, KeyUsage usage )
        throws KerberosException
    {
        try
        {
            return decode( hint, decrypt( key, data, usage ) );
        }
        catch ( IOException ioe )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, ioe );
        }
        catch ( ClassCastException cce )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, cce );
        }
    }


    private EncryptedData encrypt( EncryptionKey key, byte[] plainText, KeyUsage usage ) throws KerberosException
    {
        EncryptionEngine engine = getEngine( key );

        return engine.getEncryptedData( key, plainText, usage );
    }


    private byte[] decrypt( EncryptionKey key, EncryptedData data, KeyUsage usage ) throws KerberosException
    {
        EncryptionEngine engine = getEngine( key );

        return engine.getDecryptedData( key, data, usage );
    }


    private Encodable decode( Class encodable, byte[] plainText ) throws IOException
    {
        Class clazz = ( Class ) DEFAULT_DECODERS.get( encodable );

        if ( clazz == null )
        {
            throw new IOException( I18n.err( I18n.ERR_600, encodable ) );
        }

        DecoderFactory factory = null;

        try
        {
            factory = ( DecoderFactory ) clazz.newInstance();
        }
        catch ( IllegalAccessException iae )
        {
            throw new IOException( I18n.err( I18n.ERR_601, encodable ) );
        }
        catch ( InstantiationException ie )
        {
            throw new IOException( I18n.err( I18n.ERR_602, encodable ) );
        }

        Decoder decoder = factory.getDecoder();

        return decoder.decode( plainText );
    }


    private EncryptionEngine getEngine( EncryptionKey key ) throws KerberosException
    {
        EncryptionType encryptionType = key.getKeyType();

        Class clazz = ( Class ) DEFAULT_CIPHERS.get( encryptionType );

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
