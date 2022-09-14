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
import org.apache.directory.shared.kerberos.codec.encryptionKey.EncryptionKeyContainer;
import org.apache.directory.shared.kerberos.codec.principalName.PrincipalNameContainer;
import org.apache.directory.shared.kerberos.codec.ticket.TicketContainer;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
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
}
