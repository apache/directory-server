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
package org.apache.directory.server.dhcp.store;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;


/**
 * The definition of a Subnet.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Subnet extends DhcpConfigElement
{
    /** the subnet's address */
    private final InetAddress address;

    /** the subnet's netmask */
    private final InetAddress netmask;

    /** the subnet's range: minimum address in range */
    private InetAddress rangeMin;

    /** the subnet's range: maximum address in range */
    private InetAddress rangeMax;


    // This will suppress PMD.EmptyCatchBlock warnings in this method
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public Subnet( InetAddress address, InetAddress netmask, InetAddress rangeMin, InetAddress rangeMax )
    {
        // mask address to match subnet
        byte[] masked = netmask.getAddress();
        byte[] addrBytes = netmask.getAddress();

        for ( int i = 0; i < addrBytes.length; i++ )
        {
            masked[i] &= addrBytes[i];
        }

        if ( !Arrays.equals( masked, addrBytes ) )
        {
            try
            {
                address = InetAddress.getByAddress( masked );
            }
            catch ( UnknownHostException e )
            {
                // ignore - doesn't happen.
            }
        }

        this.address = address;
        this.netmask = netmask;
        this.rangeMin = rangeMin;
        this.rangeMax = rangeMax;
    }


    public InetAddress getAddress()
    {
        return address;
    }


    public InetAddress getNetmask()
    {
        return netmask;
    }


    public InetAddress getRangeMax()
    {
        return rangeMax;
    }


    public void setRangeMax( InetAddress rangeMax )
    {
        this.rangeMax = rangeMax;
    }


    public InetAddress getRangeMin()
    {
        return rangeMin;
    }


    public void setRangeMin( InetAddress rangeMin )
    {
        this.rangeMin = rangeMin;
    }


    /**
     * Check whether the given client address resides within this subnet and
     * possibly range.
     * 
     * @param clientAddress
     * @return boolean
     */
    public boolean contains( InetAddress clientAddress )
    {
        // check address type
        if ( !clientAddress.getClass().equals( address.getClass() ) )
        {
            return false;
        }

        byte[] client = clientAddress.getAddress();
        byte[] masked = netmask.getAddress();

        for ( int i = 0; i < masked.length; i++ )
        {
            masked[i] &= client[i];
        }

        return Arrays.equals( masked, address.getAddress() );
    }


    /**
     * Check whether the specified address is within the range for this subnet.
     * 
     * @param clientAddress
     * @return boolean
     */
    public boolean isInRange( InetAddress clientAddress )
    {
        byte[] client = clientAddress.getAddress();
        byte[] masked = netmask.getAddress();

        for ( int i = 0; i < masked.length; i++ )
        {
            masked[i] &= client[i];
        }

        if ( null != rangeMin && arrayComp( masked, rangeMin.getAddress() ) < 0 )
        {
            return false;
        }

        return ( null == rangeMin || arrayComp( masked, rangeMax.getAddress() ) <= 0 );
    }


    private static int arrayComp( byte[] a1, byte[] a2 )
    {
        for ( int i = 0; i < a1.length && i < a2.length; i++ )
        {
            if ( a1[i] != a2[i] )
            {
                return ( a1[i] & 0xff ) - ( a2[i] & 0xff );
            }
        }

        return a1.length - a2.length;
    }
}
