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

package org.apache.directory.shared.kerberos.messages;


import java.nio.BufferOverflowException;
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
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * KRB-CRED        ::= [APPLICATION 22] SEQUENCE {
 *         pvno            [0] INTEGER (5),
 *         msg-type        [1] INTEGER (22),
 *         tickets         [2] SEQUENCE OF Ticket,
 *         enc-part        [3] EncryptedData -- EncKrbCredPart
 * }
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbCred extends KerberosMessage
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( KrbCred.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** list of tickets */
    private List<Ticket> tickets;

    /** encrypted part of the message */
    private EncryptedData encPart;

    private int pvnoLen;
    private int msgTypeLen;
    private int ticketsSeqLen;
    private int ticketsLen;
    private int encPartLen;
    private int krbCredSeqLen;
    private int krbCredLen;


    /**
     * Creates a new instance of KrbCred.
     */
    public KrbCred()
    {
        super( 5, KerberosMessageType.KRB_CRED );
    }


    /**
     * Compute the KRB-CRED length
     * <pre>
     * KRB-CRED :
     * 
     * 0x76 L1 KRB-CRED APPLICATION[22]
     *  |
     *  +--> 0x30 L2 KRB-CRED sequence
     *        |
     *        +--> 0xA0 0x03 pvno tag
     *        |     |
     *        |     +--> 0x02 0x01 0x05 pvno (5)
     *        |
     *        +--> 0xA1 0x03 msg-type tag
     *        |     |
     *        |     +--> 0x02 0x01 0x16 msg-type (22)
     *        |     
     *        +--> 0xA2 L3 tickets tag
     *        |     |
     *        |     +--> 0x30 LL tickets seq tag
     *        |           |
     *        |           +--> 0x30 LL1 ticket (Ticket)
     *        |           .         ...
     *        |           +--> 0x30 LLn ticket (Ticket)
     *        |
     *        +--> 0xA3 L4 enc-part tag
     *              |
     *              +--> 0x30 L4-2 enc-part (EncryptedData)
     */
    @Override
    public int computeLength()
    {
        pvnoLen = 1 + 1 + 1;
        krbCredSeqLen = 1 + TLV.getNbBytes( pvnoLen ) + pvnoLen;

        msgTypeLen = 1 + 1 + BerValue.getNbBytes( getMessageType().getValue() );
        krbCredSeqLen += 1 + TLV.getNbBytes( msgTypeLen ) + msgTypeLen;

        for ( Ticket t : tickets )
        {
            ticketsSeqLen += t.computeLength();
        }

        ticketsLen = 1 + TLV.getNbBytes( ticketsSeqLen ) + ticketsSeqLen;

        krbCredSeqLen += 1 + TLV.getNbBytes( ticketsLen ) + ticketsLen;

        encPartLen = encPart.computeLength();
        krbCredSeqLen += 1 + TLV.getNbBytes( encPartLen ) + encPartLen;

        krbCredLen = 1 + TLV.getNbBytes( krbCredSeqLen ) + krbCredSeqLen;

        return 1 + TLV.getNbBytes( krbCredLen ) + krbCredLen;
    }


    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_148 ) );
        }

        try
        {
            // The KRB-CRED APPLICATION tag
            buffer.put( ( byte ) KerberosConstants.KRB_CRED_TAG );
            buffer.put( TLV.getBytes( krbCredLen ) );

            // The KRB-CRED sequence
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( krbCredSeqLen ) );

            // pvno tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_CRED_PVNO_TAG );
            buffer.put( TLV.getBytes( pvnoLen ) );
            BerValue.encode( buffer, getProtocolVersionNumber() );

            // msg-type tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_CRED_MSGTYPE_TAG );
            buffer.put( TLV.getBytes( msgTypeLen ) );
            BerValue.encode( buffer, getMessageType().getValue() );

            // tickets tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_CRED_TICKETS_TAG );
            buffer.put( TLV.getBytes( ticketsLen ) );

            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( ticketsSeqLen ) );

            for ( Ticket t : tickets )
            {
                t.encode( buffer );
            }

            // enc-part tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_CRED_ENCPART_TAG );
            buffer.put( TLV.getBytes( encPartLen ) );
            encPart.encode( buffer );
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_741_CANNOT_ENCODE_KRB_CRED, 1 + TLV.getNbBytes( krbCredLen )
                + krbCredLen, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            log.debug( "KrbCred encoding : {}", Strings.dumpBytes( buffer.array() ) );
            log.debug( "KrbCred initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @return the tickets
     */
    public List<Ticket> getTickets()
    {
        return tickets;
    }


    /**
     * @param tickets the tickets to set
     */
    public void setTickets( List<Ticket> tickets )
    {
        this.tickets = tickets;
    }


    /**
     * @return the encPart
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
     * adds a Ticket to the ticket list
     * 
     * @param ticket the Ticket to be added
     */
    public void addTicket( Ticket ticket )
    {
        if ( ticket == null )
        {
            throw new IllegalArgumentException( "null ticket cannot be added" );
        }

        if ( tickets == null )
        {
            tickets = new ArrayList<Ticket>();
        }

        tickets.add( ticket );
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "KRB-CRED : {\n" );
        sb.append( "    pvno: " ).append( getProtocolVersionNumber() ).append( '\n' );
        sb.append( "    msg-type: " ).append( getMessageType() ).append( '\n' );
        sb.append( "    tickets: " ).append( tickets ).append( '\n' );
        sb.append( "    en-part: " ).append( encPart ).append( '\n' );

        sb.append( "}\n" );

        return sb.toString();
    }
}
