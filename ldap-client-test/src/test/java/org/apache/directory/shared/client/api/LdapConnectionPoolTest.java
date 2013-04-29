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


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.PoolableLdapConnectionFactory;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.After;
import org.junit.Before;
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
public class LdapConnectionPoolTest extends AbstractLdapTestUnit
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
        PoolableLdapConnectionFactory factory = new PoolableLdapConnectionFactory( config );
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

        System.out.println( "Time to create and use 10 000 connections = " + ( t1 - t0 ) );
    }
}
