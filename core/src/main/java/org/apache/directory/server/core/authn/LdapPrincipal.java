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


import java.io.Serializable;
import java.security.Principal;

import javax.naming.Name;

import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * An alternative X500 user implementation that has access to the distinguished
 * name of the principal as well as the String representation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public final class LdapPrincipal implements Principal, Serializable
{
    private static final long serialVersionUID = 3906650782395676720L;

    /** the normalized distinguished name of the principal */
    private final LdapDN name;

    /** the no name anonymous user whose DN is the empty String */
    public static final LdapPrincipal ANONYMOUS = new LdapPrincipal();

    /** the authentication level for this principal */
    private final AuthenticationLevel authenticationLevel;
    
    /** The userPassword
     * @todo security risk remove this immediately
     */
    private byte[] userPassword;


    /**
     * Creates a new LDAP/X500 principal without any group associations.  Keep
     * this package friendly so only code in the package can create a
     * trusted principal.
     *
     * @param name the normalized distinguished name of the principal
     * @param authenticationLevel the authentication level for this principal
     */
    public LdapPrincipal( LdapDN name, AuthenticationLevel authenticationLevel )
    {
        this.name = name;
        this.authenticationLevel = authenticationLevel;
        this.userPassword = null;
    }

    /**
     * Creates a new LDAP/X500 principal without any group associations.  Keep
     * this package friendly so only code in the package can create a
     * trusted principal.
     *
     * @param name the normalized distinguished name of the principal
     * @param authenticationLevel the authentication level for this principal
     * @param userPassword The user password
     */
    public LdapPrincipal( LdapDN name, AuthenticationLevel authenticationLevel, byte[] userPassword )
    {
        this.name = name;
        this.authenticationLevel = authenticationLevel;
        this.userPassword = userPassword;
    }


    /**
     * Creates a principal for the no name anonymous user whose DN is the empty
     * String.
     */
    public LdapPrincipal()
    {
        name = new LdapDN();
        authenticationLevel = AuthenticationLevel.NONE;
        userPassword = null;
    }


    /**
     * Gets a cloned copy of the normalized distinguished name of this
     * principal as a JNDI {@link Name}.
     *
     * @return the normalized distinguished name of the principal as a JNDI {@link Name}
     */
    public LdapDN getJndiName()
    {
        return ( LdapDN ) name.clone();
    }


    /**
     * Returns the normalized distinguished name of the principal as a String.
     */
    public String getName()
    {
        return name.getNormName();
    }


    /**
     * Gets the authentication level associated with this LDAP principle.
     *
     * @return the authentication level
     */
    public AuthenticationLevel getAuthenticationLevel()
    {
        return authenticationLevel;
    }


    /**
     * Returns string representation of the normalized distinguished name
     * of this principal.
     */
    public String toString()
    {
        return "['" + name.getUpName() + "', '" + StringTools.utf8ToString( userPassword ) +"']'";
    }


    public byte[] getUserPassword()
    {
        return userPassword;
    }


    public void setUserPassword( byte[] userPassword )
    {
        this.userPassword = userPassword;
    }
}
