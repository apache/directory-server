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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.DefaultPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * A test class for the connection pool.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP") })
public class LightweightLdapConnectionPoolTest extends AbstractLdapTestUnit
{
    /** The connection pool */
    private LdapConnectionPool pool;

    /** The Constant DEFAULT_HOST. */
    private static final String DEFAULT_HOST = "localhost";

    /** The Constant DEFAULT_ADMIN. */
    private static final String DEFAULT_ADMIN = ServerDNConstants.ADMIN_SYSTEM_DN;

    /** The Constant DEFAULT_PASSWORD. */
    private static final String DEFAULT_PASSWORD = "secret";

    /**
     * A thread used to test the connection
     */
    private class ConnectionThread extends Thread
    {
        int threadNumber;
        CountDownLatch counter;


        public ConnectionThread( int threadNumber, CountDownLatch counter )
        {
            this.threadNumber = threadNumber;
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
                // Do nothing
            }
        }
    }


    @Before
    public void setUp() throws Exception
    {
        int port = getLdapServer().getPort();

        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( "localHost" );
        config.setLdapPort( port );
        config.setName( DEFAULT_ADMIN );
        config.setCredentials( DEFAULT_PASSWORD );
        PoolableObjectFactory<LdapConnection> factory = new DefaultPoolableLdapConnectionFactory( config );
        pool = new LdapConnectionPool( factory );
        pool.setTestOnBorrow( true );
        pool.setWhenExhaustedAction( GenericObjectPool.WHEN_EXHAUSTED_GROW );
    }


    @After
    public void tearDown() throws Exception
    {
        pool.close();
    }


    /**
     * Test the creation of many connections
     */
    @Test
    public void testManyConnections() throws Exception
    {
        CountDownLatch counter = new CountDownLatch( 10000 );

        long t0 = System.currentTimeMillis();

        for ( int i = 0; i < 100; i++ )
        {
            ConnectionThread thread = new ConnectionThread( i, counter );

            thread.start();
        }

        boolean result = counter.await( 100, TimeUnit.SECONDS );

        long t1 = System.currentTimeMillis();

//        System.out.println( "Time to create and use 10 000 connections = " + ( t1 - t0 ) );
    }


    @Test
    @Ignore
    public void testRebind() throws Exception
    {
        LdapConnection connection = pool.getConnection();
        pool.releaseConnection( connection );

        long t0 = System.currentTimeMillis();
        long t00 = t0;

        for ( int i = 0; i < 1000000; i++ )
        {
            // First, unbind
            try
            {
                if ( i % 10000 == 0 )
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
    @Ignore
    public void testRebindNoPool() throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( DEFAULT_HOST, getLdapServer().getPort() );
        connection.bind( ServerDNConstants.ADMIN_SYSTEM_DN, "secret" );

        long t0 = System.currentTimeMillis();

        for ( int i = 0; i < 10000; i++ )
        {
            if ( i % 100 == 0 )
            {
                System.out.println( "Iteration # " + i );
            }
            // First, unbind
            try
            {
                connection.unBind();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
                throw e;
            }

            //Thread.sleep( 5 );

            // Don't close the connection, we want to reuse it
            // Then bind again
            try
            {
                connection.bind( ServerDNConstants.ADMIN_SYSTEM_DN, "secret" );
            }
            catch ( Exception e )
            {
                System.out.println( "Failure after " + i + " iterations" );
                e.printStackTrace();
                throw e;
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
        }

        connection.close();
    }


    @Test
    public void testSmallPool() throws Exception
    {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( "localHost" );
        config.setLdapPort( getLdapServer().getPort() );
        config.setName( DEFAULT_ADMIN );
        config.setCredentials( DEFAULT_PASSWORD );
        PoolableObjectFactory<LdapConnection> factory = new DefaultPoolableLdapConnectionFactory( config );
        LdapConnectionPool pool = new LdapConnectionPool( factory );
        pool.setMaxActive( 1 );
        pool.setTestOnBorrow( true );
        pool.setWhenExhaustedAction( GenericObjectPool.WHEN_EXHAUSTED_FAIL );

        for ( int i = 0; i < 100; i++ )
        {
            LdapConnection connection = pool.getConnection();
            pool.releaseConnection( connection );
        }
    }
}
