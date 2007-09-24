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
 * The Encryption Type info entry container. The ASN.1 grammar is :
 * 
 *  ETYPE-INFO-ENTRY        ::= SEQUENCE {
 *          etype           [0] Int32,
 *          salt            [1] OCTET STRING OPTIONAL
 *  }
 *
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EncryptionTypeInfoEntry extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( EncryptionTypeInfoEntry.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The encryption type */
    private EncryptionType eType;
    
    /** The salt */
    private byte[] salt;

    // Storage for computed lengths
    private transient int eTypeLength;
    private transient int saltLength;
    private transient int encryptionTypeInfoEntryLength;

    /**
     * Creates a new instance of EncryptionTypeInfoEntry.
     *
     * @param eType The encryption type.
     */
    public EncryptionTypeInfoEntry( EncryptionType eType )
    {
        this.eType = eType;
        this.salt = null;
    }

    /**
     * Creates a new instance of EncryptionTypeInfoEntry.
     *
     * @param eType The encryption type. 
     * @param salt The salt
     */
    public EncryptionTypeInfoEntry( EncryptionType eType, byte[] salt )
    {
        this.eType = eType;
        this.salt = salt;
    }


    /**
     * Returns the salt.
     *
     * @return The salt.
     */
    public byte[] getSalt()
    {
        return salt;
    }


    /**
     * Returns the {@link EncryptionType}.
     *
     * @return The {@link EncryptionType}.
     */
    public EncryptionType getEncryptionType()
    {
        return eType;
    }

    /**
     * Compute the EncryptionTypeInfoEntry length
     * 
     * EncryptionTypeInfoEntry :
     * 
     * 0x30 L1 EncryptionTypeInfoEntry sequence
     *  |
     *  +--> 0xA0 L2 etype tag
     *  |     |
     *  |     +--> 0x02 L2-1 etype (int)
     *  |
     *  [+--> 0xA1 L3 salt tag
     *        |
     *        +--> 0x04 L3-1 salt (OCTET STRING)] OPTIONAL
     *        
     *  where L1 = L2 + lenght(0xA0) + length(L2) +
     *             L3 + lenght(0xA1) + length(L3) 
     *  and
     *  L2 = L2-1 + length(0x02) + length( L2-1) 
     *  L3 = L3-1 + length(0x04) + length( L3-1) 
     */
    public int computeLength()
    {
        // Compute the eType.
        eTypeLength = 1 + TLV.getNbBytes( eType.getOrdinal() ) + Value.getNbBytes( eType.getOrdinal() );
        encryptionTypeInfoEntryLength = 
            1 + TLV.getNbBytes( eTypeLength ) + eTypeLength;

        // Compute the salt length, if any
        if ( salt != null )
        {
            saltLength = 1 + TLV.getNbBytes( salt.length ) + salt.length;
            encryptionTypeInfoEntryLength += 
                1 + TLV.getNbBytes( saltLength ) + saltLength;
        }
        else
        {
            saltLength = 0;
        }

        // Compute the whole sequence length
        int encryptionTypeInfoEntrySeqLength = 1 + Value.getNbBytes( encryptionTypeInfoEntryLength ) + encryptionTypeInfoEntryLength;

        return encryptionTypeInfoEntrySeqLength;
    }


    /**
     * Encode the EncryptionTypeInfoEntry message to a PDU. 
     * 
     * EncryptionTypeInfoEntry :
     * 
     * 0x30 LL
     *   0xA0 LL 
     *     0x02 0x01 etype
     *   0xA1 LL 
     *     0x04 LL salt
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
            // The EncryptionTypeInfoEntry SEQ Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( encryptionTypeInfoEntryLength ) );

            // The etype, first the tag, then the value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( eTypeLength ) );
            Value.encode( buffer, eType.getOrdinal() );

            // The salt, first the tag, then the value, if any
            if ( salt != null )
            {
                buffer.put( ( byte ) 0xA1 );
                buffer.put( TLV.getBytes( saltLength ) );
                Value.encode( buffer, salt );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( "Cannot encode the EncryptionTypeInfoEntry object, the PDU size is {} when only {} bytes has been allocated", 1
                + TLV.getNbBytes( encryptionTypeInfoEntryLength ) + encryptionTypeInfoEntryLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "EncryptionTypeInfoEntry encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "EncryptionTypeInfoEntry initial value : {}", toString() );
        }

        return buffer;
    }

    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "EncryptionTypeInfoEntry : {\n" );
        sb.append( tabs ).append( "    eType: " ).append( eType ).append( '\n' );

        if ( salt != null )
        {
            sb.append( tabs + "    salt:" ).append( StringTools.dumpBytes( salt ) ).append( '\n' );
        }

        sb.append( tabs + "}\n" );

        return sb.toString();
    }
}
