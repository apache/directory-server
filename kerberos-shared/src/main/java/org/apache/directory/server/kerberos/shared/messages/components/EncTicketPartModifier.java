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
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosPrincipalModifier;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalName;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;
import org.apache.directory.server.kerberos.shared.messages.value.TransitedEncoding;


/**
 * Encrypted part of Tickets.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class EncTicketPartModifier
{
    private TicketFlags flags = new TicketFlags();
    private EncryptionKey sessionKey;
    private KerberosPrincipalModifier modifier = new KerberosPrincipalModifier();
    private KerberosPrincipal clientPrincipal;
    private TransitedEncoding transitedEncoding;
    private KerberosTime authTime;
    private KerberosTime startTime; //optional
    private KerberosTime endTime;
    private KerberosTime renewTill; //optional
    private HostAddresses clientAddresses; //optional
    private AuthorizationData authorizationData; //optional


    /**
     * Returns the {@link EncTicketPart}.
     *
     * @return The {@link EncTicketPart}.
     */
    public EncTicketPart getEncTicketPart()
    {
        if ( clientPrincipal == null )
        {
            clientPrincipal = modifier.getKerberosPrincipal();
        }

        return new EncTicketPart( flags, sessionKey, clientPrincipal, transitedEncoding, authTime, startTime, endTime,
            renewTill, clientAddresses, authorizationData );
    }


    /**
     * Sets the client {@link PrincipalName}.
     *
     * @param name
     */
    public void setClientName( PrincipalName name )
    {
        modifier.setPrincipalName( name );
    }


    /**
     * Sets the client realm.
     *
     * @param realm
     */
    public void setClientRealm( String realm )
    {
        modifier.setRealm( realm );
    }


    /**
     * Sets the client {@link KerberosPrincipal}.
     *
     * @param principal
     */
    public void setClientPrincipal( KerberosPrincipal principal )
    {
        clientPrincipal = principal;
    }


    /**
     * Sets the {@link AuthorizationData}.
     *
     * @param data
     */
    public void setAuthorizationData( AuthorizationData data )
    {
        authorizationData = data;
    }


    /**
     * Sets the auth {@link KerberosTime}.
     *
     * @param authtime
     */
    public void setAuthTime( KerberosTime authtime )
    {
        authTime = authtime;
    }


    /**
     * Sets the client {@link HostAddresses}.
     *
     * @param addresses
     */
    public void setClientAddresses( HostAddresses addresses )
    {
        clientAddresses = addresses;
    }


    /**
     * Sets the end {@link KerberosTime}.
     *
     * @param time
     */
    public void setEndTime( KerberosTime time )
    {
        endTime = time;
    }


    /**
     * Sets the {@link TicketFlags}.
     *
     * @param flags
     */
    public void setFlags( TicketFlags flags )
    {
        this.flags = flags;
    }


    /**
     * Sets the flag at the given index.
     *
     * @param flag
     */
    public void setFlag( int flag )
    {
        flags.set( flag );
    }


    /**
     * Clears the flag at the given index.
     *
     * @param flag
     */
    public void clearFlag( int flag )
    {
        flags.clear( flag );
    }


    /**
     * Sets the renew till {@link KerberosTime}.
     *
     * @param till
     */
    public void setRenewTill( KerberosTime till )
    {
        renewTill = till;
    }


    /**
     * Sets the sesson {@link EncryptionKey}.
     *
     * @param key
     */
    public void setSessionKey( EncryptionKey key )
    {
        sessionKey = key;
    }


    /**
     * Sets the start {@link KerberosTime}.
     *
     * @param time
     */
    public void setStartTime( KerberosTime time )
    {
        startTime = time;
    }


    /**
     * Sets the {@link TransitedEncoding}.
     *
     * @param encoding
     */
    public void setTransitedEncoding( TransitedEncoding encoding )
    {
        transitedEncoding = encoding;
    }
}
