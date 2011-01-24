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


import static org.apache.directory.server.core.authz.AutzIntegUtils.addEntryACI;
import static org.apache.directory.server.core.authz.AutzIntegUtils.addPrescriptiveACI;
import static org.apache.directory.server.core.authz.AutzIntegUtils.addSubentryACI;
import static org.apache.directory.server.core.authz.AutzIntegUtils.addUserToGroup;
import static org.apache.directory.server.core.authz.AutzIntegUtils.createAccessControlSubentry;
import static org.apache.directory.server.core.authz.AutzIntegUtils.createUser;
import static org.apache.directory.server.core.authz.AutzIntegUtils.deleteAccessControlSubentry;
import static org.apache.directory.server.core.authz.AutzIntegUtils.getAdminConnection;
import static org.apache.directory.server.core.authz.AutzIntegUtils.getConnectionAs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.filter.SearchScope;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.name.Dn;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests whether or not authorization around search, list and lookup operations
 * work properly.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(enableAccessControl = true, name = "SearchAuthorizationIT")
public class SearchAuthorizationIT extends AbstractLdapTestUnit
{

    // to avoid creating too many connections during recursive operations
    private LdapConnection reusableAdminCon;


    @Before
    public void setService() throws Exception
    {
        AutzIntegUtils.service = service;
        reusableAdminCon = getAdminConnection();
    }


    @After
    public void closeConnections()
    {
        IntegrationUtils.closeConnections();
    }

    /**
     * The search results of tests are added to this map via put (<String, Entry>)
     * the map is also cleared before each search test.  This allows further inspections
     * of the results for more specific test cases.
     */
    private Map<String, Entry> results = new HashMap<String, Entry>();


    /**
     * Generates a set of simple organizationalUnit entries where the
     * ou of the entry returned is the index of the entry in the array.
     *
     * @param count the number of entries to produce
     * @return an array of entries with length = count
     */
    private Entry[] getTestNodes( final int count )
    {
        Entry[] entries = new DefaultEntry[count];

        for ( int i = 0; i < count; i++ )
        {
            Entry entry = new DefaultEntry();

            try
            {
                entry.add( "objectClass", "organizationalUnit" );
                entry.add( "ou", "testEntry", String.valueOf( i ) );
                entry.add( "telephoneNumber", String.valueOf( count ) );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }

            entries[i] = entry;
        }

        return entries;
    }


    private void recursivelyAddSearchData( Dn parent, Entry[] children, final long sizeLimit, long[] count )
        throws Exception
    {
        Dn[] childRdns = new Dn[children.length];

        for ( int i = 0; ( i < children.length ) && ( count[0] < sizeLimit ); i++ )
        {
            Dn childRdn = new Dn();
            childRdn = childRdn.addAll( parent );
            childRdn = childRdn.add( "ou=" + i );
            childRdns[i] = childRdn;
            children[i].setDn( childRdn );
            reusableAdminCon.add( children[i] );
            count[0]++;
        }

        if ( count[0] >= sizeLimit )
        {
            return;
        }

        for ( int i = 0; ( i < children.length ) && ( count[0] < sizeLimit ); i++ )
        {
            recursivelyAddSearchData( childRdns[i], children, sizeLimit, count );
        }
    }


    /**
     * Starts creating nodes under a parent with a set number of children.  First
     * a single node is created under the parent.  Thereafter a number of children
     * determined by the branchingFactor is added.  Until a sizeLimit is reached
     * descendants are created this way in a breath first recursive descent.
     *
     * @param parent the parent under which the first node is created
     * @param branchingFactor how to brach the data
     * @param sizelimit the amount of entries 
     * @return the immediate child node created under parent which contains the subtree
     * @throws Exception on error
     */
    private Dn addSearchData( Dn parent, int branchingFactor, long sizelimit ) throws Exception
    {
        Dn base = new Dn( "ou=tests," + parent.getName() );
        Entry entry = getTestNodes( 1 )[0];
        entry.add( SchemaConstants.OU_AT, "tests" );
        entry.setDn( base );

        reusableAdminCon.add( entry );

        recursivelyAddSearchData( base, getTestNodes( branchingFactor ), sizelimit, new long[]
            { 1 } );
        return base;
    }


    /**
     * Recursively deletes all entries including the base specified.
     *
     * @param rdn the relative dn from ou=system of the entry to delete recursively
     * @throws Exception if there are problems deleting entries
     */
    private void recursivelyDelete( Dn rdn ) throws Exception
    {
        Cursor<Response> results = reusableAdminCon.search( rdn.getName(), "(objectClass=*)",
            SearchScope.ONELEVEL, "*" );

        while ( results.next() )
        {
            SearchResultEntry result = ( SearchResultEntry ) results.get();
            Dn childRdn = result.getEntry().getDn();
            recursivelyDelete( childRdn );
        }

        results.close();
        reusableAdminCon.delete( rdn );
    }


    /**
     * Performs a single level search as a specific user on newly created data and checks
     * that result set count is 3.  The basic (objectClass=*) filter is used.
     *
     * @param uid the uid Rdn attribute value for the user under ou=users,ou=system
     * @param password the password of the user
     * @return true if the search succeeds as expected, false otherwise
     * @throws Exception if there are problems conducting the search
     */
    private boolean checkCanSearchAs( String uid, String password ) throws Exception
    {
        return checkCanSearchAs( uid, password, "(objectClass=*)", SearchScope.ONELEVEL, 3 );
    }


    /**
     * Performs a single level search as a specific user on newly created data and checks
     * that result set count is equal to a user specified amount.  The basic
     * (objectClass=*) filter is used.
     *
     * @param uid the uid Rdn attribute value for the user under ou=users,ou=system
     * @param password the password of the user
     * @param resultSetSz the expected size of the results
     * @return true if the search succeeds as expected, false otherwise
     * @throws Exception if there are problems conducting the search
     */
    private boolean checkCanSearchAs( String uid, String password, int resultSetSz ) throws Exception
    {
        return checkCanSearchAs( uid, password, "(objectClass=*)", SearchScope.ONELEVEL, resultSetSz );
    }


    /**
     * Performs a search as a specific user on newly created data and checks
     * that result set count is equal to a user specified amount.  The basic
     * (objectClass=*) filter is used.
     *
     * @param uid the uid Rdn attribute value for the user under ou=users,ou=system
     * @param password the password of the user
     * @param scope search controls
     * @param resultSetSz the expected size of the results
     * @return true if the search succeeds as expected, false otherwise
     * @throws Exception if there are problems conducting the search
     */
    private boolean checkCanSearchAs( String uid, String password, SearchScope scope, int resultSetSz )
        throws Exception
    {
        return checkCanSearchAs( uid, password, "(objectClass=*)", scope, resultSetSz );
    }


    /**
     * Performs a search as a specific user on newly created data and checks
     * that result set count is equal to a user specified amount.
     *
     * @param uid the uid Rdn attribute value for the user under ou=users,ou=system
     * @param password the password of the user
     * @param filter the search filter to use
     * @param scope search scope
     * @param resultSetSz the expected size of the results
     * @return true if the search succeeds as expected, false otherwise
     * @throws Exception if there are problems conducting the search
     */
    private boolean checkCanSearchAs( String uid, String password, String filter, SearchScope scope, int resultSetSz )
        throws Exception
    {

        Dn base = addSearchData( new Dn( "ou=system" ), 3, 10 );
        Dn userDn = new Dn( "uid=" + uid + ",ou=users,ou=system" );
        results.clear();
        LdapConnection userCtx = getConnectionAs( userDn, password );
        Cursor<Response> cursor = userCtx.search( base.getName(), filter, scope, "*" );
        int counter = 0;

        while ( cursor.next() )
        {
            Entry result = ( ( SearchResultEntry ) cursor.get() ).getEntry();
            results.put( result.getDn().getName(), result );
            counter++;
        }

        cursor.close();

        recursivelyDelete( base );

        return counter == resultSetSz;
    }


    /**
     * Adds an entryACI to specified entry below ou=system and runs a search.  Then it
     * checks to see the result size is correct.
     *
     * @param uid the uid Rdn attribute value for the user under ou=users,ou=system
     * @param password the password of the user
     * @param scope the search controls
     * @param dn the rdn
     * @param aci the aci
     * @param resultSetSz the result sz
     * @return true if the search succeeds as expected, false otherwise
     * @throws Exception if there are problems conducting the search
     */
    private boolean checkSearchAsWithEntryACI( String uid, String password, SearchScope scope, Dn dn, String aci,
        int resultSetSz ) throws Exception
    {
        Dn base = addSearchData( dn, 3, 10 );
        addEntryACI( base, aci );
        Dn userDn = new Dn( "uid=" + uid + ",ou=users,ou=system" );

        results.clear();
        LdapConnection userCtx = getConnectionAs( userDn, password );
        Cursor<Response> cursor = userCtx.search( base.getName(), "(objectClass=*)", scope, "*" );
        int counter = 0;

        while ( cursor.next() )
        {
            Entry result = ( ( SearchResultEntry ) cursor.get() ).getEntry();
            results.put( result.getDn().getName(), result );
            counter++;
        }

        recursivelyDelete( base );

        return counter == resultSetSz;
    }


    /**
     * Checks to see that the addSearchData() and the recursiveDelete()
     * functions in this test work properly.
     *
     * @throws Exception if there is a problem with the implementation of
     * these utility functions
     */
    @Test
    public void testAddSearchData() throws Exception
    {
        LdapConnection connection = getAdminConnection();
        Dn base = addSearchData( new Dn( "ou=system" ), 3, 10 );

        Cursor<Response> results = connection.search( base.getName(), "(objectClass=*)", SearchScope.SUBTREE,
            "+" );
        int counter = 0;

        while ( results.next() )
        {
            results.get();
            counter++;
        }

        assertEquals( 10, counter );
        recursivelyDelete( base );

        Entry entry = connection.lookup( base.getName() );
        assertNull( entry );
    }


    // -----------------------------------------------------------------------
    // All or nothing search ACI rule tests
    // -----------------------------------------------------------------------

    /**
     * Checks to make sure group membership based userClass works for add operations.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testGrantAdministrators() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a search operation which should fail without any ACI
        assertFalse( checkCanSearchAs( "billyd", "billyd" ) );

        // Gives search perms to all users in the Administrators group for
        // entries and all attribute types and values
        createAccessControlSubentry( "searchAdmin", 
            "{ " + 
            "  identificationTag \"searchAci\", " + 
            "  precedence 14, " +
            "  authenticationLevel none, " + 
            "  itemOrUserFirst userFirst: " + 
            "  { " + 
            "    userClasses " + 
            "    { " +
            "      userGroup { \"cn=Administrators,ou=groups,ou=system\" } " + 
            "    }, " + 
            "    userPermissions " +
            "    { " + 
            "      { " + 
            "        protectedItems {entry, allUserAttributeTypesAndValues}, " +
            "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + 
            "      } " + 
            "    } " + 
            "  } " +
            "}" );

        // see if we can now search that test entry which we could not before
        // add or should still fail since billd is not in the admin group
        assertFalse( checkCanSearchAs( "billyd", "billyd" ) );

        // now add billyd to the Administrator group and try again
        addUserToGroup( "billyd", "Administrators" );

        // try a search operation which should succeed with ACI and group membership change
        assertTrue( checkCanSearchAs( "billyd", "billyd" ) );
    }


    /**
     * Checks to make sure name based userClass works for search operations.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testGrantSearchByName() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a search operation which should fail without any ACI
        assertFalse( checkCanSearchAs( "billyd", "billyd" ) );

        // now add a subentry that enables user billyd to search an entry below ou=system
        createAccessControlSubentry( "billydSearch", 
            "{ " + 
            "  identificationTag \"searchAci\", " + 
            "  precedence 14, " +
             "  authenticationLevel none, " + 
             "  itemOrUserFirst userFirst: " + 
             "  { " + 
             "    userClasses " + 
             "    { " +
            "      name { \"uid=billyd,ou=users,ou=system\" } " + 
            "    }, " + 
            "    userPermissions " + 
            "    { " +
            "      { " + 
            "        protectedItems {entry, allUserAttributeTypesAndValues}, " +
            "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + 
            "      } " + 
            "    }" + 
            "  } " +
            "}" );

        // should work now that billyd is authorized by name
        assertTrue( checkCanSearchAs( "billyd", "billyd" ) );
    }


    /**
     * Checks to make sure name based userClass works for search operations
     * when we vary the case of the Dn.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testGrantSearchByNameUserDnCase() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a search operation which should fail without any ACI
        assertFalse( checkCanSearchAs( "BillyD", "billyd" ) );

        // now add a subentry that enables user billyd to search an entry below ou=system
        createAccessControlSubentry( "billydSearch", 
            "{ " + 
            "  identificationTag \"searchAci\", " + 
            "  precedence 14, " +
            "  authenticationLevel none, " + 
            "  itemOrUserFirst userFirst: " + 
            "  { " + 
            "    userClasses " + 
            "    { " +
            "      name { \"uid=billyd,ou=users,ou=system\" } " + 
            "    }, " + 
            "    userPermissions " + 
            "    { " +
            "      { " + 
            "        protectedItems {entry, allUserAttributeTypesAndValues}, " +
            "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + 
            "      } " + 
            "    } " + 
            "  } " +
            "}" );

        // should work now that billyd is authorized by name
        assertTrue( checkCanSearchAs( "BillyD", "billyd" ) );
    }


    /**
     * Checks to make sure subtree based userClass works for search operations.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testGrantSearchBySubtree() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a search operation which should fail without any ACI
        assertFalse( checkCanSearchAs( "billyd", "billyd" ) );

        // now add a subentry that enables user billyd to search an entry below ou=system
        createAccessControlSubentry( "billySearchBySubtree", 
            "{ " + 
            "  identificationTag \"searchAci\", " +
            "  precedence 14, " + 
            "  authenticationLevel none, " + 
            "  itemOrUserFirst userFirst: " + 
            "  { " +
            "    userClasses " + 
            "    { " + 
            "      subtree " + 
            "      { " +
            "        { base \"ou=users,ou=system\" } " + 
            "      } " + 
            "    }, " + 
            "    userPermissions " + 
            "    { " +
            "      { " + 
            "        protectedItems {entry, allUserAttributeTypesAndValues}, " +
            "        grantsAndDenials {  grantRead, grantReturnDN, grantBrowse } " + 
            "      } " + 
            "    } " + 
            "  } " +
            "}" );

        // should work now that billyd is authorized by the subtree userClass
        assertTrue( checkCanSearchAs( "billyd", "billyd" ) );
    }


    /**
     * Checks to make sure <b>allUsers</b> userClass works for search operations.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testGrantSearchAllUsers() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a search operation which should fail without any ACI
        assertFalse( checkCanSearchAs( "billyd", "billyd" ) );

        // now add a subentry that enables anyone to search an entry below ou=system
        createAccessControlSubentry( "anybodySearch", 
            "{ " + 
            "  identificationTag \"searchAci\", " +
            "  precedence 14, " + 
            "  authenticationLevel none, " + 
            "  itemOrUserFirst userFirst: " + 
            "  { " +
            "    userClasses { allUsers }, " + 
            "    userPermissions " + 
            "    { " + 
            "      { " +
            "        protectedItems {entry, allUserAttributeTypesAndValues}, " +
            "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + 
            "      } " + 
            "    } " + 
            "  } " +
            "}" );

        // see if we can now search that tree which we could not before
        // should work now with billyd now that all users are authorized
        assertTrue( checkCanSearchAs( "billyd", "billyd" ) );
    }


    // -----------------------------------------------------------------------
    //
    // -----------------------------------------------------------------------

    /**
     * Checks to make sure search does not return entries not assigned the
     * perscriptiveACI and that it does not fail with an exception.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testSelectiveGrantsAllUsers() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a search operation which should fail without any ACI
        assertFalse( checkCanSearchAs( "billyd", "billyd", SearchScope.SUBTREE, 4 ) );

        // now add a subentry that enables anyone to search an entry below ou=system
        // down two more rdns for DNs of a max size of 3
        createAccessControlSubentry( "anybodySearch", "{ maximum 2 }", 
            "{ " + 
            "  identificationTag \"searchAci\", " +
            "  precedence 14, " + 
            "  authenticationLevel none, " + 
            "  itemOrUserFirst userFirst: " + 
            "  { " +
            "    userClasses { allUsers }, " + 
            "    userPermissions " + 
            "    { " + 
            "      { " +
            "        protectedItems {entry, allUserAttributeTypesAndValues}, " +
            "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + 
            "      } " + 
            "    } " + 
            "  } " +
            "}" );

        // see if we can now search that test entry which we could not before
        // should work now with billyd now that all users are authorized
        assertTrue( checkCanSearchAs( "billyd", "billyd", SearchScope.SUBTREE, 4 ) );
    }


    /**
     * Checks to make sure attributeTypes are not present when permissions are
     * not given for reading them and their values.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testHidingAttributes() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a search operation which should fail without any ACI
        assertFalse( checkCanSearchAs( "billyd", "billyd", SearchScope.SUBTREE, 4 ) );

        // now add a subentry that enables anyone to search an entry below ou=system
        // down two more rdns for DNs of a max size of 3.  It only grants access to
        // the ou and objectClass attributes however.
        createAccessControlSubentry( "excludeTelephoneNumber", "{ maximum 2 }", "{ "
            + "  identificationTag \"searchAci\", " + "  precedence 14, " + "  authenticationLevel none, "
            + "  itemOrUserFirst userFirst: " + "  { " + "    userClasses { allUsers }, " + "    userPermissions "
            + "    { " + "      { " + "        protectedItems {entry, allAttributeValues { ou, objectClass } }, "
            + "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + "      } " + "    } " + "  } "
            + "}" );

        // see if we can now search and find 4 entries
        assertTrue( checkCanSearchAs( "billyd", "billyd", SearchScope.SUBTREE, 4 ) );

        // check to make sure the telephoneNumber attribute is not present in results
        for ( Entry result : results.values() )
        {
            assertNull( result.get( "telephoneNumber" ) );
        }

        // delete the subentry to test more general rule's inclusion of telephoneNumber
        deleteAccessControlSubentry( "excludeTelephoneNumber" );

        // now add a subentry that enables anyone to search an entry below ou=system
        // down two more rdns for DNs of a max size of 3.  This time we should be able
        // to see the telephoneNumber attribute
        createAccessControlSubentry( "includeAllAttributeTypesAndValues", "{ maximum 2 }", "{ "
            + "  identificationTag \"searchAci\", " + "  precedence 14, " + "  authenticationLevel none, "
            + "  itemOrUserFirst userFirst: " + "  { " + "    userClasses { allUsers }, " + "    userPermissions "
            + "    { " + "      { " + "        protectedItems {entry, allUserAttributeTypesAndValues }, "
            + "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + "      } " + "    }" + "  } "
            + "}" );

        // again we should find four entries
        assertTrue( checkCanSearchAs( "billyd", "billyd", SearchScope.SUBTREE, 4 ) );

        // check now to make sure the telephoneNumber attribute is present in results
        for ( Entry result : results.values() )
        {
            assertNotNull( result.get( "telephoneNumber" ) );
        }
    }


    /**
     * Checks to make sure specific attribute values are not present when
     * read permission is denied.
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    public void testHidingAttributeValues() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a search operation which should fail without any ACI
        assertFalse( checkCanSearchAs( "billyd", "billyd", 3 ) );

        // now add a subentry that enables anyone to search an entry below ou=system
        // down two more rdns for DNs of a max size of 3.  It only grants access to
        // the ou and objectClass attributes however.
        createAccessControlSubentry( "excludeOUValue", "{ maximum 2 }",
            "{ " + 
            "  identificationTag \"searchAci\", " +
            "  precedence 14, " + 
            "  authenticationLevel none, " + 
            "  itemOrUserFirst userFirst: " + 
            "  { " +
            "    userClasses { allUsers }, " + 
            "    userPermissions " + 
            "    { " + 
            "      { " +
            "        protectedItems " + 
            "        {" + 
            "          entry, " + 
            "          attributeType { ou }, " +
            "          allAttributeValues { objectClass }, " + 
            "          attributeValue { ou=0, ou=1, ou=2 } " +
            "        }, " + 
            "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + 
            "      } " +
            "    } " + 
            "  } " + 
            "}" );

        // see if we can now search and find 4 entries
        assertTrue( checkCanSearchAs( "billyd", "billyd", 3 ) );

        // check to make sure the ou attribute value "testEntry" is not present in results
        for ( Entry result : results.values() )
        {
            assertFalse( result.get( "ou" ).contains( "testEntry" ) );
        }

        // delete the subentry to test more general rule's inclusion of all values
        deleteAccessControlSubentry( "excludeOUValue" );

        // now add a subentry that enables anyone to search an entry below ou=system
        // down two more rdns for DNs of a max size of 3.  This time we should be able
        // to see the telephoneNumber attribute
        createAccessControlSubentry( "includeAllAttributeTypesAndValues", "{ maximum 2 }", 
            "{ " +
            "  identificationTag \"searchAci\", " + 
            "  precedence 14, " + 
            "  authenticationLevel none, " +
            "  itemOrUserFirst userFirst: " + 
            "  { " + 
            "    userClasses { allUsers }, " + 
            "    userPermissions " +
            "    { " + 
            "      { " + 
            "        protectedItems {entry, allUserAttributeTypesAndValues }, " +
            "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + 
            "      } " + 
            "    } " + 
            "  }" +
            "}" );

        // again we should find four entries
        assertTrue( checkCanSearchAs( "billyd", "billyd", 3 ) );

        // check now to make sure the telephoneNumber attribute is present in results
        for ( Entry result : results.values() )
        {
            assertTrue( result.get( "ou" ).contains( "testEntry" ) );
        }
    }


    /**
     * Adds a perscriptiveACI to allow search, tests for success, then adds entryACI
     * to deny read, browse and returnDN to a specific entry and checks to make sure
     * that entry cannot be accessed via search as a specific user.
     *
     * @throws Exception if the test is broken
     */
    @Test
    public void testPerscriptiveGrantWithEntryDenial() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // now add an entryACI denies browse, read and returnDN to a specific entry
        String aci = "{ " + "  identificationTag \"denyAci\", " + "  precedence 14, " + "  authenticationLevel none, "
            + "  itemOrUserFirst userFirst: " + "  { " + "    userClasses { allUsers }, " + "    userPermissions "
            + "    { " + "      { " + "        protectedItems {entry, allUserAttributeTypesAndValues}, "
            + "        grantsAndDenials { denyRead, denyReturnDN, denyBrowse } " + "      } " + "    } " + "  } " + "}";

        // try a search operation which should fail without any prescriptive ACI
        Dn testsDn = new Dn( "ou=system" );

        assertFalse( checkSearchAsWithEntryACI( "billyd", "billyd", SearchScope.SUBTREE, testsDn, aci, 9 ) );

        // now add a subentry that enables anyone to search below ou=system
        createAccessControlSubentry( "anybodySearch", "{ " + "  identificationTag \"searchAci\", "
            + "  precedence 14, " + "  authenticationLevel none, " + "  itemOrUserFirst userFirst: " + "  { "
            + "    userClasses { allUsers }, " + "    userPermissions " + "    { " + "      { "
            + "        protectedItems {entry, allUserAttributeTypesAndValues}, "
            + "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + "      } " + "    } " + "  } "
            + "}" );

        // see if we can now search the tree which we could not before
        // should work with billyd now that all users are authorized
        // we should NOT see the entry we are about to deny access to
        assertTrue( checkSearchAsWithEntryACI( "billyd", "billyd", SearchScope.SUBTREE, testsDn, aci, 9 ) );
        assertNull( results.get( "ou=tests,ou=system" ) );

        // try without the entry ACI, just perscriptive and see ou=tests,ou=system
        assertTrue( checkCanSearchAs( "billyd", "billyd", SearchScope.SUBTREE, 10 ) );
        assertNotNull( results.get( "ou=tests,ou=system" ) );
    }


    /**
     * Adds a perscriptiveACI to allow search, tests for success, then adds entryACI
     * to deny read, browse and returnDN to a specific entry and checks to make sure
     * that entry cannot be accessed via search as a specific user.  Here the
     * precidence of the ACI is put to the test.
     *
     * @throws Exception if the test is broken
     */
    @Test
    public void testPerscriptiveGrantWithEntryDenialWithPrecidence() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // now add an entryACI denying browse, read and returnDN to a specific entry
        String aci = "{ " + "  identificationTag \"denyAci\", " + "  precedence 14, " + "  authenticationLevel none, "
            + "  itemOrUserFirst userFirst: " + "  { " + "    userClasses { allUsers }, " + "    userPermissions "
            + "    { " + "      { " + "        protectedItems {entry, allUserAttributeTypesAndValues}, "
            + "        grantsAndDenials { denyRead, denyReturnDN, denyBrowse } " + "      } " + "    } " + "  } " + "}";

        // try a search operation which should fail without any prescriptive ACI
        Dn testsDn = new Dn( "ou=system" );

        assertFalse( checkSearchAsWithEntryACI( "billyd", "billyd", SearchScope.SUBTREE, testsDn, aci, 9 ) );

        // now add a subentry that enables anyone to search below ou=system
        createAccessControlSubentry( "anybodySearch", "{ " + "  identificationTag \"searchAci\", "
            + "  precedence 15, " + "  authenticationLevel none, " + "  itemOrUserFirst userFirst: " + "  { "
            + "    userClasses { allUsers }, " + "    userPermissions " + "    { " + "      { "
            + "        protectedItems {entry, allUserAttributeTypesAndValues}, "
            + "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + "      } " + "    } " + "  } "
            + "}" );

        // see if we can now search the tree which we could not before
        // should work with billyd now that all users are authorized
        // we should also see the entry we are about to deny access to
        // we see it because the precidence of the grant is greater
        // than the precedence of the denial
        assertTrue( checkSearchAsWithEntryACI( "billyd", "billyd", SearchScope.SUBTREE, testsDn, aci, 10 ) );
        assertNotNull( results.get( "ou=tests,ou=system" ) );

        // now add an entryACI denies browse, read and returnDN to a specific entry
        // but this time the precedence will be higher than that of the grant
        aci = "{ " + "  identificationTag \"denyAci\", " + "  precedence 16, " + "  authenticationLevel none, "
            + "  itemOrUserFirst userFirst: " + "  { " + "    userClasses { allUsers }, " + "    userPermissions "
            + "    { " + "      { " + "        protectedItems {entry, allUserAttributeTypesAndValues}, "
            + "        grantsAndDenials { denyRead, denyReturnDN, denyBrowse } " + "      } " + "    } " + "  } " + "}";

        // see if we can now search the tree which we could not before
        // should work with billyd now that all users are authorized
        // we should NOT see the entry we are about to deny access to
        // we do NOT see it because the precidence of the grant is less
        // than the precedence of the denial - so the denial wins
        assertTrue( checkSearchAsWithEntryACI( "billyd", "billyd", SearchScope.SUBTREE, testsDn, aci, 9 ) );
        assertNull( results.get( "ou=tests,ou=system" ) );
    }


    /**
     * Performs an object level search on the specified subentry relative to ou=system as a specific user.
     *
     * @param uid the uid Rdn attribute value of the user to perform the search as
     * @param password the password of the user
     * @param dn the relative name to the subentry under the ou=system AP
     * @return the single search result if access is allowed or null
     * @throws Exception if the search fails w/ exception other than no permission
     */
    private SearchResultEntry checkCanSearhSubentryAs( String uid, String password, Dn dn ) throws Exception
    {
        LdapConnection userCtx = getConnectionAs( new Dn( "uid=" + uid + ",ou=users,ou=system" ), password );
        SearchResultEntry result = null;
        Cursor<Response> list = null;

        list = userCtx.search( dn.getName(), "(objectClass=*)", SearchScope.OBJECT, "*" );
        if ( list.next() )
        {
            result = (SearchResultEntry) list.get();
        }

        list.close();

        return result;
    }


    @Test
    public void testSubentryAccess() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // now add a subentry that enables anyone to search below ou=system
        createAccessControlSubentry( "anybodySearch", "{ " + "  identificationTag \"searchAci\", "
            + "  precedence 14, " + "  authenticationLevel none, " + "  itemOrUserFirst userFirst: " + "  { "
            + "    userClasses { allUsers }, " + "    userPermissions " + "    { " + "      { "
            + "        protectedItems {entry, allUserAttributeTypesAndValues}, "
            + "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + "      } " + "    } " + "  } "
            + "}" );

        // check and see if we can access the subentry now
        assertNotNull( checkCanSearhSubentryAs( "billyd", "billyd", new Dn( "cn=anybodySearch,ou=system" ) ) );

        // now add a denial to prevent all users except the admin from accessing the subentry
        addSubentryACI( "{ " + "  identificationTag \"searchAci\", " + "  precedence 14, "
            + "  authenticationLevel none, " + "  itemOrUserFirst userFirst: " + "  { "
            + "    userClasses { allUsers }, " + "    userPermissions " + "    { " + "      { "
            + "        protectedItems {entry, allUserAttributeTypesAndValues}, "
            + "        grantsAndDenials { denyRead, denyReturnDN, denyBrowse } " + "      } " + "    } " + "  } " + "}" );

        // now we should not be able to access the subentry with a search
        assertNull( checkCanSearhSubentryAs( "billyd", "billyd", new Dn( "cn=anybodySearch,ou=system" ) ) );
    }


    @Test
    public void testGetMatchedName() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // now add a subentry that enables anyone to search/lookup and disclose on error
        // below ou=system, with the exclusion of ou=groups and everything below it
        createAccessControlSubentry( "selectiveDiscloseOnError", "{ specificExclusions "
            + "  { chopBefore:\"ou=groups\" } " + "}", "{ " + "  identificationTag \"searchAci\", "
            + "  precedence 14, " + "  authenticationLevel none, " + "  itemOrUserFirst userFirst:" + "  { "
            + "    userClasses { allUsers }, " + "    userPermissions " + "    { " + "      { "
            + "        protectedItems {entry, allUserAttributeTypesAndValues}, " + "        grantsAndDenials "
            + "        { " + "          grantRead, " + "          grantReturnDN, " + "          grantBrowse, "
            + "          grantDiscloseOnError " + "        } " + "      } " + "    } " + "  } " + "}" );

        // get a context as the user and try a lookup of a non-existant entry under ou=groups,ou=system
        LdapConnection userCtx = getConnectionAs( "uid=billyd,ou=users,ou=system", "billyd" );

        // we should not see ou=groups,ou=system for the remaining name
        Entry entry = userCtx.lookup( "cn=blah,ou=groups" );
        assertNull( entry );

        // now delete and replace subentry with one that does not excluse ou=groups,ou=system
        deleteAccessControlSubentry( "selectiveDiscloseOnError" );
        createAccessControlSubentry( "selectiveDiscloseOnError", "{ " + "  identificationTag \"searchAci\", "
            + "  precedence 14, " + "  authenticationLevel none, " + "  itemOrUserFirst userFirst: " + "  { "
            + "    userClasses { allUsers }, " + "    userPermissions " + "    { " + "      { "
            + "        protectedItems {entry, allUserAttributeTypesAndValues}, " + "        grantsAndDenials "
            + "        { " + "          grantRead, " + "          grantReturnDN, " + "          grantBrowse, "
            + "          grantDiscloseOnError " + "        } " + "      } " + "    } " + "  } " + "}" );

        // now try a lookup of a non-existant entry under ou=groups,ou=system again
        entry = userCtx.lookup( "cn=blah,ou=groups" );
        assertNull( entry );
    }


    @Test
    public void testUserClassParentOfEntry() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // create an entry subordinate to the user
        Entry phoneBook = new DefaultEntry( new Dn( "ou=phoneBook,uid=billyd,ou=users,ou=system" ) );
        phoneBook.add( SchemaConstants.OU_AT, "phoneBook" );
        phoneBook.add( SchemaConstants.OBJECT_CLASS_AT, "organizationalUnit" );

        getAdminConnection().add( phoneBook );

        // now add a subentry that enables anyone to search below their own entries
        createAccessControlSubentry( "anybodySearchTheirSubordinates", "{ " + "  identificationTag \"searchAci\", "
            + "  precedence 14, " + "  authenticationLevel none, " + "  itemOrUserFirst userFirst: " + "  { "
            + "    userClasses { allUsers }, " + "    userPermissions " + "    { " + "      { "
            + "        protectedItems {entry, allUserAttributeTypesAndValues}, "
            + "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + "      } " + "    } " + "  } "
            + "}" );

        // check and see if we can access the subentry now
        assertNotNull( checkCanSearhSubentryAs( "billyd", "billyd", new Dn(
            "ou=phoneBook,uid=billyd,ou=users,ou=system" ) ) );

        // now add a denial to prevent all users except the admin from accessing the subentry
        addPrescriptiveACI( "anybodySearchTheirSubordinates", "{ "
            + "  identificationTag \"anybodyDontSearchTheirSubordinates\", " + "  precedence 14, "
            + "  authenticationLevel none, " + "  itemOrUserFirst userFirst: " + "  { "
            + "    userClasses { parentOfEntry }, " + "    userPermissions " + "    { " + "      { "
            + "        protectedItems {entry, allUserAttributeTypesAndValues}, "
            + "        grantsAndDenials { denyRead, denyReturnDN, denyBrowse } " + "      } " + "    } " + "  } " + "}" );

        // now we should not be able to access the subentry with a search
        assertNull( checkCanSearhSubentryAs( "billyd", "billyd", new Dn( "ou=phoneBook,uid=billyd,ou=users,ou=system" ) ) );
    }


    /**
     * Checks that we can protect a RangeOfValues item
     *
     * @throws Exception if the test encounters an error
     */
    @Test
    @Ignore
    public void testRangeOfValues() throws Exception
    {
        // create the non-admin user
        createUser( "billyd", "billyd" );

        // try a search operation which should fail without any ACI
        assertFalse( checkCanSearchAs( "billyd", "billyd" ) );

        // now add a subentry that allows a user to read the CN only
        createAccessControlSubentry( "rangeOfValues", "{ " + "  identificationTag \"rangeOfValuesAci\", "
            + "  precedence 14," + "  authenticationLevel none, " + "  itemOrUserFirst userFirst: " + "  { "
            + "    userClasses { allUsers }, " + "    userPermissions " + "    { " + "      { "
            + "        protectedItems { entry }, "
            + "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + "      }, " + "      { "
            + "        protectedItems { rangeOfValues (cn=billyd) }, "
            + "        grantsAndDenials { grantRead, grantReturnDN, grantBrowse } " + "      } " + "    } " + "  } "
            + "}" );

        // see if we can now search and find 4 entries
        assertTrue( checkCanSearchAs( "billyd", "billyd" ) );

        // check to make sure the telephoneNumber attribute is not present in results
        for ( Entry result : results.values() )
        {
            assertNotNull( result.get( "cn" ) );
        }
    }
}
