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

import java.util.Enumeration;
import java.util.Properties;

/**
 * A configuration bean for Authenticators.
 *
 * @author <a href="mailto:endisd@vergenet.com">Endi S. Dewata</a>
 */
public class AuthenticatorConfig {

    private String authenticatorName;
    private String authenticatorClass;
    private AuthenticatorContext authenticatorContext;
    private Properties properties = new Properties();

    public String getAuthenticatorName()
    {
        return authenticatorName;
    }

    public void setAuthenticatorName( String authenticatorName )
    {
        this.authenticatorName = authenticatorName;
    }

    public String getAuthenticatorClass()
    {
        return authenticatorClass;
    }

    public void setAuthenticatorClass( String authenticatorClass )
    {
        this.authenticatorClass = authenticatorClass;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public void setProperties( Properties properties )
    {
        this.properties = properties;
    }

    public String getInitParameter( String name )
    {
        return properties.getProperty( name );
    }

    public Enumeration getInitParameterNames()
    {
        return properties.keys();
    }

    public AuthenticatorContext getAuthenticatorContext()
    {
        return authenticatorContext;
    }

    public void setAuthenticatorContext( AuthenticatorContext authenticatorContext )
    {
        this.authenticatorContext = authenticatorContext;
    }
}
