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


import java.util.Hashtable;
import java.io.IOException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;

import org.apache.eve.RootNexus;
import org.apache.eve.SystemPartition;
import org.apache.eve.auth.LdapPrincipal;
import org.apache.eve.exception.EveNoPermissionException;
import org.apache.eve.exception.EveNamingException;
import org.apache.eve.exception.EveConfigurationException;
import org.apache.eve.jndi.exception.EveAuthenticationNotSupportedException;
import org.apache.eve.jndi.exception.EveNameNotFoundException;
import org.apache.eve.jndi.exception.EveAuthenticationException;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.util.ArrayUtils;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.name.NameComponentNormalizer;
import org.apache.ldap.common.name.DnParser;


/**
 * A service used to for authenticating users.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AuthenticationService implements Interceptor
{

    private static final String TYPE = Context.SECURITY_AUTHENTICATION;
    private static final String PRINCIPAL = Context.SECURITY_PRINCIPAL;
    private static final String ADMIN = SystemPartition.ADMIN_PRINCIPAL;

    /** the root nexus to all database partitions */
    private final RootNexus nexus;
    /** whether or not to allow anonymous users */
    private boolean allowAnonymous = false;
    /** the normalizing DnParser to use while parsing names */
    private final DnParser parser;


    /**
     * Creates an authentication service interceptor.
     *
     * @param nexus the root nexus to access all database partitions
     */
    public AuthenticationService( RootNexus nexus, NameComponentNormalizer normalizer,
                                  boolean allowAnonymous ) throws EveNamingException
    {
        this.nexus = nexus;
        this.allowAnonymous = allowAnonymous;
        try
        {
            this.parser = new DnParser( normalizer );
        }
        catch ( IOException e )
        {
            EveNamingException ene = new EveNamingException( ResultCodeEnum.OTHER );
            ene.setRootCause( e );
            throw ene;
        }
    }


    public void invoke( Invocation invocation ) throws NamingException
    {
        if ( invocation.getState() != InvocationStateEnum.PREINVOCATION )
        {
            return;
        }

        EveContext ctx = ( EveLdapContext ) invocation.getContextStack().peek();
        if ( ctx.getPrincipal() != null )
        {
            if ( ctx.getEnvironment().containsKey( Context.SECURITY_CREDENTIALS ) )
            {
                ctx.removeFromEnvironment( Context.SECURITY_CREDENTIALS );
            }

            return;
        }

        String principal = getPrincipal( ctx.getEnvironment() );

        if ( principal.length() == 0 )
        {
            if ( allowAnonymous )
            {
                ctx.setPrincipal( LdapPrincipal.ANONYMOUS );
                return;
            }
            else
            {
                throw new EveNoPermissionException( "" );
            }
        }

        Object creds = ctx.getEnvironment().get( Context.SECURITY_CREDENTIALS );
        if ( creds == null )
        {
            creds = ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        else if ( creds instanceof String )
        {
            creds = ( ( String ) creds ).getBytes();
        }

        LdapName principalDn = new LdapName( principal );
        Attributes userEntry = nexus.lookup( principalDn );
        if ( userEntry == null )
        {
            throw new EveNameNotFoundException();
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
            throw new EveAuthenticationException();
        }

        synchronized( parser )
        {
            ctx.setPrincipal( new LdapPrincipal( parser.parse( principal ) ) );
        }

        // remove creds so there is no security risk
        ctx.removeFromEnvironment( Context.SECURITY_CREDENTIALS );
    }


    /**
     * Gets the effective principal associated with a JNDI context's environment.
     *
     * @param env the JNDI Context environment
     * @return the effective principal
     * @throws NamingException if certain properties are not present or present
     * in wrong values or present in the wrong combinations
     */
    private String getPrincipal( Hashtable env ) throws NamingException
    {
        if ( "strong".equalsIgnoreCase( ( String ) env.get( TYPE ) ) )
        {
            throw new EveAuthenticationNotSupportedException( ResultCodeEnum.AUTHMETHODNOTSUPPORTED );
        }

        // --------------------------------------------------------------------
        // if both the authtype and principal keys not defined then the
        // princial is set to the admin user for the system
        // --------------------------------------------------------------------
        if ( ! env.containsKey( TYPE ) && ! env.containsKey( PRINCIPAL ) )
        {
            return SystemPartition.ADMIN_PRINCIPAL;
        }

        // the authtype is set but the principal is not
        if ( env.containsKey( TYPE ) && ! env.containsKey( PRINCIPAL ) )
        {
            Object val = env.get( TYPE );

            // princial is set to the anonymous user if authType is "none"
            if ( "none".equalsIgnoreCase( ( String ) val ) )
            {
                return "";
            }
            // princial is set to the admin user if authType is "simple"
            else if ( "simple".equalsIgnoreCase( ( String ) val ) )
            {
                return ADMIN;
            }

            // blow chuncks if we see any other authtype values
            throw new EveConfigurationException( "Unknown value for property " + TYPE + ": " + val );
        }

        // both are set
        if ( env.containsKey( TYPE ) && env.containsKey( PRINCIPAL ) )
        {
            Object val = env.get( TYPE );

            // princial is set to the anonymous user if authType is "none"
            if ( "none".equalsIgnoreCase( ( String ) val ) )
            {
                String msg = "Ambiguous configuration: " + TYPE;
                msg += " is set to none and the security principal";
                msg += " is set using " + PRINCIPAL + " as well";
                throw new EveConfigurationException( msg );
            }
            // princial is set to the admin user if authType is "simple"
            else if ( "simple".equalsIgnoreCase( ( String ) val ) )
            {
                return ( String ) env.get( PRINCIPAL );
            }

            // blow chuncks if we see any other authtype values
            throw new EveConfigurationException( "Unknown value for property " + TYPE + ": " + val );
        }

        // we have the principal key so we set that as the value
        if ( env.containsKey( PRINCIPAL ) )
        {
            return ( String ) env.get( PRINCIPAL );
        }

        return ADMIN;
    }
}
