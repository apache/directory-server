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
import org.apache.directory.shared.kerberos.flags.TicketFlag;
import org.apache.directory.shared.kerberos.flags.TicketFlags;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * EncTicketPart   ::= [APPLICATION 3] SEQUENCE {
 *      flags                   [0] TicketFlags,
 *      key                     [1] EncryptionKey,
 *      crealm                  [2] Realm,
 *      cname                   [3] PrincipalName,
 *      transited               [4] TransitedEncoding,
 *      authtime                [5] KerberosTime,
 *      starttime               [6] KerberosTime OPTIONAL,
 *      endtime                 [7] KerberosTime,
 *      renew-till              [8] KerberosTime OPTIONAL,
 *      caddr                   [9] HostAddresses OPTIONAL,
 *      authorization-data      [10] AuthorizationData OPTIONAL
 * }
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncTicketPart extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( EncTicketPart.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** the ticket's flags */
    private TicketFlags flags = new TicketFlags();

    /** the encryption key */
    private EncryptionKey key;

    /** the client's realm */
    private String cRealm;

    /** client's principal */
    private PrincipalName cName;

    /** field containing list of transited realm names */
    private TransitedEncoding transited;

    /** time of initial authentication */
    private KerberosTime authTime;

    /** time after which ticket is valid */
    private KerberosTime startTime;

    /** ticket's expiry time */
    private KerberosTime endTime;

    /** the maximum endtime that may be included in a renewal */
    private KerberosTime renewtill;

    /** the addresses from which this ticket can be used */
    private HostAddresses clientAddresses;

    /** the authorization data */
    private AuthorizationData authorizationData;


    private transient int flagsLen;
    private transient int keyLen;
    private transient int cRealmLen;
    private transient byte[] cRealmBytes;
    private transient int cNameLen;
    private transient int transitedLen;
    private transient int authTimeLen;
    private transient byte[] authTimeBytes;
    private transient int startTimeLen;
    private transient byte[] startTimeBytes;
    private transient int endTimeLen;
    private transient byte[] endTimeBytes;
    private transient int renewtillLen;
    private transient byte[] renewtillBytes;
    private transient int clientAddressesLen;
    private transient int authzDataLen;
    private transient int encTikcetPartSeqLen;
    private transient int encTikcetPartLen;

    /**
     * compute length for EncTicketPart:
     * <pre>
     * 0x63 L1 EncTicketPart tag
     *  |
     *  +--> 0x30 L1-2 EncTicketPart seq
     *        |
     *        +--> 0xA0 L2 flags tag
     *        |     |
     *        |     +--> 0x03 L2-2 flags (BitString)
     *        |
     *        +--> 0xA1 L3 key tag
     *        |     |
     *        |     +--> 0x30 L3-2 key (EncryptionKey)
     *        |
     *        +--> 0xA2 L4 crealm tag
     *        |     |
     *        |     +--> 0x1B L4-2 crealm (Realm)
     *        |
     *        +--> 0xA3 L5 cname tag
     *        |     |
     *        |     +--> 0x30 L5-2 cname (PrincipalName)
     *        |
     *        +--> 0xA4 L6 transited tag
     *        |     |
     *        |     +--> 0x30 L6-2 transited (TransitedEncoding)
     *        |
     *        +--> 0xA5 0x11 authtime tag
     *        |     |
     *        |     +--> 0x18 0x0F authtime (KerberosTime)
     *        |
     *        +--> [0xA6 0x11 starttime tag
     *        |     |
     *        |     +--> 0x18 0x0F starttime (KerberosTime)]
     *        |
     *        +--> 0xA7 0x11 endtime tag
     *        |     |
     *        |     +--> 0x18 0x0F endtime (KerberosTime)
     *        |
     *        +--> [0xA8 0x11 renewtill tag
     *        |     |
     *        |     +--> 0x18 0x0F renewtill (KerberosTime)]
     *        |
     *        +--> [0xA9 L7 caddr tag
     *        |     |
     *        |     +--> 0x30 L7-2 caddre (HostAddresses)]
     *        |
     *        +--> [0xAA L8 authorization-data tag
     *              |
     *              +--> 0x30 L8-2 authorization-data (AuthorizationData)]
     * </pre>
     * 
     */
    @Override
    public int computeLength()
    {
        flagsLen = flags.getData().length;
        flagsLen = 1 + TLV.getNbBytes( flagsLen ) + flagsLen;
        encTikcetPartSeqLen = 1 + TLV.getNbBytes( flagsLen ) + flagsLen;

        keyLen = key.computeLength();
        encTikcetPartSeqLen += 1 + TLV.getNbBytes( keyLen ) + keyLen;

        cRealmBytes = StringTools.getBytesUtf8( cRealm );
        cRealmLen = 1 + TLV.getNbBytes( cRealmBytes.length ) + cRealmBytes.length;
        encTikcetPartSeqLen += 1 + TLV.getNbBytes( cRealmLen ) + cRealmLen;

        cNameLen = cName.computeLength();
        encTikcetPartSeqLen += 1 + TLV.getNbBytes( cNameLen ) + cNameLen;

        transitedLen = transited.computeLength();
        encTikcetPartSeqLen += 1 + TLV.getNbBytes( transitedLen ) + transitedLen;

        authTimeBytes = authTime.getBytes();
        authTimeLen = 1 + TLV.getNbBytes( authTimeBytes.length ) + authTimeBytes.length;
        encTikcetPartSeqLen += 1 + TLV.getNbBytes( authTimeLen ) + authTimeLen;

        if ( startTime != null )
        {
            startTimeBytes = startTime.getBytes();
            startTimeLen = 1 + TLV.getNbBytes( startTimeBytes.length ) + startTimeBytes.length;
            encTikcetPartSeqLen += 1 + TLV.getNbBytes( startTimeLen ) + startTimeLen;
        }

        endTimeBytes = endTime.getBytes();
        endTimeLen = 1 + TLV.getNbBytes( endTimeBytes.length ) + endTimeBytes.length;
        encTikcetPartSeqLen += 1 + TLV.getNbBytes( endTimeLen ) + endTimeLen;

        if ( renewtill != null )
        {
            renewtillBytes = renewtill.getBytes();
            renewtillLen = 1 + TLV.getNbBytes( renewtillBytes.length ) + renewtillBytes.length;
            encTikcetPartSeqLen += 1 + TLV.getNbBytes( renewtillLen ) + renewtillLen;
        }

        if ( clientAddresses != null )
        {
            clientAddressesLen = clientAddresses.computeLength();
            encTikcetPartSeqLen += 1 + TLV.getNbBytes( clientAddressesLen ) + clientAddressesLen;
        }

        if ( authorizationData != null )
        {
            authzDataLen = authorizationData.computeLength();
            encTikcetPartSeqLen += 1 + TLV.getNbBytes( authzDataLen ) + authzDataLen;
        }

        encTikcetPartLen = 1 + TLV.getNbBytes( encTikcetPartSeqLen ) + encTikcetPartSeqLen;

        return 1 + TLV.getNbBytes( encTikcetPartLen ) + encTikcetPartLen;
    }


    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_148 ) );
        }

        try
        {
            // EncTicketPart application tag and length
            buffer.put( ( byte ) KerberosConstants.ENC_TICKET_PART_TAG );
            buffer.put( TLV.getBytes( encTikcetPartLen ) );

            // EncTicketPart sequence tag and length
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( encTikcetPartSeqLen ) );

            // flags tag and int value
            buffer.put( ( byte ) KerberosConstants.ENC_TICKET_PART_FLAGS_TAG );
            buffer.put( TLV.getBytes( flagsLen ) );
            Value.encode( buffer, flags );

            // key tag and value
            buffer.put( ( byte ) KerberosConstants.ENC_TICKET_PART_KEY_TAG );
            buffer.put( TLV.getBytes( keyLen ) );
            key.encode( buffer );

            // crealm tag and value
            buffer.put( ( byte ) KerberosConstants.ENC_TICKET_PART_CREALM_TAG );
            buffer.put( TLV.getBytes( cRealmLen ) );
            buffer.put( UniversalTag.GENERAL_STRING.getValue() );
            buffer.put( TLV.getBytes( cRealmBytes.length ) );
            buffer.put( cRealmBytes );

            // cname tag and value
            buffer.put( ( byte ) KerberosConstants.ENC_TICKET_PART_CNAME_TAG );
            buffer.put( TLV.getBytes( cNameLen ) );
            cName.encode( buffer );

            // transited tag and value
            buffer.put( ( byte ) KerberosConstants.ENC_TICKET_PART_TRANSITED_TAG );
            buffer.put( TLV.getBytes( transitedLen ) );
            transited.encode( buffer );

            // authtime tag and value
            buffer.put( ( byte ) KerberosConstants.ENC_TICKET_PART_AUTHTIME_TAG );
            buffer.put( TLV.getBytes( authTimeLen ) );
            buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
            buffer.put( ( byte ) 0x0F );
            buffer.put( authTimeBytes );

            if ( startTime != null )
            {
                // strattime tag and value
                buffer.put( ( byte ) KerberosConstants.ENC_TICKET_PART_STARTTIME_TAG );
                buffer.put( TLV.getBytes( startTimeLen ) );
                buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
                buffer.put( ( byte ) 0x0F );
                buffer.put( startTimeBytes );
            }

            // endtime tag and value
            buffer.put( ( byte ) KerberosConstants.ENC_TICKET_PART_ENDTIME_TAG );
            buffer.put( TLV.getBytes( endTimeLen ) );
            buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
            buffer.put( ( byte ) 0x0F );
            buffer.put( endTimeBytes );

            if ( renewtill != null )
            {
                // renewtill tag and value
                buffer.put( ( byte ) KerberosConstants.ENC_TICKET_PART_RENEWTILL_TAG );
                buffer.put( TLV.getBytes( renewtillLen ) );
                buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
                buffer.put( ( byte ) 0x0F );
                buffer.put( renewtillBytes );
            }

            if ( clientAddresses != null )
            {
                // caddr tag and value
                buffer.put( ( byte ) KerberosConstants.ENC_TICKET_PART_CADDR_TAG );
                buffer.put( TLV.getBytes( clientAddressesLen ) );
                clientAddresses.encode( buffer );
            }

            if ( authorizationData != null )
            {
                // authorization-data tag and value
                buffer.put( ( byte ) KerberosConstants.ENC_TICKET_PART_AUTHORIZATION_DATA_TAG );
                buffer.put( TLV.getBytes( authzDataLen ) );
                authorizationData.encode( buffer );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_742_CANNOT_ENCODE_ENC_TICKET_PART, 1 + TLV.getNbBytes( encTikcetPartLen )
                + encTikcetPartLen, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            log.debug( "EncTicketPart encoding : {}", Strings.dumpBytes(buffer.array()) );
            log.debug( "EncTicketPart initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @return the flags
     */
    public TicketFlags getFlags()
    {
        return flags;
    }


    /**
     * @param flags the flags to set
     */
    public void setFlags( TicketFlags flags )
    {
        this.flags = flags;
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
     * @return the cRealm
     */
    public String getCRealm()
    {
        return cRealm;
    }


    /**
     * @param cRealm the cRealm to set
     */
    public void setCRealm( String cRealm )
    {
        this.cRealm = cRealm;
    }


    /**
     * @return the cName
     */
    public PrincipalName getCName()
    {
        return cName;
    }


    /**
     * @param cName the cName to set
     */
    public void setCName( PrincipalName cName )
    {
        this.cName = cName;
    }


    /**
     * @return the transited
     */
    public TransitedEncoding getTransited()
    {
        return transited;
    }


    /**
     * @param transited the transited to set
     */
    public void setTransited( TransitedEncoding transited )
    {
        this.transited = transited;
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
    public KerberosTime getRenewTill()
    {
        return renewtill;
    }


    /**
     * @param renewtill the renewtill to set
     */
    public void setRenewTill( KerberosTime renewtill )
    {
        this.renewtill = renewtill;
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
     * @return the authzData
     */
    public AuthorizationData getAuthorizationData()
    {
        return authorizationData;
    }


    /**
     * @param authzData the authzData to set
     */
    public void setAuthorizationData( AuthorizationData authzData )
    {
        this.authorizationData = authzData;
    }


    /**
     * adds the given flag to the already existing flags.
     * If no flags exist then creates a new TicketFlags object then sets this flag
     * and assigns the TicketFlags to this ticket part
     * 
     * @param flag the flag to be set
     */
    public void setFlag( TicketFlag flag )
    {
        if ( flags == null )
        {
            flags = new TicketFlags();
        }
        
        flags.setFlag( flag.getValue() );
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "EncTicketPart : {\n" );

        sb.append( "    flags: " ).append( flags ).append( '\n' );
        sb.append( "    key: " ).append( key ).append( '\n' );
        sb.append( "    cRealm: " ).append( cRealm ).append( '\n' );
        sb.append( "    cName: " ).append( cName ).append( '\n' );
        sb.append( "    transited: " ).append( transited ).append( '\n' );
        sb.append( "    authTime: " ).append( authTime ).append( '\n' );

        if ( startTime != null )
        {
            sb.append( "    startTime: " ).append( startTime ).append( '\n' );
        }

        sb.append( "    endTime: " ).append( endTime ).append( '\n' );

        if ( renewtill != null )
        {
            sb.append( "    renewtill: " ).append( renewtill ).append( '\n' );
        }

        if ( clientAddresses != null )
        {
            sb.append( "    clientAddresses: " ).append( clientAddresses ).append( '\n' );
        }

        if ( authorizationData != null )
        {
            sb.append( "    authzData: " ).append( authorizationData ).append( '\n' );
        }

        sb.append( "}\n" );

        return sb.toString();
    }

}
