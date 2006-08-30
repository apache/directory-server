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
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.NamingException;
import javax.naming.directory.*;


/**
 * Tests whether or not authorization around entry renames and moves work properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MoveRenameAuthorizationITest extends AbstractAuthorizationITest
{
    /**
     * Checks if a simple entry (organizationalUnit) can be renamed at an RDN relative
     * to ou=system by a specific non-admin user.  If a permission exception
     * is encountered it is caught and false is returned, otherwise true is returned
     * when the entry is created.  The entry is deleted after being created just in case
     * subsequent calls to this method do not fail: the admin account is used to delete
     * this test entry so permissions to delete are not required to delete it by the user.
     *
     * @param uid the unique identifier for the user (presumed to exist under ou=users,ou=system)
     * @param password the password of this user
     * @param entryRdn the relative DN, relative to ou=system where entry renames are tested
     * @param newRdn the new RDN for the entry under ou=system
     * @return true if the entry can be renamed by the user at the specified location, false otherwise
     * @throws javax.naming.NamingException if there are problems conducting the test
     */
    public boolean checkCanRenameAs( String uid, String password, String entryRdn, String newRdn )
        throws NamingException
    {
        Attributes testEntry = new BasicAttributes( "ou", "testou", true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        testEntry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );

        DirContext adminContext = getContextAsAdmin();
        try
        {
            // create the new entry as the admin user
            adminContext.createSubcontext( entryRdn, testEntry );

            LdapDN userName = new LdapDN( "uid=" + uid + ",ou=users,ou=system" ); 
            DirContext userContext = getContextAs( userName, password );
            userContext.rename( entryRdn, newRdn );

            // delete the renamed context as the admin user
            adminContext.destroySubcontext( newRdn );
            return true;
        }
        catch ( LdapNoPermissionException e )
        {
            // delete the original context as the admin user since rename
            // of newly created test entry did not succeed
            adminContext.destroySubcontext( entryRdn );
            return false;
        }
    }


    /**
     * Checks to make sure group membership based userClass works for renames,
     * moves and moves with renames.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantByAdministrators() throws NamingException
    {
        // ----------------------------------------------------------------------------
        // Test simple RDN change: NO SUBTREE MOVEMENT!
        // ----------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try the rename operation which should fail without any ACI
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou", "ou=newname" ) );

        // Gives grantRename perm to all users in the Administrators group for entries
        createAccessControlSubentry( "grantRenameByAdmin", "{ " + "identificationTag \"addAci\", " + "precedence 14, "
            + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " + "userPermissions { { "
            + "protectedItems {entry}, " + "grantsAndDenials { grantRename, grantBrowse } } } } }" );

        // see if we can now rename that test entry which we could not before
        // rename op should still fail since billyd is not in the admin group
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou", "ou=newname" ) );

        // now add billyd to the Administrator group and try again
        addUserToGroup( "billyd", "Administrators" );

        // try a rename operation which should succeed with ACI and group membership change
        assertTrue( checkCanRenameAs( "billyd", "billyd", "ou=testou", "ou=newname" ) );

        // now let's cleanup
        removeUserFromGroup( "billyd", "Administrators" );
        deleteAccessControlSubentry( "grantRenameByAdmin" );
        deleteUser( "billyd" );

        // ----------------------------------------------------------------------------
        // Test move and RDN change at the same time.
        // ----------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an move w/ rdn change which should fail without any ACI
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=newname,ou=groups" ) );

        // Gives grantRename, grantImport, grantExport perm to all users in the Administrators
        // group for entries - browse is needed just to read navigate the tree at root
        createAccessControlSubentry( "grantRenameMoveByAdmin", "{ " + "identificationTag \"addAci\", "
            + "precedence 14, " + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " + "userPermissions { { "
            + "protectedItems {entry}, "
            + "grantsAndDenials { grantExport, grantImport, grantRename, grantBrowse } } } } }" );

        // see if we can move and rename the test entry which we could not before
        // op should still fail since billyd is not in the admin group
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=newname,ou=groups" ) );

        // now add billyd to the Administrator group and try again
        addUserToGroup( "billyd", "Administrators" );

        // try move w/ rdn change which should succeed with ACI and group membership change
        assertTrue( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=newname,ou=groups" ) );

        // now let's cleanup
        removeUserFromGroup( "billyd", "Administrators" );
        deleteAccessControlSubentry( "grantRenameMoveByAdmin" );
        deleteUser( "billyd" );

        // ----------------------------------------------------------------------------
        // Test move ONLY without any RDN changes.
        // ----------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try move operation which should fail without any ACI
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=testou,ou=groups" ) );

        // Gives grantImport, and grantExport perm to all users in the Administrators group for entries
        createAccessControlSubentry( "grantMoveByAdmin", "{ " + "identificationTag \"addAci\", " + "precedence 14, "
            + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " + "userPermissions { { "
            + "protectedItems {entry}, " + "grantsAndDenials { grantExport, grantImport, grantBrowse } } } } }" );

        // see if we can now move that test entry which we could not before
        // op should still fail since billyd is not in the admin group
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=testou,ou=groups" ) );

        // now add billyd to the Administrator group and try again
        addUserToGroup( "billyd", "Administrators" );

        // try move operation which should succeed with ACI and group membership change
        assertTrue( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=testou,ou=groups" ) );

        // now let's cleanup
        removeUserFromGroup( "billyd", "Administrators" );
        deleteAccessControlSubentry( "grantMoveByAdmin" );
        deleteUser( "billyd" );
    }


    /**
     * Checks to make sure name based userClass works for rename, move, and
     * rename with move operation access controls.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantByName() throws NamingException
    {
        // ----------------------------------------------------------------------------
        // Test simple RDN change: NO SUBTREE MOVEMENT!
        // ----------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try the rename operation which should fail without any ACI
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou", "ou=newname" ) );

        // Gives grantRename perm specifically to the billyd user
        createAccessControlSubentry( "grantRenameByName", "{ " + "identificationTag \"addAci\", " + "precedence 14, "
            + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { name { \"uid=billyd,ou=users,ou=system\" } }, " + "userPermissions { { "
            + "protectedItems {entry}, " + "grantsAndDenials { grantRename, grantBrowse } } } } }" );

        // try a rename operation which should succeed with ACI
        assertTrue( checkCanRenameAs( "billyd", "billyd", "ou=testou", "ou=newname" ) );

        // now let's cleanup
        deleteAccessControlSubentry( "grantRenameByName" );
        deleteUser( "billyd" );

        // ----------------------------------------------------------------------------
        // Test move and RDN change at the same time.
        // ----------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an move w/ rdn change which should fail without any ACI
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=newname,ou=groups" ) );

        // Gives grantRename, grantImport, grantExport perm to billyd user on entries
        createAccessControlSubentry( "grantRenameMoveByName", "{ " + "identificationTag \"addAci\", "
            + "precedence 14, " + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { name { \"uid=billyd,ou=users,ou=system\" } }, " + "userPermissions { { "
            + "protectedItems {entry}, "
            + "grantsAndDenials { grantExport, grantImport, grantRename, grantBrowse } } } } }" );

        // try move w/ rdn change which should succeed with ACI
        assertTrue( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=newname,ou=groups" ) );

        // now let's cleanup
        deleteAccessControlSubentry( "grantRenameMoveByName" );
        deleteUser( "billyd" );

        // ----------------------------------------------------------------------------
        // Test move ONLY without any RDN changes.
        // ----------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try move operation which should fail without any ACI
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=testou,ou=groups" ) );

        // Gives grantImport, and grantExport perm to billyd user for entries
        createAccessControlSubentry( "grantMoveByName", "{ " + "identificationTag \"addAci\", " + "precedence 14, "
            + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { name { \"uid=billyd,ou=users,ou=system\" } }, " + "userPermissions { { "
            + "protectedItems {entry}, " + "grantsAndDenials { grantExport, grantImport, grantBrowse } } } } }" );

        // try move operation which should succeed with ACI
        assertTrue( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=testou,ou=groups" ) );

        // now let's cleanup
        deleteAccessControlSubentry( "grantMoveByName" );
        deleteUser( "billyd" );
    }


    /**
     * Checks to make sure subtree based userClass works for rename, move, and
     * rename with move operation access controls.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantBySubtree() throws NamingException
    {
        // ----------------------------------------------------------------------------
        // Test simple RDN change: NO SUBTREE MOVEMENT!
        // ----------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try the rename operation which should fail without any ACI
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou", "ou=newname" ) );

        // Gives grantRename perm for entries to those users selected by the subtree
        createAccessControlSubentry( "grantRenameByTree", "{ " + "identificationTag \"addAci\", " + "precedence 14, "
            + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { subtree { { base \"ou=users,ou=system\" } } }, " + "userPermissions { { "
            + "protectedItems {entry}, " + "grantsAndDenials { grantRename, grantBrowse } } } } }" );

        // try a rename operation which should succeed with ACI
        assertTrue( checkCanRenameAs( "billyd", "billyd", "ou=testou", "ou=newname" ) );

        // now let's cleanup
        deleteAccessControlSubentry( "grantRenameByTree" );
        deleteUser( "billyd" );

        // ----------------------------------------------------------------------------
        // Test move and RDN change at the same time.
        // ----------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an move w/ rdn change which should fail without any ACI
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=newname,ou=groups" ) );

        // Gives grantRename, grantImport, grantExport for entries to users selected by subtree
        createAccessControlSubentry( "grantRenameMoveByTree", "{ " + "identificationTag \"addAci\", "
            + "precedence 14, " + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { subtree { { base \"ou=users,ou=system\" } } }, " + "userPermissions { { "
            + "protectedItems {entry}, "
            + "grantsAndDenials { grantExport, grantImport, grantRename, grantBrowse } } } } }" );

        // try move w/ rdn change which should succeed with ACI
        assertTrue( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=newname,ou=groups" ) );

        // now let's cleanup
        deleteAccessControlSubentry( "grantRenameMoveByTree" );
        deleteUser( "billyd" );

        // ----------------------------------------------------------------------------
        // Test move ONLY without any RDN changes.
        // ----------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try move operation which should fail without any ACI
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=testou,ou=groups" ) );

        // Gives grantImport, and grantExport perm for entries to subtree selected users
        createAccessControlSubentry( "grantMoveByTree", "{ " + "identificationTag \"addAci\", " + "precedence 14, "
            + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { subtree { { base \"ou=users,ou=system\" } } }, " + "userPermissions { { "
            + "protectedItems {entry}, " + "grantsAndDenials { grantExport, grantImport, grantBrowse } } } } }" );

        // try move operation which should succeed with ACI
        assertTrue( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=testou,ou=groups" ) );

        // now let's cleanup
        deleteAccessControlSubentry( "grantMoveByTree" );
        deleteUser( "billyd" );
    }


    /**
     * Checks to make sure the <b>anyUser</b> userClass works for rename, move, and
     * rename with move operation access controls.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantByAnyuser() throws NamingException
    {
        // ----------------------------------------------------------------------------
        // Test simple RDN change: NO SUBTREE MOVEMENT!
        // ----------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try the rename operation which should fail without any ACI
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou", "ou=newname" ) );

        // Gives grantRename perm for entries to any user
        createAccessControlSubentry( "grantRenameByAny", "{ " + "identificationTag \"addAci\", " + "precedence 14, "
            + "authenticationLevel none, " + "itemOrUserFirst userFirst: { " + "userClasses { allUsers }, "
            + "userPermissions { { " + "protectedItems {entry}, "
            + "grantsAndDenials { grantRename, grantBrowse } } } } }" );

        // try a rename operation which should succeed with ACI
        assertTrue( checkCanRenameAs( "billyd", "billyd", "ou=testou", "ou=newname" ) );

        // now let's cleanup
        deleteAccessControlSubentry( "grantRenameByAny" );
        deleteUser( "billyd" );

        // ----------------------------------------------------------------------------
        // Test move and RDN change at the same time.
        // ----------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an move w/ rdn change which should fail without any ACI
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=newname,ou=groups" ) );

        // Gives grantRename, grantImport, grantExport for entries to any user
        createAccessControlSubentry( "grantRenameMoveByAny", "{ " + "identificationTag \"addAci\", "
            + "precedence 14, " + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { allUsers }, " + "userPermissions { { " + "protectedItems {entry}, "
            + "grantsAndDenials { grantExport, grantImport, grantRename, grantBrowse } } } } }" );

        // try move w/ rdn change which should succeed with ACI
        assertTrue( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=newname,ou=groups" ) );

        // now let's cleanup
        deleteAccessControlSubentry( "grantRenameMoveByAny" );
        deleteUser( "billyd" );

        // ----------------------------------------------------------------------------
        // Test move ONLY without any RDN changes.
        // ----------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try move operation which should fail without any ACI
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=testou,ou=groups" ) );

        // Gives grantImport, and grantExport perm for entries to any user
        createAccessControlSubentry( "grantMoveByAny", "{ " + "identificationTag \"addAci\", " + "precedence 14, "
            + "authenticationLevel none, " + "itemOrUserFirst userFirst: { " + "userClasses { allUsers }, "
            + "userPermissions { { " + "protectedItems {entry}, "
            + "grantsAndDenials { grantExport, grantImport, grantBrowse } } } } }" );

        // try move operation which should succeed with ACI
        assertTrue( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=testou,ou=groups" ) );

        // now let's cleanup
        deleteAccessControlSubentry( "grantMoveByAny" );
        deleteUser( "billyd" );
    }
    
    
    /**
     * Checks to make sure Export and Import permissions work correctly
     * when they are defined on seperate contexts.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testExportAndImportSeperately() throws NamingException
    {
        // ----------------------------------------------------------------------------
        // Test move and RDN change at the same time.
        // ----------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an move w/ rdn change which should fail without any ACI
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=newname,ou=groups" ) );

        
        // Gives grantBrowse perm to all users in the Administrators
        // group for entries
        // It's is needed just to read navigate the tree at root
        createAccessControlSubentry(
            "grantBrowseForTheWholeNamingContext",
            "{ }",
            "{ " + "identificationTag \"browseACI\", "
            + "precedence 14, " + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " + "userPermissions { { "
            + "protectedItems { entry }, "
            + "grantsAndDenials { grantBrowse } } } } }" );
        
        // Gives grantExport, grantRename perm to all users in the Administrators
        // group for entries
        createAccessControlSubentry(
            "grantExportFromASubtree",
            "{ base \"ou=users\" }", // !!!!! =====>>>>> { base "ou=users" }
            "{ " + "identificationTag \"exportACI\", "
            + "precedence 14, " + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " + "userPermissions { { "
            + "protectedItems { entry }, "
            + "grantsAndDenials { grantExport, grantRename } } } } }" );
        
        // Gives grantImport perm to all users in the Administrators
        // group for the target context
        createAccessControlSubentry(
            "grantImportToASubtree",
            "{ base \"ou=groups\" }", // !!!!! =====>>>>> { base "ou=groups" }
            "{ " + "identificationTag \"importACI\", "
            + "precedence 14, " + "authenticationLevel none, " + "itemOrUserFirst userFirst: { "
            + "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " + "userPermissions { { "
            + "protectedItems { entry }, "
            + "grantsAndDenials { grantImport } } } } }" );

        // see if we can move and rename the test entry which we could not before
        // op should still fail since billyd is not in the admin group
        assertFalse( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=newname,ou=groups" ) );

        // now add billyd to the Administrator group and try again
        addUserToGroup( "billyd", "Administrators" );

        // try move w/ rdn change which should succeed with ACI and group membership change
        assertTrue( checkCanRenameAs( "billyd", "billyd", "ou=testou,ou=users", "ou=newname,ou=groups" ) );

        // now let's cleanup
        removeUserFromGroup( "billyd", "Administrators" );
        deleteAccessControlSubentry( "grantBrowseForTheWholeNamingContext" );
        deleteAccessControlSubentry( "grantExportFromASubtree" );
        deleteAccessControlSubentry( "grantImportToASubtree" );
        deleteUser( "billyd" );
    }
}
