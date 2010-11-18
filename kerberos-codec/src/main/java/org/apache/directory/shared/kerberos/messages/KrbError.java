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

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.kerberos.KerberosConstants;
import org.apache.directory.shared.kerberos.KerberosMessageType;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class representing KrbError message
 * 
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
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KrbError extends AbstractAsn1Object
{

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( KrbError.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** the kerberos version number, default is 5 */
    private int pvno = KerberosMessage.PVNO;

    /** the kerberos message type */
    private KerberosMessageType msgType = KerberosMessageType.KRB_ERROR; // default value

    /** the current time of client */
    private KerberosTime cTime;

    /** microseconds of the client's current time */
    private int cusec;

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

    private transient int pvnoLen;
    private transient int msgTypeLen;
    private transient int cTimeLen;
    private transient int cusecLen;
    private transient int sTimeLen;
    private transient int susecLen;
    private transient int errorCodeLen;
    private transient int cRealmLen;
    private transient int cNameLen;
    private transient int realmLen;
    private transient int sNameLen;
    private transient int eTextLen;
    private transient int eDataLen;
    private transient int krbErrorSeqLen;


    @Override
    public int computeLength()
    {
        pvnoLen = Value.getNbBytes( pvno );
        pvnoLen = 1 + TLV.getNbBytes( pvnoLen ) + pvnoLen;
        krbErrorSeqLen = pvnoLen;

        msgTypeLen = Value.getNbBytes( msgType.getValue() );
        msgTypeLen = 1 + TLV.getNbBytes( msgTypeLen ) + msgTypeLen;
        krbErrorSeqLen += msgTypeLen;

        if ( cTime != null )
        {
            cTimeLen = cTime.getBytes().length;
            cTimeLen = 1 + TLV.getNbBytes( cTimeLen ) + cTimeLen;
            krbErrorSeqLen += cTimeLen;
        }

        if ( cusec > 0 )
        {
            cusecLen = Value.getNbBytes( cusec );
            cusecLen = 1 + TLV.getNbBytes( cusecLen ) + cusecLen;
            krbErrorSeqLen += cusecLen;
        }

        sTimeLen = sTime.getBytes().length;
        sTimeLen = 1 + TLV.getNbBytes( sTimeLen ) + sTimeLen;
        krbErrorSeqLen += sTimeLen;

        susecLen = Value.getNbBytes( susec );
        susecLen = 1 + TLV.getNbBytes( susecLen ) + susecLen;
        krbErrorSeqLen += susecLen;

        errorCodeLen = Value.getNbBytes( errorCode.getOrdinal() );
        errorCodeLen = 1 + TLV.getNbBytes( errorCodeLen ) + errorCodeLen;
        krbErrorSeqLen += errorCodeLen;

        if ( cRealm != null )
        {
            cRealmLen = StringTools.getBytesUtf8( cRealm ).length;
            cRealmLen = 1 + TLV.getNbBytes( cRealmLen ) + cRealmLen;
            krbErrorSeqLen += cRealmLen;
        }

        if ( cName != null )
        {
            cNameLen = cName.computeLength();
            krbErrorSeqLen += cNameLen;
        }

        realmLen = StringTools.getBytesUtf8( realm ).length;
        realmLen = 1 + TLV.getNbBytes( realmLen ) + realmLen;
        krbErrorSeqLen += realmLen;

        sNameLen = sName.computeLength();
        krbErrorSeqLen += sNameLen;

        if ( eText != null )
        {
            eTextLen = StringTools.getBytesUtf8( eText ).length;
            eTextLen = 1 + TLV.getNbBytes( eTextLen ) + eTextLen;
            krbErrorSeqLen += eTextLen;
        }

        if ( eData != null )
        {
            eDataLen = 1 + TLV.getNbBytes( eData.length ) + eData.length;
            krbErrorSeqLen += eDataLen;
        }

        return 1 + TLV.getNbBytes( krbErrorSeqLen ) + krbErrorSeqLen;
    }


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
            buffer.put( TLV.getBytes( krbErrorSeqLen ) );

            //pvno
            buffer.put( ( byte ) KerberosConstants.KRB_ERR_PVNO_TAG );
            buffer.put( TLV.getBytes( pvnoLen ) );
            Value.encode( buffer, pvno );

            //msg-type
            buffer.put( ( byte ) KerberosConstants.KRB_ERR_MSGTYPE_TAG );
            buffer.put( TLV.getBytes( msgTypeLen ) );
            Value.encode( buffer, msgType.getValue() );

            //ctime
            if ( cTimeLen > 0 )
            {
                buffer.put( ( byte ) KerberosConstants.KRB_ERR_CTIME_TAG );
                buffer.put( TLV.getBytes( cTimeLen ) );
                Value.encode( buffer, cTime.getBytes() );
            }

            //cusec
            if ( cusec > 0 )
            {
                buffer.put( ( byte ) KerberosConstants.KRB_ERR_CUSEC_TAG );
                buffer.put( TLV.getBytes( cusecLen ) );
                Value.encode( buffer, cusec );
            }

            //stime
            buffer.put( ( byte ) KerberosConstants.KRB_ERR_STIME_TAG );
            buffer.put( TLV.getBytes( sTimeLen ) );
            Value.encode( buffer, sTime.getBytes() );

            //susec
            buffer.put( ( byte ) KerberosConstants.KRB_ERR_SUSEC_TAG );
            buffer.put( TLV.getBytes( susecLen ) );
            Value.encode( buffer, susec );

            //error-code
            buffer.put( ( byte ) KerberosConstants.KRB_ERR_ERROR_CODE_TAG );
            buffer.put( TLV.getBytes( errorCodeLen ) );
            Value.encode( buffer, errorCode.getOrdinal() );

            //crealm
            if ( cRealmLen > 0 )
            {
                buffer.put( ( byte ) KerberosConstants.KRB_ERR_CREALM_TAG );
                buffer.put( TLV.getBytes( cRealmLen ) );
                Value.encode( buffer, cRealm );
            }

            //cname
            if ( cNameLen > 0 )
            {
                buffer.put( ( byte ) KerberosConstants.KRB_ERR_CNAME_TAG );
                buffer.put( TLV.getBytes( cNameLen ) );
                cName.encode( buffer );
            }

            //realm
            buffer.put( ( byte ) KerberosConstants.KRB_ERR_REALM_TAG );
            buffer.put( TLV.getBytes( realmLen ) );
            Value.encode( buffer, realm );

            //sname
            buffer.put( ( byte ) KerberosConstants.KRB_ERR_SNAME_TAG );
            buffer.put( TLV.getBytes( sNameLen ) );
            sName.encode( buffer );

            //etext
            if ( eTextLen > 0 )
            {
                buffer.put( ( byte ) KerberosConstants.KRB_ERR_ETEXT_TAG );
                buffer.put( TLV.getBytes( eTextLen ) );
                Value.encode( buffer, eText );
            }

            //edata
            if ( eDataLen > 0 )
            {
                buffer.put( ( byte ) KerberosConstants.KRB_ERR_EDATA_TAG );
                buffer.put( TLV.getBytes( eDataLen ) );
                Value.encode( buffer, eData );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_734_CANNOT_ENCODE_KRBERROR, 1 + TLV.getNbBytes( krbErrorSeqLen )
                + krbErrorSeqLen, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            log.debug( "KrbError encoding : {}", StringTools.dumpBytes( buffer.array() ) );
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

        sb.append( "KrbError : {\n" );
        sb.append( "    pvno: " ).append( pvno ).append( '\n' );
        sb.append( "    msgType: " ).append( msgType ).append( '\n' );

        if ( cTime != null )
        {
            sb.append( "    cTime: " ).append( cTime ).append( '\n' );
        }

        if ( cusec > 0 )
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
            sb.append( "    eData: " ).append( StringTools.dumpBytes( eData ) ).append( '\n' );
        }

        sb.append( "}\n" );

        return sb.toString();
    }


    /**
     * @return the pvno
     */
    public int getPvno()
    {
        return pvno;
    }


    /**
     * @param pvno the pvno to set
     */
    public void setPvno( int pvno )
    {
        this.pvno = pvno;
    }


    /**
     * @return the msgType
     */
    public KerberosMessageType getMsgType()
    {
        return msgType;
    }


    /**
     * @param msgType the msgType to set
     */
    public void setMsgType( KerberosMessageType msgType )
    {
        this.msgType = msgType;
    }


    /**
     * @return the cTime
     */
    public KerberosTime getcTime()
    {
        return cTime;
    }


    /**
     * @param cTime the cTime to set
     */
    public void setcTime( KerberosTime cTime )
    {
        this.cTime = cTime;
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
     * @return the sTime
     */
    public KerberosTime getsTime()
    {
        return sTime;
    }


    /**
     * @param sTime the sTime to set
     */
    public void setsTime( KerberosTime sTime )
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
    public String getcRealm()
    {
        return cRealm;
    }


    /**
     * @param cRealm the cRealm to set
     */
    public void setcRealm( String cRealm )
    {
        this.cRealm = cRealm;
    }


    /**
     * @return the cName
     */
    public PrincipalName getcName()
    {
        return cName;
    }


    /**
     * @param cName the cName to set
     */
    public void setcName( PrincipalName cName )
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
     * @return the eText
     */
    public String geteText()
    {
        return eText;
    }


    /**
     * @param eText the eText to set
     */
    public void seteText( String eText )
    {
        this.eText = eText;
    }


    /**
     * @return the eData
     */
    public byte[] geteData()
    {
        return eData;
    }


    /**
     * @param eData the eData to set
     */
    public void seteData( byte[] eData )
    {
        this.eData = eData;
    }
}
