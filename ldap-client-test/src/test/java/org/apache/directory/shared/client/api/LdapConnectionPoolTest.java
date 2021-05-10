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
package org.apache.directory.shared.client.api;


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Network;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.ValidatingPoolableLdapConnectionFactory;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.ApacheDSTestExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * A test class for the connection pool.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@ExtendWith( { ApacheDSTestExtension.class } )
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
public class LdapConnectionPoolTest extends AbstractLdapTestUnit
{
    /** The connection pool */
    private LdapConnectionPool pool;

    /** The Constant DEFAULT_ADMIN. */
    private static final String DEFAULT_ADMIN = ServerDNConstants.ADMIN_SYSTEM_DN;

    /** The Constant DEFAULT_PASSWORD. */
    private static final String DEFAULT_PASSWORD = "secret";

    /**
     * A thread used to test the connection
     */
    private class ConnectionThread extends Thread
    {
        CountDownLatch counter;


        public ConnectionThread( CountDownLatch counter )
        {
            this.counter = counter;
        }


        @Override
        public void run()
        {
            try
            {
                for ( int i = 0; i < 100; i++ )
                {
                    LdapConnection connection = pool.getConnection();

                    connection.lookup( Dn.ROOT_DSE, "1.1 " );

                    pool.releaseConnection( connection );

                    counter.countDown();
                }
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }
        }
    }


    @BeforeEach
    public void setUp() throws Exception
    {
        int port = getLdapServer().getPort();

        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( Network.LOOPBACK_HOSTNAME );
        config.setLdapPort( port );
        config.setName( DEFAULT_ADMIN );
        config.setCredentials( DEFAULT_PASSWORD );
        PooledObjectFactory<LdapConnection> factory = new ValidatingPoolableLdapConnectionFactory( config );
        pool = new LdapConnectionPool( factory );
        pool.setTestOnBorrow( true );
        pool.setBlockWhenExhausted( !GenericObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED );
    }


    @AfterEach
    public void tearDown() throws Exception
    {
        pool.close();
    }


    /**
     * Test the creation of many connections
     */
    @Test
    public void testManyConnectionsUnlimited() throws Exception
    {
        pool.setMaxTotal( -1 );
        pool.setBlockWhenExhausted( false );

        CountDownLatch counter = new CountDownLatch( 10000 );

        for ( int i = 0; i < 100; i++ )
        {
            ConnectionThread thread = new ConnectionThread( counter );

            thread.start();
        }

        boolean result = counter.await( 100, TimeUnit.SECONDS );
        assertTrue( result );
    }


    /**
     * Test the creation of many connections
     */
    @Test
    public void testManyConnectionsBlocking() throws Exception
    {
        pool.setMaxTotal( 10 );
        pool.setBlockWhenExhausted( true );

        CountDownLatch counter = new CountDownLatch( 10000 );

        for ( int i = 0; i < 100; i++ )
        {
            ConnectionThread thread = new ConnectionThread( counter );

            thread.start();
        }

        boolean result = counter.await( 100, TimeUnit.SECONDS );
        assertTrue( result );
    }


    @Test
    @Disabled
    public void testRebind() throws Exception
    {
        LdapConnection connection = pool.getConnection();
        pool.releaseConnection( connection );

        long t0 = System.currentTimeMillis();
        long t00 = t0;

        for ( int i = 0; i < 10000; i++ )
        {
            // First, unbind
            try
            {
                if ( i % 100 == 0 )
                {
                    long t01 = t00;
                    t00 = System.currentTimeMillis();
                    System.out.println( "Iteration # " + i + " in " + ( t00 - t01 ) );
                }

                connection.unBind();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                throw e;
            }
            finally
            {
                assertNotNull( connection );
                pool.releaseConnection( connection );
            }

            // Then bind again
            try
            {
                connection = pool.getConnection();
                connection.bind( ServerDNConstants.ADMIN_SYSTEM_DN, "secret" );
            }
            catch ( Exception e )
            {
                System.out.println( "Failure after " + i + " iterations" );
                e.printStackTrace();
                throw e;
            }
            finally
            {
                assertNotNull( connection );
            }
        }

        long t1 = System.currentTimeMillis();

        System.out.println( "Time needed to bind/uinbind 10 000 connections : " + ( t1 - t0 ) );

        // terminate with an unbind
        try
        {
            connection.unBind();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw e;
        }
        finally
        {
            assertNotNull( connection );
            pool.releaseConnection( connection );
        }
    }


    @Test
    public void testSmallPool() throws Exception
    {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( Network.LOOPBACK_HOSTNAME );
        config.setLdapPort( getLdapServer().getPort() );
        config.setName( DEFAULT_ADMIN );
        config.setCredentials( DEFAULT_PASSWORD );
        ValidatingPoolableLdapConnectionFactory factory = new ValidatingPoolableLdapConnectionFactory( config );
        LdapConnectionPool pool = new LdapConnectionPool( factory );
        pool.setMaxTotal( 1 );
        pool.setTestOnBorrow( true );
        pool.setBlockWhenExhausted( GenericObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED );

        for ( int i = 0; i < 100; i++ )
        {
            LdapConnection connection = pool.getConnection();
            pool.releaseConnection( connection );
        }

        pool.close();
    }
}
