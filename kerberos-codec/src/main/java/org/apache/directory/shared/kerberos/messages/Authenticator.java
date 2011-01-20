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
import org.apache.directory.shared.kerberos.components.AuthorizationData;
import org.apache.directory.shared.kerberos.components.Checksum;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
 * A structure to hold the authenticator data.
 *  It will store the object described by the ASN.1 grammar :
 * <pre>
 * Authenticator   ::= [APPLICATION 2] SEQUENCE  {
 *         authenticator-vno       [0] INTEGER (5),
 *         crealm                  [1] Realm,
 *         cname                   [2] <PrincipalName>,
 *         cksum                   [3] <Checksum> OPTIONAL,
 *         cusec                   [4] Microseconds,
 *         ctime                   [5] KerberosTime,
 *         subkey                  [6] <EncryptionKey> OPTIONAL,
 *         seq-number              [7] UInt32 OPTIONAL,
 *         authorization-data      [8] <AuthorizationData> OPTIONAL
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Authenticator extends KerberosMessage
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( Authenticator.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The authenticator version number */
    private int versionNumber;
    
    /** The client realm */
    private String crealm;
    
    /** The client principalName */
    private PrincipalName cname;
    
    /** The checksum */
    private Checksum cksum;
    
    /** The client microseconds */
    private int cusec;
    
    /** The client time */
    private KerberosTime ctime;
    
    /** The sub-session key */
    private EncryptionKey subKey;

    /** The sequence number */
    private Integer seqNumber;

    /** The authorization Data */
    private AuthorizationData authorizationData;

    // Storage for computed lengths
    private transient int authenticatorVnoLength;
    private transient int crealmLength;
    private transient byte[] crealmBytes;
    private transient int cnameLength;
    private transient int cksumLength;
    private transient int cusecLength;
    private transient int ctimeLength;
    private transient int subkeyLength;
    private transient int seqNumberLength;
    private transient int authorizationDataLength;
    private transient int authenticatorSeqLength;
    private transient int authenticatorLength;
    


    /**
     * Creates a new instance of Authenticator.
     */
    public Authenticator()
    {
        super( KerberosMessageType.AUTHENTICATOR );
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
     * @param authorizationData the authorizationData to set
     */
    public void setAuthorizationData( AuthorizationData authorizationData )
    {
        this.authorizationData = authorizationData;
    }


    /**
     * @return the cksum
     */
    public Checksum getCksum()
    {
        return cksum;
    }


    /**
     * @param cksum the cksum to set
     */
    public void setCksum( Checksum cksum )
    {
        this.cksum = cksum;
    }


    /**
     * @return the cname
     */
    public PrincipalName getCName()
    {
        return cname;
    }


    /**
     * @param cname the cname to set
     */
    public void setCName( PrincipalName cname )
    {
        this.cname = cname;
    }


    /**
     * @return the crealm
     */
    public String getCRealm()
    {
        return crealm;
    }


    /**
     * @param crealm the crealm to set
     */
    public void setCRealm( String crealm )
    {
        this.crealm = crealm;
    }


    /**
     * @return the ctime
     */
    public KerberosTime getCtime()
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
     * @return the seqNumber
     */
    public int getSeqNumber()
    {
        return seqNumber;
    }


    /**
     * @param seqNumber the seqNumber to set
     */
    public void setSeqNumber( int seqNumber )
    {
        this.seqNumber = Integer.valueOf( seqNumber );
    }


    /**
     * @return the subKey
     */
    public EncryptionKey getSubKey()
    {
        return subKey;
    }


    /**
     * @param subKey the subKey to set
     */
    public void setSubKey( EncryptionKey subKey )
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
        return versionNumber;
    }


    /**
     * @param versionNumber the versionNumber to set
     */
    public void setVersionNumber( int versionNumber )
    {
        this.versionNumber = versionNumber;
    }
    
    
    /**
     * Compute the Authenticator length
     * <pre>
     * Authenticator :
     * 
     * 0x62 L1 Authenticator [APPLICATION 2]
     *  |
     *  +--> 0x30 L2 Authenticator SEQUENCE
     *        |
     *        +--> 0xA0 03 authenticator-vno tag
     *        |     |
     *        |     +--> 0x02 0x01 0x05 authenticator-vno (int, 5)
     *        |
     *        +--> 0xA1 L3 crealm tag
     *        |     |
     *        |     +--> 0x1B L3-1 crealm (KerberosString)
     *        |
     *        +--> 0xA2 L4 cname (PrincipalName)
     *        |
     *        +--> 0xA3 L5 cksum (CheckSum)
     *        |
     *        +--> 0xA4 L6 cusec tag
     *        |     |
     *        |     +--> 0x02 L6-1 nnn cusec value (Integer)
     *        |
     *        +--> 0xA5 0x11 ctime tag
     *        |     |
     *        |     +--> 0x18 0x0F ttt ctime (KerberosTime)
     *        |
     *        +--> 0xA6 L7 subkey (EncryptionKey)
     *        |
     *        +--> 0xA7 L8 seq-number tag
     *        |     |
     *        |     +--> 0x02 L8-1 nnn seq-number (Integer)
     *        |
     *        +--> 0xA8 L9 authorization-data (AuthorizationData)
     * </pre>
     */
    @Override
    public int computeLength()
    {
    	reset();
    	
        // Compute the Authenticator version length.
        authenticatorVnoLength = 1 + 1 + Value.getNbBytes( getProtocolVersionNumber() );
        authenticatorSeqLength =  1 + TLV.getNbBytes( authenticatorVnoLength ) + authenticatorVnoLength;

        // Compute the  crealm length.
        crealmBytes = StringTools.getBytesUtf8( crealm );
        crealmLength = 1 + TLV.getNbBytes( crealmBytes.length ) + crealmBytes.length;
        authenticatorSeqLength += 1 + TLV.getNbBytes( crealmLength ) + crealmLength;

        // Compute the cname length
        cnameLength = cname.computeLength();
        authenticatorSeqLength += 1 + TLV.getNbBytes( cnameLength ) + cnameLength;
        
        // Compute the cksum length if any
        if ( cksum != null )
        {
            cksumLength = cksum.computeLength();
            authenticatorSeqLength += 1 + TLV.getNbBytes( cksumLength ) + cksumLength;
        }

        // Compute the cusec length
        cusecLength = 1 + 1 + Value.getNbBytes( cusec );
        authenticatorSeqLength += 1 + TLV.getNbBytes( cusecLength ) + cusecLength;

        // Compute the ctime length
        ctimeLength = 1 + 1 + 0x0F;
        authenticatorSeqLength += 1 + 1 + ctimeLength;

        // Compute the subkey length if any
        if ( subKey != null )
        {
            subkeyLength = subKey.computeLength();
            authenticatorSeqLength += 1 + TLV.getNbBytes( subkeyLength ) + subkeyLength;
        }

        // Compute the seq-number  length if any
        if ( seqNumber != null )
        {
            seqNumberLength = 1 + 1 + Value.getNbBytes( seqNumber );
            authenticatorSeqLength += 1 + TLV.getNbBytes( seqNumberLength ) + seqNumberLength;
        }
        
        // Compute the authorization-data length if any
        if ( authorizationData != null )
        {
            authorizationDataLength = authorizationData.computeLength();
            authenticatorSeqLength += 1 + TLV.getNbBytes( authorizationDataLength ) + authorizationDataLength;
        }

        // compute the global size
        authenticatorLength = 1 + TLV.getNbBytes( authenticatorSeqLength ) + authenticatorSeqLength;
        
        return 1 + TLV.getNbBytes( authenticatorLength ) + authenticatorLength;
    }
    

    /**
     * Encode the Authenticator message to a PDU. 
     * <pre>
     * Authenticator :
     * 
     * 0x62 LL
     *   0x30 LL
     *     0xA0 0x03 
     *       0x02 0x01 0x05 authenticator-vno 
     *     0xA1 LL 
     *       0x1B LL abcd crealm
     *     0xA2 LL
     *       0x30 LL abcd cname
     *    [0xA3 LL
     *       0x30 LL abcd] cksum
     *     0xA4 LL
     *       0x02 LL nnn  cusec
     *     0xA5 0x11
     *       0x18 0x0F ttt ctime
     *    [0xA6 LL
     *       0x30 LL abcd] subkey
     *    [0xA7 LL
     *       0x02 LL nnn] seq-number
     *    [0xA8 LL
     *       0x30 LL abcd] authorization-data
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
            // The Authenticator APPLICATION Tag
            buffer.put( (byte)KerberosConstants.AUTHENTICATOR_TAG );
            buffer.put( TLV.getBytes( authenticatorLength ) );

            // The Authenticator SEQUENCE Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( authenticatorSeqLength ) );
            
            // The authenticator-vno ------------------------------------------
            // The tag
            buffer.put( (byte)KerberosConstants.AUTHENTICATOR_AUTHENTICATOR_VNO_TAG );
            buffer.put( TLV.getBytes( authenticatorVnoLength ) );
            
            // The value
            Value.encode( buffer, getProtocolVersionNumber() );
            
            // The crealm -----------------------------------------------------
            // The tag
            buffer.put( (byte)KerberosConstants.AUTHENTICATOR_CREALM_TAG );
            buffer.put( TLV.getBytes( crealmLength ) );
            
            // The value
            buffer.put( UniversalTag.GENERAL_STRING.getValue() );
            buffer.put( TLV.getBytes( crealmBytes.length ) );
            buffer.put( crealmBytes );
            
            // The cname ------------------------------------------------------
            // The tag
            buffer.put( (byte)KerberosConstants.AUTHENTICATOR_CNAME_TAG );
            buffer.put( TLV.getBytes( cnameLength ) );
            
            // The value
            cname.encode( buffer );
            
            // The cksum, if any ----------------------------------------------
            if ( cksum != null )
            {
                // The tag
                buffer.put( (byte)KerberosConstants.AUTHENTICATOR_CKSUM_TAG );
                buffer.put( TLV.getBytes( cksumLength ) );
                
                // The value
                cksum.encode( buffer );
            }
            
            // The cusec ------------------------------------------------------
            // The tag
            buffer.put( (byte)KerberosConstants.AUTHENTICATOR_CUSEC_TAG );
            buffer.put( TLV.getBytes( cusecLength ) );
            
            // The value
            Value.encode( buffer, cusec );
            
            // The ctime ------------------------------------------------------
            // The tag
            buffer.put( (byte)KerberosConstants.AUTHENTICATOR_CTIME_TAG );
            buffer.put( TLV.getBytes( ctimeLength ) );
            
            // The value
            buffer.put( (byte)UniversalTag.GENERALIZED_TIME.getValue() );
            buffer.put( (byte)0x0F );
            buffer.put( ctime.getBytes() );
            
            // The subkey if any ---------------------------------------------------
            if ( subKey != null )
            {
                // The tag
                buffer.put( (byte)KerberosConstants.AUTHENTICATOR_SUBKEY_TAG );
                buffer.put( TLV.getBytes( subkeyLength ) );
                
                // The value
                subKey.encode( buffer );
            }
            
            // The seq-number, if any -----------------------------------------
            // The tag
            buffer.put( (byte)KerberosConstants.AUTHENTICATOR_SEQ_NUMBER_TAG );
            buffer.put( TLV.getBytes( seqNumberLength ) );
            
            // The value
            Value.encode( buffer, seqNumber );
            
            // The authorization-data, if any ---------------------------------
            if ( authorizationData != null )
            {
                // The tag
                buffer.put( (byte)KerberosConstants.AUTHENTICATOR_AUTHORIZATION_DATA_TAG );
                buffer.put( TLV.getBytes( authorizationDataLength ) );
                
                // The value
                authorizationData.encode( buffer );
            }
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_139, 1 + TLV.getNbBytes( 0 )
                + 0, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "Authenticator encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            LOG.debug( "Authenticator initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * reset the transient fields used while computing length
     */
    private void reset()
    {
    	authenticatorVnoLength = 0;
        crealmLength = 0;
        crealmBytes = null;
        cnameLength = 0;
        cksumLength = 0;
        cusecLength = 0;
        ctimeLength = 0;
        subkeyLength = 0;
        seqNumberLength = 0;
        authorizationDataLength = 0;
        authenticatorSeqLength = 0;
        authenticatorLength = 0;	
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Authenticator : \n" );
        
        sb.append( "    authenticator-vno : " ).append( getVersionNumber() ).append( '\n' );
        sb.append( "    crealm : " ).append( crealm ).append( '\n' );
        sb.append( "    cname : " ).append( cname ).append( '\n' );
        
        if ( cksum != null )
        {
            sb.append( "    cksum : " ).append( cksum ).append( '\n' );
        }
        
        sb.append( "    cusec : " ).append( cusec ).append( '\n' );
        sb.append( "    ctime : " ).append( ctime ).append( '\n' );
        
        if ( subKey != null )
        {
            sb.append( "    subkey : " ).append( subKey ).append( '\n' );
        }
        
        if ( seqNumber != null )
        {
            sb.append( "    seq-number : " ).append( seqNumber ).append( '\n' );
        }
        
        if ( authorizationData != null )
        {
            sb.append( "    authorization-data : " ).append( authorizationData ).append( '\n' );
        }

        return sb.toString();
    }
}
