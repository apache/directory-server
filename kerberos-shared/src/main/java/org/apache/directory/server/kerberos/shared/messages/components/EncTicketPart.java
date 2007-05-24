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

import org.apache.directory.server.kerberos.shared.messages.Encodable;
import org.apache.directory.server.kerberos.shared.messages.value.AuthorizationData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;
import org.apache.directory.server.kerberos.shared.messages.value.TransitedEncoding;


/**
 * Encrypted part of Tickets.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EncTicketPart implements Encodable
{
    private TicketFlags flags;
    private EncryptionKey sessionKey;
    private KerberosPrincipal clientPrincipal;
    private TransitedEncoding transitedEncoding;
    private KerberosTime authtime;
    private KerberosTime startTime; //optional
    private KerberosTime endTime;
    private KerberosTime renewTill; //optional
    private HostAddresses clientAddresses; //optional
    private AuthorizationData authorizationData; //optional


    /**
     * Creates a new instance of EncTicketPart.
     *
     * @param flags
     * @param key
     * @param clientPrincipal
     * @param transited
     * @param authtime
     * @param starttime
     * @param endtime
     * @param renewTill
     * @param caddr
     * @param authorizationData
     */
    public EncTicketPart( TicketFlags flags, EncryptionKey key, KerberosPrincipal clientPrincipal,
        TransitedEncoding transited, KerberosTime authtime, KerberosTime starttime, KerberosTime endtime,
        KerberosTime renewTill, HostAddresses caddr, AuthorizationData authorizationData )
    {
        this.flags = flags;
        this.sessionKey = key;
        this.clientPrincipal = clientPrincipal;
        this.transitedEncoding = transited;
        this.authtime = authtime;
        this.startTime = starttime;
        this.endTime = endtime;
        this.renewTill = renewTill;
        this.clientAddresses = caddr;
        this.authorizationData = authorizationData;
    }


    /**
     * Returns the {@link AuthorizationData}.
     *
     * @return The {@link AuthorizationData}.
     */
    public AuthorizationData getAuthorizationData()
    {
        return authorizationData;
    }


    /**
     * Returns the auth {@link KerberosTime}
     *
     * @return The auth {@link KerberosTime}
     */
    public KerberosTime getAuthTime()
    {
        return authtime;
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
     * Returns the client {@link KerberosPrincipal}.
     *
     * @return The client {@link KerberosPrincipal}.
     */
    public KerberosPrincipal getClientPrincipal()
    {
        return clientPrincipal;
    }


    /**
     * Returns the client realm.
     *
     * @return The client realm.
     */
    public String getClientRealm()
    {
        return clientPrincipal.getRealm();
    }


    /**
     * Returns the end {@link KerberosTime}
     *
     * @return The end {@link KerberosTime}
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
     * Returns the session {@link EncryptionKey}.
     *
     * @return The session {@link EncryptionKey}.
     */
    public EncryptionKey getSessionKey()
    {
        return sessionKey;
    }


    /**
     * Returns the renew till {@link KerberosTime}
     *
     * @return The renew till {@link KerberosTime}
     */
    public KerberosTime getRenewTill()
    {
        return renewTill;
    }


    /**
     * Returns the start {@link KerberosTime}
     *
     * @return The start {@link KerberosTime}
     */
    public KerberosTime getStartTime()
    {
        return startTime;
    }


    /**
     * Returns the {@link TransitedEncoding}.
     *
     * @return The {@link TransitedEncoding}.
     */
    public TransitedEncoding getTransitedEncoding()
    {
        return transitedEncoding;
    }
}
