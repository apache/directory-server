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


import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;

import org.apache.eve.RootNexus;
import org.apache.eve.auth.LdapPrincipal;
import org.apache.ldap.common.exception.*;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.util.ArrayUtils;
import org.apache.ldap.common.name.LdapName;


/**
 * A service used to for authenticating users.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AuthenticationService implements Interceptor
{
    /** short for Context.SECURITY_AUTHENTICATION */
    private static final String AUTH_TYPE = Context.SECURITY_AUTHENTICATION;
    /** short for Context.SECURITY_PRINCIPAL */
    private static final String PRINCIPAL = Context.SECURITY_PRINCIPAL;
    /** short for Context.SECURITY_CREDENTIALS */
    private static final String CREDS = Context.SECURITY_CREDENTIALS;

    /** the root nexus to all database partitions */
    private final RootNexus nexus;
    /** whether or not to allow anonymous users */
    private boolean allowAnonymous = false;


    /**
     * Creates an authentication service interceptor.
     *
     * @param nexus the root nexus to access all database partitions
     */
    public AuthenticationService( RootNexus nexus, boolean allowAnonymous )
    {
        this.nexus = nexus;
        this.allowAnonymous = allowAnonymous;
    }


    public void invoke( Invocation invocation ) throws NamingException
    {
        // only handle preinvocation state
        if ( invocation.getState() != InvocationStateEnum.PREINVOCATION )
        {
            return;
        }

        // check if we are already authenticated and if so we return making
        // sure first that the credentials are not exposed within context
        EveContext ctx = ( EveLdapContext ) invocation.getContextStack().peek();
        if ( ctx.getPrincipal() != null )
        {
            if ( ctx.getEnvironment().containsKey( CREDS ) )
            {
                ctx.removeFromEnvironment( CREDS );
            }

            return;
        }

        // check the kind of authentication being performed
        if ( ctx.getEnvironment().containsKey( AUTH_TYPE ) )
        {
            // authentication type can be anything

            String auth = ( String ) ctx.getEnvironment().get( AUTH_TYPE );
            if ( auth.equalsIgnoreCase( "none" ) )
            {
                doAuthNone( ctx );
            }
            else if ( auth.equalsIgnoreCase( "simple" ) )
            {
                doAuthSimple( ctx );
            }
            else
            {
                doAuthSasl( ctx );
            }
        }
        else if ( ctx.getEnvironment().containsKey( CREDS ) )
        {
            // authentication type is simple here
            doAuthSimple( ctx );
        }
        else
        {
            // authentication type is anonymous
            doAuthNone( ctx );
        }

        // remove creds so there is no security risk
        ctx.removeFromEnvironment( CREDS );
    }


    private void doAuthSasl( EveContext ctx ) throws NamingException
    {
        ctx.getEnvironment(); // shut's up idea's yellow light
        ResultCodeEnum rc = ResultCodeEnum.AUTHMETHODNOTSUPPORTED; 
        throw new LdapAuthenticationNotSupportedException( rc );
    }


    private void doAuthNone( EveContext ctx ) throws NamingException
    {
        if ( allowAnonymous )
        {
            ctx.setPrincipal( LdapPrincipal.ANONYMOUS );
        }
        else
        {
            throw new LdapNoPermissionException( "Anonymous bind NOT permitted!" );
        }
    }


    private void doAuthSimple( EveContext ctx ) throws NamingException
    {
        Object creds = ctx.getEnvironment().get( CREDS );

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
        if ( ! ctx.getEnvironment().containsKey( PRINCIPAL ) )
        {
            throw new LdapAuthenticationException();
        }
        else
        {
            principal = ( String ) ctx.getEnvironment().get( PRINCIPAL );
            if ( principal == null )
            {
                throw new LdapAuthenticationException();
            }
        }

        LdapName principalDn = new LdapName( principal );
        Attributes userEntry = nexus.lookup( principalDn );
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

        ctx.setPrincipal( new LdapPrincipal( principalDn ) );
    }
}
