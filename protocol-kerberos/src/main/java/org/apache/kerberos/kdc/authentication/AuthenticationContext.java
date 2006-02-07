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
package org.apache.kerberos.kdc.authentication;

import java.util.HashMap;
import java.util.Map;

import org.apache.kerberos.kdc.KdcContext;
import org.apache.kerberos.messages.components.Ticket;
import org.apache.kerberos.messages.value.EncryptionKey;
import org.apache.kerberos.replay.ReplayCache;
import org.apache.kerberos.store.PrincipalStoreEntry;

public class AuthenticationContext extends KdcContext
{
    private static final long serialVersionUID = -2249170923251265359L;

    private Map checksumEngines = new HashMap();

    private Ticket ticket;
    private EncryptionKey clientKey;
    private EncryptionKey sessionKey;
    private ReplayCache replayCache;

    private PrincipalStoreEntry clientEntry;
    private PrincipalStoreEntry serverEntry;

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
     * @return Returns the clientEntry.
     */
    public PrincipalStoreEntry getClientEntry()
    {
        return clientEntry;
    }

    /**
     * @param clientEntry The clientEntry to set.
     */
    public void setClientEntry( PrincipalStoreEntry clientEntry )
    {
        this.clientEntry = clientEntry;
    }

    /**
     * @return Returns the checksumEngines.
     */
    public Map getChecksumEngines()
    {
        return checksumEngines;
    }

    /**
     * @param checksumEngines The checksumEngines to set.
     */
    public void setChecksumEngines( Map checksumEngines )
    {
        this.checksumEngines = checksumEngines;
    }

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
     * @return Returns the clientKey.
     */
    public EncryptionKey getClientKey()
    {
        return clientKey;
    }

    /**
     * @param clientKey The clientKey to set.
     */
    public void setClientKey( EncryptionKey clientKey )
    {
        this.clientKey = clientKey;
    }

    /**
     * @return Returns the sessionKey.
     */
    public EncryptionKey getSessionKey()
    {
        return sessionKey;
    }

    /**
     * @param sessionKey The sessionKey to set.
     */
    public void setSessionKey( EncryptionKey sessionKey )
    {
        this.sessionKey = sessionKey;
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
}
