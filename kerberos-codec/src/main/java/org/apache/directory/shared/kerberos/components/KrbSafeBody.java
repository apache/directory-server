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
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class representing KRB-SAFE-BODY message
 * 
 * <pre>
 * KRB-SAFE-BODY   ::= SEQUENCE {
 *         user-data       [0] OCTET STRING,
 *         timestamp       [1] KerberosTime OPTIONAL,
 *         usec            [2] Microseconds OPTIONAL,
 *         seq-number      [3] UInt32 OPTIONAL,
 *         s-address       [4] HostAddress,
 *         r-address       [5] HostAddress OPTIONAL
 * }
 *</pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbSafeBody extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( KrbSafeBody.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** the user data */
    private byte[] userData;

    /** the current time of the sender */
    private KerberosTime timestamp;

    /** the microsecond part of the timestamp */
    private Integer usec;

    /** the sequence number */
    private Integer seqNumber;

    /** the sender's address */
    private HostAddress senderAddress;

    /** the recipient's address */
    private HostAddress recipientAddress;

    // Storage for computed lengths
    private transient int userDataLen;
    private transient int timestampLen;
    private transient int usecLen;
    private transient int seqNumberLen;
    private transient int senderAddressLen;
    private transient int recipientAddressLen;
    private transient int krbSafeBodySeqLen;


    /**
     * Creates a new instance of KrbSafeBody.
     */
    public KrbSafeBody()
    {
    }


    /**
     * @return the userData
     */
    public byte[] getUserData()
    {
        return userData;
    }


    /**
     * @param userData the userData to set
     */
    public void setUserData( byte[] userData )
    {
        this.userData = userData;
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
    public int getUsec()
    {
        if ( usec == null )
        {
            return 0;
        }

        return usec;
    }


    /**
     * @param usec the usec to set
     */
    public void setUsec( int usec )
    {
        this.usec = usec;
    }


    /**
     * @return the seqNumber
     */
    public int getSeqNumber()
    {
        if ( seqNumber == null )
        {
            return 0;
        }

        return seqNumber;
    }


    /**
     * @param seqNumber the seqNumber to set
     */
    public void setSeqNumber( int seqNumber )
    {
        this.seqNumber = seqNumber;
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
     * Compute the KRB-SAFE-BODY length:
     * 
     * <pre>
     * 0x30 L1 KRB-SAFE-BODY SEQ
     *  |
     *  +--> 0xA0 L2 user-data tag
     *  |     |
     *  |     +--> 0x04 L2-1 user-data (Octet String)
     *  |
     *  +--> 0xA1 0x11 timestamp tag
     *  |     |
     *  |     +--> 0x18 0x0F timestamp (KerberosTime)
     *  |
     *  +--> 0xA2 L3 usec tag
     *  |     |
     *  |     +--> 0x02 L3-1 usec (Microseconds)
     *  |
     *  +--> 0xA3 L4 seq-number tag
     *  |     |
     *  |     +--> 0x02 L4-1 seqnumber (UInt32)
     *  |
     *  +--> 0xA4 L5 s-address tag
     *  |     |
     *  |     +--> 0x30 L5-1 s-address (HostAddress)
     *  |
     *  +--> 0xA5 L6 r-address tag
     *        |
     *        +--> 0x30 L6-1 r-address (HostAddress)
     * </pre>       
     */
    @Override
    public int computeLength()
    {
        userDataLen = 1 + TLV.getNbBytes( userData.length ) + userData.length;
        krbSafeBodySeqLen = 1 + TLV.getNbBytes( userDataLen ) + userDataLen;

        senderAddressLen = senderAddress.computeLength();
        krbSafeBodySeqLen += 1 + TLV.getNbBytes( senderAddressLen ) + senderAddressLen;

        if ( timestamp != null )
        {
            timestampLen = timestamp.getBytes().length;
            timestampLen = 1 + TLV.getNbBytes( timestampLen ) + timestampLen;
            krbSafeBodySeqLen += 1 + TLV.getNbBytes( timestampLen ) + timestampLen;
        }

        if ( usec != null )
        {
            usecLen = Value.getNbBytes( usec );
            usecLen = 1 + TLV.getNbBytes( usecLen ) + usecLen;
            krbSafeBodySeqLen += 1 + TLV.getNbBytes( usecLen ) + usecLen;
        }

        if ( seqNumber != null )
        {
            seqNumberLen = Value.getNbBytes( seqNumber );
            seqNumberLen = 1 + TLV.getNbBytes( seqNumberLen ) + seqNumberLen;
            krbSafeBodySeqLen += 1 + TLV.getNbBytes( seqNumberLen ) + seqNumberLen;
        }

        if ( recipientAddress != null )
        {
            recipientAddressLen = recipientAddress.computeLength();
            krbSafeBodySeqLen += 1 + TLV.getNbBytes( recipientAddressLen ) + recipientAddressLen;
        }

        return 1 + TLV.getNbBytes( krbSafeBodySeqLen ) + krbSafeBodySeqLen;
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
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( krbSafeBodySeqLen ) );

            // user-data
            buffer.put( ( byte ) KerberosConstants.KRB_SAFE_BODY_USER_DATA_TAG );
            buffer.put( TLV.getBytes( userDataLen ) );
            Value.encode( buffer, userData );

            if ( timestamp != null )
            {
                // timestamp tag
                buffer.put( ( byte ) KerberosConstants.KRB_SAFE_BODY_TIMESTAMP_TAG );
                buffer.put( TLV.getBytes( timestampLen ) );

                // timestamp value
                buffer.put( ( byte ) UniversalTag.GENERALIZED_TIME.getValue() );
                buffer.put( ( byte ) 0x0F );
                buffer.put( timestamp.getBytes() );
            }

            if ( usec != null )
            {
                // usec
                buffer.put( ( byte ) KerberosConstants.KRB_SAFE_BODY_USEC_TAG );
                buffer.put( TLV.getBytes( usecLen ) );
                Value.encode( buffer, usec );
            }

            if ( seqNumber != null )
            {
                // seq-number
                buffer.put( ( byte ) KerberosConstants.KRB_SAFE_BODY_SEQ_NUMBER_TAG );
                buffer.put( TLV.getBytes( seqNumberLen ) );
                Value.encode( buffer, seqNumber );
            }

            // s-address
            buffer.put( ( byte ) KerberosConstants.KRB_SAFE_BODY_SENDER_ADDRESS_TAG );
            buffer.put( TLV.getBytes( senderAddressLen ) );
            senderAddress.encode( buffer );

            if ( recipientAddress != null )
            {
                // s-address
                buffer.put( ( byte ) KerberosConstants.KRB_SAFE_BODY_RECIPIENT_ADDRESS_TAG );
                buffer.put( TLV.getBytes( recipientAddressLen ) );
                recipientAddress.encode( buffer );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_735_CANNOT_ENCODE_KRBSAFEBODY, 1 + TLV.getNbBytes( krbSafeBodySeqLen )
                + krbSafeBodySeqLen, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            log.debug( "KrbSafeBody encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "KrbSafeBody initial value : {}", toString() );
        }

        return buffer;
    }
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "KRB-SAFE-BODY : {\n" );
        sb.append( "    user-data: " ).append( StringTools.dumpBytes( userData ) ).append( '\n' );

        if ( timestamp != null )
        {
            sb.append( "    timestamp: " ).append( timestamp.getDate() ).append( '\n' );
        }

        if ( usec != null )
        {
            sb.append( "    usec: " ).append( usec ).append( '\n' );
        }

        if ( seqNumber != null )
        {
            sb.append( "    seq-number: " ).append( seqNumber ).append( '\n' );
        }

        sb.append( "    s-address: " ).append( senderAddress ).append( '\n' );

        if ( recipientAddress != null )
        {
            sb.append( "    r-address: " ).append( recipientAddress ).append( '\n' );
        }

        sb.append( "}\n" );

        return sb.toString();
    }
}
