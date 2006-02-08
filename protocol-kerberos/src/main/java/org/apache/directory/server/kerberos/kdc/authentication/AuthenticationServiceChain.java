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
package org.apache.directory.server.kerberos.kdc.authentication;

import org.apache.directory.server.kerberos.kdc.MonitorRequest;
import org.apache.directory.server.kerberos.kdc.preauthentication.PreAuthenticationChain;
import org.apache.directory.server.protocol.shared.chain.impl.ChainBase;

public class AuthenticationServiceChain extends ChainBase
{
    public AuthenticationServiceChain()
    {
        super();
        addCommand( new AuthenticationExceptionHandler() );
        addCommand( new MonitorRequest() );
        addCommand( new ConfigureAuthenticationChain() );
        addCommand( new GetClientEntry() );
        addCommand( new PreAuthenticationChain() );
        addCommand( new GetServerEntry() );
        addCommand( new GetSessionKey() );
        addCommand( new GenerateTicket() );
        addCommand( new BuildReply() );
        addCommand( new SealReply() );
    }
}
