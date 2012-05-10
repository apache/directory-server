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


import java.io.Serializable;

import javax.security.auth.kerberos.KerberosPrincipal;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.AbstractPolicy;

import org.apache.directory.shared.kerberos.KerberosTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * "The replay cache will store at least the server name, along with the client name,
 * time, and microsecond fields from the recently-seen authenticators, and if a
 * matching tuple is found, the KRB_AP_ERR_REPEAT error is returned."
 * 
 * We will store the entries in Ehacache instance
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ReplayCacheImpl implements ReplayCache
{

    private static final Logger LOG = LoggerFactory.getLogger( ReplayCacheImpl.class );

    /** ehcache based storage to store the entries */
    private Cache cache;

    /** default clock skew */
    private static final long DEFAULT_CLOCK_SKEW = 5 * KerberosTime.MINUTE;

    /** The clock skew */
    private long clockSkew = DEFAULT_CLOCK_SKEW;

    /**
     * A structure to hold an entry
     */
    public class ReplayCacheEntry implements Serializable
    {
        private static final long serialVersionUID = 1L;

        /** The server principal */
        private KerberosPrincipal serverPrincipal;

        /** The client principal */
        private KerberosPrincipal clientPrincipal;

        /** The client time */
        private KerberosTime clientTime;

        /** The client micro seconds */
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


        /**
         * @return create a key to be used while storing in the cache
         */
        private String createKey()
        {
            StringBuilder sb = new StringBuilder();
            sb.append( ( clientPrincipal == null ) ? "null" : clientPrincipal.getName() );
            sb.append( '#' );
            sb.append( ( serverPrincipal == null ) ? "null" : serverPrincipal.getName() );
            sb.append( '#' );
            sb.append( ( clientTime == null ) ? "null" : clientTime.getDate() );
            sb.append( '#' );
            sb.append( clientMicroSeconds );

            return sb.toString();
        }
    }

    /**
     * an expiration policy based on the clockskew
     */
    private class ClockskewExpirationPolicy extends AbstractPolicy
    {

        /**
         * {@inheritDoc}
         */
        public String getName()
        {
            return "CLOCK-SKEW";
        }


        /**
         * {@inheritDoc}
         */
        public boolean compare( Element element1, Element element2 )
        {
            ReplayCacheEntry entry = ( ReplayCacheEntry ) element2.getValue();

            if ( entry.isOutsideClockSkew( clockSkew ) )
            {
                return true;
            }

            return false;
        }
    }


    /**
     * Creates a new instance of InMemoryReplayCache. Sets the
     * delay between each cleaning run to 5 seconds.
     */
    public ReplayCacheImpl( Cache cache )
    {
        this.cache = cache;
        this.cache.setMemoryStoreEvictionPolicy( new ClockskewExpirationPolicy() );
    }


    /**
     * Creates a new instance of InMemoryReplayCache. Sets the
     * delay between each cleaning run to 5 seconds. Sets the
     * clockSkew to the given value
     * 
     * @param clockSkew the allowed skew (milliseconds)
     */
    public ReplayCacheImpl( Cache cache, long clockSkew )
    {
        this.cache = cache;
        this.clockSkew = clockSkew;
        this.cache.setMemoryStoreEvictionPolicy( new ClockskewExpirationPolicy() );
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
     * Check if an entry is a replay or not.
     */
    public synchronized boolean isReplay( KerberosPrincipal serverPrincipal, KerberosPrincipal clientPrincipal,
        KerberosTime clientTime, int clientMicroSeconds )
    {

        ReplayCacheEntry entry = new ReplayCacheEntry( serverPrincipal, clientPrincipal, clientTime, clientMicroSeconds );

        Element element = cache.get( entry.createKey() );

        if ( element == null )
        {
            return false;
        }

        entry = ( ReplayCacheEntry ) element.getValue();

        if ( serverPrincipal.equals( entry.serverPrincipal ) &&
            clientTime.equals( entry.clientTime ) &&
            ( clientMicroSeconds == entry.clientMicroSeconds ) )
        {
            return true;
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
        ReplayCacheEntry entry = new ReplayCacheEntry( serverPrincipal, clientPrincipal, clientTime, clientMicroSeconds );

        Element element = new Element( entry.createKey(), entry );
        cache.put( element );
    }


    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        LOG.debug( "removing all the elements from cache" );
        cache.removeAll();
    }
}
