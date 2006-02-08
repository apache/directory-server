/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.server.ldap.support;


import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.directory.shared.ldap.message.AbandonRequest;
import org.apache.directory.shared.ldap.message.AbandonableRequest;
import org.apache.directory.shared.ldap.message.Request;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handler for {@link org.apache.directory.shared.ldap.message.AbandonRequest}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AbandonHandler implements MessageHandler
{
    private static final Logger log = LoggerFactory.getLogger( AbandonHandler.class );

    public void messageReceived( IoSession session, Object request )
    {
        AbandonRequest req = ( AbandonRequest ) request;
        int abandonedId = req.getAbandoned();

        if ( abandonedId < 0 )
        {
            return;
        }

        Request abandonedRequest = SessionRegistry.getSingleton().getOutstandingRequest( session, abandonedId );
        
        if ( abandonedRequest == null )
        {
            if ( log.isWarnEnabled() )
            {
                log.warn( "Got abandon request from client " + session + " but request must have already " +
                        "terminated.  Abandon request "+req+" had no effect." );
            }
            return;
        }
        
        if ( abandonedRequest instanceof AbandonableRequest )
        {
            log.warn( "Abandon, Bind, Unbind, and StartTLS operations cannot be abandoned.  Abandon request will be ignored." );
        }
        
        ( ( AbandonableRequest ) abandonedRequest ).abandon();
        if ( SessionRegistry.getSingleton().removeOutstandingRequest( session, abandonedId ) == null )
        {
            if ( log.isWarnEnabled() )
            {
                log.warn( "Got abandon request from client " + session + " but request must have already " +
                        "terminated." );
            }
        }
        else
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( "Abandoned request: " + req );
            }
        }
    }
}
