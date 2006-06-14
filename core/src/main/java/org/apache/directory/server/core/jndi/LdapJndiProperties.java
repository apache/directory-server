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
package org.apache.directory.server.core.jndi;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A wrapper around a JNDI environment which checks for correct LDAP specific 
 * environment settings.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class LdapJndiProperties
{
    private static final String SASL_AUTHID = "java.naming.security.sasl.authorizationId";

    private LdapDN providerDn;
    private LdapDN bindDn;
    private String saslAuthId;
    private AuthenticationLevel level;
    private List mechanisms = new ArrayList();
    private byte[] credentials;


    public static AuthenticationLevel getAuthenticationLevel( Hashtable env ) throws NamingException
    {
        AuthenticationLevel level;
        Object credobj = env.get( Context.SECURITY_CREDENTIALS );
        Object authentication = env.get( Context.SECURITY_AUTHENTICATION );

        // -------------------------------------------------------------------
        // Figure out and set the authentication level and mechanisms
        // -------------------------------------------------------------------

        if ( authentication == null )
        {
            // if the property is not set but Context.SECURITY_CREDENTIALS is then SIMPLE
            if ( credobj == null )
            {
                level = AuthenticationLevel.NONE;
            }
            else
            {
                level = AuthenticationLevel.SIMPLE;
            }
        }
        else if ( !( authentication instanceof String ) )
        {
            throw new LdapConfigurationException( "Don't know how to interpret " + authentication.getClass()
                + " objects for environment property " + Context.SECURITY_AUTHENTICATION );
        }
        else
        {
            if ( "none".equals( authentication ) )
            {
                level = AuthenticationLevel.NONE;
            }
            else if ( "simple".equals( authentication ) )
            {
                level = AuthenticationLevel.SIMPLE;
            }
            else
            {
                level = AuthenticationLevel.STRONG;
            }
        }

        return level;
    }


    public static LdapJndiProperties getLdapJndiProperties( Hashtable env ) throws NamingException
    {
        if ( env == null )
        {
            throw new LdapConfigurationException( "environment cannot be null" );
        }

        LdapJndiProperties props = new LdapJndiProperties();
        Object principal = env.get( Context.SECURITY_PRINCIPAL );
        Object credobj = env.get( Context.SECURITY_CREDENTIALS );
        Object authentication = env.get( Context.SECURITY_AUTHENTICATION );

        // -------------------------------------------------------------------
        // check for the provider URL property 
        // -------------------------------------------------------------------

        if ( !env.containsKey( Context.PROVIDER_URL ) )
        {
            String msg = "Expected property " + Context.PROVIDER_URL;
            msg += " but could not find it in env!";
            throw new LdapConfigurationException( msg );
        }

        String url = ( String ) env.get( Context.PROVIDER_URL );
        if ( url == null )
        {
            String msg = "Expected value for property " + Context.PROVIDER_URL;
            msg += " but it was set to null in env!";
            throw new LdapConfigurationException( msg );
        }

        if ( url.trim().equals( "" ) )
        {
            props.providerDn = LdapDN.EMPTY_LDAPDN;
        }
        else
        {
            props.providerDn = new LdapDN( url );
        }

        // -------------------------------------------------------------------
        // Figure out and set the authentication level and mechanisms
        // -------------------------------------------------------------------

        if ( authentication == null )
        {
            // if the property is not set but Context.SECURITY_CREDENTIALS is then SIMPLE
            if ( credobj == null )
            {
                props.level = AuthenticationLevel.NONE;
                props.mechanisms.add( "none" );
            }
            else
            {
                props.level = AuthenticationLevel.SIMPLE;
                props.mechanisms.add( "simple" );
            }
        }
        else if ( !( authentication instanceof String ) )
        {
            throw new LdapConfigurationException( "Don't know how to interpret " + authentication.getClass()
                + " objects for environment property " + Context.SECURITY_AUTHENTICATION );
        }
        else
        {
            if ( "none".equals( authentication ) )
            {
                props.level = AuthenticationLevel.NONE;
                props.mechanisms.add( "none" );
            }
            else if ( "simple".equals( authentication ) )
            {
                props.level = AuthenticationLevel.SIMPLE;
                props.mechanisms.add( "simple" );
            }
            else
            {
                props.level = AuthenticationLevel.STRONG;
                String[] mechList = ( ( String ) authentication ).trim().split( " " );
                for ( int ii = 0; ii < mechList.length; ii++ )
                {
                    if ( !mechList[ii].trim().equals( "" ) )
                    {
                        props.mechanisms.add( mechList[ii] );
                    }
                }
            }
        }

        // -------------------------------------------------------------------
        // Figure out and set the security principal bindDn and saslAuthId
        // -------------------------------------------------------------------

        if ( principal == null )
        {
            throw new LdapConfigurationException( Context.SECURITY_PRINCIPAL + " cannot be null." );
        }

        if ( !( principal instanceof String ) )
        {
            throw new LdapConfigurationException( "Don't know how to interpret " + principal.getClass()
                + " objects for environment property " + Context.SECURITY_PRINCIPAL );
        }

        if ( ( ( String ) principal ).trim().equals( "" ) )
        {
            props.bindDn = LdapDN.EMPTY_LDAPDN;
        }
        else
        {
            props.bindDn = new LdapDN( ( String ) principal );
        }

        if ( env.get( SASL_AUTHID ) != null && props.level == AuthenticationLevel.STRONG )
        {
            Object obj = env.get( SASL_AUTHID );
            if ( obj instanceof String )
            {
                props.saslAuthId = ( String ) obj;
            }
            else
            {
                throw new LdapConfigurationException( "Don't know how to interpret " + obj.getClass()
                    + " objects for environment property " + SASL_AUTHID );
            }
            props.saslAuthId = ( String ) principal;
        }

        // -------------------------------------------------------------------
        // Figure out the credentials
        // -------------------------------------------------------------------

        if ( props.level == AuthenticationLevel.SIMPLE && credobj == null )
        {
            throw new LdapConfigurationException( "cannot specify simple authentication with supplying credentials" );
        }
        else if ( credobj != null )
        {
            if ( credobj instanceof String )
            {
                props.credentials = StringTools.getBytesUtf8( ( String ) credobj );
            }
            else if ( credobj instanceof byte[] )
            {
                props.credentials = ( byte[] ) credobj;
            }
            else
            {
                throw new LdapConfigurationException( "Don't know how to interpret " + credobj.getClass()
                    + " objects for environment property " + Context.SECURITY_CREDENTIALS );
            }
        }

        return props;
    }


    public LdapDN getBindDn()
    {
        return bindDn;
    }


    public LdapDN getProviderDn()
    {
        return providerDn;
    }


    public String getSaslAuthId()
    {
        return saslAuthId;
    }


    public AuthenticationLevel getAuthenticationLevel()
    {
        return level;
    }


    public List getAuthenticationMechanisms()
    {
        return Collections.unmodifiableList( mechanisms );
    }


    public byte[] getCredentials()
    {
        return credentials;
    }
}
