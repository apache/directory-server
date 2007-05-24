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
 * Base class for encrypted parts of KDC responses.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
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
     * Creates a new instance of EncKdcRepPart.
     */
    public EncKdcRepPart()
    {
        // built up by setter during reply generation
    }


    /**
     * Creates a new instance of EncKdcRepPart.
     *
     * @param key
     * @param lastReq
     * @param nonce
     * @param keyExpiration
     * @param flags
     * @param authtime
     * @param starttime
     * @param endtime
     * @param renewTill
     * @param serverPrincipal
     * @param caddr
     * @param componentType
     */
    public EncKdcRepPart( EncryptionKey key, LastRequest lastReq, int nonce, KerberosTime keyExpiration,
        TicketFlags flags, KerberosTime authtime, KerberosTime starttime, KerberosTime endtime, KerberosTime renewTill,
        KerberosPrincipal serverPrincipal, HostAddresses caddr, MessageComponentType componentType )
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


    /**
     * Returns the auth {@link KerberosTime}.
     *
     * @return The auth {@link KerberosTime}.
     */
    public KerberosTime getAuthTime()
    {
        return authTime;
    }


    /**
     * Returns the client {@link HostAddresses}.
     *
     * @return The client {@link HostAddresses}.
     */
    public HostAddresses getClientAddresses()
    {
        return clientAddresses;
    }


    /**
     * Returns the end {@link KerberosTime}.
     *
     * @return The end {@link KerberosTime}.
     */
    public KerberosTime getEndTime()
    {
        return endTime;
    }


    /**
     * Returns the {@link TicketFlags}.
     *
     * @return The {@link TicketFlags}.
     */
    public TicketFlags getFlags()
    {
        return flags;
    }


    /**
     * Returns the {@link EncryptionKey}.
     *
     * @return The {@link EncryptionKey}.
     */
    public EncryptionKey getKey()
    {
        return key;
    }


    /**
     * Returns the key expiration {@link KerberosTime}.
     *
     * @return The key expiration {@link KerberosTime}.
     */
    public KerberosTime getKeyExpiration()
    {
        return keyExpiration;
    }


    /**
     * Returns the {@link LastRequest}.
     *
     * @return The {@link LastRequest}.
     */
    public LastRequest getLastRequest()
    {
        return lastRequest;
    }


    /**
     * Returns the nonce.
     *
     * @return The nonce.
     */
    public int getNonce()
    {
        return nonce;
    }


    /**
     * Returns the renew till {@link KerberosTime}.
     *
     * @return The renew till {@link KerberosTime}.
     */
    public KerberosTime getRenewTill()
    {
        return renewTill;
    }


    /**
     * Returns the server {@link KerberosPrincipal}.
     *
     * @return The server {@link KerberosPrincipal}.
     */
    public KerberosPrincipal getServerPrincipal()
    {
        return serverPrincipal;
    }


    /**
     * Returns the server realm.
     *
     * @return The server realm.
     */
    public String getServerRealm()
    {
        return serverPrincipal.getRealm();
    }


    /**
     * Returns the start {@link KerberosTime}.
     *
     * @return The start {@link KerberosTime}.
     */
    public KerberosTime getStartTime()
    {
        return startTime;
    }


    /**
     * Returns the {@link MessageComponentType}.
     *
     * @return The {@link MessageComponentType}.
     */
    public MessageComponentType getComponentType()
    {
        return componentType;
    }


    /**
     * Sets the auth {@link KerberosTime}.
     *
     * @param time
     */
    public void setAuthTime( KerberosTime time )
    {
        authTime = time;
    }


    /**
     * Sets the client {@link HostAddresses}.
     *
     * @param addresses
     */
    public void setClientAddresses( HostAddresses addresses )
    {
        clientAddresses = addresses;
    }


    /**
     * Sets the end {@link KerberosTime}.
     *
     * @param time
     */
    public void setEndTime( KerberosTime time )
    {
        endTime = time;
    }


    /**
     * Sets the {@link TicketFlags}.
     *
     * @param flags
     */
    public void setFlags( TicketFlags flags )
    {
        this.flags = flags;
    }


    /**
     * Sets the {@link EncryptionKey}.
     *
     * @param key
     */
    public void setKey( EncryptionKey key )
    {
        this.key = key;
    }


    /**
     * Sets the key expiration {@link KerberosTime}.
     *
     * @param expiration
     */
    public void setKeyExpiration( KerberosTime expiration )
    {
        keyExpiration = expiration;
    }


    /**
     * Sets the {@link LastRequest}.
     *
     * @param request
     */
    public void setLastRequest( LastRequest request )
    {
        lastRequest = request;
    }


    /**
     * Sets the nonce.
     *
     * @param nonce
     */
    public void setNonce( int nonce )
    {
        this.nonce = nonce;
    }


    /**
     * Sets the renew till {@link KerberosTime}.
     *
     * @param till
     */
    public void setRenewTill( KerberosTime till )
    {
        renewTill = till;
    }


    /**
     * Sets the server {@link KerberosPrincipal}.
     *
     * @param principal
     */
    public void setServerPrincipal( KerberosPrincipal principal )
    {
        serverPrincipal = principal;
    }


    /**
     * Sets the start {@link KerberosTime}.
     *
     * @param time
     */
    public void setStartTime( KerberosTime time )
    {
        startTime = time;
    }
}
