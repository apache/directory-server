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

import org.apache.directory.server.protocol.shared.chain.impl.ChainBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kerberos Change Password and Set Password Protocols (RFC 3244)
 */
public class ChangePasswordChain extends ChainBase
{
    /** the logger for this class */
    private static final Logger log = LoggerFactory.getLogger( ChangePasswordChain.class );

    public ChangePasswordChain()
    {
        super();
        addCommand( new ChangePasswordExceptionHandler() );

        if ( log.isDebugEnabled() )
        {
            addCommand( new MonitorRequest() );
        }

        addCommand( new ConfigureChangePasswordChain() );
        addCommand( new GetAuthHeader() );
        addCommand( new VerifyServiceTicket() );
        addCommand( new GetServerEntry() );
        addCommand( new VerifyServiceTicketAuthHeader() );

        addCommand( new ExtractPassword() );

        if ( log.isDebugEnabled() )
        {
            addCommand( new MonitorContext() );
        }

        addCommand( new CheckPasswordPolicy() );
        addCommand( new ProcessPasswordChange() );
        addCommand( new BuildReply() );

        if ( log.isDebugEnabled() )
        {
            addCommand( new MonitorReply() );
        }
    }
}
