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

import org.apache.seda.listener.ClientKey;
import org.apache.seda.protocol.AbstractSingleReplyHandler;

import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.message.*;


/**
 * A single reply handler for {@link org.apache.ldap.common.message.AddRequest}s.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AddHandler extends AbstractSingleReplyHandler
{
    public Object handle( ClientKey key, Object request )
    {
        AddRequest req = ( AddRequest ) request;
        AddResponse resp = new AddResponseImpl( req.getMessageId() );
        resp.setLdapResult( new LdapResultImpl( resp ) );
        InitialContext ictx = SessionRegistry.getSingleton( null ).get( key );

        LdapName dn;

        try
        {
            dn = new LdapName( req.getName() );
        }
        catch ( NamingException e )
        {
            //resp.getLdapResult().setResultCode( ResultCodeEnum);
        }

        return resp;
    }
}
