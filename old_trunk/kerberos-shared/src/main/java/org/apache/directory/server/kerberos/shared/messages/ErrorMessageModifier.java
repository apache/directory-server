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
public class ErrorMessageModifier
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
     * Returns the {@link ErrorMessage}.
     *
     * @return The {@link ErrorMessage}.
     */
    public ErrorMessage getErrorMessage()
    {
        return new ErrorMessage( clientTime, clientMicroSecond, serverTime, serverMicroSecond, errorCode,
            clientPrincipal, serverPrincipal, explanatoryText, explanatoryData );
    }


    /**
     * Sets the client {@link KerberosPrincipal}.
     *
     * @param principal
     */
    public void setClientPrincipal( KerberosPrincipal principal )
    {
        this.clientPrincipal = principal;
    }


    /**
     * Sets the client {@link KerberosTime}.
     *
     * @param time
     */
    public void setClientTime( KerberosTime time )
    {
        this.clientTime = time;
    }


    /**
     * Sets the client microsecond.
     *
     * @param clientMicroSecond
     */
    public void setClientMicroSecond( Integer clientMicroSecond )
    {
        this.clientMicroSecond = clientMicroSecond;
    }


    /**
     * Sets the explanatory data.
     *
     * @param data
     */
    public void setExplanatoryData( byte[] data )
    {
        this.explanatoryData = data;
    }


    /**
     * Sets the error code.
     *
     * @param code
     */
    public void setErrorCode( int code )
    {
        this.errorCode = code;
    }


    /**
     * Sets the explanatory text.
     *
     * @param text
     */
    public void setExplanatoryText( String text )
    {
        this.explanatoryText = text;
    }


    /**
     * Sets the server {@link KerberosPrincipal}.
     *
     * @param principal
     */
    public void setServerPrincipal( KerberosPrincipal principal )
    {
        this.serverPrincipal = principal;
    }


    /**
     * Sets the server {@link KerberosTime}.
     *
     * @param time
     */
    public void setServerTime( KerberosTime time )
    {
        this.serverTime = time;
    }


    /**
     * Sets the server microsecond.
     *
     * @param serverMicroSecond
     */
    public void setServerMicroSecond( int serverMicroSecond )
    {
        this.serverMicroSecond = serverMicroSecond;
    }
}
