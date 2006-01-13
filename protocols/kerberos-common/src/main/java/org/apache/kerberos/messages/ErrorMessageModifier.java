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
package org.apache.kerberos.messages;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.kerberos.messages.value.KerberosTime;

public class ErrorMessageModifier
{
	private KerberosTime      clientTime;        //optional
	private Integer           clientMicroSecond; //optional
	private KerberosTime      serverTime;
	private int               serverMicroSecond;
	private int               errorCode;
	private KerberosPrincipal clientPrincipal;   //optional
	private KerberosPrincipal serverPrincipal;
	private String            explanatoryText;   //optional
	private byte[]            explanatoryData;   //optional

	public ErrorMessage getErrorMessage()
    {
        return new ErrorMessage( clientTime, clientMicroSecond, serverTime, serverMicroSecond,
                errorCode, clientPrincipal, serverPrincipal, explanatoryText, explanatoryData );
    }

    public void setClientPrincipal( KerberosPrincipal principal )
    {
        this.clientPrincipal = principal;
    }

    public void setClientTime( KerberosTime time )
    {
        this.clientTime = time;
    }

    public void setClientMicroSecond( Integer clientMicroSecond )
    {
        this.clientMicroSecond = clientMicroSecond;
    }

    public void setExplanatoryData( byte[] data )
    {
        this.explanatoryData = data;
    }

    public void setErrorCode( int code )
    {
        this.errorCode = code;
    }

    public void setExplanatoryText( String text )
    {
        this.explanatoryText = text;
    }

    public void setServerPrincipal( KerberosPrincipal principal )
    {
        this.serverPrincipal = principal;
    }

    public void setServerTime( KerberosTime time )
    {
        this.serverTime = time;
    }

    public void setServerMicroSecond( int serverMicroSecond )
    {
        this.serverMicroSecond = serverMicroSecond;
    }
}
