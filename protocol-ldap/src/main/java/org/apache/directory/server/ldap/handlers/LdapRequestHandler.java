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


import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.shared.ldap.message.AbandonRequest;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.ExtendedRequest;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.Request;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.ResultResponse;
import org.apache.directory.shared.ldap.message.ResultResponseRequest;
import org.apache.mina.common.IoFilterChain;
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
     * Checks to see if confidentiality requirements are met.  If the 
     * LdapServer requires confidentiality and the SSLFilter is engaged
     * this will return true.  If confidentiality is not required this 
     * will return true.  If confidentially is required and the SSLFilter
     * is not engaged in the IoFilterChain this will return false.
     * 
     * This method is used by handlers to determine whether to send back
     * {@link ResultCodeEnum#CONFIDENTIALITY_REQUIRED} error responses back
     * to clients.
     * 
     * @param session the MINA IoSession to check for TLS security
     * @return true if confidentiality requirement is met, false otherwise
     */
    public final boolean isConfidentialityRequirementSatisfied( IoSession session )
    {
       
       if ( ! ldapServer.isConfidentialityRequired() )
       {
           return true;
       }
       
        IoFilterChain chain = session.getFilterChain();
        return chain.contains( "sslFilter" );
    }

    
    public void rejectWithoutConfidentiality( IoSession session, ResultResponse resp ) 
    {
        LdapResult result = resp.getLdapResult();
        result.setResultCode( ResultCodeEnum.CONFIDENTIALITY_REQUIRED );
        result.setErrorMessage( "Confidentiality (TLS secured connection) is required." );
        session.write( resp );
        return;
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
        
        // TODO - session you get from LdapServer should have the ldapServer 
        // member already set no?  Should remove these lines where ever they
        // may be if that's the case.
        ldapSession.setLdapServer( ldapServer );
        
        // protect against insecure conns when confidentiality is required 
        if ( ! isConfidentialityRequirementSatisfied( session ) )
        {
            if ( message instanceof ExtendedRequest )
            {
                // Reject all extended operations except StartTls  
                ExtendedRequest req = ( ExtendedRequest ) message;
                if ( ! req.getID().equals( StartTlsHandler.EXTENSION_OID ) )
                {
                    rejectWithoutConfidentiality( session, req.getResultResponse() );
                    return;
                }
                
                // Allow StartTls extended operations to go through
            }
            else if ( message instanceof ResultResponseRequest )
            {
                // Reject all other operations that have a result response  
                rejectWithoutConfidentiality( session, ( ( ResultResponseRequest ) message ).getResultResponse() );
                return;
            }
            else // Just return from unbind, and abandon immediately
            {
                return;
            }
        }

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
