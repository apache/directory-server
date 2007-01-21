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


import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;


/**
 * Tests whether or not authorization rules for entry deletion works properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DeleteAuthorizationITest extends AbstractAuthorizationITest
{
    /**
     * Checks if a simple entry (organizationalUnit) can be deleted from the DIT at an
     * RDN relative to ou=system by a specific non-admin user.  The entry is first
     * created using the admin account which can do anything without limitations.
     * After creating the entry as the admin an attempt is made to delete it as the
     * specified user.
     *
     * If a permission exception is encountered it is caught and false is returned,
     * otherwise true is returned when the entry is created.  The entry is deleted by the
     * admin user after a delete failure to make sure the entry is deleted if subsequent
     * calls are made to this method: the admin account is used to delete this test entry
     * so permissions to delete are not required to delete it by the specified user.
     *
     * @param uid the unique identifier for the user (presumed to exist under ou=users,ou=system)
     * @param password the password of this user
     * @param entryRdn the relative DN, relative to ou=system where entry creation then deletion is tested
     * @return true if the entry can be created by the user at the specified location, false otherwise
     * @throws javax.naming.NamingException if there are problems conducting the test
     */
    public boolean checkCanDeleteEntryAs( String uid, String password, String entryRdn ) throws NamingException
    {
        Attributes testEntry = new AttributesImpl( "ou", "testou", true );
        Attribute objectClass = new AttributeImpl( "objectClass" );
        testEntry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );

        DirContext adminContext = getContextAsAdmin();
        try
        {
            // create the entry as the admin
            LdapDN userName = new LdapDN( "uid=" + uid + ",ou=users,ou=system" );
            adminContext.createSubcontext( entryRdn, testEntry );

            // delete the newly created context as the user
            DirContext userContext = getContextAs( userName, password );
            userContext.destroySubcontext( entryRdn );

            return true;
        }
        catch ( LdapNoPermissionException e )
        {
            adminContext.destroySubcontext( entryRdn );
            return false;
        }
    }


    /**
     * Checks to make sure group membership based userClass works for delete operations.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantDeleteAdministrators() throws NamingException
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a delete operation which should fail without any ACI
        assertFalse( checkCanDeleteEntryAs( "billyd", "billyd", "ou=testou" ) );

        // Gives grantRemove perm to all users in the Administrators group for
        // entries and all attribute types and values
        createAccessControlSubentry( "administratorAdd", "{ " + "identificationTag \"addAci\", " + "precedence 14, "
            + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " + "userPermissions { { "
            + "protectedItems {entry}, " + "grantsAndDenials { grantRemove, grantBrowse } } } } }" );

        // see if we can now delete that test entry which we could not before
        // delete op should still fail since billd is not in the admin group
        assertFalse( checkCanDeleteEntryAs( "billyd", "billyd", "ou=testou" ) );

        // now add billyd to the Administrator group and try again
        addUserToGroup( "billyd", "Administrators" );

        // try a delete operation which should succeed with ACI and group membership change
        assertTrue( checkCanDeleteEntryAs( "billyd", "billyd", "ou=testou" ) );
    }


    /**
     * Checks to make sure name based userClass works for delete operations.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantDeleteByName() throws NamingException
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a delete operation which should fail without any ACI
        assertFalse( checkCanDeleteEntryAs( "billyd", "billyd", "ou=testou" ) );

        // now add a subentry that enables user billyd to delete an entry below ou=system
        createAccessControlSubentry( "billydAdd", "{ " + "identificationTag \"addAci\", " + "precedence 14, "
            + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { name { \"uid=billyd,ou=users,ou=system\" } }, " + "userPermissions { { "
            + "protectedItems {entry}, " + "grantsAndDenials { grantRemove, grantBrowse } } } } }" );

        // should work now that billyd is authorized by name
        assertTrue( checkCanDeleteEntryAs( "billyd", "billyd", "ou=testou" ) );
    }


    /**
     * Checks to make sure subtree based userClass works for delete operations.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantDeleteBySubtree() throws NamingException
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a delete operation which should fail without any ACI
        assertFalse( checkCanDeleteEntryAs( "billyd", "billyd", "ou=testou" ) );

        // now add a subentry that enables user billyd to delte an entry below ou=system
        createAccessControlSubentry( "billyAddBySubtree", "{ " + "identificationTag \"addAci\", " + "precedence 14, "
            + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { subtree { { base \"ou=users,ou=system\" } } }, " + "userPermissions { { "
            + "protectedItems {entry}, " + "grantsAndDenials { grantRemove, grantBrowse } } } } }" );

        // should work now that billyd is authorized by the subtree userClass
        assertTrue( checkCanDeleteEntryAs( "billyd", "billyd", "ou=testou" ) );
    }


    /**
     * Checks to make sure <b>allUsers</b> userClass works for delete operations.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantDeleteAllUsers() throws NamingException
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a delete operation which should fail without any ACI
        assertFalse( checkCanDeleteEntryAs( "billyd", "billyd", "ou=testou" ) );

        // now add a subentry that enables anyone to add an entry below ou=system
        createAccessControlSubentry( "anybodyAdd", "{ " + "identificationTag \"addAci\", " + "precedence 14, "
            + "authenticationLevel none, " + "itemOrUserFirst userFirst: { " + "userClasses { allUsers }, "
            + "userPermissions { { " + "protectedItems {entry}, "
            + "grantsAndDenials { grantRemove, grantBrowse } } } } }" );

        // see if we can now delete that test entry which we could not before
        // should work now with billyd now that all users are authorized
        assertTrue( checkCanDeleteEntryAs( "billyd", "billyd", "ou=testou" ) );
    }
}
