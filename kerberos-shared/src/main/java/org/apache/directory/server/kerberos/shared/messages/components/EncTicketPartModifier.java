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
 * Encrypted part of Tickets
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


    public EncTicketPart getEncTicketPart()
    {
        if ( clientPrincipal == null )
        {
            clientPrincipal = modifier.getKerberosPrincipal();
        }

        return new EncTicketPart( flags, sessionKey, clientPrincipal, transitedEncoding, authTime, startTime, endTime,
            renewTill, clientAddresses, authorizationData );
    }


    public void setClientName( PrincipalName name )
    {
        modifier.setPrincipalName( name );
    }


    public void setClientRealm( String realm )
    {
        modifier.setRealm( realm );
    }


    public void setClientPrincipal( KerberosPrincipal principal )
    {
        clientPrincipal = principal;
    }


    public void setAuthorizationData( AuthorizationData data )
    {
        authorizationData = data;
    }


    public void setAuthTime( KerberosTime authtime )
    {
        authTime = authtime;
    }


    public void setClientAddresses( HostAddresses addresses )
    {
        clientAddresses = addresses;
    }


    public void setEndTime( KerberosTime time )
    {
        endTime = time;
    }


    public void setFlags( TicketFlags flags )
    {
        this.flags = flags;
    }


    public void setFlag( int flag )
    {
        flags.set( flag );
    }


    public void clearFlag( int flag )
    {
        flags.clear( flag );
    }


    public void setRenewTill( KerberosTime till )
    {
        renewTill = till;
    }


    public void setSessionKey( EncryptionKey key )
    {
        sessionKey = key;
    }


    public void setStartTime( KerberosTime time )
    {
        startTime = time;
    }


    public void setTransitedEncoding( TransitedEncoding encoding )
    {
        transitedEncoding = encoding;
    }
}
