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
package org.apache.ldap.server.authn;

import org.apache.ldap.common.exception.LdapNoPermissionException;
import org.apache.ldap.server.jndi.ServerContext;

import javax.naming.NamingException;

/**
 * A default implentation of an AuthenticationService for handling anonymous connections.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AnonymousAuthenticator extends AbstractAuthenticator
{
    public AnonymousAuthenticator()
    {
        super( "none" );
    }

    protected void doInit()
    {
    }

    /**
     * This will be called when the authentication is set to "none" on the client.
     * If server is not configured to allow anonymous connections, it throws an exception.
     */
    public LdapPrincipal authenticate( ServerContext ctx ) throws NamingException
    {
        if ( getContext().getRootConfiguration().isAllowAnonymousAccess() )
        {
            return LdapPrincipal.ANONYMOUS ;
        }
        else
        {
            throw new LdapNoPermissionException( "Anonymous bind NOT permitted!" );
        }
    }
}
