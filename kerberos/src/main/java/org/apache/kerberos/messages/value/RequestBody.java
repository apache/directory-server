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

import org.apache.kerberos.crypto.encryption.EncryptionType;
import org.apache.kerberos.messages.components.Ticket;

public class RequestBody
{
	private KdcOptions        kdcOptions;
	private KerberosPrincipal clientPrincipal;      //optional in TgsReq only
	private KerberosPrincipal serverPrincipal;
	private KerberosTime      from;                 //optional
	private KerberosTime      till;
	private KerberosTime      rtime;                //optional
	private int               nonce;
	private EncryptionType[]  eType;
	private HostAddresses     addresses;            //optional
	private EncryptedData     encAuthorizationData; //optional
	private Ticket[]          additionalTickets;    //optional

	public RequestBody( KdcOptions kdcOptions, KerberosPrincipal clientPrincipal,
            KerberosPrincipal serverPrincipal, KerberosTime from, KerberosTime till,
            KerberosTime rtime, int nonce, EncryptionType[] eType, HostAddresses addresses,
            EncryptedData encAuthorizationData, Ticket[] additionalTickets )
    {
        this.kdcOptions           = kdcOptions;
        this.clientPrincipal      = clientPrincipal;
        this.serverPrincipal      = serverPrincipal;
        this.from                 = from;
        this.till                 = till;
        this.rtime                = rtime;
        this.nonce                = nonce;
        this.eType                = eType;
        this.addresses            = addresses;
        this.encAuthorizationData = encAuthorizationData;
        this.additionalTickets    = additionalTickets;
    }

    public Ticket[] getAdditionalTickets()
    {
        return additionalTickets;
    }

    public HostAddresses getAddresses()
    {
        return addresses;
    }

    public KerberosPrincipal getClientPrincipal()
    {
        return clientPrincipal;
    }

    public KerberosPrincipal getServerPrincipal()
    {
        return serverPrincipal;
    }

    public EncryptedData getEncAuthorizationData()
    {
        return encAuthorizationData;
    }

    public EncryptionType[] getEType()
    {
        return eType;
    }

    public KerberosTime getFrom()
    {
        return from;
    }

    public KdcOptions getKdcOptions()
    {
        return kdcOptions;
    }

    public int getNonce()
    {
        return nonce;
    }

    public KerberosTime getRtime()
    {
        return rtime;
    }

    public KerberosTime getTill()
    {
        return till;
    }
}
