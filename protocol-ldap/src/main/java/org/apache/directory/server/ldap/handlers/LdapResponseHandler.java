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
package org.apache.directory.server.ldap.handlers;


import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.handler.demux.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A base class for all LDAP response handlers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class LdapResponseHandler<T extends Response> implements MessageHandler<T>
{
    /** The logger for this class */
    protected static final Logger LOG = LoggerFactory.getLogger( LdapResponseHandler.class );

    /** The reference on the Ldap server instance */
    protected LdapServer ldapServer;

    /**
     * @return The associated ldap server instance
     */
    public final LdapServer getLdapServer()
    {
        return ldapServer;
    }


    /**
     * Associates a Ldap server instance to the message handler
     * @param ldapServer the associated ldap server instance
     */
    public final void setLdapServer( LdapServer ldapServer )
    {
        this.ldapServer = ldapServer;
    }


    /**
     *{@inheritDoc} 
     */
    public final void handleMessage( IoSession session, T message ) throws Exception
    {
        LdapSession ldapSession = ldapServer.getLdapSessionManager().getLdapSession( session );

        if ( ldapSession == null )
        {
            // in some cases the session is becoming null though the client is sending the UnbindRequest
            // before closing
            LOG.info( "ignoring the message {} received from null session", message );
            
            return;
        }

        // TODO - session you get from LdapServer should have the ldapServer 
        // member already set no?  Should remove these lines where ever they
        // may be if that's the case.
        ldapSession.setLdapServer( ldapServer );

        handle( ldapSession, message );
    }


    /**
     * Handle a Ldap message associated with a session
     * 
     * @param session The associated session
     * @param message The message we have to handle
     * @throws Exception If there is an error during the processing of this message
     */
    public abstract void handle( LdapSession session, T message ) throws Exception;
}
