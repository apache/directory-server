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
package org.apache.kerberos.messages.value;

import javax.security.auth.kerberos.KerberosPrincipal;

/**
 * Kerberos credential information
 */
public class KrbCredInfo
{
	private EncryptionKey     key;
	private KerberosPrincipal clientPrincipal; //optional
	private TicketFlags       flags;           //optional
	private KerberosTime      authTime;        //optional
	private KerberosTime      startTime;       //optional
	private KerberosTime      endTime;         //optional
	private KerberosTime      renewTill;       //optional
	private KerberosPrincipal serverPrincipal; //optional
	private HostAddresses     clientAddresses; //optional

	public KrbCredInfo( EncryptionKey key, KerberosPrincipal clientPrincipal, TicketFlags flags,
            KerberosTime authTime, KerberosTime startTime, KerberosTime endTime,
            KerberosTime renewTill, KerberosPrincipal serverPrincipal, HostAddresses clientAddresses )
    {
        this.key             = key;
        this.clientPrincipal = clientPrincipal;
        this.flags           = flags;
        this.authTime        = authTime;
        this.startTime       = startTime;
        this.endTime         = endTime;
        this.renewTill       = renewTill;
        this.serverPrincipal = serverPrincipal;
        this.clientAddresses = clientAddresses;
    }

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

    public KerberosPrincipal getClientPrincipal()
    {
        return clientPrincipal;
    }

    public KerberosTime getRenewTill()
    {
        return renewTill;
    }

    public KerberosPrincipal getServerPrincipal()
    {
        return serverPrincipal;
    }

    public KerberosTime getStartTime()
    {
        return startTime;
    }
}
