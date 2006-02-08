/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.directory.server.changepw.service;

import java.net.InetAddress;

import org.apache.directory.server.changepw.ChangePasswordConfiguration;
import org.apache.directory.server.changepw.messages.AbstractPasswordMessage;
import org.apache.directory.server.protocol.shared.chain.impl.ContextBase;
import org.apache.kerberos.messages.ApplicationRequest;
import org.apache.kerberos.messages.components.Authenticator;
import org.apache.kerberos.messages.components.Ticket;
import org.apache.kerberos.replay.ReplayCache;
import org.apache.kerberos.service.LockBox;
import org.apache.kerberos.store.PrincipalStore;
import org.apache.kerberos.store.PrincipalStoreEntry;

public class ChangePasswordContext extends ContextBase
{
    private static final long serialVersionUID = -5124209294966799740L;

    private ChangePasswordConfiguration config;
    private PrincipalStore store;
    private AbstractPasswordMessage request;
    private AbstractPasswordMessage reply;
    private InetAddress clientAddress;

    private ApplicationRequest authHeader;
    private Ticket ticket;
    private Authenticator authenticator;
    private PrincipalStoreEntry serverEntry;
    private ReplayCache replayCache;
    private LockBox lockBox;
    private String password;

    /**
     * @return Returns the replayCache.
     */
    public ReplayCache getReplayCache()
    {
        return replayCache;
    }

    /**
     * @param replayCache The replayCache to set.
     */
    public void setReplayCache( ReplayCache replayCache )
    {
        this.replayCache = replayCache;
    }

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
     * @return Returns the config.
     */
    public ChangePasswordConfiguration getConfig()
    {
        return config;
    }

    /**
     * @param config The config to set.
     */
    public void setConfig( ChangePasswordConfiguration config )
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
     * @return Returns the lockBox.
     */
    public LockBox getLockBox()
    {
        return lockBox;
    }

    /**
     * @param lockBox The lockBox to set.
     */
    public void setLockBox( LockBox lockBox )
    {
        this.lockBox = lockBox;
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
    public ApplicationRequest getAuthHeader()
    {
        return authHeader;
    }

    /**
     * @param authHeader The authHeader to set.
     */
    public void setAuthHeader( ApplicationRequest authHeader )
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

    /**
     * @return Returns the password.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * @param password The password to set.
     */
    public void setPassword( String password )
    {
        this.password = password;
    }
}
