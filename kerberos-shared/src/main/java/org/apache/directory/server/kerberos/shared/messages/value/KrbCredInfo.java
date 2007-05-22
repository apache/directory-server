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


/**
 * Kerberos credential information.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KrbCredInfo
{
    private EncryptionKey key;
    private KerberosPrincipal clientPrincipal; //optional
    private TicketFlags flags; //optional
    private KerberosTime authTime; //optional
    private KerberosTime startTime; //optional
    private KerberosTime endTime; //optional
    private KerberosTime renewTill; //optional
    private KerberosPrincipal serverPrincipal; //optional
    private HostAddresses clientAddresses; //optional


    /**
     * Creates a new instance of KrbCredInfo.
     *
     * @param key
     * @param clientPrincipal
     * @param flags
     * @param authTime
     * @param startTime
     * @param endTime
     * @param renewTill
     * @param serverPrincipal
     * @param clientAddresses
     */
    public KrbCredInfo( EncryptionKey key, KerberosPrincipal clientPrincipal, TicketFlags flags, KerberosTime authTime,
        KerberosTime startTime, KerberosTime endTime, KerberosTime renewTill, KerberosPrincipal serverPrincipal,
        HostAddresses clientAddresses )
    {
        this.key = key;
        this.clientPrincipal = clientPrincipal;
        this.flags = flags;
        this.authTime = authTime;
        this.startTime = startTime;
        this.endTime = endTime;
        this.renewTill = renewTill;
        this.serverPrincipal = serverPrincipal;
        this.clientAddresses = clientAddresses;
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
     * Returns the client {@link KerberosPrincipal}.
     *
     * @return The client {@link KerberosPrincipal}.
     */
    public KerberosPrincipal getClientPrincipal()
    {
        return clientPrincipal;
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
     * Returns the start {@link KerberosTime}.
     *
     * @return The start {@link KerberosTime}.
     */
    public KerberosTime getStartTime()
    {
        return startTime;
    }
}
