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


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.spi.InitialContextFactory;

import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.Control;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ManageDsaITControl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.demux.MessageHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A single reply handler for {@link org.apache.directory.shared.ldap.message.BindRequest}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BindHandler implements MessageHandler
{
    private static final Logger log = LoggerFactory.getLogger( BindHandler.class );
    private static final Control[] EMPTY = new Control[0];


    public void messageReceived( IoSession session, Object request ) throws Exception
    {
        LdapContext ctx;
        BindRequest req = ( BindRequest ) request;
        LdapResult result = req.getResultResponse().getLdapResult();
        Hashtable env = SessionRegistry.getSingleton().getEnvironment();

        // if the bind request is not simple then we freak: no strong auth yet
        if ( !req.isSimple() )
        {
            result.setResultCode( ResultCodeEnum.AUTHMETHODNOTSUPPORTED );
            result.setErrorMessage( "Only simple binds currently supported" );
            session.write( req.getResultResponse() );
            return;
        }

        // clone the environment first then add the required security settings
        byte[] creds = req.getCredentials();

        Hashtable cloned = ( Hashtable ) env.clone();
        cloned.put( Context.SECURITY_PRINCIPAL, req.getName() );
        cloned.put( Context.SECURITY_CREDENTIALS, creds );
        cloned.put( Context.SECURITY_AUTHENTICATION, "simple" );

        if ( req.getControls().containsKey( ManageDsaITControl.CONTROL_OID ) )
        {
            cloned.put( Context.REFERRAL, "ignore" );
        }
        else
        {
            cloned.put( Context.REFERRAL, "throw" );
        }

        try
        {
            if ( cloned.containsKey( "server.use.factory.instance" ) )
            {
                InitialContextFactory factory = ( InitialContextFactory ) cloned.get( "server.use.factory.instance" );

                if ( factory == null )
                {
                    throw new NullPointerException( "server.use.factory.instance was set in env but was null" );
                }

                // Bind is a special case where we have to use the referral property to deal
                ctx = ( LdapContext ) factory.getInitialContext( cloned );
            }
            else
            {
                Control[] connCtls = ( Control[] ) req.getControls().values().toArray( EMPTY );
                ctx = new InitialLdapContext( cloned, connCtls );
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
                code = ResultCodeEnum.getBestEstimate( e, req.getType() );
                result.setResultCode( code );
            }

            String msg = "Bind failed";
            if ( log.isDebugEnabled() )
            {
                msg += ":\n" + ExceptionUtils.getStackTrace( e );
                msg += "\n\nBindRequest = \n" + req.toString();
            }

            if ( ( e.getResolvedName() != null )
                && ( ( code == ResultCodeEnum.NOSUCHOBJECT ) || ( code == ResultCodeEnum.ALIASPROBLEM )
                    || ( code == ResultCodeEnum.INVALIDDNSYNTAX ) || ( code == ResultCodeEnum.ALIASDEREFERENCINGPROBLEM ) ) )
            {
                result.setMatchedDn( (LdapDN)e.getResolvedName() );
            }

            result.setErrorMessage( msg );
            session.write( req.getResultResponse() );
            return;
        }

        SessionRegistry.getSingleton().setLdapContext( session, ctx );
        result.setResultCode( ResultCodeEnum.SUCCESS );
        session.write( req.getResultResponse() );
    }
}
