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

import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.options.OptionsField;


/**
 * Leases represent a temporary assignment of an IP address to a DHCP client.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Lease
{
    /** Lease state: newly created */
    public static final int STATE_NEW = 1;

    /** Lease state: offered to client */
    public static final int STATE_OFFERED = 2;

    /** Lease state: active - assigned to client */
    public static final int STATE_ACTIVE = 3;

    /** Lease state: released by client */
    public static final int STATE_RELEASED = 4;

    /** Lease state: expired */
    public static final int STATE_EXPIRED = 5;

    /**
     * The lease's state.
     * 
     * @see #STATE_NEW
     * @see #STATE_OFFERED
     * @see #STATE_ACTIVE
     * @see #STATE_RELEASED
     * @see #STATE_EXPIRED
     */
    private int state;

    /**
     * The assigned client address.
     */
    private InetAddress clientAddress;

    /**
     * The client's hardware address.
     */
    private HardwareAddress hardwareAddress;

    /**
     * The next-server (boot-server) address.
     */
    private InetAddress nextServerAddress;

    /**
     * The DhcpOptions to provide to the client along with the lease.
     */
    private OptionsField options = new OptionsField();

    private long acquired = -1;

    private long expires = -1;


    /**
     * @return InetAddress
     */
    public InetAddress getClientAddress()
    {
        return clientAddress;
    }


    /**
     * @return InetAddress
     */
    public InetAddress getNextServerAddress()
    {
        return nextServerAddress;
    }


    /**
     * @return OptionsField
     */
    public OptionsField getOptions()
    {
        return options;
    }


    /**
     * @return int
     */
    public int getState()
    {
        return state;
    }


    /**
     * @param state
     */
    public void setState( int state )
    {
        this.state = state;
    }


    public HardwareAddress getHardwareAddress()
    {
        return hardwareAddress;
    }


    public void setHardwareAddress( HardwareAddress hardwareAddress )
    {
        this.hardwareAddress = hardwareAddress;
    }


    public long getAcquired()
    {
        return acquired;
    }


    public void setAcquired( long acquired )
    {
        this.acquired = acquired;
    }


    public long getExpires()
    {
        return expires;
    }


    public void setExpires( long expires )
    {
        this.expires = expires;
    }


    public void setClientAddress( InetAddress clientAddress )
    {
        this.clientAddress = clientAddress;
    }

}
