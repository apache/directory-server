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
package org.apache.directory.server.core.authz;


import static org.apache.directory.server.core.authz.AutzIntegUtils.addUserToGroup;
import static org.apache.directory.server.core.authz.AutzIntegUtils.createAccessControlSubentry;
import static org.apache.directory.server.core.authz.AutzIntegUtils.createUser;
import static org.apache.directory.server.core.authz.AutzIntegUtils.getAdminConnection;
import static org.apache.directory.server.core.authz.AutzIntegUtils.getConnectionAs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.CompareResponse;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests whether or not authorization around entry compare operations work properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(enableAccessControl = true, name = "CompareAuthorizationIT")
public class CompareAuthorizationIT extends AbstractLdapTestUnit
{

    @Before
    public void setService()
    {
        AutzIntegUtils.service = service;
    }


    @After
    public void closeConnections()
    {
        IntegrationUtils.closeConnections();
    }


    /**
     * Checks if an attribute of a simple entry (an organizationalUnit's telephoneNumber)
     * with an Rdn relative to ou=system can be compared by a specific non-admin user.
     * If a permission exception is encountered it is caught and false is returned,
     * otherwise true is returned.  The entry is deleted after being created just in case
     * subsequent calls to this method are made in the same test case: the admin account
     * is used to add and delete this test entry so permissions to add and delete are not
     * required to test the compare operation by the user.
     *
     * @param uid the unique identifier for the user (presumed to exist under ou=users,ou=system)
     * @param password the password of this user
     * @param entryRdn the relative Dn, relative to ou=system where entry is created
     * for comparison test
     * @param number the telephone number to compare to this one
     * @return true if the entry's telephoneNumber can be compared by the user at the
     * specified location, false otherwise.  A false compare result still returns
     * true.
     * @throws Exception if there are problems conducting the test
     */
    public boolean checkCanCompareTelephoneNumberAs( String uid, String password, String entryRdn, String number )
        throws Exception
    {

        Dn entryDn = new Dn( entryRdn + ",ou=system" );
        boolean result = true;

        // create the entry with the telephoneNumber attribute to compare
        Entry testEntry = new DefaultEntry(entryDn);
        testEntry.add( SchemaConstants.OBJECT_CLASS_AT, "organizationalUnit" );
        testEntry.add( SchemaConstants.OU_AT, "testou" );
        testEntry.add( "telephoneNumber", "867-5309" ); // jenny don't change your number

        LdapConnection adminConnection = getAdminConnection();

        // create the entry as admin
        adminConnection.add( testEntry );

        Dn userName = new Dn( "uid=" + uid + ",ou=users,ou=system" );
        // compare the telephone numbers
        LdapConnection userConnection = getConnectionAs( userName, password );
        CompareResponse resp = userConnection.compare(entryDn, "telephoneNumber", number );

        // don't set based on compare result success/failure but based on whether the op was permitted or not
        if ( resp.getLdapResult().getResultCode() == ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS )
        {
            result = false;
        }

        // let's clean up
        adminConnection.delete( entryRdn );

        return result;
    }


    /**
     * Checks to make sure group membership based userClass works for compare operations.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testGrantCompareAdministrators() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a compare operation which should fail without any ACI
        assertFalse( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );

        // Gives grantCompare, and grantRead perm to all users in the Administrators group for
        // entries and all attribute types and values
        createAccessControlSubentry( "administratorAdd", "{ " + "  identificationTag \"addAci\", "
            + "  precedence 14, " + "  authenticationLevel none, " + "  itemOrUserFirst userFirst: " + "  { "
            + "    userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }" + "    userPermissions "
            + "    { " + "      { " + "        protectedItems { entry, allUserAttributeTypesAndValues }, "
            + "        grantsAndDenials { grantCompare, grantRead, grantBrowse } " + "      } " + "    } " + "  } "
            + "}" );

        // see if we can now add that test entry which we could not before
        // add op should still fail since billd is not in the admin group
        assertFalse( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );

        // now add billyd to the Administrator group and try again
        addUserToGroup( "billyd", "Administrators" );

        // try an add operation which should succeed with ACI and group membership change
        assertTrue( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "976-6969" ) );
    }


    /**
     * Checks to make sure name based userClass works for compare operations.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testGrantCompareByName() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an compare operation which should fail without any ACI
        assertFalse( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );

        // now add a subentry that enables user billyd to compare an entry below ou=system
        createAccessControlSubentry( "billydAdd", "{ " + "  identificationTag \"addAci\", " + "  precedence 14, "
            + "  authenticationLevel none, " + "  itemOrUserFirst userFirst: " + "  { "
            + "    userClasses { name { \"uid=billyd,ou=users,ou=system\" } }, " + "    userPermissions " + "    { "
            + "      { " + "        protectedItems { entry, allUserAttributeTypesAndValues }, "
            + "        grantsAndDenials { grantCompare, grantRead, grantBrowse } " + "      } " + "    } " + "  } "
            + "}" );

        // should work now that billyd is authorized by name
        assertTrue( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );
    }


    /**
     * Checks to make sure subtree based userClass works for compare operations.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testGrantCompareBySubtree() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a compare operation which should fail without any ACI
        assertFalse( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );

        // now add a subentry that enables user billyd to compare an entry below ou=system
        createAccessControlSubentry( "billyAddBySubtree", "{ " + "  identificationTag \"addAci\", "
            + "  precedence 14, " + "  authenticationLevel none, " + "  itemOrUserFirst userFirst: " + "  { "
            + "    userClasses " + "    { " + "      subtree { { base \"ou=users,ou=system\" } } " + "    }, "
            + "    userPermissions " + "    { " + "        { "
            + "        protectedItems {entry, allUserAttributeTypesAndValues}, "
            + "        grantsAndDenials { grantCompare, grantRead, grantBrowse } " + "      } " + "    } " + "  } "
            + "}" );

        // should work now that billyd is authorized by the subtree userClass
        assertTrue( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );
    }


    /**
     * Checks to make sure <b>allUsers</b> userClass works for compare operations.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testGrantCompareAllUsers() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an add operation which should fail without any ACI
        assertFalse( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );

        // now add a subentry that enables anyone to add an entry below ou=system
        createAccessControlSubentry( "anybodyAdd", "{ " + "  identificationTag \"addAci\", " + "  precedence 14, "
            + "  authenticationLevel none, " + "  itemOrUserFirst userFirst: " + "  { "
            + "    userClasses { allUsers }, " + "    userPermissions " + "    { " + "      { "
            + "        protectedItems {entry, allUserAttributeTypesAndValues}, "
            + "        grantsAndDenials { grantCompare, grantRead, grantBrowse } " + "      } " + "    } " + "  } "
            + "}" );

        // see if we can now compare that test entry's number which we could not before
        // should work with billyd now that all users are authorized
        assertTrue( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );
    }


    @Test
    public void testPasswordCompare() throws Exception
    {
        LdapConnection adminCtx = getAdminConnection();

        Dn userDn = new Dn( "uid=bob,ou=users,ou=system" );
        Entry user = new DefaultEntry(userDn);
        user.add( SchemaConstants.UID_AT, "bob" );
        user.add( SchemaConstants.USER_PASSWORD_AT, "bobspassword" );
        user.add( SchemaConstants.OBJECT_CLASS_AT, "person", "organizationalPerson", "inetOrgPerson" );
        user.add( SchemaConstants.SN_AT, "bob" );
        user.add( SchemaConstants.CN_AT, "bob" );

        adminCtx.add( user );

        CompareResponse resp = adminCtx.compare(userDn, "userPassword", "bobspassword" );
        assertEquals( ResultCodeEnum.COMPARE_TRUE, resp.getLdapResult().getResultCode() );
    }

}
