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


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RequestBody
{
    private KdcOptions kdcOptions;
    private KerberosPrincipal clientPrincipal; //optional in TgsReq only
    private KerberosPrincipal serverPrincipal;
    private KerberosTime from; //optional
    private KerberosTime till;
    private KerberosTime rtime; //optional
    private int nonce;
    private EncryptionType[] eType;
    private HostAddresses addresses; //optional
    private EncryptedData encAuthorizationData; //optional
    private Ticket[] additionalTickets; //optional


    /**
     * Creates a new instance of RequestBody.
     *
     * @param kdcOptions
     * @param clientPrincipal
     * @param serverPrincipal
     * @param from
     * @param till
     * @param rtime
     * @param nonce
     * @param eType
     * @param addresses
     * @param encAuthorizationData
     * @param additionalTickets
     */
    public RequestBody( KdcOptions kdcOptions, KerberosPrincipal clientPrincipal, KerberosPrincipal serverPrincipal,
        KerberosTime from, KerberosTime till, KerberosTime rtime, int nonce, EncryptionType[] eType,
        HostAddresses addresses, EncryptedData encAuthorizationData, Ticket[] additionalTickets )
    {
        this.kdcOptions = kdcOptions;
        this.clientPrincipal = clientPrincipal;
        this.serverPrincipal = serverPrincipal;
        this.from = from;
        this.till = till;
        this.rtime = rtime;
        this.nonce = nonce;
        this.eType = eType;
        this.addresses = addresses;
        this.encAuthorizationData = encAuthorizationData;
        this.additionalTickets = additionalTickets;
    }


    /**
     * Returns the additional {@link Ticket}s.
     *
     * @return The additional {@link Ticket}s.
     */
    public Ticket[] getAdditionalTickets()
    {
        return additionalTickets;
    }


    /**
     * Returns the {@link HostAddresses}.
     *
     * @return The {@link HostAddresses}.
     */
    public HostAddresses getAddresses()
    {
        return addresses;
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
     * Returns the server {@link KerberosPrincipal}.
     *
     * @return The server {@link KerberosPrincipal}.
     */
    public KerberosPrincipal getServerPrincipal()
    {
        return serverPrincipal;
    }


    /**
     * Returns the encrypted {@link AuthorizationData} as {@link EncryptedData}.
     *
     * @return The encrypted {@link AuthorizationData} as {@link EncryptedData}.
     */
    public EncryptedData getEncAuthorizationData()
    {
        return encAuthorizationData;
    }


    /**
     * Returns the requested {@link EncryptionType}s.
     *
     * @return The requested {@link EncryptionType}s.
     */
    public EncryptionType[] getEType()
    {
        return eType;
    }


    /**
     * Returns the from {@link KerberosTime}.
     *
     * @return The from {@link KerberosTime}.
     */
    public KerberosTime getFrom()
    {
        return from;
    }


    /**
     * Returns the {@link KdcOptions}.
     *
     * @return The {@link KdcOptions}.
     */
    public KdcOptions getKdcOptions()
    {
        return kdcOptions;
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
     * Returns the "R" {@link KerberosTime}.
     *
     * @return The "R" {@link KerberosTime}.
     */
    public KerberosTime getRtime()
    {
        return rtime;
    }


    /**
     * Returns the till {@link KerberosTime}.
     *
     * @return The till {@link KerberosTime}.
     */
    public KerberosTime getTill()
    {
        return till;
    }
}
