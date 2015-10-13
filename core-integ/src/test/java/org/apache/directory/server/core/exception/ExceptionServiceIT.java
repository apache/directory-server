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
package org.apache.directory.server.core.exception;


import static org.apache.directory.server.core.integ.IntegrationUtils.getAdminConnection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapAliasException;
import org.apache.directory.api.ldap.model.exception.LdapContextNotEmptyException;
import org.apache.directory.api.ldap.model.exception.LdapEntryAlreadyExistsException;
import org.apache.directory.api.ldap.model.exception.LdapNoSuchObjectException;
import org.apache.directory.api.ldap.model.message.AddRequest;
import org.apache.directory.api.ldap.model.message.AddRequestImpl;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the correct operation of the ServerExceptionService.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "ExceptionServiceIT-DS")
public class ExceptionServiceIT extends AbstractLdapTestUnit
{

    private AddResponse createSubContext( String type, String value ) throws Exception
    {
        return createSubContext( new Dn( ServerDNConstants.SYSTEM_DN ), type, value );
    }


    private AddResponse createSubContext( Dn parent, String type, String value ) throws Exception
    {
        Dn dn = parent;
        dn = dn.add( "ou=" + value );
        Entry entry = new DefaultEntry( dn );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "person" );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "OrganizationalPerson" );
        entry.add( SchemaConstants.CN_AT, value );
        entry.add( SchemaConstants.SN_AT, value );

        AddRequest addRequest = new AddRequestImpl();
        addRequest.setEntry( entry );
        addRequest.setEntryDn( dn );

        AddResponse resp = getAdminConnection( getService() ).add( addRequest );

        return resp;
    }


    @After
    public void closeConnections()
    {
        IntegrationUtils.closeConnections();
    }


    // ------------------------------------------------------------------------
    // Search Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Test search operation failure when the search base is non-existant.
     *
     * @throws Exception on error
     */
    @Test
    public void testFailSearchNoSuchObject() throws Exception
    {
        EntryCursor cursor = getAdminConnection( getService() ).search( "ou=blah", "(objectClass=*)",
            SearchScope.ONELEVEL, "*" );
        assertFalse( cursor.next() );
        cursor.close();
    }


    /**
     * Search operation control to test if normal search operations occur
     * correctly.
     *
     * @throws Exception on error
     */
    @Test
    public void testSearchControl() throws Exception
    {
        EntryCursor cursor = getAdminConnection( getService() ).search( "ou=users,ou=system", "(objectClass=*)",
            SearchScope.ONELEVEL, "*" );

        assertFalse( cursor.next() );

        cursor.close();
    }


    // ------------------------------------------------------------------------
    // Move Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Test move operation failure when the object moved is non-existant.
     *
     * @throws Exception on error
     */
    @Test
    public void testFailMoveEntryAlreadyExists() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        Entry entry = new DefaultEntry( "ou=users,ou=groups,ou=system",
            "ObjectClass: top",
            "ObjectClass: OrganizationalUnit",
            "ou: users" );

        connection.add( entry );

        connection.rename( entry.getDn(), new Rdn( "ou=users" ) );

        Entry userzEntry = new DefaultEntry( "ou=userz,ou=groups,ou=system",
            "ObjectClass: top",
            "ObjectClass: OrganizationalUnit",
            "ou: userz" );

        connection.add( userzEntry );

        try
        {
            connection.rename( "ou=userz,ou=groups,ou=system", "ou=users", true );
            fail();
        }
        catch ( LdapEntryAlreadyExistsException leaee )
        {
            assertTrue( true );
        }
    }


    /**
     * Test move operation failure when the object moved is non-existant.

     * @throws Exception on error
     */
    @Test
    public void testFailMoveNoSuchObject() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        try
        {
            connection.rename( "ou=blah,ou=groups,ou=system", "ou=blah1" );
            fail();
        }
        catch ( LdapNoSuchObjectException lnsoe )
        {
            assertTrue( true );
        }

        try
        {
            connection.rename( "ou=blah,ou=groups,ou=system", "ou=blah1" );
            fail();
        }
        catch ( LdapNoSuchObjectException lnsoe )
        {
            assertTrue( true );
        }
    }


    /**
     * Move operation control to test if normal move operations occur
     * correctly.
     *
     * @throws Exception on error
     */
    @Test
    public void testMoveControl() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        connection.move( "ou=users,ou=system", "ou=groups,ou=system" );
        Entry entry = connection.lookup( "ou=users,ou=groups,ou=system" );
        assertNotNull( entry );

        entry = connection.lookup( "ou=users,ou=system" );
        assertNull( entry );
    }


    // ------------------------------------------------------------------------
    // ModifyRdn Operation Tests
    // ------------------------------------------------------------------------
    /**
     * Test modifyRdn operation success when the object is renamed in place.
     *
     * @throws Exception on error
     */
    @Test
    public void testAllowModifyRdnEntry() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        Entry entry = connection.lookup( "ou=users,ou=system" );

        assertNotNull( entry );
        assertEquals( 1, entry.get( "ou" ).size() );
        assertEquals( "users", entry.get( "ou" ).getString() );

        connection.rename( "ou=users,ou=system", "ou=Users" );

        entry = connection.lookup( "ou=Users,ou=system" );

        assertNotNull( entry );
        assertEquals( 1, entry.get( "ou" ).size() );
        assertEquals( "users", entry.get( "ou" ).getString() );
    }


    /**
     * Test modifyRdn operation failure when the object renamed is existant.
     *
     * @throws Exception on error
     */
    @Test(expected = LdapEntryAlreadyExistsException.class)
    public void testFailModifyRdnEntryAlreadyExists() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        connection.rename( "ou=users,ou=system", "ou=Groups" );
    }


    /**
     * Test modifyRdn operation failure when the object renamed is non-existant.
     *
     * @throws Exception on error
     */
    @Test(expected = LdapNoSuchObjectException.class)
    public void testFailModifyRdnNoSuchObject() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        connection.rename( "ou=blah,ou=system", "ou=asdf" );
    }


    /**
     * Modify operation control to test if normal modify operations occur
     * correctly.
     *
     * @throws Exception on error
     */
    @Test
    public void testModifyRdnControl() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        connection.rename( "ou=users,ou=system", "ou=asdf" );
        assertNotNull( connection.lookup( "ou=asdf,ou=system" ) );

        assertNull( connection.lookup( "ou=users,ou=system" ) );
    }


    // ------------------------------------------------------------------------
    // Modify Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Test modify operation failure when the object modified is non-existant.
     *
     * @throws Exception on error
     */
    @Test
    public void testFailModifyNoSuchObject() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( "ou=blah,ou=system" ) );
        modReq.add( SchemaConstants.OU_AT, "another-value" );

        ModifyResponse modResp = connection.modify( modReq );
        assertEquals( ResultCodeEnum.NO_SUCH_OBJECT, modResp.getLdapResult().getResultCode() );
    }


    /**
     * Modify operation control to test if normal modify operations occur
     * correctly.
     *
     * @throws Exception on error
     */
    @Test
    public void testModifyControl() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( new Dn( "ou=users,ou=system" ) );
        modReq.add( SchemaConstants.OU_AT, "dummyValue" );

        connection.modify( modReq );
        Entry entry = connection.lookup( "ou=users,ou=system" );
        Attribute ou = entry.get( "ou" );
        assertTrue( ou.contains( "users" ) );
        assertTrue( ou.contains( "dummyValue" ) );
    }


    // ------------------------------------------------------------------------
    // Lookup Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Test lookup operation failure when the object looked up is non-existant.
     *
     * @throws Exception on error
     */
    @Test
    public void testFailLookupNoSuchObject() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        assertNull( connection.lookup( "ou=blah,ou=system" ) );
    }


    /**
     * Lookup operation control to test if normal lookup operations occur
     * correctly.
     *
     * @throws Exception on error
     */
    @Test
    public void testLookupControl() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        Entry entry = connection.lookup( "ou=users,ou=system" );
        assertNotNull( entry );
        assertEquals( "users", entry.get( "ou" ).getString() );
    }


    // ------------------------------------------------------------------------
    // List Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Test list operation failure when the base searched is non-existant.
     *
     * @throws Exception on error
     *
    @Test
    public void testFailListNoSuchObject() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        try
        {
            connection.list( "ou=blah" );
            fail( "Execution should never get here due to exception!" );
        }
        catch ( LdapNoSuchObjectException e )
        {
            assertEquals( "ou=system", e.getResolvedName().toString() );
            assertEquals( ResultCodeEnum.NO_SUCH_OBJECT, e.getResultCode() );
        }
    }


    /**
     * List operation control to test if normal list operations occur correctly.
     *
     * @throws Exception on error
     *
    @Test
    public void testListControl() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        NamingEnumeration<?> list = connection.list( "ou=users" );

        if ( list.hasMore() )
        {
            SearchResult result = (SearchResult)list.next();
            assertNotNull( result.getAttributes() );
            assertEquals( "uid=akarasulu,ou=users,ou=system", result.getName() );
        }

        assertFalse( list.hasMore() );
    }
    */

    // ------------------------------------------------------------------------
    // Add Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Tests for add operation failure when the parent of the entry to add does
     * not exist.
     *
     * @throws Exception on error
     */
    @Test(expected = LdapAliasException.class)
    public void testFailAddOnAlias() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        Entry entry = new DefaultEntry( "cn=toanother,ou=system" );
        entry.add( SchemaConstants.OBJECT_CLASS_AT, "alias", SchemaConstants.EXTENSIBLE_OBJECT_OC );
        entry.add( "aliasedObjectName", "ou=users,ou=system" );

        connection.add( entry );

        Entry aliasChild = new DefaultEntry( "ou=blah,cn=toanother,ou=system" );
        aliasChild.add( SchemaConstants.OBJECT_CLASS_AT, "organizationalUnit" );
        aliasChild.add( SchemaConstants.OU_AT, "blah" );

        connection.add( aliasChild );
    }


    /**
     * Tests for add operation failure when the entry to add already exists.
     *
     * @throws Exception on error
     */
    @Test
    public void testFailAddEntryAlreadyExists() throws Exception
    {
        createSubContext( "ou", "blah" );

        AddResponse resp = createSubContext( "ou", "blah" );
        assertEquals( ResultCodeEnum.ENTRY_ALREADY_EXISTS, resp.getLdapResult().getResultCode() );
    }


    /**
     * Add operation control to test if normal add operations occur correctly.
     *
     * @throws Exception on error
     */
    @Test
    public void testAddControl() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        AddResponse resp = createSubContext( "ou", "blah" );
        resp = createSubContext( new Dn( "ou=blah,ou=system" ), "ou", "subctx" );
        Entry entry = connection.lookup( "ou=subctx,ou=blah,ou=system" );
        assertNotNull( entry );
    }


    // ------------------------------------------------------------------------
    // Delete Operation Tests
    // ------------------------------------------------------------------------

    /**
     * Tests for delete failure when the entry to be deleted has child entries.
     *
     * @throws Exception on error
     */
    @Test(expected = LdapContextNotEmptyException.class)
    public void testFailDeleteNotAllowedOnNonLeaf() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        AddResponse resp = createSubContext( "ou", "blah" );
        resp = createSubContext( new Dn( "ou=blah,ou=system" ), "ou", "subctx" );

        connection.delete( "ou=blah,ou=system" );
    }


    /**
     * Tests delete to make sure it fails when we try to delete an entry that
     * does not exist.
     *
     * @throws Exception on error
     */
    @Test(expected = LdapNoSuchObjectException.class)
    public void testFailDeleteNoSuchObject() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        connection.delete( "ou=blah,ou=system" );
    }


    /**
     * Delete operation control to test if normal delete operations occur.
     *
     * @throws Exception on error
     */
    @Test
    public void testDeleteControl() throws Exception
    {
        LdapConnection connection = getAdminConnection( getService() );

        AddResponse resp = createSubContext( "ou", "blah" );

        Entry entry = connection.lookup( "ou=blah,ou=system" );
        assertNotNull( entry );
        connection.delete( entry.getDn() );

        Object respEntry = connection.lookup( entry.getDn().getName() );
        assertNull( respEntry );
    }
}
