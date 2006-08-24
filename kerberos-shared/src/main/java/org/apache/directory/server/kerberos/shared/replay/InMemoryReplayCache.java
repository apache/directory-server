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


public class InMemoryReplayCache implements ReplayCache
{
    private static final long TWO_WEEKS = 1000 * 60 * 60 * 24 * 14;

    private List list = new ArrayList();


    public synchronized boolean isReplay( KerberosTime clientTime, KerberosPrincipal clientPrincipal )
    {
        ReplayCacheEntry testEntry = new ReplayCacheEntry( clientTime, clientPrincipal );
        Iterator it = list.iterator();
        while ( it.hasNext() )
        {
            ReplayCacheEntry entry = ( ReplayCacheEntry ) it.next();
            if ( entry.equals( testEntry ) )
            {
                return true;
            }
        }
        return false;
    }


    public synchronized void save( KerberosTime clientTime, KerberosPrincipal clientPrincipal )
    {
        list.add( new ReplayCacheEntry( clientTime, clientPrincipal ) );
        purgeExpired();
    }


    /*
     * TODO - age needs to be configurable; requires store
     */
    private synchronized void purgeExpired()
    {
        long now = new Date().getTime();

        KerberosTime age = new KerberosTime( now - TWO_WEEKS );

        Iterator it = list.iterator();
        while ( it.hasNext() )
        {
            ReplayCacheEntry entry = ( ReplayCacheEntry ) it.next();
            if ( entry.olderThan( age ) )
            {
                list.remove( entry );
            }
        }
    }

    private class ReplayCacheEntry
    {
        private KerberosTime clientTime;
        private KerberosPrincipal clientPrincipal;


        public ReplayCacheEntry(KerberosTime time, KerberosPrincipal principal)
        {
            clientTime = time;
            clientPrincipal = principal;
        }


        public boolean equals( ReplayCacheEntry other )
        {
            return clientTime.equals( other.clientTime ) && clientPrincipal.equals( other.clientPrincipal );
        }


        public boolean olderThan( KerberosTime time )
        {
            return time.greaterThan( clientTime );
        }
    }
}
