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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.api.ldap.model.constants.JndiPropertyConstants;
import org.apache.directory.api.ldap.model.exception.LdapUnwillingToPerformException;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Test the Simple BindRequest
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "SimpleBindIT", allowAnonAccess = true)
public class SimpleBindJndiIT extends AbstractLdapTestUnit
{

    /**
     * A method to do a search
     */
    private NamingEnumeration<SearchResult> search( DirContext ctx, String baseDn, String filter, int scope )
        throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( scope );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[]
            { "*", "+" } );
        ctx.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, AliasDerefMode.NEVER_DEREF_ALIASES
            .getJndiValue() );

        NamingEnumeration<SearchResult> list = ctx.search( baseDn, filter, controls );
        return list;
    }


    /**
     * try to connect using a known user/password and read an entry.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindAPrincipalAPassword()
    {
        // We will bind using JNDI
        // Set up the environment for creating the initial context
        Hashtable<String, Object> env = setDefaultJNDIEnv( CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );

        DirContext ctx = null;

        // Create the initial context
        try
        {
            ctx = new InitialDirContext( env );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        try
        {
            ctx.close();
        }
        catch ( NamingException ne )
        {
            fail();
        }
    }


    /**
     * try to connect using a known user but with a bad password: we should get a invalidCredentials error.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindAPrincipalBadPassword()
    {
        // We will bind using JNDI
        Hashtable<String, Object> env = setDefaultJNDIEnv( CoreContextFactory.class.getName() );

        // Set up the environment for creating the initial context
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "badsecret" );

        // Create the initial context
        try
        {
            new InitialDirContext( env );

            // We should not be connected
            fail();
        }
        catch ( AuthenticationException ae )
        {
            assertTrue( true );
        }
        catch ( NamingException ne )
        {
            fail();
        }
    }


    /**
     * try to connect using a user with an invalid Dn: we should get a invalidDNSyntax error.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindBadPrincipalAPassword()
    {
        // We will bind using JNDI
        // Set up the environment for creating the initial context
        Hashtable<String, Object> env = setDefaultJNDIEnv( CoreContextFactory.class.getName() );
        env.put( Context.SECURITY_PRINCIPAL, "ou=system" );
        env.put( Context.SECURITY_PRINCIPAL, "admin" );

        // Create the initial context
        try
        {
            new InitialDirContext( env );

            // We should not be connected
            fail();
        }
        catch ( InvalidNameException ine )
        {
            assertTrue( true );
        }
        catch ( NamingException ne )
        {
            fail();
        }
    }


    /**
     * try to connect using a unknown user: we should get a invalidCredentials error.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindUnknowPrincipalAPassword()
    {
        // We will bind using JNDI
        // Set up the environment for creating the initial context
        Hashtable<String, Object> env = setDefaultJNDIEnv( CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=unknown,ou=system" );

        // Create the initial context
        try
        {
            new InitialDirContext( env );

            // We should not be connected
            fail();
        }
        catch ( AuthenticationException ae )
        {
            // lae.printStackTrace();
            assertTrue( ae.getMessage().startsWith( org.apache.directory.server.i18n.I18n.ERR_14003_CANNOT_AUTHENTICATE_USER.getErrorCode() ), 
                org.apache.directory.server.i18n.I18n.err( org.apache.directory.server.i18n.I18n.ERR_14003_CANNOT_AUTHENTICATE_USER ) );
        }
        catch ( NamingException ne )
        {
            fail();
        }
    }


    /**
     * covers the anonymous authentication : we should be able to read the rootDSE, but that's it
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindNoPrincipalNoPassword()
    {
        // We will bind using JNDI
        // Set up the environment for creating the initial context
        Hashtable<String, Object> env = setDefaultJNDIEnv( CoreContextFactory.class.getName() );
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.SECURITY_CREDENTIALS, "" );

        DirContext ctx = null;

        // Create the initial context
        try
        {
            ctx = new InitialDirContext( env );
        }
        catch ( NamingException ne )
        {
            ne.printStackTrace();
            fail();
        }

        // We should be anonymous here.
        // Check that we can read the rootDSE
        try
        {
            NamingEnumeration<SearchResult> list = search( ctx, "", "(ObjectClass=*)", SearchControls.OBJECT_SCOPE );

            assertNotNull( list );

            while ( list.hasMore() )
            {
                SearchResult result = list.next();
                assertNotNull( result );
            }
        }
        catch ( NamingException ne )
        {
            fail();
        }

        // Check that we cannot read another entry being anonymous
        try
        {
            NamingEnumeration<SearchResult> list = search( ctx, "uid=admin, ou=system", "(ObjectClass=*)",
                SearchControls.OBJECT_SCOPE );

            assertNotNull( list );
            assertFalse( list.hasMore() );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        try
        {
            ctx.close();
        }
        catch ( NamingException ne )
        {
            fail();
        }
    }


    /**
     * covers the Unauthenticated case : we should get a UnwillingToPerform error.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindAPrincipalNoPassword()
    {
        // We will bind using JNDI
        // Set up the environment for creating the initial context
        Hashtable<String, Object> env = setDefaultJNDIEnv( CoreContextFactory.class.getName() );
        env.put( Context.SECURITY_CREDENTIALS, "" );

        // Create the initial context
        try
        {
            new InitialDirContext( env );
        }
        catch ( OperationNotSupportedException onse )
        {
            assertEquals( "Cannot Bind for Dn uid=admin,ou=system", onse.getMessage() );
        }
        catch ( NamingException ne )
        {
            fail();
        }
    }


    /**
     * covers the Unauthenticated case : we should get a UnwillingToPerform error.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindAPrincipalNullPassword() throws Exception
    {
        Assertions.assertThrows( LdapUnwillingToPerformException.class, () -> 
        {
            LdapConnection connection = IntegrationUtils.getConnectionAs( getService(), "uid=admin,ou=system", null );
            assertFalse( connection.isAuthenticated() );
    
            connection = IntegrationUtils.getConnectionAs( getService(), "uid=admin,ou=system", "secret" );
    
            connection.bind( "uid=admin,ou=system", null );
        } );
    }


    /**
     * not allowed by the server. We should get a invalidCredentials error.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindNoPrincipalAPassword() throws Exception
    {
        // We will bind using JNDI
        // Set up the environment for creating the initial context
        Hashtable<String, Object> env = setDefaultJNDIEnv( CoreContextFactory.class.getName() );
        env.put( Context.SECURITY_PRINCIPAL, "" );

        // Create the initial context
        try
        {
            new InitialDirContext( env );
        }
        catch ( NameNotFoundException nnfe )
        {
            fail();
        }
        catch ( AuthenticationException ne )
        {
            assertTrue( true );
        }
    }


    /**
     * try to connect using a known user/password and read an entry.
     *
     * @throws Exception on error
     */
    @Test
    public void testSimpleBindWithDoubleQuote()
    {
        // We will bind using JNDI
        // Set up the environment for creating the initial context
        Hashtable<String, Object> env = setDefaultJNDIEnv( CoreContextFactory.class.getName() );
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=\"admin\",ou=\"system\"" );

        DirContext ctx = null;

        // Create the initial context
        try
        {
            ctx = new InitialDirContext( env );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        try
        {
            ctx.close();
        }
        catch ( NamingException ne )
        {
            fail();
        }
    }
}
