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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


/**
 * "The replay cache will store at least the server name, along with the client name,
 * time, and microsecond fields from the recently-seen authenticators, and if a
 * matching tuple is found, the KRB_AP_ERR_REPEAT error is returned."
 * 
 * We will store the entries using an HashMap which key will be the client
 * principal, and we will store a list of entries for each client principal.
 * 
 * A thread will run every N seconds to clean the cache from entries out of the 
 * clockSkew
 *    
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InMemoryReplayCache extends Thread implements ReplayCache
{
    /** Stores the entries in memory */
    private Map<KerberosPrincipal, List<ReplayCacheEntry>> cache = new HashMap<KerberosPrincipal, List<ReplayCacheEntry>>();

    /** default clock skew */
    private static final long DEFAULT_CLOCK_SKEW = 5 * KerberosTime.MINUTE;
    
    /** The clock skew */
    private long clockSkew = DEFAULT_CLOCK_SKEW;

    /** The default delay between each run of the cleaning process : 5 s */
    private static long DEFAULT_DELAY = 5 * 1000;  
    
    /** The delay to wait between each cache cleaning */
    private long delay;

    /**
     * A structure to hold an entry
     */
    public class ReplayCacheEntry
    {
        private KerberosPrincipal serverPrincipal;
        private KerberosPrincipal clientPrincipal;
        private KerberosTime clientTime;
        private int clientMicroSeconds;


        /**
         * Creates a new instance of ReplayCacheEntry.
         * 
         * @param serverPrincipal 
         * @param clientPrincipal 
         * @param clientTime 
         * @param clientMicroSeconds 
         */
        public ReplayCacheEntry( KerberosPrincipal serverPrincipal, KerberosPrincipal clientPrincipal,
            KerberosTime clientTime, int clientMicroSeconds )
        {
            this.serverPrincipal = serverPrincipal;
            this.clientPrincipal = clientPrincipal;
            this.clientTime = clientTime;
            this.clientMicroSeconds = clientMicroSeconds;
        }


        /**
         * Returns whether this {@link ReplayCacheEntry} is equal to another {@link ReplayCacheEntry}.
         * {@link ReplayCacheEntry}'s are equal when the server name, client name, client time, and
         * the client microseconds are equal.
         *
         * @param that
         * @return true if the ReplayCacheEntry's are equal.
         */
        public boolean equals( ReplayCacheEntry that )
        {
            return serverPrincipal.equals( that.serverPrincipal ) && clientPrincipal.equals( that.clientPrincipal )
                && clientTime.equals( that.clientTime ) && clientMicroSeconds == that.clientMicroSeconds;
        }


        /**
         * Returns whether this {@link ReplayCacheEntry} is older than a given time.
         *
         * @param clockSkew
         * @return true if the {@link ReplayCacheEntry}'s client time is outside the clock skew time.
         */
        public boolean isOutsideClockSkew( long clockSkew )
        {
            return !clientTime.isInClockSkew( clockSkew );
        }
    }

    
    /**
     * Creates a new instance of InMemoryReplayCache. Sets the
     * delay between each cleaning run to 5 seconds.
     */
    public InMemoryReplayCache()
    {
        cache = new HashMap<KerberosPrincipal, List<ReplayCacheEntry>>();
        delay = DEFAULT_DELAY;
        this.start();
    }
    
    
    /**
     * Creates a new instance of InMemoryReplayCache. Sets the
     * delay between each cleaning run to 5 seconds. Sets the
     * clockSkew to the given value
     * 
     * @param clockSkew the allowed skew (milliseconds)
     */
    public InMemoryReplayCache( long clockSkew )
    {
        cache = new HashMap<KerberosPrincipal, List<ReplayCacheEntry>>();
        delay = DEFAULT_DELAY;
        this.clockSkew = clockSkew;
        this.start();
    }
    
    
    /**
     * Creates a new instance of InMemoryReplayCache. Sets the
     * clockSkew to the given value, and set the cleaning thread 
     * kick off delay
     * 
     * @param clockSkew the allowed skew (milliseconds)
     * @param delay the interval between each run of the cache 
     * cleaning thread (milliseconds)
     */
    public InMemoryReplayCache( long clockSkew, int delay  )
    {
        cache = new HashMap<KerberosPrincipal, List<ReplayCacheEntry>>();
        this.delay = (long)delay;
        this.clockSkew = clockSkew;
        this.start();
    }
    
    
    /**
     * Creates a new instance of InMemoryReplayCache. Sets the
     * delay between each cleaning run to 5 seconds. Sets the 
     * cleaning thread kick off delay
     * 
     * @param delay the interval between each run of the cache 
     * cleaning thread (milliseconds).
     */
    public InMemoryReplayCache( int delay )
    {
        cache = new HashMap<KerberosPrincipal, List<ReplayCacheEntry>>();
        this.delay = (long)delay;
        this.clockSkew = DEFAULT_CLOCK_SKEW;
    }
    
    
    /**
     * Sets the clock skew.
     *
     * @param clockSkew
     */
    public void setClockSkew( long clockSkew )
    {
        this.clockSkew = clockSkew;
    }

    
    /**
     * Set the delay between each cleaning thread run.
     *
     * @param delay delay in milliseconds
     */
    public void setDelay( long delay )
    {
        this.delay = delay;
    }

    /**
     * Check if an entry is a replay or not.
     */
    public synchronized boolean isReplay( KerberosPrincipal serverPrincipal, KerberosPrincipal clientPrincipal,
        KerberosTime clientTime, int clientMicroSeconds )
    {
        List<ReplayCacheEntry> entries = cache.get( clientPrincipal );
        
        if ( ( entries == null ) || ( entries.size() == 0 ) )
        {
            return false;
        }
        
        for ( ReplayCacheEntry entry:entries )
        {
            if ( serverPrincipal.equals( entry.serverPrincipal ) && 
                 clientTime.equals( entry.clientTime ) && 
                 (clientMicroSeconds == entry.clientMicroSeconds ) )
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Add a new entry into the cache. A thread will clean all the timed out
     * entries.
     */
    public synchronized void save( KerberosPrincipal serverPrincipal, KerberosPrincipal clientPrincipal,
        KerberosTime clientTime, int clientMicroSeconds )
    {
        List<ReplayCacheEntry> entries = cache.get( clientPrincipal );
        
        if ( entries == null )
        {
            entries = new ArrayList<ReplayCacheEntry>();
        }
        
        entries.add( new ReplayCacheEntry( serverPrincipal, clientPrincipal, clientTime, clientMicroSeconds ) );
        
        cache.put( clientPrincipal, entries );
    }

    
    public Map<KerberosPrincipal, List<ReplayCacheEntry>> getCache()
    {
        return cache;
    }
    
    /**
     * A method to remove all the expired entries from the cache.
     */
    private synchronized void cleanCache()
    {
        Collection<List<ReplayCacheEntry>> entryList = cache.values();
        
        if ( ( entryList == null ) || ( entryList.size() == 0 ) )
        {
            return;
        }
        
        for ( List<ReplayCacheEntry> entries:entryList )
        {
            if ( ( entries == null ) || ( entries.size() == 0 ) )
            {
                continue;
            }
            
            Iterator<ReplayCacheEntry> iterator = entries.iterator();
            
            while ( iterator.hasNext() )
            {
                ReplayCacheEntry entry = iterator.next();
                
                if ( entry.isOutsideClockSkew( clockSkew ) )
                {
                    iterator.remove();
                }
                else
                {
                    break;
                }
            }
        }
    }
    
    
    /** 
     * The cleaning thread. It runs every N seconds.
     */
    public void run()
    {
        while ( true )
        {
            try
            {
                Thread.sleep( delay );
                
                cleanCache();
            }
            catch ( InterruptedException ie )
            {
                return;
            }
        }
    }
}
