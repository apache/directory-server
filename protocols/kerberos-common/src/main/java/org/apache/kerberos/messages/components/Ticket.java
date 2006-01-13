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
package org.apache.kerberos.messages.components;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.kerberos.messages.value.AuthorizationData;
import org.apache.kerberos.messages.value.EncryptedData;
import org.apache.kerberos.messages.value.EncryptionKey;
import org.apache.kerberos.messages.value.HostAddresses;
import org.apache.kerberos.messages.value.KerberosTime;
import org.apache.kerberos.messages.value.TicketFlags;
import org.apache.kerberos.messages.value.TransitedEncoding;

/**
 * Ticket message component as handed out by the ticket granting service
 */
public class Ticket
{
    public static final int TICKET_VNO = 5;

    private int               versionNumber;
    private KerberosPrincipal serverPrincipal;
    private EncryptedData     encPart;
    private EncTicketPart     encTicketPart;

    public Ticket( KerberosPrincipal serverPrincipal, EncryptedData encPart )
    {
        this( TICKET_VNO, serverPrincipal, encPart );
    }

    public Ticket( int versionNumber, KerberosPrincipal serverPrincipal, EncryptedData encPart )
    {
        this.versionNumber   = versionNumber;
        this.serverPrincipal = serverPrincipal;
        this.encPart         = encPart;
    }

    public void setEncTicketPart( EncTicketPart decryptedPart )
    {
        encTicketPart = decryptedPart;
    }

    // getters
    public int getVersionNumber()
    {
        return versionNumber;
    }

    public KerberosPrincipal getServerPrincipal()
    {
        return serverPrincipal;
    }

    public String getRealm()
    {
        return serverPrincipal.getRealm();
    }

    public EncryptedData getEncPart()
    {
        return encPart;
    }

    public EncTicketPart getEncTicketPart()
    {
        return encTicketPart;
    }

    // EncTicketPart delegate getters
    public AuthorizationData getAuthorizationData()
    {
        return encTicketPart.getAuthorizationData();
    }

    public KerberosTime getAuthTime()
    {
        return encTicketPart.getAuthTime();
    }

    public HostAddresses getClientAddresses()
    {
        return encTicketPart.getClientAddresses();
    }

    public KerberosPrincipal getClientPrincipal()
    {
        return encTicketPart.getClientPrincipal();
    }

    public String getClientRealm()
    {
        return encTicketPart.getClientPrincipal().getRealm();
    }

    public KerberosTime getEndTime()
    {
        return encTicketPart.getEndTime();
    }

    public TicketFlags getFlags()
    {
        return encTicketPart.getFlags();
    }

    public KerberosTime getRenewTill()
    {
        return encTicketPart.getRenewTill();
    }

    public EncryptionKey getSessionKey()
    {
        return encTicketPart.getSessionKey();
    }

    public KerberosTime getStartTime()
    {
        return encTicketPart.getStartTime();
    }

    public TransitedEncoding getTransitedEncoding()
    {
        return encTicketPart.getTransitedEncoding();
    }

    // EncTicketPart TicketFlag delegates
    public boolean getFlag( int flag )
    {
        return encTicketPart.getFlags().get( flag );
    }
}
