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


import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.directory.junit.tools.MultiThreadedMultiInvoker;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.MockDirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.replication.SyncReplConfiguration;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumer;
import org.apache.directory.server.ldap.replication.provider.SyncReplRequestHandler;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;


/**
 * Tests the initial refresh of a client
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ClientInitialRefreshIT
{
    @Rule
    public MultiThreadedMultiInvoker i = new MultiThreadedMultiInvoker( MultiThreadedMultiInvoker.NOT_THREADSAFE );

    private static LdapServer providerServer;

    private static SchemaManager schemaManager;

    private static CoreSession providerSession;

    private static AtomicInteger entryCount = new AtomicInteger();

    private static final int INSERT_COUNT = 10;
    
    private static final int TOTAL_COUNT = INSERT_COUNT + 1;

    private static File cookiesDir;
    
    @BeforeClass
    public static void setUp() throws Exception
    {
        Class<?> justLoadToSetControlProperties = Class.forName( FrameworkRunner.class.getName() );

        startProvider();

        // Load 1000 entries
        for ( int i = 0; i < INSERT_COUNT; i++ )
        {
            Entry entry = createEntry();

            providerSession.add( entry );
        }
        
        cookiesDir = new File( FileUtils.getTempDirectory(), MockSyncReplConsumer.COOKIES_DIR_NAME );
    }

    
    @Before
    @After
    public void deleteCookies() throws IOException
    {
        if( cookiesDir.exists() )
        {
            FileUtils.cleanDirectory( cookiesDir );
        }
    }

    
    @AfterClass
    public static void tearDown() throws Exception
    {
        providerServer.stop();
        providerServer.getDirectoryService().shutdown();

        FileUtils.deleteDirectory( providerServer.getDirectoryService().getInstanceLayout().getInstanceDirectory() );
        FileUtils.deleteDirectory( cookiesDir );
    }


    private static Entry createEntry() throws Exception
    {
        String user = "user" + entryCount.incrementAndGet();

        String dn = "cn=" + user + ",dc=example,dc=com";

        DefaultEntry entry = new DefaultEntry( schemaManager, dn,
            "objectClass", "person",
            "cn", user,
            "sn", user );

        return entry;
    }


    @CreateDS(allowAnonAccess = true, name = "provider-replication", partitions =
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
    public static void startProvider() throws Exception
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


    /**
     * Wait for the expected number of entries to be added into the client
     */
    private boolean waitForSyncReplClient( ReplicationConsumer consumer, int expected ) throws Exception
    {
        System.out.println( "\nNbAdded every 100ms : " );
        boolean isFirst = true;

        for ( int i = 0; i < 50; i++ )
        {
            int nbAdded = ( ( MockSyncReplConsumer ) consumer ).getNbAdded();

            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                System.out.print( ", " );
            }

            System.out.print( nbAdded );

            if ( nbAdded == expected )
            {
                return true;
            }

            Thread.sleep( 100 );
        }

        return false;
    }


    /**
     * Wait for the expected number of entries to be added into the client
     */
    private boolean waitUntilLimitSyncReplClient( int limit, ReplicationConsumer... consumers ) throws Exception
    {
        System.out.println( "\nCompleted so far : " );
        int nbConsumers = consumers.length;
        int[] nbAddeds = new int[nbConsumers];
        int nbCompleted = 0;

        for ( int i = 0; i < 50; i++ )
        {
            for ( int j = 0; j < nbConsumers; j++ )
            {
                if ( nbAddeds[j] != limit )
                {
                    nbAddeds[j] = ( ( MockSyncReplConsumer ) consumers[j] ).getNbAdded();

                    if ( nbAddeds[j] >= limit )
                    {
                        nbCompleted++;
                        System.out.println( "(consumer" + ( j + 1 ) + " completed) " );
                    }
                }
            }

            if ( nbCompleted == nbConsumers )
            {
                return true;
            }

            Thread.sleep( 100 );
        }

        return false;
    }


    private ReplicationConsumer createConsumer() throws Exception
    {
        final ReplicationConsumer syncreplClient = new MockSyncReplConsumer();
        final SyncReplConfiguration config = new SyncReplConfiguration();
        config.setRemoteHost( "localhost" );
        config.setRemotePort( 16000 );
        config.setReplUserDn( "uid=admin,ou=system" );
        config.setReplUserPassword( "secret".getBytes() );
        config.setUseTls( false );
        config.setBaseDn( "dc=example,dc=com" );
        config.setRefreshInterval( 1000 );

        syncreplClient.setConfig( config );

        assertTrue( true );

        Runnable consumerTask = new Runnable()
        {
            public void run()
            {
                try
                {
                    String baseDn = config.getBaseDn();

                    SearchRequest searchRequest = new SearchRequestImpl();

                    searchRequest.setBase( new Dn( baseDn ) );
                    searchRequest.setFilter( config.getFilter() );
                    searchRequest.setSizeLimit( config.getSearchSizeLimit() );
                    searchRequest.setTimeLimit( config.getSearchTimeout() );

                    searchRequest.setDerefAliases( config.getAliasDerefMode() );
                    searchRequest.setScope( config.getSearchScope() );
                    searchRequest.setTypesOnly( false );

                    searchRequest.addAttributes( config.getAttributes() );

                    DirectoryService directoryService = new MockDirectoryService();
                    directoryService.setSchemaManager( schemaManager );
                    ( ( MockSyncReplConsumer ) syncreplClient ).init( directoryService );
                    syncreplClient.start();
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( e );
                }
            }
        };

        Thread consumerThread = new Thread( consumerTask );
        consumerThread.setDaemon( true );
        consumerThread.start();

        return syncreplClient;
    }


    /**
     * Launch the consumer in a separated thread.
     */
    private void runConsumer( final ReplicationConsumer consumer ) throws Exception
    {
        Runnable consumerTask = new Runnable()
        {
            public void run()
            {
                try
                {
                    consumer.start();
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( e );
                }
            }
        };

        Thread consumerThread = new Thread( consumerTask );
        consumerThread.setDaemon( true );
        consumerThread.start();
    }


    /**
     * First test : create a consumer, and see if it gets the 1000 entries
     */
    @Test
    public void testInitialRefreshLoad() throws Exception
    {
        System.out.println( "\n---> Running testInitialRefreshLoad" );

        ReplicationConsumer consumer = createConsumer();

        // We should have 1000 entries plus the base entry = TOTAL_COUNT
        assertTrue( waitForSyncReplClient( consumer, TOTAL_COUNT ) );
        consumer.stop();

        System.out.println( "\n<-- Done" );
    }


    /**
     * Test that we can load entries, then add one entry in the producer
     * and see this entry present in the consumer
     */
    @Test
    public void testInitialRefreshLoadAndAdd() throws Exception
    {
        System.out.println( "\n---> Running testInitialRefreshLoadAndAdd" );

        ReplicationConsumer consumer = createConsumer();

        // We should have INSERT_COUNT entries plus the base entry = TOTAL_COUNT
        assertTrue( waitForSyncReplClient( consumer, TOTAL_COUNT ) );

        // Reset the added counter
        ( ( MockSyncReplConsumer ) consumer ).resetNbAdded();

        // Inject a new entry in the producer
        Entry addedEntry = createEntry();
        providerSession.add( addedEntry );

        // Now check that the entry has been copied in the consumer
        assertTrue( waitForSyncReplClient( consumer, 1 ) );

        // Removed the added entry
        providerSession.delete( addedEntry.getDn() );
        consumer.stop();

        System.out.println( "\n<-- Done" );
    }


    /**
     * Test that we can load entries, kill the consumer <b>after</b> the load (cause we cannot tell the producer when to stop),
     * add some more entries and restart the consumer and get the remaining entries added when the consumer was down.
     */
    @Test
    public void testInitialRefreshStopAndGo() throws Exception
    {
        System.out.println( "\n---> Running testInitialRefreshStopAndGo" );

        ReplicationConsumer consumer = createConsumer();

        // Load all the entries
        waitUntilLimitSyncReplClient( TOTAL_COUNT, consumer );

        // Stop the consumer
        consumer.stop();

        int additionalCount = 10;
        List<Dn> newEntries = new ArrayList<Dn>();
        for( int i = 0; i < additionalCount; i++ )
        {
            // Inject a new entry in the producer
            Entry addedEntry = createEntry();
            providerSession.add( addedEntry );
            newEntries.add( addedEntry.getDn() );
        }

        // Start it again
        runConsumer( consumer );

        // We should get only the additional values cause consumer sends a cookie now
        assertTrue( waitForSyncReplClient( consumer, additionalCount ) );
        consumer.stop();

        for( Dn dn : newEntries )
        {
            providerSession.delete( dn );
        }
        
        System.out.println( "\n<-- Done" );
    }


    /**
     * Test with 2 consumers
     */
    @Test
    public void testInitialRefresh4Consumers() throws Exception
    {
        System.out.println( "\n--->Running testInitialRefresh4Consumers" );

        ReplicationConsumer consumer1 = createConsumer();
        ReplicationConsumer consumer2 = createConsumer();
        ReplicationConsumer consumer3 = createConsumer();
        ReplicationConsumer consumer4 = createConsumer();

        assertTrue( waitUntilLimitSyncReplClient( TOTAL_COUNT, consumer1, consumer2, consumer3, consumer4 ) );

        consumer1.stop();
        consumer2.stop();
        consumer3.stop();
        consumer4.stop();

        System.out.println( "\n<-- Done" );
    }
}
