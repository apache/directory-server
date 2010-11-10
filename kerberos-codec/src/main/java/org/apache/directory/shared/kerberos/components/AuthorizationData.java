/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.kerberos.components;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A structure to hold the authorization data.
 * 
 * AuthorizationData      ::= SEQUENCE OF SEQUENCE {
 *               ad-type  [0] Int32,
 *               ad-data  [1] OCTET STRING
 * }
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthorizationData extends AbstractAsn1Object
{

    /** the type of authorization data */
    private int adType;

    /** the authorization data */
    private byte[] adData;

    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( EncryptedData.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    private transient int adTypeTagLen;
    private transient int adDataTagLen;
    private transient int authorizationDataSeqLen;


    public AuthorizationData()
    {
    }


    public AuthorizationData( int adType, byte[] adData )
    {
        this.adType = adType;
        this.adData = adData;
    }


    /**
     * Compute the AuthorizationData length
     * 
     * 0x30 L1 AuthorizationData sequence
     * |
     * +--> 0xA1 L2 adType tag
     * |     |
     * |     +--> 0x02 L2-1 adType (int)
     * |
     * +--> 0xA2 L3 adData tag
     *       |
     *       +--> 0x04 L3-1 adData (OCTET STRING)
     * 
     */
    @Override
    public int computeLength()
    {
        int adTypeLen = Value.getNbBytes( adType );
        adTypeTagLen = 1 + TLV.getNbBytes( adTypeLen ) + adTypeLen;

        adDataTagLen = 1 + TLV.getNbBytes( adData.length ) + adData.length;

        authorizationDataSeqLen = 1 + TLV.getNbBytes( adTypeTagLen ) + adTypeTagLen;
        authorizationDataSeqLen += 1 + TLV.getNbBytes( adDataTagLen ) + adDataTagLen;

        return 1 + TLV.getNbBytes( authorizationDataSeqLen ) + authorizationDataSeqLen;
    }


    /**
     * Encode the EncryptedData message to a PDU.
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
            // The AuthorizationData SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( authorizationDataSeqLen ) );

            // the adType
            buffer.put( ( byte ) KerberosConstants.AUTHORIZATION_DATA_ADTYPE_TAG );
            buffer.put( TLV.getBytes( adTypeTagLen ) );

            Value.encode( buffer, adType );

            // the adData
            // the adType
            buffer.put( ( byte ) KerberosConstants.AUTHORIZATION_DATA_ADDATA_TAG );
            buffer.put( TLV.getBytes( adDataTagLen ) );

            Value.encode( buffer, adData );
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_139, 1 + TLV.getNbBytes( authorizationDataSeqLen )
                + authorizationDataSeqLen, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "AuthorizationData encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            LOG.debug( "AuthorizationData initial value : {}", toString() );
        }

        return buffer;
    }


    public int getAdType()
    {
        return adType;
    }


    public void setAdType( int adType )
    {
        this.adType = adType;
    }


    public byte[] getAdData()
    {
        return adData;
    }


    public void setAdData( byte[] adData )
    {
        this.adData = adData;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "AuthorizationData : {\n" );
        sb.append( "    adtype: " ).append( adType ).append( '\n' );

        sb.append( "    adData: " ).append( StringTools.dumpBytes( adData ) ).append( "\n}\n" );

        return sb.toString();
    }
}
