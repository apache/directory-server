/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.ldap;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.core.session.IoSession;


/**
 * Manages sessions in a thread safe manner for the LdapServer.  This class is 
 * used primarily by the {@link LdapProtocolHandler} to manage sessions and is
 * created by the LdapServer which makes it available to the handler.  It's job
 * is simple and this class was mainly created to be able to expose the session
 * manager safely to things like the LdapProtocolHandler.
 * 
 * Basically, Ldap sessions are stored in a Map, added or removed when a new connection
 * is created or deleted. Most of the time, a new operation is processed and the associated 
 * Ldap session is pulled from the map.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapSessionManager
{
    /** Concurrent hashMap backing for IoSession to LdapSession mapping */
    private Map<IoSession, LdapSession> ldapSessions = new ConcurrentHashMap<>( 100 );


    /**
     * Gets the active sessions managed by the LdapServer.
     * 
     * @return The active sessions
     */
    public LdapSession[] getSessions()
    {
        return ldapSessions.values().toArray( new LdapSession[0] );
    }


    /**
     * Adds a new LdapSession to the LdapServer.
     *
     * @param ldapSession the newly created {@link LdapSession}
     */
    public void addLdapSession( LdapSession ldapSession )
    {
        ldapSessions.put( ldapSession.getIoSession(), ldapSession );
    }


    /**
     * Removes an LdapSession managed by the {@link LdapServer}.  This method
     * has no side effects: meaning it does not perform cleanup tasks after
     * removing the session.  This task is handled by the callers.
     *
     * @param session the MINA session of the LdapSession to be removed 
     * @return the LdapSession to remove
     */
    public LdapSession removeLdapSession( IoSession session )
    {
        return ldapSessions.remove( session );
    }


    /**
     * Gets the LdapSession associated with the MINA session.
     *
     * @param session the MINA session of the LdapSession to retrieve
     * @return the LdapSession associated with the MINA {@link IoSession}
     */
    public LdapSession getLdapSession( IoSession session )
    {
        return ldapSessions.get( session );
    }
}
