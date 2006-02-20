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


import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.ApOptions;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;


public class ApplicationRequest extends KerberosMessage
{
    private ApOptions apOptions;
    private Ticket ticket;
    private EncryptedData encPart;
    private Authenticator authenticator;


    /**
     * Class constructors
     */
    public ApplicationRequest()
    {
        super( MessageType.KRB_AP_REQ );
        // used by ASN1 decoder
    }


    public ApplicationRequest(ApOptions apOptions, Ticket ticket, EncryptedData encPart)
    {
        super( MessageType.KRB_AP_REQ );
        this.apOptions = apOptions;
        this.ticket = ticket;
        this.encPart = encPart;
    }


    public ApOptions getApOptions()
    {
        return apOptions;
    }


    public Authenticator getAuthenticator()
    {
        return authenticator;
    }


    public Ticket getTicket()
    {
        return ticket;
    }


    // delegate ApOptions methods
    public boolean getOption( int option )
    {
        return apOptions.get( option );
    }


    public void setOption( int option )
    {
        apOptions.set( option );
    }


    public void clearOption( int option )
    {
        apOptions.clear( option );
    }


    public EncryptedData getEncPart()
    {
        return encPart;
    }


    public void setEncPart( EncryptedData data )
    {
        encPart = data;
    }


    public void setApOptions( ApOptions options )
    {
        apOptions = options;
    }


    public void setAuthenticator( Authenticator authenticator )
    {
        this.authenticator = authenticator;
    }


    public void setTicket( Ticket ticket )
    {
        this.ticket = ticket;
    }
}
