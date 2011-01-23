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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.name.Dn;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A class to test the search operation with a returningAttributes parameter
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
@ApplyLdifs(
    { "dn: cn=user1,ou=users,ou=system", "objectClass: person", "objectClass: top", "sn: user1 sn", "cn: user1",

        // alias to the above entry
        "dn: cn=user1-alias,ou=users,ou=system", "objectClass: alias", "objectClass: top",
        "objectClass: extensibleObject", "aliasedObjectName: cn=user1,ou=users,ou=system", "cn: user1-alias" })
public class SearchRequestReturningAttributesTest extends AbstractLdapTestUnit
{
    private static LdapConnection connection;


    @Before
    public void setup() throws Exception
    {
        connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        Dn bindDn = new Dn( "uid=admin,ou=system" );
        connection.bind( bindDn.getName(), "secret" );
    }


    /**
     * Close the LdapConnection
     */
    @AfterClass
    public static void shutdown()
    {
        try
        {
            if ( connection != null )
            {
                connection.close();
            }
        }
        catch ( Exception ioe )
        {
            fail();
        }
    }


    /**
     * Test a search requesting all the attributes (* and +)
     *
     * @throws Exception
     */
    @Test
    public void testSearchAll() throws Exception
    {
        Cursor<Response> cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "*", "+" );
        int count = 0;
        Response response = null;

        while ( cursor.next() )
        {
            response = cursor.get();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );
        SearchResultEntry resultEntry = ( SearchResultEntry ) response;
        Entry entry = resultEntry.getEntry();

        assertEquals( 7, entry.size() );
        assertTrue( entry.containsAttribute( "objectClass" ) );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "creatorsName" ) );
        assertTrue( entry.containsAttribute( "createTimestamp" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }


    /**
     * Test a search requesting all the user attributes (*)
     *
     * @throws Exception
     */
    @Test
    public void testSearchAllUsers() throws Exception
    {
        Cursor<Response> cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "*" );
        int count = 0;
        Response response = null;

        while ( cursor.next() )
        {
            response = cursor.get();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );
        SearchResultEntry resultEntry = ( SearchResultEntry ) response;
        Entry entry = resultEntry.getEntry();

        assertEquals( 3, entry.size() );
        assertTrue( entry.containsAttribute( "objectClass" ) );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
    }


    /**
     * Test a search requesting all the operational attributes (+)
     *
     * @throws Exception
     */
    @Test
    public void testSearchAllOperationals() throws Exception
    {
        Cursor<Response> cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "+" );
        int count = 0;
        Response response = null;

        while ( cursor.next() )
        {
            response = cursor.get();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );
        SearchResultEntry resultEntry = ( SearchResultEntry ) response;
        Entry entry = resultEntry.getEntry();

        assertEquals( 4, entry.size() );
        assertTrue( entry.containsAttribute( "creatorsName" ) );
        assertTrue( entry.containsAttribute( "createTimestamp" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }


    /**
     * Test a search requesting all the user attributes plus a couple of operational
     *
     * @throws Exception
     */
    @Test
    public void testSearchAllUsersAndSomeOperationals() throws Exception
    {
        Cursor<Response> cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "*", "entryCSN", "entryUUID" );
        int count = 0;
        Response response = null;

        while ( cursor.next() )
        {
            response = cursor.get();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );
        SearchResultEntry resultEntry = ( SearchResultEntry ) response;
        Entry entry = resultEntry.getEntry();

        assertEquals( 5, entry.size() );
        assertTrue( entry.containsAttribute( "objectClass" ) );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }


    /**
     * Test a search requesting all the operational attributes and a couple of users attributes
     *
     * @throws Exception
     */
    @Test
    public void testSearchAllOperationalAndSomeUsers() throws Exception
    {
        Cursor<Response> cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "+", "cn", "sn" );
        int count = 0;
        Response response = null;

        while ( cursor.next() )
        {
            response = cursor.get();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );
        SearchResultEntry resultEntry = ( SearchResultEntry ) response;
        Entry entry = resultEntry.getEntry();

        assertEquals( 6, entry.size() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "creatorsName" ) );
        assertTrue( entry.containsAttribute( "createTimestamp" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }


    /**
     * Test a search requesting some user and Operational attributes
     *
     * @throws Exception
     */
    @Test
    public void testSearchSomeOpsAndUsers() throws Exception
    {
        Cursor<Response> cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "cn", "entryUUID", "sn", "entryCSN" );
        int count = 0;
        Response response = null;

        while ( cursor.next() )
        {
            response = cursor.get();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );
        SearchResultEntry resultEntry = ( SearchResultEntry ) response;
        Entry entry = resultEntry.getEntry();

        assertEquals( 4, entry.size() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }


    /**
     * Test a search requesting some attributes which appear more than one
     *
     * @throws Exception
     */
    @Test
    public void testSearchWithDuplicatedAttrs() throws Exception
    {
        Cursor<Response> cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "cn", "entryUUID", "cn", "sn", "entryCSN", "entryUUID" );
        int count = 0;
        Response response = null;

        while ( cursor.next() )
        {
            response = cursor.get();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );
        SearchResultEntry resultEntry = ( SearchResultEntry ) response;
        Entry entry = resultEntry.getEntry();

        assertEquals( 4, entry.size() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }


    /**
     * Test a search requesting some attributes using text and OID, and duplicated
     *
     * @throws Exception
     */
    @Test
    public void testSearchWithOIDAndtext() throws Exception
    {
        Cursor<Response> cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "cn", "1.3.6.1.1.16.4", "surName", "entryCSN", "entryUUID" );
        int count = 0;
        Response response = null;

        while ( cursor.next() )
        {
            response = cursor.get();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );
        SearchResultEntry resultEntry = ( SearchResultEntry ) response;
        Entry entry = resultEntry.getEntry();

        assertEquals( 4, entry.size() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }


    /**
     * Test a search requesting some attributes which are not present
     *
     * @throws Exception
     */
    @Test
    public void testSearchWithMissingAttributes() throws Exception
    {
        Cursor<Response> cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "cn", "1.3.6.1.1.16.4", "gn", "entryCSN", "entryUUID" );
        int count = 0;
        Response response = null;

        while ( cursor.next() )
        {
            response = cursor.get();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );
        SearchResultEntry resultEntry = ( SearchResultEntry ) response;
        Entry entry = resultEntry.getEntry();

        assertEquals( 3, entry.size() );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }


    /**
     * Test a search requesting no attributes (1.1)
     *
     * @throws Exception
     */
    @Test
    public void testSearchNoAttributes() throws Exception
    {
        Cursor<Response> cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "1.1" );
        int count = 0;
        Response response = null;

        while ( cursor.next() )
        {
            response = cursor.get();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );
        SearchResultEntry resultEntry = (SearchResultEntry) response;
        Entry entry = resultEntry.getEntry();

        assertEquals( 0, entry.size() );
    }


    /**
     * Test a search requesting no attributes (1.1) and some attributes
     *
     * @throws Exception
     */
    @Test
    public void testSearchNoAttributesAndAttributes() throws Exception
    {
        Cursor<Response> cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "1.1", "cn" );
        int count = 0;
        Response response = null;

        while ( cursor.next() )
        {
            response = cursor.get();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );
        SearchResultEntry resultEntry = ( SearchResultEntry ) response;
        Entry entry = resultEntry.getEntry();

        assertEquals( 1, entry.size() );
        assertTrue( entry.containsAttribute( "cn" ) );
    }


    /**
     * Test a search requesting no attributes (1.1) and all attributes (*, +)
     *
     * @throws Exception
     */
    @Test
    public void testSearchNoAttributesAllAttributes() throws Exception
    {
        Cursor<Response> cursor = connection.search( "cn=user1,ou=users,ou=system", "(objectclass=*)",
            SearchScope.OBJECT, "1.1", "*", "+" );
        int count = 0;
        Response response = null;

        while ( cursor.next() )
        {
            response = cursor.get();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );
        SearchResultEntry resultEntry = ( SearchResultEntry ) response;
        Entry entry = resultEntry.getEntry();

        assertEquals( 7, entry.size() );
        assertTrue( entry.containsAttribute( "objectClass" ) );
        assertTrue( entry.containsAttribute( "cn" ) );
        assertTrue( entry.containsAttribute( "sn" ) );
        assertTrue( entry.containsAttribute( "creatorsName" ) );
        assertTrue( entry.containsAttribute( "createTimestamp" ) );
        assertTrue( entry.containsAttribute( "entryUUID" ) );
        assertTrue( entry.containsAttribute( "entryCSN" ) );
    }
    
    
    /**
     *  DIRSERVER-1600
     */
    @Test
    public void testSearchTypesOnly() throws Exception
    {
        SearchRequest sr = new SearchRequestImpl();
        sr.setBase( new Dn( "uid=admin,ou=system" ) );
        sr.setFilter( "(uid=admin)" );
        sr.setScope( SearchScope.OBJECT );
        sr.setTypesOnly( true );
        
        Cursor<Response> cursor = connection.search( sr );
        int count = 0;
        Entry response = null;

        while ( cursor.next() )
        {
            response = ( ( SearchResultEntry ) cursor.get() ).getEntry();
            assertNotNull( response );
            count++;
        }
        cursor.close();

        assertEquals( 1, count );
        assertNull( response.get( SchemaConstants.UID_AT ).get() );
    }    
}
