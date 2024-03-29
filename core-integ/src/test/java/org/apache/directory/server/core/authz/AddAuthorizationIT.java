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
import static org.apache.directory.server.core.authz.AutzIntegUtils.getConnectionAs;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Tests whether or not authorization around entry addition works properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( ApacheDSTestExtension.class )
@CreateDS(name = "AddAuthorizationIT")
public class AddAuthorizationIT extends AbstractLdapTestUnit
{

    @BeforeEach
    public void setService()
    {
        AutzIntegUtils.service = getService();
        getService().setAccessControlEnabled( true );
    }


    /**
     * Checks if a simple entry (organizationalUnit) can be added to the DIT at an
     * Rdn relative to ou=system by a specific non-admin user.  If a permission exception
     * is encountered it is caught and false is returned, otherwise true is returned
     * when the entry is created.  The entry is deleted after being created just in case
     * subsequent calls to this method do not fail: the admin account is used to delete
     * this test entry so permissions to delete are not required to delete it by the user.
     *
     * @param uid the unique identifier for the user (presumed to exist under ou=users,ou=system)
     * @param password the password of this user
     * @param entryRdn the relative Dn, relative to ou=system where entry creation is tested
     * @return true if the entry can be created by the user at the specified location, false otherwise
     * @throws Exception if there are problems conducting the test
     */
    public boolean checkCanAddEntryAs( String uid, String password, String entryRdn ) throws Exception
    {
        LdapConnection connection = null;

        Dn userName = new Dn( "uid=" + uid + ",ou=users,ou=system" );
        connection = getConnectionAs( userName, password );

        String entryDn = entryRdn + ",ou=system";
        Entry entry = new DefaultEntry( entryDn );
        entry.add( "ou", "testou" );
        entry.add( "ObjectClass", "top", "organizationalUnit" );

        try
        {
            connection.add( entry );
            return true;
        }
        catch ( LdapException le )
        {
            return false;
        }
        finally
        {
            if ( connection != null )
            {
                connection.close();
            }
        }
    }


    /**
     * Checks to make sure group membership based userClass works for add operations.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testGrantAddAdministrators() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an add operation which should fail without any ACI
        assertFalse( checkCanAddEntryAs( "billyd", "billyd", "ou=testou" ) );

        // Gives grantAdd perm to all users in the Administrators group for
        // entries and all attribute types and values
        createAccessControlSubentry( "administratorAdd",
            "{ " +
                "  identificationTag \"addAci\", " +
                "  precedence 14, " +
                "  authenticationLevel none, " +
                "  itemOrUserFirst userFirst: " +
                "  { " +
                "    userClasses { userGroup { \"cn=Administrators,ou=groups,ou=system\" } }, " +
                "    userPermissions " +
                "    { " +
                "      { " +
                "        protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "        grantsAndDenials { grantAdd, grantBrowse } " +
                "      } " +
                "    } " +
                "  } " +
                "}" );

        // see if we can now add that test entry which we could not before
        // add op should still fail since billd is not in the admin group
        assertFalse( checkCanAddEntryAs( "billyd", "billyd", "ou=testou" ) );

        // now add billyd to the Administrator group and try again
        addUserToGroup( "billyd", "Administrators" );

        // try an add operation which should succeed with ACI and group membership change
        assertTrue( checkCanAddEntryAs( "billyd", "billyd", "ou=testou1" ) );

        // Now, make sure the ACI is persisted if we stop and restart the server
        // Stop the server now, we will restart it immediately 
        // And shutdown the DS too
        getService().shutdown();
        assertFalse( getService().isStarted() );

        // And restart
        getService().startup();

        assertTrue( getService().isStarted() );

        // try an add operation which should succeed with ACI and group membership change
        assertTrue( checkCanAddEntryAs( "billyd", "billyd", "ou=testou2" ) );
    }


    /**
     * Checks to make sure name based userClass works for add operations.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testGrantAddByName() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an add operation which should fail without any ACI
        assertFalse( checkCanAddEntryAs( "billyd", "billyd", "ou=testou" ) );

        // now add a subentry that enables user billyd to add an entry below ou=system
        createAccessControlSubentry( "billydAdd",
            "{ " +
                "  identificationTag \"addAci\", " +
                "  precedence 14, " +
                "  authenticationLevel none, " +
                "  itemOrUserFirst userFirst: " +
                "  { " +
                "    userClasses { name { \"uid=billyd,ou=users,ou=system\" } }, " +
                "    userPermissions " +
                "    { " +
                "      { " +
                "        protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "        grantsAndDenials { grantAdd, grantBrowse } " +
                "      } " +
                "    } " +
                "  } " +
                "}" );

        // should work now that billyd is authorized by name
        assertTrue( checkCanAddEntryAs( "billyd", "billyd", "ou=testou" ) );
    }


    /**
     * Checks to make sure subtree based userClass works for add operations.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testGrantAddBySubtree() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an add operation which should fail without any ACI
        assertFalse( checkCanAddEntryAs( "billyd", "billyd", "ou=testou" ) );

        // now add a subentry that enables user billyd to add an entry below ou=system
        createAccessControlSubentry( "billyAddBySubtree",
            "{ " +
                "  identificationTag \"addAci\", " +
                "  precedence 14, " +
                "  authenticationLevel none, " +
                "  itemOrUserFirst userFirst: " +
                "  { " +
                "    userClasses " +
                "    { " +
                "      subtree { { base \"ou=users,ou=system\" } } " +
                "    }, " +
                "    userPermissions " +
                "    { " +
                "      { " +
                "        protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "        grantsAndDenials { grantAdd, grantBrowse } " +
                "      } " +
                "    } " +
                "  } " +
                "}" );

        // should work now that billyd is authorized by the subtree userClass
        assertTrue( checkCanAddEntryAs( "billyd", "billyd", "ou=testou" ) );
    }


    /**
     * Checks to make sure <b>allUsers</b> userClass works for add operations.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testGrantAddAllUsers() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try an add operation which should fail without any ACI
        assertFalse( checkCanAddEntryAs( "billyd", "billyd", "ou=testou" ) );

        // now add a subentry that enables anyone to add an entry below ou=system
        createAccessControlSubentry( "anybodyAdd",
            "{ " +
                "  identificationTag \"addAci\", " +
                "  precedence 14, " +
                "  authenticationLevel none, " +
                "  itemOrUserFirst userFirst: " +
                "  { " +
                "    userClasses { allUsers }, " +
                "    userPermissions " +
                "    { " +
                "      { " +
                "        protectedItems {entry, allUserAttributeTypesAndValues}, " +
                "        grantsAndDenials { grantAdd, grantBrowse } " +
                "      } " +
                "    } " +
                "  } " +
                "}" );

        // see if we can now add that test entry which we could not before
        // should work now with billyd now that all users are authorized
        assertTrue( checkCanAddEntryAs( "billyd", "billyd", "ou=testou" ) );
    }
}
