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

import org.apache.directory.api.asn1.ber.tlv.BerValue;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The AdAndOr structure is used to store a AD-AND-OR associated to a type.
 * 
 * The ASN.1 grammar is :
 * <pre>
 * AD-AND-OR               ::= SEQUENCE {
 *         condition-count [0] Int32,
 *         elements        [1] <AuthorizationData>
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AdAndOr extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( AdAndOr.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The condition-count */
    private int conditionCount;

    /** The elements */
    private AuthorizationData elements;

    // Storage for computed lengths
    private int conditionCountTagLength;
    private int elementsTagLength;
    private int adAndOrSeqLength;


    /**
     * Creates a new instance of AdAndOr
     */
    public AdAndOr()
    {
    }


    /**
     * @return the conditionCount
     */
    public int getConditionCount()
    {
        return conditionCount;
    }


    /**
     * @param conditionCount the conditionCount to set
     */
    public void setConditionCount( int conditionCount )
    {
        this.conditionCount = conditionCount;
    }


    /**
     * @return the elements
     */
    public AuthorizationData getElements()
    {
        return elements;
    }


    /**
     * @param elements the elements to set
     */
    public void setElements( AuthorizationData elements )
    {
        this.elements = elements;
    }


    /**
     * Compute the AD-AND-OR length
     * <pre>
     * 0x30 L1 AD-AND-OR sequence
     *  |
     *  +--> 0xA1 L2 condition count tag
     *  |     |
     *  |     +--> 0x02 L2-1 condition count (int)
     *  |
     *  +--> 0xA2 L3 elements tag
     *        |
     *        +--> 0x30 L3-1 elements (AuthorizationData)
     * </pre>
     */
    @Override
    public int computeLength()
    {
        // Compute the condition count length
        int conditionCountLength = BerValue.getNbBytes( conditionCount );
        conditionCountTagLength = 1 + TLV.getNbBytes( conditionCountLength ) + conditionCountLength;
        adAndOrSeqLength = 1 + TLV.getNbBytes( conditionCountTagLength ) + conditionCountTagLength;

        // Compute the elements length
        elementsTagLength = elements.computeLength();
        adAndOrSeqLength += 1 + TLV.getNbBytes( elementsTagLength ) + elementsTagLength;

        // Compute the whole sequence length
        return 1 + TLV.getNbBytes( adAndOrSeqLength ) + adAndOrSeqLength;
    }


    /**
     * Encode the AD-AND-OR message to a PDU.
     * 
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    @Override
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_148 ) );
        }

        try
        {
            // The AD-AND-OR SEQ OF Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( adAndOrSeqLength ) );

            // the condition-count
            buffer.put( ( byte ) KerberosConstants.AD_AND_OR_CONDITION_COUNT_TAG );
            buffer.put( ( byte ) conditionCountTagLength );
            BerValue.encode( buffer, conditionCount );

            // the elements
            buffer.put( ( byte ) KerberosConstants.AD_AND_OR_ELEMENTS_TAG );
            buffer.put( ( byte ) elementsTagLength );

            elements.encode( buffer );
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_139, 1 + TLV.getNbBytes( adAndOrSeqLength )
                + adAndOrSeqLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "AD-AND-OR encoding : {}", Strings.dumpBytes( buffer.array() ) );
            LOG.debug( "AD-AND-OR initial value : {}", toString() );
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

        sb.append( tabs ).append( "AD-AND-OR : {\n" );
        sb.append( tabs ).append( "    condition-count: " ).append( conditionCount ).append( '\n' );
        sb.append( tabs + "    elements:" ).append( elements ).append( '\n' );
        sb.append( tabs + "}\n" );

        return sb.toString();
    }
}
