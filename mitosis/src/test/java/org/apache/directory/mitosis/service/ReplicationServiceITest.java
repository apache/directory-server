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
package org.apache.directory.mitosis.service;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.directory.mitosis.common.Replica;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.configuration.MutableReplicationInterceptorConfiguration;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.configuration.MutableStartupConfiguration;
import org.apache.directory.server.core.configuration.ShutdownConfiguration;
import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.mina.util.AvailablePortFinder;

/**
 * A test case for {@link ReplicationServiceITest}
 * 
 * @author The Apache Directory Project Team (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ReplicationServiceITest extends TestCase
{
    private Map<String, LdapContext> contexts = new HashMap<String, LdapContext>();
    private Map<String, ReplicationService> replicationServices = new HashMap<String, ReplicationService>();

    protected void setUp() throws Exception
    {
        createReplicas( new String[] { "A", "B", "C" } );
    }

    protected void tearDown() throws Exception
    {
        destroyAllReplicas();
    }

    public void testOneWay() throws Exception
    {
        String dn1 = "cn=test,ou=system";
//        String dn2 = "cn=test2,ou=system";
        testOneWayBind( dn1 );
        testOneWayModify( dn1 );
//        testOneWayRename( dn1, dn2 );
        testOneWayUnbind( dn1 );
    }
    
    /**
     * Test that the entry created last will win in the case of a conflict.
     * 
     * NOTE: This test is DISABLED as there is an occasional problem when a message is acknowledged
     * too quickly, meaning no further messages can be sent until it has timed out (DIRSERVER-998).
     *
     * @throws Exception
     */
    public void disabled_testTwoWayBind() throws Exception
    {
        LdapContext ctxA = getReplicaContext( "A" );
        LdapContext ctxB = getReplicaContext( "B" );
        LdapContext ctxC = getReplicaContext( "C" );

        Attributes entryA = new AttributesImpl( true );
        entryA.put( "cn", "test" );
        entryA.put( "ou", "A" );
        entryA.put( "objectClass", "top" ).add( "extensibleObject" );
        ctxA.bind( "cn=test,ou=system", null, entryA );
        
        // Ensure the second bind is undebatebly the second.
        Thread.sleep( 100 );

        Attributes entryB = new AttributesImpl( true );
        entryB.put( "cn", "test" );
        entryB.put( "ou", "B" );
        entryB.put( "objectClass", "top" ).add( "extensibleObject" );
        ctxB.bind( "cn=test,ou=system", null, entryB );

        // Let both replicas replicate.  Note that a replica can only receive
        // logs from one peer at a time so we must delay between replications.
        replicationServices.get( "A" ).replicate();
        
        Thread.sleep( 5000 );
        
        replicationServices.get( "B" ).replicate();
        
        Thread.sleep( 5000 );

        Assert.assertEquals( "B", getAttributeValue( ctxA, "cn=test,ou=system", "ou" ) );
        Assert.assertEquals( "B", getAttributeValue( ctxB, "cn=test,ou=system", "ou" ) );
        Assert.assertEquals( "B", getAttributeValue( ctxC, "cn=test,ou=system", "ou" ) );
    }
    
    private void testOneWayBind( String dn ) throws Exception
    {
        LdapContext ctxA = getReplicaContext( "A" );
        LdapContext ctxB = getReplicaContext( "B" );
        LdapContext ctxC = getReplicaContext( "C" );
        
        Attributes entry = new AttributesImpl( true );
        entry.put( "cn", "test" );
        entry.put( "objectClass", "top" ).add( "extensibleObject" );
        ctxA.bind( dn, null, entry );

        replicationServices.get( "A" ).replicate();
        
        Thread.sleep( 5000 );

        Assert.assertNotNull( ctxA.lookup( dn ) );
        Assert.assertNotNull( ctxB.lookup( dn ) );
        Assert.assertNotNull( ctxC.lookup( dn ) );
    }

    private void testOneWayModify( String dn ) throws Exception
    {
        LdapContext ctxA = getReplicaContext( "A" );
        LdapContext ctxB = getReplicaContext( "B" );
        LdapContext ctxC = getReplicaContext( "C" );
        
        String newValue = "anything";
        
        ctxA.modifyAttributes( dn, new ModificationItem[] {
            new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, new AttributeImpl( "ou", newValue ))} );

        replicationServices.get( "A" ).replicate();
        
        Thread.sleep( 5000 );

        Assert.assertEquals( newValue, getAttributeValue( ctxB, dn, "ou" ) );
        Assert.assertEquals( newValue, getAttributeValue( ctxC, dn, "ou" ) );
    }

//    private void testOneWayRename( String dn1, String dn2 ) throws Exception
//    {
//        LdapContext ctxA = getReplicaContext( "A" );
//        LdapContext ctxB = getReplicaContext( "B" );
//        LdapContext ctxC = getReplicaContext( "C" );
//        
//        ctxA.rename( dn1, dn2 );
//        
//        replicationServices.get( "A" ).replicate();
//
//        Thread.sleep( 5000 );
//        
//        assertNotExists( ctxA, dn1 );
//        assertNotExists( ctxB, dn1 );
//        assertNotExists( ctxC, dn1 );
//        Assert.assertNotNull( ctxA.lookup( dn2 ) );
//        Assert.assertNotNull( ctxB.lookup( dn2 ) );
//        Assert.assertNotNull( ctxC.lookup( dn2 ) );
//    }
    
    private void testOneWayUnbind( String dn ) throws Exception
    {
        LdapContext ctxA = getReplicaContext( "A" );
        LdapContext ctxB = getReplicaContext( "B" );
        LdapContext ctxC = getReplicaContext( "C" );
        
        ctxA.unbind( dn );
        
        replicationServices.get( "A" ).replicate();

        Thread.sleep( 5000 );
        
        assertNotExists( ctxA, dn );
        assertNotExists( ctxB, dn );
        assertNotExists( ctxC, dn );
    }
    
    private void assertNotExists( LdapContext ctx, String dn ) throws NamingException
    {
        try
        {
            ctx.lookup( dn );
        }
        catch ( LdapNameNotFoundException e )
        {
            // This is expected so return immediately.
            return;
        }
        throw new AssertionError( "The entry exists" );
    }
    
    private String getAttributeValue( LdapContext ctx, String name, String attrName ) throws Exception
    {
        Attribute attr = ctx.getAttributes( name ).get( attrName );
        return ( String ) attr.get();
    }

    @SuppressWarnings("unchecked")
    private void createReplicas( String[] names ) throws Exception
    {
        int lastAvailablePort = 1024;

        Replica[] replicas = new Replica[ names.length ];
        for( int i = 0; i < names.length; i++ )
        {
            int replicationPort = AvailablePortFinder
                    .getNextAvailable( lastAvailablePort );
            lastAvailablePort = replicationPort + 1;

            replicas[ i ] = new Replica( new ReplicaId( names[ i ] ),
                    new InetSocketAddress( "127.0.0.1", replicationPort ) );
        }

        Random random = new Random();
        String homeDirectory = System.getProperty( "java.io.tmpdir" )
                + File.separator + "mitosis-"
                + Long.toHexString( random.nextLong() );

        for( int i = 0; i < replicas.length; i++ )
        {
            Replica replica = replicas[ i ];
            String replicaId = replicas[ i ].getId().getId();
            MutableStartupConfiguration ldapCfg = new MutableStartupConfiguration(
                    replicaId );

            File workDir = new File( homeDirectory + File.separator
                    + ldapCfg.getInstanceId() );

            ldapCfg.setShutdownHookEnabled( false );
            ldapCfg.setWorkingDirectory( workDir );

            List<InterceptorConfiguration> interceptorCfgs = ldapCfg.getInterceptorConfigurations();

            ReplicationConfiguration replicationCfg = new ReplicationConfiguration();
            replicationCfg.setReplicaId( replica.getId() );
            // Disable automatic replication to prevent unexpected behavior
            replicationCfg.setReplicationInterval(0);
            replicationCfg.setServerPort( replica.getAddress().getPort() );
            for( int j = 0; j < replicas.length; j++ )
            {
                if( replicas[ j ] != replica )
                {
                    replicationCfg.addPeerReplica( replicas[ j ] );
                }
            }

            MutableReplicationInterceptorConfiguration interceptorCfg = 
                new MutableReplicationInterceptorConfiguration();
            interceptorCfg.setName( "mitosis" );
            interceptorCfg.setInterceptorClassName( ReplicationService.class.getName() );
            interceptorCfg.setReplicationConfiguration( replicationCfg );
            interceptorCfgs.add( interceptorCfg );

            ldapCfg.setInterceptorConfigurations( interceptorCfgs );

            if( workDir.exists() )
            {
                FileUtils.deleteDirectory( workDir );
            }

            Hashtable env = new Hashtable( ldapCfg.toJndiEnvironment() );
            env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
            env.put( Context.SECURITY_CREDENTIALS, "secret" );
            env.put( Context.SECURITY_AUTHENTICATION, "simple" );
            env.put( Context.PROVIDER_URL, "" );
            env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class
                    .getName() );

            // Initialize the server instance.
            LdapContext context = new InitialLdapContext( env, null );
            contexts.put( replicaId, context );
            ReplicationService replicationService = (ReplicationService) DirectoryService.getInstance( replicaId ).getConfiguration().getInterceptorChain().get( "mitosis" );
            replicationServices.put( replicaId, replicationService );
        }

        // Ensure all replicas have had a chance to connect to each other since the last one started.
        for( Iterator<ReplicationService> i = replicationServices.values().iterator(); i.hasNext(); )
        {
            i.next().interruptConnectors();
        }
        Thread.sleep( 5000 );
    }

    private LdapContext getReplicaContext( String name ) throws Exception
    {
        LdapContext context = contexts.get( name );
        if( context == null )
        {
            throw new IllegalArgumentException( "No such replica: " + name );
        }

        return ( LdapContext ) context.lookup( "" );
    }
    
    @SuppressWarnings("unchecked")
    private void destroyAllReplicas() throws Exception
    {
        for( Iterator<String> i = contexts.keySet().iterator(); i.hasNext(); )
        {
            String replicaId = i.next();
            File workDir = DirectoryService.getInstance( replicaId )
                    .getConfiguration().getStartupConfiguration()
                    .getWorkingDirectory();

            Hashtable env = new Hashtable();
            env.put( Context.PROVIDER_URL, "ou=system" );
            env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class
                    .getName() );
            env.putAll( new ShutdownConfiguration( replicaId )
                    .toJndiEnvironment() );
            env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
            env.put( Context.SECURITY_CREDENTIALS, "secret" );
            try
            {
                new InitialContext( env );
            }
            catch( Exception e )
            {
            }

            try
            {
                FileUtils.deleteDirectory( workDir );
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }

            workDir.getParentFile().delete();

            i.remove();
        }
    }
}
