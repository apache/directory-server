/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.client.api.operations.search;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultDone;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.ManageDsaITImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.SearchFuture;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.client.api.LdapApiIntegrationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * TODO ClientSearchRequestTest.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
@ApplyLdifs(
    {
        "dn: cn=user1,ou=users,ou=system",
        "objectClass: person",
        "objectClass: top",
        "sn: user1 sn",
        "cn: user1",

        // alias to the above entry
        "dn: cn=user1-alias,ou=users,ou=system",
        "objectClass: alias",
        "objectClass: top",
        "objectClass: extensibleObject",
        "aliasedObjectName: cn=user1,ou=users,ou=system",
        "cn: user1-alias",

        // Another user
        "dn: cn=elecharny,ou=users,ou=system",
        "objectClass: top",
        "objectClass: person",
        "objectClass: extensibleObject",
        "sn:: RW1tYW51ZWwgTMOpY2hhcm55",
        "cn: elecharny",
        "publicKey:: MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKbHnLFs5N2PHk0gkyI/g3XeIdjxnWOAW5RVap4zWZuNY4gNGH1MhfHPVHcy6WEMoo+zaxU0Xh+Iv6BzrIa70IUCAwEAAQ==",
        
        // Another test
        "dn: cn= test THIS , ou=users,ou=system",
        "objectClass: person",
        "objectClass: top",
        "sn: test sn",
        "cn: test THIS ",
        
        // Another test
        "dn: cn=test, ou=users,ou=system",
        "objectClass: person",
        "objectClass: top",
        "sn: test sn",
        "cn: test"

})
public class ClientSearchRequestTest extends AbstractLdapTestUnit
{
    private LdapNetworkConnection connection;
    
    
    @Before
    public void setup() throws Exception
    {
        connection = ( LdapNetworkConnection ) LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );
    }
    
    
    @After
    public void shutdown() throws Exception
    {
        LdapApiIntegrationUtils.releasePooledAdminConnection( connection, getLdapServer() );
    }
    
    
    @Test
    public void testSimpleSearch() throws Exception
    {
        EntryCursor cursor = connection.search( "ou=system", "(objectclass=*)", SearchScope.ONELEVEL );
        int count = 0;
    
        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            assertNotNull( entry );
            count++;
        }
    
        SearchResultDone done = cursor.getSearchResultDone();
    
        assertNotNull( done );
        assertEquals( ResultCodeEnum.SUCCESS, done.getLdapResult().getResultCode() );
        assertEquals( 5, count );
        cursor.close();
    }
    
    
    @Test
    public void testSimpleSearchWithControl() throws Exception
    {
        SearchRequest searchRequest = new SearchRequestImpl().setBase( new Dn( "ou=system" ) )
            .setFilter( "(objectclass=*)" )
            .setScope( SearchScope.ONELEVEL ).addControl( new ManageDsaITImpl() );
        SearchCursor cursor = connection.search( searchRequest );
        int count = 0;
    
        while ( cursor.next() )
        {
            Response response = cursor.get();
            assertNotNull( response );
    
            if ( response instanceof SearchResultEntry )
            {
                Entry entry = ( ( SearchResultEntry ) response ).getEntry();
                assertNotNull( entry );
            }
    
            count++;
        }
    
        SearchResultDone done = cursor.getSearchResultDone();
    
        assertNotNull( done );
        assertEquals( ResultCodeEnum.SUCCESS, done.getLdapResult().getResultCode() );
        assertEquals( 5, count );
        cursor.close();
    }
    
    
    @Test
    public void testSearch() throws Exception
    {
        EntryCursor cursor = connection.search( "ou=system", "(objectclass=*)",
            SearchScope.ONELEVEL,
            "*", "+" );
        int count = 0;
    
        while ( cursor.next() )
        {
            assertNotNull( cursor.get() );
            count++;
        }
    
        SearchResultDone done = cursor.getSearchResultDone();
    
        assertNotNull( done );
        assertEquals( ResultCodeEnum.SUCCESS, done.getLdapResult().getResultCode() );
        assertEquals( 5, count );
        cursor.close();
    }
    
    
    @Test
    public void testSearchEquality() throws Exception
    {
        EntryCursor cursor = connection.search( "ou=system", "(objectclass=organizationalUnit)",
            SearchScope.ONELEVEL, "*", "+" );
        int count = 0;
    
        while ( cursor.next() )
        {
            Entry entry = cursor.get();
            assertNotNull( entry );
            count++;
        }
    
        assertEquals( 4, count );
        cursor.close();
    }
    
    
    @Test
    public void testAsyncSearch() throws Exception
    {
        SearchFuture searchFuture = connection.searchAsync( "ou=system", "(objectclass=*)", SearchScope.ONELEVEL, "*",
            "+" );
        int count = 0;
        Response searchResponse = null;
    
        do
        {
            searchResponse = searchFuture.get( 1000, TimeUnit.MILLISECONDS );
            assertNotNull( searchResponse );
            if ( !( searchResponse instanceof SearchResultDone ) )
            {
                count++;
            }
        }
        while ( !( searchResponse instanceof SearchResultDone ) );
    
        assertEquals( 5, count );
    }
    
    
    /**
     * Test a search with a Substring filter
     * @throws Exception
     */
    @Test
    public void testSearchSubstring() throws Exception
    {
        SearchFuture searchFuture = connection.searchAsync( "ou=system", "(cn=*e*)", SearchScope.SUBTREE,
            "*", "+" );
        int count = 0;
        Response searchResponse = null;
    
        do
        {
            searchResponse = searchFuture.get( 100000, TimeUnit.MILLISECONDS );
            assertNotNull( searchResponse );
    
            if ( !( searchResponse instanceof SearchResultDone ) )
            {
                count++;
            }
        }
        while ( !( searchResponse instanceof SearchResultDone ) );
    
        assertEquals( 5, count );
    }
    
    
    /**
     * Test a search with a more evoluted Substring filter
     * @throws Exception
     */
    @Test
    public void testSearchSubstring2() throws Exception
    {
        SearchFuture searchFuture = connection.searchAsync( "ou=system", "(cn=Test *)", SearchScope.SUBTREE,
            "*", "+" );
        int count = 0;
        Response searchResponse = null;
    
        do
        {
            searchResponse = searchFuture.get( 100000, TimeUnit.MILLISECONDS );
            assertNotNull( searchResponse );
    
            if ( !( searchResponse instanceof SearchResultDone ) )
            {
                count++;
            }
        }
        while ( !( searchResponse instanceof SearchResultDone ) );
    
        assertEquals( 2, count );
    }
    
    
    @Test
    public void testSearchWithDerefAlias() throws Exception
    {
        SearchRequest searchRequest = new SearchRequestImpl();
        searchRequest.setBase( new Dn( "ou=users,ou=system" ) );
        searchRequest.setFilter( "(objectClass=*)" );
        searchRequest.setScope( SearchScope.ONELEVEL );
        searchRequest.addAttributes( "*" );
    
        int count = 0;
        Cursor<Response> cursor = connection.search( searchRequest );
    
        while ( cursor.next() )
        {
            count++;
        }
        cursor.close();
    
        // due to dereferencing of aliases we get only one entry
        assertEquals( 4, count );
    
        count = 0;
        searchRequest.setDerefAliases( AliasDerefMode.NEVER_DEREF_ALIASES );
        cursor = connection.search( searchRequest );
    
        while ( cursor.next() )
        {
            count++;
        }
        cursor.close();
    
        assertEquals( 5, count );
    }
    
    
    @Test
    public void testSearchUTF8() throws Exception
    {
        EntryCursor cursor = connection.search( "ou=users,ou=system", "(sn=Emmanuel L\u00E9charny)", SearchScope.ONELEVEL,
            "*", "+" );
    
        assertTrue( cursor.next() );
    
        Entry entry = cursor.get();
        assertNotNull( entry );
        assertTrue( entry.contains( "cn", "elecharny" ) );
        assertTrue( entry.contains( "sn", "Emmanuel L\u00E9charny" ) );
    
        cursor.close();
    }
    
    
    @Test
    public void testSearchBinary() throws Exception
    {
        connection.loadSchema();
        EntryCursor cursor = connection
            .search(
                "ou=system",
                "(publicKey=\\30\\5C\\30\\0D\\06\\09\\2A\\86\\48\\86\\F7\\0D\\01\\01\\01\\05\\00\\03\\4B\\00\\30\\48\\02\\41\\00\\A6\\C7\\9C\\B1\\6C\\E4\\DD\\8F\\1E\\4D\\20\\93\\22\\3F\\83\\75\\DE\\21\\D8\\F1\\9D\\63\\80\\5B\\94\\55\\6A\\9E\\33\\59\\9B\\8D\\63\\88\\0D\\18\\7D\\4C\\85\\F1\\CF\\54\\77\\32\\E9\\61\\0C\\A2\\8F\\B3\\6B\\15\\34\\5E\\1F\\88\\BF\\A0\\73\\AC\\86\\BB\\D0\\85\\02\\03\\01\\00\\01)",
                SearchScope.SUBTREE, "publicKey" );
    
        assertTrue( cursor.next() );
    
        Entry entry = cursor.get();
        assertNotNull( entry.get( "publicKey" ) );
    
        cursor.close();
    }
    
    
    @Test
    public void testSubDn() throws Exception
    {
        connection.loadSchema();
        EntryCursor cursor = connection.search( "ou=system", "(cn=user1)", SearchScope.SUBTREE, "publicKey" );
    
        assertTrue( cursor.next() );
    
        Entry entry = cursor.get();
        assertEquals( "cn=user1,ou=users,ou=system", entry.getDn().getName() );
    
        cursor.close();
    
        SearchRequest req = new SearchRequestImpl();
        req.setScope( SearchScope.SUBTREE );
        req.addAttributes( "*" );
        req.setTimeLimit( 0 );
        req.setBase( new Dn( "ou=system" ) );
        req.setFilter( "(cn=user1)" );
    
        SearchCursor searchCursor = connection.search( req );
    
        assertTrue( searchCursor.next() );
    
        Response response = searchCursor.get();
    
        Entry resultEntry = ( ( SearchResultEntry ) response ).getEntry();
        assertEquals( "cn=user1,ou=users,ou=system", resultEntry.getDn().getName() );
    
        searchCursor.close();
    }
    
    
    @Test
    public void testSubstring() throws Exception
    {
        connection.loadSchema();
        EntryCursor cursor = connection.search( "ou=system", "(cn=user1)", SearchScope.SUBTREE, "publicKey" );
    
        assertTrue( cursor.next() );
    
        Entry entry = cursor.get();
        assertEquals( "cn=user1,ou=users,ou=system", entry.getDn().getName() );
    
        cursor.close();
    
        SearchRequest req = new SearchRequestImpl();
        req.setScope( SearchScope.SUBTREE );
        req.addAttributes( "*" );
        req.setTimeLimit( 0 );
        req.setBase( new Dn( "ou=system" ) );
        req.setFilter( "(cn=user1)" );
    
        SearchCursor searchCursor = connection.search( req );
    
        assertTrue( searchCursor.next() );
    
        Response response = searchCursor.get();
    
        Entry resultEntry = ( ( SearchResultEntry ) response ).getEntry();
        assertEquals( "cn=user1,ou=users,ou=system", resultEntry.getDn().getName() );
    
        searchCursor.close();
    }
    
    
    /**
     * Test to demonstrate https://issues.apache.org/jira/browse/DIRAPI-140
     * Fixed to demonstrate that it works, if we loop until we don't have anymore results
     */
    @Test
    @Ignore("The test has been fixed, it's now ignored as it takes 180seconds")
    public void test_DIRAPI140() throws Exception
    {
        for ( int i = 0; i < 10000; i++ )
        {
            SearchRequest req = new SearchRequestImpl();
            req.setScope( SearchScope.SUBTREE );
            req.addAttributes( "*" );
            req.setTimeLimit( 0 );
            req.setBase( new Dn( "ou=system" ) );
            req.setFilter( "(cn=user1)" );
    
            SearchCursor searchCursor = connection.search( req );
    
            // We should have only one entry
            assertTrue( searchCursor.next() );
    
            searchCursor.close();
        }
    }
}
