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
import java.text.ParseException;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.KerberosUtils;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalName;
import org.apache.directory.server.kerberos.shared.messages.value.types.KerberosErrorType;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The KRB-ERROR message. The ASN.1 grammar is the following :
 * 
 * KRB-ERROR       ::= [APPLICATION 30] SEQUENCE {
 *       pvno            [0] INTEGER (5),
 *       msg-type        [1] INTEGER (30),
 *       ctime           [2] KerberosTime OPTIONAL,
 *       cusec           [3] Microseconds OPTIONAL,
 *       stime           [4] KerberosTime,
 *       susec           [5] Microseconds,
 *       error-code      [6] Int32,
 *       crealm          [7] Realm OPTIONAL,
 *       cname           [8] PrincipalName OPTIONAL,
 *       realm           [9] Realm -- service realm --,
 *       sname           [10] PrincipalName -- service name --,
 *       e-text          [11] KerberosString OPTIONAL,
 *       e-data          [12] OCTET STRING OPTIONAL
 * }
 * 
 * pvno and msg-type are inherited from KerberosMessage
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public class KerberosError extends KerberosMessage
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( KerberosError.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();
    
    /** The client time */
    private KerberosTime cTime; //optional
    
    /** The client microSecond */
    private int cusec; //optional : from 0 to 999. -1 means unknown
    
    /** The server time */
    private KerberosTime sTime;
    
    /** The server microseconds */
    private int susec;
    
    /** The error code */
    private KerberosErrorType errorCode;
    
    /** The client principal */    
    private PrincipalName cName; //optional
    
    /** The server principal */
    private PrincipalName sName;
    
    /** Explanatory text */
    private String explanatoryText; //optional
    private byte[] explanatoryTextBytes; //optional

    /** Explanatory data */
    private byte[] explanatoryData; //optional
    
    /** The server realm*/ 
    private String realm;
    private byte[] realmBytes;
    
    /** The client realm */
    private String cRealm;
    private byte[] cRealmBytes;

    // Storage for computed lengths
    private transient int cTimeTagLength = 0; // optionnal
    private transient int cTimeLength = 0; // optionnal
    
    private transient int cusecTagLength = 0; // optionnal
    private transient int cusecLength = 0; // optionnal
    
    private transient int sTimeTagLength;
    private transient int sTimeLength;
    
    private transient int susecTagLength;

    private transient int errorCodeTagLength;
    private transient int errorCodeLength;
    
    private transient int cRealmTagLength = 0; // optionnal
    private transient int cRealmLength = 0; // optionnal
    
    private transient int cNameTagLength = 0; // optionnal
    
    private transient int realmTagLength;
    private transient int realmLength;
    
    private transient int sNameTagLength;
    
    private transient int explanatoryTextTagLength;
    private transient int explanatoryTextLength;
    
    private transient int explanatoryDataTagLength;
    private transient int explanatoryDataLength;
    
    private transient int kerberosErrorSeqLength;
    private transient int kerberosErrorApplLength;

    /**
     * Creates a new instance of ErrorMessage.
     */
    public KerberosError()
    {
        super( MessageType.KRB_ERROR );

        // Nullify optionnal data
        cTime = null;
        cusec = KerberosUtils.NULL;
        cRealm = null;
        cName = null;
        explanatoryText = null;
        explanatoryData = null;
    }

    /**
     * Creates a new instance of ErrorMessage.
     *
     * @param clientTime
     * @param clientMicroSecond
     * @param serverTime
     * @param serverMicroSecond
     * @param errorCode
     * @param cname
     * @param serverPrincipal
     * @param explanatoryText
     * @param explanatoryData
     */
    public KerberosError( KerberosTime cTime, int cusec, KerberosTime sTime,
        int susec, KerberosErrorType errorCode, KerberosPrincipal cName, KerberosPrincipal sName,
        String explanatoryText, byte[] explanatoryData ) throws ParseException
    {
        super( MessageType.KRB_ERROR );

        this.cTime = cTime;
        this.cusec = cusec;
        this.sTime = sTime;
        this.susec = susec;
        this.errorCode = errorCode;
        this.cName = new PrincipalName( cName );
        this.cRealm = cName.getRealm();
        this.sName = new PrincipalName( sName );
        this.realm = sName.getRealm();
        this.explanatoryText = explanatoryText;
        this.explanatoryData = explanatoryData;
    }


    /**
     * Returns the client {@link KerberosPrincipal}.
     *
     * @return The client {@link KerberosPrincipal}.
     */
    public PrincipalName getClientPrincipal()
    {
        return cName;
    }

    /**
     * Set the client principal
     * @param name Set the client principal
     */
    public void setClientPrincipal( KerberosPrincipal cName )
    {
        try
        {
            this.cName = new PrincipalName( cName );
        }
        catch ( ParseException pe )
        {
            this.cName = null;
        }
    }

    /**
     * Set the client principal
     * @param name Set the client principal
     */
    public void setClientPrincipal( PrincipalName cName )
    {
        this.cName = cName;
    }


    /**
     * Returns the client {@link KerberosTime}.
     *
     * @return The client {@link KerberosTime}.
     */
    public KerberosTime getClientTime()
    {
        return cTime;
    }

    /**
     * Set the client time
     * @param cTime the client time
     */
    public void setClientTime( KerberosTime cTime )
    {
        this.cTime = cTime;
    }

    /**
     * Returns the client microsecond.
     *
     * @return The client microsecond.
     */
    public int getClientMicroSecond()
    {
        return cusec;
    }

    /**
     * Set the client Microseconds
     * @param cusec the cllient Microseconds
     */
    public void setClientMicroSecond( int cusec )
    {
        this.cusec = cusec;
    }
    

    /**
     * Returns the explanatory data.
     *
     * @return The explanatory data.
     */
    public byte[] getExplanatoryData()
    {
        return explanatoryData;
    }

    /**
     * Set the explanatory data
     * @param explanatoryData The data
     */
    public void setExplanatoryData( byte[] explanatoryData )
    {
        this.explanatoryData = explanatoryData;
    }

    /**
     * Returns the error code.
     *
     * @return The error code.
     */
    public KerberosErrorType getErrorCode()
    {
        return errorCode;
    }

    /**
     * Set the error code
     * @param errorCode The error code
     */
    public void setErrorCode( KerberosErrorType errorCode )
    {
        this.errorCode = errorCode;
    }

    /**
     * Returns the explanatory text.
     *
     * @return The explanatory text.
     */
    public String getExplanatoryText()
    {
        return explanatoryText;
    }

    /**
     * Set the explanatory text
     * @param explanatoryText
     */
    public void setExplanatoryText( String explanatoryText )
    {
        this.explanatoryText = explanatoryText;
    }

    /**
     * Returns the server {@link KerberosPrincipal}.
     *
     * @return The server {@link KerberosPrincipal}.
     */
    public PrincipalName getServerPrincipal()
    {
        return sName;
    }

    /**
     * Set the server principal
     * @param sName The server principal
     */
    public void setServerPrincipal( KerberosPrincipal sName )
    {
        try
        {
            this.sName = new PrincipalName( sName );
        }
        catch ( ParseException pe )
        {
            this.sName = null;
        }
    }
    
    /**
     * Set the server principal
     * @param sName The server principal
     */
    public void setServerPrincipal( PrincipalName sName )
    {
        this.sName = sName;
    }

    /**
     * Returns the server {@link KerberosTime}.
     *
     * @return The server {@link KerberosTime}.
     */
    public KerberosTime getServerTime()
    {
        return sTime;
    }

    /**
     * Set the server time
     * @param time The server time
     */
    public void setServerTime( KerberosTime sTime )
    {
        this.sTime = sTime;
    }

    /**
     * Returns the server microsecond.
     *
     * @return The server microsecond.
     */
    public int getServerMicroSecond()
    {
        return susec;
    }

    /**
     * Get the microsecond part of the server's
     * timestamp
     * @return the microsecond part of the server's
     * timestamp
     */
    public int getServerMicroseconds()
    {
        return susec;
    }

    /**
     * Set the microsecond part of the server's
     * timestamp
     * @susec the microsecond part of the server's
     * timestamp
     */
    public void setServerMicroseconds( int susec )
    {
        this.susec = susec;
    }

    /**
     * Get the client realm
     * @return the client realm
     */
    public String getClientRealm()
    {
        return cRealm;
    }

    /**
     * Set the client realm
     * @param realm The client realm
     */
    public void setClientRealm( String realm )
    {
        cRealm = realm;
    }

    /**
     * Get the server realm
     * @return the server realm
     */
    public String getServerRealm()
    {
        return realm;
    }

    /**
     * Set the server realm
     * @param realm The server realm
     */
    public void setServerRealm( String realm )
    {
        this.realm = realm;
    }
    
    /**
     * Return the length of a kerberos error message .
     * 
     * 0x7E L1
     *  |
     *  +--> 0x30 L2
     *        |
     *        +--> 0xA0 0x03
     *        |     |
     *        |     +--> 0x02 0x01 pvno (integer)
     *        |
     *        +--> 0xA1 0x03
     *        |     |
     *        |     +--> 0x02 0x01 messageType (integer)
     *        |
     *       [+--> 0xA2 0x11
     *        |     |
     *        |     +--> 0x18 0x0F ctime (KerberosTime, optionnal)]
     *        |
     *       [+--> 0xA3 L3
     *        |     | 
     *        |     +--> 0x02 L3-1 cusec (integer, optionnal)]
     *        |
     *        +--> 0xA4 L4 
     *        |     | 
     *        |     +--> 0x18 L4-1 stime (KerberosTime)
     *        |
     *        +--> 0xA5 L5
     *        |     | 
     *        |     +--> 0x02 L5-1 susec (integer)
     *        |
     *        +--> 0xA6 L6
     *        |     | 
     *        |     +--> 0x02 L6-1 error-code (integer)
     *        |
     *       [+--> 0xA7 L7
     *        |     | 
     *        |     +--> 0x1B L7-1 crealm (String, optionnal)]
     *        |
     *       [+--> 0xA8 L8
     *        |     | 
     *        |     +--> 0x1B L8-1 cname (String, optionnal)]
     *        |
     *        +--> 0xA9 L9
     *        |     | 
     *        |     +--> 0x1B L9-1 realm (String)
     *        |
     *        +--> 0xAA L10
     *        |     | 
     *        |     +--> 0x1B L10-1 sname (String)
     *        |
     *       [+--> 0xAB L11
     *        |     | 
     *        |     +--> 0x1B L11-1 e-text (String, optionnal)]
     *        |
     *       [+--> 0xAC L12
     *              | 
     *              +--> 0x04 L12-1 e-data (OCTET-STRING, optionnal)]
     */
    public int computeLength()
    {
        // First compute the KerberosMessage length
        kerberosErrorSeqLength = super.computeLength();
        
        // The clientTime (optionnal)
        if ( cTime != null )
        {
            // The time length
            cTimeLength = 15; 
            cTimeTagLength = 1 + 1 + cTimeLength; 
            
            kerberosErrorSeqLength += 
                1 + TLV.getNbBytes( cTimeTagLength ) + cTimeTagLength;
        }
        
        if ( cusec != KerberosUtils.NULL )
        {
            // The cusec length
            cusecLength = Value.getNbBytes( cusec );
            cusecTagLength = 1 + TLV.getNbBytes( cusecLength ) + cusecLength;
            
            kerberosErrorSeqLength += 
                1 + TLV.getNbBytes( cusecTagLength ) + cusecTagLength;
        }
        
        // The serverTime length
        sTimeLength = 15; 
        sTimeTagLength = 1 + 1 + sTimeLength; 
        
        kerberosErrorSeqLength += 
            1 + TLV.getNbBytes( sTimeTagLength ) + sTimeTagLength;

        // The susec length
        int susecLength = Value.getNbBytes( susec );
        susecTagLength = 1 + TLV.getNbBytes( susecLength ) + susecLength;
        
        kerberosErrorSeqLength += 
            1 + TLV.getNbBytes( susecTagLength ) + susecTagLength;
        
        // The error-code length
        errorCodeLength = Value.getNbBytes( errorCode.getOrdinal() );
        errorCodeTagLength = 1 + TLV.getNbBytes( errorCodeLength ) + errorCodeLength;
        
        kerberosErrorSeqLength += 
            1 + TLV.getNbBytes( errorCodeTagLength ) + errorCodeTagLength;
        
        // The client realm length
        if ( cRealm != null)
        {
            // The crealm length
            cRealmBytes = StringTools.getBytesUtf8( cRealm );
            cRealmLength = cRealmBytes.length; 
            cRealmTagLength = 1 + TLV.getNbBytes( cRealmLength ) + cRealmLength;
            
            kerberosErrorSeqLength += 
                1 + TLV.getNbBytes( cRealmTagLength ) + cRealmTagLength;
        }
        
        // The client principalName, if any
        if ( cName != null )
        {
            // The cname length
            cNameTagLength = cName.computeLength(); 
            
            kerberosErrorSeqLength += 
                1 + TLV.getNbBytes( cNameTagLength ) + cNameTagLength;
        }
        
        // The realm length
        realmBytes = StringTools.getBytesUtf8( realm );
        realmLength = realmBytes.length; 
        realmTagLength = 1 + TLV.getNbBytes( realmLength ) + realmLength;
        
        kerberosErrorSeqLength += 
            1 + TLV.getNbBytes( realmTagLength ) + realmTagLength;

        // The sname length
        sNameTagLength = sName.computeLength();
        
        kerberosErrorSeqLength += 
            1 + TLV.getNbBytes( sNameTagLength ) + sNameTagLength;

        // The explanatory length, if any
        if ( explanatoryText != null )
        {
            explanatoryTextBytes = StringTools.getBytesUtf8( explanatoryText );
            explanatoryTextLength = explanatoryTextBytes.length; 
            explanatoryTextTagLength = 1 + TLV.getNbBytes( explanatoryTextLength ) + explanatoryTextLength;
            
            kerberosErrorSeqLength += 
                1 + TLV.getNbBytes( explanatoryTextTagLength ) + explanatoryTextTagLength;
        }
        
        // The explanatoryData length, if any
        if ( explanatoryData != null )
        {
            explanatoryDataLength = explanatoryData.length; 
            explanatoryDataTagLength = 1 + TLV.getNbBytes( explanatoryDataLength ) + explanatoryDataLength;
            
            kerberosErrorSeqLength += 
                1 + TLV.getNbBytes( explanatoryDataTagLength ) + explanatoryDataTagLength;
        }

        kerberosErrorApplLength = 1 + TLV.getNbBytes( kerberosErrorSeqLength ) + kerberosErrorSeqLength;
        return 1 + TLV.getNbBytes( kerberosErrorApplLength ) + kerberosErrorApplLength;
    }
    
    /**
     * Encode the KerberosError message to a PDU. 
     * 
     * KRB-ERROR :
     * 
     * 0x7E LL
     *   0x30 LL
     *     0xA0 LL pvno 
     *     0xA1 LL msg-type
     *    [0xA2 LL ctime]
     *    [0xA3 LL cusec]
     *     0xA4 LL stime
     *     0xA5 LL susec
     *     0xA6 LL error-code
     *    [0xA7 LL crealm]
     *    [0xA8 LL cname]
     *     0xA9 LL realm
     *     0xAA LL sname
     *    [0xAB LL e-text]
     *    [0xAC LL e-data]
     * 
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            buffer = ByteBuffer.allocate( computeLength() );
        }

        try
        {
            // The KerberosError APPLICATION Tag
            buffer.put( (byte)0x7E );
            buffer.put( TLV.getBytes( kerberosErrorApplLength ) );

            // The KerberosError SEQUENCE Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( kerberosErrorSeqLength ) );

            // The pvno Tag and value
            super.encode(  buffer );

            
            // The clientTime Tag and value, if any
            if ( cTime != null )
            {
                buffer.put( ( byte )0xA2 );
                buffer.put( TLV.getBytes( cTimeTagLength ) );
                buffer.put( UniversalTag.GENERALIZED_TIME_TAG );
                buffer.put( TLV.getBytes( cTimeLength ) );
                buffer.put( StringTools.getBytesUtf8( cTime.toString() ) );
            }

            // The cusec Tag and value, if any
            if ( cusec != KerberosUtils.NULL )
            {
                buffer.put( ( byte )0xA3 );
                buffer.put( TLV.getBytes( cusecTagLength ) );
                Value.encode( buffer, cusec );
            }
            
            // The serverTime Tag and value, if any
            if ( sTime != null )
            {
                buffer.put( ( byte )0xA4 );
                buffer.put( TLV.getBytes( sTimeTagLength ) );
                buffer.put( UniversalTag.GENERALIZED_TIME_TAG );
                buffer.put( TLV.getBytes( sTimeLength ) );
                buffer.put( StringTools.getBytesUtf8( sTime.toString() ) );
            }

            // Server millisecond encoding
            buffer.put( ( byte )0xA5 );
            buffer.put( TLV.getBytes( susecTagLength ) );
            Value.encode( buffer, susec );
            
            // Error code encoding
            buffer.put( ( byte )0xA6 );
            buffer.put( TLV.getBytes( errorCodeTagLength ) );
            Value.encode( buffer, errorCode.getOrdinal() );
            
            // Client Realm encoding, if any
            if ( cRealm != null )
            {
                buffer.put( ( byte )0xA7 );
                buffer.put( TLV.getBytes( cRealmTagLength ) );
                buffer.put( UniversalTag.GENERALIZED_STRING_TAG );
                buffer.put( TLV.getBytes( cRealmLength ) );
                buffer.put( cRealmBytes );
            }
            
            // ClientPrincipal encoding, if any
            if ( cName != null )
            {
                buffer.put( ( byte )0xA8 );
                buffer.put( TLV.getBytes( cNameTagLength ) );
                cName.encode( buffer );
            }

            // ServerRealm encoding
            buffer.put( ( byte )0xA9 );
            buffer.put( TLV.getBytes( realmTagLength ) );
            buffer.put( UniversalTag.GENERALIZED_STRING_TAG );
            buffer.put( TLV.getBytes( realmLength ) );
            buffer.put( realmBytes );

            // Server principal encoding
            buffer.put( ( byte )0xAA );
            buffer.put( TLV.getBytes( sNameTagLength ) );
            sName.encode( buffer );
            
            // Explanatory Text encoding if any
            if ( explanatoryText != null )
            {
                buffer.put( ( byte )0xAB );
                buffer.put( TLV.getBytes( explanatoryTextTagLength ) );
                buffer.put( UniversalTag.GENERALIZED_STRING_TAG );
                buffer.put( TLV.getBytes( explanatoryTextLength ) );
                buffer.put( explanatoryTextBytes );
            }

            // Explanatory Data encoding if any
            if ( explanatoryData != null )
            {
                buffer.put( ( byte )0xAC );
                buffer.put( TLV.getBytes( explanatoryDataTagLength ) );
                Value.encode( buffer, explanatoryData );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( "Cannot encode the KRB-ERROR object, the PDU size is {} when only {} bytes has been allocated", 1
                + TLV.getNbBytes( kerberosErrorApplLength ) + kerberosErrorApplLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "KRB-ERROR encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "KRB-ERROR initial value : {}", toString() );
        }

        return buffer;
    }
}
