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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;
import java.util.Objects;

import javax.naming.ConfigurationException;
import javax.naming.Context;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.util.Strings;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * Tests the LdapJndiProperties.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class LdapJndiPropertiesTest
{
    @Test
    public void testEmptyEnv() throws Exception
    {
        try
        {
            LdapJndiProperties.getLdapJndiProperties( new Hashtable<String, Object>() );
            fail( "should never get here" );
        }
        catch ( ConfigurationException e )
        {
        }
    }


    @Test
    public void testNullEnv() throws Exception
    {
        try
        {
            LdapJndiProperties.getLdapJndiProperties( null );
            fail( "should never get here" );
        }
        catch ( ConfigurationException e )
        {
        }
    }


    @Test
    public void testNoAuthWithCredsEnv() throws Exception
    {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "asdf" );
        env.put( Context.PROVIDER_URL, "" );
        LdapJndiProperties props = LdapJndiProperties.getLdapJndiProperties( env );
        assertEquals( AuthenticationLevel.SIMPLE, props.getAuthenticationLevel() );
        assertTrue( Objects.deepEquals( Strings.getBytesUtf8( "asdf" ), props.getCredentials() ) );
    }


    @Test
    public void testNoAuthWithNoCredsEnv() throws Exception
    {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.PROVIDER_URL, "" );
        LdapJndiProperties props = LdapJndiProperties.getLdapJndiProperties( env );
        assertEquals( AuthenticationLevel.NONE, props.getAuthenticationLevel() );
        assertTrue( props.getCredentials() == null );
    }


    @Test
    public void testAuthWithNoCredsEnv() throws Exception
    {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        try
        {
            LdapJndiProperties.getLdapJndiProperties( env );
            fail( "should never get here" );
        }
        catch ( ConfigurationException e )
        {
        }
    }


    @Test
    public void testAuthWithNoCredsStrong() throws Exception
    {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.SECURITY_AUTHENTICATION, "DIGEST-MD5 CRAM-MD5" );
        env.put( Context.PROVIDER_URL, "" );
        LdapJndiProperties props = LdapJndiProperties.getLdapJndiProperties( env );
        assertEquals( AuthenticationLevel.STRONG, props.getAuthenticationLevel() );
        assertTrue( props.getCredentials() == null );
    }


    @Test
    public void testAuthWithCredsStrong() throws Exception
    {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.SECURITY_CREDENTIALS, "asdf" );
        env.put( Context.SECURITY_AUTHENTICATION, "DIGEST-MD5 CRAM-MD5" );
        env.put( Context.PROVIDER_URL, "" );
        LdapJndiProperties props = LdapJndiProperties.getLdapJndiProperties( env );
        assertEquals( AuthenticationLevel.STRONG, props.getAuthenticationLevel() );
        assertTrue( Objects.deepEquals( Strings.getBytesUtf8( "asdf" ), props.getCredentials() ) );
    }
}
