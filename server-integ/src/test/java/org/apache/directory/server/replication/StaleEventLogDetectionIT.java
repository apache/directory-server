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


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.api.util.FileUtils;
import org.apache.directory.api.ldap.codec.api.LdapApiService;
import org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncDoneValueFactory;
import org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncRequestValueFactory;
import org.apache.directory.api.ldap.extras.controls.syncrepl_impl.SyncStateValueFactory;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Network;
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
import org.apache.directory.server.ldap.replication.provider.ReplicaEventLog;
import org.apache.directory.server.ldap.replication.provider.SyncReplRequestHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests the stale event log removal capability
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StaleEventLogDetectionIT
{
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
        if ( cookiesDir.exists() )
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

        // Load the replication controls
        LdapApiService codec = provDirService.getLdapCodecService();
        codec.registerRequestControl( new SyncRequestValueFactory( codec ) );
        codec.registerResponseControl( new SyncDoneValueFactory( codec ) );
        codec.registerResponseControl( new SyncStateValueFactory( codec ) );

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
//        System.out.println( "\nNbAdded every 100ms : " );
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
//                System.out.print( ", " );
            }

//            System.out.print( nbAdded );

            if ( nbAdded == expected )
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
        config.setRemoteHost( Network.LOOPBACK_HOSTNAME );
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
                    syncreplClient.connect( true );
                    syncreplClient.startSync();
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


    @Test
    public void testDeleteStaleEventLog() throws Exception
    {
        //System.out.println( "\n---> Running testDeleteStaleEventLog" );

        ReplicationConsumer consumer = createConsumer();

        // We should have 1000 entries plus the base entry = TOTAL_COUNT
        assertTrue( waitForSyncReplClient( consumer, TOTAL_COUNT ) );
        consumer.stop();

        Thread.sleep( 5 * 1000 ); // let the journal be created and put in the map

        SyncReplRequestHandler syncreplHandler = ( SyncReplRequestHandler ) providerServer.getReplicationReqHandler();

        ReplicaEventLog log = syncreplHandler.getReplicaLogMap().values().iterator().next();
        log.setMaxIdlePeriod( 10 ); // in seconds

        syncreplHandler.getLogJanitor().setSleepTime( 1000 ); // every second
        syncreplHandler.getLogJanitor().interrupt();

        File replDir = providerServer.getDirectoryService().getInstanceLayout().getReplDirectory();
        File logFile = new File( replDir, log.getName() + ".db" );

        // there should be only one log file
        assertTrue( logFile.exists() );

        // let it sleep for 10 seconds + 5 seconds (above)
        Thread.sleep( 10 * 1000 );

        // there should be only one log file
        assertFalse( logFile.exists() );

        //System.out.println( "\n<-- Done" );
    }
}
