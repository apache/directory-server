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
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.codec.types.AuthorizationType;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A structure to hold the authorization data.
 * 
 * <pre>
 * AuthorizationData      ::= SEQUENCE OF SEQUENCE {
 *               ad-type  [0] Int32,
 *               ad-data  [1] OCTET STRING
 * }
 *</pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AuthorizationData extends AbstractAsn1Object
{
    /** The list of AuthorizationData elements */
    private List<AuthorizationDataEntry> authorizationData = new ArrayList<AuthorizationDataEntry>();
    
    /** The current AD being processed */
    private AuthorizationDataEntry currentAD;

    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( EncryptedData.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    // Storage for computed lengths
    private int adTypeTagLen[];
    private int adDataTagLen[];
    private int authorizationDataSeqLen[];
    private int authorizationDataSeqSeqLen;


    /**
     * Creates a new set of AuthorizationData
     */
    public AuthorizationData()
    {
    }
    
    
    /**
     * Compute the AuthorizationData length
     * <pre>
     * 0x30 L1 AuthorizationData sequence
     *  |
     *  +-- 0x30 L2 The AD sequence
     *       |
     *       +--> 0xA0 L3 adType tag
     *       |     |
     *       |     +--> 0x02 L3-1 adType (int)
     *       |
     *       +--> 0xA1 L4 adData tag
     *             |
     *             +--> 0x04 L<4-1 adData (OCTET STRING)
     * </pre>
     */
    @Override
    public int computeLength()
    {
        int i = 0;
        authorizationDataSeqLen = new int[authorizationData.size()];
        adTypeTagLen = new int[authorizationData.size()];
        adDataTagLen = new int[authorizationData.size()];
        authorizationDataSeqSeqLen = 0;
        
        for ( AuthorizationDataEntry ad : authorizationData )
        {
            int adTypeLen = Value.getNbBytes( ad.getAdType().getValue() );
            adTypeTagLen[i] = 1 + TLV.getNbBytes( adTypeLen ) + adTypeLen;
            adDataTagLen[i] = 1 + TLV.getNbBytes( ad.getAdDataRef().length ) + ad.getAdDataRef().length;
            
            authorizationDataSeqLen[i] = 1 + TLV.getNbBytes( adTypeTagLen[i] ) + adTypeTagLen[i] + 
                                         1 + TLV.getNbBytes( adDataTagLen[i] ) + adDataTagLen[i];
            
            authorizationDataSeqSeqLen += 1 + TLV.getNbBytes( authorizationDataSeqLen[i] ) + authorizationDataSeqLen[i];
            i++;
        }

        return 1 + TLV.getNbBytes(authorizationDataSeqSeqLen) + authorizationDataSeqSeqLen;
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
            // The AuthorizationData SEQ OF Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( authorizationDataSeqSeqLen ) );
            
            int i = 0;
            
            for ( AuthorizationDataEntry ad : authorizationData )
            {
                buffer.put( UniversalTag.SEQUENCE.getValue() );
                buffer.put( TLV.getBytes( authorizationDataSeqLen[i] ) );
                
                // the adType
                buffer.put( ( byte ) KerberosConstants.AUTHORIZATION_DATA_ADTYPE_TAG );
                buffer.put( TLV.getBytes( adTypeTagLen[i] ) );
                Value.encode( buffer, ad.getAdType().getValue() );
    
                // the adData
                buffer.put( ( byte ) KerberosConstants.AUTHORIZATION_DATA_ADDATA_TAG );
                buffer.put( TLV.getBytes( adDataTagLen[i] ) );
                Value.encode( buffer, ad.getAdDataRef() );
                
                i++;
            }
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_139, 1 + TLV.getNbBytes( authorizationDataSeqSeqLen )
                + authorizationDataSeqSeqLen, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "AuthorizationData encoding : {}", Strings.dumpBytes(buffer.array()) );
            LOG.debug( "AuthorizationData initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @return the currentAD type
     */
    public AuthorizationType getCurrentAdType()
    {
        return currentAD.getAdType();
    }


    /**
     * Set the current AD type
     */
    public void setCurrentAdType( AuthorizationType adType )
    {
        currentAD.setAdType( adType );
    }


    /**
     * @return the currentAD data
     */
    public byte[] getCurrentAdData()
    {
        return currentAD.getAdData();
    }


    /**
     * Set the current AD data
     */
    public void setCurrentAdData( byte[] adData )
    {
        currentAD.setAdData( adData );
    }


    /**
     * @return the currentAD
     */
    public AuthorizationDataEntry getCurrentAD()
    {
        return currentAD;
    }


    /**
     * Create a new currentAD
     */
    public void createNewAD()
    {
        currentAD = new AuthorizationDataEntry();
        authorizationData.add( currentAD );
    }


    /**
     * Add a new AuthorizationDataEntry
     */
    public void addEntry( AuthorizationDataEntry entry)
    {
        authorizationData.add( entry );
    }


    /**
     * @return the authorizationData
     */
    public List<AuthorizationDataEntry> getAuthorizationData()
    {
        return authorizationData;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( authorizationData == null ) ? 0 : authorizationData.hashCode() );
        result = prime * result + ( ( currentAD == null ) ? 0 : currentAD.hashCode() );
        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( obj == null )
        {
            return false;
        }
        
        AuthorizationData other = ( AuthorizationData ) obj;
        
        if ( authorizationData == null )
        {
            if ( other.authorizationData != null )
            {
                return false;
            }
        }
        else if ( !authorizationData.equals( other.authorizationData ) )
        {
            return false;
        }
        
        if ( currentAD == null )
        {
            if ( other.currentAD != null )
            {
                return false;
            }
        }
        else if ( !currentAD.equals( other.currentAD ) )
        {
            return false;
        }
        
        return true;
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "AuthorizationData : \n" );
        
        for ( AuthorizationDataEntry ad : authorizationData )
        {
            sb.append( ad.toString( tabs + "    " ) );
        }

        return sb.toString();
    }
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}
