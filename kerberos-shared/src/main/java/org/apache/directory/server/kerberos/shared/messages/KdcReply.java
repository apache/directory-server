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

import org.apache.directory.server.kerberos.shared.messages.components.EncKdcRepPart;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptedData;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.HostAddresses;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.LastRequest;
import org.apache.directory.server.kerberos.shared.messages.value.PaData;
import org.apache.directory.server.kerberos.shared.messages.value.TicketFlags;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KdcReply extends KerberosMessage implements Encodable
{
    private PaData[] paData; //optional
    private KerberosPrincipal clientPrincipal;
    private Ticket ticket;

    private EncKdcRepPart encKDCRepPart = new EncKdcRepPart();
    private EncryptedData encPart;


    /**
     * Creates a new instance of KdcReply.
     *
     * @param msgType
     */
    public KdcReply( MessageType msgType )
    {
        super( msgType );
    }


    /**
     * Creates a new instance of KdcReply.
     *
     * @param paData
     * @param clientPrincipal
     * @param ticket
     * @param encPart
     * @param msgType
     */
    public KdcReply( PaData[] paData, KerberosPrincipal clientPrincipal, Ticket ticket,
        EncryptedData encPart, MessageType msgType )
    {
        this( msgType );
        this.paData = paData;
        this.clientPrincipal = clientPrincipal;
        this.ticket = ticket;
        this.encPart = encPart;
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
     * Returns the client realm.
     *
     * @return The client realm.
     */
    public String getClientRealm()
    {
        return clientPrincipal.getRealm();
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
     * Returns an array of {@link PaData}s.
     *
     * @return The array of {@link PaData}s.
     */
    public PaData[] getPaData()
    {
        return paData;
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
     * Sets the client {@link KerberosPrincipal}.
     *
     * @param clientPrincipal
     */
    public void setClientPrincipal( KerberosPrincipal clientPrincipal )
    {
        this.clientPrincipal = clientPrincipal;
    }


    /**
     * Sets the {@link EncKdcRepPart}.
     *
     * @param repPart
     */
    public void setEncKDCRepPart( EncKdcRepPart repPart )
    {
        encKDCRepPart = repPart;
    }


    /**
     * Sets the {@link EncryptedData}.
     *
     * @param part
     */
    public void setEncPart( EncryptedData part )
    {
        encPart = part;
    }


    /**
     * Sets the array of {@link PaData}s.
     *
     * @param data
     */
    public void setPaData( PaData[] data )
    {
        paData = data;
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


    // EncKdcRepPart delegate getters

    /**
     * Returns the auth {@link KerberosTime}.
     *
     * @return The auth {@link KerberosTime}.
     */
    public KerberosTime getAuthTime()
    {
        return encKDCRepPart.getAuthTime();
    }


    /**
     * Returns the client {@link HostAddresses}.
     *
     * @return The client {@link HostAddresses}.
     */
    public HostAddresses getClientAddresses()
    {
        return encKDCRepPart.getClientAddresses();
    }


    /**
     * Return the end {@link KerberosTime}.
     *
     * @return The end {@link KerberosTime}.
     */
    public KerberosTime getEndTime()
    {
        return encKDCRepPart.getEndTime();
    }


    /**
     * Returns the {@link TicketFlags}.
     *
     * @return The {@link TicketFlags}.
     */
    public TicketFlags getFlags()
    {
        return encKDCRepPart.getFlags();
    }


    /**
     * Returns the {@link EncryptionKey}.
     *
     * @return The {@link EncryptionKey}.
     */
    public EncryptionKey getKey()
    {
        return encKDCRepPart.getKey();
    }


    /**
     * Returns the key expiration {@link KerberosTime}.
     *
     * @return The key expiration {@link KerberosTime}.
     */
    public KerberosTime getKeyExpiration()
    {
        return encKDCRepPart.getKeyExpiration();
    }


    /**
     * Returns the {@link LastRequest}.
     *
     * @return The {@link LastRequest}.
     */
    public LastRequest getLastRequest()
    {
        return encKDCRepPart.getLastRequest();
    }


    /**
     * Returns the nonce.
     *
     * @return The nonce.
     */
    public int getNonce()
    {
        return encKDCRepPart.getNonce();
    }


    /**
     * Returns the renew till {@link KerberosTime}.
     *
     * @return The renew till {@link KerberosTime}.
     */
    public KerberosTime getRenewTill()
    {
        return encKDCRepPart.getRenewTill();
    }


    /**
     * Returns the server {@link KerberosPrincipal}.
     *
     * @return The server {@link KerberosPrincipal}.
     */
    public KerberosPrincipal getServerPrincipal()
    {
        return encKDCRepPart.getServerPrincipal();
    }


    /**
     * Return the server realm.
     *
     * @return The server realm.
     */
    public String getServerRealm()
    {
        return encKDCRepPart.getServerRealm();
    }


    /**
     * Returns the start {@link KerberosTime}.
     *
     * @return The start {@link KerberosTime}.
     */
    public KerberosTime getStartTime()
    {
        return encKDCRepPart.getStartTime();
    }


    // EncKdcRepPart delegate setters

    /**
     * Sets the auth {@link KerberosTime}.
     *
     * @param time
     */
    public void setAuthTime( KerberosTime time )
    {
        encKDCRepPart.setAuthTime( time );
    }


    /**
     * Sets the client {@link HostAddresses}.
     *
     * @param addresses
     */
    public void setClientAddresses( HostAddresses addresses )
    {
        encKDCRepPart.setClientAddresses( addresses );
    }


    /**
     * Sets the end {@link KerberosTime}.
     *
     * @param time
     */
    public void setEndTime( KerberosTime time )
    {
        encKDCRepPart.setEndTime( time );
    }


    /**
     * Sets the {@link TicketFlags}.
     *
     * @param flags
     */
    public void setFlags( TicketFlags flags )
    {
        encKDCRepPart.setFlags( flags );
    }


    /**
     * Sets the {@link EncryptionKey}.
     *
     * @param key
     */
    public void setKey( EncryptionKey key )
    {
        encKDCRepPart.setKey( key );
    }


    /**
     * Sets the key expiration {@link KerberosTime}.
     *
     * @param expiration
     */
    public void setKeyExpiration( KerberosTime expiration )
    {
        encKDCRepPart.setKeyExpiration( expiration );
    }


    /**
     * Sets the {@link LastRequest}.
     *
     * @param request
     */
    public void setLastRequest( LastRequest request )
    {
        encKDCRepPart.setLastRequest( request );
    }


    /**
     * Sets the nonce.
     *
     * @param nonce
     */
    public void setNonce( int nonce )
    {
        encKDCRepPart.setNonce( nonce );
    }


    /**
     * Sets the renew till {@link KerberosTime}.
     *
     * @param till
     */
    public void setRenewTill( KerberosTime till )
    {
        encKDCRepPart.setRenewTill( till );
    }


    /**
     * Sets the server {@link KerberosPrincipal}.
     *
     * @param principal
     */
    public void setServerPrincipal( KerberosPrincipal principal )
    {
        encKDCRepPart.setServerPrincipal( principal );
    }


    /**
     * Sets the start {@link KerberosTime}.
     *
     * @param time
     */
    public void setStartTime( KerberosTime time )
    {
        encKDCRepPart.setStartTime( time );
    }
}
