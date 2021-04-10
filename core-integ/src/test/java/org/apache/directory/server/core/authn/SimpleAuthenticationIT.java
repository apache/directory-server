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
import static org.apache.directory.server.core.integ.IntegrationUtils.getConnectionAs;
import static org.apache.directory.server.core.integ.IntegrationUtils.getUserAddLdif;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Objects;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * A set of simple tests to make sure simple authentication is working as it
 * should.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "SimpleAuthenticationIT-DS")
public class SimpleAuthenticationIT extends AbstractLdapTestUnit
{
    /**
     * Checks all attributes of the admin account entry minus the userPassword
     * attribute.
     *
     * @param entry the entries attributes
     */
    protected void performAdminAccountChecks( Entry entry )
    {
        assertTrue( entry.get( "objectClass" ).contains( "top" ) );
        assertTrue( entry.get( "objectClass" ).contains( "person" ) );
        assertTrue( entry.get( "objectClass" ).contains( "organizationalPerson" ) );
        assertTrue( entry.get( "objectClass" ).contains( "inetOrgPerson" ) );
        assertTrue( entry.get( "displayName" ).contains( "Directory Superuser" ) );
    }


    @AfterEach
    public void closeConnections()
    {
        IntegrationUtils.closeConnections();
    }


    /**
     * Check the creation of the admin account and persistence across restarts.
     *
     * @throws Exception if there are failures
     */
    @Test
    public void testAdminAccountCreation() throws Exception
    {
        String userDn = "uid=admin,ou=system";
        LdapConnection connection = getConnectionAs( getService(), userDn, "secret" );

        Entry entry = connection.lookup( userDn );
        performAdminAccountChecks( entry );
        assertTrue( Objects.deepEquals( entry.get( "userPassword" ).get().getBytes(), Strings
            .getBytesUtf8( "secret" ) ) );
        connection.close();

        getService().shutdown();
        getService().startup();

        connection = getConnectionAs( getService(), userDn, "secret" );
        entry = connection.lookup( userDn );
        performAdminAccountChecks( entry );
        assertTrue( Objects.deepEquals( entry.get( "userPassword" ).get().getBytes(), Strings
            .getBytesUtf8( "secret" ) ) );
        connection.close();
    }


    @Test
    public void test3UseAkarasulu() throws Exception
    {
        apply( getService(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapConnection connection = getConnectionAs( getService(), userDn, "test" );

        Entry entry = connection.lookup( userDn );
        Attribute ou = entry.get( "ou" );
        assertTrue( ou.contains( "Engineering" ) );
        assertTrue( ou.contains( "People" ) );

        Attribute objectClass = entry.get( "objectClass" );
        assertTrue( objectClass.contains( "top" ) );
        assertTrue( objectClass.contains( "person" ) );
        assertTrue( objectClass.contains( "organizationalPerson" ) );
        assertTrue( objectClass.contains( "inetOrgPerson" ) );

        assertTrue( entry.get( "telephonenumber" ).contains( "+1 408 555 4798" ) );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );
        assertTrue( entry.get( "givenname" ).contains( "Alex" ) );
        assertTrue( entry.get( "mail" ).contains( "akarasulu@apache.org" ) );
        assertTrue( entry.get( "l" ).contains( "Bogusville" ) );
        assertTrue( entry.get( "sn" ).contains( "Karasulu" ) );
        assertTrue( entry.get( "cn" ).contains( "Alex Karasulu" ) );
        assertTrue( entry.get( "facsimiletelephonenumber" ).contains( "+1 408 555 9751" ) );
        assertTrue( entry.get( "roomnumber" ).contains( "4612" ) );
        connection.close();
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
        LdapConnection connection = getConnectionAs( getService(), userDn, "secret" );
        assertTrue( connection.isAuthenticated() );
        connection.close();
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
        apply( getService(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapConnection connection = getConnectionAs( getService(), userDn, "test" );
        assertTrue( connection.isAuthenticated() );
        connection.close();
    }


    @Test
    public void test11InvalidateCredentialCache() throws Exception
    {
        apply( getService(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";

        LdapConnection connection = getConnectionAs( getService(), userDn, "test" );

        Entry entry = connection.lookup( userDn );
        Attribute ou = entry.get( "ou" );
        assertTrue( ou.contains( "Engineering" ) );
        assertTrue( ou.contains( "People" ) );

        Attribute objectClass = entry.get( "objectClass" );
        assertTrue( objectClass.contains( "top" ) );
        assertTrue( objectClass.contains( "person" ) );
        assertTrue( objectClass.contains( "organizationalPerson" ) );
        assertTrue( objectClass.contains( "inetOrgPerson" ) );

        assertTrue( entry.get( "telephonenumber" ).contains( "+1 408 555 4798" ) );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );
        assertTrue( entry.get( "givenname" ).contains( "Alex" ) );
        assertTrue( entry.get( "mail" ).contains( "akarasulu@apache.org" ) );
        assertTrue( entry.get( "l" ).contains( "Bogusville" ) );
        assertTrue( entry.get( "sn" ).contains( "Karasulu" ) );
        assertTrue( entry.get( "cn" ).contains( "Alex Karasulu" ) );
        assertTrue( entry.get( "facsimiletelephonenumber" ).contains( "+1 408 555 9751" ) );
        assertTrue( entry.get( "roomnumber" ).contains( "4612" ) );

        // now modify the password for akarasulu
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( getService().getSchemaManager(), userDn ) );
        modReq.replace( "userPassword", "newpwd" );
        connection.modify( modReq );

        // close and try with old password (should fail)
        connection.close();

        try
        {
            connection.bind( userDn, "test" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            assertTrue( true );
        }

        // close and try again now with new password (should succeed)
        connection.bind( userDn, "newpwd" );

        entry = connection.lookup( userDn );
        ou = entry.get( "ou" );
        assertTrue( ou.contains( "Engineering" ) );
        assertTrue( ou.contains( "People" ) );

        objectClass = entry.get( "objectClass" );
        assertTrue( objectClass.contains( "top" ) );
        assertTrue( objectClass.contains( "person" ) );
        assertTrue( objectClass.contains( "organizationalPerson" ) );
        assertTrue( objectClass.contains( "inetOrgPerson" ) );

        assertTrue( entry.get( "telephonenumber" ).contains( "+1 408 555 4798" ) );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );
        assertTrue( entry.get( "givenname" ).contains( "Alex" ) );
        assertTrue( entry.get( "mail" ).contains( "akarasulu@apache.org" ) );
        assertTrue( entry.get( "l" ).contains( "Bogusville" ) );
        assertTrue( entry.get( "sn" ).contains( "Karasulu" ) );
        assertTrue( entry.get( "cn" ).contains( "Alex Karasulu" ) );
        assertTrue( entry.get( "facsimiletelephonenumber" ).contains( "+1 408 555 9751" ) );
        assertTrue( entry.get( "roomnumber" ).contains( "4612" ) );
    }


    @Test
    public void testSHA() throws Exception
    {
        apply( getService(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapConnection connection = getConnectionAs( getService(), userDn, "test" );

        // Check that we can get the attributes

        Entry entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using SHA
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( getService().getSchemaManager(), userDn ) );
        modReq.replace( "userPassword", "{SHA}5en6G6MezRroT3XKqkdPOmY/BfQ=" );
        connection.modify( modReq );

        // close and try with old password (should fail)
        connection.close();

        try
        {
            connection.bind( userDn, "test" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            assertTrue( true );
        }

        // try again now with new password (should be successful)
        connection.bind( userDn, "secret" );
        assertTrue( connection.isAuthenticated() );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // close and try again now with new password, to check that the
        // cache is updated (should be successfull)
        connection.close();
        connection.bind( userDn, "secret" );
        assertTrue( connection.isAuthenticated() );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testSSHA() throws Exception
    {
        apply( getService(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapConnection connection = getConnectionAs( getService(), userDn, "test" );

        // Check that we can get the attributes
        Entry entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using SHA
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( getService().getSchemaManager(), userDn ) );
        modReq.replace( "userPassword", "{SSHA}mjVVxasFkk59wMW4L1Ldt+YCblfhULHs03WW7g==" );
        connection.modify( modReq );

        // close and try with old password (should fail)
        connection.close();

        try
        {
            connection.bind( userDn, "test" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            assertTrue( true );
        }

        // try again now with new password (should be successful)
        connection.bind( userDn, "secret" );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // close and try again now with new password, to check that the
        // cache is updated (should be successfull)
        connection.close();
        connection.bind( userDn, "secret" );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testSSHA4BytesSalt() throws Exception
    {
        apply( getService(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapConnection connection = getConnectionAs( getService(), userDn, "test" );

        // Check that we can get the attributes
        Entry entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'test123', encrypted using SHA with a 4 bytes salt
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( getService().getSchemaManager(), userDn ) );
        modReq.replace( "userPassword", "{SSHA}0TT388zsWzHKtMEpIU/8/W68egchNEWp" );
        connection.modify( modReq );

        // close and try with old password (should fail)
        connection.close();

        try
        {
            connection.bind( userDn, "test" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            assertTrue( true );
        }

        // try again now with new password (should be successful)
        connection.bind( userDn, "test123" );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // close and try again now with new password, to check that the
        // cache is updated (should be successfull)
        connection.close();
        connection.bind( userDn, "test123" );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testMD5() throws Exception
    {
        apply( getService(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapConnection connection = getConnectionAs( getService(), userDn, "test" );

        // Check that we can get the attributes
        Entry entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using MD5
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( getService().getSchemaManager(), userDn ) );
        modReq.replace( "userPassword", "{MD5}Xr4ilOzQ4PCOq3aQ0qbuaQ==" );
        connection.modify( modReq );

        // close and try with old password (should fail)
        connection.close();

        try
        {
            connection.bind( userDn, "test" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            assertTrue( true );
        }

        // try again now with new password (should be successful)
        connection.bind( userDn, "secret" );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // try again now with new password, to check that the
        // cache is updated (should be successfull)

        connection.close();
        connection.bind( userDn, "secret" );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testSMD5() throws Exception
    {
        apply( getService(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapConnection connection = getConnectionAs( getService(), userDn, "test" );

        // Check that we can get the attributes
        Entry entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using SMD5
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( getService().getSchemaManager(), userDn ) );
        modReq.replace( "userPassword", "{SMD5}tQ9wo/VBuKsqBtylMMCcORbnYOJFMyDJ" );
        connection.modify( modReq );

        // close and try with old password (should fail)
        connection.close();

        try
        {
            connection.bind( userDn, "test" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            assertTrue( true );
        }

        // try again now with new password (should be successful)
        connection.bind( userDn, "secret" );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // try again now with new password, to check that the
        // cache is updated (should be successfull)
        connection.close();
        connection.bind( userDn, "secret" );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testCRYPT() throws Exception
    {
        apply( getService(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapConnection connection = getConnectionAs( getService(), userDn, "test" );

        // Check that we can get the attributes
        Entry entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using CRYPT
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( getService().getSchemaManager(), userDn ) );
        modReq.replace( "userPassword", "{crypt}qFkH8Z1woBlXw" );
        connection.modify( modReq );

        // close and try with old password (should fail)
        connection.close();

        try
        {
            connection.bind( userDn, "test" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            assertTrue( true );
        }

        // try again now with new password (should be successful)
        connection.bind( userDn, "secret" );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // try again now with new password, to check that the
        // cache is updated (should be successfull)
        connection.bind( userDn, "secret" );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testBCRYPT() throws Exception
    {
        apply( getService(), getUserAddLdif() );
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapConnection connection = getConnectionAs( getService(), userDn, "test" );

        // Check that we can get the attributes
        Entry entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // now modify the password for akarasulu : 'secret', encrypted using CRYPT
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( userDn ) );

        // The hash is for 'secret'
        modReq.replace( "userPassword", "{crypt}$2a$06$LH2xIb/TZmajuLJGDNuegeeY.SCwkg6YAVLNXTh8n4Xfb1uwmLXg6" );
        connection.modify( modReq );

        // close and try with old password (should fail)
        connection.close();

        try
        {
            connection.bind( userDn, "test" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            assertTrue( true );
        }

        // try again now with new password (should be successful)
        connection.bind( userDn, "secret" );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );

        // try again now with new password, to check that the
        // cache is updated (should be successfull)
        connection.bind( userDn, "secret" );
        entry = connection.lookup( userDn );
        assertNotNull( entry );
        assertTrue( entry.get( "uid" ).contains( "akarasulu" ) );
    }


    @Test
    public void testInvalidateCredentialCacheForUpdatingAnotherUsersPassword() throws Exception
    {
        apply( getService(), getUserAddLdif() );

        // bind as akarasulu
        String userDn = "uid=akarasulu,ou=users,ou=system";
        LdapConnection connection = getConnectionAs( getService(), userDn, "test" );
        connection.close();

        // bind as admin
        String adminUserDn = "uid=admin,ou=system";
        connection.bind( adminUserDn, "secret" );

        // now modify the password for akarasulu (while we're admin)
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( getService().getSchemaManager(), userDn ) );
        modReq.replace( "userPassword", "newpwd" );
        connection.modify( modReq );
        connection.close();

        try
        {
            connection.bind( userDn, "test" );
            fail();
        }
        catch ( LdapAuthenticationException lae )
        {
            assertTrue( true );
        }
    }
}
