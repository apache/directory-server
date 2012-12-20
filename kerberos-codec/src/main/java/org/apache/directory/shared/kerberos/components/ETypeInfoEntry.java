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
package org.apache.directory.shared.kerberos.components;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.AbstractAsn1Object;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.tlv.BerValue;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides encryption info information sent to the client.
 * 
 * The ASN.1 grammar for this structure is :
 * <pre>
 * ETYPE-INFO-ENTRY        ::= SEQUENCE {
 *            etype           [0] Int32,
 *            salt            [1] OCTET STRING OPTIONAL
 *    }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ETypeInfoEntry extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( ETypeInfoEntry.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The encryption type */
    private EncryptionType etype;

    /** The salt */
    private byte[] salt;

    // Storage for computed lengths
    private int etypeTagLength;
    private int saltTagLength;
    private int etypeInfoEntrySeqLength;


    /**
     * Creates a new instance of ETypeInfoEntry.
     * 
     * @param etype the Encryption type
     * @param salt the salt
     */
    public ETypeInfoEntry( EncryptionType etype, byte[] salt )
    {
        this.etype = etype;
        this.salt = salt;
    }


    /**
     * Creates a new instance of ETypeInfoEntry.
     */
    public ETypeInfoEntry()
    {
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
     * @param salt the salt to set
     */
    public void setSalt( byte[] salt )
    {
        this.salt = salt;
    }


    /**
     * Returns the {@link EncryptionType}.
     *
     * @return The {@link EncryptionType}.
     */
    public EncryptionType getEType()
    {
        return etype;
    }


    /**
     * @param encryptionType the encryptionType to set
     */
    public void setEType( EncryptionType etype )
    {
        this.etype = etype;
    }


    /**
     * Compute the ETYPE-INFO-ENTRY length
     * <pre>
     * ETYPE-INFO-ENTRY :
     * 
     * 0x30 L1 ETYPE-INFO-ENTRY sequence
     *  |
     *  +--> 0xA0 L2 etype tag
     *  |     |
     *  |     +--> 0x02 L2-1etype (int)
     *  |
     *  +--> 0xA1 L3 salt tag
     *        |
     *        +--> 0x04 L3-1 salt (OCTET STRING)
     *        
     *  where L1 = L2 + lenght(0xA0) + length(L2) +
     *             L3 + lenght(0xA1) + length(L3) 
     *  and
     *  L2 = L2-1 + length(0x02) + length( L2-1) 
     *  L3 = L3-1 + length(0x04) + length( L3-1) 
     *  </pre>
     */
    public int computeLength()
    {
        // Compute the etype. The Length will always be contained in 1 byte
        int etypeLength = BerValue.getNbBytes( etype.getValue() );
        etypeTagLength = 1 + TLV.getNbBytes( etypeLength ) + etypeLength;
        etypeInfoEntrySeqLength = 1 + TLV.getNbBytes( etypeTagLength ) + etypeTagLength;

        // Compute the salt
        if ( salt != null )
        {
            saltTagLength = 1 + TLV.getNbBytes( salt.length ) + salt.length;
            etypeInfoEntrySeqLength += 1 + TLV.getNbBytes( saltTagLength ) + saltTagLength;
        }

        return 1 + TLV.getNbBytes( etypeInfoEntrySeqLength ) + etypeInfoEntrySeqLength;
    }


    /**
     * Encode the ETYPE-INFO-ENTRY message to a PDU. 
     * <pre>
     * ETYPE-INFO-ENTRY :
     * 
     * 0x30 LL
     *   0xA1 LL 
     *     0x02 0x01 etype
     *   0xA2 LL 
     *     0x04 LL salt
     * </pre>
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_148 ) );
        }

        try
        {
            // The ETYPE-INFO-ENTRY SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( etypeInfoEntrySeqLength ) );

            // The etype, first the tag, then the value
            buffer.put( ( byte ) KerberosConstants.ETYPE_INFO_ENTRY_ETYPE_TAG );
            buffer.put( TLV.getBytes( etypeTagLength ) );
            BerValue.encode( buffer, etype.getValue() );

            // The salt, first the tag, then the value, if salt is not null
            if ( salt != null )
            {
                buffer.put( ( byte ) KerberosConstants.ETYPE_INFO_ENTRY_SALT_TAG );
                buffer.put( TLV.getBytes( saltTagLength ) );
                BerValue.encode( buffer, salt );
            }
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_145, 1 + TLV.getNbBytes( etypeInfoEntrySeqLength )
                + etypeInfoEntrySeqLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "ETYPE-INFO-ENTRY encoding : {}", Strings.dumpBytes( buffer.array() ) );
            LOG.debug( "ETYPE-INFO-ENTRY initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "ETYPE-INFO-ENTRY : {\n" );
        sb.append( "    etype: " ).append( etype ).append( '\n' );

        if ( salt != null )
        {
            sb.append( "    salt: " ).append( Strings.dumpBytes( salt ) ).append( '\n' );
        }

        sb.append( "}\n" );

        return sb.toString();
    }
}
