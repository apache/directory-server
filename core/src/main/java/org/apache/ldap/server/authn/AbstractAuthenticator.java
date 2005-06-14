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

import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.jndi.ServerContext;


/**
 * Base class for all Authenticators.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractAuthenticator implements Authenticator
{

    /** authenticator config */
    private AuthenticatorContext ctx;

    /** authenticator type */
    private String authenticatorType;


    /**
     * Create a new AuthenticationService.
     *
     * @param type authenticator's type
     */
    protected AbstractAuthenticator( String type )
    {
        this.authenticatorType = type;
    }


    /**
     * Returns a reference to the AuthenticatorContext in which this authenticator is running.
     */
    public AuthenticatorContext getContext()
    {
        return ctx;
    }


    /**
     * Returns this authenticator's type.
     */
    public String getAuthenticatorType()
    {
        return authenticatorType;
    }


    /**
     * Called by the server to indicate to an authenticator that the authenticator
     * is being placed into service.
     */
    public final void init( AuthenticatorContext ctx ) throws NamingException
    {
        this.ctx = ctx;
        doInit();
    }


    /**
     * A convenience method which can be overridden so that there's no need to
     * call super.init( authenticatorConfig ).
     */
    protected abstract void doInit();


    /**
     * Perform the authentication operation and return the authorization id if successfull.
     */
    public abstract LdapPrincipal authenticate( ServerContext ctx ) throws NamingException;


    /**
     * Allows a means to create an LDAP principal without exposing LdapPrincipal creation
     * to the rest of the world.
     *
     * @param dn the distinguished name of the X.500 principal
     * @return the principal for the dn
     * @throws NamingException if there is a problem parsing the dn
     */
    protected LdapPrincipal createLdapPrincipal( String dn ) throws NamingException
    {
        LdapName principalDn = new LdapName( dn );
        return new LdapPrincipal( principalDn );
    }
}
