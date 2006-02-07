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
package org.apache.kerberos.kdc.ticketgrant;

import org.apache.kerberos.kdc.KdcContext;
import org.apache.kerberos.messages.ApplicationRequest;
import org.apache.kerberos.messages.components.Authenticator;
import org.apache.kerberos.messages.components.Ticket;
import org.apache.kerberos.messages.value.EncryptionKey;
import org.apache.kerberos.replay.ReplayCache;
import org.apache.kerberos.store.PrincipalStoreEntry;

public class TicketGrantingContext extends KdcContext
{
    private static final long serialVersionUID = 2130665703752837491L;

    private ApplicationRequest authHeader;
    private Ticket tgt;
    private Ticket newTicket;
    private EncryptionKey sessionKey;
    private Authenticator authenticator;
    private ReplayCache replayCache;

    private PrincipalStoreEntry ticketPrincipalEntry;
    private PrincipalStoreEntry requestPrincipalEntry;

    /**
     * @return Returns the requestPrincipalEntry.
     */
    public PrincipalStoreEntry getRequestPrincipalEntry()
    {
        return requestPrincipalEntry;
    }

    /**
     * @param requestPrincipalEntry The requestPrincipalEntry to set.
     */
    public void setRequestPrincipalEntry( PrincipalStoreEntry requestPrincipalEntry )
    {
        this.requestPrincipalEntry = requestPrincipalEntry;
    }

    /**
     * @return Returns the ticketPrincipalEntry.
     */
    public PrincipalStoreEntry getTicketPrincipalEntry()
    {
        return ticketPrincipalEntry;
    }

    /**
     * @param ticketPrincipalEntry The ticketPrincipalEntry to set.
     */
    public void setTicketPrincipalEntry( PrincipalStoreEntry ticketPrincipalEntry )
    {
        this.ticketPrincipalEntry = ticketPrincipalEntry;
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
     * @return Returns the newTicket.
     */
    public Ticket getNewTicket()
    {
        return newTicket;
    }

    /**
     * @param newTicket The newTicket to set.
     */
    public void setNewTicket( Ticket newTicket )
    {
        this.newTicket = newTicket;
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
     * @return Returns the tgt.
     */
    public Ticket getTgt()
    {
        return tgt;
    }

    /**
     * @param tgt The tgt to set.
     */
    public void setTgt( Ticket tgt )
    {
        this.tgt = tgt;
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
}
