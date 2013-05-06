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

package org.apache.directory.server.replication;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.Csn;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.junit.tools.MultiThreadedMultiInvoker;
import org.apache.directory.server.annotations.CreateConsumer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumer;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumerImpl;
import org.apache.directory.server.ldap.replication.provider.SyncReplRequestHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;


/**
 * Tests for replication subsystem in client-server mode.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ClientServerReplicationIT
{
    @Rule
    public MultiThreadedMultiInvoker i = new MultiThreadedMultiInvoker( MultiThreadedMultiInvoker.NOT_THREADSAFE );

    private static LdapServer providerServer;

    private static LdapServer consumerServer;

    private static SchemaManager schemaManager;

    private static CoreSession providerSession;

    private static CoreSession consumerSession;

    private static AtomicInteger entryCount = new AtomicInteger();


    @BeforeClass
    public static void setUp() throws Exception
    {
        Class.forName( FrameworkRunner.class.getName() );
        CountDownLatch counter = new CountDownLatch( 2 );

        startProvider( counter );
        startConsumer( counter );

        // Wait for the two servers to be up and running
        counter.await();
    }


    @AfterClass
    public static void tearDown() throws Exception
    {
        consumerServer.stop();
        consumerServer.getDirectoryService().shutdown();
        providerServer.stop();
        providerServer.getDirectoryService().shutdown();
    }


    private void dump( CoreSession session, Dn entryDn )
    {
        try
        {
            SearchRequest searchRequest = new SearchRequestImpl();

            searchRequest.setBase( new Dn( schemaManager, "dc=example,dc=com" ) );
            searchRequest.setFilter( "(objectClass=*)" );
            searchRequest.setScope( SearchScope.SUBTREE );
            searchRequest.addAttributes( "entryUuid" );

            System.out.println( "-----------> Dumping the server <-----------" );
            System.out.println( "-----------> Looking for " + entryDn.getNormName() + " <-----------" );

            EntryFilteringCursor cursor = session.search( searchRequest );

            while ( cursor.next() )
            {
                Entry entry = cursor.get();

                if ( entry.getDn().equals( entryDn ) )
                {
                    System.out.println( "The searched entry exists !!!" );
                    System.out.println( "found Entry " + entry.getDn().getNormName() + " exists, entrtyUuid = "
                        + entry.get( "entryUuid" ) );
                    continue;
                }

                System.out.println( "Entry " + entry.getDn().getNormName() + " exists, entrtyUuid = "
                    + entry.get( "entryUuid" ) );
            }

            cursor.close();

            System.out.println( "-----------> Dump done <-----------" );
        }
        catch ( Exception le )
        {
            // Do nothing
            le.printStackTrace();
        }
    }


    /**
     * Check that the entry was replicated to the target server. That is the case when the entry exists on the target
     * server and its entryCSN is greater than or equal compared to the source entry. We wait up to 10 seconds, 
     * by 100ms steps, until either the entry s found, or we have exhausted the 10 seconds delay.
     */
    private boolean checkEntryReplicated( Dn entryDn ) throws Exception
    {
        return checkEntryReplicated( entryDn, false );
    }


    /**
     * Check that the entry was replicated to the target server. That is the case when the entry exists on the target
     * server and its entryCSN is greater than or equal compared to the source entry. We wait up to 10 seconds, 
     * by 100ms steps, until either the entry s found, or we have exhausted the 10 seconds delay.
     */
    private boolean checkEntryReplicated( Dn entryDn, boolean print ) throws Exception
    {
        boolean replicated = false;

        for ( int i = 0; i < 100; i++ )
        {
            Thread.sleep( 50 );

            if ( consumerSession.exists( entryDn ) )
            {
                if ( print )
                {
                    System.out.println( entryDn.getName() + " exists " );
                }

                Entry providerEntry = providerSession.lookup( entryDn, "*", "+" );
                Entry consumerEntry = consumerSession.lookup( entryDn, "*", "+" );
                Csn providerCSN = new Csn( providerEntry.get( SchemaConstants.ENTRY_CSN_AT ).getString() );
                Csn consumerCSN = new Csn( consumerEntry.get( SchemaConstants.ENTRY_CSN_AT ).getString() );

                if ( consumerCSN.compareTo( providerCSN ) >= 0 )
                {
                    if ( print )
                    {
                        System.out.println( entryDn.getName() + " replicated " );
                    }

                    replicated = true;
                    break;
                }
            }

            Thread.sleep( 50 );
        }

        if ( replicated == false )
        {
            dump( consumerSession, entryDn );
        }

        return replicated;
    }


    /**
     * Check that the entry exists and has been deleted in the target server. We wait up to 10 seconds, by
     * 100ms steps, until either the entry is deleted, or we have exhausted the 10 seconds delay,
     * or the entry was never found to start with.
     */
    private boolean checkEntryDeletion( CoreSession session, Dn entryDn ) throws Exception
    {
        boolean exists = session.exists( entryDn );

        if ( !exists )
        {
            return true;
        }

        for ( int i = 0; i < 100; i++ )
        {
            Thread.sleep( 100 );

            exists = session.exists( entryDn );

            if ( !exists )
            {
                return true;
            }

            Thread.sleep( 100 );
        }

        dump( session, entryDn );

        return false;
    }


    @Test
    public void testModify() throws Exception
    {
        Entry provUser = createEntry();

        // precondition: entry does not exist
        assertFalse( providerSession.exists( provUser.getDn() ) );
        assertFalse( consumerSession.exists( provUser.getDn() ) );

        // add the entry and check it is replicated
        System.out.println( ">--------------------------------------- Adding " + provUser );
        providerSession.add( provUser );
        System.out.println( ">--------------------------------------- Added " );

        assertTrue( providerSession.exists( provUser.getDn() ) );
        assertTrue( checkEntryReplicated( provUser.getDn() ) );

        // modify the entry and check it is replicated
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( provUser.getDn() );
        modReq.add( "userPassword", "secret" );

        System.out.println( ">--------------------------------------- Modifying " + modReq );
        providerSession.modify( modReq );
        System.out.println( ">--------------------------------------- Modified " );

        assertTrue( checkEntryReplicated( provUser.getDn() ) );
        compareEntries( provUser.getDn() );
    }


    @Test
    public void testModDn() throws Exception
    {
        Entry provUser = createEntry();
        Dn userDn = provUser.getDn();

        assertFalse( consumerSession.exists( userDn ) );

        // Add entry "cn=entryN,dc=example,dc=com" and check it is replicated
        providerSession.add( provUser );

        assertTrue( checkEntryReplicated( userDn ) );

        // Add container for users "ou=users,dc=example,dc=com" and check it is replicated
        Dn usersContainerDn = new Dn( schemaManager, "ou=users,dc=example,dc=com" );

        Entry userContainer = new DefaultEntry( schemaManager, usersContainerDn,
            "objectClass: organizationalUnit",
            "ou: users" );

        providerSession.add( userContainer );

        assertTrue( checkEntryReplicated( usersContainerDn ) );
        compareEntries( userContainer.getDn() );

        // Move entry "cn=entryN,dc=example,dc=com" to "ou=users,dc=example,dc=com" and check it is replicated
        providerSession.move( userDn, usersContainerDn );

        // The moved entry : "cn=entryN,ou=users,dc=example,dc=com"
        Dn movedEntryDn = usersContainerDn.add( userDn.getRdn() );

        assertTrue( checkEntryReplicated( movedEntryDn ) );
        compareEntries( movedEntryDn );

        // Rename "cn=entryN,ou=users,dc=example,dc=com" to "cn=entryNrenamed,ou=users,dc=example,dc=com" and check it is replicated
        Rdn newName = new Rdn( schemaManager, movedEntryDn.getRdn().getName() + "renamed" );
        providerSession.rename( movedEntryDn, newName, true );

        Dn renamedEntryDn = usersContainerDn.add( newName );

        assertTrue( checkEntryReplicated( renamedEntryDn ) );
        compareEntries( renamedEntryDn );

        // now move and rename
        Dn newParent = usersContainerDn.getParent();

        newName = new Rdn( schemaManager, renamedEntryDn.getRdn().getName() + "MovedAndRenamed" );

        // Move and rename "cn=entryNrenamed,ou=users,dc=example,dc=com" to
        // "cn=entryNMovedAndRenamed,dc=example,dc=com"  and check it is replicated
        providerSession.moveAndRename( renamedEntryDn, newParent, newName, false );

        Dn movedAndRenamedEntryDn = newParent.add( newName );

        assertTrue( checkEntryReplicated( movedAndRenamedEntryDn ) );
        compareEntries( movedAndRenamedEntryDn );

        // cleanup
        providerSession.delete( usersContainerDn );
    }


    @Test
    @Ignore("Run this test alone, otherwise it conflicts with moddn")
    public void testModDnLoop() throws Exception
    {
        for ( int i = 0; i < 10; i++ )
        {
            System.out.println( ">>>>>> loop " + ( i + 1 ) + " <<<<<<" );
            Entry newuser = createEntry();

            assertFalse( consumerSession.exists( newuser.getDn() ) );

            // Add entry : "cn=entryN,dc=example,dc=com"
            providerSession.add( newuser ); // 1

            Dn usersContainer = new Dn( schemaManager, "ou=users,dc=example,dc=com" );

            DefaultEntry usersEntry = new DefaultEntry( schemaManager, usersContainer,
                "objectClass: organizationalUnit",
                "ou: users" );

            // Add entry "ou=users,dc=example,dc=com"
            providerSession.add( usersEntry ); // 2

            assertTrue( checkEntryReplicated( usersContainer ) );
            compareEntries( usersEntry.getDn() );

            // Move entry "cn=entryN,dc=example,dc=com" to "ou=users,dc=example,dc=com"
            Dn userDn = newuser.getDn();
            providerSession.move( userDn, usersContainer );

            // The moved entry : "cn=entryN,ou=users,dc=example,dc=com"
            Dn movedEntryDn = usersContainer.add( userDn.getRdn() );

            assertTrue( checkEntryReplicated( movedEntryDn ) );
            compareEntries( movedEntryDn );

            Rdn newName = new Rdn( schemaManager, movedEntryDn.getRdn().getName() + "renamed" );

            // Rename "cn=entryN,ou=users,dc=example,dc=com" to "cn=entryNrenamed,ou=users,dc=example,dc=com"
            providerSession.rename( movedEntryDn, newName, true );

            Dn renamedEntryDn = usersContainer.add( newName );

            assertTrue( checkEntryReplicated( renamedEntryDn ) );
            compareEntries( renamedEntryDn );

            // now move and rename
            Dn newParent = usersContainer.getParent();

            newName = new Rdn( schemaManager, renamedEntryDn.getRdn().getName() + "MovedAndRenamed" );

            // Move and rename "cn=entryNrenamed,ou=users,dc=example,dc=com" to
            // "cn=entryNMovedAndRenamed,dc=example,dc=com"
            providerSession.moveAndRename( renamedEntryDn, newParent, newName, false ); //4

            Dn movedAndRenamedEntry = newParent.add( newName );

            assertTrue( checkEntryReplicated( movedAndRenamedEntry ) );
            compareEntries( movedAndRenamedEntry );

            // Ok, no failure, revert everything
            providerSession.delete( movedAndRenamedEntry );
            providerSession.delete( usersContainer );
        }
    }


    /**
     * Test the replication of a deleted entry
     */
    @Test
    public void testDelete() throws Exception
    {
        Entry provUser = createEntry();

        providerSession.add( provUser );

        assertTrue( checkEntryReplicated( provUser.getDn() ) );
        compareEntries( provUser.getDn() );

        assertTrue( providerSession.exists( provUser.getDn() ) );
        assertTrue( consumerSession.exists( provUser.getDn() ) );

        providerSession.delete( provUser.getDn() );

        assertTrue( checkEntryDeletion( consumerSession, provUser.getDn() ) );
        assertFalse( providerSession.exists( provUser.getDn() ) );
    }


    private Entry restartConsumer( Entry provUser ) throws Exception
    {
        System.out.println( "------------------------------------- Stop consumer" );
        // Now stop the consumer
        consumerServer.stop();

        // And delete the entry in the provider
        Dn deletedUserDn = provUser.getDn();

        providerSession.delete( deletedUserDn );

        // Create a new entry
        provUser = createEntry();
        Dn addedUserDn = provUser.getDn();

        providerSession.add( provUser );

        // let the provider log the events before the consumer sends a request
        // we are dealing with fraction of seconds cause of the programmatic simulation
        // it is impossible in the real world scenario
        Thread.sleep( 1000 );

        // Restart the consumer
        System.out.println( "------------------------------------- Start consumer" );
        consumerServer.start();

        assertTrue( checkEntryDeletion( consumerSession, deletedUserDn ) );

        assertTrue( checkEntryReplicated( addedUserDn ) );
        compareEntries( addedUserDn );

        return provUser;
    }


    @Test
    public void testRebootConsumer() throws Exception
    {
        Entry provUser = createEntry();

        assertFalse( providerSession.exists( provUser.getDn() ) );
        assertFalse( consumerSession.exists( provUser.getDn() ) );

        providerSession.add( provUser );

        assertTrue( checkEntryReplicated( provUser.getDn() ) );
        compareEntries( provUser.getDn() );

        assertTrue( providerSession.exists( provUser.getDn() ) );
        assertTrue( consumerSession.exists( provUser.getDn() ) );

        for ( int i = 0; i < 10; i++ )
        {
            provUser = restartConsumer( provUser );
        }
    }


    private void compareEntries( Dn dn ) throws Exception
    {
        String[] searchAttributes = new String[]
            {
                SchemaConstants.ALL_USER_ATTRIBUTES,
                SchemaConstants.ENTRY_UUID_AT
        };

        Entry providerEntry = providerSession.lookup( dn, searchAttributes );
        Entry consumerEntry = consumerSession.lookup( dn, searchAttributes );

        assertEquals( providerEntry, consumerEntry );
    }


    private Entry createEntry() throws Exception
    {
        String user = "user" + entryCount.incrementAndGet();

        String dn = "cn=" + user + ",dc=example,dc=com";

        DefaultEntry entry = new DefaultEntry( schemaManager, dn,
            "objectClass", "person",
            "cn", user,
            "sn", user );

        return entry;
    }


    @CreateDS(
        allowAnonAccess = true,
        name = "provider-replication",
        enableChangeLog = false,
        partitions =
            {
                @CreatePartition(
                    name = "example",
                    suffix = "dc=example,dc=com",
                    indexes =
                        {
                            @CreateIndex(attribute = "objectClass"),
                            @CreateIndex(attribute = "dc"),
                            @CreateIndex(attribute = "ou")
                    },
                    contextEntry = @ContextEntry(entryLdif =
                        "dn: dc=example,dc=com\n" +
                            "objectClass: domain\n" +
                            "dc: example"))
        })
    @CreateLdapServer(transports =
        { @CreateTransport(port = 16000, protocol = "LDAP") })
    public static void startProvider( final CountDownLatch counter ) throws Exception
    {
        DirectoryService provDirService = DSAnnotationProcessor.getDirectoryService();

        providerServer = ServerAnnotationProcessor.getLdapServer( provDirService );
        providerServer.setReplicationReqHandler( new SyncReplRequestHandler() );
        providerServer.startReplicationProducer();

        Runnable r = new Runnable()
        {

            public void run()
            {
                try
                {
                    schemaManager = providerServer.getDirectoryService().getSchemaManager();
                    providerSession = providerServer.getDirectoryService().getAdminSession();
                    counter.countDown();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        };

        Thread t = new Thread( r );
        t.setDaemon( true );
        t.start();
        t.join();
    }


    @CreateDS(
        allowAnonAccess = true,
        enableChangeLog = false,
        name = "consumer-replication",
        partitions =
            {
                @CreatePartition(
                    name = "example",
                    suffix = "dc=example,dc=com",
                    indexes =
                        {
                            @CreateIndex(attribute = "objectClass"),
                            @CreateIndex(attribute = "dc"),
                            @CreateIndex(attribute = "ou")
                    },
                    contextEntry = @ContextEntry(entryLdif =
                        "dn: dc=example,dc=com\n" +
                            "objectClass: domain\n" +
                            "dc: example"))
        })
    @CreateLdapServer(transports =
        { @CreateTransport(port = 17000, protocol = "LDAP") })
    @CreateConsumer
        (
            remoteHost = "localhost",
            remotePort = 16000,
            replUserDn = "uid=admin,ou=system",
            replUserPassword = "secret",
            useTls = false,
            baseDn = "dc=example,dc=com",
            refreshInterval = 1000,
            replicaId = 1
        )
        public static void startConsumer( final CountDownLatch counter ) throws Exception
    {
        DirectoryService provDirService = DSAnnotationProcessor.getDirectoryService();
        consumerServer = ServerAnnotationProcessor.getLdapServer( provDirService );

        final ReplicationConsumerImpl consumer = ( ReplicationConsumerImpl ) ServerAnnotationProcessor.createConsumer();

        List<ReplicationConsumer> replConsumers = new ArrayList<ReplicationConsumer>();
        replConsumers.add( consumer );

        consumerServer.setReplConsumers( replConsumers );
        consumerServer.startReplicationConsumers();

        Runnable r = new Runnable()
        {
            public void run()
            {
                try
                {
                    DirectoryService ds = consumerServer.getDirectoryService();

                    Dn configDn = new Dn( ds.getSchemaManager(), "ads-replConsumerId=localhost,ou=system" );
                    consumer.getConfig().setConfigEntryDn( configDn );

                    Entry provConfigEntry = new DefaultEntry( ds.getSchemaManager(), configDn,
                        "objectClass: ads-replConsumer",
                        "ads-replConsumerId: localhost",
                        "ads-searchBaseDN", consumer.getConfig().getBaseDn(),
                        "ads-replProvHostName", consumer.getConfig().getRemoteHost(),
                        "ads-replProvPort", String.valueOf( consumer.getConfig().getRemotePort() ),
                        "ads-replRefreshInterval", String.valueOf( consumer.getConfig().getRefreshInterval() ),
                        "ads-replRefreshNPersist", String.valueOf( consumer.getConfig().isRefreshNPersist() ),
                        "ads-replSearchScope", consumer.getConfig().getSearchScope().getLdapUrlValue(),
                        "ads-replSearchFilter", consumer.getConfig().getFilter(),
                        "ads-replSearchSizeLimit", String.valueOf( consumer.getConfig().getSearchSizeLimit() ),
                        "ads-replSearchTimeOut", String.valueOf( consumer.getConfig().getSearchTimeout() ),
                        "ads-replUserDn", consumer.getConfig().getReplUserDn(),
                        "ads-replUserPassword", consumer.getConfig().getReplUserPassword() );

                    provConfigEntry.put( "ads-replAliasDerefMode", consumer.getConfig().getAliasDerefMode()
                        .getJndiValue() );
                    provConfigEntry.put( "ads-replAttributes", consumer.getConfig().getAttributes() );

                    consumerSession = consumerServer.getDirectoryService().getAdminSession();
                    consumerSession.add( provConfigEntry );
                    counter.countDown();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        };

        Thread t = new Thread( r );
        t.setDaemon( true );
        t.start();
        t.join();
    }
}
