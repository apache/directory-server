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
package org.apache.directory.server.core.jndi;


import java.util.Hashtable;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.util.Strings;


/**
 * A wrapper around a JNDI environment which checks for correct LDAP specific 
 * environment settings.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapJndiProperties
{
    private static final String SASL_AUTHID = "java.naming.security.sasl.authorizationId";

    private Dn providerDn;
    private Dn bindDn;
    private String saslAuthId;
    private AuthenticationLevel level;
    private String saslMechanism;
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
            throw new ConfigurationException( I18n.err( I18n.ERR_483, authentication.getClass(), 
                Context.SECURITY_AUTHENTICATION ) );
        }
        else
        {
            if ( AuthenticationLevel.NONE.toString().equals( authentication ) )
            {
                level = AuthenticationLevel.NONE;
            }
            else if ( AuthenticationLevel.SIMPLE.toString().equals( authentication ) )
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
            throw new ConfigurationException( "environment cannot be null" );
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
            String msg = I18n.err( I18n.ERR_484, Context.PROVIDER_URL );
            throw new ConfigurationException( msg );
        }

        String url = ( String ) env.get( Context.PROVIDER_URL );

        if ( url == null )
        {
            String msg = I18n.err( I18n.ERR_485, Context.PROVIDER_URL );
            throw new ConfigurationException( msg );
        }

        if ( url.trim().equals( "" ) )
        {
            props.providerDn = Dn.ROOT_DSE;
        }
        else
        {
            try
            {
                props.providerDn = new Dn( url );
            }
            catch ( LdapInvalidDnException lide )
            {
                String msg = I18n.err( I18n.ERR_733, url );
                throw new ConfigurationException( msg );
            }
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
            }
            else
            {
                props.level = AuthenticationLevel.SIMPLE;
            }
        }
        else if ( !( authentication instanceof String ) )
        {
            throw new ConfigurationException( I18n.err( I18n.ERR_483, authentication.getClass(), 
                Context.SECURITY_AUTHENTICATION ) );
        }
        else
        {
            if ( AuthenticationLevel.NONE.toString().equals( authentication ) )
            {
                props.level = AuthenticationLevel.NONE;
            }
            else if ( AuthenticationLevel.SIMPLE.toString().equals( authentication ) )
            {
                props.level = AuthenticationLevel.SIMPLE;
            }
            else
            {
                props.level = AuthenticationLevel.STRONG;
                props.saslMechanism = ( String ) authentication;
//                String[] mechList = ( ( String ) authentication ).trim().split( " " );
//                for ( String mech : mechList )
//                {
//                    if ( !mech.trim().equals( "" ) )
//                    {
//                        props.mechanisms.add( mech );
//                    }
//                }
            }
        }

        // -------------------------------------------------------------------
        // Figure out and set the security principal bindDn and saslAuthId
        // -------------------------------------------------------------------

        if ( principal == null && props.level == AuthenticationLevel.SIMPLE )
        {
            throw new ConfigurationException( I18n.err( I18n.ERR_487, Context.SECURITY_PRINCIPAL ) );
        }
        else if ( principal == null && props.level == AuthenticationLevel.NONE )
        {
            props.bindDn = Dn.EMPTY_DN;
        }
        else if ( !( principal instanceof String ) )
        {
            throw new ConfigurationException( I18n.err( I18n.ERR_483, principal.getClass(), Context.SECURITY_PRINCIPAL ) );
        }
        else if ( ( ( String ) principal ).trim().equals( "" ) )
        {
            props.bindDn = Dn.EMPTY_DN;
        }
        else
        {
            try
            {
                props.providerDn = new Dn( ( String ) principal );
            }
            catch ( LdapInvalidDnException lide )
            {
                String msg = I18n.err( I18n.ERR_733, principal );
                throw new ConfigurationException( msg );
            }

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
                throw new ConfigurationException( I18n.err( I18n.ERR_483, obj.getClass(), SASL_AUTHID ) );
            }
            props.saslAuthId = ( String ) principal;
        }

        // -------------------------------------------------------------------
        // Figure out the credentials
        // -------------------------------------------------------------------

        if ( props.level == AuthenticationLevel.SIMPLE && credobj == null )
        {
            throw new ConfigurationException( I18n.err( I18n.ERR_489 ) );
        }
        else if ( credobj != null )
        {
            if ( credobj instanceof String )
            {
                props.credentials = Strings.getBytesUtf8((String) credobj);
            }
            else if ( credobj instanceof byte[] )
            {
                props.credentials = ( byte[] ) credobj;
            }
            else
            {
                throw new ConfigurationException( I18n.err( I18n.ERR_483, credobj.getClass(), Context.SECURITY_CREDENTIALS ) );
            }
        }

        return props;
    }


    public Dn getBindDn()
    {
        return bindDn;
    }


    public Dn getProviderDn()
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


    public String getSaslMechanism()
    {
        return saslMechanism;
    }


    public byte[] getCredentials()
    {
        return credentials;
    }
}
