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
package org.apache.kerberos.messages.components;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.kerberos.messages.value.EncryptedData;
import org.apache.kerberos.messages.value.KerberosPrincipalModifier;
import org.apache.kerberos.messages.value.PrincipalName;

public class TicketModifier
{
	private int                       ticketVersionNumber;
	private KerberosPrincipalModifier serverModifier = new KerberosPrincipalModifier();
	private EncryptedData             encPart;
	
	public Ticket getTicket()
    {
        KerberosPrincipal serverPrincipal = serverModifier.getKerberosPrincipal();
        return new Ticket( ticketVersionNumber, serverPrincipal, encPart );
    }

    public void setTicketVersionNumber( int versionNumber )
    {
        ticketVersionNumber = versionNumber;
    }

    public void setEncPart( EncryptedData part )
    {
        encPart = part;
    }

    public void setServerName( PrincipalName name )
    {
        serverModifier.setPrincipalName( name );
    }

    public void setServerRealm( String realm )
    {
        serverModifier.setRealm( realm );
    }
}
