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
package org.apache.directory.server.ldap.support;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.ldap.LdapConfiguration;
import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.directory.server.ldap.support.bind.BindHandlerChain;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.message.*;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.apache.mina.handler.demux.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.spi.InitialContextFactory;
import java.util.Hashtable;
import java.util.Set;


/**
 * A single reply handler for {@link org.apache.directory.shared.ldap.message.BindRequest}s.
 * 
 * Implements server-side of RFC 2222, sections 4.2 and 4.3.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BindHandler extends AbstractLdapHandler implements MessageHandler
{
    private static final Logger LOG = LoggerFactory.getLogger( BindHandler.class );

    /** A class to handle SASL bind requests */
    private IoHandlerCommand saslBindHandler;
    
    /** Definition of SIMPLE and STRONG authentication constants */
    private static final String SIMPLE_AUTHENTICATION_LEVEL = "simple";
        
    //private static final String STRONG_AUTHENTICATION_LEVEL = "strong";
    
    /** An empty Contol array used to get back the controls if any */
    private static final MutableControl[] EMPTY_CONTROL = new MutableControl[0];


    /**
     * Creates a new instance of BindHandler.
     */                                                                      
    public BindHandler()
    {
    }


    public void setDirectoryService( DirectoryService directoryService )
    {
        saslBindHandler = new BindHandlerChain( directoryService );
    }


    /**
     * Create an environment object and inject the Bond informations collected
     * from the BindRequest message :
     *  - the principal : the user's who issued the Bind request
     *  - the credentials : principal's password, if auth level is 'simple'
     *  - the authentication level : either 'simple' or 'strong'
     *  - how to handle referral : either 'ignore' or 'throw'
     */
    private Hashtable<String, Object> getEnvironment( IoSession session, BindRequest bindRequest, String authenticationLevel )
    {
        LdapDN principal = bindRequest.getName();

        /**
         * For simple, this is a password.  For strong, this is unused.
         */
        Object credentials = bindRequest.getCredentials();

        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "{} {}", Context.SECURITY_PRINCIPAL, principal );
            LOG.debug( "{} {}", Context.SECURITY_CREDENTIALS, credentials );
            LOG.debug( "{} {}", Context.SECURITY_AUTHENTICATION, authenticationLevel );
        }

        // clone the environment first then add the required security settings
        Hashtable<String, Object> env = SessionRegistry.getSingleton().getEnvironmentByCopy();
        
        // Store the principal
        env.put( Context.SECURITY_PRINCIPAL, principal );

        // Store the credentials
        if ( credentials != null )
        {
            env.put( Context.SECURITY_CREDENTIALS, credentials );
        }

        // Store the authentication level
        env.put( Context.SECURITY_AUTHENTICATION, authenticationLevel );

        // Store the referral handling method
        if ( bindRequest.getControls().containsKey( ManageDsaITControl.CONTROL_OID ) )
        {
            env.put( Context.REFERRAL, "ignore" );
        }
        else
        {
            env.put( Context.REFERRAL, "throw" );
        }

        return env;
    }

    /**
     * Create the Context associated with the BindRequest.
     */
    private LdapContext getLdapContext( IoSession session, BindRequest bindRequest, Hashtable<String, Object> env )
    {
        LdapResult result = bindRequest.getResultResponse().getLdapResult();
        LdapContext ctx = null;

        try
        {
            if ( env.containsKey( "server.use.factory.instance" ) )
            {
                InitialContextFactory factory = ( InitialContextFactory ) env.get( "server.use.factory.instance" );

                if ( factory == null )
                {
                    LOG.error( "The property 'server.use.factory.instance'  was set in env but was null" );
                    throw new NullPointerException( "server.use.factory.instance was set in env but was null" );
                }

                // Bind is a special case where we have to use the referral property to deal
                ctx = ( LdapContext ) factory.getInitialContext( env );
            }
            else
            {
                MutableControl[] connCtls = bindRequest.getControls().values().toArray( EMPTY_CONTROL );
                ctx = new InitialLdapContext( env, connCtls );
            }
        }
        catch ( NamingException e )
        {
            ResultCodeEnum code;

            if ( e instanceof LdapException )
            {
                code = ( ( LdapException ) e ).getResultCode();
                result.setResultCode( code );
            }
            else
            {
                code = ResultCodeEnum.getBestEstimate( e, bindRequest.getType() );
                result.setResultCode( code );
            }

            String msg = "Bind failed: " + e.getMessage();

            if ( LOG.isDebugEnabled() )
            {
                msg += ":\n" + ExceptionUtils.getStackTrace( e );
                msg += "\n\nBindRequest = \n" + bindRequest.toString();
                LOG.debug(  msg  );
            }

            if ( ( e.getResolvedName() != null )
                && ( ( code == ResultCodeEnum.NO_SUCH_OBJECT ) || ( code == ResultCodeEnum.ALIAS_PROBLEM )
                    || ( code == ResultCodeEnum.INVALID_DN_SYNTAX ) || ( code == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM ) ) )
            {
                result.setMatchedDn( ( LdapDN ) e.getResolvedName() );
            }

            result.setErrorMessage( msg );
            session.write( bindRequest.getResultResponse() );
            ctx = null;
        }
        
        return ctx;
    }

    /**
     * This method handle a 'simple' authentication. Of course, the 'SIMPLE' mechanism 
     * must have been allowed in the configuration, otherwise an error is thrown.
     * 
     */
    private void handleSimpleAuth( IoSession session, BindRequest bindRequest ) throws NamingException
    {
        LdapConfiguration config = ( LdapConfiguration ) session.getAttribute( LdapConfiguration.class.toString() );
        
        @SuppressWarnings( "unchecked" )
        Set<String> supportedMechanisms = config.getSupportedMechanisms();
        LdapResult bindResult = bindRequest.getResultResponse().getLdapResult();

        // First, deal with Simple Authentication
        // Guard clause:  Reject SIMPLE mechanism.
        if ( !supportedMechanisms.contains( "SIMPLE" ) )
        {
            LOG.error( "Bind error : SIMPLE authentication not supported. Please check the server.xml configuration file (supportedMechanisms field)" );

            bindResult.setResultCode( ResultCodeEnum.STRONG_AUTH_REQUIRED );
            bindResult.setErrorMessage( "Simple binds are disabled." );
            session.write( bindRequest.getResultResponse() );
            return;
        }

        // Initialize the environment which will be used to create the context
        Hashtable<String, Object> env = getEnvironment( session, bindRequest, SIMPLE_AUTHENTICATION_LEVEL );
        
        // Now, get the context
        LdapContext ctx = getLdapContext( session, bindRequest, env );
        
        // Test that we successfully got one. If not, an error has already been returned.
        if ( ctx != null )
        {
            ServerLdapContext newCtx = ( ServerLdapContext ) ctx.lookup( "" );
            setRequestControls( newCtx, bindRequest );
            SessionRegistry.getSingleton().setLdapContext( session, newCtx );
            bindResult.setResultCode( ResultCodeEnum.SUCCESS );
            BindResponse response = ( BindResponse ) bindRequest.getResultResponse();
            response.addAll( newCtx.getResponseControls() );
            session.write( response );
            LOG.debug( "Returned SUCCESS message." );
        }
    }

    /**
     * Deal with a received BindRequest
     */
    public void messageReceived( IoSession session, Object message ) throws Exception
    {
        BindRequest bindRequest = ( BindRequest ) message;

        if ( LOG.isDebugEnabled() )
        {
        	LOG.debug( "User {} is binding", bindRequest.getName() );
        	
            if ( bindRequest.isSimple() )
            {
                LOG.debug( "Using simple authentication." );
                
            }
            else
            {
                LOG.debug( "Using SASL authentication with mechanism:  {}", bindRequest.getSaslMechanism() );
            }
        }
        
        // Guard clause:  LDAP version 3
        if ( !bindRequest.getVersion3() )
        {
            LOG.error( "Bind error : Only LDAP v3 is supported." );
            LdapResult bindResult = bindRequest.getResultResponse().getLdapResult();
            bindResult.setResultCode( ResultCodeEnum.PROTOCOL_ERROR );
            bindResult.setErrorMessage( "Only LDAP v3 is supported." );
            session.write( bindRequest.getResultResponse() );
            return;
        }
        
        // Deal with the two kinds of authen :
        // - if it's simple, handle it in this class for speed
        // - for sasl, we go through a chain right now (but it may change in the near future)
        if ( bindRequest.isSimple() )
        {
            handleSimpleAuth( session, bindRequest );
        }
        else
        {
            saslBindHandler.execute( null, session, message );
        }
    }
}
