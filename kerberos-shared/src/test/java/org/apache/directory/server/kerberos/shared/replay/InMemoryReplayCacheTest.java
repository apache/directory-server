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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.junit.tools.MultiThreadedMultiInvoker;
import org.apache.directory.server.kerberos.shared.replay.InMemoryReplayCache.ReplayCacheEntry;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.PrincipalNameType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the InMemory replay cache
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class InMemoryReplayCacheTest
{
    @Rule
    public MultiThreadedMultiInvoker i = new MultiThreadedMultiInvoker( MultiThreadedMultiInvoker.THREADSAFE );

    /**
     * Test that the cache is working well. We will create a new entry
     * every 20 ms, with 10 different serverPrincipals.
     * 
     * After this period of time, we should only have 25 entries in the cache
     */
    @Test
    public void testCacheSetting() throws Exception
    {
        int delay = 500;
        long clockSkew = 100;
        
        // Set a delay of 500 ms and a clock skew of 100 ms
        InMemoryReplayCache cache = new InMemoryReplayCache( clockSkew, delay );
        
        // Loop for 2 seconds, then check that the cache is clean
        int i = 0;
        int nbClient = 20;
        int nbServer = 10;
        
        // Inject 100 entries, one every 20 ms
        while ( i < 100 )
        {
            KerberosPrincipal serverPrincipal = new KerberosPrincipal( "server" + i%nbServer + "@APACHE.ORG", PrincipalNameType.KRB_NT_PRINCIPAL.getValue() );
            KerberosPrincipal clientPrincipal = new KerberosPrincipal( "client" + i%nbClient + "@APACHE.ORG", PrincipalNameType.KRB_NT_PRINCIPAL.getValue() );
            
            cache.save( serverPrincipal, clientPrincipal, new KerberosTime( System.currentTimeMillis() ), 0 );
            
            Thread.sleep( 20 );
            i++;
        }
        
        Map<KerberosPrincipal, List<ReplayCacheEntry>> map = cache.getCache();

        // We should have 20 List of entries, as we have injected 20 different
        // clientPrincipals
        assertEquals( nbClient, map.size() );
        
        int nbEntries = 0;
        
        // Loop into the cache to see how many entries we have
        Collection<List<ReplayCacheEntry>> entryList = map.values();
        
        for ( List<ReplayCacheEntry> entries:entryList )
        {
            if ( ( entries == null ) || ( entries.size() == 0 ) )
            {
                continue;
            }
            
            Iterator<ReplayCacheEntry> iterator = entries.iterator();
            
            while ( iterator.hasNext() )
            {
                iterator.next();
                nbEntries ++;
            }
        }

        // We should have some
        assertNotNull( nbEntries );
        assertTrue(nbEntries > 0);
        
        // Wait another delay, so that the cleaning thread will be kicked off
        Thread.sleep( delay + 50 );
        // It's not guaranteed that the thread run, so invoke the method manually
        cache.cleanCache();
        
        nbEntries = 0;
        
        for ( List<ReplayCacheEntry> entries:entryList )
        {
            if ( ( entries == null ) || ( entries.size() == 0 ) )
            {
                continue;
            }
            
            Iterator<ReplayCacheEntry> iterator = entries.iterator();
            
            while ( iterator.hasNext() )
            {
                iterator.next();
                nbEntries ++;
            }
        }

        // We should not have anymore entry in the cache
        assertEquals( 0, nbEntries );

        // Stop background thread
        cache.interrupt();
    }
}
