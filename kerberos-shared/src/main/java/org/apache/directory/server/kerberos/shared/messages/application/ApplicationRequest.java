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
package org.apache.directory.server.kerberos.shared.messages.application;


import org.apache.directory.server.kerberos.shared.messages.KerberosMessage;
import org.apache.directory.server.kerberos.shared.messages.MessageType;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.flags.ApOption;
import org.apache.directory.server.kerberos.shared.messages.value.flags.ApOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implement the AP-REQ message. An AP message is sent by
 * a client to the targeted Server it want to be authenticatd on.
 * 
 * The ASN.1 grammar is the following :
 * 
 * AP-REQ          ::= [APPLICATION 14] SEQUENCE {
 *        pvno            [0] INTEGER (5),
 *        msg-type        [1] INTEGER (14),
 *        ap-options      [2] APOptions,
 *        ticket          [3] Ticket,
 *        authenticator   [4] EncryptedData -- Authenticator
 * }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 546366 $, $Date: 2007-06-12 05:29:33 +0200 (Tue, 12 Jun 2007) $
 */
public class ApplicationRequest extends KerberosMessage
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( ApplicationRequest.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The request options */
    private ApOptions apOptions;
    
    /** The ticket */
    private Ticket ticket;
    
    /** The encrypted authenticator */
    private EncryptedData encPart; // Authenticator


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
        return apOptions.isFlagSet( option );
    }


    /**
     * Returns the option at a specified index.
     *
     * @param option
     * @return The option.
     */
    public boolean getOption( ApOption option )
    {
        return apOptions.isFlagSet( option );
    }


    /**
     * Sets the option at a specified index.
     *
     * @param option
     */
    public void setOption( int option )
    {
        apOptions.setFlag( option );
    }
    

    /**
     * Sets the option at a specified index.
     *
     * @param option
     */
    public void setOption( ApOption option )
    {
        apOptions.setFlag( option );
    }

    
    /**
     * Clears the option at a specified index.
     *
     * @param option
     */
    public void clearOption( int option )
    {
        apOptions.clearFlag( option );
    }

    /**
     * Clears the option at a specified index.
     *
     * @param option
     */
    public void clearOption( ApOption option )
    {
        apOptions.clearFlag( option );
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
     * Sets the {@link Ticket}.
     *
     * @param ticket
     */
    public void setTicket( Ticket ticket )
    {
        this.ticket = ticket;
    }
}
