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

package org.apache.directory.server.sasl;

import java.util.Hashtable;

import javax.naming.ldap.LdapContext;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;

import org.apache.directory.server.ldap.SessionRegistry;
import org.apache.directory.server.ldap.support.BindHandler;
import org.apache.directory.shared.ldap.message.BindRequest;
import org.apache.directory.shared.ldap.message.BindResponse;
import org.apache.directory.shared.ldap.message.LdapResult;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaslBindHandler extends BindHandler
{
    private static final String KEY_SASL_SERVER =
        SaslBindHandler.class.getName() + ".saslServer";
    
    private final Logger log = LoggerFactory.getLogger( SaslBindHandler.class );

    public SaslBindHandler()
    {
        super();
    }

    public void messageReceived( IoSession session, Object message ) throws Exception
    {
        LdapContext ctx;
        BindRequest req = ( BindRequest ) message;
        LdapResult result = req.getResultResponse().getLdapResult();
        Hashtable env = SessionRegistry.getSingleton().getEnvironment();

        // If the bind request is simple, then forward the request to the
        // original BindHandler implementation.
        if( req.isSimple() )
        {
            super.messageReceived( session, message );
            return;
        }
        
        SaslServer saslServer = ( SaslServer ) session.getAttribute( KEY_SASL_SERVER );
        if( saslServer == null )
        {
            String mechanism = req.getSaslMechanism().toUpperCase();
            if( mechanism.equals( "CRAM-MD5" ) || mechanism.equals( "DIGEST-MD5" ) )
            {
                // Create an instance of SaslServer
                saslServer = Sasl.createSaslServer(
                        mechanism, "ldap",
                        "example.com", null, null);
                session.setAttribute( KEY_SASL_SERVER, saslServer );
            }
            else
            {
                result.setResultCode( ResultCodeEnum.AUTHMETHODNOTSUPPORTED );
                result.setErrorMessage( "Unsupported SASL mechanism: " + mechanism );
                session.write( req.getResultResponse() );
                return;
            }
        }
        
        if( !saslServer.isComplete() ) {
            // Generate a challenge
            byte[] clientResponse = req.getCredentials();
            if( clientResponse == null ) {
                clientResponse = new byte[0];
            }
            byte[] challenge = saslServer.evaluateResponse( clientResponse );
            
            // Return the challenge if not complete
            if( !saslServer.isComplete() )
            {
                result.setResultCode( ResultCodeEnum.SASLBINDINPROGRESS );
                BindResponse response = ( BindResponse ) req.getResultResponse();
                response.setServerSaslCreds( challenge );
                session.write( req.getResultResponse() );
            }
        }
        
        // Wait for the next BindRequest if not complete yet.
        if( !saslServer.isComplete() )
        {
            return;
        }
        
        // Remove the session attribute in case user initiates bind again.
        session.removeAttribute( KEY_SASL_SERVER );
        
        result.setResultCode( ResultCodeEnum.SUCCESS );
        session.write( req.getResultResponse() );
        
        /*
        boolean emptyDn = StringTools.isEmpty( req.getName() );

        // clone the environment first then add the required security settings
        String dn = ( emptyDn ? "" : req.getName() );
        byte[] creds = req.getCredentials();

        Hashtable cloned = ( Hashtable ) env.clone();
        cloned.put( Context.SECURITY_PRINCIPAL, dn );
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
                result.setMatchedDn( e.getResolvedName().toString() );
            }

            result.setErrorMessage( msg );
            session.write( req.getResultResponse() );
            return;
        }

        SessionRegistry.getSingleton().setLdapContext( session, ctx );
        result.setResultCode( ResultCodeEnum.SUCCESS );
        session.write( req.getResultResponse() );
        */
    }
    
    
}
