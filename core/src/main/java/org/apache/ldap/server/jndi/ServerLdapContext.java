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
package org.apache.ldap.server.jndi;


import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.NotImplementedException;
import org.apache.ldap.server.DirectoryService;
import org.apache.ldap.server.authn.LdapPrincipal;
import org.apache.ldap.server.referral.ReferralService;

import com.sun.jndi.ldap.LdapName;


/**
 * An implementation of a JNDI LdapContext.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ServerLdapContext extends ServerDirContext implements LdapContext
{
    private static final Control[] EMPTY_CONTROLS = new Control[0];
    private Control[] requestControls = EMPTY_CONTROLS;
    private Control[] responseControls = EMPTY_CONTROLS;
    private Control[] connectControls = EMPTY_CONTROLS;


    /**
     * Creates an instance of an ServerLdapContext.
     *
     * @param service the parent service that manages this context
     * @param env the JNDI environment parameters
     * @throws NamingException the context cannot be created
     */
    public ServerLdapContext( DirectoryService service, Hashtable env ) throws NamingException
    {
        super( service, env );
    }


    /**
     * Creates a new ServerDirContext with a distinguished name which is used to
     * set the PROVIDER_URL to the distinguished name for this context.
     *
     * @param principal the directory user principal that is propagated
     * @param nexusProxy the intercepting proxy to the nexus
     * @param env the environment properties used by this context
     * @param dn the distinguished name of this context
     */
    ServerLdapContext( DirectoryService service, LdapPrincipal principal, Name dn ) throws NamingException
    {
        super( service, principal, dn );
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
        ServerLdapContext ctx = new ServerLdapContext( getService(), getPrincipal(), getDn() );
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
    public void setRequestControls( Control[] requestControls ) throws NamingException
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


    // ------------------------------------------------------------------------
    // Additional ApacheDS Specific JNDI Functionality
    // ------------------------------------------------------------------------


    /**
     * Explicitly exposes an LDAP compare operation which JNDI does not
     * directly provide.  All normalization and schema checking etcetera
     * is handled by this call.
     *
     * @param name the name of the entri
     * @param oid the name or object identifier for the attribute to compare
     * @param value the value to compare the attribute to
     * @return true if the entry has the value for the attribute, false otherwise
     * @throws NamingException if the backing store cannot be accessed, or
     * permission is not allowed for this operation or the oid is not recognized,
     * or the attribute is not present in the entry ... you get the picture.
     */
    public boolean compare( Name name, String oid, Object value ) throws NamingException
    {
       return super.getNexusProxy().compare( name, oid, value );
    }
    
    
    /**
     * Calling this method tunnels an unbind call down into the partition holding 
     * the bindDn.  The bind() counter part is not exposed because it is automatically
     * called when you create a new initial context for a new connection (on wire) or 
     * (programatic) caller.
     * 
     * @throws NamingException
     */
    public void ldapUnbind() throws NamingException
    {
        String bindDn = ( String ) getEnvironment().get( Context.SECURITY_PRINCIPAL );
        super.getNexusProxy().unbind( new LdapName( bindDn ) );
    }
    
    
    private transient ReferralService refService;
    public boolean isReferral( String name ) throws NamingException
    {
        if ( refService == null )
        {
            refService = ( ReferralService ) getService().getConfiguration().getInterceptorChain().
                get( ReferralService.NAME );
        }
        
        return refService.isReferral( name );
    }
}
