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
import org.apache.directory.shared.ldap.codec.bind.BindRequest;
import org.apache.directory.shared.ldap.message.Request;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.ResultResponse;
import org.apache.directory.shared.ldap.message.ResultResponseRequest;
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
     * TODO - add notes about how this protects against unauthorized access
     * and sets up the ldapSession's coreContext.
     * 
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

        if ( ! ( message instanceof BindRequest ) )
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
            }
            else
            {
                coreSession = getLdapServer().getDirectoryService().getSession();
                ldapSession.setCoreSession( coreSession );
            }
            
            /*
             * Perform checks to see if anonymous access is allowed and enforce 
             * anonymous policy.
             */
            if ( coreSession.isAnonymous() && ! ldapServer.isAllowAnonymousAccess() )
            {
                if ( message instanceof ResultResponseRequest )
                {
                    ResultResponse response = ( ( ResultResponseRequest ) message ).getResultResponse();
                    response.getLdapResult().setErrorMessage( "Anonymous access disabled." );
                    response.getLdapResult().setResultCode( ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS );
                    ldapSession.getIoSession().write( response );
                }
                
                return;
            }
        }

        handle( ldapSession, message );
    }

    
    public abstract void handle( LdapSession session, T message ) throws Exception;
}
