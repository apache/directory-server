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
package org.apache.directory.server.kerberos.shared.messages;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


public class ErrorMessage extends KerberosMessage
{
    private KerberosTime clientTime; //optional
    private Integer clientMicroSecond; //optional
    private KerberosTime serverTime;
    private int serverMicroSecond;
    private int errorCode;
    private KerberosPrincipal clientPrincipal; //optional
    private KerberosPrincipal serverPrincipal;
    private String explanatoryText; //optional
    private byte[] explanatoryData; //optional


    public ErrorMessage(KerberosTime clientTime, Integer clientMicroSecond, KerberosTime serverTime,
        int serverMicroSecond, int errorCode, KerberosPrincipal clientPrincipal, KerberosPrincipal serverPrincipal,
        String explanatoryText, byte[] explanatoryData)
    {
        super( MessageType.KRB_ERROR );

        this.clientTime = clientTime;
        this.clientMicroSecond = clientMicroSecond;
        this.serverTime = serverTime;
        this.serverMicroSecond = serverMicroSecond;
        this.errorCode = errorCode;
        this.clientPrincipal = clientPrincipal;
        this.serverPrincipal = serverPrincipal;
        this.explanatoryText = explanatoryText;
        this.explanatoryData = explanatoryData;
    }


    public KerberosPrincipal getClientPrincipal()
    {
        return clientPrincipal;
    }


    public KerberosTime getClientTime()
    {
        return clientTime;
    }


    public Integer getClientMicroSecond()
    {
        return clientMicroSecond;
    }


    public byte[] getExplanatoryData()
    {
        return explanatoryData;
    }


    public int getErrorCode()
    {
        return errorCode;
    }


    public String getExplanatoryText()
    {
        return explanatoryText;
    }


    public KerberosPrincipal getServerPrincipal()
    {
        return serverPrincipal;
    }


    public KerberosTime getServerTime()
    {
        return serverTime;
    }


    public int getServerMicroSecond()
    {
        return serverMicroSecond;
    }
}
