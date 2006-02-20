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


import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A no reply protocol handler implementation for LDAP {@link
 * org.apache.directory.shared.ldap.message.UnbindRequest}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class UnbindHandler implements MessageHandler
{
    private static final Logger log = LoggerFactory.getLogger( UnbindHandler.class );


    public void messageReceived( IoSession session, Object request )
    {
        SessionRegistry registry = SessionRegistry.getSingleton();

        try
        {
            LdapContext ctx = ( LdapContext ) SessionRegistry.getSingleton().getLdapContext( session, null, false );

            if ( ctx != null )
            {
                if ( ctx instanceof ServerLdapContext && ( ( ServerLdapContext ) ctx ).getService().isStarted() )
                {
                    ( ( ServerLdapContext ) ctx ).ldapUnbind();
                }
                ctx.close();
            }
            registry.terminateSession( session );
            registry.remove( session );
        }
        catch ( NamingException e )
        {
            log.error( "failed to unbind session properly", e );
        }
    }
}
