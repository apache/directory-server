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
package org.apache.directory.server.ldap.replication;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.DeleteRequest;
import org.apache.directory.api.ldap.model.message.DeleteRequestImpl;
import org.apache.directory.api.ldap.model.message.DeleteResponse;
import org.apache.directory.api.ldap.model.message.ModifyDnRequest;
import org.apache.directory.api.ldap.model.message.ModifyDnRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyDnResponse;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.url.LdapUrl;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateConsumer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumer;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumerImpl;
import org.apache.directory.server.ldap.replication.provider.SyncReplRequestHandler;

/**
 * Holds the configuration and instances of connections
 * 
 * WARN: this class is not yet ready to run as a junit test cause it requires 
 *       some manual setup of servers and test is invoked by main()
 *       The existing annotation based server instances are not used yet.
 *       
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MmrTester
{
    private static final String PARTITION_SUFFIX = "dc=example,dc=com";

    private static final String RDN_PREFIX = "p";

    private Set<String> urls = new LinkedHashSet<String>();
    
    private List<LdapNetworkConnection> connections;
    
    private static final int TOTAL_ENTRY_COUNT = 200;
    
    private AtomicInteger count = new AtomicInteger(-1);
    
    private Random rand = new Random();

    private boolean verbose = false;
    
    private static LdapServer peer1Server;

    private static LdapServer peer2Server;

    static
    {
        try
        {
            Class.forName( FrameworkRunner.class.getName() );
        }
        catch(Exception e)
        {
            throw new RuntimeException( e );
        }
    }
    
    public MmrTester( String... ldapUrls )
    {
        if( ( ldapUrls == null ) || ( ldapUrls.length < 2 ) )
        {
            throw new IllegalArgumentException( "Atleast two servers are required to run MMR tests" );
        }
        
        for( String u : ldapUrls )
        {
            urls.add( u.toLowerCase() );
        }
    }
    
    
    public void prepareConnections() throws Exception
    {
        connections = new ArrayList<LdapNetworkConnection>();
        
        for( String u : urls )
        {
            LdapUrl url = new LdapUrl( u );
            boolean useSsl = false;
            if( url.getScheme().equals( "ldaps" ) )
            {
                useSsl = true;
            }
            
            LdapNetworkConnection c = new LdapNetworkConnection( url.getHost(), url.getPort(), useSsl );
            c.connect();
            c.bind( ServerDNConstants.ADMIN_SYSTEM_DN, "secret" );
            System.out.println( "connected to the server " + url );
            connections.add( c );
        }
    }
    
    
    public void addAndCompare()
    {
        List<Dn> injected = new ArrayList<Dn>();
        int batch = 20;
        
        if( batch > TOTAL_ENTRY_COUNT )
        {
            batch = TOTAL_ENTRY_COUNT;
        }
        else
        {
            batch = TOTAL_ENTRY_COUNT;
        }
        
        for( int i=0; i < batch; i++ )
        {
            int index = rand.nextInt( connections.size() );

            LdapNetworkConnection nc = connections.get( index );
            
            if( verbose )
            {
                System.out.println( "inserting into the server " + nc.getConfig().getLdapHost() + ":" + nc.getConfig().getLdapPort() );
            }
            
            Entry e = createEntry( count.incrementAndGet() );
            if ( inject( nc, e ) )
            {
                injected.add( e.getDn() );
            }
        }
        
        compareEntries( injected );
    }
    
    
    public List<Dn> modify() throws Exception
    {
        List<Dn> modified = new ArrayList<Dn>();
        int batch = 20;
        
        if( batch > TOTAL_ENTRY_COUNT )
        {
            batch = TOTAL_ENTRY_COUNT;
        }
        else
        {
            batch = (int) ( TOTAL_ENTRY_COUNT * 0.10 ); // modify 10% of total entries
        }

        for( int i=0; i < batch; i++ )
        {
            int connectionIndex = rand.nextInt( connections.size() );
            
            int entryIndex = rand.nextInt( batch );
            
            LdapNetworkConnection nc = connections.get( connectionIndex );
            
            String cn = RDN_PREFIX + entryIndex;
            Dn personDn = new Dn( "cn=" + cn + "," + PARTITION_SUFFIX );

            if ( verbose )
            {
                System.out.println( "modifying " + personDn + " on the server " + nc.getConfig().getLdapHost() + ":" + nc.getConfig().getLdapPort() );
            }
            
            ModifyRequest modReq = new ModifyRequestImpl();
            modReq.setName( personDn );
            modReq.replace( SchemaConstants.SN_AT, "sn_" + i );
            
            ModifyResponse resp = nc.modify( modReq );
            ResultCodeEnum rc = resp.getLdapResult().getResultCode();
            if( rc != ResultCodeEnum.SUCCESS )
            {
                System.out.println( "Error modifying " + personDn + " on the server " + nc.getConfig().getLdapHost() + ":" + nc.getConfig().getLdapPort() + " with result code " + rc );
            }

            modified.add( personDn );
        }
        
        return modified;
    }
    
    
    public void moveAndCompare( Dn superiorDn ) throws Exception
    {
        List<Dn> moved = new ArrayList<Dn>();
        int batch = 5;
        
        if( batch > TOTAL_ENTRY_COUNT )
        {
            batch = TOTAL_ENTRY_COUNT;
        }
        else
        {
            batch = (int) ( TOTAL_ENTRY_COUNT * 0.20 ); // move 20% of total entries
        }
        
        for( int i=0; i < batch; i++ )
        {
            int connectionIndex = rand.nextInt( connections.size() );
            
            LdapNetworkConnection nc = connections.get( connectionIndex );
            
            String cn = RDN_PREFIX + i;
            Dn personDn = new Dn( "cn=" + cn + "," + PARTITION_SUFFIX );
            
            if( verbose )
            {
                System.out.println( "moving " + personDn + " on the server " + nc.getConfig().getLdapHost() + ":" + nc.getConfig().getLdapPort() + ":" + nc.getConfig().getLdapPort() );
            }
            
            ModifyDnRequest modReq = new ModifyDnRequestImpl();
            modReq.setName( personDn );
            modReq.setNewRdn( personDn.getRdn() );
            modReq.setNewSuperior( superiorDn );
            
            ModifyDnResponse resp = nc.modifyDn( modReq );
            ResultCodeEnum rc = resp.getLdapResult().getResultCode();
            if( rc != ResultCodeEnum.SUCCESS )
            {
                System.out.println( "Error moving " + personDn + " on the server " + nc.getConfig().getLdapHost() + ":" + nc.getConfig().getLdapPort() + ":" + nc.getConfig().getLdapPort() + " with result code " + rc );
            }
            
            moved.add( superiorDn.add( personDn.getRdn() ) );
        }
        
        compareEntries( moved );
    }

    
    public List<Dn> renameAndCompare( Dn containerDn ) throws Exception
    {
        List<Dn> renamed = new ArrayList<Dn>();
        List<Dn> present = new ArrayList<Dn>();
        
        int connectionIndex = rand.nextInt( connections.size() );

        LdapNetworkConnection nc = connections.get( connectionIndex );
        EntryCursor cursor = nc.search( containerDn, "(cn=" + RDN_PREFIX + "*)", SearchScope.ONELEVEL, SchemaConstants.NO_ATTRIBUTE_ARRAY );
        
        while( cursor.next() )
        {
            present.add( cursor.get().getDn() );
        }
        
        cursor.close();
        
        for( int i=0; i < present.size(); i++ )
        {
            connectionIndex = rand.nextInt( connections.size() );
            nc = connections.get( connectionIndex );
            
            Dn personDn = present.get( i );
            
            ModifyDnRequest modReq = new ModifyDnRequestImpl();
            Rdn newRdn = new Rdn( "cn=p_rename" + i );
            modReq.setName( personDn );
            modReq.setNewRdn( newRdn );
            
            if( verbose )
            {
                System.out.println( "renaming " + personDn + " on the server " + nc.getConfig().getLdapHost() + ":" + nc.getConfig().getLdapPort() );    
            }
            
            ModifyDnResponse resp = nc.modifyDn( modReq );
            ResultCodeEnum rc = resp.getLdapResult().getResultCode();
            if( rc != ResultCodeEnum.SUCCESS )
            {
                System.out.println( "Error renaming " + personDn + " on the server " + nc.getConfig().getLdapHost() + ":" + nc.getConfig().getLdapPort() + " with result code " + rc );
            }
            
            renamed.add( personDn.getParent().add( newRdn ) );
        }
        
        compareEntries( renamed );
        
        return renamed;
    }

    
    public void deleteAndVerify( List<Dn> present ) throws Exception
    {
        List<Dn> deleted = new ArrayList<Dn>();
        
        int count = present.size();//( present.size() - 2 );
        for( int i=0; i < count; i++ )
        {
            int connectionIndex = rand.nextInt( connections.size() );
            
            LdapNetworkConnection nc = connections.get( connectionIndex );
            
            Dn personDn = present.get( i );
            
            DeleteRequest delReq = new DeleteRequestImpl();
            delReq.setName( personDn );
            
            if( verbose )
            {
                System.out.println( "deleting " + personDn + " on the server " + nc.getConfig().getLdapHost() + ":" + nc.getConfig().getLdapPort() );
            }
            
            DeleteResponse resp = nc.delete( delReq );
            ResultCodeEnum rc = resp.getLdapResult().getResultCode();

            if( rc != ResultCodeEnum.SUCCESS )
            {
                System.out.println( "Error deleting " + personDn + " on the server " + nc.getConfig().getLdapHost() + ":" + nc.getConfig().getLdapPort() + " with result code " + rc );
            }
            
            deleted.add( personDn );
        }
        
        verifyDeleted( deleted );
    }

    
    public void compareEntries( List<Dn> injected )
    {
        for( Dn dn : injected )
        {
            Entry baseEntry = null;
            Iterator<LdapNetworkConnection> itr = connections.iterator();
            LdapNetworkConnection c = itr.next();
            baseEntry = lookupWithWait( c, dn );
            
            while( itr.hasNext() )
            {
                c = itr.next();
                Entry replicaEntry = lookupWithWait( c, dn );
                boolean equal = baseEntry.equals( replicaEntry );
                if( !equal )
                {
                    System.out.println( "base entry: " + baseEntry );
                    System.out.println( "replica entry: " + replicaEntry );
                }
                
                assertTrue( equal );
            }
        }
    }
    

    public void verifyDeleted( List<Dn> injected )
    {
        for( Dn dn : injected )
        {
            Iterator<LdapNetworkConnection> itr = connections.iterator();
            outer: while( itr.hasNext() )
            {
                LdapNetworkConnection c = itr.next();
                try
                {
                    for( int i = 1; i <= 10; )
                    {
                        if( !c.exists( dn ) )
                        {
                            continue outer;
                        }
                        
                        Thread.sleep( 1000 * i );
                    }
                    
                    throw new RuntimeException( "deleted Entry " + dn + " found on server " + c.getConfig().getLdapHost() + ":" + c.getConfig().getLdapPort() );
                }
                catch( Exception e )
                {
                    throw new RuntimeException( e );
                }
            }
        }
    }
    
    
    public Entry lookupWithWait( LdapNetworkConnection c, Dn dn )
    {
        try
        {
            for( int i = 1; i <= 10; )
            {
                if( !c.exists( dn ) )
                {
                    Thread.sleep( 1000 * i );
                    continue;
                }
                
                return c.lookup( dn, SchemaConstants.ALL_USER_ATTRIBUTES, SchemaConstants.ENTRY_UUID_AT );
            }
        }
        catch( Exception e )
        {
            throw new RuntimeException( e );
        }
        
        throw new RuntimeException( "Entry " + dn + " not found on server " + c.getConfig().getLdapHost() + ":" + c.getConfig().getLdapPort() );
    }
    
    
    public boolean inject( LdapNetworkConnection nc, Entry e )
    {
        try
        {
            nc.add( e );
            return true;
        }
        catch( Exception ex )
        {
            count.decrementAndGet();
            ex.printStackTrace();
            return false;
        }
    }

    
    public boolean isCountReached()
    {
        return ( count.get() >= TOTAL_ENTRY_COUNT );
    }
    
    
    public void injectAndWaitTillReplicates( Entry ctxEntry ) throws Exception
    {
        LdapNetworkConnection c = connections.get( 0 );
        
        if( !c.exists( ctxEntry.getDn() ) )
        {
            c.add( ctxEntry );
        }
        
        compareEntries( Collections.singletonList( ctxEntry.getDn() ) );
    }
    
    
    private Entry createEntry( int num )
    {
        try
        {
            String cn = RDN_PREFIX + num;
            Dn personDn = new Dn( "cn=" + cn + "," + PARTITION_SUFFIX );
            Entry person = new DefaultEntry(
                personDn.toString(),
                "ObjectClass: top",
                "ObjectClass: person",
                "cn: " + cn,
                "sn: sn_" + cn,
                "userPassword: 12345" );
            return person;
        }
        catch( Exception e )
        {
            throw new RuntimeException( e );
        }
    }
    
    
    public void closeConnections()
    {
        for( LdapNetworkConnection nc : connections )
        {
            try
            {
                nc.close();
            }
            catch( Exception e )
            {
                //ignore
                e.printStackTrace();
            }
        }
    }
    
    public static void main( String[] args )
    {
        MmrTester cc = new MmrTester( "ldap://localhost:16001", "ldap://localhost:16000" );
        
        if( TOTAL_ENTRY_COUNT < 150 )
        {
            cc.verbose = true;
        }
        
        try
        {
            Thread t1 = new Thread( new Runnable()
            {
                
                @Override
                public void run()
                {
                    try
                    {
                        startPeer1();
                    }
                    catch(Exception e )
                    {
                        e.printStackTrace();
                    }
                }
            } );

            t1.setDaemon( true );
            
            Thread t2 = new Thread( new Runnable()
            {
                
                @Override
                public void run()
                {
                    try
                    {
                        startPeer2();
                    }
                    catch(Exception e )
                    {
                        e.printStackTrace();
                    }
                }
            } );

            t2.setDaemon( true );
            
            t1.start();
            t2.start();
            
            t1.join();
            t2.join();
            
            cc.prepareConnections();

            Entry ctxEntry = new DefaultEntry( "dc=example,dc=com",
                "objectClass: domain",
                "objectClass: top",
                "dc: example" );

            cc.injectAndWaitTillReplicates( ctxEntry );
            cc.addAndCompare();
            
            List<Dn> modified = cc.modify();
            Thread.sleep( 15000 );
            
            cc.compareEntries( modified );
            
            Entry groupEntry = new DefaultEntry( "ou=groups,dc=example,dc=com",
                "objectClass: organizationalUnit",
                "objectClass: top",
                "ou: groups" );
            
            cc.injectAndWaitTillReplicates( groupEntry );
            
            cc.moveAndCompare( groupEntry.getDn() );
            
            List<Dn> renamed = cc.renameAndCompare( groupEntry.getDn() );
            
            cc.deleteAndVerify( renamed );
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
        finally
        {
            cc.closeConnections();
            shutdown();
        }
    }

    @CreateDS(
        allowAnonAccess = true,
        enableChangeLog = false,
        name = "peer1",
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
                    })
        })
    @CreateLdapServer(transports =
        { @CreateTransport(port = 16000, protocol = "LDAP") })
    @CreateConsumer
    (
        remoteHost = "localhost",
        remotePort = 16001,
        replUserDn = "uid=admin,ou=system",
        replUserPassword = "secret",
        useTls = false,
        baseDn = "dc=example,dc=com",
        replicaId = 1,
        refreshNPersist = true
    )
    public static void startPeer1() throws Exception
    {
        DirectoryService provDirService = DSAnnotationProcessor.getDirectoryService();

        peer1Server = ServerAnnotationProcessor.getLdapServer( provDirService );
        peer1Server.setReplicationReqHandler( new SyncReplRequestHandler() );
        peer1Server.startReplicationProducer();
        
        final ReplicationConsumerImpl consumer = ( ReplicationConsumerImpl ) ServerAnnotationProcessor.createConsumer();
        List<ReplicationConsumer> replConsumers = new ArrayList<ReplicationConsumer>();
        replConsumers.add( consumer );
        
        peer1Server.setReplConsumers( replConsumers );
        peer1Server.startReplicationConsumers();

        Runnable r = new Runnable()
        {
            public void run()
            {
                try
                {
                    DirectoryService ds = peer1Server.getDirectoryService();

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

                    CoreSession consumerSession = peer1Server.getDirectoryService().getAdminSession();
                    consumerSession.add( provConfigEntry );
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( e );
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
        name = "peer2",
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
                    })
        })
    @CreateLdapServer(transports =
        { @CreateTransport(port = 16001, protocol = "LDAP") })
    @CreateConsumer
    (
        remoteHost = "localhost",
        remotePort = 16000,
        replUserDn = "uid=admin,ou=system",
        replUserPassword = "secret",
        useTls = false,
        baseDn = "dc=example,dc=com",
        refreshNPersist = true,
        replicaId = 1
    )
    public static void startPeer2() throws Exception
    {
        DirectoryService provDirService = DSAnnotationProcessor.getDirectoryService();

        peer2Server = ServerAnnotationProcessor.getLdapServer( provDirService );
        peer2Server.setReplicationReqHandler( new SyncReplRequestHandler() );
        peer2Server.startReplicationProducer();
        
        final ReplicationConsumerImpl consumer = ( ReplicationConsumerImpl ) ServerAnnotationProcessor.createConsumer();
        List<ReplicationConsumer> replConsumers = new ArrayList<ReplicationConsumer>();
        replConsumers.add( consumer );
        
        peer2Server.setReplConsumers( replConsumers );
        peer2Server.startReplicationConsumers();

        Runnable r = new Runnable()
        {
            public void run()
            {
                try
                {
                    DirectoryService ds = peer2Server.getDirectoryService();

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

                    CoreSession consumerSession = peer2Server.getDirectoryService().getAdminSession();
                    consumerSession.add( provConfigEntry );
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( e );
                }
            }
        };

        Thread t = new Thread( r );
        t.setDaemon( true );
        t.start();
        t.join();
    }
    
    public static void shutdown()
    {
        peer1Server.stop();
        peer2Server.stop();
    }
}
