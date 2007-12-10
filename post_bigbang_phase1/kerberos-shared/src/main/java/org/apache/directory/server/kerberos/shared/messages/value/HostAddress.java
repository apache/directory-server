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

import org.apache.directory.server.kerberos.shared.messages.value.types.HostAddrType;
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
    private static final Logger LOG = LoggerFactory.getLogger( HostAddress.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

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
    private HostAddrType addrType;

    /** The address */
    private byte[] address;

    // Storage for computed lengths
    private transient int addrTypeLength;
    private transient int addressLength;
    private transient int hostAddressLength;
    private transient int hostAddressSeqLength;


    /**
     * Creates a new instance of HostAddress.
     *
     * @param addrType
     * @param addr
     */
    public HostAddress( HostAddrType addrType, byte[] address )
    {
        this.addrType = addrType;
        this.address = address;
    }


    /**
     * Creates a new instance of HostAddress.
     *
     * @param internetAddress
     */
    public HostAddress( InetAddress internetAddress )
    {
        addrType = HostAddrType.ADDRTYPE_INET;
        byte[] newAddress = internetAddress.getAddress();
        address = new byte[newAddress.length];
        System.arraycopy( newAddress, 0, address, 0, newAddress.length );
    }


    /**
     * Returns whether one {@link HostAddress} is equal to another.
     *
     * @param that The {@link HostAddress} to compare with
     * @return true if the {@link HostAddress}'s are equal.
     */
    public boolean equals( Object that )
    {
        if ( this == that )
        {
            return true;
        }
        
        if ( !(that instanceof HostAddress ) )
        {
            return false;
        }
        
        HostAddress hostAddress = (HostAddress)that;
        
        if ( addrType != hostAddress.addrType || ( address != null && hostAddress.address == null )
            || ( address == null && hostAddress.address != null ) )
        {
            return false;
        }

        if ( address != null && hostAddress.address != null )
        {
            if ( address.length != hostAddress.address.length )
            {
                return false;
            }

            for ( int ii = 0; ii < address.length; ii++ )
            {
                if ( address[ii] != hostAddress.address[ii] )
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
     * Set the address 
     *
     * @param addresse The address
     */
    public void setAddress( byte[] addresse )
    {
        this.address = addresse;
    }


    /**
     * Compute the host address length
     * 
     * HostAddress :
     * 
     * 0x30 L1 hostAddress sequence
     *  |
     *  +--> 0xA0 L2 addrType tag
     *  |     |
     *  |     +--> 0x02 L2-1 addrType (int)
     *  |
     *  +--> 0xA1 L3 address tag
     *        |
     *        +--> 0x04 L3-1 address (OCTET STRING)
     *        
     *  where L1 = L2 + length(0xA0) + length(L2) +
     *             L3 + length(0xA1) + length(L3) 
     *  and
     *  L2 = L2-1 + length(0x02) + length( L2-1) 
     *  L3 = L3-1 + length(0x04) + length( L3-1) 
     */
    public int computeLength()
    {
        // Compute the keyType. The Length will always be contained in 1 byte
        addrTypeLength = 1 + 1 + Value.getNbBytes( addrType.getOrdinal() );
        hostAddressLength = 1 + TLV.getNbBytes( addrTypeLength ) + addrTypeLength;

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
            buffer.put( TLV.getBytes( addrTypeLength ) );
            Value.encode( buffer, addrType.getOrdinal() );

            // The address, first the tag, then the value
            buffer.put( ( byte ) 0xA1 );
            buffer.put( TLV.getBytes( addressLength ) );
            Value.encode( buffer, address );
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error(
                "Cannot encode the HostAddress object, the PDU size is {} when only {} bytes has been allocated", 1
                    + TLV.getNbBytes( hostAddressLength ) + hostAddressLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "Checksum encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            LOG.debug( "Checksum initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * Returns the {@link HostaddrType} of this {@link HostAddress}.
     *
     * @return The {@link HostaddrType}.
     */
    public HostAddrType getAddrType()
    {
        return addrType;
    }


    /**
     * Set the addr-type field
     *
     * @param addrType The address type
     */
    public void setAddrType( HostAddrType addrType )
    {
        this.addrType = addrType;
    }


    /**
     * Set the addr-type field
     *
     * @param addrType The address type
     */
    public void setAddrType( int addrType )
    {
        this.addrType = HostAddrType.getTypeByOrdinal( addrType );
    }


    /**
     * @see Object#toString()
     */
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
