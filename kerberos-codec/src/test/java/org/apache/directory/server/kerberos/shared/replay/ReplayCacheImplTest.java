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
package org.apache.directory.server.kerberos.shared.replay;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.directory.junit.tools.MultiThreadedMultiInvoker;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * Test the InMemory replay cache
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class ReplayCacheImplTest
{
    @Rule
    public MultiThreadedMultiInvoker i = new MultiThreadedMultiInvoker( MultiThreadedMultiInvoker.THREADSAFE );


    /**
     * Test that the cache is working well.
     * We will create 4 new entries, with 4 different serverPrincipals.
     * Those 4 entries should remain in cache and replay should be detected
     * After expiration time the entries should have been expired.
     */
    @Test
    public void testCacheSetting() throws Exception
    {
        try
        {
            long clockSkew = 1000; // 1 sec

            ReplayCacheImpl cache = new ReplayCacheImpl( clockSkew );

            // Inject 4 entries
            List<Triple<KerberosPrincipal, KerberosPrincipal, KerberosTime>> triples = new ArrayList<>();
            for ( int i = 0; i < 4; i++ )
            {
                KerberosPrincipal serverPrincipal = new KerberosPrincipal( "server" + i + "@APACHE.ORG",
                    PrincipalNameType.KRB_NT_PRINCIPAL.getValue() );
                KerberosPrincipal clientPrincipal = new KerberosPrincipal( "client" + i + "@APACHE.ORG",
                    PrincipalNameType.KRB_NT_PRINCIPAL.getValue() );
                KerberosTime clientTime = new KerberosTime( System.currentTimeMillis() );

                cache.save( serverPrincipal, clientPrincipal, clientTime, 0 );

                triples.add( Triple.of( serverPrincipal, clientPrincipal, clientTime ) );
            }

            // Get the 4 cache keys
            Set<String> keys = cache.cache.asMap().keySet();
            assertEquals( 4, keys.size() );
            assertEquals( 4L, cache.cache.estimatedSize() );

            // Wait a bit without exceeding timetolive time
            Thread.sleep( 200L );

            // Verify that cache entries are valid and replay is detected
            for ( String key : keys )
            {
                assertNotNull( cache.cache.getIfPresent( key ) );
            }
            for ( Triple<KerberosPrincipal, KerberosPrincipal, KerberosTime> triple : triples )
            {
                boolean isReplay = cache.isReplay( triple.getLeft(), triple.getMiddle(), triple.getRight(), 0 );
                assertTrue( isReplay );
            }

            // Wait till the timetolive time exceeds
            Thread.sleep( 1000L );

            // Verify that cache entries are expired and no replay is detected
            for ( Triple<KerberosPrincipal, KerberosPrincipal, KerberosTime> triple : triples )
            {
                boolean isReplay = cache.isReplay( triple.getLeft(), triple.getMiddle(), triple.getRight(), 0 );
                assertFalse( isReplay );
            }

            // then access the cache so that the objects present in the cache will be expired
            for ( String key : keys )
            {
                assertNull( cache.cache.getIfPresent( key ) );
            }

            // After forced cache cleanup the size is recalculated
            cache.cache.cleanUp();
            assertEquals( 0L, cache.cache.estimatedSize() );
        }
        finally
        {
        }
    }
}
