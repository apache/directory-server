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
package org.apache.directory.server.dhcp.service;


import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.DhcpMessage;
import org.apache.directory.server.dhcp.messages.MessageType;
import org.apache.directory.server.dhcp.options.AddressOption;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.dhcp.ClientIdentifier;
import org.apache.directory.server.dhcp.options.dhcp.IpAddressLeaseTime;
import org.apache.directory.server.dhcp.options.dhcp.MaximumDhcpMessageSize;
import org.apache.directory.server.dhcp.options.dhcp.ParameterRequestList;
import org.apache.directory.server.dhcp.options.dhcp.RequestedIpAddress;
import org.apache.directory.server.dhcp.options.dhcp.ServerIdentifier;
import org.apache.directory.server.dhcp.store.DhcpStore;


/**
 * A default implementation of the DHCP service. Does the tedious low-level
 * chores of handling DHCP messages, but delegates the lease-handling to a
 * supplied DhcpStore.
 * 
 * @see org.apache.directory.server.dhcp.store.DhcpStore
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreBasedDhcpService extends AbstractDhcpService
{
    private final DhcpStore dhcpStore;


    public StoreBasedDhcpService( DhcpStore dhcpStore )
    {
        this.dhcpStore = dhcpStore;
    }


    /**
     * Try to get an existing lease. The lease may have been created during
     * earlier DHCP negotiations or a recent DHCPDISCOVER.
     * 
     * @param clientAddress
     * @param request
     * @return
     * @throws DhcpException
     */
    private Lease getExistingLease( InetSocketAddress clientAddress, DhcpMessage request ) throws DhcpException
    {
        // determine requested lease time
        IpAddressLeaseTime requestedLeaseTimeOption = ( IpAddressLeaseTime ) request.getOptions().get(
            IpAddressLeaseTime.class );
        long requestedLeaseTime = null != requestedLeaseTimeOption ? requestedLeaseTimeOption.getIntValue() * 1000
            : -1L;

        // try to get the lease (address) requested by the client
        InetAddress requestedAddress = null;
        AddressOption requestedAddressOption = ( AddressOption ) request.getOptions().get( RequestedIpAddress.class );

        if ( null != requestedAddressOption )
        {
            requestedAddress = requestedAddressOption.getAddress();
        }

        if ( null == requestedAddress )
        {
            requestedAddress = request.getCurrentClientAddress();
        }

        InetAddress selectionBase = determineSelectionBase( clientAddress, request );

        Lease lease = dhcpStore.getExistingLease( request.getHardwareAddress(), requestedAddress, selectionBase,
            requestedLeaseTime, request.getOptions() );

        if ( null == lease )
        {
            return null;
        }

        return lease;
    }


    /**
     * Determine a lease to offer in response to a DHCPDISCOVER message.
     * <p>
     * When a server receives a DHCPDISCOVER message from a client, the server
     * chooses a network address for the requesting client. If no address is
     * available, the server may choose to report the problem to the system
     * administrator. If an address is available, the new address SHOULD be
     * chosen as follows:
     * <ul>
     * <li> The client's current address as recorded in the client's current
     * binding, ELSE
     * <li> The client's previous address as recorded in the client's (now
     * expired or released) binding, if that address is in the server's pool of
     * available addresses and not already allocated, ELSE
     * <li> The address requested in the 'Requested IP Address' option, if that
     * address is valid and not already allocated, ELSE
     * <li> A new address allocated from the server's pool of available
     * addresses; the address is selected based on the subnet from which the
     * message was received (if 'giaddr' is 0) or on the address of the relay
     * agent that forwarded the message ('giaddr' when not 0).
     * </ul>
     * 
     * @param clientAddress
     * @param request
     * @return
     */
    private Lease getLeaseOffer( InetSocketAddress clientAddress, DhcpMessage request ) throws DhcpException
    {
        // determine requested lease time
        IpAddressLeaseTime requestedLeaseTimeOption = ( IpAddressLeaseTime ) request.getOptions().get(
            IpAddressLeaseTime.class );
        long requestedLeaseTime = null != requestedLeaseTimeOption ? requestedLeaseTimeOption.getIntValue() * 1000
            : -1L;

        // try to get the lease (address) requested by the client
        InetAddress requestedAddress = null;
        AddressOption requestedAddressOption = ( AddressOption ) request.getOptions().get( RequestedIpAddress.class );

        if ( null != requestedAddressOption )
        {
            requestedAddress = requestedAddressOption.getAddress();
        }

        InetAddress selectionBase = determineSelectionBase( clientAddress, request );

        Lease lease = dhcpStore.getLeaseOffer( request.getHardwareAddress(), requestedAddress, selectionBase,
            requestedLeaseTime, request.getOptions() );

        return lease;
    }


    /*
     * @see org.apache.directory.server.dhcp.service.AbstractDhcpService#handleRELEASE(java.net.InetSocketAddress,
     *      java.net.InetSocketAddress,
     *      org.apache.directory.server.dhcp.messages.DhcpMessage)
     */
    protected DhcpMessage handleRELEASE( InetSocketAddress localAddress, InetSocketAddress clientAddress,
        DhcpMessage request ) throws DhcpException
    {
        // check server ident
        AddressOption serverIdentOption = ( AddressOption ) request.getOptions().get( ServerIdentifier.class );

        if ( null != serverIdentOption && serverIdentOption.getAddress().isAnyLocalAddress() )
        {
            return null; // not me?! FIXME: handle authoritative server case
        }

        Lease lease = getExistingLease( clientAddress, request );

        DhcpMessage reply = initGeneralReply( localAddress, request );

        if ( null == lease )
        {
            // null lease? send NAK
            // FIXME...
            reply.setMessageType( MessageType.DHCPNAK );
            reply.setCurrentClientAddress( null );
            reply.setAssignedClientAddress( null );
            reply.setNextServerAddress( null );
        }
        else
        {
            dhcpStore.releaseLease( lease );

            // lease Ok, send ACK
            // FIXME...
            reply.getOptions().merge( lease.getOptions() );

            reply.setAssignedClientAddress( lease.getClientAddress() );
            reply.setNextServerAddress( lease.getNextServerAddress() );

            // fix options
            OptionsField options = reply.getOptions();

            // these options must not be present
            options.remove( RequestedIpAddress.class );
            options.remove( ParameterRequestList.class );
            options.remove( ClientIdentifier.class );
            options.remove( MaximumDhcpMessageSize.class );

            // these options must be present
            options.add( new IpAddressLeaseTime( ( lease.getExpires() - System.currentTimeMillis() ) / 1000L ) );

            stripUnwantedOptions( request, options );
        }

        return reply;

    }


    /*
     * @see org.apache.directory.server.dhcp.service.AbstractDhcpService#handleDISCOVER(java.net.InetSocketAddress,
     *      org.apache.directory.server.dhcp.messages.DhcpMessage)
     */
    protected DhcpMessage handleDISCOVER( InetSocketAddress localAddress, InetSocketAddress clientAddress,
        DhcpMessage request ) throws DhcpException
    {
        Lease lease = getLeaseOffer( clientAddress, request );

        // null lease? don't offer one.
        if ( null == lease )
        {
            return null;
        }

        DhcpMessage reply = initGeneralReply( localAddress, request );

        reply.getOptions().merge( lease.getOptions() );

        reply.setMessageType( MessageType.DHCPOFFER );

        reply.setAssignedClientAddress( lease.getClientAddress() );
        reply.setNextServerAddress( lease.getNextServerAddress() );

        // fix options
        OptionsField options = reply.getOptions();

        // these options must not be present
        options.remove( RequestedIpAddress.class );
        options.remove( ParameterRequestList.class );
        options.remove( ClientIdentifier.class );
        options.remove( MaximumDhcpMessageSize.class );

        // these options must be present
        options.add( new IpAddressLeaseTime( ( lease.getExpires() - System.currentTimeMillis() ) / 1000L ) );

        stripUnwantedOptions( request, options );

        return reply;
    }


    /*
     * @see org.apache.directory.server.dhcp.service.AbstractDhcpService#handleREQUEST(java.net.InetSocketAddress,
     *      org.apache.directory.server.dhcp.messages.DhcpMessage)
     */
    protected DhcpMessage handleREQUEST( InetSocketAddress localAddress, InetSocketAddress clientAddress,
        DhcpMessage request ) throws DhcpException
    {
        // check server ident
        AddressOption serverIdentOption = ( AddressOption ) request.getOptions().get( ServerIdentifier.class );

        if ( null != serverIdentOption && serverIdentOption.getAddress().isAnyLocalAddress() )
        {
            return null; // not me?! FIXME: handle authoritative server case
        }

        Lease lease = getExistingLease( clientAddress, request );

        DhcpMessage reply = initGeneralReply( localAddress, request );

        if ( null == lease )
        {
            // null lease? send NAK
            reply.setMessageType( MessageType.DHCPNAK );
            reply.setCurrentClientAddress( null );
            reply.setAssignedClientAddress( null );
            reply.setNextServerAddress( null );
        }
        else
        {
            // lease Ok, send ACK
            reply.getOptions().merge( lease.getOptions() );

            reply.setAssignedClientAddress( lease.getClientAddress() );
            reply.setNextServerAddress( lease.getNextServerAddress() );

            // fix options
            OptionsField options = reply.getOptions();

            // these options must not be present
            options.remove( RequestedIpAddress.class );
            options.remove( ParameterRequestList.class );
            options.remove( ClientIdentifier.class );
            options.remove( MaximumDhcpMessageSize.class );

            // these options must be present
            options.add( new IpAddressLeaseTime( ( lease.getExpires() - System.currentTimeMillis() ) / 1000L ) );

            stripUnwantedOptions( request, options );
        }
        return reply;
    }
}
