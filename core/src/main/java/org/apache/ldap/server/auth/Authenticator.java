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
package org.apache.ldap.server.auth;


import org.apache.ldap.server.jndi.ServerContext;

import javax.naming.NamingException;


/**
 * Endi when you have a chance please document this class with the proper javadocs.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Authenticator
{
    AuthenticatorContext getAuthenticatorContext();

    String getType();

    /**
     * Called by the authenticator container to indicate that the authenticator is being placed into service.
     *
     * @param authenticatorConfig
     * @throws NamingException
     */
    void init( AuthenticatorConfig authenticatorConfig ) throws NamingException;

    /**
     * A convenience method which can be overridden so that there's no need to call super.init( authenticatorConfig ).
     */
    void init() throws NamingException;

    /**
     * Perform the authentication operation and return the authorization id if successfull.
     *
     * @param ctx
     * @return the authorization id
     * @throws NamingException
     */
    LdapPrincipal authenticate( ServerContext ctx ) throws NamingException;
}
