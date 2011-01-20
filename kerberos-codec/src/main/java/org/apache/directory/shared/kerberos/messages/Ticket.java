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
import org.apache.directory.shared.kerberos.components.EncTicketPart;
import org.apache.directory.shared.kerberos.components.EncryptedData;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.exceptions.InvalidTicketException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Ticket message component as handed out by the ticket granting service. It will store
 * the object described by the ASN.1 grammar :
 * <pre>
 * Ticket          ::= [APPLICATION 1] SEQUENCE {
 *         tkt-vno         [0] INTEGER (5),
 *         realm           [1] Realm,
 *         sname           [2] <PrincipalName>,
 *         enc-part        [3] <EncryptedData> -- EncTicketPart
 * }
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Ticket extends KerberosMessage
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( Ticket.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();
    
    /** Constant for the {@link Ticket} version number (5) */
    public static final int TICKET_VNO = KerberosConstants.KERBEROS_V5;

    /** A storage for a byte array representation of the realm */
    private byte[] realmBytes;
    
    /** The server principal name */
    private PrincipalName sName;
    
    /** The server realm */
    private String realm;
    
    /** The encoded part */
    private EncryptedData encPart;
    
    /** The encoded ticket part, stored in its original form (not encoded) */
    private transient EncTicketPart encTicketPart;
    
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
    public Ticket( PrincipalName sName, EncryptedData encPart ) throws InvalidTicketException
    {
        this( TICKET_VNO, sName, encPart );

        setSName( sName );
    }


    /**
     * Creates a new instance of Ticket.
     */
    public Ticket()
    {
        super( KerberosMessageType.TICKET );
    }
    
    
    /**
     * Creates a new instance of Ticket.
     *
     * @param tktvno The Kerberos version number
     * @param serverPrincipal The server principal
     * @param encPart The encoded part
     */
    public Ticket( int tktvno, PrincipalName sName, EncryptedData encPart ) throws InvalidTicketException
    {
        super( tktvno, KerberosMessageType.TICKET );
        this.encPart = encPart;
        setSName( sName );
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
     * Returns the server {@link PrincipalName}.
     *
     * @return The server {@link PrincipalName}.
     */
    public PrincipalName getSName()
    {
        return sName;
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
     * Gets the Ticket Version number
     * @return The ticket version number
     */
    public int getTktVno()
    {
        return getProtocolVersionNumber();
    }
    
    
    /**
     * Sets the Ticket Version number
     * @param tktVno The new version number
     */
    public void setTktVno( int tktVno )
    {
        setProtocolVersionNumber( tktVno );
    }
    

    /**
     * @return the encTicketPart
     */
    public EncTicketPart getEncTicketPart()
    {
        return encTicketPart;
    }


    /**
     * @param encTicketPart the encTicketPart to set
     */
    public void setEncTicketPart( EncTicketPart encTicketPart )
    {
        this.encTicketPart = encTicketPart;
    }

    
    /**
     * Compute the Ticket length
     * <pre>
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
     * </pre>
     */
    public int computeLength()
    {
        // Compute the Ticket version length.
        tktvnoLength = 1 + 1 + Value.getNbBytes( getProtocolVersionNumber() );

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
     * <pre>
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
     * </pre>
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
            buffer.put( (byte)KerberosConstants.TICKET_TAG );
            buffer.put( TLV.getBytes( ticketLength ) );

            // The Ticket SEQUENCE Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( ticketSeqLength ) );

            // The tkt-vno Tag and value
            buffer.put( ( byte )KerberosConstants.TICKET_TKT_VNO_TAG );
            buffer.put( TLV.getBytes( tktvnoLength ) );
            Value.encode( buffer, getProtocolVersionNumber() );

            // The realm Tag and value
            buffer.put( ( byte )KerberosConstants.TICKET_REALM_TAG );
            buffer.put( TLV.getBytes( realmLength ) );
            buffer.put( UniversalTag.GENERAL_STRING.getValue() );
            buffer.put( TLV.getBytes( realmBytes.length ) );
            buffer.put( realmBytes );

            // The sname Tag and value
            buffer.put( ( byte )KerberosConstants.TICKET_SNAME_TAG );
            buffer.put( TLV.getBytes( sNameLength ) );
            sName.encode( buffer );
            
            // The encPartLength Tag and value
            buffer.put( ( byte )KerberosConstants.TICKET_ENC_PART_TAG );
            buffer.put( TLV.getBytes( encPartLength ) );
            encPart.encode( buffer );
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_137, 1 + TLV.getNbBytes( ticketLength ) + ticketLength, 
                buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "Ticket encoding : {}", Strings.dumpBytes(buffer.array()) );
            LOG.debug( "Ticket initial value : {}", toString() );
        }

        return buffer;
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( encPart == null ) ? 0 : encPart.hashCode() );
        result = prime * result + ( ( realm == null ) ? 0 : realm.hashCode() );
        result = prime * result + ( ( sName == null ) ? 0 : sName.hashCode() );
        return result;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        
        if ( obj == null )
        {
            return false;
        }
        
        Ticket other = ( Ticket ) obj;
        
        if ( encPart == null )
        {
            if ( other.encPart != null )
            {
                return false;
            }
        }
        else if ( !encPart.equals( other.encPart ) )
        {
            return false;
        }
        
        if ( realm == null )
        {
            if ( other.realm != null )
            {
                return false;
            }
        }
        else if ( !realm.equals( other.realm ) )
        {
            return false;
        }
        
        if ( sName == null )
        {
            if ( other.sName != null )
            {
                return false;
            }
        }
        else if ( !sName.equals( other.sName ) )
        {
            return false;
        }
        
        return true;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "Ticket :\n" );
        sb.append( "  tkt-vno : " ).append( getProtocolVersionNumber() ).append( "\n" );
        sb.append( "  realm : " ).append( realm ).append( "\n" );
        sb.append( "  sname : " ).append( sName ).append( "\n" );
        sb.append( "  enc-part : " ).append( encPart ).append( "\n" );
        
        return sb.toString();
    }
}
