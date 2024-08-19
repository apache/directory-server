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
package org.apache.directory.server.core.api;


import java.net.SocketAddress;
import java.security.Principal;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.i18n.I18n;


/**
 * An alternative X500 user implementation that has access to the distinguished
 * name of the principal as well as the String representation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class LdapPrincipal implements Principal, Cloneable
{
    /** the normalized distinguished name of the principal */
    private Dn dn = Dn.EMPTY_DN;

    /** the authentication level for this principal */
    private AuthenticationLevel authenticationLevel;

    /** The userPassword
     * @todo security risk remove this immediately
     */
    private byte[][] userPasswords;

    /** The SchemaManager */
    private SchemaManager schemaManager;

    private SocketAddress clientAddress;
    private SocketAddress serverAddress;


    /**
     * Creates a new LDAP/X500 principal without any group associations.  Keep
     * this package friendly so only code in the package can create a
     * trusted principal.
     *
     * @param schemaManager The SchemaManager
     * @param dn the normalized distinguished name of the principal
     * @param authenticationLevel the authentication level for this principal
     */
    public LdapPrincipal( SchemaManager schemaManager, Dn dn, AuthenticationLevel authenticationLevel )
    {
        this.schemaManager = schemaManager;
        this.dn = dn;

        if ( !dn.isSchemaAware() )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_02038_NAMES_OF_PRINCIPAL_MUST_BE_NORMALIZED ) );
        }

        this.authenticationLevel = authenticationLevel;
        this.userPasswords = null;
    }


    /**
     * Creates a new LDAP/X500 principal without any group associations.  Keep
     * this package friendly so only code in the package can create a
     * trusted principal.
     *
     * @param schemaManager The SchemaManager
     * @param dn the normalized distinguished name of the principal
     * @param authenticationLevel the authentication level for this principal
     * @param userPassword The user password
     */
    public LdapPrincipal( SchemaManager schemaManager, Dn dn, AuthenticationLevel authenticationLevel,
        byte[] userPassword )
    {
        this.dn = dn;
        this.authenticationLevel = authenticationLevel;
        this.userPasswords = new byte[1][];
        this.userPasswords[0] = new byte[userPassword.length];
        System.arraycopy( userPassword, 0, this.userPasswords[0], 0, userPassword.length );
        this.schemaManager = schemaManager;
    }


    /**
     * Creates a principal for the no name anonymous user whose Dn is the empty
     * String.
     */
    public LdapPrincipal()
    {
        authenticationLevel = AuthenticationLevel.NONE;
        userPasswords = null;
    }


    /**
     * Creates a principal for the no name anonymous user whose Dn is the empty
     * String.
     * 
     * @param schemaManager The SchemaManager
     */
    public LdapPrincipal( SchemaManager schemaManager )
    {
        authenticationLevel = AuthenticationLevel.NONE;
        userPasswords = null;
        this.schemaManager = schemaManager;
    }


    /**
     * Gets a cloned copy of the normalized distinguished name of this
     * principal as a {@link org.apache.directory.api.ldap.model.name.Dn}.
     *
     * @return the cloned distinguished name of the principal as a {@link org.apache.directory.api.ldap.model.name.Dn}
     */
    public Dn getDn()
    {
        return dn;
    }


    /**
     * Returns the normalized distinguished name of the principal as a String.
     */
    @Override
    public String getName()
    {
        return dn.getNormName();
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


    public byte[][] getUserPasswords()
    {
        return userPasswords;
    }


    public void setUserPassword( byte[]... userPasswords )
    {
        this.userPasswords = new byte[userPasswords.length][];
        int pos = 0;

        for ( byte[] userPassword : userPasswords )
        {
            this.userPasswords[pos] = new byte[userPassword.length];
            System.arraycopy( userPassword, 0, this.userPasswords[pos], 0, userPassword.length );
            pos++;
        }
    }


    /**
     * Clone the object. This is done so that we don't store the 
     * password in a LdapPrincipal more than necessary.
     */
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        LdapPrincipal clone = ( LdapPrincipal ) super.clone();

        if ( userPasswords != null )
        {
            clone.setUserPassword( userPasswords );
        }

        return clone;
    }


    /**
     * @return the schemaManager
     */
    public SchemaManager getSchemaManager()
    {
        return schemaManager;
    }


    /**
     * @param schemaManager the schemaManager to set
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;

        if ( !dn.isSchemaAware() )
        {
            try
            {
                dn = new Dn( schemaManager, dn );
            }
            catch ( LdapInvalidDnException lide )
            {
                // TODO: manage this exception
            }
        }
    }


    /**
     * @return the clientAddress
     */
    public SocketAddress getClientAddress()
    {
        return clientAddress;
    }


    /**
     * @param clientAddress the clientAddress to set
     */
    public void setClientAddress( SocketAddress clientAddress )
    {
        this.clientAddress = clientAddress;
    }


    /**
     * @return the serverAddress
     */
    public SocketAddress getServerAddress()
    {
        return serverAddress;
    }


    /**
     * @param serverAddress the serverAddress to set
     */
    public void setServerAddress( SocketAddress serverAddress )
    {
        this.serverAddress = serverAddress;
    }


    /**
     * Returns string representation of the normalized distinguished name
     * of this principal.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if ( dn.isSchemaAware() )
        {
            sb.append( "(n)" );
        }

        sb.append( "['" );
        sb.append( dn.getName() );
        sb.append( "'" );

        if ( clientAddress != null )
        {
            sb.append( ", client@" );
            sb.append( clientAddress );
        }

        if ( serverAddress != null )
        {
            sb.append( ", server@" );
            sb.append( serverAddress );
        }

        sb.append( "]" );

        return sb.toString();
    }
}
