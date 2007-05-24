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
public class RequestBodyModifier
{
    private KerberosPrincipalModifier clientModifier = new KerberosPrincipalModifier(); //optional in TgsReq only
    private KerberosPrincipalModifier serverModifier = new KerberosPrincipalModifier();
    private KdcOptions kdcOptions;
    private KerberosTime from; //optional
    private KerberosTime till;
    private KerberosTime rtime; //optional
    private int nonce;
    private EncryptionType[] eType;
    private HostAddresses addresses; //optional
    private EncryptedData encAuthorizationData; //optional
    private Ticket[] additionalTickets; //optional


    /**
     * Returns the {@link RequestBody}.
     *
     * @return The {@link RequestBody}.
     */
    public RequestBody getRequestBody()
    {
        KerberosPrincipal clientPrincipal = clientModifier.getKerberosPrincipal();
        KerberosPrincipal serverPrincipal = serverModifier.getKerberosPrincipal();

        return new RequestBody( kdcOptions, clientPrincipal, serverPrincipal, from, till, rtime, nonce, eType,
            addresses, encAuthorizationData, additionalTickets );
    }


    /**
     * Sets the client {@link PrincipalName}.
     *
     * @param clientName
     */
    public void setClientName( PrincipalName clientName )
    {
        clientModifier.setPrincipalName( clientName );
    }


    /**
     * Sets the server {@link PrincipalName}.
     *
     * @param serverName
     */
    public void setServerName( PrincipalName serverName )
    {
        serverModifier.setPrincipalName( serverName );
    }


    /**
     * Sets the realm.
     *
     * @param realm
     */
    public void setRealm( String realm )
    {
        clientModifier.setRealm( realm );
        serverModifier.setRealm( realm );
    }


    /**
     * Sets additional {@link Ticket}s.
     *
     * @param tickets
     */
    public void setAdditionalTickets( Ticket[] tickets )
    {
        additionalTickets = tickets;
    }


    /**
     * Sets the {@link HostAddresses}.
     *
     * @param addresses
     */
    public void setAddresses( HostAddresses addresses )
    {
        this.addresses = addresses;
    }


    /**
     * Sets the encrypted authorization data.
     *
     * @param authorizationData
     */
    public void setEncAuthorizationData( EncryptedData authorizationData )
    {
        encAuthorizationData = authorizationData;
    }


    /**
     * Sets the requested {@link EncryptionType}s.
     *
     * @param type
     */
    public void setEType( EncryptionType[] type )
    {
        eType = type;
    }


    /**
     * Sets the from {@link KerberosTime}.
     *
     * @param from
     */
    public void setFrom( KerberosTime from )
    {
        this.from = from;
    }


    /**
     * Sets the {@link KdcOptions}.
     *
     * @param options
     */
    public void setKdcOptions( KdcOptions options )
    {
        kdcOptions = options;
    }


    /**
     * Sets the nonce.
     *
     * @param nonce
     */
    public void setNonce( int nonce )
    {
        this.nonce = nonce;
    }


    /**
     * Sets the "R" {@link KerberosTime}.
     *
     * @param rtime
     */
    public void setRtime( KerberosTime rtime )
    {
        this.rtime = rtime;
    }


    /**
     * Sets the till {@link KerberosTime}.
     *
     * @param till
     */
    public void setTill( KerberosTime till )
    {
        this.till = till;
    }
}
