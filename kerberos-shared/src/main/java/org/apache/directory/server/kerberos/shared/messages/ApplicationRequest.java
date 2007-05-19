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


import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.ApOptions;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ApplicationRequest extends KerberosMessage
{
    private ApOptions apOptions;
    private Ticket ticket;
    private EncryptedData encPart;
    private Authenticator authenticator;


    /**
     * Creates a new instance of ApplicationRequest.
     */
    public ApplicationRequest()
    {
        super( MessageType.KRB_AP_REQ );
        // used by ASN1 decoder
    }


    /**
     * Creates a new instance of ApplicationRequest.
     *
     * @param apOptions
     * @param ticket
     * @param encPart
     */
    public ApplicationRequest( ApOptions apOptions, Ticket ticket, EncryptedData encPart )
    {
        super( MessageType.KRB_AP_REQ );
        this.apOptions = apOptions;
        this.ticket = ticket;
        this.encPart = encPart;
    }


    /**
     * Returns the {@link ApOptions}.
     *
     * @return The {@link ApOptions}.
     */
    public ApOptions getApOptions()
    {
        return apOptions;
    }


    /**
     * Returns the {@link Authenticator}.
     *
     * @return The {@link Authenticator}.
     */
    public Authenticator getAuthenticator()
    {
        return authenticator;
    }


    /**
     * Returns the {@link Ticket}.
     *
     * @return The {@link Ticket}.
     */
    public Ticket getTicket()
    {
        return ticket;
    }


    /**
     * Returns the option at a specified index.
     *
     * @param option
     * @return The option.
     */
    public boolean getOption( int option )
    {
        return apOptions.get( option );
    }


    /**
     * Sets the option at a specified index.
     *
     * @param option
     */
    public void setOption( int option )
    {
        apOptions.set( option );
    }


    /**
     * Clears the option at a specified index.
     *
     * @param option
     */
    public void clearOption( int option )
    {
        apOptions.clear( option );
    }


    /**
     * Returns the {@link EncryptedData}.
     *
     * @return The {@link EncryptedData}.
     */
    public EncryptedData getEncPart()
    {
        return encPart;
    }


    /**
     * Sets the {@link EncryptedData}.
     *
     * @param data
     */
    public void setEncPart( EncryptedData data )
    {
        encPart = data;
    }


    /**
     * Sets the {@link ApOptions}.
     *
     * @param options
     */
    public void setApOptions( ApOptions options )
    {
        apOptions = options;
    }


    /**
     * Sets the {@link Authenticator}.
     *
     * @param authenticator
     */
    public void setAuthenticator( Authenticator authenticator )
    {
        this.authenticator = authenticator;
    }


    /**
     * Sets the {@link Ticket}.
     *
     * @param ticket
     */
    public void setTicket( Ticket ticket )
    {
        this.ticket = ticket;
    }
}
