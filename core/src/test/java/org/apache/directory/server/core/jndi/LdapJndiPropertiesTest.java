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

import javax.naming.Context;

import org.apache.commons.lang.ArrayUtils;
import org.apache.directory.server.core.jndi.LdapJndiProperties;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.util.StringTools;

import junit.framework.TestCase;


/**
 * Tests the LdapJndiProperties.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
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
        assertTrue( ArrayUtils.isEquals( StringTools.getBytesUtf8( "asdf" ), props.getCredentials() ) );
    }


    public void testNoAuthWithNoCredsEnv() throws Exception
    {
        Hashtable env = new Hashtable();
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.PROVIDER_URL, "" );
        LdapJndiProperties props = LdapJndiProperties.getLdapJndiProperties( env );
        assertEquals( AuthenticationLevel.NONE, props.getAuthenticationLevel() );
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
        assertTrue( ArrayUtils.isEquals( StringTools.getBytesUtf8( "asdf" ), props.getCredentials() ) );
    }
}
