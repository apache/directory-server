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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalLong;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.junit.tools.MultiThreadedMultiInvoker;
import org.apache.directory.server.kerberos.shared.replay.ReplayCacheImpl.ReplayCacheEntry;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
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
     * Test that the cache is working well. We will create a new entry
     * every 500 ms, with 4 different serverPrincipals.
     *
     * After this period of time, we should only have 2 entries in the cache
     */
    @Test
    public void testCacheSetting() throws Exception
    {
        CacheManager cacheManager = null;

        try
        {
            long clockSkew = 1000; // 1 sec

            CachingProvider cachingProvider = Caching.getCachingProvider();
            CacheManager cm = cachingProvider.getCacheManager();
            CaffeineConfiguration<String, ReplayCacheEntry> config = new CaffeineConfiguration<>();
            config.setMaximumSize( OptionalLong.of( 4L ) );
            config.setExpireAfterAccess( OptionalLong.of( 1000L * 1000L * 1000L) );
            Cache<String, ReplayCacheEntry> kdcReplayCache = cm.createCache( "kdcReplayCache", config );

            ReplayCacheImpl cache = new ReplayCacheImpl( kdcReplayCache, clockSkew );

            int i = 0;

            // Inject 4 entries
            while ( i < 4 )
            {
                KerberosPrincipal serverPrincipal = new KerberosPrincipal( "server" + i + "@APACHE.ORG",
                    PrincipalNameType.KRB_NT_PRINCIPAL.getValue() );
                KerberosPrincipal clientPrincipal = new KerberosPrincipal( "client" + i + "@APACHE.ORG",
                    PrincipalNameType.KRB_NT_PRINCIPAL.getValue() );

                cache.save( serverPrincipal, clientPrincipal, new KerberosTime( System.currentTimeMillis() ), 0 );

                i++;
            }

            List<String> keys = new ArrayList<>();
            Iterator<Cache.Entry<String, ReplayCacheEntry>> it = kdcReplayCache.iterator();
            
            while (it.hasNext())
            {
                keys.add(it.next().getKey());
            }

            // We should have 4 entries
            assertEquals( 4, keys.size() );
            assertTrue( keys.size() != 0 );

            // Wait till the timetolive time exceeds
            Thread.sleep( 1200 );

            // then access the cache so that the objects present in the cache will be expired
            for ( String k : keys )
            {
                assertNull( kdcReplayCache.get( k ) );
            }

            assertFalse( kdcReplayCache.iterator().hasNext() );
        }
        finally
        {
            if ( cacheManager != null && !cacheManager.isClosed() )
            {
                cacheManager.close();
            }
        }
    }
}
