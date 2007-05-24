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

import org.apache.directory.server.kerberos.shared.crypto.encryption.CipherTextHandler;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.server.kerberos.shared.messages.ApplicationRequest;
import org.apache.directory.server.kerberos.shared.messages.components.Authenticator;
import org.apache.directory.server.kerberos.shared.messages.components.Ticket;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.replay.ReplayCache;
import org.apache.directory.server.kerberos.shared.service.VerifyAuthHeader;
import org.apache.mina.common.IoSession;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class VerifyServiceTicketAuthHeader extends VerifyAuthHeader
{
    private String contextKey = "context";


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        ChangePasswordContext changepwContext = ( ChangePasswordContext ) session.getAttribute( getContextKey() );

        ApplicationRequest authHeader = changepwContext.getAuthHeader();
        Ticket ticket = changepwContext.getTicket();

        EncryptionType encryptionType = ticket.getEncPart().getEncryptionType();
        EncryptionKey serverKey = changepwContext.getServerEntry().getKeyMap().get( encryptionType );

        long clockSkew = changepwContext.getConfig().getAllowableClockSkew();
        ReplayCache replayCache = changepwContext.getReplayCache();
        boolean emptyAddressesAllowed = changepwContext.getConfig().isEmptyAddressesAllowed();
        InetAddress clientAddress = changepwContext.getClientAddress();
        CipherTextHandler cipherTextHandler = changepwContext.getCipherTextHandler();

        Authenticator authenticator = verifyAuthHeader( authHeader, ticket, serverKey, clockSkew, replayCache,
            emptyAddressesAllowed, clientAddress, cipherTextHandler, KeyUsage.NUMBER11 );

        changepwContext.setAuthenticator( authenticator );

        next.execute( session, message );
    }


    public String getContextKey()
    {
        return ( this.contextKey );
    }
}
