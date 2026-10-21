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
package org.apache.directory.server.operations.search;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultDone;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.message.controls.PersistentSearch;
import org.apache.directory.api.ldap.model.message.controls.PersistentSearchImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.future.SearchFuture;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Checks that the entries sent back to a persistent search client are subject to
 * the same authentication and authorization checks as a normal search.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateDS(
    name = "PersistentSearchAuthorizationIT",
    enableAccessControl = true,
    allowAnonAccess = false)
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP")
    })
@ApplyLdifs(
    {
        "dn: uid=billyd,ou=users,ou=system",
        "objectClass: top",
        "objectClass: person",
        "objectClass: organizationalPerson",
        "objectClass: inetOrgPerson",
        "uid: billyd",
        "cn: Billy D",
        "sn: D",
        "userPassword: billyd"
})
public class PersistentSearchAuthorizationIT extends AbstractLdapTestUnit
{
    private static final String BASE = "ou=system";
    private static final String VICTIM_DN = "cn=Victim,ou=system";

    /** The time we wait for a notification which should not come */
    private static final long NO_RESPONSE_WAIT_MS = 2000L;

    /** The time we wait for a notification which should come */
    private static final long RESPONSE_WAIT_MS = 10000L;

    private LdapNetworkConnection adminConnection;
    private LdapNetworkConnection userConnection;


    @BeforeEach
    public void setUp() throws Exception
    {
        adminConnection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );
        adminConnection.bind( "uid=admin,ou=system", "secret" );

        userConnection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME, getLdapServer().getPort() );
    }


    @AfterEach
    public void tearDown() throws Exception
    {
        if ( adminConnection.exists( VICTIM_DN ) )
        {
            adminConnection.delete( VICTIM_DN );
        }

        adminConnection.close();
        userConnection.close();
    }


    private SearchFuture startPersistentSearch( LdapNetworkConnection connection ) throws Exception
    {
        SearchRequest req = new SearchRequestImpl();
        req.setBase( new Dn( BASE ) );
        req.setScope( SearchScope.SUBTREE );
        req.setFilter( "(objectClass=*)" );
        req.addAttributes( "*" );

        PersistentSearch psearch = new PersistentSearchImpl();
        psearch.setChangesOnly( true );
        psearch.setReturnECs( false );
        psearch.setChangeTypes( 15 );
        req.addControl( psearch );

        return connection.searchAsync( req );
    }


    private Response nextResponse( SearchFuture future, long waitMs ) throws Exception
    {
        // Returns null if nothing has been received before the timeout
        return future.get( waitMs, TimeUnit.MILLISECONDS );
    }


    /**
     * Wait for the registration of the persistent search listeners
     */
    private void waitForListeners( int expected ) throws Exception
    {
        long deadline = System.currentTimeMillis() + RESPONSE_WAIT_MS;

        while ( getService().getEventService().getRegistrationEntries().size() < expected )
        {
            assertTrue( System.currentTimeMillis() < deadline, "Persistent search listeners not registered" );
            Thread.sleep( 50 );
        }
    }


    private void addVictim() throws Exception
    {
        adminConnection.add( new DefaultEntry( VICTIM_DN,
            "objectClass: top",
            "objectClass: person",
            "cn: Victim",
            "sn: Victim",
            "userPassword: s3cr3t" ) );
    }


    private void createReadAciWithoutPassword() throws Exception
    {
        adminConnection.modify( BASE, new DefaultModification( ModificationOperation.ADD_ATTRIBUTE,
            "administrativeRole", "accessControlSpecificArea" ) );

        adminConnection.add( new DefaultEntry( "cn=psearchAci,ou=system",
            "objectClass: top",
            "objectClass: subentry",
            "objectClass: accessControlSubentry",
            "subtreeSpecification: {}",
            "prescriptiveACI: { identificationTag \"readAll\", precedence 10, authenticationLevel simple, "
                + "itemOrUserFirst userFirst: { userClasses { allUsers }, userPermissions { "
                + "{ protectedItems { entry, allUserAttributeTypesAndValues }, "
                + "grantsAndDenials { grantRead, grantReturnDN, grantBrowse } }, "
                + "{ protectedItems { attributeType { userPassword }, allAttributeValues { userPassword } }, "
                + "grantsAndDenials { denyRead, denyBrowse } } } } }" ) );
    }


    /**
     * A user without any read permission must not be notified of the changes made
     * by the administrator, whereas the administrator is.
     */
    @Test
    public void testChangesOnlyWithoutReadPermission() throws Exception
    {
        userConnection.bind( "uid=billyd,ou=users,ou=system", "billyd" );
        int initialListeners = getService().getEventService().getRegistrationEntries().size();

        SearchFuture userFuture = startPersistentSearch( userConnection );
        SearchFuture adminFuture = startPersistentSearch( adminConnection );
        waitForListeners( initialListeners + 2 );

        addVictim();
        adminConnection.modify( VICTIM_DN, new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
            "userPassword", "n3wS3cr3t" ) );
        adminConnection.delete( VICTIM_DN );

        // The administrator gets the notifications...
        Response adminResponse = nextResponse( adminFuture, RESPONSE_WAIT_MS );
        assertNotNull( adminResponse );
        assertTrue( adminResponse instanceof SearchResultEntry );
        assertEquals( VICTIM_DN, ( ( SearchResultEntry ) adminResponse ).getObjectName().getName() );

        // ... but not the user, who is not allowed to read anything
        assertNull( nextResponse( userFuture, NO_RESPONSE_WAIT_MS ) );
    }


    /**
     * A user allowed to read entries, but not the userPassword attribute, must be
     * notified of the changes without the userPassword attribute.
     */
    @Test
    public void testChangesOnlyHonoursAci() throws Exception
    {
        createReadAciWithoutPassword();

        userConnection.bind( "uid=billyd,ou=users,ou=system", "billyd" );
        int initialListeners = getService().getEventService().getRegistrationEntries().size();

        SearchFuture userFuture = startPersistentSearch( userConnection );
        waitForListeners( initialListeners + 1 );

        addVictim();

        Response response = nextResponse( userFuture, RESPONSE_WAIT_MS );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );

        Entry added = ( ( SearchResultEntry ) response ).getEntry();
        assertEquals( VICTIM_DN, added.getDn().getName() );
        assertTrue( added.contains( "sn", "Victim" ) );
        assertFalse( added.containsAttribute( "userPassword" ) );

        // The deletion is also notified, without the password
        adminConnection.delete( VICTIM_DN );

        response = nextResponse( userFuture, RESPONSE_WAIT_MS );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultEntry );

        Entry deleted = ( ( SearchResultEntry ) response ).getEntry();
        assertEquals( VICTIM_DN, deleted.getDn().getName() );
        assertFalse( deleted.containsAttribute( "userPassword" ) );
    }


    /**
     * An anonymous client must not be able to register a persistent search when
     * anonymous access is disabled.
     */
    @Test
    public void testChangesOnlyAnonymousRejected() throws Exception
    {
        userConnection.connect();
        int initialListeners = getService().getEventService().getRegistrationEntries().size();

        SearchFuture future = startPersistentSearch( userConnection );

        Response response = nextResponse( future, RESPONSE_WAIT_MS );
        assertNotNull( response );
        assertTrue( response instanceof SearchResultDone );
        assertNotEquals( ResultCodeEnum.SUCCESS, ( ( SearchResultDone ) response ).getLdapResult().getResultCode() );
        assertEquals( initialListeners, getService().getEventService().getRegistrationEntries().size() );

        addVictim();

        assertNull( nextResponse( future, NO_RESPONSE_WAIT_MS ) );
    }


    /**
     * The notifications must not make a write operation fail, even when there are
     * more listeners than the event executor can run at once.
     */
    @Test
    public void testManyListenersDoNotBreakWrites() throws Exception
    {
        int initialListeners = getService().getEventService().getRegistrationEntries().size();
        List<LdapNetworkConnection> connections = new ArrayList<>();

        try
        {
            for ( int i = 0; i < 4; i++ )
            {
                LdapNetworkConnection connection = new LdapNetworkConnection( Network.LOOPBACK_HOSTNAME,
                    getLdapServer().getPort() );
                connection.bind( "uid=admin,ou=system", "secret" );
                connections.add( connection );

                for ( int j = 0; j < 40; j++ )
                {
                    startPersistentSearch( connection );
                }
            }

            waitForListeners( initialListeners + 160 );

            addVictim();
            assertTrue( adminConnection.exists( VICTIM_DN ) );
        }
        finally
        {
            for ( LdapNetworkConnection connection : connections )
            {
                connection.close();
            }
        }
    }
}
