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


import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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

    private SchemaManager schemaManager;
    
    private CoreSession providerSession;
    
    private CoreSession consumerSession;
    
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

    
    @Before
    public void accessFields()
    {
        schemaManager = providerServer.getDirectoryService().getSchemaManager();
        providerSession = providerServer.getDirectoryService().getAdminSession();
        consumerSession = consumerServer.getDirectoryService().getAdminSession();
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
       
       // sleep for 2 sec (twice the refresh interval), just to let the first refresh request succeed
       Thread.sleep( 2000 );
       
       assertTrue( consumerSession.exists( dn ) );
       
       Entry providerEntry = providerSession.lookup( entry.getDn(), "*", "+" );
       
       Entry consumerEntry = consumerSession.lookup( entry.getDn(), "*", "+" );
       assertEquals( providerEntry, consumerEntry );
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
        SyncreplConfiguration config = new SyncreplConfiguration();
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
    }
}
