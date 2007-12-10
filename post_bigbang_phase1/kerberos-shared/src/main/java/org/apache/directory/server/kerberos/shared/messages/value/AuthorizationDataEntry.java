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

import org.apache.directory.server.kerberos.shared.messages.value.types.AuthorizationType;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A single AuthorizationData
 * 
 * The ASN.1 grammar is :
 * -- NOTE: AuthorizationData is always used as an OPTIONAL field and
 * -- should not be empty.
 * AuthorizationDataEntry       ::= SEQUENCE {
 *        ad-type         [0] Int32,
 *        ad-data         [1] OCTET STRING
 * }
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AuthorizationDataEntry extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( AuthorizationDataEntry.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The Authorization type. One of :
     * DER encoding of AD-IF-RELEVANT        1
     * DER encoding of AD-KDCIssued          4
     * DER encoding of AD-AND-OR             5
     * DER encoding of AD-MANDATORY-FOR-KDC  8 
     **/
    private AuthorizationType adType;

    /** The data, encrypted */
    private byte[] adData;

    // Storage for computed lengths
    private transient int adTypeLength;
    private transient int adDataLength;
    private transient int authorizationDataEntryLength;


    /**
     * Creates a new instance of AuthorizationDataEntry.
     */
    public AuthorizationDataEntry()
    {
    }

    
    /**
     * Creates a new instance of AuthorizationDataEntry.
     *
     * @param adType The authorizationType
     * @param adData The authorization data
     */
    public AuthorizationDataEntry( AuthorizationType adType, byte[] adData )
    {
        this.adType = adType;
        this.adData = adData;
    }


    /**
     * Returns the raw bytes of the authorization data.
     *
     * @return The raw bytes of the authorization data.
     */
    public byte[] getAdData()
    {
        return adData;
    }


    /**
     * Set the authorization data
     * 
     * @param adData The data
     */
    public void setAdData( byte[] adData ) 
    {
        this.adData = adData;
    }

    
    /**
     * Returns the {@link AuthorizationType}.
     *
     * @return The {@link AuthorizationType}.
     */
    public AuthorizationType getAdType()
    {
        return adType;
    }


    /**
     * Set the authorization type
     * @param adType The authorization type
     */
    public void setAdType( int adType ) 
    {
        this.adType = AuthorizationType.getTypeByOrdinal( adType );
    }

    
    /**
     * Set the authorization type
     * @param adType The authorization type
     */
    public void setAdType( AuthorizationType adType ) 
    {
        this.adType = adType;
    }

    
    /**
     * Compute the AuthorizationDataEntry length
     * 
     * AuthorizationDataEntry :
     * 
     * 0x30 L1 AuthorizationDataEntry
     *  |
     *  +--> 0xA0 L2 adType tag
     *  |     |
     *  |     +--> 0x02 L2-1 adType (int)
     *  |
     *  +--> 0xA1 L3 adData tag
     *        |
     *        +--> 0x04 L3-1 adData (OCTET STRING)
     *        
     *  where L1 = L2 + lenght(0xA0) + length(L2) +
     *             L3 + lenght(0xA1) + length(L3) 
     *  and
     *  L2 = L2-1 + length(0x02) + length( L2-1) 
     *  L3 = L3-1 + length(0x04) + length( L3-1) 
     */
    public int computeLength()
    {
        // Compute the adType. The Length will always be contained in 1 byte
        adTypeLength = 1 + 1 + Value.getNbBytes( adType.getOrdinal() );
        authorizationDataEntryLength = 1 + TLV.getNbBytes( adTypeLength ) + adTypeLength;

        // Compute the keyValue
        if ( adData == null )
        {
            adDataLength = 1 + 1;
        }
        else
        {
            adDataLength = 1 + TLV.getNbBytes( adData.length ) + adData.length;
        }

        authorizationDataEntryLength += 1 + TLV.getNbBytes( adDataLength ) + adDataLength;

        // Compute the whole sequence length
        int authorizationDataEntrySeqLength = 1 + Value.getNbBytes( authorizationDataEntryLength )
            + authorizationDataEntryLength;

        return authorizationDataEntrySeqLength;

    }


    /**
     * Encode the AuthorizationDataEntry message to a PDU. 
     * 
     * AuthorizationDataEntry :
     * 
     * 0x30 LL
     *   0xA0 LL 
     *     0x02 0x01 adType
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
            buffer.put( TLV.getBytes( authorizationDataEntryLength ) );

            // The adType, first the tag, then the value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( adTypeLength ) );
            Value.encode( buffer, adType.getOrdinal() );

            // The adData, first the tag, then the value
            buffer.put( ( byte ) 0xA1 );
            buffer.put( TLV.getBytes( adDataLength ) );
            Value.encode( buffer, adData );
        }
        catch ( BufferOverflowException boe )
        {
            log
                .error(
                    "Cannot encode the AuthorizationDataEntry object, the PDU size is {} when only {} bytes has been allocated",
                    1 + TLV.getNbBytes( authorizationDataEntryLength ) + authorizationDataEntryLength, buffer
                        .capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "AuthorizationDataEntry encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "AuthorizationDataEntry initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "    " );
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "AuthorizationDataEntry : {\n" );
        sb.append( tabs ).append( "    ad-type: " ).append( adType ).append( '\n' );

        sb.append( tabs ).append( "    ad-data: " ).append( StringTools.dumpBytes( adData ) )
            .append( "\n" + tabs + "}" );

        return sb.toString();
    }
}
