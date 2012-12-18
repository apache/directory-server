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

import org.apache.directory.api.asn1.ber.tlv.BerValue;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.options.KdcOptions;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.apache.directory.shared.util.Strings;


/**
 * The KDC-REQ-BODY data structure. It will store the object described by the ASN.1 grammar :
 * <pre>
 * KDC-REQ-BODY    ::= SEQUENCE {
 *      kdc-options             [0] KDCOptions,
 *      cname                   [1] PrincipalName OPTIONAL
 *                                  -- Used only in AS-REQ --,
 *      realm                   [2] Realm
 *                                  -- Server's realm
 *                                  -- Also client's in AS-REQ --,
 *      sname                   [3] PrincipalName OPTIONAL,
 *      from                    [4] KerberosTime OPTIONAL,
 *      till                    [5] KerberosTime,
 *      rtime                   [6] KerberosTime OPTIONAL,
 *      nonce                   [7] UInt32,
 *      etype                   [8] SEQUENCE OF Int32 -- EncryptionType
 *                                  -- in preference order --,
 *      addresses               [9] HostAddresses OPTIONAL,
 *      enc-authorization-data  [10] EncryptedData OPTIONAL
 *                                  -- AuthorizationData --,
 *      additional-tickets      [11] SEQUENCE OF Ticket OPTIONAL
 *                                      -- NOTE: not empty
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KdcReqBody extends AbstractAsn1Object
{
    /** The KDC options */
    private KdcOptions kdcOptions;

    /** The Client Principal, if the request is an AS-REQ */
    private PrincipalName cName;

    /** The realm */
    private String realm;

    /** The Server Principal */
    private PrincipalName sName;

    /** The start time for the requested ticket */
    private KerberosTime from;

    /** The expiration date for the requested ticket */
    private KerberosTime till;

    /** The renew-till date for the requested ticket */
    private KerberosTime rtime;

    /** Random number to avoid MiM attacks */
    private int nonce;

    /** List of desired encryption types */
    private List<EncryptionType> eType;

    /** Addresses valid for the requested ticket */
    private HostAddresses addresses;

    /** Encoded authorizationData, used by the TGS-REQ only */
    private EncryptedData encAuthorizationData;

    /** Additional tickets */
    private List<Ticket> additionalTickets;

    // Storage for computed lengths
    private int kdcOptionsLength;
    private int cNameLength;
    private int realmLength;
    private byte[] realmBytes;
    private int sNameLength;
    private int fromLength;
    private int tillLength;
    private int rtimeLength;
    private int nonceLength;
    private int eTypeLength;
    private int eTypeSeqLength;
    private int[] eTypeLengths;
    private int addressesLength;
    private int encAuthzDataLength;
    private int additionalTicketLength;
    private int additionalTicketSeqLength;
    private int[] additionalTicketsLengths;
    private int kdcReqBodySeqLength;
    private int kdcReqBodyLength;


    /**
     * Creates a new instance of RequestBody.
     */
    public KdcReqBody()
    {
        additionalTickets = new ArrayList<Ticket>();
        eType = new ArrayList<EncryptionType>();
    }


    /**
     * Returns the additional {@link Ticket}s.
     *
     * @return The additional {@link Ticket}s.
     */
    public Ticket[] getAdditionalTickets()
    {
        return additionalTickets.toArray( new Ticket[]
            {} );
    }


    /**
     * Set the list of additional Ticket
     * @param additionalTickets the additionalTickets to set
     */
    public void setAdditionalTickets( List<Ticket> additionalTickets )
    {
        this.additionalTickets = additionalTickets;
    }


    /**
     * Add a new Ticket to the list of additional tickets
     * @param additionalTickets the additionalTickets to set
     */
    public void addAdditionalTicket( Ticket additionalTicket )
    {
        this.additionalTickets.add( additionalTicket );
    }


    /**
     * Returns the {@link HostAddresses}.
     *
     * @return The {@link HostAddresses}.
     */
    public HostAddresses getAddresses()
    {
        return addresses;
    }


    /**
     * @param addresses the addresses to set
     */
    public void setAddresses( HostAddresses addresses )
    {
        this.addresses = addresses;
    }


    /**
     * @return the client PrincipalName
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
     * Returns the encrypted {@link AuthorizationData} as {@link EncryptedData}.
     *
     * @return The encrypted {@link AuthorizationData} as {@link EncryptedData}.
     */
    public EncryptedData getEncAuthorizationData()
    {
        return encAuthorizationData;
    }


    /**
     * @param encAuthorizationData the encAuthorizationData to set
     */
    public void setEncAuthorizationData( EncryptedData encAuthorizationData )
    {
        this.encAuthorizationData = encAuthorizationData;
    }


    /**
     * Returns the requested {@link EncryptionType}s.
     *
     * @return The requested {@link EncryptionType}s.
     */
    public List<EncryptionType> getEType()
    {
        return eType;
    }


    /**
     * @param eType the eType to set
     */
    public void setEType( List<EncryptionType> eType )
    {
        this.eType = eType;
    }


    /**
     * @param eType the eType to add
     */
    public void addEType( EncryptionType eType )
    {
        this.eType.add( eType );
    }


    /**
     * Returns the from {@link KerberosTime}.
     *
     * @return The from {@link KerberosTime}.
     */
    public KerberosTime getFrom()
    {
        return from;
    }


    /**
     * @param from the from to set
     */
    public void setFrom( KerberosTime from )
    {
        this.from = from;
    }


    /**
     * Returns the {@link KdcOptions}.
     *
     * @return The {@link KdcOptions}.
     */
    public KdcOptions getKdcOptions()
    {
        return kdcOptions;
    }


    /**
     * @param kdcOptions the kdcOptions to set
     */
    public void setKdcOptions( KdcOptions kdcOptions )
    {
        this.kdcOptions = kdcOptions;
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
     * @param nonce the nonce to set
     */
    public void setNonce( int nonce )
    {
        this.nonce = nonce;
    }


    /**
     * @return the realm
     */
    public String getRealm()
    {
        return realm;
    }


    /**
     * @param realm the realm to set
     */
    public void setRealm( String realm )
    {
        this.realm = realm;
    }


    /**
     * Returns the RenewTime {@link KerberosTime}.
     *
     * @return The RenewTime {@link KerberosTime}.
     */
    public KerberosTime getRTime()
    {
        return rtime;
    }


    /**
     * @param rtime the renewTime to set
     */
    public void setRtime( KerberosTime rtime )
    {
        this.rtime = rtime;
    }


    /**
     * Returns the server {@link PrincipalName}.
     *
     * @return The server {@link PrincipalName}.
     */
    public PrincipalName getSName()
    {
        return sName;
    }


    /**
     * @param sName the sName to set
     */
    public void setSName( PrincipalName sName )
    {
        this.sName = sName;
    }


    /**
     * Returns the till {@link KerberosTime}.
     *
     * @return The till {@link KerberosTime}.
     */
    public KerberosTime getTill()
    {
        return till;
    }


    /**
     * @param till the till to set
     */
    public void setTill( KerberosTime till )
    {
        this.till = till;
    }


    /**
     * Compute the KdcReqBody length
     * <pre>
     * KdcReqBody :
     *
     * 0x30 L1 KdcReqBody sequence
     *  |
     *  +--> 0xA0 L2 kdc-options tag
     *  |     |
     *  |     +--> 0x03 L2-1 kdc-options (BitString)
     *  |
     *  +--> 0xA1 L3 cname tag
     *  |     |
     *  |     +--> 0x30 L3-1 cname (PrincipalName)
     *  |
     *  +--> 0xA2 L4 realm tag
     *  |     |
     *  |     +--> 0x1B L4-1 realm (Realm, KerberosString)
     *  |
     *  +--> 0xA3 L5 sname tag
     *  |     |
     *  |     +--> 0x30 L5-1 sname (PrincipalName)
     *  |
     *  +--> 0xA4 L6 from tag
     *  |     |
     *  |     +--> 0x18 L6-1 from (KerberosTime)
     *  |
     *  +--> 0xA5 L7 till tag
     *  |     |
     *  |     +--> 0x18 L7-1 till (KerberosTime)
     *  |
     *  +--> 0xA6 L8 rtime tag
     *  |     |
     *  |     +--> 0x18 L8-1 rtime (KerberosTime)
     *  |
     *  +--> 0xA7 L9 nonce tag
     *  |     |
     *  |     +--> 0x02 L9-1 nonce (Int)
     *  |
     *  +--> 0xA8 L10 etype tag
     *  |     |
     *  |     +--> 0x30 L10-1 SEQ
     *  |           |
     *  |           +--> 0x02 L10-1-1 etype
     *  |           |
     *  |           +--> 0x02 L10-1-2 etype
     *  |           |
     *  |           :
     *  |
     *  +--> 0xA9 L11 addresses tag
     *  |     |
     *  |     +--> 0x30 L11-1 addresses (HostAddresses)
     *  |
     *  +--> 0xAA L12 enc-authorization-data tag
     *  |     |
     *  |     +--> 0x30 L12-1 enc-authorization-data
     *  |
     *  +--> 0xAB L13 additional-tickets tag
     *        |
     *        +--> 0x30 L13-1 additional-tickets
     *              |
     *              +--> 0x61 L13-1-1 Ticket
     *              |
     *              +--> 0x61 L13-1-2 Ticket
     *              |
     *              :
     * </pre>
     */
    @Override
    public int computeLength()
    {
        reset();

        // The KdcOptions length
        kdcOptionsLength = 1 + 1 + kdcOptions.getBytes().length;
        kdcReqBodySeqLength = 1 + TLV.getNbBytes( kdcOptionsLength ) + kdcOptionsLength;

        // The cname length
        if ( cName != null )
        {
            cNameLength = cName.computeLength();
            kdcReqBodySeqLength += 1 + TLV.getNbBytes( cNameLength ) + cNameLength;
        }

        // Compute the realm length.
        realmBytes = Strings.getBytesUtf8( realm );
        realmLength = 1 + TLV.getNbBytes( realmBytes.length ) + realmBytes.length;
        kdcReqBodySeqLength += 1 + TLV.getNbBytes( realmLength ) + realmLength;

        // The sname length
        if ( sName != null )
        {
            sNameLength = sName.computeLength();
            kdcReqBodySeqLength += 1 + TLV.getNbBytes( sNameLength ) + sNameLength;
        }

        // The from length
        if ( from != null )
        {
            fromLength = 1 + 1 + 0x0F;
            kdcReqBodySeqLength += 1 + 1 + fromLength;
        }

        // The till length
        tillLength = 1 + 1 + 0x0F;
        kdcReqBodySeqLength += 1 + 1 + tillLength;

        // The rtime length
        if ( rtime != null )
        {
            rtimeLength = 1 + 1 + 0x0F;
            kdcReqBodySeqLength += 1 + 1 + rtimeLength;
        }

        // The nonce length
        nonceLength = 1 + 1 + BerValue.getNbBytes( nonce );
        kdcReqBodySeqLength += 1 + 1 + nonceLength;

        // The eType length
        eTypeLengths = new int[eType.size()];
        int pos = 0;
        eTypeSeqLength = 0;

        for ( EncryptionType encryptionType : eType )
        {
            eTypeLengths[pos] = 1 + 1 + BerValue.getNbBytes( encryptionType.getValue() );
            eTypeSeqLength += eTypeLengths[pos];
            pos++;
        }

        eTypeLength = 1 + TLV.getNbBytes( eTypeSeqLength ) + eTypeSeqLength;
        kdcReqBodySeqLength += 1 + TLV.getNbBytes( eTypeLength ) + eTypeLength;

        // The Addresses length
        if ( addresses != null )
        {
            addressesLength = addresses.computeLength();
            kdcReqBodySeqLength += 1 + TLV.getNbBytes( addressesLength ) + addressesLength;
        }

        // The EncAuthorizationData length
        if ( encAuthorizationData != null )
        {
            encAuthzDataLength = encAuthorizationData.computeLength();
            kdcReqBodySeqLength += 1 + TLV.getNbBytes( encAuthzDataLength ) + encAuthzDataLength;
        }

        // The additionalTickets length
        if ( additionalTickets.size() != 0 )
        {
            additionalTicketsLengths = new int[additionalTickets.size()];
            additionalTicketSeqLength = 0;
            pos = 0;

            for ( Ticket ticket : additionalTickets )
            {
                additionalTicketsLengths[pos] = ticket.computeLength();
                additionalTicketSeqLength += additionalTicketsLengths[pos];
                pos++;
            }

            additionalTicketLength = 1 + TLV.getNbBytes( additionalTicketSeqLength ) + additionalTicketSeqLength;
            kdcReqBodySeqLength += 1 + TLV.getNbBytes( additionalTicketLength ) + additionalTicketLength;
        }

        // compute the global size
        kdcReqBodyLength = 1 + TLV.getNbBytes( kdcReqBodySeqLength ) + kdcReqBodySeqLength;

        return kdcReqBodyLength;
    }


    /**
     * Encode the KDC-REQ-BODY component
     *
     * @param buffer The buffer containing the encoded result
     * @return The encoded component
     * @throws EncoderException If the encoding failed
     */
    @Override
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_148 ) );
        }

        // The KDC-REQ-BODY SEQ Tag
        buffer.put( UniversalTag.SEQUENCE.getValue() );
        buffer.put( TLV.getBytes( kdcReqBodySeqLength ) );

        // The KdcOptions -----------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REQ_BODY_KDC_OPTIONS_TAG );
        buffer.put( TLV.getBytes( kdcOptionsLength ) );

        // The value
        BerValue.encode( buffer, kdcOptions );

        // The cname if any ---------------------------------------------------
        if ( cName != null )
        {
            // The tag
            buffer.put( ( byte ) KerberosConstants.KDC_REQ_BODY_CNAME_TAG );
            buffer.put( TLV.getBytes( cNameLength ) );

            // The value
            cName.encode( buffer );
        }

        // The realm ----------------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REQ_BODY_REALM_TAG );
        buffer.put( TLV.getBytes( realmLength ) );

        // The value
        buffer.put( UniversalTag.GENERAL_STRING.getValue() );
        buffer.put( TLV.getBytes( realmBytes.length ) );
        buffer.put( realmBytes );

        // The sname, if any --------------------------------------------------
        if ( sName != null )
        {
            // The tag
            buffer.put( ( byte ) KerberosConstants.KDC_REQ_BODY_SNAME_TAG );
            buffer.put( TLV.getBytes( sNameLength ) );

            // The value
            sName.encode( buffer );
        }

        // The from, if any ---------------------------------------------------
        if ( from != null )
        {
            // The tag
            buffer.put( ( byte ) KerberosConstants.KDC_REQ_BODY_FROM_TAG );
            buffer.put( TLV.getBytes( fromLength ) );

            // The value
            buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
            buffer.put( ( byte ) 0x0F );
            buffer.put( from.getBytes() );
        }

        // The till -----------------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REQ_BODY_TILL_TAG );
        buffer.put( TLV.getBytes( tillLength ) );

        // The value
        buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
        buffer.put( ( byte ) 0x0F );
        buffer.put( till.getBytes() );

        // The rtime if any ---------------------------------------------------
        if ( rtime != null )
        {
            // The tag
            buffer.put( ( byte ) KerberosConstants.KDC_REQ_BODY_RTIME_TAG );
            buffer.put( TLV.getBytes( rtimeLength ) );

            // The value
            buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
            buffer.put( ( byte ) 0x0F );
            buffer.put( rtime.getBytes() );
        }

        // The nonce ----------------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REQ_BODY_NONCE_TAG );
        buffer.put( TLV.getBytes( nonceLength ) );

        // The value
        BerValue.encode( buffer, nonce );

        // The etype ----------------------------------------------------------
        // The tag
        buffer.put( ( byte ) KerberosConstants.KDC_REQ_BODY_ETYPE_TAG );
        buffer.put( TLV.getBytes( eTypeLength ) );

        // The sequence
        buffer.put( UniversalTag.SEQUENCE.getValue() );
        buffer.put( TLV.getBytes( eTypeSeqLength ) );

        // The values
        for ( EncryptionType encryptionType : eType )
        {
            BerValue.encode( buffer, encryptionType.getValue() );
        }

        // The addresses if any -----------------------------------------------
        if ( addresses != null )
        {
            // The tag
            buffer.put( ( byte ) KerberosConstants.KDC_REQ_BODY_ADDRESSES_TAG );
            buffer.put( TLV.getBytes( addressesLength ) );

            // The value
            addresses.encode( buffer );
        }

        // The enc-authorization-data, if any ---------------------------------
        if ( encAuthorizationData != null )
        {
            // The tag
            buffer.put( ( byte ) KerberosConstants.KDC_REQ_BODY_ENC_AUTHZ_DATA_TAG );
            buffer.put( TLV.getBytes( encAuthzDataLength ) );

            // The value
            encAuthorizationData.encode( buffer );
        }

        // The additional-tickets, if any -------------------------------------
        if ( additionalTickets.size() != 0 )
        {
            // The tag
            buffer.put( ( byte ) KerberosConstants.KDC_REQ_BODY_ADDITIONAL_TICKETS_TAG );
            buffer.put( TLV.getBytes( additionalTicketLength ) );

            // The sequence
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( additionalTicketSeqLength ) );

            // The values
            for ( Ticket ticket : additionalTickets )
            {
                ticket.encode( buffer );
            }
        }

        return buffer;
    }


    /**
     * reset the fields used while computing length
     */
    private void reset()
    {
        kdcOptionsLength = 0;
        cNameLength = 0;
        realmLength = 0;
        realmBytes = null;
        sNameLength = 0;
        fromLength = 0;
        tillLength = 0;
        rtimeLength = 0;
        nonceLength = 0;
        eTypeLength = 0;
        eTypeSeqLength = 0;
        eTypeLengths = null;
        addressesLength = 0;
        encAuthzDataLength = 0;
        additionalTicketLength = 0;
        additionalTicketSeqLength = 0;
        additionalTicketsLengths = null;
        kdcReqBodySeqLength = 0;
        kdcReqBodyLength = 0;
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "KDCOptions : " ).append( kdcOptions ).append( '\n' );

        if ( cName != null )
        {
            sb.append( "cname : " ).append( cName ).append( '\n' );
        }

        sb.append( "realm : " ).append( realm ).append( '\n' );

        if ( sName != null )
        {
            sb.append( "sname : " ).append( sName ).append( '\n' );
        }

        if ( from != null )
        {
            sb.append( "from : " ).append( from ).append( '\n' );
        }

        sb.append( "till : " ).append( till ).append( '\n' );

        if ( rtime != null )
        {
            sb.append( "rtime : " ).append( rtime ).append( '\n' );
        }

        sb.append( "nonce : " ).append( nonce ).append( '\n' );

        sb.append( "etype : " );
        boolean isFirst = true;

        for ( EncryptionType encryptionType : eType )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( " " );
            }

            sb.append( encryptionType );
        }

        sb.append( '\n' );

        if ( addresses != null )
        {
            sb.append( "addresses : " );
            isFirst = true;

            for ( HostAddress hostAddress : addresses.getAddresses() )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( " " );
                }

                sb.append( hostAddress );
            }

            sb.append( '\n' );
        }

        if ( encAuthorizationData != null )
        {
            sb.append( "enc-authorization-data" ).append( encAuthorizationData ).append( '\n' );
        }

        if ( additionalTickets.size() != 0 )
        {
            sb.append( "Tickets : " );
            isFirst = true;

            for ( Ticket ticket : additionalTickets )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( " " );
                }

                sb.append( ticket );
            }

            sb.append( '\n' );
        }

        return sb.toString();
    }
}
