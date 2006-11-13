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
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.directory.mitosis.common.Replica;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.configuration.MutableInterceptorConfiguration;
import org.apache.directory.server.core.configuration.MutableStartupConfiguration;
import org.apache.directory.server.core.configuration.ShutdownConfiguration;
import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.mina.util.AvailablePortFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A test case for {@link ReplicationServiceIntegrationTest}
 * 
 * @author The Apache Directory Project Team (dev@directory.apache.org)
 * @version $Rev$, $Date$
 */
public class ReplicationServiceIntegrationTest extends TestCase
{
    private final Logger log = LoggerFactory.getLogger( ReplicationServiceIntegrationTest.class );

    private Map contexts = new HashMap();

    protected void setUp() throws Exception
    {
        createReplicas( new String[] { "A", "B", "C" } );
    }

    protected void tearDown() throws Exception
    {
        destroyAllReplicas();
    }

    public void testOneWayBind() throws Exception
    {
        LdapContext ctxA = getReplicaContext( "A" );

        Attributes entry = new BasicAttributes( true );
        entry.put( "cn", "test" );
        entry.put( "objectClass", "top" );
        ctxA.bind( "cn=test,ou=system", entry );

        Thread.sleep( 5000 );

        LdapContext ctxB = getReplicaContext( "B" );
        Assert.assertNotNull( ctxB.lookup( "cn=test,ou=system" ) );

        LdapContext ctxC = getReplicaContext( "C" );
        Assert.assertNotNull( ctxC.lookup( "cn=test,ou=system" ) );
    }

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

            List interceptorCfgs = ldapCfg.getInterceptorConfigurations();

            ReplicationConfiguration replicationCfg = new ReplicationConfiguration();
            replicationCfg.setReplicaId( replica.getId() );
            replicationCfg.setServerPort( replica.getAddress().getPort() );
            for( int j = 0; j < replicas.length; j++ )
            {
                if( replicas[ j ] != replica )
                {
                    replicationCfg.addPeerReplica( replicas[ j ] );
                }
            }

            ReplicationService replicationService = new ReplicationService();
            replicationService.setConfiguration( replicationCfg );

            MutableInterceptorConfiguration interceptorCfg = new MutableInterceptorConfiguration();
            interceptorCfg.setName( "mitosis" );
            interceptorCfg.setInterceptor( replicationService );

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
        }
    }

    private LdapContext getReplicaContext( String name ) throws Exception
    {
        LdapContext context = ( LdapContext ) contexts.get( name );
        if( context == null )
        {
            throw new IllegalArgumentException( "No such replica: " + name );
        }

        return context;
    }

    private void destroyAllReplicas() throws Exception
    {
        for( Iterator i = contexts.keySet().iterator(); i.hasNext(); )
        {
            String replicaId = ( String ) i.next();
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
