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


import org.apache.ldap.server.jndi.ServerContext;
import org.apache.ldap.common.name.LdapName;

import javax.naming.NamingException;
import java.util.Enumeration;


/**
 * Base class for all Authenticators.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractAuthenticator implements Authenticator, AuthenticatorConfig
{

    /** authenticator config */
    public AuthenticatorConfig authenticatorConfig;

    /** authenticator type */
    public String authenticatorType;


    /**
     * Create a new AuthenticationService.
     *
     * @param type authenticator's type
     */
    public AbstractAuthenticator( String type )
    {
        this.authenticatorType = type;
    }


    /**
     * Returns a reference to the AuthenticatorContext in which this authenticator is running.
     */
    public AuthenticatorContext getAuthenticatorContext()
    {
        return authenticatorConfig.getAuthenticatorContext();
    }


    /**
     * Returns this authenticator's type.
     */
    public String getAuthenticatorType()
    {
        return authenticatorType;
    }


    /**
     * Return this authenticator's AuthenticatorConfig object.
     */
    public AuthenticatorConfig getAuthenticatorConfig()
    {
        return authenticatorConfig;
    }


    /**
     * Called by the server to indicate to an authenticator that the authenticator
     * is being placed into service.
     */
    public void init( AuthenticatorConfig authenticatorConfig ) throws NamingException
    {
        this.authenticatorConfig = authenticatorConfig;

        init();
    }


    /**
     * A convenience method which can be overridden so that there's no need to
     * call super.init( authenticatorConfig ).
     */
    public void init() throws NamingException
    {
    }


    /**
     * Perform the authentication operation and return the authorization id if successfull.
     */
    public abstract LdapPrincipal authenticate( ServerContext ctx ) throws NamingException;


    /**
     * Returns the name of this authenticator instance.
     */
    public String getAuthenticatorName()
    {
        return authenticatorConfig.getAuthenticatorName();
    }


    /**
     * Returns a String containing the value of the named initialization parameter, or null if the parameter does not exist.
     */
    public String getInitParameter( String name )
    {
        return authenticatorConfig.getInitParameter( name );
    }


    /**
     * Returns the names of the servlet's initialization parameters as an Enumeration of String objects, or an empty Enumeration if the servlet has no initialization parameters.
     */
    public Enumeration getInitParameterNames()
    {
        return authenticatorConfig.getInitParameterNames();
    }


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
