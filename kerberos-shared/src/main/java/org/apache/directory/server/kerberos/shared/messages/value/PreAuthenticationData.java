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

import org.apache.directory.server.kerberos.shared.messages.value.types.PreAuthenticationDataType;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Pre-Authentication data. Tha ASN.1 GRAMMAR IS :
 * 
 * PA-DATA         ::= SEQUENCE {
 *         -- NOTE: first tag is [1], not [0]
 *         padata-type     [1] Int32,
 *         padata-value    [2] OCTET STRING -- might be encoded AP-REQ
 * }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PreAuthenticationData extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( PreAuthenticationData.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The Pre-authentication type */
    private PreAuthenticationDataType paDataType;
    
    /** The authentication data */
    private byte[] paDataValue;

    // Storage for computed lengths
    private transient int paDataTypeTagLength;
    private transient int paDataValueTagLength;
    private transient int preAuthenticationDataSeqLength;
    

    /**
     * Creates a new instance of PreAuthenticationData.
     *
     * @param paDataType
     * @param dataValue
     */
    public PreAuthenticationData( PreAuthenticationDataType paDataType, byte[] paDataValue )
    {
        this.paDataType = paDataType;
        this.paDataValue = paDataValue;
    }


    /**
     * Returns the {@link PreAuthenticationDataType}.
     *
     * @return The {@link PreAuthenticationDataType}.
     */
    public PreAuthenticationDataType getDataType()
    {
        return paDataType;
    }


    /**
     * Returns the raw bytes of the {@link PreAuthenticationData}.
     *
     * @return The raw bytes of the {@link PreAuthenticationData}.
     */
    public byte[] getDataValue()
    {
        return paDataValue;
    }
    
    /**
     * Compute the PreAuthenticationData length
     * 
     * PreAuthenticationData :
     * 
     * 0x30 L1 PreAuthenticationData sequence
     *  |
     *  +--> 0xA0 L2 padata-type tag
     *  |     |
     *  |     +--> 0x02 L2-1 padata-type (int)
     *  |
     *  +--> 0xA1 L3 padata-value tag
     *        |
     *        +--> 0x04 L3-1 padata-value (OCTET STRING)
     *        
     *  where L1 = L2 + lenght(0xA0) + length(L2) +
     *             L3 + lenght(0xA1) + length(L3) 
     *  and
     *  L2 = L2-1 + length(0x02) + length( L2-1) 
     *  L3 = L3-1 + length(0x04) + length( L3-1) 
     */
    public int computeLength()
    {
        // Compute the paDataType. The Length will always be contained in 1 byte
        int paDataTypeLength = Value.getNbBytes( paDataType.getOrdinal() );
        paDataTypeTagLength = 1 + TLV.getNbBytes( paDataTypeLength ) + paDataTypeLength;
        preAuthenticationDataSeqLength = 1 + TLV.getNbBytes( paDataTypeTagLength ) + paDataTypeTagLength;

        // Compute the paDataValue
        if ( paDataValue == null )
        {
            paDataValueTagLength = 1 + 1;
        }
        else
        {
            paDataValueTagLength = 1 + TLV.getNbBytes( paDataValue.length ) + paDataValue.length;
        }

        // Compute the whole sequence length
        preAuthenticationDataSeqLength += 1 + TLV.getNbBytes( paDataValueTagLength ) + paDataValueTagLength;

        return 1 + TLV.getNbBytes( preAuthenticationDataSeqLength ) + preAuthenticationDataSeqLength;

    }


    /**
     * Encode the PreAuthenticationData message to a PDU. 
     * 
     * PreAuthenticationData :
     * 
     * 0x30 LL
     *   0xA0 LL 
     *     0x02 0x01 padata-type
     *   0xA1 LL 
     *     0x04 LL padata-value
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
            // The Checksum SEQ Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( preAuthenticationDataSeqLength ) );

            // The cksumtype, first the tag, then the value
            buffer.put( ( byte ) 0xA1 );
            buffer.put( TLV.getBytes( paDataTypeTagLength ) );
            Value.encode( buffer, paDataType.getOrdinal() );

            // The checksum, first the tag, then the value
            buffer.put( ( byte ) 0xA2 );
            buffer.put( TLV.getBytes( paDataValueTagLength ) );
            Value.encode( buffer, paDataValue );
        }
        catch ( BufferOverflowException boe )
        {
            log.error( "Cannot encode the PreAuthenticationData object, the PDU size is {} when only {} bytes has been allocated", 1
                + TLV.getNbBytes( preAuthenticationDataSeqLength ) + preAuthenticationDataSeqLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "PreAuthenticationData encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "PreAuthenticationData initial value : {}", toString() );
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

        sb.append( tabs ).append( "PreAuthenticationData : {\n" );
        sb.append( tabs ).append( "    padata-type: " ).append( paDataType ).append( '\n' );

        if ( paDataValue != null )
        {
            sb.append( tabs + "    padata-value:" ).append( StringTools.dumpBytes( paDataValue ) ).append( '\n' );
        }

        sb.append( tabs + "}\n" );

        return sb.toString();
    }
}
