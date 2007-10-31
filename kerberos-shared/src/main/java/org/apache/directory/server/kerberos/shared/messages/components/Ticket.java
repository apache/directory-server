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

import org.apache.directory.server.kerberos.shared.KerberosConstants;
import org.apache.directory.server.kerberos.shared.KerberosUtils;
import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.messages.value.AuthorizationData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalName;
import org.apache.directory.server.kerberos.shared.messages.value.TransitedEncoding;
import org.apache.directory.server.kerberos.shared.messages.value.flags.TicketFlags;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Ticket message component as handed out by the ticket granting service.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Ticket extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( Ticket.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();
    
    /** Constant for the {@link Ticket} version number (5) */
    public static final int TICKET_VNO = KerberosConstants.KERBEROS_V5;

    /** The Kerberos version number. Should be 5 */
    private int tktvno;
    
    /** A storage for a byte array representation of the realm */
    private byte[] realmBytes;
    
    /** The server principal name */
    private PrincipalName sName;
    
    /** The server realm */
    private String realm;
    
    /** The encoded part */
    private EncryptedData encPart;
    
    /** The decoded ticket part */
    private EncTicketPart encTicketPart;

    // Storage for computed lengths
    private transient int tktvnoLength;
    private transient int realmLength;
    private transient int sNameLength;
    private transient int encPartLength;
    private transient int ticketSeqLength;
    private transient int ticketLength;

    /**
     * Creates a new instance of Ticket.
     *
     * @param serverPrincipal The server principal
     * @param encPart The encoded part
     */
    public Ticket( KerberosPrincipal serverPrincipal, EncryptedData encPart ) throws InvalidTicketException
    {
        this( TICKET_VNO, serverPrincipal, encPart );

        setServerPrincipal( serverPrincipal );
    }


    /**
     * Creates a new instance of Ticket.
     */
    public Ticket()
    {
    }
    
    
    /**
     * Creates a new instance of Ticket.
     *
     * @param tktvno The Kerberos version number
     * @param serverPrincipal The server principal
     * @param encPart The encoded part
     */
    public Ticket( int tktvno, KerberosPrincipal serverPrincipal, EncryptedData encPart ) throws InvalidTicketException
    {
        this.tktvno = tktvno;
        this.encPart = encPart;
        setServerPrincipal( serverPrincipal );
    }


    /**
     * Sets the {@link EncTicketPart}.
     *
     * @param decryptedPart
     */
    public void setEncTicketPart( EncTicketPart decryptedPart )
    {
        encTicketPart = decryptedPart;
    }


    /**
     * Returns the version number.
     *
     * @return The version number.
     */
    public int getTktVno()
    {
        return tktvno;
    }
    
    
    /**
     * Set the ticket version number
     * @param tktvno the ticket version number
     */
    public void setTktVno( int tktvno )
    {
        this.tktvno = tktvno;
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
     * Returns the server {@link KerberosPrincipal}.
     *
     * @return The server {@link KerberosPrincipal}.
     */
    public KerberosPrincipal getServerPrincipal()
    {
        return KerberosUtils.getKerberosPrincipal( sName, realm );
    }

    
    /**
     * Set the server principalName
     * @param sName the server principalName
     */
    public void setSName( PrincipalName sName )
    {
        this.sName = sName;
    }
    

    /**
     * Set the server KerberosPrincipal
     * @param sName the server KerberosPrincipal
     */
    public void setServerPrincipal( KerberosPrincipal serverPrincipal ) throws InvalidTicketException
    {
        try
        {
            sName = new PrincipalName( serverPrincipal.getName(), serverPrincipal.getNameType() );
            realm = serverPrincipal.getRealm();
        }
        catch ( ParseException pe )
        {
            LOG.error( "Cannot create a ticket for the {} KerberosPrincipal, error : {}", serverPrincipal, pe.getMessage() );
            throw new InvalidTicketException( ErrorType.KRB_ERR_GENERIC, "Cannot create a ticket : " + pe.getMessage() );
        }
    }
    

    /**
     * Returns the server realm.
     *
     * @return The server realm.
     */
    public String getRealm()
    {
        return realm;
    }


    /**
     * Set the server realm
     * @param realm the server realm
     */
    public void setRealm( String realm )
    {
        this.realm = realm;
    }
    
   
    /**
     * Returns the {@link EncryptedData}.
     *
     * @return The {@link EncryptedData}.
     */
    public EncryptedData getEncPart()
    {
        return encPart;
    }

    
    /**
     * Set the encrypted ticket part
     * @param encPart the encrypted ticket part
     */
    public void setEncPart( EncryptedData encPart )
    {
        this.encPart = encPart; 
    }
    

    /**
     * Returns the {@link EncTicketPart}.
     *
     * @return The {@link EncTicketPart}.
     */
    public EncTicketPart getEncTicketPart()
    {
        return encTicketPart;
    }


    /**
     * Returns the {@link AuthorizationData}.
     *
     * @return The {@link AuthorizationData}.
     *
    public AuthorizationData getAuthorizationData()
    {
        return encTicketPart.getAuthorizationData();
    }


    /**
     * Returns the auth {@link KerberosTime}.
     *
     * @return The auth {@link KerberosTime}.
     *
    public KerberosTime getAuthTime()
    {
        return encTicketPart.getAuthTime();
    }


    /**
     * Returns the client {@link HostAddresses}.
     *
     * @return The client {@link HostAddresses}.
     *
    public HostAddresses getClientAddresses()
    {
        return encTicketPart.getClientAddresses();
    }


    /**
     * Returns the client {@link KerberosPrincipal}.
     *
     * @return The client {@link KerberosPrincipal}.
     *
    public KerberosPrincipal getClientPrincipal()
    {
        return encTicketPart.getClientPrincipal();
    }

    /**
     * Returns the client {@link PrincipalName}.
     *
     * @return The client {@link PrincipalName}.
     *
    public PrincipalName getClientPrincipalName()
    {
        return encTicketPart.getClientPrincipalName();
    }


    /**
     * Returns the client realm.
     *
     * @return The client realm.
     *
    public String getClientRealm()
    {
        return encTicketPart.getClientRealm();
    }


    /**
     * Returns the end {@link KerberosTime}.
     *
     * @return The end {@link KerberosTime}.
     *
    public KerberosTime getEndTime()
    {
        return encTicketPart.getEndTime();
    }


    /**
     * Returns the {@link TicketFlags}.
     *
     * @return The {@link TicketFlags}.
     *
    public TicketFlags getFlags()
    {
        return encTicketPart.getFlags();
    }

    /**
     * Returns the integer value for the {@link TicketFlags}.
     *
     * @return The {@link TicketFlags}.
     *
    public int getFlagsIntValue()
    {
        return encTicketPart.getFlags().getIntValue();
    }


    /**
     * Returns the renew till {@link KerberosTime}.
     *
     * @return The renew till {@link KerberosTime}.
     *
    public KerberosTime getRenewTill()
    {
        return encTicketPart.getRenewTill();
    }


    /**
     * Returns the session {@link EncryptionKey}.
     *
     * @return The session {@link EncryptionKey}.
     *
    public EncryptionKey getSessionKey()
    {
        return encTicketPart.getSessionKey();
    }


    /**
     * Returns the start {@link KerberosTime}.
     *
     * @return The start {@link KerberosTime}.
     *
    public KerberosTime getStartTime()
    {
        return encTicketPart.getStartTime();
    }


    /**
     * Returns the {@link TransitedEncoding}.
     *
     * @return The {@link TransitedEncoding}.
     *
    public TransitedEncoding getTransitedEncoding()
    {
        return encTicketPart.getTransitedEncoding();
    }


    /**
     * Returns the flag at the given index.
     *
     * @param flag
     * @return true if the flag at the given index is set.
     *
    public boolean getFlag( int flag )
    {
        return encTicketPart.getFlags().isFlagSet( flag );
    }

    /**
     * Compute the Ticket length
     * 
     * Ticket :
     * 
     * 0x61 L1 Ticket [APPLICATION 1]
     *  |
     *  +--> 0x30 L2 Ticket SEQUENCE
     *        |
     *        +--> 0xA0 L3 tkt-vno tag
     *        |     |
     *        |     +--> 0x02 L3-1 tkt-vno (int, 5)
     *        |
     *        +--> 0xA1 L4 realm tag
     *        |     |
     *        |     +--> 0x1B L4-1 realm (KerberosString)
     *        |
     *        +--> 0xA2 L5 sname (PrincipalName)
     *        |
     *        +--> 0xA3 L6 enc-part (EncryptedData)
     */
    public int computeLength()
    {
        // Compute the Ticket version length.
        tktvnoLength = 1 + TLV.getNbBytes( tktvno ) + Value.getNbBytes( tktvno );

        // Compute the Ticket realm length.
        realmBytes = StringTools.getBytesUtf8( realm );
        realmLength = 1 + TLV.getNbBytes( realmBytes.length ) + realmBytes.length;

        // Compute the principal length
        sNameLength = sName.computeLength();
        
        // Compute the encrypted data
        encPartLength = encPart.computeLength();

        // Compute the sequence size
        ticketSeqLength = 
            1 + TLV.getNbBytes( tktvnoLength ) + tktvnoLength +
            1 + TLV.getNbBytes( realmLength ) + realmLength +
            1 + TLV.getNbBytes( sNameLength ) + sNameLength + 
            1 + TLV.getNbBytes( encPartLength ) + encPartLength;
        
        // compute the global size
        ticketLength = 1 + TLV.getNbBytes( ticketSeqLength ) + ticketSeqLength;
        
        return 1 + TLV.getNbBytes( ticketLength ) + ticketLength;
    }
    
    /**
     * Encode the Ticket message to a PDU. 
     * 
     * Ticket :
     * 
     * 0x61 LL
     *   0x30 LL
     *     0xA0 LL tktvno 
     *     0xA1 LL realm
     *     0xA2 LL
     *       sname (PrincipalName)
     *     0xA3 LL
     *       enc-part (EncryptedData)
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
            // The Ticket APPLICATION Tag
            buffer.put( (byte)0x61 );
            buffer.put( TLV.getBytes( ticketLength ) );

            // The Ticket SEQUENCE Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( ticketSeqLength ) );

            // The tkt-vno Tag and value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( tktvnoLength ) );
            Value.encode( buffer, tktvno );

            // The realm Tag and value
            buffer.put( ( byte ) 0xA1 );
            buffer.put( TLV.getBytes( realmLength ) );
            buffer.put( UniversalTag.GENERALIZED_STRING_TAG );
            buffer.put( TLV.getBytes( realmBytes.length ) );
            buffer.put( realmBytes );

            // The sname Tag and value
            buffer.put( ( byte ) 0xA2 );
            buffer.put( TLV.getBytes( sNameLength ) );
            sName.encode( buffer );
            
            // The encPartLength Tag and value
            buffer.put( ( byte ) 0xA3 );
            buffer.put( TLV.getBytes( encPartLength ) );
            encPart.encode( buffer );
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( "Cannot encode the Ticket object, the PDU size is {} when only {} bytes has been allocated", 1
                + TLV.getNbBytes( ticketLength ) + ticketLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "Ticket encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            LOG.debug( "Ticket initial value : {}", toString() );
        }

        return buffer;
    }
}
