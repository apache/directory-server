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

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Encrypted part of the application response.
 *  It will store the object described by the ASN.1 grammar :
 * <pre>
 * EncAPRepPart    ::= [APPLICATION 27] SEQUENCE {
 *         ctime           [0] KerberosTime,
 *         cusec           [1] Microseconds,
 *         subkey          [2] <EncryptionKey> OPTIONAL,
 *         seq-number      [3] UInt32 OPTIONAL
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncApRepPart extends KerberosMessage
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( EncApRepPart.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The client time */
    private KerberosTime ctime;

    /** the microsecond part of the client's timestamp */
    private int cusec;

    /** Encryption key */
    private EncryptionKey subkey; //optional

    /** Sequence number */
    private Integer seqNumber; //optional

    // Storage for computed lengths
    private int ctimeLength;
    private int cusecLength;
    private int subKeyLength;
    private int seqNumberLength;
    private int encApRepPartSeqLength;
    private int encApRepPartLength;


    /**
     * Creates a new instance of EncApRepPart.
     */
    public EncApRepPart()
    {
        super( KerberosMessageType.ENC_AP_REP_PART );
    }


    /**
     * Returns the client {@link KerberosTime}.
     *
     * @return The client {@link KerberosTime}.
     */
    public KerberosTime getCTime()
    {
        return ctime;
    }


    /**
     * @param ctime the ctime to set
     */
    public void setCTime( KerberosTime ctime )
    {
        this.ctime = ctime;
    }


    /**
     * @return the cusec
     */
    public int getCusec()
    {
        return cusec;
    }


    /**
     * @param cusec the cusec to set
     */
    public void setCusec( int cusec )
    {
        this.cusec = cusec;
    }


    /**
     * @return the subkey
     */
    public EncryptionKey getSubkey()
    {
        return subkey;
    }


    /**
     * @param subkey the subkey to set
     */
    public void setSubkey( EncryptionKey subkey )
    {
        this.subkey = subkey;
    }


    /**
     * @return the seqNumber
     */
    public Integer getSeqNumber()
    {
        return seqNumber;
    }


    /**
     * @param seqNumber the seqNumber to set
     */
    public void setSeqNumber( Integer seqNumber )
    {
        this.seqNumber = seqNumber;
    }


    /**
     * Compute the Authenticator length
     * <pre>
     * Authenticator :
     * 
     * 0x7B L1 EncApRepPart [APPLICATION 27]
     *  |
     *  +--> 0x30 L2 SEQ
     *        |
     *        +--> 0xA0 11 ctime tag
     *        |     |
     *        |     +--> 0x18 0x0F ttt ctime (KerberosTime)
     *        |
     *        +--> 0xA1 L3 cusec tag
     *        |     |
     *        |     +--> 0x02 L3-1 cusec (INTEGER)
     *        |
     *        +--> 0xA2 L4 subkey (EncryptionKey)
     *        |
     *        +--> 0xA3 L5 seq-number tag
     *              |
     *              +--> 0x02 L5-1 NN seq-number (INTEGER)
     * </pre>
     */
    @Override
    public int computeLength()
    {
        // Compute the ctime length.
        ctimeLength = 1 + 1 + 0x0F;
        encApRepPartSeqLength = 1 + TLV.getNbBytes( ctimeLength ) + ctimeLength;

        // Compute the cusec length
        cusecLength = 1 + 1 + Value.getNbBytes( cusec );
        encApRepPartSeqLength += 1 + TLV.getNbBytes( cusecLength ) + cusecLength;

        // Compute the subkey length, if any
        if ( subkey != null )
        {
            subKeyLength = subkey.computeLength();
            encApRepPartSeqLength += 1 + TLV.getNbBytes( subKeyLength ) + subKeyLength;
        }

        // Compute the sequence size, if any
        if ( seqNumber != null )
        {
            seqNumberLength = 1 + 1 + Value.getNbBytes( seqNumber );
            encApRepPartSeqLength += 1 + TLV.getNbBytes( seqNumberLength ) + seqNumberLength;
        }

        encApRepPartLength = 1 + TLV.getNbBytes( encApRepPartSeqLength ) + encApRepPartSeqLength;

        return 1 + TLV.getNbBytes( encApRepPartLength ) + encApRepPartLength;
    }


    /**
     * Encode the EncApRepPart message to a PDU. 
     * <pre>
     * EncApRepPart :
     * 
     * 0x7B LL
     *   0x30 LL
     *     0xA0 0x11 
     *       0x18 0x0F ttt ctime 
     *     0xA1 LL 
     *       0x02 LL NN cusec
     *    [0xA2 LL
     *       0x30 LL abcd] subkey
     *    [0xA3 LL
     *       0x02 LL NN] seq-number
     * </pre>
     * @return The constructed PDU.
     */
    @Override
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            buffer = ByteBuffer.allocate( computeLength() );
        }

        try
        {
            // The EncApRepPart APPLICATION Tag
            buffer.put( ( byte ) KerberosConstants.ENC_AP_REP_PART_TAG );
            buffer.put( TLV.getBytes( encApRepPartLength ) );

            // The EncApRepPart SEQ Tag
            buffer.put( ( byte ) UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( encApRepPartSeqLength ) );

            // The ctime ------------------------------------------------------
            // The tag
            buffer.put( ( byte ) KerberosConstants.ENC_AP_REP_PART_CTIME_TAG );
            buffer.put( ( byte ) 0x11 );

            // The value
            buffer.put( ( byte ) UniversalTag.GENERALIZED_TIME.getValue() );
            buffer.put( ( byte ) 0x0F );
            buffer.put( ctime.getBytes() );

            // The cusec ------------------------------------------------------
            // The tag
            buffer.put( ( byte ) KerberosConstants.ENC_AP_REP_PART_CUSEC_TAG );
            buffer.put( TLV.getBytes( cusecLength ) );

            // The value
            Value.encode( buffer, cusec );

            // The subkey if any ----------------------------------------------
            if ( subkey != null )
            {
                // The tag
                buffer.put( ( byte ) KerberosConstants.ENC_AP_REP_PART_SUB_KEY_TAG );
                buffer.put( TLV.getBytes( subKeyLength ) );

                // The value
                subkey.encode( buffer );
            }

            // The seq-number, if any -----------------------------------------
            if ( seqNumber != null )
            {
                // The tag
                buffer.put( ( byte ) KerberosConstants.ENC_AP_REP_PART_SEQ_NUMBER_TAG );
                buffer.put( TLV.getBytes( seqNumberLength ) );

                // The value
                Value.encode( buffer, seqNumber );
            }

        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_139, 1 + TLV.getNbBytes( encApRepPartLength )
                + encApRepPartLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "EncApRepPart encoding : {}", Strings.dumpBytes( buffer.array() ) );
            LOG.debug( "EncApRepPart initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "EncApRepPart : \n" );

        sb.append( "    ctime : " ).append( ctime ).append( '\n' );
        sb.append( "    cusec : " ).append( cusec ).append( '\n' );

        if ( subkey != null )
        {
            sb.append( "    subkey : " ).append( subkey ).append( '\n' );
        }

        if ( seqNumber != null )
        {
            sb.append( "    seq-number : " ).append( seqNumber ).append( '\n' );
        }

        return sb.toString();
    }
}
