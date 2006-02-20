/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
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
 * Encrypted part of Tickets
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


    public EncTicketPart(TicketFlags flags, EncryptionKey key, KerberosPrincipal clientPrincipal,
        TransitedEncoding transited, KerberosTime authtime, KerberosTime starttime, KerberosTime endtime,
        KerberosTime renewTill, HostAddresses caddr, AuthorizationData authorizationData)
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


    public AuthorizationData getAuthorizationData()
    {
        return authorizationData;
    }


    public KerberosTime getAuthTime()
    {
        return authtime;
    }


    public HostAddresses getClientAddresses()
    {
        return clientAddresses;
    }


    public KerberosPrincipal getClientPrincipal()
    {
        return clientPrincipal;
    }


    public String getClientRealm()
    {
        return clientPrincipal.getRealm();
    }


    public KerberosTime getEndTime()
    {
        return endTime;
    }


    public TicketFlags getFlags()
    {
        return flags;
    }


    public EncryptionKey getSessionKey()
    {
        return sessionKey;
    }


    public KerberosTime getRenewTill()
    {
        return renewTill;
    }


    public KerberosTime getStartTime()
    {
        return startTime;
    }


    public TransitedEncoding getTransitedEncoding()
    {
        return transitedEncoding;
    }
}
