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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.directory.api.ldap.model.entry.Entry;
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
    { @CreateTransport(protocol = "LDAP", port = 10389) })
public class LightweightLdapConnectionPoolTest extends AbstractLdapTestUnit
{
    /** The connection pool */
    private LdapConnectionPool pool;

    /** The Constant DEFAULT_ADMIN. */
    private static final String DEFAULT_ADMIN = ServerDNConstants.ADMIN_SYSTEM_DN;

    /** The Constant DEFAULT_PASSWORD. */
    private static final String DEFAULT_PASSWORD = "secret";

    /**
     * A thread used to test the connection taken from a pool
     */
    private class ConnectionThreadPool extends Thread
    {
        CountDownLatch counter;
        int nbIterations;
        boolean success = true;
        LdapConnectionPool poolNoIdle;


        public ConnectionThreadPool( LdapConnectionPool poolNoIdle, int nbIterations, CountDownLatch counter )
        {
            this.counter = counter;
            this.nbIterations = nbIterations;
            this.poolNoIdle = poolNoIdle;
        }


        @Override
        public void run()
        {
            int i = 0;
            long t0 = System.currentTimeMillis();

            for ( i = 0; i < nbIterations; i++ )
            {
                try
                {
                    long count = counter.getCount();

                    if ( i % 10000 == 0 )
                    {
                        System.out.println( "iteration # " + count );
                    }

                    LdapConnection connection = poolNoIdle.getConnection();

                    //connection.bind( DEFAULT_ADMIN, DEFAULT_PASSWORD );
                    Entry entry = connection.lookup( "uid=admin,ou=system", "*" );

                    poolNoIdle.releaseConnection( connection );

                    counter.countDown();
                }
                catch ( Exception e )
                {
                    System.out
                        .println( this + " failed to get a connection on iteration " + i + " : " + e.getMessage() );
                    e.printStackTrace();
                    success = false;
                    break;
                }
            }
            long t1 = System.currentTimeMillis();

            if ( success )
            {
                System.out.println( "Thread " + this + " completed in " + ( t1 - t0 ) + "ms" );
            }
        }
    }

    /**
     * A thread used to test the connection, using no pool
     */
    private class ConnectionThreadNoPool extends Thread
    {
        CountDownLatch counter;
        int nbIterations;
        boolean success = true;


        public ConnectionThreadNoPool( int nbIterations, CountDownLatch counter )
        {
            this.counter = counter;
            this.nbIterations = nbIterations;
        }


        @Override
        public void run()
        {
            int i = 0;
            LdapConnectionConfig config = new LdapConnectionConfig();
            
            try
            {
                config.setLdapHost( InetAddress.getLocalHost().getHostName() );
            }
            catch ( UnknownHostException e1 )
            {
                e1.printStackTrace();
            }
            
            config.setLdapPort( 10389 );
            config.setName( DEFAULT_ADMIN );
            config.setCredentials( DEFAULT_PASSWORD );
            config.setTimeout( 30000 );

            long t0 = System.currentTimeMillis();

            for ( i = 0; i < nbIterations; i++ )
            {
                try
                {
                    if ( i % 10000 == 0 )
                    {
                        System.out.println( "iteration # " + i + " for thread " + this + " in "
                            + ( System.currentTimeMillis() - t0 ) );
                    }

                    //this.sleep( 1 );

                    LdapConnection connection = new LdapNetworkConnection( config );
                    connection.bind();

                    Entry entry = connection.lookup( Dn.ROOT_DSE, "1.1 " );

                    connection.unBind();
                    //connection.close();

                    counter.countDown();
                }
                catch ( Exception e )
                {
                    System.out
                        .println( this + " failed to get a connection on iteration " + i + " : " + e.getMessage()
                            + " in " + ( System.currentTimeMillis() - t0 ) );
                    e.printStackTrace();
                    success = false;
                }
            }
            long t1 = System.currentTimeMillis();

            if ( success )
            {
                System.out.println( "Thread " + this + " completed in " + ( t1 - t0 ) + "ms" );
                //}
                //else
                //{
            }
        }
    }


    @Before
    public void setUp() throws Exception
    {
        int port = getLdapServer().getPort();

        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( InetAddress.getLocalHost().getHostName() );
        config.setLdapPort( port );
        config.setName( DEFAULT_ADMIN );
        config.setCredentials( DEFAULT_PASSWORD );
        config.setTimeout( 30000 );
        PoolableObjectFactory<LdapConnection> factory = new DefaultPoolableLdapConnectionFactory( config );
        pool = new LdapConnectionPool( factory );
        pool.setTestOnBorrow( true );
        pool.setWhenExhaustedAction( GenericObjectPool.WHEN_EXHAUSTED_GROW );
        pool.setMaxIdle( 0 );

        System.out.println( "Max Active connections =: " + pool.getMaxActive() );
    }


    @After
    public void tearDown() throws Exception
    {
        pool.close();
    }


    /**
     * Test the creation of many connections, using a pool that does not let 
     * connections becoming idle
     */
    @Test
    @Ignore
    public void testManyConnectionsPoolNoIdle() throws Exception
    {
        int port = getLdapServer().getPort();

        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( InetAddress.getLocalHost().getHostName() );
        config.setLdapPort( port );
        config.setName( DEFAULT_ADMIN );
        config.setCredentials( DEFAULT_PASSWORD );
        config.setTimeout( 30000 );
        PoolableObjectFactory<LdapConnection> factory = new DefaultPoolableLdapConnectionFactory( config );
        LdapConnectionPool poolNoIdle = new LdapConnectionPool( factory );
        poolNoIdle.setTestOnBorrow( true );
        poolNoIdle.setWhenExhaustedAction( GenericObjectPool.WHEN_EXHAUSTED_GROW );
        poolNoIdle.setMaxIdle( 0 );

        System.out.println( "Max Active connections =: " + pool.getMaxActive() );

        for ( int j = 0; j < 1; j++ )
        {
            System.out.println( "-------------------" );
            System.out.println( "Iteration " + j );
            int nbIterations = 20000;
            int nbThreads = 10;
            CountDownLatch counter = new CountDownLatch( nbIterations * nbThreads );

            long t0 = System.currentTimeMillis();

            for ( int i = 0; i < nbThreads; i++ )
            {
                ConnectionThreadPool thread = new ConnectionThreadPool( poolNoIdle, nbIterations, counter );

                thread.start();
            }

            boolean result = counter.await( 300, TimeUnit.SECONDS );

            long t1 = System.currentTimeMillis();

            System.out.println( "Time to create and use " + nbIterations + " connections with " + nbThreads
                + "  threads = "
                + ( t1 - t0 ) );
        }
    }


    /**
     * Test the creation of many connections, using a standard pool
     */
    @Test
    @Ignore
    public void testManyConnectionsPool() throws Exception
    {
        int port = getLdapServer().getPort();

        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( InetAddress.getLocalHost().getHostName() );
        config.setLdapPort( port );
        config.setName( DEFAULT_ADMIN );
        config.setCredentials( DEFAULT_PASSWORD );
        config.setTimeout( 30000 );
        PoolableObjectFactory<LdapConnection> factory = new DefaultPoolableLdapConnectionFactory( config );
        LdapConnectionPool poolWithIdle = new LdapConnectionPool( factory );
        poolWithIdle.setTestOnBorrow( true );
        poolWithIdle.setWhenExhaustedAction( GenericObjectPool.WHEN_EXHAUSTED_GROW );

        System.out.println( "Max Active connections =: " + pool.getMaxActive() );

        for ( int j = 0; j < 1; j++ )
        {
            int nbIterations = 20000;
            int nbThreads = 100;
            CountDownLatch counter = new CountDownLatch( nbIterations * nbThreads );

            long t0 = System.currentTimeMillis();

            for ( int i = 0; i < nbThreads; i++ )
            {
                ConnectionThreadPool thread = new ConnectionThreadPool( poolWithIdle, nbIterations, counter );

                thread.start();
            }

            boolean result = counter.await( 300, TimeUnit.SECONDS );

            long t1 = System.currentTimeMillis();

            System.out.println( "Time to create and use " + nbIterations * nbThreads + " connections with " + nbThreads
                + "  threads = "
                + ( t1 - t0 ) );
        }
    }


    /**
     * Test the creation of many connections, not using a pool.
     * This test is very dependent on the TIME_WAIT duration, which can
     * be set by changing the net.inet.tcp.msl parameter :
     * <pre>
     * on Mac OSX :
     * $ sudo sysctl -w net.inet.tcp.msl=500
     * on LINUX :
     * $ sudo echo "1" > /proc/sys/net/ipv4/tcp_fin_timeout
     * </pre>
     * Note that this parameter is *not* to be made permanent. There is no
     * reason for creating ten of thousands of client connections, except for
     * a benchmark.
     */
    @Test
    @Ignore
    public void testManyConnectionsNoPool() throws Exception
    {
        for ( int j = 0; j < 1; j++ )
        {
            System.out.println( "-------------------" );
            System.out.println( "Iteration " + j );
            int nbIterations = 20000;
            int nbThreads = 1;
            CountDownLatch counter = new CountDownLatch( nbIterations * nbThreads );

            long t0 = System.currentTimeMillis();

            for ( int i = 0; i < nbThreads; i++ )
            {
                ConnectionThreadNoPool thread = new ConnectionThreadNoPool( nbIterations, counter );

                thread.start();
            }

            boolean result = counter.await( 3000, TimeUnit.SECONDS );

            assertEquals( 0, counter.getCount() );

            long t1 = System.currentTimeMillis();

            System.out.println( "Time to create and use " + nbIterations + " connections with " + nbThreads
                + "  threads = "
                + ( t1 - t0 ) );
        }
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
        LdapConnection connection = new LdapNetworkConnection( InetAddress.getLocalHost().getHostName(), getLdapServer().getPort() );
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
        config.setLdapHost( InetAddress.getLocalHost().getHostName() );
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
