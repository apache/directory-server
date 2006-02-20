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
package org.apache.directory.server.core.authn;


import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.AuthenticatorConfiguration;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.name.LdapName;


/**
 * Base class for all Authenticators.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractAuthenticator implements Authenticator
{
    private DirectoryServiceConfiguration factoryCfg;
    private AuthenticatorConfiguration cfg;

    /** authenticator type */
    private String authenticatorType;


    /**
     * Creates a new instance.
     *
     * @param type the type of this authenticator (e.g. <tt>'simple'</tt>, <tt>'none'</tt>...)
     */
    protected AbstractAuthenticator(String type)
    {
        this.authenticatorType = type;
    }


    /**
     * Returns {@link DirectoryServiceConfiguration} of {@link InitialContextFactory}
     * which initialized this authenticator.
     */
    public DirectoryServiceConfiguration getFactoryConfiguration()
    {
        return factoryCfg;
    }


    /**
     * Returns the configuration of this authenticator.
     */
    public AuthenticatorConfiguration getConfiguration()
    {
        return cfg;
    }


    public String getAuthenticatorType()
    {
        return authenticatorType;
    }


    /**
     * Initializes default properties (<tt>factoryConfiguration</tt> and
     * <tt>configuration</tt>, and calls {@link #doInit()} method.
     * Please put your initialization code into {@link #doInit()}.
     */
    public final void init( DirectoryServiceConfiguration factoryCfg, AuthenticatorConfiguration cfg )
        throws NamingException
    {
        this.factoryCfg = factoryCfg;
        this.cfg = cfg;
        doInit();
    }


    /**
     * Implement your initialization code here.
     */
    protected void doInit() throws NamingException
    {
    }


    /**
     * Calls {@link #doDestroy()} method, and clears default properties
     * (<tt>factoryConfiguration</tt> and <tt>configuration</tt>).
     * Please put your deinitialization code into {@link #doDestroy()}. 
     */
    public final void destroy()
    {
        try
        {
            doDestroy();
        }
        finally
        {
            this.factoryCfg = null;
            this.cfg = null;
        }
    }


    /**
     * Implement your deinitialization code here.
     */
    protected void doDestroy()
    {
    }


    public abstract LdapPrincipal authenticate( ServerContext ctx ) throws NamingException;


    /**
     * Returns a new {@link LdapPrincipal} instance whose value is the specified
     * <tt>name</tt>.
     *
     * @param name the distinguished name of the X.500 principal
     * @param authenticationLeve
     * @return the principal for the <tt>name</tt>
     * @throws NamingException if there is a problem parsing <tt>name</tt>
     */
    // does not seem to be used
    protected static LdapPrincipal createLdapPrincipal( String name, AuthenticationLevel authenticationLeve )
        throws NamingException
    {
        LdapName principalDn = new LdapName( name );
        return new LdapPrincipal( principalDn, AuthenticationLevel.SIMPLE );
    }
}
