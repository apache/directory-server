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

/**
 * An authenticator configuration object used by the server to pass information to an authenticator
 * during initialization.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface AuthenticatorConfig {

    /**
     * Returns the name of this authenticator instance.
     */
    public String getAuthenticatorName();

    /**
     * Returns a String containing the value of the named initialization parameter, or null if the parameter does not exist.
     */
    public String getInitParameter( String name );

    /**
     * Returns the names of the servlet's initialization parameters as an Enumeration of String objects, or an empty Enumeration if the servlet has no initialization parameters.
     */
    public Enumeration getInitParameterNames();

    /**
     * Returns a reference to the AuthenticatorContext.
     */
    public AuthenticatorContext getAuthenticatorContext();
}
