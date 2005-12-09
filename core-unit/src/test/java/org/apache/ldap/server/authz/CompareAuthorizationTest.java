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
package org.apache.ldap.server.authz;


import org.apache.ldap.common.exception.LdapNoPermissionException;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.jndi.ServerLdapContext;

import javax.naming.NamingException;
import javax.naming.directory.*;


/**
 * Tests whether or not authorization around entry compare operations work properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CompareAuthorizationTest extends AbstractAuthorizationTest
{
    /**
     * Checks if an attribute of a simple entry (an organizationalUnit's telephoneNumber)
     * with an RDN relative to ou=system can be compared by a specific non-admin user.
     * If a permission exception is encountered it is caught and false is returned,
     * otherwise true is returned.  The entry is deleted after being created just in case
     * subsequent calls to this method are made in the same test case: the admin account
     * is used to add and delete this test entry so permissions to add and delete are not
     * required to test the compare operation by the user.
     *
     * @param uid the unique identifier for the user (presumed to exist under ou=users,ou=system)
     * @param password the password of this user
     * @param entryRdn the relative DN, relative to ou=system where entry is created
     * for comparison test
     * @param number the telephone number to compare to this one
     * @return true if the entry's telephoneNumber can be compared by the user at the
     * specified location, false otherwise.  A false compare result still returns
     * true.
     * @throws javax.naming.NamingException if there are problems conducting the test
     */
    public boolean checkCanCompareTelephoneNumberAs( String uid, String password, String entryRdn, String number )
            throws NamingException
    {
        // create the entry with the telephoneNumber attribute to compare
        Attributes testEntry = new BasicAttributes( "ou", "testou", true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        testEntry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );
        testEntry.put( "telephoneNumber", "867-5309" );  // jenny don't change your number

        DirContext adminContext = getContextAsAdmin();

        try
        {
            // create the entry as admin
            LdapName userName = new LdapName( "uid="+uid+",ou=users,ou=system" );
            adminContext.createSubcontext( entryRdn, testEntry );

            // compare the telephone numbers
            DirContext userContext = getContextAs( userName, password );
            ServerLdapContext ctx = ( ServerLdapContext ) userContext.lookup( "" );
            ctx.compare( new LdapName( entryRdn + ",ou=system" ), "telephoneNumber", number );

            // don't return compare result which can be false but true since op was permitted
            return true;
        }
        catch ( LdapNoPermissionException e )
        {
            return false;
        }
        finally
        {
            // let's clean up
            adminContext.destroySubcontext( entryRdn );
        }
    }


    /**
     * Checks to make sure group membership based userClass works for compare operations.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantCompareAdministrators() throws NamingException
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a compare operation which should fail without any ACI
        assertFalse( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );

        // Gives grantCompare, and grantRead perm to all users in the Administrators group for
        // entries and all attribute types and values
        createAccessControlSubentry( "administratorAdd", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " +
                "userPermissions { { " +
                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "grantsAndDenials { grantCompare, grantRead, grantBrowse } } } } }" );

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
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantCompareByName() throws NamingException
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an compare operation which should fail without any ACI
        assertFalse( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );

        // now add a subentry that enables user billyd to compare an entry below ou=system
        createAccessControlSubentry( "billydAdd", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { name { \"uid=billyd,ou=users,ou=system\" } }, " +
                "userPermissions { { " +
                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "grantsAndDenials { grantCompare, grantRead, grantBrowse } } } } }" );

        // should work now that billyd is authorized by name
        assertTrue( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );
    }


    /**
     * Checks to make sure subtree based userClass works for compare operations.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantCompareBySubtree() throws NamingException
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a compare operation which should fail without any ACI
        assertFalse( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );

        // now add a subentry that enables user billyd to compare an entry below ou=system
        createAccessControlSubentry( "billyAddBySubtree", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { subtree { { base \"ou=users,ou=system\" } } }, " +
                "userPermissions { { " +
                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "grantsAndDenials { grantCompare, grantRead, grantBrowse } } } } }" );

        // should work now that billyd is authorized by the subtree userClass
        assertTrue( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );
    }


    /**
     * Checks to make sure <b>allUsers</b> userClass works for compare operations.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantCompareAllUsers() throws NamingException
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an add operation which should fail without any ACI
        assertFalse( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );

        // now add a subentry that enables anyone to add an entry below ou=system
        createAccessControlSubentry( "anybodyAdd", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { allUsers }, " +
                "userPermissions { { " +
                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "grantsAndDenials { grantCompare, grantRead, grantBrowse } } } } }" );

        // see if we can now compare that test entry's number which we could not before
        // should work with billyd now that all users are authorized
        assertTrue( checkCanCompareTelephoneNumberAs( "billyd", "billyd", "ou=testou", "867-5309" ) );
    }
    
    public void testPasswordCompare() throws NamingException {
        DirContext adminCtx = getContextAsAdmin();
        Attributes user = new BasicAttributes( "uid", "bob", true );
        user.put( "userPassword", "bobspassword".getBytes() );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        user.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "person" );
        objectClass.add( "organizationalPerson" );
        objectClass.add( "inetOrgPerson" );
        user.put( "sn", "bob" );
        user.put( "cn", "bob" );
        adminCtx.createSubcontext( "uid=bob,ou=users", user );

        ServerLdapContext ctx = ( ServerLdapContext ) adminCtx.lookup( "" );
        assertTrue(ctx.compare(new LdapName( "uid=bob,ou=users,ou=system"), "userPassword", "bobspassword"));
    }
    
}
