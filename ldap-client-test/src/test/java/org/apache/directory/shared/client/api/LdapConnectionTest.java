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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.directory.api.ldap.codec.api.ConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.StringValue;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
    /** The Constant DEFAULT_HOST. */
    private static final String DEFAULT_HOST = "localhost";

    private static final String ADMIN_DN = "uid=admin,ou=system";

    private LdapConnection connection;


    @Before
    public void setup() throws Exception
    {
        connection = LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );
    }


    @After
    public void shutdown() throws Exception
    {
        LdapApiIntegrationUtils.releasePooledAdminConnection( connection, getLdapServer() );
    }


    /**
     * Test a successful bind request
     *
     * @throws IOException
     */
    @Test
    public void testBindRequest() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( "localhost", getLdapServer().getPort() );
        try
        {
            connection.bind( ADMIN_DN, "secret" );

            assertTrue( connection.isAuthenticated() );
        }
        finally
        {
            if ( connection != null )
            {
                connection.close();
            }
        }
    }


    @Test
    @Ignore
    public void testRebindNoPool() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( DEFAULT_HOST, getLdapServer().getPort() );
        connection.bind( ServerDNConstants.ADMIN_SYSTEM_DN, "secret" );

        for ( int i = 0; i < 10000; i++ )
        {
            if ( i % 100 == 0 )
            {
                System.out.println( "Iteration # " + i );
            }
            // First, unbind
            try
            {
                connection.unBind();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                throw e;
            }

            //Thread.sleep( 5 );

            // Don't close the connection, we want to reuse it
            // Then bind again
            try
            {
                connection.bind( ServerDNConstants.ADMIN_SYSTEM_DN, "secret" );
            }
            catch ( Exception e )
            {
                System.out.println( "Failure after " + i + " iterations" );
                e.printStackTrace();
                throw e;
            }
        }

        // terminate with an unbind
        try
        {
            connection.unBind();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        connection.close();
    }


    @Test
    public void testGetSupportedControls() throws Exception
    {
        List<String> controlList = connection.getSupportedControls();
        assertNotNull( controlList );
        assertFalse( controlList.isEmpty() );
    }


    @ApplyLdifs(
        {
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

        EntryCursor cursor = connection.search( ADMIN_DN, filter.toString(), SearchScope.SUBTREE, "+" );
        cursor.next();

        Entry readEntry = cursor.get();
        assertEquals( uuid, readEntry.get( SchemaConstants.ENTRY_UUID_AT ).getString() );

        cursor.close();
    }


    @Test
    public void testRetrieveBinaryAttibute() throws Exception
    {
        // test with a local connection using a local BinaryAttributeDetector
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( "localhost" );
        config.setLdapPort( ldapServer.getPort() );
        config.setName( ServerDNConstants.ADMIN_SYSTEM_DN );
        config.setCredentials( "secret" );
        config.setBinaryAttributeDetector( new DefaultConfigurableBinaryAttributeDetector() );

        LdapConnection myConnection = new LdapNetworkConnection( config );

        // Remove the UserPassword from the list
        ( ( ConfigurableBinaryAttributeDetector ) config.getBinaryAttributeDetector() ).
            removeBinaryAttribute( "userPassword" );
        myConnection.bind( "uid=admin,ou=system", "secret" );
        Entry entry = myConnection.lookup( "uid=admin,ou=system" );
        assertTrue( entry.get( SchemaConstants.USER_PASSWORD_AT ).get().isHumanReadable() );

        // Now, load a new binary Attribute
        ( ( ConfigurableBinaryAttributeDetector ) config.getBinaryAttributeDetector() ).
            addBinaryAttribute( "userPassword" );
        entry = myConnection.lookup( "uid=admin,ou=system" );
        assertFalse( entry.get( SchemaConstants.USER_PASSWORD_AT ).get().isHumanReadable() );

        // Now, test using the scerver's schema
        ( ( LdapNetworkConnection ) connection ).loadSchema();
        connection.bind( "uid=admin,ou=system", "secret" );

        // Use the default list of binary Attributes
        entry = connection.lookup( "uid=admin,ou=system" );
        assertFalse( entry.get( SchemaConstants.USER_PASSWORD_AT ).get().isHumanReadable() );

        myConnection.close();
    }


    @Test
    public void testLoadSchema() throws Exception
    {
        connection.loadSchema();
        SchemaManager manager = connection.getSchemaManager();
        assertNotNull( manager );
        assertTrue( manager.isEnabled( "system" ) );
        assertFalse( manager.isEnabled( "nis" ) );
        assertEquals( manager.getLoader().getAllSchemas().size(), manager.getEnabled().size() );
    }


    /**
     * this test is intended to test the behavior of CursorList when the RootDSE searchrequest was sent over
     *  wire
     */
    @Test
    public void testSearchEmptyDNWithOneLevelScopeAndNoObjectClassPresenceFilter() throws Exception
    {
        EntryCursor cursor = connection.search( "", "(objectClass=*)", SearchScope.ONELEVEL, "*", "+" );
        HashMap<String, Entry> map = new HashMap<String, Entry>();

        while ( cursor.next() )
        {
            Entry result = cursor.get();
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
        getLdapServer().getDirectoryService().setAllowAnonymousAccess( true );
        LdapNetworkConnection connection = new LdapNetworkConnection( "localhost", getLdapServer().getPort() );

        connection.bind();
        assertTrue( connection.isAuthenticated() );
        connection.close();
    }


    /**
     * Test a connection which does not have any schemaManager loaded but in which connection we
     * inject a BinaryAttributeDetector
     */
    @Test
    public void testNoSchemaConnectionWithBinaryDetector() throws Exception
    {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( "localhost" );
        config.setLdapPort( ldapServer.getPort() );
        config.setBinaryAttributeDetector( new DefaultConfigurableBinaryAttributeDetector() );

        LdapConnection ldapConnection = new LdapNetworkConnection( config );

        ldapConnection.bind( "uid=admin,ou=system", "secret" );

        // Try to retrieve a binary attribute : it should be seen as a byte[]
        Entry entry = ldapConnection.lookup( "uid=admin,ou=system" );
        assertFalse( entry.get( SchemaConstants.USER_PASSWORD_AT ).get().isHumanReadable() );

        ldapConnection.close();
    }
}
