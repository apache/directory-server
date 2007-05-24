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


/**
 * Provides host address information.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class HostAddress
{
    private HostAddressType addressType;
    private byte[] address;


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
        }

        return result;
    }
}
