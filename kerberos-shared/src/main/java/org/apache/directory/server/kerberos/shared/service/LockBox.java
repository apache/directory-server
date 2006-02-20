/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.directory.server.kerberos.shared.service;


import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.kerberos.shared.crypto.encryption.Des3CbcMd5Encryption;
import org.apache.directory.server.kerberos.shared.crypto.encryption.Des3CbcSha1Encryption;
import org.apache.directory.server.kerberos.shared.crypto.encryption.DesCbcCrcEncryption;
import org.apache.directory.server.kerberos.shared.crypto.encryption.DesCbcMd4Encryption;
import org.apache.directory.server.kerberos.shared.crypto.encryption.DesCbcMd5Encryption;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionEngine;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.io.decoder.AuthenticatorDecoder;
import org.apache.directory.server.kerberos.shared.io.decoder.AuthorizationDataDecoder;
import org.apache.directory.server.kerberos.shared.io.decoder.Decoder;
import org.apache.directory.server.kerberos.shared.io.decoder.DecoderFactory;
import org.apache.directory.server.kerberos.shared.io.decoder.EncKrbPrivPartDecoder;
import org.apache.directory.server.kerberos.shared.io.decoder.EncTicketPartDecoder;
import org.apache.directory.server.kerberos.shared.io.decoder.EncryptedTimestampDecoder;
import org.apache.directory.server.kerberos.shared.io.encoder.EncApRepPartEncoder;
import org.apache.directory.server.kerberos.shared.io.encoder.EncAsRepPartEncoder;
import org.apache.directory.server.kerberos.shared.io.encoder.EncKrbPrivPartEncoder;
import org.apache.directory.server.kerberos.shared.io.encoder.EncTgsRepPartEncoder;
import org.apache.directory.server.kerberos.shared.io.encoder.EncTicketPartEncoder;
import org.apache.directory.server.kerberos.shared.io.encoder.Encoder;
import org.apache.directory.server.kerberos.shared.io.encoder.EncoderFactory;
import org.apache.directory.server.kerberos.shared.messages.AuthenticationReply;
import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.server.kerberos.shared.messages.TicketGrantReply;
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.EncApRepPart;
import org.apache.directory.server.kerberos.shared.messages.components.EncKrbPrivPart;
import org.apache.directory.server.kerberos.shared.messages.components.EncTicketPart;
import org.apache.directory.server.kerberos.shared.messages.value.AuthorizationData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedTimeStamp;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;


/**
 * A Hashed Adapter encapsulating ASN.1 encoders and decoders and cipher text engines to
 * perform seal() and unseal() operations.  A seal() operation performs an encode and an
 * encrypt, while an unseal() operation performs a decrypt and a decode.
 */
public class LockBox
{
    /** a map of the default encodable class names to the encoder class names */
    private static final Map DEFAULT_ENCODERS;
    /** a map of the default encodable class names to the decoder class names */
    private static final Map DEFAULT_DECODERS;
    /** a map of the default encryption types to the encryption engine class names */
    private static final Map DEFAULT_CIPHERS;

    static
    {
        Map map = new HashMap();

        map.put( EncTicketPart.class, EncTicketPartEncoder.class );
        map.put( AuthenticationReply.class, EncAsRepPartEncoder.class );
        map.put( TicketGrantReply.class, EncTgsRepPartEncoder.class );
        map.put( EncKrbPrivPart.class, EncKrbPrivPartEncoder.class );
        map.put( EncApRepPart.class, EncApRepPartEncoder.class );

        DEFAULT_ENCODERS = Collections.unmodifiableMap( map );
    }

    static
    {
        Map map = new HashMap();

        map.put( EncTicketPart.class, EncTicketPartDecoder.class );
        map.put( Authenticator.class, AuthenticatorDecoder.class );
        map.put( EncryptedTimeStamp.class, EncryptedTimestampDecoder.class );
        map.put( AuthorizationData.class, AuthorizationDataDecoder.class );
        map.put( EncKrbPrivPart.class, EncKrbPrivPartDecoder.class );

        DEFAULT_DECODERS = Collections.unmodifiableMap( map );
    }

    static
    {
        Map map = new HashMap();

        map.put( EncryptionType.DES_CBC_CRC, DesCbcCrcEncryption.class );
        map.put( EncryptionType.DES_CBC_MD4, DesCbcMd4Encryption.class );
        map.put( EncryptionType.DES_CBC_MD5, DesCbcMd5Encryption.class );
        map.put( EncryptionType.DES3_CBC_MD5, Des3CbcMd5Encryption.class );
        map.put( EncryptionType.DES3_CBC_SHA1, Des3CbcSha1Encryption.class );

        DEFAULT_CIPHERS = Collections.unmodifiableMap( map );
    }


    public EncryptedData seal( EncryptionKey key, Encodable encodable ) throws KerberosException
    {
        try
        {
            return encrypt( key, encode( encodable ) );
        }
        catch ( IOException ioe )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY );
        }
        catch ( ClassCastException cce )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY );
        }
    }


    public Encodable unseal( Class hint, EncryptionKey key, EncryptedData data ) throws KerberosException
    {
        try
        {
            return decode( hint, decrypt( key, data ) );
        }
        catch ( IOException ioe )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY );
        }
        catch ( ClassCastException cce )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY );
        }
    }


    private EncryptedData encrypt( EncryptionKey key, byte[] plainText ) throws KerberosException
    {
        EncryptionEngine engine = getEngine( key );

        return engine.getEncryptedData( key, plainText );
    }


    private byte[] decrypt( EncryptionKey key, EncryptedData data ) throws KerberosException
    {
        EncryptionEngine engine = getEngine( key );

        return engine.getDecryptedData( key, data );
    }


    private byte[] encode( Encodable encodable ) throws IOException
    {
        Class encodableClass = encodable.getClass();

        Class clazz = ( Class ) DEFAULT_ENCODERS.get( encodableClass );

        if ( clazz == null )
        {
            throw new IOException( "Encoder unavailable for " + encodableClass );
        }

        EncoderFactory factory = null;

        try
        {
            factory = ( EncoderFactory ) clazz.newInstance();
        }
        catch ( IllegalAccessException iae )
        {
            throw new IOException( "Error accessing encoder for " + encodableClass );
        }
        catch ( InstantiationException ie )
        {
            throw new IOException( "Error instantiating encoder for " + encodableClass );
        }

        Encoder encoder = factory.getEncoder();

        return encoder.encode( encodable );
    }


    private Encodable decode( Class encodable, byte[] plainText ) throws IOException
    {
        Class clazz = ( Class ) DEFAULT_DECODERS.get( encodable );

        if ( clazz == null )
        {
            throw new IOException( "Decoder unavailable for " + encodable );
        }

        DecoderFactory factory = null;

        try
        {
            factory = ( DecoderFactory ) clazz.newInstance();
        }
        catch ( IllegalAccessException iae )
        {
            throw new IOException( "Error accessing decoder for " + encodable );
        }
        catch ( InstantiationException ie )
        {
            throw new IOException( "Error instantiating decoder for " + encodable );
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
            throw new KerberosException( ErrorType.KDC_ERR_ETYPE_NOSUPP );
        }
        catch ( InstantiationException ie )
        {
            throw new KerberosException( ErrorType.KDC_ERR_ETYPE_NOSUPP );
        }
    }
}
