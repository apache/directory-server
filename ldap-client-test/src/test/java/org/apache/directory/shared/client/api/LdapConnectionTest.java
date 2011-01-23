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
package org.apache.directory.shared.client.api;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

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
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.model.message.BindResponse;
import org.apache.directory.shared.ldap.model.message.Response;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Test the LdapConnection class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
public class LdapConnectionTest extends AbstractLdapTestUnit
{

    private static final String ADMIN_DN = "uid=admin,ou=system";

    private static LdapConnection connection;


    @Before
    public void bindConnection() throws Exception
    {
        connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        connection.bind( ADMIN_DN, "secret" );
    }


    @After
    public void unbindConnection() throws Exception
    {
        if ( connection != null )
        {
            connection.close();
        }
    }


    /**
     * Test a successful bind request
     *
     * @throws IOException
     */
    @Test
    public void testBindRequest() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        try
        {
            BindResponse bindResponse = connection.bind( ADMIN_DN, "secret" );

            assertNotNull( bindResponse );

            //connection.unBind();
        }
        catch ( LdapException le )
        {
            fail();
        }
        catch ( IOException ioe )
        {
            fail();
        }
        finally
        {
            try
            {
                connection.close();
            }
            catch ( IOException ioe )
            {
                fail();
            }
        }
    }


    @Test
    public void testGetSupportedControls() throws Exception
    {
        List<String> controlList = connection.getSupportedControls();
        assertNotNull( controlList );
        assertFalse( controlList.isEmpty() );
    }


    @ApplyLdifs({
        "dn: uid=kayyagari,ou=system",
        "objectClass: extensibleObject",
        "objectClass: uidObject",
        "objectClass: referral",
        "objectClass: top",
        "uid: kayyagari",
        "ref: ldap://ad.example.com/uid=kayyagari,ou=system"
    })
    @Test
    public void testLookup() throws Exception
    {
        Entry entry = connection.lookup( ADMIN_DN );
        assertNull( entry.get( SchemaConstants.ENTRY_UUID_AT ) );

        // perform lookup with operational attributes
        entry = connection.lookup( ADMIN_DN, "+", "*" );
        assertNotNull( entry.get( SchemaConstants.ENTRY_UUID_AT ) );
        
        entry = connection.lookup( "uid=kayyagari,ou=system" );
        assertNull( entry );
    }


    @Test
    public void searchByEntryUuid() throws Exception
    {
        Entry entry = connection.lookup( ADMIN_DN, "+" );

        String uuid = entry.get( SchemaConstants.ENTRY_UUID_AT ).getString();

        EqualityNode<String> filter = new EqualityNode<String>( SchemaConstants.ENTRY_UUID_AT, new StringValue( uuid ) );

        Cursor<Response> cursor = connection.search( ADMIN_DN, filter.toString(), SearchScope.SUBTREE, "+" );
        cursor.next();

        Entry readEntry = ( (SearchResultEntry) cursor.get() ).getEntry();
        assertEquals( uuid, readEntry.get( SchemaConstants.ENTRY_UUID_AT ).getString() );

        cursor.close();
    }


    @Test
    public void testRetrieveBinaryAttibute() throws Exception
    {
        Entry entry = connection.lookup( "uid=admin,ou=system" );
        assertFalse( entry.get( SchemaConstants.USER_PASSWORD_AT ).get().isBinary() );

        connection.loadSchema();

        entry = connection.lookup( "uid=admin,ou=system" );
        assertTrue( entry.get( SchemaConstants.USER_PASSWORD_AT ).get().isBinary() );
    }


    @Test
    public void testLoadSchema() throws Exception
    {
        connection.loadSchema();
        SchemaManager manager = connection.getSchemaManager();
        assertNotNull( manager );
        assertTrue( manager.isEnabled( "system" ) );
        assertTrue( manager.isEnabled( "nis" ) );
        assertEquals( manager.getLoader().getAllSchemas().size(), manager.getEnabled().size() );
    }


    /**
     * this test is intended to test the behavior of CursorList when the RootDSE searchrequest was sent over
     *  wire
     */
    @Test
    public void testSearchEmptyDNWithOneLevelScopeAndNoObjectClassPresenceFilter() throws Exception
    {
        Cursor<Response> cursor = connection.search( "", "(objectClass=*)", SearchScope.ONELEVEL, "*", "+" );
        HashMap<String, Entry> map = new HashMap<String, Entry>();

        while ( cursor.next() )
        {
            Entry result = ( ( SearchResultEntry ) cursor.get() ).getEntry();
            map.put( result.getDn().getName(), result );
        }
        cursor.close();

        assertEquals( 2, map.size() );

        assertTrue( map.containsKey( "ou=system" ) );
        assertTrue( map.containsKey( "ou=schema" ) );
    }

    
    @Test
    public void testAnonBind() throws Exception
    {
        connection = new LdapNetworkConnection( "localhost", ldapServer.getPort() );
        
        BindResponse resp = connection.bind();
        assertEquals( ResultCodeEnum.SUCCESS, resp.getLdapResult().getResultCode() );
        connection.close();
    }
}
