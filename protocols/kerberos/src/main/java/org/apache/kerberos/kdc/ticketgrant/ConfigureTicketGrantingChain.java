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

import org.apache.kerberos.replay.InMemoryReplayCache;
import org.apache.kerberos.replay.ReplayCache;
import org.apache.kerberos.service.LockBox;
import org.apache.protocol.common.chain.Context;
import org.apache.protocol.common.chain.impl.CommandBase;

public class ConfigureTicketGrantingChain extends CommandBase
{
    private static final ReplayCache replayCache = new InMemoryReplayCache();
    private static final LockBox lockBox = new LockBox();

    public boolean execute( Context context ) throws Exception
    {
        TicketGrantingContext tgsContext = (TicketGrantingContext) context;

        tgsContext.setReplayCache( replayCache );
        tgsContext.setLockBox( lockBox );

        return CONTINUE_CHAIN;
    }
}
