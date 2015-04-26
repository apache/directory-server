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

package org.apache.directory.shared.client.api.operations;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateLdapServer(transports =
    { @CreateTransport(protocol = "LDAP"), @CreateTransport(protocol = "LDAPS") })
public class ConcurrentSearchAndUnbindTest extends AbstractLdapTestUnit
{

    private static final int NUM_ROUNDS = 50;


    /**
     * DIRAPI-236
     */
    @Test
    @Ignore
    public void testConcurrentSearchAndUnbind() throws Exception
    {
        final LdapConnection connection = new LdapNetworkConnection( "localhost", getLdapServer().getPort() );

        ExecutorService pool = Executors.newFixedThreadPool( 2 );

        final AtomicLong unbindCount = new AtomicLong();
        final AtomicLong bindCount = new AtomicLong();
        final AtomicLong searchCount = new AtomicLong();
        final AtomicLong resultCount = new AtomicLong();

        // Thread that permanently unbinds the connection
        Callable<Void> unbindCallable = new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                while ( true )
                {
                    try
                    {
                        System.out.println( "unbind thread: unbind" );
                        unbindCount.incrementAndGet();
                        connection.unBind();
                        //connection.close();
                    }
                    catch ( Exception e )
                    {
                        // expected when other thread didn't bind
                        //System.out.println( "unbind thread: " + e.getMessage() );
                    }
                    Thread.sleep( 1000 );
                }
            }
        };
        Future<Void> unbindFuture = pool.submit( unbindCallable );

        // Thread that permanently uses the connection, until the other thread unbinds
        Callable<Void> bindAndSearchCallable = new Callable<Void>()
        {
            @Override
            public Void call()
            {
                for ( int i = 0; i < NUM_ROUNDS; i++ )
                {
                    System.out.println( "search thread: round " + i );
                    try
                    {
                        bindCount.incrementAndGet();
                        connection.bind( "uid=admin,ou=system", "secret" );

                        while ( true )
                        {
                            searchCount.incrementAndGet();
                            EntryCursor cursor = connection.search( new Dn( "ou=system" ), "(objectClass=*)",
                                SearchScope.SUBTREE, "*" );
                            while ( cursor.next() )
                            {
                                resultCount.incrementAndGet();
                                cursor.get();
                            }
                            cursor.close();
                        }
                    }
                    catch ( Throwable e )
                    {
                        // expected when other thread unbinds
                        //System.out.println( "search thread: " + e.getMessage() );
                    }
                }
                return null;
            }
        };
        Future<Void> bindAndSearchFuture = pool.submit( bindAndSearchCallable );

        // wait till bindAndSearchFuture is done
        bindAndSearchFuture.get( 600, TimeUnit.SECONDS );

        // assert counters
        System.out.println( "unbindCount: " + unbindCount );
        System.out.println( "bindCount: " + bindCount );
        System.out.println( "searchCount: " + searchCount );
        System.out.println( "resultCount: " + resultCount );
        assertTrue( unbindCount.get() >= NUM_ROUNDS );
        assertEquals( NUM_ROUNDS, bindCount.get() );
        assertTrue( searchCount.get() > 0 );
        assertTrue( resultCount.get() > 0 );

        // cleanup
        pool.shutdownNow();
        pool.awaitTermination( 60, TimeUnit.SECONDS );
        assertTrue( unbindFuture.isDone() );
        assertTrue( bindAndSearchFuture.isDone() );
        connection.close();
    }
}
