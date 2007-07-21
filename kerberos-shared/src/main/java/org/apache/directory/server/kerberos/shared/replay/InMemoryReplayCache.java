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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;


/**
 * "The replay cache will store at least the server name, along with the client name,
 * time, and microsecond fields from the recently-seen authenticators, and if a
 * matching tuple is found, the KRB_AP_ERR_REPEAT error is returned."
 *    
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InMemoryReplayCache implements ReplayCache
{
    private static final long TWO_WEEKS = 1000 * 60 * 60 * 24 * 14;

    private List<ReplayCacheEntry> list = new ArrayList<ReplayCacheEntry>();


    public synchronized boolean isReplay( KerberosPrincipal serverPrincipal, KerberosPrincipal clientPrincipal,
        KerberosTime clientTime, int clientMicroSeconds )
    {
        ReplayCacheEntry testEntry = new ReplayCacheEntry( serverPrincipal, clientPrincipal, clientTime,
            clientMicroSeconds );

        Iterator<ReplayCacheEntry> it = list.iterator();
        while ( it.hasNext() )
        {
            ReplayCacheEntry entry = it.next();
            if ( entry.equals( testEntry ) )
            {
                return true;
            }
        }

        return false;
    }


    public synchronized void save( KerberosPrincipal serverPrincipal, KerberosPrincipal clientPrincipal,
        KerberosTime clientTime, int clientMicroSeconds )
    {
        list.add( new ReplayCacheEntry( serverPrincipal, clientPrincipal, clientTime, clientMicroSeconds ) );
        purgeExpired();
    }


    /*
     * TODO - age needs to be configurable; requires store
     */
    private synchronized void purgeExpired()
    {
        long now = new Date().getTime();

        KerberosTime age = new KerberosTime( now - TWO_WEEKS );

        Iterator<ReplayCacheEntry> it = list.iterator();
        while ( it.hasNext() )
        {
            ReplayCacheEntry entry = it.next();
            if ( entry.olderThan( age ) )
            {
                list.remove( entry );
            }
        }
    }

    private class ReplayCacheEntry
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
         * @param time
         * @return true if the {@link ReplayCacheEntry} is older.
         */
        public boolean olderThan( KerberosTime time )
        {
            return time.greaterThan( clientTime );
        }
    }
}
