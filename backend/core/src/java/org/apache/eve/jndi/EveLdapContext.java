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
package org.apache.eve.jndi;


import java.util.Hashtable;
import java.security.Principal;

import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.NotImplementedException;

import org.apache.eve.PartitionNexus;


/**
 * An Eve implementation of a JNDI LdapContext.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EveLdapContext extends EveDirContext implements LdapContext
{
    private static final Control[] EMPTY_CONTROLS = new Control[0];
    private Control[] requestControls = EMPTY_CONTROLS;
    private Control[] responseControls = EMPTY_CONTROLS;
    private Control[] connectControls = EMPTY_CONTROLS;


    /**
     * Creates an instance of an EveLdapContext.
     *
     * @param nexusProxy the proxy to a partition nexus
     * @param env the JNDI environment parameters
     * @throws NamingException the context cannot be created
     */
    public EveLdapContext( PartitionNexus nexusProxy, Hashtable env ) throws NamingException
    {
        super( nexusProxy, env );
    }


    /**
     * Creates a new EveDirContext with a distinguished name which is used to
     * set the PROVIDER_URL to the distinguished name for this context.
     *
     * @param principal the directory user principal that is propagated
     * @param nexusProxy the intercepting proxy to the nexus
     * @param env the environment properties used by this context
     * @param dn the distinguished name of this context
     */
    EveLdapContext( Principal principal, PartitionNexus nexusProxy, Hashtable env, Name dn )
    {
        super( principal, nexusProxy, env, dn );
    }


    /**
     * @see javax.naming.ldap.LdapContext#extendedOperation(
     * javax.naming.ldap.ExtendedRequest)
     */
    public ExtendedResponse extendedOperation( ExtendedRequest request )
    {
        throw new NotImplementedException();
    }


    /**
     * @see javax.naming.ldap.LdapContext#newInstance(
     * javax.naming.ldap.Control[])
     */
    public LdapContext newInstance( Control[] requestControls )
        throws NamingException
    {
        EveLdapContext ctx = new EveLdapContext( getPrincipal(), getNexusProxy(),
                getEnvironment(), getDn() );
        ctx.setRequestControls( requestControls );
        return ctx;
    }


    /**
     * @see javax.naming.ldap.LdapContext#reconnect(javax.naming.ldap.Control[])
     */
    public void reconnect( Control[] connCtls ) throws NamingException
    {
        this.connectControls = connCtls;
    }


    /**
     * @see javax.naming.ldap.LdapContext#getConnectControls()
     */
    public Control[] getConnectControls() throws NamingException
    {
        return this.connectControls;
    }


    /**
     * @see javax.naming.ldap.LdapContext#setRequestControls(
     * javax.naming.ldap.Control[])
     */
    public void setRequestControls( Control[] requestControls )
        throws NamingException
    {
        this.requestControls = requestControls;
    }


    /**
     * @see javax.naming.ldap.LdapContext#getRequestControls()
     */
    public Control[] getRequestControls() throws NamingException
    {
        return requestControls;
    }


    /**
     * @see javax.naming.ldap.LdapContext#getResponseControls()
     */
    public Control[] getResponseControls() throws NamingException
    {
        return responseControls;
    }
}
