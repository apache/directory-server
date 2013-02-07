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
package org.apache.directory.kerberos.client;


import java.net.InetAddress;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.directory.shared.kerberos.codec.types.EncryptionType;


/**
 * Parameters for controlling a connection to a Kerberos server (KDC).
 * 
 * 3.1.1.  Generation of KRB_AS_REQ Message
 * 
 * The client may specify a number of options in the initial request.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ClientRequestOptions
{
    /** The number of milliseconds in a minute. */
    public static final int MINUTE = 60000;

    /** The number of milliseconds in a day. */
    public static final int DAY = MINUTE * 1440;

    /** The number of milliseconds in a week. */
    public static final int WEEK = MINUTE * 10080;

    /** The allowed clock skew. */
    private long allowedClockSkew = 5 * MINUTE;

    /** Whether pre-authentication by encrypted timestamp is used. */
    private boolean usePaEncTimestamp = true;

    /** Whether forwardable addresses are allowed. */
    private boolean isForwardable = false;

    /** Whether proxiable addresses are allowed. */
    private boolean isProxiable = false;

    /** Whether the request is for a proxy ticket. */
    private boolean isProxy = false;

    /** Whether the request is for a forwarded ticket. */
    private boolean isForwarded = false;

    /** The encryption types. */
    private Set<EncryptionType> encryptionTypes = new LinkedHashSet<EncryptionType>();

    /** The client addresses. */
    private Set<InetAddress> clientAddresses;

    /** The UDP preference limit. */
    private int udpPreferenceLimit = 1500;

    /** The ticket lifetime. */
    private long lifeTime = DAY;

    /** The ticket start time. */
    private Date startTime;

    /** The renewable lifetime. */
    private long renewableLifetime;

    /** Whether to allow postdating of derivative tickets. */
    private boolean isAllowPostdate;

    /**
     * Whether a renewable ticket will be accepted in lieu of a non-renewable ticket if the
     * requested ticket expiration date cannot be satisfied by a non-renewable ticket (due to
     * configuration constraints).
     */
    private boolean isRenewableOk;


    /**
     * Creates a new instance of KdcControls.
     */
    public ClientRequestOptions()
    {
        encryptionTypes.add( EncryptionType.DES_CBC_MD5 );
    }


    /**
     * Returns the allowed clock skew.
     *
     * @return The allowed clock skew.
     */
    public long getAllowedClockSkew()
    {
        return allowedClockSkew;
    }


    /**
     * @param allowedClockSkew The allowedClockSkew to set.
     */
    public void setAllowedClockSkew( long allowedClockSkew )
    {
        this.allowedClockSkew = allowedClockSkew;
    }


    /**
     * Returns whether pre-authentication by encrypted timestamp is to be performed.
     *
     * @return Whether pre-authentication by encrypted timestamp is to be performed.
     */
    public boolean isUsePaEncTimestamp()
    {
        return usePaEncTimestamp;
    }


    /**
     * @param usePaEncTimestamp Whether to use encrypted timestamp pre-authentication.
     */
    public void setUsePaEncTimestamp( boolean usePaEncTimestamp )
    {
        this.usePaEncTimestamp = usePaEncTimestamp;
    }


    /**
     * @return The udpPreferenceLimit.
     */
    public int getUdpPreferenceLimit()
    {
        return udpPreferenceLimit;
    }


    /**
     * Default is UDP.  Set to 1 to use TCP.
     * 
     * @param udpPreferenceLimit 
     */
    public void setUdpPreferenceLimit( int udpPreferenceLimit )
    {
        this.udpPreferenceLimit = udpPreferenceLimit;
    }


    /**
     * Returns the start time.
     *
     * @return The start time.
     */
    public Date getStartTime()
    {
        return startTime;
    }


    /**
     * Request a postdated ticket, valid starting at the specified start time.  Postdated
     * tickets are issued in an invalid state and must be validated by the KDC before use.
     * 
     * @param startTime 
     */
    public void setStartTime( Date startTime )
    {
        this.startTime = startTime;
    }


    /**
     * Returns whether to request a forwardable ticket.
     *
     * @return true if the request is for a forwardable ticket.
     */
    public boolean isForwardable()
    {
        return isForwardable;
    }


    /**
     * Sets whether to request a forwardable ticket.
     *
     * @param isForwardable
     */
    public void setForwardable( boolean isForwardable )
    {
        this.isForwardable = isForwardable;
    }


    /**
     * Returns whether to request a forwarded ticket.
     *
     * @return true if the request is for a forwarded ticket.
     */
    public boolean isForwarded()
    {
        return isForwarded;
    }


    /**
     * Sets whether to request a forwarded ticket.
     *
     * @param isForwarded
     */
    public void setForwarded( boolean isForwarded )
    {
        this.isForwarded = isForwarded;
    }


    /**
     * Returns whether to request a proxiable ticket.
     * 
     * @return true if the request is for a proxiable ticket.
     */
    public boolean isProxiable()
    {
        return isProxiable;
    }


    /**
     * Sets whether to request a proxiable ticket.
     *
     * @param isProxiable
     */
    public void setProxiable( boolean isProxiable )
    {
        this.isProxiable = isProxiable;
    }


    /**
     * Returns whether to request a proxy ticket.
     * 
     * @return true if the request is for a proxy ticket.
     */
    public boolean isProxy()
    {
        return isProxy;
    }


    /**
     * Sets whether to request a proxy ticket.
     *
     * @param isProxy
     */
    public void setProxy( boolean isProxy )
    {
        this.isProxy = isProxy;
    }


    /**
     * @return The lifetime in milliseconds.
     */
    public long getLifeTime()
    {
        return lifeTime;
    }


    /**
     * Requests a ticket with the specified lifetime.  The value for lifetime is
     * in milliseconds.  Constants are provided for MINUTE, DAY, and WEEK.
     * 
     * @param lifeTime The lifetime to set.
     */
    public void setLifeTime( long lifeTime )
    {
        this.lifeTime = lifeTime;
    }


    /**
     * @return The renewable lifetime.
     */
    public long getRenewableLifetime()
    {
        return renewableLifetime;
    }


    /**
     * Requests a ticket with the specified total lifetime.  The value for
     * lifetime is in milliseconds.  Constants are provided for MINUTE, DAY,
     * and WEEK.
     * 
     * @param renewableLifetime The renewable lifetime to set.
     */
    public void setRenewableLifetime( long renewableLifetime )
    {
        this.renewableLifetime = renewableLifetime;
    }


    /**
     * Returns the encryption types.
     *
     * @return The encryption types.
     */
    public Set<EncryptionType> getEncryptionTypes()
    {
        return encryptionTypes;
    }


    /**
     * @param encryptionTypes The encryption types to set.
     */
    public void setEncryptionTypes( Set<EncryptionType> encryptionTypes )
    {
        this.encryptionTypes = encryptionTypes;
    }


    /**
     * Returns the client addresses.
     *
     * @return The client addresses.
     */
    public Set<InetAddress> getClientAddresses()
    {
        return clientAddresses;
    }


    /**
     * Sets the client addresses.
     *
     * @param clientAddresses
     */
    public void setClientAddresses( Set<InetAddress> clientAddresses )
    {
        this.clientAddresses = clientAddresses;
    }


    /**
     * Returns whether postdating is allowed.
     * 
     * @return true if postdating is allowed.
     */
    public boolean isAllowPostdate()
    {
        return isAllowPostdate;
    }


    /**
     * Sets whether postdating is allowed.
     * 
     * @param isAllowPostdate Whether postdating is allowed.
     */
    public void setAllowPostdate( boolean isAllowPostdate )
    {
        this.isAllowPostdate = isAllowPostdate;
    }


    /**
     * Returns whether renewable tickets are OK.
     * 
     * @return true if renewable tickets are OK.
     */
    public boolean isRenewableOk()
    {
        return isRenewableOk;
    }


    /**
     * Sets whether renewable tickets are OK.
     * 
     * @param isRenewableOk Whether renewable tickets are OK.
     */
    public void setRenewableOk( boolean isRenewableOk )
    {
        this.isRenewableOk = isRenewableOk;
    }
}
