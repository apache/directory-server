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
import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationData;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalName;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosRequestBody;
import org.apache.directory.server.kerberos.shared.messages.value.flags.KdcOption;
import org.apache.directory.server.kerberos.shared.messages.value.flags.KdcOptions;
import org.apache.directory.server.kerberos.shared.messages.value.flags.KerberosFlag;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implements the KDC-REQ message.
 * 
 * The ASN.1 grammar is the following :
 * 
 * KDC-REQ         ::= SEQUENCE {
 *        -- NOTE: first tag is [1], not [0]
 *        pvno            [1] INTEGER (5) ,
 *        msg-type        [2] INTEGER (10 -- AS -- | 12 -- TGS --),
 *        padata          [3] SEQUENCE OF PA-DATA OPTIONAL
 *                            -- NOTE: not empty --,
 *        req-body        [4] KDC-REQ-BODY
 * }
 * 
 * The pvno and msg-type are handled by the KerberosMessage inherited class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KdcRequest extends KerberosMessage
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( KdcRequest.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The pre-authentication data */
    private List<PreAuthenticationData> paData; //optional
    
    /** The request body */
    private KerberosRequestBody reqBody;
    
    /** A byte[] representing the request body */
    private byte[] reqBodyBytes;


    // Storage for computed lengths
    private transient int kdcReqSeqLength;
    
    private transient int paDataTagLength;
    private transient int paDataSeqLength;
    
    private transient int reqBodyTagLength;
    
    /**
     * Creates a new instance of KdcRequest.
     *
     * @param pvno
     * @param messageType
     * @param preAuthData
     * @param requestBody
     */
    public KdcRequest( int pvno, MessageType messageType, List<PreAuthenticationData> paData, KerberosRequestBody reqBody )
    {
        super( pvno, messageType );
        this.paData = paData;
        this.reqBody = reqBody;
    }

    /**
     * Creates a new instance of KdcRequest.
     *
     * @param pvno
     * @param messageType
     * @param preAuthData
     * @param requestBody
     */
    public KdcRequest( MessageType messageType, List<PreAuthenticationData> paData, KerberosRequestBody reqBody )
    {
        super( messageType );
        this.paData = paData;
        this.reqBody = reqBody;
    }

    /**
     * Returns an array of {@link PreAuthenticationData}s.
     *
     * @return The array of {@link PreAuthenticationData}s.
     */
    public List<PreAuthenticationData> getPreAuthData()
    {
        return paData;
    }


    /**
     * Returns the bytes of the body.  This is used for verifying checksums in
     * the Ticket-Granting Service (TGS).
     *
     * @return The bytes of the body.
     */
    public byte[] getBodyBytes()
    {
        return reqBodyBytes;
    }


    // RequestBody delegate methods

    /**
     * Returns additional {@link Ticket}s.
     *
     * @return The {@link Ticket}s.
     */
    public List<Ticket> getAdditionalTickets()
    {
        return reqBody.getAdditionalTickets();
    }


    /**
     * Returns the {@link HostAddresses}.
     *
     * @return The {@link HostAddresses}.
     */
    public HostAddresses getAddresses()
    {
        return reqBody.getAddresses();
    }


    /**
     * Returns the client {@link PrincipalName}.
     *
     * @return The client {@link PrincipalName}.
     */
    public PrincipalName getClientPrincipalName()
    {
        return reqBody.getClientPrincipalName();
    }


    /**
     * Returns the client {@link KerberosPrincipal}.
     *
     * @return The client {@link KerberosPrincipal}.
     */
    public KerberosPrincipal getClientPrincipal()
    {
        return reqBody.getClientPrincipal();
    }


    /**
     * Returns the realm of the server principal.
     *
     * @return The realm.
     */
    public String getRealm()
    {
        return reqBody.getRealm();
    }


    /**
     * Returns the {@link EncryptedData}.
     *
     * @return The {@link EncryptedData}.
     */
    public EncryptedData getEncAuthorizationData()
    {
        return reqBody.getEncAuthorizationData();
    }


    /**
     * Returns an array of requested {@link EncryptionType}s.
     *
     * @return The array of {@link EncryptionType}s.
     */
    public List<EncryptionType> getEType()
    {
        return reqBody.getEType();
    }


    /**
     * Returns the from {@link KerberosTime}.
     *
     * @return The from {@link KerberosTime}.
     */
    public KerberosTime getFrom()
    {
        return reqBody.getFrom();
    }


    /**
     * Returns the {@link KdcOptions}.
     *
     * @return The {@link KdcOptions}.
     */
    public KdcOptions getKdcOptions()
    {
        return reqBody.getKdcOptions();
    }


    /**
     * Returns the nonce.
     *
     * @return The nonce.
     */
    public int getNonce()
    {
        return reqBody.getNonce();
    }


    /**
     * Returns the renew-till" {@link KerberosTime}.
     *
     * @return The renew-till" {@link KerberosTime}.
     */
    public KerberosTime getRenewtime()
    {
        return reqBody.getRenewtime();
    }


    /**
     * Returns the server {@link KerberosPrincipal}.
     *
     * @return The server {@link KerberosPrincipal}.
     */
    public KerberosPrincipal getServerPrincipal()
    {
        return reqBody.getServerPrincipal();
    }

    /**
     * Returns the server {@link PrincipalName}.
     *
     * @return The server {@link PrincipalName}.
     */
    public PrincipalName getServerPrincipalName()
    {
        return reqBody.getServerPrincipalName();
    }

    /**
     * Returns the till {@link KerberosTime}.
     *
     * @return The till {@link KerberosTime}.
     */
    public KerberosTime getTill()
    {
        return reqBody.getTill();
    }


    // RequestBody KdcOptions delegate accesors

    /**
     * Returns the option at the specified index.
     *
     * @param option
     * @return The option.
     */
    public boolean getOption( int option )
    {
        try
        {
            return reqBody.getKdcOptions().getBit( option );
        }
        catch ( DecoderException de )
        {
            return false;
        }
    }

    /**
     * Returns the option for the specified flag
     *
     * @param option
     * @return The option.
     */
    public boolean getOption( KerberosFlag option )
    {
        return reqBody.getKdcOptions().isFlagSet( option );
    }


    /**
     * Sets the option at the specified index.
     *
     * @param option
     */
    public void setOption( int option )
    {
        reqBody.getKdcOptions().setBit( option );
    }


    /**
     * Sets the option at the specified index.
     *
     * @param option
     */
    public void setOption( KdcOption option )
    {
        reqBody.getKdcOptions().setFlag( option );
    }


    /**
     * Clears the option at the specified index.
     *
     * @param option
     */
    public void clearOption( int option )
    {
        reqBody.getKdcOptions().clearBit( option );
    }

    /**
     * Return the length of a KdcRequest message .
     * 
     * 0x30 L1
     *  |
     *  +--> 0xA1 0x03
     *  |     |
     *  |     +--> 0x02 0x01 pvno (integer)
     *  |
     *  +--> 0xA2 0x03
     *  |     |
     *  |     +--> 0x02 0x01 msg-type (integer)
     *  |
     *  +--> [0xA3 L2
     *  |     |
     *  |     +--> 0x30 L3-1 padata
     *  |           |
     *  |           +--> 0x30 L2-1-1 padata (PA-DATA)
     *  |           |
     *  |           +--> ...
     *  |           |
     *  |           +--> 0x61 L2-1-N padata ]
     *  |
     *  +--> 0xA4 L4 
     *        | 
     *        +--> 0x30 L4-1 req-body (KDC-REQ-BODY)
     */
    public int computeLength()
    {
        // First compute the KerberosMessage length
        kdcReqSeqLength = super.computeLength();
        
        // The pa-data length
        if ( paData == null )
        {
            return -1;
        }
        
        paDataSeqLength = 0;
        
        for ( PreAuthenticationData data:paData )
        {
            paDataSeqLength += data.computeLength();
        }
        
        paDataTagLength = 1 + TLV.getNbBytes( paDataSeqLength ) + paDataSeqLength;
        kdcReqSeqLength += 1 + TLV.getNbBytes( paDataTagLength ) + paDataTagLength;
        
        // The request body data length
        if ( reqBody == null )
        {
            return -1;
        }
        
        reqBodyTagLength = reqBody.computeLength();
        
        kdcReqSeqLength += 
            1 + TLV.getNbBytes( reqBodyTagLength ) + reqBodyTagLength;


        return 1 + TLV.getNbBytes( kdcReqSeqLength ) + kdcReqSeqLength;
    }
    
    /**
     * Encode the KdcRequest message to a PDU. 
     * 
     * KdcRequest :
     * 
     * 0x30 LL
     *   0xA1 LL pvno 
     *   0xA2 LL msg-type
     *   0xA3 LL pa-datas
     *     0x30 LL 
     *       0x30 LL pa-data
     *       ...
     *       0x30 LL pa-data
     *   0xA4 LL req-body
     * 
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( "Cannot put a PDU in a null buffer !" );
        }

        try
        {
            // The kdcRequest SEQUENCE Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( kdcReqSeqLength ) );
            
            // As the first tag is not 0xA0, we have to inform the super class.
            setStartingTag( (byte)0xA1 );

            // The pvno and msg-type Tag and value
            super.encode(  buffer );
            
            // The padata, if any
            buffer.put( (byte)0xA3 );
            buffer.put( TLV.getBytes( paDataTagLength ) );
            
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( paDataSeqLength ) );

            if ( paData != null )
            {
                for ( PreAuthenticationData pa:paData )
                {
                    pa.encode( buffer );
                }
            }
            
            // REQ-BODY encoding
            buffer.put( (byte)0xA4 );
            buffer.put( TLV.getBytes( reqBodyTagLength ) );
            
            if ( reqBody != null )
            {
                reqBody.encode( buffer );
            }
            else
            {
                log.error( "Null REQ-BODY part" );
                throw new EncoderException( "The REQ-BODY must not be null" );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( "Cannot encode the KRB-CRED object, the PDU size is {} when only {} bytes has been allocated", 1
                + TLV.getNbBytes( kdcReqSeqLength ) + kdcReqSeqLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "KdcRequest encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "KdcRequest initial value : {}", toString() );
        }

        return buffer;
    }

    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( tabs ).append( "NYI\n" );
        sb.append( super.toString( tabs + "    " ) );
        
        return sb.toString();
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}
