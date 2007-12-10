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

import org.apache.directory.server.kerberos.shared.messages.value.types.TransitedEncodingType;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The TransitedEncoding structure.
 * 
 * The ASN.1 grammar is :
 * 
 * -- encoded Transited field
 * TransitedEncoding       ::= SEQUENCE {
 *         tr-type         [0] Int32 -- must be registered --,
 *         contents        [1] OCTET STRING
 * }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class TransitedEncoding extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( TransitedEncoding.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The transited type. One of :
     * NULL
     * DOMAIN_X500_COMPRESS
     */
    private TransitedEncodingType trType;

    /** The transited data */
    private byte[] contents;

    // Storage for computed lengths
    private transient int trTypeLength;
    private transient int contentsLength;
    private transient int transitedEncodingLength;


    /**
     * Creates a new instance of TransitedEncoding.
     */
    public TransitedEncoding()
    {
        trType = TransitedEncodingType.NULL;
        contents = new byte[0];
    }


    /**
     * Creates a new instance of TransitedEncoding.
     *
     * @param type
     * @param contents
     */
    public TransitedEncoding( TransitedEncodingType trType, byte[] contents )
    {
        this.trType = trType;
        this.contents = contents;
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
     */
    public int computeLength()
    {
        // Compute the trType. The Length will always be contained in 1 byte
        trTypeLength = 1 + 1 + Value.getNbBytes( trType.getOrdinal() );
        transitedEncodingLength = 1 + TLV.getNbBytes( trTypeLength ) + trTypeLength;

        // Compute the keyValue
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
        int transitedEncodingSeqLength = 1 + Value.getNbBytes( transitedEncodingLength ) + transitedEncodingLength;

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
     *     0x04 LL adData
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
            // The AuthorizationDataEntry SEQ Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( transitedEncodingLength ) );

            // The tr-type, first the tag, then the value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( trTypeLength ) );
            Value.encode( buffer, trType.getOrdinal() );

            // The contents, first the tag, then the value
            buffer.put( ( byte ) 0xA1 );
            buffer.put( TLV.getBytes( contentsLength ) );
            Value.encode( buffer, contents );
        }
        catch ( BufferOverflowException boe )
        {
            log.error(
                "Cannot encode the TransitedEncoding object, the PDU size is {} when only {} bytes has been allocated",
                1 + TLV.getNbBytes( transitedEncodingLength ) + transitedEncodingLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "TransitedEncoding encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "TransitedEncoding initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "TransitedEncoding : {\n" );
        sb.append( "    tr-type: " ).append( trType ).append( '\n' );

        sb.append( "    contents: " ).append( StringTools.dumpBytes( contents ) ).append( "\n}\n" );

        return sb.toString();
    }
}
