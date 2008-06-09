/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.jndi;


import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.entry.ServerBinaryValue;
import org.apache.directory.server.core.entry.ServerStringValue;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.referral.ReferralInterceptor;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * An implementation of a JNDI LdapContext.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ServerLdapContext extends ServerDirContext implements LdapContext
{
    /** A reference to the RTeferralService interceptor */
    private transient ReferralInterceptor refService;
    

    /**
     * Creates an instance of an ServerLdapContext.
     *
     * @param service the parent service that manages this context
     * @param env the JNDI environment parameters
     * @throws NamingException the context cannot be created
     */
    public ServerLdapContext( DirectoryService service, Hashtable<String, Object> env ) throws Exception
    {
        super( service, env );
        refService = ( ( ReferralInterceptor ) service.getInterceptorChain().get( ReferralInterceptor.class.getName() ) );
    }


    /**
     * Creates a new ServerDirContext with a distinguished name which is used to
     * set the PROVIDER_URL to the distinguished name for this context.
     *
     * @param principal the directory user principal that is propagated
     * @param dn the distinguished name of this context
     * @param service the directory service core
     * @throws NamingException if there are problems instantiating 
     */
    public ServerLdapContext( DirectoryService service, LdapPrincipal principal, LdapDN dn ) throws Exception
    {
        super( service, principal, dn );
        refService = ( ( ReferralInterceptor ) 
            service.getInterceptorChain().get( ReferralInterceptor.class.getName() ) );
    }


    public ServerLdapContext( DirectoryService service, CoreSession session, LdapDN bindDn ) throws Exception
    {
        super( service, session, bindDn );
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
    public LdapContext newInstance( Control[] requestControls ) throws NamingException
    {
        ServerLdapContext ctx = null;
        try
        {
            ctx = new ServerLdapContext( getService(), getSession().getEffectivePrincipal(), ( LdapDN ) getDn() );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
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
    public boolean compare( LdapDN name, String oid, Object value ) throws NamingException
    {
        Value<?> val = null;
        
        AttributeType attributeType = getService().getRegistries().getAttributeTypeRegistry().lookup( oid );
        
        // make sure we add the request controls to operation
        if ( attributeType.getSyntax().isHumanReadable() )
        {
            if ( value instanceof String )
            {
                val = new ServerStringValue( attributeType, (String)value );
            }
            else if ( value instanceof byte[] )
            {
                val = new ServerStringValue( attributeType, StringTools.utf8ToString( (byte[])value ) );
            }
            else
            {
                throw new NamingException( "Bad value for the OID " + oid );
            }
        }
        else
        {
            if ( value instanceof String )
            {
                val = new ServerBinaryValue( attributeType, StringTools.getBytesUtf8( (String)value ) );
            }
            else if ( value instanceof byte[] )
            {
                val = new ServerBinaryValue( attributeType, (byte[])value );
            }
            else
            {
                throw new NamingException( "Bad value for the OID " + oid );
            }
        }
        
        
        CompareOperationContext opCtx = new CompareOperationContext( getSession(), name, oid, val );
        opCtx.addRequestControls( requestControls );

        // execute operation
        boolean result = false;
        try
        {
            result = super.getNexusProxy().compare( opCtx );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
        
        // extract the response controls from the operation and return
        responseControls = getResponseControls();
        requestControls = EMPTY_CONTROLS;
        return result;
    }


    /**
     * Calling this method tunnels an unbind call down into the partition holding 
     * the bindDn.  The bind() counter part is not exposed because it is automatically
     * called when you create a new initial context for a new connection (on wire) or 
     * (programatic) caller.
     * 
     * @throws NamingException if there are failures encountered while unbinding
     */
    public void ldapUnbind() throws NamingException
    {
        LdapDN principalDn = getSession().getEffectivePrincipal().getJndiName();
        UnbindOperationContext opCtx = new UnbindOperationContext( getSession() );
        opCtx.addRequestControls( requestControls );
        try
        {
            super.getNexusProxy().unbind( opCtx );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
        responseControls = opCtx.getResponseControls();
        requestControls = EMPTY_CONTROLS;
    }


    /**
     * Check if a Name is a referral
     * @param name The Name to check
     * @return <code>true</code> if the Name is a referral.
     * @throws NamingException If the Name is incorrect
     */
    public boolean isReferral( String name ) throws NamingException
    {
        return refService.isReferral( name );
    }
    

    /**
     * Check if a Name is a referral
     * @param name The Name to check
     * @return <code>true</code> if the Name is a referral.
     * @throws NamingException If the Name is incorrect
     */
    public boolean isReferral( LdapDN name ) throws NamingException
    {
        return refService.isReferral( name );
    }

    
    public ServerContext getRootContext() throws NamingException
    {
        ServerContext ctx = null;
        
        try
        {
            ctx = new ServerLdapContext( getService(), getSession().getEffectivePrincipal(), new LdapDN() );
        }
        catch ( Exception e )
        {
            JndiUtils.wrap( e );
        }
        
        return ctx;
    }
}
