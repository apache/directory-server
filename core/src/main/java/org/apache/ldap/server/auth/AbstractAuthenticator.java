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


import javax.naming.NamingException;


/**
 * Base class for all Authenticators.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractAuthenticator implements Authenticator
{

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
    public AbstractAuthenticator( String type )
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


    public void init( AuthenticatorConfig authenticatorConfig ) throws NamingException
    {
        this.authenticatorConfig = authenticatorConfig;

        this.authenticatorContext = authenticatorConfig.getAuthenticatorContext();

        init();
    }
}