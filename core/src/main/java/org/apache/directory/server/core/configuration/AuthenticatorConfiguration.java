/*
 *   @(#) $Id$
 *
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
package org.apache.directory.server.core.configuration;


import org.apache.directory.server.core.authn.Authenticator;


/**
 * A configuration for {@link Authenticator}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AuthenticatorConfiguration
{
    private String name;
    private Authenticator authenticator;


    /**
     * Creates a new instance.
     */
    protected AuthenticatorConfiguration()
    {
    }


    /**
     * Returns the {@link Authenticator} this configuration is configuring.
     */
    public Authenticator getAuthenticator()
    {
        return authenticator;
    }


    /**
     * Sets the {@link Authenticator} to configure.
     */
    protected void setAuthenticator( Authenticator authenticator )
    {
        this.authenticator = authenticator;
    }


    /**
     * Returns the user-defined name of the {@link Authenticator} that
     * this configuration configures..
     */
    public String getName()
    {
        return name;
    }


    /**
     * Sets the user-defined name of the {@link Authenticator} that
     * this configuration configures.
     */
    protected void setName( String name )
    {
        this.name = name.trim();
    }


    /**
     * Validates all properties of this configuration.
     * @throws ConfigurationException if this configuration is not valid. 
     */
    public void validate()
    {
        if ( name == null )
        {
            throw new ConfigurationException( "Name is not specified." );
        }

        if ( authenticator == null )
        {
            throw new ConfigurationException( "Authenticator is not specified." );
        }
    }
}
