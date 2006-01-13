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
package org.apache.kerberos.messages;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.kerberos.messages.components.EncKdcRepPart;
import org.apache.kerberos.messages.components.Ticket;
import org.apache.kerberos.messages.value.EncryptedData;
import org.apache.kerberos.messages.value.EncryptionKey;
import org.apache.kerberos.messages.value.HostAddresses;
import org.apache.kerberos.messages.value.KerberosTime;
import org.apache.kerberos.messages.value.LastRequest;
import org.apache.kerberos.messages.value.PreAuthenticationData;
import org.apache.kerberos.messages.value.TicketFlags;

public class KdcReply extends KerberosMessage implements Encodable
{
	private PreAuthenticationData[] paData;  //optional
	private KerberosPrincipal       clientPrincipal;
	private Ticket                  ticket;
	
	private EncKdcRepPart encKDCRepPart = new EncKdcRepPart();
	private EncryptedData encPart;

	public KdcReply( MessageType msgType )
    {
        super( msgType );
    }

    public KdcReply( PreAuthenticationData[] paData, KerberosPrincipal clientPrincipal,
            Ticket ticket, EncryptedData encPart, MessageType msgType )
    {
        this( msgType );
        this.paData = paData;
        this.clientPrincipal = clientPrincipal;
        this.ticket = ticket;
        this.encPart = encPart;
    }

    // getters
    public KerberosPrincipal getClientPrincipal()
    {
        return clientPrincipal;
    }

    public String getClientRealm()
    {
        return clientPrincipal.getRealm();
    }

    public EncryptedData getEncPart()
    {
        return encPart;
    }

    public PreAuthenticationData[] getPaData()
    {
        return paData;
    }

    public Ticket getTicket()
    {
        return ticket;
    }

    // setters
    public void setClientPrincipal( KerberosPrincipal clientPrincipal )
    {
        this.clientPrincipal = clientPrincipal;
    }

    public void setEncKDCRepPart( EncKdcRepPart repPart )
    {
        encKDCRepPart = repPart;
    }

    public void setEncPart( EncryptedData part )
    {
        encPart = part;
    }

    public void setPaData( PreAuthenticationData[] data )
    {
        paData = data;
    }

    public void setTicket( Ticket ticket )
    {
        this.ticket = ticket;
    }

    // EncKdcRepPart delegate getters
    public KerberosTime getAuthTime()
    {
        return encKDCRepPart.getAuthTime();
    }

    public HostAddresses getClientAddresses()
    {
        return encKDCRepPart.getClientAddresses();
    }

    public KerberosTime getEndTime()
    {
        return encKDCRepPart.getEndTime();
    }

    public TicketFlags getFlags()
    {
        return encKDCRepPart.getFlags();
    }

    public EncryptionKey getKey()
    {
        return encKDCRepPart.getKey();
    }

    public KerberosTime getKeyExpiration()
    {
        return encKDCRepPart.getKeyExpiration();
    }

    public LastRequest getLastRequest()
    {
        return encKDCRepPart.getLastRequest();
    }

    public int getNonce()
    {
        return encKDCRepPart.getNonce();
    }

    public KerberosTime getRenewTill()
    {
        return encKDCRepPart.getRenewTill();
    }

    public KerberosPrincipal getServerPrincipal()
    {
        return encKDCRepPart.getServerPrincipal();
    }

    public String getServerRealm()
    {
        return encKDCRepPart.getServerRealm();
    }

    public KerberosTime getStartTime()
    {
        return encKDCRepPart.getStartTime();
    }

    // EncKdcRepPart delegate setters
    public void setAuthTime( KerberosTime time )
    {
        encKDCRepPart.setAuthTime( time );
    }

    public void setClientAddresses( HostAddresses addresses )
    {
        encKDCRepPart.setClientAddresses( addresses );
    }

    public void setEndTime( KerberosTime time )
    {
        encKDCRepPart.setEndTime( time );
    }

    public void setFlags( TicketFlags flags )
    {
        encKDCRepPart.setFlags( flags );
    }

    public void setKey( EncryptionKey key )
    {
        encKDCRepPart.setKey( key );
    }

    public void setKeyExpiration( KerberosTime expiration )
    {
        encKDCRepPart.setKeyExpiration( expiration );
    }

    public void setLastRequest( LastRequest request )
    {
        encKDCRepPart.setLastRequest( request );
    }

    public void setNonce( int nonce )
    {
        encKDCRepPart.setNonce( nonce );
    }

    public void setRenewTill( KerberosTime till )
    {
        encKDCRepPart.setRenewTill( till );
    }

    public void setServerPrincipal( KerberosPrincipal principal )
    {
        encKDCRepPart.setServerPrincipal( principal );
    }

    public void setStartTime( KerberosTime time )
    {
        encKDCRepPart.setStartTime( time );
    }
}
