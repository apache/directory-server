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

import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.kerberos.messages.ApplicationRequest;
import org.apache.kerberos.messages.components.Authenticator;
import org.apache.kerberos.messages.components.Ticket;
import org.apache.kerberos.messages.value.EncryptionKey;
import org.apache.kerberos.replay.ReplayCache;
import org.apache.kerberos.service.LockBox;
import org.apache.kerberos.service.VerifyAuthHeader;

public class VerifyServiceTicketAuthHeader extends VerifyAuthHeader
{
    public boolean execute( Context context ) throws Exception
    {
        ChangePasswordContext changepwContext = (ChangePasswordContext) context;

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

        return CONTINUE_CHAIN;
    }
}
