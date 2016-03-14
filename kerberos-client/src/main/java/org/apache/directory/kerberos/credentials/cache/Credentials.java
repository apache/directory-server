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
package org.apache.directory.kerberos.credentials.cache;

import java.text.ParseException;

import org.apache.directory.kerberos.client.AbstractTicket;
import org.apache.directory.kerberos.client.TgTicket;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.apache.directory.shared.kerberos.components.AuthorizationData;
import org.apache.directory.shared.kerberos.components.EncKdcRepPart;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.components.HostAddresses;
import org.apache.directory.shared.kerberos.components.PrincipalName;
import org.apache.directory.shared.kerberos.flags.TicketFlags;
import org.apache.directory.shared.kerberos.messages.Ticket;

/**
 * Looks like KrbCredInfo can be used here, however it's not enough for this
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Credentials
{

    private PrincipalName clientName;
    private String clientRealm;
    private PrincipalName serverName;
    private String serverRealm;
    private EncryptionKey key;
    private KerberosTime authTime;
    private KerberosTime startTime;
    private KerberosTime endTime;
    private KerberosTime renewTill;
    private HostAddresses clientAddresses;
    private AuthorizationData authzData;
    private boolean isEncInSKey;
    private TicketFlags flags;
    private Ticket ticket;
    private Ticket secondTicket;
    
    public Credentials(
            PrincipalName cname,
            PrincipalName sname,
            EncryptionKey ekey,
            KerberosTime authtime,
            KerberosTime starttime,
            KerberosTime endtime,
            KerberosTime renewTill,
            boolean isEncInSKey,
            TicketFlags flags,
            HostAddresses caddr,
            AuthorizationData authData,
            Ticket ticket,
            Ticket secondTicket)
    {
        this.clientName = (PrincipalName) cname;

        if (cname.getRealm() != null)
        {
            clientRealm = cname.getRealm();
        }

        this.serverName = (PrincipalName) sname;

        if (sname.getRealm() != null)
        {
            serverRealm = sname.getRealm();
        }

        this.key = ekey;

        this.authTime = authtime;
        this.startTime = starttime;
        this.endTime = endtime;
        this.renewTill = renewTill;
        this.clientAddresses = caddr;
        this.authzData = authData;
        this.isEncInSKey = isEncInSKey;
        this.flags = flags;
        this.ticket = ticket;
        this.secondTicket = secondTicket;
    }

    public Credentials( TgTicket tgt )
    {
    	PrincipalName clientPrincipal = null;
    	try {
			clientPrincipal = new PrincipalName( tgt.getClientName(), 
					PrincipalNameType.KRB_NT_PRINCIPAL );
		} catch (ParseException e) {
			throw new RuntimeException( "Invalid tgt with bad client name" );
		}
    	
    	clientPrincipal.setRealm( tgt.getRealm() );
    	
    	init( tgt, clientPrincipal );
    }
    
    public Credentials( AbstractTicket tkt, PrincipalName clientPrincipal ) 
    {
    	init( tkt, clientPrincipal );
    }
    
    private void init( AbstractTicket tkt, PrincipalName clientPrincipal )
    {
    	EncKdcRepPart kdcRepPart = tkt.getEncKdcRepPart();
    	
        this.serverName = kdcRepPart.getSName();
        this.serverRealm = kdcRepPart.getSRealm();
        this.serverName.setRealm(serverRealm);

        this.clientName = clientPrincipal;
        
        this.key = kdcRepPart.getKey();
        this.authTime = kdcRepPart.getAuthTime();
        this.startTime = kdcRepPart.getStartTime();
        this.endTime = kdcRepPart.getEndTime();

        this.renewTill = kdcRepPart.getRenewTill();

        this.flags = kdcRepPart.getFlags();
        this.clientAddresses = kdcRepPart.getClientAddresses();

        this.ticket = tkt.getTicket();
        
        this.isEncInSKey = false;
        
        this.secondTicket = null;
    }

    public PrincipalName getServicePrincipal()
    {
        return serverName;
    }

    public KerberosTime getAuthTime()
    {
        return authTime;
    }

    public KerberosTime getEndTime()
    {
        return endTime;
    }

    public TicketFlags getTicketFlags()
    {
        return flags;
    }

    public int getEType()
    {
        return key.getKeyType().getValue();
    }

	public PrincipalName getClientName()
	{
		return clientName;
	}

	public PrincipalName getServerName()
	{
		return serverName;
	}
	
	public String getClientRealm()
	{
		return clientRealm;
	}

	public EncryptionKey getKey()
	{
		return key;
	}

	public KerberosTime getStartTime()
	{
		return startTime;
	}

	public KerberosTime getRenewTill()
	{
		return renewTill;
	}

	public HostAddresses getClientAddresses()
	{
		return clientAddresses;
	}

	public AuthorizationData getAuthzData()
	{
		return authzData;
	}

	public boolean isEncInSKey()
	{
		return isEncInSKey;
	}

	public TicketFlags getFlags()
	{
		return flags;
	}

	public Ticket getTicket()
	{
		return ticket;
	}
	
	public Ticket getSecondTicket()
	{
		return secondTicket;
	}
}
