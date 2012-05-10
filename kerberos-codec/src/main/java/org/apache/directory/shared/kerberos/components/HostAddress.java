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
package org.apache.directory.shared.kerberos.components;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.BerValue;
import org.apache.directory.shared.kerberos.codec.types.HostAddrType;
import org.apache.directory.shared.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides host address information.
 * 
 * The ASN.1 grammar for this structure is :
 * <pre>
 * HostAddress     ::= SEQUENCE  {
 *        addr-type       [0] Int32,
 *        address         [1] OCTET STRING
 * }
 * </pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
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
    private int addrTypeLength;
    private int addressLength;
    private int hostAddressLength;
    private int hostAddressSeqLength;


    /**
     * Creates an empty HostAdress instance
     */
    public HostAddress()
    {
    }


    /**
     * Creates a new instance of HostAddress.
     *
     * @param addrType The type of address
     * @param address The address
     */
    public HostAddress( HostAddrType addrType, byte[] address )
    {
        this.addrType = addrType;
        this.address = address;
    }


    /**
     * Creates a new instance of HostAddress.
     *
     * @param internetAddress The Inet form address
     */
    public HostAddress( InetAddress internetAddress )
    {
        addrType = HostAddrType.ADDRTYPE_INET;
        byte[] newAddress = internetAddress.getAddress();
        address = new byte[newAddress.length];
        System.arraycopy( newAddress, 0, address, 0, newAddress.length );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int hash = 37;
        hash = hash * 17 + addrType.hashCode();

        if ( address != null )
        {
            hash = hash * 17 + Arrays.hashCode( address );
        }

        return hash;
    }


    /**
     * Returns whether one {@link HostAddress} is equal to another.
     *
     * @param that The {@link HostAddress} to compare with
     * @return true if the {@link HostAddress}'s are equal.
     */
    @Override
    public boolean equals( Object that )
    {
        if ( this == that )
        {
            return true;
        }

        if ( !( that instanceof HostAddress ) )
        {
            return false;
        }

        HostAddress hostAddress = ( HostAddress ) that;

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
     * <pre>
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
     *  </pre>
     */
    public int computeLength()
    {
        // Compute the keyType. The Length will always be contained in 1 byte
        addrTypeLength = 1 + 1 + BerValue.getNbBytes( addrType.getValue() );
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
        hostAddressSeqLength = 1 + BerValue.getNbBytes( hostAddressLength ) + hostAddressLength;

        return hostAddressSeqLength;
    }


    /**
     * Encode the HostAddress message to a PDU. 
     * <pre>
     * HostAddress :
     * 
     * 0x30 LL
     *   0xA0 LL 
     *     0x02 0x01 addr-type
     *   0xA1 LL 
     *     0x04 LL address
     * </pre>
     * @param buffer The buffer where to put the PDU. It should have been allocated
     * before, with the right size.
     * @return The constructed PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_148 ) );
        }

        try
        {
            // The HostAddress SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
            buffer.put( TLV.getBytes( hostAddressLength ) );

            // The addr-type, first the tag, then the value
            buffer.put( ( byte ) 0xA0 );
            buffer.put( TLV.getBytes( addrTypeLength ) );
            BerValue.encode( buffer, addrType.getValue() );

            // The address, first the tag, then the value
            buffer.put( ( byte ) 0xA1 );
            buffer.put( TLV.getBytes( addressLength ) );
            BerValue.encode( buffer, address );
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error( I18n.err( I18n.ERR_143, 1 + TLV.getNbBytes( hostAddressLength )
                + hostAddressLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "Checksum encoding : {}", Strings.dumpBytes( buffer.array() ) );
            LOG.debug( "Checksum initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * Returns the {@link HostAddrType} of this {@link HostAddress}.
     *
     * @return The {@link HostAddrType}.
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
     * {@inheritDoc}
     */
    public String toString()
    {
        try
        {
            return InetAddress.getByAddress( address ).getHostAddress();
        }
        catch ( UnknownHostException uhe )
        {
            return "Unknow host : " + Strings.utf8ToString( address );
        }
    }
}
