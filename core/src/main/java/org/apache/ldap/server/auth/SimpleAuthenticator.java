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
package org.apache.ldap.server.auth;

import org.apache.ldap.server.RootNexus;
import org.apache.ldap.server.jndi.ServerContext;
import org.apache.ldap.common.exception.LdapNameNotFoundException;
import org.apache.ldap.common.exception.LdapAuthenticationException;
import org.apache.ldap.common.util.ArrayUtils;
import org.apache.ldap.common.name.LdapName;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;

/**
 * @author <a href="mailto:endisd@vergenet.com">Endi S. Dewata</a>
 */
public class SimpleAuthenticator extends Authenticator {

    public SimpleAuthenticator( )
    {
        super( "simple" );
    }

    public LdapPrincipal authenticate( ServerContext ctx ) throws NamingException
    {
        Object creds = ctx.getEnvironment().get( Context.SECURITY_CREDENTIALS );

        if ( creds == null )
        {
            creds = ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        else if ( creds instanceof String )
        {
            creds = ( ( String ) creds ).getBytes();
        }

        // let's get the principal now
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

        LdapName principalDn = new LdapName( principal );
        RootNexus rootNexus = getAuthenticatorContext().getRootNexus();
        Attributes userEntry = rootNexus.lookup( principalDn );

        if ( userEntry == null )
        {
            throw new LdapNameNotFoundException();
        }

        Object userPassword;
        Attribute userPasswordAttr = userEntry.get( "userPassword" );
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
