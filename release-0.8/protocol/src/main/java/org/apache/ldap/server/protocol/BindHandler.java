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
package org.apache.ldap.server.protocol;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;

import org.apache.apseda.listener.ClientKey;
import org.apache.apseda.protocol.AbstractSingleReplyHandler;

import org.apache.ldap.common.message.*;
import org.apache.ldap.common.util.ExceptionUtils;
import org.apache.ldap.common.exception.LdapException;
import org.apache.apseda.listener.ClientKey;


/**
 * A single reply handler for {@link org.apache.ldap.common.message.BindRequest}s.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BindHandler extends AbstractSingleReplyHandler
{
    private static final Control[] EMPTY = new Control[0];


    /**
     * @see org.apache.apseda.protocol.SingleReplyHandler#handle(ClientKey,Object)
     */
    public Object handle( ClientKey key, Object request )
    {
        InitialLdapContext ictx;
        BindRequest req = ( BindRequest ) request;
        BindResponse resp = new BindResponseImpl( req.getMessageId() );
        LdapResult result = new LdapResultImpl( resp );
        resp.setLdapResult( result );
        Hashtable env = SessionRegistry.getSingleton().getEnvironment();

        // if the bind request is not simple then we freak: no strong auth yet
        if ( ! req.isSimple() )
        {
            result.setResultCode( ResultCodeEnum.AUTHMETHODNOTSUPPORTED );
            result.setErrorMessage( "Only simple binds currently supported" );
            return resp;
        }

        // clone the environment first then add the required security settings
        String dn = req.getName();
        byte[] creds = req.getCredentials();

        env = ( Hashtable ) env.clone();
        env.put( Context.SECURITY_PRINCIPAL, dn );
        env.put( Context.SECURITY_CREDENTIALS, creds );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );

        Control[] connCtls = ( Control[] ) req.getControls().toArray( EMPTY );
        try
        {
            ictx = new InitialLdapContext( env, connCtls );
        }
        catch( NamingException e )
        {
            if ( e instanceof LdapException )
            {
                result.setResultCode( ( ( LdapException ) e ).getResultCode() );
            }
            else
            {
                result.setResultCode( ResultCodeEnum.getBestEstimate( e,
                        req.getType() ) );
            }

            String msg = "Bind failure:\n" + ExceptionUtils.getStackTrace( e );
            msg += "\n\nBindRequest = \n" + req.toString();
            result.setErrorMessage( msg );
            return resp;
        }

        SessionRegistry.getSingleton().setInitialLdapContext( key, ictx );
        result.setResultCode( ResultCodeEnum.SUCCESS );
        result.setMatchedDn( req.getName() );
        return resp;
    }
}
