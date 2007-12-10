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


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Store a list of addresses.
 * 
 * The ASN.1 grammar is :
 * 
 * -- NOTE: HostAddresses is always used as an OPTIONAL field and
 * -- should not be empty.
 * HostAddresses   -- NOTE: subtly different from rfc1510,
 *                 -- but has a value mapping and encodes the same
 *         ::= SEQUENCE OF HostAddress
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class HostAddresses extends AbstractAsn1Object
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( HostAddresses.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** List of all HostAddress stored */
    private List<HostAddress> addresses;

    // Storage for computed lengths
    private transient int addressesLength;


    /**
     * Creates a new instance of HostAddresses.
     *
     * @param addresses
     */
    public HostAddresses()
    {
        this.addresses = new ArrayList<HostAddress>();
    }
    

    /**
     * Creates a new instance of HostAddresses.
     *
     * @param addresses The associated addresses
     */
    public HostAddresses( HostAddress[] addresses )
    {
        if ( addresses == null )
        {
            this.addresses = new ArrayList<HostAddress>();
        }
        else
        {
            this.addresses = Arrays.asList( addresses );
        }
    }


    public void addHostAddress( HostAddress hostAddress )
    {
        addresses.add( hostAddress );
    }


    /**
     * Returns true if this {@link HostAddresses} contains a specified {@link HostAddress}.
     *
     * @param address
     * @return true if this {@link HostAddresses} contains a specified {@link HostAddress}.
     */
    public boolean contains( HostAddress address )
    {
        if ( addresses != null )
        {
            return addresses.contains( address );
        }

        return false;
    }


    /**
     * Returns true if two {@link HostAddresses} are equal.
     *
     * @param that
     * @return true if two {@link HostAddresses} are equal.
     */
    public boolean equals( HostAddresses that )
    {
        if ( ( addresses == null && that.addresses != null )
            || ( addresses != null && that.addresses == null ) )
        {
            return false;
        }

        if ( addresses != null && that.addresses != null )
        {
            if ( addresses.size() != that.addresses.size() )
            {
                return false;
            }

            HostAddress[] thisHostAddresses = ( HostAddress[] ) addresses.toArray();
            HostAddress[] thatHostAddresses = ( HostAddress[] ) that.addresses.toArray();

            for ( int i = 0; i < thisHostAddresses.length; i++ )
            {
                if ( !thisHostAddresses[i].equals( thatHostAddresses[i] ) )
                {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * Returns the contained {@link HostAddress}s as an array.
     *
     * @return An array of {@link HostAddress}s.
     */
    public HostAddress[] getAddresses()
    {
        return ( HostAddress[] ) addresses.toArray();
    }


    /**
     * Compute the hostAddresses length
     * 
     * HostAddresses :
     * 
     * 0x30 L1 hostAddresses sequence of HostAddresses
     *  |
     *  +--> 0x30 L2[1] Hostaddress[1]
     *  |
     *  +--> 0x30 L2[2] Hostaddress[2]
     *  |
     *  ...
     *  |
     *  +--> 0x30 L2[n] Hostaddress[n]
     *        
     *  where L1 = sum( L2[1], l2[2], ..., L2[n] )
     */
    public int computeLength()
    {
        // Compute the addresses length.
        addressesLength = 0;

        if ( ( addresses != null ) && ( addresses.size() != 0 ) )
        {
            for ( HostAddress hostAddress : addresses )
            {
                int length = hostAddress.computeLength();
                addressesLength += length;
            }
        }

        return 1 + TLV.getNbBytes( addressesLength ) + addressesLength;
    }


    /**
     * Encode the HostAddress message to a PDU. 
     * 
     * HostAddress :
     * 
     * 0x30 LL
     *   0x30 LL hostaddress[1] 
     *   0x30 LL hostaddress[1]
     *   ... 
     *   0x30 LL hostaddress[1] 
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
            // The HostAddresses SEQ Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( addressesLength ) );

            // The hostAddress list, if it's not empty
            if ( ( addresses != null ) && ( addresses.size() != 0 ) )
            {
                for ( HostAddress hostAddress : addresses )
                {
                    hostAddress.encode( buffer );
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            LOG.error(
                "Cannot encode the HostAddresses object, the PDU size is {} when only {} bytes has been allocated", 1
                    + TLV.getNbBytes( addressesLength ) + addressesLength, buffer.capacity() );
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "HostAddresses encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            LOG.debug( "HostAddresses initial value : {}", toString() );
        }

        return buffer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        for ( HostAddress hostAddress : addresses )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }

            sb.append( hostAddress.toString() );
        }

        return sb.toString();
    }
}
