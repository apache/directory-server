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
package org.apache.directory.server.kerberos.shared.messages;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KdcOptions;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.PreAuthenticationData;
import org.apache.directory.server.kerberos.shared.messages.value.RequestBody;


public class KdcRequest extends KerberosMessage
{
    private PreAuthenticationData[] preAuthData; //optional
    private RequestBody requestBody;
    private byte[] bodyBytes;


    public KdcRequest(int pvno, MessageType messageType, PreAuthenticationData[] preAuthData, RequestBody requestBody)
    {
        super( pvno, messageType );
        this.preAuthData = preAuthData;
        this.requestBody = requestBody;
    }


    public KdcRequest(int pvno, MessageType messageType, PreAuthenticationData[] preAuthData, RequestBody requestBody,
        byte[] bodyBytes)
    {
        this( pvno, messageType, preAuthData, requestBody );
        this.bodyBytes = bodyBytes;
    }


    public PreAuthenticationData[] getPreAuthData()
    {
        return preAuthData;
    }


    public byte[] getBodyBytes()
    {
        return bodyBytes;
    }


    // RequestBody delegate methods
    public Ticket[] getAdditionalTickets()
    {
        return requestBody.getAdditionalTickets();
    }


    public HostAddresses getAddresses()
    {
        return requestBody.getAddresses();
    }


    public KerberosPrincipal getClientPrincipal()
    {
        return requestBody.getClientPrincipal();
    }


    public String getRealm()
    {
        return requestBody.getServerPrincipal().getRealm();
    }


    public EncryptedData getEncAuthorizationData()
    {
        return requestBody.getEncAuthorizationData();
    }


    public EncryptionType[] getEType()
    {
        return requestBody.getEType();
    }


    public KerberosTime getFrom()
    {
        return requestBody.getFrom();
    }


    public KdcOptions getKdcOptions()
    {
        return requestBody.getKdcOptions();
    }


    public int getNonce()
    {
        return requestBody.getNonce();
    }


    public KerberosTime getRtime()
    {
        return requestBody.getRtime();
    }


    public KerberosPrincipal getServerPrincipal()
    {
        return requestBody.getServerPrincipal();
    }


    public KerberosTime getTill()
    {
        return requestBody.getTill();
    }


    // RequestBody KdcOptions delegate accesors
    public boolean getOption( int option )
    {
        return requestBody.getKdcOptions().get( option );
    }


    public void setOption( int option )
    {
        requestBody.getKdcOptions().set( option );
    }


    public void clearOption( int option )
    {
        requestBody.getKdcOptions().clear( option );
    }
}
