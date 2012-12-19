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
package org.apache.directory.shared.kerberos.messages;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.tlv.BerValue;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.codec.options.ApOptions;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * AP-REQ message component . It will store the object described by the ASN.1 grammar :
 * <pre>
 * AP-REQ          ::= [APPLICATION 14] SEQUENCE {
 *         pvno            [0] INTEGER (5),
 *         msg-type        [1] INTEGER (14),
 *         ap-options      [2] APOptions,
 *         ticket          [3] Ticket,
 *         authenticator   [4] EncryptedData -- Authenticator
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ApReq extends KerberosMessage
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( ApReq.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The AP options */
    private ApOptions apOptions;

    /** The Ticket */
    private Ticket ticket;

    /** The encryptedData, an encrypted Authenticator */
    private EncryptedData authenticator;

    // Storage for computed lengths
    private int pvnoLength;
    private int msgTypeLength;
    private int apOptionsLength;
    private int ticketLength;
    private int authenticatorLength;
    private int apReqLength;
    private int apReqSeqLength;


    /**
     * Creates a new instance of ApplicationRequest.
     */
    public ApReq()
    {
        super( KerberosMessageType.AP_REQ );
    }


    /**
     * Returns the {@link ApOptions}.
     *
     * @return The {@link ApOptions}.
     */
    public ApOptions getApOptions()
    {
        return apOptions;
    }


    /**
     * Returns the {@link Ticket}.
     *
     * @return The {@link Ticket}.
     */
    public Ticket getTicket()
    {
        return ticket;
    }


    /**
     * Returns the option at a specified index.
     *
     * @param option
     * @return The option.
     */
    public boolean getOption( int option )
    {
        return apOptions.get( option );
    }


    /**
     * Sets the option at a specified index.
     *
     * @param option
     */
    public void setOption( ApOptions apOptions )
    {
        this.apOptions = apOptions;
    }


    /**
     * Clears the option at a specified index.
     *
     * @param option
     */
    public void clearOption( int option )
    {
        apOptions.clear( option );
    }


    /**
     * Returns the {@link EncryptedData}.
     *
     * @return The {@link EncryptedData}.
     */
    public EncryptedData getAuthenticator()
    {
        return authenticator;
    }


    /**
     * Sets the {@link EncryptedData}.
     *
     * @param authenticator The encrypted authenticator
     */
    public void setAuthenticator( EncryptedData authenticator )
    {
        this.authenticator = authenticator;
    }


    /**
     * Sets the {@link ApOptions}.
     *
     * @param options
     */
    public void setApOptions( ApOptions options )
    {
        apOptions = options;
    }


    /**
     * Sets the {@link Ticket}.
     *
     * @param ticket
     */
    public void setTicket( Ticket ticket )
    {
        this.ticket = ticket;
    }


    /**
     * Compute the AP-REQ length
     * <pre>
     * AP-REQ :
     * 
     * 0x6E L1 AP-REQ [APPLICATION 14]
     *  |
     *  +--> 0x30 L2
     *        |
     *        +--> 0xA0 0x03 pvno tag
     *        |     |
     *        |     +--> 0x02 0x01 0x05 pvno (5)
     *        |
     *        +--> 0xA1 0x03 msg-type tag
     *        |     |
     *        |     +--> 0x02 0x01 0x0E msg-type (14)
     *        |
     *        +--> 0xA2 0x03 APOptions tag
     *        |     |
     *        |     +--> 0x03 0x05 0x00 b1 b2 b3 b4 APOtions
     *        |
     *        +--> 0xA3 L3 ticket tag
     *        |     |
     *        |     +--> 0x61 L3-1 ticket
     *        |
     *        +--> 0xA4 L4 authenticator tag
     *              |
     *              +--> 0x30 L4-1 authenticator (encrypted)
     * </pre>
     */
    public int computeLength()
    {
        reset();

        // Compute the PVNO length.
        pvnoLength = 1 + 1 + BerValue.getNbBytes( getProtocolVersionNumber() );

        // Compute the msg-type length
        msgTypeLength = 1 + 1 + BerValue.getNbBytes( getMessageType().getValue() );

        // Compute the APOptions length
        apOptionsLength = 1 + 1 + apOptions.getBytes().length;

        // Compute the ticket length
        ticketLength = ticket.computeLength();

        // Compute the authenticator length
        authenticatorLength = authenticator.computeLength();

        // Compute the sequence size
        apReqLength =
            1 + TLV.getNbBytes( pvnoLength ) + pvnoLength +
                1 + TLV.getNbBytes( msgTypeLength ) + msgTypeLength +
                1 + TLV.getNbBytes( apOptionsLength ) + apOptionsLength +
                1 + TLV.getNbBytes( ticketLength ) + ticketLength +
                1 + TLV.getNbBytes( authenticatorLength ) + authenticatorLength;

        apReqSeqLength = 1 + TLV.getNbBytes( apReqLength ) + apReqLength;

        return 1 + TLV.getNbBytes( apReqSeqLength ) + apReqSeqLength;
    }


    /**
     * Encode the AP-REQ component
     * 
     * @param buffer The buffer containing the encoded result
     * @return The encoded component
     * @throws EncoderException If the encoding failed
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            buffer = ByteBuffer.allocate( computeLength() );
        }

        try
        {
            // The AP-REP Tag
            buffer.put( ( byte ) KerberosConstants.AP_REQ_TAG );
            buffer.put( TLV.getBytes( apReqSeqLength ) );

            // The AP-REP SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( apReqLength ) );

            // The PVNO -------------------------------------------------------
            // The tag
            buffer.put( ( byte ) KerberosConstants.AP_REQ_PVNO_TAG );
            buffer.put( TLV.getBytes( pvnoLength ) );

            // The value
            BerValue.encode( buffer, getProtocolVersionNumber() );

            // The msg-type ---------------------------------------------------
            // The tag
            buffer.put( ( byte ) KerberosConstants.AP_REQ_MSG_TYPE_TAG );
            buffer.put( TLV.getBytes( msgTypeLength ) );

            // The value
            BerValue.encode( buffer, getMessageType().getValue() );

            // The ap-options -------------------------------------------------
            // The tag
            buffer.put( ( byte ) KerberosConstants.AP_REQ_AP_OPTIONS_TAG );
            buffer.put( TLV.getBytes( apOptionsLength ) );

            // The value
            BerValue.encode( buffer, apOptions );

            // The ticket -----------------------------------------------------
            // The tag
            buffer.put( ( byte ) KerberosConstants.AP_REQ_TICKET_TAG );
            buffer.put( TLV.getBytes( ticketLength ) );

            // The value
            ticket.encode( buffer );

            // The authenticator ----------------------------------------------
            // The tag
            buffer.put( ( byte ) KerberosConstants.AP_REQ_AUTHENTICATOR_TAG );
            buffer.put( TLV.getBytes( authenticatorLength ) );

            // The value
            authenticator.encode( buffer );
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_137, 1 + TLV.getNbBytes( apReqLength ) + apReqLength,
                buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "AP-REQ encoding : {}", Strings.dumpBytes( buffer.array() ) );
            LOG.debug( "AP-REQ initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * reset the fields used while computing length
     */
    private void reset()
    {
        pvnoLength = 0;
        msgTypeLength = 0;
        apOptionsLength = 0;
        ticketLength = 0;
        authenticatorLength = 0;
        apReqLength = 0;
        apReqSeqLength = 0;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "AP-REQ :\n" );
        sb.append( "  pvno : " ).append( getProtocolVersionNumber() ).append( "\n" );
        sb.append( "  msg-type : " ).append( getMessageType() ).append( "\n" );
        sb.append( "  ap-options : " ).append( apOptions ).append( "\n" );
        sb.append( "  ticket : " ).append( ticket ).append( "\n" );
        sb.append( "  authenticator : " ).append( authenticator ).append( "\n" );

        return sb.toString();
    }
}
