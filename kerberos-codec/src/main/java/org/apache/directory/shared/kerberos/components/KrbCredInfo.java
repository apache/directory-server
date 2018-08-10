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
 * KrbCredInfo     ::= SEQUENCE {
 *      key             [0] EncryptionKey,
 *      prealm          [1] Realm OPTIONAL,
 *      pname           [2] PrincipalName OPTIONAL,
 *      flags           [3] TicketFlags OPTIONAL,
 *      authtime        [4] KerberosTime OPTIONAL,
 *      starttime       [5] KerberosTime OPTIONAL,
 *      endtime         [6] KerberosTime OPTIONAL,
 *      renew-till      [7] KerberosTime OPTIONAL,
 *      srealm          [8] Realm OPTIONAL,
 *      sname           [9] PrincipalName OPTIONAL,
 *      caddr           [10] HostAddresses OPTIONAL
 * }
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbCredInfo implements Asn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( KrbCredInfo.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** the encryption key */
    private EncryptionKey key;

    /** principal identity's realm */
    private String pRealm;

    /** principal identity's name */
    private PrincipalName pName;

    /** the ticket flags */
    private TicketFlags ticketFlags;

    /** the time of initial authentication */
    private KerberosTime authTime;

    /** the time after which the ticket is valid */
    private KerberosTime startTime;

    /** the expiration time of ticket */
    private KerberosTime endTime;

    /** the maximum endtime that may be included in a renewal */
    private KerberosTime renewtill;

    /** searver's realm */
    private String sRealm;

    /** server's principal name */
    private PrincipalName sName;

    /** the addresses for which the ticket can be used */
    private HostAddresses clientAddresses;

    private int keyLen;
    private int pRealmLen;
    private byte[] pRealmBytes;
    private int pNameLen;
    private int ticketFlagsLen;
    private int authTimeLen;
    private byte[] authTimeBytes;
    private int startTimeLen;
    private byte[] startTimeBytes;
    private int endTimeLen;
    private byte[] endTimeBytes;
    private int renewtillLen;
    private byte[] renewtillBytes;
    private int sRealmLen;
    private byte[] sRealmBytes;
    private int sNameLen;
    private int clientAddressesLen;
    private int krbKredInfoSeqLen;


    /**
     * Calculate the length od KrbCredInfo:
     * 
     * <pre>
     * 0x30 L1 KrbCredInfo SEQ tag
     *  |
     *  |
     *  +--&gt; 0xA0 L2 key tag
     *  |     |
     *  |     +--&gt; 0x30 L2-2 key
     *  |
     *  +--&gt; [0xA1 L3 prealm tag
     *  |      |
     *  |      +--&gt; 0x1B L3-2 prealm]
     *  |
     *  +--&gt; [0xA2 L4 pname tag
     *  |      |
     *  |      +--&gt; 0x30 L4-2 pname]
     *  |
     *  +--&gt; [0xA3 L5 flags tag
     *  |      |
     *  |      +--&gt; 0x02 L5-2 flags]
     *  |
     *  +--&gt; [0xA4 0x11 authtime tag
     *  |      |
     *  |      +--&gt; 0x18 0x1F authtime]
     *  |
     *  +--&gt; [0xA5 0x11 starttime tag
     *  |      |
     *  |      +--&gt; 0x18 0x1F starttime]
     *  |
     *  +--&gt; [0xA6 0x11 endtime tag
     *  |      |
     *  |      +--&gt; 0x18 0x1F endtime]
     *  |
     *  +--&gt; [0xA7 0x11 renew-till tag
     *  |      |
     *  |      +--&gt; 0x18 0x1F renew-till]
     *  |
     *  +--&gt; [0xA8 L6 srealm tag
     *  |      |
     *  |      +--&gt; 0x1B L6-2 srealm]
     *  |
     *  +--&gt; [0xA9 L7 sname tag
     *  |      |
     *  |      +--&gt; 0x30 L7-2 sname]
     *  |
     *  +--&gt; [0xAA L8 caddr tag
     *         |
     *         +--&gt; 0x30 L8-2 caddr 
     * </pre>
     */
    @Override
    public int computeLength()
    {
        keyLen = key.computeLength();
        krbKredInfoSeqLen = 1 + TLV.getNbBytes( keyLen ) + keyLen;

        if ( pRealm != null )
        {
            pRealmBytes = Strings.getBytesUtf8( pRealm );
            pRealmLen = 1 + TLV.getNbBytes( pRealmBytes.length ) + pRealmBytes.length;
            krbKredInfoSeqLen += 1 + TLV.getNbBytes( pRealmLen ) + pRealmLen;
        }

        if ( pName != null )
        {
            pNameLen = pName.computeLength();
            krbKredInfoSeqLen += 1 + TLV.getNbBytes( pNameLen ) + pNameLen;
        }

        if ( ticketFlags != null )
        {
            ticketFlagsLen = ticketFlags.getData().length;
            ticketFlagsLen = 1 + TLV.getNbBytes( ticketFlagsLen ) + ticketFlagsLen;
            krbKredInfoSeqLen += 1 + TLV.getNbBytes( ticketFlagsLen ) + ticketFlagsLen;
        }

        if ( authTime != null )
        {
            authTimeBytes = authTime.getBytes();
            authTimeLen = 1 + TLV.getNbBytes( authTimeBytes.length ) + authTimeBytes.length;
            krbKredInfoSeqLen += 1 + TLV.getNbBytes( authTimeLen ) + authTimeLen;
        }

        if ( startTime != null )
        {
            startTimeBytes = startTime.getBytes();
            startTimeLen = 1 + TLV.getNbBytes( startTimeBytes.length ) + startTimeBytes.length;
            krbKredInfoSeqLen += 1 + TLV.getNbBytes( startTimeLen ) + startTimeLen;
        }

        if ( endTime != null )
        {
            endTimeBytes = endTime.getBytes();
            endTimeLen = 1 + TLV.getNbBytes( endTimeBytes.length ) + endTimeBytes.length;
            krbKredInfoSeqLen += 1 + TLV.getNbBytes( endTimeLen ) + endTimeLen;
        }

        if ( renewtill != null )
        {
            renewtillBytes = renewtill.getBytes();
            renewtillLen = 1 + TLV.getNbBytes( renewtillBytes.length ) + renewtillBytes.length;
            krbKredInfoSeqLen += 1 + TLV.getNbBytes( renewtillLen ) + renewtillLen;
        }

        if ( sRealm != null )
        {
            sRealmBytes = Strings.getBytesUtf8( sRealm );
            sRealmLen = 1 + TLV.getNbBytes( sRealmBytes.length ) + sRealmBytes.length;
            krbKredInfoSeqLen += 1 + TLV.getNbBytes( sRealmLen ) + sRealmLen;
        }

        if ( sName != null )
        {
            sNameLen = sName.computeLength();
            krbKredInfoSeqLen += 1 + TLV.getNbBytes( sNameLen ) + sNameLen;
        }

        if ( clientAddresses != null )
        {
            clientAddressesLen = clientAddresses.computeLength();
            krbKredInfoSeqLen += 1 + TLV.getNbBytes( clientAddressesLen ) + clientAddressesLen;
        }

        return 1 + TLV.getNbBytes( krbKredInfoSeqLen ) + krbKredInfoSeqLen;
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
            buffer.put( TLV.getBytes( krbKredInfoSeqLen ) );

            //key tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_CRED_INFO_KEY_TAG );
            buffer.put( TLV.getBytes( keyLen ) );
            key.encode( buffer );

            if ( pRealm != null )
            {
                // prealm tag and value
                buffer.put( ( byte ) KerberosConstants.KRB_CRED_INFO_PREALM_TAG );
                buffer.put( TLV.getBytes( pRealmLen ) );

                buffer.put( UniversalTag.GENERAL_STRING.getValue() );
                buffer.put( TLV.getBytes( pRealmBytes.length ) );
                buffer.put( pRealmBytes );
            }

            if ( pName != null )
            {
                // pname tag and value
                buffer.put( ( byte ) KerberosConstants.KRB_CRED_INFO_PNAME_TAG );
                buffer.put( TLV.getBytes( pNameLen ) );
                pName.encode( buffer );
            }

            if ( ticketFlags != null )
            {
                // flags tag and value
                buffer.put( ( byte ) KerberosConstants.KRB_CRED_INFO_FLAGS_TAG );
                buffer.put( TLV.getBytes( ticketFlagsLen ) );
                BerValue.encode( buffer, ticketFlags );
            }

            if ( authTime != null )
            {
                // authtime tag and value
                buffer.put( ( byte ) KerberosConstants.KRB_CRED_INFO_AUTHTIME_TAG );
                buffer.put( TLV.getBytes( authTimeLen ) );

                buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
                buffer.put( ( byte ) 0x0F );
                buffer.put( authTimeBytes );
            }

            if ( startTime != null )
            {
                // starttime tag and value
                buffer.put( ( byte ) KerberosConstants.KRB_CRED_INFO_STARTTIME_TAG );
                buffer.put( TLV.getBytes( startTimeLen ) );

                buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
                buffer.put( ( byte ) 0x0F );
                buffer.put( startTimeBytes );
            }

            if ( endTime != null )
            {
                // endtime tag and value
                buffer.put( ( byte ) KerberosConstants.KRB_CRED_INFO_ENDTIME_TAG );
                buffer.put( TLV.getBytes( endTimeLen ) );

                buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
                buffer.put( ( byte ) 0x0F );
                buffer.put( endTimeBytes );
            }

            if ( renewtill != null )
            {
                // renewtill tag and value
                buffer.put( ( byte ) KerberosConstants.KRB_CRED_INFO_RENEWTILL_TAG );
                buffer.put( TLV.getBytes( renewtillLen ) );

                buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
                buffer.put( ( byte ) 0x0F );
                buffer.put( renewtillBytes );
            }

            if ( sRealm != null )
            {
                // srealm tag and value
                buffer.put( ( byte ) KerberosConstants.KRB_CRED_INFO_SREALM_TAG );
                buffer.put( TLV.getBytes( sRealmLen ) );

                buffer.put( UniversalTag.GENERAL_STRING.getValue() );
                buffer.put( TLV.getBytes( sRealmBytes.length ) );
                buffer.put( sRealmBytes );
            }

            if ( sName != null )
            {
                // sname tag and value
                buffer.put( ( byte ) KerberosConstants.KRB_CRED_INFO_SNAME_TAG );
                buffer.put( TLV.getBytes( sNameLen ) );
                sName.encode( buffer );
            }

            if ( clientAddresses != null )
            {
                // caddr tag and value
                buffer.put( ( byte ) KerberosConstants.KRB_CRED_INFO_CADDR_TAG );
                buffer.put( TLV.getBytes( clientAddressesLen ) );
                clientAddresses.encode( buffer );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_739_CANNOT_ENCODE_KRB_CRED_INFO, 1 + TLV.getNbBytes( krbKredInfoSeqLen )
                + krbKredInfoSeqLen, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ), boe );
        }

        if ( IS_DEBUG )
        {
            log.debug( "KrbCredInfo encoding : {}", Strings.dumpBytes( buffer.array() ) );
            log.debug( "KrbCredInfo initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @return the key
     */
    public EncryptionKey getKey()
    {
        return key;
    }


    /**
     * @param key the key to set
     */
    public void setKey( EncryptionKey key )
    {
        this.key = key;
    }


    /**
     * @return the pRealm
     */
    public String getpRealm()
    {
        return pRealm;
    }


    /**
     * @param pRealm the pRealm to set
     */
    public void setpRealm( String pRealm )
    {
        this.pRealm = pRealm;
    }


    /**
     * @return the pName
     */
    public PrincipalName getpName()
    {
        return pName;
    }


    /**
     * @param pName the pName to set
     */
    public void setpName( PrincipalName pName )
    {
        this.pName = pName;
    }


    /**
     * @return the ticketFlags
     */
    public TicketFlags getTicketFlags()
    {
        return ticketFlags;
    }


    /**
     * @param ticketFlags the ticketFlags to set
     */
    public void setTicketFlags( TicketFlags ticketFlags )
    {
        this.ticketFlags = ticketFlags;
    }


    /**
     * @return the authTime
     */
    public KerberosTime getAuthTime()
    {
        return authTime;
    }


    /**
     * @param authTime the authTime to set
     */
    public void setAuthTime( KerberosTime authTime )
    {
        this.authTime = authTime;
    }


    /**
     * @return the startTime
     */
    public KerberosTime getStartTime()
    {
        return startTime;
    }


    /**
     * @param startTime the startTime to set
     */
    public void setStartTime( KerberosTime startTime )
    {
        this.startTime = startTime;
    }


    /**
     * @return the endTime
     */
    public KerberosTime getEndTime()
    {
        return endTime;
    }


    /**
     * @param endTime the endTime to set
     */
    public void setEndTime( KerberosTime endTime )
    {
        this.endTime = endTime;
    }


    /**
     * @return the renewtill
     */
    public KerberosTime getRenewtill()
    {
        return renewtill;
    }


    /**
     * @param renewtill the renewtill to set
     */
    public void setRenewtill( KerberosTime renewtill )
    {
        this.renewtill = renewtill;
    }


    /**
     * @return the sRealm
     */
    public String getsRealm()
    {
        return sRealm;
    }


    /**
     * @param sRealm the sRealm to set
     */
    public void setsRealm( String sRealm )
    {
        this.sRealm = sRealm;
    }


    /**
     * @return the sName
     */
    public PrincipalName getsName()
    {
        return sName;
    }


    /**
     * @param sName the sName to set
     */
    public void setsName( PrincipalName sName )
    {
        this.sName = sName;
    }


    /**
     * @return the clientAddresses
     */
    public HostAddresses getClientAddresses()
    {
        return clientAddresses;
    }


    /**
     * @param clientAddresses the clientAddresses to set
     */
    public void setClientAddresses( HostAddresses clientAddresses )
    {
        this.clientAddresses = clientAddresses;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "KrbCredInfo : {\n" );
        sb.append( "    key: " ).append( key ).append( '\n' );

        if ( pRealm != null )
        {
            sb.append( "    pRealm: " ).append( pRealm ).append( '\n' );
        }

        if ( pName != null )
        {
            sb.append( "    pName: " ).append( pName ).append( '\n' );
        }

        if ( ticketFlags != null )
        {
            sb.append( "    ticketFlags: " ).append( ticketFlags ).append( '\n' );
        }

        if ( authTime != null )
        {
            sb.append( "    authTime: " ).append( authTime ).append( '\n' );
        }

        if ( startTime != null )
        {
            sb.append( "    startTime: " ).append( startTime ).append( '\n' );
        }

        if ( endTime != null )
        {
            sb.append( "    endTime: " ).append( endTime ).append( '\n' );
        }

        if ( renewtill != null )
        {
            sb.append( "    renewtill: " ).append( renewtill ).append( '\n' );
        }

        if ( sRealm != null )
        {
            sb.append( "    sRealm: " ).append( sRealm ).append( '\n' );
        }

        if ( sName != null )
        {
            sb.append( "    sName: " ).append( sName ).append( '\n' );
        }

        if ( clientAddresses != null )
        {
            sb.append( "    clientAddresses: " ).append( clientAddresses ).append( '\n' );
        }

        sb.append( "}\n" );

        return sb.toString();

    }
}
