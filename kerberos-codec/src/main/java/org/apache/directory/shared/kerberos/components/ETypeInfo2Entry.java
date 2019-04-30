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

import org.apache.directory.api.asn1.Asn1Object;
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
 * ETYPE-INFO2-ENTRY        ::= SEQUENCE {
 *            etype           [0] Int32,
 *            salt            [1] KerberosString OPTIONAL,
 *            s2kparams       [2] OCTET STRING OPTIONAL
 *    }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ETypeInfo2Entry implements Asn1Object
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( ETypeInfo2Entry.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The encryption type */
    private EncryptionType etype;

    /** The salt */
    private String salt;
    private byte[] saltBytes;

    /** The s2k params */
    private byte[] s2kparams;

    // Storage for computed lengths
    private int etypeTagLength;
    private int saltTagLength;
    private int s2kparamsTagLength;
    private int etypeInfo2EntrySeqLength;


    /**
     * Creates a new instance of ETypeInfo2Entry.
     */
    public ETypeInfo2Entry()
    {
    }


    public ETypeInfo2Entry( EncryptionType etype )
    {
        this.etype = etype;
    }


    /**
     * Returns the salt.
     *
     * @return The salt.
     */
    public String getSalt()
    {
        return salt;
    }


    /**
     * @param salt the salt to set
     */
    public void setSalt( String salt )
    {
        this.salt = salt;
    }


    /**
     * Returns the s2kparams.
     *
     * @return The s2kparams.
     */
    public byte[] getS2kparams()
    {
        return s2kparams;
    }


    /**
     * @param s2kparams the s2kparams to set
     */
    public void setS2kparams( byte[] s2kparams )
    {
        this.s2kparams = s2kparams;
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
     * @param etype the encryptionType to set
     */
    public void setEType( EncryptionType etype )
    {
        this.etype = etype;
    }


    /**
     * Compute the ETYPE-INFO2-ENTRY length
     * <pre>
     * ETYPE-INFO-ENTRY :
     * 
     * 0x30 L1 ETYPE-INFO2-ENTRY sequence
     *  |
     *  +--&gt; 0xA0 L2 etype tag
     *  |     |
     *  |     +--&gt; 0x02 L2-1etype (int)
     *  |
     *  +--&gt; 0xA1 L3 salt tag
     *  |     |
     *  |     +--&gt; 0x1B L3-1 salt (KerberosString)
     *  |
     *  +--&gt; 0xA2 L4 s2kparams tag
     *        |
     *        +--&gt; 0x04 L4-1 salt (OCTET STRING)
     *        
     *  where L1 = L2 + length(0xA0) + length(L2) +
     *             L3 + length(0xA1) + length(L3) +
     *             L4 + length(0xA2) + length( L4)
     *  and
     *  L2 = L2-1 + length(0x02) + length( L2-1) 
     *  L3 = L3-1 + length(0x1B) + length( L3-1) 
     *  L4 = L4-1 + length(0x04) + length( L4-1) 
     *  </pre>
     */
    public int computeLength()
    {
        // Compute the etype. The Length will always be contained in 1 byte
        int etypeLength = BerValue.getNbBytes( etype.getValue() );
        etypeTagLength = 1 + TLV.getNbBytes( etypeLength ) + etypeLength;
        etypeInfo2EntrySeqLength = 1 + TLV.getNbBytes( etypeTagLength ) + etypeTagLength;

        // Compute the salt
        if ( salt != null )
        {
            saltBytes = Strings.getBytesUtf8( salt );
            saltTagLength = 1 + TLV.getNbBytes( saltBytes.length ) + saltBytes.length;
            etypeInfo2EntrySeqLength += 1 + TLV.getNbBytes( saltTagLength ) + saltTagLength;
        }

        // Compute the s2kparams
        if ( s2kparams != null )
        {
            s2kparamsTagLength = 1 + TLV.getNbBytes( s2kparams.length ) + s2kparams.length;
            etypeInfo2EntrySeqLength += 1 + TLV.getNbBytes( s2kparamsTagLength ) + s2kparamsTagLength;
        }

        return 1 + TLV.getNbBytes( etypeInfo2EntrySeqLength ) + etypeInfo2EntrySeqLength;
    }


    /**
     * Encode the ETYPE-INFO2-ENTRY message to a PDU. 
     * <pre>
     * ETYPE-INFO2-ENTRY :
     * 
     * 0x30 LL
     *   0xA0 LL 
     *     0x02 0x01 etype
     *   0xA1 LL 
     *     0x1B LL salt
     *   0xA2 LL 
     *     0x04 LL s2kparams
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
            // The ETYPE-INFO2-ENTRY SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( etypeInfo2EntrySeqLength ) );

            // The etype, first the tag, then the value
            buffer.put( ( byte ) KerberosConstants.ETYPE_INFO2_ENTRY_ETYPE_TAG );
            buffer.put( TLV.getBytes( etypeTagLength ) );
            BerValue.encode( buffer, etype.getValue() );

            // The salt, first the tag, then the value, if salt is not null
            if ( salt != null )
            {
                // The tag
                buffer.put( ( byte ) KerberosConstants.ETYPE_INFO2_ENTRY_SALT_TAG );
                buffer.put( TLV.getBytes( saltTagLength ) );

                // The value
                buffer.put( UniversalTag.GENERAL_STRING.getValue() );
                buffer.put( TLV.getBytes( saltBytes.length ) );
                buffer.put( saltBytes );
            }

            // The s2kparams, first the tag, then the value, if s2kparams is not null
            if ( s2kparams != null )
            {
                buffer.put( ( byte ) KerberosConstants.ETYPE_INFO2_ENTRY_S2KPARAMS_TAG );
                buffer.put( TLV.getBytes( saltTagLength ) );
                BerValue.encode( buffer, s2kparams );
            }
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_145, 1 + TLV.getNbBytes( etypeInfo2EntrySeqLength )
                + etypeInfo2EntrySeqLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ), boe );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "ETYPE-INFO2-ENTRY encoding : {}", Strings.dumpBytes( buffer.array() ) );
            LOG.debug( "ETYPE-INFO2-ENTRY initial value : {}", this );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "ETYPE-INFO2-ENTRY : {\n" );
        sb.append( "    etype: " ).append( etype ).append( '\n' );

        if ( salt != null )
        {
            sb.append( "    salt: " ).append( salt ).append( '\n' );
        }

        if ( salt != null )
        {
            sb.append( "    s2kparams: " ).append( Strings.dumpBytes( s2kparams ) ).append( '\n' );
        }

        sb.append( "}\n" );

        return sb.toString();
    }
}
