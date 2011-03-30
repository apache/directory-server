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
package org.apache.directory.server.core.operations.lookup;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.model.cursor.SearchCursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the lookup operation
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "LookupIT")
@ApplyLdifs(
    {
        // Entry # 1
        "dn: cn=test,ou=system",
        "objectClass: person",
        "cn: test",
        "sn: sn_test" })
public class LookupIT extends AbstractLdapTestUnit
{
    /** The ldap connection */
    private LdapConnection connection;


    @Before
    public void setup() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( getService() );
    }


    @After
    public void shutdown() throws Exception
    {
        connection.close();
    }


    /**
     * Test a lookup( Dn, "*") operation
     */
    @Test
    public void testLookupStar() throws Exception
    {
        Entry entry = connection.lookup( "cn=test,ou=system", "*" );
        assertNotNull( entry );

        // Check that we don't have any operational attributes :
        // We should have only 3 attributes : objectClass, cn and sn
        assertEquals( 3, entry.size() );

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
    }


    /**
     * Test a lookup( Dn, "+") operation
     */
    @Test
    @Ignore( "The '+' special attribute is not correctly handled." )
    public void testLookupPlus() throws Exception
    {
        getService().setDenormalizeOpAttrsEnabled( true );
        Entry entry = connection.lookup( "cn=test,ou=system", "+" );
        assertNotNull( entry );

        // We should have 4 attributes
        assertEquals( 4, entry.size() );

        // Check that all the user attributes are abstent
        assertNull( entry.get( "cn" ) );
        assertNull( entry.get( "sn" ) );
        assertNull( entry.get( "objectClass" ) );
        
        /*
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
        */

        // Check that we have all the operational attributes :
        // We should have 4 operational attributes : createTime, createUser, entryCSN and entryUUID
        assertNotNull( entry.get( "createTimestamp" ).getString() );
        assertEquals( "uid=admin,ou=system", entry.get( "creatorsName" ).getString() );
        assertNotNull( entry.get( "entryCSN" ).getString() );
        assertNotNull( entry.get( "entryUUID" ).getString() );
    }


    /**
     * Test a lookup( Dn, []) operation
     */
    @Test
    public void testLookupEmptyAtrid() throws Exception
    {
        Entry entry = connection.lookup( "cn=test,ou=system", ( String[] ) null );
        assertNotNull( entry );

        // We should have 3 attributes
        assertEquals( 3, entry.size() );

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
    }


    /**
     * Test a lookup( Dn ) operation
     */
    @Test
    public void testLookup() throws Exception
    {
        Entry entry = connection.lookup( "cn=test,ou=system" );

        assertNotNull( entry );

        // We should have 3 attributes
        assertEquals( 3, entry.size() );

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
    }


    /**
     * Test a lookup( Dn ) operation on the subschema subentry
     */
    @Test
    public void testLookupSubSchemaSubEntry() throws Exception
    {
        Entry entry = connection.lookup( "cn=schema", "+" );

        assertNotNull( entry );

        // We should have 2 attributes
        assertEquals( 2, entry.size() );

        // Check that all the user attributes are present
        assertEquals( "schema", entry.get( "cn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "subschema", "subentry", "apacheSubschema" ) );
    }


    /**
     * Test a lookup( Dn ) operation with a list of attributes
     */
    @Test
    public void testLookupWithAttrs() throws Exception
    {
        SearchCursor cursor = connection.search( "cn=test,ou=system", "(ObjectClass=*)",SearchScope.SUBTREE, "name" );
        
        while ( cursor.next() )
        {
            SearchResultEntry result = (SearchResultEntry)cursor.get();
            Entry entry = result.getEntry();
            assertNotNull( entry );
            assertEquals( 2, entry.size() );
            assertEquals( "test", entry.get( "cn" ).getString() );
            assertEquals( "sn_test", entry.get( "sn" ).getString() );
            assertFalse( entry.containsAttribute( "objectClass" ) );
        }
        
        cursor.close();

        //Entry entry = connection.lookup( "cn=test,ou=system", "name" );
        //assertNotNull( entry );

        // We should have 2 attributes
        //assertEquals( 2, entry.size() );

        // Check that all the user attributes are present
        //assertEquals( "test", entry.get( "cn" ).getString() );
        //assertEquals( "sn_test", entry.get( "sn" ).getString() );
        //assertFalse( entry.containsAttribute( "objectClass" ) );
    }


    /**
     * Test a lookup( Dn ) operation with no attributes
     */
    @Test
    public void testLookupWithNoAttrs() throws Exception
    {
        Entry entry = connection.lookup( "cn=test,ou=system", "1.1" );
        assertNotNull( entry );

        // We should have 0 attributes
        assertEquals( 0, entry.size() );
    }
}
