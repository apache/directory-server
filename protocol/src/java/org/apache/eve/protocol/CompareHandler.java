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


import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.Attribute;

import org.apache.seda.protocol.AbstractSingleReplyHandler;
import org.apache.seda.listener.ClientKey;

import org.apache.ldap.common.util.ExceptionUtils;
import org.apache.ldap.common.message.*;


/**
 * A single reply handler for {@link org.apache.ldap.common.message.CompareRequest}s.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CompareHandler extends AbstractSingleReplyHandler
{
    /**
     * @see org.apache.seda.protocol.SingleReplyHandler#handle(ClientKey,Object)
     */
    public Object handle( ClientKey key, Object request )
    {
        CompareRequest req = ( CompareRequest ) request;
        CompareResponse resp = new CompareResponseImpl( req.getMessageId() );
        resp.setLdapResult( new LdapResultImpl( resp ) );
        InitialContext ictx = SessionRegistry.getSingleton().get( key );

        try
        {
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
            code = ResultCodeEnum.getBestEstimate( e, req.getType() );
            resp.getLdapResult().setResultCode( code );
            resp.getLdapResult().setErrorMessage( msg );

            if ( e.getResolvedName() != null )
            {
                resp.getLdapResult().setMatchedDn( e.getResolvedName().toString() );
            }
        }

        resp.getLdapResult().setMatchedDn( req.getName() );
        return resp;
    }
}
