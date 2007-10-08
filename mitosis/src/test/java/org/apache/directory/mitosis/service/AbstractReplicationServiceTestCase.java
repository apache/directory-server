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

package org.apache.directory.mitosis.service;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.directory.mitosis.common.Replica;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.configuration.ReplicationConfiguration;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.mina.util.AvailablePortFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.*;


/**
 * An abstract base class for replication tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractReplicationServiceTestCase extends TestCase
{
    private static final Logger LOG = LoggerFactory.getLogger( AbstractReplicationServiceTestCase.class );
    protected Map<String, LdapContext> contexts = new HashMap<String, LdapContext>();
    protected Map<String, DirectoryService> services = new HashMap<String, DirectoryService>();
    protected Map<String, ReplicationService> replicationServices = new HashMap<String, ReplicationService>();

    protected void setUp() throws Exception
    {
        createReplicas( new String[] { "A", "B", "C" } );
    }

    protected void tearDown() throws Exception
    {
        destroyAllReplicas();
    }

    @SuppressWarnings("unchecked")
    protected void createReplicas( String[] names ) throws Exception
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

        for ( Replica replica : replicas )
        {
            String replicaId = replica.getId().getId();
            DirectoryService service = new DefaultDirectoryService();
            service.setInstanceId( replicaId );

            File workDir = new File( homeDirectory + File.separator
                    + service.getInstanceId() );

            service.setShutdownHookEnabled( false );
            service.setWorkingDirectory( workDir );

            List<Interceptor> interceptors = service.getInterceptors();

            ReplicationConfiguration replicationCfg = new ReplicationConfiguration();
            replicationCfg.setReplicaId( replica.getId() );
            // Disable automatic replication to prevent unexpected behavior
            replicationCfg.setReplicationInterval( 0 );
            replicationCfg.setServerPort( replica.getAddress().getPort() );
            for ( Replica replica1 : replicas )
            {
                if ( replica1 != replica )
                {
                    replicationCfg.addPeerReplica( replica1 );
                }
            }

            ReplicationService replicationService = new ReplicationService();
            replicationService.setConfiguration( replicationCfg );
            interceptors.add( replicationService );

            service.setInterceptors( interceptors );

            if ( workDir.exists() )
            {
                FileUtils.deleteDirectory( workDir );
            }

            service.startup();
            
            Hashtable env = new Hashtable();
            env.put( DirectoryService.JNDI_KEY, service );
            env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
            env.put( Context.SECURITY_CREDENTIALS, "secret" );
            env.put( Context.SECURITY_AUTHENTICATION, "simple" );
            env.put( Context.PROVIDER_URL, "" );
            env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class
                    .getName() );

            // Initialize the server instance.
            LdapContext context = new InitialLdapContext( env, null );
            contexts.put( replicaId, context );
            services.put( replicaId, service );
            replicationServices.put( replicaId, replicationService );
        }

        // Ensure all replicas have had a chance to connect to each other since the last one started.
        for ( ReplicationService replicationService : replicationServices.values() )
        {
            replicationService.interruptConnectors();
        }
        Thread.sleep( 5000 );
    }

    protected LdapContext getReplicaContext( String name ) throws Exception
    {
        LdapContext context = contexts.get( name );
        if( context == null )
        {
            throw new IllegalArgumentException( "No such replica: " + name );
        }

        return ( LdapContext ) context.lookup( "" );
    }

    @SuppressWarnings("unchecked")
    protected void destroyAllReplicas() throws Exception
    {
        for( Iterator<DirectoryService> i = services.values().iterator(); i.hasNext(); )
        {
            DirectoryService replica = i.next();
            File workDir = replica.getWorkingDirectory();

            try
            {
                replica.shutdown();
            }
            catch( Exception e )
            {
                LOG.error( "Encountered error while shutting down replica {}", replica.getInstanceId(), e );
                throw e;
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
