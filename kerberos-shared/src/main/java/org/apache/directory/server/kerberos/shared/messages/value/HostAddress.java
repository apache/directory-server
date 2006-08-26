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
 */
public class HostAddress
{
    private HostAddressType addressType;
    private byte[] address;


    /**
     * Class constructors
     */
    public HostAddress(HostAddressType addressType, byte[] address)
    {
        this.addressType = addressType;
        this.address = address;
    }


    public HostAddress(InetAddress internetAddress)
    {
        addressType = HostAddressType.ADDRTYPE_INET;
        byte[] newAddress = internetAddress.getAddress();
        address = new byte[newAddress.length];
        System.arraycopy( newAddress, 0, address, 0, newAddress.length );
    }


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


    public byte[] getAddress()
    {
        return address;
    }


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
