package org.apache.ldap.server.jndi;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.ldap.common.aci.AuthenticationLevel;
import org.apache.ldap.common.exception.LdapConfigurationException;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.util.StringTools;


/**
 * A wrapper around a JNDI environment which checks for correct LDAP specific 
 * environment settings.
 */
public class LdapJndiProperties
{
    private static final String SASL_AUTHID = "java.naming.security.sasl.authorizationId";
    
    private LdapName providerDn;
    private LdapName bindDn;
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
        else if ( ! ( authentication instanceof String ) ) 
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

        if ( ! env.containsKey( Context.PROVIDER_URL ) )
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
            props.providerDn = LdapName.EMPTY_LDAP_NAME;
        }
        else
        {
            props.providerDn = new LdapName( url );
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
        else if ( ! ( authentication instanceof String ) ) 
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
                    if ( ! mechList[ii].trim().equals( "" ) )
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

        if ( ! ( principal instanceof String ) )
        {
            throw new LdapConfigurationException( "Don't know how to interpret " + principal.getClass()
                + " objects for environment property " + Context.SECURITY_PRINCIPAL );
        }
        
        
        if ( ( ( String ) principal ).trim().equals( "" ) )
        {
            props.bindDn = LdapName.EMPTY_LDAP_NAME;
        }
        else
        {
            props.bindDn = new LdapName( ( String ) principal );
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

    
    public LdapName getBindDn()
    {
        return bindDn;
    }

    
    public LdapName getProviderDn()
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
