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


import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import javax.naming.directory.DirContext;
import javax.naming.directory.Attributes;
import javax.naming.*;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.InitialLdapContext;

import org.apache.ldap.common.util.ArrayUtils;
import org.apache.ldap.common.exception.LdapConfigurationException;
import org.apache.ldap.common.exception.LdapNoPermissionException;


/**
 * A set of simple tests to make sure simple authentication is working as it
 * should.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SimpleAuthenticationTest extends AbstractJndiTest
{
    /**
     * Cleans up old database files on creation.
     *
     * @throws IOException if we can't clean the files
     */
    public SimpleAuthenticationTest() throws IOException
    {
        doDelete( new File( "target" + File.separator + "eve" ) );
    }


    /**
     * Customizes setup for each test case.
     *
     * <ul>
     *   <li>sets doDelete to false for test1AdminAccountCreation</li>
     *   <li>sets doDelete to false for test2AccountExistsOnRestart</li>
     *   <li>sets doDelete to true for all other cases</li>
     *   <li>bypasses normal setup for test3BuildDbNoNothing</li>
     *   <li>bypasses normal setup for test5BuildDbNoPassWithPrincAuthNone</li>
     *   <li>bypasses normal setup for test4BuildDbNoPassNoPrincAuthNone</li>
     *   <li>bypasses normal setup for test6BuildDbNoPassNotAdminPrinc</li>
     *   <li>bypasses normal setup for test7BuildDbNoPassNoPrincAuthNoneAnonOff</li>
     * </ul>
     *
     * @throws Exception
     */
    protected void setUp() throws Exception
    {
        if ( getName().equals( "test1AdminAccountCreation" ) ||
             getName().equals( "test2AccountExistsOnRestart" ) )
        {
            super.doDelete = false;
        }
        else
        {
            super.doDelete = true;
        }

        if ( getName().equals( "test3BuildDbNoNothing" ) ||
             getName().equals( "test5BuildDbNoPassWithPrincAuthNone" ) ||
                getName().equals( "test6BuildDbNoPassNotAdminPrinc" ) ||
                getName().equals( "test7BuildDbNoPassNoPrincAuthNoneAnonOff" ) ||
             getName().equals( "test4BuildDbNoPassNoPrincAuthNone" ) )
        {
            return;
        }

        super.setUp();
    }


    /**
     * Checks all attributes of the admin account entry minus the userPassword
     * attribute.
     *
     * @param attrs the entries attributes
     */
    protected void performAdminAccountChecks( Attributes attrs )
    {
        assertTrue( attrs.get( "objectClass" ).contains( "top" ) );
        assertTrue( attrs.get( "objectClass" ).contains( "person" ) );
        assertTrue( attrs.get( "objectClass" ).contains( "organizationalPerson" ) );
        assertTrue( attrs.get( "objectClass" ).contains( "inetOrgPerson" ) );
        assertTrue( attrs.get( "displayName" ).contains( "Directory Superuser" ) );
    }


    /**
     * Check the creation of the admin account.
     *
     * @throws NamingException if there are failures
     */
    public void test1AdminAccountCreation() throws NamingException
    {
        DirContext ctx = ( DirContext ) sysRoot.lookup( "uid=admin" );
        Attributes attrs = ctx.getAttributes( "" );
        performAdminAccountChecks( attrs );
        assertTrue( attrs.get( "userPassword" ).contains( "testing" ) );
    }


    /**
     * Check the creation of the admin account even after a restart.
     *
     * @throws NamingException if there are failures
     */
    public void test2AccountExistsOnRestart() throws NamingException
    {
        DirContext ctx = ( DirContext ) sysRoot.lookup( "uid=admin" );
        Attributes attrs = ctx.getAttributes( "" );

        performAdminAccountChecks( attrs );
        assertTrue( attrs.get( "userPassword" ).contains( "testing" ) );
    }


    /**
     * Checks that we can give basically the minimal set of properties without
     * any security information to build and bootstrap a new system.  The admin
     * user is presumed and no password is set.  The admin password defaults to
     * the empty byte array.
     *
     * @throws Exception if there are problems
     */
    public void test3BuildDbNoNothing() throws Exception
    {
        // clean out the database
        doDelete( new File( "target" + File.separator + "eve" ) );
        LdapContext ctx = setSysRoot( new Hashtable() );
        Attributes attributes = ctx.getAttributes( "uid=admin" );
        assertNotNull( attributes );

        // Eve has started now so we access another context w/o the wkdir
        Hashtable env = new Hashtable();
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.eve.jndi.EveContextFactory" );
        InitialContext initial = new InitialContext( env );
        ctx = ( LdapContext ) initial.lookup( "uid=admin" );
        assertNotNull( ctx );
        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );

        performAdminAccountChecks( attributes );
        assertTrue( attributes.get( "userPassword" ).contains( ArrayUtils.EMPTY_BYTE_ARRAY ) );
    }


    /**
     * Tests to make sure we throw an error when Context.SECURITY_AUTHENTICATION
     * is set to "none" when trying to bootstrap the system.  Only the admin
     * user is allowed to bootstrap.
     *
     * @throws Exception if anything goes wrong
     */
    public void test4BuildDbNoPassNoPrincAuthNone() throws Exception
    {
        // clean out the database
        tearDown();
        doDelete( new File( "target" + File.separator + "eve" ) );
        Hashtable env = new Hashtable();
        env.put( Context.SECURITY_AUTHENTICATION, "none" );

        try
        {
            setSysRoot( env );
            fail( "should not get here due to exception" );
        }
        catch( LdapConfigurationException e )
        {
        }
        catch( LdapNoPermissionException e )
        {
        }

        // ok this should start up the system now as admin
        Hashtable anonymous = new Hashtable();
        anonymous.put( EveContextFactory.ANONYMOUS_ENV, "true" );
        InitialLdapContext ctx = ( InitialLdapContext ) setSysRoot( anonymous );
        assertNotNull( ctx );

        // now go in as anonymous user and we should be wh
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.eve.jndi.EveContextFactory" );

        InitialLdapContext initial = new InitialLdapContext( env, null );

        try
        {
            ctx = ( InitialLdapContext ) initial.lookup( "uid=admin" );
            fail( "should not get here due to exception cuz anonymous user is "
                    + "not allowed read access to the admin account entry" );
        }
        catch( LdapConfigurationException e )
        {
        }
        catch( LdapNoPermissionException e )
        {
        }
    }


    /**
     * Tests to make sure we throw an error when Context.SECURITY_AUTHENTICATION
     * is set to "none" when trying to bootstrap the system even when the
     * principal is set to the admin user.  Only the admin user is allowed to
     * bootstrap.  This is a configuration issue or a nonsense set of property
     * values.
     *
     * @throws Exception if anything goes wrong
     */
    public void test5BuildDbNoPassWithPrincAuthNone() throws Exception
    {
        // clean out the database
        tearDown();
        doDelete( new File( "target" + File.separator + "eve" ) );
        Hashtable env = new Hashtable();
        env.put( Context.SECURITY_AUTHENTICATION, "none" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );

        try
        {
            setSysRoot( env );
            fail( "should not get here due to exception" );
        }
        catch( ConfigurationException e )
        {
        }
    }


    /**
     * Tests to make sure we throw an error when Context.SECURITY_AUTHENTICATION
     * is set to "simple" when trying to bootstrap the system but the admin is
     * not the principal.  Only the admin user is allowed to bootstrap.
     * Subsequent calls can 'bind' (authenticate in our case since there is no
     * network connection) anonymously though.
     *
     * @throws Exception if anything goes wrong
     */
    public void test6BuildDbNoPassNotAdminPrinc() throws Exception
    {
        // clean out the database
        tearDown();
        doDelete( new File( "target" + File.separator + "eve" ) );
        Hashtable env = new Hashtable();
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=akarasulu,ou=users,ou=system" );

        try
        {
            setSysRoot( env );
            fail( "should not get here due to exception" );
        }
        catch( ConfigurationException e )
        {
        }
    }


    /**
     * Tests to make sure we throw an error when Context.SECURITY_AUTHENTICATION
     * is set to "none" when trying to get a context from an already
     * bootstrapped system when anonymous users are not turned on.
     *
     * @throws Exception if anything goes wrong
     */
    public void test7BuildDbNoPassNoPrincAuthNoneAnonOff() throws Exception
    {
        // clean out the database
        tearDown();
        doDelete( new File( "target" + File.separator + "eve" ) );

        // ok this should start up the system now as admin
        InitialLdapContext ctx = ( InitialLdapContext ) setSysRoot( new Hashtable() );
        assertNotNull( ctx );

        // now go in as anonymous user and we should be rejected
        Hashtable env = new Hashtable();
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.SECURITY_AUTHENTICATION, "none" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.eve.jndi.EveContextFactory" );

        try
        {
            new InitialContext( env );
            fail( "should never get here due to an exception" );
        }
        catch ( NoPermissionException e )
        {
        }
    }


    /**
     * Tests to make sure we can authenticate after the database has already
     * been build as the admin user when simple authentication is in effect.
     *
     * @throws Exception if anything goes wrong
     */
    public void test8PassPrincAuthTypeSimple() throws Exception
    {
        // now go in as anonymous user and we should be rejected
        Hashtable env = new Hashtable();
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "testing" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.eve.jndi.EveContextFactory" );
        assertNotNull( new InitialContext( env ) );
    }


    /**
     * Checks to see if we can authenticate as a test user after the admin fires
     * up and builds the the system database.
     *
     * @throws Exception if anything goes wrong
     */
    public void test10TestNonAdminUser() throws Exception
    {
        // now go in as anonymous user and we should be rejected
        Hashtable env = new Hashtable();
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=akarasulu,ou=users,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "test" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.eve.jndi.EveContextFactory" );
        assertNotNull( new InitialContext( env ) );
    }
}
