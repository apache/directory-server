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
import static org.junit.Assert.assertTrue;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.message.SearchResultEntry;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.shared.ldap.entry.Entry;
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
@RunWith ( FrameworkRunner.class )
@ApplyLdifs( {
    // Entry # 1
    "dn: cn=test,ou=system",
    "objectClass: person",
    "cn: test",
    "sn: sn_test"
})
public class LookupIT extends AbstractLdapTestUnit
{
    /** The ldap connection */
    private LdapConnection connection;

    @Before
    public void setup() throws Exception
    {
        connection = IntegrationUtils.getAdminConnection( service );
    }


    @After
    public void shutdown() throws Exception
    {
        connection.close();
    }


    /**
     * Test a lookup( DN, "*") operation
     */
    @Test
    public void testLookupStar() throws Exception
    {
        SearchResultEntry result = (SearchResultEntry)connection.lookup( "cn=test,ou=system", "*" );
        Entry entry = result.getEntry();

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
     * Test a lookup( DN, "+") operation
     */
    @Test
    public void testLookupPlus() throws Exception
    {
        service.setDenormalizeOpAttrsEnabled( true );
        SearchResultEntry result = (SearchResultEntry)connection.lookup( "cn=test,ou=system", "+" );
        Entry entry = result.getEntry();

        assertNotNull( entry );

        // We should have 5 attributes
        assertEquals( 7, entry.size() );

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );

        // Check that we have all the operational attributes :
        // We should have 3 users attributes : objectClass, cn and sn
        // and 2 operational attributes : createTime and createUser
        assertNotNull( entry.get( "createTimestamp" ).getString() );
        assertEquals( "uid=admin,ou=system", entry.get( "creatorsName" ).getString() );
    }


    /**
     * Test a lookup( DN, []) operation
     */
    @Test
    public void testLookupEmptyAtrid() throws Exception
    {
        SearchResultEntry result = (SearchResultEntry)connection.lookup( "cn=test,ou=system", (String[])null );
        Entry entry = result.getEntry();

        assertNotNull( entry );

        // We should have 3 attributes
        assertEquals( 3, entry.size() );

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
    }


    /**
     * Test a lookup( DN ) operation
     */
    @Test
    public void testLookup() throws Exception
    {
        SearchResultEntry result = (SearchResultEntry)connection.lookup( "cn=test,ou=system" );
        Entry entry = result.getEntry();

        assertNotNull( entry );

        // We should have 3 attributes
        assertEquals( 3, entry.size() );

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertTrue( entry.contains( "objectClass", "top", "person" ) );
    }


    /**
     * Test a lookup( DN ) operation with a list of attributes
     */
    @Test
    @Ignore
    public void testLookupWithAttrs() throws Exception
    {
        SearchResultEntry result = (SearchResultEntry)connection.lookup( "cn=test,ou=system", "name" );
        Entry entry = result.getEntry();

        assertNotNull( entry );

        // We should have 3 attributes
        assertEquals( 2, entry.size() );

        // Check that all the user attributes are present
        assertEquals( "test", entry.get( "cn" ).getString() );
        assertEquals( "sn_test", entry.get( "sn" ).getString() );
        assertFalse( entry.containsAttribute( "objectClass" ) );
    }
}
