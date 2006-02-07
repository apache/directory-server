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

import org.apache.kerberos.messages.TicketGrantReply;
import org.apache.kerberos.messages.components.Authenticator;
import org.apache.kerberos.messages.components.Ticket;
import org.apache.kerberos.messages.value.EncryptedData;
import org.apache.kerberos.service.LockBox;
import org.apache.protocol.common.chain.Context;
import org.apache.protocol.common.chain.impl.CommandBase;

public class SealReply extends CommandBase
{
    public boolean execute( Context ctx ) throws Exception
    {
        TicketGrantingContext tgsContext = (TicketGrantingContext) ctx;

        TicketGrantReply reply = (TicketGrantReply) tgsContext.getReply();
        Ticket tgt = tgsContext.getTgt();
        LockBox lockBox = tgsContext.getLockBox();
        Authenticator authenticator = tgsContext.getAuthenticator();

        EncryptedData encryptedData;

        if ( authenticator.getSubSessionKey() != null )
        {
            encryptedData = lockBox.seal( authenticator.getSubSessionKey(), reply );
        }
        else
        {
            encryptedData = lockBox.seal( tgt.getSessionKey(), reply );
        }

        reply.setEncPart( encryptedData );

        return CONTINUE_CHAIN;
    }
}
