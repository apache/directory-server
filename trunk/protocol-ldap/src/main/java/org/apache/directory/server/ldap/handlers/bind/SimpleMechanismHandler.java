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
package org.apache.directory.server.ldap.handlers.bind;


import javax.security.sasl.SaslServer;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.ldap.LdapProtocolUtils;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationException;
import org.apache.directory.shared.ldap.model.message.BindRequest;
import org.apache.directory.shared.ldap.model.message.BindResponse;
import org.apache.directory.shared.ldap.model.message.LdapResult;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Dummy mechanism handler for Simple mechanism: not really used but needed
 * for the mechanism map.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SimpleMechanismHandler implements MechanismHandler
{
    /** The logger instance */
    private static final Logger LOG = LoggerFactory.getLogger( SimpleMechanismHandler.class );


    public SaslServer handleMechanism( LdapSession ldapSession, BindRequest bindRequest ) throws Exception
    {
        // create a new Bind context, with a null session, as we don't have 
        // any context yet.
        BindOperationContext bindContext = new BindOperationContext( null );

        // Stores the Dn of the user to check, and its password
        bindContext.setDn( bindRequest.getDn() );
        bindContext.setCredentials( bindRequest.getCredentials() );
        bindContext.setInterceptors( ldapSession.getLdapServer().getDirectoryService()
            .getInterceptors( OperationEnum.BIND ) );

        // Stores the request controls into the operation context
        LdapProtocolUtils.setRequestControls( bindContext, bindRequest );

        try
        {
            CoreSession adminSession = ldapSession.getLdapServer().getDirectoryService().getAdminSession();

            // And call the OperationManager bind operation.
            adminSession.getDirectoryService().getOperationManager().bind( bindContext );

            // As a result, store the created session in the Core Session
            ldapSession.setCoreSession( bindContext.getSession() );

            // Return the successful response
            BindResponse response = ( BindResponse ) bindRequest.getResultResponse();
            response.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );
            LdapProtocolUtils.setResponseControls( bindContext, response );

            // Write it back to the client
            ldapSession.getIoSession().write( response );
            LOG.debug( "Returned SUCCESS message: {}.", response );
        }
        catch ( LdapException e )
        {
            // Something went wrong. Write back an error message            
            ResultCodeEnum code = null;
            LdapResult result = bindRequest.getResultResponse().getLdapResult();

            if ( e instanceof LdapOperationException )
            {
                code = ( ( LdapOperationException ) e ).getResultCode();
                result.setResultCode( code );
            }
            else
            {
                code = ResultCodeEnum.getBestEstimate( e, bindRequest.getType() );
                result.setResultCode( code );
            }

            String msg = "Bind failed: " + e.getLocalizedMessage();

            if ( LOG.isDebugEnabled() )
            {
                msg += ":\n" + ExceptionUtils.getStackTrace( e );
                msg += "\n\nBindRequest = \n" + bindRequest.toString();
            }

            Dn name = null;

            if ( e instanceof LdapAuthenticationException )
            {
                name = ( ( LdapAuthenticationException ) e ).getResolvedDn();
            }

            if ( ( name != null )
                && ( ( code == ResultCodeEnum.NO_SUCH_OBJECT ) || ( code == ResultCodeEnum.ALIAS_PROBLEM )
                    || ( code == ResultCodeEnum.INVALID_DN_SYNTAX ) || ( code == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM ) ) )
            {
                result.setMatchedDn( name );
            }

            result.setDiagnosticMessage( msg );
            ldapSession.getIoSession().write( bindRequest.getResultResponse() );
        }

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public void init( LdapSession ldapSession )
    {
        // Do nothing
    }


    /**
     * {@inheritDoc}
     */
    public void cleanup( LdapSession ldapSession )
    {
        ldapSession.clearSaslProperties();
    }
}
