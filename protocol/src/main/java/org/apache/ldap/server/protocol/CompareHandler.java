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


import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.directory.DirContext;
import javax.naming.directory.Attribute;

import org.apache.apseda.protocol.AbstractSingleReplyHandler;
import org.apache.apseda.listener.ClientKey;

import org.apache.ldap.common.util.ExceptionUtils;
import org.apache.ldap.common.message.*;
import org.apache.ldap.common.exception.LdapException;
import org.apache.apseda.listener.ClientKey;
import org.apache.apseda.protocol.AbstractSingleReplyHandler;


/**
 * A single reply handler for {@link org.apache.ldap.common.message.CompareRequest}s.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CompareHandler extends AbstractSingleReplyHandler
{
    /**
     * @see org.apache.apseda.protocol.SingleReplyHandler#handle(org.apache.apseda.listener.ClientKey,Object)
     */
    public Object handle( ClientKey key, Object request )
    {
        CompareRequest req = ( CompareRequest ) request;
        CompareResponse resp = new CompareResponseImpl( req.getMessageId() );
        resp.setLdapResult( new LdapResultImpl( resp ) );

        try
        {
            InitialLdapContext ictx = SessionRegistry.getSingleton()
                    .getInitialLdapContext( key, null, true );
            DirContext ctx = ( DirContext ) ictx.lookup( "" );
            Attribute attr = ctx.getAttributes( req.getName() ).get( req.getAttributeId() );

            if ( attr == null )
            {
                resp.getLdapResult().setResultCode( ResultCodeEnum.COMPAREFALSE );
            }
            else if ( attr.contains( req.getAssertionValue() ) )
            {
                resp.getLdapResult().setResultCode( ResultCodeEnum.COMPARETRUE );
            }
            else
            {
                resp.getLdapResult().setResultCode( ResultCodeEnum.COMPAREFALSE );
            }
        }
        catch ( NamingException e )
        {
            String msg = "failed to add entry " + req.getName() + ":\n";
            msg += ExceptionUtils.getStackTrace( e );
            ResultCodeEnum code;

            if ( e instanceof LdapException )
            {
                code = ( ( LdapException ) e ).getResultCode() ;
            }
            else
            {
                code = ResultCodeEnum.getBestEstimate( e, req.getType() );
            }

            resp.getLdapResult().setResultCode( code );
            resp.getLdapResult().setErrorMessage( msg );

            if ( e.getResolvedName() != null )
            {
                resp.getLdapResult().setMatchedDn( e.getResolvedName().toString() );
            }

            return resp;
        }

        resp.getLdapResult().setMatchedDn( req.getName() );
        return resp;
    }
}
