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


import static org.apache.directory.server.core.integ.IntegrationUtils.apply;
import static org.apache.directory.server.core.integ.IntegrationUtils.getUserAddLdif;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.ArrayUtils;
import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A set of simple tests to make sure simple authentication is working as it
 * should.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith( FrameworkRunner.class )
public class SimpleAuthenticationIT extends AbstractLdapTestUnit
{
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
    public void testAdminAccountCreation() throws Exception
    {
        String userDn = "uid=admin,ou=system";
        LdapContext ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( "ou=system" ) );
        Attributes attrs = ctx.getAttributes( "uid=admin" );
        performAdminAccountChecks( attrs );
        assertTrue( ArrayUtils.isEquals( attrs.get( "userPassword" ).get(), StringTools.getBytesUtf8( "secret" ) ) );
        ctx.close();

        service.shutdown();
        service.startup();

        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( "ou=system" ) );
        attrs = ctx.getAttributes( "uid=admin" );
        performAdminAccountChecks( attrs );
        assertTrue( ArrayUtils.isEquals( attrs.get( "userPassword" ).get(), StringTools.getBytesUtf8( "secret" ) ) );
        ctx.close();
    }


    @Test
    public void test3UseAkarasulu() throws Exception
    {
        apply( service, getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );

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
        assertNotNull( new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( userDn ) ) );
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
        apply( service, getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        assertNotNull( new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) ) );
    }


    @Test
    public void test11InvalidateCredentialCache() throws Exception
    {
        apply( service, getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        
        LdapContext ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );
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
        Attribute userPasswordAttribute = new BasicAttribute( "userPassword", "newpwd" );
        ctx.modifyAttributes( "", new ModificationItem[] {
            new ModificationItem( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();

        try
        {
            new ServerLdapContext( service, 
                service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );
            fail( "Authentication with old password should fail" );
        }
        catch ( NamingException e )
        {
            // we should fail
        }

        // close and try again now with new password (should fail)
        ctx.close();
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "newpwd".getBytes() ), new LdapDN( userDn ) ); 
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


    @Test
    public void testSHA() throws Exception
    {
        apply( service, getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) ); 

        // Check that we can get the attributes
        Attributes attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using SHA
        Attribute userPasswordAttribute = new BasicAttribute( "userPassword", "{SHA}5en6G6MezRroT3XKqkdPOmY/BfQ=" );
        ctx.modifyAttributes( "", new ModificationItem[] {
            new ModificationItem( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();

        try
        {
            ctx = new ServerLdapContext( service, 
                service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );
            fail( "Authentication with old password should fail" );
        }
        catch ( Exception e )
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
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( userDn ) );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // close and try again now with new password, to check that the
        // cache is updated (should be successfull)
        ctx.close();
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( userDn ) ); 
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testSSHA() throws Exception
    {
        apply( service, getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );

        // Check that we can get the attributes
        Attributes attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using SHA
        BasicAttribute userPasswordAttribute = new BasicAttribute( "userPassword", "{SSHA}mjVVxasFkk59wMW4L1Ldt+YCblfhULHs03WW7g==" );
        ctx.modifyAttributes( "", new ModificationItem[] {
            new ModificationItem( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();

        try
        {
            ctx = new ServerLdapContext( service, 
                service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );
            fail( "Authentication with old password should fail" );
        }
        catch ( Exception e )
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
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( userDn ) );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // close and try again now with new password, to check that the
        // cache is updated (should be successfull)
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( userDn ) );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
    }
    
    
    @Test
    public void testSSHA4BytesSalt() throws Exception
    {
        apply( service, getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );

        // Check that we can get the attributes
        Attributes attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'test123', encrypted using SHA with a 4 bytes salt
        BasicAttribute userPasswordAttribute = new BasicAttribute( "userPassword", "{SSHA}0TT388zsWzHKtMEpIU/8/W68egchNEWp" );
        ctx.modifyAttributes( "", new ModificationItem[] {
            new ModificationItem( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();

        try
        {
            ctx = new ServerLdapContext( service, 
                service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );
            fail( "Authentication with old password should fail" );
        }
        catch ( Exception e )
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

        // try again now with new password (should be successful)
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "test123".getBytes() ), new LdapDN( userDn ) );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // close and try again now with new password, to check that the
        // cache is updated (should be successfull)
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "test123".getBytes() ), new LdapDN( userDn ) );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testMD5() throws Exception
    {
        apply( service, getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );

        // Check that we can get the attributes
        Attributes attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using MD5
        Attribute userPasswordAttribute = new BasicAttribute( "userPassword", "{MD5}Xr4ilOzQ4PCOq3aQ0qbuaQ==" );
        ctx.modifyAttributes( "", new ModificationItem[] {
            new ModificationItem( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();

        try
        {
            ctx = new ServerLdapContext( service, 
                service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );
            fail( "Authentication with old password should fail" );
        }
        catch ( Exception e )
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
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( userDn ) );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // try again now with new password, to check that the
        // cache is updated (should be successfull)
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( userDn ) );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testSMD5() throws Exception
    {
        apply( service, getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );

        // Check that we can get the attributes
        Attributes attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using SMD5
        Attribute userPasswordAttribute = new BasicAttribute( "userPassword", "{SMD5}tQ9wo/VBuKsqBtylMMCcORbnYOJFMyDJ" );
        ctx.modifyAttributes( "", new ModificationItem[] {
            new ModificationItem( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();

        try
        {
            ctx = new ServerLdapContext( service, 
                service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );
            fail( "Authentication with old password should fail" );
        }
        catch ( Exception e )
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

        // try again now with new password (should be successful)
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( userDn ) );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // try again now with new password, to check that the
        // cache is updated (should be successfull)
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( userDn ) );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testCRYPT() throws Exception
    {
        apply( service, getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );

        // Check that we can get the attributes
        Attributes attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using CRYPT
        Attribute userPasswordAttribute = new BasicAttribute( "userPassword", "{crypt}qFkH8Z1woBlXw" );
        ctx.modifyAttributes( "", new ModificationItem[] {
            new ModificationItem( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );

        // close and try with old password (should fail)
        ctx.close();

        try
        {
            ctx = new ServerLdapContext( service, 
                service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );
            fail( "Authentication with old password should fail" );
        }
        catch ( Exception e )
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
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( userDn ) );
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );

        // try again now with new password, to check that the
        // cache is updated (should be successfull)
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( userDn ) ); 
        attrs = ctx.getAttributes( "" );
        assertNotNull( attrs );
        assertTrue( attrs.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testInvalidateCredentialCacheForUpdatingAnotherUsersPassword() throws Exception
    {
        apply( service, getUserAddLdif() );

        // bind as akarasulu
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapContext ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );
        ctx.close();

        // bind as admin
        userDn = "uid=admin,ou=system";
        ctx = new ServerLdapContext( service, 
            service.getSession( new LdapDN( userDn ), "secret".getBytes() ), new LdapDN( userDn ) );

        // now modify the password for akarasulu (while we're admin)
        Attribute userPasswordAttribute = new BasicAttribute( "userPassword", "newpwd" );
        ctx.modifyAttributes( "", new ModificationItem[] {
            new ModificationItem( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute ) } );
        ctx.close();

        try
        {
            ctx = new ServerLdapContext( service, 
                service.getSession( new LdapDN( userDn ), "test".getBytes() ), new LdapDN( userDn ) );
            fail( "Authentication with old password should fail" );
        }
        catch ( Exception e )
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
