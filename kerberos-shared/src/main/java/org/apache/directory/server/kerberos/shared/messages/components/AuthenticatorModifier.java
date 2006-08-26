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
import org.apache.directory.server.kerberos.shared.messages.value.Checksum;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosPrincipalModifier;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PrincipalName;


public class AuthenticatorModifier
{
    private int versionNumber;
    private KerberosPrincipalModifier clientModifier = new KerberosPrincipalModifier();
    private Checksum checksum;
    private int clientMicroSecond;
    private KerberosTime clientTime;
    private EncryptionKey subSessionKey;
    private int sequenceNumber;
    private AuthorizationData authorizationData;


    public Authenticator getAuthenticator()
    {

        KerberosPrincipal clientPrincipal = clientModifier.getKerberosPrincipal();

        return new Authenticator( versionNumber, clientPrincipal, checksum, clientMicroSecond, clientTime,
            subSessionKey, sequenceNumber, authorizationData );
    }


    public void setVersionNumber( int versionNumber )
    {
        this.versionNumber = versionNumber;
    }


    public void setClientName( PrincipalName name )
    {
        clientModifier.setPrincipalName( name );
    }


    public void setClientRealm( String realm )
    {
        clientModifier.setRealm( realm );
    }


    public void setAuthorizationData( AuthorizationData data )
    {
        authorizationData = data;
    }


    public void setChecksum( Checksum checksum )
    {
        this.checksum = checksum;
    }


    public void setClientMicroSecond( int microSecond )
    {
        clientMicroSecond = microSecond;
    }


    public void setClientTime( KerberosTime time )
    {
        clientTime = time;
    }


    public void setSequenceNumber( int number )
    {
        sequenceNumber = number;
    }


    public void setSubSessionKey( EncryptionKey sessionKey )
    {
        subSessionKey = sessionKey;
    }
}
