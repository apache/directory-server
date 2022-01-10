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


import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.directory.api.ldap.codec.api.ConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.exception.InvalidConnectionException;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
/**
 * Test the LdapConnection class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
public class LdapConnectionTest extends AbstractLdapTestUnit
{
    private static final String ADMIN_DN = "uid=admin,ou=system";

    private LdapConnection connection;


    @BeforeEach
    public void setup() throws Exception
    {
        connection = LdapApiIntegrationUtils.getPooledAdminConnection( getLdapServer() );
    }


    @AfterEach
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
        LdapConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );
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


    /**
     * Test for DIRAPI-342: Unbind breaks connection
     */
    @Test
    public void testBindAndUnbindLoop() throws Exception
    {
        try ( LdapConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME,
            getLdapServer().getPort() ) )
        {
            for ( int i = 0; i < 100; i++ )
            {
                connection.bind( "uid=admin,ou=system", "secret" );
                assertTrue( connection.isAuthenticated() );

                connection.unBind();
                assertFalse( connection.isAuthenticated() );
            }
        }
    }


    /**
     * Test for DIRAPI-342: Unbind breaks connection
     */
    @Test
    public void testBindAndCloseLoop() throws Exception
    {
        try ( LdapConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME,
            getLdapServer().getPort() ) )
        {
            for ( int i = 0; i < 100; i++ )
            {
                connection.bind( "uid=admin,ou=system", "secret" );
                assertTrue( connection.isAuthenticated() );

                connection.close();
                assertFalse( connection.isAuthenticated() );
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

        EqualityNode<String> filter = new EqualityNode<String>( SchemaConstants.ENTRY_UUID_AT, uuid );

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
        config.setLdapHost( Network.LOOPBACK_HOSTNAME );
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
        assertEquals( manager.getAllSchemas().size(), manager.getEnabled().size() );
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
        LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );

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
        config.setLdapHost( Network.LOOPBACK_HOSTNAME );
        config.setLdapPort( ldapServer.getPort() );
        config.setBinaryAttributeDetector( new DefaultConfigurableBinaryAttributeDetector() );

        LdapConnection ldapConnection = new LdapNetworkConnection( config );

        ldapConnection.bind( "uid=admin,ou=system", "secret" );

        // Try to retrieve a binary attribute : it should be seen as a byte[]
        Entry entry = ldapConnection.lookup( "uid=admin,ou=system" );
        assertFalse( entry.get( SchemaConstants.USER_PASSWORD_AT ).get().isHumanReadable() );

        ldapConnection.close();
    }


    // TODO @Test(expected = InvalidConnectionException.class)
    public void testConnectionWrongHost() throws LdapException, IOException
    {
        try ( LdapConnection connection = new LdapNetworkConnection( "notexisting", 1234 ) )
        {
            connection.setTimeOut( 10000L );
            connection.connect();
        }
        catch ( Exception e )
        {
            assertThat( e, is( instanceOf( InvalidConnectionException.class ) ) );
            assertThat( e.getMessage(), containsString( "ERR_04121_CANNOT_RESOLVE_HOSTNAME" ) );
            assertThat( e.getMessage(), containsString( "notexisting" ) );
            throw e;
        }
    }


    // TODO @Test(expected = InvalidConnectionException.class)
    public void testConnectionWrongPort() throws LdapException, IOException
    {
        try ( LdapConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, 123 ) )
        {
            connection.setTimeOut( 10000L );
            connection.connect();
        }
        catch ( Exception e )
        {
            assertThat( e, is( instanceOf( InvalidConnectionException.class ) ) );
            assertThat( e.getMessage(), containsString( "ERR_04110_CANNOT_CONNECT_TO_SERVER" ) );
            assertThat( e.getMessage(), containsString( "Connection refused" ) );
            throw e;
        }
    }


    @Test
    public void testConfigSetting() throws LdapException, IOException
    {
        LdapConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, ldapServer.getPort() );
        connection.connect();

        connection.close();
    }

}
