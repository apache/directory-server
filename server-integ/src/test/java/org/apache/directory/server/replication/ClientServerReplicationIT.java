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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.replication.ReplicationConsumer;
import org.apache.directory.server.ldap.replication.SyncReplConsumer;
import org.apache.directory.server.ldap.replication.SyncReplRequestHandler;
import org.apache.directory.server.ldap.replication.SyncreplConfiguration;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.ModifyRequest;
import org.apache.directory.shared.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for replication subsystem in client-server mode.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ClientServerReplicationIT
{
    private static LdapServer providerServer;

    private static LdapServer consumerServer;

    private static SchemaManager schemaManager;
    
    private static CoreSession providerSession;
    
    private static CoreSession consumerSession;
    
    private static AtomicInteger entryCount = new AtomicInteger();
    
    @BeforeClass
    public static void setUp() throws Exception
    {
        Class justLoadToSetControlProperties = Class.forName( FrameworkRunner.class.getName() );
        
        startProvider();
        startConsumer();
    }


    @AfterClass
    public static void tearDown()
    {
        consumerServer.stop();
        providerServer.stop();
    }

    
    @Test
    public void testInjectContextEntry() throws Exception
    {
       String dn = "dc=example,dc=com";
        
       DefaultEntry entry = new DefaultEntry( schemaManager, dn );
       entry.add( "objectClass", "domain" );
       entry.add( "dc", "example" );
       
       assertFalse( consumerSession.exists( dn ) );
       
       providerSession.add( entry );
       
       waitAndCompareEntries( entry.getDn() );
    }

    
    @Test
    public void testModify() throws Exception
    {
        Entry provUser = createEntry();
        
        assertFalse( consumerSession.exists( provUser.getDn() ) );
        
        providerSession.add( provUser );
        
        ModifyRequest modReq = new ModifyRequestImpl();
        modReq.setName( provUser.getDn() );
        modReq.add( "userPassword", "secret" );
        
        providerSession.modify( modReq );
        
        waitAndCompareEntries( provUser.getDn() );
    }
    
    
    @Test
    public void testModDn() throws Exception
    {
        Entry provUser = createEntry();
        
        assertFalse( consumerSession.exists( provUser.getDn() ) );
        
        providerSession.add( provUser );
     
        Dn usersContainer = new Dn( schemaManager, "ou=users,dc=example,dc=com" );
        
        DefaultEntry entry = new DefaultEntry( schemaManager, usersContainer );
        entry.add( "objectClass", "organizationalUnit" );
        entry.add( "ou", "users" );
        
        providerSession.add( entry );
        
        waitAndCompareEntries( entry.getDn() );
        
        // move
        Dn userDn = provUser.getDn();
        providerSession.move( userDn, usersContainer );
        
        userDn = usersContainer.add( userDn.getRdn() );
        
        waitAndCompareEntries( userDn );
        
        // now try renaming
        Rdn newName = new Rdn( schemaManager, userDn.getRdn().getName() + "renamed");
        
        providerSession.rename( userDn, newName, true );
        
        userDn = usersContainer.add( newName );
        
        waitAndCompareEntries( userDn );
        
        // now move and rename
        Dn newParent = usersContainer.getParent();
        
        newName = new Rdn( schemaManager, userDn.getRdn().getName() + "MovedAndRenamed");
        
        providerSession.moveAndRename( userDn, newParent, newName, false );
        
        userDn = newParent.add( newName );
        waitAndCompareEntries( userDn );
    }
    
    
    @Test
    public void testDelete() throws Exception
    {
        Entry provUser = createEntry();
        
        providerSession.add( provUser );
        
        waitAndCompareEntries( provUser.getDn() );
        
        providerSession.delete( provUser.getDn() );
        
        Thread.sleep( 2000 );
        assertFalse( consumerSession.exists( provUser.getDn() ) );
    }
    
    
    @Test
    @Ignore("this test often fails due to a timing issue")
    public void testRebootConsumer() throws Exception
    {
        Entry provUser = createEntry();
        
        providerSession.add( provUser );
        
        waitAndCompareEntries( provUser.getDn() );
        
        consumerServer.stop();
        
        Dn deletedUserDn = provUser.getDn();
        providerSession.delete( deletedUserDn );
        
        provUser = createEntry();
        Dn addedUserDn = provUser.getDn();
        providerSession.add( provUser );
        
        startConsumer();
        
        Thread.sleep( 5000 );
        assertFalse( consumerSession.exists( deletedUserDn ) );
        waitAndCompareEntries( addedUserDn );
    }
    
    
    private void waitAndCompareEntries( Dn dn ) throws Exception
    {
        // sleep for 2 sec (twice the refresh interval), just to let the first refresh request succeed
        Thread.sleep( 2000 );

        Entry providerEntry = providerSession.lookup( dn, "*", "+" );
        
        Entry consumerEntry = consumerSession.lookup( dn, "*", "+" );
        assertEquals( providerEntry, consumerEntry );
    }
    
    
    private Entry createEntry() throws Exception
    {
        String user = "user"+ entryCount.incrementAndGet();
        
        String dn = "cn=" + user + ",dc=example,dc=com";
        
        DefaultEntry entry = new DefaultEntry( schemaManager, dn );
        entry.add( "objectClass", "person" );
        entry.add( "cn", user );
        entry.add( "sn", user );
        
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
                })
             })
    @CreateLdapServer(transports =
        { @CreateTransport( port=16000, protocol = "LDAP") })
    private static void startProvider() throws Exception
    {
        Method createProviderMethod = ClientServerReplicationIT.class.getDeclaredMethod( "startProvider" );
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
    
    
    @CreateDS(allowAnonAccess = true, name = "consumer-replication", partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                indexes =
                {
                    @CreateIndex(attribute = "objectClass"),
                    @CreateIndex(attribute = "dc"),
                    @CreateIndex(attribute = "ou")
                })
             })
    @CreateLdapServer(transports =
        { @CreateTransport( port=17000, protocol = "LDAP") })
    private static void startConsumer() throws Exception
    {
        Method createProviderMethod = ClientServerReplicationIT.class.getDeclaredMethod( "startConsumer" );
        CreateDS dsAnnotation = createProviderMethod.getAnnotation( CreateDS.class );
        DirectoryService provDirService = DSAnnotationProcessor.createDS( dsAnnotation );

        CreateLdapServer serverAnnotation = createProviderMethod.getAnnotation( CreateLdapServer.class );

        consumerServer = ServerAnnotationProcessor.instantiateLdapServer( serverAnnotation, provDirService, 0 );
        
        SyncReplConsumer syncreplClient = new SyncReplConsumer();
        final SyncreplConfiguration config = new SyncreplConfiguration();
        config.setProviderHost( "localhost" );
        config.setPort( 16000 );
        config.setReplUserDn( "uid=admin,ou=system" );
        config.setReplUserPassword( "secret".getBytes() );
        config.setUseTls( false );
        config.setBaseDn( "dc=example,dc=com" );
        config.setRefreshInterval( 1000 );
        
        syncreplClient.setConfig( config );
        
        List<ReplicationConsumer> replConsumers = new ArrayList<ReplicationConsumer>();
        replConsumers.add( syncreplClient );
        
        consumerServer.setReplConsumers( replConsumers );
        
        Runnable r = new Runnable()
        {
            public void run()
            {
                try
                {
                    consumerServer.start();
                    
                    DirectoryService ds = consumerServer.getDirectoryService();
                    
                    Dn configDn = new Dn( ds.getSchemaManager(), "ads-replConsumerId=localhost,ou=system" );
                    config.setConfigEntryDn( configDn );
                    
                    Entry provConfigEntry = new DefaultEntry( ds.getSchemaManager(), configDn );
                    provConfigEntry.add( "objectClass", "ads-replConsumer" );
                    provConfigEntry.add( "ads-replConsumerId", "localhost" );
                    provConfigEntry.add( "ads-searchBaseDN", config.getBaseDn() );
                    provConfigEntry.add( "ads-replProvHostName", config.getProviderHost() );
                    provConfigEntry.add( "ads-replProvPort", String.valueOf( config.getPort() ) );
                    provConfigEntry.add( "ads-replAliasDerefMode", config.getAliasDerefMode().getJndiValue() );
                    provConfigEntry.add( "ads-replAttributes", config.getAttributes() );
                    provConfigEntry.add( "ads-replRefreshInterval", String.valueOf( config.getRefreshInterval() ) );
                    provConfigEntry.add( "ads-replRefreshNPersist", String.valueOf( config.isRefreshNPersist() ) );
                    provConfigEntry.add( "ads-replSearchScope", config.getSearchScope().getLdapUrlValue() );
                    provConfigEntry.add( "ads-replSearchFilter", config.getFilter() );
                    provConfigEntry.add( "ads-replSearchSizeLimit", String.valueOf( config.getSearchSizeLimit() ) );
                    provConfigEntry.add( "ads-replSearchTimeOut", String.valueOf( config.getSearchTimeout() ) );
                    provConfigEntry.add( "ads-replUserDn", config.getReplUserDn() );
                    provConfigEntry.add( "ads-replUserPassword", config.getReplUserPassword() );
                    
                    consumerSession = consumerServer.getDirectoryService().getAdminSession();
                    consumerSession.add( provConfigEntry );
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
}
