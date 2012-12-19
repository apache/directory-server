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


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.tlv.BerValue;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.messages.KerberosMessage;


/**
 * The KDC-REQ data structure. It will store the object described by the ASN.1 grammar :
 * <pre>
 * KDC-REQ    ::= SEQUENCE {
 *      -- NOTE: first tag is [1], not [0]
 *      pvno            [1] INTEGER (5) ,
 *      msg-type        [2] INTEGER (10 -- AS -- | 12 -- TGS --),
 *      padata          [3] SEQUENCE OF <PA-DATA> OPTIONAL
                            -- NOTE: not empty --,
 *      req-body        [4] <KDC-REQ-BODY>
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class KdcReq extends KerberosMessage
{
    /** The PA-DATAs */
    private List<PaData> paData;

    /** The KDC-REQ-BODY */
    private KdcReqBody kdcReqBody;

    // Storage for computed lengths
    private int pvnoLength;
    private int msgTypeLength;
    private int paDataLength;
    private int paDataSeqLength;
    private int[] paDataLengths;
    private int kdcReqBodyLength;
    private int kdcReqSeqLength;
    private int kdcReqLength;


    /**
     * Creates a new instance of KDC-REQ.
     */
    public KdcReq( KerberosMessageType msgType )
    {
        super( msgType );
        paData = new ArrayList<PaData>();
    }


    /**
     * @return the pvno
     */
    public int getPvno()
    {
        return getProtocolVersionNumber();
    }


    /**
     * @param pvno the pvno to set
     */
    public void setPvno( int pvno )
    {
        setProtocolVersionNumber( pvno );
    }


    /**
     * @return the paData
     */
    public List<PaData> getPaData()
    {
        return paData;
    }


    /**
     * @param paData the paData to set
     */
    public void addPaData( PaData paData )
    {
        this.paData.add( paData );
    }


    /**
     * @return the kdcReqBody
     */
    public KdcReqBody getKdcReqBody()
    {
        return kdcReqBody;
    }


    /**
     * @param kdcReqBody the kdcReqBody to set
     */
    public void setKdcReqBody( KdcReqBody kdcReqBody )
    {
        this.kdcReqBody = kdcReqBody;
    }


    /**
     * Compute the KDC-REQ length
     * <pre>
     * KDC-REQ :
     * 
     * 0x30 L1 KDC-REQ sequence
     *  |
     *  +--> 0xA1 0x03 pvno tag
     *  |     |
     *  |     +--> 0x02 0x01 0x05 pvno (5)
     *  |
     *  +--> 0xA2 0x03 msg-type tag
     *  |     |
     *  |     +--> 0x02 0x01 0x0A/0x0C msg-type : either AS-REQ (0x0A) or TGS-REQ (0x0C)
     *  |     
     *  +--> 0xA3 L2 pa-data tag
     *  |     |
     *  |     +--> 0x30 L2-1 pa-data SEQ
     *  |           |
     *  |           +--> 0x30 L2-1-1 pa-data
     *  |           |
     *  |           +--> 0x30 L2-1-2 pa-data
     *  |           :
     *  |     
     *  +--> 0xA4 L3 req-body tag
     *  |     |
     *  |     +--> 0x30 L3-1 req-body (KDC-REQ-BODY)
     * </pre>       
     */
    public int computeLength()
    {
        // The pvno length
        pvnoLength = 1 + 1 + 1;
        kdcReqSeqLength = 1 + TLV.getNbBytes( pvnoLength ) + pvnoLength;

        // The msg-type length
        msgTypeLength = 1 + 1 + 1;
        kdcReqSeqLength += 1 + TLV.getNbBytes( msgTypeLength ) + msgTypeLength;

        // Compute the pa-data length.
        if ( paData.size() > 0 )
        {
            paDataLengths = new int[paData.size()];
            int pos = 0;
            paDataSeqLength = 0;

            for ( PaData paDataElem : paData )
            {
                paDataLengths[pos] = paDataElem.computeLength();
                paDataSeqLength += paDataLengths[pos];
                pos++;
            }

            paDataLength = 1 + TLV.getNbBytes( paDataSeqLength ) + paDataSeqLength;
            kdcReqSeqLength += 1 + TLV.getNbBytes( paDataLength ) + paDataLength;
        }

        // The KDC-REQ-BODY length
        kdcReqBodyLength = kdcReqBody.computeLength();
        kdcReqSeqLength += 1 + TLV.getNbBytes( kdcReqBodyLength ) + kdcReqBodyLength;

        // compute the global size
        kdcReqLength = 1 + TLV.getNbBytes( kdcReqSeqLength ) + kdcReqSeqLength;

        return kdcReqLength;
    }


    /**
     * Encode the KDC-REQ component
     * 
     * @param buffer The buffer containing the encoded result
     * @return The encoded component
     * @throws EncoderException If the encoding failed
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_148 ) );
        }

        // The KDC-REQ SEQ Tag
        buffer.put( UniversalTag.SEQUENCE.getValue() );
        buffer.put( TLV.getBytes( kdcReqSeqLength ) );

        // The PVNO -----------------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REQ_PVNO_TAG );
        buffer.put( TLV.getBytes( pvnoLength ) );

        // The value
        BerValue.encode( buffer, getProtocolVersionNumber() );

        // The msg-type if any ------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REQ_MSG_TYPE_TAG );
        buffer.put( TLV.getBytes( msgTypeLength ) );

        // The value
        BerValue.encode( buffer, getMessageType().getValue() );

        // The PD-DATA if any -------------------------------------------------
        if ( paData.size() > 0 )
        {
            // The tag
            buffer.put( ( byte ) KerberosConstants.KDC_REQ_PA_DATA_TAG );
            buffer.put( TLV.getBytes( paDataLength ) );

            // The sequence
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( paDataSeqLength ) );

            // The values
            for ( PaData paDataElem : paData )
            {
                paDataElem.encode( buffer );
            }
        }

        // The KDC-REQ-BODY ---------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REQ_KDC_REQ_BODY_TAG );
        buffer.put( TLV.getBytes( kdcReqBodyLength ) );

        // The value
        kdcReqBody.encode( buffer );

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if ( getMessageType() == KerberosMessageType.AS_REQ )
        {
            sb.append( "AS-REQ" ).append( '\n' );
        }
        else if ( getMessageType() == KerberosMessageType.TGS_REQ )
        {
            sb.append( "TGS-REQ" ).append( '\n' );
        }
        else
        {
            sb.append( "Unknown" ).append( '\n' );
        }

        sb.append( "pvno : " ).append( getProtocolVersionNumber() ).append( '\n' );

        sb.append( "msg-type : " );

        for ( PaData paDataElem : paData )
        {
            sb.append( "padata : " ).append( paDataElem ).append( '\n' );
        }

        sb.append( "kdc-req-body : " ).append( kdcReqBody ).append( '\n' );

        return sb.toString();
    }
}
