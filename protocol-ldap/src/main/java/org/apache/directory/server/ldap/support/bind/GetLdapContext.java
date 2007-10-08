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
package org.apache.directory.server.ldap.support.bind;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.spi.InitialContextFactory;

import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.MutableControl;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GetLdapContext implements IoHandlerCommand
{
    private static final Logger LOG = LoggerFactory.getLogger( GetLdapContext.class );
    private static final MutableControl[] EMPTY = new MutableControl[0];

    private final SessionRegistry registry;


    public GetLdapContext( SessionRegistry registry )
    {
        this.registry = registry;
    }


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        Hashtable env = getEnvironment( session, message );
        BindRequest request = ( BindRequest ) message;
        LdapResult result = request.getResultResponse().getLdapResult();
        LdapContext ctx;

        try
        {
            if ( env.containsKey( "server.use.factory.instance" ) )
            {
                InitialContextFactory factory = ( InitialContextFactory ) env.get( "server.use.factory.instance" );

                if ( factory == null )
                {
                    throw new NullPointerException( "server.use.factory.instance was set in env but was null" );
                }

                // Bind is a special case where we have to use the referral property to deal
                ctx = ( LdapContext ) factory.getInitialContext( env );
            }
            else
            {
                //noinspection SuspiciousToArrayCall
                MutableControl[] connCtls = request.getControls().values().toArray( EMPTY );
                ctx = new InitialLdapContext( env, connCtls );
            }

            registry.setLdapContext( session, ctx );
            
            // add the bind response controls 
            request.getResultResponse().addAll( ctx.getResponseControls() );
            
            next.execute( session, message );
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
                code = ResultCodeEnum.getBestEstimate( e, request.getType() );
                result.setResultCode( code );
            }

            String msg = "Bind failed: " + e.getMessage();

            if ( LOG.isDebugEnabled() )
            {
                msg += ":\n" + ExceptionUtils.getStackTrace( e );
                msg += "\n\nBindRequest = \n" + request.toString();
            }

            if ( ( e.getResolvedName() != null )
                && ( ( code == ResultCodeEnum.NO_SUCH_OBJECT ) || ( code == ResultCodeEnum.ALIAS_PROBLEM )
                    || ( code == ResultCodeEnum.INVALID_DN_SYNTAX ) || ( code == ResultCodeEnum.ALIAS_DEREFERENCING_PROBLEM ) ) )
            {
                result.setMatchedDn( ( LdapDN ) e.getResolvedName() );
            }

            result.setErrorMessage( msg );
            session.write( request.getResultResponse() );
            //noinspection UnusedAssignment
            ctx = null;
        }
    }


    private Hashtable getEnvironment( IoSession session, Object message )
    {
        Object principal = session.getAttribute( Context.SECURITY_PRINCIPAL );

        /**
         * For simple, this is a password.  For strong, this is unused.
         */
        Object credentials = session.getAttribute( Context.SECURITY_CREDENTIALS );

        String sessionMechanism = ( String ) session.getAttribute( "sessionMechanism" );
        String authenticationLevel = getAuthenticationLevel( sessionMechanism );

        LOG.debug( "{} {}", Context.SECURITY_PRINCIPAL, principal );
        LOG.debug( "{} {}", Context.SECURITY_CREDENTIALS, credentials );
        LOG.debug( "{} {}", Context.SECURITY_AUTHENTICATION, authenticationLevel );

        // clone the environment first then add the required security settings
        Hashtable<String, Object> env = registry.getEnvironmentByCopy();
        env.put( Context.SECURITY_PRINCIPAL, principal );

        if ( credentials != null )
        {
            env.put( Context.SECURITY_CREDENTIALS, credentials );
        }

        env.put( Context.SECURITY_AUTHENTICATION, authenticationLevel );

        BindRequest request = ( BindRequest ) message;

        if ( request.getControls().containsKey( ManageDsaITControl.CONTROL_OID ) )
        {
            env.put( Context.REFERRAL, "ignore" );
        }
        else
        {
            env.put( Context.REFERRAL, "throw" );
        }

        return env;
    }


    private String getAuthenticationLevel( String sessionMechanism )
    {
        if ( sessionMechanism.equals( "SIMPLE" ) )
        {
            return "simple";
        }
        else
        {
            return "strong";
        }
    }
}
