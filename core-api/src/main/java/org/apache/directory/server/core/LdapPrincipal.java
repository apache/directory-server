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
package org.apache.directory.server.core;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.Principal;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.util.Strings;


/**
 * An alternative X500 user implementation that has access to the distinguished
 * name of the principal as well as the String representation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class LdapPrincipal implements Principal, Cloneable, Externalizable
{
    private static final long serialVersionUID = 3906650782395676720L;

    /** the normalized distinguished name of the principal */
    private DN dn;

    /** the no name anonymous user whose DN is the empty String */
    public static final LdapPrincipal ANONYMOUS = new LdapPrincipal();

    /** the authentication level for this principal */
    private AuthenticationLevel authenticationLevel;
    
    /** The userPassword
     * @todo security risk remove this immediately
     * The field is transient to avoid being serialized
     */
    transient private byte[] userPassword;


    /**
     * Creates a new LDAP/X500 principal without any group associations.  Keep
     * this package friendly so only code in the package can create a
     * trusted principal.
     *
     * @param dn the normalized distinguished name of the principal
     * @param authenticationLevel the authentication level for this principal
     */
    public LdapPrincipal( DN dn, AuthenticationLevel authenticationLevel )
    {
        this.dn = dn;
        
        if ( ! dn.isNormalized() )
        {
            throw new IllegalStateException( I18n.err( I18n.ERR_436 ) );
        }
        
        this.authenticationLevel = authenticationLevel;
        this.userPassword = null;
    }

    /**
     * Creates a new LDAP/X500 principal without any group associations.  Keep
     * this package friendly so only code in the package can create a
     * trusted principal.
     *
     * @param dn the normalized distinguished name of the principal
     * @param authenticationLevel the authentication level for this principal
     * @param userPassword The user password
     */
    public LdapPrincipal( DN dn, AuthenticationLevel authenticationLevel, byte[] userPassword )
    {
        this.dn = dn;
        this.authenticationLevel = authenticationLevel;
        this.userPassword = new byte[ userPassword.length ];
        System.arraycopy( userPassword, 0, this.userPassword, 0, userPassword.length );
    }


    /**
     * Creates a principal for the no name anonymous user whose DN is the empty
     * String.
     */
    public LdapPrincipal()
    {
        dn = new DN();
        authenticationLevel = AuthenticationLevel.NONE;
        userPassword = null;
    }


    /**
     * Gets a reference to the distinguished name of this
     * principal as a {@link DN}.
     *
     * @return the distinguished name of the principal as a {@link DN}
     */
    public DN getDNRef()
    {
        return dn;
    }


    /**
     * Gets a cloned copy of the normalized distinguished name of this
     * principal as a {@link DN}.
     *
     * @return the cloned distinguished name of the principal as a {@link DN}
     */
    public DN getDN()
    {
        return dn;
    }


    /**
     * Returns the normalized distinguished name of the principal as a String.
     */
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


    /**
     * Returns string representation of the normalized distinguished name
     * of this principal.
     */
    public String toString()
    {
        return "['" + dn.getName() + "', '" + Strings.utf8ToString(userPassword) +"']'";
    }


    public byte[] getUserPassword()
    {
        return userPassword;
    }


    public void setUserPassword( byte[] userPassword )
    {
        this.userPassword = new byte[ userPassword.length ];
        System.arraycopy( userPassword, 0, this.userPassword, 0, userPassword.length );
    }
    
    
    /**
     * Clone the object. This is done so that we don't store the 
     * password in a LdapPrincipal more than necessary.
     */
    public Object clone() throws CloneNotSupportedException
    {
        LdapPrincipal clone = (LdapPrincipal)super.clone();
        
        if ( userPassword != null )
        {
            clone.setUserPassword( userPassword );
        }
        
        return clone;
    }
    
    
    /**
     * @see Externalizable#readExternal(ObjectInput)
     * 
     * @param in The stream from which the LdapPrincipal is read
     * @throws IOException If the stream can't be read
     * @throws ClassNotFoundException If the LdapPrincipal can't be created 
     */
    public void readExternal( ObjectInput in ) throws IOException , ClassNotFoundException
    {
        // Read the name
        dn = (DN)in.readObject();
        
        // read the authentication level
        int level = in.readInt();
        
        authenticationLevel = AuthenticationLevel.getLevel( level );
    }


    /**
     * Note: The password won't be written !
     * 
     * @see Externalizable#readExternal(ObjectInput)
     *
     * @param out The stream in which the LdapPrincipal will be serialized. 
     *
     * @throws IOException If the serialization fail
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        // Write the name
        if ( dn == null )
        {
            out.writeObject( DN.EMPTY_DN );
        }
        else
        {
            out.writeObject( dn );
        }
        
        // write the authentication level
        if ( authenticationLevel == null )
        {
            out.writeInt( AuthenticationLevel.NONE.getLevel() );
        }
        else
        {
            out.writeInt( authenticationLevel.getLevel() );
        }
    }
}
