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
package org.apache.directory.server.core.authz;


import org.apache.directory.shared.ldap.exception.LdapNoPermissionException;
import org.apache.directory.shared.ldap.name.LdapName;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.Name;
import javax.naming.directory.*;
import java.util.List;
import java.util.ArrayList;


/**
 * Tests whether or not authorization around entry modify operations work properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ModifyAuthorizationTest extends AbstractAuthorizationTest
{
    /**
     * Checks if an attribute of a simple entry (an organizationalUnit) with an RDN
     * relative to ou=system can be modified by a specific non-admin user.  If a
     * permission exception is encountered it is caught and false is returned,
     * otherwise true is returned.  The entry is deleted after being created just in case
     * subsequent calls to this method are made in the same test case: the admin account
     * is used to add and delete this test entry so permissions to add and delete are not
     * required to test the modify operation by the user.
     *
     * @param uid the unique identifier for the user (presumed to exist under ou=users,ou=system)
     * @param password the password of this user
     * @param entryRdn the relative DN, relative to ou=system where entry is created
     * for modification test
     * @param mods the modifications to make to the entry
     * @return true if the modifications can be made by the user at the specified location,
     * false otherwise.
     * @throws javax.naming.NamingException if there are problems conducting the test
     */
    public boolean checkCanModifyAs( String uid, String password, String entryRdn, ModificationItem[] mods )
            throws NamingException
    {
        // create the entry with the telephoneNumber attribute to modify
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

            // modify the entry as the user
            DirContext userContext = getContextAs( userName, password );
            userContext.modifyAttributes( entryRdn, mods );

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
     * Checks if an attribute of a simple entry (an organizationalUnit) with an RDN
     * relative to ou=system can be modified by a specific non-admin user.  If a
     * permission exception is encountered it is caught and false is returned,
     * otherwise true is returned.  The entry is deleted after being created just in case
     * subsequent calls to this method are made in the same test case: the admin account
     * is used to add and delete this test entry so permissions to add and delete are not
     * required to test the modify operation by the user.
     *
     * @param uid the unique identifier for the user (presumed to exist under ou=users,ou=system)
     * @param password the password of this user
     * @param entryRdn the relative DN, relative to ou=system where entry is created
     * for modification test
     * @param mods the attributes to modify in the entry
     * @param modOp the modification operation to use for all attributes
     * @return true if the modifications can be made by the user at the specified location,
     * false otherwise.
     * @throws javax.naming.NamingException if there are problems conducting the test
     */
    public boolean checkCanModifyAs( String uid, String password, String entryRdn, int modOp, Attributes mods )
            throws NamingException
    {
        // create the entry with the telephoneNumber attribute to modify
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

            // modify the entry as the user
            DirContext userContext = getContextAs( userName, password );
            userContext.modifyAttributes( entryRdn, modOp, mods );

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
     * Checks if a user can modify an attribute of their own entry.  Users are
     * presumed to reside under ou=users,ou=system.  If a permission exception is
     * encountered it is caught and false is returned, otherwise true is returned.
     *
     * @param uid the unique identifier for the user (presumed to exist under ou=users,ou=system)
     * @param password the password of this user
     * @param mods the attributes to modify in the entry
     * @param modOp the modification operation to use for all attributes
     * @return true if the modifications can be made by the user his/her own entry,
     * false otherwise.
     * @throws javax.naming.NamingException if there are problems conducting the test
     */
    public boolean checkCanSelfModify( String uid, String password, int modOp, Attributes mods )
            throws NamingException
    {
        try
        {
            // modify the entry as the user
            Name userEntry = new LdapName( "uid="+uid+",ou=users,ou=system" );
            DirContext userContext = getContextAs( userEntry, password, userEntry.toString() );
            userContext.modifyAttributes( "", modOp, mods );
            return true;
        }
        catch ( LdapNoPermissionException e )
        {
            return false;
        }
    }


    /**
     * Checks if a user can modify an attribute of their own entry.  Users are
     * presumed to reside under ou=users,ou=system.  If a permission exception is
     * encountered it is caught and false is returned, otherwise true is returned.
     *
     * @param uid the unique identifier for the user (presumed to exist under ou=users,ou=system)
     * @param password the password of this user
     * @param mods the attributes to modify in the entry
     * @return true if the modifications can be made by the user his/her own entry,
     * false otherwise.
     * @throws javax.naming.NamingException if there are problems conducting the test
     */
    public boolean checkCanSelfModify( String uid, String password, ModificationItem[] mods )
            throws NamingException
    {
        try
        {
            // modify the entry as the user
            Name userEntry = new LdapName( "uid="+uid+",ou=users,ou=system" );
            DirContext userContext = getContextAs( userEntry, password, userEntry.toString() );
            userContext.modifyAttributes( "", mods );
            return true;
        }
        catch ( LdapNoPermissionException e )
        {
            return false;
        }
    }


    /**
     * Converts a set of attributes and a modification operation type into a MoficationItem array.
     *
     * @param modOp the modification operation to perform
     * @param changes the modifications to the attribute
     * @return the array of modification items represting the changes
     * @throws NamingException if there are problems accessing attributes
     */
    private ModificationItem[] toItems( int modOp, Attributes changes ) throws NamingException
    {
        List mods = new ArrayList();
        NamingEnumeration list = changes.getAll();
        while ( list.hasMore() )
        {
            Attribute attr = ( Attribute ) list.next();
            mods.add( new ModificationItem( modOp, attr ) );
        }
        ModificationItem[] modArray = new ModificationItem[mods.size()];
        return ( ModificationItem[] ) mods.toArray( modArray );
    }


    public void testSelfModification() throws NamingException
    {
        // ----------------------------------------------------------------------------------
        // Modify with Attribute Addition
        // ----------------------------------------------------------------------------------

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // create the password modification
        ModificationItem[] mods = toItems( DirContext.REPLACE_ATTRIBUTE,
                new BasicAttributes( "userPassword", "williams", true ) );

        // try a modify operation which should fail without any ACI
        assertFalse( checkCanSelfModify( "billyd", "billyd", mods ) );

        // Gives grantModify, and grantRead perm to all users in the Administrators group for
        // entries and all attribute types and values
        createAccessControlSubentry( "selfModifyUserPassword",
                "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { thisEntry }, " +
                "userPermissions { " +
                        "{ protectedItems {entry}, grantsAndDenials { grantModify, grantBrowse, grantRead } }, " +
                        "{ protectedItems {allAttributeValues {userPassword}}, grantsAndDenials { grantAdd, grantRemove } } " +
                        "} } }" );

        // try a modify operation which should succeed with ACI
        assertTrue( checkCanSelfModify( "billyd", "billyd", mods ) );
        deleteAccessControlSubentry( "selfModifyUserPassword" );
    }


    /**
     * Checks to make sure group membership based userClass works for modify operations.
     *
     * @throws javax.naming.NamingException if the test encounters an error
     */
    public void testGrantModifyByAdministrators() throws NamingException
    {
        // ----------------------------------------------------------------------------------
        // Modify with Attribute Addition
        // ----------------------------------------------------------------------------------

        // create the add modifications
        ModificationItem[] mods = toItems( DirContext.ADD_ATTRIBUTE,
                new BasicAttributes( "registeredAddress", "100 Park Ave.", true ) );

        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a modify operation which should fail without any ACI
        assertFalse( checkCanModifyAs( "billyd", "billyd", "ou=testou", mods ) );

        // Gives grantModify, and grantRead perm to all users in the Administrators group for
        // entries and all attribute types and values
        createAccessControlSubentry( "administratorModifyAdd",
                "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " +
                "userPermissions { " +
                        "{ protectedItems {entry}, grantsAndDenials { grantModify, grantBrowse } }, " +
                        "{ protectedItems {allAttributeValues {registeredAddress}}, grantsAndDenials { grantAdd } } " +
                        "} } }" );

        // see if we can now add that test entry which we could not before
        // add op should still fail since billd is not in the admin group
        assertFalse( checkCanModifyAs( "billyd", "billyd", "ou=testou", mods ) );

        // now add billyd to the Administrator group and try again
        addUserToGroup( "billyd", "Administrators" );

        // try a modify operation which should succeed with ACI and group membership change
        assertTrue( checkCanModifyAs( "billyd", "billyd", "ou=testou", mods ) );
        deleteAccessControlSubentry( "administratorModifyAdd" );

        // ----------------------------------------------------------------------------------
        // Modify with Attribute Removal
        // ----------------------------------------------------------------------------------

        // now let's test to see if we can perform a modify with a delete op
        mods = toItems( DirContext.REMOVE_ATTRIBUTE,
                new BasicAttributes( "telephoneNumber", "867-5309", true ) );

        // make sure we cannot remove the telephone number from the test entry
        assertFalse( checkCanModifyAs( "billyd", "billyd", "ou=testou", mods ) );

        // Gives grantModify, and grantRead perm to all users in the Administrators group for
        // entries and all attribute types and values
        createAccessControlSubentry( "administratorModifyRemove", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " +
                "userPermissions { " +
                        "{ protectedItems {entry}, grantsAndDenials { grantModify, grantBrowse } }, " +
                        "{ protectedItems {allAttributeValues {telephoneNumber}}, grantsAndDenials { grantRemove } } " +
                        "} } }" );

        // try a modify operation which should succeed with ACI and group membership change
        assertTrue( checkCanModifyAs( "billyd", "billyd", "ou=testou", mods ) );
        deleteAccessControlSubentry( "administratorModifyRemove" );

        // ----------------------------------------------------------------------------------
        // Modify with Attribute Replace (requires both grantRemove and grantAdd on attrs)
        // ----------------------------------------------------------------------------------

        // now let's test to see if we can perform a modify with a delete op
        mods = toItems( DirContext.REPLACE_ATTRIBUTE,
                new BasicAttributes( "telephoneNumber", "867-5309", true ) );

        // make sure we cannot remove the telephone number from the test entry
        assertFalse( checkCanModifyAs( "billyd", "billyd", "ou=testou", mods ) );

        // Gives grantModify, and grantRead perm to all users in the Administrators group for
        // entries and all attribute types and values
        createAccessControlSubentry( "administratorModifyReplace", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " +
                "userPermissions { " +
                        "{ protectedItems {entry}, grantsAndDenials { grantModify, grantBrowse } }, " +
                        "{ protectedItems {allAttributeValues {telephoneNumber}}, grantsAndDenials { grantAdd, grantRemove } } " +
                        "} } }" );

        // try a modify operation which should succeed with ACI and group membership change
        assertTrue( checkCanModifyAs( "billyd", "billyd", "ou=testou", mods ) );
        deleteAccessControlSubentry( "administratorModifyReplace" );

        /* =================================================================================
         *              DO IT ALL OVER AGAIN BUT USE THE OTHER MODIFY METHOD
         * ================================================================================= */

        // ----------------------------------------------------------------------------------
        // Modify with Attribute Addition
        // ----------------------------------------------------------------------------------

        // create the add modifications
        Attributes changes = new BasicAttributes( "registeredAddress", "100 Park Ave.", true );

        // try a modify operation which should fail without any ACI
        assertFalse( checkCanModifyAs( "billyd", "billyd", "ou=testou", DirContext.ADD_ATTRIBUTE, changes ) );

        // Gives grantModify, and grantRead perm to all users in the Administrators group for
        // entries and all attribute types and values
        createAccessControlSubentry( "administratorModifyAdd", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " +
                "userPermissions { " +
                        "{ protectedItems {entry}, grantsAndDenials { grantModify, grantBrowse } }, " +
                        "{ protectedItems {allAttributeValues {registeredAddress}}, grantsAndDenials { grantAdd } } " +
                        "} } }" );

        // try a modify operation which should succeed with ACI and group membership change
        assertTrue( checkCanModifyAs( "billyd", "billyd", "ou=testou", DirContext.ADD_ATTRIBUTE, changes ) );
        deleteAccessControlSubentry( "administratorModifyAdd" );

        // ----------------------------------------------------------------------------------
        // Modify with Attribute Removal
        // ----------------------------------------------------------------------------------

        // now let's test to see if we can perform a modify with a delete op
        changes = new BasicAttributes( "telephoneNumber", "867-5309", true );

        // make sure we cannot remove the telephone number from the test entry
        assertFalse( checkCanModifyAs( "billyd", "billyd", "ou=testou", DirContext.REMOVE_ATTRIBUTE, changes ) );

        // Gives grantModify, and grantRead perm to all users in the Administrators group for
        // entries and all attribute types and values
        createAccessControlSubentry( "administratorModifyRemove", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " +
                "userPermissions { " +
                        "{ protectedItems {entry}, grantsAndDenials { grantModify, grantBrowse } }, " +
                        "{ protectedItems {allAttributeValues {telephoneNumber}}, grantsAndDenials { grantRemove } } " +
                        "} } }" );

        // try a modify operation which should succeed with ACI and group membership change
        assertTrue( checkCanModifyAs( "billyd", "billyd", "ou=testou", DirContext.REMOVE_ATTRIBUTE, changes ) );
        deleteAccessControlSubentry( "administratorModifyRemove" );

        // ----------------------------------------------------------------------------------
        // Modify with Attribute Replace (requires both grantRemove and grantAdd on attrs)
        // ----------------------------------------------------------------------------------

        // now let's test to see if we can perform a modify with a delete op
        changes = new BasicAttributes( "telephoneNumber", "867-5309", true );

        // make sure we cannot remove the telephone number from the test entry
        assertFalse( checkCanModifyAs( "billyd", "billyd", "ou=testou", DirContext.REPLACE_ATTRIBUTE, changes ) );

        // Gives grantModify, and grantRead perm to all users in the Administrators group for
        // entries and all attribute types and values
        createAccessControlSubentry( "administratorModifyReplace", "{ " +
                "identificationTag \"addAci\", " +
                "precedence 14, " +
                "authenticationLevel none, " +
                "itemOrUserFirst userFirst: { " +
                "userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " +
                "userPermissions { " +
                        "{ protectedItems {entry}, grantsAndDenials { grantModify, grantBrowse } }, " +
                        "{ protectedItems {allAttributeValues {telephoneNumber}}, grantsAndDenials { grantAdd, grantRemove } } " +
                        "} } }" );

        // try a modify operation which should succeed with ACI and group membership change
        assertTrue( checkCanModifyAs( "billyd", "billyd", "ou=testou", DirContext.REPLACE_ATTRIBUTE, changes ) );
        deleteAccessControlSubentry( "administratorModifyReplace" );
    }


//    /**
//     * Checks to make sure name based userClass works for modify operations.
//     *
//     * @throws javax.naming.NamingException if the test encounters an error
//     */
//    public void testGrantModifyByName() throws NamingException
//    {
//        // create the non-admin user
//        createUser( "billyd", "billyd" );
//
//        // try an modify operation which should fail without any ACI
//        assertFalse( checkCanModifyAs( "billyd", "billyd", "ou=testou", "867-5309" ) );
//
//        // now add a subentry that enables user billyd to modify an entry below ou=system
//        createAccessControlSubentry( "billydAdd", "{ " +
//                "identificationTag \"addAci\", " +
//                "precedence 14, " +
//                "authenticationLevel none, " +
//                "itemOrUserFirst userFirst: { " +
//                "userClasses { name { \"uid=billyd,ou=users,ou=system\" } }, " +
//                "userPermissions { { " +
//                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
//                "grantsAndDenials { grantModify, grantRead, grantBrowse } } } } }" );
//
//        // should work now that billyd is authorized by name
//        assertTrue( checkCanModifyAs( "billyd", "billyd", "ou=testou", "867-5309" ) );
//    }
//
//
//    /**
//     * Checks to make sure subtree based userClass works for modify operations.
//     *
//     * @throws javax.naming.NamingException if the test encounters an error
//     */
//    public void testGrantModifyBySubtree() throws NamingException
//    {
//        // create the non-admin user
//        createUser( "billyd", "billyd" );
//
//        // try a modify operation which should fail without any ACI
//        assertFalse( checkCanModifyAs( "billyd", "billyd", "ou=testou", "867-5309" ) );
//
//        // now add a subentry that enables user billyd to modify an entry below ou=system
//        createAccessControlSubentry( "billyAddBySubtree", "{ " +
//                "identificationTag \"addAci\", " +
//                "precedence 14, " +
//                "authenticationLevel none, " +
//                "itemOrUserFirst userFirst: { " +
//                "userClasses { subtree { { base \"ou=users,ou=system\" } } }, " +
//                "userPermissions { { " +
//                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
//                "grantsAndDenials { grantModify, grantRead, grantBrowse } } } } }" );
//
//        // should work now that billyd is authorized by the subtree userClass
//        assertTrue( checkCanModifyAs( "billyd", "billyd", "ou=testou", "867-5309" ) );
//    }
//
//
//    /**
//     * Checks to make sure <b>allUsers</b> userClass works for modify operations.
//     *
//     * @throws javax.naming.NamingException if the test encounters an error
//     */
//    public void testGrantModifyAllUsers() throws NamingException
//    {
//        // create the non-admin user
//        createUser( "billyd", "billyd" );
//
//        // try an add operation which should fail without any ACI
//        assertFalse( checkCanModifyAs( "billyd", "billyd", "ou=testou", "867-5309" ) );
//
//        // now add a subentry that enables anyone to add an entry below ou=system
//        createAccessControlSubentry( "anybodyAdd", "{ " +
//                "identificationTag \"addAci\", " +
//                "precedence 14, " +
//                "authenticationLevel none, " +
//                "itemOrUserFirst userFirst: { " +
//                "userClasses { allUsers }, " +
//                "userPermissions { { " +
//                "protectedItems {entry, allUserAttributeTypesAndValues}, " +
//                "grantsAndDenials { grantModify, grantRead, grantBrowse } } } } }" );
//
//        // see if we can now modify that test entry's number which we could not before
//        // should work with billyd now that all users are authorized
//        assertTrue( checkCanModifyAs( "billyd", "billyd", "ou=testou", "867-5309" ) );
//    }
}
