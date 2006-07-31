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
package org.apache.directory.server.core.authn;


import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.InitialLdapContext;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.util.ArrayUtils;


/**
 * A set of simple tests to make sure simple authentication is working as it
 * should.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SimpleAuthenticationITest extends AbstractAdminTestCase
{
    /**
     * Cleans up old database files on creation.
     * @throws IOException
     */
    public SimpleAuthenticationITest() throws IOException
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
     *   <li>bypasses normal setup for test5BuildDbNoPassWithPrincAuthNone</li>
     *   <li>bypasses normal setup for test4BuildDbNoPassNoPrincAuthNone</li>
     *   <li>bypasses normal setup for test6BuildDbNoPassNotAdminPrinc</li>
     * </ul>
     *
     * @throws Exception
     */
    protected void setUp() throws Exception
    {
        super.doDelete = !( getName().equals( "test1AdminAccountCreation" ) || getName().equals(
            "test2AccountExistsOnRestart" ) );

        if ( getName().equals( "test5BuildDbNoPassWithPrincAuthNone" )
            || getName().equals( "test6BuildDbNoPassNotAdminPrinc" )
            || getName().equals( "test4BuildDbNoPassNoPrincAuthNone" ) )
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
        assertTrue( ArrayUtils.isEquals( attrs.get( "userPassword" ).get(), "secret".getBytes() ) );
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
        assertTrue( ArrayUtils.isEquals( attrs.get( "userPassword" ).get(), "secret".getBytes() ) );
    }


    public void test3UseAkarasulu() throws NamingException
    {
        Hashtable env = new Hashtable( configuration.toJndiEnvironment() );
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=akarasulu,ou=users,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "test" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        InitialDirContext ic = new InitialDirContext( env );
        Attributes attrs = ic.getAttributes( "uid=akarasulu,ou=users" );
        Attribute ou = attrs.get( "ou" );
        assertTrue( ou.contains( "Engineering" ) );
        assertTrue( ou.contains( "People" ) );

        Attribute objectClass = attrs.get( "objectClass" );
        assertTrue( objectClass.contains( "top" ) );
        assertTrue( objectClass.contains( "person" ) );
        assertTrue( objectClass.contains( "organizationalPerson" ) );
        assertTrue( objectClass.contains( "inetOrgPerson" ) );

        assertTrue( attrs.get( "telephonenumber" ).contains( "+1 408 555 4798" ) );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
        assertTrue( attrs.get( "givenname" ).contains( "Alex" ) );
        assertTrue( attrs.get( "mail" ).contains( "akarasulu@apache.org" ) );
        assertTrue( attrs.get( "l" ).contains( "Bogusville" ) );
        assertTrue( attrs.get( "sn" ).contains( "Karasulu" ) );
        assertTrue( attrs.get( "cn" ).contains( "Alex Karasulu" ) );
        assertTrue( attrs.get( "facsimiletelephonenumber" ).contains( "+1 408 555 9751" ) );
        assertTrue( attrs.get( "roomnumber" ).contains( "4612" ) );
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
        Hashtable env = new Hashtable( configuration.toJndiEnvironment() );
        env.put( Context.SECURITY_AUTHENTICATION, "none" );

        configuration.setAllowAnonymousAccess( false );
        try
        {
            setSysRoot( env );
            fail( "should not get here due to exception" );
        }
        catch ( LdapNoPermissionException e )
        {
        }
        tearDown();

        // ok this should start up the system now as admin
        env = new Hashtable( configuration.toJndiEnvironment() );
        env.put( Context.SECURITY_AUTHENTICATION, "none" );
        configuration.setAllowAnonymousAccess( true );

        InitialLdapContext ctx = ( InitialLdapContext ) setSysRoot( env );
        assertNotNull( ctx );

        // now go in as anonymous user and we should be wh
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );

        InitialLdapContext initial = new InitialLdapContext( env, null );

        try
        {
            initial.lookup( "uid=admin" );
            fail( "should not get here due to exception cuz anonymous user is "
                + "not allowed read access to the admin account entry" );
        }
        catch ( LdapConfigurationException e )
        {
        }
        catch ( LdapNoPermissionException e )
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
        Hashtable env = new Hashtable( configuration.toJndiEnvironment() );
        env.put( Context.SECURITY_AUTHENTICATION, "none" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );

        try
        {
            setSysRoot( env );
            fail( "should not get here due to exception" );
        }
        catch ( ConfigurationException e )
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
        Hashtable env = new Hashtable( configuration.toJndiEnvironment() );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=akarasulu,ou=users,ou=system" );

        try
        {
            setSysRoot( env );
            fail( "should not get here due to exception" );
        }
        catch ( ConfigurationException e )
        {
        }
    }


    /**
     * Tests to make sure we can authenticate after the database has already
     * been started by the admin user when simple authentication is in effect.
     *
     * @throws Exception if anything goes wrong
     */
    public void test8PassPrincAuthTypeSimple() throws Exception
    {
        Hashtable env = new Hashtable( configuration.toJndiEnvironment() );
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
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
        Hashtable env = new Hashtable( configuration.toJndiEnvironment() );
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=akarasulu,ou=users,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "test" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        assertNotNull( new InitialContext( env ) );
    }


    public void test11InvalidateCredentialCache() throws NamingException
    {
        Hashtable env = new Hashtable( configuration.toJndiEnvironment() );
        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=akarasulu,ou=users,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "test" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        InitialDirContext ic = new InitialDirContext( env );
        Attributes attrs = ic.getAttributes( "uid=akarasulu,ou=users" );
        Attribute ou = attrs.get( "ou" );
        assertTrue( ou.contains( "Engineering" ) );
        assertTrue( ou.contains( "People" ) );

        Attribute objectClass = attrs.get( "objectClass" );
        assertTrue( objectClass.contains( "top" ) );
        assertTrue( objectClass.contains( "person" ) );
        assertTrue( objectClass.contains( "organizationalPerson" ) );
        assertTrue( objectClass.contains( "inetOrgPerson" ) );

        assertTrue( attrs.get( "telephonenumber" ).contains( "+1 408 555 4798" ) );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
        assertTrue( attrs.get( "givenname" ).contains( "Alex" ) );
        assertTrue( attrs.get( "mail" ).contains( "akarasulu@apache.org" ) );
        assertTrue( attrs.get( "l" ).contains( "Bogusville" ) );
        assertTrue( attrs.get( "sn" ).contains( "Karasulu" ) );
        assertTrue( attrs.get( "cn" ).contains( "Alex Karasulu" ) );
        assertTrue( attrs.get( "facsimiletelephonenumber" ).contains( "+1 408 555 9751" ) );
        assertTrue( attrs.get( "roomnumber" ).contains( "4612" ) );
        
        // now modify the password for akarasulu
        LockableAttributeImpl userPasswordAttribute = new LockableAttributeImpl( "userPassword", "newpwd" );
        ic.modifyAttributes( "uid=akarasulu,ou=users", new ModificationItem[] { 
            new ModificationItem( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );
        
        // close and try with old password (should fail)
        ic.close();
        env.put( Context.SECURITY_CREDENTIALS, "test" );
        try
        {
            ic = new InitialDirContext( env );
            fail( "Authentication with old password should fail" );
        }
        catch ( NamingException e )
        {
            // we should fail 
        }

        // close and try again now with new password (should fail)
        ic.close();
        env.put( Context.SECURITY_CREDENTIALS, "newpwd" );
        ic = new InitialDirContext( env );
        attrs = ic.getAttributes( "uid=akarasulu,ou=users" );
        ou = attrs.get( "ou" );
        assertTrue( ou.contains( "Engineering" ) );
        assertTrue( ou.contains( "People" ) );

        objectClass = attrs.get( "objectClass" );
        assertTrue( objectClass.contains( "top" ) );
        assertTrue( objectClass.contains( "person" ) );
        assertTrue( objectClass.contains( "organizationalPerson" ) );
        assertTrue( objectClass.contains( "inetOrgPerson" ) );

        assertTrue( attrs.get( "telephonenumber" ).contains( "+1 408 555 4798" ) );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
        assertTrue( attrs.get( "givenname" ).contains( "Alex" ) );
        assertTrue( attrs.get( "mail" ).contains( "akarasulu@apache.org" ) );
        assertTrue( attrs.get( "l" ).contains( "Bogusville" ) );
        assertTrue( attrs.get( "sn" ).contains( "Karasulu" ) );
        assertTrue( attrs.get( "cn" ).contains( "Alex Karasulu" ) );
        assertTrue( attrs.get( "facsimiletelephonenumber" ).contains( "+1 408 555 9751" ) );
        assertTrue( attrs.get( "roomnumber" ).contains( "4612" ) );
    }
}
