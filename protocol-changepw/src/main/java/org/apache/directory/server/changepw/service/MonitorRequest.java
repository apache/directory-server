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


import org.apache.directory.server.changepw.messages.ChangePasswordRequest;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MonitorRequest implements IoHandlerCommand
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( MonitorRequest.class );

    private String contextKey = "context";

    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        if ( log.isDebugEnabled() )
        {
            try
            {
                ChangePasswordContext changepwContext = ( ChangePasswordContext ) session.getAttribute( getContextKey() );

                ChangePasswordRequest request = ( ChangePasswordRequest ) changepwContext.getRequest();
                short authHeaderLength = request.getAuthHeaderLength();
                short messageLength = request.getMessageLength();
                short versionNumber = request.getVersionNumber();

                StringBuffer sb = new StringBuffer();
                sb.append( "Responding to change password request:" );
                sb.append( "\n\t" + "authHeaderLength " + authHeaderLength );
                sb.append( "\n\t" + "messageLength    " + messageLength );
                sb.append( "\n\t" + "versionNumber    " + versionNumber );

                log.debug( sb.toString() );
            }
            catch ( Exception e )
            {
                // This is a monitor.  No exceptions should bubble up.
                log.error( "Error in request monitor", e );
            }
        }

        next.execute( session, message );
    }


    public String getContextKey()
    {
        return ( this.contextKey );
    }
}
