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

import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.asn1.ber.tlv.BerValue;
import org.apache.directory.api.asn1.ber.tlv.TLV;
import org.apache.directory.api.asn1.ber.tlv.UniversalTag;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class representing KRB-ERROR message
 * 
 * <pre>
 * KRB-ERROR       ::= [APPLICATION 30] SEQUENCE {
 *      pvno            [0] INTEGER (5),
 *      msg-type        [1] INTEGER (30),
 *      ctime           [2] KerberosTime OPTIONAL,
 *      cusec           [3] Microseconds OPTIONAL,
 *      stime           [4] KerberosTime,
 *      susec           [5] Microseconds,
 *      error-code      [6] Int32,
 *      crealm          [7] Realm OPTIONAL,
 *      cname           [8] PrincipalName OPTIONAL,
 *      realm           [9] Realm -- service realm --,
 *      sname           [10] PrincipalName -- service name --,
 *      e-text          [11] KerberosString OPTIONAL,
 *      e-data          [12] OCTET STRING OPTIONAL
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbError extends KerberosMessage
{

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( KrbError.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** the current time of client */
    private KerberosTime cTime;

    /** microseconds of the client's current time */
    private Integer cusec;

    /** current time on the server */
    private KerberosTime sTime;

    /** microseconds of the server's time */
    private int susec;

    /** the error code */
    private ErrorType errorCode;

    /** the name of the realm to which the requesting client belongs */
    private String cRealm;

    /** the client's principal */
    private PrincipalName cName;

    /** the realm that issued the ticket */
    private String realm;

    /** the server's principal */
    private PrincipalName sName;

    /** the error text */
    private String eText;

    /** the error data */
    private byte[] eData;

    // Storage for computed lengths
    private int pvnoLength;
    private int msgTypeLength;
    private int cTimeLength;
    private int cusecLength;
    private int sTimeLength;
    private int susecLength;
    private int errorCodeLength;
    private int cRealmLength;
    private byte[] crealmBytes;
    private int cNameLength;
    private int realmLength;
    private byte[] realmBytes;
    private int sNameLength;
    private int eTextLength;
    private byte[] eTextBytes;
    private int eDataLength;
    private int krbErrorSeqLength;
    private int krbErrorLength;


    /**
     * Creates a new instance of KrbError.
     */
    public KrbError()
    {
        super( KerberosMessageType.KRB_ERROR );
    }


    /**
     * @return the cTime
     */
    public KerberosTime getCTime()
    {
        return cTime;
    }


    /**
     * @param cTime the cTime to set
     */
    public void setCTime( KerberosTime cTime )
    {
        this.cTime = cTime;
    }


    /**
     * @return the cusec
     */
    public int getCusec()
    {
        if ( cusec == null )
        {
            return 0;
        }

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
     * @return the sTime
     */
    public KerberosTime getSTime()
    {
        return sTime;
    }


    /**
     * @param sTime the sTime to set
     */
    public void setSTime( KerberosTime sTime )
    {
        this.sTime = sTime;
    }


    /**
     * @return the susec
     */
    public int getSusec()
    {
        return susec;
    }


    /**
     * @param susec the susec to set
     */
    public void setSusec( int susec )
    {
        this.susec = susec;
    }


    /**
     * @return the errorCode
     */
    public ErrorType getErrorCode()
    {
        return errorCode;
    }


    /**
     * @param errorCode the errorCode to set
     */
    public void setErrorCode( ErrorType errorCode )
    {
        this.errorCode = errorCode;
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
     * @return the sName
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
     * @return the eText
     */
    public String getEText()
    {
        return eText;
    }


    /**
     * @param eText the eText to set
     */
    public void setEText( String eText )
    {
        this.eText = eText;
    }


    /**
     * @return the eData
     */
    public byte[] getEData()
    {
        return eData;
    }


    /**
     * @param eData the eData to set
     */
    public void setEData( byte[] eData )
    {
        this.eData = eData;
    }


    /**
     * Compute the KRB-ERROR length
     * <pre>
     * KRB-ERROR :
     * 
     * 0x7E L1 KRB-ERROR APPLICATION[30]
     *  |
     *  +--> 0x30 L2 KRB-ERROR sequence
     *        |
     *        +--> 0xA0 0x03 pvno tag
     *        |     |
     *        |     +--> 0x02 0x01 0x05 pvno (5)
     *        |
     *        +--> 0xA1 0x03 msg-type tag
     *        |     |
     *        |     +--> 0x02 0x01 0x1E msg-type (30)
     *        |     
     *        +--> 0xA2 0x11 ctime tag
     *        |     |
     *        |     +--> 0x18 0x0F ttt ctime (KerberosTime)
     *        |     
     *        +--> 0xA3 L3 cusec tag
     *        |     |
     *        |     +--> 0x02 L3-1 cusec
     *        |     
     *        +--> 0xA4 0x11 stime tag
     *        |     |
     *        |     +--> 0x18 0x0F ttt stime (KerberosTime)
     *        |     
     *        +--> 0xA5 L4 susec tag
     *        |     |
     *        |     +--> 0x02 L4-1 susec (KerberosTime)
     *        |     
     *        +--> 0xA6 L5 error-code tag
     *        |     |
     *        |     +--> 0x02 L5-1 nnn error-code
     *        |     
     *        +--> 0xA7 L6 crealm tag
     *        |     |
     *        |     +--> 0x1B L6-1 crealm (KerberosString)
     *        |     
     *        +--> 0xA8 L7 cname tag
     *        |     |
     *        |     +--> 0x30 L7-1 cname (PrincipalName)
     *        |
     *        +--> 0xA9 L8 realm tag
     *        |     |
     *        |     +--> 0x1B L8-1 realm (KerberosString)
     *        |     
     *        +--> 0xAA L9 sname tag
     *        |     |
     *        |     +--> 0x30 L9-1 sname (PrincipalName)
     *        |     
     *        +--> 0xAB L10 e-text tag
     *        |     |
     *        |     +--> 0x1B L10-1 e-text (KerberosString)
     *        |
     *        +--> 0xAC L11 e-data
     *              |
     *              +--> 0x04 L11-1 e-data (Octet String)
     * </pre>       
     */
    public int computeLength()
    {
        // The PVNO
        pvnoLength = 1 + 1 + 1;
        krbErrorSeqLength = 1 + TLV.getNbBytes( pvnoLength ) + pvnoLength;

        // The message type
        msgTypeLength = 1 + 1 + BerValue.getNbBytes( getMessageType().getValue() );
        krbErrorSeqLength += 1 + TLV.getNbBytes( msgTypeLength ) + msgTypeLength;

        // The ctime, if any
        if ( cTime != null )
        {
            cTimeLength = 1 + 1 + 0x0F;
            krbErrorSeqLength += 1 + TLV.getNbBytes( cTimeLength ) + cTimeLength;
        }

        // The cusec, if any
        if ( cusec != null )
        {
            int cusecLen = BerValue.getNbBytes( cusec );
            cusecLength = 1 + TLV.getNbBytes( cusecLen ) + cusecLen;
            krbErrorSeqLength += 1 + TLV.getNbBytes( cusecLength ) + cusecLength;
        }

        // The stime
        sTimeLength = 1 + 1 + 0x0F;
        krbErrorSeqLength += 1 + TLV.getNbBytes( sTimeLength ) + sTimeLength;

        // The susec
        int susecLen = BerValue.getNbBytes( susec );
        susecLength = 1 + TLV.getNbBytes( susecLen ) + susecLen;
        krbErrorSeqLength += 1 + TLV.getNbBytes( susecLength ) + susecLength;

        // The error-code
        errorCodeLength = 1 + 1 + BerValue.getNbBytes( errorCode.getValue() );
        krbErrorSeqLength += 1 + TLV.getNbBytes( errorCodeLength ) + errorCodeLength;

        // The crealm, if any
        if ( cRealm != null )
        {
            crealmBytes = Strings.getBytesUtf8( cRealm );
            cRealmLength = 1 + TLV.getNbBytes( crealmBytes.length ) + crealmBytes.length;
            krbErrorSeqLength += 1 + TLV.getNbBytes( cRealmLength ) + cRealmLength;
        }

        // The cname if any
        if ( cName != null )
        {
            cNameLength = cName.computeLength();
            krbErrorSeqLength += 1 + TLV.getNbBytes( cNameLength ) + cNameLength;
        }

        // The realm
        realmBytes = Strings.getBytesUtf8( realm );
        realmLength = 1 + TLV.getNbBytes( realmBytes.length ) + realmBytes.length;
        krbErrorSeqLength += 1 + TLV.getNbBytes( realmLength ) + realmLength;

        // The sname
        sNameLength = sName.computeLength();
        krbErrorSeqLength += 1 + TLV.getNbBytes( sNameLength ) + sNameLength;

        // The e-text, if any
        if ( eText != null )
        {
            eTextBytes = Strings.getBytesUtf8( eText );
            eTextLength = 1 + TLV.getNbBytes( eTextBytes.length ) + eTextBytes.length;
            krbErrorSeqLength += 1 + TLV.getNbBytes( eTextLength ) + eTextLength;
        }

        // The e-data, if any
        if ( eData != null )
        {
            eDataLength = 1 + TLV.getNbBytes( eData.length ) + eData.length;
            krbErrorSeqLength += 1 + TLV.getNbBytes( eDataLength ) + eDataLength;
        }

        // The global sequence length
        krbErrorLength = 1 + TLV.getNbBytes( krbErrorSeqLength ) + krbErrorSeqLength;

        return 1 + TLV.getNbBytes( krbErrorLength ) + krbErrorLength;
    }


    /**
     * Encode the KRB-ERROR message to a PDU. 
     * <pre>
     * KRB-ERROR :
     * 
     * 0x7E LL
     *   0x30 LL
     *     0xA0 0x03 
     *       0x02 0x01 0x05  pvno 
     *     0xA1 0x03 
     *       0x02 0x01 0x1E msg-type
     *    [0xA2 0x11
     *       0x18 0x0F ttt] ctime
     *    [0xA3 LL
     *       0x02 LL nnn] cusec
     *     0xA4 0x11
     *       0x18 0x0F ttt  stime
     *     0xA5 LL
     *       0x02 LL nnn susec
     *     0xA6 LL
     *       0x02 LL nnn error-code
     *    [0xA7 LL
     *       0x1B LL abcd] crealm
     *    [0xA8 LL
     *       0x30 LL abcd] cname
     *     0xA9 LL
     *       0x1B LL abcd realm
     *     0xAA LL
     *       0x30 LL abcd sname
     *    [0xAB LL
     *       0x1B LL abcd] e-text
     *    [0xAC LL
     *       0x04 LL abcd] e-data
     * </pre>
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
            // The KRB-ERROR APPLICATION tag
            buffer.put( ( byte ) KerberosConstants.KRB_ERROR_TAG );
            buffer.put( TLV.getBytes( krbErrorLength ) );

            // The KRB_ERROR sequence
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( krbErrorSeqLength ) );

            // pvno tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_ERROR_PVNO_TAG );
            buffer.put( TLV.getBytes( pvnoLength ) );
            BerValue.encode( buffer, getProtocolVersionNumber() );

            // msg-type tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_ERROR_MSGTYPE_TAG );
            buffer.put( TLV.getBytes( msgTypeLength ) );
            BerValue.encode( buffer, getMessageType().getValue() );

            // ctime tag and value if any
            if ( cTimeLength > 0 )
            {
                // The tag
                buffer.put( ( byte ) KerberosConstants.KRB_ERROR_CTIME_TAG );
                buffer.put( TLV.getBytes( cTimeLength ) );

                // The value
                buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
                buffer.put( ( byte ) 0x0F );
                buffer.put( cTime.getBytes() );
            }

            // cusec tag and value if any
            if ( cusec != null )
            {
                buffer.put( ( byte ) KerberosConstants.KRB_ERROR_CUSEC_TAG );
                buffer.put( TLV.getBytes( cusecLength ) );
                BerValue.encode( buffer, cusec );
            }

            // stime tag and value
            // The tag
            buffer.put( ( byte ) KerberosConstants.KRB_ERROR_STIME_TAG );
            buffer.put( TLV.getBytes( sTimeLength ) );

            // The value
            buffer.put( UniversalTag.GENERALIZED_TIME.getValue() );
            buffer.put( ( byte ) 0x0F );
            buffer.put( sTime.getBytes() );

            // susec tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_ERROR_SUSEC_TAG );
            buffer.put( TLV.getBytes( susecLength ) );
            BerValue.encode( buffer, susec );

            // error-code tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_ERROR_ERROR_CODE_TAG );
            buffer.put( TLV.getBytes( errorCodeLength ) );
            BerValue.encode( buffer, errorCode.getValue() );

            // crealm tage and value, if any
            if ( cRealm != null )
            {
                buffer.put( ( byte ) KerberosConstants.KRB_ERROR_CREALM_TAG );
                buffer.put( TLV.getBytes( cRealmLength ) );

                buffer.put( UniversalTag.GENERAL_STRING.getValue() );
                buffer.put( TLV.getBytes( crealmBytes.length ) );
                buffer.put( crealmBytes );
            }

            // cname tag and value, if any
            if ( cName != null )
            {
                buffer.put( ( byte ) KerberosConstants.KRB_ERROR_CNAME_TAG );
                buffer.put( TLV.getBytes( cNameLength ) );
                cName.encode( buffer );
            }

            // realm tag and value
            // the tag
            buffer.put( ( byte ) KerberosConstants.KRB_ERROR_REALM_TAG );
            buffer.put( TLV.getBytes( realmLength ) );

            // The value
            buffer.put( UniversalTag.GENERAL_STRING.getValue() );
            buffer.put( TLV.getBytes( realmBytes.length ) );
            buffer.put( realmBytes );

            // sname tag and value
            buffer.put( ( byte ) KerberosConstants.KRB_ERROR_SNAME_TAG );
            buffer.put( TLV.getBytes( sNameLength ) );
            sName.encode( buffer );

            // etext tag and value, if any
            if ( eText != null )
            {
                buffer.put( ( byte ) KerberosConstants.KRB_ERROR_ETEXT_TAG );
                buffer.put( TLV.getBytes( eTextLength ) );

                buffer.put( UniversalTag.GENERAL_STRING.getValue() );
                buffer.put( TLV.getBytes( eTextBytes.length ) );
                buffer.put( eTextBytes );
            }

            // edata tag and value, if any
            if ( eData != null )
            {
                buffer.put( ( byte ) KerberosConstants.KRB_ERROR_EDATA_TAG );
                buffer.put( TLV.getBytes( eDataLength ) );
                BerValue.encode( buffer, eData );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_734_CANNOT_ENCODE_KRBERROR, 1 + TLV.getNbBytes( krbErrorLength )
                + krbErrorLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            log.debug( "KrbError encoding : {}", Strings.dumpBytes( buffer.array() ) );
            log.debug( "KrbError initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "\nKRB-ERROR : {\n" );
        sb.append( "    pvno: " ).append( getProtocolVersionNumber() ).append( '\n' );
        sb.append( "    msgType: " ).append( getMessageType() ).append( '\n' );

        if ( cTime != null )
        {
            sb.append( "    cTime: " ).append( cTime ).append( '\n' );
        }

        if ( cusec != null )
        {
            sb.append( "    cusec: " ).append( cusec ).append( '\n' );
        }

        sb.append( "    sTime: " ).append( sTime ).append( '\n' );
        sb.append( "    susec: " ).append( susec ).append( '\n' );
        sb.append( "    errorCode: " ).append( errorCode ).append( '\n' );

        if ( cRealm != null )
        {
            sb.append( "    cRealm: " ).append( cRealm ).append( '\n' );
        }

        if ( cName != null )
        {
            sb.append( "    cName: " ).append( cName ).append( '\n' );
        }

        sb.append( "    realm: " ).append( realm ).append( '\n' );

        sb.append( "    sName: " ).append( sName ).append( '\n' );

        if ( eText != null )
        {
            sb.append( "    eText: " ).append( eText ).append( '\n' );
        }

        if ( eData != null )
        {
            sb.append( "    eData: " ).append( Strings.dumpBytes( eData ) ).append( '\n' );
        }

        sb.append( "}\n" );

        return sb.toString();
    }
}
