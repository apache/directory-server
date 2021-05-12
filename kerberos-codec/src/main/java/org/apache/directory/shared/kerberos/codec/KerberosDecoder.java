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
package org.apache.directory.shared.kerberos.codec;


import java.nio.ByteBuffer;

import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.asn1.ber.Asn1Container;
import org.apache.directory.api.asn1.ber.Asn1Decoder;
import org.apache.directory.api.asn1.ber.tlv.TLVStateEnum;
import org.apache.directory.shared.kerberos.codec.EncKdcRepPart.EncKdcRepPartContainer;
import org.apache.directory.shared.kerberos.codec.apRep.ApRepContainer;
import org.apache.directory.shared.kerberos.codec.apReq.ApReqContainer;
import org.apache.directory.shared.kerberos.codec.authenticator.AuthenticatorContainer;
import org.apache.directory.shared.kerberos.codec.authorizationData.AuthorizationDataContainer;
import org.apache.directory.shared.kerberos.codec.encApRepPart.EncApRepPartContainer;
import org.apache.directory.shared.kerberos.codec.encAsRepPart.EncAsRepPartContainer;
import org.apache.directory.shared.kerberos.codec.encKrbPrivPart.EncKrbPrivPartContainer;
import org.apache.directory.shared.kerberos.codec.encTgsRepPart.EncTgsRepPartContainer;
import org.apache.directory.shared.kerberos.codec.encTicketPart.EncTicketPartContainer;
import org.apache.directory.shared.kerberos.codec.encryptedData.EncryptedDataContainer;
import org.apache.directory.shared.kerberos.codec.encryptionKey.EncryptionKeyContainer;
import org.apache.directory.shared.kerberos.codec.krbPriv.KrbPrivContainer;
import org.apache.directory.shared.kerberos.codec.paEncTsEnc.PaEncTsEncContainer;
import org.apache.directory.shared.kerberos.codec.principalName.PrincipalNameContainer;
import org.apache.directory.shared.kerberos.codec.ticket.TicketContainer;
import org.apache.directory.shared.kerberos.components.AuthorizationData;
import org.apache.directory.shared.kerberos.components.EncKdcRepPart;
import org.apache.directory.shared.kerberos.components.EncKrbPrivPart;
import org.apache.directory.shared.kerberos.components.EncTicketPart;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.PaEncTsEnc;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.kerberos.messages.ApRep;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.apache.directory.shared.kerberos.messages.Authenticator;
import org.apache.directory.shared.kerberos.messages.EncApRepPart;
import org.apache.directory.shared.kerberos.messages.EncAsRepPart;
import org.apache.directory.shared.kerberos.messages.EncTgsRepPart;
import org.apache.directory.shared.kerberos.messages.KrbPriv;
import org.apache.directory.shared.kerberos.messages.Ticket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosDecoder
{

    /** The logger */
    private static Logger LOG = LoggerFactory.getLogger( KerberosDecoder.class );

    /** A speedup for logger */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    public static Object decode( KerberosMessageContainer kerberosMessageContainer ) throws DecoderException
    {
        ByteBuffer buf = kerberosMessageContainer.getStream();
        
        if ( kerberosMessageContainer.isTCP() )
        {
            if ( buf.remaining() > 4 )
            {
                kerberosMessageContainer.setTcpLength( buf.getInt() );
                buf.mark();
            }
            else
            {
                return null;
            }
        }
        else
        {
            buf.mark();
        }

        while ( buf.hasRemaining() )
        {
            try
            {
                Asn1Decoder.decode( buf, kerberosMessageContainer );
                
                if ( kerberosMessageContainer.getState() == TLVStateEnum.PDU_DECODED )
                {
                    if ( IS_DEBUG )
                    {
                        LOG.debug( "Decoded KerberosMessage : {}", kerberosMessageContainer.getMessage() );
                        buf.mark();
                    }
        
                    return kerberosMessageContainer.getMessage();
                }
            }
            catch ( DecoderException de )
            {
                LOG.warn( "error while decoding", de );
                buf.clear();
                kerberosMessageContainer.clean();
                throw de;
            }
        }
        
        return null;
    }
    
    
    /**
     * Decode an EncrytedData structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of EncryptedData
     * @throws KerberosException If the decoding fails
     */
    public static EncryptedData decodeEncryptedData( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a EncryptedData Container
        Asn1Container encryptedDataContainer = new EncryptedDataContainer();

        // Decode the EncryptedData PDU
        try
        {
            Asn1Decoder.decode( stream, encryptedDataContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded EncryptedData
        return ( ( EncryptedDataContainer ) encryptedDataContainer ).getEncryptedData();
    }
    
    
    /**
     * Decode an PaEncTsEnc structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of PaEncTsEnc
     * @throws KerberosException If the decoding fails
     */
    public static PaEncTsEnc decodePaEncTsEnc( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a PaEncTsEnc Container
        Asn1Container paEncTsEncContainer = new PaEncTsEncContainer();

        // Decode the PaEncTsEnc PDU
        try
        {
            Asn1Decoder.decode( stream, paEncTsEncContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded PaEncTsEnc
        return ( ( PaEncTsEncContainer ) paEncTsEncContainer ).getPaEncTsEnc();
    }
    
    
    /**
     * Decode an EncApRepPart structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of EncApRepPart
     * @throws KerberosException If the decoding fails
     */
    public static EncApRepPart decodeEncApRepPart( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a EncApRepPart Container
        Asn1Container encApRepPartContainer = new EncApRepPartContainer( stream );

        // Decode the EncApRepPart PDU
        try
        {
            Asn1Decoder.decode( stream, encApRepPartContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded EncApRepPart
        return ( ( EncApRepPartContainer ) encApRepPartContainer ).getEncApRepPart();
    }
    
    
    /**
     * Decode an EncKdcRepPart structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of EncKdcRepPart
     * @throws KerberosException If the decoding fails
     */
    public static EncKdcRepPart decodeEncKdcRepPart( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a EncKdcRepPart Container
        Asn1Container encKdcRepPartContainer = new EncKdcRepPartContainer( stream );

        // Decode the EncKdcRepPart PDU
        try
        {
            Asn1Decoder.decode( stream, encKdcRepPartContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded EncKdcRepPart
        return ( ( EncKdcRepPartContainer ) encKdcRepPartContainer ).getEncKdcRepPart();
    }
    
    
    /**
     * Decode an EncKrbPrivPart structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of EncKrbPrivPart
     * @throws KerberosException If the decoding fails
     */
    public static EncKrbPrivPart decodeEncKrbPrivPart( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a EncKrbPrivPart Container
        Asn1Container encKrbPrivPartContainer = new EncKrbPrivPartContainer( stream );

        // Decode the EncKrbPrivPart PDU
        try
        {
            Asn1Decoder.decode( stream, encKrbPrivPartContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded EncKrbPrivPart
        return ( ( EncKrbPrivPartContainer ) encKrbPrivPartContainer ).getEncKrbPrivPart();
    }
    
    
    /**
     * Decode an EncTicketPart structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of EncTicketPart
     * @throws KerberosException If the decoding fails
     */
    public static EncTicketPart decodeEncTicketPart( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a EncTicketPart Container
        Asn1Container encTicketPartContainer = new EncTicketPartContainer( stream );

        // Decode the EncTicketPart PDU
        try
        {
            Asn1Decoder.decode( stream, encTicketPartContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded EncTicketPart
        return ( ( EncTicketPartContainer ) encTicketPartContainer ).getEncTicketPart();
    }
    
    
    /**
     * Decode an EncryptionKey structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of EncryptionKey
     * @throws KerberosException If the decoding fails
     */
    public static EncryptionKey decodeEncryptionKey( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a EncryptionKey Container
        Asn1Container encryptionKeyContainer = new EncryptionKeyContainer();

        // Decode the EncryptionKey PDU
        try
        {
            Asn1Decoder.decode( stream, encryptionKeyContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded EncryptionKey
        return ( ( EncryptionKeyContainer ) encryptionKeyContainer ).getEncryptionKey();
    }
    
    
    /**
     * Decode an PrincipalName structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of PrincipalName
     * @throws KerberosException If the decoding fails
     */
    public static PrincipalName decodePrincipalName( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a PrincipalName Container
        Asn1Container principalNameContainer = new PrincipalNameContainer();

        // Decode the PrincipalName PDU
        try
        {
            Asn1Decoder.decode( stream, principalNameContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded PrincipalName
        return ( ( PrincipalNameContainer ) principalNameContainer ).getPrincipalName();
    }
    
    
    /**
     * Decode a Ticket structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of Ticket
     * @throws KerberosException If the decoding fails
     */
    public static Ticket decodeTicket( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a Ticket Container
        Asn1Container ticketContainer = new TicketContainer( stream );

        // Decode the Ticket PDU
        try
        {
            Asn1Decoder.decode( stream, ticketContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded Ticket
        return ( ( TicketContainer ) ticketContainer ).getTicket();
    }
    
    
    /**
     * Decode a Authenticator structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of Authenticator
     * @throws KerberosException If the decoding fails
     */
    public static Authenticator decodeAuthenticator( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a Authenticator Container
        Asn1Container authenticatorContainer = new AuthenticatorContainer( stream );

        // Decode the Ticket PDU
        try
        {
            Asn1Decoder.decode( stream, authenticatorContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded Authenticator
        return ( ( AuthenticatorContainer ) authenticatorContainer ).getAuthenticator();
    }
    
    
    /**
     * Decode a AuthorizationData structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of AuthorizationData
     * @throws KerberosException If the decoding fails
     */
    public static AuthorizationData decodeAuthorizationData( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a AuthorizationData Container
        Asn1Container authorizationDataContainer = new AuthorizationDataContainer();

        // Decode the Ticket PDU
        try
        {
            Asn1Decoder.decode( stream, authorizationDataContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded AuthorizationData
        return ( ( AuthorizationDataContainer ) authorizationDataContainer ).getAuthorizationData();
    }

    
    /**
     * Decode a AP-REP structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of ApRep
     * @throws KerberosException If the decoding fails
     */
    public static ApRep decodeApRep( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a ApRep Container
        Asn1Container apRepContainer = new ApRepContainer( stream );

        // Decode the ApRep PDU
        try
        {
            Asn1Decoder.decode( stream, apRepContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded ApRep
        return ( ( ApRepContainer ) apRepContainer ).getApRep();
    }

    
    /**
     * Decode a AP-REQ structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of ApReq
     * @throws KerberosException If the decoding fails
     */
    public static ApReq decodeApReq( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a ApReq Container
        Asn1Container apReqContainer = new ApReqContainer( stream );

        // Decode the ApReq PDU
        try
        {
            Asn1Decoder.decode( stream, apReqContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded ApReq
        return ( ( ApReqContainer ) apReqContainer ).getApReq();
    }

    
    /**
     * Decode a KRB-PRIV structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of KrbPriv
     * @throws KerberosException If the decoding fails
     */
    public static KrbPriv decodeKrbPriv( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a KrbPriv Container
        Asn1Container krbPrivContainer = new KrbPrivContainer( stream );

        // Decode the KrbPriv PDU
        try
        {
            Asn1Decoder.decode( stream, krbPrivContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded KrbPriv
        return ( ( KrbPrivContainer ) krbPrivContainer ).getKrbPriv();
    }
    
    
    /**
     * Decode an EncAsRepPart structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of EncAsRepPart
     * @throws KerberosException If the decoding fails
     */
    public static EncAsRepPart decodeEncAsRepPart( byte[] data ) throws KerberosException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a EncAsRepPart Container
        Asn1Container encAsRepPartContainer = new EncAsRepPartContainer( stream );

        // Decode the EncAsRepPart PDU
        try
        {
            Asn1Decoder.decode( stream, encAsRepPartContainer );
        }
        catch ( DecoderException de )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_BAD_INTEGRITY, de );
        }

        // get the decoded EncAsRepPart
        return ( ( EncAsRepPartContainer ) encAsRepPartContainer ).getEncAsRepPart();
    }

    
    /**
     * Decode an EncTgsRepPart structure
     * 
     * @param data The byte array containing the data structure to decode
     * @return An instance of EncTgsRepPart
     * @throws DecodeException If the decoding fails
     */
    public static EncTgsRepPart decodeEncTgsRepPart( byte[] data ) throws DecoderException
    {
        ByteBuffer stream = ByteBuffer.allocate( data.length );
        stream.put( data );
        stream.flip();
        
        // Allocate a EncTgsRepPart Container
        Asn1Container encTgsRepPartContainer = new EncTgsRepPartContainer( stream );

        // Decode the EncTgsRepPart PDU
        Asn1Decoder.decode( stream, encTgsRepPartContainer );

        // get the decoded EncTgsRepPart
        return ( ( EncTgsRepPartContainer ) encTgsRepPartContainer ).getEncTgsRepPart();
    }
}
