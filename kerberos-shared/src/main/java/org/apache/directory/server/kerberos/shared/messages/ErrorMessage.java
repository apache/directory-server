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

import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
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


    /**
     * Creates a new instance of ErrorMessage.
     *
     * @param clientTime
     * @param clientMicroSecond
     * @param serverTime
     * @param serverMicroSecond
     * @param errorCode
     * @param clientPrincipal
     * @param serverPrincipal
     * @param explanatoryText
     * @param explanatoryData
     */
    public ErrorMessage( KerberosTime clientTime, Integer clientMicroSecond, KerberosTime serverTime,
        int serverMicroSecond, int errorCode, KerberosPrincipal clientPrincipal, KerberosPrincipal serverPrincipal,
        String explanatoryText, byte[] explanatoryData )
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


    /**
     * Returns the client {@link KerberosPrincipal}.
     *
     * @return The client {@link KerberosPrincipal}.
     */
    public KerberosPrincipal getClientPrincipal()
    {
        return clientPrincipal;
    }


    /**
     * Returns the client {@link KerberosTime}.
     *
     * @return The client {@link KerberosTime}.
     */
    public KerberosTime getClientTime()
    {
        return clientTime;
    }


    /**
     * Returns the client microsecond.
     *
     * @return The client microsecond.
     */
    public Integer getClientMicroSecond()
    {
        return clientMicroSecond;
    }


    /**
     * Returns the explanatory data.
     *
     * @return The explanatory data.
     */
    public byte[] getExplanatoryData()
    {
        return explanatoryData;
    }


    /**
     * Returns the error code.
     *
     * @return The error code.
     */
    public int getErrorCode()
    {
        return errorCode;
    }


    /**
     * Returns the explanatory text.
     *
     * @return The explanatory text.
     */
    public String getExplanatoryText()
    {
        return explanatoryText;
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
     * Returns the server {@link KerberosTime}.
     *
     * @return The server {@link KerberosTime}.
     */
    public KerberosTime getServerTime()
    {
        return serverTime;
    }


    /**
     * Returns the server microsecond.
     *
     * @return The server microsecond.
     */
    public int getServerMicroSecond()
    {
        return serverMicroSecond;
    }
}
