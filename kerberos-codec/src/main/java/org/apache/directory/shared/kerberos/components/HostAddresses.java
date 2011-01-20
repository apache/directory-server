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


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Store a list of addresses.
 * 
 * The ASN.1 grammar is :
 * <pre>
 * -- NOTE: HostAddresses is always used as an OPTIONAL field and
 * -- should not be empty.
 * HostAddresses   -- NOTE: subtly different from rfc1510,
 *                 -- but has a value mapping and encodes the same
 *         ::= SEQUENCE OF HostAddress
 *</pre>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
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


    /**
     * Adds an {@link HostAddresses} to the list
     * @param hostAddress The address to add
     */
    public void addHostAddress( HostAddress hostAddress )
    {
        addresses.add( hostAddress );
    }


    /**
     * Returns true if this {@link HostAddresses} contains a specified {@link HostAddress}.
     *
     * @param address The address we are looking for in the existing list
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
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int hash = 37;
        
        if ( addresses != null )
        {
            hash = hash * 17 + addresses.size();
            hash = 17 + addresses.hashCode();
        }
        
        return hash;
    }


    /**
     * Returns true if two {@link HostAddresses} are equal.
     *
     * @param that Th {@link HostAddresses} we want to compare with the current one
     * @return true if two {@link HostAddresses} are equal.
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( obj == null ) 
        {
            return false;
        }
        
        HostAddresses that = ( HostAddresses ) obj;
        
        // Addresses can't be null after creation
        if ( addresses.size() != that.addresses.size() )
        {
            return false;
        }

        for ( int i = 0; i < addresses.size(); i++ )
        {
            if ( !addresses.get( i ).equals( that.addresses.get( i ) ) )
            {
                return false;
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
        return addresses.toArray( new HostAddress[0] );
    }


    /**
     * Compute the hostAddresses length
     * <pre>
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
     * </pre>
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
     * <pre>
     * HostAddress :
     * 
     * 0x30 LL
     *   0x30 LL hostaddress[1] 
     *   0x30 LL hostaddress[1]
     *   ... 
     *   0x30 LL hostaddress[1] 
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
            // The HostAddresses SEQ Tag
            buffer.put( UniversalTag.SEQUENCE.getValue() );
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
            LOG.error( I18n.err( I18n.ERR_144, 1 + TLV.getNbBytes( addressesLength )
                + addressesLength, buffer.capacity() ) );
            throw new EncoderException( I18n.err( I18n.ERR_138 ) );
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
