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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.server.kerberos.shared.messages.value.types.HostAddressType;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides host address information.
 * 
 * The ASN.1 grammaor for this structure is :
 * 
 * HostAddress     ::= SEQUENCE  {
 *        addr-type       [0] Int32,
 *        address         [1] OCTET STRING
 * }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class HostAddress extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( HostAddress.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The host address type. One of :
     *    Address Type                   Value
     *
     *    IPv4                             2
     *    Directional                      3
     *    ChaosNet                         5
     *    XNS                              6
     *    ISO                              7
     *    DECNET Phase IV                 12
     *    AppleTalk DDP                   16
     *    NetBios                         20
     *    IPv6                            24
     */
    private HostAddressType addressType;

    /** The address */
    private byte[] address;

    // Storage for computed lengths
    private transient int addressTypeLength;
    private transient int addressLength;
    private transient int hostAddressLength;
    private transient int hostAddressSeqLength;


    /**
     * Creates a new instance of HostAddress.
     *
     * @param addressType
     * @param address
     */
    public HostAddress( HostAddressType addressType, byte[] address )
    {
        this.addressType = addressType;
        this.address = address;
    }


    /**
     * Creates a new instance of HostAddress.
     *
     * @param internetAddress
     */
    public HostAddress( InetAddress internetAddress )
    {
        addressType = HostAddressType.ADDRTYPE_INET;
        byte[] newAddress = internetAddress.getAddress();
        address = new byte[newAddress.length];
        System.arraycopy( newAddress, 0, address, 0, newAddress.length );
    }


    /**
     * Returns whether one {@link HostAddress} is equal to another.
     *
     * @param that
     * @return true if the {@link HostAddress}'s are equal.
     */
    public boolean equals( HostAddress that )
    {
        if ( this.addressType != that.addressType || ( this.address != null && that.address == null )
            || ( this.address == null && that.address != null ) )
        {
            return false;
        }

        if ( this.address != null && that.address != null )
        {
            if ( this.address.length != that.address.length )
            {
                return false;
            }

            for ( int ii = 0; ii < this.address.length; ii++ )
            {
                if ( this.address[ii] != that.address[ii] )
                {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * Get the bytes for this address.
     *
     * @return The bytes of this address.
     */
    public byte[] getAddress()
    {
        return address;
    }


    /**
     * Compute the host address length
     * 
     * HostAddress :
     * 
     * 0x30 L1 hostAddress sequence
     *  |
     *  +--> 0xA0 L2 addressType tag
     *  |     |
     *  |     +--> 0x02 L2-1 addressType (int)
     *  |
     *  +--> 0xA1 L3 address tag
     *        |
     *        +--> 0x04 L3-1 address (OCTET STRING)
     *        
     *  where L1 = L2 + lenght(0xA0) + length(L2) +
     *             L3 + lenght(0xA1) + length(L3) 
     *  and
     *  L2 = L2-1 + length(0x02) + length( L2-1) 
     *  L3 = L3-1 + length(0x04) + length( L3-1) 
     */
    public int computeLength()
    {
        // Compute the keyType. The Length will always be cobntained in 1 byte
        addressTypeLength = 1 + 1 + Value.getNbBytes( addressType.getOrdinal() );
        hostAddressLength = 1 + TLV.getNbBytes( addressTypeLength ) + addressTypeLength;

        // Compute the keyValue
        if ( address == null )
        {
            addressLength = 1 + 1;
        }
        else
        {
            addressLength = 1 + TLV.getNbBytes( address.length ) + address.length;
        }

        hostAddressLength += 1 + TLV.getNbBytes( addressLength ) + addressLength;

        // Compute the whole sequence length
        hostAddressSeqLength = 1 + Value.getNbBytes( hostAddressLength ) + hostAddressLength;

        return hostAddressSeqLength;
    }


    /**
     * Encode the HostAddress message to a PDU. 
     * 
     * HostAddress :
     * 
     * 0x30 LL
     *   0xA0 LL 
     *     0x02 0x01 addr-type
     *   0xA1 LL 
     *     0x04 LL address
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
            // The HostAddress SEQ Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( hostAddressLength ) );

            // The addr-type, first the tag, then the value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( addressTypeLength ) );
            Value.encode( buffer, addressType.getOrdinal() );

            // The address, first the tag, then the value
            buffer.put( ( byte ) 0xA1 );
            buffer.put( TLV.getBytes( addressLength ) );
            Value.encode( buffer, address );
        }
        catch ( BufferOverflowException boe )
        {
            log.error(
                "Cannot encode the HostAddress object, the PDU size is {} when only {} bytes has been allocated", 1
                    + TLV.getNbBytes( hostAddressLength ) + hostAddressLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "Checksum encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "Checksum initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * Returns the {@link HostAddressType} of this {@link HostAddress}.
     *
     * @return The {@link HostAddressType}.
     */
    public HostAddressType getAddressType()
    {
        return addressType;
    }


    public String toString()
    {
        String result = "";

        try
        {
            result = InetAddress.getByAddress( address ).getHostAddress();
        }
        catch ( UnknownHostException uhe )
        {
            // Allow default to return.
        }

        return result;
    }
}
