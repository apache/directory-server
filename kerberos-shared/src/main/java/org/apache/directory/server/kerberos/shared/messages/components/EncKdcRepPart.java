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
package org.apache.directory.server.kerberos.shared.messages.components;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.LastRequest;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;


/**
 * Base class for encrypted parts of KDC responses
 */
public class EncKdcRepPart
{
    private EncryptionKey key;
    private LastRequest lastRequest;
    private int nonce;
    private KerberosTime keyExpiration; //optional
    private TicketFlags flags = new TicketFlags();
    private KerberosTime authTime;
    private KerberosTime startTime; //optional
    private KerberosTime endTime;
    private KerberosTime renewTill; //optional
    private KerberosPrincipal serverPrincipal;
    private HostAddresses clientAddresses; //optional
    private MessageComponentType componentType;


    /**
     * Class constructors
     */
    public EncKdcRepPart()
    {
        // built up by setter during reply generation
    }


    public EncKdcRepPart(EncryptionKey key, LastRequest lastReq, int nonce, KerberosTime keyExpiration,
        TicketFlags flags, KerberosTime authtime, KerberosTime starttime, KerberosTime endtime, KerberosTime renewTill,
        KerberosPrincipal serverPrincipal, HostAddresses caddr, MessageComponentType componentType)
    {
        this.key = key;
        this.lastRequest = lastReq;
        this.nonce = nonce;
        this.keyExpiration = keyExpiration;
        this.flags = flags;
        this.authTime = authtime;
        this.startTime = starttime;
        this.endTime = endtime;
        this.renewTill = renewTill;
        this.serverPrincipal = serverPrincipal;
        this.clientAddresses = caddr;
        this.componentType = componentType;
    }


    // getters
    public KerberosTime getAuthTime()
    {
        return authTime;
    }


    public HostAddresses getClientAddresses()
    {
        return clientAddresses;
    }


    public KerberosTime getEndTime()
    {
        return endTime;
    }


    public TicketFlags getFlags()
    {
        return flags;
    }


    public EncryptionKey getKey()
    {
        return key;
    }


    public KerberosTime getKeyExpiration()
    {
        return keyExpiration;
    }


    public LastRequest getLastRequest()
    {
        return lastRequest;
    }


    public int getNonce()
    {
        return nonce;
    }


    public KerberosTime getRenewTill()
    {
        return renewTill;
    }


    public KerberosPrincipal getServerPrincipal()
    {
        return serverPrincipal;
    }


    public String getServerRealm()
    {
        return serverPrincipal.getRealm();
    }


    public KerberosTime getStartTime()
    {
        return startTime;
    }


    public MessageComponentType getComponentType()
    {
        return componentType;
    }


    // setters
    public void setAuthTime( KerberosTime time )
    {
        authTime = time;
    }


    public void setClientAddresses( HostAddresses addresses )
    {
        clientAddresses = addresses;
    }


    public void setEndTime( KerberosTime time )
    {
        endTime = time;
    }


    public void setFlags( TicketFlags flags )
    {
        this.flags = flags;
    }


    public void setKey( EncryptionKey key )
    {
        this.key = key;
    }


    public void setKeyExpiration( KerberosTime expiration )
    {
        keyExpiration = expiration;
    }


    public void setLastRequest( LastRequest request )
    {
        lastRequest = request;
    }


    public void setNonce( int nonce )
    {
        this.nonce = nonce;
    }


    public void setRenewTill( KerberosTime till )
    {
        renewTill = till;
    }


    public void setServerPrincipal( KerberosPrincipal principal )
    {
        serverPrincipal = principal;
    }


    public void setStartTime( KerberosTime time )
    {
        startTime = time;
    }
}
