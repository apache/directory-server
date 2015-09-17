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
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The AdKdcIssued structure is used to store a AD-KDCIssued associated to a type.
 * 
 * The ASN.1 grammar is :
 * <pre>
 * AD-KDCIssued            ::= SEQUENCE {
 *         ad-checksum     [0] Checksum,
 *         i-realm         [1] Realm OPTIONAL,
 *         i-sname         [2] PrincipalName OPTIONAL,
 *         elements        [3] AuthorizationData
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AdKdcIssued implements Asn1Object
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( AdKdcIssued.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The checksum */
    private Checksum adChecksum;

    /** The realm */
    private String irealm;

    /** The PrincipalName */
    private PrincipalName isname;

    /** The AuthorizationData */
    private AuthorizationData elements;

    // Storage for computed lengths
    private int adCheksumTagLength;
    private int irealmTagLength;
    private byte[] irealmBytes;
    private int isnameTagLength;
    private int elementsTagLength;
    private int adKdcIssuedSeqLength;


    /**
     * Creates a new instance of AdKdcIssued
     */
    public AdKdcIssued()
    {
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
     * @return the adChecksum
     */
    public Checksum getAdChecksum()
    {
        return adChecksum;
    }


    /**
     * @param adChecksum the adChecksum to set
     */
    public void setAdChecksum( Checksum adChecksum )
    {
        this.adChecksum = adChecksum;
    }


    /**
     * @return the irealm
     */
    public String getIRealm()
    {
        return irealm;
    }


    /**
     * @param irealm the irealm to set
     */
    public void setIRealm( String irealm )
    {
        this.irealm = irealm;
    }


    /**
     * @return the isname
     */
    public PrincipalName getISName()
    {
        return isname;
    }


    /**
     * @param isname the isname to set
     */
    public void setISName( PrincipalName isname )
    {
        this.isname = isname;
    }


    /**
     * Compute the AD-KDCIssued length
     * <pre>
     * 0x30 L1 AD-KDCIssued sequence
     *  |
     *  +--> 0xA1 L2 ad-checksum tag
     *  |     |
     *  |     +--> 0x30 L2-1 ad-checksum value ( Checksum )
     *  |
     *  +--> 0xA2 L3 i-realm tag
     *  |     |
     *  |     +--> 0x1B L3-1 i-realm value ( KerberosString )
     *  |
     *  +--> 0xA3 L4 i-sname tag
     *  |     |
     *  |     +--> 0x30 L4-1 i-sname value ( PrincipalName )
     *  |
     *  +--> 0xA4 L5 elements tag
     *        |
     *        +--> 0x30 L5-1 elements (AuthorizationData)
     * </pre>
     */
    @Override
    public int computeLength()
    {
        // Compute the ad-cheksum count length
        adCheksumTagLength = adChecksum.computeLength();
        adKdcIssuedSeqLength = 1 + TLV.getNbBytes( adCheksumTagLength ) + adCheksumTagLength;

        // Compute the i-realm length, if any
        if ( irealm != null )
        {
            irealmBytes = irealm.getBytes();
            irealmTagLength = 1 + TLV.getNbBytes( irealmBytes.length ) + irealmBytes.length;
            adKdcIssuedSeqLength += 1 + TLV.getNbBytes( irealmTagLength ) + irealmTagLength;
        }

        // Compute the i-sname length, if any
        if ( isname != null )
        {
            isnameTagLength = isname.computeLength();
            adKdcIssuedSeqLength += 1 + TLV.getNbBytes( isnameTagLength ) + isnameTagLength;
        }

        // Compute the elements count length
        elementsTagLength = elements.computeLength();
        adKdcIssuedSeqLength += 1 + TLV.getNbBytes( elementsTagLength ) + elementsTagLength;

        // Compute the whole sequence length
        return 1 + TLV.getNbBytes( adKdcIssuedSeqLength ) + adKdcIssuedSeqLength;
    }


    /**
     * Encode the AD-KDCIssued message to a PDU.
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
            // The AD-KDCIssued SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( adKdcIssuedSeqLength ) );

            // the ad-checksum
            buffer.put( ( byte ) KerberosConstants.AD_KDC_ISSUED_AD_CHECKSUM_TAG );
            buffer.put( ( byte ) adCheksumTagLength );
            adChecksum.encode( buffer );

            // the i-realm, if any
            if ( irealm != null )
            {
                buffer.put( ( byte ) KerberosConstants.AD_KDC_ISSUED_I_REALM_TAG );
                buffer.put( ( byte ) irealmTagLength );
                buffer.put( UniversalTag.GENERAL_STRING.getValue() );
                buffer.put( ( byte ) irealmBytes.length );
                buffer.put( irealmBytes );
            }

            // the i-sname, if any
            if ( isname != null )
            {
                buffer.put( ( byte ) KerberosConstants.AD_KDC_ISSUED_I_SNAME_TAG );
                buffer.put( ( byte ) isnameTagLength );
                isname.encode( buffer );
            }

            // the elements
            buffer.put( ( byte ) KerberosConstants.AD_KDC_ISSUED_ELEMENTS_TAG );
            buffer.put( ( byte ) elementsTagLength );
            elements.encode( buffer );
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_139, 1 + TLV.getNbBytes( adKdcIssuedSeqLength )
                + adKdcIssuedSeqLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ), boe );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "AD-KDCIssued encoding : {}", Strings.dumpBytes( buffer.array() ) );
            LOG.debug( "AD-KDCIssued initial value : {}", toString() );
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

        sb.append( tabs ).append( "AD-KDCIssued : {\n" );
        sb.append( tabs ).append( "    ad-cheksum: " ).append( adChecksum.toString( tabs + "    " ) ).append( '\n' );

        if ( irealm != null )
        {
            sb.append( tabs ).append( "    i-realm: " ).append( irealm ).append( '\n' );
        }

        if ( isname != null )
        {
            sb.append( tabs ).append( "    i-sname: " ).append( isname.toString() ).append( '\n' );
        }

        sb.append( tabs + "    elements:" ).append( elements.toString( tabs + "    " ) ).append( '\n' );
        sb.append( tabs + "}\n" );

        return sb.toString();
    }
}
