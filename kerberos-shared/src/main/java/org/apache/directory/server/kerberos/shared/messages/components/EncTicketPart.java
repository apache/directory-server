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

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.server.kerberos.shared.messages.value.AuthorizationData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddress;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalName;
import org.apache.directory.server.kerberos.shared.messages.value.TransitedEncoding;
import org.apache.directory.server.kerberos.shared.messages.value.flags.TicketFlags;
import org.apache.directory.server.kerberos.shared.messages.value.flags.TicketFlag;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.primitives.BitString;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Encrypted part of Tickets.
 * 
 * The ASN.1 grammar used to describe tis structure is the following :
 * 
 * EncTicketPart   ::= [APPLICATION 3] SEQUENCE {
 *       flags                   [0] TicketFlags,
 *       key                     [1] EncryptionKey,
 *       crealm                  [2] Realm,
 *       cname                   [3] PrincipalName,
 *       transited               [4] TransitedEncoding,
 *       authtime                [5] KerberosTime,
 *       starttime               [6] KerberosTime OPTIONAL,
 *       endtime                 [7] KerberosTime,
 *       renew-till              [8] KerberosTime OPTIONAL,
 *       caddr                   [9] HostAddresses OPTIONAL,
 *       authorization-data      [10] AuthorizationData OPTIONAL
 * }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EncTicketPart extends AbstractAsn1Object implements Encodable
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( EncTicketPart.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();
    
    /** The ticket flags */
    private TicketFlags flags;
    
    /** The session key */
    private EncryptionKey key;
    
    /** The client realm */
    private String cRealm;
    
    /** A byte array to store the client realm */
    private transient byte[] cRealmBytes;
    
    /** The client principalName */
    private PrincipalName cName;
    
    /** The client KerberosPrincipal */
    private KerberosPrincipal clientPrincipal;
    
    /** 
     * The names of the Kerberos realms that took part
     * in authenticating the user to whom this ticket was issued
     */
    private TransitedEncoding transited;
    
    /** The time of initial authentication for the named principal */
    private KerberosTime authTime;
    
    /** The time after which the ticket is valid */ 
    private KerberosTime startTime; //optional
    
    /** The time after which the ticket will not be honored */
    private KerberosTime endTime;
    
    /** The maximum endtime that may be included in a renewal */
    private KerberosTime renewTill; //optional
    
    /** The addresses from which the ticket can be used */
    private HostAddresses caddr; //optional
    
    /** 
     * used to pass authorization data from the principal on 
     * whose behalf a ticket was issued to the application service
     */
    private AuthorizationData authorizationData; //optional

    // Storage for computed lengths
    private transient int encTicketPartAppLength;
    private transient int encTicketPartSeqLength;
    
    private transient int keyTagLength;
    
    private transient int flagsTagLength;
    private transient int flagsLength;

    private transient int realmTagLength;
    
    private transient int cNameTagLength;
    
    private transient int authTimeTagLength;
    private transient int authTimeLength;

    private transient int startTimeTagLength;
    private transient int startTimeLength;

    private transient int endTimeTagLength;
    private transient int endTimeLength;

    private transient int renewTillTagLength;
    private transient int renewTillLength;


    /**
     * Creates a new instance of EncTicketPart.
     */
    public EncTicketPart()
    {
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
     * @param data The authorization data
     */
    public void setAuthorizationData( AuthorizationData data )
    {
        authorizationData = data;
    }


    /**
     * Returns the auth {@link KerberosTime}
     *
     * @return The auth {@link KerberosTime}
     */
    public KerberosTime getAuthTime()
    {
        return authTime;
    }


    /**
     * Sets the auth {@link KerberosTime}.
     *
     * @param authtime
     */
    public void setAuthTime( KerberosTime authtime )
    {
        this.authTime = authtime;
    }


    /**
     * Returns the client {@link HostAddresses}.
     *
     * @return The client {@link HostAddresses}.
     */
    public HostAddresses getClientAddresses()
    {
        return caddr;
    }


    /**
     * Sets the client {@link HostAddresses}.
     *
     * @param addresses The client addresses
     */
    public void setClientAddresses( HostAddresses addresses )
    {
        this.caddr = addresses;
    }


    /**
     * Add a client {@link HostAddresses}.
     *
     * @param addresses The client address to add
     */
    public void addClientAddresses( HostAddress hostAddress )
    {
        caddr.addHostAddress( hostAddress );
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
     * Returns the client {@link PrincipalName}.
     *
     * @return The client {@link PrincipalName}.
     */
    public KerberosPrincipal getClientPrincipal()
    {
        return clientPrincipal;
    }

    
    /**
     * Sets the client {@link KerberosPrincipal}.
     *
     * @param clientPrincipal
     */
    public void setClientPrincipal( KerberosPrincipal clientPrincipal ) throws ParseException
    {
        this.cName = new PrincipalName( clientPrincipal );
        this.clientPrincipal = clientPrincipal;
        this.cRealm = clientPrincipal.getRealm();
    }
    
    /**
     * Sets the client {@link PrincipalName}.
     *
     * @param cName The principalName
     */
    public void setClientPrincipalName( PrincipalName cName ) throws ParseException
    {
        this.cName = cName;
    }
    
    /**
     * Returns the client realm.
     *
     * @return The client realm.
     */
    public String getClientRealm()
    {
        return cRealm;
    }


    /**
     * Sets the client realm.
     *
     * @param realm The client realm
     */
    public void setClientRealm( String cRealm )
    {
        this.cRealm = cRealm;
    }
    
    /**
     * Returns the end {@link KerberosTime}
     *
     * @return The end {@link KerberosTime}
     */
    public KerberosTime getEndTime()
    {
        return endTime;
    }


    /**
     * Sets the end {@link KerberosTime}.
     *
     * @param time The ending time
     */
    public void setEndTime( KerberosTime time )
    {
        endTime = time;
    }

    
    /**
     * Returns the TicketFlags.
     *
     * @return The  TicketFlags.
     */
    public TicketFlags getFlags()
    {
        return flags;
    }


    /**
     * get a TicketFlags.
     *
     * @param flag The flag to set
     */
    public boolean getFlag( TicketFlag flag )
    {
        return flags.isFlagSet( flag );
    }


    /**
     * Clear a TicketFlags.
     *
     * @param flag The flag to clear
     */
    public void clearFlag( TicketFlag flag )
    {
        flags.clearFlag( flag );
    }


    /**
     * Sets the TicketFlags.
     *
     * @param flags
     */
    public void setFlags( int flags )
    {
        this.flags = new TicketFlags( flags );
    }


    /**
     * Sets the TicketFlags.
     *
     * @param flags
     */
    public void setFlags( TicketFlags flags )
    {
        this.flags = flags;
    }


    /**
     * Sets the specified flag
     *
     * @param flag The flag to be set
     */
    public void setFlag( TicketFlag flag )
    {
        flags.setFlag( flag );
    }

    
    /**
     * Sets the flag at the given index.
     *
     * @param flag The flag to be set
     */
    public void setFlag( int flag )
    {
        flags.setFlag( flag );
    }

    
    
    /**
     * Returns the session {@link EncryptionKey}.
     *
     * @return The session {@link EncryptionKey}.
     */
    public EncryptionKey getSessionKey()
    {
        return key;
    }


    /**
     * Sets the session {@link EncryptionKey}.
     *
     * @param key The session key
     */
    public void setSessionKey( EncryptionKey key )
    {
        this.key = key;
    }


    /**
     * Returns the renew till {@link KerberosTime}
     *
     * @return The renew till {@link KerberosTime}
     */
    public KerberosTime getRenewTill()
    {
        return renewTill;
    }


    /**
     * Sets the renew till {@link KerberosTime}.
     *
     * @param till The renew time
     */
    public void setRenewTill( KerberosTime till )
    {
        renewTill = till;
    }


    /**
     * Returns the start {@link KerberosTime}
     *
     * @return The start {@link KerberosTime}
     */
    public KerberosTime getStartTime()
    {
        return startTime;
    }

    
    /**
     * Sets the start {@link KerberosTime}.
     *
     * @param time The starting time
     */
    public void setStartTime( KerberosTime time )
    {
        startTime = time;
    }
    

    /**
     * Returns the {@link TransitedEncoding}.
     *
     * @return The {@link TransitedEncoding}.
     */
    public TransitedEncoding getTransitedEncoding()
    {
        return transited;
    }

    
    /**
     * Set the {@link TransitedEncoding}.
     */
    public void setTransitedEncoding( TransitedEncoding transited )
    {
        this.transited = transited;
    }

    
    /**
     * Compute the EncTicketPart length
     * 
     * EncTicketPart :
     * 
     * 0x63 L1 EncTicketPart
     *  |
     *  +--> 0x30 L2 EncTicketPart SEQUENCE
     *        |
     *        +--> 0xA0 L3 flags tag
     *        |     |
     *        |     +--> 0x05 L3-1 flags (bitstring)
     *        |
     *        +--> 0xA1 L4 key (EncryptionKey)
     *        |
     *        +--> 0xA2 L5 crealm tag
     *        |     |
     *        |     +--> 0x1B L5-1 crealm (generalizedString)
     *        |
     *        +--> 0xA3 L6 cname (PrincipalName)
     *        |
     *        +--> 0xA4 L7 transited (TransitedEncoding)
     *        |
     *        +--> 0xA5 L8 authtime tag
     *        |     |
     *        |     +--> 0x18 L8-1 authtime (generalizedTime)
     *        |
     *        +--> [0xA6 L9 starttime tag
     *        |     |
     *        |     +--> 0x18 L9-1 starttime (generalizedTime)]
     *        |
     *        +--> 0xA7 L10 endtime tag
     *        |     |
     *        |     +--> 0x18 L10-1 endtime (generalizedTime)
     *        |
     *        +--> [0xA8 L11 renew-till tag
     *        |     |
     *        |     +--> 0x18 L11-1 renew-till (generalizedTime)]
     *        |
     *        +--> [0xA9 L12 caddr:HostAddresses]
     *        |
     *        +--> [0xAA L13 authorization-data:AuthorizationData]
     */
    public int computeLength()
    {
        // The flags size (always 0x01 0x05 b1 b2 b3 b4 b5)
        flagsLength = 5;
        flagsTagLength = 1 + TLV.getNbBytes( flagsLength ) + flagsLength;
        encTicketPartSeqLength = 1 + TLV.getNbBytes( flagsTagLength ) + flagsTagLength; 
    	
    	// The encryption key is computed in its own class
        keyTagLength = key.computeLength();
    	encTicketPartSeqLength += 1 + TLV.getNbBytes( keyTagLength ) + keyTagLength;
        
        // The client Realm
        cRealmBytes = StringTools.getBytesUtf8( cRealm );
        realmTagLength = 1 + TLV.getNbBytes( cRealmBytes.length ) + cRealmBytes.length;
        encTicketPartSeqLength += 1 + TLV.getNbBytes( realmTagLength ) + realmTagLength;

        // The clientPrincipal length
        cNameTagLength = cName.computeLength();
        encTicketPartSeqLength += 1 + TLV.getNbBytes( cNameTagLength ) + cNameTagLength;
        
    	// The transited part
        encTicketPartSeqLength += transited.computeLength();
        
        // Compute the authTime length
        authTimeLength = 15;
        authTimeTagLength = 1 + 1 + authTimeLength;
        encTicketPartSeqLength += 
            1 + TLV.getNbBytes( authTimeTagLength ) + authTimeTagLength;
        
        // Compute the startTime length, if any
        if ( startTime != null )
        {
            startTimeLength = 15;
            startTimeTagLength = 1 + 1 + startTimeLength;
            encTicketPartSeqLength += 
                1 + TLV.getNbBytes( startTimeTagLength ) + startTimeTagLength;
        }
        
        // Compute the endTime length
        endTimeLength = 15;
        endTimeTagLength = 1 + 1 + endTimeLength;
        encTicketPartSeqLength += 
            1 + TLV.getNbBytes( endTimeTagLength ) + endTimeTagLength;
        
        // Compute the renew-till length, if any
        if ( renewTill != null )
        {
            renewTillLength = 15;
            renewTillTagLength = 1 + 1 + renewTillLength;
            encTicketPartSeqLength += 
                1 + TLV.getNbBytes( renewTillTagLength ) + renewTillTagLength;
        }
        
        // Compute the clientAddresses length, if any
        if ( caddr != null )
        {
            encTicketPartSeqLength += caddr.computeLength();
        }
        
        // Compute the authorizationData length, if any
        if ( authorizationData != null )
        {
            encTicketPartSeqLength += authorizationData.computeLength();
        }

        // compute the global size
        encTicketPartAppLength = 1 + TLV.getNbBytes( encTicketPartSeqLength ) + encTicketPartSeqLength;

        int result = 1 + TLV.getNbBytes( encTicketPartAppLength ) + encTicketPartAppLength;
        
        if ( IS_DEBUG )
        {
            log.debug( "EncTicketPart PDU length = {}", Integer.valueOf( result ) );
        }

        return result;
    }
    
    /**
     * Encode the EncTicketPart message to a PDU. 
     * 
     * EncTicketPart :
     * 
     * 0x63 LL
     *   0x30 LL 
     *     0xA0 LL 
     *       0x03 LL flags (BIT STRING)
     *   0xA1 LL
     *     0x30 LL key (EncryptionKey)
     *   0xA2 LL
     *     0x1B LL crealm (KerberosString)
     *   0xA3 LL
     *     0x30 LL cname (PrincipalName)
     *   0xA4 LL
     *     0x30 LL transited (TransitedEncoding)
     *   0xA5 11
     *     0x18 0x0F authtime (KerberosTime)
     *   [0xA6 11
     *     0x18 0x0F starttime (KerberosTime) (optional)]
     *   0xA7 11
     *     0x18 0x0F endtime (KerberosTime)
     *   [0xA8 11
     *     0x18 0x0F renew-till (KerberosTime) (optional)]
     *   [0xA9 LL
     *     0x30 LL addresses (HostAddresses) (optional)]
     *   [0xAA LL
     *     0x30 LL authorization-data (AuthorizationData) (optional)]
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            buffer = ByteBuffer.allocate( computeLength() );
        }

        try
        {
            // The encTicketPart SEQ Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( encTicketPartSeqLength ) );

            // The flags, first the tag, then the value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( flagsTagLength ) );

            // Th BIT STRING element
            Value.encode( buffer, flags );
            
            // The session key
            buffer.put( ( byte ) 0xA1 );
            buffer.put( TLV.getBytes( keyTagLength ) );
            key.encode( buffer );
            
            // The clientprincipalName, if any
            if ( cName != null )
            {
                buffer.put( (byte)0xA1 );
                buffer.put( TLV.getBytes( cNameTagLength ) );
                cName.encode( buffer );
            }
            
            // The server realm
            buffer.put( (byte)0xA2 );
            buffer.put( TLV.getBytes( realmTagLength ) );
            
            buffer.put( UniversalTag.GENERALIZED_STRING_TAG );
            buffer.put( TLV.getBytes( realmBytes.length ) );
            buffer.put( realmBytes );

            // The serverprincipalName, if any
            if ( sName != null )
            {
                buffer.put( (byte)0xA3 );
                buffer.put( TLV.getBytes( sNameTagLength ) );
                sName.encode( buffer );
            }
            
            // The from KerberosTime Tag and value, if any
            if ( from != null )
            {
                buffer.put( (byte)0xA4 );
                buffer.put( TLV.getBytes( fromTagLength ) );
                buffer.put( UniversalTag.GENERALIZED_TIME_TAG );
                buffer.put( TLV.getBytes( fromLength ) );
                buffer.put( StringTools.getBytesUtf8( from.toString() ) );
            }
            
            // The till KerberosTime Tag and value, if any
            buffer.put( (byte)0xA5 );
            buffer.put( TLV.getBytes( tillTagLength ) );
            buffer.put( UniversalTag.GENERALIZED_TIME_TAG );
            buffer.put( TLV.getBytes( tillLength ) );
            buffer.put( StringTools.getBytesUtf8( till.toString() ) );
            
            // The from KerberosTime Tag and value, if any
            if ( rTime != null )
            {
                buffer.put( (byte)0xA6 );
                buffer.put( TLV.getBytes( rTimeTagLength ) );
                buffer.put( UniversalTag.GENERALIZED_TIME_TAG );
                buffer.put( TLV.getBytes( rTimeLength ) );
                buffer.put( StringTools.getBytesUtf8( rTime.toString() ) );
            }
            
            // The nonce, first the tag, then the value
            buffer.put( ( byte ) 0xA7 );
            buffer.put( TLV.getBytes( nonceTagLength ) );
            Value.encode( buffer, nonce );
            
            // The EncryptionTypes
            if ( ( eType == null ) || ( eType.size() == 0 ) )
            {
                log.error( "We should have at least one encryption type" );
                throw new EncoderException( "No encryptionType available" );
            }
            
            // Fisrt, the tag
            buffer.put( (byte)0xA8 );
            buffer.put( TLV.getBytes( eTypeTagLength ) );
            
            // Then the sequence
            buffer.put( (byte)0x30 );
            buffer.put( TLV.getBytes( eTypeSeqLength ) );

            // Now, the eTypes
            for ( EncryptionType type:eType )
            {
                Value.encode( buffer, type.getOrdinal() );
            }
            
            // The addresses
            if ( addresses != null )
            {
                buffer.put( (byte)0xA9 );
                buffer.put( TLV.getBytes( addressesTagLength ) );
                addresses.encode( buffer );
            }
            
            // The enc-authorization-data
            if ( encAuthorizationData != null )
            {
                buffer.put( (byte)0xAA );
                buffer.put( TLV.getBytes( encAuthorizationDataTagLength ) );
                encAuthorizationData.encode( buffer );
            }
            
            // The additional tickets
            if ( additionalTickets != null )
            {
                buffer.put( (byte)0xAB );
                buffer.put( TLV.getBytes( additionalTicketsTagLength ) );
                
                buffer.put( UniversalTag.SEQUENCE_TAG );
                buffer.put( TLV.getBytes( additionalTicketsSeqLength ) );
                
                for ( Ticket ticket:additionalTickets )
                {
                    ticket.encode( buffer );
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error(
                "Cannot encode the EncTicketPart object, the PDU size is {} when only {} bytes has been allocated", 1
                    + TLV.getNbBytes( encTicketPartAppLength ) + encTicketPartAppLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "EncTicketPart encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "EncTicketPart initial value : {}", toString() );
        }

        return buffer;
    }
}
