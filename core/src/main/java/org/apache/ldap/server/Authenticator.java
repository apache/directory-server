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
package org.apache.ldap.server;

import org.apache.ldap.server.jndi.ServerContext;
import org.apache.ldap.server.auth.LdapPrincipal;

import javax.naming.NamingException;


/**
 * Base class for all Authenticators.
 *
 * @author <a href="mailto:endisd@vergenet.com">Endi S. Dewata</a>
 */
public class Authenticator {

    /** authenticator config */
    public AuthenticatorConfig authenticatorConfig;
    /** authenticator context */
    public AuthenticatorContext authenticatorContext;
    /** authenticator type */
    public String type;

    /**
     * Create a new Authenticator.
     *
     * @param type authenticator's type
     */
    public Authenticator( String type )
    {
        this.type = type;
    }

    public AuthenticatorContext getAuthenticatorContext()
    {
        return authenticatorContext;
    }

    public String getType()
    {
        return type;
    }

    /**
     * Called by the authenticator container to indicate that the authenticator is being placed into service.
     *
     * @param authenticatorConfig
     * @throws NamingException
     */
    public void init( AuthenticatorConfig authenticatorConfig ) throws NamingException
    {
        this.authenticatorConfig = authenticatorConfig;
        this.authenticatorContext = authenticatorConfig.getAuthenticatorContext();
        init();
    }

    /**
     * A convenience method which can be overridden so that there's no need to call super.init( authenticatorConfig ).
     */
    public void init() throws NamingException
    {

    }

    /**
     * Perform the authentication operation and return the authorization id if successfull.
     *
     * @param ctx
     * @return the authorization id
     * @throws NamingException
     */
    public LdapPrincipal authenticate( ServerContext ctx ) throws NamingException
    {
        return null;
    }

}