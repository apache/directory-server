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
package org.apache.directory.server.core.authn;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.changelog.Tag;
import org.apache.directory.server.core.unit.TestMode;
import org.apache.directory.server.core.unit.DirectoryServiceFactory;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ArrayUtils;
import static org.apache.directory.server.core.unit.IntegrationUtils.*;

import org.junit.*;
import static org.junit.Assert.*;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;
import java.util.Arrays;
import java.util.List;


/**
 * A set of simple tests to make sure simple authentication is working as it
 * should.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SimpleAuthenticationITest
{
    private static DirectoryService service;
    private static Tag startTag;

    private String password;
    private TestMode mode = TestMode.ROLLBACK;
    private DirectoryServiceFactory factory = DirectoryServiceFactory.DEFAULT;


    public SimpleAuthenticationITest()
    {
    }


    public SimpleAuthenticationITest( String password )
    {
        this.password = password;
    }


    public String getPassword()
    {
        return password;
    }


    @AfterClass
    public static void afterClass() throws Exception
    {
        if ( service != null && service.isStarted() )
        {
            service.shutdown();
        }

        if ( service != null )
        {
            doDelete( service.getWorkingDirectory() );
        }

        service = null;
    }


    @Before
    public void setUp() throws Exception
    {
        if ( service == null )
        {
            service = factory.newInstance();
        }

        if ( mode == TestMode.PRISTINE )
        {
            doDelete( service.getWorkingDirectory() );
        }

        if ( mode == TestMode.ROLLBACK )
        {
            service.getChangeLog().setEnabled( true );
        }

        if ( ! service.isStarted() )
        {
            service.startup();
        }

        startTag = service.getChangeLog().tag();
    }


    @After
    public void tearDown() throws Exception
    {
        switch( mode.ordinal )
        {
            case( TestMode.PRISTINE_ORDINAL ):
                service.shutdown();
                doDelete( service.getWorkingDirectory() );
                service = null;
                break;
            case( TestMode.RESTART_ORDINAL ):
                service.startup();
                break;
            case( TestMode.ROLLBACK_ORDINAL ):
                if ( startTag != null && ( startTag.getRevision() < service.getChangeLog().getCurrentRevision() ) )
                {
                    service.revert( startTag.getRevision() );
                }
                break;
            case( TestMode.ADDITIVE_ORDINAL ):
                break;
            default:
                throw new IllegalStateException( "Unidentified test mode: " + mode.ordinal );
        }
    }


    public static LdapContext getRootDSE() throws NamingException
    {
        if ( service.isStarted() )
        {
            LdapDN dn = new LdapDN( "uid=admin,ou=system" );
            dn.normalize( service.getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
            return service.getJndiContext( new LdapPrincipal( dn, AuthenticationLevel.SIMPLE ) );
        }

        throw new IllegalStateException( "Cannot acquire rootDSE before the service has been started!" );
    }


    public static LdapContext getRootDSE( String bindDn ) throws NamingException
    {
        if ( service.isStarted() )
        {
            LdapDN dn = new LdapDN( bindDn );
            dn.normalize( service.getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
            return service.getJndiContext( new LdapPrincipal( dn, AuthenticationLevel.SIMPLE ) );
        }

        throw new IllegalStateException( "Cannot acquire rootDSE before the service has been started!" );
    }


    public static LdapContext getSystemRoot() throws NamingException
    {
        if ( service.isStarted() )
        {
            LdapDN dn = new LdapDN( "uid=admin,ou=system" );
            dn.normalize( service.getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
            return service.getJndiContext( new LdapPrincipal(
                    dn, AuthenticationLevel.SIMPLE ), "ou=system" );
        }

        throw new IllegalStateException( "Cannot acquire rootDSE before the service has been started!" );
    }


    public static LdapContext getSystemRoot( String bindDn ) throws NamingException
    {
        if ( service.isStarted() )
        {
            LdapDN dn = new LdapDN( bindDn );
            dn.normalize( service.getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
            return service.getJndiContext( new LdapPrincipal( dn, AuthenticationLevel.SIMPLE ), "ou=system" );
        }

        throw new IllegalStateException( "Cannot acquire rootDSE before the service has been started!" );
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
     * Check the creation of the admin account and persistence across restarts.
     *
     * @throws NamingException if there are failures
     */
    @Test
    public void testAdminAccountCreation() throws NamingException
    {
        String userDn = "uid=admin,ou=system";
        LdapContext ctx = service.getJndiContext( new LdapDN( userDn ), userDn,
                "secret".getBytes(), "simple", "ou=system" );
        Attributes attrs = ctx.getAttributes( "uid=admin" );
        performAdminAccountChecks( attrs );
        assertTrue( ArrayUtils.isEquals( attrs.get( "userPassword" ).get(), "secret".getBytes() ) );
        ctx.close();

        service.shutdown();
        service.startup();

        ctx = service.getJndiContext( new LdapDN( userDn ), userDn,
                "secret".getBytes(), "simple", "ou=system" );
        attrs = ctx.getAttributes( "uid=admin" );
        performAdminAccountChecks( attrs );
        assertTrue( ArrayUtils.isEquals( attrs.get( "userPassword" ).get(), "secret".getBytes() ) );
        ctx.close();
    }


    @Test
    public void test3UseAkarasulu() throws NamingException
    {
        apply( getRootDSE(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );

        Attributes attrs = ctx.getAttributes( "" );
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
     * Tests to make sure we can authenticate after the database has already
     * been started by the admin user when simple authentication is in effect.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void test8PassPrincAuthTypeSimple() throws Exception
    {
        String userDn = "uid=admin,ou=system";
        assertNotNull( service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn ) );
    }


    /**
     * Checks to see if we can authenticate as a test user after the admin fires
     * up and builds the the system database.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void test10TestNonAdminUser() throws Exception
    {
        apply( getRootDSE(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        assertNotNull( service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn ) );
    }


    @Test
    public void test11InvalidateCredentialCache() throws NamingException
    {
        apply( getRootDSE(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );
        assertNotNull( ctx );
        Attributes attrs = ctx.getAttributes( "" );
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
        AttributeImpl userPasswordAttribute = new AttributeImpl( "userPassword", "newpwd" );
        ctx.modifyAttributes( "", new ModificationItemImpl[] {
            new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();
        try
        {
            service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );
            fail( "Authentication with old password should fail" );
        }
        catch ( NamingException e )
        {
            // we should fail
        }

        // close and try again now with new password (should fail)
        ctx.close();
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "newpwd".getBytes(), "simple", userDn );
        attrs = ctx.getAttributes( "" );
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


    // @Parameterized.Parameters
    public static List<?> getHashedSecrets()
    {
        //noinspection RedundantArrayCreation
        return Arrays.asList( new Object[] {
                "",
                "",
                ""
        } );
    }


    // @RunWith(Parameterized.class)
    @Ignore ( "This test was put here just to figure out how to use parameterization in junit 4" )
    public void testHashedAuthentication() throws NamingException
    {
        apply( getRootDSE(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );

        // Check that we can get the attributes
        Attributes attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using SHA
        AttributeImpl userPasswordAttribute = new AttributeImpl( "userPassword", password );
        ctx.modifyAttributes( "", new ModificationItemImpl[] {
            new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();

        try
        {
            ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );
            fail( "Authentication with old password should fail" );
        }
        catch ( NamingException e )
        {
            // we should fail
        }
        finally
        {
            if ( ctx != null )
            {
                ctx.close();
            }
        }

        // try again now with new password (should be successfull)
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // close and try again now with new password, to check that the
        // cache is updated (should be successfull)
        ctx.close();
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testSHA() throws NamingException
    {
        apply( getRootDSE(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );

        // Check that we can get the attributes
        Attributes attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using SHA
        AttributeImpl userPasswordAttribute = new AttributeImpl( "userPassword", "{SHA}5en6G6MezRroT3XKqkdPOmY/BfQ=" );
        ctx.modifyAttributes( "", new ModificationItemImpl[] {
            new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();

        try
        {
            ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );
            fail( "Authentication with old password should fail" );
        }
        catch ( NamingException e )
        {
            // we should fail
        }
        finally
        {
            if ( ctx != null )
            {
                ctx.close();
            }
        }

        // try again now with new password (should be successfull)
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // close and try again now with new password, to check that the
        // cache is updated (should be successfull)
        ctx.close();
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testSSHA() throws NamingException
    {
        apply( getRootDSE(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );

        // Check that we can get the attributes
        Attributes attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using SHA
        AttributeImpl userPasswordAttribute = new AttributeImpl( "userPassword", "{SSHA}mjVVxasFkk59wMW4L1Ldt+YCblfhULHs03WW7g==" );
        ctx.modifyAttributes( "", new ModificationItemImpl[] {
            new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();

        try
        {
            ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );
            fail( "Authentication with old password should fail" );
        }
        catch ( NamingException e )
        {
            // we should fail
        }
        finally
        {
            if ( ctx != null )
            {
                ctx.close();
            }
        }

        // try again now with new password (should be successfull)
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // close and try again now with new password, to check that the
        // cache is updated (should be successfull)
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testMD5() throws NamingException
    {
        apply( getRootDSE(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );

        // Check that we can get the attributes
        Attributes attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using MD5
        AttributeImpl userPasswordAttribute = new AttributeImpl( "userPassword", "{MD5}Xr4ilOzQ4PCOq3aQ0qbuaQ==" );
        ctx.modifyAttributes( "", new ModificationItemImpl[] {
            new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();

        try
        {
            ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );
            fail( "Authentication with old password should fail" );
        }
        catch ( NamingException e )
        {
            // we should fail
        }
        finally
        {
            if ( ctx != null )
            {
                ctx.close();
            }
        }

        // try again now with new password (should be successfull)
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // try again now with new password, to check that the
        // cache is updated (should be successfull)
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testSMD5() throws NamingException
    {
        apply( getRootDSE(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );

        // Check that we can get the attributes
        Attributes attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using SHA
        AttributeImpl userPasswordAttribute = new AttributeImpl( "userPassword", "{SMD5}tQ9wo/VBuKsqBtylMMCcORbnYOJFMyDJ" );
        ctx.modifyAttributes( "", new ModificationItemImpl[] {
            new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();

        try
        {
            ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );
            fail( "Authentication with old password should fail" );
        }
        catch ( NamingException e )
        {
            // we should fail
        }
        finally
        {
            if ( ctx != null )
            {
                ctx.close();
            }
        }

        // try again now with new password (should be successfull)
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // try again now with new password, to check that the
        // cache is updated (should be successfull)
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testCRYPT() throws NamingException
    {
        apply( getRootDSE(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );

        // Check that we can get the attributes
        Attributes attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using CRYPT
        AttributeImpl userPasswordAttribute = new AttributeImpl( "userPassword", "{crypt}qFkH8Z1woBlXw" );
        ctx.modifyAttributes( "", new ModificationItemImpl[] {
            new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();

        try
        {
            ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );
            fail( "Authentication with old password should fail" );
        }
        catch ( NamingException e )
        {
            // we should fail
        }
        finally
        {
            if ( ctx != null )
            {
                ctx.close();
            }
        }

        // try again now with new password (should be successfull)
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // try again now with new password, to check that the
        // cache is updated (should be successfull)
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testInvalidateCredentialCacheForUpdatingAnotherUsersPassword() throws NamingException
    {
        apply( getRootDSE(), getUserAddLdif() );

        // bind as akarasulu
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );
        ctx.close();

        // bind as admin
        userDn = "uid=admin,ou=system";
        ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "secret".getBytes(), "simple", userDn );

        // now modify the password for akarasulu (while we're admin)
        AttributeImpl userPasswordAttribute = new AttributeImpl( "userPassword", "newpwd" );
        ctx.modifyAttributes( "", new ModificationItemImpl[] {
            new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );
        ctx.close();

        try
        {
            ctx = service.getJndiContext( new LdapDN( userDn ), userDn, "test".getBytes(), "simple", userDn );
            fail( "Authentication with old password should fail" );
        }
        catch ( NamingException e )
        {
            // we should fail
        }
        finally
        {
            if ( ctx != null )
            {
                ctx.close();
            }
        }
    }
}
