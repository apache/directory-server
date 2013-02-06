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
package org.apache.directory.server.kerberos.changepwd.service;


import java.net.InetAddress;

import org.apache.directory.server.kerberos.ChangePasswordConfig;
import org.apache.directory.server.kerberos.changepwd.messages.AbstractPasswordMessage;
import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.shared.kerberos.messages.ApReq;
import org.apache.directory.shared.kerberos.messages.Authenticator;
import org.apache.directory.shared.kerberos.messages.ChangePasswdData;
import org.apache.directory.shared.kerberos.messages.Ticket;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChangePasswordContext
{
    private ChangePasswordConfig config;
    private PrincipalStore store;
    private AbstractPasswordMessage request;
    private AbstractPasswordMessage reply;
    private InetAddress clientAddress;

    private ApReq authHeader;
    private Ticket ticket;
    private Authenticator authenticator;
    private PrincipalStoreEntry serverEntry;
    private CipherTextHandler cipherTextHandler;
    
    private ReplayCache replayCache;

    private ChangePasswdData passwordData;

    /**
     * @return Returns the serverEntry.
     */
    public PrincipalStoreEntry getServerEntry()
    {
        return serverEntry;
    }


    /**
     * @param serverEntry The serverEntry to set.
     */
    public void setServerEntry( PrincipalStoreEntry serverEntry )
    {
        this.serverEntry = serverEntry;
    }


    /**
     * @return gets the config.
     */
    public ChangePasswordConfig getConfig()
    {
        return config;
    }


    /**
     * @param config The config to set.
     */
    public void setConfig( ChangePasswordConfig config )
    {
        this.config = config;
    }


    /**
     * @return Returns the reply.
     */
    public AbstractPasswordMessage getReply()
    {
        return reply;
    }


    /**
     * @param reply The reply to set.
     */
    public void setReply( AbstractPasswordMessage reply )
    {
        this.reply = reply;
    }


    /**
     * @return Returns the request.
     */
    public AbstractPasswordMessage getRequest()
    {
        return request;
    }


    /**
     * @param request The request to set.
     */
    public void setRequest( AbstractPasswordMessage request )
    {
        this.request = request;
    }


    /**
     * @return Returns the store.
     */
    public PrincipalStore getStore()
    {
        return store;
    }


    /**
     * @param store The store to set.
     */
    public void setStore( PrincipalStore store )
    {
        this.store = store;
    }


    /**
     * @return Returns the {@link CipherTextHandler}.
     */
    public CipherTextHandler getCipherTextHandler()
    {
        return cipherTextHandler;
    }


    /**
     * @param cipherTextHandler The {@link CipherTextHandler} to set.
     */
    public void setCipherTextHandler( CipherTextHandler cipherTextHandler )
    {
        this.cipherTextHandler = cipherTextHandler;
    }


    /**
     * @return Returns the authenticator.
     */
    public Authenticator getAuthenticator()
    {
        return authenticator;
    }


    /**
     * @param authenticator The authenticator to set.
     */
    public void setAuthenticator( Authenticator authenticator )
    {
        this.authenticator = authenticator;
    }


    /**
     * @return Returns the authHeader.
     */
    public ApReq getAuthHeader()
    {
        return authHeader;
    }


    /**
     * @param authHeader The authHeader to set.
     */
    public void setAuthHeader( ApReq authHeader )
    {
        this.authHeader = authHeader;
    }


    /**
     * @return Returns the ticket.
     */
    public Ticket getTicket()
    {
        return ticket;
    }


    /**
     * @param ticket The ticket to set.
     */
    public void setTicket( Ticket ticket )
    {
        this.ticket = ticket;
    }


    /**
     * @return Returns the clientAddress.
     */
    public InetAddress getClientAddress()
    {
        return clientAddress;
    }


    /**
     * @param clientAddress The clientAddress to set.
     */
    public void setClientAddress( InetAddress clientAddress )
    {
        this.clientAddress = clientAddress;
    }


    public ChangePasswdData getPasswordData()
    {
        return passwordData;
    }


    public void setChngPwdData( ChangePasswdData passwordData )
    {
        this.passwordData = passwordData;
    }


    public ReplayCache getReplayCache()
    {
        return replayCache;
    }


    public void setReplayCache( ReplayCache replayCache )
    {
        this.replayCache = replayCache;
    }
}
