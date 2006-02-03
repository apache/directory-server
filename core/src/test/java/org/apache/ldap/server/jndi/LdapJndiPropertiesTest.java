package org.apache.ldap.server.jndi;
 

import java.util.Hashtable;

import javax.naming.Context;

import org.apache.commons.lang.ArrayUtils;
import org.apache.ldap.common.aci.AuthenticationLevel;
import org.apache.ldap.common.exception.LdapConfigurationException;
import org.apache.ldap.common.util.StringTools;

import junit.framework.TestCase;


public class LdapJndiPropertiesTest extends TestCase
{
    public void testEmptyEnv() throws Exception
    {
        try
        {
            LdapJndiProperties.getLdapJndiProperties( new Hashtable() );
            fail( "should never get here" );
        }
        catch ( LdapConfigurationException e )
        {
        }
    }

    
    public void testNullEnv() throws Exception
    {
        try
        {
            LdapJndiProperties.getLdapJndiProperties( null );
            fail( "should never get here" );
        }
        catch ( LdapConfigurationException e )
        {
        }
    }


    public void testNoAuthWithCredsEnv() throws Exception
    {
        Hashtable env = new Hashtable();
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "asdf" );
        env.put( Context.PROVIDER_URL, "" );
        LdapJndiProperties props = LdapJndiProperties.getLdapJndiProperties( env );
        assertEquals( AuthenticationLevel.SIMPLE, props.getAuthenticationLevel() );
        assertEquals( 1, props.getAuthenticationMechanisms().size() );
        assertEquals( "simple", props.getAuthenticationMechanisms().get( 0 ) );
        assertTrue( ArrayUtils.isEquals( StringTools.getBytesUtf8( "asdf" ), props.getCredentials() ) );
    }


    public void testNoAuthWithNoCredsEnv() throws Exception
    {
        Hashtable env = new Hashtable();
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.PROVIDER_URL, "" );
        LdapJndiProperties props = LdapJndiProperties.getLdapJndiProperties( env );
        assertEquals( AuthenticationLevel.NONE, props.getAuthenticationLevel() );
        assertEquals( 1, props.getAuthenticationMechanisms().size() );
        assertEquals( "none", props.getAuthenticationMechanisms().get( 0 ) );
        assertTrue( props.getCredentials() == null );
    }


    public void testAuthWithNoCredsEnv() throws Exception
    {
        Hashtable env = new Hashtable();
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        try
        {
            LdapJndiProperties.getLdapJndiProperties( env );
            fail( "should never get here" );
        }
        catch ( LdapConfigurationException e )
        {
        }
    }


    public void testAuthWithNoCredsStrong() throws Exception
    {
        Hashtable env = new Hashtable();
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.SECURITY_AUTHENTICATION, "DIGEST-MD5 CRAM-MD5" );
        env.put( Context.PROVIDER_URL, "" );
        LdapJndiProperties props = LdapJndiProperties.getLdapJndiProperties( env );
        assertEquals( AuthenticationLevel.STRONG, props.getAuthenticationLevel() );
        assertEquals( 2, props.getAuthenticationMechanisms().size() );
        assertEquals( "DIGEST-MD5", props.getAuthenticationMechanisms().get( 0 ) );
        assertEquals( "CRAM-MD5", props.getAuthenticationMechanisms().get( 1 ) );
        assertTrue( props.getCredentials() == null );
    }


    public void testAuthWithCredsStrong() throws Exception
    {
        Hashtable env = new Hashtable();
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.SECURITY_CREDENTIALS, "asdf" );
        env.put( Context.SECURITY_AUTHENTICATION, "DIGEST-MD5 CRAM-MD5" );
        env.put( Context.PROVIDER_URL, "" );
        LdapJndiProperties props = LdapJndiProperties.getLdapJndiProperties( env );
        assertEquals( AuthenticationLevel.STRONG, props.getAuthenticationLevel() );
        assertEquals( 2, props.getAuthenticationMechanisms().size() );
        assertEquals( "DIGEST-MD5", props.getAuthenticationMechanisms().get( 0 ) );
        assertEquals( "CRAM-MD5", props.getAuthenticationMechanisms().get( 1 ) );
        assertTrue( ArrayUtils.isEquals( StringTools.getBytesUtf8( "asdf" ), props.getCredentials() ) );
    }
}
