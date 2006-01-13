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
package org.apache.changepw.service;

import org.apache.changepw.messages.ChangePasswordReply;
import org.apache.kerberos.messages.application.ApplicationReply;
import org.apache.kerberos.messages.application.PrivateMessage;
import org.apache.protocol.common.chain.Context;
import org.apache.protocol.common.chain.impl.CommandBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitorReply extends CommandBase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( MonitorReply.class );

    public boolean execute( Context context ) throws Exception
    {
        if ( log.isDebugEnabled() )
        {
            try
            {
                ChangePasswordContext changepwContext = (ChangePasswordContext) context;

                ChangePasswordReply reply = (ChangePasswordReply) changepwContext.getReply();
                ApplicationReply appReply = reply.getApplicationReply();
                PrivateMessage priv = reply.getPrivateMessage();

                StringBuffer sb = new StringBuffer();
                sb.append( "Responding with change password reply:" );
                sb.append( "\n\t" + "appReply               " + appReply );
                sb.append( "\n\t" + "priv                   " + priv );

                log.debug( sb.toString() );
            }
            catch ( Exception e )
            {
                // This is a monitor.  No exceptions should bubble up.
                log.error( "Error in reply monitor", e );
            }
        }

        return CONTINUE_CHAIN;
    }
}
