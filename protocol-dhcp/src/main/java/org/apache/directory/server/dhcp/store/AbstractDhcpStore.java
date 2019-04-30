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
import java.util.Map;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.vendor.HostName;
import org.apache.directory.server.dhcp.options.vendor.SubnetMask;
import org.apache.directory.server.dhcp.service.Lease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract base implementation of a {@link DhcpStore}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractDhcpStore implements DhcpStore
{
    private static final Logger LOG = LoggerFactory.getLogger( AbstractDhcpStore.class );


    /*
     * @see org.apache.directory.server.dhcp.service.DhcpStore#getLeaseOffer(org.apache.directory.server.dhcp.messages.HardwareAddress,
     *      java.net.InetAddress, java.net.InetAddress, long,
     *      org.apache.directory.server.dhcp.options.OptionsField)
     */
    public Lease getLeaseOffer( HardwareAddress hardwareAddress, InetAddress requestedAddress,
        InetAddress selectionBase, long requestedLeaseTime, OptionsField options ) throws DhcpException
    {
        Subnet subnet = findSubnet( selectionBase );

        if ( null == subnet )
        {
            LOG.warn( "Don't know anything about the sbnet containing {}", selectionBase );
            return null;
        }

        // try to find existing lease
        Lease lease = null;
        lease = findExistingLease( hardwareAddress, lease );

        if ( null != lease )
        {
            return lease;
        }

        Host host = null;
        host = findDesignatedHost( hardwareAddress );

        if ( null != host )
        {
            // make sure that the host is actually within the subnet. Depending
            // on the way the DhcpStore configuration is implemented, it is not
            // possible to violate this condition, but we can't be sure.
            if ( !subnet.contains( host.getAddress() ) )
            {
                LOG.warn( "Host {} is not within the subnet for which an address is requested", host );
            }
            else
            {
                // build properties map
                Map properties = getProperties( subnet );
                properties.putAll( getProperties( host ) );

                // build lease
                lease = new Lease();
                lease.setAcquired( System.currentTimeMillis() );

                long leaseTime = determineLeaseTime( requestedLeaseTime, properties );

                lease.setExpires( System.currentTimeMillis() + leaseTime );

                lease.setHardwareAddress( hardwareAddress );
                lease.setState( Lease.STATE_NEW );
                lease.setClientAddress( host.getAddress() );

                // set lease options
                OptionsField o = lease.getOptions();

                // set (client) host name
                o.add( new HostName( host.getName() ) );

                // add subnet settings
                o.add( new SubnetMask( subnet.getNetmask() ) );
                o.merge( subnet.getOptions() );

                // add the host's options. they override existing
                // subnet options as they take the precedence.
                o.merge( host.getOptions() );
            }
        }

        // update the lease state
        if ( null != lease && lease.getState() != Lease.STATE_ACTIVE )
        {
            lease.setState( Lease.STATE_OFFERED );
            updateLease( lease );
        }

        return lease;
    }


    /*
     * @see org.apache.directory.server.dhcp.store.DhcpStore#getExistingLease(org.apache.directory.server.dhcp.messages.HardwareAddress,
     *      java.net.InetAddress, java.net.InetAddress, long,
     *      org.apache.directory.server.dhcp.options.OptionsField)
     */
    public Lease getExistingLease( HardwareAddress hardwareAddress, InetAddress requestedAddress,
        InetAddress selectionBase, long requestedLeaseTime, OptionsField options ) throws DhcpException
    {
        // try to find existing lease. if we don't find a lease based on the
        // client's
        // hardware address, we send a NAK.
        Lease lease = null;
        lease = findExistingLease( hardwareAddress, lease );

        if ( null == lease )
        {
            return null;
        }

        // check whether the notions of the client address match
        if ( !lease.getClientAddress().equals( requestedAddress ) )
        {
            LOG.warn( "Requested address " + requestedAddress + " for " + hardwareAddress
                + " doesn't match existing lease " + lease );
            return null;
        }

        // check whether addresses and subnet match
        Subnet subnet = findSubnet( selectionBase );

        if ( null == subnet )
        {
            LOG.warn( "No subnet found for existing lease {}", lease );
            return null;
        }

        if ( !subnet.contains( lease.getClientAddress() ) )
        {
            LOG.warn( "Client with existing lease {} is on wrong subnet {}", lease, subnet );
            return null;
        }

        if ( !subnet.isInRange( lease.getClientAddress() ) )
        {
            LOG.warn( "Client with existing lease {} is out of valid range for subnet {}", lease, subnet );
            return null;
        }

        // build properties map
        Map properties = getProperties( subnet );

        // update lease options
        OptionsField o = lease.getOptions();
        o.clear();

        // add subnet settings
        o.add( new SubnetMask( subnet.getNetmask() ) );
        o.merge( subnet.getOptions() );

        // check whether there is a designated host.
        Host host = findDesignatedHost( hardwareAddress );
        if ( null != host )
        {
            // check whether the host matches the address (using a fixed
            // host address is mandatory).
            if ( host.getAddress() != null && !host.getAddress().equals( lease.getClientAddress() ) )
            {
                LOG.warn( "Existing fixed address for " + hardwareAddress + " conflicts with existing lease "
                    + lease );
                return null;
            }

            properties.putAll( getProperties( host ) );

            // set (client) host name
            o.add( new HostName( host.getName() ) );

            // add the host's options
            o.merge( host.getOptions() );
        }

        // update other lease fields
        long leaseTime = determineLeaseTime( requestedLeaseTime, properties );
        lease.setExpires( System.currentTimeMillis() + leaseTime );
        lease.setHardwareAddress( hardwareAddress );

        // update the lease state
        if ( lease.getState() != Lease.STATE_ACTIVE )
        {
            lease.setState( Lease.STATE_ACTIVE );
            updateLease( lease );
        }

        // store information about the lease
        updateLease( lease );

        return lease;
    }


    /**
     * Determine the lease time based on the time requested by the client, the
     * properties and a global default.
     * 
     * @param requestedLeaseTime
     * @param properties
     * @return long
     */
    private long determineLeaseTime( long requestedLeaseTime, Map properties )
    {
        // built-in default
        long leaseTime = 1000L * 3600;
        Integer propMaxLeaseTime = ( Integer ) properties.get( DhcpConfigElement.PROPERTY_MAX_LEASE_TIME );

        if ( null != propMaxLeaseTime )
        {
            if ( requestedLeaseTime > 0 )
            {
                leaseTime = Math.min( propMaxLeaseTime.intValue() * 1000L, requestedLeaseTime );
            }
            else
            {
                leaseTime = propMaxLeaseTime.intValue() * 1000L;
            }
        }

        return leaseTime;
    }


    /*
     * @see org.apache.directory.server.dhcp.store.DhcpStore#releaseLease(org.apache.directory.server.dhcp.service.Lease)
     */
    public void releaseLease( Lease lease )
    {
        lease.setState( Lease.STATE_RELEASED );
        updateLease( lease );
    }


    /**
     * Update the (possibly changed) lease in the store.
     * 
     * @param lease
     */
    protected abstract void updateLease( Lease lease );


    /**
     * Return a list of all options applicable to the given config element. List
     * list must contain the options specified for the element and all parent
     * elements in an aggregated fashion. For instance, the options for a host
     * must include the global default options, the options of classes the host
     * is a member of, the host's group options and the host's options.
     * 
     * @param element
     * @return OptionsField
     */
    protected abstract OptionsField getOptions( DhcpConfigElement element );


    /**
     * Return a list of all options applicable to the given config element. List
     * list must contain the options specified for the element and all parent
     * elements in an aggregated fashion. For instance, the options for a host
     * must include the global default options, the options of classes the host
     * is a member of, the host's group options and the host's options.
     * 
     * @param element
     * @return Map
     */
    protected abstract Map getProperties( DhcpConfigElement element );


    /**
     * Find an existing lease in the store.
     * 
     * @param hardwareAddress
     * @param existingLease
     * @return Map
     */
    protected abstract Lease findExistingLease( HardwareAddress hardwareAddress, Lease existingLease );


    /**
     * Find a host to with the explicitely designated hardware address.
     * 
     * @param hardwareAddress
     * @return Host
     * @throws DhcpException
     */
    protected abstract Host findDesignatedHost( HardwareAddress hardwareAddress ) throws DhcpException;


    /**
     * Find the subnet definition matching the given address.
     * 
     * @param clientAddress
     * @return Subnet
     */
    protected abstract Subnet findSubnet( InetAddress clientAddress );
}
