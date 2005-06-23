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

import javax.naming.NamingException;

import org.apache.ldap.common.exception.LdapNoPermissionException;
import org.apache.ldap.server.jndi.ServerContext;

/**
 * An {@link Authenticator} that handles anonymous connections
 * (type <tt>'none'</tt>).
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AnonymousAuthenticator extends AbstractAuthenticator
{
    /**
     * Creates a new instance.
     */
    public AnonymousAuthenticator()
    {
        super( "none" );
    }

    /**
     * If the context is not configured to allow anonymous connections,
     * this method throws a {@link javax.naming.NoPermissionException}.
     */
    public LdapPrincipal authenticate( ServerContext ctx ) throws NamingException
    {
        if ( getFactoryConfiguration().getStartupConfiguration().isAllowAnonymousAccess() )
        {
            return LdapPrincipal.ANONYMOUS ;
        }
        else
        {
            throw new LdapNoPermissionException( "Anonymous bind NOT permitted!" );
        }
    }
}
