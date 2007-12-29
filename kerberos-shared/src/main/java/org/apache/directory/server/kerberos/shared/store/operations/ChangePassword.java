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
package org.apache.directory.server.kerberos.shared.store.operations;


import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.server.protocol.shared.store.ContextOperation;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;


/**
 * Command for changing a principal's password in a JNDI context.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChangePassword implements ContextOperation
{
    private static final long serialVersionUID = -7147685183641418353L;

    /** The Kerberos principal who's password is to be changed. */
    protected KerberosPrincipal principal;
    /** The new password for the update. */
    protected String newPassword;


    /**
     * Creates the action to be used against the embedded ApacheDS DIT.
     * 
     * @param principal The principal to change the password for.
     * @param newPassword The password to change.
     */
    public ChangePassword( KerberosPrincipal principal, String newPassword )
    {
        this.principal = principal;
        this.newPassword = newPassword;
    }


    public Object execute( DirContext ctx, Name searchBaseDn ) throws NamingException
    {
        if ( principal == null )
        {
            return null;
        }

        ModificationItemImpl[] mods = new ModificationItemImpl[2];
        Attribute newPasswordAttribute = new AttributeImpl( SchemaConstants.USER_PASSWORD_AT, newPassword );
        mods[0] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, newPasswordAttribute );
        Attribute principalAttribute = new AttributeImpl( "krb5PrincipalName", principal.getName() );
        mods[1] = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, principalAttribute );

        String dn = null;

        dn = search( ctx, principal.getName() );
        Name rdn = getRelativeName( ctx.getNameInNamespace(), dn );
        ctx.modifyAttributes( rdn, mods );

        return dn;
    }


    private String search( DirContext ctx, String principal ) throws NamingException
    {
        String[] attrIDs =
            { KerberosAttribute.KRB5_PRINCIPAL_NAME_AT, KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT, KerberosAttribute.KRB5_KEY_AT };

        Attributes matchAttrs = new AttributesImpl( true );
        matchAttrs.put( new AttributeImpl( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT, principal ) );

        NamingEnumeration<SearchResult> answer = ctx.search( "", matchAttrs, attrIDs );

        if ( answer.hasMore() )
        {
            SearchResult sr = answer.next();
            if ( sr != null )
            {
                return sr.getName();
            }
        }

        return null;
    }


    private Name getRelativeName( String nameInNamespace, String baseDn ) throws NamingException
    {
        Properties props = new Properties();
        props.setProperty( "jndi.syntax.direction", "right_to_left" );
        props.setProperty( "jndi.syntax.separator", "," );
        props.setProperty( "jndi.syntax.ignorecase", "true" );
        props.setProperty( "jndi.syntax.trimblanks", "true" );

        Name searchBaseDn = null;

        Name ctxRoot = new CompoundName( nameInNamespace, props );
        searchBaseDn = new CompoundName( baseDn, props );

        if ( !searchBaseDn.startsWith( ctxRoot ) )
        {
            throw new NamingException( "Invalid search base " + baseDn );
        }

        for ( int ii = 0; ii < ctxRoot.size(); ii++ )
        {
            searchBaseDn.remove( 0 );
        }

        return searchBaseDn;
    }
}
