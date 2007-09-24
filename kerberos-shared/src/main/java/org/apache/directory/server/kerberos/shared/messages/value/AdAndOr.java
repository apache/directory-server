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
import java.util.List;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An AD AND-OR container. AD AND-OR are contained in the ad-data part
 * of the AuthorizationData
 * 
 * The ASN.1 grammar is :
 * 
 * AD-AND-OR               ::= SEQUENCE {
 *         condition-count [0] Int32,
 *         elements        [1] AuthorizationData
 * }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public class AdAndOr extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( AdAndOr.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The number of AuthorizationData */
    private int conditionCounts;

    /** The list of AuthorizationData elements */
    private AuthorizationData elements;

    /** The OR condition is used when the conditionCounts value is 1 */
    public static final int OR_CONDITION = 1;

    // Storage for computed lengths
    private transient int conditionCountsLength;
    private transient int elementsLength;
    private transient int adAndOrLength;


    /**
     * Creates a new instance of AdAndOr.
     */
    public AdAndOr()
    {
        // used by ASN.1 decoder
    }


    /**
     * Sets {@link AuthorizationData} to this {@link AdAndOr}.
     *
     * @param elements the authorizationData
     */
    public void setAuthorizationData( AuthorizationData elements )
    {
        this.elements = elements;
        conditionCounts = OR_CONDITION;
    }


    /**
     * Sets {@link AuthorizationData} to this {@link AdAndOr}.
     *
     * @param elements the authorizationData
     */
    public void setORAuthorizationData( AuthorizationData elements )
    {
        this.elements = elements;
        conditionCounts = OR_CONDITION;
    }


    /**
     * Sets {@link AuthorizationData} to this {@link AdAndOr}.
     *
     * @param elements the authorizationData
     */
    public void setANDAuthorizationData( AuthorizationData elements )
    {
        this.elements = elements;

        if ( elements != null )
        {
            List<AuthorizationDataEntry> entries = elements.getEntries();

            if ( entries != null )
            {
                conditionCounts = elements.getEntries().size();
            }
            else
            {
                conditionCounts = OR_CONDITION;
            }
        }
        else
        {
            conditionCounts = OR_CONDITION;
        }
    }


    /**
     * Sets {@link AuthorizationData} to this {@link AdAndOr}.
     *
     * @param elements the authorizationData
     */
    public void setConditionCounts( int conditionCounts )
    {
        this.conditionCounts = conditionCounts;
    }


    /**
     * Compute the AdAndOr length
     * 
     * AdAndOr :
     * 
     * 0x30 L1 AdAndOr
     *  |
     *  +--> 0xA0 L2 conditionCounts tag
     *  |     |
     *  |     +--> 0x02 L2-1 conditionCounts (int)
     *  |
     *  +--> 0xA1 L3 AuthorizationData
     *        |
     *        +--> 0x02 L3-1 AuthorizationData object
     */
    public int computeLength()
    {
        // Compute the AdAndOr length.
        conditionCountsLength = 1 + TLV.getNbBytes( conditionCounts ) + Value.getNbBytes( conditionCounts );

        adAndOrLength = 1 + TLV.getNbBytes( conditionCountsLength ) + conditionCountsLength;

        if ( elements != null )
        {
            elementsLength = elements.computeLength();
            adAndOrLength += 1 + TLV.getNbBytes( elementsLength ) + elementsLength;
        }

        return 1 + TLV.getNbBytes( adAndOrLength ) + adAndOrLength;
    }


    /**
     * Encode the AdAndOr message to a PDU. 
     * 
     * AdAndOr :
     * 
     * 0x30 LL
     *   0xA0 LL conditionCounts 
     *   0xA1 LL 
     *     AuthorizationData
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
            // The AdAndOr SEQ Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( adAndOrLength ) );

            // The conditionCounts Tag and value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( conditionCountsLength ) );
            Value.encode( buffer, conditionCounts );

            // The elements Tag and value
            buffer.put( ( byte ) 0xA1 );
            buffer.put( TLV.getBytes( elementsLength ) );
            elements.encode( buffer );
        }
        catch ( BufferOverflowException boe )
        {
            log.error( "Cannot encode the AdAndOr object, the PDU size is {} when only {} bytes has been allocated", 1
                + TLV.getNbBytes( adAndOrLength ) + adAndOrLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "AdAndOr encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "AdAndOr initial value : {}", toString() );
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

        sb.append( tabs ).append( "AdAndOr : {\n" );
        sb.append( tabs ).append( "    condition-counts: " );

        sb.append( ( conditionCounts == OR_CONDITION ) ? "OR\n" : "AND\n" );

        if ( elements != null )
        {
            sb.append( elements.toString( tabs + "    " ) ).append( '\n' );
        }

        sb.append( tabs + "}\n" );

        return sb.toString();
    }
}
