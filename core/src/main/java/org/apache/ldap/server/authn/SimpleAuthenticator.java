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
package org.apache.ldap.server.authn;


import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.ldap.common.exception.LdapAuthenticationException;
import org.apache.ldap.common.exception.LdapNameNotFoundException;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.util.ArrayUtils;
import org.apache.ldap.server.PartitionNexus;
import org.apache.ldap.server.authn.AbstractAuthenticator;
import org.apache.ldap.server.authn.LdapPrincipal;
import org.apache.ldap.server.jndi.ServerContext;


/**
 * A simple Authenticator that just authenticates clear text passwords
 * contained within the <code>userPassword</code> attribute.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SimpleAuthenticator extends AbstractAuthenticator
{
    /**
     * Creates a simple authenticator for clear text passwords in
     * userPassword attributes.
     */
    public SimpleAuthenticator( )
    {
        super( "simple" );
    }


    /**
     * Uses the userPassword field of the user to authenticate.
     *
     * @see org.apache.ldap.server.authn.Authenticator#authenticate(org.apache.ldap.server.jndi.ServerContext)
     */
    public LdapPrincipal authenticate( ServerContext ctx ) throws NamingException
    {
        // ---- extract password from JNDI environment

        Object creds = ctx.getEnvironment().get( Context.SECURITY_CREDENTIALS );

        if ( creds == null )
        {
            creds = ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        else if ( creds instanceof String )
        {
            creds = ( ( String ) creds ).getBytes();
        }

        // ---- extract principal from JNDI environment

        String principal;

        if ( ! ctx.getEnvironment().containsKey( Context.SECURITY_PRINCIPAL ) )
        {
            throw new LdapAuthenticationException();
        }
        else
        {
            principal = ( String ) ctx.getEnvironment().get( Context.SECURITY_PRINCIPAL );

            if ( principal == null )
            {
                throw new LdapAuthenticationException();
            }
        }

        // ---- lookup the principal entry's userPassword attribute

        LdapName principalDn = new LdapName( principal );

        PartitionNexus rootNexus = getAuthenticatorContext().getPartitionNexus();

        Attributes userEntry = rootNexus.lookup( principalDn );

        if ( userEntry == null )
        {
            throw new LdapNameNotFoundException();
        }

        Object userPassword;

        Attribute userPasswordAttr = userEntry.get( "userPassword" );

        // ---- assert that credentials match

        if ( userPasswordAttr == null )
        {
            userPassword = ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        else
        {
            userPassword = userPasswordAttr.get();

            if ( userPassword instanceof String )
            {
                userPassword = ( ( String ) userPassword ).getBytes();
            }
        }

        if ( ! ArrayUtils.isEquals( creds, userPassword ) )
        {
            throw new LdapAuthenticationException();
        }

        return new LdapPrincipal( principalDn );
    }
}
