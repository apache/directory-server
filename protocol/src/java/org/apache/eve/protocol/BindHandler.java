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
package org.apache.eve.protocol;


import java.util.Hashtable;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.seda.listener.ClientKey;
import org.apache.seda.protocol.AbstractSingleReplyHandler;

import org.apache.ldap.common.message.*;
import org.apache.ldap.common.util.ExceptionUtils;


/**
 * A single reply handler for {@link org.apache.ldap.common.message.BindRequest}s.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BindHandler extends AbstractSingleReplyHandler
{
    /**
     * @see org.apache.seda.protocol.SingleReplyHandler#handle(ClientKey,Object)
     */
    public Object handle( ClientKey key, Object request )
    {
        BindRequest req = ( BindRequest ) request;
        BindResponse resp = new BindResponseImpl( req.getMessageId() );

        if ( ! req.isSimple() )
        {
            resp.setLdapResult( new LdapResultImpl( resp ) );
            resp.getLdapResult().setResultCode( ResultCodeEnum.AUTHMETHODNOTSUPPORTED );
            resp.getLdapResult().setErrorMessage( "Only simple binds currently supported" );
            return resp;
        }

        String dn = req.getName();
        byte[] creds = req.getCredentials();
        Hashtable env = SessionRegistry.getSingleton( null ).getEnvironment();
        InitialContext ictx;

        try
        {
            ictx = new InitialContext( env );
        }
        catch( NamingException e )
        {
            resp.setLdapResult( new LdapResultImpl( resp ) );
            resp.getLdapResult().setResultCode( ResultCodeEnum.OTHER );
            String msg = "Bind failure:\n" + ExceptionUtils.getStackTrace( e );
            msg += "\n\nBindRequest = \n" + req.toString();
            resp.getLdapResult().setErrorMessage( msg );
            return resp;
        }

        SessionRegistry.getSingleton( null ).put( key, ictx );
        resp.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );
        resp.getLdapResult().setMatchedDn( req.getName() );
        return resp;
    }
}
