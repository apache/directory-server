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
package org.apache.directory.server.operations.bind;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSearchResults;
import netscape.ldap.LDAPUrl;

import org.apache.directory.junit.tools.MultiThreadedMultiInvoker;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * An {@link AbstractServerTest} testing SIMPLE authentication.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@ApplyLdifs(
    {
        // Entry # 1
        "dn: uid=hnelson,ou=users,ou=system", 
        "objectClass: inetOrgPerson", 
        "objectClass: organizationalPerson",
        "objectClass: person", 
        "objectClass: top", 
        "userPassword: secret", 
        "uid: hnelson",
        "cn: Horatio Nelson",
        "sn: Nelson" })
@CreateDS(allowAnonAccess = true, name = "SimpleBindIT-class")
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
public class SimpleBindIT extends AbstractLdapTestUnit
{
    @Rule
    public MultiThreadedMultiInvoker i = new MultiThreadedMultiInvoker( MultiThreadedMultiInvoker.NOT_THREADSAFE );

    private static final String BASE = "ou=users,ou=system";


    /**
     * Convenience method for creating a person.
     */
    protected Attributes getPersonAttributes( String sn, String cn, String uid, String userPassword )
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" ); // sn $ cn
        ocls.add( "inetOrgPerson" ); // uid
        attrs.put( ocls );
        attrs.put( "cn", cn );
        attrs.put( "sn", sn );
        attrs.put( "uid", uid );
        attrs.put( "userPassword", userPassword );

        return attrs;
    }


    /**
     * Convenience method for creating an organizational unit.
     */
    protected Attributes getOrgUnitAttributes( String ou )
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "organizationalUnit" );
        attrs.put( ocls );
        attrs.put( "ou", ou );

        return attrs;
    }


    /**
     * Tests to make sure SIMPLE binds works.
     */
    @Test
    public void testSimpleBind()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + getLdapServer().getPort() );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=hnelson," + BASE );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );

        try
        {
            DirContext context = new InitialDirContext( env );

            String[] attrIDs =
                { "uid" };

            Attributes attrs = context.getAttributes( "uid=hnelson," + BASE, attrIDs );

            String uid = null;

            if ( attrs.get( "uid" ) != null )
            {
                uid = ( String ) attrs.get( "uid" ).get();
            }

            assertEquals( uid, "hnelson" );
        }
        catch ( NamingException e )
        {
            fail( "Should not have caught exception." );
        }
    }


    /**
     * Tests to make sure SIMPLE binds below the RootDSE fail if the password is bad.
     */
    @Test
    public void testSimpleBindBadPassword()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + getLdapServer().getPort() );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=hnelson," + BASE );
        env.put( Context.SECURITY_CREDENTIALS, "badsecret" );

        try
        {
            new InitialDirContext( env );
        }
        catch ( AuthenticationException ae )
        {
            // Error code 49 : LDAP_INVALID_CREDENTIALS
            assertTrue( ae.getMessage().contains( "error code 49" ) );
        }
        catch ( NamingException e )
        {
            fail();
        }
    }


    /**
     * try to connect using a user with an invalid Dn: we should get a invalidDNSyntax error.
     */
    @Test
    public void testSimpleBindBadPrincipalAPassword()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + getLdapServer().getPort() );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "hnelson" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );

        try
        {
            new InitialDirContext( env );
        }
        catch ( InvalidNameException ine )
        {
            // Error code 34 : LDAP_INVALID_DN_SYNTAX
            assertTrue( ine.getMessage().startsWith( "[LDAP: error code 34 - Incorrect Dn given" ) );
        }
        catch ( NamingException e )
        {
            fail();
        }
    }


    /**
     * try to connect using a unknown user: we should get a invalidCredentials error.
     */
    @Test
    public void testSimpleBindUnknowPrincipalAPassword()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + getLdapServer().getPort() );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=unknown,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );

        try
        {
            new InitialDirContext( env );
        }
        catch ( AuthenticationException ae )
        {
        }
        catch ( NamingException e )
        {
            fail( "Expected AuthenticationException with error code 49 for invalidate credentials instead got: "
                + e.getMessage() );
        }
    }


    /**
     * covers the anonymous authentication : we should be able to read the rootDSE, but that's it
     */
    @Test
    public void testSimpleBindNoPrincipalNoPassword()
    {
        boolean oldValue = getLdapServer().getDirectoryService().isAllowAnonymousAccess();
        getLdapServer().getDirectoryService().setAllowAnonymousAccess( false );

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + getLdapServer().getPort() );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.SECURITY_CREDENTIALS, "" );

        String[] attrIDs =
            { "*", "+" };

        // Create the initial context
        try
        {
            new InitialDirContext( env );
            fail();
        }
        catch ( NamingException ne )
        {
            // Expected, as the server forbid anonymous access
        }

        // Check that we can read the rootDSE
        try
        {
            // Use the netscape API as JNDI cannot be used to do a search without
            // first binding.
            LDAPUrl url = new LDAPUrl( "localhost", getLdapServer().getPort(), "", new String[]
                { "vendorName" }, 0, "(ObjectClass=*)" );
            LDAPSearchResults results = LDAPConnection.search( url );

            if ( results.hasMoreElements() )
            {
                LDAPEntry entry = results.next();

                LDAPAttribute vendorName = entry.getAttribute( "vendorName" );

                if ( vendorName != null )
                {
                    assertEquals( "Apache Software Foundation", vendorName.getStringValueArray()[0] );
                }
                else
                {
                    fail();
                }
            }
            else
            {
                fail();
            }
        }
        catch ( LDAPException e )
        {
            fail( "Should not have caught exception." );
        }

        // Check that we cannot read another entry being anonymous
        try
        {
            // Use the netscape API as JNDI cannot be used to do a search without
            // first binding.
            LDAPUrl url = new LDAPUrl( "localhost", getLdapServer().getPort(), "uid=admin,ou=system", attrIDs, 0,
                "(ObjectClass=*)" );
            LDAPConnection.search( url );

            fail();
        }
        catch ( LDAPException e )
        {
            // Expected
            assertTrue( true );
        }

        getLdapServer().getDirectoryService().setAllowAnonymousAccess( oldValue );
    }


    /**
     * covers the Unauthenticated case : we should get a UnwillingToPerform error.
     */
    @Test
    public void testSimpleBindPrincipalNoPassword()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + getLdapServer().getPort() );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "" );

        // Create the initial context
        try
        {
            new InitialDirContext( env );
        }
        catch ( OperationNotSupportedException onse )
        {
            // Error code 53 : LDAP_UNWILLING_TO_PERFORM
            assertTrue( onse.getMessage().contains( "error code 53" ) );
        }
        catch ( NamingException ne )
        {
            fail();
        }
    }


    /**
     * not allowed by the server. We should get a invalidCredentials error.
     */
    @Test
    public void testSimpleBindNoUserAPassword() throws Exception
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + getLdapServer().getPort() );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );

        // Create the initial context
        try
        {
            new InitialDirContext( env );
        }
        catch ( AuthenticationException ae )
        {
        }
        catch ( NamingException ne )
        {
            fail( "Expected AuthenticationException but instead got: " + ne.getMessage() );
        }
    }


    /**
     * Tests to make sure we still have anonymous access to the RootDSE.
     * The configuration for this test case MUST disable anonymous access.
     */
    @Test
    public void testAnonymousRootDSESearch()
    {

        boolean oldValue = getLdapServer().getDirectoryService().isAllowAnonymousAccess();
        getLdapServer().getDirectoryService().setAllowAnonymousAccess( false );

        try
        {
            // Use the netscape API as JNDI cannot be used to do a search without
            // first binding.
            LDAPUrl url = new LDAPUrl( "localhost", getLdapServer().getPort(), "", new String[]
                { "vendorName" }, 0, "(ObjectClass=*)" );
            LDAPSearchResults results = LDAPConnection.search( url );

            if ( results.hasMoreElements() )
            {
                LDAPEntry entry = results.next();

                LDAPAttribute vendorName = entry.getAttribute( "vendorName" );

                if ( vendorName != null )
                {
                    assertEquals( "Apache Software Foundation", vendorName.getStringValueArray()[0] );
                }
                else
                {
                    fail();
                }
            }
            else
            {
                fail();
            }
        }
        catch ( LDAPException e )
        {
            fail( "Should not have caught exception." );
        }
        finally
        {
            getLdapServer().getDirectoryService().setAllowAnonymousAccess( oldValue );
        }
    }


    /**
     * Tests bind operation on referral entry.
     */
    @Test
    public void testBindWithDoubleQuote() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( "localhost", getLdapServer().getPort() );

        connection.bind( "uid=\"admin\",ou=\"system\"", "secret" );
        assertTrue( connection.isAuthenticated() );
        connection.close();
    }
}
