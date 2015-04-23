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
package org.apache.directory.server.core.authn;


import javax.naming.Context;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyException;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.shared.partition.DefaultPartitionNexus;


/**
 * Authenticates users who access {@link DefaultPartitionNexus}.
 * <p>
 * {@link Authenticator}s are registered to and configured by
 * {@link AuthenticationInterceptor} interceptor.
 * <p>
 * {@link AuthenticationInterceptor} authenticates users by calling
 * {@link #authenticate(BindOperationContext)}, and then {@link Authenticator}
 * checks JNDI {@link Context} environment properties
 * ({@link Context#SECURITY_PRINCIPAL} and {@link Context#SECURITY_CREDENTIALS})
 * of current {@link Context}.
 *
 * @see AbstractAuthenticator
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface Authenticator
{
    /**
     * Returns the type of this authenticator (e.g. <tt>'simple'</tt>,
     * <tt>'none'</tt>,...).
     */
    AuthenticationLevel getAuthenticatorType();


    /**
     * Called by {@link AuthenticationInterceptor} to indicate that this
     * authenticator is being placed into service.
     */
    void init( DirectoryService directoryService ) throws LdapException;


    /**
     * Called by {@link AuthenticationInterceptor} to indicate that this
     * authenticator is being removed from service.
     */
    void destroy();


    /**
     * Callback used to respond to password changes by invalidating a password
     * cache if implemented.  This is an additional feature of an authenticator
     * which need not be implemented: empty implementation is sufficient.  This
     * is called on every del, modify, and modifyRdn operation.
     * 
     * @param bindDn the already normalized distinguished name of the bind principal
     */
    void invalidateCache( Dn bindDn );


    /**
     * Performs authentication and returns the principal if succeeded.
     * 
     * @param bindContext The Bind context
     * @exception If the authentication failed
     */
    LdapPrincipal authenticate( BindOperationContext bindContext ) throws Exception;


    /**
     *  performs checks on the given entry based on the specified password policy configuration
     *
     * @param userEntry the user entry to be checked for authentication
     * @throws PasswordPolicyException
     */
    void checkPwdPolicy( Entry userEntry ) throws LdapException;


    /**
     * Check that this selector is a valid one. The DN we want to authenticate has to be 
     * part of the DIT selection associated with teh Authenticator
     *
     * @param bindDn The DN we want to authenticate
     * @return <code>true</code> if the Auhenticator is supporting the DN
     */
    boolean isValid( Dn bindDn );


    /**
     * @return The Authenticator base DN
     */
    Dn getBaseDn();


    /**
     * Set the baseDN into the Authenticator
     * 
     * @param baseDn The Base DN to set
     */
    public void setBaseDn( Dn baseDn );

    /**
     * Performs an unbind on the given context
     * 
     * @param unbindContext The Unbind context
     * @exception If the unbind failed
     */
    //void unbind( UnbindOperationContext unbindContext ) throws Exception;
}
