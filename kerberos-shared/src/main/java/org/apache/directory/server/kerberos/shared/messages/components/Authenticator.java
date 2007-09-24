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
package org.apache.directory.server.kerberos.shared.messages.components;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.text.ParseException;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.KerberosUtils;
import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.server.kerberos.shared.messages.value.AuthorizationData;
import org.apache.directory.server.kerberos.shared.messages.value.Checksum;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalName;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The Authenticator class
 * 
 * The ASN.1 grammar is the following :
 * 
 * -- Unencrypted authenticator
 * Authenticator   ::= [APPLICATION 2] SEQUENCE  {
 *        authenticator-vno       [0] INTEGER (5),
 *        crealm                  [1] Realm,
 *        cname                   [2] PrincipalName,
 *        cksum                   [3] Checksum OPTIONAL,
 *        cusec                   [4] Microseconds,
 *        ctime                   [5] KerberosTime,
 *        subkey                  [6] EncryptionKey OPTIONAL,
 *        seq-number              [7] UInt32 OPTIONAL,
 *        authorization-data      [8] AuthorizationData OPTIONAL
 * }
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Authenticator extends AbstractAsn1Object implements Encodable
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( Authenticator.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();
    
    /**
     * Constant for the authenticator version number.
     */
    public static final int AUTHENTICATOR_VNO = 5;

    /** the version number for the format of the authenticator */
    private int authenticatorVno;
    
    /** The client PrincipalName */
    private PrincipalName cName;
    
    /** The client KerberosPrincipal */
    private KerberosPrincipal clientPrincipal;
    
    /** The client realm */
    private String cRealm;
    
    /** The client realm as a byte array */
    private byte[] cRealmBytes;
    
    /** checksum of the the application data */
    private Checksum cksum;
    
    /** the microsecond part of the client's timestamp */
    private int cusec;
    
    /** the current time on the client's host */
    private KerberosTime cTime;
    
    /** the client's choice for an encryption key */
    private EncryptionKey subKey;
    
    /** the initial sequence number to be used by the KRB_PRIV or KRB_SAFE messages */
    private int seqNumber;
    
    /** Authorization data */
    private AuthorizationData authorizationData;

    // Storage for computed lengths
    private transient int authenticatorAppLength;
    private transient int authenticatorSeqLength;
    
    private transient int authenticatorVnoTagLength;
    
    private transient int cRealmTagLength;
    
    private transient int cNameTagLength;
    
    private transient int cksumTagLength;
    
    private transient int cusecTagLength;
    
    private transient int cTimeTagLength;
    private transient int cTimeLength;
    
    private transient int subKeyTagLength;
    
    private transient int seqNumberTagLength;
    
    private transient int authorizationDataTagLength;

    /**
     * Creates a new instance of Authenticator.
     */
    public Authenticator()
    {
        cksum = null;          // optional
        subKey = null;            // optional
        authorizationData = null; // optional
        seqNumber = KerberosUtils.NULL; // optional
    }

    /**
     * Returns the client {@link PrincipalName}.
     *
     * @return The client {@link PrincipalName}.
     */
    public PrincipalName getClientPrincipalName()
    {
        return cName;
    }

    
    /**
     * Returns the client {@link KerberosPrincipal}.
     *
     * @return The client {@link KerberosPrincipal}.
     */
    public KerberosPrincipal getClientPrincipal()
    {
        return clientPrincipal;
    }


    /**
     * Sets the client {@link PrincipalName}.
     *
     * @param clientPrincipal
     */
    public void setClientPrincipalName( PrincipalName cName )
    {
        this.cName = cName;
    }

    
    /**
     * Sets the client {@link KerberosPrincipal}.
     *
     * @param clientPrincipal
     */
    public void setClientPrincipal( KerberosPrincipal clientPrincipal )
    {
        this.clientPrincipal = clientPrincipal;
        
        try
        {
            this.cName = new PrincipalName( clientPrincipal.getName(), clientPrincipal.getNameType() );
        }
        catch ( ParseException pe )
        {
            this.cName = null;
        }
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
     * Sets the client {@link KerberosTime}.
     *
     * @param time the client {@link KerberosTime}.
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
     * Sets the client microsecond.
     *
     * @param microSecond the client microsecond.
     */
    public void setClientMicroSecond( int cusec )
    {
        this.cusec = cusec;
    }


    /**
     * Returns the {@link AuthorizationData}.
     *
     * @return The {@link AuthorizationData}.
     */
    public AuthorizationData getAuthorizationData()
    {
        return authorizationData;
    }

    
    /**
     * Sets the {@link AuthorizationData}.
     *
     * @param data the {@link AuthorizationData}.
     */
    public void setAuthorizationData( AuthorizationData authorizationData )
    {
        this.authorizationData = authorizationData;
    }

    
    /**
     * Returns the {@link Checksum}.
     *
     * @return The {@link Checksum}.
     */
    public Checksum getChecksum()
    {
        return cksum;
    }

    
    /**
     * Sets the {@link Checksum}.
     *
     * @param checksum the {@link Checksum}.
     */
    public void setChecksum( Checksum cksum )
    {
        this.cksum = cksum;
    }

    
    /**
     * Returns the sequence number.
     *
     * @return The sequence number.
     */
    public int getSequenceNumber()
    {
        return seqNumber;
    }

    
    /**
     * Sets the sequence number.
     *
     * @param seqNumber the sequence number
     */
    public void setSequenceNumber( int seqNumber )
    {
        this.seqNumber = seqNumber;
    }

    
    /**
     * Returns the sub-session key.
     *
     * @return The sub-session key.
     */
    public EncryptionKey getSubSessionKey()
    {
        return subKey;
    }

    
    /**
     * Sets the sub-session {@link EncryptionKey}.
     *
     * @param subKey the sub-session
     */
    public void setSubSessionKey( EncryptionKey subKey )
    {
        this.subKey = subKey;
    }

    
    /**
     * Returns the version number of the {@link Authenticator}.
     *
     * @return The version number of the {@link Authenticator}.
     */
    public int getVersionNumber()
    {
        return authenticatorVno;
    }
    
    
    /**
     * Sets the version number.
     *
     * @param versionNumber The version number
     */
    public void setVersionNumber( int authenticatorVno )
    {
        this.authenticatorVno = authenticatorVno;
    }


    /**
     * @return The client realm
     */
    public String getClientRealm()
    {
        return cRealm;
    }

    /**
     * Sets the client realm.
     *
     * @param realm the client realm.
     */
    public void setClientRealm( String cRealm )
    {
        this.cRealm = cRealm;
    }
    
    /**
     * Compute the Authenticator length
     * 
     * Authenticator :
     * 
     * 0x62 L1 Authenticator Tag (Application 2)
     *  |
     *  +-->  0x30 L2 Authenticator sequence
     *         |
     *         +--> 0xA0 L2 authenticator-vno tag
     *         |     |
     *         |     +--> 0x02 L2-1 authenticator-vno (int)
     *         |
     *         +--> 0xA1 L3 crealm tag
     *         |     |
     *         |     +--> 0x1B L3-1 crealm (crealm)
     *         |
     *         +--> 0xA2 L4 cname tag
     *         |     |
     *         |     +--> 0x30 L4-1 cname (PrincipalName)
     *         |
     *         +--> [0xA3 L5 cksum tag
     *         |     |
     *         |     +--> 0x30 L5-1 cksum (Checksum)] (optional)
     *         |
     *         +--> 0xA4 L6 cusec tag
     *         |     |
     *         |     +--> 0x02 L6-1 cusec (int)
     *         |
     *         +--> 0xA5 0x11 ctime tag
     *         |     |
     *         |     +--> 0x18 0x0F ctime (KerberosTime)
     *         |
     *         +--> [0xA6 L7 subkey tag
     *         |     |
     *         |     +--> 0x30 L7-1 subkey (EncryptionKey)] (optional)
     *         |
     *         +--> [0xA7 L8 seqNumber tag
     *         |     |
     *         |     +--> 0x02 L8-1 seqNulber (int > 0)] (optional)
     *         |
     *         +--> [0xA8 L9 authorization-data tag
     *               |
     *               +--> 0x30 L9-1 authorization-data (AuthorizationData)] (optional)
     */
    public int computeLength()
    {
        authenticatorAppLength = 0;
        authenticatorSeqLength = 0;

        // Compute the authenticator-vno length
        int authenticatorVnoLength = Value.getNbBytes( authenticatorVno );
        authenticatorVnoTagLength = 1 + TLV.getNbBytes( authenticatorVnoLength ) + authenticatorVnoLength;
        
        authenticatorSeqLength += 1 + TLV.getNbBytes( authenticatorVnoTagLength ) + authenticatorVnoTagLength;

        // Compute the client Realm length
        cRealmBytes = StringTools.getBytesUtf8( cRealm );
        cRealmTagLength = 1 + TLV.getNbBytes( cRealmBytes.length ) + cRealmBytes.length;
        authenticatorSeqLength += 1 + TLV.getNbBytes( cRealmTagLength ) + cRealmTagLength;
        
        // Compute the clientPrincipalName length
        cNameTagLength = cName.computeLength();
        authenticatorSeqLength += 1 + TLV.getNbBytes( cNameTagLength ) + cNameTagLength;
        
        // Compute the cksum length, if any
        if ( cksum != null )
        {
            cksumTagLength = cksum.computeLength();
            authenticatorSeqLength += 1 + TLV.getNbBytes( cksumTagLength ) + cksumTagLength;
        }
        
        // Compute the cusec length
        int cusecLength = Value.getNbBytes( cusec );
        cusecTagLength = 1 + TLV.getNbBytes( cusecLength ) + cusecLength;
        authenticatorSeqLength += 1 + TLV.getNbBytes( cusecTagLength ) + cusecTagLength;
        
        // Compute the clientTime length
        cTimeLength = 15; 
        cTimeTagLength = 1 + 1 + cTimeLength; 
        
        authenticatorSeqLength += 
            1 + TLV.getNbBytes( cTimeTagLength ) + cTimeTagLength;
        
        // Compute the subkey length, if any
        if ( subKey != null )
        {
            subKeyTagLength = subKey.computeLength();
            authenticatorSeqLength += 1 + TLV.getNbBytes( subKeyTagLength ) + subKeyTagLength;
        }
        
        // Compute the seqNumber length
        int seqNumberLength = Value.getNbBytes( seqNumber );
        seqNumberTagLength = 1 + TLV.getNbBytes( seqNumberLength ) + seqNumberLength;
        authenticatorSeqLength += 1 + TLV.getNbBytes( seqNumberTagLength ) + seqNumberTagLength;
        
        // Compute the authorization-data length, if any
        if ( authorizationData != null )
        {
            authorizationDataTagLength = authorizationData.computeLength();
            authenticatorSeqLength += 1 + TLV.getNbBytes( authorizationDataTagLength ) + authorizationDataTagLength;
        }
        
        // Compute the whole sequence length
        authenticatorAppLength = 1 + TLV.getNbBytes( authenticatorSeqLength ) + authenticatorSeqLength;
        
        // Compute the whole application length
        return 1 + TLV.getNbBytes( authenticatorAppLength ) + authenticatorAppLength;
    }
    
    /**
     * Encode the Authenticator message to a PDU. 
     * 
     * Authenticator :
     * 
     * 0x62 LL
     *   0x30 LL
     *     0xA0 LL 
     *       0x03 LL authenticator-vno (int)
     *     0xA1 LL
     *       0x1B LL crealm (KerberosString)
     *     0xA2 LL
     *       0x30 LL cname (PrincipalName)
     *     [0xA3 LL
     *       0x30 LL cksum (Checksum) (optional)]
     *     0xA4 LL
     *       0x02 LL cusec (int)
     *     0xA5 0x11
     *       0x18 0x0F ctime (KerberosTime)
     *     [0xA6 LL
     *       0x30 LL subkey (EncryptionKey) (optional)]
     *     [0xA7 LL
     *       0x02 LL seqNulber (int) (optional)]
     *     [0xA8 LL
     *       0x30 LL authorization-data (AuthorizationData) (optional)]
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
            // The authenticator APP Tag
            buffer.put( (byte)0x62 );
            buffer.put( TLV.getBytes( authenticatorAppLength ) );

            // The authenticator SEQ Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( authenticatorSeqLength ) );

            // The authenticator-vno encoding, first the tag, then the value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( authenticatorVnoTagLength ) );

            Value.encode( buffer, authenticatorVno );
            
            // The client realm encoding
            buffer.put( (byte)0xA1 );
            buffer.put( TLV.getBytes( cRealmTagLength ) );
            
            buffer.put( UniversalTag.GENERALIZED_STRING_TAG );
            buffer.put( TLV.getBytes( cRealmBytes.length ) );
            buffer.put( cRealmBytes );

            // The clientprincipalName encoding
            buffer.put( (byte)0xA2 );
            buffer.put( TLV.getBytes( cNameTagLength ) );
            cName.encode( buffer );
            
            // The cksum encoding, if any
            if ( cksum != null )
            {
                buffer.put( (byte)0xA3 );
                buffer.put( TLV.getBytes( cksumTagLength ) );
                cksum.encode( buffer );
            }
            
            // Client millisecond encoding
            buffer.put( ( byte )0xA4 );
            buffer.put( TLV.getBytes( cusecTagLength ) );
            Value.encode( buffer, cusec );
            
            // The clientTime Tag and value
            buffer.put( ( byte )0xA5 );
            buffer.put( TLV.getBytes( cTimeTagLength ) );
            buffer.put( UniversalTag.GENERALIZED_TIME_TAG );
            buffer.put( TLV.getBytes( cTimeLength ) );
            buffer.put( StringTools.getBytesUtf8( cTime.toString() ) );
            
            // The subkey encoding, if any
            if ( subKey != null )
            {
                buffer.put( (byte)0xA6 );
                buffer.put( TLV.getBytes( subKeyTagLength ) );
                subKey.encode( buffer );
            }
            
            // The seqNumber encoding, if any
            if ( seqNumber != KerberosUtils.NULL )
            {
                buffer.put( ( byte )0xA7 );
                buffer.put( TLV.getBytes( seqNumberTagLength ) );
                Value.encode( buffer, seqNumber );
            }
            
            // The authorization-data encoding, if any
            if ( authorizationData != null )
            {
                buffer.put( (byte)0xA8 );
                buffer.put( TLV.getBytes( authorizationDataTagLength ) );
                authorizationData.encode( buffer );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error(
                "Cannot encode the Authenticator object, the PDU size is {} when only {} bytes has been allocated", 1
                    + TLV.getNbBytes( authenticatorAppLength ) + authenticatorAppLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "Authenticator encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "Authenticator initial value : {}", toString() );
        }

        return buffer;
    }
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "NYI";
    }
}
