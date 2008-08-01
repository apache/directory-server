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
package org.apache.directory.server.newldap.handlers;


import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.newldap.LdapServer;
import org.apache.directory.server.newldap.LdapSession;
import org.apache.directory.shared.ldap.message.AbandonRequest;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.Request;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;


/**
 * A base class for all LDAP request handlers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 541827 $
 */
public abstract class LdapRequestHandler<T extends Request> implements MessageHandler<T>
{
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
     * Handle a LDAP message received during a session.
     * 
     * @param session the user session created when the user first connected
     * to the server
     * @param message the LDAP message received. Can be any of the LDAP Request
     * @throws Exception the thrown exception if something went wrong during 
     * the message processing
     */
    public final void messageReceived( IoSession session, T message ) throws Exception
    {
        LdapSession ldapSession = ldapServer.getLdapSession( session );

        // We should check that the server allows anonymous requests
        // only if it's not a BindRequest
        if ( message instanceof BindRequest )
        {
        	handle( ldapSession, message );
        }
        else
        {
            CoreSession coreSession = null;
            
            /*
             * All requests except bind automatically presume the authentication 
             * is anonymous if the session has not been authenticated.  Hence a
             * default bind is presumed as the anonymous identity.
             */
            if ( ldapSession.isAuthenticated() )
            {
                coreSession = ldapSession.getCoreSession();
                handle( ldapSession, message );
                return;
            }
            
            coreSession = getLdapServer().getDirectoryService().getSession();
            ldapSession.setCoreSession( coreSession );

            if ( message instanceof AbandonRequest )
            {
                return;
            }
            
            handle( ldapSession, message );
            return;

            /*
            if ( coreSession.getDirectoryService().isAllowAnonymousAccess() )
            {
            	// We are not authenticated, and the server allows anonymous access,
            	// we have create a new Anonymous session. Just return.
            	handle( ldapSession, message );
            	return;
            }
            else if ( message instanceof ResultResponseRequest )
            {
            	// The server does not allow anonymous access, and the client
            	// is not authenticated : get out if the request expect a
            	// response.
                ResultResponse response = ( ( ResultResponseRequest ) message ).getResultResponse();
                response.getLdapResult().setErrorMessage( "Anonymous access disabled." );
                response.getLdapResult().setResultCode( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS );
                ldapSession.getIoSession().write( response );
                return;
            }
            else
            {
            	// Last case : the AbandonRequest. We just quit.
                return;
            }
            */
        }
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
