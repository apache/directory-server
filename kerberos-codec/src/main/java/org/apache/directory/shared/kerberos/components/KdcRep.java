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

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.BerValue;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.messages.KerberosMessage;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.apache.directory.shared.util.Strings;


/**
 * The KDC-REP data structure. It will store the object described by the ASN.1 grammar :
 * <pre>
 * KDC-REP         ::= SEQUENCE {
 *         pvno            [0] INTEGER (5),
 *         msg-type        [1] INTEGER (11 -- AS -- | 13 -- TGS --),
 *         padata          [2] SEQUENCE OF PA-DATA OPTIONAL
 *                                 -- NOTE: not empty --,
 *         crealm          [3] Realm,
 *         cname           [4] <PrincipalName>,
 *         ticket          [5] <Ticket>,
 *         enc-part        [6] <EncryptedData>
 *                                 -- EncASRepPart or EncTGSRepPart,
 *                                 -- as appropriate
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KdcRep extends KerberosMessage
{
    /** The PA-DATAs */
    private List<PaData> paData;

    /** The client realm */
    private String crealm;

    /** A storage for a byte array representation of the realm */
    private byte[] crealmBytes;

    /** The client principal name */
    private PrincipalName cname;

    /** The ticket tickets */
    private Ticket ticket;

    /** Encoded part */
    private EncryptedData encPart;

    /** The decoded KDC-REP part */
    protected EncKdcRepPart encKdcRepPart;

    // Storage for computed lengths
    private int pvnoLength;
    private int msgTypeLength;
    private int paDataLength;
    private int paDataSeqLength;
    private int[] paDataLengths;
    private int cnameLength;
    private int crealmLength;
    private int ticketLength;
    private int encPartLength;
    private int kdcRepSeqLength;
    private int kdcRepLength;


    /**
     * Creates a new instance of KDC-REP.
     */
    public KdcRep( KerberosMessageType msgType )
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
     * Returns the client realm.
     *
     * @return The client realm.
     */
    public String getCRealm()
    {
        return crealm;
    }


    /**
     * Set the client realm
     * @param crealm the client realm
     */
    public void setCRealm( String crealm )
    {
        this.crealm = crealm;
    }


    /**
     * Returns the client {@link PrincipalName}.
     *
     * @return The client {@link PrincipalName}.
     */
    public PrincipalName getCName()
    {
        return cname;
    }


    /**
     * Set the client principalName
     * @param cname the client principalName
     */
    public void setCName( PrincipalName cname )
    {
        this.cname = cname;
    }


    /**
     * Returns the {@link Ticket}
     *
     * @return The {@link Ticket}
     */
    public Ticket getTicket()
    {
        return ticket;
    }


    /**
     * Set the Ticket
     * @param ticket the ticket to set
     */
    public void setTicket( Ticket ticket )
    {
        this.ticket = ticket;
    }


    /**
     * Returns the encrypted part as {@link EncryptedData}.
     *
     * @return The encrypted part as {@link EncryptedData}.
     */
    public EncryptedData getEncPart()
    {
        return encPart;
    }


    /**
     * @param encPart the encPart to set
     */
    public void setEncPart( EncryptedData encPart )
    {
        this.encPart = encPart;
    }


    /**
     * @return the encKdcRepPart
     */
    public EncKdcRepPart getEncKdcRepPart()
    {
        return encKdcRepPart;
    }


    /**
     * @param encKdcRepPart the encKdcRepPart to set
     */
    public void setEncKdcRepPart( EncKdcRepPart encKdcRepPart )
    {
        this.encKdcRepPart = encKdcRepPart;
    }


    /**
     * Compute the KDC-REP length
     * <pre>
     * KDC-REP :
     * 
     * 0x30 L1 KDC-REP sequence
     *  |
     *  +--> 0xA0 0x03 pvno tag
     *  |     |
     *  |     +--> 0x02 0x01 0x05 pvno (5)
     *  |
     *  +--> 0xA1 0x03 msg-type tag
     *  |     |
     *  |     +--> 0x02 0x01 0x0B/0x0D msg-type : either AS-REP (0x0B) or TGS-REP (0x0D)
     *  |     
     *  +--> 0xA2 L2 pa-data tag
     *  |     |
     *  |     +--> 0x30 L2-1 pa-data SEQ
     *  |           |
     *  |           +--> 0x30 L2-1-1 pa-data
     *  |           |
     *  |           +--> 0x30 L2-1-2 pa-data
     *  |           :
     *  |     
     *  +--> 0xA3 L3 crealm tag
     *  |     |
     *  |     +--> 0x1B L3-1 crealm
     *  |
     *  +--> 0xA4 L4 cname tag
     *  |     |
     *  |     +--> 0x30 L4-1 cname
     *  |
     *  +--> 0xA5 L5 ticket tag
     *  |     |
     *  |     +--> 0x61 L5-1 ticket
     *  |
     *  +--> 0xA6 L6 enc-part tag
     *        |
     *        +--> 0x30 L6-1 enc-part
     *  
     * </pre>       
     */
    public int computeLength()
    {
        // The pvno length
        pvnoLength = 1 + 1 + 1;
        kdcRepSeqLength = 1 + TLV.getNbBytes( pvnoLength ) + pvnoLength;

        // The msg-type length
        msgTypeLength = 1 + 1 + 1;
        kdcRepSeqLength += 1 + TLV.getNbBytes( msgTypeLength ) + msgTypeLength;

        // Compute the pa-data length.
        if ( paData.size() != 0 )
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
            kdcRepSeqLength += 1 + TLV.getNbBytes( paDataLength ) + paDataLength;
        }

        // The crealm length
        crealmBytes = Strings.getBytesUtf8( crealm );
        crealmLength = 1 + TLV.getNbBytes( crealmBytes.length ) + crealmBytes.length;
        kdcRepSeqLength += 1 + TLV.getNbBytes( crealmLength ) + crealmLength;

        // Compute the client principalName length
        cnameLength = cname.computeLength();
        kdcRepSeqLength += 1 + TLV.getNbBytes( cnameLength ) + cnameLength;

        // Compute the ticket length
        ticketLength = ticket.computeLength();
        kdcRepSeqLength += 1 + TLV.getNbBytes( ticketLength ) + ticketLength;

        // Compute the encrypted part
        encPartLength = encPart.computeLength();
        kdcRepSeqLength += 1 + TLV.getNbBytes( encPartLength ) + encPartLength;

        // compute the global size
        kdcRepLength = 1 + TLV.getNbBytes( kdcRepSeqLength ) + kdcRepSeqLength;

        return kdcRepLength;
    }


    /**
     * Encode the KDC-REP component
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

        // The KDC-REP SEQ Tag
        buffer.put( UniversalTag.SEQUENCE.getValue() );
        buffer.put( TLV.getBytes( kdcRepSeqLength ) );

        // The PVNO -----------------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REP_PVNO_TAG );
        buffer.put( TLV.getBytes( pvnoLength ) );

        // The value
        BerValue.encode( buffer, getProtocolVersionNumber() );

        // The MSG-TYPE if any ------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REP_MSG_TYPE_TAG );
        buffer.put( TLV.getBytes( msgTypeLength ) );

        // The value
        BerValue.encode( buffer, getMessageType().getValue() );

        // The PD-DATA if any -------------------------------------------------
        if ( paData.size() != 0 )
        {
            // The tag
            buffer.put( ( byte ) KerberosConstants.KDC_REP_PA_DATA_TAG );
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

        // The CREALM ---------------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REP_CREALM_TAG );
        buffer.put( TLV.getBytes( crealmLength ) );

        // The value
        buffer.put( UniversalTag.GENERAL_STRING.getValue() );
        buffer.put( TLV.getBytes( crealmBytes.length ) );
        buffer.put( crealmBytes );

        // The CNAME ----------------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REP_CNAME_TAG );
        buffer.put( TLV.getBytes( cnameLength ) );

        // The value
        cname.encode( buffer );

        // The TICKET ---------------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REP_TICKET_TAG );
        buffer.put( TLV.getBytes( ticketLength ) );

        // The value
        ticket.encode( buffer );

        // The ENC-PART -------------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REP_ENC_PART_TAG );
        buffer.put( TLV.getBytes( encPartLength ) );

        // The value
        encPart.encode( buffer );

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if ( getMessageType() == KerberosMessageType.AS_REP )
        {
            sb.append( "AS-REP" ).append( '\n' );
        }
        else if ( getMessageType() == KerberosMessageType.TGS_REP )
        {
            sb.append( "TGS-REP" ).append( '\n' );
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

        sb.append( "crealm : " ).append( crealm ).append( '\n' );
        sb.append( "cname : " ).append( cname ).append( '\n' );
        sb.append( "ticket : " ).append( ticket ).append( '\n' );
        sb.append( "enc-part : " ).append( encPart ).append( '\n' );

        return sb.toString();
    }
}
