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
import org.apache.directory.api.asn1.ber.tlv.BerValue;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.flags.TicketFlags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class for encrypted parts of KDC responses.
 * 
 * The ASN.1 grammar for this structure is :
 * <pre>
 * EncKDCRepPart   ::= SEQUENCE {
 *         key             [0] EncryptionKey,
 *         last-req        [1] LastReq,
 *         nonce           [2] UInt32,
 *         key-expiration  [3] KerberosTime OPTIONAL,
 *         flags           [4] TicketFlags,
 *         authtime        [5] KerberosTime,
 *         starttime       [6] KerberosTime OPTIONAL,
 *         endtime         [7] KerberosTime,
 *         renew-till      [8] KerberosTime OPTIONAL,
 *         srealm          [9] Realm,
 *         sname           [10] PrincipalName,
 *         caddr           [11] HostAddresses OPTIONAL
 * }
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncKdcRepPart implements Asn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( EncKdcRepPart.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The encryption key */
    private EncryptionKey key;

    /** The time of the last request */
    private LastReq lastReq;

    /** The nonce */
    private int nonce;

    /** The KeyExpiration */
    private KerberosTime keyExpiration; //optional

    /** The Ticket flags */
    private TicketFlags flags = new TicketFlags();

    /** The initial Authentication time */
    private KerberosTime authTime;

    /** The ticket's start time */
    private KerberosTime startTime; //optional

    /** The Ticket expiration time */
    private KerberosTime endTime;

    /** Maximum endtime in a renewal */
    private KerberosTime renewTill; //optional

    /** The server's realm */
    private String srealm;

    /** The server's principal */
    private PrincipalName sname;

    /** The client addresses */
    private HostAddresses caddr; //optional

    // Storage for computed lengths
    private int keyLength;
    private int lastReqLength;
    private int nonceLength;
    private int flagsLength;
    private byte[] srealmBytes;
    private int srealmLength;
    private int snameLength;
    private int caddrLength;
    private int encKdcRepPartSeqLength;


    /**
     * Creates a new instance of EncKdcRepPart.
     */
    public EncKdcRepPart()
    {
    }


    /**
     * Returns the auth {@link KerberosTime}.
     *
     * @return The auth {@link KerberosTime}.
     */
    public KerberosTime getAuthTime()
    {
        return authTime;
    }


    /**
     * Sets the auth {@link KerberosTime}.
     *
     * @param time
     */
    public void setAuthTime( KerberosTime time )
    {
        authTime = time;
    }


    /**
     * Returns the client {@link HostAddresses}.
     *
     * @return The client {@link HostAddresses}.
     */
    public HostAddresses getClientAddresses()
    {
        return caddr;
    }


    /**
     * Sets the client {@link HostAddresses}.
     *
     * @param caddr The client addresses
     */
    public void setClientAddresses( HostAddresses caddr )
    {
        this.caddr = caddr;
    }


    /**
     * Returns the end {@link KerberosTime}.
     *
     * @return The end {@link KerberosTime}.
     */
    public KerberosTime getEndTime()
    {
        return endTime;
    }


    /**
     * Sets the end {@link KerberosTime}.
     *
     * @param time
     */
    public void setEndTime( KerberosTime time )
    {
        endTime = time;
    }


    /**
     * Returns the {@link TicketFlags}.
     *
     * @return The {@link TicketFlags}.
     */
    public TicketFlags getFlags()
    {
        return flags;
    }


    /**
     * Sets the {@link TicketFlags}.
     *
     * @param flags
     */
    public void setFlags( TicketFlags flags )
    {
        this.flags = flags;
    }


    /**
     * Returns the {@link EncryptionKey}.
     *
     * @return The {@link EncryptionKey}.
     */
    public EncryptionKey getKey()
    {
        return key;
    }


    /**
     * Sets the {@link EncryptionKey}.
     *
     * @param key
     */
    public void setKey( EncryptionKey key )
    {
        this.key = key;
    }


    /**
     * Returns the key expiration {@link KerberosTime}.
     *
     * @return The key expiration {@link KerberosTime}.
     */
    public KerberosTime getKeyExpiration()
    {
        return keyExpiration;
    }


    /**
     * Sets the key expiration {@link KerberosTime}.
     *
     * @param expiration
     */
    public void setKeyExpiration( KerberosTime expiration )
    {
        keyExpiration = expiration;
    }


    /**
     * Returns the {@link LastReq}.
     *
     * @return The {@link LastReq}.
     */
    public LastReq getLastReq()
    {
        return lastReq;
    }


    /**
     * Sets the {@link LastReq}.
     *
     * @param lastReq The LastReq to set
     */
    public void setLastReq( LastReq lastReq )
    {
        this.lastReq = lastReq;
    }


    /**
     * Returns the nonce.
     *
     * @return The nonce.
     */
    public int getNonce()
    {
        return nonce;
    }


    /**
     * Sets the nonce.
     *
     * @param nonce
     */
    public void setNonce( int nonce )
    {
        this.nonce = nonce;
    }


    /**
     * Returns the renew till {@link KerberosTime}.
     *
     * @return The renew till {@link KerberosTime}.
     */
    public KerberosTime getRenewTill()
    {
        return renewTill;
    }


    /**
     * Sets the renew till {@link KerberosTime}.
     *
     * @param till
     */
    public void setRenewTill( KerberosTime till )
    {
        renewTill = till;
    }


    /**
     * Returns the server {@link PrincipalName}.
     *
     * @return The server {@link PrincipalName}.
     */
    public PrincipalName getSName()
    {
        return sname;
    }


    /**
     * Sets the server {@link PrincipalName}.
     *
     * @param sname The server PrincipalName
     */
    public void setSName( PrincipalName sname )
    {
        this.sname = sname;
    }


    /**
     * Returns the server realm.
     *
     * @return The server realm.
     */
    public String getSRealm()
    {
        return srealm;
    }


    /**
     * Sets the server realm.
     *
     * @param srealm The server realm
     */
    public void setSRealm( String srealm )
    {
        this.srealm = srealm;
    }


    /**
     * Returns the start {@link KerberosTime}.
     *
     * @return The start {@link KerberosTime}.
     */
    public KerberosTime getStartTime()
    {
        return startTime;
    }


    /**
     * Sets the start {@link KerberosTime}.
     *
     * @param time he start time to set
     */
    public void setStartTime( KerberosTime time )
    {
        startTime = time;
    }


    /**
     * Compute the EncKdcRepPart length
     * <pre>
     * EncKdcRepPart :
     * 
     * 0x30 L1 EncKdcRepPart sequence
     *  |
     *  +--&gt; 0xA0 L2 key tag
     *  |     |
     *  |     +--&gt; 0x30 L2-1 key ( EncryptionKey)
     *  |
     *  +--&gt; 0xA1 L3 last-req tag
     *  |     |
     *  |     +--&gt; 0x30 L3-1 last-req ( LastReq )
     *  |     
     *  +--&gt; 0xA2 L4 nonce tag
     *  |     |
     *  |     +--&gt; 0x02 L4-1 nonce (Int)
     *  |     
     * [+--&gt; 0xA3 0x11 key-expiration tag]
     *  |     |
     *  |     +--&gt; 0x18 0x0F key-expiration ( KerberosTime )
     *  |     
     *  +--&gt; 0xA4 0x07 flags tag 
     *  |     |
     *  |     +--&gt; 0x03 0x05 flags ( TicketFlags )
     *  |     
     *  +--&gt; 0xA5 0x11 authtime tag
     *  |     |
     *  |     +--&gt; 0x18 0x0F authtime ( KerberosTime )
     *  |     
     * [+--&gt; 0xA6 0x11 starttime tag]
     *  |     |
     *  |     +--&gt; 0x18 0x0F starttime ( KerberosTime )
     *  |     
     *  +--&gt; 0xA7 0x11 endtime tag
     *  |     |
     *  |     +--&gt; 0x18 0x0F endtime ( KerberosTime )
     *  |     
     * [+--&gt; 0xA8 0x11 renew-till tag]
     *  |     |
     *  |     +--&gt; 0x18 0x0F renew-till ( KerberosTime )
     *  |     
     *  +--&gt; 0xA9 L5 srealm tag
     *  |     |
     *  |     +--&gt; 0x1B L5-1 srealm ( KerberosString )
     *  |     
     *  +--&gt; 0xAA L6 sname tag
     *  |     |
     *  |     +--&gt; 0x30 L6-1 sname ( PrincipalName )
     *  |     
     * [+--&gt; 0xAB L7 caddr tag]
     *        |
     *        +--&gt; 0x30 L7-1 caddr ( HostAddresses )
     *  </pre>
     */
    public int computeLength()
    {
        // The key
        keyLength = key.computeLength();
        encKdcRepPartSeqLength = 1 + TLV.getNbBytes( keyLength ) + keyLength;

        // The last-req
        lastReqLength = lastReq.computeLength();
        encKdcRepPartSeqLength += 1 + TLV.getNbBytes( lastReqLength ) + lastReqLength;

        // The nonce
        nonceLength = BerValue.getNbBytes( nonce );
        nonceLength = 1 + TLV.getNbBytes( nonceLength ) + nonceLength;
        encKdcRepPartSeqLength += 1 + TLV.getNbBytes( nonceLength ) + nonceLength;

        // The keyExpiration
        if ( keyExpiration != null )
        {
            encKdcRepPartSeqLength += 1 + 1 + 0x11;
        }

        // The flags
        flagsLength = 1 + 1 + 5;
        encKdcRepPartSeqLength += 1 + TLV.getNbBytes( flagsLength ) + flagsLength;

        // The authtime
        encKdcRepPartSeqLength += 1 + 1 + 0x11;

        // The starttime, if any
        if ( startTime != null )
        {
            encKdcRepPartSeqLength += 1 + 1 + 0x11;
        }

        // The endtime
        encKdcRepPartSeqLength += 1 + 1 + 0x11;

        // The renew-till, if any
        if ( renewTill != null )
        {
            encKdcRepPartSeqLength += 1 + 1 + 0x11;
        }

        // The srealm
        srealmBytes = srealm.getBytes();
        srealmLength = 1 + TLV.getNbBytes( srealmBytes.length ) + srealmBytes.length;
        encKdcRepPartSeqLength += 1 + TLV.getNbBytes( srealmLength ) + srealmLength;

        // The sname
        snameLength = sname.computeLength();
        encKdcRepPartSeqLength += 1 + TLV.getNbBytes( snameLength ) + snameLength;

        // The caddr if any
        if ( caddr != null )
        {
            caddrLength = caddr.computeLength();
            encKdcRepPartSeqLength += 1 + TLV.getNbBytes( caddrLength ) + caddrLength;
        }

        return 1 + TLV.getNbBytes( encKdcRepPartSeqLength ) + encKdcRepPartSeqLength;
    }


    /**
     * Encode the EncKdcRepPart message to a PDU. 
     * 
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_148 ) );
        }

        try
        {
            // The EncKdcRepPart sequence
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( encKdcRepPartSeqLength ) );

            // The Key
            buffer.put( ( byte ) KerberosConstants.ENC_KDC_REP_PART_KEY_TAG );
            buffer.put( TLV.getBytes( keyLength ) );
            key.encode( buffer );

            // The LastReq
            buffer.put( ( byte ) KerberosConstants.ENC_KDC_REP_PART_LAST_REQ_TAG );
            buffer.put( TLV.getBytes( lastReqLength ) );
            lastReq.encode( buffer );

            // The nonce
            buffer.put( ( byte ) KerberosConstants.ENC_KDC_REP_PART_NONCE_TAG );
            buffer.put( TLV.getBytes( nonceLength ) );
            BerValue.encode( buffer, nonce );

            // The key-expiration, if any
            if ( keyExpiration != null )
            {
                buffer.put( ( byte ) KerberosConstants.ENC_KDC_REP_PART_KEY_EXPIRATION_TAG );
                buffer.put( TLV.getBytes( 0x11 ) );

                buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
                buffer.put( ( byte ) 0x0F );
                buffer.put( keyExpiration.getBytes() );
            }

            // The flags
            buffer.put( ( byte ) KerberosConstants.ENC_KDC_REP_PART_FLAGS_TAG );
            buffer.put( TLV.getBytes( 0x07 ) );
            BerValue.encode( buffer, flags );

            // The authtime
            buffer.put( ( byte ) KerberosConstants.ENC_KDC_REP_PART_AUTH_TIME_TAG );
            buffer.put( TLV.getBytes( 0x11 ) );
            buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
            buffer.put( ( byte ) 0x0F );
            buffer.put( authTime.getBytes() );

            // The starttime if any
            if ( startTime != null )
            {
                buffer.put( ( byte ) KerberosConstants.ENC_KDC_REP_PART_START_TIME_TAG );
                buffer.put( TLV.getBytes( 0x11 ) );
                buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
                buffer.put( ( byte ) 0x0F );
                buffer.put( startTime.getBytes() );
            }

            // The endtime
            buffer.put( ( byte ) KerberosConstants.ENC_KDC_REP_PART_END_TIME_TAG );
            buffer.put( TLV.getBytes( 0x11 ) );
            buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
            buffer.put( ( byte ) 0x0F );
            buffer.put( endTime.getBytes() );

            // The renew-till if any
            if ( renewTill != null )
            {
                buffer.put( ( byte ) KerberosConstants.ENC_KDC_REP_PART_RENEW_TILL_TAG );
                buffer.put( TLV.getBytes( 0x11 ) );
                buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
                buffer.put( ( byte ) 0x0F );
                buffer.put( renewTill.getBytes() );
            }

            // The srealm
            buffer.put( ( byte ) KerberosConstants.ENC_KDC_REP_PART_SREALM_TAG );
            buffer.put( TLV.getBytes( srealmLength ) );
            buffer.put( UniversalTag.GENERAL_STRING.getValue() );
            buffer.put( TLV.getBytes( srealmBytes.length ) );
            buffer.put( srealmBytes );

            // The sname
            buffer.put( ( byte ) KerberosConstants.ENC_KDC_REP_PART_SNAME_TAG );
            buffer.put( TLV.getBytes( snameLength ) );
            sname.encode( buffer );

            // The caddr if any
            if ( caddr != null )
            {
                buffer.put( ( byte ) KerberosConstants.ENC_KDC_REP_PART_CADDR_TAG );
                buffer.put( TLV.getBytes( caddrLength ) );
                caddr.encode( buffer );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_140, 1 + TLV.getNbBytes( 0 ) + 0,
                buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ), boe );
        }

        if ( IS_DEBUG )
        {
            log.debug( "EncKdcRepPart encoding : {}", Strings.dumpBytes( buffer.array() ) );
            log.debug( "EncKdcRepPart initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "EncKdcRepPart : \n" );
        sb.append( "    key : " ).append( key ).append( "\n" );
        sb.append( "    last-req : " ).append( lastReq ).append( "\n" );
        sb.append( "    nonce : " ).append( nonce ).append( "\n" );

        if ( keyExpiration != null )
        {
            sb.append( "    key-expiration : " ).append( keyExpiration ).append( "\n" );
        }

        sb.append( "    flags : " ).append( flags ).append( "\n" );
        sb.append( "    authtime : " ).append( authTime ).append( "\n" );

        if ( startTime != null )
        {
            sb.append( "    starttime : " ).append( startTime ).append( "\n" );
        }

        sb.append( "    endtime : " ).append( endTime ).append( "\n" );

        if ( renewTill != null )
        {
            sb.append( "    renew-till : " ).append( renewTill ).append( "\n" );
        }

        sb.append( "    srealm : " ).append( srealm ).append( "\n" );
        sb.append( "    sname : " ).append( sname ).append( "\n" );

        if ( caddr != null )
        {
            sb.append( "    caddr : " ).append( caddr ).append( "\n" );
        }

        return sb.toString();
    }
}
