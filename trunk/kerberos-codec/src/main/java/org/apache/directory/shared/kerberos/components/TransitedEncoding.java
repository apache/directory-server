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
import java.util.Arrays;

import org.apache.directory.api.asn1.Asn1Object;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.tlv.BerValue;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.types.TransitedEncodingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The TransitedEncoding structure.
 * 
 * The ASN.1 grammar is :
 * <pre>
 * -- encoded Transited field
 * TransitedEncoding       ::= SEQUENCE {
 *         tr-type         [0] Int32 -- must be registered --,
 *         contents        [1] OCTET STRING
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TransitedEncoding implements Asn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( TransitedEncoding.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** 
     * The transited type. One of :
     * NULL
     * DOMAIN_X500_COMPRESS
     */
    private TransitedEncodingType trType;

    /** The transited data */
    private byte[] contents;

    // Storage for computed lengths
    private int trTypeLength;
    private int contentsLength;
    private int transitedEncodingLength;


    /**
     * Creates a new instance of TransitedEncoding.
     */
    public TransitedEncoding()
    {
        trType = TransitedEncodingType.NULL;
        contents = Strings.EMPTY_BYTES;
    }


    /**
     * Returns the contents.
     *
     * @return The contents.
     */
    public byte[] getContents()
    {
        return contents;
    }


    /**
     * Set the contents
     * @param contents The contents
     */
    public void setContents( byte[] contents )
    {
        this.contents = contents;
    }


    /**
     * Returns the {@link TransitedEncodingType}.
     *
     * @return The {@link TransitedEncodingType}.
     */
    public TransitedEncodingType getTrType()
    {
        return trType;
    }


    /**
     * Set the transited encoding type
     * @param trType The transited encoding type
     */
    public void setTrType( TransitedEncodingType trType )
    {
        this.trType = trType;
    }


    /**
     * Compute the TransitedEncoding length
     * 
     * <pre>
     * TransitedEncoding :
     * 
     * 0x30 L1 TransitedEncoding
     *  |
     *  +--> 0xA0 L2 trType tag
     *  |     |
     *  |     +--> 0x02 L2-1 trType (int)
     *  |
     *  +--> 0xA1 L3 contents tag
     *        |
     *        +--> 0x04 L3-1 contents (OCTET STRING)
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
        // Compute the trType. The Length will always be contained in 1 byte
        trTypeLength = 1 + 1 + BerValue.getNbBytes( trType.getValue() );
        transitedEncodingLength = 1 + TLV.getNbBytes( trTypeLength ) + trTypeLength;

        // Compute the contents length
        if ( contents == null )
        {
            contentsLength = 1 + 1;
        }
        else
        {
            contentsLength = 1 + TLV.getNbBytes( contents.length ) + contents.length;
        }

        transitedEncodingLength += 1 + TLV.getNbBytes( contentsLength ) + contentsLength;

        // Compute the whole sequence length
        int transitedEncodingSeqLength = 1 + TLV.getNbBytes( transitedEncodingLength ) + transitedEncodingLength;

        return transitedEncodingSeqLength;
    }


    /**
     * Encode the TransitedEncoding message to a PDU. 
     * 
     * TransitedEncoding :
     * 
     * 0x30 LL
     *   0xA0 LL 
     *     0x02 0x01 trType
     *   0xA1 LL 
     *     0x04 LL contents
     * 
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
            // The AuthorizationDataEntry SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( transitedEncodingLength ) );

            // The tr-type, first the tag, then the value
            buffer.put( ( byte ) KerberosConstants.TRANSITED_ENCODING_TR_TYPE_TAG );
            buffer.put( TLV.getBytes( trTypeLength ) );
            BerValue.encode( buffer, trType.getValue() );

            // The contents, first the tag, then the value
            buffer.put( ( byte ) KerberosConstants.TRANSITED_ENCODING_CONTENTS_TAG );
            buffer.put( TLV.getBytes( contentsLength ) );
            BerValue.encode( buffer, contents );
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_147, 1 + TLV.getNbBytes( transitedEncodingLength )
                + transitedEncodingLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            log.debug( "TransitedEncoding encoding : {}", Strings.dumpBytes( buffer.array() ) );
            log.debug( "TransitedEncoding initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode( contents );
        result = prime * result + ( ( trType == null ) ? 0 : trType.hashCode() );
        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null )
        {
            return false;
        }

        TransitedEncoding other = ( TransitedEncoding ) obj;

        if ( !Arrays.equals( contents, other.contents ) )
        {
            return false;
        }

        if ( trType != other.trType )
        {
            return false;
        }

        return true;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "TransitedEncoding : {\n" );
        sb.append( "    tr-type: " ).append( trType ).append( '\n' );

        sb.append( "    contents: " ).append( Strings.dumpBytes( contents ) ).append( "\n}\n" );

        return sb.toString();
    }
}
