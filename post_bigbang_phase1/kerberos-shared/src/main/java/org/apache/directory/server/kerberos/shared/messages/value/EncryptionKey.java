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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Kerberos symmetric encryption key, which includes metadata support for
 * the associated key type and key version number.
 * 
 * The ASN.1 description for this structure is :
 * EncryptionKey   ::= SEQUENCE {
 *       keytype         [0] Int32 -- actually encryption type --,
 *       keyvalue        [1] OCTET STRING
 * }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EncryptionKey extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( EncryptionKey.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    // The encryption type
    private EncryptionType keyType;

    // The encrypted value
    private byte[] keyValue;
    
    // The key version
    private int keyVersion;

    // Storage for computed lengths
    private transient int keyTypeLength;
    private transient int keyValueLength;
    private transient int encryptionKeyLength;


    /**
     * Creates a new instance of EncryptionKey.
     */
    public EncryptionKey()
    {
    }
    

    /**
     * Creates a new instance of EncryptionKey.
     *
     * @param keyType The encryptionType 
     * @param keyValue The value
     */
    public EncryptionKey( EncryptionType keyType, byte[] keyValue )
    {
        this.keyType = keyType;
        this.keyValue = keyValue;
    }


    /**
     * Creates a new instance of EncryptionKey.
     *
     * @param keyType The encryptionType 
     * @param keyValue The value
     * @param keyVersion ???
     */
    public EncryptionKey( EncryptionType keyType, byte[] keyValue, int keyVersion )
    {
        this.keyType = keyType;
        this.keyValue = keyValue;
        this.keyVersion = keyVersion;
    }


    /**
     * Destroys this key by overwriting the symmetric key material with zeros.
     */
    public synchronized void destroy()
    {
        if ( keyValue != null )
        {
            Arrays.fill( keyValue, ( byte ) 0x00 );
        }
    }


    /**
     * Returns the key type.
     *
     * @return The key type.
     */
    public EncryptionType getKeyType()
    {
        return keyType;
    }


    /**
     * Set the encryption type
     * @param keyType The encryption type
     */
    public void setKeyType( EncryptionType keyType ) 
    {
        this.keyType = keyType;
    }


    /**
     * Returns the key value.
     *
     * @return The key value.
     */
    public byte[] getKeyValue()
    {
        return keyValue;
    }


    /**
     * Returns the key version.
     *
     * @return The key version.
     */
    public int getKeyVersion()
    {
        return keyVersion;
    }

    
    /**
     * Returns the key version.
     *
     * @return The key version.
     */
    public void setKeyVersion( int keyVersion)
    {
        this.keyVersion = keyVersion;
    }

    
    /**
     * Set the key value
     * @param keyValue The key value
     */
    public void setKeyValue( byte[] keyValue ) 
    {
        this.keyValue = keyValue;
    }


    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( ( o == null ) || !( o instanceof EncryptionKey ) )
        {
            return false;
        }

        EncryptionKey that = ( EncryptionKey ) o;
        return ( this.keyType == that.keyType ) && ( Arrays.equals( this.keyValue, that.keyValue ) );
    }


    /**
     * Compute the EncryptionKey length
     * 
     * EncryptionKey :
     * 
     * 0x30 L1 EncryptionKey
     *  |
     *  +--> 0xA0 L2 keyType tag
     *  |     |
     *  |     +--> 0x02 L2-1 keyType (int)
     *  |
     *  +--> 0xA1 L3 keyValue tag
     *        |
     *        +--> 0x04 L3-1 keyValue (OCTET STRING)
     *        
     *  where L1 = L2 + lenght(0xA0) + length(L2) +
     *             L3 + lenght(0xA1) + length(L3) 
     *  and
     *  L2 = L2-1 + length(0x02) + length( L2-1) 
     *  L3 = L3-1 + length(0x04) + length( L3-1) 
     */
    public int computeLength()
    {
        // Compute the keyType. The Length will always be cobntained in 1 byte
        keyTypeLength = 1 + 1 + Value.getNbBytes( keyType.getOrdinal() );
        encryptionKeyLength = 1 + TLV.getNbBytes( keyTypeLength ) + keyTypeLength;

        // Compute the keyValue
        if ( keyValue == null )
        {
            keyValueLength = 1 + 1;
        }
        else
        {
            keyValueLength = 1 + TLV.getNbBytes( keyValue.length ) + keyValue.length;
        }

        encryptionKeyLength += 1 + TLV.getNbBytes( keyValueLength ) + keyValueLength;

        // Compute the whole sequence length
        int encryptionKeySeqLength = 1 + Value.getNbBytes( encryptionKeyLength ) + encryptionKeyLength;

        return encryptionKeySeqLength;

    }


    /**
     * Encode the EncryptionKey message to a PDU. 
     * 
     * EncryptionKey :
     * 
     * 0x30 LL
     *   0xA0 LL 
     *     0x02 0x01 keyType
     *   0xA1 LL 
     *     0x04 LL keyValue
     * 
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            // The EncryptionKey SEQ Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( encryptionKeyLength ) );

            // The keyType, first the tag, then the value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( keyTypeLength ) );
            Value.encode( buffer, keyType.getOrdinal() );

            // The keyValue, first the tag, then the value
            buffer.put( ( byte ) 0xA1 );
            buffer.put( TLV.getBytes( keyValueLength ) );
            Value.encode( buffer, keyValue );
        }
        catch ( BufferOverflowException boe )
        {
            log.error(
                "Cannot encode the EncryptionKey object, the PDU size is {} when only {} bytes has been allocated", 1
                    + TLV.getNbBytes( encryptionKeyLength ) + encryptionKeyLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "EncryptionKey encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "EncryptionKey initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return keyType.toString() + " (" + keyType.getOrdinal() + ")";
    }
}
