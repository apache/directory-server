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

import org.apache.directory.api.asn1.AbstractAsn1Object;
import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.tlv.BerValue;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * EncKrbCredPart  ::= [APPLICATION 29] SEQUENCE {
 *      ticket-info     [0] SEQUENCE OF KrbCredInfo,
 *      nonce           [1] UInt32 OPTIONAL,
 *      timestamp       [2] KerberosTime OPTIONAL,
 *      usec            [3] Microseconds OPTIONAL,
 *      s-address       [4] HostAddress OPTIONAL,
 *      r-address       [5] HostAddress OPTIONAL
 * }
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncKrbCredPart extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( EncKrbCredPart.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** list of KrbCredInfo */
    private List<KrbCredInfo> ticketInfo;

    /** the nonce */
    private Integer nonce;

    /** the timestamp */
    private KerberosTime timestamp;

    /** the microseconds part of the timestamp */
    private Integer usec;

    /** the sender's address */
    private HostAddress senderAddress;

    /** the recipient's address */
    private HostAddress recipientAddress;

    private int ticketInfoSeqLen;
    private int ticketInfoLen;
    private int nonceLen;
    private int timestampLen;
    private byte[] timestampBytes;
    private int usecLen;
    private int senderAddressLen;
    private int recipientAddressLen;
    private int encKrbCredPartSeqLen;
    private int encKrbCredPartLen;


    /**
     * computing length of EncKrbCredPart:
     * 
     * <pre>
     *  0x7D L1
     *   |
     *   +--> 0x30 L1-2 EncKrbCredPart seq tag
     *         |
     *         +--> 0xA0 L2 seq of KrbCredInfo tag
     *         |     |
     *         |     +--> 0x30 L2-2 seq tag
     *         |     |
     *         |     +--> 0x30 LL1 KrbCredInfo
     *         |     .      ....
     *         |     +--> 0x30 LLn KrbCredInfo
     *         |
     *         +--> 0xA1 L3 nonce tag
     *         |     |
     *         |     +--> 0x02 L3-2 nonce (UInt32)
     *         |
     *         +--> 0xA2 11 timestamp tag
     *         |     |
     *         |     +--> 0x18 0x0F timestamp (KerberosTime)
     *         |
     *         +--> 0xA3 L4 usec tag
     *         |     |
     *         |     +--> 0x02 L4-2 usec (Microseconds)
     *         |
     *         +--> 0xA4 L5 s-address tag
     *         |     |
     *         |     +--> 0x30 L5-2 s-address (HostAddress)
     *         |
     *         +--> 0xA5 L6 r-address tag
     *               |
     *               +--> 0x30 L6-2 s-address (HostAddress) 
     *   
     * </pre> 
     */
    @Override
    public int computeLength()
    {
        for ( KrbCredInfo kci : ticketInfo )
        {
            ticketInfoSeqLen += kci.computeLength();
        }

        ticketInfoLen = 1 + TLV.getNbBytes( ticketInfoSeqLen ) + ticketInfoSeqLen;

        encKrbCredPartSeqLen = 1 + TLV.getNbBytes( ticketInfoLen ) + ticketInfoLen;

        if ( nonce != null )
        {
            nonceLen = BerValue.getNbBytes( nonce );
            nonceLen = 1 + TLV.getNbBytes( nonceLen ) + nonceLen;
            encKrbCredPartSeqLen += 1 + TLV.getNbBytes( nonceLen ) + nonceLen;
        }

        if ( timestamp != null )
        {
            timestampBytes = timestamp.getBytes();
            timestampLen = 1 + TLV.getNbBytes( timestampBytes.length ) + timestampBytes.length;
            encKrbCredPartSeqLen += 1 + TLV.getNbBytes( timestampLen ) + timestampLen;
        }

        if ( usec != null )
        {
            usecLen = BerValue.getNbBytes( usec );
            usecLen = 1 + TLV.getNbBytes( usecLen ) + usecLen;
            encKrbCredPartSeqLen += 1 + TLV.getNbBytes( usecLen ) + usecLen;
        }

        if ( senderAddress != null )
        {
            senderAddressLen = senderAddress.computeLength();
            encKrbCredPartSeqLen += 1 + TLV.getNbBytes( senderAddressLen ) + senderAddressLen;
        }

        if ( recipientAddress != null )
        {
            recipientAddressLen = recipientAddress.computeLength();
            encKrbCredPartSeqLen += 1 + TLV.getNbBytes( recipientAddressLen ) + recipientAddressLen;
        }

        encKrbCredPartLen = 1 + TLV.getNbBytes( encKrbCredPartSeqLen ) + encKrbCredPartSeqLen;

        return 1 + TLV.getNbBytes( encKrbCredPartLen ) + encKrbCredPartLen;
    }


    /**
     * {@inheritDoc}
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
            //EncKrbCredPart application tag
            buffer.put( ( byte ) KerberosConstants.ENC_KRB_CRED_PART_TAG );
            buffer.put( TLV.getBytes( encKrbCredPartLen ) );

            //EncKrbCredPart sequence tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( encKrbCredPartSeqLen ) );

            // ticket-info tag
            buffer.put( ( byte ) KerberosConstants.ENC_KRB_CRED_TICKET_INFO_TAG );
            buffer.put( TLV.getBytes( ticketInfoLen ) );

            // sequence of ticket-info seq tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( ticketInfoSeqLen ) );

            for ( KrbCredInfo ki : ticketInfo )
            {
                ki.encode( buffer );
            }

            if ( nonce != null )
            {
                // nonce tag and value
                buffer.put( ( byte ) KerberosConstants.ENC_KRB_CRED_PART_NONCE_TAG );
                buffer.put( TLV.getBytes( nonceLen ) );
                BerValue.encode( buffer, nonce );
            }

            if ( timestamp != null )
            {
                // timestamp tag and value
                buffer.put( ( byte ) KerberosConstants.ENC_KRB_CRED_PART_TIMESTAMP_TAG );
                buffer.put( TLV.getBytes( timestampLen ) );

                buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
                buffer.put( ( byte ) 0x0F );
                buffer.put( timestampBytes );
            }

            if ( usec != null )
            {
                // usec tag and value
                buffer.put( ( byte ) KerberosConstants.ENC_KRB_CRED_PART_USEC_TAG );
                buffer.put( TLV.getBytes( usecLen ) );
                BerValue.encode( buffer, usec );
            }

            if ( senderAddress != null )
            {
                // s-address tag and value
                buffer.put( ( byte ) KerberosConstants.ENC_KRB_CRED_PART_SENDER_ADDRESS_TAG );
                buffer.put( TLV.getBytes( senderAddressLen ) );
                senderAddress.encode( buffer );
            }

            if ( recipientAddress != null )
            {
                // r-address tag and value
                buffer.put( ( byte ) KerberosConstants.ENC_KRB_CRED_PART_RECIPIENT_ADDRESS_TAG );
                buffer.put( TLV.getBytes( recipientAddressLen ) );
                recipientAddress.encode( buffer );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_740_CANNOT_ENCODE_ENC_KRB_CRED_PART, 1 + TLV.getNbBytes( encKrbCredPartLen )
                + encKrbCredPartLen, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            log.debug( "EncKrbCredPart encoding : {}", Strings.dumpBytes( buffer.array() ) );
            log.debug( "EncKrbCredPart initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @return the ticketInfo
     */
    public List<KrbCredInfo> getTicketInfo()
    {
        return ticketInfo;
    }


    /**
     * @param ticketInfo the ticketInfo to set
     */
    public void setTicketInfo( List<KrbCredInfo> ticketInfo )
    {
        this.ticketInfo = ticketInfo;
    }


    /**
     * @return the nonce
     */
    public Integer getNonce()
    {
        return nonce;
    }


    /**
     * @param nonce the nonce to set
     */
    public void setNonce( Integer nonce )
    {
        this.nonce = nonce;
    }


    /**
     * @return the timestamp
     */
    public KerberosTime getTimestamp()
    {
        return timestamp;
    }


    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp( KerberosTime timestamp )
    {
        this.timestamp = timestamp;
    }


    /**
     * @return the usec
     */
    public Integer getUsec()
    {
        return usec;
    }


    /**
     * @param usec the usec to set
     */
    public void setUsec( Integer usec )
    {
        this.usec = usec;
    }


    /**
     * @return the senderAddress
     */
    public HostAddress getSenderAddress()
    {
        return senderAddress;
    }


    /**
     * @param senderAddress the senderAddress to set
     */
    public void setSenderAddress( HostAddress senderAddress )
    {
        this.senderAddress = senderAddress;
    }


    /**
     * @return the recipientAddress
     */
    public HostAddress getRecipientAddress()
    {
        return recipientAddress;
    }


    /**
     * @param recipientAddress the recipientAddress to set
     */
    public void setRecipientAddress( HostAddress recipientAddress )
    {
        this.recipientAddress = recipientAddress;
    }


    /**
     * add KrbCredInfo object to the existing list of ticket-info
     *
     * @param info the KrbCredInfo
     */
    public void addTicketInfo( KrbCredInfo info )
    {
        if ( info == null )
        {
            throw new IllegalArgumentException();
        }

        if ( ticketInfo == null )
        {
            ticketInfo = new ArrayList<KrbCredInfo>();
        }

        ticketInfo.add( info );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "EncKrbCredPart : {\n" );

        sb.append( "    ticketInfo: " ).append( ticketInfo ).append( '\n' );

        if ( nonce != null )
        {
            sb.append( "    nonce: " ).append( nonce ).append( '\n' );
        }

        if ( timestamp != null )
        {
            sb.append( "    timestamp: " ).append( timestamp ).append( '\n' );
        }

        if ( usec != null )
        {
            sb.append( "    usec: " ).append( usec ).append( '\n' );
        }

        if ( senderAddress != null )
        {
            sb.append( "    senderAddress: " ).append( senderAddress ).append( '\n' );
        }

        if ( recipientAddress != null )
        {
            sb.append( "    recipientAddress: " ).append( recipientAddress ).append( '\n' );
        }

        sb.append( "}\n" );

        return sb.toString();
    }
}
