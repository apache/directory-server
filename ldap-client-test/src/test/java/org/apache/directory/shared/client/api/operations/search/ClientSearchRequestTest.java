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
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.apache.directory.ldap.client.api.LdapAsyncConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.SearchFuture;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.SearchCursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.SearchScope;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.model.message.SearchResultDone;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.junit.After;
import org.junit.Before;
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
    { "dn: cn=user1,ou=users,ou=system", "objectClass: person", "objectClass: top", "sn: user1 sn",
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
        "objectClass: person",
        "objectClass: top",
        "sn:: RW1tYW51ZWwgTMOpY2hhcm55",
        "cn: elecharny"

    })
public class ClientSearchRequestTest extends AbstractLdapTestUnit
{
    private LdapAsyncConnection connection;


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
    @After
    public void shutdown()
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


    @Test
    public void testSearch() throws Exception
    {
        SearchCursor cursor = connection.search( "ou=system", "(objectclass=*)",
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
        SearchCursor cursor = connection.search( "ou=system", "(objectclass=organizationalUnit)",
            SearchScope.ONELEVEL, "*", "+" );
        int count = 0;
        while ( cursor.next() )
        {
            Entry entry = ( ( SearchResultEntry ) cursor.get() ).getEntry();
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
            searchResponse = ( Response ) searchFuture.get( 1000, TimeUnit.MILLISECONDS );
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
    public void testSearchPersonSubstring() throws Exception
    {
        SearchFuture searchFuture = connection.searchAsync( "ou=system", "(objectclass=*ers*)", SearchScope.SUBTREE,
            "*", "+" );
        int count = 0;
        Response searchResponse = null;

        do
        {
            searchResponse = ( Response ) searchFuture.get( 100000, TimeUnit.MILLISECONDS );
            assertNotNull( searchResponse );

            if ( !( searchResponse instanceof SearchResultDone ) )
            {
                count++;
            }
        }
        while ( !( searchResponse instanceof SearchResultDone ) );

        assertEquals( 3, count );
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
        assertEquals( 2, count );

        count = 0;
        searchRequest.setDerefAliases( AliasDerefMode.NEVER_DEREF_ALIASES );
        cursor = connection.search( searchRequest );

        while ( cursor.next() )
        {
            count++;
        }
        cursor.close();

        assertEquals( 3, count );
    }


    @Test(expected = LdapException.class)
    public void testSearchUTF8() throws Exception
    {
        connection.search( "ou=system", "(sn=Emmanuel L\u00e9charny)", SearchScope.ONELEVEL, "*", "+" );
        fail();
    }
}
