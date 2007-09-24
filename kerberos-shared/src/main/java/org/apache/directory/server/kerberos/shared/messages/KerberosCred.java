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
package org.apache.directory.server.kerberos.shared.messages;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The KRB-CRED message. The ASN.1 grammar is the following :
 * 
 * KRB-CRED        ::= [APPLICATION 22] SEQUENCE {
 *        pvno            [0] INTEGER (5),
 *        msg-type        [1] INTEGER (22),
 *        tickets         [2] SEQUENCE OF Ticket,
 *        enc-part        [3] EncryptedData -- EncKrbCredPart
 * }
 *  
 * pvno and msg-type are inherited from KerberosMessage
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public class KerberosCred extends KerberosMessage
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( KerberosCred.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();
    
    /** The ticket list */
    private List<Ticket> tickets;
    
    /** The encrypted part */
    private EncryptedData encPart;

    // Storage for computed lengths
    private transient int encPartTagLength = 0;
    
    private transient int ticketsTagLength = 0;
    private transient int ticketsSeqLength = 0;

    private transient int kerberosCredSeqLength;
    private transient int kerberosCredApplLength;
    
    /**
     * Creates a new instance of KerberosCred.
     */
    public KerberosCred()
    {
        super( MessageType.KRB_CRED );
        encPart = null;
        tickets = new ArrayList<Ticket>();
    }

    /**
     * @return The encrypted part
     */
    public EncryptedData getEncPart()
    {
        return encPart;
    }

    /**
     * Set the encrypted part
     * @param encPart The encrypted part
     */
    public void setEncPart( EncryptedData encPart )
    {
        this.encPart = encPart;
    }
    
    /**
     * @return The ticket list
     */
    public List<Ticket> getTickets()
    {
        return tickets;
    }
    
    /**
     * Set a ticket list
     * @param tickets The ticket list
     */
    public void setTickets( List<Ticket> tickets )
    {
        this.tickets = tickets;
    }
    
    /**
     * Add a ticket to the KRB-CRED
     * @param ticket The added ticket
     */
    public void addTicket( Ticket ticket )
    {
        tickets.add( ticket );
    }
    
    
    /**
     * Return the length of a Kerberos Cred message .
     * 
     * 0x72 L1
     *  |
     *  +--> 0x30 L2
     *        |
     *        +--> 0xA0 0x03
     *        |     |
     *        |     +--> 0x02 0x01 pvno (integer)
     *        |
     *        +--> 0xA1 0x03
     *        |     |
     *        |     +--> 0x02 0x01 msg-type (integer)
     *        |
     *        +--> 0xA2 L3
     *        |     |
     *        |     +--> 0x30 L3-1 tickets
     *        |           |
     *        |           +--> 0x61 L3-1-1 ticket
     *        |           |
     *        |           +--> ...
     *        |           |
     *        |           +--> 0x61 L3-1-N ticket
     *        |
     *        +--> 0xA3 L4
     *              | 
     *              +--> 0x02 L4-1 enc-part (EncryptedData)
     */
    public int computeLength()
    {
        // First compute the KerberosMessage length
        kerberosCredSeqLength = super.computeLength();
        
        // The tickets length
        if ( tickets == null )
        {
            return -1;
        }
        
        ticketsSeqLength = 0;
        
        for ( Ticket ticket:tickets )
        {
            ticketsSeqLength += ticket.computeLength();
        }
        
        ticketsTagLength = 1 + TLV.getNbBytes( ticketsSeqLength ) + ticketsSeqLength;
        kerberosCredSeqLength += 1 + TLV.getNbBytes( ticketsTagLength ) + ticketsTagLength;
        
        // The encrypted data length
        if ( encPart == null )
        {
            return -1;
        }
        
        encPartTagLength = encPart.computeLength();
        
        kerberosCredSeqLength += 
            1 + TLV.getNbBytes( encPartTagLength ) + encPartTagLength;


        kerberosCredApplLength = 1 + TLV.getNbBytes( kerberosCredSeqLength ) + kerberosCredSeqLength;
        return 1 + TLV.getNbBytes( kerberosCredApplLength ) + kerberosCredApplLength;
    }
    
    /**
     * Encode the KerberosCred message to a PDU. 
     * 
     * KRB-Cred :
     * 
     * 0x72 LL
     *   0x30 LL
     *     0xA0 LL pvno 
     *     0xA1 LL msg-type
     *     0xA2 LL tickets
     *       0x30 LL 
     *         0x61 LL ticket
     *         ...
     *         0x61 LL ticket
     *     0xA3 LL enc-part
     * 
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            int bufferCapacity = computeLength();
            
            if ( bufferCapacity == -1 )
            {
                log.error( "Cannot compute the buffer size" );
                throw new EncoderException( "Cannot compute the buffer size" );
            }
            
            buffer = ByteBuffer.allocate( bufferCapacity );
        }

        try
        {
            // The KerberosCred APPLICATION Tag
            buffer.put( (byte)0x72 );
            buffer.put( TLV.getBytes( kerberosCredApplLength ) );

            // The KerberosCred SEQUENCE Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( kerberosCredSeqLength ) );

            // The pvno and msg-type Tag and value
            super.encode(  buffer );

            // The tickets
            buffer.put( (byte)0xA2 );
            buffer.put( TLV.getBytes( ticketsTagLength ) );
            
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( ticketsSeqLength ) );

            if ( tickets != null )
            {
                for ( Ticket ticket:tickets )
                {
                    ticket.encode( buffer );
                }
            }
            
            // Encrypted Data encoding
            buffer.put( (byte)0xA3 );
            buffer.put( TLV.getBytes( encPartTagLength ) );
            
            if ( encPart != null )
            {
                encPart.encode( buffer );
            }
            else
            {
                log.error( "Null Encrypted Data part" );
                throw new EncoderException( "The encrypted Data must not be null" );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( "Cannot encode the KRB-CRED object, the PDU size is {} when only {} bytes has been allocated", 1
                + TLV.getNbBytes( kerberosCredApplLength ) + kerberosCredApplLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "KRB-CRED encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "KRB-CRED initial value : {}", toString() );
        }

        return buffer;
    }
}
