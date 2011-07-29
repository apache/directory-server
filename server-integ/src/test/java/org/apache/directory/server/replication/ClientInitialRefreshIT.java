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

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.MockDirectoryService;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.replication.ReplicationConsumer;
import org.apache.directory.server.ldap.replication.SyncReplRequestHandler;
import org.apache.directory.server.ldap.replication.SyncreplConfiguration;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the initial refresh of a client
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ClientInitialRefreshIT
{
    private static LdapServer providerServer;

    private static SchemaManager schemaManager;
    
    private static CoreSession providerSession;
    
    private static AtomicInteger entryCount = new AtomicInteger();
    
    @BeforeClass
    public static void setUp() throws Exception
    {
        Class<?> justLoadToSetControlProperties = Class.forName( FrameworkRunner.class.getName() );
        
        startProvider();
        
        // Load 1000 entries
        for ( int i = 0; i < 1000; i++ )
        {
            Entry entry = createEntry();

            providerSession.add( entry );
        }
    }


    @AfterClass
    public static void tearDown()
    {
        providerServer.stop();
    }

    
    /**
     * Check that the entry exists in the target server. We wait up to 10 seconds, by
     * 100ms steps, until either the entry s found, or we have exhausted the 10 seconds delay.
     */
    private boolean checkEntryExistence( CoreSession session, Dn entryDn ) throws Exception
    {
        boolean replicated = false;
        
        for ( int i = 0; i < 100; i++ )
        {
            Thread.sleep( 100 );
            
            if ( session.exists( entryDn ) )
            {
                replicated = true;
                break;
            }
        }
        
        return replicated;
    }
    
    
    private void waitAndCompareEntries( Dn dn ) throws Exception
    {
        // sleep for 2 sec (twice the refresh interval), just to let the first refresh request succeed
        Entry providerEntry = providerSession.lookup( dn, "*", "+" );
        
        //Entry consumerEntry = consumerSession.lookup( dn, "*", "+" );
        //assertEquals( providerEntry, consumerEntry );
    }
    
    
    private static Entry createEntry() throws Exception
    {
        String user = "user"+ entryCount.incrementAndGet();
        
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
                contextEntry=@ContextEntry( entryLdif = 
                    "dn: dc=example,dc=com\n" +
                    "objectClass: domain\n" +
                    "dc: example" ) )
             })
    @CreateLdapServer(transports =
        { @CreateTransport( port=16000, protocol = "LDAP") })
    private static void startProvider() throws Exception
    {
        Method createProviderMethod = ClientInitialRefreshIT.class.getDeclaredMethod( "startProvider" );
        CreateDS dsAnnotation = createProviderMethod.getAnnotation( CreateDS.class );
        DirectoryService provDirService = DSAnnotationProcessor.createDS( dsAnnotation );

        CreateLdapServer serverAnnotation = createProviderMethod.getAnnotation( CreateLdapServer.class );

        providerServer = ServerAnnotationProcessor.instantiateLdapServer( serverAnnotation, provDirService, 0 );
        
        providerServer.setReplicationReqHandler( new SyncReplRequestHandler() );
        
        Runnable r = new Runnable()
        {
            public void run()
            {
                try
                {
                    providerServer.start();
                    schemaManager = providerServer.getDirectoryService().getSchemaManager();
                    providerSession = providerServer.getDirectoryService().getAdminSession();
                }
                catch( Exception e )
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
            int nbAdded = ((MockSyncReplConsumer)consumer).getNbAdded();
            
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
    

    private ReplicationConsumer createConsumer() throws Exception
    {
        final ReplicationConsumer syncreplClient = new MockSyncReplConsumer();
        final SyncreplConfiguration config = new SyncreplConfiguration();
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
                    ((MockSyncReplConsumer)syncreplClient).init( directoryService );
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
     * First test : create a consumer, and see if it gets the 1000 entries
     */
    @Test
    public void testInitialRefreshLoad() throws Exception
    {
        ReplicationConsumer syncreplClient = createConsumer();
        
        // We should have 1000 entries plus the base entry = 1001
        assertTrue( waitForSyncReplClient( syncreplClient, 1001 ) ); 
    }
    
    
    /**
     * Test that we can load entries, then add one entry in the producer
     * and see this entry present in the consumer
     */
    @Test
    public void testInitialRefreshLoadAndAdd() throws Exception
    {
        ReplicationConsumer syncreplClient = createConsumer();
        
        // We should have 1000 entries plus the base entry = 1001
        assertTrue( waitForSyncReplClient( syncreplClient, 1001 ) );
        
        // Injext a new intry in the producer
        Entry addedEntry = createEntry();
        providerSession.add( addedEntry );
        
        // Now check that the entry has been copied in the consumer
        assertTrue( waitForSyncReplClient( syncreplClient, 1002 ) );
    }
}
