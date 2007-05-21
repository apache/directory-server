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

import org.apache.directory.server.kerberos.shared.messages.value.AuthorizationData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;
import org.apache.directory.server.kerberos.shared.messages.value.TransitedEncoding;


/**
 * Ticket message component as handed out by the ticket granting service.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Ticket
{
    /**
     * Constant for the {@link Ticket} version number (5).
     */
    public static final int TICKET_VNO = 5;

    private int versionNumber;
    private KerberosPrincipal serverPrincipal;
    private EncryptedData encPart;
    private EncTicketPart encTicketPart;


    /**
     * Creates a new instance of Ticket.
     *
     * @param serverPrincipal
     * @param encPart
     */
    public Ticket( KerberosPrincipal serverPrincipal, EncryptedData encPart )
    {
        this( TICKET_VNO, serverPrincipal, encPart );
    }


    /**
     * Creates a new instance of Ticket.
     *
     * @param versionNumber
     * @param serverPrincipal
     * @param encPart
     */
    public Ticket( int versionNumber, KerberosPrincipal serverPrincipal, EncryptedData encPart )
    {
        this.versionNumber = versionNumber;
        this.serverPrincipal = serverPrincipal;
        this.encPart = encPart;
    }


    /**
     * Sets the {@link EncTicketPart}.
     *
     * @param decryptedPart
     */
    public void setEncTicketPart( EncTicketPart decryptedPart )
    {
        encTicketPart = decryptedPart;
    }


    /**
     * Returns the version number.
     *
     * @return The version number.
     */
    public int getVersionNumber()
    {
        return versionNumber;
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
    public String getRealm()
    {
        return serverPrincipal.getRealm();
    }


    /**
     * Returns the {@link EncryptedData}.
     *
     * @return The {@link EncryptedData}.
     */
    public EncryptedData getEncPart()
    {
        return encPart;
    }


    /**
     * Returns the {@link EncTicketPart}.
     *
     * @return The {@link EncTicketPart}.
     */
    public EncTicketPart getEncTicketPart()
    {
        return encTicketPart;
    }


    /**
     * Returns the {@link AuthorizationData}.
     *
     * @return The {@link AuthorizationData}.
     */
    public AuthorizationData getAuthorizationData()
    {
        return encTicketPart.getAuthorizationData();
    }


    /**
     * Returns the auth {@link KerberosTime}.
     *
     * @return The auth {@link KerberosTime}.
     */
    public KerberosTime getAuthTime()
    {
        return encTicketPart.getAuthTime();
    }


    /**
     * Returns the client {@link HostAddresses}.
     *
     * @return The client {@link HostAddresses}.
     */
    public HostAddresses getClientAddresses()
    {
        return encTicketPart.getClientAddresses();
    }


    /**
     * Returns the client {@link KerberosPrincipal}.
     *
     * @return The client {@link KerberosPrincipal}.
     */
    public KerberosPrincipal getClientPrincipal()
    {
        return encTicketPart.getClientPrincipal();
    }


    /**
     * Returns the client realm.
     *
     * @return The client realm.
     */
    public String getClientRealm()
    {
        return encTicketPart.getClientPrincipal().getRealm();
    }


    /**
     * Returns the end {@link KerberosTime}.
     *
     * @return The end {@link KerberosTime}.
     */
    public KerberosTime getEndTime()
    {
        return encTicketPart.getEndTime();
    }


    /**
     * Returns the {@link TicketFlags}.
     *
     * @return The {@link TicketFlags}.
     */
    public TicketFlags getFlags()
    {
        return encTicketPart.getFlags();
    }


    /**
     * Returns the renew till {@link KerberosTime}.
     *
     * @return The renew till {@link KerberosTime}.
     */
    public KerberosTime getRenewTill()
    {
        return encTicketPart.getRenewTill();
    }


    /**
     * Returns the session {@link EncryptionKey}.
     *
     * @return The session {@link EncryptionKey}.
     */
    public EncryptionKey getSessionKey()
    {
        return encTicketPart.getSessionKey();
    }


    /**
     * Returns the start {@link KerberosTime}.
     *
     * @return The start {@link KerberosTime}.
     */
    public KerberosTime getStartTime()
    {
        return encTicketPart.getStartTime();
    }


    /**
     * Returns the {@link TransitedEncoding}.
     *
     * @return The {@link TransitedEncoding}.
     */
    public TransitedEncoding getTransitedEncoding()
    {
        return encTicketPart.getTransitedEncoding();
    }


    /**
     * Returns the flag at the given index.
     *
     * @param flag
     * @return true if the flag at the given index is set.
     */
    public boolean getFlag( int flag )
    {
        return encTicketPart.getFlags().get( flag );
    }
}
