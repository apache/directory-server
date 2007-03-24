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
package org.apache.directory.server.changepw.service;


import java.net.InetAddress;

import org.apache.directory.server.kerberos.shared.messages.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.service.LockBox;
import org.apache.directory.server.kerberos.shared.service.VerifyAuthHeader;
import org.apache.mina.common.IoSession;


public class VerifyServiceTicketAuthHeader extends VerifyAuthHeader
{
    private String contextKey = "context";

    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        ChangePasswordContext changepwContext = ( ChangePasswordContext ) session.getAttribute( getContextKey() );

        ApplicationRequest authHeader = changepwContext.getAuthHeader();
        Ticket ticket = changepwContext.getTicket();
        EncryptionKey serverKey = changepwContext.getServerEntry().getEncryptionKey();
        long clockSkew = changepwContext.getConfig().getClockSkew();
        ReplayCache replayCache = changepwContext.getReplayCache();
        boolean emptyAddressesAllowed = changepwContext.getConfig().isEmptyAddressesAllowed();
        InetAddress clientAddress = changepwContext.getClientAddress();
        LockBox lockBox = changepwContext.getLockBox();

        Authenticator authenticator = verifyAuthHeader( authHeader, ticket, serverKey, clockSkew, replayCache,
            emptyAddressesAllowed, clientAddress, lockBox );

        changepwContext.setAuthenticator( authenticator );

        next.execute( session, message );
    }


    public String getContextKey()
    {
        return ( this.contextKey );
    }
}
